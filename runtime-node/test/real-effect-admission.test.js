import test from 'node:test';
import assert from 'node:assert/strict';
import { setImmediate as immediate } from 'node:timers/promises';
import * as api from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { realFixture, runRealCases } from '../../conformance/generic-hooks/real-effect.mjs';
import { fixture, approval, registrations } from './reference-helpers.js';

const rejected = (result, code = 'ADMISSION_INVALID', path = '/binding/qualification') => {
  assert.equal(result.boundModel, null);
  assert.deepEqual(result.diagnostic, { code, path, message: 'The selected artifact or host configuration is invalid.' });
};
function prepared(f, binding = f.bind()) {
  assert.equal(binding.diagnostic, null);
  const result = api.prepareProgram(f.bytes, binding.boundModel, f.admission);
  assert.equal(result.diagnostic, null); return result.prepared;
}
const noIntent = outcome => { assert.deepEqual(outcome.effects, []); assert.equal(outcome.trace.some(row => row.event === 'EFFECT_INTENT'), false); };

test('shared effect/3 protocol facts and same-artifact mode outcomes', async () => {
  const results = await runRealCases(api);
  for (const suffix of ['main', 'zero', 'after-write-read']) {
    const simulation = results.find(row => row.id === `simulation-${suffix}`);
    const real = results.find(row => row.id === `real-${suffix}`);
    assert.deepEqual(simulation.outcome, real.outcome);
    assert.deepEqual(simulation.calls, real.calls);
    assert.deepEqual(simulation.consumption, real.consumption);
  }
});

test('qualification omission, explicit null and earlier profiles stay distinct', () => {
  const f = realFixture(api);
  rejected(f.bind(undefined)); rejected(f.bind(null));
  const sim = realFixture(api, { mode: 'simulation' });
  assert.equal(sim.bind(undefined).diagnostic, null); rejected(sim.bind(null)); rejected(sim.bind(sim.qualification()));
  for (const profile of ['read', 'effect']) {
    const old = fixture(profile);
    assert.equal(api.bindModel(old.dcmBytes, approval(old), registrations(old), undefined).diagnostic, null);
    rejected(api.bindModel(old.dcmBytes, approval(old), registrations(old), f.qualification()));
  }
  const old = fixture('effect');
  const denied = api.bindModel(old.dcmBytes, approval(old, { mode: 'real' }), registrations(old));
  assert.equal(denied.diagnostic.path, '/binding/approval/mode');
});

test('closed qualification identities reject before current checks or invokers', () => {
  for (const mutate of [
    q => { q.extra = true; }, q => { q.evidence.extra = true; }, q => { q.isCurrent = true; },
    q => { q.evidence.contract = 'unknown'; }, q => { q.evidence.id = ''; }, q => { q.evidence.id = 'é'.repeat(65); },
    q => { q.evidence.revision = 0; }, q => { q.evidence.revision = 1.5; }, q => { q.evidence.revision = 2 ** 53; },
    q => { q.evidence.scope = 'another-scope'; }, q => { q.evidence.keyType = 'other/1'; },
    q => { q.evidence.modelRef.digest = 'sha256:' + '0'.repeat(64); }, q => { q.evidence.modelRef.extra = true; },
    q => { q.evidence.environment.celNode = 'unknown'; }, q => { q.evidence.environment.extra = true; },
    q => { q.evidence.hostBuild = 'sha256:' + 'A'.repeat(64); }, q => { q.evidence.reportDigest = 'bad'; },
    q => { q.evidence.capabilities.reverse(); }, q => { q.evidence.capabilities.push(q.evidence.capabilities[0]); },
    q => { q.evidence.capabilities[0].version = 'unknown'; }, q => { q.evidence.capabilities[0].extra = true; },
  ]) {
    const f = realFixture(api), q = f.qualification(); mutate(q);
    rejected(f.bind(q)); assert.equal(f.state.checks, 0); assert.equal(f.calls.length, 0);
  }
  const f = realFixture(api);
  assert.equal(f.bind(f.qualification({ id: 'é'.repeat(64) })).diagnostic?.code, 'ADMISSION_INVALID');
  // A bounded but unapproved ID is denied by the independently selected host record.
  f.state.approved.id = 'é'.repeat(64);
  assert.equal(f.bind(f.qualification()).diagnostic, null);
});

test('qualification cap is inclusive before copying and later field validation', () => {
  const f = realFixture(api), q = f.qualification();
  q.evidence.id = '';
  const overhead = canonicalBytes(q.evidence).length;
  q.evidence.id = 'x'.repeat(16_384 - overhead);
  assert.equal(canonicalBytes(q.evidence).length, 16_384);
  rejected(f.bind(q)); assert.equal(f.state.checks, 0);
  q.evidence.id += 'x';
  rejected(f.bind(q), 'BUDGET_EXCEEDED'); assert.equal(f.state.checks, 0);
});

test('qualification and invoker descriptors are validated without getters', () => {
  for (const place of ['qualification', 'evidence', 'nested', 'catalog', 'invokers']) {
    const f = realFixture(api), q = f.qualification(); let getters = 0;
    const getter = { enumerable: true, configurable: true, get() { getters++; throw Error('private'); } };
    if (place === 'qualification') Object.defineProperty(q, 'isCurrent', getter);
    if (place === 'evidence') Object.defineProperty(q, 'evidence', getter);
    if (place === 'nested') Object.defineProperty(q.evidence.environment, 'celJava', getter);
    if (place === 'catalog') Object.defineProperty(q.evidence.capabilities[0], 'version', getter);
    if (place === 'invokers') Object.defineProperty(f.invokers, 'tasks.get', getter);
    const result = f.bind(q);
    assert.equal(result.boundModel, null); assert.equal(getters, 0); assert.equal(f.state.checks, 0);
  }
});

test('invalid current callbacks are synchronous and do not assimilate thenables or leak native rejections', async t => {
  let getters = 0, thenCalls = 0;
  const failures = [];
  const onUnhandled = failure => failures.push(failure);
  process.on('unhandledRejection', onUnhandled); t.after(() => process.off('unhandledRejection', onUnhandled));
  const thenable = { get then() { getters++; return () => { thenCalls++; }; } };
  const native = Promise.reject(Error('private rejected callback'));
  Object.defineProperty(native, 'then', { get() { getters++; throw Error('private getter'); } });
  for (const callback of [() => false, () => null, () => 'true', () => new Boolean(true), () => { throw Error('private'); },
    () => thenable, () => native, async () => { throw Error('private async'); }]) {
    const f = realFixture(api);
    rejected(f.bind(f.qualification({}, callback)));
    assert.equal(f.calls.length, 0);
  }
  await immediate();
  assert.deepEqual(failures, []); assert.equal(getters, 0); assert.equal(thenCalls, 0);
});

test('snapshots all authority references before the initial host callback', async () => {
  const f = realFixture(api), q = f.qualification();
  q.isCurrent = () => {
    f.approved.scope = 'changed'; f.approved.mode = 'simulation';
    f.invokers['tasks.create'].invoke = () => { throw Error('Replacement must not run'); };
    q.evidence.id = 'changed'; q.isCurrent = () => false;
    return true;
  };
  const handle = prepared(f, f.bind(q));
  const outcome = await api.invokeProgram(handle, { taskId: 'task1' }, f.invocation);
  assert.equal(outcome.classification, 'COMPLETED'); assert.equal(outcome.effects[0].state, 'CONFIRMED');
  assert.equal(f.calls.length, 2);
});

test('scope, mode and current decisions never move through shared pure caches', async () => {
  const first = realFixture(api), second = realFixture(api);
  const a = prepared(first), b = prepared(second);
  first.state.current = false;
  const denied = await api.invokeProgram(a, { taskId: 'task1' }, first.invocation); noIntent(denied);
  assert.equal(first.state.authorizations, 0); assert.equal(first.state.keyChecks, 0);
  assert.equal((await api.invokeProgram(b, { taskId: 'task1' }, second.invocation)).classification, 'COMPLETED');
});

test('fresh adapter budgets include qualification callback time', async () => {
  const f = realFixture(api), handle = prepared(f);
  f.state.onCheck = () => { f.state.now += 5; };
  const outcome = await api.invokeProgram(handle, { taskId: 'task1' }, f.invocation);
  assert.equal(outcome.classification, 'COMPLETED'); assert.equal(f.budgets.length, 2);
  assert.ok(f.budgets[0] < 1000 && f.budgets[1] < f.budgets[0]);
});

test('revocation during a pending authorizer cannot reserve a WRITE', async () => {
  const f = realFixture(api), handle = prepared(f);
  let entered, release;
  const started = new Promise(resolve => { entered = resolve; });
  const barrier = new Promise(resolve => { release = resolve; });
  f.state.onAuthorize = request => { if (request.capability === 'tasks.create') { entered(); return barrier; } };
  const pending = api.invokeProgram(handle, { taskId: 'task1' }, f.invocation);
  await started; f.state.current = false; release();
  const outcome = await pending;
  noIntent(outcome); assert.equal(outcome.error.code, 'AUTHORITY_DENIED'); assert.equal(f.calls.length, 1);
});

test('timeout-first UNKNOWN remains immutable after a late confirmed response', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const f = realFixture(api), handle = prepared(f);
  let entered, finish;
  const started = new Promise(resolve => { entered = resolve; });
  f.state.writeResponse = new Promise(resolve => { finish = resolve; });
  f.state.afterWrite = () => entered();
  const pending = api.invokeProgram(handle, { taskId: 'task1' }, f.invocation);
  await started; f.state.now = 1000; t.mock.timers.tick(1000);
  const outcome = await pending, snapshot = JSON.stringify(outcome);
  assert.equal(outcome.classification, 'UNCERTAIN'); assert.equal(outcome.effects[0].state, 'UNKNOWN');
  finish({ kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' }); await Promise.resolve(); await Promise.resolve();
  assert.equal(JSON.stringify(outcome), snapshot); assert.equal(f.calls.length, 2);
});

test('forged second WRITE and cross-mode preparation fail before dispatch', () => {
  const f = realFixture(api), binding = f.bind();
  const artifact = JSON.parse(f.bytes);
  artifact.steps.push({ ...artifact.steps[3], id: 'secondWrite' });
  const bytes = canonicalBytes(artifact);
  const forged = api.prepareProgram(bytes, binding.boundModel, { ...f.admission, expectedArtifactDigest: digest(bytes) });
  assert.equal(forged.prepared, null); assert.equal(f.calls.length, 0);
  const mixed = api.prepareProgram(f.bytes, binding.boundModel, { ...f.admission, mode: 'simulation' });
  assert.equal(mixed.prepared, null); assert.equal(mixed.diagnostic.path, '/admission/mode');
});
