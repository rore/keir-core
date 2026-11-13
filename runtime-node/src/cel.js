import { create, createFileRegistry, fromBinary, fromJson, toBinary } from '@bufbuild/protobuf';
import { DurationSchema, FileDescriptorSetSchema, TimestampSchema, file_google_protobuf_timestamp } from '@bufbuild/protobuf/wkt';
import { isReflectMessage } from '@bufbuild/protobuf/reflect';
import { BinaryReader } from '@bufbuild/protobuf/wire';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { celEnv, celError, celFunc, objectType, listType, CelScalar, plan, isCelError, isCelList } from '@bufbuild/cel';
import { Fault, fieldType, numericType, pointer, requireThat as ok, validateValue } from './validation.js';
import * as numeric from './numeric.js';

const pairs = {
  '_==_': ['equals', 2], '_!=_': ['not_equals', 2], '!_': ['logical_not', 1],
  '_&&_': ['logical_and', 2], '_||_': ['logical_or', 2], '_?_:_': ['conditional', 3],
  '_+_': ['add_string', 2], '@in': ['in_list', 2], '_<_': ['less_timestamp', 2],
  timestamp: ['string_to_timestamp', 1], duration: ['string_to_duration', 1],
};
const p = '/artifact/expressions';
const decimalName = 'keir.numeric.Decimal';
const numericArity = { decimal: 1, toDecimal: 1, toInteger: 1, size: 1, '-_': 1, '_+_': 2, '_-_': 2, '_*_': 2, '_/_': 2,
  '_<_': 2, '_<=_': 2, '_>_': 2, '_>=_': 2, round: 3, multiplyRounded: 4, divideRounded: 4 };
const numericOperators = { '_+_': 'add', '_-_': 'subtract', '_*_': 'multiply', '_/_': 'divide', '-_': 'negate',
  '_<_': 'less', '_<=_': 'less_equals', '_>_': 'greater', '_>=_': 'greater_equals' };
const roundingModes = new Set(['UNNECESSARY', 'DOWN', 'FLOOR', 'CEILING', 'HALF_UP', 'HALF_EVEN']);
function decimalValue(value) {
  if (value?.desc?.typeName !== decimalName || value.message?.$typeName !== decimalName || value.message.$unknown?.length)
    throw new TypeError('Invalid Decimal engine value.');
  return numeric.decimal(value.message.value);
}
const decimalMessage = (schema, value) => Object.freeze(create(schema, { value: numeric.text(value) }));
function numericFunctions(registry) {
  const schema = registry.getMessage(decimalName), D = objectType(schema), I = CelScalar.INT, S = CelScalar.STRING, B = CelScalar.BOOL;
  const functions = [];
  function add(name, args, result, operation) {
    functions.push(celFunc(name, args, result, (...values) => {
      try { return operation(...values); } catch (error) { throw celError(error); }
    }));
  }
  const result = value => decimalMessage(schema, value);
  add('decimal', [S], D, value => result(numeric.decimal(value)));
  add('toDecimal', [I], D, value => result(numeric.fromInteger(value)));
  add('toInteger', [D], I, value => numeric.toInteger(decimalValue(value)));
  add('_/_', [I, I], I, numeric.divideInteger);
  for (const [name, operation] of [['_+_', numeric.add], ['_-_', numeric.subtract], ['_*_', numeric.multiply], ['_/_', numeric.divide]])
    add(name, [D, D], D, (a, b) => result(operation(decimalValue(a), decimalValue(b))));
  add('-_', [D], D, a => result(numeric.negate(decimalValue(a))));
  for (const [name, compare] of [['_<_', n => n < 0], ['_<=_', n => n <= 0], ['_>_', n => n > 0], ['_>=_', n => n >= 0]])
    add(name, [D, D], B, (a, b) => compare(numeric.compare(decimalValue(a), decimalValue(b))));
  const scale = n => { if (n < 0n || n > 18n) throw new TypeError('Invalid rounding scale.'); return Number(n); };
  add('round', [D, I, S], D, (a, s, mode) => result(numeric.round(decimalValue(a), scale(s), mode)));
  add('multiplyRounded', [D, D, I, S], D, (a, b, s, mode) => result(numeric.multiplyRounded(decimalValue(a), decimalValue(b), scale(s), mode)));
  add('divideRounded', [D, D, I, S], D, (a, b, s, mode) => result(numeric.divideRounded(decimalValue(a), decimalValue(b), scale(s), mode)));
  return functions;
}
const timestamp = objectType(TimestampSchema);
const duration = objectType(DurationSchema);
const minimumSecond = -62135596800n;
const maximumSecond = 253402300799n;
const maxDuration = 31_536_000;
// CEL can merge an escaping Boolean error and drop its original cause. Artifact node IDs are positive.
const timestampOverflowId = -1n;
class TimestampOverflow extends Error {}
const checkedAddition = celFunc('_+_', [timestamp, duration], timestamp, (left, right) => {
  const { seconds, nanos } = left.message;
  const elapsed = right.message.seconds;
  const result = seconds + elapsed;
  if (result < minimumSecond || result > maximumSecond) throw celError(new TimestampOverflow(), timestampOverflowId);
  return create(TimestampSchema, { seconds: result, nanos });
});
function base64(value, max, path) {
  ok(typeof value === 'string' && value.length > 0 && value.length <= Math.ceil(max / 3) * 4 && /^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(value), path);
  const bytes = Buffer.from(value, 'base64');
  ok(bytes.length <= max && bytes.toString('base64') === value, path);
  return bytes;
}
const wireType = scalar => [9, 12].includes(scalar) ? 2 : [1, 6, 16].includes(scalar) ? 1 : [2, 7, 15].includes(scalar) ? 5 : 0;

// Bound protobuf nesting before the library materializes recursive messages.
function inspectWire(schema, bytes, depth = 1) {
  ok(depth <= 36, p, 'BUDGET_EXCEEDED');
  const reader = new BinaryReader(bytes);
  while (reader.pos < reader.len) {
    const [number, wire] = reader.tag();
    const field = schema.fields.find(f => f.number === number);
    ok(field && [0, 1, 2, 5].includes(wire), p);
    if (field.fieldKind === 'map') {
      ok(wire === 2, p);
      const entry = new BinaryReader(reader.bytes());
      const seen = new Set();
      while (entry.pos < entry.len) {
        const [key, kind] = entry.tag();
        ok([1, 2].includes(key) && !seen.has(key), p); seen.add(key);
        if (key === 2 && field.mapKind === 'message') { ok(kind === 2, p); inspectWire(field.message, entry.bytes(), depth + 2); }
        else { ok(kind === wireType(key === 1 ? field.mapKey : field.scalar), p); entry.skip(kind); }
      }
      ok(seen.size === 2, p);
    } else if (field.fieldKind === 'message' || field.fieldKind === 'list' && field.listKind === 'message') {
      ok(wire === 2, p); inspectWire(field.message, reader.bytes(), depth + 1);
    } else {
      const expected = field.fieldKind === 'enum' || field.listKind === 'enum' ? 0 : wireType(field.scalar);
      ok(wire === expected || field.fieldKind === 'list' && expected !== 2 && wire === 2, p);
      reader.skip(wire);
    }
  }
}
function decode(schema, bytes) {
  inspectWire(schema, bytes);
  const message = fromBinary(schema, bytes, { readUnknownFields: false });
  ok(Buffer.from(toBinary(schema, message)).equals(bytes), p);
  return message;
}

function descriptorSet(dcm) {
  const records = Object.keys(dcm.types).filter(k => dcm.types[k].kind === 'record').sort();
  let usesTimestamp = Object.values(dcm.capabilities ?? {}).some(capability =>
    Object.values(capability.parameters).some(parameter => parameter.type === 'Timestamp'));
  const messageType = records.map(name => {
    const fields = dcm.types[name].fields;
    const names = Object.keys(fields).sort(), used = new Set(names), oneofDecl = [];
    const field = names.map((name, index) => {
      const declaration = fields[name], type = fieldType(declaration);
      const list = dcm.types[type]?.kind === 'list' ? dcm.types[type] : undefined;
      const scalar = list ? list.element : type;
      const proto = { name, jsonName: name, number: index + 1, label: list ? 3 : 1,
        type: numericType(dcm, scalar) && scalar === 'Integer' ? 3 : scalar === 'Boolean' ? 8 : 9 };
      if (type === 'Timestamp') {
        usesTimestamp = true;
        proto.type = 11;
        proto.typeName = '.google.protobuf.Timestamp';
      } else if (numericType(dcm, type) && type === 'Decimal') {
        proto.type = 11; proto.typeName = '.' + decimalName;
      } else if (typeof declaration !== 'string') {
        let oneof = '_' + name;
        while (used.has(oneof)) oneof = '_' + oneof;
        used.add(oneof);
        proto.proto3Optional = true;
        proto.oneofIndex = oneofDecl.length;
        oneofDecl.push({ name: oneof });
      }
      return proto;
    });
    return { name, field, oneofDecl };
  });
  const dependency = file_google_protobuf_timestamp.proto;
  const numbers = dcm.format === 'keir-dcm/4';
  const decimal = { name: 'keir_numeric.proto', package: 'keir.numeric', syntax: 'proto3', messageType: [
    { name: 'Decimal', field: [{ name: 'value', jsonName: 'value', number: 1, label: 1, type: 9 }] },
  ] };
  return create(FileDescriptorSetSchema, { file: [
    ...(usesTimestamp ? [dependency] : []),
    ...(numbers ? [decimal] : []),
    { name: 'keir_read.proto', package: 'keir.read', syntax: 'proto3', messageType,
      dependency: [...(usesTimestamp ? [dependency.name] : []), ...(numbers ? [decimal.name] : [])] },
  ] });
}

export function prepareModelTypes(dcm, path = '/binding/dcm/types') {
  const descriptor = descriptorSet(dcm), bytes = Buffer.from(toBinary(FileDescriptorSetSchema, descriptor));
  ok(bytes.length <= 65536, path, 'BUDGET_EXCEEDED');
  return { registry: createFileRegistry(descriptor) };
}

export function prepareExpressions(contexts, { dcm, types: { registry } }) {
  const prepared = new Map();
  let totalNodes = 0;
  const numbers = dcm.format === 'keir-dcm/4';
  const functions = [checkedAddition, ...(numbers ? numericFunctions(registry) : [])];
  const scalar = type => type === 'Boolean' || type === 'String' || type === 'Timestamp' || numericType(dcm, type) || ['nominal', 'enum'].includes(dcm.types[type]?.kind);
  const list = type => dcm.types[type]?.kind === 'list';
  const literals = value => ({ type: 'String', literals: [value] });
  const assignable = (value, type) => value.type === type || value.literals && (type === 'String' || dcm.types[type]?.kind === 'enum' && value.literals.every(s => dcm.types[type].values.includes(s)));
  function equality(left, right) {
    ok(scalar(left.type) && scalar(right.type), p);
    if (left.type === right.type) return;
    ok(dcm.types[left.type]?.kind === 'enum' && right.literals && assignable(right, left.type) || dcm.types[right.type]?.kind === 'enum' && left.literals && assignable(left, right.type), p);
  }
  function joinBranches(left, right) {
    ok(scalar(left.type) && scalar(right.type), p);
    if (left.literals && right.literals) return { type: 'String', literals: [...left.literals, ...right.literals] };
    if (left.type === right.type) return { type: left.type };
    if (assignable(left, right.type)) return { type: right.type };
    if (assignable(right, left.type)) return { type: left.type };
    throw new Fault('ARTIFACT_INVALID', p);
  }
  function matchesType(checked, type) {
    const kind = checked?.typeKind;
    if (numericType(dcm, type)) return type === 'Integer' ? kind?.case === 'primitive' && kind.value === 2
      : kind?.case === 'messageType' && kind.value === decimalName;
    if (type === 'Timestamp' || type === 'Duration') return kind?.case === 'wellKnown' && kind.value === (type === 'Timestamp' ? 2 : 3);
    if (list(type)) return kind?.case === 'listType' && matchesType(kind.value.elemType, dcm.types[type].element);
    if (dcm.types[type]?.kind === 'record') return kind?.case === 'messageType' && kind.value === 'keir.read.' + type;
    return scalar(type) && kind?.case === 'primitive' && kind.value === (type === 'Boolean' ? 1 : 5);
  }
  function engineType(type) {
    if (numericType(dcm, type)) return type === 'Integer' ? CelScalar.INT : objectType(registry.getMessage(decimalName));
    if (type === 'Timestamp') return timestamp;
    if (list(type)) return listType(engineType(dcm.types[type].element));
    if (dcm.types[type]?.kind === 'record') return objectType(registry.getMessage('keir.read.' + type));
    return type === 'Boolean' ? CelScalar.BOOL : CelScalar.STRING;
  }
  for (const [id, context] of contexts) {
    const guardedResults = context.guardedResults ?? {};
    const checkedBytes = base64(context.checked, 16384, p);
    const checked = decode(CheckedExprSchema, checkedBytes);
    ok(checked.expr && checked.exprVersion === '' && checked.sourceInfo && checked.sourceInfo.syntaxVersion === '' && checked.sourceInfo.extensions.length === 0 && Object.keys(checked.sourceInfo.macroCalls).length === 0, p);
    ok(checked.sourceInfo.location === '<input>', p);
    // CEL-Java records the final line boundary one past the source length.
    ok(checked.sourceInfo.lineOffsets.every(n => Number.isSafeInteger(n) && n >= 0 && n <= 513) && Object.values(checked.sourceInfo.positions).every(n => Number.isSafeInteger(n) && n >= 0 && n <= 512), p);
    const ids = new Set(), references = new Set();
    function visit(expr, depth = 1, present = new Set(), guards = new Set(), selectionOperand = false) {
      ok(depth <= 16 && ids.size < 64, p, 'BUDGET_EXCEEDED');
      ok(expr && expr.id > 0n && expr.id <= BigInt(Number.MAX_SAFE_INTEGER) && !ids.has(String(expr.id)), p);
      const node = String(expr.id); ids.add(node);
      const kind = expr.exprKind;
      const value = kind.value;
      let inferred;
      if (kind.case === 'constExpr') {
        if (value.constantKind.case === 'boolValue') inferred = { type: 'Boolean' };
        else if (numbers && value.constantKind.case === 'int64Value') inferred = { type: 'Integer' };
        else {
          ok(value.constantKind.case === 'stringValue', p);
          validateValue(dcm, 'String', value.constantKind.value, p, 'ARTIFACT_INVALID');
          inferred = literals(value.constantKind.value);
        }
      } else if (kind.case === 'identExpr') {
        ok(Object.hasOwn(context.variables, value.name), p);
        const ref = checked.referenceMap[node];
        ok(ref && ref.name === value.name && ref.overloadId.length === 0 && ref.value === undefined, p); references.add(node);
        if (Object.hasOwn(guardedResults, value.name)) ok(selectionOperand && guards.has(guardedResults[value.name]), p);
        inferred = { type: context.variables[value.name] };
      } else if (kind.case === 'selectExpr') {
        const operand = visit(value.operand, depth + 1, present, guards, true);
        const record = dcm.types[operand.type];
        ok(record?.kind === 'record' && Object.hasOwn(record.fields, value.field), p);
        const field = record.fields[value.field];
        if (value.operand.exprKind.case === 'identExpr' && Object.hasOwn(guardedResults, value.operand.exprKind.value.name))
          ok(scalar(fieldType(field)), p);
        if (value.testOnly || typeof field !== 'string') {
          const variable = value.operand.exprKind;
          ok(variable.case === 'identExpr' && typeof field !== 'string', p);
          const key = variable.value.name + '.' + value.field;
          if (value.testOnly) inferred = { type: 'Boolean', presence: { key, positive: true } };
          else {
            ok(present.has(key), p);
            inferred = { type: fieldType(field) };
          }
        } else inferred = { type: field };
      } else if (kind.case === 'callExpr') {
        const operation = value.function;
        const pair = numbers && Object.hasOwn(numericArity, operation) ? [null, numericArity[operation]]
          : Object.hasOwn(pairs, operation) ? pairs[operation] : undefined;
        ok(pair && !value.target && value.args.length === pair[1], p);
        const first = visit(value.args[0], depth + 1, present, guards);
        const args = [first, ...value.args.slice(1).map((arg, index) => {
          const proof = first.presence;
          const guarded = proof && (operation === '_&&_' && proof.positive ||
            operation === '_?_:_' && proof.positive === (index === 0));
          const condition = value.args[0].exprKind;
          const guardId = condition.case === 'identExpr' ? condition.value.name : undefined;
          const exactGuard = operation === '_?_:_' && index === 0 && Object.values(guardedResults).includes(guardId);
          return visit(arg, depth + 1, guarded ? new Set([...present, proof.key]) : present,
            exactGuard ? new Set([...guards, guardId]) : guards);
        })];
        const timeAddition = operation === '_+_' && args[0].type === 'Timestamp' && args[1].type === 'Duration';
        const numericCall = numbers ? admitNumeric(operation, value.args, args) : undefined;
        const overload = numericCall?.overload ?? (timeAddition ? 'add_timestamp_duration' : pairs[operation]?.[0]);
        const ref = checked.referenceMap[node];
        ok(ref && ref.name === '' && ref.value === undefined && ref.overloadId.length === 1 && ref.overloadId[0] === overload, p);
        references.add(node);
        if (numericCall) inferred = { type: numericCall.type };
        else if (operation === '_+_') {
          ok(timeAddition || args.every(arg => arg.type === 'String'), p);
          inferred = { type: timeAddition ? 'Timestamp' : 'String' };
        } else if (operation === 'timestamp' || operation === 'duration') {
          const constant = value.args[0].exprKind;
          ok(constant.case === 'constExpr' && constant.value.constantKind.case === 'stringValue', p);
          const literal = constant.value.constantKind.value;
          if (operation === 'timestamp') {
            validateValue(dcm, 'Timestamp', literal, p, 'ARTIFACT_INVALID');
            inferred = { type: 'Timestamp' };
          } else {
            ok(/^(?:0|[1-9][0-9]{0,7})s$/.test(literal) && Number(literal.slice(0, -1)) <= maxDuration, p);
            inferred = { type: 'Duration' };
          }
        } else if (operation === '@in') {
          ok(list(args[1].type), p);
          const { element } = dcm.types[args[1].type];
          const directLiteral = value.args[0].exprKind.case === 'constExpr';
          ok(first.type === element || directLiteral && assignable(first, element), p);
          inferred = { type: 'Boolean' };
        } else if (operation === '_<_') {
          ok(args.every(arg => arg.type === 'Timestamp'), p);
          inferred = { type: 'Boolean' };
        } else if (operation === '_==_' || operation === '_!=_') {
          ok(operation === '_==_' || args.every(arg => arg.type !== 'Timestamp'), p);
          equality(args[0], args[1]);
          inferred = { type: 'Boolean' };
        } else if (operation === '_?_:_') {
          ok(first.type === 'Boolean', p);
          inferred = joinBranches(args[1], args[2]);
        } else {
          ok(args.every(arg => arg.type === 'Boolean'), p);
          inferred = { type: 'Boolean' };
          if (operation === '!_' && first.presence?.positive) inferred.presence = { key: first.presence.key, positive: false };
        }
      } else throw new Fault('ARTIFACT_INVALID', p);
      ok(matchesType(checked.typeMap[node], inferred.type), p);
      return inferred;
    }
    const result = visit(checked.expr);
    ok((scalar(context.resultType) || list(context.resultType)) && assignable(result, context.resultType), p);
    ok(Object.keys(checked.typeMap).length === ids.size && Object.keys(checked.typeMap).every(k => ids.has(k)), p);
    ok(Object.keys(checked.referenceMap).length === references.size && Object.keys(checked.referenceMap).every(k => references.has(k)), p);
    totalNodes += ids.size; ok(totalNodes <= 512, p, 'BUDGET_EXCEEDED');
    const variables = Object.fromEntries(Object.entries(context.variables).map(([name, type]) => [name, engineType(type)]));
    const evaluate = plan(celEnv({ registry, variables, funcs: functions }), checked);
    prepared.set(id, { evaluate, variables: context.variables, resultType: context.resultType,
      guardedResults: context.guardedResults });
  }
  return { expressions: prepared, registry, totalNodes };

  function admitNumeric(operation, expressions, args) {
    if (operation === 'decimal') {
      const c = expressions[0].exprKind;
      ok(c.case === 'constExpr' && c.value.constantKind.case === 'stringValue', p);
      numeric.decimal(c.value.constantKind.value);
      return { type: 'Decimal', overload: 'keir_decimal_from_string' };
    }
    if (operation === 'toDecimal' || operation === 'toInteger') {
      ok(args[0].type === (operation === 'toDecimal' ? 'Integer' : 'Decimal'), p);
      return { type: operation === 'toDecimal' ? 'Decimal' : 'Integer', overload: operation === 'toDecimal' ? 'keir_decimal_from_integer' : 'keir_integer_from_decimal' };
    }
    if (operation === 'size') {
      ok(list(args[0].type), p);
      return { type: 'Integer', overload: 'size_list' };
    }
    if (['round', 'multiplyRounded', 'divideRounded'].includes(operation)) {
      const index = args.length - 2, scale = expressions[index].exprKind, mode = expressions[index + 1].exprKind;
      ok(args.slice(0, index).every(a => a.type === 'Decimal') && scale.case === 'constExpr' && scale.value.constantKind.case === 'int64Value'
        && scale.value.constantKind.value >= 0n && scale.value.constantKind.value <= 18n && mode.case === 'constExpr'
        && mode.value.constantKind.case === 'stringValue' && roundingModes.has(mode.value.constantKind.value), p);
      return { type: 'Decimal', overload: { round: 'keir_decimal_round', multiplyRounded: 'keir_decimal_multiply_rounded', divideRounded: 'keir_decimal_divide_rounded' }[operation] };
    }
    if (!Object.hasOwn(numericOperators, operation) || !args.some(a => numericType(dcm, a.type))) return;
    const type = args[0].type, name = numericOperators[operation];
    ok(numericType(dcm, type) && args.every(a => a.type === type), p);
    return { type: ['less', 'less_equals', 'greater', 'greater_equals'].includes(name) ? 'Boolean' : type,
      overload: type === 'Integer' ? name + '_int64' : 'keir_decimal_' + name.replace('_equals', '_equal') };
  }
}

// Values and lazy projections belong to one invocation, never a prepared closure.
export function createActivation(dcm, prepared) {
  const slots = new Map(), counts = { projections: 0, boundaryParses: 0 };
  function project(type, canonical, path, numbers) {
    if (numericType(dcm, type)) return type === 'Integer' ? numbers.get(path)
      : decimalMessage(prepared.registry.getMessage(decimalName), numbers.get(path));
    if (type === 'Timestamp') return Object.freeze(fromJson(TimestampSchema, canonical));
    if (dcm.types[type]?.kind !== 'record') return canonical;
    const fields = dcm.types[type].fields;
    const values = Object.fromEntries(Object.entries(canonical).map(([name, value]) =>
      [name, project(fieldType(fields[name]), value, path + '/' + pointer(name), numbers)]));
    return Object.freeze(create(prepared.registry.getMessage('keir.read.' + type), values));
  }
  function accept(id, type, raw, path, code) {
    const numbers = new Map(), canonical = validateValue(dcm, type, raw, path, code, numbers);
    counts.boundaryParses += numbers.size;
    slots.set(id, { type, canonical, path, numbers });
    return canonical;
  }
  return { counts, accept,
    retain(id, type, canonical, engine) { slots.set(id, { type, canonical, engine }); },
    get(id, type, canonical, path) {
      if (!slots.has(id)) accept(id, type, canonical, path, 'EXPRESSION_FAILED');
      const slot = slots.get(id);
      ok(slot.type === type, path, 'EXPRESSION_FAILED');
      if (!Object.hasOwn(slot, 'engine')) { slot.engine = project(type, slot.canonical, slot.path, slot.numbers); counts.projections++; }
      return slot.engine;
    },
  };
}

export function evaluateBinding(binding, state, dcm, prepared, path, activation = createActivation(dcm, prepared), retainedId) {
  if (Object.hasOwn(binding, 'literal')) return retainedId === undefined ? binding.literal
    : activation.accept(retainedId, binding.type, binding.literal, path, 'EXPRESSION_FAILED');
  const expression = prepared.expressions.get(binding.expr);
  const variables = Object.create(null);
  for (const [name, type] of Object.entries(expression.variables)) {
    if (expression.guardedResults !== undefined) {
      if (Object.hasOwn(expression.guardedResults, name)) {
        const guard = state[expression.guardedResults[name]];
        ok(typeof guard === 'boolean', path, 'EXPRESSION_FAILED');
        if (!guard) continue;
      }
      ok(Object.hasOwn(state, name), path, 'EXPRESSION_FAILED');
    }
    variables[name] = activation.get(name, type, state[name], path);
  }
  let result;
  try { result = expression.evaluate(variables); }
  catch { throw new Fault('EXPRESSION_FAILED', path); }
  if (isCelError(result)) throw new Fault(result.exprId === timestampOverflowId ? 'BUDGET_EXCEEDED' : 'EXPRESSION_FAILED', path);
  const type = expression.resultType;
  if (numericType(dcm, type)) {
    let canonical, engine;
    try {
      if (type === 'Integer') {
        if (typeof result !== 'bigint') throw new TypeError();
        canonical = numeric.integer(result.toString()).toString(); engine = result;
      } else {
        canonical = numeric.text(decimalValue(result)); engine = Object.freeze(result.message);
      }
    } catch { throw new Fault('EXPRESSION_FAILED', path); }
    if (retainedId !== undefined) activation.retain(retainedId, type, canonical, engine);
    return canonical;
  }
  if (type === 'Timestamp') {
    ok(isReflectMessage(result, TimestampSchema), path, 'EXPRESSION_FAILED');
    const { seconds, nanos } = result.message;
    ok(seconds >= minimumSecond && seconds <= maximumSecond &&
      Number.isInteger(nanos) && nanos >= 0 && nanos < 1_000_000_000 && nanos % 1_000_000 === 0, path, 'EXPRESSION_FAILED');
    result = new Date(Number(seconds * 1000n + BigInt(nanos / 1_000_000))).toISOString();
  } else if (dcm.types[type]?.kind === 'list') {
    ok(isCelList(result), path, 'EXPRESSION_FAILED');
    ok(result.size <= dcm.types[type].maxItems, path, 'BUDGET_EXCEEDED');
    result = Array.from(result);
  }
  return retainedId === undefined ? validateValue(dcm, type, result, path, 'EXPRESSION_FAILED')
    : activation.accept(retainedId, type, result, path, 'EXPRESSION_FAILED');
}
