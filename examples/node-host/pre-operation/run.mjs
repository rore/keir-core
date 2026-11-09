import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import * as api from '@keir/runtime-node';
import { createDecisionHost } from '../decision-host.mjs';

if (process.argv.length > 3) throw new Error('Usage: node examples/node-host/pre-operation/run.mjs [artifact.json]');

const model = readFileSync(new URL('./dcm.json', import.meta.url));
const fixture = new URL('../../../conformance/keir-3/', import.meta.url);
const artifact = readFileSync(process.argv[2] ?? new URL('artifact-guard.json', fixture));
if (process.argv[2] === undefined) {
  const expected = readFileSync(new URL('artifact-guard.sha256', fixture), 'utf8').trim();
  const actual = 'sha256:' + createHash('sha256').update(artifact).digest('hex');
  if (actual !== expected) throw new Error('Pinned example artifact digest mismatch');
}

// The existing teaching host counts simulated native continuation, not durable commits.
for (const [name, expected] of [['allow', 'ALLOWED'], ['deny', 'DENIED'], ['unavailable', 'BLOCKED']]) {
  const host = createDecisionHost(api, model, artifact);
  if (host.diagnostic) throw new Error(host.diagnostic.code);
  host.state.restricted = name === 'deny';
  if (name === 'unavailable') host.state.readResponse = { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED' };
  const { consumption } = await host.attempt({ restricted: false }, host.context);
  const { nativeAttempts, nativeResults, writes } = host.counts();
  assert.equal(consumption.status, expected);
  assert.equal(nativeAttempts, name === 'allow' ? 1 : 0);
  assert.equal(nativeResults, nativeAttempts);
  assert.equal(writes, 0);
  const message = consumption.status === 'DENIED' ? consumption.result.message ?? '' : '';
  console.log(`${name} ${consumption.status} | native attempts=${nativeAttempts}${message ? ' | ' + message : ''}`);
}
