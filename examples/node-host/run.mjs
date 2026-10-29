import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { performance } from 'node:perf_hooks';
import { prepare, invoke, ENVIRONMENT } from '@keir/runtime-node';

const consumer = Object.freeze({ id: 'sample.classification', version: '1', kind: 'DISPLAY' });
function consume(outcome) {
  if (outcome.classification !== 'COMPLETED') return { status: 'NOT_DISPLAYED', code: outcome.error.code };
  const result = outcome.result;
  if (!result || Object.keys(result).length !== 1 || !['DONE', 'PENDING'].includes(result.classification)) return { status: 'NOT_DISPLAYED', code: 'OUTPUT_INVALID' };
  return { status: 'DISPLAYED', result: { classification: result.classification } };
}

export async function runScenarios(bytes, expectedArtifactDigest, expectedDcmDigest) {
  const outputs = [];
  for (const [scenario, row] of [['completed', { id: 'task1', status: 'COMPLETED' }], ['open', { id: 'task1', status: 'OPEN' }], ['missing', undefined], ['wrong-task', { id: 'different-task', status: 'COMPLETED' }]]) {
    const calls = [];
    const selected = prepare(bytes, { expectedArtifactDigest, expectedDcmDigest, entrypoint: 'ClassifyTask', consumer, compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT, mode: 'simulation', grants: { 'tasks.get': 1 } }, {
      'tasks.get': { version: '1', mode: 'simulation', invoke: async (agentArgs, protectedArgs, remainingMs) => {
        calls.push({ agentArgs: { ...agentArgs }, protectedArgs: { ...protectedArgs } });
        if (remainingMs <= 0) throw new Error('Adapter budget exhausted');
        if (protectedArgs.actorId !== 'user1') return { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED' };
        if (!row) return { kind: 'BUSINESS_REJECTION', code: 'TASK_NOT_FOUND' };
        // The host adapter owns the requested-resource relationship; the generic runtime cannot infer it.
        if (row.id !== agentArgs.taskId) throw new Error('Scenario adapter returned the wrong resource');
        return { kind: 'SUCCESS', value: { ...row } };
      } }
    });
    if (selected.code) throw new Error('Artifact admission failed');
    const clock = { nowMs: () => Math.floor(performance.now()) };
    const outcome = await invoke(selected, { taskId: 'task1' }, {
      bindings: { actorId: 'user1' }, grants: { 'tasks.get': 1 }, deadline: { clock, expiresAtMs: clock.nowMs() + 1000 },
      authorize: async request => request.entrypoint === 'ClassifyTask' && (!request.capability || request.capability === 'tasks.get' && request.version === '1')
    });
    outputs.push({ scenario, calls, outcome, consumption: consume(outcome) });
  }
  return outputs;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const [artifactPath, expectedArtifactDigest, expectedDcmDigest] = process.argv.slice(2);
  try {
    if (!artifactPath || !expectedArtifactDigest || !expectedDcmDigest) throw new Error('Missing host selection');
    console.log(JSON.stringify(await runScenarios(readFileSync(artifactPath), expectedArtifactDigest, expectedDcmDigest), null, 2));
  } catch {
    console.error('The selected sample artifact could not be executed. Supply its path and independently held artifact/model digests.');
    process.exitCode = 1;
  }
}
