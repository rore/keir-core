import assert from 'node:assert/strict';
import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { delimiter, join } from 'node:path';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';

export async function runConcurrentQualification(context) {
  const { here, fixture, compiler, host, jdk, temporary, limits, child, requireExit, metrics, output } = context;
  const start = performance.now(), totalMs = 3 * limits.childMs;
  const remaining = () => {
    const left = totalMs - (performance.now() - start);
    assert.ok(left > 0, 'Concurrent qualification exhausted its total budget');
    return Math.min(limits.childMs, left);
  };
  const write = (name, content) => { writeFileSync(join(temporary, name), content); return name; };
  const pinned = name => {
    const bytes = readFileSync(join(fixture, name + '.json'));
    assert.equal(digest(bytes), readFileSync(join(fixture, name + '.sha256'), 'ascii').trim());
    return bytes;
  };
  const model = pinned('dcm'), artifact = pinned('producers/0.4.0/artifact');
  assert.equal(JSON.parse(model).format, 'keir-dcm/3');
  const source = readFileSync(join(fixture, 'source.yaml'));
  const scenario = JSON.parse(readFileSync(join(fixture, 'scenarios/main/normal.json')));
  const manifest = {
    model: write('concurrent.dcm', model), artifact: 'concurrent.artifact',
    source: write('concurrent.source', source), input: write('concurrent-input.json', canonicalBytes(scenario.given.input)),
    expected: scenario.expect.outcome,
    limits: { warmups: 128, blocks: 3, perBlock: 512, concurrency: [1, 32, 128], invocationMs: 10_000,
      blockedClocks: 64, observedProcessBytes: 512 * 1_048_576 },
  };
  output.pins = { model: digest(model), source: digest(source), artifact: digest(artifact),
    expected: digest(canonicalBytes(manifest.expected)) };
  output.limits = { ...manifest.limits, heapMiB: 256, childMs: limits.childMs,
    captureBytes: limits.captureBytes, totalMs, virtualSchedulerParallelism: 2, virtualSchedulerMaxPoolSize: 4 };
  const classpath = [compiler, host].join(delimiter);
  const setup = await child(jdk('javac'), ['-cp', classpath, '-d', temporary, join(here, 'ConcurrentProbe.java')],
    { timeout: remaining() });
  requireExit(setup); output.probeCompilation = metrics(setup);
  const produced = await child(jdk('java'), ['-Xmx256m', '-jar', compiler,
    join(temporary, manifest.source), join(temporary, manifest.model)],
  { timeout: Math.min(remaining(), Math.max(1, limits.childMs - setup.elapsedMs)) });
  requireExit(produced); assert.deepEqual(produced.stdout, artifact);
  write(manifest.artifact, produced.stdout); output.producer = metrics(produced);
  if (process.env.KEIR_CANONICAL_BASELINE_REF) {
    const resolved = await child('git', ['rev-parse', '--verify', '--end-of-options', process.env.KEIR_CANONICAL_BASELINE_REF + '^{commit}'],
      { timeout: remaining() });
    requireExit(resolved);
    const commit = resolved.stdout.toString().trim(); assert.match(commit, /^[0-9a-f]{40}$/);
    const baseline = await child('git', ['show', commit + ':runtime-node/src/canonical.js'], { timeout: remaining() });
    requireExit(baseline); manifest.baselineModule = write('baseline-canonical.mjs', baseline.stdout);
    output.baseline = { commit, path: 'runtime-node/src/canonical.js', moduleDigest: digest(baseline.stdout) };
  }
  const manifestName = write('concurrent-manifest.json', canonicalBytes(manifest));
  output.runtimes = {};
  for (const engine of ['java', 'node']) {
    const executable = engine === 'java' ? jdk('java') : process.execPath;
    const args = engine === 'java'
      ? ['-Xmx256m', '-Djdk.virtualThreadScheduler.parallelism=2', '-Djdk.virtualThreadScheduler.maxPoolSize=4',
        '-cp', [temporary, host].join(delimiter), 'dev.keir.runtime.ConcurrentProbe', temporary, manifestName]
      : ['--expose-gc', '--max-old-space-size=256', join(here, 'concurrent-node.mjs'), temporary, manifestName];
    const run = await child(executable, args, { timeout: remaining() });
    const progress = join(temporary, 'concurrent-' + engine + '-progress.json');
    const report = run.stdout.length ? JSON.parse(run.stdout) : undefined;
    output.runtimes[engine] = { process: metrics(run), ...(report ? { report } : {}),
      ...(existsSync(progress) ? { lastCheckpoint: JSON.parse(readFileSync(progress)) } : {}) };
    requireExit(run);
    assert.equal(report.status, 'passed');
    assert.equal(report.completedWarmups, manifest.limits.warmups);
    assert.equal(report.completedInvocations, manifest.limits.concurrency.length * manifest.limits.blocks * manifest.limits.perBlock);
    assert.equal(report.dispatchCount, 2 * (report.completedWarmups + report.completedInvocations));
    assert.equal(report.artifactDigest, output.pins.artifact); assert.equal(report.outcomeDigest, output.pins.expected);
    assert.equal(report.blocks.length, manifest.limits.blocks * manifest.limits.concurrency.length);
    assert.deepEqual(report.concurrency.map(row => row.checked), manifest.limits.concurrency.map(() => manifest.limits.blocks * manifest.limits.perBlock));
  }
  assert.equal(output.runtimes.java.report.blockedClocks.checkedBudgetFailures, manifest.limits.blockedClocks);
  assert.equal(output.runtimes.java.report.blockedClocks.virtualChecks, manifest.limits.blockedClocks);
  assert.equal(output.runtimes.java.report.blockedClocks.platformChecks, 0);
  assert.equal(output.runtimes.node.report.admission.dispatchCount, 0);
  output.failureChecks = [];
  for (const fault of ['outcome', 'memory']) {
    const directory = join(temporary, 'negative-' + fault); mkdirSync(directory);
    for (const name of [manifest.model, manifest.artifact, manifest.input, 'dev']) {
      cpSync(join(temporary, name), join(directory, name), { recursive: true });
    }
    const negative = structuredClone(manifest);
    if (fault === 'outcome') negative.expected.result.status = 'WRONG';
    else negative.limits.observedProcessBytes = 1;
    writeFileSync(join(directory, manifestName), canonicalBytes(negative));
    for (const engine of ['java', 'node']) {
      if (engine === 'java' && fault === 'memory' && !output.runtimes.java.report.process.supported) {
        output.failureChecks.push({ fault, engine, status: 'unsupported', reason: 'Java process memory requires Linux /proc/self/status' });
        continue;
      }
      const args = engine === 'java'
        ? ['-Xmx256m', '-cp', [directory, host].join(delimiter), 'dev.keir.runtime.ConcurrentProbe', directory, manifestName]
        : ['--expose-gc', '--max-old-space-size=256', join(here, 'concurrent-node.mjs'), directory, manifestName];
      const run = await child(engine === 'java' ? jdk('java') : process.execPath, args, { timeout: remaining() });
      requireExit(run, 1);
      const report = JSON.parse(run.stdout);
      assert.equal(report.status, 'failed');
      assert.equal(report.completedWarmups, 0); assert.equal(report.completedInvocations, 0);
      assert.match(report.error, fault === 'outcome' ? /Unexpected complete outcome/ : /Observed process memory exceeded workload budget/);
      assert.equal(report.dispatchCount, fault === 'outcome' ? 64 : 0);
      output.failureChecks.push({ fault, engine, process: metrics(run), report });
    }
  }
  output.status = 'passed'; output.elapsedMs = performance.now() - start;
  assert.ok(output.elapsedMs <= totalMs);
}
