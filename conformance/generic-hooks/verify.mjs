import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { dirname, resolve, delimiter } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const repository = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
export function verifyGeneric({ repo = repository, run }) {
  const compiler = resolve(repo, 'compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar');
  const hostJar = resolve(repo, 'examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar');
  const classes = resolve(repo, 'runtime-java/target/test-classes');
  const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
  const parse = bytes => JSON.parse(bytes.toString('utf8'));
  const expected = {}, directories = {};
  for (const profile of ['read', 'effect']) {
    const directory = resolve(repo, 'conformance/generic-hooks', profile); directories[profile] = directory;
    const resource = resolve(classes, 'generic-hooks', profile);
    const programs = profile === 'read' ? ['main', 'repeated', 'alternate'] : ['main', 'project2', 'title2'];
    const dcm = readFileSync(resolve(directory, 'dcm.json'));
    assert.equal(`sha256:${createHash('sha256').update(dcm).digest('hex')}`, readFileSync(resolve(directory, 'dcm.sha256'), 'utf8').trim(), 'Generic DCM independent pin differs');
    for (const name of programs) {
      const suffix = name === 'main' ? '' : `-${name}`;
      const args = ['-jar', compiler, resolve(directory, `source${suffix}.yaml`), resolve(directory, 'dcm.json'), `--profile=keir-${profile}/2`];
      const first = run(java, args, `First generic ${profile}/${name} compiler run`);
      const second = run(java, args, `Second generic ${profile}/${name} compiler run`);
      assert.deepEqual(first, second, `Generic ${profile}/${name} compiler is nondeterministic`);
      assert.deepEqual(first, readFileSync(resolve(directory, `artifact${suffix}.json`)), 'Generic artifact differs from independent compiler output');
      assert.equal(`sha256:${createHash('sha256').update(first).digest('hex')}`, readFileSync(resolve(directory, `artifact${suffix}.sha256`), 'utf8').trim(), 'Generic artifact pin differs');
      for (const file of [`artifact${suffix}.json`, `artifact${suffix}.sha256`]) assert.deepEqual(readFileSync(resolve(resource, file)), readFileSync(resolve(directory, file)), `Stale Java generic resource: ${file}`);
    }
    for (const file of ['dcm.json', 'dcm.sha256', 'scenarios.json']) assert.deepEqual(readFileSync(resolve(resource, file)), readFileSync(resolve(directory, file)), `Stale Java generic resource: ${file}`);
    const scenarios = parse(readFileSync(resolve(directory, 'scenarios.json')));
    expected[profile] = scenarios.map(row => ({ id: row.id, ...row.expect }));
  }
  const javaRows = parse(run(java, ['-cp', `${hostJar}${delimiter}${classes}`, 'dev.keir.runtime.GenericScenarioDriver'], 'Java generic shared batch'));
  const nodeRows = parse(run(process.execPath, [resolve(repo, 'conformance/generic-hooks/run-node.mjs')], 'Node generic shared batch'));
  assert.deepEqual(javaRows, expected, 'Java generic results differ from independently authored expectations');
  assert.deepEqual(nodeRows, javaRows, 'Java and Node generic full transport differs');
  for (const profile of ['read', 'effect']) {
    const javaHost = parse(run(java, ['-cp', hostJar, 'dev.keir.examples.GenericHost', directories[profile]], `Packaged Java generic ${profile} host`));
    const nodeHost = parse(run(process.execPath, [resolve(repo, 'examples/node-host/generic.mjs'), directories[profile]], `Packaged Node generic ${profile} host`));
    assert.deepEqual(javaHost, expected[profile], 'Packaged Java generic host differs from expectations');
    assert.deepEqual(nodeHost, javaHost, 'Packaged generic hosts differ');
  }
  return { read: expected.read.length, effect: expected.effect.length };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    assert.equal(process.argv.length, 2, 'Verifier accepts no arguments');
    const run = (command, args, phase) => {
      const result = spawnSync(command, args, { cwd: repository, encoding: null, timeout: 30_000, maxBuffer: 1_048_576 });
      assert.equal(result.error, undefined, `${phase} failed or timed out`); assert.equal(result.status, 0, `${phase} returned a failure status`);
      assert.ok(result.stdout.length <= 1_048_576, `${phase} output exceeded bound`); return result.stdout;
    };
    const counts = verifyGeneric({ run }); console.log(`Verified ${counts.read} generic read and ${counts.effect} generic effect scenarios, six deterministic compiled programs and four packaged hosts.`);
  } catch (failure) { console.error(`Verification failed: ${String(failure?.message ?? 'Generic verification failed').split('\n', 1)[0].replaceAll(repository, '<repo>')}`); process.exitCode = 1; }
}
