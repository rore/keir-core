import assert from 'node:assert/strict';
import { AsyncLocalStorage } from 'node:async_hooks';
import { createHarness } from './generic-capacity.mjs';
import { consumeEffect } from './effect-consumer.mjs';
import { reconcileCalls } from './effect-simulation.mjs';

const fixtures = ['normal', 'invocation-denied', 'call-denied', 'zero-effect', 'wrong-scope', 'revoked-after-read', 'invalid-key', 'wrong-origin', 'read-throws', 'lost-response', 'response-expired'];
const sharedFixtures = ['normal', 'invocation-denied', 'call-denied', 'wrong-scope', 'read-throws'];
const own = (value, name) => Object.hasOwn(value, name);
function shape(value, required) {
  assert.ok(value && typeof value === 'object' && !Array.isArray(value), 'Expected fixture record');
  assert.ok(Object.keys(value).length === required.length && required.every(name => own(value, name)), 'Closed fixture record');
}
function string(value, max = 256, nonempty = false) {
  assert.ok(typeof value === 'string' && value.isWellFormed() && Buffer.byteLength(value) <= max && (!nonempty || value.length > 0), 'Bounded fixture string');
}
const quote = value => '"' + value.replace(/["\\\u0000-\u001f]/g, char => char === '"' ? '\\"' : char === '\\' ? '\\\\' : '\\u00' + char.charCodeAt(0).toString(16).padStart(2, '0')) + '"';
function typed(value, type, dcm) {
  const definition = dcm.types[type];
  if (definition?.kind === 'record') {
    shape(value, Object.keys(definition.fields));
    for (const name of Object.keys(definition.fields)) typed(value[name], definition.fields[name], dcm);
    const canonical = '{' + Object.keys(value).sort().map(name => quote(name) + ':' + (typeof value[name] === 'boolean' ? String(value[name]) : quote(value[name]))).join(',') + '}';
    assert.ok(Buffer.byteLength(canonical) <= 8192, 'Bounded fixture record');
  } else if (type === 'Boolean') assert.equal(typeof value, 'boolean');
  else { string(value); if (definition?.kind === 'enum') assert.ok(definition.values.includes(value), 'Fixture enum'); }
}
function json(value, max = 65536) {
  const text = JSON.stringify(value, (_key, item) => {
    assert.ok(item !== undefined && !['function', 'symbol', 'bigint'].includes(typeof item), 'Data-only fixture');
    assert.ok(typeof item !== 'number' || Number.isFinite(item), 'Finite fixture number'); return item;
  });
  assert.ok(Buffer.byteLength(text) <= max, 'CAPACITY_EXCEEDED'); return JSON.parse(text);
}
function callShape(call, dcm, stub) {
  shape(call, ['step', 'capability', 'version', 'args', 'protectedArgs', ...(stub ? ['response'] : [])]);
  assert.match(call.step, /^[A-Za-z][A-Za-z0-9]{0,63}$/);
  assert.ok(own(dcm.capabilities, call.capability), 'Published fixture capability');
  const cap = dcm.capabilities[call.capability]; assert.equal(call.version, cap.version);
  for (const [field, binding] of [['args', 'agent'], ['protectedArgs', 'host']]) {
    const parameters = Object.entries(cap.parameters).filter(([, value]) => value.binding === binding);
    shape(call[field], parameters.map(([name]) => name));
    for (const [name, value] of parameters) typed(call[field][name], value.type, dcm);
  }
  if (!stub) return;
  const response = call.response, write = cap.effect === 'WRITE';
  if (response?.kind === 'SUCCESS') {
    shape(response, ['kind', 'value', ...(write ? ['effect'] : [])]); typed(response.value, cap.result, dcm);
    if (write) assert.equal(response.effect, 'CONFIRMED');
  } else if (response?.kind === 'BUSINESS_REJECTION') {
    shape(response, ['kind', 'code', ...(write ? ['effect'] : [])]); assert.ok(cap.errors.includes(response.code));
    if (write) assert.equal(response.effect, 'NONE');
  } else if (response?.kind === 'TRANSIENT_FAILURE') {
    shape(response, ['kind', ...(write ? ['effect'] : [])]); if (write) assert.equal(response.effect, 'NONE');
  } else { shape(response, ['kind', 'effect']); assert.ok(write); assert.equal(response.kind, 'UNKNOWN_COMMIT'); assert.equal(response.effect, 'UNKNOWN'); }
}

export function consumeGeneric(outcome, dcm) {
  const entry = Object.values(dcm.entrypoints)[0], effect = dcm.format.startsWith('keir-dcm/effect-');
  try {
    if (own(outcome, 'result')) typed(outcome.result, entry.output, dcm);
    if (effect) return consumeEffect(outcome);
    shape(outcome, ['classification', 'trace', outcome.classification === 'COMPLETED' ? 'result' : 'error']);
    assert.ok(['COMPLETED', 'BUSINESS_REJECTION', 'FAILED', 'LIMIT_EXCEEDED'].includes(outcome.classification));
    assert.ok(Array.isArray(outcome.trace) && outcome.trace.length >= 2 && outcome.trace.length <= 64);
    assert.equal(outcome.trace[0].event, 'INVOCATION_START'); assert.equal(outcome.trace.at(-1).event, 'TERMINATED');
    const fields = { INVOCATION_START: [], STEP_START: ['step'], VALUE_BOUND: ['step'], CALL_DISPATCHED: ['step', 'capability'], CALL_SUCCEEDED: ['step', 'capability'], RETURNED: [], TERMINATED: [] };
    outcome.trace.forEach((event, sequence) => {
      assert.ok(typeof event.event === 'string' && own(fields, event.event));
      shape(event, ['sequence', 'event', ...fields[event.event], ...(event.event === 'TERMINATED' && own(event, 'code') ? ['code'] : [])]);
      assert.equal(event.sequence, sequence);
      for (const name of ['step', 'capability', 'code']) if (own(event, name)) string(event[name], 256, true);
      if (event.event === 'INVOCATION_START') assert.equal(sequence, 0);
      if (event.event === 'TERMINATED') assert.equal(sequence, outcome.trace.length - 1);
    });
    if (outcome.classification === 'COMPLETED') {
      assert.equal(own(outcome.trace.at(-1), 'code'), false); return { status: 'DISPLAYED', result: json(outcome.result) };
    }
    shape(outcome.error, ['code', 'path']); string(outcome.error.code, 64, true); string(outcome.error.path, 1024);
    assert.equal(outcome.trace.at(-1).code, outcome.error.code); return { status: 'NOT_DISPLAYED', code: outcome.error.code };
  } catch { return { status: effect ? 'NOT_REPORTED' : 'NOT_DISPLAYED', code: 'OUTCOME_INVALID' }; }
}

export function validateGenericScenario(row, dcm) {
  json(row); const effect = dcm.format.startsWith('keir-dcm/effect-'), effect3 = dcm.format === 'keir-dcm/effect-3', entry = Object.values(dcm.entrypoints)[0];
  shape(row, ['format', 'id', 'given', 'stubs', 'expect']); assert.equal(row.format, 'keir-scenario/' + (effect3 ? 'effect-3' : effect ? 'effect-2' : 'read-2'));
  assert.match(row.id, /^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$/);
  shape(row.given, ['input', 'fixture', 'program']); typed(row.given.input, entry.input, dcm);
  assert.ok((effect ? fixtures : sharedFixtures).includes(row.given.fixture), 'Known trusted fixture');
  assert.ok((effect3 ? ['main', 'zero', 'after-write-read'] : effect ? ['main', 'project2', 'title2'] : ['main', 'repeated', 'alternate']).includes(row.given.program), 'Fixed program selector');
  assert.ok(Array.isArray(row.stubs) && row.stubs.length <= 4, 'CAPACITY_EXCEEDED'); row.stubs.forEach(call => callShape(call, dcm, true));
  shape(row.expect, ['outcome', 'calls', 'consumption']); assert.ok(Array.isArray(row.expect.calls) && row.expect.calls.length <= 4);
  row.expect.calls.forEach(call => callShape(call, dcm, false));
  const receipt = consumeGeneric(row.expect.outcome, dcm); assert.deepEqual(receipt, row.expect.consumption);
  assert.ok(effect ? receipt.status === 'REPORTED' : receipt.code !== 'OUTCOME_INVALID', 'Valid expected outcome');
}

const keys = new WeakMap();
function nativeKey() {
  const fail = () => { throw new Error('Opaque key conversion'); };
  const key = Object.freeze(Object.defineProperties({}, { toString: { value: fail }, toJSON: { value: fail },
    [Symbol.toPrimitive]: { value: fail }, privateValue: { enumerable: true, get: fail } }));
  keys.set(key, '00000000-0000-4000-8000-000000000001'); return key;
}

export async function runBatch(api, bundle, scenarios) {
  shape(bundle, ['dcmBytes', 'dcmDigest', 'programs']);
  assert.ok(Array.isArray(scenarios) && scenarios.length > 0 && scenarios.length <= 128, 'CAPACITY_EXCEEDED');
  scenarios = json(scenarios, 1048576);
  const dcm = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bundle.dcmBytes));
  assert.ok(['keir-dcm/read-2', 'keir-dcm/effect-2', 'keir-dcm/effect-3'].includes(dcm.format));
  const effect = dcm.format.startsWith('keir-dcm/effect-'), effect3 = dcm.format === 'keir-dcm/effect-3', ids = new Set();
  for (const row of scenarios) { validateGenericScenario(row, dcm); assert.ok(!ids.has(row.id)); ids.add(row.id); }
  const names = effect3 ? ['main', 'zero', 'after-write-read'] : effect ? ['main', 'project2', 'title2'] : ['main', 'repeated', 'alternate']; shape(bundle.programs, names);
  const scope = 'sample.scope', storage = new AsyncLocalStorage(), harness = createHarness(api);
  const [entryId, entry] = Object.entries(dcm.entrypoints)[0];
  const invokers = Object.fromEntries(Object.entries(dcm.capabilities).map(([capability, cap]) => [capability, {
    version: cap.version, mode: 'simulation', ...(effect ? { keyType: dcm.hostKey.type } : {}),
    invoke: async function(args, protectedArgs, remainingMs, host) {
      const state = storage.getStore(); assert.ok(state, 'Request-local fixture context');
      let stub;
      try {
        const capture = json({ capability, version: cap.version, args, protectedArgs }); state.calls.push(capture);
        assert.ok(state.calls.length <= 4 && Number.isSafeInteger(remainingMs) && remainingMs > 0 && remainingMs <= 1000);
        assert.deepEqual(capture.protectedArgs, { actorId: 'user1' });
        if (effect) { shape(host, ['requestKey', 'event']); assert.strictEqual(host.requestKey, state.key); assert.deepEqual(host.event, state.event); }
        else assert.equal(arguments.length, 3);
        stub = state.row.stubs[state.used]; assert.ok(stub, 'Missing stub');
        assert.deepEqual(capture, { capability: stub.capability, version: stub.version, args: stub.args, protectedArgs: stub.protectedArgs });
        if (cap.effect === 'READ' && own(args, 'taskId') && stub.response.kind === 'SUCCESS' && own(stub.response.value, 'id')) assert.equal(stub.response.value.id, args.taskId);
        state.used++;
      } catch { state.violations.push('Call, arguments, protected origin or native key mismatch'); throw new Error('Simulation mismatch'); }
      if (cap.effect === 'READ') {
        if (state.row.given.fixture === 'read-throws' && state.reads === 0) throw new Error('Unavailable read');
        state.reads++;
      } else {
        if (state.row.given.fixture === 'lost-response') throw new Error('Lost response');
        if (state.row.given.fixture === 'response-expired') state.now = 1000;
      }
      return json(stub.response);
    },
  }]));
  const selected = harness.bind(bundle.dcmBytes, { scope, modelRef: { domainId: dcm.domain.id, domainVersion: dcm.domain.version,
    format: dcm.format, digest: bundle.dcmDigest, descriptorRecipe: 'keir-record-descriptors/1' },
  profile: effect3 ? 'keir-effect/3' : effect ? 'keir-effect/2' : 'keir-read/2', environment: api.ENVIRONMENT, mode: 'simulation',
  ...(effect ? { keyBinding: { type: dcm.hostKey.type, validate: value => keys.has(value) } } : {}) }, invokers);
  assert.equal(selected.diagnostic, null, 'Model binding failed');
  const prepared = new Map();
  try {
    for (const name of names) {
      const program = bundle.programs[name]; shape(program, ['bytes', 'digest']);
      const result = harness.prepare(program.bytes, selected.boundModel, { expectedArtifactDigest: program.digest, expectedDcmDigest: bundle.dcmDigest,
        entrypoint: entryId, consumer: entry.consumer, compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: api.ENVIRONMENT, mode: 'simulation' });
      assert.equal(result.diagnostic, null, 'Program preparation failed'); prepared.set(name, result.prepared);
    }
    const results = [];
    for (const row of scenarios) {
      const state = { row, now: 0, used: 0, reads: 0, key: nativeKey(), event: Object.freeze({ id: 'event1', source: row.given.fixture === 'wrong-origin' ? 'other-task' : 'task1' }), calls: [], violations: [] };
      const context = { scope: row.given.fixture === 'wrong-scope' ? 'different.scope' : scope, bindings: { actorId: 'user1' },
        deadline: { clock: { nowMs: () => state.now }, expiresAtMs: 1000 },
        authorize: async request => {
          try { shape(request, ['entrypoint', ...(own(request, 'capability') ? ['capability', 'version'] : [])]); }
          catch { return false; }
          if (request.entrypoint !== entryId) return false;
          if (own(request, 'capability') && (typeof request.capability !== 'string' || !own(dcm.capabilities, request.capability) || request.version !== dcm.capabilities[request.capability].version)) return false;
          if (row.given.fixture === 'invocation-denied') return false;
          if (own(request, 'capability')) {
            if (row.given.fixture === 'call-denied' || row.given.fixture === 'revoked-after-read' && state.reads > 0) return false;
            if (effect && dcm.capabilities[request.capability].effect === 'WRITE' && state.event.source !== row.given.input.taskId) return false;
          }
          return true;
        },
        ...(effect ? { requestKey: row.given.fixture === 'invalid-key' ? 'unbranded' : state.key, event: state.event,
          ...(row.given.fixture === 'zero-effect' ? { effectGrant: 0 } : {}) } : {}),
      };
      const outcome = json(await storage.run(state, () => harness.invoke(prepared.get(row.given.program), json(row.given.input), context)));
      if (state.used !== row.stubs.length) state.violations.push('Unconsumed stub');
      const calls = reconcileCalls(state.calls, outcome.trace, row.stubs, state.violations);
      assert.deepEqual(state.violations, [], `${row.id}: simulation violations`);
      const actual = { id: row.id, outcome, calls, consumption: consumeGeneric(outcome, dcm) };
      assert.deepEqual(actual, { id: row.id, ...row.expect }, `${row.id}: simulation result mismatch`); results.push(actual);
    }
    return results;
  } finally {
    for (const handle of prepared.values()) assert.ok(harness.releasePrepared(handle));
    assert.ok(harness.releaseModel(selected.boundModel)); storage.disable();
  }
}
