import assert from 'node:assert/strict';
import { spawn, execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync, mkdtempSync, realpathSync } from 'node:fs';
import { request, Agent } from 'node:http';
import { createServer } from 'node:net';
import { join, delimiter } from 'node:path';
import { fileURLToPath } from 'node:url';
import { canonicalBytes } from '../../../runtime-node/src/canonical.js';

// Linux-only, opt-in diagnostic. Output and per-request samples stay in ignored build/.
const root = fileURLToPath(new URL('../../../', import.meta.url));
const here = fileURLToPath(new URL('./', import.meta.url));
const mode = process.argv[2] ?? '--smoke';
assert.ok(['--smoke', '--sample', '--followup'].includes(mode));
assert.equal(process.platform, 'linux');
assert.ok(process.versions.node.startsWith('24.'));
assert.equal(realpathSync(root), process.cwd(), 'Run from the repository root');
mkdirSync(join(root, 'build'), { recursive: true });
const dir = mkdtempSync(join(root, 'build/java-http-'));
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const git = (...args) => execFileSync('git', args, { cwd: root, encoding: 'utf8' }).trim();
assert.equal(git('diff', 'HEAD', '--', 'compiler-java/src/main/java', 'runtime-java/src/main/java'), '', 'Build from unmodified Core sources');
const cpuStat = () => Object.fromEntries(readFileSync('/sys/fs/cgroup/cpu.stat', 'utf8').trim().split('\n').map(s => {
  const [key, value] = s.split(/\s+/); return [key, Number(value)];
}));
const read = name => readFileSync(join(root, name));
const save = (name, object) => writeFileSync(join(dir, name), JSON.stringify(object, null, 2) + '\n');
const hostCpus = process.env.KEIR_HTTP_CPUS ?? '0,1';
assert.match(hostCpus, /^\d+(?:[-,]\d+)*$/);
const cpuSet = spec => [...new Set(spec.split(',').flatMap(part => {
  const [a, b = a] = part.split('-').map(Number);
  assert.ok(b >= a && b - a < 1024); return Array.from({ length: b - a + 1 }, (_, i) => a + i);
}))].sort((a, b) => a - b);
assert.equal(cpuSet(hostCpus).length, 2, 'Choose exactly two host CPUs');
const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin/java') : 'java';
const jars = ['compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar',
  'examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar'];
const cp = [dir, ...jars.map(p => join(root, p))].join(delimiter);
const receipts = [], children = new Set();
const report = {
  schema: 'keir-java-http/1', mode, collectedAtUtc: new Date().toISOString(), sourceCommit: git('rev-parse', 'HEAD'),
  sourceTrees: Object.fromEntries(['compiler-java/src/main/java', 'runtime-java/src/main/java'].map(p => [p, git('rev-parse', 'HEAD:' + p)])),
  trackedChangesAtStart: git('diff', '--name-only', 'HEAD').split('\n').filter(Boolean),
  harnessSha256: Object.fromEntries(['HttpProbe.java', 'run.mjs'].map(p => [p, hash(readFileSync(join(here, p)))])),
  binarySha256: Object.fromEntries(jars.map(p => [p, hash(read(p))])),
  environment: { node: process.version, kernel: execFileSync('uname', ['-sr'], { encoding: 'utf8' }).trim(),
    cpuModel: readFileSync('/proc/cpuinfo', 'utf8').match(/^model name\s*:\s*(.*)$/m)?.[1],
    generatorCpuList: readFileSync('/proc/self/status', 'utf8').match(/^Cpus_allowed_list:\s*(.*)$/m)?.[1],
    cgroupCpuMax: readFileSync('/sys/fs/cgroup/cpu.max', 'utf8').trim(),
    cgroupMemoryMax: readFileSync('/sys/fs/cgroup/memory.max', 'utf8').trim() },
  limits: { hostCpus, activeProcessorCount: 2, heapMiB: 512, sampledRssStopBytes: 1073741824,
    rssIntervalMs: 100, admittedSlots: 32, httpBacklog: 128, outstandingGuard: mode === '--smoke' ? 4 : 512,
    requestTimeoutMs: 5000, childCaptureBytes: 1048576, serverWatchdogMs: 300000, stageMaxSeconds: 60 },
  fixtureSha256: {}, stages: [], children: receipts,
};
function child(label, executable, args, timeout = 45000) {
  const p = spawn(executable, args, { cwd: root, stdio: ['pipe', 'pipe', 'pipe'] });
  children.add(p);
  let out = '', err = '', bytes = 0, reason = 'exit';
  const stop = why => { if (reason === 'exit') { reason = why; p.kill('SIGKILL'); } };
  let timer = setTimeout(() => stop('timeout'), timeout);
  for (const [stream, key] of [[p.stdout, 'out'], [p.stderr, 'err']]) stream.on('data', b => {
    bytes += b.length;
    if (bytes > 1048576) return stop('output-cap');
    if (key === 'out') out += b.toString(); else err += b.toString();
  });
  let spawnError;
  p.on('error', e => { spawnError = e; });
  const done = new Promise(resolve => p.on('close', (code, signal) => {
    clearTimeout(timer); children.delete(p);
    const receipt = { label, reason, code, signal, capturedBytes: Math.min(bytes, 1048576), closed: true };
    save(label + '-output.json', { stdout: out, stderr: err });
    receipts.push(receipt); resolve({ ...receipt, out, err, spawnError });
  }));
  return { p, done, stop, output: () => out,
    deadline(ms) { clearTimeout(timer); timer = setTimeout(() => stop('timeout'), ms); } };
}
async function requireExit(c) {
  c.p.stdin.end(); const r = await c.done;
  assert.equal(r.reason, 'exit', JSON.stringify(r)); assert.equal(r.code, 0, r.err); assert.equal(r.spawnError, undefined);
  return r;
}
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function ready(c) {
  const deadline = performance.now() + 15000;
  while (!c.output().includes('\n')) {
    assert.ok(children.has(c.p), 'Server exited before ready');
    assert.ok(performance.now() < deadline, 'Server startup timeout'); await pause(20);
  }
  return JSON.parse(c.output().split('\n')[0]);
}
async function portReleased(port) {
  await new Promise((resolve, reject) => {
    const probe = createServer(); probe.once('error', reject); probe.listen(port, '127.0.0.1', () => probe.close(resolve));
  });
}
function fixture(name, pin = false) {
  const path = 'conformance/keir-3/' + name, bytes = read(path);
  report.fixtureSha256[path] = hash(bytes);
  if (pin) assert.equal('sha256:' + hash(bytes), read(path.replace(/\.json$/, '.sha256')).toString().trim());
  return bytes;
}
function write(name, value) { writeFileSync(join(dir, name), Buffer.isBuffer(value) ? value : canonicalBytes(value)); }
function scans(n) { const a = Math.floor(n / 2); return n === 1 ? '(input.targetLabel in input.labels)' : '(' + scans(a) + ' || ' + scans(n - a) + ')'; }
const typicalModel = fixture('dcm.json', true), denseModel = fixture('dcm-inventory.json', true);
const dcm = JSON.parse(denseModel), inventory = JSON.parse(fixture('scenarios/inventory/inventory-max-no-match.json')).given.input;
assert.equal(inventory.labels.length, 16);
assert.equal(Buffer.byteLength(inventory.targetLabel), 256);
assert.ok(inventory.labels.every(s => Buffer.byteLength(s) === 256 && s !== inventory.targetLabel));
const denseSource = { language: 'keir/2', domain: { ...dcm.domain, fingerprint: 'sha256:' + hash(denseModel) }, entrypoint: 'InventoryArrived',
  name: 'http-dense-membership', steps: Array.from({ length: 8 }, (_, i) => ({ id: 'scan' + i, let: { cel: scans(10) } })),
  return: { record: { eligible: { cel: 'scan7' } } } };
const trace = [{ sequence: 0, event: 'INVOCATION_START' }];
for (let i = 0; i < 8; i++) {
  trace.push({ sequence: trace.length, event: 'STEP_START', step: 'scan' + i });
  trace.push({ sequence: trace.length, event: 'VALUE_BOUND', step: 'scan' + i });
}
trace.push({ sequence: trace.length, event: 'RETURNED' }); trace.push({ sequence: trace.length, event: 'TERMINATED' });
write('typical.source', fixture('source.yaml')); write('typical.model', typicalModel);
write('typical.expected', JSON.parse(fixture('scenarios/main/normal.json')).expect.outcome);
write('dense.source', denseSource); write('dense.model', denseModel); write('dense.input', inventory);
write('dense.expected', { classification: 'COMPLETED', result: { eligible: false }, effects: [], trace });
const agent = new Agent({ keepAlive: true, maxSockets: report.limits.outstandingGuard });
let port, server, sampler, rssPeak = 0, rssSamples = 0, rssBreach = false;
function http(path, id) {
  return new Promise(resolve => {
    let timer;
    const finish = value => { clearTimeout(timer); resolve(value); };
    const req = request({ hostname: '127.0.0.1', port, path, agent }, res => {
      let bytes = 0, body = '';
      res.on('data', b => { bytes += b.length; if (bytes > 16384) req.destroy(new Error('response-cap')); else body += b; });
      res.on('end', () => {
        try { finish({ status: res.statusCode, body: JSON.parse(body), id }); }
        catch { finish({ error: 'response-json', id }); }
      });
      res.on('error', () => finish({ error: 'response-stream', id }));
    });
    timer = setTimeout(() => { req.destroy(); finish({ error: 'absolute-timeout', id }); }, 5000);
    req.on('error', e => finish({ error: e.code ?? 'request-error', id })); req.end();
  });
}
async function stats(path = '/stats') { const r = await http(path); assert.equal(r.status, 200); return r.body; }
function distribution(values) {
  if (!values.length) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const q = p => sorted[Math.ceil(sorted.length * p) - 1];
  // Re-sort from raw samples to check recorded nearest-rank summaries at publication time.
  return { n: sorted.length, minMs: sorted[0], p50Ms: q(.5), p95Ms: q(.95), p99Ms: q(.99), maxMs: sorted.at(-1) };
}
let serial = 0;
async function stage(phase, rule, engine, rate, seconds, delay = 2) {
  assert.ok(rate > 0 && rate <= 4800 && seconds > 0 && seconds <= 60);
  const before = await stats('/reset'); rssPeak = 0; rssSamples = 0;
  const quotaBefore = cpuStat();
  const row = { phase, rule, engine, offeredPerSecond: rate, seconds, callbackDelayMs: rule === 'typical' ? delay : 0,
    offered: rate * seconds, correct: 0, busy: 0, generatorDrops: 0, transportErrors: 0, transportKinds: {}, incorrect: 0 };
  const raw = { successMs: [], responseMs: [], dispatchLagMs: [] };
  const inflight = new Set(); let cursor = 0;
  const start = performance.now();
  await new Promise(resolve => {
    function pump() {
      const elapsed = performance.now() - start;
      while (cursor < row.offered && cursor * 1000 / rate <= elapsed) {
        const planned = start + cursor++ * 1000 / rate;
        if (inflight.size >= report.limits.outstandingGuard) { row.generatorDrops++; continue; }
        const id = ++serial; raw.dispatchLagMs.push(performance.now() - planned);
        const operation = http('/run?rule=' + rule + '&mode=' + engine + '&delay=' + delay + '&id=' + id, id).then(r => {
          const elapsed = performance.now() - planned;
          if (r.error) { row.transportErrors++; row.transportKinds[r.error] = (row.transportKinds[r.error] ?? 0) + 1; }
          else {
            raw.responseMs.push(elapsed);
            if (r.status === 200 && r.body.correct === true && r.body.id === id) { row.correct++; raw.successMs.push(elapsed); }
            else if (r.status === 503 && r.body.busy === true) row.busy++;
            else row.incorrect++;
          }
        }).finally(() => inflight.delete(operation));
        inflight.add(operation);
      }
      if (cursor === row.offered && elapsed >= seconds * 1000) resolve(); else setTimeout(pump, 1);
    }
    pump();
  });
  await Promise.all(inflight); // Absolute per-request deadline also bounds the final drain.
  row.elapsedIncludingDrainMs = performance.now() - start;
  const after = await stats();
  assert.equal(after.active, 0); assert.equal(after.pendingCallbacks, 0);
  assert.equal(row.offered, row.correct + row.busy + row.generatorDrops + row.transportErrors + row.incorrect);
  assert.equal(after.correct, row.correct); assert.equal(after.busy, row.busy); assert.equal(after.failures, row.incorrect);
  row.successPerOfferedSecond = row.correct / seconds;
  row.successPerElapsedSecond = row.correct / (row.elapsedIncludingDrainMs / 1000);
  row.latency = { success: distribution(raw.successMs), allResponses: distribution(raw.responseMs), dispatchLag: distribution(raw.dispatchLagMs) };
  row.cpuSeconds = (after.cpuNanos - before.cpuNanos) / 1e9;
  row.meanOccupiedCores = row.cpuSeconds / (row.elapsedIncludingDrainMs / 1000);
  const quotaAfter = cpuStat();
  row.sharedCgroupThrottledUsec = quotaAfter.throttled_usec - quotaBefore.throttled_usec;
  row.sharedCgroupThrottledPeriods = quotaAfter.nr_throttled - quotaBefore.nr_throttled;
  row.sampledPeakRssBytes = rssPeak; row.rssSamples = rssSamples;
  row.heapUsedBeforeBytes = before.heapUsedBytes; row.heapUsedAfterBytes = after.heapUsedBytes;
  row.sampledPeakHeapBytes = after.sampledPeakHeapBytes; row.peakAdmitted = after.peakActive;
  row.cleanScreen = row.correct === row.offered && row.latency.success?.p99Ms < 50 && row.latency.dispatchLag?.p99Ms < 10;
  row.capacityScreen = row.cleanScreen ? 'passed' : 'failed';
  report.stages.push(row); save('result.json', report); save('samples-' + report.stages.length + '.json', raw);
  assert.ok(!rssBreach, 'RSS watchdog breached');
  assert.equal(row.incorrect, 0, 'Unexpected incorrect invocation/response');
  if (mode === '--smoke') assert.equal(row.correct, row.offered, 'Smoke requires every offered request to succeed');
  process.stderr.write(JSON.stringify({ phase, rule, engine, rate, correct: row.correct, busy: row.busy, drops: row.generatorDrops, p99Ms: row.latency.success?.p99Ms }) + '\n');
  return row;
}
try {
  // This image can run the javac module even when the javac launcher is absent.
  await requireExit(child('javac', java, ['-m', 'jdk.compiler/com.sun.tools.javac.Main', '-source', '21', '-target', '21', '-cp', cp, '-d', dir, join(here, 'HttpProbe.java')]));
  const compiled = await requireExit(child('compiler', java, ['-Xmx512m', '-cp', cp, 'dev.keir.benchmark.HttpProbe', 'compile', dir]));
  report.compilation = JSON.parse(compiled.out);
  assert.deepEqual(readFileSync(join(dir, 'typical.artifact')), fixture('artifact.json', true));
  report.generatedSha256 = Object.fromEntries(['typical', 'dense'].flatMap(r => ['source', 'model', 'artifact', 'expected', ...(r === 'dense' ? ['input'] : [])].map(s => [r + '.' + s, hash(readFileSync(join(dir, r + '.' + s)))])));
  const serverArgs = ['-c', hostCpus, java, '-Xmx512m', '-XX:ActiveProcessorCount=2', '-Dsun.net.httpserver.nodelay=true', '-cp', cp, 'dev.keir.benchmark.HttpProbe', 'serve', dir];
  server = child('server', 'taskset', serverArgs, 300000);
  const info = await ready(server); port = info.port;
  report.environment.server = { java: info.java, availableProcessors: info.availableProcessors, runtimePins: info.environment, tcpNoDelay: true,
    cpuList: readFileSync('/proc/' + server.p.pid + '/status', 'utf8').match(/^Cpus_allowed_list:\s*(.*)$/m)?.[1] };
  assert.equal(info.availableProcessors, 2); assert.deepEqual(cpuSet(report.environment.server.cpuList), cpuSet(hostCpus));
  sampler = setInterval(() => {
    try {
      const status = readFileSync('/proc/' + server.p.pid + '/status', 'utf8');
      const rss = Number(status.match(/^VmRSS:\s*(\d+)/m)?.[1]) * 1024;
      assert.ok(Number.isFinite(rss) && rss > 0); rssPeak = Math.max(rssPeak, rss); rssSamples++;
      if (rss > report.limits.sampledRssStopBytes) { rssBreach = true; server.stop('rss-watchdog'); }
    } catch { if (children.has(server.p)) { rssBreach = true; server.stop('sampler-failure'); } }
  }, 100);
  // Oracle failure is expected only here, before stage counters reset.
  const negative = await http('/run?rule=typical&mode=keir&delay=2&id=1&bad=1');
  assert.equal(negative.status, 500); assert.equal(negative.body.error, 'ASSERTION');
  report.negativeOracleRejected = true;
  if (mode === '--smoke') {
    for (const rule of ['typical', 'dense']) for (const engine of ['keir', 'native']) await stage('smoke', rule, engine, 10, 1);
  } else if (mode === '--sample') {
    await stage('cold-warmup', 'typical', 'keir', 300, 3);
    for (const rate of [300, 600, 1200]) await stage('ramp', 'typical', 'keir', rate, 4);
    await stage('cold-warmup', 'dense', 'keir', 300, 3);
    for (const rate of [600, 1200, 2400]) await stage('ramp', 'dense', 'keir', rate, 4);
    for (const [rule, fallback] of [['typical', 300], ['dense', 600]]) {
      const passing = report.stages.filter(r => r.phase === 'ramp' && r.rule === rule && r.cleanScreen);
      const rate = passing.length ? Math.max(...passing.map(r => r.offeredPerSecond)) : fallback;
      for (const engine of ['keir', 'native']) {
        await stage('warmup', rule, engine, rate, 3);
        await stage('confirmation', rule, engine, rate, 30);
      }
    }
  } else {
    // Focused completion after the first retained sample stopped on transport errors.
    for (const engine of ['keir', 'native']) {
      await stage('warmup', 'typical', engine, 300, 3);
      await stage('confirmation', 'typical', engine, 300, 30);
    }
    await stage('warmup', 'dense', 'native', 600, 3);
    await stage('confirmation', 'dense', 'native', 600, 30);
  }
  if (mode !== '--smoke') {
    await stage('slow-callbacks', 'typical', 'keir', 1200, 6, 20);
    await stage('recovery', 'typical', 'keir', 100, 3);
    assert.ok(report.stages.some(r => r.busy > 0 || r.generatorDrops > 0), 'No overload accounting exercised');
    assert.ok(report.stages.at(-1).cleanScreen, 'Recovery failed');
  }
  clearInterval(sampler); sampler = undefined;
  agent.destroy(); const closed = await requireExit(server);
  assert.ok(closed.out.includes('{"shutdown":true}'), 'Missing executor shutdown receipt');
  await portReleased(port);
  report.portReleased = true;
  const timeout = child('server-timeout-probe', 'taskset', serverArgs, 15000);
  const idle = await ready(timeout); timeout.deadline(100);
  const killed = await timeout.done; assert.equal(killed.reason, 'timeout'); assert.ok(killed.closed);
  await portReleased(idle.port);
  report.timeoutCleanupVerified = true;
  report.allConfirmationsClean = report.stages.filter(r => r.phase === 'confirmation').every(r => r.cleanScreen);
  report.status = 'completed'; // Collection checks passed; each capacity screen has its own verdict.
} catch (e) {
  report.status = 'failed'; report.failure = e.message;
  throw e;
} finally {
  clearInterval(sampler); agent.destroy();
  for (const p of children) p.kill('SIGKILL');
  // All child promises are resolved only after close; preserve failure receipts too.
  while (children.size) await pause(10);
  save('result.json', report);
  process.stderr.write('Evidence: build/' + dir.split('/').at(-1) + '/result.json\n');
}
