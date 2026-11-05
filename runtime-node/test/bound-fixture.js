import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createServer, request } from 'node:http';
import { once } from 'node:events';
import { digest } from '../src/canonical.js';

const read = name => readFileSync(new URL(`../../conformance/keir-1/${name}`, import.meta.url));
const programs = new Set(['main', 'zero', 'after-write-read', 'no-call', 'handler', 'title', 'title-max', 'title-composed']);

// Fixed host-owned native checks; these examples do not qualify a deployment.
export function boundFixture(api, { mode = 'real', program = 'main' } = {}) {
  assert.ok(programs.has(program)); assert.ok(['real', 'simulation'].includes(mode));
  const name = program === 'main' ? 'artifact' : `artifact-${program}`;
  const bytes = read(`${name}.json`), pin = read(`${name}.sha256`).toString().trim();
  const model = program.startsWith('title') ? 'dcm-title' : 'dcm';
  const dcmBytes = read(`${model}.json`), dcm = JSON.parse(dcmBytes);
  assert.equal(digest(bytes), pin); assert.equal(digest(dcmBytes), read(`${model}.sha256`).toString().trim());
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
        return { kind: 'SUCCESS', value: { id: args.taskId, projectId: state.project ?? 'project1', status: 'COMPLETED',
          ...(model === 'dcm-title' ? { title: state.title } : {}) } };
      }
      await state.afterWrite?.();
      if (state.writeError) throw Error('Native response lost');
      return state.writeResponse ?? { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
    },
  }]));
  return { bytes, pin, dcmBytes, dcm, implementations, invocation, invocationFor, state, calls, nativeActions, contexts, budgets,
    bind() { return api.bindModel(dcmBytes, implementations); } };
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
