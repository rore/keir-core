import test from 'node:test';
import assert from 'node:assert/strict';
import { bindModel } from '../src/index.js';
import { approval, dcmBytes, effectApproval, effectModelRef, invokers, modelRef } from './model-binding-helpers.js';

const malformedDcm = Uint8Array.of(0x7b);

test('approval model reference fields are diagnosed before malformed DCM parsing', () => {
  for (const [field, value] of [
    ['digest', 'sha256:bad'], ['digest', null], ['domainId', ''], ['domainId', ['sample.binding']],
    ['domainVersion', ''], ['domainVersion', 1], ['format', 'keir-dcm/effect-2'], ['format', ''],
  ]) {
    const result = bindModel(malformedDcm, approval({ modelRef: { ...modelRef, [field]: value } }), invokers());
    assert.deepEqual(result.diagnostic, { code: 'ADMISSION_INVALID', path: `/binding/approval/modelRef/${field}`, message: 'The selected artifact or host configuration is invalid.' });
  }
});

test('unsupported descriptor recipe is diagnosed before malformed DCM parsing', () => {
  const result = bindModel(malformedDcm, approval({ modelRef: { ...modelRef, descriptorRecipe: 'unsupported/1' } }), invokers());
  assert.deepEqual(result.diagnostic, { code: 'ADMISSION_INVALID', path: '/binding/approval/modelRef/descriptorRecipe', message: 'The selected artifact or host configuration is invalid.' });
});

test('effect approval requires the effect DCM format before parsing', () => {
  const result = bindModel(malformedDcm, effectApproval({ modelRef: { ...effectModelRef, format: 'keir-dcm/read-2' } }), {});
  assert.deepEqual(result.diagnostic, { code: 'ADMISSION_INVALID', path: '/binding/approval/modelRef/format', message: 'The selected artifact or host configuration is invalid.' });
});

test('approval profile accessors are rejected without invoking the getter', () => {
  for (const approved of [approval(), effectApproval()]) {
    let reads = 0;
    const profile = approved.profile;
    Object.defineProperty(approved, 'profile', { enumerable: true, get() { reads++; return profile; } });
    const result = bindModel(dcmBytes, approved, invokers());
    assert.equal(reads, 0);
    assert.equal(result.boundModel, null);
    assert.equal(result.diagnostic.code, 'ADMISSION_INVALID');
    assert.equal(result.diagnostic.path, '/binding/approval');
  }
});
