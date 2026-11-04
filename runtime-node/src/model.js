import { canonicalBytes, CanonicalLimitError, digest, parseCanonical, parseModelJson } from './canonical.js';
import { prepareModelTypes } from './cel.js';
import { ENVIRONMENT, LIMITS, Fault, requireThat as ok, keys, shape, text, integer, freeze, pointer, validateModel } from './validation.js';

const models = new WeakMap();
const fields = ['domainId', 'domainVersion', 'format', 'digest', 'descriptorRecipe'];
export const modelFor = handle => models.get(handle);
const same = (left, right) => canonicalBytes(left).equals(canonicalBytes(right));

function qualificationSnapshot(value, approval, dcm, modelRef) {
  const path = '/binding/qualification';
  const real = approval.profile === 'keir-effect/3' && approval.mode === 'real';
  if (!real) { ok(value === undefined, path); return undefined; }
  shape(value, ['evidence', 'isCurrent'], path);
  const isCurrent = value.isCurrent;
  ok(typeof isCurrent === 'function', path);
  let evidence;
  try { evidence = parseCanonical(canonicalBytes(value.evidence, 16_384), 16_384); }
  catch (error) { throw new Fault(error instanceof CanonicalLimitError ? 'BUDGET_EXCEEDED' : 'ADMISSION_INVALID', path); }
  shape(evidence, ['contract', 'id', 'revision', 'scope', 'modelRef', 'environment', 'hostBuild', 'keyType', 'capabilities', 'reportDigest'], path);
  ok(evidence.contract === 'keir-real-effect-evidence/1', path);
  text(evidence.id, path, true, 128); integer(evidence.revision, Number.MAX_SAFE_INTEGER, path);
  ok(evidence.scope === approval.scope && same(evidence.modelRef, modelRef) && same(evidence.environment, ENVIRONMENT), path);
  ok(evidence.keyType === dcm.hostKey.type && evidence.keyType === approval.keyBinding.type, path);
  for (const field of ['hostBuild', 'reportDigest']) ok(typeof evidence[field] === 'string' && /^sha256:[0-9a-f]{64}$/.test(evidence[field]), path);
  const catalog = Object.keys(dcm.capabilities).sort().map(id => ({ id, version: dcm.capabilities[id].version }));
  ok(same(evidence.capabilities, catalog), path);
  return Object.freeze({ evidence: freeze(evidence), isCurrent });
}

export function requireCurrent(bound, code, path) {
  if (!bound?.qualification) return;
  let current;
  try { current = bound.qualification.isCurrent(); } catch { /* A failed host check denies authority. */ }
  if (current === true) return;
  // Consume rejected native Promises without reading or assimilating arbitrary thenables.
  try { Promise.prototype.then.call(current, undefined, () => {}); } catch { /* Not a native Promise. */ }
  throw new Fault(code, path);
}

export function bindModel(dcmBytes, approval, invokers, qualification) {
  let path = '/binding/approval';
  try {
    const profile = approval && Object.getOwnPropertyDescriptor(approval, 'profile');
    const selectedProfile = profile && Object.hasOwn(profile, 'value') ? profile.value : undefined;
    const effect = selectedProfile === 'keir-effect/2' || selectedProfile === 'keir-effect/3';
    shape(approval, ['scope', 'modelRef', 'profile', 'environment', 'mode', ...(effect ? ['keyBinding'] : [])], path);
    let keyValidator;
    for (const field of Object.keys(approval).sort()) {
      path = '/binding/approval/' + field;
      if (field === 'environment') {
        shape(approval.environment, Object.keys(ENVIRONMENT), path); ok(same(approval.environment, ENVIRONMENT), path);
      } else if (field === 'keyBinding') {
        shape(approval.keyBinding, ['type', 'validate'], path); text(approval.keyBinding.type, path, true);
        ok(typeof approval.keyBinding.validate === 'function', path); keyValidator = approval.keyBinding.validate;
      } else if (field === 'mode') ok(effect && selectedProfile !== 'keir-effect/3' ? approval.mode === 'simulation' : ['simulation', 'real'].includes(approval.mode), path);
      else if (field === 'modelRef') {
        shape(approval.modelRef, fields, path);
        const modelRefPath = path;
        for (const part of [...fields].sort()) {
          const at = modelRefPath + '/' + part;
          path = at;
          if (part === 'descriptorRecipe') ok(approval.modelRef[part] === 'keir-record-descriptors/1', at);
          else if (part === 'digest') ok(typeof approval.modelRef.digest === 'string' && /^sha256:[0-9a-f]{64}$/.test(approval.modelRef.digest), at);
          else if (part === 'format') ok(approval.modelRef.format === (selectedProfile === 'keir-effect/3' ? 'keir-dcm/effect-3' : effect ? 'keir-dcm/effect-2' : 'keir-dcm/read-2'), at);
          else text(approval.modelRef[part], at, true);
        }
      } else if (field === 'profile') ok(['keir-read/2', 'keir-effect/2', 'keir-effect/3'].includes(approval.profile), path);
      else text(approval.scope, path, true);
    }
    path = '/binding/dcm';
    ok(dcmBytes instanceof Uint8Array, path, 'ADMISSION_INVALID');
    ok(dcmBytes.byteLength <= LIMITS.dcmBytes, path, 'BUDGET_EXCEEDED');
    const raw = Buffer.from(dcmBytes), dcm = parseModelJson(raw), canonical = canonicalBytes(dcm);
    ok(canonical.length <= LIMITS.dcmBytes, path, 'BUDGET_EXCEEDED');
    const model = validateModel(dcm, approval.profile, path);
    const modelRef = { domainId: dcm.domain.id, domainVersion: dcm.domain.version, format: dcm.format, digest: digest(canonical), descriptorRecipe: 'keir-record-descriptors/1' };
    path = '/binding/approval/modelRef'; ok(same(modelRef, approval.modelRef), path);
    if (effect) { path = '/binding/approval/keyBinding'; ok(approval.keyBinding.type === dcm.hostKey.type, path); }
    path = '/binding/dcm/types'; const types = prepareModelTypes(dcm, path);
    path = '/binding/qualification';
    const retained = qualificationSnapshot(qualification, approval, dcm, modelRef);
    path = '/binding/invokers';
    shape(invokers, Object.keys(dcm.capabilities), path);
    const adapters = Object.create(null);
    for (const id of keys(invokers, path, 8).sort()) {
      path = '/binding/invokers/' + pointer(id);
      const invoker = invokers[id]; shape(invoker, ['version', 'mode', 'invoke', ...(effect ? ['keyType'] : [])], path);
      ok(invoker.version === dcm.capabilities[id].version && invoker.mode === approval.mode && typeof invoker.invoke === 'function', path);
      if (effect) ok(invoker.keyType === dcm.hostKey.type, path);
      adapters[id] = Object.freeze({ invoke: invoker.invoke });
    }
    const bound = Object.freeze({ dcm: freeze(dcm), raw, canonical, modelRef: freeze(modelRef), model,
      scope: approval.scope, profile: approval.profile, mode: approval.mode, effect, keyValidator, types, adapters: Object.freeze(adapters), qualification: retained });
    path = '/binding/qualification'; requireCurrent(bound, 'ADMISSION_INVALID', path);
    const boundModel = Object.freeze(Object.create(null));
    models.set(boundModel, bound);
    return Object.freeze({ boundModel, diagnostic: null });
  } catch (error) {
    const diagnostic = Object.freeze({ code: error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'ADMISSION_INVALID', path,
      message: 'The selected artifact or host configuration is invalid.' });
    return Object.freeze({ boundModel: null, diagnostic });
  }
}
