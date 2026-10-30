import { readFileSync } from 'node:fs';
import { canonicalBytes, digest } from '../src/canonical.js';
import { prepare, ENVIRONMENT } from '../src/index.js';

const fixture = new URL('../../conformance/single-effect/', import.meta.url);
export const effectBytes = readFileSync(new URL('artifact.json', fixture));
export const effectArtifact = JSON.parse(effectBytes);
export const artifactPin = readFileSync(new URL('artifact.sha256', fixture), 'utf8').trim();
export const modelPin = readFileSync(new URL('dcm.sha256', fixture), 'utf8').trim();
const brandedKeys = new WeakMap();
let keyOrdinal = 0;
export function nativeKey() {
  const fail = () => { throw new Error('Private opaque key was inspected'); };
  const key = Object.defineProperties({}, {
    hidden: { enumerable: true, get: fail }, toString: { value: fail },
    toJSON: { value: fail }, [Symbol.toPrimitive]: { value: fail },
  });
  brandedKeys.set(key, '00000000-0000-4000-8000-' + (++keyOrdinal).toString(16).padStart(12, '0'));
  return Object.freeze(key);
}
export const keyType = effectArtifact.dcm.hostKey.type;
export function effectAdmission(overrides = {}) {
  return {
    expectedArtifactDigest: artifactPin, expectedDcmDigest: modelPin,
    entrypoint: 'TaskCompleted', consumer: { id: 'sample.followup.report', version: '1', kind: 'EVENT_REPORT' },
    compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT,
    mode: 'simulation', grants: { 'tasks.get': 1, 'tasks.createFollowUp': 1 }, effectGrant: 1,
    keyBinding: { type: keyType, validate: value => brandedKeys.has(value) }, ...overrides,
  };
}
export function effectAdapters(read = async args => ({ kind: 'SUCCESS', value: { id: args.taskId, projectId: 'project1', status: 'COMPLETED' } }),
  write = async () => ({ kind: 'SUCCESS', value: { id: 'task2' }, effect: 'CONFIRMED' })) {
  return {
    'tasks.get': { version: '1', mode: 'simulation', keyType, invoke: read },
    'tasks.createFollowUp': { version: '1', mode: 'simulation', keyType, invoke: write },
  };
}
export function effectContext(overrides = {}) {
  return {
    bindings: { actorId: 'user1' }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 10000 },
    authorize: async () => true, grants: { 'tasks.get': 1, 'tasks.createFollowUp': 1 },
    effectGrant: 1, requestKey: nativeKey(), event: { id: 'event1', source: 'task1' }, ...overrides,
  };
}
export function effectPrepared(registrations = effectAdapters(), admission = effectAdmission(), bytes = effectBytes) {
  return prepare(bytes, admission, registrations);
}
export function alteredEffect(mutate) {
  const artifact = structuredClone(effectArtifact); mutate(artifact);
  artifact.dcmDigest = digest(canonicalBytes(artifact.dcm));
  const bytes = canonicalBytes(artifact);
  return { artifact, bytes, admission: effectAdmission({ expectedArtifactDigest: digest(bytes), expectedDcmDigest: artifact.dcmDigest }) };
}
export const effectInput = { taskId: 'task1', projectId: 'project1', title: 'Follow up' };
