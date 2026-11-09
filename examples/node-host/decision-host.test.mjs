import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as api from '../../runtime-node/src/index.js';
import { createDecisionHost } from './decision-host.mjs';

const fixtures = new URL('../../conformance/keir-3/', import.meta.url);
function host(artifact = 'guard', model = 'guard', runtime = api) {
  const h = createDecisionHost(runtime, readFileSync(new URL(`dcm-${model}.json`, fixtures)),
    readFileSync(new URL(`artifact-${artifact}.json`, fixtures)));
  assert.equal(h.diagnostic, null);
  return h;
}
const run = (h, input = { restricted: false }) => h.attempt(input, h.context);
function counters(h, nativeAttempts = 0, nativeResults = nativeAttempts, reads = 0, invocations = 1) {
  assert.deepEqual(h.counts(), { invocations, reads, writes: 0, nativeAttempts, nativeResults });
}
function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return { promise, resolve };
}

test('setup binds the complete catalog and prepares once using only the public API', async () => {
  let binds = 0, prepares = 0, invokes = 0;
  const h = host('guard', 'guard', {
    ...api,
    bindModel(bytes, implementations) {
      binds++;
      assert.deepEqual(Object.keys(implementations).sort(), ['native.write', 'policy.get']);
      return api.bindModel(bytes, implementations);
    },
    prepareProgram(...args) { prepares++; return api.prepareProgram(...args); },
    invokeProgram(prepared, input, context) {
      invokes++;
      assert.equal(context.requestKey, h.context.requestKey);
      assert.equal(context.deadline.expiresAtMs, h.context.deadline.expiresAtMs - 100);
      assert.deepEqual(input, { restricted: false });
      return api.invokeProgram(prepared, input, context);
    },
  });
  assert.equal((await run(h)).nativeResult.status, 'COMMITTED');
  assert.deepEqual({ binds, prepares, invokes }, { binds: 1, prepares: 1, invokes: 1 });
  counters(h, 1, 1, 1);
});

for (const artifact of ['guard-input', 'guard', 'guard-reversed']) {
  for (const restricted of [false, true]) {
    test(`${artifact}: ${restricted ? 'DENY' : 'ALLOW'} has an independent native result`, async () => {
      const h = host(artifact, artifact === 'guard-reversed' ? 'guard-reversed' : 'guard');
      const result = await run(h, { restricted });
      assert.equal(result.outcome.classification, 'COMPLETED');
      assert.deepEqual(result.outcome.effects, []);
      assert.deepEqual(result.consumption, { status: restricted ? 'DENIED' : 'ALLOWED',
        result: { decision: restricted ? 'DENY' : 'ALLOW', message: restricted ? 'Operation restricted.' : '' } });
      assert.equal(result.nativeResult.status, restricted ? 'DENIED' : 'COMMITTED');
      counters(h, restricted ? 0 : 1, restricted ? 0 : 1, artifact === 'guard-input' ? 0 : 1);
    });
  }
}

test('a local READ restriction denies independently of the proposed input', async () => {
  const h = host(); h.state.restricted = true;
  const result = await run(h);
  assert.equal(result.consumption.status, 'DENIED');
  assert.equal(result.nativeResult.status, 'DENIED');
  counters(h, 0, 0, 1);
});

for (const [name, response] of [
  ['denied', { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED' }],
  ['transient', { kind: 'TRANSIENT_FAILURE' }],
  ['malformed', { kind: 'SUCCESS', value: { restricted: 'false' } }],
]) {
  test(`${name} READ blocks without a native attempt`, async () => {
    const h = host(); h.state.readResponse = response;
    const result = await run(h);
    assert.equal(result.consumption.status, 'BLOCKED');
    assert.equal(result.nativeResult.status, 'BLOCKED');
    counters(h, 0, 0, 1);
  });
}

test('READ exception blocks without a native attempt', async () => {
  const h = host(); h.state.readError = new Error('Unavailable');
  const result = await run(h);
  assert.equal(result.outcome.classification, 'FAILED');
  assert.equal(result.consumption.status, 'BLOCKED');
  counters(h, 0, 0, 1);
});

test('handled READ rejection returning ALLOW remains BLOCKED', async () => {
  const h = host('guard-handler');
  h.state.readResponse = { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED' };
  const result = await run(h);
  assert.equal(result.outcome.classification, 'BUSINESS_REJECTION');
  assert.equal(result.outcome.result.decision, 'ALLOW');
  assert.deepEqual(result.consumption, { status: 'BLOCKED' });
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 1);
});

for (const artifact of ['guard-input', 'guard']) {
  for (const change of ['key', 'actor', 'event']) {
    test(`${artifact}: native authentication blocks changed ${change} after ALLOW`, async () => {
      const h = host(artifact);
      h.state.beforeCommit = () => {
        if (change === 'key') h.context.requestKey = {};
        if (change === 'actor') h.state.actorId = 'other';
        if (change === 'event') h.context.event.source = 'other';
      };
      const result = await run(h);
      assert.equal(result.outcome.result.decision, 'ALLOW');
      assert.equal(result.consumption.status, 'ALLOWED');
      assert.equal(result.nativeResult.status, 'BLOCKED');
      counters(h, 0, 0, artifact === 'guard-input' ? 0 : 1);
    });
  }
}

test('forged nonnull key is rejected by host authority before invocation', async () => {
  const h = host('guard-input'); h.context.requestKey = {};
  const result = await run(h);
  assert.equal(result.outcome, null);
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 0, 0);
});

for (const change of ['selection', 'approval', 'input', 'permission', 'fact', 'deadline']) {
  test(`commit gate blocks changed ${change} after READ ALLOW`, async () => {
    const h = host(), input = { restricted: false };
    h.state.beforeCommit = () => {
      if (change === 'selection') h.state.selection = { kind: 'SELECTED', id: 'other' };
      if (change === 'approval') h.state.approvalVersion++;
      if (change === 'input') input.restricted = true;
      if (change === 'permission') h.state.permitted = false;
      if (change === 'fact') h.state.factVersion++;
      if (change === 'deadline') h.state.nowMs = 1000;
    };
    const result = await run(h, input);
    assert.equal(result.consumption.status, 'ALLOWED');
    assert.equal(result.nativeResult.status, 'BLOCKED');
    counters(h, 0, 0, 1);
  });
}

test('permission revoked while READ is pending blocks the native boundary', async () => {
  const h = host(); h.state.afterRead = () => { h.state.permitted = false; };
  const result = await run(h);
  assert.equal(result.outcome.classification, 'COMPLETED');
  assert.equal(result.consumption.status, 'ALLOWED');
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 1);
});

test('reserve exhaustion blocks before runtime invocation', async () => {
  const h = host(); h.state.nowMs = 900;
  const result = await run(h);
  assert.equal(result.outcome, null);
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 0, 0);
});

test('late READ response cannot spend the native reserve', async () => {
  const h = host(); h.state.afterRead = () => { h.state.nowMs = 900; };
  const result = await run(h);
  assert.equal(result.outcome.classification, 'LIMIT_EXCEEDED');
  assert.equal(result.consumption.status, 'BLOCKED');
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 1);
});

test('regressing clock during READ blocks native continuation', async () => {
  const h = host(); h.state.nowMs = 10;
  h.state.afterRead = () => { h.state.nowMs = 9; };
  const result = await run(h);
  assert.equal(result.outcome.classification, 'FAILED');
  assert.equal(result.outcome.error.code, 'HOST_CONTEXT_INVALID');
  assert.equal(result.consumption.status, 'BLOCKED');
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 1);
});

test('cancellation during pending READ cannot reopen on late valid ALLOW or retry', async () => {
  const h = host(), entered = deferred(), release = deferred();
  h.state.afterRead = async () => { entered.resolve(); await release.promise; };
  const pending = run(h);
  await entered.promise;
  const before = h.counts();
  assert.equal(before.nativeAttempts, 0);
  assert.equal((await run(h)).nativeResult.status, 'BLOCKED');
  assert.deepEqual(h.counts(), before);
  h.state.cancelled = true;
  release.resolve();
  const result = await pending;
  assert.equal(result.outcome.classification, 'COMPLETED');
  assert.equal(result.consumption.status, 'ALLOWED');
  assert.equal(result.nativeResult.status, 'BLOCKED');
  assert.deepEqual(h.counts(), before);
  assert.equal((await run(h)).nativeResult.status, 'BLOCKED');
  assert.deepEqual(h.counts(), before);
});

test('authoritative absence continues ordinary native validation', async () => {
  const h = host(); h.state.selection = Object.freeze({ kind: 'ABSENT' });
  h.state.approved = false; h.state.available = false; h.state.approvalVersion++;
  h.context.bindings = {};
  const result = await run(h);
  assert.equal(result.outcome, null);
  assert.equal(result.consumption, null);
  assert.equal(result.nativeResult.status, 'COMMITTED');
  counters(h, 1, 1, 0, 0);
  const invalid = host(); invalid.state.selection = { kind: 'ABSENT' };
  assert.equal((await run(invalid, { restricted: 'false' })).nativeResult.status, 'BLOCKED');
  counters(invalid, 0, 0, 0, 0);
});

test('selected but unavailable cannot fall back to absence', async () => {
  const h = host(); h.state.available = false;
  const result = await run(h);
  assert.equal(result.outcome, null);
  assert.deepEqual(result.consumption, { status: 'BLOCKED' });
  assert.equal(result.nativeResult.status, 'BLOCKED');
  counters(h, 0, 0, 0, 0);
});

for (const artifactBytes of [undefined, Buffer.from('{'), Buffer.from('{"steps":"invalid"}')]) {
  test(`selected unavailable artifact (${artifactBytes === undefined ? 'missing' : artifactBytes}) fails closed at setup`, async () => {
    const h = createDecisionHost(api, readFileSync(new URL('dcm-guard.json', fixtures)), artifactBytes);
    assert.ok(h.diagnostic);
    const result = await run(h);
    assert.equal(result.outcome, null);
    assert.deepEqual(result.consumption, { status: 'BLOCKED' });
    assert.equal(result.nativeResult.status, 'BLOCKED');
    counters(h, 0, 0, 0, 0);
  });
}

test('malformed host invocation data blocks without escaping or native fallback', async () => {
  for (const invocation of [undefined, null, {}, { get requestKey() { throw new Error('untrusted getter'); } }]) {
    const h = host('guard-input');
    const result = await h.attempt({ restricted: false }, invocation);
    assert.equal(result.outcome, null);
    assert.equal(result.nativeResult.status, 'BLOCKED');
    counters(h, 0, 0, 0, 0);
  }
});

test('getter and throwing proxy inputs block without a native attempt', async () => {
  let getterCalls = 0;
  for (const input of [
    { get restricted() { getterCalls++; throw new Error('untrusted getter'); } },
    new Proxy({}, { ownKeys() { throw new Error('untrusted proxy'); } }),
  ]) {
    const h = host('guard-input');
    const result = await run(h, input);
    assert.equal(result.outcome, null);
    assert.equal(result.nativeResult.status, 'BLOCKED');
    counters(h, 0, 0, 0, 0);
  }
  assert.equal(getterCalls, 0);
});

test('native failure stays separate from successful ALLOW consumption', async () => {
  const h = host(); h.state.nativeFailure = true;
  const result = await run(h);
  assert.equal(result.outcome.classification, 'COMPLETED');
  assert.equal(result.consumption.status, 'ALLOWED');
  assert.equal(result.nativeResult.status, 'FAILED');
  counters(h, 1, 0, 1);
});
