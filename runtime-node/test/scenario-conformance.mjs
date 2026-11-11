import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { consumeDecision } from '../../examples/node-host/decision-consumer.mjs';
import { consumeEffect } from '../../examples/node-host/effect-consumer.mjs';
import { canonicalBytes, digest } from '../src/canonical.js';
import { bindModel, invokeProgram, prepareProgram } from '../src/index.js';
import { validateValue } from '../src/validation.js';
import { lexical } from './scenario-lexical.mjs';

const revision = process.argv[3] ?? '2';
assert.ok(['2', '3'].includes(revision), 'Scenario revision must be 2 or 3');
assert.ok(process.argv.length <= 4, 'Usage: node scenario-conformance.mjs [PYTHON [2|3]]');
const corpus = new URL('../../conformance/keir-' + revision + '/', import.meta.url);
const read = path => readFileSync(new URL(path, corpus));
const groups = new Map([
  ['main', ['artifact', 'dcm']],
  ['zero', ['artifact-zero', 'dcm']],
  ['after-write-read', ['artifact-after-write-read', 'dcm']],
  ['no-call', ['artifact-no-call', 'dcm']],
  ['handler', ['artifact-handler', 'dcm']],
  ['title', ['artifact-title', 'dcm-title']],
  ['title-max', ['artifact-title-max', 'dcm-title']],
  ['title-composed', ['artifact-title-composed', 'dcm-title']],
  ['inventory', ['artifact-inventory', 'dcm-inventory']],
  ['inventory-algebra', ['artifact-inventory-algebra', 'dcm-inventory']],
  ['inventory-enum', ['artifact-inventory-enum', 'dcm-inventory-enum']],
]);
const fixtures = new Set(['normal', 'read-throws', 'lost-response', 'response-expired']);
const withoutBindings = new Set(['no-call', 'inventory-enum']);
if (revision === '3') {
  groups.set('guard', ['artifact-guard', 'dcm-guard']);
  groups.set('guard-input', ['artifact-guard-input', 'dcm-guard']);
  groups.set('guard-handler', ['artifact-guard-handler', 'dcm-guard']);
  groups.set('guard-reversed', ['artifact-guard-reversed', 'dcm-guard-reversed']);
  groups.set('guarded-result', ['artifact-guarded-result', 'dcm-guarded-result']);
  groups.set('guarded-result-read', ['artifact-guarded-result-read', 'dcm-guarded-result-read']);
  groups.set('guarded-result-handler', ['artifact-guarded-result-handler', 'dcm-guarded-result']);
  groups.set('guarded-result-unused', ['artifact-guarded-result-unused', 'dcm-guarded-result']);
  fixtures.add('read-expired');
  fixtures.add('read-malformed');
  withoutBindings.add('guard-input');
}
assert.equal(groups.size, revision === '2' ? 11 : 15 + 4);

function vectorBytes(vector) {
  if (Object.hasOwn(vector, 'utf8')) return Buffer.from(vector.utf8);
  if (Object.hasOwn(vector, 'base64')) return Buffer.from(vector.base64, 'base64');
  let bytes = read('scenarios/' + vector.scenario + '.json');
  if (vector.replace) {
    const [search, template] = vector.replace;
    const replacement = vector.expand ? template.replace('@', () => vector.expand[0].repeat(vector.expand[1])) : template;
    const text = bytes.toString('utf8');
    assert.ok(text.includes(search), 'Missing replacement target for ' + vector.id);
    bytes = Buffer.from(text.replace(search, () => replacement));
  }
  if (vector.padToBytes !== undefined) {
    assert.ok(bytes.length <= vector.padToBytes, 'Padding target is shorter than scenario');
    bytes = Buffer.concat([bytes, Buffer.alloc(vector.padToBytes - bytes.length, 0x20)]);
  }
  return bytes;
}

function structural(entries, python) {
  const decoded = [];
  for (const entry of entries) {
    try { entry.document = lexical(entry.bytes); }
    catch (error) {
      if (!(error instanceof assert.AssertionError)) throw error;
      entry.lexicalError = error;
      continue;
    }
    decoded.push(entry);
  }
  const checker = fileURLToPath(new URL('./scenario-schema.py', import.meta.url));
  const result = spawnSync(python, ['-B', checker, revision], {
    input: JSON.stringify(decoded.map(entry => entry.document)),
    encoding: 'utf8',
    timeout: 30_000,
  });
  if (result.error) throw result.error;
  assert.equal(result.status, 0, result.stderr);
  const valid = JSON.parse(result.stdout);
  assert.equal(valid.length, decoded.length);
  decoded.forEach((entry, index) => { entry.schemaValid = valid[index]; });
}

function byteFields({ stubs, expect }) {
  function checkText(fields) {
    if (Object.hasOwn(fields, 'capability')) assert.ok(Buffer.byteLength(fields.capability) <= 256, 'Capability byte limit');
    if (Object.hasOwn(fields, 'version')) assert.ok(Buffer.byteLength(fields.version) <= 256, 'Version byte limit');
  }
  const effects = expect.consumption.effects ?? [];
  for (const row of [...stubs, ...expect.calls, ...expect.outcome.trace, ...expect.outcome.effects, ...effects]) checkText(row);
  for (const error of [expect.outcome.error, expect.consumption.error]) {
    if (!error) continue;
    assert.ok(Buffer.byteLength(error.path) <= 1024, 'Error path byte limit');
    if (error.cause) assert.ok(Buffer.byteLength(error.cause.path) <= 1024, 'Cause path byte limit');
  }
}

function pinned(name) {
  const bytes = read(name + '.json');
  assert.equal(digest(bytes), read(name + '.sha256').toString('ascii').trim(), name + ' byte pin');
  return bytes;
}

const fixtureNames = new Set([...groups.values()].flat());
assert.equal(fixtureNames.size, revision === '2' ? 15 : 21 + 6);
const pinnedBytes = new Map([...fixtureNames].map(name => [name, pinned(name)]));

function withFormat(bytes, format) {
  return format === undefined ? bytes : canonicalBytes({ ...JSON.parse(bytes), format });
}

function checkTypes(document, dcm, entrypoint) {
  const { stubs, given, expect } = document;
  const type = (name, value, path) => assert.doesNotThrow(() => validateValue(dcm, name, value, path, 'SCENARIO_INVALID'), path);
  type(entrypoint.input, given.input, 'Scenario input');
  for (const row of [expect.outcome, expect.consumption]) {
    if (Object.hasOwn(row, 'result')) type(entrypoint.output, row.result, 'Scenario result');
  }
  for (const row of [...stubs, ...expect.calls]) {
    assert.ok(Object.hasOwn(dcm.capabilities, row.capability), 'Unknown scenario capability');
    const capability = dcm.capabilities[row.capability];
    assert.equal(row.version, capability.version, 'Scenario capability version');
    for (const [binding, args] of [['agent', row.args], ['host', row.protectedArgs]]) {
      const params = Object.entries(capability.parameters).filter(([, param]) => param.binding === binding);
      for (const name of Object.keys(args)) {
        assert.ok(Object.hasOwn(capability.parameters, name) && capability.parameters[name].binding === binding, 'Unknown scenario argument ' + name);
      }
      for (const [name, param] of params) {
        if (param.optional && !Object.hasOwn(args, name)) continue;
        type(param.type, args[name], 'Scenario argument ' + name);
      }
    }
    if (!row.response) continue;
    const response = row.response;
    const write = capability.effect === 'WRITE';
    assert.equal(Object.hasOwn(response, 'effect'), write, 'Scenario stub write evidence');
    if (response.kind === 'SUCCESS') type(capability.result, response.value, 'Scenario stub result');
    if (response.kind === 'BUSINESS_REJECTION') assert.ok(capability.errors.includes(response.code), 'Undeclared scenario stub error');
  }
}

const dispatches = outcome => outcome.trace.filter(event => event.event === 'CALL_DISPATCHED');
const target = ({ step, capability }) => ({ step, capability });
const stubCall = ({ response, ...call }) => call;
const reportInventory = ({ trace, ...outcome }) => ({ status: 'REPORTED', ...outcome });

function checkSemantics(document, artifact, dcm, report) {
  const { expect, stubs } = document;
  const dispatched = dispatches(expect.outcome);
  assert.deepEqual(stubs.map(stubCall), expect.calls, 'Scenario stubs and expected calls');
  assert.deepEqual(dispatched.map(target), expect.calls.map(target), 'Scenario expected dispatch order');
  for (const guard of expect.outcome.trace.filter(event => event.event === 'GUARD_EVALUATED' && !event.taken)) {
    assert.ok(!dispatched.some(call => call.step === guard.step), 'Scenario expected dispatch with a false guard');
  }
  const writes = expect.calls.filter(call => dcm.capabilities[call.capability].effect === 'WRITE').map(target);
  const intents = expect.outcome.trace.filter(event => event.event === 'EFFECT_INTENT');
  const receipts = expect.outcome.trace.filter(event => event.event === 'EFFECT_RECORDED');
  assert.deepEqual(intents.map(target), writes, 'Scenario expected write intent');
  assert.deepEqual(receipts.map(target), writes, 'Scenario expected write receipt targets');
  assert.deepEqual(receipts.map(({ step, capability, state }) => ({ step, capability, state })), expect.outcome.effects, 'Scenario expected write receipts');
  for (const intent of intents) {
    const dispatch = dispatched.find(event => event.step === intent.step);
    const receipt = receipts.find(event => event.step === intent.step);
    assert.ok(intent.sequence < dispatch?.sequence && dispatch.sequence < receipt?.sequence, 'Scenario write event order');
  }
  for (const call of expect.calls) {
    const step = artifact.steps.find(step => step.id === call.step);
    assert.ok(step?.op === 'CALL' && step.capability === call.capability, 'Scenario expected artifact step');
  }
  const consumption = report(expect.outcome);
  if (report === consumeDecision) {
    assert.ok(['ALLOWED', 'DENIED', 'BLOCKED'].includes(consumption.status), 'Invalid scenario expected decision');
  } else {
    assert.equal(consumption.status, 'REPORTED', 'Invalid scenario expected outcome');
  }
  assert.deepEqual(consumption, expect.consumption, 'Scenario expected consumer report');
}

async function run(entry) {
  const captures = [];
  let phase = 'lexical';
  let adapterMismatch;
  try {
    if (entry.lexicalError) throw entry.lexicalError;
    const document = entry.document;
    phase = 'structural';
    assert.equal(entry.schemaValid, true, 'Scenario schema');
    phase = 'byte-fields';
    byteFields(document);
    if (entry.expected === 'accept-byte-fields') return { phase: 'accept-byte-fields', dispatchCount: captures.length };

    phase = 'compatibility';
    const pair = groups.get(entry.group);
    assert.ok(pair, 'No trusted scenario row');
    const [artifactName, modelName] = pair;
    const artifactBytes = withFormat(pinnedBytes.get(artifactName), entry.vector?.artifactFormat);
    const dcmBytes = withFormat(pinnedBytes.get(entry.vector?.model ?? modelName), entry.vector?.modelFormat);
    const dcm = JSON.parse(dcmBytes);
    const artifact = JSON.parse(artifactBytes);
    const requestKey = Object.freeze(Object.create(null));
    const inventory = entry.group.startsWith('inventory');
    const guard = dcm.entrypoints[artifact.entrypoint]?.consumer.kind === 'DECISION';
    const event = Object.freeze(guard ? { id: 'attempt1', source: 'scope1' } : { id: 'event1', source: inventory ? 'item1' : 'task1' });
    const report = guard ? consumeDecision : inventory || entry.group.startsWith('guarded-result') ? reportInventory : consumeEffect;
    const expiresAtMs = 10_000;
    let now = 0;
    let fixture;
    const implementations = Object.fromEntries(Object.entries(dcm.capabilities).map(([capability, registration]) => [capability, {
      version: registration.version,
      mode: 'simulation',
      keyType: dcm.hostKey.type,
      invoke(args, protectedArgs, remainingMs, context) {
        const index = captures.push(null) - 1;
        const stub = document.stubs[index];
        try {
          assert.ok(stub, 'Unexpected native dispatch');
          const capture = structuredClone({ capability, version: registration.version, args, protectedArgs });
          captures[index] = capture;
          const { step, response, ...wanted } = stub;
          assert.deepEqual(capture, wanted, 'Native stub request');
          assert.equal(context.requestKey, requestKey, 'Native opaque key');
          assert.deepEqual(context.event, event, 'Native trusted event');
          assert.equal(context.deadline.expiresAtMs, expiresAtMs, 'Native trusted deadline');
          assert.equal(context.deadline.clock.nowMs(), now, 'Native trusted clock');
          assert.equal(remainingMs, expiresAtMs - now, 'Native remaining budget');
        } catch (error) {
          adapterMismatch ??= error;
          throw error;
        }
        const write = registration.effect === 'WRITE';
        if (fixture === 'read-throws' && index === 0 && !write) throw new Error('Fixture READ failure');
        if (fixture === 'lost-response' && write) throw new Error('Fixture lost WRITE response');
        if (fixture === 'response-expired' && write) now = expiresAtMs;
        if (fixture === 'read-expired' && !write) now = expiresAtMs;
        if (fixture === 'read-malformed' && !write) return { kind: 'SUCCESS', value: { restricted: 'false' } };
        return structuredClone(stub.response);
      },
    }]));
    const binding = bindModel(dcmBytes, implementations);
    assert.equal(binding.diagnostic, null, 'Scenario DCM compatibility: ' + JSON.stringify(binding.diagnostic));
    const preparation = prepareProgram(artifactBytes, binding.boundModel);
    assert.equal(preparation.diagnostic, null, 'Scenario artifact compatibility: ' + JSON.stringify(preparation.diagnostic));

    phase = 'fixture';
    fixture = document.given.fixture;
    assert.ok(fixtures.has(fixture), 'Unknown trusted scenario fixture');
    phase = 'typing';
    checkTypes(document, dcm, dcm.entrypoints[artifact.entrypoint]);
    phase = 'semantics';
    checkSemantics(document, artifact, dcm, report);

    phase = 'execution';
    assert.equal(entry.expected, 'accept', 'Rejection vector passed every preflight phase');
    assert.equal(captures.length, 0, 'Preflight dispatched a native stub');
    const outcome = await invokeProgram(preparation.prepared, document.given.input, {
      bindings: withoutBindings.has(entry.group) ? {} : { actorId: 'user1' },
      deadline: { clock: { nowMs: () => now }, expiresAtMs },
      requestKey, event,
    });
    if (adapterMismatch) throw adapterMismatch;
    const dispatched = dispatches(outcome);
    assert.equal(captures.length, document.stubs.length, 'Not all scenario stubs were consumed');
    assert.equal(dispatched.length, captures.length, 'Trace and native dispatch counts differ');
    const calls = captures.map((call, index) => {
      assert.equal(call.capability, dispatched[index].capability, 'Trace and native dispatch capabilities differ');
      return { step: dispatched[index].step, ...call };
    });
    const consumption = report(outcome);
    assert.deepEqual({ outcome, calls, consumption }, document.expect, 'Scenario runtime report');
    return {
      phase: 'accept',
      dispatchCount: captures.length,
      report: { group: entry.group, id: document.id, calls, outcome, consumption },
    };
  } catch (error) {
    if (!(error instanceof assert.AssertionError) || phase !== entry.expected) {
      throw new Error(entry.label + ' at ' + phase + ': ' + error.message, { cause: error });
    }
    assert.equal(captures.length, 0, 'Rejected scenario dispatched a native stub');
    return { phase, dispatchCount: captures.length };
  }
}

const scenarios = [];
for (const group of [...groups.keys()].sort()) {
  for (const file of readdirSync(new URL('scenarios/' + group + '/', corpus)).sort()) {
    if (!file.endsWith('.json')) continue;
    const label = group + '/' + file;
    scenarios.push({ group, bytes: read('scenarios/' + label), label, expected: 'accept' });
  }
}
const vectors = JSON.parse(read('scenario-vectors.json')).vectors.map(vector => {
  const group = vector.scenario?.split('/')[0];
  if (vector.scenario) assert.ok(groups.has(group), 'Missing vector artifact row');
  if (vector.model) assert.ok(pinnedBytes.has(vector.model), 'Missing vector model pin');
  return { vector, group, bytes: vectorBytes(vector), label: vector.id, expected: vector.expect };
});
assert.equal(scenarios.length, revision === '2' ? 28 : 39 + 15);
assert.equal(vectors.length, 51);
const forged = JSON.parse(read('scenarios/main/open.json'));
const receipt = { ...target(forged.expect.calls[0]), state: 'CONFIRMED' };
forged.expect.outcome.trace.splice(-1, 0, { event: 'EFFECT_RECORDED', ...receipt });
forged.expect.outcome.trace.forEach((event, sequence) => { event.sequence = sequence; });
forged.expect.outcome.effects = [receipt];
forged.expect.consumption.effects = [receipt];
const forgedReadReceipt = { group: 'main', bytes: Buffer.from(JSON.stringify(forged)), label: 'receipt-for-read', expected: 'semantics' };
const falseGuard = JSON.parse(read('scenarios/main/normal.json'));
const guard = falseGuard.expect.outcome.trace.find(event => event.event === 'GUARD_EVALUATED' && event.step === 'created');
assert.equal(guard.taken, true);
guard.taken = false;
const dispatchAfterFalseGuard = { group: 'main', bytes: Buffer.from(JSON.stringify(falseGuard)), label: 'dispatch-after-false-guard', expected: 'semantics' };
structural([...scenarios, ...vectors, forgedReadReceipt, dispatchAfterFalseGuard], process.argv[2] ?? 'python');
for (const invalid of [forgedReadReceipt, dispatchAfterFalseGuard]) {
  assert.deepEqual(await run(invalid), { phase: 'semantics', dispatchCount: 0 });
}

const scenarioReports = [];
const ids = new Set();
for (const scenario of scenarios) {
  if (!scenario.schemaValid) continue;
  const key = scenario.group + '/' + scenario.document.id;
  assert.ok(!ids.has(key), 'Duplicate scenario id ' + key);
  ids.add(key);
}
for (const scenario of scenarios) {
  const { report } = await run(scenario);
  scenarioReports.push(report);
}
const vectorReports = [];
for (const entry of vectors) {
  const { phase, dispatchCount } = await run(entry);
  assert.equal(phase, entry.expected, entry.label);
  vectorReports.push({ id: entry.label, phase, dispatchCount });
}
process.stdout.write(JSON.stringify({ scenarios: scenarioReports, vectors: vectorReports }) + '\n');
