import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as api from '../../runtime-node/src/index.js';
import { fixtures, loadScenarios } from '../../conformance/generic-hooks/run-node.mjs';
import { consumeGeneric, runBatch } from './generic-simulation.mjs';

const load = (profile, name) => JSON.parse(readFileSync(new URL(`../../conformance/generic-hooks/${profile}/${name}.json`, import.meta.url)));
test('generic consumers reject forged typed output and trace data without changing legacy consumers', () => {
  for (const profile of ['read', 'effect']) {
    const dcm = load(profile, 'dcm'), completed = load(profile, 'scenarios')[0].expect.outcome;
    const invalid = profile === 'effect' ? 'NOT_REPORTED' : 'NOT_DISPLAYED';
    for (const mutate of [
      value => { value.result.extra = true; }, value => { value.result[profile === 'effect' ? 'status' : 'classification'] = 'FORGED'; },
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
