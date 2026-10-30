import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import * as api from '../../runtime-node/src/index.js';
import { runBatch } from '../../examples/node-host/effect-simulation.mjs';

export const bytes = readFileSync(new URL('artifact.json', import.meta.url));
export const pins = {
  artifact: readFileSync(new URL('artifact.sha256', import.meta.url), 'utf8').trim(),
  dcm: readFileSync(new URL('dcm.sha256', import.meta.url), 'utf8').trim(),
};
export const loadScenarios = () => JSON.parse(readFileSync(new URL('scenarios.json', import.meta.url), 'utf8'));
export const runCorpus = (scenarios = loadScenarios()) => runBatch(api, bytes, pins, scenarios);

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try { assert.equal(process.argv.length, 2, 'Runner accepts no arguments'); console.log(JSON.stringify(await runCorpus(), null, 2)); }
  catch (error) { console.error(String(error.message).split('\n', 1)[0]); process.exitCode = 1; }
}
