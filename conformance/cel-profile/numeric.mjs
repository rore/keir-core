// Dedicated finite hosted entrypoint. Never imports either legacy qualify.mjs.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, realpathSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { cpus, availableParallelism, totalmem } from 'node:os';

const here = dirname(fileURLToPath(import.meta.url));
const source = resolve(here, '../..');
const mode = process.argv[2];
const root = process.env.NUMERIC_ROOT;
assert(process.platform === 'linux' && process.versions.node.startsWith('24.') && root && process.env.RUNNER_TEMP);
assert.equal(dirname(resolve(root)), realpathSync(process.env.RUNNER_TEMP));
assert.equal(realpathSync(root), resolve(root), 'No symlinked qualification root');
for (const name of ['logs', 'output', 'evidence']) mkdirSync(join(root, name), { recursive: true });
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const save = (name, data) => writeFileSync(join(root, 'output', name), JSON.stringify(data, null, 2) + '\n');
const readJson = path => {
  assert(statSync(path).isFile() && !statSync(path).isSymbolicLink() && statSync(path).size <= 1048576);
  return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(readFileSync(path)));
};
const sourcePaths = [
  'conformance/cel-profile/src/main/java/qualification/NumericProbe.java', 'conformance/cel-profile/numeric-fixtures.json',
  'conformance/cel-profile/numeric-profile.mjs', 'conformance/cel-profile/numeric.mjs', '.github/workflows/numeric-seam-qualification.yml',
];

// Same finite direct-child protocol as resource qualification, without its control probes.
// A sampled RSS screen and JVM/V8 heap settings are not hard address-space containment.
async function child(command, args, name, timeoutMs, workload = false) {
  assert(timeoutMs > 0 && timeoutMs <= (workload ? 45000 : 480000));
  const start = performance.now(), chunks = [], childProcess = spawn(command, args, {
    cwd: source, env: process.env, detached: true, stdio: ['ignore', 'pipe', 'pipe'],
  });
  let captured = 0, reason = 'exit', peakRssBytes = 0, spawnError, stopError, readyMs;
  function stop(why) {
    if (reason !== 'exit') return;
    reason = why;
    if (childProcess.pid) { try { process.kill(-childProcess.pid, 'SIGKILL'); } catch (error) { if (error.code !== 'ESRCH') stopError = error.code; } }
  }
  const interrupted = () => stop('supervisor-signal');
  process.once('SIGTERM', interrupted); process.once('SIGINT', interrupted);
  const timer = setTimeout(() => stop('timeout'), timeoutMs);
  const sampler = workload ? setInterval(() => {
    try {
      const status = readFileSync(`/proc/${childProcess.pid}/status`, 'utf8');
      const match = /^VmHWM:\s+(\d+) kB$/m.exec(status);
      if (match) { peakRssBytes = Math.max(peakRssBytes, Number(match[1]) * 1024); if (peakRssBytes > 512 * 1024 * 1024) stop('observed-rss'); }
      const children = readFileSync(`/proc/${childProcess.pid}/task/${childProcess.pid}/children`, 'utf8').trim();
      if (children) stop('unexpected-descendant');
    } catch (error) { if (error.code !== 'ENOENT' && error.code !== 'ESRCH') stop('sampler-failure'); }
  }, 100) : undefined;
  for (const stream of [childProcess.stdout, childProcess.stderr]) stream.on('data', bytes => {
    if (captured + bytes.length > 1048576) { stop('output-cap'); return; }
    chunks.push(bytes); captured += bytes.length;
    if (readyMs === undefined && Buffer.concat(chunks).includes('NUMERIC_READY\n')) readyMs = performance.now() - start;
  });
  childProcess.once('error', error => { spawnError = error.code; });
  const close = await new Promise(resolveClose => childProcess.once('close', (code, signal) => resolveClose({ code, signal })));
  clearTimeout(timer); clearInterval(sampler);
  process.off('SIGTERM', interrupted); process.off('SIGINT', interrupted);
  // Do not declare closure while a task-owned process-group member remains.
  let groupClosed = true;
  if (childProcess.pid) {
    try { process.kill(-childProcess.pid, 0); groupClosed = false; }
    catch (error) { if (error.code !== 'ESRCH') groupClosed = false; }
    if (!groupClosed) { try { process.kill(-childProcess.pid, 'SIGKILL'); } catch { /* Closure remains failed. */ } }
  }
  writeFileSync(join(root, 'logs', name + '.log'), Buffer.concat(chunks));
  const result = { name, reason, ...close, spawnError: spawnError ?? null, stopError: stopError ?? null, elapsedMs: performance.now() - start,
    capturedBytes: captured, processReadyMs: readyMs ?? null, peakRssBytes, rssSamplingMs: workload ? 100 : null, directChildClosed: true, processGroupClosed: groupClosed };
  const status = result.reason === 'exit' && result.code === 0 && !spawnError && !stopError && groupClosed ? 'passed' : 'failed';
  save(name + '-process.json', { status, ...result });
  assert.equal(status, 'passed', 'Bounded child failed: ' + name + '/' + reason);
  return result;
}

async function setup() {
  assert.equal(process.env.MAVEN_USER_HOME, join(root, 'maven'));
  assert.equal(process.env.NPM_CONFIG_CACHE, join(root, 'npm-cache'));
  await child('java', ['--version'], 'java-version', 45000);
  await child('./mvnw', ['-B', '-ntp', '-f', 'conformance/cel-profile/pom.xml',
    '-Dmaven.repo.local=' + join(root, 'maven/repository'), '-DskipTests', 'package'], 'probe-build', 480000);
  await child('npm', ['ci', '--prefix', 'runtime-node', '--cache', join(root, 'npm-cache'), '--ignore-scripts', '--no-audit', '--no-fund'], 'node-dependencies', 480000);
}
async function workload() {
  const start = performance.now(), deadline = start + 135000;
  const remaining = () => { const left = Math.floor(deadline - performance.now()); assert(left > 0, '135-second workload exhausted'); return Math.min(45000, left); };
  const fixture = join(here, 'numeric-fixtures.json'); readJson(fixture);
  const manifest = { revision: process.env.GITHUB_SHA, node: process.version, cpuModel: cpus()[0]?.model,
    visibleLogicalCpus: cpus().length, availableParallelism: availableParallelism(), hostMemoryBytes: totalmem(),
    packages: { celJava: '0.14.0', celNode: '0.6.1', protobufJava: '4.35.1', protobufNode: '2.6.2' },
    sourceHashes: Object.fromEntries(sourcePaths.map(path => [path, sha(readFileSync(join(source, path)))])),
    fixtureSha256: sha(readFileSync(fixture)), native: {}, limitations: [
      'Raw CheckedExpr/component probe, not a numeric Keir artifact or full runtime/host qualification.',
      'Latency excludes assertions where stated; elapsed throughput includes validation/loop overhead.',
      'Java four-thread shared-plan CPU versus Node same-isolate serialized/interleaved lanes are not equivalent parallelism.',
      'RSS sampling may miss spikes; heap flags are not process/address-space limits. Retained heap is not allocation rate.',
      'Fixed9 cannot satisfy the exact small-rate requirement; its rejection is comparison evidence, not a substitute success.',
    ] };
  for (const path of ['/sys/fs/cgroup/cpu.max', '/sys/fs/cgroup/memory.max']) {
    if (existsSync(path)) manifest.native[path] = readFileSync(path, 'utf8');
  }
  manifest.native.process = readFileSync('/proc/self/status', 'utf8').split('\n')
    .filter(line => /^(?:Cpus_allowed_list|Threads|VmRSS|VmHWM):/.test(line));
  const jar = join(here, 'target/scalar-probe-0.1.0.jar'); manifest.probeJarSha256 = sha(readFileSync(jar));
  manifest.packageManifestSha256 = sha(readFileSync(join(source, 'runtime-node/package-lock.json')));
  save('manifest.json', manifest);
  await child('java', ['-Xmx256m', '-cp', jar, 'qualification.NumericProbe', fixture, join(root, 'output')], 'java-workload', remaining(), true);
  await child(process.execPath, ['--expose-gc', '--max-old-space-size=256', fileURLToPath(import.meta.url), 'replay'], 'node-workload', remaining(), true);
  const java = readJson(join(root, 'output/java-report.json')), node = readJson(join(root, 'output/node-report.json'));
  assert.equal(java.status, 'passed'); assert.equal(node.status, 'passed');
  assert.equal(java.fixtureSha256, manifest.fixtureSha256); assert.equal(node.fixtureSha256, manifest.fixtureSha256);
  assert.deepEqual(java.cases, node.cases); // Additional parity check; each engine already checked the independent fixture oracle.
  save('workload.json', { status: 'passed', elapsedMs: performance.now() - start, originalJavaCheckedBytes: true,
    independentOracles: true, scope: 'qualification-only', fixed9MeetsSmallRateRequirement: false });
  assert(performance.now() <= deadline, '135-second workload exhausted');
}
async function replay() {
  const { replay, hash, progress } = await import('./numeric-profile.mjs');
  console.log('NUMERIC_READY');
  try {
    const fixturesPath = join(here, 'numeric-fixtures.json'), fixtures = readJson(fixturesPath), bundle = readJson(join(root, 'output/bundle.json'));
    assert.equal(hash(readFileSync(fixturesPath)), bundle.fixtureSha256);
    const report = await replay(fixtures, bundle); report.fixtureSha256 = bundle.fixtureSha256;
    save('node-report.json', report);
  } catch (error) { save('node-report.json', { status: 'failed', progress, error: String(error.stack) }); throw error; }
}
function evidence() {
  const names = ['bundle.json', 'manifest.json', 'java-report.json', 'java-progress.json', 'node-report.json', 'workload.json', 'setup.json',
    'probe-build-process.json', 'node-dependencies-process.json', 'java-version-process.json', 'java-workload-process.json', 'node-workload-process.json'];
  const logs = ['probe-build', 'node-dependencies', 'java-version', 'java-workload', 'node-workload'];
  const paths = [...names.map(name => ['output', name]), ...logs.map(name => ['logs', name + '.log'])];
  const replacements = [[root, '<qualification-root>'], [source, '<source-checkout>'], [process.env.HOME, '<runner-home>']]
    .filter(([value]) => value).sort((a, b) => b[0].length - a[0].length);
  const inventory = []; let total = 0;
  for (const [directory, name] of paths) {
    const path = join(root, directory, name); if (!existsSync(path)) continue;
    assert.equal(realpathSync(path), path); const stat = statSync(path); assert(stat.isFile() && stat.size <= 1048576);
    let text = new TextDecoder('utf-8', { fatal: true }).decode(readFileSync(path));
    for (const [value, replacement] of replacements) text = text.replaceAll(value, replacement);
    const bytes = Buffer.from(text), target = directory + '-' + name;
    total += bytes.length; assert(total <= 10 * 1024 * 1024);
    writeFileSync(join(root, 'evidence', target), bytes); inventory.push({ name: target, bytes: bytes.length, sha256: sha(bytes) });
  }
  const passed = process.env.SETUP_OUTCOME === 'success' && process.env.WORKLOAD_OUTCOME === 'success';
  const receipt = Buffer.from(JSON.stringify({ status: passed ? 'passed' : 'failed', revision: process.env.GITHUB_SHA,
    setup: process.env.SETUP_OUTCOME, workload: process.env.WORKLOAD_OUTCOME, inventory,
    evidenceLimitBytes: 10 * 1024 * 1024, sourceScope: 'numeric engine qualification only', noLocalExecution: true }, null, 2) + '\n');
  assert(total + receipt.length <= 10 * 1024 * 1024);
  writeFileSync(join(root, 'evidence/receipt.json'), receipt);
  console.log('Bounded allowlisted evidence prepared; status=' + (passed ? 'passed' : 'failed'));
}
try {
  assert(['setup', 'workload', 'replay', 'evidence'].includes(mode));
  if (mode === 'setup') await setup();
  else if (mode === 'workload') await workload();
  else if (mode === 'replay') await replay();
  else evidence();
} catch (error) {
  if (mode !== 'replay' && mode !== 'evidence') save(mode + '.json', { status: 'failed', error: String(error.stack) });
  console.error(error.stack); process.exitCode = 1;
}
