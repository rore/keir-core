import assert from 'node:assert/strict';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { delimiter, join } from 'node:path';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';

function scans(count) {
  if (count === 1) return '(input.targetLabel in input.labels)';
  const left = Math.floor(count / 2);
  return '(' + scans(left) + ' || ' + scans(count - left) + ')';
}

export async function runDenseQualification(context) {
  const { here, fixture, compiler, host, jdk, temporary, limits, child, runJvm, requireExit, metrics, output } = context;
  const start = performance.now();
  const remaining = () => {
    const left = 3 * limits.childMs - (performance.now() - start);
    assert.ok(left > 0, 'Dense workload exhausted its total 135-second budget');
    return Math.min(limits.childMs, left);
  };
  const write = (name, bytes) => { writeFileSync(join(temporary, name), bytes); return name; };
  const pinned = name => {
    const bytes = readFileSync(join(fixture, name + '.json'));
    assert.equal(digest(bytes), readFileSync(join(fixture, name + '.sha256'), 'ascii').trim());
    return bytes;
  };
  const model = pinned('dcm-inventory'), dcm = JSON.parse(model);
  const scenarioBytes = readFileSync(join(fixture, 'scenarios/inventory/inventory-max-no-match.json'));
  const input = JSON.parse(scenarioBytes).given.input;
  assert.equal(input.labels.length, 16);
  assert.ok(input.labels.every(label => Buffer.byteLength(label) === 256 && label !== input.targetLabel));
  assert.equal(Buffer.byteLength(input.targetLabel), 256);
  const expression = scans(10);
  assert.ok(Buffer.byteLength(expression) <= limits.expressionSourceBytes);
  const source = canonicalBytes({
    language: 'keir/2', domain: { ...dcm.domain, fingerprint: digest(model) }, entrypoint: 'InventoryArrived',
    name: 'dense-bounded-membership',
    steps: Array.from({ length: 8 }, (_, index) => ({ id: 'scan' + index, let: { cel: expression } })),
    return: { record: { eligible: { cel: 'scan7' } } },
  });
  const trace = [{ sequence: 0, event: 'INVOCATION_START' }];
  for (let index = 0; index < 8; index++) {
    trace.push({ sequence: trace.length, event: 'STEP_START', step: 'scan' + index });
    trace.push({ sequence: trace.length, event: 'VALUE_BOUND', step: 'scan' + index });
  }
  trace.push({ sequence: trace.length, event: 'RETURNED' }, { sequence: trace.length + 1, event: 'TERMINATED' });
  const expected = { classification: 'COMPLETED', result: { eligible: false }, effects: [], trace };
  const manifest = {
    model: write('dense.dcm', model), artifact: 'dense.json', input: write('dense-input.json', canonicalBytes(input)),
    source: write('dense-source.json', source),
    limits: { warmups: 100, blocks: 5, perBlock: 1000, invocationMs: 10_000,
      peakWorkingSetBytes: limits.soak.peakWorkingSetBytes }, expected,
  };
  const manifestName = write('dense-manifest.json', canonicalBytes(manifest));
  output.pins = { model: digest(model), source: digest(source), input: digest(canonicalBytes(input)),
    referenceScenario: digest(scenarioBytes), inputBytes: canonicalBytes(input).length,
    expressionSourceBytes: Buffer.byteLength(expression), expected: digest(canonicalBytes(expected)) };
  output.limits = { ...manifest.limits, childMs: limits.childMs, captureBytes: limits.captureBytes,
    heapMiB: limits.soak.heapMiB, totalMs: 3 * limits.childMs };
  const classpath = [compiler, host].join(delimiter);
  const setup = await child(jdk('javac'), ['-cp', classpath, '-d', temporary, join(here, 'ResourceProbe.java')],
    { timeout: remaining() });
  output.probeCompilation = metrics(setup);
  requireExit(setup);
  const java = mode => ['-Xmx256m', '-cp', [temporary, compiler, host].join(delimiter),
    'dev.keir.runtime.ResourceProbe', mode, temporary, manifestName];
  const producer = await child(jdk('java'), java('dense-compiler'),
    { timeout: Math.min(remaining(), Math.max(1, limits.childMs - setup.elapsedMs)) });
  output.producer = { process: metrics(producer) };
  requireExit(producer);
  Object.assign(output.producer, JSON.parse(producer.stdout));
  const artifact = readFileSync(join(temporary, manifest.artifact));
  output.pins.artifact = digest(artifact);
  assert.equal(output.producer.artifactDigest, output.pins.artifact);
  assert.deepEqual(output.producer.nodesPerStep, Array(8).fill(59));
  assert.equal(output.producer.totalNodes, 473);
  assert.equal(output.producer.membershipScans, 80);
  assert.equal(output.producer.elementVisits, 1280);
  assert.equal(output.producer.calls, 0);
  assert.ok(output.producer.depth <= limits.expressionDepth);
  output.runtimes = {};
  for (const engine of ['java', 'node']) {
    const run = engine === 'java'
      ? await runJvm(java('dense-runtime'), { timeout: remaining() })
      : await child(process.execPath, ['--expose-gc', '--max-old-space-size=256',
        join(here, 'dense-node.mjs'), temporary, manifestName], { timeout: remaining() });
    const progressPath = join(temporary, 'dense-' + engine + '-progress.json');
    output.runtimes[engine] = { process: metrics(run), workingSet: run.workingSet,
      ...(existsSync(progressPath) ? { lastCheckpoint: JSON.parse(readFileSync(progressPath)) } : {}) };
    if (run.stdout.length) output.runtimes[engine].report = JSON.parse(run.stdout);
    requireExit(run);
    const report = output.runtimes[engine].report;
    assert.equal(report.status, 'passed');
    assert.equal(report.completedWarmups, 100);
    assert.equal(report.completedInvocations, 5000);
    assert.equal(report.dispatchCount, 0);
    assert.equal(report.artifactDigest, output.pins.artifact);
    assert.equal(report.outcomeDigest, output.pins.expected);
  }
  output.elapsedMs = performance.now() - start;
  assert.ok(output.elapsedMs <= 3 * limits.childMs, 'Dense workload exceeded its total execution budget');
  output.status = 'passed';
}
