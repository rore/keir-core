import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import * as api from '../../runtime-node/src/index.js';
import { consumeDecision } from './decision-consumer.mjs';
import { consumeEffect } from './effect-consumer.mjs';

const root = new URL('../../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');

test('pre-operation teaching model matches the shared guard model', () => {
  assert.deepEqual(JSON.parse(read('examples/node-host/pre-operation/dcm.json')),
    JSON.parse(read('conformance/keir-3/dcm-guard.json')));
});

test('installed runtime guards native continuation in the pre-operation command', () => {
  for (const args of [[], ['conformance/keir-3/artifact-guard.json']]) {
    const output = execFileSync(process.execPath, ['examples/node-host/pre-operation/run.mjs', ...args], {
      cwd: root, encoding: 'utf8', timeout: 10_000,
    });
    assert.deepEqual(output.trimEnd().split(/\r?\n/), [
      'allow ALLOWED | native attempts=1',
      'deny DENIED | native attempts=0 | Operation restricted.',
      'unavailable BLOCKED | native attempts=0',
    ]);
  }
});

test('mixed catalog applies the selected entrypoint consumer and write grant', async () => {
  const modelBytes = read('conformance/keir-3/dcm-mixed.json'), model = JSON.parse(modelBytes);
  const calls = [];
  const registration = { version: '1', mode: 'simulation', keyType: model.hostKey.type };
  const binding = api.bindModel(Buffer.from(modelBytes), {
    'policy.get': { ...registration, invoke: () => {
      calls.push('READ');
      return { kind: 'SUCCESS', value: { restricted: false } };
    } },
    'native.write': { ...registration, invoke: () => {
      calls.push('WRITE');
      return { kind: 'SUCCESS', value: { restricted: false }, effect: 'CONFIRMED' };
    } },
  });
  assert.equal(binding.diagnostic, null);
  for (const [name, expected, call, effects] of [
    ['event', 'REPORTED', 'WRITE', 1], ['guard', 'ALLOWED', 'READ', 0],
  ]) {
    const bytes = Buffer.from(read(`conformance/keir-3/artifact-mixed-${name}.json`));
    const artifact = JSON.parse(bytes);
    const decision = model.entrypoints[artifact.entrypoint].consumer.kind === 'DECISION';
    const preparation = api.prepareProgram(bytes, binding.boundModel);
    assert.equal(preparation.diagnostic, null);
    calls.length = 0;
    const outcome = await api.invokeProgram(preparation.prepared, { restricted: false }, {
      bindings: decision ? { actorId: 'user1' } : {}, requestKey: Object.freeze({}),
      event: { id: 'attempt1', source: 'scope1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 },
    });
    assert.equal(outcome.classification, 'COMPLETED');
    assert.equal(outcome.effects.length, effects);
    assert.deepEqual(calls, [call]);
    assert.equal((decision ? consumeDecision : consumeEffect)(outcome).status, expected);
  }
});
