import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as api from '../../runtime-node/src/index.js';
import { fixtures, loadScenarios } from '../../conformance/generic-hooks/run-node.mjs';
import { consumeGeneric, runBatch } from './generic-simulation.mjs';

const load = (profile, name) => JSON.parse(readFileSync(new URL(`../../conformance/generic-hooks/${profile}/${name}.json`, import.meta.url)));
test('generic consumers reject forged typed output and trace data without changing legacy consumers', () => {
  for (const profile of ['read', 'effect', 'effect3']) {
    const dcm = load(profile, 'dcm'), completed = load(profile, 'scenarios')[0].expect.outcome;
    const effect = profile !== 'read', invalid = effect ? 'NOT_REPORTED' : 'NOT_DISPLAYED';
    for (const mutate of [
      value => { value.result.extra = true; }, value => { value.result[effect ? 'status' : 'classification'] = 'FORGED'; },
      value => { value.trace[0].event = ['INVOCATION_START']; }, value => { value.trace[1].sequence = 99; },
    ]) { const value = structuredClone(completed); mutate(value); assert.equal(consumeGeneric(value, dcm).status, invalid); }
  }
});

test('host authorization independently rejects unknown operations, versions and request shapes', async () => {
  const requests = [
    { entrypoint: 'ClassifyTask', capability: 'unpublished.operation', version: '999' },
    { entrypoint: 'ClassifyTask', capability: 'tasks.get', version: '999' },
    { entrypoint: 'ClassifyTask', capability: ['tasks.get'], version: '1' },
    { entrypoint: 'ClassifyTask', version: '1' }, { entrypoint: 'ClassifyTask', extra: true },
  ];
  const wrapped = { ...api, invokeProgram: async (prepared, input, context) => {
    for (const request of requests) assert.equal(await context.authorize(request), false);
    return api.invokeProgram(prepared, input, context);
  } };
  const row = loadScenarios('read')[0];
  assert.deepEqual(await runBatch(wrapped, fixtures.read, [row]), [{ id: row.id, ...row.expect }]);
});

test('a pending batch keeps its admitted fixture snapshot when caller data changes', async () => {
  let entered, release;
  const started = new Promise(resolve => { entered = resolve; }), gate = new Promise(resolve => { release = resolve; });
  const wrapped = { ...api, invokeProgram: async (...args) => { entered(); await gate; return api.invokeProgram(...args); } };
  const original = loadScenarios('read')[0], row = structuredClone(original);
  const pending = runBatch(wrapped, fixtures.read, [row]); await started;
  row.given.input.taskId = 'changed'; row.stubs[0].args.taskId = 'changed'; row.stubs[0].response.value.id = 'changed';
  release(); assert.deepEqual(await pending, [{ id: original.id, ...original.expect }]);
});

test('effect/4 uses two-argument binding and preparation with no legacy authority knobs', async () => {
  const directory = new URL('../../conformance/generic-hooks/effect4/', import.meta.url);
  const read = name => readFileSync(new URL(name, directory));
  const scenarios = JSON.parse(read('scenarios.json'));
  const programs = Object.fromEntries(['main', 'zero', 'after-write-read', 'no-call'].map(name => {
    const stem = name === 'main' ? 'artifact' : `artifact-${name}`;
    return [name, { bytes: read(`${stem}.json`), digest: read(`${stem}.sha256`).toString('utf8').trim() }];
  }));
  const bundle = { dcmBytes: read('dcm.json'), dcmDigest: read('dcm.sha256').toString('utf8').trim(), programs };
  assert.deepEqual(await runBatch(api, bundle, scenarios), scenarios.map(row => ({ id: row.id, ...row.expect })));
  for (const mutate of [row => { row.given.scope = 'forged'; }, row => { row.given.fixture = 'wrong-scope'; }, row => { row.qualification = {}; }]) {
    const row = structuredClone(scenarios[0]); mutate(row); await assert.rejects(runBatch(api, bundle, [row]));
  }
  const readFailure = structuredClone(scenarios.find(row => row.id === 'read-throws'));
  readFailure.given.fixture = 'normal'; readFailure.stubs[0].response.value.id = 'wrong-resource';
  await assert.rejects(runBatch(api, bundle, [readFailure]), /simulation violations/);
  const wrongStep = structuredClone(scenarios[0]); wrongStep.stubs[0].step = 'wrongStep';
  await assert.rejects(runBatch(api, bundle, [wrongStep]), /simulation violations/);
});
