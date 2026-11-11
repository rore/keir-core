import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { bindModel, invokeProgram, prepareProgram } from '../src/index.js';
import { canonicalBytes } from '../src/canonical.js';

const read = name => readFileSync(new URL('../../conformance/keir-3/' + name, import.meta.url));
const cases = [
  { model: 'dcm.json', current: 'artifact.json', old: 'event.json', scenario: 'main/normal', calls: ['tasks.get', 'tasks.create'] },
  { model: 'dcm-guard.json', current: 'artifact-guard-input.json', old: 'decision.json', scenario: 'guard-input/guard-input-allow', calls: [] },
];
function binding(row, key, calls) {
  const bytes = read(row.model), model = JSON.parse(bytes);
  return bindModel(bytes, Object.fromEntries(Object.entries(model.capabilities).map(([id, cap]) => [id, {
    version: cap.version, mode: 'simulation', keyType: model.hostKey.type,
    invoke(_args, _bindings, _remaining, context) {
      assert.equal(context.requestKey, key); calls.push(id);
      if (id === 'tasks.get') return { kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'COMPLETED' } };
      assert.equal(id, 'tasks.create');
      return { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
    },
  }])));
}

test('original 0.3.0 and compiler-produced 0.3.1 event and decision artifacts execute unchanged', async () => {
  for (const row of cases) for (const [producer, path] of [
    ['0.3.0', 'compatibility/0.3.0/' + row.old], ['0.3.1', row.current],
  ]) {
    const bytes = read(path); assert.equal(JSON.parse(bytes).compiler, 'keir-compiler-java/' + producer);
    const key = Object.freeze({}), calls = [], bound = binding(row, key, calls);
    assert.equal(bound.diagnostic, null);
    const prepared = prepareProgram(bytes, bound.boundModel); assert.equal(prepared.diagnostic, null);
    const scenario = JSON.parse(read('scenarios/' + row.scenario + '.json'));
    const outcome = await invokeProgram(prepared.prepared, scenario.given.input, {
      bindings: row.calls.length ? { actorId: 'user1' } : {}, requestKey: key,
      event: { id: 'event1', source: 'task1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 },
    });
    assert.deepEqual(outcome, scenario.expect.outcome); assert.deepEqual(calls, row.calls);
  }
});

test('unknown and cross-format producers fail before any dispatch', () => {
  for (const row of cases) {
    const calls = [], bound = binding(row, Object.freeze({}), calls), original = JSON.parse(read(row.current));
    for (const compiler of ['0.3.2', '0.3.1-preview', '0.2.0', '0.3.01']) {
      const result = prepareProgram(canonicalBytes({ ...original, compiler: 'keir-compiler-java/' + compiler }), bound.boundModel);
      assert.equal(result.prepared, null); assert.equal(result.diagnostic.code, 'PIN_MISMATCH');
      assert.equal(result.diagnostic.path, '/artifact/compiler');
    }
    const result = prepareProgram(canonicalBytes({ ...original, compiler: 'keir-compiler-java/0.3.1', format: 'keir-artifact/2' }), bound.boundModel);
    assert.equal(result.prepared, null); assert.equal(result.diagnostic.code, 'PIN_MISMATCH');
    assert.equal(result.diagnostic.path, '/artifact/compiler'); assert.deepEqual(calls, []);
  }
});
