import test from 'node:test';
import assert from 'node:assert/strict';
import { bindModel, prepareProgram, invokeProgram, invoke } from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { prepared as legacyPrepared, context as legacyContext } from './helpers.js';
import { fixture, approval, admission, registrations, bind, prepared, context, nativeKey, confirmed } from './reference-helpers.js';

const input = { taskId: 'task1' };
const receipt = state => [{ step: 'created', capability: 'tasks.create', state }];
const run = (handle, ctx = context(), value = input) => invokeProgram(handle, value, ctx);
function failure(out, code, path, effects = []) {
  assert.deepEqual(out.error, { code, path }); assert.deepEqual(out.effects ?? [], effects);
  assert.equal(Object.hasOwn(out, 'result'), false);
}
const effectTrace = [
  { sequence: 0, event: 'INVOCATION_START' },
  { sequence: 1, event: 'STEP_START', step: 'eligibleProject' },
  { sequence: 2, event: 'VALUE_BOUND', step: 'eligibleProject' },
  { sequence: 3, event: 'STEP_START', step: 'source' },
  { sequence: 4, event: 'CALL_DISPATCHED', step: 'source', capability: 'tasks.get' },
  { sequence: 5, event: 'CALL_SUCCEEDED', step: 'source', capability: 'tasks.get' },
  { sequence: 6, event: 'STEP_START', step: 'eligible' },
  { sequence: 7, event: 'VALUE_BOUND', step: 'eligible' },
  { sequence: 8, event: 'STEP_START', step: 'created' },
  { sequence: 9, event: 'GUARD_EVALUATED', step: 'created', taken: true },
  { sequence: 10, event: 'EFFECT_INTENT', step: 'created', capability: 'tasks.create' },
  { sequence: 11, event: 'CALL_DISPATCHED', step: 'created', capability: 'tasks.create' },
  { sequence: 12, event: 'EFFECT_RECORDED', step: 'created', capability: 'tasks.create', state: 'CONFIRMED' },
  { sequence: 13, event: 'CALL_SUCCEEDED', step: 'created', capability: 'tasks.create' },
  { sequence: 14, event: 'RETURNED' }, { sequence: 15, event: 'TERMINATED' },
];

test('all six independently pinned Java programs execute and source alone selects project/title/capability', async () => {
  const pins = new Set();
  for (const [profile, variants] of [['read', ['main', 'repeated', 'alternate']], ['effect', ['main', 'project2', 'title2']]]) {
    const main = fixture(profile); const calls = []; let selectedProject = 'project1';
    const invokers = registrations(main);
    for (const [id, row] of Object.entries(invokers)) row.invoke = async (args, protectedArgs, left, host) => {
      calls.push({ id, args, protectedArgs, left, host });
      return id === 'tasks.create' ? confirmed : { kind: 'SUCCESS', value: { id: 'task1', ...(profile === 'effect' ? { projectId: selectedProject } : {}), status: 'COMPLETED' } };
    };
    assert.equal(Object.keys(invokers).length, profile === 'read' ? 3 : 2);
    const model = bind(main, invokers);
    for (const variant of variants) {
      calls.length = 0; selectedProject = variant === 'project2' ? 'project2' : 'project1';
      const f = fixture(profile, variant); pins.add(f.pin); assert.equal(f.dcmPin, main.dcmPin);
      const ctx = context(profile); const out = await run(prepared(f, model), ctx);
      assert.equal(out.classification, 'COMPLETED');
      assert.deepEqual(out.result, profile === 'effect' ? { status: 'CREATED' } : { classification: 'DONE' });
      assert.deepEqual(calls.map(c => c.id), profile === 'effect' ? ['tasks.get', 'tasks.create'] : variant === 'repeated' ? ['tasks.get', 'tasks.get'] : [variant === 'alternate' ? 'tasks.lookup' : 'tasks.get']);
      assert.deepEqual({ ...calls[0].args }, { taskId: 'task1' });
      if (profile === 'effect') {
        assert.deepEqual({ ...calls[1].args }, { projectId: selectedProject, title: variant === 'title2' ? 'Follow up again' : 'Follow up' });
        assert.deepEqual(out.effects, receipt('CONFIRMED')); assert.deepEqual(out.trace, effectTrace);
        for (const call of calls) { assert.strictEqual(call.host.requestKey, ctx.requestKey); assert.deepEqual(call.host.event, ctx.event); }
      }
      for (const call of calls) { assert.deepEqual({ ...call.protectedArgs }, { actorId: 'user1' }); assert.equal(call.left, 1000); }
    }
  }
  assert.equal(pins.size, 6);
});

test('opaque closed wrappers and profile boundaries reject lookalikes and legacy handles', async () => {
  const f = fixture(); const binding = bindModel(f.dcmBytes, approval(f), registrations(f));
  const preparation = prepareProgram(f.bytes, binding.boundModel, admission(f));
  for (const [wrapper, fields, handle] of [[binding, ['boundModel', 'diagnostic'], binding.boundModel], [preparation, ['diagnostic', 'prepared'], preparation.prepared]]) {
    assert.deepEqual(Object.keys(wrapper).sort(), fields); assert.ok(Object.isFrozen(wrapper));
    assert.ok(Object.isFrozen(handle)); assert.deepEqual(Reflect.ownKeys(handle), []);
  }
  for (const fake of [{}, Object.freeze(Object.create(null)), binding, preparation]) {
    assert.equal(prepareProgram(f.bytes, fake, admission(f)).diagnostic.path, '/binding');
    failure(await run(fake), 'HOST_CONTEXT_INVALID', '');
  }
  failure(await run(legacyPrepared()), 'HOST_CONTEXT_INVALID', '');
  failure(await invoke(preparation.prepared, input, legacyContext()), 'HOST_CONTEXT_INVALID', '');
});

test('same task response produces NO_OP when the source selects another project', async () => {
  const f = fixture(); const model = bind(f);
  assert.deepEqual((await run(prepared(f, model))).result, { status: 'CREATED' });
  assert.deepEqual((await run(prepared(fixture('effect', 'project2'), model))).result, { status: 'NO_OP' });
});

test('binding snapshots bytes, approval, complete registrations and validator; replacement retains old dispatch', async () => {
  const f = fixture(); f.dcmBytes = Uint8Array.from(f.dcmBytes);
  const approved = approval(f); let validations = 0, reads = 0;
  const originalValidator = approved.keyBinding.validate;
  approved.keyBinding.validate = key => { validations++; return originalValidator(key); };
  const invokers = registrations(f, { 'tasks.get': { invoke: async () => { reads++; return { kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'COMPLETED' } }; } } });
  const model = bind(f, invokers, approved); assert.equal(validations, 0); assert.equal(reads, 0);
  f.dcmBytes.fill(0); approved.scope = 'other'; approved.modelRef.digest = 'changed'; approved.environment.celNode = 'changed';
  approved.keyBinding.validate = () => false; approved.keyBinding.type = 'changed';
  for (const row of Object.values(invokers)) { row.version = 'changed'; row.mode = 'real'; row.keyType = 'changed'; row.invoke = () => { throw new Error('replacement'); }; }
  f.bytes = Uint8Array.from(f.bytes);
  const h = prepared(f, model); f.bytes.fill(0);
  assert.equal((await run(h)).classification, 'COMPLETED'); assert.ok(validations > 0); assert.equal(reads, 1);
  const fresh = fixture(); const replacement = bind(fresh, registrations(fresh, { 'tasks.get': { invoke: async () => ({ kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'OPEN' } }) } }));
  assert.deepEqual((await run(prepared(fresh, replacement))).result, { status: 'NO_OP' });
  assert.deepEqual((await run(h)).result, { status: 'CREATED' }); assert.equal(reads, 2);
});

test('read binding requires unused WRITE and exact catalog before any execution', () => {
  const f = fixture('read'); let callbacks = 0;
  for (const mutate of [rows => { delete rows['tasks.create']; }, rows => { rows.extra = rows['tasks.get']; }, rows => { rows['tasks.lookup'].version = '2'; }]) {
    const rows = registrations(f); for (const row of Object.values(rows)) row.invoke = () => { callbacks++; };
    mutate(rows); const result = bindModel(f.dcmBytes, approval(f), rows);
    assert.equal(result.boundModel, null); assert.equal(result.diagnostic.code, 'ADMISSION_INVALID');
  }
  assert.equal(callbacks, 0);
});

test('same-named Task types coexist across models and scopes; reused Prepared keeps concurrent request snapshots', async () => {
  const f = fixture(); const captured = []; let release;
  const gate = new Promise(resolve => { release = resolve; });
  const invokers = registrations(f);
  for (const [id, row] of Object.entries(invokers)) { const original = row.invoke; row.invoke = async (...args) => { captured.push({ id, args }); await gate; return original(...args); }; }
  const h = prepared(f, bind(f, invokers));
  const contexts = [context(), context('effect', { bindings: { actorId: 'user2' }, event: { id: 'event2', source: 'task2' } })];
  const keys = contexts.map(c => c.requestKey); const values = [{ taskId: 'task1' }, { taskId: 'task2' }];
  const pending = contexts.map((c, i) => run(h, c, values[i]));
  contexts.forEach(c => { c.bindings.actorId = 'changed'; c.event.source = 'changed'; c.requestKey = nativeKey(); }); values.forEach(v => { v.taskId = 'changed'; });
  release(); const results = await Promise.all(pending); assert.ok(results.every(o => o.classification === 'COMPLETED'));
  assert.equal(captured.length, 4);
  for (const { args } of captured) { const i = args[1].actorId === 'user1' ? 0 : 1; assert.equal(args[1].actorId, `user${i + 1}`); assert.strictEqual(args[3].requestKey, keys[i]); assert.deepEqual(args[3].event, { id: `event${i + 1}`, source: `task${i + 1}` }); if (args[0].taskId) assert.equal(args[0].taskId, `task${i + 1}`); }
  const read = fixture('read'); assert.equal((await run(prepared(read), context('read'))).classification, 'COMPLETED');
  const scope2 = prepared(f, bind(f, registrations(f), approval(f, { scope: 'scope2' })));
  failure(await run(scope2), 'HOST_CONTEXT_INVALID', ''); assert.equal((await run(scope2, context('effect', { scope: 'scope2' }))).classification, 'COMPLETED');
  assert.equal((await run(h)).classification, 'COMPLETED');
});

test('fresh revocation after READ prevents WRITE authorization, intent and dispatch', async () => {
  const f = fixture(); let allowed = true, writes = 0; const requests = [];
  const h = prepared(f, bind(f, registrations(f, {
    'tasks.get': { invoke: async () => { allowed = false; return { kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'COMPLETED' } }; } },
    'tasks.create': { invoke: async () => { writes++; return confirmed; } },
  })));
  const out = await run(h, context('effect', { authorize: async request => { requests.push(request); return allowed; } }));
  failure(out, 'AUTHORITY_DENIED', '/steps/3'); assert.equal(writes, 0);
  assert.deepEqual(requests, [{ entrypoint: 'TaskCompleted' }, { entrypoint: 'TaskCompleted', capability: 'tasks.get', version: '1' }, { entrypoint: 'TaskCompleted', capability: 'tasks.create', version: '1' }]);
  assert.equal(out.trace.some(e => e.event === 'EFFECT_INTENT'), false);
});

test('malformed READ stops at its real step and a lost WRITE response retains UNKNOWN', async () => {
  const f = fixture(); let writes = 0;
  const h = prepared(f, bind(f, registrations(f, {
    'tasks.get': { invoke: async () => ({ kind: 'SUCCESS', value: { id: 'task1', status: 'COMPLETED' } }) },
    'tasks.create': { invoke: async () => { writes++; return confirmed; } },
  })));
  failure(await run(h), 'ADAPTER_INVALID', '/steps/1'); assert.equal(writes, 0);
  const lost = prepared(f, bind(f, registrations(f, { 'tasks.create': { invoke: async () => { throw new Error('private transport detail'); } } })));
  const out = await run(lost); failure(out, 'ADAPTER_EXCEPTION', '/steps/3', receipt('UNKNOWN'));
  assert.equal(out.classification, 'UNCERTAIN'); assert.doesNotMatch(JSON.stringify(out), /private transport detail/);
});

test('unbranded native keys fail before authorizers or adapters run', async () => {
  const f = fixture(); let callbacks = 0;
  const rows = registrations(f); for (const row of Object.values(rows)) row.invoke = async () => { callbacks++; return confirmed; };
  const h = prepared(f, bind(f, rows));
  for (const requestKey of [null, 'key', Object.freeze({})]) {
    failure(await run(h, context('effect', { requestKey, authorize: async () => { callbacks++; return true; } })), 'HOST_CONTEXT_INVALID', '');
  }
  assert.equal(callbacks, 0);
});

test('narrowed authority allows an unreached WRITE but denies a reached WRITE; malformed/widened restrictions fail preflight', async () => {
  const f = fixture();
  for (const restriction of [{ effectGrant: 0 }, { capabilityPolicy: { deny: ['tasks.create'] } }]) {
    for (const status of ['OPEN', 'COMPLETED']) {
      let writes = 0;
      const h = prepared(f, bind(f, registrations(f, { 'tasks.get': { invoke: async () => ({ kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status } }) }, 'tasks.create': { invoke: async () => { writes++; return confirmed; } } })));
      const out = await run(h, context('effect', restriction));
      if (status === 'OPEN') assert.deepEqual(out.result, { status: 'NO_OP' }); else failure(out, 'AUTHORITY_DENIED', '/steps/3');
      assert.equal(writes, 0);
    }
  }
  const h = prepared(f, undefined, { capabilityPolicy: { maxCalls: { 'tasks.create': 1 } } });
  for (const restriction of [{ effectGrant: 2 }, { effectGrant: null }, { capabilityPolicy: null }, { capabilityPolicy: { maxCalls: { 'tasks.create': 2 } } }, { capabilityPolicy: { allow: ['unknown'] } }]) {
    let checks = 0; failure(await run(h, context('effect', { ...restriction, authorize: async () => { checks++; return true; } })), 'HOST_CONTEXT_INVALID', ''); assert.equal(checks, 0);
  }
});

test('repeated READ static policy quota and aggregate call limit are independent', async () => {
  const f = fixture('read', 'repeated'); const model = bind(f);
  for (const [extra, code, path] of [[{ capabilityPolicy: { maxCalls: { 'tasks.get': 1 } } }, 'AUTHORITY_DENIED', '/admission/capabilityPolicy'], [{ limits: { calls: 1 } }, 'BUDGET_EXCEEDED', '/artifact/steps']]) {
    const rejected = prepareProgram(f.bytes, model, admission(f, extra)); assert.equal(rejected.prepared, null); assert.equal(rejected.diagnostic.code, code); assert.equal(rejected.diagnostic.path, path);
  }
  assert.equal((await run(prepared(f, model, { capabilityPolicy: { maxCalls: { 'tasks.get': 2 } }, limits: { calls: 2 } }), context('read'))).classification, 'COMPLETED');
});

test('winning WRITE evidence settles before expiry, clock regression and invalid clock', async () => {
  const f = fixture();
  for (const response of [confirmed, { kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' }, { kind: 'TRANSIENT_FAILURE', effect: 'NONE' }]) for (const [next, code, path] of [[1000, 'BUDGET_EXCEEDED', '/steps/3'], [9, 'HOST_CONTEXT_INVALID', ''], [NaN, 'HOST_CONTEXT_INVALID', '']]) {
    let now = 10;
    const h = prepared(f, bind(f, registrations(f, { 'tasks.create': { invoke: async () => { now = next; return response; } } })));
    const out = await run(h, context('effect', { deadline: { clock: { nowMs: () => now }, expiresAtMs: 1000 } }));
    failure(out, code, path, receipt(response.effect)); assert.equal(out.trace.at(-2).event, 'EFFECT_RECORDED');
  }
});

test('timeout winner keeps UNKNOWN receipt and cannot be rewritten by late WRITE responses', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  try {
    const f = fixture();
    for (const response of [confirmed, { kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' }, { kind: 'TRANSIENT_FAILURE', effect: 'NONE' }]) {
      let release, started; const dispatched = new Promise(resolve => { started = resolve; });
      const h = prepared(f, bind(f, registrations(f, { 'tasks.create': { invoke: () => new Promise(resolve => { release = resolve; started(); }) } })));
      const pending = run(h); await dispatched; t.mock.timers.tick(1000);
      const out = await pending; failure(out, 'BUDGET_EXCEEDED', '/steps/3', receipt('UNKNOWN')); assert.equal(out.classification, 'UNCERTAIN');
      const before = JSON.stringify(out); release(response); await new Promise(resolve => setImmediate(resolve)); assert.equal(JSON.stringify(out), before);
      assert.equal(out.trace.filter(e => e.event === 'EFFECT_RECORDED').length, 1);
    }
  } finally { t.mock.timers.reset(); }
});

test('mutated real Java program rejects nominal forgery, environment forgery and checked blob replacement', () => {
  const f = fixture(); const model = bind(f);
  for (const mutate of [
    a => { a.steps[0].value.type = 'TaskId'; },
    a => { a.environment.celNode = 'unknown'; },
    a => { a.expressions.e2.checked = a.expressions.e3.checked; },
  ]) {
    const a = structuredClone(f.artifact); mutate(a); const bytes = canonicalBytes(a);
    const rejected = prepareProgram(bytes, model, admission(f, { expectedArtifactDigest: digest(bytes) }));
    assert.equal(rejected.prepared, null); assert.equal(rejected.diagnostic.code, 'ARTIFACT_INVALID');
  }
});

test('second guarded WRITE rejects conservatively with cloned Java expressions and sequential IDs', () => {
  const f = fixture(); const a = structuredClone(f.artifact);
  a.expressions.e4 = a.expressions.e3;
  a.expressions.e3 = structuredClone(a.expressions.e2);
  a.return.fields.status.expr = 'e4';
  const second = structuredClone(a.steps[3]); second.id = 'createdAgain'; second.args.projectId.expr = 'e3';
  a.steps.push(second);
  const bytes = canonicalBytes(a);
  const rejected = prepareProgram(bytes, bind(f), admission(f, { expectedArtifactDigest: digest(bytes) }));
  assert.equal(rejected.prepared, null);
  assert.equal(rejected.diagnostic.code, 'BUDGET_EXCEEDED'); assert.equal(rejected.diagnostic.path, '/artifact/steps/4');
});

test('frozen envelope field order selects compiler before invalid format or profile', () => {
  const f = fixture('read'); const model = bind(f);
  for (const field of ['format', 'profile']) {
    const value = structuredClone(f.artifact); value.compiler = ''; value[field] = 'bad'; const bytes = canonicalBytes(value);
    const result = prepareProgram(bytes, model, admission(f, { expectedArtifactDigest: digest(bytes) }));
    assert.equal(result.diagnostic.code, 'ARTIFACT_INVALID'); assert.equal(result.diagnostic.path, '/artifact/compiler');
  }
});

test('unknown /2 calls retain the published capability diagnostic', () => {
  const f = fixture('read'), value = structuredClone(f.artifact); value.steps[0].capability = 'tasks.unknown';
  const bytes = canonicalBytes(value), result = prepareProgram(bytes, bind(f), admission(f, { expectedArtifactDigest: digest(bytes) }));
  assert.equal(result.diagnostic.code, 'CAPABILITY_UNKNOWN'); assert.equal(result.diagnostic.path, '/artifact/steps/0');
  value.steps[0].capability = ['tasks.get']; const malformed = canonicalBytes(value);
  const invalid = prepareProgram(malformed, bind(f), admission(f, { expectedArtifactDigest: digest(malformed) }));
  assert.equal(invalid.diagnostic.code, 'ARTIFACT_INVALID'); assert.equal(invalid.diagnostic.path, '/artifact/steps/0');
});
