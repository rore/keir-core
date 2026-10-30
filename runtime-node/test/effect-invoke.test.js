import test from 'node:test';
import assert from 'node:assert/strict';
import { invoke } from '../src/index.js';
import { effectInput, effectPrepared, effectAdapters, effectContext, effectAdmission, nativeKey } from './effect-helpers.js';

const cap = 'tasks.createFollowUp';
const confirmed = { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
const rejected = { kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' };
const transient = { kind: 'TRANSIENT_FAILURE', effect: 'NONE' };
const receipt = state => [{ step: 'created', capability: cap, state }];
const run = (handle, context = effectContext(), input = effectInput) => {
  assert.equal(handle.code, undefined, JSON.stringify(handle));
  return invoke(handle, input, context);
};
const error = (outcome, classification, code, state, path = '/steps/2') => {
  assert.equal(outcome.classification, classification);
  assert.deepEqual(outcome.error, { code, path });
  assert.equal(Object.hasOwn(outcome, 'result'), false);
  assert.deepEqual(outcome.effects, state ? receipt(state) : []);
  assert.equal(outcome.trace.at(-1).event, 'TERMINATED');
};
const successTrace = [
  { sequence: 0, event: 'INVOCATION_START' },
  { sequence: 1, event: 'STEP_START', step: 'source' },
  { sequence: 2, event: 'CALL_DISPATCHED', step: 'source', capability: 'tasks.get' },
  { sequence: 3, event: 'CALL_SUCCEEDED', step: 'source', capability: 'tasks.get' },
  { sequence: 4, event: 'STEP_START', step: 'eligible' },
  { sequence: 5, event: 'VALUE_BOUND', step: 'eligible' },
  { sequence: 6, event: 'STEP_START', step: 'created' },
  { sequence: 7, event: 'GUARD_EVALUATED', step: 'created', taken: true },
  { sequence: 8, event: 'EFFECT_INTENT', step: 'created', capability: cap },
  { sequence: 9, event: 'CALL_DISPATCHED', step: 'created', capability: cap },
  { sequence: 10, event: 'EFFECT_RECORDED', step: 'created', capability: cap, state: 'CONFIRMED' },
  { sequence: 11, event: 'CALL_SUCCEEDED', step: 'created', capability: cap },
  { sequence: 12, event: 'RETURNED' },
  { sequence: 13, event: 'TERMINATED' },
];

test('confirmed effect has exact trace, separated arguments and untouched opaque key at both adapters', async () => {
  const calls = [], requests = [];
  const context = effectContext({ authorize: async request => { requests.push(request); return true; } });
  const handle = effectPrepared(effectAdapters(
    async (args, protectedArgs, remainingMs, hostContext) => {
      calls.push({ args, protectedArgs, remainingMs, hostContext });
      return { kind: 'SUCCESS', value: { id: args.taskId, projectId: 'project1', status: 'COMPLETED' } };
    }, async (args, protectedArgs, remainingMs, hostContext) => {
      calls.push({ args, protectedArgs, remainingMs, hostContext }); return confirmed;
    },
  ));
  const outcome = await run(handle, context);
  assert.deepEqual(outcome, { classification: 'COMPLETED', result: { status: 'CREATED' }, effects: receipt('CONFIRMED'), trace: successTrace });
  assert.deepEqual(calls.map(call => ({ args: { ...call.args }, protectedArgs: { ...call.protectedArgs }, remainingMs: call.remainingMs })), [
    { args: { taskId: 'task1' }, protectedArgs: { actorId: 'user1' }, remainingMs: 10000 },
    { args: { projectId: 'project1', sourceTaskId: 'task1', title: 'Follow up' }, protectedArgs: { actorId: 'user1' }, remainingMs: 10000 },
  ]);
  for (const call of calls) {
    assert.strictEqual(call.hostContext.requestKey, context.requestKey);
    assert.deepEqual(call.hostContext.event, { id: 'event1', source: 'task1' });
    assert.deepEqual(Object.keys(call.hostContext).sort(), ['event', 'requestKey']);
    assert.ok(Object.isFrozen(call.hostContext) && Object.isFrozen(call.hostContext.event));
  }
  assert.deepEqual(requests, [
    { entrypoint: 'TaskCompleted' }, { entrypoint: 'TaskCompleted', capability: 'tasks.get', version: '1' },
    { entrypoint: 'TaskCompleted', capability: cap, version: '1' },
  ]);
  assert.ok(Object.isFrozen(outcome.effects) && Object.isFrozen(outcome.effects[0]));
  assert.doesNotMatch(JSON.stringify(outcome.trace), /task1|user1|event1|Private/);
});

test('false guards exclude unavailable results and skip write grants, authorization and stubs', async () => {
  for (const value of [{ status: 'OPEN', projectId: 'project1' }, { status: 'COMPLETED', projectId: 'other' }]) {
    let writes = 0; const requests = [];
    const handle = effectPrepared(effectAdapters(async () => ({ kind: 'SUCCESS', value: { id: 'task1', ...value } }),
      async () => { writes++; return confirmed; }));
    const outcome = await run(handle, effectContext({ effectGrant: 0, grants: { 'tasks.get': 1 }, authorize: async request => { requests.push(request); return true; } }));
    assert.deepEqual(outcome, {
      classification: 'COMPLETED', result: { status: 'NO_OP' }, effects: [],
      trace: [...successTrace.slice(0, 7),
        { sequence: 7, event: 'GUARD_EVALUATED', step: 'created', taken: false },
        { sequence: 8, event: 'STEP_SKIPPED', step: 'created' },
        { sequence: 9, event: 'RETURNED' }, { sequence: 10, event: 'TERMINATED' },
      ],
    });
    assert.equal(writes, 0); assert.equal(requests.length, 2);
  }
});

test('handled and unhandled read rejection stop before the effect', async () => {
  for (const code of ['TASK_NOT_FOUND', 'ACCESS_DENIED']) {
    let writes = 0;
    const out = await run(effectPrepared(effectAdapters(async () => ({ kind: 'BUSINESS_REJECTION', code }), async () => { writes++; return confirmed; })));
    assert.equal(out.classification, 'BUSINESS_REJECTION');
    assert.deepEqual(out.error, { code, path: '/steps/0' }); assert.deepEqual(out.effects, []);
    if (code === 'TASK_NOT_FOUND') {
      assert.deepEqual(out.result, { status: 'SOURCE_MISSING' });
      assert.deepEqual(out.trace, [...successTrace.slice(0, 3), { sequence: 3, event: 'RETURNED' }, { sequence: 4, event: 'TERMINATED', code }]);
    } else assert.equal(Object.hasOwn(out, 'result'), false);
    assert.equal(writes, 0);
  }
});

test('read transient, exception and malformed/effectful outcomes cannot start a write', async () => {
  for (const [read, code] of [
    [async () => ({ kind: 'TRANSIENT_FAILURE' }), 'ADAPTER_TRANSIENT'],
    [async () => { throw new Error('private read detail'); }, 'ADAPTER_EXCEPTION'],
    [async () => null, 'ADAPTER_INVALID'],
    [async () => ({ kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'INVALID' } }), 'ADAPTER_INVALID'],
    [async () => ({ kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'COMPLETED' }, effect: 'CONFIRMED' }), 'ADAPTER_INVALID'],
    [async () => ({ kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' }), 'ADAPTER_INVALID'],
  ]) {
    let writes = 0;
    const out = await run(effectPrepared(effectAdapters(read, async () => { writes++; return confirmed; })));
    error(out, 'FAILED', code, undefined, '/steps/0');
    assert.equal(writes, 0); assert.equal(out.trace.some(event => event.event === 'EFFECT_INTENT'), false);
    assert.doesNotMatch(JSON.stringify(out), /private read detail/);
  }
});

test('definite write rejection keeps provenance, typed handler and NONE evidence', async () => {
  const out = await run(effectPrepared(effectAdapters(undefined, async () => rejected)));
  assert.deepEqual(out, {
    classification: 'BUSINESS_REJECTION', result: { status: 'REJECTED' },
    error: { code: 'CREATE_REJECTED', path: '/steps/2' }, effects: receipt('NONE'),
    trace: [...successTrace.slice(0, 10),
      { sequence: 10, event: 'EFFECT_RECORDED', step: 'created', capability: cap, state: 'NONE' },
      { sequence: 11, event: 'RETURNED' }, { sequence: 12, event: 'TERMINATED', code: 'CREATE_REJECTED' },
    ],
  });
});

test('effect and capability authority are checked before write authorization or intent', async () => {
  for (const restriction of [{ effectGrant: 0 }, { grants: { 'tasks.get': 1 } }]) {
    let writes = 0; const requests = [];
    const out = await run(effectPrepared(effectAdapters(undefined, async () => { writes++; return confirmed; })),
      effectContext({ ...restriction, authorize: async request => { requests.push(request); return true; } }));
    error(out, 'FAILED', 'AUTHORITY_DENIED');
    assert.equal(writes, 0); assert.equal(requests.length, 2);
    assert.equal(out.trace.some(event => event.event === 'EFFECT_INTENT'), false);
  }
});

test('native key/event/grant validation fails before any authorization or adapter work', async () => {
  for (const overrides of [
    { requestKey: null }, { requestKey: 'uuid' }, { requestKey: Object.freeze({}) },
    { event: { id: 2, source: 'task1' } }, { event: { id: 'event1' } },
    { event: { id: 'event1', source: 'task1', extra: true } },
    { effectGrant: undefined }, { effectGrant: 2 }, { effectGrant: -1 }, { effectGrant: 0.5 },
  ]) {
    let callbacks = 0;
    const handle = effectPrepared(effectAdapters(async () => { callbacks++; return {}; }));
    const out = await run(handle, effectContext({ ...overrides, authorize: async () => { callbacks++; return true; } }));
    error(out, 'FAILED', 'HOST_CONTEXT_INVALID', undefined, ''); assert.equal(callbacks, 0);
  }
  for (const validate of [() => false, () => 'true', () => { throw new Error('private validation secret'); }]) {
    const handle = effectPrepared(undefined, effectAdmission({ keyBinding: { type: 'sample.RequestUuid/1', validate } }));
    const out = await run(handle); error(out, 'FAILED', 'HOST_CONTEXT_INVALID', undefined, '');
    assert.doesNotMatch(JSON.stringify(out), /private validation secret/);
  }
});

test('closed write response evidence distinguishes unknown protocol failures from confirmed invalid values', async t => {
  for (const [label, response, classification, code, state, path] of [
    ['transient', transient, 'FAILED', 'ADAPTER_TRANSIENT', 'NONE'],
    ['unknown', { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' }, 'UNCERTAIN', 'UNKNOWN_COMMIT', 'UNKNOWN'],
    ['null', null, 'UNCERTAIN', 'ADAPTER_INVALID', 'UNKNOWN'],
    ['missing evidence', { kind: 'SUCCESS', value: { id: 'task2' } }, 'UNCERTAIN', 'ADAPTER_INVALID', 'UNKNOWN'],
    ['extra outer field', { ...confirmed, extra: 'private' }, 'UNCERTAIN', 'ADAPTER_INVALID', 'UNKNOWN'],
    ['incompatible evidence', { ...confirmed, effect: 'NONE' }, 'UNCERTAIN', 'ADAPTER_INVALID', 'UNKNOWN'],
    ['invalid confirmed value', { ...confirmed, value: null }, 'FAILED', 'ADAPTER_INVALID', 'CONFIRMED'],
    ['extra confirmed value', { ...confirmed, value: { id: 'task2', extra: true } }, 'FAILED', 'ADAPTER_INVALID', 'CONFIRMED'],
    ['oversized confirmed value', { ...confirmed, value: { id: 'x'.repeat(257) } }, 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', 'CONFIRMED', '/steps/2/id'],
    ['undeclared rejection', { ...rejected, code: 'UNDECLARED' }, 'FAILED', 'ADAPTER_INVALID', 'NONE'],
  ]) await t.test(label, async () => {
    const out = await run(effectPrepared(effectAdapters(undefined, async () => response)));
    error(out, classification, code, state, path);
    assert.equal(out.trace.filter(event => event.event === 'EFFECT_RECORDED').length, 1);
    assert.equal(out.trace.some(event => event.event === 'CALL_SUCCEEDED' && event.step === 'created'), false);
  });
});

test('response-winning CONFIRMED and both NONE outcomes settle before expiry/regression/invalid clock', async t => {
  for (const response of [confirmed, rejected, transient]) for (const [label, next, classification, code, path] of [
    ['expiry', 10000, 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', '/steps/2'],
    ['regression', 9, 'FAILED', 'HOST_CONTEXT_INVALID', ''], ['invalid', NaN, 'FAILED', 'HOST_CONTEXT_INVALID', ''],
  ]) await t.test(response.kind + ' ' + label, async () => {
    let now = 10;
    const handle = effectPrepared(effectAdapters(undefined, async () => { now = next; return response; }));
    const out = await run(handle, effectContext({ deadline: { clock: { nowMs: () => now }, expiresAtMs: 10000 } }));
    error(out, classification, code, response.effect, path);
    assert.equal(out.trace.at(-2).event, 'EFFECT_RECORDED');
    assert.equal(out.trace.some(event => event.event === 'RETURNED'), false);
  });
});

test('winning write envelope is snapshotted before later promise jobs can change its tags', async () => {
  for (const initial of [rejected, { kind: 'INVALID', effect: 'CONFIRMED', value: { id: 'task2' } }]) {
    const response = structuredClone(initial);
    const handle = effectPrepared(effectAdapters(undefined, () => {
      queueMicrotask(() => queueMicrotask(() => {
        for (const key of Object.keys(response)) delete response[key];
        Object.assign(response, confirmed);
      }));
      return response;
    }));
    const out = await run(handle);
    if (initial === rejected) {
      assert.equal(out.classification, 'BUSINESS_REJECTION'); assert.deepEqual(out.result, { status: 'REJECTED' });
      assert.deepEqual(out.effects, receipt('NONE'));
    } else error(out, 'UNCERTAIN', 'ADAPTER_INVALID', 'UNKNOWN');
  }
});

test('timeout-winning stationary clock discards late CONFIRMED and both NONE responses', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  try {
    for (const response of [confirmed, rejected, transient]) {
      let release, started;
      const dispatched = new Promise(resolve => { started = resolve; });
      const handle = effectPrepared(effectAdapters(undefined, () => new Promise(resolve => { release = resolve; started(); })));
      const pending = run(handle);
      await dispatched; t.mock.timers.tick(10000);
      const out = await pending; error(out, 'UNCERTAIN', 'BUDGET_EXCEEDED', 'UNKNOWN');
      const before = JSON.stringify(out);
      release(response); await new Promise(resolve => setImmediate(resolve));
      assert.equal(JSON.stringify(out), before);
      assert.equal(out.trace.filter(event => event.event === 'EFFECT_RECORDED').length, 1);
    }
  } finally { t.mock.timers.reset(); }
});

test('write exceptions are redacted and still recheck deadline before choosing error code', async () => {
  for (const [next, code, path] of [[0, 'ADAPTER_EXCEPTION', '/steps/2'], [10000, 'BUDGET_EXCEEDED', '/steps/2'], [-1, 'HOST_CONTEXT_INVALID', '']]) {
    let now = 0;
    const handle = effectPrepared(effectAdapters(undefined, () => { now = next; throw new Error('private adapter secret'); }));
    const out = await run(handle, effectContext({ deadline: { clock: { nowMs: () => now }, expiresAtMs: 10000 } }));
    error(out, 'UNCERTAIN', code, 'UNKNOWN', path); assert.doesNotMatch(JSON.stringify(out), /private adapter secret/);
  }
});

test('five-slot write reservation prevents an unrecorded dispatch and preserves later confirmed failure', async () => {
  for (const [traceEvents, classification, writes] of [[12, 'LIMIT_EXCEEDED', 0], [13, 'LIMIT_EXCEEDED', 1], [14, 'COMPLETED', 1]]) {
    let calls = 0;
    const handle = effectPrepared(effectAdapters(undefined, async () => { calls++; return confirmed; }), effectAdmission({ limits: { traceEvents } }));
    const out = await run(handle); assert.equal(out.classification, classification); assert.equal(calls, writes);
    assert.deepEqual(out.effects, writes ? receipt('CONFIRMED') : []);
    if (traceEvents === 13) error(out, 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', 'CONFIRMED', '/return');
    assert.ok(out.trace.length <= traceEvents);
  }
});

test('timer setup failure after write authorization has no attempted effect', async t => {
  const original = globalThis.setTimeout;
  let writeAuthorized = false, writes = 0;
  t.mock.method(globalThis, 'setTimeout', (...args) => {
    if (writeAuthorized) throw new Error('private timer setup failure');
    return original(...args);
  });
  try {
    const handle = effectPrepared(effectAdapters(undefined, async () => { writes++; return confirmed; }));
    const out = await run(handle, effectContext({ authorize: async request => {
      if (request.capability === cap) writeAuthorized = true;
      return true;
    } }));
    error(out, 'FAILED', 'HOST_CONTEXT_INVALID', undefined, '');
    assert.equal(writes, 0); assert.equal(out.trace.some(event => event.event === 'EFFECT_INTENT'), false);
    assert.doesNotMatch(JSON.stringify(out), /private timer/);
  } finally { t.mock.restoreAll(); }
});

test('protected event nominal scalars preserve inclusive UTF-8 bounds', async () => {
  const exact = 'é'.repeat(128);
  const handle = effectPrepared();
  assert.equal((await run(handle, effectContext({ event: { id: exact, source: 'task1' } }))).classification, 'COMPLETED');
  error(await run(handle, effectContext({ event: { id: exact + 'x', source: 'task1' } })), 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', undefined, '');
});

test('logical deadline sweep preserves business cause on handler failure and confirmation on return failure', async () => {
  for (const response of [confirmed, rejected]) {
    const handle = effectPrepared(effectAdapters(undefined, async () => response));
    let observed = false;
    for (let ticks = 1; ticks <= 100; ticks++) {
      let now = 0;
      const out = await run(handle, effectContext({ deadline: { clock: { nowMs: () => (now += 1000) }, expiresAtMs: ticks * 1000 } }));
      if (response === confirmed && out.error?.path === '/return') {
        error(out, 'LIMIT_EXCEEDED', 'BUDGET_EXCEEDED', 'CONFIRMED', '/return'); observed = true;
      }
      if (response === rejected && out.error?.cause) {
        assert.deepEqual(out.error.cause, { code: 'CREATE_REJECTED', path: '/steps/2' });
        assert.match(out.error.path, /^\/steps\/2\/onError\/CREATE_REJECTED/);
        assert.deepEqual(out.effects, receipt('NONE')); assert.equal(Object.hasOwn(out, 'result'), false); observed = true;
      }
      if (out.classification === (response === confirmed ? 'COMPLETED' : 'BUSINESS_REJECTION') && out.result) break;
    }
    assert.equal(observed, true, 'Sweep must reach the later-stage deadline, not only admission/dispatch expiry');
  }
});

test('Prepared snapshots bindings and validator references, and isolated invocations keep original keys/events', async () => {
  const calls = []; const selected = effectAdmission(); const registrations = effectAdapters(
    async (args, host, left, context) => { calls.push({ args, host, context }); return { kind: 'SUCCESS', value: { id: args.taskId, projectId: 'project1', status: 'COMPLETED' } }; },
    async (args, host, left, context) => { calls.push({ args, host, context }); return confirmed; },
  );
  const handle = effectPrepared(registrations, selected);
  selected.keyBinding.validate = () => false; selected.keyBinding.type = 'changed';
  for (const registration of Object.values(registrations)) { registration.keyType = 'changed'; registration.invoke = () => { throw new Error('changed'); }; }
  const first = effectContext(); const second = effectContext({ requestKey: nativeKey(), event: { id: 'event2', source: 'task2' }, bindings: { actorId: 'user2' } });
  const outcomes = await Promise.all([run(handle, first), run(handle, second, { ...effectInput, taskId: 'task2' })]);
  assert.ok(outcomes.every(out => out.classification === 'COMPLETED'));
  assert.equal(calls.length, 4);
  for (const call of calls) {
    const context = call.host.actorId === 'user1' ? first : second;
    assert.strictEqual(call.context.requestKey, context.requestKey); assert.deepEqual(call.context.event, context.event);
    assert.equal(call.args.taskId ?? call.args.sourceTaskId, context.event.source);
  }
});

test('input/event/key/grants snapshot precedes pending invocation authorization', async () => {
  let release; const captured = [];
  const pendingCheck = new Promise(resolve => { release = resolve; }); let checks = 0;
  const context = effectContext({ authorize: async () => ++checks === 1 ? pendingCheck : true });
  const originalKey = context.requestKey; const input = { ...effectInput };
  const handle = effectPrepared(effectAdapters(
    async (args, host, left, native) => { captured.push({ args, host, native }); return { kind: 'SUCCESS', value: { id: args.taskId, projectId: 'project1', status: 'COMPLETED' } }; },
    async (args, host, left, native) => { captured.push({ args, host, native }); return confirmed; },
  ));
  const pending = run(handle, context, input);
  input.taskId = 'changed'; input.title = 'changed'; context.bindings.actorId = 'changed'; context.event.source = 'changed';
  context.requestKey = nativeKey(); context.grants[cap] = 2; context.effectGrant = 0;
  release(true); const out = await pending; assert.equal(out.classification, 'COMPLETED');
  assert.equal(captured.length, 2); assert.equal(captured[1].args.title, 'Follow up');
  for (const call of captured) {
    assert.strictEqual(call.native.requestKey, originalKey); assert.equal(call.native.event.source, 'task1'); assert.equal(call.host.actorId, 'user1');
  }
});
