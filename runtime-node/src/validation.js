import { canonicalBytes, digest } from './canonical.js';

export const ENVIRONMENT = Object.freeze({ celJava: '0.14.0', celNode: '0.6.1', protobufJava: '4.35.1', protobufNode: '2.6.2' });
export const LIMITS = Object.freeze({ artifactBytes: 1048576, dcmBytes: 65536, steps: 8, calls: 4, expressionNodes: 64, totalAstNodes: 512, traceEvents: 64, valueBytes: 8192, stringBytes: 256 });
const namePattern = /^[A-Za-z][A-Za-z0-9]{0,63}$/;
const idPattern = /^[A-Za-z][A-Za-z0-9.]{0,99}$/;
const reserved = new Set(['true', 'false', 'null', 'in', 'as', 'break', 'const', 'continue', 'else', 'for', 'function', 'if', 'import', 'let', 'loop', 'package', 'namespace', 'return', 'var', 'void', 'while']);
const builtinTypes = new Set(['Boolean', 'String', 'bool', 'bytes', 'double', 'dyn', 'int', 'list', 'map', 'null_type', 'string', 'type', 'uint', 'input']);

export class Fault extends Error {
  constructor(code, path) { super(code); this.code = code; this.path = path; }
}
export const requireThat = (condition, path, code = 'ARTIFACT_INVALID') => { if (!condition) throw new Fault(code, path); };
export const pointer = key => String(key).replaceAll('~', '~0').replaceAll('/', '~1');
export function keys(value, path, max = Infinity) {
  requireThat(value && typeof value === 'object' && !Array.isArray(value) && [Object.prototype, null].includes(Object.getPrototypeOf(value)), path);
  const names = Reflect.ownKeys(value);
  requireThat(names.length <= max && names.every(k => typeof k === 'string' && Object.getOwnPropertyDescriptor(value, k).enumerable && Object.hasOwn(Object.getOwnPropertyDescriptor(value, k), 'value')), path);
  return names;
}
export function shape(value, expected, path) {
  const actual = keys(value, path);
  requireThat(actual.length === expected.length && expected.every(k => Object.hasOwn(value, k)), path);
}
export function text(value, path, nonempty = false, max = 256) {
  requireThat(typeof value === 'string' && value.isWellFormed() && Buffer.byteLength(value) <= max && (!nonempty || value.length > 0), path);
}
export function integer(value, max, path, min = 1) { requireThat(Number.isSafeInteger(value) && value >= min && value <= max, path); }
export function named(value, path, dotted = false) { requireThat(typeof value === 'string' && (dotted ? idPattern : namePattern).test(value) && !reserved.has(value), path); }
export function freeze(value) {
  if (value && typeof value === 'object') { for (const item of Object.values(value)) freeze(item); Object.freeze(value); }
  return value;
}

export function narrowPolicy(dcm, policy, previous, path) {
  const catalog = keys(dcm.capabilities, path, 8).sort();
  const ceilings = Object.fromEntries(catalog.map(id => [id, previous[id]]));
  if (policy === undefined) return ceilings;
  const fields = keys(policy, path, 3);
  requireThat(fields.every(field => ['allow', 'deny', 'maxCalls'].includes(field)), path);
  function list(field) {
    const values = policy[field];
    requireThat(Array.isArray(values) && Object.getPrototypeOf(values) === Array.prototype && values.length <= catalog.length, path);
    requireThat(Reflect.ownKeys(values).length === values.length + 1, path);
    for (let i = 0; i < values.length; i++) {
      const item = Object.getOwnPropertyDescriptor(values, String(i));
      requireThat(item && Object.hasOwn(item, 'value') && catalog.includes(item.value), path);
    }
    requireThat(new Set(values).size === values.length, path); return values;
  }
  if (Object.hasOwn(policy, 'allow')) {
    const allow = list('allow'); requireThat(allow.every(id => previous[id] > 0), path);
    for (const id of catalog) if (!allow.includes(id)) ceilings[id] = 0;
  }
  if (Object.hasOwn(policy, 'deny')) for (const id of list('deny')) ceilings[id] = 0;
  if (Object.hasOwn(policy, 'maxCalls')) for (const id of keys(policy.maxCalls, path, 8).sort()) {
    requireThat(catalog.includes(id), path); integer(policy.maxCalls[id], ceilings[id], path, 0);
    ceilings[id] = policy.maxCalls[id];
  }
  return ceilings;
}

export function validateModel(dcm, profile, p = '/artifact/dcm') {
  const v2 = profile === 'keir-read/2' || profile === 'keir-effect/2';
  const effect = v2 ? profile === 'keir-effect/2' : dcm?.format === 'keir-dcm/effect-1';
  shape(dcm, ['format', 'domain', 'types', 'capabilities', 'entrypoints', ...(effect ? ['hostKey', 'assurance'] : [])], p);
  requireThat(v2 ? dcm.format === (effect ? 'keir-dcm/effect-2' : 'keir-dcm/read-2') : effect || dcm.format === 'keir-dcm/read-1', p);
  requireThat(canonicalBytes(dcm).length <= LIMITS.dcmBytes, p, 'BUDGET_EXCEEDED');
  shape(dcm.domain, ['id', 'version'], p + '/domain');
  text(dcm.domain.id, p, true); text(dcm.domain.version, p, true);
  const types = keys(dcm.types, p + '/types', 16);
  requireThat(types.length > 0, p);
  for (const name of types) {
    named(name, p); requireThat(!builtinTypes.has(name), p);
    const type = dcm.types[name];
    if (type?.kind === 'nominal') { shape(type, ['kind', 'base'], p); requireThat(type.base === 'String', p); }
    else if (type?.kind === 'enum') {
      shape(type, ['kind', 'values'], p);
      requireThat(Array.isArray(type.values) && type.values.length > 0 && type.values.length <= 16 && new Set(type.values).size === type.values.length, p);
      for (const value of type.values) text(value, p, true);
    } else if (type?.kind === 'record') {
      shape(type, ['kind', 'fields'], p);
      const fields = keys(type.fields, p, 16); requireThat(fields.length > 0, p);
      for (const field of fields) named(field, p);
    } else throw new Fault('DCM_INVALID', p);
  }
  const scalar = type => (!v2 || typeof type === 'string') && (type === 'Boolean' || type === 'String' || ['nominal', 'enum'].includes(dcm.types[type]?.kind));
  const record = type => (!v2 || typeof type === 'string') && dcm.types[type]?.kind === 'record';
  if (effect) {
    requireThat(dcm.assurance === 'SIMULATION_ONLY', p);
    shape(dcm.hostKey, ['source', 'type'], p + '/hostKey');
    requireThat(dcm.hostKey.source === 'requestKey', p + '/hostKey');
    text(dcm.hostKey.type, p + '/hostKey/type', true);
  }
  for (const type of Object.values(dcm.types)) if (type.kind === 'record') for (const fieldType of Object.values(type.fields)) requireThat(scalar(fieldType), p);
  for (const id of keys(dcm.capabilities, p + '/capabilities', 8)) {
    named(id, p, true);
    const cap = dcm.capabilities[id]; shape(cap, ['version', 'parameters', 'result', 'effect', 'errors'], p);
    text(cap.version, p, true); requireThat((cap.effect === 'READ' || (effect || v2) && cap.effect === 'WRITE') && record(cap.result), p);
    for (const param of keys(cap.parameters, p, 16)) {
      named(param, p); shape(cap.parameters[param], ['type', 'binding'], p);
      requireThat(scalar(cap.parameters[param].type) && ['agent', 'host'].includes(cap.parameters[param].binding), p);
    }
    requireThat(Array.isArray(cap.errors) && new Set(cap.errors).size === cap.errors.length, p);
    if (effect || v2) requireThat(cap.errors.length <= 16, p);
    for (const error of cap.errors) requireThat(typeof error === 'string' && /^[A-Z][A-Z0-9_]{0,63}$/.test(error), p);
  }
  const entries = keys(dcm.entrypoints, p + '/entrypoints', 4); requireThat(entries.length > 0, p);
  const policies = Object.create(null), effectGrants = Object.create(null);
  for (const id of entries) {
    named(id, p, true);
    const entry = dcm.entrypoints[id];
    shape(entry, ['input', 'output', 'trigger', 'consumer', ...(v2 ? (Object.hasOwn(entry, 'capabilityPolicy') ? ['capabilityPolicy'] : []) : ['grants']),
      ...(effect ? ['placement', 'event', ...(!v2 || Object.hasOwn(entry, 'effectGrant') ? ['effectGrant'] : [])] : [])], p);
    requireThat(record(entry.input) && record(entry.output) && entry.trigger === (effect ? 'EVENT' : 'REQUEST'), p);
    if (effect) {
      requireThat(entry.placement === 'AFTER_COMMIT', p);
      if (!v2 || Object.hasOwn(entry, 'effectGrant')) integer(entry.effectGrant, 1, p, 0);
      effectGrants[id] = entry.effectGrant ?? 1;
      shape(entry.event, ['idType', 'sourceType'], p);
      requireThat((!v2 || typeof entry.event.idType === 'string' && typeof entry.event.sourceType === 'string') && dcm.types[entry.event.idType]?.kind === 'nominal' && dcm.types[entry.event.sourceType]?.kind === 'nominal', p);
    }
    shape(entry.consumer, ['id', 'version', 'kind'], p);
    named(entry.consumer.id, p, true); text(entry.consumer.version, p, true); requireThat(entry.consumer.kind === (effect ? 'EVENT_REPORT' : 'DISPLAY'), p);
    if (v2) {
      if (Object.hasOwn(entry, 'capabilityPolicy')) requireThat(entry.capabilityPolicy !== undefined, p);
      policies[id] = narrowPolicy(dcm, entry.capabilityPolicy, Object.fromEntries(Object.entries(dcm.capabilities).map(([cap, value]) => [cap, effect || value.effect === 'READ' ? LIMITS.calls : 0])), p);
    } else for (const cap of keys(entry.grants, p, 8)) { requireThat(Object.hasOwn(dcm.capabilities, cap), p); integer(entry.grants[cap], LIMITS.calls, p); }
  }
  return { scalar, record, policies, effectGrants };
}

export function validateValue(dcm, typeName, value, path, code) {
  try {
    const type = dcm.types[typeName];
    if (typeName === 'Boolean') requireThat(typeof value === 'boolean', path);
    else if (typeName === 'String' || type?.kind === 'nominal' || type?.kind === 'enum') {
      requireThat(typeof value === 'string' && value.isWellFormed(), path);
      requireThat(Buffer.byteLength(value) <= LIMITS.stringBytes, path, 'BUDGET_EXCEEDED');
      if (type?.kind === 'enum') requireThat(type.values.includes(value), path);
    } else if (type?.kind === 'record') {
      shape(value, Object.keys(type.fields), path);
      for (const [field, scalar] of Object.entries(type.fields)) validateValue(dcm, scalar, value[field], path + '/' + pointer(field), code);
    } else throw new Fault(code, path);
    requireThat(canonicalBytes(value).length <= LIMITS.valueBytes, path, 'BUDGET_EXCEEDED');
    return freeze(structuredClone(value));
  } catch (error) {
    if (error instanceof Fault && error.code === 'BUDGET_EXCEEDED') throw error;
    throw new Fault(code, path);
  }
}

export function validateEnvelope(artifact, bound) {
  const v2 = bound !== undefined;
  shape(artifact, ['format', 'profile', 'compiler', 'environment', 'sourceDigest', ...(v2 ? ['modelRef'] : ['dcmDigest', 'dcm', 'descriptors']), 'entrypoint', 'expressions', 'steps', 'return'], '/artifact');
  const effect = v2 ? bound.profile === 'keir-effect/2' : artifact.profile === 'keir-effect/1';
  if (v2) {
    shape(artifact.environment, Object.keys(ENVIRONMENT), '/artifact/environment');
    shape(artifact.modelRef, ['domainId', 'domainVersion', 'format', 'digest', 'descriptorRecipe'], '/artifact/modelRef');
    text(artifact.compiler, '/artifact/compiler', true);
    requireThat(typeof artifact.entrypoint === 'string' && Object.hasOwn(bound.dcm.entrypoints, artifact.entrypoint), '/artifact/entrypoint');
    requireThat(Object.keys(ENVIRONMENT).every(k => artifact.environment[k] === ENVIRONMENT[k]), '/artifact/environment');
    requireThat(artifact.format === (effect ? 'keir-artifact/effect-2' : 'keir-artifact/read-2'), '/artifact/format');
    requireThat(canonicalBytes(artifact.modelRef).equals(canonicalBytes(bound.modelRef)), '/artifact/modelRef');
    requireThat(artifact.profile === bound.profile, '/artifact/profile');
    requireThat(typeof artifact.sourceDigest === 'string' && /^sha256:[0-9a-f]{64}$/.test(artifact.sourceDigest), '/artifact/sourceDigest');
    return { v2, effect, dcm: bound.dcm };
  }
  requireThat(effect ? artifact.format === 'keir-artifact/effect-1' : artifact.format === 'keir-artifact/read-1' && artifact.profile === 'keir-read/1', '/artifact');
  text(artifact.compiler, '/artifact/compiler', true);
  shape(artifact.environment, Object.keys(ENVIRONMENT), '/artifact/environment');
  requireThat(Object.keys(ENVIRONMENT).every(k => artifact.environment[k] === ENVIRONMENT[k]), '/artifact/environment', 'PIN_MISMATCH');
  for (const key of ['sourceDigest', 'dcmDigest']) requireThat(typeof artifact[key] === 'string' && /^sha256:[0-9a-f]{64}$/.test(artifact[key]), '/artifact/' + key);
  requireThat(digest(canonicalBytes(artifact.dcm)) === artifact.dcmDigest, '/artifact/dcmDigest');
  requireThat(artifact.dcm.format === (effect ? 'keir-dcm/effect-1' : 'keir-dcm/read-1'), '/artifact/dcm');
  return { v2, effect, dcm: artifact.dcm };
}

export function validatePlan(artifact, bound) {
  const { v2, effect, dcm } = validateEnvelope(artifact, bound);
  const { scalar } = v2 ? bound.model : validateModel(dcm);
  requireThat(Object.hasOwn(dcm.entrypoints, artifact.entrypoint), '/artifact/entrypoint');
  const entrypoint = dcm.entrypoints[artifact.entrypoint];
  const policy = v2 ? bound.model.policies[artifact.entrypoint] : entrypoint.grants;
  const effectGrant = v2 ? bound.model.effectGrants[artifact.entrypoint] : entrypoint.effectGrant;
  requireThat(Array.isArray(artifact.steps) && artifact.steps.length > 0 && artifact.steps.length <= LIMITS.steps, '/artifact/steps', 'BUDGET_EXCEEDED');
  const expressionNames = keys(artifact.expressions, '/artifact/expressions', LIMITS.totalAstNodes);
  const variables = Object.fromEntries([['input', entrypoint.input]]);
  const contexts = new Map();
  let nextExpression = 0, callCount = 0, writeCount = 0;
  const calls = Object.create(null);
  const names = new Set(['input', 'Boolean', 'String', ...Object.keys(dcm.types)]);
  const booleanBinds = new Set();
  function binding(value, type, path) {
    const isLiteral = Object.hasOwn(value ?? {}, 'literal');
    shape(value, [isLiteral ? 'literal' : 'expr', 'type'], path);
    requireThat(value.type === type && scalar(type), path);
    if (isLiteral) {
      requireThat(v2 || dcm.types[type]?.kind !== 'nominal', path);
      validateValue(dcm, type, value.literal, path, 'ARTIFACT_INVALID');
    } else {
      requireThat(value.expr === `e${nextExpression++}` && Object.hasOwn(artifact.expressions, value.expr), path);
      const expr = artifact.expressions[value.expr]; shape(expr, ['checked', 'variables', 'resultType'], path);
      shape(expr.variables, Object.keys(variables), path);
      requireThat(Object.keys(variables).every(k => expr.variables[k] === variables[k]) && expr.resultType === type, path);
      contexts.set(value.expr, { ...expr, variables: { ...variables } });
    }
  }
  function returned(value, path) {
    shape(value, ['type', 'fields'], path);
    requireThat(value.type === entrypoint.output, path);
    const output = dcm.types[entrypoint.output]; shape(value.fields, Object.keys(output.fields), path + '/fields');
    for (const field of Object.keys(output.fields).sort()) binding(value.fields[field], output.fields[field], path + '/fields/' + field);
  }
  artifact.steps.forEach((step, index) => {
    const p = '/artifact/steps/' + index;
    named(step?.id, p); requireThat(!names.has(step.id), p); names.add(step.id);
    if (step.op === 'CALL') {
      shape(step, ['op', 'id', 'capability', 'version', 'args', 'resultType',
        ...(effect && Object.hasOwn(step, 'when') ? ['when'] : []), ...(effect && Object.hasOwn(step, 'onError') ? ['onError'] : [])], p);
      if (v2) requireThat(typeof step.capability === 'string', p);
      requireThat(Object.hasOwn(dcm.capabilities, step.capability), p, v2 ? 'CAPABILITY_UNKNOWN' : 'ARTIFACT_INVALID');
      const cap = dcm.capabilities[step.capability];
      requireThat(step.version === cap.version && step.resultType === cap.result, p);
      if (Object.hasOwn(step, 'when')) requireThat(booleanBinds.has(step.when), p + '/when');
      const agent = Object.keys(cap.parameters).filter(k => cap.parameters[k].binding === 'agent').sort();
      shape(step.args, agent, p + '/args');
      for (const arg of agent) binding(step.args[arg], cap.parameters[arg].type, p + '/args/' + arg);
      if (Object.hasOwn(step, 'onError')) {
        const codes = keys(step.onError, p + '/onError', 16); requireThat(codes.length > 0, p + '/onError');
        for (const code of codes.sort()) {
          requireThat(cap.errors.includes(code), p + '/onError');
          returned(step.onError[code], p + '/onError/' + pointer(code));
        }
      }
      callCount++; calls[step.capability] = (calls[step.capability] ?? 0) + 1;
      requireThat(callCount <= LIMITS.calls && (v2 || calls[step.capability] <= (entrypoint.grants[step.capability] ?? 0)), p, 'BUDGET_EXCEEDED');
      if (cap.effect === 'WRITE') {
        writeCount++;
        requireThat(writeCount <= 1, p, 'BUDGET_EXCEEDED');
        requireThat(effect && writeCount <= effectGrant, p, 'ENTRY_GRANT_DENIED');
      }
      if (v2) requireThat(calls[step.capability] <= policy[step.capability], p, 'ENTRY_GRANT_DENIED');
    } else if (step.op === 'BIND') {
      shape(step, ['op', 'id', 'value', 'resultType'], p);
      binding(step.value, step.resultType, p + '/value');
      if (step.resultType === 'Boolean') booleanBinds.add(step.id);
    } else throw new Fault('ARTIFACT_INVALID', p);
    if (!Object.hasOwn(step, 'when')) variables[step.id] = step.resultType;
  });
  returned(artifact.return, '/artifact/return');
  requireThat(nextExpression === expressionNames.length, '/artifact/expressions');
  return { entrypoint, contexts, calls, callCount, writeCount, effect, policy, effectGrant };
}
