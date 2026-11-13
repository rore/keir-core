// Finite hosted production-runtime measurements, not the common declaration matrix.
import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { bindModel, prepareProgram, invokeProgram } from '../src/index.js';
import { canonicalBytes } from '../src/canonical.js';
import { modelFor } from '../src/model.js';
import { validatePlan, validateValue } from '../src/validation.js';
import { createActivation, prepareExpressions, evaluateBinding } from '../src/cel.js';

console.log('NUMERIC_READY');
const fixture = readFileSync(process.argv[2]), document = JSON.parse(fixture);
const digest = bytes => 'sha256:' + createHash('sha256').update(bytes).digest('hex');
const ns = ms => Math.round(ms * 1_000_000);
function summary(samples, wall) {
  samples.sort((a, b) => a - b);
  return { p50Ns: samples[499], p95Ns: samples[949], maxNs: samples[999], wallNs: ns(wall), perSecond: Math.floor(1_000_000 / wall) };
}
async function measure(action, verify) {
  for (let i = 0; i < 100; i++) verify(await action());
  const samples = [], start = performance.now();
  for (let i = 0; i < 1000; i++) {
    const at = performance.now(), value = action();
    const result = value instanceof Promise ? await value : value;
    samples.push(ns(performance.now() - at)); verify(result);
  }
  return { calls: 1100, samples: 1000, warmups: 100, ...summary(samples, performance.now() - start) };
}
function input(row, i, failure) {
  return { owner: 'owner' + i, value: failure ? row.failure.value : row.values[i % 2], divisor: failure ? row.failure.divisor : row.divisor };
}
function expected(row, i, failure) {
  const trace = [], emit = (event, fields = {}) => trace.push({ sequence: trace.length, event, ...fields });
  emit('INVOCATION_START');
  if (!failure || row.failure.code !== 'INPUT_INVALID') for (const step of ['a', 'b', 'c', 'd']) {
    emit('STEP_START', { step }); if (failure && step === 'c') break;
    emit('VALUE_BOUND', { step });
  }
  if (!failure) emit('RETURNED');
  emit('TERMINATED', failure ? { code: row.failure.code } : {});
  return { classification: failure ? 'FAILED' : 'COMPLETED', effects: [], trace,
    ...(failure ? { error: { code: row.failure.code, path: row.failure.path } } : { result: { owner: 'owner' + i, value: row.expected[i % 2] } }) };
}
function invoke(prepared, row, i, failure) {
  return invokeInput(prepared, input(row, i, failure), i);
}
function invokeInput(prepared, input, i) {
  return invokeProgram(prepared, input, { bindings: {}, requestKey: Object.freeze({}),
    event: { id: 'event1', source: 'owner' + i }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 10_000 } });
}
const reports = [];
for (const row of document.cases) {
  const modelBytes = canonicalBytes(row.dcm), artifactBytes = canonicalBytes(row.artifact);
  const bound = bindModel(modelBytes, {}); assert.equal(bound.diagnostic, null);
  const prepared = prepareProgram(artifactBytes, bound.boundModel); assert.equal(prepared.diagnostic, null);
  const model = modelFor(bound.boundModel), plan = validatePlan(row.artifact, model), expressions = prepareExpressions(plan.contexts, model);
  global.gc?.(); const before = process.memoryUsage();
  const phases = {};
  phases.bind = await measure(() => bindModel(modelBytes, {}), value => assert.equal(value.diagnostic, null));
  phases.prepare = await measure(() => prepareProgram(artifactBytes, bound.boundModel), value => assert.equal(value.diagnostic, null));
  phases.validateProject = await measure(() => {
    const activation = createActivation(row.dcm, expressions);
    const value = activation.accept('input', 'Input', input(row, 0, false), '/input', 'INPUT_INVALID');
    activation.get('input', 'Input', value, '/input'); return activation;
  }, activation => {
    assert.equal(activation.counts.projections, 1); const before = { ...activation.counts };
    activation.get('input', 'Input', input(row, 0, false), '/input'); assert.deepEqual(activation.counts, before);
  });
  phases.invoke = await measure(() => invoke(prepared.prepared, row, 0, false), value => assert.deepEqual(value, expected(row, 0, false)));
  const result = { owner: 'owner0', value: row.expected[0] };
  phases.validateResult = await measure(() => validateValue(row.dcm, 'Output', result, '/result', 'EXPRESSION_FAILED'), value => assert.deepEqual(value, result));
  const invalid = { owner: 'owner0', value: '1'.repeat(257), divisor: row.divisor };
  const rejected = { classification: 'FAILED', effects: [], error: { code: 'INPUT_INVALID', path: '/input' }, trace: [
    { sequence: 0, event: 'INVOCATION_START' }, { sequence: 1, event: 'TERMINATED', code: 'INPUT_INVALID' },
  ] };
  phases.reject = await measure(() => invokeInput(prepared.prepared, invalid, 0), value => assert.deepEqual(value, rejected));
  const activation = createActivation(row.dcm, expressions), state = {};
  state.input = activation.accept('input', 'Input', input(row, 0, false), '/input', 'INPUT_INVALID');
  for (const step of row.artifact.steps) state[step.id] = evaluateBinding(step.value, state, row.dcm, expressions, '/steps', activation, step.id);
  assert.equal(state.d, row.expected[0]);
  const batches = [];
  for (let batch = 0; batch < 3; batch++) {
    let next = 0; const start = performance.now(), samples = [];
    await Promise.all(Array.from({ length: 4 }, async () => {
      while (next < 1000) {
        const i = next++, failure = i % 7 === 0, oracle = expected(row, i, failure), at = performance.now();
        const outcome = await invoke(prepared.prepared, row, i, failure);
        samples.push(ns(performance.now() - at)); assert.deepEqual(outcome, oracle);
      }
    }));
    const wall = performance.now() - start; global.gc?.();
    batches.push({ invocations: 1000, lanes: 4, ...summary(samples, wall), memoryAfterGcRequest: process.memoryUsage() });
  }
  reports.push({ id: row.id, phases, memoryAfterInitialGcRequest: before, memoryAfter: process.memoryUsage(),
    allocationBytes: 'unsupported', chainProjections: activation.counts.projections, chainBoundaryParses: activation.counts.boundaryParses,
    batches, expectedSha256: digest(canonicalBytes(expected(row, 0, false))) });
}
const report = { status: 'passed', fixtureSha256: digest(fixture), engine: 'Node', nodeVersion: process.version, flags: process.execArgv, cases: reports,
  limitations: ['Production bind/prepare/invoke; assertions excluded from latency samples, included in wall throughput.',
    '100 warmups and 1000 samples per phase; warm JIT/GC effects and phase order are not controlled experiments.',
    'Four same-isolate request-like lanes, at most four in flight; three fixed 1000-request batches. This is not CPU parallelism.',
    'Requested GC and before/after RSS/heap do not measure allocation or guarantee peak memory; supervisor sampling may miss spikes.',
    'Boundary parses/projections are directly counted. Decimal operand/result reparsing is retained and not instrumented; no coefficient memo or parse-once claim.',
    'validateProject measures complete input admission/projection; validateResult measures complete output admission without unnecessary output-engine projection.'] };
writeFileSync(process.argv[3], JSON.stringify(report));
