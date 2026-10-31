import { canonicalBytes, digest, parseModelJson } from './canonical.js';
import { prepareModelTypes } from './cel.js';
import { ENVIRONMENT, LIMITS, Fault, requireThat as ok, keys, shape, text, freeze, pointer, validateModel } from './validation.js';

const models = new WeakMap();
const fields = ['domainId', 'domainVersion', 'format', 'digest', 'descriptorRecipe'];
export const modelFor = handle => models.get(handle);
const same = (left, right) => canonicalBytes(left).equals(canonicalBytes(right));

export function bindModel(dcmBytes, approval, invokers) {
  let path = '/binding/approval';
  try {
    const effect = approval?.profile === 'keir-effect/2';
    shape(approval, ['scope', 'modelRef', 'profile', 'environment', 'mode', ...(effect ? ['keyBinding'] : [])], path);
    let keyValidator;
    for (const field of Object.keys(approval).sort()) {
      path = '/binding/approval/' + field;
      if (field === 'environment') {
        shape(approval.environment, Object.keys(ENVIRONMENT), path); ok(same(approval.environment, ENVIRONMENT), path);
      } else if (field === 'keyBinding') {
        shape(approval.keyBinding, ['type', 'validate'], path); text(approval.keyBinding.type, path, true);
        ok(typeof approval.keyBinding.validate === 'function', path); keyValidator = approval.keyBinding.validate;
      } else if (field === 'mode') ok(effect ? approval.mode === 'simulation' : ['simulation', 'real'].includes(approval.mode), path);
      else if (field === 'modelRef') {
        shape(approval.modelRef, fields, path);
        for (const part of [...fields].sort()) {
          const at = path + '/' + part;
          if (part === 'descriptorRecipe') { if (approval.modelRef[part] !== 'keir-record-descriptors/1') { path = at; throw new Fault('ADMISSION_INVALID', at); } }
          else if (part === 'digest') ok(typeof approval.modelRef.digest === 'string' && /^sha256:[0-9a-f]{64}$/.test(approval.modelRef.digest), path);
          else text(approval.modelRef[part], path, true);
        }
      } else if (field === 'profile') ok(['keir-read/2', 'keir-effect/2'].includes(approval.profile), path);
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
    const boundModel = Object.freeze(Object.create(null));
    models.set(boundModel, Object.freeze({ dcm: freeze(dcm), raw, canonical, modelRef: freeze(modelRef), model,
      scope: approval.scope, profile: approval.profile, mode: approval.mode, effect, keyValidator, types, adapters: Object.freeze(adapters) }));
    return Object.freeze({ boundModel, diagnostic: null });
  } catch (error) {
    const diagnostic = Object.freeze({ code: error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'ADMISSION_INVALID', path,
      message: 'The selected artifact or host configuration is invalid.' });
    return Object.freeze({ boundModel: null, diagnostic });
  }
}
