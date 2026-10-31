import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { fixtures, loadScenarios, runProfile, runCorpus } from './run-node.mjs';

test('independent model pins and six exact compiler artifacts remain selected', () => {
  for (const profile of ['read', 'effect']) {
    const fixture = fixtures[profile];
    assert.equal(`sha256:${createHash('sha256').update(fixture.dcmBytes).digest('hex')}`, fixture.dcmDigest);
    for (const program of Object.values(fixture.programs)) assert.equal(`sha256:${createHash('sha256').update(program.bytes).digest('hex')}`, program.digest);
  }
  assert.equal(fixtures.effect.dcmBytes.length, 1534);
  assert.equal(fixtures.effect.dcmDigest, 'sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04');
});
test('full generic scenarios compare independent outcomes, ordered calls, traces and consumption', async t => {
  for (const profile of ['read', 'effect']) {
    const rows = loadScenarios(profile), before = JSON.stringify(rows);
    for (const row of rows) await t.test(`${profile}/${row.id}`, async () => {
      assert.deepEqual(await runProfile(profile, [row]), [{ id: row.id, ...row.expect }]);
    });
    assert.equal(JSON.stringify(rows), before);
    assert.deepEqual(await runProfile(profile), rows.map(row => ({ id: row.id, ...row.expect })));
  }
});
test('fixture applicability and all fixed program variants remain explicit', () => {
  const common = ['normal', 'invocation-denied', 'call-denied', 'wrong-scope', 'read-throws'];
  for (const profile of ['read', 'effect']) {
    const rows = loadScenarios(profile), fixturesUsed = new Set(rows.map(row => row.given.fixture));
    for (const fixture of common) assert.ok(fixturesUsed.has(fixture));
    assert.deepEqual([...new Set(rows.map(row => row.given.program))].sort(), Object.keys(fixtures[profile].programs).sort());
    for (const row of rows) assert.deepEqual(Object.keys(row.given.input), ['taskId']);
  }
  for (const fixture of ['zero-effect', 'revoked-after-read', 'invalid-key', 'wrong-origin', 'lost-response', 'response-expired']) {
    assert.ok(loadScenarios('effect').some(row => row.given.fixture === fixture));
    assert.ok(!loadScenarios('read').some(row => row.given.fixture === fixture));
  }
});
test('closed corpus cannot configure paths, protected authority or executable payload', async () => {
  for (const mutate of [
    row => { row.given.program = '../artifact'; }, row => { row.given.fixture = '../private'; },
    row => { row.given.requestKey = 'chosen'; }, row => { row.given.input.title = 'chosen'; },
    row => { row.plugin = 'custom'; }, row => { row.stubs[0].invoke = 'executable'; },
    row => { row.stubs[1].args.sourceTaskId = 'task1'; }, row => { row.stubs[0].response.effect = 'NONE'; },
    row => { row.stubs[1].response.effect = 'NONE'; }, row => { row.stubs[0].response.value.extra = true; },
  ]) { const row = structuredClone(loadScenarios('effect')[0]); mutate(row); await assert.rejects(runProfile('effect', [row])); }
});
test('missing, extra, wrong-order, wrong-step and mismatched stubs fail independently of runtime handling', async () => {
  for (const mutate of [
    row => { row.stubs = []; }, row => { row.stubs.reverse(); }, row => { row.stubs.push(structuredClone(row.stubs[1])); },
    row => { row.stubs[0].step = 'different'; }, row => { row.stubs[0].args.taskId = 'other'; },
    row => { row.stubs[0].protectedArgs.actorId = 'other'; }, row => { row.stubs[0].response.value.id = 'wrong-resource'; },
  ]) { const row = structuredClone(loadScenarios('effect')[0]); mutate(row); await assert.rejects(runProfile('effect', [row]), /simulation violations/i); }
  const hidden = structuredClone(loadScenarios('effect').find(row => row.id === 'read-throws'));
  hidden.given.fixture = 'normal'; hidden.stubs[0].args.taskId = 'other'; await assert.rejects(runProfile('effect', [hidden]), /simulation violations/i);
});
test('repeated identical READ calls derive step labels from dispatch evidence', async () => {
  const row = structuredClone(loadScenarios('read').find(row => row.id === 'repeated'));
  [row.stubs[0].step, row.stubs[1].step] = [row.stubs[1].step, row.stubs[0].step];
  await assert.rejects(runProfile('read', [row]), /simulation violations/i);
});
test('changed expectations, duplicate IDs and excessive rows are rejected', async () => {
  const row = structuredClone(loadScenarios('effect')[0]); row.expect.outcome.trace[9].taken = false;
  await assert.rejects(runProfile('effect', [row]));
  const normal = loadScenarios('read')[0]; await assert.rejects(runProfile('read', [normal, structuredClone(normal)]));
  const rows = Array.from({ length: 129 }, (_, index) => ({ ...structuredClone(normal), id: `row${index}` }));
  await assert.rejects(runProfile('read', rows), /CAPACITY_EXCEEDED/);
});
test('bounded source CLI emits the complete batch and accepts no scenario configuration arguments', async () => {
  const path = fileURLToPath(new URL('run-node.mjs', import.meta.url));
  const result = spawnSync(process.execPath, [path], { encoding: 'utf8', timeout: 30_000, maxBuffer: 1_048_576 });
  assert.equal(result.error, undefined); assert.equal(result.status, 0, result.stderr); assert.equal(result.stderr, '');
  assert.deepEqual(JSON.parse(result.stdout), await runCorpus());
  const bad = spawnSync(process.execPath, [path, 'unexpected'], { encoding: 'utf8', timeout: 30_000, maxBuffer: 1_048_576 });
  assert.equal(bad.status, 1); assert.equal(bad.stdout, ''); assert.match(bad.stderr, /accepts no arguments/);
  const source = readFileSync(new URL('../../examples/node-host/generic.mjs', import.meta.url), 'utf8');
  assert.match(source, /from ['"]@keir\/runtime-node['"]/); assert.doesNotMatch(source, /runtime-node\/src/);
});
