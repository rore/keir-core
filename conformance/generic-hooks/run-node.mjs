import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import * as api from '../../runtime-node/src/index.js';
import { runBatch } from '../../examples/node-host/generic-simulation.mjs';

const names = { read: ['main', 'repeated', 'alternate'], effect: ['main', 'project2', 'title2'], effect3: ['main', 'zero', 'after-write-read'], effect4: ['main', 'zero', 'after-write-read', 'no-call'] };
const read = (profile, file) => readFileSync(new URL(`${profile}/${file}`, import.meta.url));
const pin = (profile, file) => read(profile, file).toString('utf8').trim();
export const fixtures = Object.fromEntries(Object.entries(names).map(([profile, programs]) => [profile, {
  dcmBytes: read(profile, 'dcm.json'), dcmDigest: pin(profile, 'dcm.sha256'),
  programs: Object.fromEntries(programs.map(name => {
    const file = name === 'main' ? 'artifact' : `artifact-${name}`;
    return [name, { bytes: read(profile, `${file}.json`), digest: pin(profile, `${file}.sha256`) }];
  })),
}]));
export function loadScenarios(profile) {
  assert.ok(Object.hasOwn(names, profile), 'Unknown fixed profile');
  const bytes = read(profile, 'scenarios.json'); assert.ok(bytes.length <= 1_048_576, 'CAPACITY_EXCEEDED');
  return JSON.parse(bytes.toString('utf8'));
}
export async function runProfile(profile, scenarios = loadScenarios(profile)) {
  assert.ok(Object.hasOwn(names, profile), 'Unknown fixed profile');
  return runBatch(api, fixtures[profile], scenarios);
}
export async function runCorpus() {
  return { read: await runProfile('read'), effect: await runProfile('effect'), effect3: await runProfile('effect3'), effect4: await runProfile('effect4') };
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try { assert.equal(process.argv.length, 2, 'Runner accepts no arguments'); console.log(JSON.stringify(await runCorpus())); }
  catch (error) { console.error(String(error?.message ?? 'Generic scenario batch failed').split('\n', 1)[0]); process.exitCode = 1; }
}
