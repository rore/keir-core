import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { prepare, invoke, ENVIRONMENT } from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { context } from './helpers.js';

const strings = ['toString', 'toJSON', 'valueOf', 'input', 'text0', 'text1', 'text2', 'text3'];
const modelDigest = digest(readFileSync(new URL('fixtures/scalars.dcm.json', import.meta.url)));
function scalarInput(overrides = {}) { return { constructor: 'task1', flag: false, choice: 'DONE', ...Object.fromEntries(strings.map(k => [k, ''])), ...overrides }; }
function handle(name = 'scalars', inspect = () => {}) {
  const bytes = readFileSync(new URL(`fixtures/${name}.artifact.json`, import.meta.url));
  const expected = readFileSync(new URL(`fixtures/${name}.sha256`, import.meta.url), 'utf8').trim();
  const prepared = prepare(bytes, {
    expectedArtifactDigest: expected, expectedDcmDigest: modelDigest, entrypoint: 'ClassifyTask',
    consumer: { id: 'fixture.scalars', version: '1', kind: 'DISPLAY' }, compiler: 'keir-compiler-java/0.1.0-SNAPSHOT',
    environment: ENVIRONMENT, mode: 'simulation', grants: { 'tasks.get': 1 }
  }, { 'tasks.get': { version: '1', mode: 'simulation', invoke: async (args, protectedArgs) => {
    inspect(args, protectedArgs);
    return { kind: 'SUCCESS', value: { id: args.taskId, status: 'COMPLETED' } };
  } } });
  assert.equal(prepared.code, undefined);
  return prepared;
}
const resultFor = value => ({ classification: value.flag ? 'DONE' : 'PENDING', leftEnum: value.flag ? value.choice : 'PENDING', rightEnum: value.flag ? 'DONE' : value.choice, constructor: value.constructor, ...Object.fromEntries(strings.map(k => [k, value[k]])) });

test('real Java artifact preserves false and empty strings through record projection and scalar returns', async () => {
  const value = scalarInput();
  const outcome = await invoke(handle(), value, context());
  assert.equal(outcome.classification, 'COMPLETED');
  assert.deepEqual(outcome.result, resultFor(value));
});

test('valid reserved JavaScript field names reach adapters and returns unchanged', async () => {
  let args;
  const value = scalarInput({ flag: true, toString: 'kept-toString', toJSON: 'kept-toJSON', valueOf: 'kept-valueOf' });
  const outcome = await invoke(handle('scalars', seen => { args = seen; }), value, context());
  assert.deepEqual({ ...args }, { taskId: 'task1' });
  assert.deepEqual(outcome.result, resultFor(value));
});

test('changing only the authored condition changes behavior without changing the host adapter', async () => {
  const value = scalarInput({ flag: true });
  const completed = await invoke(handle('scalars'), value, context());
  const changed = await invoke(handle('changed-condition'), value, context());
  assert.equal(completed.result.classification, 'DONE');
  assert.equal(changed.result.classification, 'PENDING');
});

test('Unicode and escaped controls remain exact and are not normalized', async () => {
  const value = scalarInput({ toString: 'é😀', toJSON: 'e\u0301', valueOf: '\u0000\t\n"\\' });
  const outcome = await invoke(handle(), value, context());
  assert.deepEqual(outcome.result, resultFor(value));
  assert.notEqual(outcome.result.toString, outcome.result.toJSON);
});

test('accepts the exact 256-byte scalar string ceiling', async () => {
  const outcome = await invoke(handle(), scalarInput({ text0: 'é'.repeat(128) }), context());
  assert.equal(outcome.classification, 'COMPLETED');
  assert.equal(Buffer.byteLength(outcome.result.text0), 256);
  const excess = await invoke(handle(), scalarInput({ text0: 'x'.repeat(257) }), context());
  assert.equal(excess.classification, 'LIMIT_EXCEEDED');
  assert.equal(excess.error.code, 'BUDGET_EXCEEDED');
});

function fillRecord(target, measure) {
  const value = scalarInput();
  let left = target - canonicalBytes(measure(value)).length;
  for (const key of strings) {
    const controls = Math.min(256, Math.floor(left / 6));
    const ascii = Math.min(256 - controls, left - controls * 6);
    value[key] = '\u0000'.repeat(controls) + 'x'.repeat(ascii);
    left -= controls * 6 + ascii;
  }
  assert.equal(left, 0);
  assert.equal(canonicalBytes(measure(value)).length, target);
  return value;
}

test('enforces the exact serialized result ceiling and rejects one additional byte', async () => {
  const exact = fillRecord(8192, resultFor);
  assert.ok(canonicalBytes(exact).length <= 8192);
  const completed = await invoke(handle(), exact, context());
  assert.equal(completed.classification, 'COMPLETED');
  assert.equal(canonicalBytes(completed.result).length, 8192);
  const excess = fillRecord(8193, resultFor);
  assert.ok(canonicalBytes(excess).length <= 8192);
  const rejected = await invoke(handle(), excess, context());
  assert.deepEqual(rejected.error, { code: 'BUDGET_EXCEEDED', path: '/return' });
  assert.equal(rejected.classification, 'LIMIT_EXCEEDED');
  assert.equal(Object.hasOwn(rejected, 'result'), false);
});

test('rejects a serialized input above 8192 bytes before adapter dispatch', async () => {
  let calls = 0;
  const value = fillRecord(8193, input => input);
  const outcome = await invoke(handle('scalars', () => { calls++; }), value, context());
  assert.deepEqual(outcome.error, { code: 'BUDGET_EXCEEDED', path: '/input' });
  assert.equal(calls, 0);
});

test('literal bindings need no implicit call grant or expression budget', async () => {
  const bytes = readFileSync(new URL('fixtures/literal.artifact.json', import.meta.url));
  const expected = readFileSync(new URL('fixtures/literal.sha256', import.meta.url), 'utf8').trim();
  const selected = prepare(bytes, {
    expectedArtifactDigest: expected, expectedDcmDigest: 'sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f', entrypoint: 'ClassifyTask',
    consumer: { id: 'sample.classification', version: '1', kind: 'DISPLAY' }, compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT,
    mode: 'simulation', grants: {}, limits: { steps: 1, calls: 0, totalAstNodes: 0, traceEvents: 5 }
  }, {});
  assert.equal(selected.code, undefined);
  const outcome = await invoke(selected, { taskId: 'task1' }, context({ bindings: {}, grants: {} }));
  assert.deepEqual(outcome.result, { classification: 'PENDING' });
  assert.equal(outcome.trace.length, 5);
  assert.equal(outcome.trace.some(event => event.event === 'CALL_DISPATCHED'), false);
});
