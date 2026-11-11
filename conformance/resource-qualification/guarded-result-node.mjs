import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { bindModel, prepareProgram, invokeProgram } from '../../runtime-node/src/index.js';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';

const [, , directory, manifestName] = process.argv;
const read = name => readFileSync(join(directory, name));
const manifest = JSON.parse(read(manifestName)), { limits } = manifest;
const outcomeDigests = [], blocks = [];
let current, callbackError, completedWarmups = 0, completedInvocations = 0, dispatchCount = 0, maxRetainedResults = 0;
const reference = (index, step) => ('r' + index + '-' + step).padEnd(256, 'r');
const retained = () => { global.gc(); return process.memoryUsage().heapUsed; };
const progress = () => writeFileSync(join(directory, 'guarded-node-progress.json'), JSON.stringify({
  completedWarmups, completedInvocations, dispatchCount, blocks,
}));

async function run() {
  assert.ok(process.versions.node.startsWith('24.'));
  assert.ok([8192, 8193].includes(canonicalBytes(manifest.value).length));
  const modelBytes = read(manifest.model), model = JSON.parse(modelBytes), artifact = read(manifest.artifact);
  const binding = bindModel(modelBytes, Object.fromEntries(Object.entries(model.capabilities).map(([id, capability]) => [id, {
    version: capability.version, mode: 'simulation', keyType: model.hostKey.type,
    invoke(args, protectedArgs, remaining, context) {
      try {
        assert.equal(context.requestKey, current.key);
        assert.deepEqual(context.event, { id: 'event', source: 'existing' });
        assert.deepEqual({ ...protectedArgs }, { actorId: 'actor' });
        assert.ok(remaining > 0 && remaining <= limits.invocationMs);
        assert.equal(id, current.example.calls[current.ordinal]);
        assert.deepEqual({ ...args }, id === 'records.create' ? { label: 'label' } : { reference: 'existing' });
        const step = current.steps[current.ordinal++]; dispatchCount++;
        const value = { ...manifest.value, reference: reference(current.index, step) };
        return { kind: 'SUCCESS', value, ...(id === 'records.create' ? { effect: 'CONFIRMED' } : {}) };
      } catch (error) { callbackError = error; throw error; }
    },
  }])));
  assert.equal(binding.diagnostic, null);
  const preparation = prepareProgram(artifact, binding.boundModel);
  assert.equal(preparation.diagnostic, null);
  async function invoke(index) {
    const example = manifest.expected[index % manifest.expected.length];
    current = { key: Object.freeze({}), index, example, ordinal: 0,
      steps: ['a0', 'a1', 'a2', 'a3'].filter((step, i) => i === 0 || i === 3 ? example.input.createNew : example.input.enabled) };
    const expected = structuredClone(example.outcome);
    if (example.input.createNew && expected.result) expected.result.reference = reference(index, 'a3');
    const outcome = await invokeProgram(preparation.prepared, example.input, {
      bindings: { actorId: 'actor' }, requestKey: current.key, event: { id: 'event', source: 'existing' },
      deadline: { clock: { nowMs: () => Math.floor(performance.now()) }, expiresAtMs: Math.floor(performance.now()) + limits.invocationMs },
    });
    if (callbackError) throw callbackError;
    assert.equal(current.ordinal, example.calls.length);
    assert.ok(canonicalBytes(outcome).equals(canonicalBytes(expected)), 'Unexpected complete outcome');
    if (outcome.classification === 'COMPLETED') maxRetainedResults = Math.max(maxRetainedResults, current.steps.length);
    outcomeDigests.push(digest(canonicalBytes(outcome))); current = undefined;
  }
  for (let index = 0; index < limits.warmups; index++) { await invoke(index); completedWarmups++; }
  const baselineHeapBytes = retained(); progress();
  for (let block = 0; block < limits.blocks; block++) {
    const start = performance.now(), cpu = process.cpuUsage();
    for (let index = 0; index < limits.perBlock; index++) {
      await invoke(limits.warmups + completedInvocations); completedInvocations++;
    }
    const used = process.cpuUsage(cpu);
    blocks.push({ block, checked: limits.perBlock, elapsedMs: performance.now() - start,
      cpuUs: used.user + used.system, retainedHeapBytes: retained() }); progress();
  }
  return { status: 'passed', completedWarmups, completedInvocations, dispatchCount, maxRetainedResults,
    resultBytes: canonicalBytes(manifest.value).length, artifactDigest: digest(artifact),
    outcomesDigest: digest(canonicalBytes(outcomeDigests)), baselineHeapBytes, blocks,
    process: { counter: 'Node process.resourceUsage().maxRSS, KiB converted to bytes', peakRssBytes: process.resourceUsage().maxRSS * 1024 } };
}
try { console.log(JSON.stringify(await run())); }
catch (error) {
  progress(); console.log(JSON.stringify({ status: 'failed', completedWarmups, completedInvocations,
    dispatchCount, error: String(error.message).slice(0, 1000) })); process.exitCode = 1;
}
