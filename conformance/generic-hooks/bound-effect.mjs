import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { createServer, request } from 'node:http';
import { once } from 'node:events';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';
import { consumeEffect } from '../../examples/node-host/effect-consumer.mjs';

const read = name => readFileSync(new URL(`effect4/${name}`, import.meta.url));
const programs = new Set(['main', 'zero', 'after-write-read', 'no-call']);
export const boundCases = JSON.parse(read('admission-cases.json'));

// Fixed host-owned native checks; these examples do not qualify a deployment.
export function boundFixture(api, { mode = 'real', program = 'main' } = {}) {
  assert.ok(programs.has(program)); assert.ok(['real', 'simulation'].includes(mode));
  const name = program === 'main' ? 'artifact' : `artifact-${program}`;
  const bytes = read(`${name}.json`), pin = read(`${name}.sha256`).toString().trim();
  const dcmBytes = read('dcm.json'), dcm = JSON.parse(dcmBytes);
  assert.equal(digest(bytes), pin); assert.equal(digest(dcmBytes), read('dcm.sha256').toString().trim());
  const calls = [], nativeActions = [], contexts = [], budgets = [], keys = new WeakMap();
  const state = { now: 0, allowed: true, checks: 0 };
  function invocationFor({ actor = 'user1', event = 'event1', source = 'task1', expiresAtMs = 1000 } = {}) {
    const poison = () => { throw Error('Native key cannot be converted'); };
    const key = { toString: poison, valueOf: poison, toJSON: poison, [Symbol.toPrimitive]: poison };
    keys.set(key, { actor, event, source, expiresAtMs });
    return { bindings: program === 'no-call' ? {} : { actorId: actor },
      deadline: { clock: { nowMs() { return state.now; } }, expiresAtMs },
      requestKey: key, event: { id: event, source } };
  }
  const invocation = invocationFor();
  const implementations = Object.fromEntries(Object.entries(dcm.capabilities).map(([capability, cap]) => [capability, {
    version: cap.version, mode, keyType: dcm.hostKey.type,
    async invoke(args, protectedArgs, remainingMs, context) {
      calls.push({ capability, version: cap.version, args: structuredClone(args), protectedArgs: structuredClone(protectedArgs) });
      contexts.push(context); budgets.push(remainingMs);
      await state.beforeNative?.(capability, context);
      state.checks++;
      const key = keys.get(context.requestKey), write = cap.effect === 'WRITE';
      const allowed = state.allowed && key && key.actor === protectedArgs.actorId && key.event === context.event.id &&
        key.source === context.event.source && key.expiresAtMs === context.deadline.expiresAtMs &&
        context.deadline.clock.nowMs() < context.deadline.expiresAtMs;
      if (!allowed) return { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED', ...(write ? { effect: 'NONE' } : {}) };
      nativeActions.push(capability);
      if (!write) {
        await state.afterRead?.();
        return { kind: 'SUCCESS', value: { id: args.taskId, projectId: state.project ?? 'project1', status: 'COMPLETED' } };
      }
      await state.afterWrite?.();
      if (state.writeError) throw Error('Native response lost');
      return state.writeResponse ?? { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
    },
  }]));
  return { bytes, pin, dcmBytes, dcm, implementations, invocation, invocationFor, state, calls, nativeActions, contexts, budgets,
    bind() { return api.bindModel(dcmBytes, implementations); } };
}

export async function runBoundCase(api, row) {
  assert.ok(boundCases.some(candidate => candidate.id === row.id), 'Unknown fixed host-bound case');
  const id = row.id, mode = id.startsWith('simulation-') ? 'simulation' : 'real';
  const program = id.endsWith('-no-call') || id === 'empty-catalog' ? 'no-call' : id.endsWith('-zero') ? 'zero' :
    id.includes('after-write-read') || id === 'denied-after-confirmed' ? 'after-write-read' : 'main';
  const f = boundFixture(api, { mode, program });
  let dcmBytes = f.dcmBytes, bytes = f.bytes, implementations = f.implementations;
  if (id === 'missing-implementation') delete implementations['tasks.create'];
  if (id === 'mixed-mode') implementations['tasks.get'].mode = 'simulation';
  if (id === 'empty-catalog') {
    const dcm = structuredClone(f.dcm); dcm.capabilities = {}; dcmBytes = canonicalBytes(dcm); implementations = {};
    const artifact = JSON.parse(bytes); artifact.modelRef.digest = digest(dcmBytes); bytes = canonicalBytes(artifact);
  }
  const binding = api.bindModel(dcmBytes, implementations);
  if (binding.diagnostic) return { id, facts: { phase: 'binding', code: binding.diagnostic.code, path: binding.diagnostic.path, calls: f.calls.length } };
  if (id === 'unsupported-compiler') { const artifact = JSON.parse(bytes); artifact.compiler = 'unsupported'; bytes = canonicalBytes(artifact); }
  const preparation = api.prepareProgram(bytes, binding.boundModel);
  if (preparation.diagnostic) return { id, facts: { phase: 'preparation', code: preparation.diagnostic.code, path: preparation.diagnostic.path, calls: f.calls.length } };
  if (id === 'denied-entry') f.state.allowed = false;
  if (id === 'denied-write') f.state.afterRead = () => { f.state.allowed = false; };
  if (id === 'denied-pending-native') f.state.beforeNative = async capability => { if (capability === 'tasks.create') { await Promise.resolve(); f.state.allowed = false; } };
  if (id === 'denied-after-confirmed') f.state.afterWrite = () => { f.state.allowed = false; };
  if (id === 'invalid-key' || id === 'real-no-call') f.invocation.requestKey = {};
  if (id === 'false-guard') f.state.project = 'project2';
  if (id === 'expired-entry') f.state.now = 1000;
  if (id === 'confirmed-response-expired') f.state.afterWrite = () => { f.state.now = 1000; };
  if (id === 'definite-mismatch') f.state.writeResponse = { kind: 'BUSINESS_REJECTION', code: 'KEY_PAYLOAD_MISMATCH', effect: 'NONE' };
  if (id === 'transient-none') f.state.writeResponse = { kind: 'TRANSIENT_FAILURE', effect: 'NONE' };
  if (id === 'unknown-commit') f.state.writeResponse = { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' };
  if (id === 'lost-response') f.state.writeError = true;
  if (id === 'malformed-confirmed-value') f.state.writeResponse = { kind: 'SUCCESS', value: {}, effect: 'CONFIRMED' };
  const outcome = await api.invokeProgram(preparation.prepared, { taskId: 'task1' }, f.invocation);
  const consumption = consumeEffect(outcome);
  const facts = { phase: 'invocation', classification: outcome.classification, calls: f.calls.map(call => call.capability),
    effects: outcome.effects.map(effect => effect.state), nativeActions: f.nativeActions, reported: consumption.status,
    ...(outcome.result ? { result: outcome.result } : {}), ...(outcome.error ? { error: outcome.error } : {}) };
  return { id, facts, outcome, calls: f.calls, consumption };
}

export async function runBoundCases(api) {
  const results = [];
  for (const row of boundCases) { const result = await runBoundCase(api, row); assert.deepEqual(result.facts, row.expect, row.id); results.push(result); }
  return results;
}

// Test-owned ephemeral service, using the same public APIs as an in-process host.
export async function verifyBoundTransports(api) {
  for (const transport of ['local', 'loopback']) for (const scenario of ['confirmed', 'denied', 'wrong-key', 'lost-response']) {
    const f = boundFixture(api); let commits = 0, attempts = 0, transportError;
    const key = f.invocation.requestKey;
    async function nativeCreate(body) {
      await Promise.resolve(); attempts++;
      if (scenario === 'denied' || body.token !== 'native-token' || body.actor !== 'user1' ||
          body.event !== 'event1' || body.source !== 'task1' || body.expiresAtMs <= f.state.now)
        return { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED', effect: 'NONE' };
      commits++;
      if (scenario === 'lost-response') throw Error('Response lost after native commit');
      return { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
    }
    let server;
    try {
      if (transport === 'loopback') {
        server = createServer(async (incoming, response) => {
          try {
            const chunks = []; let size = 0;
            for await (const chunk of incoming) { size += chunk.length; assert.ok(size <= 1024); chunks.push(chunk); }
            const result = await nativeCreate(JSON.parse(Buffer.concat(chunks)));
            response.setHeader('content-type', 'application/json'); response.end(JSON.stringify(result));
          } catch { response.destroy(); }
        });
        server.listen(0, '127.0.0.1'); await once(server, 'listening');
      }
      f.implementations['tasks.create'].invoke = async (args, protectedArgs, remainingMs, context) => {
        assert.strictEqual(context.requestKey, key); assert.ok(remainingMs > 0);
        const body = { actor: protectedArgs.actorId, event: context.event.id, source: context.event.source,
          token: scenario === 'wrong-key' ? 'untrusted' : 'native-token', expiresAtMs: context.deadline.expiresAtMs,
          projectId: args.projectId, title: args.title };
        if (transport === 'local') return nativeCreate(body);
        return new Promise((resolve, reject) => {
          const outgoing = request({ host: '127.0.0.1', port: server.address().port, path: '/tasks', method: 'POST',
            headers: { 'content-type': 'application/json' } }, response => {
            const chunks = []; let size = 0;
            response.on('data', chunk => { size += chunk.length; if (size > 1024) response.destroy(Error('Response exceeds bound')); else chunks.push(chunk); });
            response.on('error', reject);
            response.on('end', () => { try { resolve(JSON.parse(Buffer.concat(chunks))); } catch (error) { reject(error); } });
          });
          outgoing.on('error', error => { transportError = error.message; reject(error); });
          outgoing.setTimeout(remainingMs, () => outgoing.destroy(Error('Native deadline elapsed')));
          outgoing.end(JSON.stringify(body));
        });
      };
      const binding = f.bind(); assert.equal(binding.diagnostic, null);
      const preparation = api.prepareProgram(f.bytes, binding.boundModel); assert.equal(preparation.diagnostic, null);
      assert.equal(attempts, 0);
      const outcome = await api.invokeProgram(preparation.prepared, { taskId: 'task1' }, f.invocation);
      assert.equal(attempts, 1, JSON.stringify({ transport, scenario, transportError, outcome }));
      assert.equal(commits, ['confirmed', 'lost-response'].includes(scenario) ? 1 : 0);
      assert.equal(outcome.effects[0].state, scenario === 'confirmed' ? 'CONFIRMED' : scenario === 'lost-response' ? 'UNKNOWN' : 'NONE');
      assert.equal(outcome.classification, scenario === 'confirmed' ? 'COMPLETED' : scenario === 'lost-response' ? 'UNCERTAIN' : 'BUSINESS_REJECTION');
    } finally {
      if (server) { server.closeAllConnections(); await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve())); }
    }
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    assert.ok(process.argv.length === 2 || process.argv.length === 3 && process.argv[2] === '--installed');
    const url = process.argv.length === 3 ? '../../examples/node-host/node_modules/@keir/runtime-node/src/index.js' : '../../runtime-node/src/index.js';
    const api = await import(new URL(url, import.meta.url));
    const rows = await runBoundCases(api); await verifyBoundTransports(api);
    console.log(JSON.stringify(rows));
  } catch (error) { console.error(String(error?.message ?? 'Host-bound fixture failure').split('\n', 1)[0]); process.exitCode = 1; }
}
