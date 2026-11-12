import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { fromBinary, toBinary } from '@bufbuild/protobuf';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { bindModel, prepareProgram, invokeProgram } from '../src/index.js';
import { canonicalBytes } from '../src/canonical.js';
import { prepareExpressions, evaluateBinding } from '../src/cel.js';
import { modelFor } from '../src/model.js';
import { validatePlan } from '../src/validation.js';

const read = name => readFileSync(new URL('../../conformance/keir-3/' + name, import.meta.url));
const at = '2026-01-01T00:00:00.000Z';
const input = createNew => ({ createNew, existingReference: 'existing', label: 'label', enabled: true, state: 'EXISTING', at });
const value = reference => ({ reference, flag: false, note: '', enabled: false, state: 'CREATED', at, labels: ['kept'] });
function host(program = 'guarded-result', modelBytes = read(program === 'guarded-result-read' ? 'dcm-guarded-result-read.json' : 'dcm-guarded-result.json')) {
  const key = Object.freeze({}), calls = [], state = { now: 0,
    response: { kind: 'SUCCESS', effect: 'CONFIRMED', value: value('actual') },
    lookupResponse: { kind: 'SUCCESS', value: value('read-actual') } };
  const model = JSON.parse(modelBytes);
  const adapters = Object.fromEntries(Object.keys(model.capabilities).map(id => [id, {
    version: '1', mode: 'simulation', keyType: 'sample.RecordKey/1',
    invoke(args, protectedArgs, budget, context) {
      try {
        assert.equal(context.requestKey, key);
        assert.deepEqual({ ...protectedArgs }, { actorId: 'actor' });
        assert.deepEqual({ ...args }, id === 'records.create' ? { label: 'label' }
          : { reference: program === 'guarded-result-read' ? 'existing' : 'actual' });
      } catch (error) { state.callbackError = error; throw error; }
      calls.push(id);
      if (state.throws) throw Error('adapter failed');
      if (state.expired) state.now = 1000;
      return id === 'records.create' ? state.response : state.lookupResponse;
    },
  }]));
  const bound = bindModel(modelBytes, adapters);
  assert.equal(bound.diagnostic, null);
  const artifactBytes = read('artifact-' + program + '.json');
  const preparation = prepareProgram(artifactBytes, bound.boundModel);
  assert.equal(preparation.diagnostic, null);
  const invocation = { bindings: { actorId: 'actor' }, requestKey: key,
    event: { id: 'event', source: 'existing' }, deadline: { clock: { nowMs: () => state.now }, expiresAtMs: 1000 } };
  return { calls, state, artifactBytes, bound: bound.boundModel, prepared: preparation.prepared,
    invoke: async guard => {
      const outcome = await invokeProgram(preparation.prepared, input(guard), invocation);
      if (state.callbackError) throw state.callbackError;
      return outcome;
    } };
}

test('retains the actual guarded result with fresh sparse activations', async () => {
  const f = host();
  for (const guard of [false, true, false, true]) {
    const outcome = await f.invoke(guard);
    assert.equal(outcome.classification, 'COMPLETED', JSON.stringify(outcome));
    assert.deepEqual(outcome.result, { reference: guard ? 'actual' : 'existing', message: guard ? '' : 'unchanged',
      enabled: !guard, flag: !guard, state: guard ? 'CREATED' : 'EXISTING', at });
    assert.deepEqual(outcome.effects, guard ? [{ step: 'created', capability: 'records.create', state: 'CONFIRMED' }] : []);
  }
  assert.deepEqual(f.calls, ['records.create', 'records.create']);
});

test('overlapping guarded results keep values, receipts and contexts invocation-local', { timeout: 5000 }, async () => {
  const gates = [0, 1].map(() => ({ entered: Promise.withResolvers(), response: Promise.withResolvers() }));
  const calls = [], running = [], settled = new Set();
  const cases = ['slow', 'fast', 'skipped'].map((name, index) => ({
    name,
    input: { ...input(name !== 'skipped'), label: name, existingReference: 'existing-' + name,
      enabled: index !== 0, at: `2026-01-0${index + 1}T00:00:00.000Z` },
    invocation: { bindings: { actorId: 'actor-' + name }, requestKey: Object.freeze({}),
      event: { id: 'event-' + name, source: 'source-' + name },
      deadline: { clock: { nowMs: () => index * 10 }, expiresAtMs: 1000 + index * 10 } },
  }));
  const adapters = Object.fromEntries(['records.create', 'records.lookup'].map(capability => [capability, {
    version: '1', mode: 'simulation', keyType: 'sample.RecordKey/1',
    invoke(args, protectedArgs, remainingMs, context) {
      const gate = gates[calls.length];
      const call = { capability, args, protectedArgs, remainingMs, context };
      calls.push(call);
      if (!gate) return Promise.reject(Error('Unexpected additional dispatch'));
      gate.entered.resolve(call);
      return gate.response.promise;
    },
  }]));
  const bound = bindModel(read('dcm-guarded-result.json'), adapters);
  assert.equal(bound.diagnostic, null);
  const prepared = prepareProgram(read('artifact-guarded-result.json'), bound.boundModel);
  assert.equal(prepared.diagnostic, null);
  const begin = item => {
    const pending = invokeProgram(prepared.prepared, item.input, item.invocation);
    running.push(pending);
    pending.then(() => settled.add(item.name), () => settled.add(item.name));
    return pending;
  };
  const within = async (pending, phase) => {
    let timer;
    try {
      return await Promise.race([pending, new Promise((_, reject) => {
        timer = setTimeout(() => reject(Error('Timed out waiting for ' + phase)), 1000);
      })]);
    } finally { clearTimeout(timer); }
  };
  const assertCall = (call, item) => {
    assert.equal(call.capability, 'records.create');
    assert.deepEqual({ ...call.args }, { label: item.name });
    assert.deepEqual({ ...call.protectedArgs }, item.invocation.bindings);
    assert.strictEqual(call.context.requestKey, item.invocation.requestKey);
    assert.deepEqual({ ...call.context.event }, item.invocation.event);
    assert.equal(call.context.deadline.expiresAtMs, item.invocation.deadline.expiresAtMs);
    assert.equal(call.context.deadline.clock.nowMs(), item.invocation.deadline.clock.nowMs());
    assert.ok(call.remainingMs > 0 && call.remainingMs <= 1000);
  };
  const returnedSlow = { ...value('returned-slow'), flag: true, enabled: true, note: 'slow-note', at: '2026-01-04T00:00:00.000Z' };
  const returnedFast = { ...value('returned-fast'), note: 'fast-note', at: '2026-01-05T00:00:00.000Z' };
  delete returnedFast.enabled;
  const replies = [returnedSlow, returnedFast].map(value => ({ kind: 'SUCCESS', effect: 'CONFIRMED', value }));
  const receipt = [{ step: 'created', capability: 'records.create', state: 'CONFIRMED' }];
  const expectedSlow = { reference: 'returned-slow', message: 'slow-note', enabled: true, flag: true,
    state: 'CREATED', at: '2026-01-04T00:00:00.000Z' };
  const expectedFast = { reference: 'returned-fast', message: 'fast-note', enabled: false, flag: false,
    state: 'CREATED', at: '2026-01-05T00:00:00.000Z' };
  const expectedSkipped = { reference: 'existing-skipped', message: 'unchanged', enabled: true, flag: true,
    state: 'EXISTING', at: '2026-01-03T00:00:00.000Z' };
  try {
    const slow = begin(cases[0]);
    assertCall(await within(gates[0].entered.promise, 'first dispatch'), cases[0]);
    const fast = begin(cases[1]);
    assertCall(await within(gates[1].entered.promise, 'second dispatch'), cases[1]);
    const skipped = await within(begin(cases[2]), 'skipped invocation');
    assert.equal(skipped.classification, 'COMPLETED', JSON.stringify(skipped));
    assert.deepEqual(skipped.result, expectedSkipped);
    assert.deepEqual(skipped.effects, []);
    assert.deepEqual(skipped.trace.filter(row => row.event === 'STEP_SKIPPED').map(row => row.step), ['created']);
    assert.equal(calls.length, 2);
    assert.equal(settled.has('slow'), false);
    assert.equal(settled.has('fast'), false);

    gates[1].response.resolve(replies[1]);
    const fastOutcome = await within(fast, 'second response');
    assert.equal(fastOutcome.classification, 'COMPLETED', JSON.stringify(fastOutcome));
    assert.deepEqual(fastOutcome.result, expectedFast);
    assert.deepEqual(fastOutcome.effects, receipt);
    assert.equal(settled.has('slow'), false);

    gates[0].response.resolve(replies[0]);
    const slowOutcome = await within(slow, 'first response');
    assert.equal(slowOutcome.classification, 'COMPLETED', JSON.stringify(slowOutcome));
    assert.deepEqual(slowOutcome.result, expectedSlow);
    assert.deepEqual(slowOutcome.effects, receipt);
    assert.deepEqual(fastOutcome.result, expectedFast);
    assert.deepEqual(fastOutcome.effects, receipt);
    assert.deepEqual(skipped.result, expectedSkipped);
    assert.deepEqual(skipped.effects, []);
    assert.equal(calls.length, 2);
    calls.forEach((call, index) => assertCall(call, cases[index]));
  } finally {
    gates.forEach((gate, index) => gate.response.resolve(replies[index]));
    await within(Promise.allSettled(running), 'cleanup');
  }
});

test('retains unused successful results but never requires or converts skipped declarations', async () => {
  const f = host('guarded-result-unused');
  for (const guard of [true, false, true, false]) {
    const outcome = await f.invoke(guard);
    assert.equal(outcome.classification, 'COMPLETED');
    assert.equal(outcome.result.reference, 'existing');
    assert.equal(outcome.result.message, 'unused');
  }
  const bound = modelFor(f.bound), artifact = JSON.parse(f.artifactBytes);
  const prepared = prepareExpressions(validatePlan(artifact, bound).contexts, bound);
  assert.equal(evaluateBinding({ expr: 'e2' }, { input: input(false), createNew: false }, bound.dcm, prepared, '/test'), at);
  for (const state of [{ input: input(true), createNew: true }, { input: input(false) }, { createNew: false },
    { input: input(false), createNew: 'false' }]) {
    assert.throws(() => evaluateBinding({ expr: 'e2' }, state, bound.dcm, prepared, '/test'),
      error => error.code === 'EXPRESSION_FAILED' && error.path === '/test');
  }
  const poisoned = { input: input(false), createNew: false };
  Object.defineProperty(poisoned, 'created', { get() { throw Error('Skipped record was accessed'); } });
  assert.equal(evaluateBinding({ expr: 'e2' }, poisoned, bound.dcm, prepared, '/test'), at);
});

test('optional scalar reads remain lazy and a true-guard missing record cannot become field absence', async () => {
  const f = host();
  for (const note of [undefined, '', 'present']) {
    const returned = value('actual');
    if (note === undefined) { delete returned.note; delete returned.enabled; }
    else returned.note = note;
    f.state.response = { kind: 'SUCCESS', effect: 'CONFIRMED', value: returned };
    const outcome = await f.invoke(true);
    assert.equal(outcome.classification, 'COMPLETED');
    assert.equal(outcome.result.message, note ?? '');
    assert.equal(outcome.result.enabled, false);
  }
  const bound = modelFor(f.bound), prepared = prepareExpressions(validatePlan(JSON.parse(f.artifactBytes), bound).contexts, bound);
  assert.throws(() => evaluateBinding({ expr: 'e6' }, { input: input(true), createNew: true, reference: 'actual' },
    bound.dcm, prepared, '/test'), error => error.code === 'EXPRESSION_FAILED');
});

test('guarded reads feed decisions with no write authority or receipts', async () => {
  const f = host('guarded-result-read');
  for (const [guard, denied] of [[false, false], [true, false], [true, true], [false, true]]) {
    f.state.lookupResponse = { kind: 'SUCCESS', value: { ...value('read-actual'), flag: denied } };
    const outcome = await f.invoke(guard);
    assert.equal(outcome.classification, 'COMPLETED');
    assert.deepEqual(outcome.result, { decision: guard && denied ? 'DENY' : 'ALLOW', message: guard ? '' : 'skipped' });
    assert.deepEqual(outcome.effects, []);
  }
  assert.deepEqual(f.calls, ['records.lookup', 'records.lookup']);
});

test('later rejection handlers can read an earlier guarded result without seeing their own result', async () => {
  const f = host('guarded-result-handler');
  f.state.lookupResponse = { kind: 'BUSINESS_REJECTION', code: 'DENIED' };
  const outcome = await f.invoke(true);
  assert.equal(outcome.classification, 'BUSINESS_REJECTION');
  assert.deepEqual(outcome.error, { code: 'DENIED', path: '/steps/3' });
  assert.equal(outcome.result.reference, 'actual');
  assert.deepEqual(outcome.effects, [{ step: 'created', capability: 'records.create', state: 'CONFIRMED' }]);
  assert.deepEqual(f.calls, ['records.create', 'records.lookup']);
});

test('guarded failures never choose fallback and confirmed effects survive malformed or late values', async () => {
  const cases = [
    [{ kind: 'BUSINESS_REJECTION', code: 'DENIED', effect: 'NONE' }, 'BUSINESS_REJECTION', 'DENIED', 'NONE'],
    [{ kind: 'TRANSIENT_FAILURE', effect: 'NONE' }, 'FAILED', 'ADAPTER_TRANSIENT', 'NONE'],
    [{ kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' }, 'UNCERTAIN', 'UNKNOWN_COMMIT', 'UNKNOWN'],
    [{ kind: 'SUCCESS', effect: 'CONFIRMED', value: {} }, 'FAILED', 'ADAPTER_INVALID', 'CONFIRMED'],
    [{ kind: 'SUCCESS', effect: 'CONFIRMED', value: { ...value('actual'), note: 'x'.repeat(257) } }, 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', 'CONFIRMED'],
  ];
  for (const [response, classification, code, effect] of cases) {
    const f = host(); f.state.response = response;
    const outcome = await f.invoke(true);
    assert.equal(outcome.classification, classification, JSON.stringify(outcome));
    assert.deepEqual(outcome.error, { code, path: code === 'BUDGET_EXCEEDED' ? '/steps/1/note' : '/steps/1' });
    assert.equal(Object.hasOwn(outcome, 'result'), false);
    assert.deepEqual(outcome.effects, [{ step: 'created', capability: 'records.create', state: effect }]);
    assert.deepEqual(f.calls, ['records.create']);
  }
  for (const failure of ['throws', 'expired']) {
    const f = host(); f.state[failure] = true;
    const outcome = await f.invoke(true);
    assert.equal(outcome.classification, failure === 'throws' ? 'UNCERTAIN' : 'LIMIT_EXCEEDED');
    assert.equal(outcome.error.code, failure === 'throws' ? 'ADAPTER_EXCEPTION' : 'BUDGET_EXCEEDED');
    assert.equal(Object.hasOwn(outcome, 'result'), false);
    assert.equal(outcome.effects[0].state, failure === 'throws' ? 'UNKNOWN' : 'CONFIRMED');
  }
});

test('independent preparation rejects forged provenance and a valid checked tree without exact domination', () => {
  const f = host(), original = JSON.parse(f.artifactBytes);
  for (const change of [
    artifact => { delete artifact.expressions.e2.variables.created; },
    artifact => { artifact.expressions.e2.variables.future = 'CreatedRecord'; },
    artifact => { artifact.expressions.e2.variables.created = 'RecordInput'; },
    artifact => { artifact.steps[1].when = 'reference'; },
    artifact => { [artifact.steps[0], artifact.steps[1]] = [artifact.steps[1], artifact.steps[0]]; },
    artifact => { artifact.expressions.e2.guardedResults = { created: 'createNew' }; },
    artifact => {
      const checked = fromBinary(CheckedExprSchema, Buffer.from(artifact.expressions.e2.checked, 'base64'));
      const condition = checked.expr.exprKind.value.args[0];
      condition.exprKind = { case: 'constExpr', value: { $typeName: 'cel.expr.Constant', constantKind: { case: 'boolValue', value: true } } };
      delete checked.referenceMap[String(condition.id)];
      artifact.expressions.e2.checked = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');
    },
  ]) {
    const artifact = structuredClone(original); change(artifact);
    const prepared = prepareProgram(canonicalBytes(artifact), f.bound);
    assert.equal(prepared.prepared, null); assert.equal(prepared.diagnostic.code, 'ARTIFACT_INVALID');
    assert.deepEqual(f.calls, []);
  }
  for (const producer of ['0.3.0', '0.3.1']) {
    const prepared = prepareProgram(canonicalBytes({ ...original, compiler: 'keir-compiler-java/' + producer }), f.bound);
    assert.equal(prepared.prepared, null); assert.equal(prepared.diagnostic.code, 'ARTIFACT_INVALID');
  }
  const legacyModel = JSON.parse(read('dcm-guarded-result.json')); legacyModel.format = 'keir-dcm/2';
  const legacyBinding = bindModel(canonicalBytes(legacyModel), Object.fromEntries(Object.keys(legacyModel.capabilities).map(id => [id, {
    version: '1', mode: 'simulation', keyType: 'sample.RecordKey/1', invoke() { assert.fail('Unexpected dispatch'); },
  }])));
  assert.equal(legacyBinding.diagnostic, null);
  const legacyDenied = prepareProgram(canonicalBytes({ ...original, format: 'keir-artifact/2',
    compiler: 'keir-compiler-java/0.2.0', modelRef: modelFor(legacyBinding.boundModel).modelRef }), legacyBinding.boundModel);
  assert.equal(legacyDenied.prepared, null); assert.equal(legacyDenied.diagnostic.code, 'ARTIFACT_INVALID');
  assert.equal(legacyDenied.diagnostic.path, '/artifact/steps/2/value');
});
