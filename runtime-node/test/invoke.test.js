import test from 'node:test';
import assert from 'node:assert/strict';
import { adapters, context, prepared } from './helpers.js';
import { invoke } from '../src/index.js';

const goodTask = { id: 'task1', status: 'COMPLETED' };
const input = { taskId: 'task1' };
const run = (handle, value = input, host = context()) => invoke(handle, value, host);

test('one immutable prepared handle serves independent callers and returns DONE or PENDING', async () => {
  const handle = prepared(async args => ({ kind: 'SUCCESS', value: { id: args.taskId, status: args.taskId === 'task1' ? 'COMPLETED' : 'OPEN' } }));
  const [done, pending] = await Promise.all([
    run(handle), run(handle, { taskId: 'task2' }, context({ bindings: { actorId: 'user2' } })),
  ]);
  assert.deepEqual(done.result, { classification: 'DONE' });
  assert.deepEqual(pending.result, { classification: 'PENDING' });
  assert.equal(Object.isFrozen(handle), true);
  assert.equal(Object.isFrozen(done), true);
  assert.equal(Object.isFrozen(done.trace), true);
});

test('keeps caller input, adapter arguments, and protected arguments separate and immutable', async () => {
  const original = structuredClone(input);
  let seen;
  const handle = prepared(async (agentArgs, protectedArgs, remaining) => {
    seen = { agentArgs, protectedArgs, remaining };
    assert.equal(Object.isFrozen(agentArgs), true);
    assert.equal(Object.isFrozen(protectedArgs), true);
    return { kind: 'SUCCESS', value: goodTask };
  });
  await run(handle);
  assert.deepEqual(input, original);
  assert.deepEqual({ ...seen.agentArgs }, { taskId: 'task1' });
  assert.deepEqual({ ...seen.protectedArgs }, { actorId: 'user1' });
  assert.ok(seen.remaining > 0);
  assert.equal(Object.hasOwn(seen.agentArgs, 'actorId'), false);
});

test('validates input and protected bindings before dispatch', async t => {
  for (const [label, value] of [['missing', undefined], ['null', null], ['wrong type', { taskId: 3 }], ['unknown field', { ...input, secret: 'x' }], ['oversized value', { taskId: 'x'.repeat(9000) }]]) {
    await t.test(label, async () => {
      let calls = 0;
      const handle = prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; });
      const out = value === undefined ? await invoke(handle, undefined, context()) : await run(handle, value);
      assert.equal(out.classification, label === 'oversized value' ? 'LIMIT_EXCEEDED' : 'FAILED');
      assert.equal(out.error.code, label === 'oversized value' ? 'BUDGET_EXCEEDED' : 'INPUT_INVALID');
      assert.equal(calls, 0);
    });
  }
  for (const [label, bindings] of [['missing', {}], ['extra', { actorId: 'user1', secret: 'x' }], ['wrong type', { actorId: 4 }]]) {
    await t.test(`host binding ${label}`, async () => {
      let calls = 0;
      const out = await run(prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; }), input, context({ bindings }));
      assert.equal(out.classification, 'FAILED');
      assert.equal(out.error.code, 'HOST_BINDING_INVALID');
      assert.equal(calls, 0);
    });
  }
});

test('validates grants and authorizes invocation then capability before dispatch', async () => {
  let calls = 0;
  const handle = prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; });
  const denied = await run(handle, input, context({ grants: {} }));
  assert.equal(denied.error.code, 'AUTHORITY_DENIED');
  assert.equal(calls, 0);
  const requests = [];
  const out = await run(handle, input, context({ authorize: async request => { requests.push(request); return true; } }));
  assert.equal(out.classification, 'COMPLETED');
  assert.deepEqual(requests, [
    { entrypoint: 'ClassifyTask' },
    { entrypoint: 'ClassifyTask', capability: 'tasks.get', version: '1' },
  ]);
});

test('classifies authorization denial and callback failures without dispatch or leaking details', async t => {
  for (const [label, authorize, code] of [
    ['false', async () => false, 'AUTHORITY_DENIED'],
    ['throws', async () => { throw new Error('private authorization secret'); }, 'AUTHORITY_CHECK_FAILED'],
    ['rejects', async () => Promise.reject(new Error('private authorization secret')), 'AUTHORITY_CHECK_FAILED'],
    ['non-boolean', async () => 'yes', 'AUTHORITY_CHECK_FAILED'],
  ]) {
    await t.test(label, async () => {
      let calls = 0;
      const out = await run(prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; }), input, context({ authorize }));
      assert.equal(out.error.code, code);
      assert.equal(calls, 0);
      assert.doesNotMatch(JSON.stringify(out), /private authorization secret/);
    });
  }
});

test('returns only declared adapter outcomes and rejects malformed or undeclared responses', async t => {
  const cases = [
    ['declared business rejection', async () => ({ kind: 'BUSINESS_REJECTION', code: 'TASK_NOT_FOUND' }), 'BUSINESS_REJECTION', 'TASK_NOT_FOUND'],
    ['transient failure', async () => ({ kind: 'TRANSIENT_FAILURE' }), 'FAILED', 'ADAPTER_TRANSIENT'],
    ['throw', async () => { throw new Error('private adapter stack'); }, 'FAILED', 'ADAPTER_EXCEPTION'],
    ['extra field', async () => ({ kind: 'SUCCESS', value: goodTask, secret: 'x' }), 'FAILED', 'ADAPTER_INVALID'],
    ['invalid enum', async () => ({ kind: 'SUCCESS', value: { id: 'task1', status: 'UNKNOWN' } }), 'FAILED', 'ADAPTER_INVALID'],
    ['oversized value', async () => ({ kind: 'SUCCESS', value: { id: 'x'.repeat(9000), status: 'COMPLETED' } }), 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED'],
    ['missing result field', async () => ({ kind: 'SUCCESS', value: { id: 'task1' } }), 'FAILED', 'ADAPTER_INVALID'],
    ['null response', async () => null, 'FAILED', 'ADAPTER_INVALID'],
    ['undeclared business code', async () => ({ kind: 'BUSINESS_REJECTION', code: 'SECRET' }), 'FAILED', 'ADAPTER_INVALID'],
  ];
  for (const [label, adapter, classification, code] of cases) await t.test(label, async () => {
    const out = await run(prepared(adapter));
    assert.equal(out.classification, classification);
    if (classification === 'COMPLETED') assert.equal(Object.hasOwn(out, 'error'), false);
    else {
      assert.equal(out.error.code, code);
      assert.equal(Object.hasOwn(out, 'result'), false);
    }
    assert.doesNotMatch(JSON.stringify(out), /private adapter stack|secret/);
    assert.equal(Object.isFrozen(out), true);
    assert.equal(Object.isFrozen(out.trace), true);
  });
});

test('emits an ordered redacted success trace', async () => {
  const out = await run(prepared(async () => ({ kind: 'SUCCESS', value: goodTask })));
  assert.deepEqual(out.trace.map(({ event, step, capability }) => [event, step, capability]), [
    ['INVOCATION_START', undefined, undefined], ['STEP_START', 'source', undefined],
    ['CALL_DISPATCHED', 'source', 'tasks.get'], ['CALL_SUCCEEDED', 'source', 'tasks.get'],
    ['STEP_START', 'completed', undefined], ['VALUE_BOUND', 'completed', undefined],
    ['RETURNED', undefined, undefined], ['TERMINATED', undefined, undefined],
  ]);
  assert.doesNotMatch(JSON.stringify(out.trace), /task1|user1|COMPLETED/);
});

test('rejects an invalid invocation context and terminates when the trace budget is exhausted', async () => {
  const handle = prepared(async () => ({ kind: 'SUCCESS', value: goodTask }), { limits: { traceEvents: 2 } });
  const invalid = await run(handle, input, { ...context(), deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 }, unexpected: true });
  assert.equal(invalid.error.code, 'HOST_CONTEXT_INVALID');
  let calls = 0;
  const limited = await run(prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; }, { limits: { traceEvents: 2 } }));
  assert.equal(limited.classification, 'LIMIT_EXCEEDED');
  assert.equal(limited.error.code, 'BUDGET_EXCEEDED');
  assert.equal(calls, 0);
});

test('validates monotonic clock values and expiry at the host boundary', async t => {
  for (const [label, readings] of [
    ['expiry equality', [1000]], ['negative', [-1]], ['fractional', [0.5]], ['NaN', [NaN]], ['unsafe integer', [Number.MAX_SAFE_INTEGER + 1]], ['regression', [1, 0]],
  ]) await t.test(label, async () => {
    let index = 0;
    const out = await run(prepared(async () => ({ kind: 'SUCCESS', value: goodTask })), input, context({ deadline: { clock: { nowMs: () => readings[Math.min(index++, readings.length - 1)] }, expiresAtMs: 1000 } }));
    assert.equal(out.classification, label === 'expiry equality' ? 'LIMIT_EXCEEDED' : 'FAILED');
    assert.equal(out.error.code, label === 'expiry equality' ? 'BUDGET_EXCEEDED' : 'HOST_CONTEXT_INVALID');
  });
  await t.test('clock throws', async () => {
    const out = await run(prepared(async () => ({ kind: 'SUCCESS', value: goodTask })), input, context({ deadline: { clock: { nowMs: () => { throw new Error('clock secret'); } }, expiresAtMs: 1000 } }));
    assert.equal(out.error.code, 'HOST_CONTEXT_INVALID');
  });
});

test('discards an adapter result when the host clock reaches expiry during the adapter callback', async () => {
  let now = 0;
  let release;
  const adapter = () => new Promise(resolve => { release = () => { now = 1000; resolve({ kind: 'SUCCESS', value: goodTask }); }; });
  const pending = run(prepared(adapter), input, context({ deadline: { clock: { nowMs: () => now }, expiresAtMs: 1000 } }));
  await new Promise(resolve => setImmediate(resolve));
  release();
  const out = await pending;
  assert.equal(out.classification, 'LIMIT_EXCEEDED');
  assert.equal(out.error.code, 'BUDGET_EXCEEDED');
  assert.equal(Object.hasOwn(out, 'result'), false);
});

test('checks expiry after authorization and before dispatch', async () => {
  let now = 0, calls = 0, checks = 0;
  const out = await run(prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; }), input, context({
    deadline: { clock: { nowMs: () => now }, expiresAtMs: 1000 },
    authorize: async () => { if (++checks === 2) now = 1000; return true; },
  }));
  assert.equal(out.classification, 'LIMIT_EXCEEDED');
  assert.equal(out.error.code, 'BUDGET_EXCEEDED');
  assert.equal(calls, 0);
  assert.equal(out.trace.some(event => event.event === 'CALL_DISPATCHED'), false);
});

test('native async budget expires even if an accepted logical clock does not advance', async t => {
  for (const boundary of ['authorization', 'adapter']) await t.test(boundary, async () => {
    let release;
    const waiting = () => new Promise(resolve => { release = resolve; });
    const handle = prepared(boundary === 'adapter' ? waiting : async () => ({ kind: 'SUCCESS', value: goodTask }));
    const host = context({ deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 20 }, ...(boundary === 'authorization' ? { authorize: waiting } : {}) });
    const outcome = await run(handle, input, host);
    assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
    assert.equal(outcome.error.code, 'BUDGET_EXCEEDED');
    release(boundary === 'authorization' ? false : { kind: 'SUCCESS', value: goodTask });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
    assert.equal(Object.hasOwn(outcome, 'result'), false);
  });
});

test('reserves call-success and termination trace capacity before adapter dispatch', async () => {
  let calls = 0;
  const outcome = await run(prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; }, { limits: { traceEvents: 4 } }));
  assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
  assert.equal(calls, 0);
  assert.equal(outcome.trace.some(event => event.event === 'CALL_DISPATCHED'), false);
  assert.equal(outcome.trace.at(-1).event, 'TERMINATED');
});

test('snapshots inputs, protected bindings and grants across asynchronous authorization', async () => {
  let release, seen;
  const value = { taskId: 'task1' };
  const host = context({ authorize: () => new Promise(resolve => { release = resolve; }) });
  const handle = prepared(async (agent, protectedArgs) => { seen = { ...agent, ...protectedArgs }; return { kind: 'SUCCESS', value: goodTask }; });
  let checks = 0;
  host.authorize = () => ++checks === 1 ? new Promise(resolve => { release = resolve; }) : true;
  const pending = run(handle, value, host);
  value.taskId = 'changed'; host.bindings.actorId = 'changed'; host.grants['tasks.get'] = 2;
  release(true);
  const outcome = await pending;
  assert.equal(outcome.classification, 'COMPLETED');
  assert.deepEqual(seen, { taskId: 'task1', actorId: 'user1' });
});

test('rejects zero, widened and unknown fresh grant counts as invalid host context', async () => {
  for (const grants of [{ 'tasks.get': 0 }, { 'tasks.get': 2 }, { unknown: 1 }]) {
    let calls = 0;
    const outcome = await run(prepared(async () => { calls++; return { kind: 'SUCCESS', value: goodTask }; }), input, context({ grants }));
    assert.equal(outcome.error.code, 'HOST_CONTEXT_INVALID');
    assert.equal(calls, 0);
  }
});

test('long native budgets use finite timer chunks without 32-bit overflow', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  let release;
  try {
    const pending = run(prepared(), input, context({ deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 2147483648 }, authorize: () => new Promise(resolve => { release = resolve; }) }));
    t.mock.timers.tick(2147483647);
    t.mock.timers.tick(1);
    const outcome = await pending;
    assert.equal(outcome.error.code, 'BUDGET_EXCEEDED');
    release(false);
  } finally { t.mock.timers.reset(); }
});

test('native timeout checks expiry and clock validity while authorization remains pending', async t => {
  for (const [label, next, code] of [['expiry', 20, 'BUDGET_EXCEEDED'], ['regression', -1, 'HOST_CONTEXT_INVALID']]) await t.test(label, async () => {
    let now = 0;
    const pending = run(prepared(), input, context({ deadline: { clock: { nowMs: () => now }, expiresAtMs: 20 }, authorize: () => new Promise(() => {}) }));
    now = next;
    const outcome = await pending;
    assert.equal(outcome.error.code, code);
    assert.equal(Object.hasOwn(outcome, 'result'), false);
  });
});

test('synchronous callback exceptions still recheck the consumed deadline', async t => {
  for (const boundary of ['authorization', 'adapter']) await t.test(boundary, async () => {
    let now = 0;
    const failing = () => { now = 1000; throw new Error('private callback error'); };
    const handle = prepared(boundary === 'adapter' ? failing : undefined);
    const outcome = await run(handle, input, context({ deadline: { clock: { nowMs: () => now }, expiresAtMs: 1000 }, ...(boundary === 'authorization' ? { authorize: failing } : {}) }));
    assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
    assert.equal(outcome.error.code, 'BUDGET_EXCEEDED');
    assert.doesNotMatch(JSON.stringify(outcome), /private callback error/);
  });
});
