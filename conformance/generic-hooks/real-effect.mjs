import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';
import { consumeEffect } from '../../examples/node-host/effect-consumer.mjs';

const read = name => readFileSync(new URL(`effect3/${name}`, import.meta.url));
const programs = new Set(['main', 'zero', 'after-write-read']);
export const admissionCases = JSON.parse(read('admission-cases.json'));

// These are deterministic host fixtures, not qualification of a native deployment.
export function realFixture(api, { mode = 'real', program = 'main' } = {}) {
  assert.ok(programs.has(program)); assert.ok(['real', 'simulation'].includes(mode));
  const bytes = read(program === 'main' ? 'artifact.json' : `artifact-${program}.json`);
  const pin = read(program === 'main' ? 'artifact.sha256' : `artifact-${program}.sha256`).toString().trim();
  const dcmBytes = read('dcm.json'), dcm = JSON.parse(dcmBytes), dcmPin = read('dcm.sha256').toString().trim();
  assert.equal(digest(bytes), pin); assert.equal(digest(dcmBytes), dcmPin);
  const artifact = JSON.parse(bytes), calls = [], budgets = [];
  const poison = () => { throw Error('Native key cannot be converted'); };
  const key = Object.freeze({ toString: poison, valueOf: poison, toJSON: poison, [Symbol.toPrimitive]: poison });
  const state = { current: true, now: 0, authorizations: 0, keyChecks: 0, checks: 0, reads: 0,
    approved: { id: 'reviewed-host', revision: 1, hostBuild: 'sha256:' + '1'.repeat(64), reportDigest: 'sha256:' + '2'.repeat(64) } };
  const approved = { scope: 'scope1', modelRef: structuredClone(artifact.modelRef), profile: 'keir-effect/3', environment: { ...api.ENVIRONMENT }, mode,
    keyBinding: { type: dcm.hostKey.type, validate(value) { state.keyChecks++; state.onKey?.(); return value === key; } } };
  const qualification = (overrides = {}, check) => {
    const evidence = { contract: 'keir-real-effect-evidence/1', ...state.approved, scope: approved.scope,
      modelRef: structuredClone(approved.modelRef), environment: { ...api.ENVIRONMENT }, keyType: dcm.hostKey.type,
      capabilities: Object.keys(dcm.capabilities).sort().map(id => ({ id, version: dcm.capabilities[id].version })), ...overrides };
    const selected = structuredClone(evidence);
    return { evidence, isCurrent: check ?? (() => {
      state.checks++;
      state.onCheck?.();
      return state.current && Object.keys(state.approved).every(field => selected[field] === state.approved[field]);
    }) };
  };
  const invokers = Object.fromEntries(Object.entries(dcm.capabilities).map(([capability, cap]) => [capability, {
    version: cap.version, mode, keyType: dcm.hostKey.type,
    async invoke(args, protectedArgs, remainingMs, context) {
      assert.strictEqual(context.requestKey, key); assert.deepEqual(context.event, { id: 'event1', source: 'task1' });
      assert.deepEqual({ ...protectedArgs }, { actorId: 'user1' });
      assert.equal(remainingMs, 1000 - state.now); assert.ok(remainingMs > 0);
      budgets.push(remainingMs);
      calls.push({ capability, version: cap.version, args: structuredClone(args), protectedArgs: structuredClone(protectedArgs) });
      if (cap.effect === 'READ') {
        state.reads++; state.afterRead?.();
        return { kind: 'SUCCESS', value: { id: args.taskId, projectId: state.project ?? 'project1', status: state.status ?? 'COMPLETED' } };
      }
      state.afterWrite?.();
      return state.writeResponse ?? { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
    },
  }]));
  const admission = { expectedArtifactDigest: pin, expectedDcmDigest: dcmPin, entrypoint: artifact.entrypoint,
    consumer: structuredClone(dcm.entrypoints[artifact.entrypoint].consumer), compiler: artifact.compiler, environment: { ...api.ENVIRONMENT }, mode };
  const invocation = { scope: 'scope1', bindings: { actorId: 'user1' }, deadline: { clock: { nowMs: () => state.now }, expiresAtMs: 1000 },
    requestKey: key, event: { id: 'event1', source: 'task1' }, authorize: async request => { state.authorizations++; await state.onAuthorize?.(request); return true; } };
  return { bytes, pin, dcmBytes, dcm, approved, invokers, admission, invocation, state, calls, budgets, key, qualification,
    bind(...args) { return api.bindModel(dcmBytes, approved, invokers, args.length ? args[0] : mode === 'real' ? qualification() : undefined); } };
}

export async function runRealCase(api, row) {
  assert.ok(admissionCases.some(candidate => candidate.id === row.id), 'Unknown fixed admission case');
  const id = row.id;
  const mode = id.startsWith('simulation-') ? 'simulation' : 'real';
  const program = id.endsWith('-zero') ? 'zero' : id.includes('after-write-read') || id === 'revoked-after-confirmed' ? 'after-write-read' : 'main';
  const f = realFixture(api, { mode, program });
  let q = mode === 'real' ? f.qualification() : undefined;
  if (id === 'missing-qualification') q = undefined;
  if (id === 'null-qualification') q = null;
  if (id === 'simulation-with-qualification') q = f.qualification();
  if (id === 'wrong-build') q = f.qualification({ hostBuild: 'sha256:' + '3'.repeat(64) });
  if (id === 'stale-revision') q = f.qualification({ revision: 2 });
  if (id === 'bad-catalog') q.evidence.capabilities.pop();
  if (id === 'oversized-evidence') q.evidence.id = 'x'.repeat(16_384);
  const binding = f.bind(q);
  if (binding.diagnostic) return { id, facts: { phase: 'binding', code: binding.diagnostic.code, path: binding.diagnostic.path, calls: f.calls.length } };
  if (id === 'revoked-preparation' || id === 'digest-before-qualification') f.state.current = false;
  if (id === 'digest-before-qualification') f.admission.expectedArtifactDigest = 'sha256:' + '0'.repeat(64);
  const preparation = api.prepareProgram(f.bytes, binding.boundModel, f.admission);
  if (preparation.diagnostic) return { id, facts: { phase: 'preparation', code: preparation.diagnostic.code, path: preparation.diagnostic.path, calls: f.calls.length } };
  if (id === 'revoked-entry') f.state.current = false;
  if (id === 'revoked-pending-authorization') f.state.onAuthorize = async request => { if (request.capability) { await Promise.resolve(); f.state.current = false; } };
  if (id === 'revoked-after-read') f.state.afterRead = () => { f.state.current = false; };
  if (id === 'revoked-after-confirmed') f.state.afterWrite = () => { f.state.current = false; };
  if (id === 'false-guard') f.state.project = 'project2';
  if (id === 'zero-effect-ceiling') f.invocation.effectGrant = 0;
  if (id === 'confirmed-response-expired') f.state.afterWrite = () => { f.state.now = 1000; };
  if (id === 'qualification-expired') {
    f.state.onAuthorize = request => { if (request.capability) f.state.expire = true; };
    f.state.onCheck = () => { if (f.state.expire) f.state.now = 1000; };
  }
  if (id === 'definite-mismatch') f.state.writeResponse = { kind: 'BUSINESS_REJECTION', code: 'KEY_PAYLOAD_MISMATCH', effect: 'NONE' };
  if (id === 'transient-none') f.state.writeResponse = { kind: 'TRANSIENT_FAILURE', effect: 'NONE' };
  if (id === 'unknown-commit') f.state.writeResponse = { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' };
  if (id === 'malformed-confirmed-value') f.state.writeResponse = { kind: 'SUCCESS', value: {}, effect: 'CONFIRMED' };
  const outcome = await api.invokeProgram(preparation.prepared, { taskId: 'task1' }, f.invocation);
  const consumption = consumeEffect(outcome);
  const facts = { phase: 'invocation', classification: outcome.classification, calls: f.calls.map(call => call.capability),
    effects: outcome.effects.map(effect => effect.state), reported: consumption.status,
    ...(outcome.result ? { result: outcome.result } : {}), ...(outcome.error ? { error: outcome.error } : {}) };
  return { id, facts, outcome, calls: f.calls, consumption };
}

export async function runRealCases(api) {
  const results = [];
  for (const row of admissionCases) {
    const result = await runRealCase(api, row);
    assert.deepEqual(result.facts, row.expect, row.id);
    results.push(result);
  }
  return results;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    assert.ok(process.argv.length === 2 || process.argv.length === 3 && process.argv[2] === '--installed');
    const url = process.argv.length === 3 ? '../../examples/node-host/node_modules/@keir/runtime-node/src/index.js' : '../../runtime-node/src/index.js';
    console.log(JSON.stringify(await runRealCases(await import(new URL(url, import.meta.url)))));
  } catch (error) { console.error(String(error?.message ?? 'Real effect fixture failure').split('\n', 1)[0]); process.exitCode = 1; }
}
