import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { prepare, invoke } from '../../runtime-node/src/index.js';

const artifact = readFileSync(new URL('artifact.json', import.meta.url));
const admission = {
  expectedArtifactDigest: 'sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424',
  expectedDcmDigest: 'sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f',
  entrypoint: 'ClassifyTask', consumer: { id: 'sample.classification', version: '1', kind: 'DISPLAY' },
  compiler: 'keir-compiler-java/0.1.0-SNAPSHOT',
  environment: { celJava: '0.14.0', celNode: '0.6.1', protobufJava: '4.35.1', protobufNode: '2.6.2' },
  mode: 'simulation', grants: { 'tasks.get': 1 },
};
const actions = ['ALLOW', 'DENY', 'THROW', 'NON_BOOLEAN'];
const own = (value, key) => Object.hasOwn(value, key);
function record(value) { assert.ok(value && typeof value === 'object' && !Array.isArray(value), 'Expected a record'); }
function shape(value, required, optional = []) {
  record(value);
  assert.ok(required.every(key => own(value, key)), 'Missing fixture field');
  assert.ok(Object.keys(value).every(key => required.includes(key) || optional.includes(key)), 'Unknown fixture field');
}
function time(value) { assert.ok(Number.isSafeInteger(value), 'Fixture time must be a safe integer'); }
function list(value) { assert.ok(Array.isArray(value), 'Expected a fixture array'); }
function json(value) {
  return JSON.parse(JSON.stringify(value, (_key, item) => {
    assert.ok(item !== undefined && !['function', 'symbol', 'bigint'].includes(typeof item), 'Non-JSON public value');
    assert.ok(typeof item !== 'number' || Number.isFinite(item), 'Non-JSON public number');
    return item;
  }));
}

export function validateCase(row) {
  shape(row, ['id', 'runs', 'expected'], ['prepare']);
  assert.match(row.id, /^[a-z0-9][a-z0-9_-]*$/);
  if (own(row, 'prepare')) shape(row.prepare, [], ['grants', 'limits']);
  list(row.runs);
  for (const run of row.runs) {
    shape(run, ['input', 'bindings', 'grants', 'clock', 'authorizations', 'calls']);
    shape(run.clock, ['nowMs', 'expiresAtMs']); time(run.clock.nowMs); time(run.clock.expiresAtMs);
    for (const kind of ['authorizations', 'calls']) {
      list(run[kind]);
      for (const stub of run[kind]) {
        if (kind === 'authorizations') {
          shape(stub, ['request', 'action'], ['atMs']);
          shape(stub.request, ['entrypoint'], ['capability', 'version']);
          assert.equal(own(stub.request, 'capability'), own(stub.request, 'version'));
          assert.ok(actions.includes(stub.action), 'Unknown authorization action');
        } else {
          shape(stub, ['request'], ['response', 'throw', 'atMs']);
          shape(stub.request, ['capability', 'version', 'agentArgs', 'protectedArgs', 'remainingMs']);
          record(stub.request.agentArgs); record(stub.request.protectedArgs); time(stub.request.remainingMs);
          assert.notEqual(own(stub, 'response'), own(stub, 'throw'), 'Adapter needs exactly one response or throw');
          if (own(stub, 'throw')) assert.equal(stub.throw, true);
        }
        if (own(stub, 'atMs')) time(stub.atMs);
      }
    }
  }
  shape(row.expected, ['prepare', 'runs']);
  const selected = row.expected.prepare;
  if (selected.status === 'PREPARED') shape(selected, ['status']);
  else { shape(selected, ['status', 'code', 'path']); assert.equal(selected.status, 'REJECTED'); }
  list(row.expected.runs);
  assert.equal(row.expected.runs.length, row.runs.length, 'Expected and requested runs must correspond');
  if (selected.status === 'REJECTED') assert.equal(row.runs.length, 0, 'Rejected preparation cannot request runs');
  for (const run of row.expected.runs) {
    shape(run, ['outcome', 'calls', 'authorizations', 'consumption']);
    record(run.outcome); list(run.calls); list(run.authorizations); record(run.consumption);
  }
}

export function loadCorpus() {
  const corpus = JSON.parse(readFileSync(new URL('scenarios.json', import.meta.url), 'utf8'));
  shape(corpus, ['format', 'cases']);
  assert.equal(corpus.format, 'keir-read-scenarios/1'); list(corpus.cases);
  assert.ok(corpus.cases.length > 0);
  const ids = new Set();
  for (const row of corpus.cases) { validateCase(row); assert.ok(!ids.has(row.id), 'Duplicate scenario id'); ids.add(row.id); }
  return corpus;
}

// DISPLAY owns its validation independently of the runtime's type checker.
export function consume(outcome) {
  if (outcome.classification !== 'COMPLETED') return { status: 'NOT_DISPLAYED', code: outcome.error.code };
  const result = outcome.result;
  if (!result || typeof result !== 'object' || Array.isArray(result) || Object.keys(result).length !== 1 ||
      !own(result, 'classification') || !['DONE', 'PENDING'].includes(result.classification)) {
    return { status: 'NOT_DISPLAYED', code: 'OUTPUT_INVALID' };
  }
  return { status: 'DISPLAYED', result: { classification: result.classification } };
}

export async function runCase(row) {
  validateCase(row);
  const violations = [];
  let active;
  function next(kind, request) {
    try {
      assert.ok(active, 'Callback outside an invocation');
      const captured = json(request);
      active.output[kind].push(captured);
      const stub = active.run[kind][active.used[kind]++];
      assert.ok(stub, `Unexpected ${kind} callback`);
      assert.deepEqual(captured, stub.request, `Out-of-order or mismatched ${kind} callback`);
      if (own(stub, 'atMs')) active.now = stub.atMs;
      return stub;
    } catch (error) {
      // Runtime callbacks may become safe failure outcomes; fixture errors must still fail the harness.
      violations.push(error.message);
      throw new Error('Conformance callback mismatch');
    }
  }
  const selected = prepare(artifact, { ...admission, ...row.prepare }, {
    'tasks.get': { version: '1', mode: 'simulation', invoke: async (agentArgs, protectedArgs, remainingMs) => {
      const stub = next('calls', { capability: 'tasks.get', version: '1', agentArgs, protectedArgs, remainingMs });
      if (stub.throw) throw new Error('Private adapter fixture failure');
      return json(stub.response);
    } },
  });
  const output = { id: row.id, prepare: { status: 'PREPARED' }, runs: [] };
  if (typeof selected.code === 'string') {
    shape(selected, ['code', 'path', 'message']);
    assert.equal(typeof selected.message, 'string');
    output.prepare = { status: 'REJECTED', code: selected.code, path: selected.path };
  } else for (const run of row.runs) {
    const captured = { calls: [], authorizations: [] };
    active = { run, now: run.clock.nowMs, used: { calls: 0, authorizations: 0 }, output: captured };
    const outcome = json(await invoke(selected, json(run.input), {
      bindings: json(run.bindings), grants: json(run.grants),
      deadline: { clock: { nowMs: () => active.now }, expiresAtMs: run.clock.expiresAtMs },
      authorize: async request => {
        const stub = next('authorizations', request);
        if (stub.action === 'THROW') throw new Error('Private authorization fixture failure');
        return stub.action === 'NON_BOOLEAN' ? 'invalid' : stub.action === 'ALLOW';
      },
    }));
    for (const kind of ['calls', 'authorizations']) {
      if (active.used[kind] !== run[kind].length) violations.push(`Unused ${kind} stub`);
    }
    active = undefined;
    output.runs.push({ outcome, ...captured, consumption: consume(outcome) });
  }
  assert.deepEqual(violations, [], `${row.id}: harness violations`);
  assert.deepEqual(output, { id: row.id, ...row.expected }, `${row.id}: conformance mismatch`);
  return output;
}

export async function runCorpus(corpus = loadCorpus()) {
  const results = [];
  for (const row of corpus.cases) results.push(await runCase(row));
  return results;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try { assert.equal(process.argv.length, 2, 'Runner accepts no arguments'); console.log(JSON.stringify(await runCorpus(), null, 2)); }
  catch (error) { console.error(error.message); process.exitCode = 1; }
}
