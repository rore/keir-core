import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { decodeScenario, prepareScenario, verifyBatch, verifyGroup } from './verify-scenarios.mjs';
import { typed } from './generic-simulation.mjs';

const python = process.env.KEIR_SCENARIO_PYTHON ?? 'python';
const corpus = new URL('../../conformance/keir-2/', import.meta.url);
const fixture = path => readFileSync(new URL(path, corpus));
const read = path => fixture('scenarios/' + path);
const groups = ['main', 'zero', 'after-write-read', 'no-call', 'handler', 'title', 'title-max', 'title-composed'];

test('fixture record byte ceiling uses canonical control-character escapes', () => {
  const names = ['a', 'b', 'c', 'd', 'e', 'f'];
  const fields = Object.fromEntries(names.map(name => [name, 'String']));
  const value = Object.fromEntries(names.map(name => [name, '\n'.repeat(256)]));
  assert.equal(Buffer.byteLength(JSON.stringify(value)), 3115);
  assert.throws(() => typed(value, 'Oversized', { format: 'keir-dcm/4', types: { Oversized: { kind: 'record', fields } } }), /Bounded fixture record/);
});

test('numeric scenario4 uses complete stock/quote consumers and rejects relabeled scenarios', async () => {
  for (const group of ['stock', 'quote']) {
    const directory = new URL('../numeric/' + group + '/', import.meta.url);
    const scenario = readFileSync(new URL('scenarios/normal.json', directory));
    const pair = [readFileSync(new URL('../../compiler-java/target/numeric-' + group + '.json', import.meta.url)),
      readFileSync(new URL('dcm.json', directory))];
    const actual = await prepareScenario(group, scenario, python, pair).run();
    assert.equal(actual.consumption.status, group === 'stock' ? 'REPORTED' : 'ALLOWED');
    const row = JSON.parse(scenario); row.format = 'keir-scenario/3';
    assert.throws(() => prepareScenario(group, Buffer.from(JSON.stringify(row)), python, pair), /Scenario schema/);
  }
});

test('scenario4 retained-authoring branch keeps nominal Integer/Decimal strings', async () => {
  const batch = JSON.parse(readFileSync(new URL('../../compiler-java/target/numeric-retained.json', import.meta.url)));
  const outcome = { classification: 'COMPLETED', result: { value: true }, effects: [], trace: [
    { sequence: 0, event: 'INVOCATION_START' }, { sequence: 1, event: 'STEP_START', step: 'value' },
    { sequence: 2, event: 'VALUE_BOUND', step: 'value' }, { sequence: 3, event: 'RETURNED' }, { sequence: 4, event: 'TERMINATED' },
  ] };
  const row = { format: 'keir-scenario/4', id: 'retained-names', given: { input: batch.input, fixture: 'normal' }, stubs: [],
    expect: { calls: [], outcome, consumption: { status: 'REPORTED', classification: 'COMPLETED', result: { value: true }, effects: [] } } };
  // This compiler-emitted batch already has canonical key order; no sibling runtime fallback.
  const pair = [Buffer.from(JSON.stringify(batch.artifact)), Buffer.from(JSON.stringify(batch.dcm))];
  assert.deepEqual(await prepareScenario('main', Buffer.from(JSON.stringify(row)), python, pair).run(), { id: row.id, ...row.expect });
});

test('installed package verifies the eight frozen scenario groups', async () => {
  let count = 0;
  for (const group of groups) count += (await verifyGroup(group, python)).length;
  assert.equal(count, 20);
});

test('preflight rejects lexical and structural errors before producing a runnable scenario', () => {
  assert.throws(() => prepareScenario('main', Buffer.from([0xff]), python), /Invalid scenario UTF-8/);
  const row = JSON.parse(read('main/normal.json'));
  row.given.program = 'main';
  assert.throws(() => prepareScenario('main', Buffer.from(JSON.stringify(row)), python), /Scenario schema.*additionalProperties/);
});

test('preflight rejects untrusted fixtures and inconsistent typed expectations', () => {
  const row = JSON.parse(read('main/normal.json'));
  row.given.fixture = 'unknown';
  const prepare = () => prepareScenario('main', Buffer.from(JSON.stringify(row)), python);
  assert.throws(prepare, /Unknown scenario fixture/);
  row.given.fixture = 'normal';
  row.stubs[0].args.taskId = false;
  assert.throws(prepare, /Bounded fixture string/);
  row.stubs[0].args.taskId = 'task1';
  row.stubs[0].protectedArgs.actorId = 'user2';
  assert.throws(prepare, /Expected protected actor/);
  row.stubs[0].protectedArgs.actorId = 'user1';
  const guard = row.expect.outcome.trace.find(event => event.event === 'GUARD_EVALUATED');
  guard.taken = false;
  assert.throws(prepare, /Expected dispatch after a false guard/);
  guard.taken = true;
  const receipt = row.expect.outcome.trace.find(event => event.event === 'EFFECT_RECORDED');
  receipt.state = 'UNKNOWN';
  assert.throws(prepare, /Expected receipt states agree with the trace/);
  receipt.state = 'CONFIRMED';
  row.stubs.reverse();
  assert.throws(prepare, /Stub and expected call order/);
  row.stubs.reverse();
  const removed = row.stubs.pop();
  assert.throws(prepare, /Stub and expected call order/);
  row.stubs.push(removed, removed);
  assert.throws(prepare, /Stub and expected call order/);
});

test('duplicate IDs reject the group before execution', async () => {
  const row = read('main/normal.json');
  await assert.rejects(verifyBatch('main', [row, row], python), /Duplicate scenario ID/);
});

function vectorBytes(vector) {
  if (Object.hasOwn(vector, 'utf8')) return Buffer.from(vector.utf8);
  if (Object.hasOwn(vector, 'base64')) return Buffer.from(vector.base64, 'base64');
  let bytes = read(vector.scenario + '.json');
  if (vector.replace) {
    const [search, template] = vector.replace;
    const replacement = vector.expand ? template.replace('@', () => vector.expand[0].repeat(vector.expand[1])) : template;
    assert.ok(bytes.includes(search), 'Missing vector target');
    bytes = Buffer.from(bytes.toString('utf8').replace(search, () => replacement));
  }
  if (vector.padToBytes) bytes = Buffer.concat([bytes, Buffer.alloc(vector.padToBytes - bytes.length, 0x20)]);
  return bytes;
}

test('the example enforces the first failure for the teaching vectors', async () => {
  const { vectors: sharedVectors } = JSON.parse(fixture('scenario-vectors.json'));
  const vectors = sharedVectors.filter(({ scenario }) => !scenario || groups.includes(scenario.split('/')[0]));
  assert.equal(vectors.length, 45);
  const errors = {
    lexical: /Scenario byte limit|scenario UTF-8|Scenario BOM|scenario token|scenario object key|scenario surrogate|scenario integer|scenario value|scenario JSON|Scenario nesting limit/,
    structural: /Scenario schema:/,
    'byte-fields': /Scenario .* byte limit/,
    fixture: /Unknown scenario fixture/,
    typing: /Bounded fixture string|Published fixture capability|Expected values to be strictly equal/,
    semantics: /Invalid expected outcome/,
    compatibility: /Scenario (model|artifact) compatibility/,
  };
  for (const vector of vectors) {
    const bytes = vectorBytes(vector);
    const group = vector.scenario?.split('/')[0];
    if (vector.expect === 'accept') {
      await prepareScenario(group, bytes, python).run();
    } else if (vector.expect === 'accept-byte-fields') {
      assert.doesNotThrow(() => decodeScenario(bytes, python), vector.id);
    } else {
      let testPair;
      if (vector.expect === 'compatibility') {
        const artifact = fixture('artifact-no-call.json').toString('utf8');
        const model = fixture((vector.model ?? 'dcm') + '.json').toString('utf8');
        testPair = [Buffer.from(artifact.replace('keir-artifact/2', vector.artifactFormat ?? 'keir-artifact/2')),
          Buffer.from(model.replace('keir-dcm/2', vector.modelFormat ?? 'keir-dcm/2'))];
      }
      assert.throws(() => prepareScenario(group, bytes, python, testPair), errors[vector.expect], vector.id);
    }
  }
});
