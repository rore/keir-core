import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runReadSimulation } from '@keir/runtime-node/read-simulation';
import * as runtime from '@keir/runtime-node';
import { canonicalBytes } from '../src/canonical.js';
import { fixture as reference } from './reference-helpers.js';

const root = new URL('../../conformance/read-simulation/', import.meta.url);
const load = (name, bundle = 'task-advice') => readFileSync(new URL(bundle + '/' + name, root));
const scenario = (name = 'open', bundle) => JSON.parse(load(name + '.json', bundle));
const bytes = value => Buffer.from(JSON.stringify(value));
const clone = value => structuredClone(value);
function selection(bundle = 'task-advice') {
  return { ...JSON.parse(load('selection.json', bundle)), dcmBytes: load('dcm.json', bundle), artifactBytes: load('artifact.json', bundle) };
}
function fixture(overrides = {}) {
  return { scope: 'test-simulation', bindings: { actorId: 'user1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 },
    authorize: async () => true,
    consume: outcome => outcome.classification === 'COMPLETED' ? { status: 'DISPLAYED', code: outcome.result.classification } : { status: 'NOT_DISPLAYED' },
    allowedReceipts: [...['DONE', 'PENDING', 'NEEDS_ATTENTION'].map(code => ({ status: 'DISPLAYED', code })), { status: 'NOT_DISPLAYED' }],
    ...overrides };
}
const run = (row = scenario(), host = fixture(), selected = selection()) => runReadSimulation(selected, host, bytes(row));
function bounded(report) {
  assert.deepEqual(Object.keys(report).sort(), ['format', 'identity', 'mismatches', 'status',
    ...(Object.hasOwn(report, 'consumption') ? ['consumption'] : []), ...(Object.hasOwn(report, 'native') ? ['native'] : [])].sort());
  assert.equal(report.format, 'keir-read-simulation-report/1'); assert.ok(Object.isFrozen(report));
  assert.ok(canonicalBytes(report).length <= 16384); assert.ok(report.mismatches.length <= 8);
  if (['SETUP_ERROR', 'INVALID_SCENARIO'].includes(report.status)) assert.equal(Object.hasOwn(report, 'native'), false);
  else {
    assert.ok(['COMPLETED', 'BUSINESS_REJECTION', 'FAILED', 'LIMIT_EXCEEDED'].includes(report.native.classification));
    assert.deepEqual(Object.keys(report.native).sort(), report.native.classification === 'COMPLETED' ? ['classification'] : ['classification', 'code']);
    if (report.native.classification !== 'COMPLETED') assert.ok([
      'AUTHORITY_DENIED', 'AUTHORITY_CHECK_FAILED', 'HOST_CONTEXT_INVALID', 'HOST_BINDING_INVALID', 'INPUT_INVALID',
      'ADAPTER_EXCEPTION', 'ADAPTER_INVALID', 'ADAPTER_TRANSIENT', 'EXPRESSION_FAILED', 'BUDGET_EXCEEDED', 'NATIVE_ERROR',
    ].includes(report.native.code));
  }
  for (const mismatch of report.mismatches) {
    assert.deepEqual(Object.keys(mismatch).sort(), ['code', 'ordinal', 'stage']);
    assert.ok(Number.isInteger(mismatch.ordinal) && mismatch.ordinal >= 0 && mismatch.ordinal <= 4);
  }
}
const has = (report, code, ordinal) => report.mismatches.some(item => item.code === code && (ordinal === undefined || item.ordinal === ordinal));

test('opt-in subpath preserves root API and all independently authored task-advice cases', async () => {
  for (const name of ['bindModel', 'prepareProgram', 'invokeProgram', 'prepare', 'invoke']) assert.equal(typeof runtime[name], 'function');
  for (const name of ['open', 'completed', 'missing', 'denied', 'business-denied', 'wrong-error', 'transient', 'mismatch']) {
    const host = fixture(name === 'denied' ? { authorize: async () => false } : {});
    const report = await run(scenario(name), host); bounded(report);
    const mismatch = ['mismatch', 'wrong-error'].includes(name);
    assert.equal(report.status, mismatch ? 'MISMATCH' : 'PASS', name);
    assert.deepEqual(report.identity, { modelDigest: selection().modelDigest, artifactDigest: selection().artifactDigest });
    if (mismatch) assert.ok(has(report, 'OUTCOME_MISMATCH'));
  }
});

test('a distinguishable repeated-call artifact is consumed in order without searching', async () => {
  const selected = selection('repeated');
  const report = await run(scenario('repeated', 'repeated'), fixture(), selected); bounded(report); assert.equal(report.status, 'PASS');
  const reordered = await run(scenario('reordered', 'repeated'), fixture(), selected); bounded(reordered);
  assert.equal(reordered.status, 'MISMATCH'); assert.ok(has(reordered, 'STUB_MISMATCH', 1));
});

test('all unused stubs, argument/result types and expectations fail before callbacks', async () => {
  const mutations = [
    row => { row.authorize = true; }, row => { row.fixture = 'normal'; }, row => { row.input.actorId = 'forged'; },
    row => { row.stubs[0].protectedArgs = { actorId: 'forged' }; },
    row => { row.stubs[0].capability = 'unpublished'; }, row => { row.stubs[0].version = 'wrong'; },
    row => { row.stubs[0].args.taskId = true; }, row => { row.stubs[0].response.value.status = 'UNKNOWN'; },
    row => { row.stubs.push({ ...clone(row.stubs[0]), response: { kind: 'SUCCESS', value: {} } }); },
    row => { row.stubs.push({ ...clone(row.stubs[0]), response: { kind: 'BUSINESS_REJECTION', code: 'UNKNOWN' } }); },
    row => { row.stubs[0].response = { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' }; },
    row => { row.expect.calls[0].args.taskId = 1; }, row => { row.expect.outcome.result.classification = 'UNKNOWN'; },
    row => { row.expect.consumption = { status: 'DISPLAYED', code: 'UNLISTED' }; },
    row => { row.expect.outcome.effects = []; }, row => { row.expect.outcome.trace = []; },
    row => { row.expect.outcome.error = { code: 'INPUT_INVALID', path: '' }; }, row => { row.id = '9invalid'; },
  ];
  let callbacks = 0;
  const host = fixture({ authorize: () => { callbacks++; return true; }, consume: () => { callbacks++; return { status: 'NOT_DISPLAYED' }; },
    responseCheck: () => { callbacks++; return true; } });
  for (const mutate of mutations) {
    const row = scenario(); mutate(row); const report = await run(row, host); bounded(report);
    assert.equal(report.status, 'INVALID_SCENARIO'); assert.equal(callbacks, 0);
    assert.deepEqual(report.mismatches, [{ stage: 'SCENARIO', code: 'INVALID_SCENARIO', ordinal: 0 }]);
    assert.equal(Object.hasOwn(report, 'consumption'), false);
  }
  const dotted = scenario(); dotted.id = 'Case.with-dots_and_underscores'; assert.equal((await run(dotted)).status, 'PASS');
});

test('setup pin/model/fixture failures are redacted and never expose an identity or callback', async () => {
  let callbacks = 0;
  const host = fixture({ authorize: () => { callbacks++; return true; }, consume: () => { callbacks++; return {}; } });
  for (const mutate of [
    value => { value.modelDigest = 'secret-invalid-pin'; }, value => { value.artifactDigest = 'sha256:' + '0'.repeat(64); },
    value => { value.compiler = 'secret-wrong-compiler'; }, value => { value.consumer = {}; },
    value => { value.entrypoint = 'Unpublished'; }, value => { value.invokers = {}; },
    value => { value.artifactBytes = new Uint8Array(1048577); }, value => { value.dcmBytes = new Uint8Array(65537); },
  ]) {
    const selected = selection(); mutate(selected); const report = await run(scenario(), host, selected); bounded(report);
    assert.equal(report.status, 'SETUP_ERROR'); assert.deepEqual(report.identity, {}); assert.equal(callbacks, 0);
    assert.doesNotMatch(JSON.stringify(report), /secret|compiler|consumer|Unpublished/);
  }
  for (const overrides of [
    { allowedReceipts: [] }, { allowedReceipts: Array(17).fill({ status: 'NOT_DISPLAYED' }) },
    { allowedReceipts: [{ status: 'NOT_DISPLAYED' }, { status: 'NOT_DISPLAYED' }] },
    { allowedReceipts: [{ status: 'DISPLAYED', code: 'bad\n' }] }, { consume: null }, { responseCheck: null },
    { bindings: {} }, { bindings: { actorId: 1 } }, { bindings: { actorId: 'user1', extra: 'bad' } },
    { deadline: { clock: { nowMs: 1 }, expiresAtMs: 1000 } },
  ]) assert.equal((await run(scenario(), fixture(overrides))).status, 'SETUP_ERROR');
  let getters = 0;
  const receipt = { status: 'DISPLAYED' }; Object.defineProperty(receipt, 'code', { enumerable: true, get() { getters++; return 'DONE'; } });
  assert.equal((await run(scenario(), fixture({ allowedReceipts: [receipt] }))).status, 'SETUP_ERROR'); assert.equal(getters, 0);
});

test('caught adapter mismatch cannot pass even when expected native failure exactly agrees', async () => {
  const row = scenario(); row.stubs = [];
  row.expect.outcome = { classification: 'FAILED', error: { code: 'ADAPTER_EXCEPTION', path: '/steps/0' } };
  row.expect.consumption = { status: 'NOT_DISPLAYED' };
  const report = await run(row); bounded(report);
  assert.equal(report.status, 'MISMATCH'); assert.ok(has(report, 'MISSING_STUB', 1));
  assert.equal(has(report, 'OUTCOME_MISMATCH'), false);
  assert.deepEqual(report.native, { classification: 'FAILED', code: 'ADAPTER_EXCEPTION' });
});

test('leftovers, mismatching arguments and trace step labels have independent ordered ledger entries', async () => {
  const extra = scenario(); extra.stubs.push(clone(extra.stubs[0]));
  const leftovers = await run(extra); assert.deepEqual(leftovers.mismatches, [{ stage: 'STUB', code: 'EXTRA_STUB', ordinal: 2 }]);
  const wrong = scenario(); wrong.stubs[0].args.taskId = 'task2';
  const mismatch = await run(wrong); assert.equal(mismatch.mismatches[0].code, 'STUB_MISMATCH'); assert.ok(has(mismatch, 'EXTRA_STUB', 1));
  const label = scenario(); label.stubs[0].step = 'other';
  const trace = await run(label); assert.equal(trace.status, 'MISMATCH'); assert.ok(has(trace, 'TRACE_MISMATCH', 1));
});

test('consumer errors override mismatches and never echo offending receipts or exceptions', async () => {
  const canary = 'CANARY_PRIVATE_VALUE';
  for (const consume of [
    () => { throw new Error(canary); }, () => ({ status: 'DISPLAYED', code: canary }),
    () => ({ status: 'NOT_DISPLAYED', result: canary }), () => null,
    () => Promise.resolve({ status: 'NOT_DISPLAYED' }), () => Promise.reject(new Error(canary)),
  ]) {
    const row = scenario('mismatch'); const report = await run(row, fixture({ consume })); bounded(report);
    assert.equal(report.status, 'CONSUMER_ERROR'); assert.ok(has(report, 'CONSUMER_ERROR'));
    assert.ok(has(report, 'OUTCOME_MISMATCH')); assert.equal(Object.hasOwn(report, 'consumption'), false);
    assert.doesNotMatch(JSON.stringify(report), /CANARY|result|exception/);
  }
  const wrong = await run(scenario(), fixture({ consume: () => ({ status: 'DISPLAYED', code: 'DONE' }) }));
  assert.equal(wrong.status, 'MISMATCH'); assert.ok(has(wrong, 'CONSUMPTION_MISMATCH'));
  assert.deepEqual(wrong.consumption, { status: 'DISPLAYED', code: 'DONE' });
});

test('consumer still runs for valid settled outcomes after a stub mismatch; malformed expectation never runs it', async () => {
  let consumed = 0;
  const host = fixture({ consume: () => { consumed++; return { status: 'NOT_DISPLAYED' }; } });
  const wrong = scenario(); wrong.stubs[0].args.taskId = 'task2';
  assert.equal((await run(wrong, host)).status, 'MISMATCH'); assert.equal(consumed, 1);
  wrong.expect.outcome.classification = 'FAILED';
  wrong.expect.outcome.result = { classification: 'DONE' };
  assert.equal((await run(wrong, host)).status, 'INVALID_SCENARIO'); assert.equal(consumed, 1);
});

test('optional response checks are host-owned synchronous relationships, never implicit task rules', async () => {
  const row = scenario(); row.stubs[0].response.value.id = 'other-task';
  assert.equal((await run(row)).status, 'PASS');
  for (const responseCheck of [
    (_capability, args, response) => response.value.id === args.taskId,
    () => { throw new Error('PRIVATE_RESPONSE_EXCEPTION'); }, () => Promise.reject(new Error('PRIVATE_RESPONSE_EXCEPTION')),
  ]) {
    const report = await run(row, fixture({ responseCheck })); bounded(report);
    assert.equal(report.status, 'MISMATCH'); assert.ok(has(report, 'STUB_MISMATCH', 1)); assert.doesNotMatch(JSON.stringify(report), /PRIVATE|other-task/);
  }
});

test('a declared transient read is consumed and compared as the native failure', async () => {
  const row = scenario();
  row.stubs[0].response = { kind: 'TRANSIENT_FAILURE' };
  row.expect.outcome = { classification: 'FAILED', error: { code: 'ADAPTER_TRANSIENT', path: '/steps/0' } };
  row.expect.consumption = { status: 'NOT_DISPLAYED' };
  const report = await run(row); bounded(report); assert.equal(report.status, 'PASS');
  assert.deepEqual(report.native, { classification: 'FAILED', code: 'ADAPTER_TRANSIENT' });
});

test('fresh current authorization is enforced on reused host fixtures', async () => {
  let allowed = true, responses = 0;
  const host = fixture({ authorize: async () => allowed, responseCheck: () => { responses++; return true; } });
  assert.equal((await run(scenario(), host)).status, 'PASS'); assert.equal(responses, 1);
  allowed = false;
  const denied = await run(scenario('denied'), host); bounded(denied); assert.equal(denied.status, 'PASS'); assert.equal(responses, 1);
  allowed = true; assert.equal((await run(scenario(), host)).status, 'PASS'); assert.equal(responses, 2);
});

test('report redaction excludes scenario IDs, business facts, protected values and step labels', async () => {
  const secret = 'CANARYPRIVATEFACT';
  const row = scenario(); row.id = secret; row.input.taskId = secret;
  row.stubs[0].args.taskId = secret; row.stubs[0].response.value.id = secret; row.stubs[0].step = secret;
  row.expect.calls[0].args.taskId = secret;
  const report = await run(row, fixture({ scope: secret, bindings: { actorId: secret } }));
  bounded(report); assert.equal(report.status, 'MISMATCH');
  assert.doesNotMatch(JSON.stringify(report), /CANARY|actorId|taskId|source|scope/);
  assert.deepEqual(report.native, { classification: 'COMPLETED' });
});

test('raw case limits, duplicate keys, depth and non-data inputs reject before callbacks', async () => {
  let callbacks = 0;
  const host = fixture({ authorize: () => { callbacks++; return true; } });
  const good = bytes(scenario());
  const exact = Buffer.concat([good, Buffer.alloc(65536 - good.length, 32)]);
  assert.equal((await runReadSimulation(selection(), host, exact)).status, 'PASS');
  callbacks = 0;
  for (const bad of [
    Buffer.concat([exact, Buffer.from(' ')]), null, {}, Buffer.from([0xff]),
    Buffer.from('{"format":"duplicate","format":"keir-read-simulation/1"}'),
    Buffer.from('{"format":"duplicate","\\u0066ormat":"keir-read-simulation/1"}'),
    Buffer.from('{"x":' + '['.repeat(20) + 'true' + ']'.repeat(20) + '}'),
    Buffer.from('{"x":null}'), Buffer.from('{"x":-1}'), Buffer.from('{"x":1.5}'),
  ]) {
    const report = await runReadSimulation(selection(), host, bad); bounded(report); assert.equal(report.status, 'INVALID_SCENARIO'); assert.equal(callbacks, 0);
  }
});

test('four stub/call and receipt bounds remain distinct from reached native work', async () => {
  for (const count of [4, 5]) {
    const row = scenario(); row.stubs = Array.from({ length: count }, () => clone(row.stubs[0]));
    const report = await run(row); bounded(report); assert.equal(report.status, count === 4 ? 'MISMATCH' : 'INVALID_SCENARIO');
    row.stubs = [row.stubs[0]]; row.expect.calls = Array.from({ length: count }, () => clone(row.expect.calls[0]));
    assert.equal((await run(row)).status, count === 4 ? 'MISMATCH' : 'INVALID_SCENARIO');
  }
  assert.equal((await run(scenario('missing'), fixture({ allowedReceipts: [{ status: 'NOT_DISPLAYED' }] }))).status, 'PASS');
  const receipts = fixture().allowedReceipts;
  receipts.push(...Array.from({ length: 12 }, (_, i) => ({ status: 'DISPLAYED', code: 'CODE_' + i })));
  assert.equal((await run(scenario(), fixture({ allowedReceipts: receipts }))).status, 'PASS');
});

test('scope, bindings, callbacks, allowlist and selection snapshots survive pending authorization', async () => {
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  const selected = selection(), host = fixture({ authorize: async () => { await gate; return true; } });
  const pending = run(scenario(), host, selected);
  selected.dcmBytes.fill(0); selected.artifactBytes.fill(0); selected.consumer.id = 'changed';
  host.scope = 'changed'; host.bindings.actorId = 'changed'; host.allowedReceipts.length = 0;
  host.consume = () => { throw new Error('changed'); };
  release(); const report = await pending; bounded(report); assert.equal(report.status, 'PASS');
});

test('compact expectations reject trace and nested native details before invocation', async () => {
  let callbacks = 0;
  const host = fixture({ authorize: () => { callbacks++; return true; }, consume: () => { callbacks++; return { status: 'NOT_DISPLAYED' }; } });
  for (const mutate of [
    row => { row.expect.outcome.trace = []; },
    row => { row.expect.outcome.error.cause = { code: 'ACCESS_DENIED', path: '/steps/0' }; },
    row => { row.expect.outcome.result = { classification: 'DONE' }; },
    row => { row.expect.outcome.error.secret = 'PRIVATE_DETAIL'; },
  ]) {
    const row = scenario('missing'); mutate(row);
    const report = await run(row, host); bounded(report); assert.equal(report.status, 'INVALID_SCENARIO'); assert.equal(callbacks, 0);
  }
});

test('complete catalogs including unused WRITE bind without exposing WRITE stubs', async () => {
  const f = reference('read');
  const generic = JSON.parse(readFileSync(new URL('../../conformance/generic-hooks/read/scenarios.json', import.meta.url)));
  const source = generic.find(row => row.given.fixture === 'normal' && row.given.program === 'main');
  const fields = call => ({ step: call.step, capability: call.capability, version: call.version, args: call.args });
  const row = { format: 'keir-read-simulation/1', id: 'Catalog', input: source.given.input,
    stubs: source.stubs.map(stub => ({ ...fields(stub), response: stub.response })),
    expect: { calls: source.expect.calls.map(fields), outcome: { classification: source.expect.outcome.classification, result: source.expect.outcome.result },
      consumption: { status: 'DISPLAYED', code: 'DONE' } } };
  const selected = { dcmBytes: f.dcmBytes, artifactBytes: f.bytes, modelDigest: f.dcmPin, artifactDigest: f.pin,
    compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', entrypoint: 'ClassifyTask', consumer: f.dcm.entrypoints.ClassifyTask.consumer };
  assert.equal((await run(row, fixture(), selected)).status, 'PASS');
  row.stubs.push({ step: 'unused', capability: 'tasks.create', version: '1', args: { projectId: 'p', title: 't' }, response: { kind: 'SUCCESS', value: {} } });
  assert.equal((await run(row, fixture(), selected)).status, 'INVALID_SCENARIO');
});

test('public summaries preserve fixed runtime codes but never domain codes or diagnostic paths', async () => {
  const missing = await run(scenario('missing')); bounded(missing); assert.equal(missing.status, 'PASS');
  assert.deepEqual(missing.native, { classification: 'BUSINESS_REJECTION', code: 'NATIVE_ERROR' });
  assert.doesNotMatch(JSON.stringify(missing), /TASK_NOT_FOUND|steps|error|path/);
  const denied = await run(scenario('denied'), fixture({ authorize: async () => false }));
  assert.deepEqual(denied.native, { classification: 'FAILED', code: 'AUTHORITY_DENIED' });
  const consumer = await run(scenario('missing'), fixture({ consume: () => { throw new Error('PRIVATE_ERROR'); } }));
  bounded(consumer); assert.equal(consumer.status, 'CONSUMER_ERROR');
  assert.deepEqual(consumer.native, { classification: 'BUSINESS_REJECTION', code: 'NATIVE_ERROR' });
  assert.equal(Object.hasOwn(consumer, 'consumption'), false);
  assert.doesNotMatch(JSON.stringify(consumer), /PRIVATE|TASK_NOT_FOUND|path/);
  const limitedCase = scenario('denied'); limitedCase.expect.outcome = { classification: 'LIMIT_EXCEEDED', error: { code: 'BUDGET_EXCEEDED', path: '' } };
  const limited = await run(limitedCase, fixture({ deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 0 } }));
  bounded(limited); assert.equal(limited.status, 'PASS'); assert.deepEqual(limited.native, { classification: 'LIMIT_EXCEEDED', code: 'BUDGET_EXCEEDED' });
});

test('private outcome comparison distinguishes domain codes that share the same public fallback', async () => {
  const row = scenario('missing'); row.expect.outcome.error.code = 'ACCESS_DENIED';
  const wrong = await run(row); bounded(wrong);
  assert.equal(wrong.status, 'MISMATCH'); assert.ok(has(wrong, 'OUTCOME_MISMATCH'));
  assert.deepEqual(wrong.native, { classification: 'BUSINESS_REJECTION', code: 'NATIVE_ERROR' });
  assert.doesNotMatch(JSON.stringify(wrong), /TASK_NOT_FOUND|ACCESS_DENIED/);
});

test('business ACCESS_DENIED consumes one dispatched stub; host denial dispatches none', async () => {
  let checked = 0, authorized = 0;
  const host = fixture({ authorize: async () => { authorized++; return true; }, responseCheck: () => { checked++; return true; } });
  const row = scenario('missing'); row.stubs[0].response.code = 'ACCESS_DENIED'; row.expect.outcome.error.code = 'ACCESS_DENIED';
  const business = await run(row, host); bounded(business); assert.equal(business.status, 'PASS');
  assert.equal(checked, 1); assert.equal(authorized, 2);
  assert.deepEqual(business.native, { classification: 'BUSINESS_REJECTION', code: 'NATIVE_ERROR' });
  checked = 0; authorized = 0;
  const rejected = await run(scenario('denied'), { ...host, authorize: async () => { authorized++; return false; } });
  bounded(rejected); assert.equal(rejected.status, 'PASS'); assert.equal(checked, 0); assert.equal(authorized, 1);
  assert.deepEqual(rejected.native, { classification: 'FAILED', code: 'AUTHORITY_DENIED' });
});

test('case and DCM UTF-8 BOMs reject locally without invoking any host callbacks', async () => {
  let callbacks = 0;
  const host = fixture({ authorize: () => { callbacks++; return true; }, consume: () => { callbacks++; return { status: 'NOT_DISPLAYED' }; } });
  const bom = Buffer.from([0xef, 0xbb, 0xbf]);
  const invalid = await runReadSimulation(selection(), host, Buffer.concat([bom, bytes(scenario())]));
  bounded(invalid); assert.equal(invalid.status, 'INVALID_SCENARIO'); assert.equal(callbacks, 0);
  const selected = selection(); selected.dcmBytes = Buffer.concat([bom, selected.dcmBytes]);
  const setup = await run(scenario(), host, selected); bounded(setup); assert.equal(setup.status, 'SETUP_ERROR');
  assert.deepEqual(setup.identity, {}); assert.equal(callbacks, 0);
});
