import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { loadCorpus, validateCase, runCase, consume } from './run-node.mjs';

const corpus = loadCorpus();
const success = corpus.cases.find(row => row.expected.runs[0]?.outcome.classification === 'COMPLETED');
assert.ok(success, 'Corpus needs a completed scenario for harness regression tests');

test('primary Java artifact bytes and golden repository text remain unchanged', () => {
  const hashes = {
    'source.yaml': '602750580d3f320e9eb62de803cb62e93979a7ae30296cbd5d6d0cc42da3c76f',
    'dcm.json': 'a1dc9a4d9ec0e62bfd38b1e64ff15ef355903262f3a0db646f038495fbe06ac0',
    'artifact.json': '7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424',
    'artifact.sha256': '6ec6f4e1f62bd411bd510067d0b25380e09c83b52d7f1630323c1046e3f406ef',
  };
  for (const [file, hash] of Object.entries(hashes)) {
    const bytes = readFileSync(new URL(file, import.meta.url));
    const repositoryBytes = file === 'artifact.json' ? bytes : bytes.toString('utf8').replaceAll('\r\n', '\n');
    assert.equal(createHash('sha256').update(repositoryBytes).digest('hex'), hash, file);
  }
});

test('shared scenarios assert entire results, ordered calls, exact traces and consumption', async t => {
  const before = JSON.stringify(corpus);
  for (const row of corpus.cases) await t.test(row.id, () => runCase(row));
  assert.equal(JSON.stringify(corpus), before, 'Execution must not mutate fixture controls or expectations');
});

test('batch CLI emits exactly the shared result transport and rejects configuration arguments', () => {
  const command = fileURLToPath(new URL('run-node.mjs', import.meta.url));
  const options = { encoding: 'utf8', timeout: 30000 };
  const batch = spawnSync(process.execPath, [command], options);
  assert.equal(batch.error, undefined); assert.equal(batch.status, 0, batch.stderr);
  assert.equal(batch.stderr, '');
  assert.deepEqual(JSON.parse(batch.stdout), corpus.cases.map(row => ({ id: row.id, ...row.expected })));
  const invalid = spawnSync(process.execPath, [command, 'unexpected'], options);
  assert.equal(invalid.error, undefined); assert.equal(invalid.status, 1);
  assert.equal(invalid.stdout, ''); assert.match(invalid.stderr, /Runner accepts no arguments/);
});

test('fixture assertions reject ambiguous stubs, unknown controls and inconsistent runs', () => {
  for (const mutate of [
    row => { row.unknown = true; },
    row => { row.prepare = { plugin: 'unexpected' }; },
    row => { row.runs[0].calls[0].throw = true; },
    row => { delete row.runs[0].calls[0].response; },
    row => { row.runs[0].authorizations[0].action = 'MAYBE'; },
    row => { row.runs[0].authorizations[0].atMs = 0.5; },
    row => { row.expected.runs = []; },
    row => { row.expected.prepare = { status: 'REJECTED', code: 'ADMISSION_INVALID', path: '/admission' }; },
  ]) {
    const row = structuredClone(success); mutate(row);
    assert.throws(() => validateCase(row));
  }
});

test('callback mismatch fails outside runtime even when its safe failure matches the expectation', async () => {
  const row = structuredClone(success);
  row.runs[0].calls = [];
  row.expected.runs[0].outcome = {
    classification: 'FAILED', error: { code: 'ADAPTER_EXCEPTION', path: '/steps/0' },
    trace: [
      { sequence: 0, event: 'INVOCATION_START' }, { sequence: 1, event: 'STEP_START', step: 'source' },
      { sequence: 2, event: 'CALL_DISPATCHED', step: 'source', capability: 'tasks.get' },
      { sequence: 3, event: 'TERMINATED', code: 'ADAPTER_EXCEPTION' },
    ],
  };
  row.expected.runs[0].consumption = { status: 'NOT_DISPLAYED', code: 'ADAPTER_EXCEPTION' };
  await assert.rejects(runCase(row), /harness violations/);
});

test('missing, unused, out-of-order and mismatched callbacks cannot silently pass', async () => {
  for (const mutate of [
    row => { row.runs[0].authorizations = []; },
    row => { row.runs[0].authorizations.reverse(); },
    row => { row.runs[0].authorizations.push(structuredClone(row.runs[0].authorizations[1])); },
    row => { row.runs[0].calls.push(structuredClone(row.runs[0].calls[0])); },
    row => { row.runs[0].calls[0].request.protectedArgs.actorId = 'different-fixture-user'; },
  ]) {
    const row = structuredClone(success); mutate(row);
    await assert.rejects(runCase(row), /harness violations/);
  }
});

test('full-result comparison rejects changed event fields instead of projecting them away', async () => {
  const row = structuredClone(success);
  row.expected.runs[0].outcome.trace[0].extra = 'unexpected';
  await assert.rejects(runCase(row), /conformance mismatch/);
});

test('DISPLAY validates the complete result shape independently', () => {
  for (const result of [null, false, [], {}, { classification: 'UNKNOWN' }, { classification: 'DONE', extra: true }]) {
    assert.deepEqual(consume({ classification: 'COMPLETED', result }), { status: 'NOT_DISPLAYED', code: 'OUTPUT_INVALID' });
  }
  for (const classification of ['DONE', 'PENDING']) {
    assert.deepEqual(consume({ classification: 'COMPLETED', result: { classification } }), {
      status: 'DISPLAYED', result: { classification },
    });
  }
});
