import test from 'node:test';
import assert from 'node:assert/strict';
import { bindModel, prepareProgram, invokeProgram, invoke, prepare, ENVIRONMENT } from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { approval, dcm, dcmBytes, invokers, modelRef } from './model-binding-helpers.js';
import { artifactBytes as legacyBytes, admission as legacyAdmission, adapters as legacyAdapters } from './helpers.js';

const bound = () => { const result = bindModel(dcmBytes, approval(), invokers()); assert.equal(result.diagnostic, null); return result.boundModel; };
const admitted = (bytes, overrides = {}) => ({ expectedArtifactDigest: digest(bytes), expectedDcmDigest: modelRef.digest,
  entrypoint: 'Main', consumer: dcm.entrypoints.Main.consumer, compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT, mode: 'simulation', ...overrides });
const rejected = (bytes, model, admission, code, path) => {
  const result = prepareProgram(bytes, model, admission);
  assert.deepEqual(Object.keys(result).sort(), ['diagnostic', 'prepared']); assert.ok(Object.isFrozen(result));
  assert.equal(result.prepared, null); assert.equal(result.diagnostic.code, code); assert.equal(result.diagnostic.path, path);
  assert.ok(Object.isFrozen(result.diagnostic)); return result;
};
// Negative envelope only: no compiled expression or successful program is synthesized here.
const envelope = () => ({ format: 'keir-artifact/read-2', profile: 'keir-read/2', compiler: 'keir-compiler-java/0.1.0-SNAPSHOT',
  environment: ENVIRONMENT, sourceDigest: 'sha256:' + '0'.repeat(64), modelRef, entrypoint: 'Main', expressions: {}, steps: [], return: {} });

test('preparation authenticates binding before admission and artifact input', () => {
  for (const model of [null, {}, Object.freeze(Object.create(null)), undefined, 1]) rejected(null, model, null, 'ADMISSION_INVALID', '/binding');
  rejected(null, bound(), {}, 'ADMISSION_INVALID', '/admission');
  rejected(null, bound(), admitted(Buffer.from('{'), { expectedArtifactDigest: 'bad' }), 'ADMISSION_INVALID', '/admission/expectedArtifactDigest');
  for (const field of ['grants', 'keyBinding', 'scope', 'invokers', 'profile', 'effectGrant']) {
    rejected(null, bound(), admitted(Buffer.alloc(0), { [field]: {} }), 'ADMISSION_INVALID', '/admission');
  }
});

test('raw type/size and digest-before-parse checks return closed redacted diagnostics', () => {
  const model = bound();
  rejected(null, model, admitted(Buffer.alloc(0)), 'ARTIFACT_INVALID', '/artifact');
  rejected(new Uint8Array(1048577), model, admitted(Buffer.alloc(0)), 'BUDGET_EXCEEDED', '/artifact');
  rejected(Buffer.from('{'), model, admitted(Buffer.alloc(0)), 'PIN_MISMATCH', '/artifact');
  rejected(Buffer.from('{'), model, admitted(Buffer.from('{')), 'ARTIFACT_INVALID', '/artifact');
});

test('reference profile, environment, fields and identity precede admission policy/plan checks', () => {
  for (const [mutate, path] of [
    [value => { value.format = 'keir-artifact/read-1'; }, '/artifact/format'],
    [value => { value.profile = 'keir-effect/2'; }, '/artifact/profile'],
    [value => { value.environment = { ...ENVIRONMENT, celJava: 'different' }; }, '/artifact/environment'],
    [value => { value.dcm = dcm; }, '/artifact'],
    [value => { value.descriptors = 'AA=='; }, '/artifact'],
    [value => { value.modelRef = { ...modelRef, descriptorRecipe: 'other/1' }; }, '/artifact/modelRef'],
    [value => { value.modelRef = { ...modelRef, digest: 'sha256:' + '1'.repeat(64) }; }, '/artifact/modelRef'],
    [value => { value.modelRef = { ...modelRef, extra: true }; }, '/artifact/modelRef'],
    [value => { value.entrypoint = 'Unknown'; }, '/artifact/entrypoint'],
    [value => { value.entrypoint = ['Main']; }, '/artifact/entrypoint'],
  ]) {
    const value = envelope(); mutate(value); const bytes = canonicalBytes(value);
    rejected(bytes, bound(), admitted(bytes, { capabilityPolicy: null }), 'ARTIFACT_INVALID', path);
  }
});

test('remaining external pins and malformed restrictions precede the negative empty plan', () => {
  const bytes = canonicalBytes(envelope());
  for (const [overrides, path] of [
    [{ expectedDcmDigest: 'sha256:' + '0'.repeat(64) }, '/admission/expectedDcmDigest'],
    [{ compiler: 'other/1' }, '/admission/compiler'], [{ consumer: {} }, '/admission/consumer'],
    [{ entrypoint: 'Other' }, '/admission/entrypoint'], [{ environment: {} }, '/admission/environment'],
    [{ mode: 'real' }, '/admission/mode'], [{ capabilityPolicy: null }, '/admission/capabilityPolicy'],
    [{ capabilityPolicy: undefined }, '/admission/capabilityPolicy'],
    [{ limits: null }, '/admission/limits'], [{ limits: { steps: 9 } }, '/admission/limits'],
  ]) rejected(bytes, bound(), admitted(bytes, overrides), 'ADMISSION_INVALID', path);
  rejected(bytes, bound(), admitted(bytes), 'BUDGET_EXCEEDED', '/artifact/steps');
  rejected(bytes, bound(), admitted(bytes, { capabilityPolicy: { allow: ['sample.write'] } }), 'BUDGET_EXCEEDED', '/artifact/steps');
});

test('legacy and reference API families reject wrong or forged handles without rejected promises', async () => {
  const legacy = prepare(legacyBytes, legacyAdmission(), legacyAdapters()); assert.equal(legacy.code, undefined);
  for (const handle of [legacy, null, {}, bound()]) {
    const result = await invokeProgram(handle, {}, {});
    assert.equal(result.classification, 'FAILED'); assert.deepEqual(result.error, { code: 'HOST_CONTEXT_INVALID', path: '' });
  }
  const wrong = canonicalBytes(envelope());
  assert.equal(prepare(wrong, { ...legacyAdmission(), expectedArtifactDigest: digest(wrong) }, legacyAdapters()).code, 'ARTIFACT_INVALID');
  rejected(legacyBytes, bound(), admitted(legacyBytes), 'ARTIFACT_INVALID', '/artifact');
  const fake = await invoke(Object.freeze(Object.create(null)), {}, {}); assert.equal(fake.error.code, 'HOST_CONTEXT_INVALID');
});

test('model parsing accepts formatted input and exact raw boundary; rejects duplicates and invalid unused definitions', () => {
  const pretty = Buffer.from(JSON.stringify(dcm, null, 2)); assert.equal(bindModel(pretty, approval(), invokers()).diagnostic, null);
  const exact = Buffer.concat([dcmBytes, Buffer.alloc(65536 - dcmBytes.length, 32)]);
  assert.equal(bindModel(exact, approval(), invokers()).diagnostic, null);
  const plusOne = bindModel(Buffer.concat([exact, Buffer.from(' ')]), approval(), invokers());
  assert.deepEqual({ code: plusOne.diagnostic.code, path: plusOne.diagnostic.path }, { code: 'BUDGET_EXCEEDED', path: '/binding/dcm' });
  for (const bytes of [
    Buffer.from('{"format":"a","format":"b"}'), Buffer.from('{"format":"a","\\u0066ormat":"b"}'),
    Buffer.from('{"a":[1,]}'), Buffer.from('{"a":null}'), Buffer.from('{"a":-1}'), Buffer.from('{"a":1.5}'), Buffer.from('{"a":true,}'), Buffer.from('{"a":"unterminated'),
    Buffer.from([0xff]),
  ]) { const result = bindModel(bytes, approval(), invokers()); assert.equal(result.diagnostic.code, 'ADMISSION_INVALID'); assert.equal(result.diagnostic.path, '/binding/dcm'); }
  const invalid = structuredClone(dcm); invalid.capabilities['sample.unused'].parameters = { broken: { type: 'Unknown', binding: 'agent' } };
  const bytes = canonicalBytes(invalid), selected = approval({ modelRef: { ...modelRef, digest: digest(bytes) } });
  assert.equal(bindModel(bytes, selected, invokers()).diagnostic.path, '/binding/dcm');
  const expansion = Buffer.from('{"unknown":' + JSON.stringify('\n'.repeat(20000)) + '}');
  assert.ok(expansion.length < 65536);
  assert.equal(bindModel(expansion, approval(), invokers()).diagnostic.code, 'BUDGET_EXCEEDED');
});

test('binding rejects accessors without invoking them or any registration callback', () => {
  let calls = 0;
  const bad = { ...approval() }; Object.defineProperty(bad, 'scope', { enumerable: true, get() { calls++; return 'tenant-a'; } });
  assert.equal(bindModel(dcmBytes, bad, invokers()).diagnostic.path, '/binding/approval');
  const registrations = invokers(); Object.defineProperty(registrations['sample.used'], 'invoke', { enumerable: true, get() { calls++; return () => {}; } });
  assert.equal(bindModel(dcmBytes, approval(), registrations).diagnostic.path, '/binding/invokers/sample.used');
  assert.equal(calls, 0);
});

test('model references are typed strings, never array-coercible aliases', () => {
  for (const mutate of [
    value => { value.entrypoints.Main.input = ['Input']; }, value => { value.entrypoints.Main.output = ['Output']; },
    value => { value.capabilities['sample.unused'].result = ['Output']; },
    value => { value.capabilities['sample.unused'].parameters = { id: { type: ['Id'], binding: 'agent' } }; },
    value => { value.types.Input.fields.id = ['Id']; },
  ]) {
    const value = structuredClone(dcm); mutate(value); const bytes = canonicalBytes(value);
    const result = bindModel(bytes, approval({ modelRef: { ...modelRef, digest: digest(bytes) } }), invokers());
    assert.equal(result.diagnostic.code, 'ADMISSION_INVALID'); assert.equal(result.diagnostic.path, '/binding/dcm');
  }
});
