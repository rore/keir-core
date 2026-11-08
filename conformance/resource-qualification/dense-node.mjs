import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import * as api from '../../runtime-node/src/index.js';
import { canonicalBytes, digest, parseCanonical, parseModelJson } from '../../runtime-node/src/canonical.js';

const [, , directory, manifest] = process.argv;
let completedWarmups = 0, completedInvocations = 0, dispatchCount = 0;
try {
  assert.equal(typeof global.gc, 'function', 'Run with --expose-gc');
  const read = name => readFileSync(join(directory, name));
  const { model, artifact, input: inputFile, limits, expected } = JSON.parse(read(manifest));
  const { warmups, blocks, perBlock, invocationMs, peakWorkingSetBytes } = limits;
  const modelBytes = read(model), dcm = parseModelJson(modelBytes);
  const inventory = readFileSync(new URL('../keir-2/dcm-inventory.json', import.meta.url));
  assert.ok(canonicalBytes(dcm).equals(canonicalBytes(parseModelJson(inventory))), 'Expected unchanged Inventory model');
  const artifactBytes = read(artifact), plan = parseCanonical(artifactBytes), input = parseCanonical(read(inputFile));
  assert.equal(plan.steps.length, 8);
  assert.ok(plan.steps.every(step => step.op === 'BIND'));
  assert.equal(input.labels.length, 16);
  assert.ok(input.labels.every(label => Buffer.byteLength(label) === 256));
  assert.equal(Buffer.byteLength(input.targetLabel), 256);
  assert.ok(input.labels.every(label => label !== input.targetLabel));
  const implementations = Object.fromEntries(Object.entries(dcm.capabilities).map(([id, cap]) => [id, {
    version: cap.version, mode: 'simulation', keyType: dcm.hostKey.type,
    invoke() { dispatchCount++; throw Error('Unexpected capability dispatch'); },
  }]));
  const binding = api.bindModel(modelBytes, implementations);
  assert.equal(binding.diagnostic, null);
  const prepareCpu = process.cpuUsage(), prepareStart = performance.now();
  const preparation = api.prepareProgram(artifactBytes, binding.boundModel);
  const prepareWallUs = (performance.now() - prepareStart) * 1000;
  const prepareUsage = process.cpuUsage(prepareCpu);
  assert.equal(preparation.diagnostic, null);
  const expectedBytes = canonicalBytes(expected);
  const samples = [], blockResults = [];
  const retained = () => { global.gc(); return process.memoryUsage().heapUsed; };
  const check = outcome => {
    assert.ok(canonicalBytes(outcome).equals(expectedBytes), 'Outcome differs from expected canonical snapshot');
    assert.equal(dispatchCount, 0);
  };
  async function invoke() {
    const poison = () => { throw Error('Opaque request key cannot be converted'); };
    const requestKey = { toString: poison, valueOf: poison, toJSON: poison, [Symbol.toPrimitive]: poison };
    const expiresAtMs = Math.floor(performance.now()) + invocationMs;
    const invocation = { bindings: {}, requestKey, event: { id: 'event1', source: 'item1' },
      deadline: { clock: { nowMs: () => Math.floor(performance.now()) }, expiresAtMs } };
    const start = performance.now();
    const outcome = await api.invokeProgram(preparation.prepared, input, invocation);
    const elapsedUs = (performance.now() - start) * 1000;
    return { outcome, elapsedUs };
  }
  for (let i = 0; i < warmups; i++) {
    const { outcome } = await invoke(); check(outcome); completedWarmups++;
  }
  const baselineHeapBytes = retained();
  const progress = () => writeFileSync(join(directory, 'dense-node-progress.json'), JSON.stringify({
    completedWarmups, completedInvocations, completedBlocks: blockResults.length, baselineHeapBytes, blocks: blockResults,
  }));
  progress();
  for (let block = 1; block <= blocks; block++) {
    const blockCpu = process.cpuUsage();
    const blockSamples = [];
    for (let i = 0; i < perBlock; i++) {
      const { outcome, elapsedUs } = await invoke();
      check(outcome); completedInvocations++;
      blockSamples.push(elapsedUs);
    }
    const usage = process.cpuUsage(blockCpu), cpuUs = usage.user + usage.system;
    samples.push(...blockSamples);
    const postGcHeapBytes = retained(), peakRssBytes = process.resourceUsage().maxRSS * 1024;
    blockResults.push({ block, invocations: perBlock, cpuUs, postGcHeapBytes, peakRssBytes });
    progress();
    assert.ok(peakRssBytes <= peakWorkingSetBytes, 'Peak RSS exceeds configured ceiling');
  }
  const finalHeaps = blockResults.slice(-3).map(block => block.postGcHeapBytes);
  assert.equal(finalHeaps.length, 3);
  const finalHeapSpanBytes = Math.max(...finalHeaps) - Math.min(...finalHeaps);
  const heapSpanLimitBytes = Math.max(8 * 1024 * 1024, baselineHeapBytes * 0.1);
  assert.ok(finalHeapSpanBytes <= heapSpanLimitBytes, 'Final post-GC heap span exceeds screening threshold');
  samples.sort((a, b) => a - b);
  const runtimePackage = JSON.parse(readFileSync(new URL('../../runtime-node/package.json', import.meta.url)));
  console.log(JSON.stringify({ status: 'passed', completedWarmups, completedInvocations, dispatchCount,
    artifactDigest: digest(artifactBytes), outcomeDigest: digest(expectedBytes),
    environment: { runtime: 'node', version: process.versions.node, platform: process.platform, arch: process.arch,
      versions: process.versions, pins: api.ENVIRONMENT, dependencies: runtimePackage.dependencies },
    prepare: { wallUs: prepareWallUs, cpuUs: prepareUsage.user + prepareUsage.system },
    wall: { minUs: samples[0], medianUs: samples[Math.floor(samples.length / 2)],
      p95Us: samples[Math.ceil(samples.length * 0.95) - 1], maxUs: samples.at(-1) },
    baselineHeapBytes, finalHeapSpanBytes, heapSpanLimitBytes, blocks: blockResults }));
} catch (error) {
  console.log(JSON.stringify({ status: 'failed', completedWarmups, completedInvocations,
    error: String(error?.message ?? error).slice(0, 1000) }));
  process.exitCode = 1;
}
