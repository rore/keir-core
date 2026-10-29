import { readFileSync } from 'node:fs';
import { canonicalBytes, digest } from '../src/canonical.js';
import { prepare, ENVIRONMENT } from '../src/index.js';

// The default is the shared Java-produced fixture; an interim compiler output can be selected locally.
export const artifactBytes = readFileSync(process.env.KEIR_ARTIFACT_FILE ?? new URL('../../conformance/read-and-decide/artifact.json', import.meta.url));
export const artifact = JSON.parse(artifactBytes);
export function admission(overrides = {}, bytes = artifactBytes) {
  return {
    expectedArtifactDigest: digest(bytes), expectedDcmDigest: artifact.dcmDigest,
    entrypoint: 'ClassifyTask', consumer: { id: 'sample.classification', version: '1', kind: 'DISPLAY' },
    compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT,
    mode: 'simulation', grants: { 'tasks.get': 1 }, ...overrides
  };
}
export function adapters(invoke = async args => ({ kind: 'SUCCESS', value: { id: args.taskId, status: 'COMPLETED' } })) {
  return { 'tasks.get': { version: '1', mode: 'simulation', invoke } };
}
export function context(overrides = {}) {
  return { bindings: { actorId: 'user1' }, grants: { 'tasks.get': 1 }, deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 }, authorize: async () => true, ...overrides };
}
export function prepared(invoke, policy = {}) { return prepare(artifactBytes, admission(policy), adapters(invoke)); }
export function altered(mutate) {
  const value = structuredClone(artifact); mutate(value);
  const bytes = canonicalBytes(value);
  return { value, bytes, admission: admission({}, bytes) };
}
