import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { bindModel, prepareProgram, invokeProgram } from '../src/index.js';
import { canonicalBytes } from '../src/canonical.js';
import { modelFor } from '../src/model.js';
import { validatePlan } from '../src/validation.js';
import { createActivation, prepareExpressions, evaluateBinding } from '../src/cel.js';

const pinned = new URL('../../conformance/keir-4/engine-matrix.json', import.meta.url);
const batch = existsSync(pinned) ? pinned : new URL('../../compiler-java/target/numeric-integration.json', import.meta.url);
const { cases } = JSON.parse(readFileSync(batch, 'utf8'));
assert.equal(cases.length, 97);
function prepare(row) {
  const bound = bindModel(canonicalBytes(row.dcm), {});
  assert.equal(bound.diagnostic, null);
  return prepareProgram(canonicalBytes(row.artifact), bound.boundModel);
}
function expected(row) {
  const trace = [{ sequence: 0, event: 'INVOCATION_START' }];
  const ids = ['a', 'b', 'i', 'j', 'flag', 'text', ...(['bind', 'expression-only-size'].includes(row.id) ? ['d'] : [])];
  for (const step of ids) {
    trace.push({ sequence: trace.length, event: 'STEP_START', step });
    trace.push({ sequence: trace.length, event: 'VALUE_BOUND', step });
  }
  if (['ERROR', 'TIME_ERROR'].includes(row.expected)) {
    const code = row.expected === 'TIME_ERROR' ? 'BUDGET_EXCEEDED' : 'EXPRESSION_FAILED';
    trace.push({ sequence: trace.length, event: 'TERMINATED', code });
    return { classification: row.expected === 'TIME_ERROR' ? 'LIMIT_EXCEEDED' : 'FAILED', error: { code, path: '/return' }, effects: [], trace };
  }
  trace.push({ sequence: trace.length, event: 'RETURNED' });
  trace.push({ sequence: trace.length, event: 'TERMINATED' });
  return { classification: 'COMPLETED', result: { value: row.expected }, effects: [], trace };
}
for (const row of cases) test('numeric full outcome/' + row.id, async () => {
  const prepared = prepare(row);
  assert.equal(prepared.diagnostic, null);
  const outcome = await invokeProgram(prepared.prepared, row.input, { bindings: {}, requestKey: 'original-key',
    event: { id: 'event1', source: 'owner1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 10_000 } });
  assert.deepEqual(outcome, expected(row));
});

test('relabeled historical producer and crossed descriptor recipe fail admission', () => {
  const row = structuredClone(cases.find(row => row.id === 'decimal-add'));
  row.artifact.compiler = 'keir-compiler-java/0.4.0';
  assert.equal(prepare(row).diagnostic.code, 'PIN_MISMATCH');
  row.artifact.compiler = 'keir-compiler-java/0.5.0';
  row.artifact.modelRef.descriptorRecipe = 'keir-record-descriptors/2';
  assert.equal(prepare(row).diagnostic.code, 'PIN_MISMATCH');
});

test('invocation projections reuse actual Decimal payloads without shared values', () => {
  const row = cases.find(row => row.id === 'decimal-add');
  const bound = modelFor(bindModel(canonicalBytes(row.dcm), {}).boundModel);
  const plan = validatePlan(row.artifact, bound), prepared = prepareExpressions(plan.contexts, bound);
  const activation = createActivation(row.dcm, prepared), state = {};
  state.input = activation.accept('input', 'Input', row.input, '/input', 'INPUT_INVALID');
  for (const step of row.artifact.steps) state[step.id] = evaluateBinding(step.value, state, row.dcm, prepared, '/steps', activation, step.id);
  const binding = row.artifact.return.fields.value;
  assert.equal(evaluateBinding(binding, state, row.dcm, prepared, '/return', activation, 'sum'), '3.25');
  const message = activation.get('sum', 'Decimal', '3.25', '/return');
  assert.equal(message.$typeName, 'keir.numeric.Decimal');
  assert.equal(activation.get('sum', 'Decimal', '3.25', '/return'), message);
  const before = { ...activation.counts };
  assert.equal(evaluateBinding(binding, state, row.dcm, prepared, '/return', activation), '3.25');
  assert.deepEqual(activation.counts, before);
  const fresh = createActivation(row.dcm, prepared);
  fresh.accept('sum', 'Decimal', '9', '/return', 'EXPRESSION_FAILED');
  assert.notEqual(fresh.get('sum', 'Decimal', '9', '/return'), message);
});
