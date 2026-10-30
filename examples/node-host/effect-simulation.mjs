import assert from 'node:assert/strict';
import { consumeEffect } from './effect-consumer.mjs';

const get = 'tasks.get', write = 'tasks.createFollowUp';
const fixtureIds = new Set(['normal', 'no-effect-grant', 'no-write-grant', 'write-denied', 'invocation-denied', 'invalid-key', 'wrong-origin', 'read-throws', 'lost-response', 'response-expired', 'response-bad-clock', 'trace-after-confirmed', 'simulated-duplicate', 'simulated-payload-mismatch']);
const own = (value, key) => Object.hasOwn(value, key);
function shape(value, required, optional = []) {
  assert.ok(value && typeof value === 'object' && !Array.isArray(value), 'Expected a scenario record');
  assert.ok(required.every(key => own(value, key)) && Object.keys(value).every(key => required.includes(key) || optional.includes(key)), 'Closed scenario fields');
}
function scalar(value) { assert.ok(typeof value === 'string' && value.isWellFormed() && Buffer.byteLength(value) <= 256, 'Bounded scenario scalar'); }
function fields(value, names) { shape(value, names); for (const name of names) scalar(value[name]); }
function json(value) {
  const text = JSON.stringify(value, (_key, item) => {
    assert.ok(item !== undefined && !['function', 'symbol', 'bigint'].includes(typeof item), 'Non-JSON scenario value');
    assert.ok(typeof item !== 'number' || Number.isFinite(item), 'Non-JSON number');
    return item;
  });
  assert.ok(Buffer.byteLength(text) <= 65536, 'Scenario exceeds raw byte limit');
  return JSON.parse(text);
}
function callShape(call, stub = false) {
  shape(call, ['step', 'capability', 'version', 'args', 'protectedArgs', ...(stub ? ['response'] : [])]);
  assert.match(call.step, /^[A-Za-z][A-Za-z0-9]{0,63}$/);
  assert.ok([get, write].includes(call.capability), 'Unknown simulation capability');
  assert.equal(call.version, '1');
  fields(call.args, call.capability === get ? ['taskId'] : ['sourceTaskId', 'projectId', 'title']);
  fields(call.protectedArgs, ['actorId']);
  if (!stub) return;
  const response = call.response, isWrite = call.capability === write;
  assert.ok(response && typeof response === 'object', 'Missing simulation response');
  if (response.kind === 'SUCCESS') {
    shape(response, ['kind', 'value', ...(isWrite ? ['effect'] : [])]);
    fields(response.value, isWrite ? ['id'] : ['id', 'projectId', 'status']);
    if (isWrite) assert.equal(response.effect, 'CONFIRMED');
    else assert.ok(['OPEN', 'COMPLETED'].includes(response.value.status));
  } else if (response.kind === 'BUSINESS_REJECTION') {
    shape(response, ['kind', 'code', ...(isWrite ? ['effect'] : [])]);
    assert.ok((isWrite ? ['CREATE_REJECTED', 'ACCESS_DENIED'] : ['TASK_NOT_FOUND', 'ACCESS_DENIED']).includes(response.code));
    if (isWrite) assert.equal(response.effect, 'NONE');
  } else if (response.kind === 'TRANSIENT_FAILURE') {
    shape(response, ['kind', ...(isWrite ? ['effect'] : [])]);
    if (isWrite) assert.equal(response.effect, 'NONE');
  } else {
    shape(response, ['kind', 'effect']); assert.ok(isWrite); assert.equal(response.kind, 'UNKNOWN_COMMIT'); assert.equal(response.effect, 'UNKNOWN');
  }
}

export function validateScenario(row) {
  json(row); shape(row, ['format', 'id', 'given', 'stubs', 'expect']);
  assert.equal(row.format, 'keir-scenario/effect-1'); assert.match(row.id, /^[a-z0-9][a-z0-9_-]*$/);
  shape(row.given, ['input', 'fixture']); fields(row.given.input, ['taskId', 'projectId', 'title']);
  assert.ok(fixtureIds.has(row.given.fixture), 'Unknown trusted fixture ID');
  assert.ok(Array.isArray(row.stubs) && row.stubs.length <= 4, 'At most four simulation stubs');
  row.stubs.forEach(stub => callShape(stub, true));
  shape(row.expect, ['outcome', 'calls', 'consumption']); assert.ok(Array.isArray(row.expect.calls));
  row.expect.calls.forEach(call => callShape(call));
  assert.deepEqual(consumeEffect(row.expect.outcome), row.expect.consumption, 'Invalid expected consumer receipt');
  assert.equal(row.expect.consumption.status, 'REPORTED', 'Expected runtime outcome must be valid');
}

// This host brand never enters generic scalar conversion, serialization or a trace.
const keyValues = new WeakMap();
function nativeKey() {
  const fail = () => { throw new Error('Opaque native key conversion'); };
  const key = Object.freeze(Object.defineProperties({}, {
    toString: { value: fail }, toJSON: { value: fail }, [Symbol.toPrimitive]: { value: fail },
    privateValue: { enumerable: true, get: fail },
  }));
  keyValues.set(key, '00000000-0000-4000-8000-000000000001');
  return key;
}

export function reconcileCalls(captures, trace, stubs, violations) {
  const dispatches = trace.filter(event => event.event === 'CALL_DISPATCHED');
  if (captures.length !== dispatches.length || captures.length !== stubs.length) violations.push('Dispatch/capture/stub count mismatch');
  return captures.map((call, ordinal) => {
    const event = dispatches[ordinal], stub = stubs[ordinal];
    if (!event || event.capability !== call.capability) violations.push('Dispatch capability mismatch');
    if (!stub || event?.step !== stub.step) violations.push('Dispatch step mismatch');
    return { step: event?.step, ...call };
  });
}

export async function runScenario(api, bytes, pins, row) {
  validateScenario(row);
  const fixture = row.given.fixture, key = nativeKey(), violations = [], captures = [];
  const event = { id: 'event1', source: fixture === 'wrong-origin' ? 'other-task' : 'task1' };
  let now = 0, used = 0;
  const guarded = action => {
    try { return action(); }
    catch { violations.push('Adapter arguments, native identity or fixture mismatch'); throw new Error('Simulation adapter mismatch'); }
  };
  const adapters = Object.fromEntries([get, write].map(capability => [capability, {
    version: '1', mode: 'simulation', keyType: 'sample.RequestUuid/1',
    invoke: async (args, protectedArgs, remainingMs, host) => {
      const stub = guarded(() => {
        const call = json({ capability, version: '1', args, protectedArgs }); captures.push(call);
        assert.ok(Number.isSafeInteger(remainingMs) && remainingMs > 0 && remainingMs <= 1000);
        shape(host, ['requestKey', 'event']); assert.strictEqual(host.requestKey, key); assert.deepEqual(host.event, event);
        assert.equal(protectedArgs.actorId, 'user1');
        const selected = row.stubs[used]; assert.ok(selected, 'Unexpected adapter call');
        assert.deepEqual(call, { capability: selected.capability, version: selected.version, args: selected.args, protectedArgs: selected.protectedArgs });
        if (capability === get && selected.response.kind === 'SUCCESS') assert.equal(selected.response.value.id, args.taskId);
        if (capability === write) assert.equal(event.source, args.sourceTaskId, 'Protected origin mismatch');
        if (capability === write && fixture.startsWith('simulated-')) {
          assert.equal(event.id, 'event1'); assert.equal(args.sourceTaskId, 'task1'); assert.equal(args.projectId, 'project1');
          if (fixture === 'simulated-duplicate') {
            assert.equal(args.title, 'Follow up');
            assert.deepEqual(selected.response, { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' });
          } else {
            assert.notEqual(args.title, 'Follow up');
            assert.deepEqual(selected.response, { kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' });
          }
        }
        used++; return selected;
      });
      if (fixture === 'read-throws' && capability === get || fixture === 'lost-response' && capability === write) throw new Error('Simulated unavailable response');
      if (capability === write && fixture === 'response-expired') now = 1000;
      if (capability === write && fixture === 'response-bad-clock') now = -1;
      return json(stub.response);
    },
  }]));
  const prepared = api.prepare(bytes, {
    expectedArtifactDigest: pins.artifact, expectedDcmDigest: pins.dcm, entrypoint: 'TaskCompleted',
    consumer: { id: 'sample.followup.report', version: '1', kind: 'EVENT_REPORT' }, compiler: 'keir-compiler-java/0.1.0-SNAPSHOT',
    environment: api.ENVIRONMENT, mode: 'simulation', grants: { [get]: 1, [write]: 1 }, effectGrant: 1,
    keyBinding: { type: 'sample.RequestUuid/1', validate: value => value === key && keyValues.has(value) },
    ...(fixture === 'trace-after-confirmed' ? { limits: { traceEvents: 13 } } : {}),
  }, adapters);
  assert.equal(prepared.code, undefined, 'Effect artifact admission failed');
  const outcome = json(await api.invoke(prepared, json(row.given.input), {
    bindings: { actorId: 'user1' }, grants: fixture === 'no-write-grant' ? { [get]: 1 } : { [get]: 1, [write]: 1 },
    effectGrant: fixture === 'no-effect-grant' ? 0 : 1, requestKey: fixture === 'invalid-key' ? 'not-a-native-key' : key, event,
    deadline: { clock: { nowMs: () => now }, expiresAtMs: 1000 },
    authorize: async request => {
      if (request.entrypoint !== 'TaskCompleted' || own(request, 'capability') && (!adapters[request.capability] || request.version !== '1')) return false;
      if (fixture === 'invocation-denied') return false;
      return request.capability !== write || fixture !== 'write-denied' && event.source === row.given.input.taskId;
    },
  }));
  if (used !== row.stubs.length) violations.push('Unconsumed scenario stub');
  const calls = reconcileCalls(captures, outcome.trace, row.stubs, violations);
  assert.deepEqual(violations, [], `${row.id}: simulation violations`);
  const actual = { outcome, calls, consumption: consumeEffect(outcome) };
  assert.deepEqual(actual, row.expect, `${row.id}: simulation result mismatch`);
  return { id: row.id, ...actual };
}

export async function runBatch(api, bytes, pins, scenarios) {
  assert.ok(Array.isArray(scenarios) && scenarios.length > 0 && scenarios.length <= 64, 'Bounded scenario batch');
  const ids = new Set(), results = [];
  for (const row of scenarios) {
    assert.ok(!ids.has(row.id), 'Duplicate scenario ID'); ids.add(row.id);
    results.push(await runScenario(api, bytes, pins, row));
  }
  return results;
}
