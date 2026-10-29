import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { canonicalBytes, digest, parseCanonical } from '../../src/canonical.js';

const [compilerJar, modelPath] = process.argv.slice(2);
assert(compilerJar, 'Provide the built Java compiler CLI jar');
const sampleModel = JSON.parse(readFileSync(modelPath ?? new URL('../../../conformance/read-and-decide/dcm.json', import.meta.url)));
const model = structuredClone(sampleModel);
const strings = ['toString', 'toJSON', 'valueOf', 'input', 'text0', 'text1', 'text2', 'text3'];
model.types.ClassifyInput.fields = { constructor: 'TaskId', flag: 'Boolean', choice: 'Classification', ...Object.fromEntries(strings.map(k => [k, 'String'])) };
model.types.ClassifyResult.fields = { classification: 'Classification', leftEnum: 'Classification', rightEnum: 'Classification', constructor: 'TaskId', ...Object.fromEntries(strings.map(k => [k, 'String'])) };
model.entrypoints.ClassifyTask.consumer.id = 'fixture.scalars';
const modelBytes = canonicalBytes(model);
const source = {
  language: 'keir/v0', domain: { ...model.domain, fingerprint: digest(modelBytes) }, entrypoint: 'ClassifyTask', name: 'scalar-boundaries',
  steps: [{ id: 'source', call: 'tasks.get', args: { taskId: { cel: 'input.constructor' } } }, { id: 'completed', let: { cel: 'input.flag && source.status == "COMPLETED"' } }],
  return: { record: { classification: { cel: 'completed ? "DONE" : "PENDING"' }, leftEnum: { cel: 'completed ? input.choice : "PENDING"' }, rightEnum: { cel: 'completed ? "DONE" : input.choice' }, constructor: { cel: 'input.constructor' }, ...Object.fromEntries(strings.map(k => [k, { cel: 'input.' + k }])) } }
};
const file = name => fileURLToPath(new URL(name, import.meta.url));
writeFileSync(file('scalars.dcm.json'), modelBytes);
for (const [name, condition] of [['scalars', 'COMPLETED'], ['changed-condition', 'OPEN']]) {
  source.steps[1].let.cel = `input.flag && source.status == "${condition}"`;
  writeFileSync(file(name + '.source.json'), canonicalBytes(source));
  const compile = () => spawnSync('java', ['-jar', compilerJar, file(name + '.source.json'), file('scalars.dcm.json')], { timeout: 30000, maxBuffer: 1048576 });
  const first = compile(), second = compile();
  assert.equal(first.status, 0, 'Java fixture compilation failed');
  assert.equal(second.status, 0, 'Repeated Java fixture compilation failed');
  assert(first.stdout.equals(second.stdout), 'Java fixture output must be deterministic');
  const artifact = parseCanonical(first.stdout);
  assert.equal(artifact.dcmDigest, digest(modelBytes));
  writeFileSync(file(name + '.artifact.json'), first.stdout);
  writeFileSync(file(name + '.sha256'), digest(first.stdout) + '\n');
}
const literal = { language: 'keir/v0', domain: { ...sampleModel.domain, fingerprint: digest(canonicalBytes(sampleModel)) }, entrypoint: 'ClassifyTask', name: 'literal-values', steps: [{ id: 'constant', let: { literal: false } }], return: { record: { classification: { literal: 'PENDING' } } } };
writeFileSync(file('literal.source.json'), canonicalBytes(literal));
const modelFile = modelPath ?? fileURLToPath(new URL('../../../conformance/read-and-decide/dcm.json', import.meta.url));
const compileLiteral = () => spawnSync('java', ['-jar', compilerJar, file('literal.source.json'), modelFile], { timeout: 30000, maxBuffer: 1048576 });
const literalOne = compileLiteral(), literalTwo = compileLiteral();
assert.equal(literalOne.status, 0); assert.equal(literalTwo.status, 0);
assert(literalOne.stdout.equals(literalTwo.stdout)); parseCanonical(literalOne.stdout);
writeFileSync(file('literal.artifact.json'), literalOne.stdout);
writeFileSync(file('literal.sha256'), digest(literalOne.stdout) + '\n');
console.log('Generated three deterministic Java-compiled scalar fixtures.');
