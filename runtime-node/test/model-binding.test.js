import test from 'node:test';
import assert from 'node:assert/strict';
import { bindModel, ENVIRONMENT } from '../src/index.js';
import { approval, dcmBytes, effectApproval, effectDcmBytes, effectInvokers, invokers, modelRef } from './model-binding-helpers.js';

const reject = (bytes, approved, registrations, path) => {
  let invoked = 0;
  const guarded = Object.fromEntries(Object.entries(registrations).map(([id, registration]) => [id, typeof registration?.invoke === 'function' ? { ...registration, invoke() { invoked++; } } : registration]));
  const result = bindModel(bytes, approved, guarded);
  assert.deepEqual(Object.keys(result).sort(), ['boundModel', 'diagnostic']);
  assert.equal(result.boundModel, null);
  assert.deepEqual(result.diagnostic, { code: 'ADMISSION_INVALID', path, message: 'The selected artifact or host configuration is invalid.' });
  assert.equal(invoked, 0);
  return result;
};

test('binds the exact complete catalog atomically, including unused WRITE in read/2', () => {
  let calls = 0;
  const registrations = invokers({ 'sample.used': { version: '1', mode: 'simulation', invoke() { calls++; } } });
  const result = bindModel(dcmBytes, approval(), registrations);
  assert.deepEqual(Object.keys(result).sort(), ['boundModel', 'diagnostic']);
  assert.equal(result.diagnostic, null);
  assert.equal(Object.isFrozen(result), true);
  assert.equal(Object.isFrozen(result.boundModel), true);
  assert.deepEqual(Reflect.ownKeys(result.boundModel), []);
  assert.equal(calls, 0);
});

test('pins approval scope, model reference, profile, environment and mode', () => {
  assert.equal(ENVIRONMENT.celNode, '0.6.1');
  for (const [invalid, path] of [
    [approval({ scope: '' }), '/binding/approval/scope'], [approval({ scope: 'x'.repeat(257) }), '/binding/approval/scope'],
    [approval({ modelRef: { ...modelRef, digest: 'sha256:' + '0'.repeat(64) } }), '/binding/approval/modelRef'],
    [approval({ modelRef: { ...modelRef, descriptorRecipe: 'unknown/1' } }), '/binding/approval/modelRef/descriptorRecipe'],
    [approval({ profile: 'keir-read/3' }), '/binding/approval/profile'],
    [approval({ environment: { ...ENVIRONMENT, celNode: 'different' } }), '/binding/approval/environment'],
    [approval({ unexpected: true }), '/binding/approval'],
  ]) reject(dcmBytes, invalid, invokers(), path);
  const realRead = bindModel(dcmBytes, approval({ mode: 'real' }), Object.fromEntries(Object.entries(invokers()).map(([id, row]) => [id, { ...row, mode: 'real' }])));
  assert.equal(realRead.diagnostic, null);
  reject(effectDcmBytes, effectApproval({ mode: 'real' }), effectInvokers(), '/binding/approval/mode');
  reject(dcmBytes, approval({ keyBinding: { type: 'x', validate() {} } }), invokers(), '/binding/approval');
  reject(Uint8Array.of(0x7b), approval(), invokers(), '/binding/dcm');
});

test('rejects incomplete, extra, non-callable and mismatched registrations without dispatch', () => {
  const missing = invokers(); delete missing['sample.unused'];
  const extra = invokers({ 'sample.extra': { version: '1', mode: 'simulation', invoke() {} } });
  const noncallable = invokers({ 'sample.used': { version: '1', mode: 'simulation', invoke: 1 } });
  const version = invokers({ 'sample.used': { version: '2', mode: 'simulation', invoke() {} } });
  const mode = invokers({ 'sample.used': { version: '1', mode: 'real', invoke() {} } });
  for (const [registrations, path] of [[missing, '/binding/invokers'], [extra, '/binding/invokers'], [noncallable, '/binding/invokers/sample.used'], [version, '/binding/invokers/sample.used'], [mode, '/binding/invokers/sample.used']]) reject(dcmBytes, approval(), registrations, path);
});

test('snapshots model bytes and registration metadata/function references', () => {
  const bytes = Uint8Array.from(dcmBytes);
  const fn = () => 'original';
  const row = { version: '1', mode: 'simulation', invoke: fn };
  const registrations = invokers({ 'sample.used': row });
  const result = bindModel(bytes, approval(), registrations);
  assert.equal(result.diagnostic, null);
  bytes.fill(0); row.version = 'changed'; row.invoke = () => 'replacement';
  registrations['sample.used'] = { version: 'bad', mode: 'real', invoke() {} };
  assert.equal(Object.isFrozen(result.boundModel), true);
});

test('effect registration checks key type and snapshots, but does not call, key validator', () => {
  let validated = 0;
  const approved = effectApproval({ keyBinding: { type: 'sample.RequestUuid/1', validate() { validated++; return true; } } });
  const success = bindModel(effectDcmBytes, approved, effectInvokers());
  assert.equal(success.diagnostic, null);
  assert.equal(validated, 0);
  reject(effectDcmBytes, approved, effectInvokers('wrong/1'), '/binding/invokers/sample.used');
  assert.equal(validated, 0);
});
