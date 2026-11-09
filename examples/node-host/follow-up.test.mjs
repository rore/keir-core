import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const root = new URL('../../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');

test('teaching inputs match the frozen main source and model', () => {
  assert.deepEqual(JSON.parse(read('examples/node-host/follow-up/dcm.json')),
    JSON.parse(read('conformance/keir-3/dcm.json')));
  assert.equal(read('examples/node-host/follow-up/source.yaml').replaceAll('\r\n', '\n'),
    read('conformance/keir-3/source.yaml').replaceAll('\r\n', '\n'));
});

test('installed runtime runs the direct follow-up host', () => {
  const output = execFileSync(process.execPath, ['examples/node-host/follow-up/run.mjs'], {
    cwd: root,
    encoding: 'utf8',
    timeout: 10_000
  });
  assert.deepEqual(output.trimEnd().split(/\r?\n/), [
    'task1 CREATED CONFIRMED | Follow up',
    'task2 NO_OP -',
    'task3 UNCERTAIN UNKNOWN'
  ]);
});
