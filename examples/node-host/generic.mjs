import assert from 'node:assert/strict';
import { openSync, readSync, closeSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import * as api from '@keir/runtime-node';
import { runBatch } from './generic-simulation.mjs';

export async function runGeneric(directory) {
  const load = (name, max) => {
    const descriptor = openSync(resolve(directory, name), 'r');
    try {
      const buffer = Buffer.alloc(max + 1); let length = 0;
      while (length < buffer.length) { const read = readSync(descriptor, buffer, length, buffer.length - length, null); if (!read) break; length += read; }
      assert.ok(length <= max, 'Bounded trusted fixture file'); return Buffer.from(buffer.subarray(0, length));
    } finally { closeSync(descriptor); }
  };
  const dcmBytes = load('dcm.json', 65536);
  const dcm = JSON.parse(dcmBytes), effect = dcm.format.startsWith('keir-dcm/effect-'), effect3 = dcm.format === 'keir-dcm/effect-3';
  assert.ok(['keir-dcm/read-2', 'keir-dcm/effect-2', 'keir-dcm/effect-3'].includes(dcm.format));
  const names = effect3 ? ['main', 'zero', 'after-write-read'] : effect ? ['main', 'project2', 'title2'] : ['main', 'repeated', 'alternate'];
  const programs = Object.fromEntries(names.map(name => {
    const stem = name === 'main' ? 'artifact' : 'artifact-' + name;
    const bytes = load(stem + '.json', 1048576);
    return [name, { bytes, digest: load(stem + '.sha256', 128).toString('utf8').trim() }];
  }));
  const scenarioBytes = load('scenarios.json', 1048576);
  return runBatch(api, { dcmBytes, dcmDigest: load('dcm.sha256', 128).toString('utf8').trim(), programs }, JSON.parse(scenarioBytes));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    assert.equal(process.argv.length, 3);
    const output = JSON.stringify(await runGeneric(process.argv[2]), null, 2); assert.ok(Buffer.byteLength(output) <= 1048576); console.log(output);
  }
  catch { console.error('The trusted generic fixture directory could not be executed.'); process.exitCode = 1; }
}
