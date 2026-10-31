import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { runScenarios } from './run.mjs';
import { runEffectScenarios } from './effect.mjs';
import { runGeneric } from './generic.mjs';

test('packed Node runtime consumes the Java artifact and displays only validated completed results', async () => {
  const bytes = readFileSync(process.env.KEIR_ARTIFACT_FILE ?? new URL('../../conformance/read-and-decide/artifact.json', import.meta.url));
  const outputs = await runScenarios(bytes, 'sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424', 'sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f');
  assert.deepEqual(outputs.map(item => item.consumption.status), ['DISPLAYED', 'DISPLAYED', 'NOT_DISPLAYED', 'NOT_DISPLAYED']);
  assert.equal(outputs[0].consumption.result.classification, 'DONE');
  assert.equal(outputs[1].consumption.result.classification, 'PENDING');
  assert.equal(outputs[2].outcome.error.code, 'TASK_NOT_FOUND');
  assert.equal(outputs[3].outcome.error.code, 'ADAPTER_EXCEPTION');
  for (const item of outputs) {
    assert.deepEqual(item.calls, [{ agentArgs: { taskId: 'task1' }, protectedArgs: { actorId: 'user1' } }]);
    assert.doesNotMatch(JSON.stringify(item.outcome.trace), /task1|user1/);
  }
});

test('installed bound-model runtime matches both full shared /2 fixture batches', async () => {
  for (const profile of ['read', 'effect']) {
    const directory = new URL(`../../conformance/generic-hooks/${profile}/`, import.meta.url);
    const scenarios = JSON.parse(readFileSync(new URL('scenarios.json', directory), 'utf8'));
    assert.deepEqual(await runGeneric(fileURLToPath(directory)), scenarios.map(row => ({ id: row.id, ...row.expect })));
  }
});

test('installed effect runtime executes the complete independent scenario/consumer batch', async () => {
  const fixture = new URL('../../conformance/single-effect/', import.meta.url);
  const rows = await runEffectScenarios(readFileSync(new URL('artifact.json', fixture)),
    readFileSync(new URL('artifact.sha256', fixture), 'utf8').trim(), readFileSync(new URL('dcm.sha256', fixture), 'utf8').trim());
  const scenarios = JSON.parse(readFileSync(new URL('scenarios.json', fixture), 'utf8'));
  assert.deepEqual(rows, scenarios.map(row => ({ id: row.id, ...row.expect })));
  const confirmedFailure = rows.find(row => row.id === 'confirmed-return-limit');
  assert.equal(confirmedFailure.consumption.classification, 'LIMIT_EXCEEDED');
  assert.equal(confirmedFailure.consumption.effects[0].state, 'CONFIRMED');
  assert.equal(Object.hasOwn(confirmedFailure.consumption, 'result'), false);
  assert.equal(rows.find(row => row.id === 'write-rejected').consumption.classification, 'BUSINESS_REJECTION');
  assert.equal(rows.find(row => row.id === 'lost-response').consumption.classification, 'UNCERTAIN');
});
