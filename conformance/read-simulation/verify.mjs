import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { resolve, dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const repository = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const hash = bytes => `sha256:${createHash('sha256').update(bytes).digest('hex')}`;
export function verifyReadSimulation({ repo = repository, run }) {
  const jar = resolve(repo, 'examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar');
  const compiler = resolve(repo, 'compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar');
  const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
  const node = resolve(repo, 'examples/node-host/read-simulation.mjs');
  const output = resolve(repo, '.tools/read-simulation-reports'); mkdirSync(output, { recursive: true });
  const bundles = {};
  for (const name of ['task-advice', 'repeated']) {
    const path = resolve(repo, 'conformance/read-simulation', name);
    const selection = JSON.parse(readFileSync(resolve(path, 'selection.json')));
    assert.equal(hash(readFileSync(resolve(path, 'dcm.json'))), selection.modelDigest);
    const bytes = readFileSync(resolve(path, 'artifact.json'));
    assert.equal(hash(bytes), selection.artifactDigest);
    assert.deepEqual(run(java, ['-jar', compiler, resolve(path, 'source.yaml'), resolve(path, 'dcm.json'), '--profile=keir-read/2'], 'Simulation artifact recompilation'), bytes);
    bundles[name] = { path, selection };
  }
  const provenance = JSON.parse(readFileSync(resolve(bundles['task-advice'].path, 'provenance.json')));
  assert.equal(hash(readFileSync(resolve(bundles['task-advice'].path, 'source.yaml'))), provenance.rawSourceDigest, 'Exported raw source evidence differs');
  const rows = [
    ['task-advice', 'open', 'normal', 'PASS'], ['task-advice', 'completed', 'normal', 'PASS'],
    ['task-advice', 'missing', 'normal', 'PASS'], ['task-advice', 'denied', 'denied', 'PASS'],
    ['task-advice', 'transient', 'normal', 'PASS'],
    ['task-advice', 'mismatch', 'normal', 'MISMATCH'], ['task-advice', 'open', 'consumer-throws', 'CONSUMER_ERROR'],
    ['task-advice', 'open', 'consumer-unlisted', 'CONSUMER_ERROR'], ['task-advice', 'open', 'consumer-wrong', 'MISMATCH'],
    ['repeated', 'repeated', 'normal', 'PASS'], ['repeated', 'reordered', 'normal', 'MISMATCH'],
  ];
  const base = JSON.parse(readFileSync(resolve(bundles['task-advice'].path, 'open.json')));
  const bad = {
    authority: JSON.stringify({ ...base, bindings: { actorId: 'PRIVATE_CANARY' } }),
    duplicate: '{"format":"keir-read-simulation/1","format":"PRIVATE_CANARY"}',
    depth: '['.repeat(17) + '0' + ']'.repeat(17),
    oversized: ' '.repeat(65537),
  };
  let count = 0;
  function compare(bundle, file, fixture, status, label) {
    const javaBytes = run(java, ['-cp', jar, 'dev.keir.examples.ReadSimulationHost', bundle.path, fixture, file], 'Packaged Java read simulation');
    const nodeBytes = run(process.execPath, [node, bundle.path, fixture, file], 'Installed Node read simulation');
    // The Java command adds one platform line ending around its canonical report.
    const javaReport = javaBytes.subarray(0, javaBytes.length - (javaBytes.at(-2) === 13 ? 2 : 1));
    assert.equal(javaBytes.at(-1), 10); assert.deepEqual(javaReport, nodeBytes, `${label}: canonical report byte parity`);
    const report = JSON.parse(javaBytes); assert.equal(report.status, status, label);
    assert.ok(javaBytes.length <= 16385); assert.ok(!javaBytes.includes(Buffer.from('PRIVATE_CANARY')));
    if (status === 'PASS') assert.deepEqual(report.mismatches, []);
    if (status === 'CONSUMER_ERROR') assert.equal(Object.hasOwn(report, 'consumption'), false);
    if (status !== 'SETUP_ERROR') assert.deepEqual(report.identity, { modelDigest: bundle.selection.modelDigest, artifactDigest: bundle.selection.artifactDigest });
    writeFileSync(resolve(output, `${count++}.json`), javaBytes);
    return report;
  }
  for (const [bundle, name, fixture, status] of rows) compare(bundles[bundle], resolve(bundles[bundle].path, name + '.json'), fixture, status, `${bundle}/${name}/${fixture}`);
  for (const [name, value] of Object.entries(bad)) {
    const file = resolve(output, name + '-case.json'); writeFileSync(file, value);
    // File overflow is rejected by the fixed host before the native tooling entry.
    compare(bundles['task-advice'], file, 'normal', name === 'oversized' ? 'SETUP_ERROR' : 'INVALID_SCENARIO', name);
  }
  return count;
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    assert.equal(process.argv.length, 2);
    const run = (command, args, phase) => {
      const result = spawnSync(command, args, { cwd: repository, timeout: 30000, maxBuffer: 1048576 });
      assert.equal(result.error, undefined, `${phase}: process error`); assert.equal(result.status, 0, `${phase}: failed`);
      return result.stdout;
    };
    console.log(`Verified ${verifyReadSimulation({ run })} packaged read simulation reports and two exact compiled artifacts.`);
  } catch { console.error('Read simulation verification failed.'); process.exitCode = 1; }
}
