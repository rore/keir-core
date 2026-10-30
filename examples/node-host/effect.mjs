import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import * as api from '@keir/runtime-node';
import { runBatch } from './effect-simulation.mjs';

export function runEffectScenarios(bytes, artifact, dcm) {
  const scenarios = JSON.parse(readFileSync(new URL('../../conformance/single-effect/scenarios.json', import.meta.url), 'utf8'));
  return runBatch(api, bytes, { artifact, dcm }, scenarios);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    assert.equal(process.argv.length, 5);
    const [path, artifact, dcm] = process.argv.slice(2);
    console.log(JSON.stringify(await runEffectScenarios(readFileSync(path), artifact, dcm), null, 2));
  } catch {
    console.error('The selected simulation artifact could not be executed with its independently held pins.'); process.exitCode = 1;
  }
}
