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

export function validateModel(dcm) {
  const p = '/artifact/dcm';
  shape(dcm, ['format', 'domain', 'types', 'capabilities', 'entrypoints'], p);
  requireThat(dcm.format === 'keir-dcm/read-1', p);
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
  const scalar = type => type === 'Boolean' || type === 'String' || ['nominal', 'enum'].includes(dcm.types[type]?.kind);
  const record = type => dcm.types[type]?.kind === 'record';
  for (const type of Object.values(dcm.types)) if (type.kind === 'record') for (const fieldType of Object.values(type.fields)) requireThat(scalar(fieldType), p);
  for (const id of keys(dcm.capabilities, p + '/capabilities', 8)) {
    named(id, p, true);
    const cap = dcm.capabilities[id]; shape(cap, ['version', 'parameters', 'result', 'effect', 'errors'], p);
    text(cap.version, p, true); requireThat(cap.effect === 'READ' && record(cap.result), p);
    for (const param of keys(cap.parameters, p, 16)) {
      named(param, p); shape(cap.parameters[param], ['type', 'binding'], p);
      requireThat(scalar(cap.parameters[param].type) && ['agent', 'host'].includes(cap.parameters[param].binding), p);
    }
    requireThat(Array.isArray(cap.errors) && new Set(cap.errors).size === cap.errors.length, p);
    for (const error of cap.errors) requireThat(typeof error === 'string' && /^[A-Z][A-Z0-9_]{0,63}$/.test(error), p);
  }
  const entries = keys(dcm.entrypoints, p + '/entrypoints', 4); requireThat(entries.length > 0, p);
  for (const id of entries) {
    named(id, p, true);
    const entry = dcm.entrypoints[id]; shape(entry, ['input', 'output', 'trigger', 'consumer', 'grants'], p);
    requireThat(record(entry.input) && record(entry.output) && entry.trigger === 'REQUEST', p);
    shape(entry.consumer, ['id', 'version', 'kind'], p);
    named(entry.consumer.id, p, true); text(entry.consumer.version, p, true); requireThat(entry.consumer.kind === 'DISPLAY', p);
    for (const cap of keys(entry.grants, p, 8)) { requireThat(Object.hasOwn(dcm.capabilities, cap), p); integer(entry.grants[cap], LIMITS.calls, p); }
  }
  return { scalar, record };
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

export function validatePlan(artifact) {
  shape(artifact, ['format', 'profile', 'compiler', 'environment', 'sourceDigest', 'dcmDigest', 'dcm', 'entrypoint', 'descriptors', 'expressions', 'steps', 'return'], '/artifact');
  requireThat(artifact.format === 'keir-artifact/read-1' && artifact.profile === 'keir-read/1', '/artifact');
  text(artifact.compiler, '/artifact/compiler', true);
  shape(artifact.environment, Object.keys(ENVIRONMENT), '/artifact/environment');
  requireThat(Object.keys(ENVIRONMENT).every(k => artifact.environment[k] === ENVIRONMENT[k]), '/artifact/environment', 'PIN_MISMATCH');
  for (const key of ['sourceDigest', 'dcmDigest']) requireThat(typeof artifact[key] === 'string' && /^sha256:[0-9a-f]{64}$/.test(artifact[key]), '/artifact/' + key);
  requireThat(digest(canonicalBytes(artifact.dcm)) === artifact.dcmDigest, '/artifact/dcmDigest');
  const { scalar } = validateModel(artifact.dcm);
  const dcm = artifact.dcm;
  requireThat(Object.hasOwn(dcm.entrypoints, artifact.entrypoint), '/artifact/entrypoint');
  const entrypoint = dcm.entrypoints[artifact.entrypoint];
  requireThat(Array.isArray(artifact.steps) && artifact.steps.length > 0 && artifact.steps.length <= LIMITS.steps, '/artifact/steps', 'BUDGET_EXCEEDED');
  const expressionNames = keys(artifact.expressions, '/artifact/expressions', LIMITS.totalAstNodes);
  const variables = Object.fromEntries([['input', entrypoint.input]]);
  const contexts = new Map();
  let nextExpression = 0, callCount = 0;
  const calls = Object.create(null);
  function binding(value, type, path) {
    const isLiteral = Object.hasOwn(value ?? {}, 'literal');
    shape(value, [isLiteral ? 'literal' : 'expr', 'type'], path);
    requireThat(value.type === type && scalar(type), path);
    if (isLiteral) {
      requireThat(dcm.types[type]?.kind !== 'nominal', path);
      validateValue(dcm, type, value.literal, path, 'ARTIFACT_INVALID');
    } else {
      requireThat(value.expr === `e${nextExpression++}` && Object.hasOwn(artifact.expressions, value.expr), path);
      const expr = artifact.expressions[value.expr]; shape(expr, ['checked', 'variables', 'resultType'], path);
      shape(expr.variables, Object.keys(variables), path);
      requireThat(Object.keys(variables).every(k => expr.variables[k] === variables[k]) && expr.resultType === type, path);
      contexts.set(value.expr, { ...expr, variables: { ...variables } });
    }
  }
  artifact.steps.forEach((step, index) => {
    const p = '/artifact/steps/' + index;
    named(step?.id, p); requireThat(!['input', 'Boolean', 'String'].includes(step.id) && !Object.hasOwn(variables, step.id) && !Object.hasOwn(dcm.types, step.id), p);
    if (step.op === 'CALL') {
      shape(step, ['op', 'id', 'capability', 'version', 'args', 'resultType'], p);
      requireThat(Object.hasOwn(dcm.capabilities, step.capability), p);
      const cap = dcm.capabilities[step.capability];
      requireThat(step.version === cap.version && step.resultType === cap.result, p);
      const agent = Object.keys(cap.parameters).filter(k => cap.parameters[k].binding === 'agent').sort();
      shape(step.args, agent, p + '/args');
      for (const arg of agent) binding(step.args[arg], cap.parameters[arg].type, p + '/args/' + arg);
      callCount++; calls[step.capability] = (calls[step.capability] ?? 0) + 1;
      requireThat(callCount <= LIMITS.calls && calls[step.capability] <= (entrypoint.grants[step.capability] ?? 0), p, 'BUDGET_EXCEEDED');
    } else if (step.op === 'BIND') {
      shape(step, ['op', 'id', 'value', 'resultType'], p);
      binding(step.value, step.resultType, p + '/value');
    } else throw new Fault('ARTIFACT_INVALID', p);
    variables[step.id] = step.resultType;
  });
  shape(artifact.return, ['type', 'fields'], '/artifact/return');
  requireThat(artifact.return.type === entrypoint.output, '/artifact/return');
  const output = dcm.types[entrypoint.output]; shape(artifact.return.fields, Object.keys(output.fields), '/artifact/return/fields');
  for (const field of Object.keys(output.fields).sort()) binding(artifact.return.fields[field], output.fields[field], '/artifact/return/fields/' + field);
  requireThat(nextExpression === expressionNames.length, '/artifact/expressions');
  return { entrypoint, contexts, calls, callCount };
}
