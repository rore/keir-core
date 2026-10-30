import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { canonicalBytes, digest } from '../src/canonical.js';
import { invoke } from '../src/index.js';
import { effectAdmission, effectAdapters, effectContext, effectInput, effectPrepared } from './effect-helpers.js';

const file = name => new URL('fixtures/effect-handler.' + name, import.meta.url);
const bytes = readFileSync(file('artifact.json'));
const artifactPin = readFileSync(file('artifact.sha256'), 'utf8').trim();
const modelPin = readFileSync(file('dcm.sha256'), 'utf8').trim();

test('actual Java handler fixture independently matches its artifact and model pins', () => {
  const model = JSON.parse(readFileSync(file('dcm.json')));
  assert.equal(digest(bytes), artifactPin); assert.equal(digest(canonicalBytes(model)), modelPin);
  assert.deepEqual(JSON.parse(bytes).dcm, model);
  assert.equal(JSON.parse(bytes).dcmDigest, modelPin);
});

test('handler result accepts exactly 8192 bytes and rejects 8193 retaining original rejection and NONE', async () => {
  const handle = effectPrepared(effectAdapters(undefined, async () => ({ kind: 'BUSINESS_REJECTION', code: 'CREATE_REJECTED', effect: 'NONE' })),
    effectAdmission({ expectedArtifactDigest: artifactPin, expectedDcmDigest: modelPin }), bytes);
  assert.equal(handle.code, undefined);
  const title = '\0'.repeat(56) + 'x'.repeat(200);
  assert.equal(Buffer.byteLength(title), 256);
  for (const size of [17, 18]) {
    const input = { ...effectInput, title, padding: 'x'.repeat(size) };
    const expected = Object.fromEntries(Array.from({ length: 16 }, (_, index) => ['f' + index, index === 15 ? input.padding : title]));
    assert.equal(canonicalBytes(expected).length, size === 17 ? 8192 : 8193);
    assert.ok(canonicalBytes(input).length < 8192);
    const out = await invoke(handle, input, effectContext());
    assert.deepEqual(out.effects, [{ step: 'created', capability: 'tasks.createFollowUp', state: 'NONE' }]);
    if (size === 17) {
      assert.equal(out.classification, 'BUSINESS_REJECTION'); assert.deepEqual(out.result, expected);
      assert.deepEqual(out.error, { code: 'CREATE_REJECTED', path: '/steps/2' });
      assert.equal(out.trace.at(-2).event, 'RETURNED');
    } else {
      assert.equal(out.classification, 'LIMIT_EXCEEDED'); assert.equal(Object.hasOwn(out, 'result'), false);
      assert.deepEqual(out.error, { code: 'BUDGET_EXCEEDED', path: '/steps/2/onError/CREATE_REJECTED',
        cause: { code: 'CREATE_REJECTED', path: '/steps/2' } });
      assert.equal(out.trace.some(event => event.event === 'RETURNED'), false);
    }
  }
});
