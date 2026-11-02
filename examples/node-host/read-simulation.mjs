import { openSync, readSync, closeSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { runReadSimulation } from '@keir/runtime-node/read-simulation';

function read(path, max) {
  const descriptor = openSync(path, 'r');
  try {
    const buffer = Buffer.alloc(max + 1); let length = 0;
    while (length < buffer.length) { const count = readSync(descriptor, buffer, length, buffer.length - length, null); if (!count) break; length += count; }
    if (length > max) throw new Error('Bounded simulation input');
    return Buffer.from(buffer.subarray(0, length));
  } finally { closeSync(descriptor); }
}

export async function runExample(bundleDir, fixtureId, caseFile) {
  try {
    if (!['normal', 'denied', 'consumer-throws', 'consumer-unlisted', 'consumer-wrong'].includes(fixtureId)) throw new Error('Unknown fixture');
    const selection = JSON.parse(read(resolve(bundleDir, 'selection.json'), 4096));
    selection.dcmBytes = read(resolve(bundleDir, 'dcm.json'), 65536);
    selection.artifactBytes = read(resolve(bundleDir, 'artifact.json'), 1048576);
    const fixture = {
      scope: 'read-simulation', bindings: { actorId: 'user1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 },
      authorize: async () => fixtureId !== 'denied',
      allowedReceipts: [...['DONE', 'PENDING', 'NEEDS_ATTENTION'].map(code => ({ status: 'DISPLAYED', code })), { status: 'NOT_DISPLAYED' }],
      consume(outcome) {
        if (fixtureId === 'consumer-throws') throw new Error('Consumer failed');
        if (fixtureId === 'consumer-unlisted') return { status: 'DISPLAYED', code: 'UNLISTED' };
        const receipt = outcome.classification === 'COMPLETED' ? { status: 'DISPLAYED', code: outcome.result.classification } : { status: 'NOT_DISPLAYED' };
        return fixtureId === 'consumer-wrong' ? { status: 'DISPLAYED', code: 'DONE' } : receipt;
      },
      responseCheck: (capability, args, response) => capability !== 'tasks.get' || response.kind !== 'SUCCESS' || response.value.id === args.taskId,
    };
    return await runReadSimulation(selection, fixture, read(caseFile, 65536));
  } catch { return runReadSimulation(null, null, new Uint8Array()); }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const report = process.argv.length === 5 ? await runExample(...process.argv.slice(2)) : await runReadSimulation(null, null, new Uint8Array());
  // Closed reports contain only ASCII tokens, pins and bounded integers.
  process.stdout.write(JSON.stringify(report, ['artifactDigest', 'code', 'consumption', 'format', 'identity', 'mismatches', 'modelDigest', 'ordinal', 'stage', 'status']));
}
