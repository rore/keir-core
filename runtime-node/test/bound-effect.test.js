import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fromBinary, fromJson, toBinary } from '@bufbuild/protobuf';
import { celEnv, CelScalar, plan } from '@bufbuild/cel';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import * as api from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { boundFixture, verifyBoundTransports } from './bound-fixture.js';

const input = { taskId: 'task1' };

test('pinned CEL evaluates the largest admitted concatenation tree to 8192 bytes', () => {
  const f = boundFixture(api, { program: 'title-max' });
  const artifact = JSON.parse(f.bytes);
  const title = artifact.expressions[artifact.steps.at(-1).args.title.expr];
  const checked = fromBinary(CheckedExprSchema, Buffer.from(title.checked, 'base64'));
  assert.equal(Object.keys(checked.typeMap).length, 63);
  assert.equal(checked.expr.exprKind.value.function, '_+_');
  assert.equal(Object.keys(checked.referenceMap).filter(id => checked.referenceMap[id].overloadId[0] === 'add_string').length, 31);
  const evaluate = plan(celEnv({ variables: { base: CelScalar.STRING } }), checked);
  const value = evaluate({ base: 'é'.repeat(128) });
  assert.equal(value, 'é'.repeat(4096));
  assert.equal(Buffer.byteLength(value), 8192);
});
function prepared(f) {
  const binding = f.bind(); assert.equal(binding.diagnostic, null);
  const result = api.prepareProgram(f.bytes, binding.boundModel); assert.equal(result.diagnostic, null);
  assert.equal(f.calls.length, 0); return result.prepared;
}
const denied = (result, path, code = 'ADMISSION_INVALID') => {
  assert.equal(Object.hasOwn(result, 'boundModel') ? result.boundModel : result.prepared, null);
  assert.equal(result.diagnostic.code, code); assert.equal(result.diagnostic.path, path);
};

test('the same pinned artifacts produce identical results and calls in real and simulation modes', async () => {
  for (const program of ['main', 'zero', 'after-write-read', 'no-call', 'title']) {
    const runs = [];
    for (const mode of ['real', 'simulation']) {
      const f = boundFixture(api, { mode, program });
      f.state.title = 'Résumé 🚀';
      const outcome = await api.invokeProgram(prepared(f), input, f.invocation);
      assert.equal(outcome.classification, 'COMPLETED', program);
      runs.push({ outcome, calls: f.calls, nativeActions: f.nativeActions });
    }
    assert.deepEqual(runs[0], runs[1], program);
  }
  const f = boundFixture(api, { program: 'title-composed' });
  f.state.title = 'x'.repeat(256);
  f.state.project = 'project2';
  const outcome = await api.invokeProgram(prepared(f), input, f.invocation);
  assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
  assert.equal(outcome.trace.some(row => row.event === 'EFFECT_INTENT'), false);
  assert.deepEqual(f.nativeActions, ['tasks.get']);
});

test('complete compatible registrations include unused capabilities and remain snapshotted', async () => {
  for (const mutate of [
    f => { delete f.implementations['tasks.get']; }, f => { f.implementations.extra = f.implementations['tasks.get']; },
    f => { f.implementations['tasks.get'].version = '2'; }, f => { f.implementations['tasks.get'].mode = 'simulation'; },
    f => { f.implementations['tasks.get'].keyType = 'wrong'; }, f => { f.implementations['tasks.get'].invoke = null; },
    f => { f.implementations['tasks.get'].extra = true; },
  ]) { const f = boundFixture(api, { program: 'no-call' }); mutate(f); assert.equal(f.bind().boundModel, null); assert.equal(f.calls.length, 0); }
  const f = boundFixture(api), handle = prepared(f);
  f.implementations['tasks.create'].invoke = () => { throw Error('replacement'); };
  delete f.implementations['tasks.get'];
  assert.equal((await api.invokeProgram(handle, input, f.invocation)).classification, 'COMPLETED');
});

test('binding inspects descriptors without invoking getters or native implementations', () => {
  for (const place of ['map', 'registration']) {
    const f = boundFixture(api); let getters = 0;
    Object.defineProperty(place === 'map' ? f.implementations : f.implementations['tasks.get'], place === 'map' ? 'tasks.get' : 'invoke',
      { enumerable: true, get() { getters++; throw Error('private'); } });
    assert.equal(f.bind().boundModel, null); assert.equal(getters, 0); assert.equal(f.calls.length, 0);
  }
});

test('binding snapshots model bytes and retains exact identity for unused catalog data', () => {
  const f = boundFixture(api, { program: 'no-call' });
  const bytes = Buffer.from(JSON.stringify(f.dcm, null, 2));
  const { boundModel, diagnostic } = api.bindModel(bytes, f.implementations);
  assert.equal(diagnostic, null);
  bytes.fill(0);
  assert.equal(api.prepareProgram(f.bytes, boundModel).diagnostic, null);
  const duplicate = Buffer.concat([Buffer.from('{"format":"keir-dcm/1",'), f.dcmBytes.subarray(1)]);
  denied(api.bindModel(duplicate, f.implementations), '/binding/dcm');
  const dcm = structuredClone(f.dcm);
  dcm.capabilities['tasks.create'].errors.push('UNUSED_ERROR');
  const binding = api.bindModel(canonicalBytes(dcm), f.implementations);
  assert.equal(binding.diagnostic, null);
  denied(api.prepareProgram(f.bytes, binding.boundModel), '/artifact/modelRef', 'ARTIFACT_INVALID');
  assert.equal(f.calls.length, 0);
});

test('public model binding matches the shared byte boundary cases without dispatch', () => {
  const root = new URL('../../conformance/model-binding/', import.meta.url);
  const read = file => readFileSync(new URL(file, root));
  const model = JSON.parse(read('valid.json'));
  let dispatches = 0;
  const implementations = Object.fromEntries(Object.entries(model.capabilities).map(([id, cap]) => [id, {
    version: cap.version, mode: 'simulation', keyType: model.hostKey.type,
    invoke() { dispatches++; },
  }]));
  for (const { file, diagnostic } of JSON.parse(read('cases.json'))) {
    const binding = api.bindModel(read(file), implementations);
    if (diagnostic === null) {
      assert.ok(binding.boundModel, file);
      assert.equal(binding.diagnostic, null, file);
    } else {
      assert.equal(binding.boundModel, null, file);
      assert.deepEqual({ code: binding.diagnostic?.code, path: binding.diagnostic?.path }, diagnostic, file);
    }
    assert.equal(dispatches, 0, file);
  }
});

test('only the host-bound API is exported and extra admission inputs fail closed', async () => {
  assert.deepEqual(Object.keys(api).sort(), ['ENVIRONMENT', 'bindModel', 'invokeProgram', 'prepareProgram']);
  const f = boundFixture(api), binding = f.bind(), handle = prepared(f);
  assert.equal(api.bindModel(f.dcmBytes, f.implementations, undefined).boundModel, null);
  assert.equal(api.bindModel(f.dcmBytes, {}, f.implementations).boundModel, null);
  denied(api.prepareProgram(f.bytes, binding.boundModel, undefined), '/admission');
  denied(api.prepareProgram(f.bytes, {}), '/binding');
  for (const field of ['authorize', 'scope', 'qualification', 'keyBinding', 'capabilityPolicy', 'effectGrant', 'limits']) {
    const outcome = await api.invokeProgram(handle, input, { ...f.invocation, [field]: field === 'authorize' ? () => true : {} });
    assert.deepEqual(outcome.error, { code: 'HOST_CONTEXT_INVALID', path: '' });
  }
  assert.equal((await api.invokeProgram(handle, input, f.invocation, undefined)).error.code, 'HOST_CONTEXT_INVALID');
  assert.equal((await api.invokeProgram({}, input, f.invocation)).error.code, 'HOST_CONTEXT_INVALID');
  assert.equal(f.calls.length, 0);
});

test('preparation requires the public envelope, compiler and model pins', () => {
  const f = boundFixture(api), bound = f.bind().boundModel;
  for (const [mutate, path, code] of [
    [a => { a.compiler = 'keir-compiler-java/0.1.0-SNAPSHOT'; }, '/artifact/compiler', 'PIN_MISMATCH'],
    [a => { a.compiler = 'keir-compiler-java/0.2.0'; }, '/artifact/compiler', 'PIN_MISMATCH'],
    [a => { a.environment.celJava = 'unsupported'; }, '/artifact/environment', 'PIN_MISMATCH'],
    [a => { a.modelRef.digest = 'sha256:' + '0'.repeat(64); }, '/artifact/modelRef'],
    [a => { a.modelRef.descriptorRecipe = 'keir-record-descriptors/2'; }, '/artifact/modelRef'],
    [a => { a.modelRef.format = 'keir-dcm/effect-4'; }, '/artifact/modelRef'],
    [a => { a.modelRef.domainId = [a.modelRef.domainId]; }, '/artifact/modelRef'],
    [a => { a.modelRef.domainVersion = 'unsupported'; }, '/artifact/modelRef'],
    [a => { a.profile = 'keir-effect/4'; }, '/artifact'],
    [a => { a.dcm = f.dcm; }, '/artifact'],
    [a => { a.descriptors = 'YQ=='; }, '/artifact'],
    [a => { a.sourceDigest = 'unsupported'; }, '/artifact/sourceDigest'],
  ]) {
    const artifact = JSON.parse(f.bytes);
    mutate(artifact);
    denied(api.prepareProgram(canonicalBytes(artifact), bound), path, code ?? 'ARTIFACT_INVALID');
  }
  for (const suffix of ['read-1', 'read-2', 'effect-1', 'effect-2', 'effect-3', 'effect-4']) {
    const artifact = { ...JSON.parse(f.bytes), format: `keir-artifact/${suffix}` };
    denied(api.prepareProgram(canonicalBytes(artifact), bound), '/artifact/format', 'ARTIFACT_INVALID');
    denied(api.bindModel(canonicalBytes({ ...f.dcm, format: `keir-dcm/${suffix}` }), f.implementations), '/binding/dcm');
  }
  const legacyModel = { ...f.dcm, assurance: 'HOST_BOUND_SINGLE_WRITE' };
  denied(api.bindModel(canonicalBytes(legacyModel), f.implementations), '/binding/dcm');
  for (const bytes of [
    Buffer.concat([Buffer.from('{"format":"keir-artifact/1",'), f.bytes.subarray(1)]),
    Buffer.concat([f.bytes, Buffer.from('\n')]),
    Buffer.concat([Buffer.from([0xef, 0xbb, 0xbf]), f.bytes]),
    Buffer.from([0x7b, 0xff, 0x7d]),
  ]) denied(api.prepareProgram(bytes, bound), '/artifact', 'ARTIFACT_INVALID');
  assert.equal(api.prepareProgram(new Uint8Array(1_048_577), bound).diagnostic.code, 'BUDGET_EXCEEDED');
  assert.equal(f.calls.length, 0);
});

test('the current handler requires valid guard, error and checked expression metadata', async () => {
  const f = boundFixture(api, { mode: 'simulation', program: 'handler' }), { bytes } = f;
  const binding = f.bind();
  assert.equal(binding.diagnostic, null);
  for (const [mutate, path] of [
    [a => { a.steps[3].when = 'missing'; }, '/artifact/steps/3/when'],
    [a => { a.steps[3].onError = { NOT_DECLARED: a.steps[3].onError.CREATE_REJECTED }; }, '/artifact/steps/3/onError'],
    [a => { a.expressions.e0.checked = '/w=='; }, '/artifact'],
  ]) {
    const artifact = JSON.parse(bytes);
    mutate(artifact);
    denied(api.prepareProgram(canonicalBytes(artifact), binding.boundModel), path, 'ARTIFACT_INVALID');
    assert.equal(f.calls.length, 0, path);
  }
  for (const field of ['when', 'onError']) {
    const artifact = JSON.parse(bytes);
    artifact.steps[3][field] = null;
    denied(api.prepareProgram(Buffer.from(JSON.stringify(artifact)), binding.boundModel), '/artifact', 'ARTIFACT_INVALID');
  }
  const guarded = JSON.parse(bytes);
  guarded.expressions.e3.variables.created = 'CreatedTask';
  denied(api.prepareProgram(canonicalBytes(guarded), binding.boundModel), '/artifact/return/fields/status', 'ARTIFACT_INVALID');
  const obsoleteLocation = JSON.parse(bytes);
  const expression = obsoleteLocation.expressions.e0;
  const checked = fromBinary(CheckedExprSchema, Buffer.from(expression.checked, 'base64'));
  assert.equal(checked.sourceInfo.location, '<input>');
  checked.sourceInfo.location = 'keir-read/1';
  expression.checked = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');
  denied(api.prepareProgram(canonicalBytes(obsoleteLocation), binding.boundModel), '/artifact/expressions', 'ARTIFACT_INVALID');
  const result = api.prepareProgram(bytes, binding.boundModel);
  assert.equal(result.diagnostic, null);
  assert.equal(f.calls.length, 0);
  f.state.writeResponse = { kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' };
  const outcome = await api.invokeProgram(result.prepared, input, f.invocation);
  assert.equal(outcome.classification, 'BUSINESS_REJECTION');
  assert.deepEqual(outcome.error, { code: 'CREATE_REJECTED', path: '/steps/3' });
  assert.deepEqual(outcome.result, { status: 'NO_OP' });
  assert.deepEqual(f.calls.map(call => call.capability), ['tasks.get', 'tasks.create']);
  assert.deepEqual(outcome.effects.map(effect => effect.state), ['NONE']);
});

test('handler output preserves rejection and write evidence across the record byte limit', async () => {
  const title = '\0'.repeat(56) + 'x'.repeat(200);
  for (const padding of [17, 18]) {
    const f = boundFixture(api, { program: 'handler' });
    const result = Object.fromEntries(Array.from({ length: 16 }, (_, index) =>
      ['f' + index, index === 15 ? 'x'.repeat(padding) : title]));
    assert.equal(canonicalBytes(result).length, padding === 17 ? 8192 : 8193);
    const fields = Object.fromEntries(Object.entries(result).map(([name, literal]) =>
      [name, { type: 'String', literal }]));
    const artifact = JSON.parse(f.bytes);
    delete artifact.expressions.e3;
    artifact.return.fields = fields;
    artifact.steps[3].onError.CREATE_REJECTED.fields = fields;
    f.dcm.types.FollowupResult.fields = Object.fromEntries(Object.keys(fields).map(name => [name, 'String']));
    const dcmBytes = canonicalBytes(f.dcm);
    artifact.modelRef.digest = digest(dcmBytes);
    const binding = api.bindModel(dcmBytes, f.implementations);
    assert.equal(binding.diagnostic, null);
    const preparation = api.prepareProgram(canonicalBytes(artifact), binding.boundModel);
    assert.equal(preparation.diagnostic, null);
    f.state.writeResponse = rejected;

    const outcome = await api.invokeProgram(preparation.prepared, input, f.invocation);
    const cause = { code: 'CREATE_REJECTED', path: '/steps/3' };
    assert.deepEqual(outcome.effects, [{ step: 'created', capability: 'tasks.create', state: 'NONE' }]);
    if (padding === 17) {
      assert.equal(outcome.classification, 'BUSINESS_REJECTION');
      assert.deepEqual(outcome.result, result);
      assert.deepEqual(outcome.error, cause);
      assert.equal(outcome.trace.at(-2).event, 'RETURNED');
    } else {
      assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
      assert.equal(Object.hasOwn(outcome, 'result'), false);
      assert.deepEqual(outcome.error, { code: 'BUDGET_EXCEEDED', path: '/steps/3/onError/CREATE_REJECTED', cause });
      assert.equal(outcome.trace.some(event => event.event === 'RETURNED'), false);
    }
  }
});

test('one bound model and prepared program receive fresh actors events opaque keys and absolute deadlines', async () => {
  const f = boundFixture(api), handle = prepared(f);
  for (let index = 0; index < 2; index++) {
    const invocation = f.invocationFor({ actor: `user${index}`, event: `event${index}`, expiresAtMs: 1000 + index });
    assert.equal((await api.invokeProgram(handle, input, invocation)).classification, 'COMPLETED');
    for (const context of f.contexts.slice(index * 2, index * 2 + 2)) {
      assert.strictEqual(context.requestKey, invocation.requestKey); assert.deepEqual(context.event, invocation.event);
      assert.equal(context.deadline.expiresAtMs, 1000 + index); assert.equal(context.deadline.clock.nowMs(), f.state.now);
      assert.ok(Object.isFrozen(context)); assert.ok(Object.isFrozen(context.event)); assert.ok(Object.isFrozen(context.deadline));
    }
    assert.equal(Object.isFrozen(invocation.requestKey), false); assert.equal(Object.isFrozen(invocation.deadline.clock), false);
  }
  assert.deepEqual(f.calls.map(call => call.protectedArgs.actorId), ['user0', 'user0', 'user1', 'user1']);
});

test('context and clock function are snapshotted before awaiting without freezing caller objects', async () => {
  const f = boundFixture(api), handle = prepared(f); let release, enter;
  const started = new Promise(resolve => { enter = resolve; }), barrier = new Promise(resolve => { release = resolve; });
  f.state.beforeNative = capability => { if (capability === 'tasks.get') { enter(); return barrier; } };
  const clock = { value: 0, nowMs() { return this.value; } };
  f.invocation.deadline.clock = clock;
  const key = f.invocation.requestKey;
  const pending = api.invokeProgram(handle, input, f.invocation); await started;
  f.invocation.bindings.actorId = 'changed'; f.invocation.event.id = 'changed'; f.invocation.requestKey = {};
  f.invocation.deadline.expiresAtMs = 1; clock.nowMs = () => 1000;
  release(); const outcome = await pending;
  assert.equal(outcome.classification, 'COMPLETED'); assert.strictEqual(f.contexts[1].requestKey, key);
  assert.equal(f.contexts[1].deadline.expiresAtMs, 1000); assert.equal(f.contexts[1].deadline.clock.nowMs(), 0);
  assert.equal(Object.isFrozen(clock), false);
});

test('overlapping invocations on a prepared program keep their original host contexts', async () => {
  const f = boundFixture(api), handle = prepared(f);
  const first = f.invocationFor({ actor: 'userA', event: 'eventA' });
  const second = f.invocationFor({ actor: 'userB', event: 'eventB' });
  let enter, release;
  const started = new Promise(resolve => { enter = resolve; });
  const barrier = new Promise(resolve => { release = resolve; });
  f.state.beforeNative = (capability, context) => {
    if (capability === 'tasks.get' && context.requestKey === first.requestKey) {
      enter();
      return barrier;
    }
  };
  const pending = api.invokeProgram(handle, input, first);
  await started;
  try {
    assert.equal((await api.invokeProgram(handle, input, second)).classification, 'COMPLETED');
  } finally {
    release();
  }
  assert.equal((await pending).classification, 'COMPLETED');
  assert.deepEqual(f.calls.map(call => call.protectedArgs.actorId), ['userA', 'userB', 'userB', 'userA']);
  for (const [index, invocation] of [first, second, second, first].entries()) {
    assert.strictEqual(f.contexts[index].requestKey, invocation.requestKey);
    assert.deepEqual(f.contexts[index].event, invocation.event);
  }
});

test('typed bounded context and input reject before any adapter dispatch', async () => {
  for (const mutate of [
    f => { f.invocation.bindings = {}; }, f => { f.invocation.bindings.extra = true; },
    f => { f.invocation.bindings.actorId = true; }, f => { f.invocation.event = {}; },
    f => { f.invocation.event.extra = true; }, f => { f.invocation.event.source = true; },
    f => { f.invocation.requestKey = null; }, f => { f.invocation.deadline = null; },
    f => { f.invocation.deadline.clock.nowMs = null; }, f => { f.invocation.deadline.expiresAtMs = -1; },
    f => { f.state.now = 1000; },
  ]) { const f = boundFixture(api), handle = prepared(f); mutate(f); assert.notEqual((await api.invokeProgram(handle, input, f.invocation)).classification, 'COMPLETED'); assert.equal(f.calls.length, 0); }
  const f = boundFixture(api), handle = prepared(f);
  assert.equal((await api.invokeProgram(handle, { taskId: true }, f.invocation)).error.code, 'INPUT_INVALID');
  assert.equal(f.calls.length, 0);
});

test('no-call and empty-catalog programs require ordinary structure but no native permission or key authenticity', async () => {
  const f = boundFixture(api, { program: 'no-call' }); f.state.allowed = false; f.invocation.requestKey = {};
  assert.equal((await api.invokeProgram(prepared(f), input, f.invocation)).result.status, 'NO_OP'); assert.equal(f.state.checks, 0);
  const dcm = structuredClone(f.dcm); dcm.capabilities = {};
  const bound = api.bindModel(canonicalBytes(dcm), {}).boundModel; assert.ok(bound);
  const artifact = JSON.parse(f.bytes); artifact.modelRef.digest = digest(canonicalBytes(dcm));
  const result = api.prepareProgram(canonicalBytes(artifact), bound); assert.equal(result.diagnostic, null);
  assert.equal((await api.invokeProgram(result.prepared, input, f.invocation)).classification, 'COMPLETED');
  const call = JSON.parse(boundFixture(api).bytes); call.modelRef.digest = artifact.modelRef.digest;
  assert.equal(api.prepareProgram(canonicalBytes(call), bound).diagnostic.code, 'CAPABILITY_UNKNOWN');
});

test('host rechecks current permission and absolute deadline after its own pending work before commit', async () => {
  for (const expires of [false, true]) {
    const f = boundFixture(api), handle = prepared(f); let enter, release;
    const started = new Promise(resolve => { enter = resolve; }), barrier = new Promise(resolve => { release = resolve; });
    f.state.beforeNative = capability => { if (capability === 'tasks.create') { enter(); return barrier; } };
    const pending = api.invokeProgram(handle, input, f.invocation); await started;
    if (expires) f.state.now = 1000; else f.state.allowed = false;
    release(); const outcome = await pending;
    assert.deepEqual(f.nativeActions, ['tasks.get']); assert.equal(outcome.effects[0].state, 'NONE');
    assert.equal(outcome.classification, expires ? 'LIMIT_EXCEEDED' : 'BUSINESS_REJECTION');
  }
});

const confirmed = { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
const rejected = { kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' };
const transient = { kind: 'TRANSIENT_FAILURE', effect: 'NONE' };

test('write evidence distinguishes invalid envelopes from invalid confirmed values', async () => {
  for (const [response, state, code, classification] of [
    [null, 'UNKNOWN', 'ADAPTER_INVALID', 'UNCERTAIN'],
    [{ kind: 'SUCCESS', value: confirmed.value }, 'UNKNOWN', 'ADAPTER_INVALID', 'UNCERTAIN'],
    [{ ...confirmed, extra: true }, 'UNKNOWN', 'ADAPTER_INVALID', 'UNCERTAIN'],
    [{ ...confirmed, effect: 'NONE' }, 'UNKNOWN', 'ADAPTER_INVALID', 'UNCERTAIN'],
    [{ ...confirmed, value: {} }, 'CONFIRMED', 'ADAPTER_INVALID', 'FAILED'],
    [{ ...confirmed, value: { id: 'x'.repeat(257) } }, 'CONFIRMED', 'BUDGET_EXCEEDED', 'LIMIT_EXCEEDED'],
    [{ ...rejected, code: 'UNDECLARED' }, 'NONE', 'ADAPTER_INVALID', 'FAILED'],
  ]) {
    const f = boundFixture(api);
    f.implementations['tasks.create'].invoke = () => response;
    const outcome = await api.invokeProgram(prepared(f), input, f.invocation);
    assert.equal(outcome.classification, classification);
    assert.equal(outcome.error.code, code);
    assert.deepEqual(outcome.effects, [{ capability: 'tasks.create', step: 'created', state }]);
    assert.equal(outcome.trace.filter(event => event.event === 'EFFECT_RECORDED').length, 1);
    assert.equal(outcome.trace.some(event => event.event === 'CALL_SUCCEEDED' && event.step === 'created'), false);
  }
});

test('winning responses establish write evidence before clock failures discard their values', async () => {
  for (const response of [confirmed, rejected, transient]) {
    for (const [now, classification, code, path] of [
      [1000, 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', '/steps/3'],
      [9, 'FAILED', 'HOST_CONTEXT_INVALID', ''],
      [NaN, 'FAILED', 'HOST_CONTEXT_INVALID', ''],
    ]) {
      const f = boundFixture(api);
      f.state.now = 10;
      f.state.writeResponse = response;
      f.state.afterWrite = () => { f.state.now = now; };
      const outcome = await api.invokeProgram(prepared(f), input, f.invocation);
      assert.equal(outcome.classification, classification);
      assert.deepEqual(outcome.error, { code, path });
      assert.deepEqual(outcome.effects, [{ capability: 'tasks.create', step: 'created', state: response.effect }]);
      assert.equal(outcome.trace.at(-2).event, 'EFFECT_RECORDED');
      assert.equal(Object.hasOwn(outcome, 'result'), false);
    }
  }
});

test('the winning write envelope is snapshotted before later promise jobs can mutate it', async () => {
  const f = boundFixture(api, { program: 'handler' });
  const response = { ...rejected };
  f.implementations['tasks.create'].invoke = () => {
    queueMicrotask(() => queueMicrotask(() => {
      delete response.code;
      Object.assign(response, confirmed);
    }));
    return response;
  };
  const outcome = await api.invokeProgram(prepared(f), input, f.invocation);
  assert.equal(response.effect, 'CONFIRMED');
  assert.equal(outcome.classification, 'BUSINESS_REJECTION');
  assert.deepEqual(outcome.error, { code: 'CREATE_REJECTED', path: '/steps/3' });
  assert.deepEqual(outcome.result, { status: 'NO_OP' });
  assert.equal(outcome.effects[0].state, 'NONE');
});

test('write timer setup failure creates no receipt or native attempt', async t => {
  const f = boundFixture(api), handle = prepared(f);
  const setTimeout = globalThis.setTimeout;
  let readSettled = false;
  f.state.afterRead = () => { readSettled = true; };
  t.mock.method(globalThis, 'setTimeout', (...args) => {
    if (readSettled) throw Error('private timer setup failure');
    return setTimeout(...args);
  });
  const outcome = await api.invokeProgram(handle, input, f.invocation);
  assert.deepEqual(outcome.error, { code: 'HOST_CONTEXT_INVALID', path: '' });
  assert.deepEqual(outcome.effects, []);
  assert.deepEqual(f.nativeActions, ['tasks.get']);
  assert.equal(outcome.trace.some(event => event.event === 'EFFECT_INTENT'), false);
});

test('timeout-first UNKNOWN ignores late definite responses and never retries', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  for (const response of [confirmed, rejected, transient]) {
    const f = boundFixture(api), handle = prepared(f);
    let enter, finish;
    const started = new Promise(resolve => { enter = resolve; });
    f.state.writeResponse = response;
    f.state.afterWrite = () => { enter(); return new Promise(resolve => { finish = resolve; }); };
    const pending = api.invokeProgram(handle, input, f.invocation);
    await started;
    t.mock.timers.tick(1000);
    const outcome = await pending, before = JSON.stringify(outcome);
    assert.equal(outcome.classification, 'UNCERTAIN');
    assert.equal(outcome.effects[0].state, 'UNKNOWN');
    assert.deepEqual(outcome.error, { code: 'BUDGET_EXCEEDED', path: '/steps/3' });
    finish();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(JSON.stringify(outcome), before);
    assert.equal(outcome.trace.filter(event => event.event === 'EFFECT_RECORDED').length, 1);
    assert.deepEqual(f.nativeActions, ['tasks.get', 'tasks.create']);
  }
});

test('long host budgets time out in finite timer chunks', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const f = boundFixture(api), handle = prepared(f);
  const invocation = f.invocationFor({ expiresAtMs: 2_147_483_648 });
  let release, complete = false;
  f.state.afterRead = () => new Promise(resolve => { release = resolve; });
  const pending = api.invokeProgram(handle, input, invocation).then(outcome => {
    complete = true;
    return outcome;
  });
  await Promise.resolve();
  assert.equal(typeof release, 'function');
  t.mock.timers.tick(2_147_483_647);
  await Promise.resolve();
  assert.equal(complete, false);
  t.mock.timers.tick(1);
  const outcome = await pending;
  release();
  assert.deepEqual(outcome.error, { code: 'BUDGET_EXCEEDED', path: '/steps/1' });
  assert.deepEqual(outcome.effects, []);
  assert.deepEqual(f.nativeActions, ['tasks.get']);
});

test('fixed step call write and DCM policy ceilings remain active on the normal API', async () => {
  const f = boundFixture(api), bound = f.bind().boundModel;
  for (const [kind, limit] of [['BIND', 8], ['CALL', 4]]) for (const count of [limit, limit + 1]) {
    const artifact = JSON.parse(boundFixture(api, { program: 'no-call' }).bytes); artifact.expressions = {};
    artifact.steps = Array.from({ length: count }, (_, i) => kind === 'BIND' ?
      { id: `bound${i}`, op: 'BIND', resultType: 'Boolean', value: { type: 'Boolean', literal: true } } :
      { id: `read${i}`, op: 'CALL', capability: 'tasks.get', version: '1', resultType: 'Task', args: { taskId: { type: 'TaskId', literal: 'task1' } } });
    artifact.return = { type: 'FollowupResult', fields: { status: { type: 'FollowupStatus', literal: 'NO_OP' } } };
    const result = api.prepareProgram(canonicalBytes(artifact), bound);
    if (count <= limit) assert.equal(result.diagnostic, null); else assert.equal(result.diagnostic.code, 'BUDGET_EXCEEDED');
  }
  const secondWrite = JSON.parse(boundFixture(api, { program: 'no-call' }).bytes);
  const write = JSON.parse(f.bytes).steps[3];
  write.args.projectId = { type: 'ProjectId', literal: 'project1' };
  secondWrite.steps = [
    { id: 'take', op: 'BIND', resultType: 'Boolean', value: { type: 'Boolean', literal: true } },
    { ...write, when: 'take' },
    { id: 'skip', op: 'BIND', resultType: 'Boolean', value: { type: 'Boolean', literal: false } },
    { ...write, id: 'secondWrite', when: 'skip' },
  ];
  secondWrite.expressions = {};
  secondWrite.return.fields.status = { type: 'FollowupStatus', literal: 'NO_OP' };
  denied(api.prepareProgram(canonicalBytes(secondWrite), bound), '/artifact/steps/3', 'BUDGET_EXCEEDED');
  for (const field of ['capabilityPolicy', 'effectGrant']) {
    const dcm = structuredClone(f.dcm); dcm.entrypoints.TaskCompleted[field] = field === 'effectGrant' ? 0 : { maxCalls: { 'tasks.create': 0 } };
    const binding = api.bindModel(canonicalBytes(dcm), f.implementations); assert.equal(binding.diagnostic, null);
    const artifact = JSON.parse(f.bytes); artifact.modelRef.digest = digest(canonicalBytes(dcm));
    denied(api.prepareProgram(canonicalBytes(artifact), binding.boundModel), '/artifact/steps/3', 'ENTRY_GRANT_DENIED');
  }
  denied(api.bindModel(new Uint8Array(65_537), f.implementations), '/binding/dcm', 'BUDGET_EXCEEDED');
});

test('local and loopback hosts own authentication permissions and native commit evidence', async () => {
  await verifyBoundTransports(api);
});

test('checked CEL node and scalar byte ceilings remain enforced through public APIs', async () => {
  const f = boundFixture(api, { program: 'no-call' }), bound = f.bind().boundModel;
  for (const leaves of [32, 33]) {
    let next = 0; const typeMap = {}, referenceMap = {};
    function tree(count) {
      const id = String(++next); typeMap[id] = { primitive: 'BOOL' };
      if (count === 1) return { id, constExpr: { boolValue: true } };
      referenceMap[id] = { overloadId: ['logical_and'] };
      return { id, callExpr: { function: '_&&_', args: [tree(Math.floor(count / 2)), tree(count - Math.floor(count / 2))] } };
    }
    const expr = tree(leaves), checked = fromJson(CheckedExprSchema, { expr, sourceInfo: { location: '<input>' }, typeMap, referenceMap });
    const artifact = JSON.parse(f.bytes);
    artifact.expressions.e1 = artifact.expressions.e0;
    artifact.expressions.e0 = { checked: Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64'), resultType: 'Boolean', variables: { input: 'TaskCompletedInput' } };
    artifact.steps[0].value = { type: 'Boolean', expr: 'e0' };
    artifact.return.fields.status.expr = 'e1';
    const result = api.prepareProgram(canonicalBytes(artifact), bound);
    if (leaves === 32) { assert.equal(result.diagnostic, null); assert.equal((await api.invokeProgram(result.prepared, input, f.invocation)).classification, 'COMPLETED'); }
    else assert.equal(result.diagnostic.code, 'BUDGET_EXCEEDED');
  }
  const exactDcm = Buffer.concat([f.dcmBytes, Buffer.alloc(65_536 - f.dcmBytes.length, ' ')]);
  assert.equal(api.bindModel(exactDcm, f.implementations).diagnostic, null);
  const calling = boundFixture(api), handle = prepared(calling);
  assert.equal((await api.invokeProgram(handle, { taskId: 'x'.repeat(256) }, calling.invocation)).classification, 'COMPLETED');
  const before = calling.calls.length;
  assert.equal((await api.invokeProgram(handle, { taskId: 'x'.repeat(257) }, calling.invocation)).error.code, 'BUDGET_EXCEEDED');
  assert.equal(calling.calls.length, before);
});

test('serialized input and result records enforce the exact 8192-byte boundary', async () => {
  const f = boundFixture(api);
  const fields = {
    flag: 'Boolean',
    empty: 'String',
    toString: 'String',
    toJSON: 'String',
    valueOf: 'String',
    ...Object.fromEntries(Array.from({ length: 6 }, (_, index) => ['text' + index, 'String'])),
  };
  const dcm = structuredClone(f.dcm);
  dcm.types.BoundaryRecord = { kind: 'record', fields };
  dcm.entrypoints.TaskCompleted.input = 'BoundaryRecord';
  dcm.entrypoints.TaskCompleted.output = 'BoundaryRecord';
  const dcmBytes = canonicalBytes(dcm);
  const { boundModel, diagnostic } = api.bindModel(dcmBytes, f.implementations);
  assert.equal(diagnostic, null);

  function record(bytes) {
    const value = Object.fromEntries(Object.keys(fields).map(name => [name, '']));
    Object.assign(value, { flag: false, toString: 'é🚀', toJSON: 'e\u0301', valueOf: '\u0000\t\n"\\' });
    let remaining = bytes - canonicalBytes(value).length;
    for (const name of Object.keys(fields).filter(name => name.startsWith('text'))) {
      const controls = Math.min(256, Math.floor(remaining / 6));
      const ascii = Math.min(256 - controls, remaining - controls * 6);
      value[name] = '\u0000'.repeat(controls) + 'x'.repeat(ascii);
      remaining -= controls * 6 + ascii;
    }
    assert.equal(canonicalBytes(value).length, bytes);
    return value;
  }

  function prepare(value) {
    const artifact = JSON.parse(f.bytes);
    const read = artifact.steps[1];
    read.args.taskId = { type: 'TaskId', literal: 'task1' };
    artifact.steps = [read];
    artifact.expressions = {};
    artifact.modelRef.digest = digest(dcmBytes);
    artifact.return = { type: 'BoundaryRecord', fields: Object.fromEntries(Object.entries(value)
      .map(([name, literal]) => [name, { type: fields[name], literal }])) };
    const result = api.prepareProgram(canonicalBytes(artifact), boundModel);
    assert.equal(result.diagnostic, null);
    return result.prepared;
  }

  const exact = record(8192), oversized = record(8193);
  const handle = prepare(exact);
  const accepted = await api.invokeProgram(handle, exact, f.invocation);
  assert.equal(accepted.classification, 'COMPLETED');
  assert.deepEqual(accepted.result, exact);
  assert.equal(f.calls.length, 1);
  const inputRejected = await api.invokeProgram(handle, oversized, f.invocation);
  assert.deepEqual(inputRejected.error, { code: 'BUDGET_EXCEEDED', path: '/input' });
  assert.equal(f.calls.length, 1);
  const outputRejected = await api.invokeProgram(prepare(oversized), exact, f.invocation);
  assert.deepEqual(outputRejected.error, { code: 'BUDGET_EXCEEDED', path: '/return' });
  assert.equal(Object.hasOwn(outputRejected, 'result'), false);
  assert.equal(f.calls.length, 2);
});
