import { types as nodeTypes } from 'node:util';
import { CanonicalLimitError, canonicalBytes } from './canonical.js';

export const ENVIRONMENT = Object.freeze({ celJava: '0.14.0', celNode: '0.6.1', protobufJava: '4.35.1', protobufNode: '2.6.2' });
export const LIMITS = Object.freeze({ artifactBytes: 1048576, dcmBytes: 65536, steps: 8, calls: 4, expressionNodes: 64, totalAstNodes: 512, traceEvents: 64, valueBytes: 8192, stringBytes: 256 });
const namePattern = /^[A-Za-z][A-Za-z0-9]{0,63}$/;
const idPattern = /^[A-Za-z][A-Za-z0-9.]{0,99}$/;
const reserved = new Set(['true', 'false', 'null', 'in', 'as', 'break', 'const', 'continue', 'else', 'for', 'function', 'if', 'import', 'let', 'loop', 'package', 'namespace', 'return', 'var', 'void', 'while']);
const builtinTypes = new Set(['Boolean', 'String', 'Timestamp', 'Duration', 'bool', 'bytes', 'double', 'dyn', 'int', 'list', 'map', 'null_type', 'string', 'type', 'uint', 'input']);
const timestampPattern = /^(?!0000)[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}Z$/;

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
function shapeWithOptional(value, declared, optional, path) {
  const actual = keys(value, path, declared.length);
  requireThat(actual.every(name => declared.includes(name)) &&
    declared.every(name => Object.hasOwn(value, name) || optional(name)), path);
  return actual;
}
export const fieldType = declaration => typeof declaration === 'string' ? declaration : declaration.type;
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

export function validateModel(dcm, p) {
  shape(dcm, ['format', 'domain', 'types', 'capabilities', 'entrypoints', 'hostKey'], p);
  requireThat(dcm.format === 'keir-dcm/2' || dcm.format === 'keir-dcm/3', p);
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
    } else if (type?.kind === 'list') {
      shape(type, ['kind', 'element', 'maxItems'], p);
      integer(type.maxItems, 16, p, 0);
    } else throw new Fault('DCM_INVALID', p);
  }
  const listElement = type => typeof type === 'string' && (type === 'Boolean' || type === 'String' || ['nominal', 'enum'].includes(dcm.types[type]?.kind));
  const scalar = type => type === 'Timestamp' || listElement(type);
  const valueType = type => scalar(type) || typeof type === 'string' && dcm.types[type]?.kind === 'list';
  const record = type => typeof type === 'string' && dcm.types[type]?.kind === 'record';
  shape(dcm.hostKey, ['source', 'type'], p + '/hostKey');
  requireThat(dcm.hostKey.source === 'requestKey', p + '/hostKey');
  text(dcm.hostKey.type, p + '/hostKey/type', true);
  for (const type of Object.values(dcm.types)) {
    if (type.kind === 'list') requireThat(listElement(type.element), p);
    if (type.kind !== 'record') continue;
    for (const declaration of Object.values(type.fields)) {
      if (typeof declaration === 'string') requireThat(valueType(declaration), p);
      else {
        shape(declaration, ['type', 'optional'], p);
        requireThat(declaration.optional === true && scalar(declaration.type), p);
      }
    }
  }
  for (const id of keys(dcm.capabilities, p + '/capabilities', 8)) {
    named(id, p, true);
    const cap = dcm.capabilities[id]; shape(cap, ['version', 'parameters', 'result', 'effect', 'errors'], p);
    text(cap.version, p, true); requireThat(['READ', 'WRITE'].includes(cap.effect) && record(cap.result), p);
    for (const param of keys(cap.parameters, p, 16)) {
      named(param, p);
      const parameter = cap.parameters[param], optional = Object.hasOwn(parameter ?? {}, 'optional');
      shape(parameter, ['type', 'binding', ...(optional ? ['optional'] : [])], p);
      requireThat(valueType(parameter.type) && ['agent', 'host'].includes(parameter.binding), p);
      if (optional) requireThat(parameter.optional === true && parameter.binding === 'agent' && scalar(parameter.type), p);
    }
    requireThat(Array.isArray(cap.errors) && new Set(cap.errors).size === cap.errors.length, p);
    requireThat(cap.errors.length <= 16, p, 'BUDGET_EXCEEDED');
    for (const error of cap.errors) requireThat(typeof error === 'string' && /^[A-Z][A-Z0-9_]{0,63}$/.test(error), p);
  }
  const entries = keys(dcm.entrypoints, p + '/entrypoints', 4); requireThat(entries.length > 0, p);
  const policies = Object.create(null), effectGrants = Object.create(null);
  for (const id of entries) {
    named(id, p, true);
    const entry = dcm.entrypoints[id];
    shape(entry, ['input', 'output', 'trigger', 'consumer', 'placement', 'event',
      ...['capabilityPolicy', 'effectGrant'].filter(field => Object.hasOwn(entry, field))], p);
    requireThat(record(entry.input) && record(entry.output), p);
    shape(entry.event, ['idType', 'sourceType'], p);
    requireThat(typeof entry.event.idType === 'string' && typeof entry.event.sourceType === 'string' &&
      dcm.types[entry.event.idType]?.kind === 'nominal' && dcm.types[entry.event.sourceType]?.kind === 'nominal', p);
    shape(entry.consumer, ['id', 'version', 'kind'], p);
    named(entry.consumer.id, p, true); text(entry.consumer.version, p, true);
    const decision = entry.consumer.kind === 'DECISION';
    if (decision) {
      requireThat(dcm.format === 'keir-dcm/3' && entry.trigger === 'REQUEST' && entry.placement === 'BEFORE_COMMIT', p);
      const field = dcm.types[entry.output].fields.decision;
      const type = typeof field === 'string' ? dcm.types[field] : undefined;
      requireThat(type?.kind === 'enum' && type.values.length === 2 &&
        type.values.includes('ALLOW') && type.values.includes('DENY'), p);
      requireThat(Object.values(dcm.types[entry.output].fields)
        .every(declaration => scalar(fieldType(declaration))), p);
    } else {
      requireThat(entry.trigger === 'EVENT' && entry.placement === 'AFTER_COMMIT' && entry.consumer.kind === 'EVENT_REPORT', p);
    }
    if (Object.hasOwn(entry, 'effectGrant')) integer(entry.effectGrant, decision ? 0 : 1, p, 0);
    effectGrants[id] = entry.effectGrant ?? (decision ? 0 : 1);
    policies[id] = narrowPolicy(dcm, entry.capabilityPolicy, Object.fromEntries(Object.keys(dcm.capabilities).map(cap => [cap, LIMITS.calls])), p);
  }
  return { scalar, valueType, policies, effectGrants };
}

function checkValue(dcm, typeName, value, path) {
  const type = dcm.types[typeName];
  if (typeName === 'Boolean') requireThat(typeof value === 'boolean', path);
  else if (typeName === 'Timestamp') {
    requireThat(typeof value === 'string' && timestampPattern.test(value), path);
    const millis = Date.parse(value);
    requireThat(Number.isFinite(millis) && new Date(millis).toISOString() === value, path);
  } else if (typeName === 'String' || type?.kind === 'nominal' || type?.kind === 'enum') {
    requireThat(typeof value === 'string' && value.isWellFormed(), path);
    requireThat(Buffer.byteLength(value) <= LIMITS.stringBytes, path, 'BUDGET_EXCEEDED');
    if (type?.kind === 'enum') requireThat(type.values.includes(value), path);
  } else if (type?.kind === 'list') {
    requireThat(Array.isArray(value) && !nodeTypes.isProxy(value) && Object.getPrototypeOf(value) === Array.prototype, path);
    requireThat(value.length <= type.maxItems, path, 'BUDGET_EXCEEDED');
    requireThat(Reflect.ownKeys(value).length === value.length + 1, path);
    for (let i = 0; i < value.length; i++) {
      const item = Object.getOwnPropertyDescriptor(value, String(i));
      requireThat(item?.enumerable && Object.hasOwn(item, 'value'), path);
      checkValue(dcm, type.element, item.value, path + '/' + i);
    }
  } else if (type?.kind === 'record') {
    requireThat(!nodeTypes.isProxy(value), path);
    shapeWithOptional(value, Object.keys(type.fields), field => typeof type.fields[field] !== 'string', path);
    for (const [field, declaration] of Object.entries(type.fields)) {
      if (Object.hasOwn(value, field)) checkValue(dcm, fieldType(declaration), value[field], path + '/' + pointer(field));
    }
  } else throw new Fault('ARTIFACT_INVALID', path);
}

export function validateValue(dcm, typeName, value, path, code) {
  try {
    checkValue(dcm, typeName, value, path);
    canonicalBytes(value, LIMITS.valueBytes);
    return freeze(structuredClone(value));
  } catch (error) {
    if (error instanceof Fault && error.code === 'BUDGET_EXCEEDED') throw error;
    if (error instanceof CanonicalLimitError) throw new Fault('BUDGET_EXCEEDED', path);
    throw new Fault(code, path);
  }
}

export function validateEnvelope(artifact, bound) {
  shape(artifact, ['format', 'compiler', 'environment', 'sourceDigest', 'modelRef', 'entrypoint', 'expressions', 'steps', 'return'], '/artifact');
  shape(artifact.environment, Object.keys(ENVIRONMENT), '/artifact/environment');
  shape(artifact.modelRef, ['domainId', 'domainVersion', 'format', 'digest', 'descriptorRecipe'], '/artifact/modelRef');
  text(artifact.compiler, '/artifact/compiler', true);
  requireThat(typeof artifact.entrypoint === 'string' && Object.hasOwn(bound.dcm.entrypoints, artifact.entrypoint), '/artifact/entrypoint');
  requireThat(Object.keys(ENVIRONMENT).every(k => artifact.environment[k] === ENVIRONMENT[k]), '/artifact/environment', 'PIN_MISMATCH');
  requireThat(['keir-artifact/2', 'keir-artifact/3'].includes(artifact.format), '/artifact/format');
  const legacy = artifact.format === 'keir-artifact/2';
  requireThat(artifact.compiler === (legacy ? 'keir-compiler-java/0.2.0' : 'keir-compiler-java/0.3.0'), '/artifact/compiler', 'PIN_MISMATCH');
  requireThat(bound.dcm.format === (legacy ? 'keir-dcm/2' : 'keir-dcm/3'), '/artifact/modelRef', 'PIN_MISMATCH');
  requireThat(canonicalBytes(artifact.modelRef).equals(canonicalBytes(bound.modelRef)), '/artifact/modelRef');
  requireThat(typeof artifact.sourceDigest === 'string' && /^sha256:[0-9a-f]{64}$/.test(artifact.sourceDigest), '/artifact/sourceDigest');
}

export function validatePlan(artifact, { dcm, model }) {
  const { scalar, valueType } = model;
  const entrypoint = dcm.entrypoints[artifact.entrypoint];
  const policy = model.policies[artifact.entrypoint];
  const effectGrant = model.effectGrants[artifact.entrypoint];
  requireThat(Array.isArray(artifact.steps) && artifact.steps.length > 0 && artifact.steps.length <= LIMITS.steps, '/artifact/steps', 'BUDGET_EXCEEDED');
  const expressionNames = keys(artifact.expressions, '/artifact/expressions', LIMITS.totalAstNodes);
  const variables = Object.fromEntries([['input', entrypoint.input]]);
  const contexts = new Map();
  let nextExpression = 0, callCount = 0, writeCount = 0;
  const calls = Object.create(null);
  const names = new Set(['input', 'Boolean', 'String', 'Timestamp', 'Duration', ...Object.keys(dcm.types)]);
  const booleanBinds = new Set();
  function binding(value, type, path) {
    const isLiteral = Object.hasOwn(value ?? {}, 'literal');
    shape(value, [isLiteral ? 'literal' : 'expr', 'type'], path);
    requireThat(value.type === type && (isLiteral ? scalar(type) && type !== 'Timestamp' : valueType(type)), path);
    if (isLiteral) {
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
    const { fields } = dcm.types[entrypoint.output];
    const supplied = shapeWithOptional(value.fields, Object.keys(fields), field => typeof fields[field] !== 'string', path + '/fields');
    for (const field of supplied.sort()) binding(value.fields[field], fieldType(fields[field]), path + '/fields/' + field);
  }
  function countCall(capability, cap, path) {
    callCount++; calls[capability] = (calls[capability] ?? 0) + 1;
    requireThat(callCount <= LIMITS.calls, path, 'BUDGET_EXCEEDED');
    if (cap.effect === 'WRITE') {
      writeCount++;
      requireThat(writeCount <= 1, path, 'BUDGET_EXCEEDED');
      requireThat(writeCount <= effectGrant, path, 'ENTRY_GRANT_DENIED');
    }
    requireThat(calls[capability] <= policy[capability], path, 'ENTRY_GRANT_DENIED');
  }
  artifact.steps.forEach((step, index) => {
    const p = '/artifact/steps/' + index;
    named(step?.id, p); requireThat(!names.has(step.id), p); names.add(step.id);
    if (step.op === 'CALL') {
      shape(step, ['op', 'id', 'capability', 'version', 'args', 'resultType',
        ...['when', 'onError'].filter(field => Object.hasOwn(step, field))], p);
      requireThat(typeof step.capability === 'string', p);
      requireThat(Object.hasOwn(dcm.capabilities, step.capability), p, 'CAPABILITY_UNKNOWN');
      const cap = dcm.capabilities[step.capability];
      requireThat(step.version === cap.version && step.resultType === cap.result, p);
      if (Object.hasOwn(step, 'when')) requireThat(booleanBinds.has(step.when), p + '/when');
      const agent = Object.keys(cap.parameters).filter(k => cap.parameters[k].binding === 'agent');
      const supplied = shapeWithOptional(step.args, agent, name => cap.parameters[name].optional === true, p + '/args');
      countCall(step.capability, cap, p);
      for (const arg of supplied.sort()) binding(step.args[arg], cap.parameters[arg].type, p + '/args/' + arg);
      if (Object.hasOwn(step, 'onError')) {
        const codes = keys(step.onError, p + '/onError', 16); requireThat(codes.length > 0, p + '/onError');
        for (const code of codes.sort()) {
          requireThat(cap.errors.includes(code), p + '/onError');
          returned(step.onError[code], p + '/onError/' + pointer(code));
        }
      }
    } else if (step.op === 'BIND') {
      shape(step, ['op', 'id', 'value', 'resultType'], p);
      binding(step.value, step.resultType, p + '/value');
      if (step.resultType === 'Boolean') booleanBinds.add(step.id);
    } else throw new Fault('ARTIFACT_INVALID', p);
    if (!Object.hasOwn(step, 'when')) variables[step.id] = step.resultType;
  });
  returned(artifact.return, '/artifact/return');
  requireThat(nextExpression === expressionNames.length, '/artifact/expressions');
  return { entrypoint, contexts, calls, policy, effectGrant };
}
