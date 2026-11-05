import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import * as api from '../../runtime-node/src/index.js';
import { parseCanonical, parseModelJson } from '../../runtime-node/src/canonical.js';
import { boundFixture } from '../generic-hooks/bound-effect.mjs';

const [, , directory, manifest] = process.argv;
const read = name => readFileSync(join(directory, name));
const { limits, dcmMax, dcmOver, wideModel, admissible } = JSON.parse(read(manifest));
const { warmups, repeats, handles, adapterMs } = limits;
const timings = {}, facts = {};
const us = () => Math.floor(performance.now() * 1000);
const retained = () => { global.gc(); return process.memoryUsage().heapUsed; };
const baseline = retained();
const memory = () => {
  const { heapUsed, rss } = process.memoryUsage();
  return { heap: heapUsed, rss };
};
async function measure(name, action) {
  const before = memory(), cpu = process.cpuUsage(), start = us();
  const result = await action();
  const elapsedUs = us() - start, { user, system } = process.cpuUsage(cpu), after = memory();
  timings[name] = { elapsedUs, cpuUs: user + system,
    sampledHeapBytes: Math.max(before.heap, after.heap), sampledRssBytes: Math.max(before.rss, after.rss) };
  return result;
}
function diagnostic(id, actual, code, path) {
  assert.deepEqual({ code: actual?.code, path: actual?.path }, { code, path }, id);
  facts[id] = { code, path };
}
const observed = ({ classification, effects, error }) => ({
  classification, effects: effects.map(effect => effect.state), ...(error ? { error } : {}),
});
function outcome(result, classification, states) {
  assert.equal(result.classification, classification, JSON.stringify(observed(result)));
  assert.deepEqual(observed(result).effects, states);
  return result;
}
function bind(bytes, host) {
  const result = api.bindModel(bytes, host.implementations);
  assert.ok(result.boundModel, JSON.stringify(result.diagnostic));
  return result.boundModel;
}
function prepare(bytes, model) {
  const result = api.prepareProgram(bytes, model);
  assert.ok(result.prepared, JSON.stringify(result.diagnostic));
  return result.prepared;
}
function invoke(host, handle, budget = 2000, input = { taskId: 'task1' }) {
  const invocation = host.invocationFor({ expiresAtMs: budget }), start = performance.now();
  invocation.deadline.clock.nowMs = () => Math.floor(performance.now() - start);
  return api.invokeProgram(handle, input, invocation);
}
function clear(host) {
  for (const list of [host.calls, host.nativeActions, host.contexts, host.budgets]) list.length = 0;
}
async function warm(id, host, prepared, classification, states) {
  const one = async () => {
    const result = outcome(await invoke(host, prepared), classification, states);
    const writes = host.calls.filter(call => call.capability === 'tasks.create');
    assert.equal(writes.length, states.length);
    assert.equal(result.trace.some(event => event.event === 'EFFECT_INTENT'), states.length > 0);
    if (states.length) assert.equal(writes[0].args.title, id === 'title-256' ? 'x'.repeat(255) + '!' : 'Valid title');
    clear(host);
    return result;
  };
  for (let i = 0; i < warmups; i++) await one();
  return measure('invoke-' + id, async () => {
    const samples = [];
    let last;
    for (let i = 0; i < repeats; i++) {
      const start = us();
      last = await one();
      samples.push(us() - start);
    }
    samples.sort((a, b) => a - b);
    timings['distribution-' + id] = { minUs: samples[0], medianUs: samples[Math.floor(repeats / 2)], maxUs: samples.at(-1) };
    return last;
  });
}

const noCall = boundFixture(api, { program: 'no-call' });
const base = boundFixture(api);
const modelMax = read(dcmMax), artifactMax = read('artifact-max.json');
await measure('parse-dcm-max', () => parseModelJson(modelMax));
await measure('parse-artifact-max', () => parseCanonical(artifactMax));
const bound = await measure('bind-dcm-max', () => bind(modelMax, base));
const invalidModel = read(dcmOver);
const rejected = await measure('bind-dcm-over', () => api.bindModel(invalidModel, base.implementations));
diagnostic('dcm-over', rejected.diagnostic, 'BUDGET_EXCEEDED', '/binding/dcm');
for (const id of admissible) {
  const bytes = read(id + '.json');
  const prepared = await measure('prepare-' + id, () => prepare(bytes, bound));
  if (id === 'aggregate-max') await warm(id, noCall, prepared, 'COMPLETED', []);
  else outcome(await invoke(noCall, prepared), 'COMPLETED', []);
  facts[id] = 'COMPLETED';
}
for (const id of ['artifact-max', 'artifact-over', 'aggregate-forged', 'nodes-forged', 'depth-forged']) {
  const bytes = read(id + '.json');
  const rejected = await measure('prepare-' + id, () => api.prepareProgram(bytes, bound));
  diagnostic(id, rejected.diagnostic, id === 'artifact-max' ? 'ARTIFACT_INVALID' : 'BUDGET_EXCEEDED',
    id.endsWith('-forged') ? '/artifact/expressions' : '/artifact');
}

const wideBytes = read(wideModel), wideArtifact = read('value.json');
const wide = await measure('bind-wide', () => bind(wideBytes, base));
const wideProgram = await measure('prepare-value', () => prepare(wideArtifact, wide));
for (const id of ['value-max', 'value-over']) {
  const input = parseCanonical(read(id + '.json'));
  const result = await measure('invoke-' + id, () => invoke(noCall, wideProgram, 2000, input));
  facts[id] = observed(outcome(result, id === 'value-max' ? 'COMPLETED' : 'LIMIT_EXCEEDED', []));
}

const title = boundFixture(api, { program: 'title' });
const titleBound = await measure('bind-title', () => bind(title.dcmBytes, title));
for (const id of ['title-256', 'title-257', 'concat', 'boolean-valid', 'boolean-over']) {
  const program = id.startsWith('title') ? 'title' : id.startsWith('boolean') ? 'boolean' : id;
  const bytes = read(program + '.json');
  const prepared = await measure('prepare-' + id, () => prepare(bytes, titleBound));
  title.state.title = 'x'.repeat(id === 'title-256' ? 255 : 256);
  title.state.project = id === 'boolean-over' ? 'project2' : 'project1';
  const success = id === 'title-256' || id === 'boolean-valid';
  facts[id] = observed(await warm(id, title, prepared, success ? 'COMPLETED' : 'LIMIT_EXCEEDED', success ? ['CONFIRMED'] : []));
}

const main = read('main.json'), afterWrite = read('confirmed.json');
let prepared = await measure('prepare-handles', () => Array.from({ length: handles }, () => prepare(main, bound)));
const heapWithLiveHandlesBytes = retained();
const overlap = Promise.withResolvers(), dispatched = Promise.withResolvers();
base.state.afterRead = () => {
  if (base.calls.length === handles) dispatched.resolve();
  return overlap.promise;
};
facts.overlap = await measure('invoke-overlap', async () => {
  const active = prepared.map(handle => invoke(base, handle));
  await dispatched.promise;
  assert.equal(base.calls.length, handles);
  assert.ok(base.calls.every(call => call.capability === 'tasks.get'));
  overlap.resolve();
  for (const result of await Promise.all(active)) outcome(result, 'COMPLETED', ['CONFIRMED']);
  return active.length;
});
prepared = null;
clear(base);
base.state.afterRead = undefined;
const heapAfterReleasingHandlesBytes = retained();

const normal = prepare(main, bound), confirmedFirst = prepare(afterWrite, bound);
for (const prior of [false, true]) {
  const id = prior ? 'prior-confirmed' : 'settled-unknown';
  const pending = Promise.withResolvers();
  if (prior) base.state.afterRead = () => base.calls.length === 3 ? pending.promise : undefined;
  else base.state.afterWrite = () => pending.promise;
  const result = await measure(id, () => invoke(base, prior ? confirmedFirst : normal, adapterMs));
  outcome(result, prior ? 'LIMIT_EXCEEDED' : 'UNCERTAIN', [prior ? 'CONFIRMED' : 'UNKNOWN']);
  assert.deepEqual(base.calls.map(call => call.capability),
    prior ? ['tasks.get', 'tasks.create', 'tasks.get'] : ['tasks.get', 'tasks.create']);
  assert.equal(base.nativeActions.length, prior ? 3 : 2);
  const snapshot = structuredClone(result);
  pending.resolve();
  await new Promise(resolve => setImmediate(resolve));
  assert.deepEqual(result, snapshot);
  facts[id] = observed(result);
  clear(base);
  base.state.afterWrite = undefined;
}
console.log(JSON.stringify({ version: process.versions.node, environment: api.ENVIRONMENT, timings, facts,
  peakRssBytes: process.resourceUsage().maxRSS * 1024, heapWithLiveHandlesBytes, heapAfterReleasingHandlesBytes,
  retainedHeapBytes: { baseline, after: retained() } }));
