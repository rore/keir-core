import { canonicalBytes, digest } from '../src/canonical.js';
import { ENVIRONMENT } from '../src/validation.js';

export const dcm = {
  format: 'keir-dcm/read-2',
  domain: { id: 'sample.binding', version: '1' },
  types: { Id: { kind: 'nominal', base: 'String' }, Input: { kind: 'record', fields: { id: 'Id' } }, Output: { kind: 'record', fields: { id: 'Id' } } },
  capabilities: {
    'sample.used': { version: '1', parameters: {}, result: 'Output', effect: 'READ', errors: [] },
    'sample.unused': { version: '2', parameters: {}, result: 'Output', effect: 'READ', errors: [] },
    'sample.write': { version: '1', parameters: {}, result: 'Output', effect: 'WRITE', errors: [] },
  },
  entrypoints: { Main: { input: 'Input', output: 'Output', trigger: 'REQUEST', consumer: { id: 'sample.binding', version: '1', kind: 'DISPLAY' } } },
};
export const dcmBytes = canonicalBytes(dcm);
export const modelRef = Object.freeze({ domainId: dcm.domain.id, domainVersion: dcm.domain.version, format: dcm.format, digest: digest(dcmBytes), descriptorRecipe: 'keir-record-descriptors/1' });
export const environment = ENVIRONMENT;
export const approval = (overrides = {}) => ({ scope: 'tenant-a', modelRef, profile: 'keir-read/2', environment, mode: 'simulation', ...overrides });
export const invokers = (overrides = {}) => ({
  'sample.used': { version: '1', mode: 'simulation', invoke() {} },
  'sample.unused': { version: '2', mode: 'simulation', invoke() {} },
  // read/2 binds published WRITE definitions even though its plan cannot call them.
  'sample.write': { version: '1', mode: 'simulation', invoke() {} },
  ...overrides,
});

export const effectDcm = {
  format: 'keir-dcm/effect-2', domain: { id: 'sample.binding', version: '1' },
  types: { Id: { kind: 'nominal', base: 'String' }, Input: { kind: 'record', fields: { id: 'Id' } }, Output: { kind: 'record', fields: { id: 'Id' } } },
  capabilities: { 'sample.used': { version: '1', parameters: {}, result: 'Output', effect: 'READ', errors: [] } },
  entrypoints: { Main: { input: 'Input', output: 'Output', trigger: 'EVENT', placement: 'AFTER_COMMIT', consumer: { id: 'sample.binding', version: '1', kind: 'EVENT_REPORT' }, event: { idType: 'Id', sourceType: 'Id' } } },
  hostKey: { source: 'requestKey', type: 'sample.RequestUuid/1' }, assurance: 'SIMULATION_ONLY',
};
export const effectDcmBytes = canonicalBytes(effectDcm);
export const effectModelRef = Object.freeze({ domainId: effectDcm.domain.id, domainVersion: effectDcm.domain.version, format: effectDcm.format, digest: digest(effectDcmBytes), descriptorRecipe: 'keir-record-descriptors/1' });
export const effectApproval = (overrides = {}) => ({ scope: 'tenant-a', modelRef: effectModelRef, profile: 'keir-effect/2', environment, mode: 'simulation', keyBinding: { type: 'sample.RequestUuid/1', validate() {} }, ...overrides });
export const effectInvokers = (keyType = 'sample.RequestUuid/1') => ({ 'sample.used': { version: '1', mode: 'simulation', keyType, invoke() {} } });
