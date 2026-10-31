import test from 'node:test';
import assert from 'node:assert/strict';
import { createHarness } from './generic-capacity.mjs';
import * as runtime from '../../runtime-node/src/index.js';
import { fixture, approval, admission, registrations } from '../../runtime-node/test/reference-helpers.js';

const deferred = () => {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
};

function fakeApi(overrides = {}) {
  let next = 0;
  const calls = { bind: 0, prepare: 0, invoke: 0 };
  const api = {
    bindModel() { calls.bind++; return { boundModel: Object.freeze({ id: ++next }), diagnostic: null }; },
    prepareProgram() { calls.prepare++; return { prepared: Object.freeze({ id: ++next }), diagnostic: null }; },
    invokeProgram() { calls.invoke++; return Promise.resolve('ok'); },
    ...overrides,
  };
  return { api, calls };
}

const bytes = n => new Uint8Array(n);

test('enforces model and artifact exact limits before calling the API', () => {
  const { api, calls } = fakeApi();
  const h = createHarness(api);
  const handles = Array.from({ length: 4 }, () => h.bind(bytes(65_536), {}, {}).boundModel);
  assert.equal(calls.bind, 4);
  assert.equal(h.usage().canonicalModelBytesReserved, 262_144); assert.equal(h.usage().descriptorBytesReserved, 262_144);
  assert.throws(() => h.bind(bytes(65_537), {}, {}), e => e.reason === 'CAPACITY_EXCEEDED');
  assert.throws(() => h.bind(bytes(0), {}, {}), e => e.reason === 'CAPACITY_EXCEEDED');
  assert.equal(calls.bind, 4);
  const isolated = createHarness(api);
  const model = isolated.bind(bytes(1), {}, {}).boundModel;
  const artifacts = Array.from({ length: 8 }, () => isolated.prepare(bytes(1_048_576), model, {}).prepared);
  assert.equal(calls.prepare, 8);
  assert.throws(() => isolated.prepare(bytes(1_048_577), model, {}), e => e.reason === 'CAPACITY_EXCEEDED');
  assert.throws(() => isolated.prepare(bytes(0), model, {}), e => e.reason === 'CAPACITY_EXCEEDED');
  assert.equal(calls.prepare, 8);
  assert.deepEqual(isolated.usage(), { models: 1, canonicalModelBytesReserved: 65_536, descriptorBytesReserved: 65_536, prepared: 8,
    artifactBytesRetained: 8_388_608, inflight: 0 });
  assert.equal(artifacts.length, 8);
  assert.equal(handles.length, 4);
});

test('raw byte overflow rejects even when handle capacity remains available', () => {
  const { api, calls } = fakeApi(); const h = createHarness(api);
  assert.throws(() => h.bind(bytes(65_537), {}, {}), e => e.reason === 'CAPACITY_EXCEEDED'); assert.equal(calls.bind, 0);
  const model = h.bind(bytes(65_536), {}, {}).boundModel;
  assert.throws(() => h.prepare(bytes(1_048_577), model, {}), e => e.reason === 'CAPACITY_EXCEEDED'); assert.equal(calls.prepare, 0);
  assert.ok(h.prepare(bytes(1_048_576), model, {}).prepared);
});

test('blocks reentrant bind and rejects foreign handles without API calls', () => {
  let h;
  let nested;
  let bindCalls = 0;
  const api = { bindModel() {
    bindCalls++;
    try { h.bind(bytes(1), {}, {}); } catch (e) { nested = e; }
    return { boundModel: Object.freeze({}), diagnostic: null };
  } };
  h = createHarness(api);
  const bound = h.bind(bytes(1), {}, {}).boundModel;
  assert.equal(nested.reason, 'CAPACITY_EXCEEDED');
  assert.equal(bindCalls, 1);
  assert.throws(() => h.prepare(bytes(1), {}, {}), e => e.reason === 'CAPACITY_EXCEEDED');
  assert.equal(h.releaseModel(Object.freeze({})), false);
  assert.equal(bound !== undefined, true);
});

test('pins models through prepared and in-flight use, then permits release and cold rebind', async () => {
  const pending = deferred();
  const { api } = fakeApi({ invokeProgram: () => pending.promise });
  const h = createHarness(api);
  const model = h.bind(bytes(1), {}, {}).boundModel;
  const prepared = h.prepare(bytes(1), model, {}).prepared;
  assert.equal(h.releaseModel(model), false);
  const running = h.invoke(prepared, null, {});
  assert.equal(h.releasePrepared(prepared), false);
  assert.equal(h.releaseModel(model), false);
  pending.resolve('done');
  assert.equal(await running, 'done');
  assert.equal(h.releasePrepared(prepared), true);
  assert.equal(h.releaseModel(model), true);
  assert.ok(h.bind(bytes(1), {}, {}).boundModel);
});

test('limits concurrent invocations to two without queueing and isolates instances', async () => {
  const pending = [deferred(), deferred(), deferred()];
  let calls = 0;
  const { api } = fakeApi({ invokeProgram: () => pending[calls++].promise });
  const a = createHarness(api), b = createHarness(api);
  const m = a.bind(bytes(1), {}, {}).boundModel;
  const p = a.prepare(bytes(1), m, {}).prepared;
  const other = b.bind(bytes(1), {}, {}).boundModel;
  const q = b.prepare(bytes(1), other, {}).prepared;
  const first = a.invoke(p, null, {}), second = a.invoke(p, null, {});
  await assert.rejects(a.invoke(p, null, {}), e => e.reason === 'CAPACITY_EXCEEDED');
  const independent = b.invoke(q, null, {});
  assert.equal(calls, 3);
  pending.forEach(x => x.resolve('ok'));
  await Promise.all([first, second, independent]);
});

test('underlying bind and prepare failures do not consume slots', () => {
  let failBind = true, failPrepare = true;
  const { api, calls } = fakeApi({
    bindModel() { calls.bind++; if (failBind) throw new Error('bind'); return { boundModel: Object.freeze({}), diagnostic: null }; },
    prepareProgram() { calls.prepare++; if (failPrepare) throw new Error('prepare'); return { prepared: Object.freeze({}), diagnostic: null }; },
  });
  const h = createHarness(api);
  assert.throws(() => h.bind(bytes(1), {}, {}), /bind/);
  failBind = false;
  const model = h.bind(bytes(1), {}, {}).boundModel;
  assert.throws(() => h.prepare(bytes(1), model, {}), /prepare/);
  failPrepare = false;
  assert.ok(h.prepare(bytes(1), model, {}).prepared);
  assert.equal(h.usage().models, 1);
  assert.equal(h.usage().prepared, 1);
});

test('prepare reentrancy and invocation failure release no queue or stale reservation', async () => {
  let h, model, calls = 0;
  const { api } = fakeApi({ prepareProgram() {
    calls++;
    assert.throws(() => h.prepare(bytes(1), model, {}), e => e.reason === 'CAPACITY_EXCEEDED');
    return { prepared: Object.freeze({}), diagnostic: null };
  }, invokeProgram: async () => { throw new Error('Unavailable'); } });
  h = createHarness(api); model = h.bind(bytes(1), {}, {}).boundModel;
  const p = h.prepare(bytes(1), model, {}).prepared; assert.equal(calls, 1);
  await assert.rejects(h.invoke(p, {}, {}), /Unavailable/);
  assert.equal(h.usage().inflight, 0); assert.ok(h.releasePrepared(p)); assert.ok(h.releaseModel(model));
});

test('actual approved models count by handle and actual Prepared handles pin their model', () => {
  let binds = 0, prepares = 0;
  const api = { ...runtime, bindModel: (...args) => { binds++; return runtime.bindModel(...args); },
    prepareProgram: (...args) => { prepares++; return runtime.prepareProgram(...args); } };
  const f = fixture('read'), h = createHarness(api);
  const models = Array.from({ length: 4 }, () => h.bind(f.dcmBytes, approval(f), registrations(f)).boundModel);
  assert.equal(new Set(models).size, 4);
  assert.throws(() => h.bind(f.dcmBytes, approval(f), registrations(f)), e => e.reason === 'CAPACITY_EXCEEDED'); assert.equal(binds, 4);
  const programs = Array.from({ length: 8 }, () => h.prepare(f.bytes, models[0], admission(f)).prepared);
  assert.equal(new Set(programs).size, 8); assert.equal(h.releaseModel(models[0]), false);
  assert.throws(() => h.prepare(f.bytes, models[0], admission(f)), e => e.reason === 'CAPACITY_EXCEEDED'); assert.equal(prepares, 8);
  programs.forEach(handle => assert.ok(h.releasePrepared(handle))); models.forEach(handle => assert.ok(h.releaseModel(handle)));
  assert.equal(h.usage().models, 0); assert.equal(h.usage().prepared, 0);
});
