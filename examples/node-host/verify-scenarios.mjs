import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdtempSync, readdirSync, rmSync, rmdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { bindModel, invokeProgram, prepareProgram } from '@keir/runtime-node';
import { lexical } from '../../runtime-node/test/scenario-lexical.mjs';
import { consumeEffect } from './effect-consumer.mjs';
import { reconcileCalls } from './effect-simulation.mjs';
import { readBoundedFixture } from './generic-capacity.mjs';
import { callShape, nativeKey, typed } from './generic-simulation.mjs';

const corpus = new URL('../../conformance/keir-2/', import.meta.url);
const checker = fileURLToPath(new URL('../../scripts/check-keir-contracts.py', import.meta.url));
const groups = new Map([
  ['main', ['artifact', 'dcm']],
  ['zero', ['artifact-zero', 'dcm']],
  ['after-write-read', ['artifact-after-write-read', 'dcm']],
  ['no-call', ['artifact-no-call', 'dcm']],
  ['handler', ['artifact-handler', 'dcm']],
  ['title', ['artifact-title', 'dcm-title']],
  ['title-max', ['artifact-title-max', 'dcm-title']],
  ['title-composed', ['artifact-title-composed', 'dcm-title']],
]);
const fixtures = new Set(['normal', 'read-throws', 'lost-response', 'response-expired']);

function pinned(name) {
  const limit = name.startsWith('dcm') ? 65_536 : 1_048_576;
  const bytes = readBoundedFixture(new URL(name + '.json', corpus), limit);
  const expected = readBoundedFixture(new URL(name + '.sha256', corpus), 128).toString('ascii').trim();
  assert.equal('sha256:' + createHash('sha256').update(bytes).digest('hex'), expected, name + ' byte pin');
  return bytes;
}

function checkSchema(bytes, python) {
  const directory = mkdtempSync(join(tmpdir(), 'keir-scenario-'));
  const file = join(directory, 'scenario.json');
  try {
    writeFileSync(file, bytes, { flag: 'wx', mode: 0o600 });
    const result = spawnSync(python, ['-B', checker, '--revision', '2', 'scenario', file], { encoding: 'utf8', timeout: 30_000 });
    if (result.error) throw result.error;
    assert.ok(result.status === 0 || result.status === 1, result.stderr);
    const check = JSON.parse(result.stdout);
    assert.ok(result.status === 0 && check.valid === true, 'Scenario schema: ' + JSON.stringify(check.diagnostics));
  } finally {
    rmSync(file, { force: true });
    rmdirSync(directory);
  }
}

function checkByteFields({ stubs, expect }) {
  const rows = [...stubs, ...expect.calls, ...expect.outcome.trace, ...expect.outcome.effects,
    ...(expect.consumption.effects ?? [])];
  for (const row of rows) {
    for (const field of ['capability', 'version']) {
      if (Object.hasOwn(row, field)) assert.ok(Buffer.byteLength(row[field]) <= 256, 'Scenario ' + field + ' byte limit');
    }
  }
  for (const error of [expect.outcome.error, expect.consumption.error]) {
    if (!error) continue;
    assert.ok(Buffer.byteLength(error.path) <= 1024, 'Scenario error path byte limit');
    if (error.cause) assert.ok(Buffer.byteLength(error.cause.path) <= 1024, 'Scenario cause path byte limit');
  }
}

function checkExpectations(row, artifact, dcm) {
  const { expect, stubs } = row;
  assert.deepEqual(stubs.map(({ response, ...call }) => call), expect.calls, 'Stub and expected call order');
  const target = ({ step, capability }) => ({ step, capability });
  const trace = expect.outcome.trace;
  const dispatched = trace.filter(event => event.event === 'CALL_DISPATCHED');
  const intents = trace.filter(event => event.event === 'EFFECT_INTENT');
  const receipts = trace.filter(event => event.event === 'EFFECT_RECORDED');
  const writes = expect.calls.filter(call => dcm.capabilities[call.capability].effect === 'WRITE');
  assert.deepEqual(dispatched.map(target), expect.calls.map(target), 'Expected dispatch order');
  for (const guard of trace.filter(event => event.event === 'GUARD_EVALUATED' && !event.taken)) {
    assert.ok(!dispatched.some(call => call.step === guard.step), 'Expected dispatch after a false guard');
  }
  assert.deepEqual(intents.map(target), writes.map(target), 'Expected write intents');
  assert.deepEqual(receipts.map(target), writes.map(target), 'Expected write receipts');
  assert.deepEqual(receipts.map(({ step, capability, state }) => ({ step, capability, state })),
    expect.outcome.effects, 'Expected receipt states agree with the trace');
  for (const [index, intent] of intents.entries()) {
    const call = dispatched.find(event => event.step === intent.step);
    assert.ok(intent.sequence < call.sequence && call.sequence < receipts[index].sequence, 'Expected write event order');
  }
  for (const call of expect.calls) {
    const step = artifact.steps.find(step => step.id === call.step);
    assert.ok(step?.op === 'CALL' && step.capability === call.capability, 'Expected artifact call');
  }
  const report = consumeEffect(expect.outcome);
  assert.equal(report.status, 'REPORTED', 'Invalid expected outcome');
  assert.deepEqual(report, expect.consumption, 'Expected consumer report');
}

export function decodeScenario(bytes, python) {
  const row = lexical(bytes);
  checkSchema(bytes, python);
  checkByteFields(row);
  return row;
}

export function prepareScenario(group, bytes, python, testPair) {
  const row = decodeScenario(bytes, python);
  const pair = groups.get(group);
  assert.ok(pair, 'Unknown scenario group');
  const [artifactBytes, modelBytes] = testPair ?? pair.map(pinned);
  const artifact = JSON.parse(artifactBytes);
  const dcm = JSON.parse(modelBytes);
  const key = nativeKey();
  const event = Object.freeze({ id: 'event1', source: 'task1' });
  const captures = [];
  const violations = [];
  const expiresAtMs = 1000;
  let now = 0;
  const deadline = { clock: { nowMs: () => now }, expiresAtMs };
  const implementations = Object.fromEntries(Object.entries(dcm.capabilities).map(([capability, cap]) => [capability, {
    version: cap.version,
    mode: 'simulation',
    keyType: dcm.hostKey.type,
    invoke(args, protectedArgs, remainingMs, host) {
      const index = captures.push(structuredClone({ capability, version: cap.version, args, protectedArgs })) - 1;
      const stub = row.stubs[index];
      try {
        assert.ok(stub, 'Missing stub');
        const { step: _step, response, ...wanted } = stub;
        assert.deepEqual(captures[index], wanted, 'Native call and stub arguments');
        assert.deepEqual(captures[index].protectedArgs, { actorId: 'user1' }, 'Protected actor');
        assert.equal(host.requestKey, key, 'Original opaque key');
        assert.deepEqual(host.event, event, 'Trusted event');
        assert.equal(host.deadline.expiresAtMs, expiresAtMs, 'Absolute deadline');
        assert.equal(host.deadline.clock.nowMs(), now, 'Trusted clock');
        assert.ok(remainingMs > 0 && remainingMs === expiresAtMs - now, 'Remaining deadline budget');
        if (cap.effect === 'READ' && response.kind === 'SUCCESS') assert.equal(response.value.id, args.taskId);
        if (cap.effect === 'WRITE') assert.equal(row.given.input.taskId, event.source);
      } catch (error) {
        violations.push(error.message);
        throw error;
      }
      const fixture = row.given.fixture;
      if (cap.effect === 'READ' && index === 0 && fixture === 'read-throws') throw new Error('Read failed');
      if (cap.effect === 'WRITE' && fixture === 'lost-response') throw new Error('Lost write response');
      if (cap.effect === 'WRITE' && fixture === 'response-expired') now = expiresAtMs;
      return structuredClone(stub.response);
    },
  }]));
  const binding = bindModel(modelBytes, implementations);
  assert.equal(binding.diagnostic, null, 'Scenario model compatibility');
  const preparation = prepareProgram(artifactBytes, binding.boundModel);
  assert.equal(preparation.diagnostic, null, 'Scenario artifact compatibility');

  assert.ok(fixtures.has(row.given.fixture), 'Unknown scenario fixture');
  const entry = dcm.entrypoints[artifact.entrypoint];
  typed(row.given.input, entry.input, dcm);
  for (const value of [row.expect.outcome, row.expect.consumption]) {
    if (Object.hasOwn(value, 'result')) typed(value.result, entry.output, dcm);
  }
  for (const stub of row.stubs) {
    callShape(stub, dcm, true);
    assert.deepEqual(stub.protectedArgs, { actorId: 'user1' }, 'Expected protected actor');
  }
  row.expect.calls.forEach(call => callShape(call, dcm, false));
  checkExpectations(row, artifact, dcm);
  assert.equal(captures.length, 0, 'Preflight dispatched a stub');

  return {
    id: row.id,
    async run() {
      const outcome = await invokeProgram(preparation.prepared, row.given.input, {
        bindings: group === 'no-call' ? {} : { actorId: 'user1' }, deadline, requestKey: key, event,
      });
      const calls = reconcileCalls(captures, outcome.trace, row.stubs, violations);
      assert.deepEqual(violations, [], row.id + ': host adapter violations');
      const consumption = consumeEffect(outcome);
      assert.deepEqual({ outcome, calls, consumption }, row.expect, row.id + ': scenario report');
      return { id: row.id, outcome, calls, consumption };
    },
  };
}

export async function verifyGroup(group, python) {
  assert.ok(groups.has(group), 'Unknown scenario group');
  const directory = new URL('scenarios/' + group + '/', corpus);
  const files = readdirSync(directory).filter(name => name.endsWith('.json')).sort();
  assert.ok(files.length > 0 && files.length <= 128, 'Scenario file count');
  function* documents() {
    for (const file of files) yield readBoundedFixture(new URL(file, directory), 65_536);
  }
  return verifyBatch(group, documents(), python);
}

export async function verifyBatch(group, documents, python) {
  const scenarios = [];
  let batchBytes = 0;
  for (const bytes of documents) {
    assert.ok(scenarios.length < 128, 'Scenario file count');
    batchBytes += bytes.length;
    assert.ok(batchBytes <= 1_048_576, 'Scenario batch byte limit');
    scenarios.push(prepareScenario(group, bytes, python));
  }
  assert.ok(scenarios.length > 0, 'No scenario files');
  assert.equal(new Set(scenarios.map(scenario => scenario.id)).size, scenarios.length, 'Duplicate scenario ID');
  const reports = [];
  for (const scenario of scenarios) reports.push(await scenario.run());
  return reports;
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  const [group, python = 'python'] = process.argv.slice(2);
  assert.ok(group && process.argv.length <= 4, 'Usage: node examples/node-host/verify-scenarios.mjs <group> [python]');
  console.log(JSON.stringify(await verifyGroup(group, python)));
}
