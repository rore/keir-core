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
const { programs } = JSON.parse(readFileSync(new URL('../../compiler-java/target/numeric-programs.json', import.meta.url), 'utf8'));
assert.equal(cases.length, 97);
test('retained nominal Integer and Decimal names keep String meaning in new artifacts', async () => {
  const row = JSON.parse(readFileSync(new URL('../../compiler-java/target/numeric-retained.json', import.meta.url), 'utf8'));
  const prepared = prepare(row);
  assert.equal(prepared.diagnostic, null);
  const outcome = await invokeProgram(prepared.prepared, row.input, { bindings: {}, requestKey: 'retained-key',
    event: { id: 'event1', source: 'owner1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 10_000 } });
  assert.deepEqual(outcome, { classification: 'COMPLETED', result: { value: true }, effects: [], trace: [
    { sequence: 0, event: 'INVOCATION_START' }, { sequence: 1, event: 'STEP_START', step: 'value' },
    { sequence: 2, event: 'VALUE_BOUND', step: 'value' }, { sequence: 3, event: 'RETURNED' }, { sequence: 4, event: 'TERMINATED' },
  ] });
});
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

function programExpected(name, spec) {
  const trace = [], effects = [];
  const emit = (event, fields = {}) => trace.push({ sequence: trace.length, event, ...fields });
  emit('INVOCATION_START');
  let result, classification = spec.classification ?? 'COMPLETED';
  const error = typeof spec.error === 'string' ? { code: spec.error, path: '/input' } : spec.error;
  if (name === 'quote') {
    if (error) classification = 'FAILED';
    else {
      for (const step of ['rate', 'valid', 'amount', 'allowed']) {
        emit('STEP_START', { step }); emit('VALUE_BOUND', { step });
      }
      result = { decision: spec.decision, amount: spec.amount,
        message: spec.decision === 'ALLOW' ? '' : 'Check the amount, discount and approval limit.' };
      emit('RETURNED');
    }
  } else {
    emit('STEP_START', { step: 'stock' });
    emit('CALL_DISPATCHED', { step: 'stock', capability: 'stock.get' });
    if (!['read-number', 'read-noncanonical'].includes(spec.id)) {
      emit('CALL_SUCCEEDED', { step: 'stock', capability: 'stock.get' });
      emit('STEP_START', { step: 'low' }); emit('VALUE_BOUND', { step: 'low' });
      emit('STEP_START', { step: 'quantity' });
      if (spec.id !== 'overflow') {
        emit('VALUE_BOUND', { step: 'quantity' }); emit('STEP_START', { step: 'order' });
        const taken = Object.hasOwn(spec, 'quantity');
        emit('GUARD_EVALUATED', { step: 'order', taken });
        if (!taken) emit('STEP_SKIPPED', { step: 'order' });
        else {
          const target = { step: 'order', capability: 'stock.request' };
          emit('EFFECT_INTENT', target); emit('CALL_DISPATCHED', target);
          const receipt = { ...target, state: spec.response.effect };
          effects.push(receipt); emit('EFFECT_RECORDED', receipt);
          if (spec.classification === 'COMPLETED') emit('CALL_SUCCEEDED', target);
        }
        if (spec.result) emit('RETURNED');
      }
    }
    result = spec.result;
  }
  emit('TERMINATED', error ? { code: error.code } : {});
  return { classification, ...(error ? { error } : {}), ...(result ? { result } : {}), effects, trace };
}

for (const program of programs) test('complete numeric program/' + program.name, async () => {
  const stock = program.name === 'stock';
  const { cases } = JSON.parse(readFileSync(new URL('../../examples/numeric/' + program.name + '/cases.json', import.meta.url), 'utf8'));
  const implementations = Object.fromEntries(Object.entries(program.dcm.capabilities).map(([capability, cap]) => [capability, {
    version: cap.version, mode: 'simulation', keyType: program.dcm.hostKey.type,
    async invoke(args, protectedArgs, remainingMs, context) {
      const key = context.requestKey, spec = key.spec;
      assert.deepEqual({ ...protectedArgs }, { actorId: 'user1' });
      assert.equal(remainingMs, 10_000);
      assert.deepEqual(context.event, { id: 'event1', source: 'item1' });
      const wanted = { itemId: 'item1', ...(capability === 'stock.request' ? { quantity: spec.quantity } : {}) };
      assert.deepEqual({ ...args }, wanted);
      key.calls.push({ capability, args: structuredClone(args), protectedArgs: structuredClone(protectedArgs) });
      await Promise.resolve(); // Interleave requests in one isolate; this is not parallel CPU execution.
      return structuredClone(capability === 'stock.get' ? { kind: 'SUCCESS', value: { available: spec.available } } : spec.response);
    },
  }]));
  const bound = bindModel(canonicalBytes(program.dcm), implementations);
  assert.equal(bound.diagnostic, null);
  const prepared = prepareProgram(canonicalBytes(program.artifact), bound.boundModel);
  assert.equal(prepared.diagnostic, null);
  // A bounded four-lane batch shares the prepared handle, including successful/failed/guarded values.
  let next = 0;
  await Promise.all(Array.from({ length: 4 }, async () => {
    while (next < cases.length) {
      const spec = cases[next++], key = { spec, calls: [] };
      const outcome = await invokeProgram(prepared.prepared, stock ? { itemId: 'item1' } : spec.input, {
        bindings: stock ? { actorId: 'user1' } : {}, requestKey: key,
        event: { id: 'event1', source: stock ? 'item1' : 'quote1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 10_000 },
      });
      assert.deepEqual(outcome, programExpected(program.name, spec), program.name + '/' + spec.id);
      assert.equal(key.calls.length, !stock ? 0 : Object.hasOwn(spec, 'quantity') ? 2 : 1);
    }
  }));
});
