import { canonicalBytes, digest, parseModelJson } from './canonical.js';
import { prepareModelTypes } from './cel.js';
import { LIMITS, Fault, requireThat as ok, keys, shape, freeze, pointer, validateModel } from './validation.js';

const models = new WeakMap();
export const modelFor = handle => models.get(handle);

export function bindModel(dcmBytes, implementations) {
  let path = '/binding';
  try {
    ok(arguments.length === 2, path, 'ADMISSION_INVALID');
    path = '/binding/dcm';
    ok(dcmBytes instanceof Uint8Array, path, 'ADMISSION_INVALID');
    ok(dcmBytes.byteLength <= LIMITS.dcmBytes, path, 'BUDGET_EXCEEDED');
    const raw = Buffer.from(dcmBytes), dcm = parseModelJson(raw), canonical = canonicalBytes(dcm);
    const model = validateModel(dcm, path);
    const modelRef = { domainId: dcm.domain.id, domainVersion: dcm.domain.version, format: dcm.format,
      digest: digest(canonical), descriptorRecipe: dcm.format === 'keir-dcm/4' ? 'keir-record-descriptors/3' : 'keir-record-descriptors/2' };
    path = '/binding/dcm/types';
    const types = prepareModelTypes(dcm, path);
    path = '/binding/implementations';
    shape(implementations, Object.keys(dcm.capabilities), path);
    const adapters = Object.create(null);
    let mode;
    for (const id of keys(implementations, path, 8).sort()) {
      path = '/binding/implementations/' + pointer(id);
      const implementation = implementations[id];
      shape(implementation, ['version', 'mode', 'keyType', 'invoke'], path);
      ok(implementation.version === dcm.capabilities[id].version && ['simulation', 'real'].includes(implementation.mode) &&
        implementation.keyType === dcm.hostKey.type && typeof implementation.invoke === 'function', path);
      if (mode === undefined) mode = implementation.mode;
      ok(implementation.mode === mode, path);
      adapters[id] = Object.freeze({ invoke: implementation.invoke });
    }
    const bound = Object.freeze({ dcm: freeze(dcm), modelRef: freeze(modelRef), model, types,
      adapters: Object.freeze(adapters) });
    const boundModel = Object.freeze(Object.create(null));
    models.set(boundModel, bound);
    return Object.freeze({ boundModel, diagnostic: null });
  } catch (error) {
    return Object.freeze({ boundModel: null, diagnostic: Object.freeze({
      code: error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'ADMISSION_INVALID', path,
      message: 'The selected artifact or host configuration is invalid.' }) });
  }
}
