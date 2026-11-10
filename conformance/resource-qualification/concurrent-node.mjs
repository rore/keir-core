import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import * as api from '../../runtime-node/src/index.js';
import * as canonical from '../../runtime-node/src/canonical.js';
import { ARTIFACT_FIELDS } from '../../runtime-node/src/validation.js';

const [, , directory, manifestName] = process.argv;
const manifest = JSON.parse(readFileSync(join(directory, manifestName)));
const { limits } = manifest;
const state = new WeakMap(), blocks = [];
let id = 0, completedWarmups = 0, completedInvocations = 0, dispatchCount = 0, active = 0;
const nowMs = () => Math.floor(performance.now());
const us = () => Math.floor(performance.now() * 1000);
const retained = () => { global.gc(); return process.memoryUsage().heapUsed; };
const expected = canonical.canonicalBytes(manifest.expected);
const read = name => readFileSync(join(directory, name));
const checkpoint = () => writeFileSync(join(directory, 'concurrent-node-progress.json'), JSON.stringify({
  completedWarmups, completedInvocations, completedBlocks: blocks.length, blocks,
}));
function processObservation() {
  const peakRssBytes = process.resourceUsage().maxRSS * 1024;
  assert.ok(peakRssBytes <= limits.observedProcessBytes, 'Observed process memory exceeded workload budget');
  return { counter: 'Node process.resourceUsage().maxRSS, KiB converted to bytes', peakRssBytes };
}

async function run() {
  assert.ok(process.versions.node.startsWith('24.'));
  const modelBytes = read(manifest.model), model = JSON.parse(modelBytes), artifact = read(manifest.artifact);
  const input = JSON.parse(read(manifest.input));
  const implementations = Object.fromEntries(Object.entries(model.capabilities).map(([capability, cap]) => [capability, {
    version: cap.version, mode: 'simulation', keyType: model.hostKey.type,
    invoke(args, bindings, remaining, context) {
      const current = state.get(context.requestKey);
      assert.ok(current && context.requestKey === current.key, 'Original key identity changed');
      assert.deepEqual({ ...bindings }, { actorId: current.actor });
      assert.deepEqual(context.event, { id: current.event, source: 'task1' });
      assert.ok(remaining > 0 && remaining <= limits.invocationMs);
      dispatchCount++;
      const ordinal = current.calls++;
      if (capability === 'tasks.get') {
        assert.equal(ordinal, 0); assert.deepEqual({ ...args }, { taskId: 'task1' });
        return current.read;
      }
      assert.equal(capability, 'tasks.create'); assert.equal(ordinal, 1);
      assert.deepEqual({ ...args }, { projectId: 'project1', title: 'Follow up' });
      return { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
    },
  }]));
  const binding = api.bindModel(modelBytes, implementations); assert.equal(binding.diagnostic, null);
  const preparation = api.prepareProgram(artifact, binding.boundModel); assert.equal(preparation.diagnostic, null);
  async function block(count, concurrency, warmup = false) {
    const samples = new Array(count);
    for (let offset = 0; offset < count; offset += concurrency) {
      const batch = [], futures = [];
      try {
        for (let index = 0; index < Math.min(concurrency, count - offset); index++) {
          const key = Object.freeze({ toString() { throw Error('The native key must remain opaque'); } });
          const current = { key, actor: 'actor' + (++id), event: 'event' + id, calls: 0 };
          current.read = new Promise(resolve => { current.release = resolve; });
          state.set(key, current); active++; batch.push(current);
          const slot = offset + index, start = us();
          futures.push(api.invokeProgram(preparation.prepared, input, {
            bindings: { actorId: current.actor }, deadline: { clock: { nowMs }, expiresAtMs: nowMs() + limits.invocationMs },
            requestKey: key, event: { id: current.event, source: 'task1' },
          }).then(outcome => {
            samples[slot] = us() - start;
            assert.equal(current.calls, 2);
            assert.ok(canonical.canonicalBytes(outcome).equals(expected), 'Unexpected complete outcome');
            if (warmup) completedWarmups++; else completedInvocations++;
          }));
        }
        assert.ok(batch.every(current => current.calls === 1), 'Batch did not overlap at READ');
        for (const current of batch) current.release({ kind: 'SUCCESS', value: { id: 'task1', projectId: 'project1', status: 'COMPLETED' } });
        await Promise.all(futures);
      } finally { for (const current of batch) { state.delete(current.key); active--; } }
    }
    return samples;
  }
  processObservation();
  await block(limits.warmups, 32, true);
  const baselineHeapBytes = retained(); checkpoint(); processObservation();
  const distributions = [];
  for (const concurrency of limits.concurrency) {
    const samples = [];
    for (let index = 0; index < limits.blocks; index++) {
      const start = us(), cpu = process.cpuUsage();
      const part = await block(limits.perBlock, concurrency);
      const elapsedUs = us() - start, used = process.cpuUsage(cpu);
      samples.push(...part);
      blocks.push({ concurrency, block: index, checked: part.length, elapsedUs, cpuUs: used.user + used.system,
        retainedHeapBytes: retained(), process: processObservation() });
      checkpoint();
    }
    samples.sort((a, b) => a - b);
    distributions.push({ concurrency, checked: samples.length, medianUs: samples[Math.floor(samples.length / 2)],
      p95Us: samples[Math.ceil(samples.length * .95) - 1], maxUs: samples.at(-1) });
  }
  assert.equal(active, 0);
  const admission = await admissionCases(binding.boundModel);
  return { status: 'passed', node: process.version, completedWarmups, completedInvocations, dispatchCount,
    artifactDigest: canonical.digest(artifact), outcomeDigest: canonical.digest(expected), baselineHeapBytes,
    concurrency: distributions, blocks, admission, process: processObservation() };
}

async function admissionCases(boundModel) {
  const keys = Array.from({ length: 60_000 }, (_, i) => String(i)).sort();
  const prefix = '{' + keys.map(key => JSON.stringify(key) + ':0').join(',') + ',"zzPad":"';
  const bytes = Buffer.from(prefix + 'x'.repeat(1_048_576 - Buffer.byteLength(prefix) - 2) + '"}');
  assert.equal(bytes.length, 1_048_576);
  const wide = Object.fromEntries(Array.from({ length: 300_000 }, (_, i) => [String(i), 0]));
  const modules = { current: canonical };
  if (manifest.baselineModule) modules.baseline = await import(pathToFileURL(join(directory, manifest.baselineModule)));
  for (const module of Object.values(modules)) {
    assert.deepEqual(module.parseCanonical(Buffer.from('{"a":0}')), { a: 0 });
    assert.equal(module.canonicalBytes({ a: 0 }).toString(), '{"a":0}');
  }
  const samples = [], callsBefore = dispatchCount;
  for (let round = 0; round < 3; round++) {
    for (const name of round % 2 ? Object.keys(modules).reverse() : Object.keys(modules)) {
      const module = modules[name]; retained();
      // Both versions apply the same closed-envelope rule; only the current decoder can preflight it.
      const start = us();
      let rejected = false;
      try {
        const value = module.parseCanonical(bytes, 1_048_576, ARTIFACT_FIELDS.length);
        rejected = Object.keys(value).length !== ARTIFACT_FIELDS.length;
      } catch { rejected = true; }
      const elapsedUs = us() - start; assert.ok(rejected);
      samples.push({ round, implementation: name, operation: 'canonical decoding plus closed root validation', elapsedUs });
      retained();
      const oversizedStart = us();
      assert.throws(() => module.canonicalBytes(wide), /Canonical JSON exceeds byte limit/);
      samples.push({ round, implementation: name, operation: 'oversized wide-object serialization', elapsedUs: us() - oversizedStart });
    }
    retained();
    let tick;
    const pendingTick = new Promise(resolve => setTimeout(() => { tick = us(); resolve(); }, 0));
    const start = us(), result = api.prepareProgram(bytes, boundModel), elapsedUs = us() - start;
    await pendingTick;
    assert.equal(result.prepared, null);
    assert.deepEqual({ code: result.diagnostic.code, path: result.diagnostic.path }, { code: 'ARTIFACT_INVALID', path: '/artifact' });
    samples.push({ round, implementation: 'current', operation: 'public prepareProgram', elapsedUs, eventLoopTickUs: tick - start });
    processObservation();
  }
  assert.equal(dispatchCount, callsBefore, 'Admission invoked an adapter');
  return { artifactBytes: bytes.length, rootFields: 60_001, oversizedObjectFields: 300_000, dispatchCount: 0, samples };
}

try { console.log(JSON.stringify(await run())); }
catch (failure) {
  checkpoint();
  console.log(JSON.stringify({ status: 'failed', completedWarmups, completedInvocations, dispatchCount,
    error: String(failure).slice(0, 1000) }));
  process.exitCode = 1;
}
