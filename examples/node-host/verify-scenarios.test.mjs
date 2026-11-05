import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { decodeScenario, prepareScenario, verifyBatch, verifyGroup } from './verify-scenarios.mjs';

const python = process.env.KEIR_SCENARIO_PYTHON ?? 'python';
const corpus = new URL('../../conformance/keir-1/', import.meta.url);
const fixture = path => readFileSync(new URL(path, corpus));
const read = path => fixture('scenarios/' + path);

test('installed package verifies the eight frozen scenario groups', async () => {
  const groups = ['main', 'zero', 'after-write-read', 'no-call', 'handler', 'title', 'title-max', 'title-composed'];
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

test('the example enforces the first failure for all frozen raw vectors', async () => {
  const { vectors } = JSON.parse(fixture('scenario-vectors.json'));
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
        testPair = [Buffer.from(artifact.replace('keir-artifact/1', vector.artifactFormat ?? 'keir-artifact/1')),
          Buffer.from(model.replace('keir-dcm/1', vector.modelFormat ?? 'keir-dcm/1'))];
      }
      assert.throws(() => prepareScenario(group, bytes, python, testPair), errors[vector.expect], vector.id);
    }
  }
});
