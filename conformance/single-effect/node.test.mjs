import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import * as api from '../../runtime-node/src/index.js';
import { bytes, pins, loadScenarios, runCorpus } from './run-node.mjs';
import { runScenario, validateScenario, reconcileCalls } from '../../examples/node-host/effect-simulation.mjs';

const scenarios = loadScenarios();
const created = scenarios.find(row => row.id === 'created');
const run = row => runScenario(api, bytes, pins, row);

test('independent primary effect pins remain fixed', () => {
  assert.equal(bytes.length, 5439);
  assert.equal('sha256:' + createHash('sha256').update(bytes).digest('hex'), 'sha256:d441413373a59a0a40a509143146fef8fe9c1c2426bee430965efddee591f31c');
  assert.equal(pins.artifact, 'sha256:d441413373a59a0a40a509143146fef8fe9c1c2426bee430965efddee591f31c');
  assert.equal(pins.dcm, 'sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77');
});

test('hand-authored shared effect scenarios compare full outcomes, calls, traces and receipts', async t => {
  const before = JSON.stringify(scenarios);
  for (const row of scenarios) await t.test(row.id, () => run(row));
  assert.equal(JSON.stringify(scenarios), before);
});

test('source-runtime batch CLI matches the full transport without an example-host installation', () => {
  const path = fileURLToPath(new URL('run-node.mjs', import.meta.url));
  const result = spawnSync(process.execPath, [path], { encoding: 'utf8', timeout: 30000 });
  assert.equal(result.error, undefined); assert.equal(result.status, 0, result.stderr); assert.equal(result.stderr, '');
  assert.deepEqual(JSON.parse(result.stdout), scenarios.map(row => ({ id: row.id, ...row.expect })));
  const bad = spawnSync(process.execPath, [path, 'unexpected'], { encoding: 'utf8', timeout: 30000 });
  assert.equal(bad.error, undefined); assert.equal(bad.status, 1); assert.equal(bad.stdout, ''); assert.match(bad.stderr, /accepts no arguments/);
});

test('closed scenarios cannot configure protected host authority, plugins, paths or malformed responses', () => {
  for (const mutate of [
    row => { row.given.requestKey = 'chosen'; }, row => { row.given.fixture = '../../private'; },
    row => { row.given.grants = {}; }, row => { row.plugin = 'custom'; },
    row => { row.stubs[0].response.effect = 'NONE'; }, row => { row.stubs[1].response.effect = 'NONE'; },
    row => { row.stubs[0].response.value.extra = true; }, row => { row.stubs[1].throw = true; },
    row => { row.stubs[0].response = { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' }; },
    row => { row.stubs[0].response = { kind: 'BUSINESS_REJECTION', code: 'UNDECLARED' }; },
    row => { row.given.input.title = 'x'.repeat(257); }, row => { row.given.input.title = '\ud800'; },
    row => { row.stubs = [...row.stubs, ...row.stubs, row.stubs[0]]; },
    row => { row.expect.consumption.classification = 'COMPLETED_BUT_UNCHECKED'; },
  ]) { const row = structuredClone(created); mutate(row); assert.throws(() => validateScenario(row)); }
});

test('missing, extra, wrong-order, wrong-step and mismatched simulation stubs fail outside runtime', async () => {
  for (const mutate of [
    row => { row.stubs = []; }, row => { row.stubs.reverse(); },
    row => { row.stubs.push(structuredClone(row.stubs[1])); },
    row => { row.stubs[0].step = 'different'; }, row => { row.stubs[0].args.taskId = 'other'; },
    row => { row.stubs[0].protectedArgs.actorId = 'other'; },
    row => { row.stubs[0].response.value.id = 'wrong-resource'; },
  ]) { const row = structuredClone(created); mutate(row); await assert.rejects(run(row), /simulation violations/); }
});

test('safe runtime adapter failure cannot hide a mismatch even if the expected outcome agrees', async () => {
  const row = structuredClone(scenarios.find(row => row.id === 'read-unavailable'));
  row.given.fixture = 'normal'; row.stubs[0].args.taskId = 'mismatch';
  await assert.rejects(run(row), /simulation violations/);
});

test('identical read arguments at distinct sites derive labels only from dispatch evidence', () => {
  const call = { capability: 'tasks.get', version: '1', args: { taskId: 'task1' }, protectedArgs: { actorId: 'user1' } };
  const trace = ['first', 'second'].map((step, sequence) => ({ sequence, event: 'CALL_DISPATCHED', step, capability: 'tasks.get' }));
  const stubs = ['second', 'first'].map(step => ({ step, ...structuredClone(call) }));
  const violations = [];
  const actual = reconcileCalls([call, structuredClone(call)], trace, stubs, violations);
  assert.deepEqual(actual.map(row => row.step), ['first', 'second']);
  assert.deepEqual(violations, ['Dispatch step mismatch', 'Dispatch step mismatch']);
  for (const [captures, events] of [[[call], trace], [[call], [{ ...trace[0], capability: 'tasks.other' }]]]) {
    const failed = []; reconcileCalls(captures, events, stubs, failed); assert.ok(failed.length > 0);
  }
});

test('full result comparison rejects changed guard evidence and duplicate batch IDs', async () => {
  const row = structuredClone(created); row.expect.outcome.trace[7].taken = false;
  await assert.rejects(run(row), /simulation result mismatch/);
  await assert.rejects(runCorpus([created, structuredClone(created)]), /Duplicate scenario ID/);
  const source = readFileSync(new URL('../../examples/node-host/effect.mjs', import.meta.url), 'utf8');
  assert.match(source, /from '@keir\/runtime-node'/); assert.doesNotMatch(source, /runtime-node\/src/);
});
