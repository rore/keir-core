import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { dirname, resolve, delimiter } from 'node:path';
import { fileURLToPath } from 'node:url';

const repo = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
try {
  const fixture = resolve(repo, 'conformance/read-and-decide');
  const generated = resolve(repo, '.tools/contract-verification/artifact.json');
  const artifactDigest = readFileSync(resolve(fixture, 'artifact.sha256'), 'utf8').trim();
  const dcmDigest = 'sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f';
  const cases = JSON.parse(readFileSync(resolve(fixture, 'scenarios.json'), 'utf8')).cases;
  const run = (command, args, phase) => {
    const result = spawnSync(command, args, { cwd: repo, encoding: null, timeout: 30000, maxBuffer: 1_048_576 });
    assert.equal(result.error, undefined, `${phase} subprocess failed or timed out`);
    assert.equal(result.status, 0, `${phase} subprocess returned a failure status`);
    assert.ok(result.stdout.length <= 1_048_576, `${phase} output exceeded limit`);
    return result.stdout;
  };
  const parse = bytes => JSON.parse(bytes.toString('utf8'));
  const jsonRun = (command, args, phase) => parse(run(command, args, phase));

  const compiler = resolve(repo, 'compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar');
  const hostJar = resolve(repo, 'examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar');
  const runtimeClasses = resolve(repo, 'runtime-java/target/test-classes');
  const source = resolve(fixture, 'source.yaml');
  const dcm = resolve(fixture, 'dcm.json');
  const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
  const jar = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'jar.exe' : 'jar') : 'jar';
  const local = path => path.slice(repo.length + 1).replaceAll('\\', '/');
  for (const path of [compiler, hostJar, runtimeClasses, resolve(runtimeClasses, 'read-and-decide/artifact.json'), resolve(runtimeClasses, 'read-and-decide/scenarios.json'), resolve(repo, 'examples/node-host/node_modules/@keir/runtime-node/package.json')]) {
    assert.ok(existsSync(path), `Missing required input: ${local(path)}`);
  }
  assert.equal(process.argv.length, 2, 'This verifier accepts no arguments');
  for (const file of ['canonical.js', 'cel.js', 'index.js', 'validation.js']) {
    assert.deepEqual(readFileSync(resolve(repo, 'examples/node-host/node_modules/@keir/runtime-node/src', file)),
      readFileSync(resolve(repo, 'runtime-node/src', file)), `Installed Node runtime differs from reviewed source: ${file}`);
  }
  const entries = run(jar, ['--list', '--file', hostJar], 'Packaged Java dependency check').toString('utf8').split(/\r?\n/);
  const forbidden = entries.find(entry => /^(dev\/keir\/compiler\/|dev\/cel\/(compiler|checker|parser)\/|org\/antlr\/|org\/yaml\/)/.test(entry));
  assert.ok(!forbidden, `Packaged Java host contains a compiler/parser dependency: ${forbidden}`);
  const first = run(java, ['-jar', compiler, source, dcm], 'First compiler run');
  const second = run(java, ['-jar', compiler, source, dcm], 'Second compiler run');
  assert.deepEqual(first, second, 'Fresh compiler runs produced different bytes');
  assert.deepEqual(first, readFileSync(resolve(fixture, 'artifact.json')), 'Generated artifact differs from committed artifact');
  assert.equal(`sha256:${createHash('sha256').update(first).digest('hex')}`, artifactDigest, 'Generated artifact digest differs from independent digest file');
  mkdirSync(dirname(generated), { recursive: true });
  writeFileSync(generated, first);

  assert.deepEqual(readFileSync(resolve(runtimeClasses, 'read-and-decide/artifact.json')), first, 'Java test resources contain a stale artifact');
  assert.deepEqual(JSON.parse(readFileSync(resolve(runtimeClasses, 'read-and-decide/scenarios.json'), 'utf8')), JSON.parse(readFileSync(resolve(fixture, 'scenarios.json'), 'utf8')), 'Java test resources contain stale scenarios');
  const scenarioRows = jsonRun(java, ['-cp', `${hostJar}${delimiter}${runtimeClasses}`, 'dev.keir.runtime.ScenarioDriver'], 'Java scenario batch');
  const expectedRows = cases.map(row => ({ id: row.id, ...row.expected }));
  assert.deepEqual(scenarioRows, expectedRows, 'Java and repository scenario expectations differ');
  const nodeRows = jsonRun(process.execPath, [resolve(fixture, 'run-node.mjs')], 'Node scenario batch');
  assert.deepEqual(nodeRows, scenarioRows, 'Node and Java scenario results differ');

  const nodeHost = resolve(repo, 'examples/node-host/run.mjs');
  const nodeHostRows = jsonRun(process.execPath, [nodeHost, generated, artifactDigest, dcmDigest], 'Node sample host batch');
  const javaHostRows = jsonRun(java, ['-jar', hostJar, generated, artifactDigest], 'Java sample host batch');
  const ids = ['completed', 'open', 'missing', 'wrong-task'];
  assert.deepEqual(nodeHostRows.map(row => row.scenario), ids, 'Node host scenario order changed');
  assert.deepEqual(javaHostRows.map(row => row.scenario), ids, 'Java host scenario order changed');
  const expectedById = new Map(cases.map(row => [row.id, row.expected.runs[0]]));
  const fixtureIds = ['done', 'pending', 'task_not_found', 'adapter-throw'];
  for (const rows of [nodeHostRows, javaHostRows]) for (let i = 0; i < rows.length; i++) {
    const expected = expectedById.get(fixtureIds[i]);
    assert.ok(expected, 'Host expectation is missing from the shared corpus');
    assert.deepEqual(rows[i].outcome, expected.outcome, `${rows[i].scenario} host outcome differs from corpus`);
    assert.deepEqual(rows[i].consumption, expected.consumption, `${rows[i].scenario} host consumption differs from corpus`);
  }
  const effectFixture = resolve(repo, 'conformance/single-effect');
  const effectCases = parse(readFileSync(resolve(effectFixture, 'scenarios.json')));
  const effectPin = readFileSync(resolve(effectFixture, 'artifact.sha256'), 'utf8').trim();
  const effectDcmPin = readFileSync(resolve(effectFixture, 'dcm.sha256'), 'utf8').trim();
  const effectArgs = ['-jar', compiler, resolve(effectFixture, 'source.yaml'), resolve(effectFixture, 'dcm.json'), '--profile=keir-effect/1'];
  const effectFirst = run(java, effectArgs, 'First effect compiler run');
  const effectSecond = run(java, effectArgs, 'Second effect compiler run');
  assert.deepEqual(effectFirst, effectSecond, 'Fresh effect compiler runs produced different bytes');
  assert.deepEqual(effectFirst, readFileSync(resolve(effectFixture, 'artifact.json')), 'Generated effect artifact differs from committed bytes');
  assert.equal(`sha256:${createHash('sha256').update(effectFirst).digest('hex')}`, effectPin, 'Fresh effect artifact differs from independent pin');
  const effectGenerated = resolve(repo, '.tools/contract-verification/effect-artifact.json');
  writeFileSync(effectGenerated, effectFirst);
  for (const file of ['README.md', 'package.json']) {
    assert.deepEqual(readFileSync(resolve(repo, 'examples/node-host/node_modules/@keir/runtime-node', file)),
      readFileSync(resolve(repo, 'runtime-node', file)), `Installed Node package differs from reviewed file: ${file}`);
  }
  assert.deepEqual(readFileSync(resolve(runtimeClasses, 'single-effect/artifact.json')), effectFirst, 'Java effect resources contain stale artifact bytes');
  assert.deepEqual(parse(readFileSync(resolve(runtimeClasses, 'single-effect/scenarios.json'))), effectCases, 'Java effect resources contain stale scenarios');
  const effectExpected = effectCases.map(row => ({ id: row.id, ...row.expect }));
  const effectJava = jsonRun(java, ['-cp', `${hostJar}${delimiter}${runtimeClasses}`, 'dev.keir.runtime.EffectScenarioDriver'], 'Java effect scenario batch');
  const effectNode = jsonRun(process.execPath, [resolve(effectFixture, 'run-node.mjs')], 'Node effect scenario batch');
  assert.deepEqual(effectJava, effectExpected, 'Java effect rows differ from hand-authored expectations');
  assert.deepEqual(effectNode, effectJava, 'Complete Java and Node effect rows differ');
  const effectNodeHost = jsonRun(process.execPath, [resolve(repo, 'examples/node-host/effect.mjs'), effectGenerated, effectPin, effectDcmPin], 'Installed Node effect host batch');
  const effectJavaHost = jsonRun(java, ['-cp', hostJar, 'dev.keir.examples.EffectHost', effectGenerated, effectPin, effectDcmPin], 'Packaged Java effect host batch');
  assert.deepEqual(effectNodeHost, effectExpected, 'Installed Node effect host differs from full shared expectations');
  assert.deepEqual(effectJavaHost, effectNodeHost, 'Packaged effect hosts differ');
  console.log(`Verified ${cases.length} read cases/${cases.reduce((count, row) => count + row.runs.length, 0)} invocations and ${effectCases.length} effect scenarios, 4 compiler runs, 4 host batches; artifacts ${artifactDigest}, ${effectPin}.`);
} catch (error) {
  const message = String(error?.message ?? 'Verification failed').split('\n', 1)[0]
    .replaceAll(repo, '<repo>');
  console.error(`Verification failed: ${message}`);
  process.exitCode = 1;
}
