import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram, ENVIRONMENT } from '../src/index.js';
import { digest } from '../src/canonical.js';

// Positive programs are the immutable Java compiler fixtures, with independently stored pins.
export function fixture(profile = 'effect', variant = 'main') {
  const root = new URL(`../../conformance/generic-hooks/${profile}/`, import.meta.url);
  const name = variant === 'main' ? 'artifact' : `artifact-${variant}`;
  const bytes = readFileSync(new URL(`${name}.json`, root));
  const pin = readFileSync(new URL(`${name}.sha256`, root), 'utf8').trim();
  const dcmBytes = readFileSync(new URL('dcm.json', root));
  const dcmPin = readFileSync(new URL('dcm.sha256', root), 'utf8').trim();
  assert.equal(digest(bytes), pin);
  assert.equal(digest(dcmBytes), dcmPin);
  return { bytes, pin, dcmBytes, dcmPin, artifact: JSON.parse(bytes), dcm: JSON.parse(dcmBytes), profile };
}
const keys = new WeakSet();
const effectProfile = profile => profile === 'effect' || profile === 'effect3';
export function nativeKey() {
  const poison = () => { throw new Error('Native key must never be converted'); };
  const key = Object.freeze({ toString: poison, valueOf: poison, toJSON: poison, [Symbol.toPrimitive]: poison });
  keys.add(key); return key;
}
export const confirmed = { kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' };
export function approval(f, overrides = {}) {
  return { scope: 'scope1', modelRef: { domainId: f.dcm.domain.id, domainVersion: f.dcm.domain.version, format: f.dcm.format,
    digest: f.dcmPin, descriptorRecipe: 'keir-record-descriptors/1' }, profile: f.profile === 'effect3' ? 'keir-effect/3' : 'keir-' + f.profile + '/2',
    environment: { ...ENVIRONMENT }, mode: 'simulation',
    ...(effectProfile(f.profile) ? { keyBinding: { type: f.dcm.hostKey.type, validate: key => keys.has(key) } } : {}), ...overrides };
}
export function registrations(f, overrides = {}) {
  return Object.fromEntries(Object.entries(f.dcm.capabilities).map(([id, cap]) => [id, {
    version: '1', mode: 'simulation', ...(effectProfile(f.profile) ? { keyType: f.dcm.hostKey.type } : {}),
    invoke: id === 'tasks.create' ? async () => confirmed : async args => ({ kind: 'SUCCESS', value: {
      id: args.taskId, ...(effectProfile(f.profile) ? { projectId: 'project1' } : {}), status: 'COMPLETED' } }),
    ...overrides[id],
  }]));
}
export function admission(f, overrides = {}) {
  const entrypoint = effectProfile(f.profile) ? 'TaskCompleted' : 'ClassifyTask';
  return { expectedArtifactDigest: f.pin, expectedDcmDigest: f.dcmPin, entrypoint,
    consumer: structuredClone(f.dcm.entrypoints[entrypoint].consumer), compiler: 'keir-compiler-java/0.1.0-SNAPSHOT',
    environment: { ...ENVIRONMENT }, mode: 'simulation', ...overrides };
}
export function bind(f, invokers = registrations(f), approved = approval(f)) {
  const result = bindModel(f.dcmBytes, approved, invokers); assert.equal(result.diagnostic, null, JSON.stringify(result.diagnostic)); return result.boundModel;
}
export function prepared(f, model = bind(f), overrides = {}) {
  const result = prepareProgram(f.bytes, model, admission(f, overrides)); assert.equal(result.diagnostic, null, JSON.stringify(result.diagnostic)); return result.prepared;
}
export function context(profile = 'effect', overrides = {}) {
  return { scope: 'scope1', bindings: { actorId: 'user1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 }, authorize: async () => true,
    ...(effectProfile(profile) ? { requestKey: nativeKey(), event: { id: 'event1', source: 'task1' } } : {}), ...overrides };
}
