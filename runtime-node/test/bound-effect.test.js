import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fromBinary, fromJson, toBinary } from '@bufbuild/protobuf';
import { celEnv, CelScalar, plan } from '@bufbuild/cel';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import * as api from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { boundFixture, runBoundCases, verifyBoundTransports } from '../../conformance/generic-hooks/bound-effect.mjs';
import { realFixture } from '../../conformance/generic-hooks/real-effect.mjs';

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

test('shared host-bound protocol and identical real/simulation artifact results', async () => {
  const rows = await runBoundCases(api);
  const pairs = ['main', 'zero', 'after-write-read', 'no-call'].map(program => [`real-${program}`, `simulation-${program}`]);
  pairs.push(['title-unicode', 'title-simulation']);
  for (const [realId, simulationId] of pairs) {
    const real = rows.find(row => row.id === realId), simulation = rows.find(row => row.id === simulationId);
    assert.deepEqual(real.outcome, simulation.outcome); assert.deepEqual(real.calls, simulation.calls);
    assert.deepEqual(real.consumption, simulation.consumption);
  }
  const overflow = rows.find(row => row.id === 'title-composed-overflow');
  assert.equal(overflow.outcome.trace.some(row => row.event === 'EFFECT_INTENT'), false);
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

test('new and legacy API families reject extra deployment or admission inputs', async () => {
  const f = boundFixture(api), binding = f.bind(), handle = prepared(f), old = realFixture(api);
  assert.equal(api.bindModel(old.dcmBytes, old.invokers).boundModel, null);
  assert.equal(api.bindModel(f.dcmBytes, old.approved, f.implementations).boundModel, null);
  denied(api.prepareProgram(f.bytes, binding.boundModel, undefined), '/admission');
  assert.equal(api.prepareProgram(old.bytes, binding.boundModel).prepared, null);
  assert.equal(api.prepareProgram(f.bytes, old.bind().boundModel).prepared, null);
  for (const field of ['authorize', 'scope', 'qualification', 'keyBinding', 'capabilityPolicy', 'effectGrant', 'limits']) {
    const outcome = await api.invokeProgram(handle, input, { ...f.invocation, [field]: field === 'authorize' ? () => true : {} });
    assert.deepEqual(outcome.error, { code: 'HOST_CONTEXT_INVALID', path: '' });
  }
  assert.equal((await api.invokeProgram(handle, input, f.invocation, undefined)).error.code, 'HOST_CONTEXT_INVALID');
  assert.equal((await api.invoke(handle, input, f.invocation)).error.code, 'HOST_CONTEXT_INVALID');
  assert.equal(f.calls.length, 0);
});

test('preparation validates compiler environment model and artifact structure internally', () => {
  const f = boundFixture(api), bound = f.bind().boundModel;
  for (const field of ['compiler', 'environment', 'modelRef', 'format', 'profile']) {
    const artifact = JSON.parse(f.bytes);
    if (field === 'environment') artifact.environment.celJava = 'unsupported';
    else if (field === 'modelRef') artifact.modelRef.digest = 'sha256:' + '0'.repeat(64);
    else artifact[field] = 'unsupported';
    const result = api.prepareProgram(canonicalBytes(artifact), bound);
    assert.equal(result.prepared, null); assert.equal(result.diagnostic.path, `/artifact/${field}`);
  }
  assert.equal(api.prepareProgram(new Uint8Array(1_048_577), bound).diagnostic.code, 'BUDGET_EXCEEDED');
  assert.equal(f.calls.length, 0);
});

test('effect/4 structural artifact requires native preparation', async () => {
  const fixtures = new URL('../../conformance/effect4-structural/', import.meta.url);
  const bytes = readFileSync(new URL('artifact-handler.json', fixtures));
  assert.equal(digest(bytes), readFileSync(new URL('artifact-handler.sha256', fixtures), 'utf8').trim());
  const f = boundFixture(api, { mode: 'simulation' });
  const binding = f.bind();
  assert.equal(binding.diagnostic, null);
  for (const row of JSON.parse(readFileSync(new URL('residuals.json', fixtures)))) {
    const artifact = JSON.parse(bytes);
    const target = row.path.slice(0, -1).reduce((current, part) => current[part], artifact);
    target[row.path.at(-1)] = row.value;
    denied(api.prepareProgram(canonicalBytes(artifact), binding.boundModel), row.nodePath, 'ARTIFACT_INVALID');
    assert.equal(f.calls.length, 0, row.id);
  }
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

test('timeout-first UNKNOWN is immutable after a late confirmed response and never retries', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const f = boundFixture(api), handle = prepared(f); let enter, finish;
  const started = new Promise(resolve => { enter = resolve; });
  f.state.afterWrite = () => { enter(); return new Promise(resolve => { finish = resolve; }); };
  const pending = api.invokeProgram(handle, input, f.invocation); await started;
  f.state.now = 1000; t.mock.timers.tick(1000);
  const outcome = await pending, before = JSON.stringify(outcome);
  assert.equal(outcome.classification, 'UNCERTAIN'); assert.equal(outcome.effects[0].state, 'UNKNOWN');
  finish(); await Promise.resolve(); await Promise.resolve();
  assert.equal(JSON.stringify(outcome), before); assert.deepEqual(f.nativeActions, ['tasks.get', 'tasks.create']);
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
  const secondWrite = JSON.parse(f.bytes); secondWrite.steps.push({ ...secondWrite.steps[3], id: 'secondWrite' });
  assert.equal(api.prepareProgram(canonicalBytes(secondWrite), bound).diagnostic.code, 'BUDGET_EXCEEDED');
  for (const field of ['capabilityPolicy', 'effectGrant']) {
    const dcm = structuredClone(f.dcm); dcm.entrypoints.TaskCompleted[field] = field === 'effectGrant' ? 0 : { maxCalls: { 'tasks.create': 0 } };
    const binding = api.bindModel(canonicalBytes(dcm), f.implementations); assert.equal(binding.diagnostic, null);
    const artifact = JSON.parse(f.bytes); artifact.modelRef.digest = digest(canonicalBytes(dcm));
    assert.equal(api.prepareProgram(canonicalBytes(artifact), binding.boundModel).prepared, null);
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
    const expr = tree(leaves), checked = fromJson(CheckedExprSchema, { expr, sourceInfo: { location: 'keir-read/1' }, typeMap, referenceMap });
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
