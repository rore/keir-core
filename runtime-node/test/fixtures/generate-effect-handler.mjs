import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { canonicalBytes, digest } from '../../src/canonical.js';

assert.equal(process.argv.length, 3, 'Supply the existing effect-capable Java compiler CLI jar');
const cli = resolve(process.argv[2]);
const primary = new URL('../../../conformance/single-effect/', import.meta.url);
const model = JSON.parse(readFileSync(new URL('dcm.json', primary)));
assert.equal(digest(canonicalBytes(model)), 'sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77');
model.domain = { id: 'sample.effect.handlerbounds', version: '1' };
model.types.FollowupInput.fields.padding = 'String';
const fields = Array.from({ length: 16 }, (_, index) => 'f' + index);
model.types.FollowupResult.fields = Object.fromEntries(fields.map(field => [field, 'String']));
const modelBytes = canonicalBytes(model), modelPin = digest(modelBytes);
const source = {
  language: 'keir/v0', domain: { ...model.domain, fingerprint: modelPin }, entrypoint: 'TaskCompleted', name: 'handler-output-bound',
  steps: [
    { id: 'source', call: 'tasks.get', args: { taskId: { cel: 'input.taskId' } } },
    { id: 'eligible', let: { cel: 'true' } },
    { id: 'created', when: 'eligible', call: 'tasks.createFollowUp', args: {
      sourceTaskId: { cel: 'source.id' }, projectId: { cel: 'input.projectId' }, title: { cel: 'input.title' },
    }, onError: { CREATE_REJECTED: { record: Object.fromEntries(fields.map((field, index) => [field, { cel: index === 15 ? 'input.padding' : 'input.title' }])) } } },
  ],
  return: { record: Object.fromEntries(fields.map(field => [field, { literal: '' }])) },
};
const output = name => new URL('effect-handler.' + name, import.meta.url);
writeFileSync(output('dcm.json'), modelBytes);
writeFileSync(output('dcm.sha256'), modelPin + '\n');
writeFileSync(output('source.json'), canonicalBytes(source));
const runs = [];
for (let index = 0; index < 2; index++) {
  const run = spawnSync('java', ['-jar', cli, fileURLToPath(output('source.json')),
    fileURLToPath(output('dcm.json')), '--profile=keir-effect/1'], { timeout: 15000, maxBuffer: 1048576 });
  assert.ifError(run.error); assert.equal(run.status, 0, run.stderr.toString()); assert.equal(run.stderr.length, 0);
  runs.push(run.stdout);
}
assert.deepEqual(runs[0], runs[1]);
assert.equal(JSON.parse(runs[0]).dcmDigest, modelPin);
writeFileSync(output('artifact.json'), runs[0]);
writeFileSync(output('artifact.sha256'), digest(runs[0]) + '\n');
console.log(JSON.stringify({ bytes: runs[0].length, artifact: digest(runs[0]), model: modelPin }));
