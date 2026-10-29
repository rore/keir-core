import assert from 'node:assert/strict';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { performance } from 'node:perf_hooks';
import { Worker } from 'node:worker_threads';
import { createHash } from 'node:crypto';
import { pb, cel, prepare, input, evaluate, validateChecked, limits, CheckedExprSchema } from './profile.mjs';

const directory = new URL('./', import.meta.url);
const bundlePath = process.argv[2] ?? fileURLToPath(new URL('target/probe/bundle.json', directory));
const bundleBytes = readFileSync(bundlePath);
const bundle = JSON.parse(bundleBytes);
const fixtures = JSON.parse(readFileSync(new URL('fixtures.json', directory)));
const before = process.memoryUsage();
const start = performance.now();
const prepared = prepare(bundle);
const prepareMs = performance.now() - start;
let checks = 0;
function equal(actual, expected) { assert.deepEqual(actual, expected); checks++; }
function rejects(fn, pattern) { assert.throws(fn, pattern); checks++; }

for (const test of fixtures.expressions) {
  for (const [i, fixture] of fixtures.inputs.entries()) {
    const value = evaluate(prepared, test.id, input(fixture.task, prepared.descriptor));
    equal(value, test.expected[i]);
    const java = bundle.javaResults.find(r => r.id === test.id && r.input === fixture.name);
    assert(java, `missing Java result: ${test.id}/${fixture.name}`);
    equal(value, java.value);
  }
}
equal(new Set(bundle.rejects.map(x => x.id)), new Set(fixtures.rejects.map(x => x.id)));
const task = fixtures.inputs[0].task;
const invalids = [];
for (const field of Object.keys(task)) {
  const missing = { ...task }; delete missing[field]; invalids.push(missing);
  invalids.push({ ...task, [field]: null });
}
invalids.push({ ...task, enabled: 'true' }, { ...task, status: 'UNKNOWN' }, { ...task, id: '' }, { ...task, extra: true }, { ...task, title: 'a'.repeat(257) }, { ...task, title: '\ud800' });
for (const invalid of invalids) rejects(() => input(invalid, prepared.descriptor));
equal(bundle.javaProjectionRejects, invalids.length + 1);
equal(evaluate(prepared, 'status', {}).error, true);
equal(evaluate(prepared, 'short_circuit', {}), false);
// Raw proto3 defaults are observable: only the pre-projection gate enforces DCM required fields.
equal(evaluate(prepared, 'field', { task: pb.create(prepared.descriptor) }), '');
equal(evaluate(prepared, 'field', input({ ...task, title: 'é'.repeat(128) }, prepared.descriptor)), 'OPEN');
rejects(() => input({ ...task, title: 'é'.repeat(129) }, prepared.descriptor));

function mutated(id, mutate) {
  const checked = structuredClone(prepared.expressions.get(id).checked);
  mutate(checked);
  return checked;
}
rejects(() => validateChecked(mutated('status', c => { c.referenceMap[c.expr.id].overloadId = ['not_an_admitted_overload']; })), /overload/);
rejects(() => validateChecked(mutated('status', c => { c.expr.exprKind.value.function = 'matches'; })), /call/);
rejects(() => validateChecked(mutated('status', c => { delete c.typeMap[c.expr.id]; })), /type/);
rejects(() => validateChecked(mutated('field', c => { c.expr.exprKind.value.testOnly = true; })), /selection/);
rejects(() => prepare({ ...bundle, expressions: [{ ...bundle.expressions[0], checkedBase64: Buffer.alloc(limits.checkedBytes + 1).toString('base64') }] }), /checked bytes/);
rejects(() => prepare({ ...bundle, expressions: [{ ...bundle.expressions[0], checkedBase64: Buffer.from([255]).toString('base64') }] }));
rejects(() => prepare({ ...bundle, expressions: [{ ...bundle.expressions[0], source: 'x'.repeat(limits.sourceBytes + 1) }] }), /source bytes/);
rejects(() => prepare({ ...bundle, descriptorBase64: Buffer.alloc(limits.descriptorBytes + 1).toString('base64') }), /descriptor bytes/);

// CEL-ES ignores checked maps; our separate gate rejects their corruption before planning.
const damaged = mutated('status', c => { c.typeMap = {}; c.referenceMap = {}; });
equal(cel.plan(prepared.env, damaged)(input(task, prepared.descriptor)), false);
rejects(() => validateChecked(damaged));

// Re-encode is not required to execute: both runtimes consume Java's original checked bytes.
equal(pb.fromBinary(CheckedExprSchema, Buffer.from(bundle.expressions[0].checkedBase64, 'base64')).expr.id, prepared.expressions.get(bundle.expressions[0].id).checked.expr.id);
const vars = input({ ...task, title: 'é'.repeat(128) }, prepared.descriptor);
equal(evaluate(prepared, 'bounded_string', vars), true);
for (let i = 0; i < 1000; i++) evaluate(prepared, 'classify', vars);
const durations = [];
for (let i = 0; i < 1000; i++) {
  const t = performance.now();
  const value = evaluate(prepared, 'classify', vars);
  durations.push(performance.now() - t);
  equal(value, 'ACTIVE');
}
function percentiles(values) {
  const sorted = [...values].sort((a, b) => a - b);
  return { p50: sorted[Math.floor(sorted.length * .5)], p95: sorted[Math.floor(sorted.length * .95)], p99: sorted[Math.floor(sorted.length * .99)], max: sorted.at(-1) };
}
const maxAstDurations = [];
for (let i = 0; i < 1000; i++) evaluate(prepared, 'max_nodes', vars);
for (let i = 0; i < 1000; i++) {
  const t = performance.now();
  const value = evaluate(prepared, 'max_nodes', vars);
  maxAstDurations.push(performance.now() - t);
  equal(value, true);
}
const stringVars = input({ ...task, title: 'é'.repeat(127) + 'ê' }, prepared.descriptor);
const maxStringDurations = [];
for (let i = 0; i < 1000; i++) evaluate(prepared, 'bounded_string', stringVars);
for (let i = 0; i < 1000; i++) {
  const t = performance.now();
  const value = evaluate(prepared, 'bounded_string', stringVars);
  maxStringDurations.push(performance.now() - t);
  equal(value, false);
}
const workerStart = performance.now();
const worker = new Worker(new URL('worker.mjs', directory), { workerData: { bundle, task: { ...task, title: 'é'.repeat(128) } }, resourceLimits: { maxOldGenerationSizeMb: 32 } });
function message() {
  return new Promise((resolve, reject) => {
    const cleanup = () => { clearTimeout(timer); worker.off('message', received); worker.off('error', failed); worker.off('exit', exited); };
    const received = value => { cleanup(); resolve(value); };
    const failed = error => { cleanup(); reject(error); };
    const exited = code => failed(new Error(`Worker exited before response: ${code}`));
    const timer = setTimeout(() => failed(new Error('Worker response deadline')), 10000);
    worker.once('message', received); worker.once('error', failed); worker.once('exit', exited);
  });
}
let startupMs;
let workerMemory;
const roundTrips = [];
let termination;
try {
  const ready = await message();
  equal(ready.ready, true);
  startupMs = performance.now() - workerStart;
  for (let i = 0; i < 200; i++) {
    const t = performance.now();
    const response = message();
    worker.postMessage({ id: 'classify' });
    const result = await response;
    equal(result.value, 'ACTIVE');
    workerMemory = result.memory;
    roundTrips.push(performance.now() - t);
  }
  const batchStart = performance.now();
  let completed = false;
  worker.once('message', () => { completed = true; });
  worker.postMessage({ id: 'classify', iterations: 10000000 });
  await new Promise(resolve => setTimeout(resolve, 20));
  equal(completed, false);
  const terminationStart = performance.now();
  const exitCode = await worker.terminate();
  termination = { supervisorWaitMs: terminationStart - batchStart, stopMs: performance.now() - terminationStart, exitCode, batchCompleted: completed };
} finally {
  await worker.terminate();
}
const report = {
  node: process.version, packages: { cel: '0.6.1', celSpec: '0.6.1', protobuf: '2.6.2' },
  bundleSha256: createHash('sha256').update(bundleBytes).digest('hex'),
  checks, expressions: fixtures.expressions.length, parityCases: fixtures.expressions.length * fixtures.inputs.length,
  rejectedExpressions: bundle.rejects.length, rejectedInputs: invalids.length + 1,
  limits, astNodes: Object.fromEntries([...prepared.expressions].map(([id, x]) => [id, x.nodes])),
  observedOverloads: [...new Set([...prepared.expressions.values()].flatMap(x => Object.values(x.checked.referenceMap).flatMap(r => r.overloadId)))].sort(),
  prepareMs, inProcessMs: percentiles(durations), maxAstInProcessMs: percentiles(maxAstDurations), maxStringInProcessMs: percentiles(maxStringDurations), workerStartupMs: startupMs, workerRoundTripMs: percentiles(roundTrips),
  termination, memory: { before, after: process.memoryUsage(), worker: workerMemory },
  caveat: 'Single local qualification run. Worker termination used a batch of real scalar CEL evaluations, not one hostile CEL expression. Limits are probe gates, not proven production CPU/memory/deadline guarantees.'
};
const reportPath = new URL('target/node-report.json', directory);
mkdirSync(new URL('target/', directory), { recursive: true });
writeFileSync(reportPath, JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
