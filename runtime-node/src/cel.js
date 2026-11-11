import { create, createFileRegistry, fromBinary, fromJson, toBinary } from '@bufbuild/protobuf';
import { DurationSchema, FileDescriptorSetSchema, TimestampSchema, file_google_protobuf_timestamp } from '@bufbuild/protobuf/wkt';
import { isReflectMessage } from '@bufbuild/protobuf/reflect';
import { BinaryReader } from '@bufbuild/protobuf/wire';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { celEnv, celError, celFunc, objectType, listType, CelScalar, plan, isCelError, isCelList } from '@bufbuild/cel';
import { Fault, fieldType, requireThat as ok, validateValue } from './validation.js';

const pairs = {
  '_==_': ['equals', 2], '_!=_': ['not_equals', 2], '!_': ['logical_not', 1],
  '_&&_': ['logical_and', 2], '_||_': ['logical_or', 2], '_?_:_': ['conditional', 3],
  '_+_': ['add_string', 2], '@in': ['in_list', 2], '_<_': ['less_timestamp', 2],
  timestamp: ['string_to_timestamp', 1], duration: ['string_to_duration', 1],
};
const p = '/artifact/expressions';
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
      const proto = { name, jsonName: name, number: index + 1, label: list ? 3 : 1, type: scalar === 'Boolean' ? 8 : 9 };
      if (type === 'Timestamp') {
        usesTimestamp = true;
        proto.type = 11;
        proto.typeName = '.google.protobuf.Timestamp';
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
  return create(FileDescriptorSetSchema, { file: [
    ...(usesTimestamp ? [dependency] : []),
    { name: 'keir_read.proto', package: 'keir.read', syntax: 'proto3', messageType,
      dependency: usesTimestamp ? [dependency.name] : [] },
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
  const scalar = type => type === 'Boolean' || type === 'String' || type === 'Timestamp' || ['nominal', 'enum'].includes(dcm.types[type]?.kind);
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
    if (type === 'Timestamp' || type === 'Duration') return kind?.case === 'wellKnown' && kind.value === (type === 'Timestamp' ? 2 : 3);
    if (list(type)) return kind?.case === 'listType' && matchesType(kind.value.elemType, dcm.types[type].element);
    if (dcm.types[type]?.kind === 'record') return kind?.case === 'messageType' && kind.value === 'keir.read.' + type;
    return scalar(type) && kind?.case === 'primitive' && kind.value === (type === 'Boolean' ? 1 : 5);
  }
  function engineType(type) {
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
        const pair = Object.hasOwn(pairs, operation) ? pairs[operation] : undefined;
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
        const overload = timeAddition ? 'add_timestamp_duration' : pair[0];
        const ref = checked.referenceMap[node];
        ok(ref && ref.name === '' && ref.value === undefined && ref.overloadId.length === 1 && ref.overloadId[0] === overload, p);
        references.add(node);
        if (operation === '_+_') {
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
    const evaluate = plan(celEnv({ registry, variables, funcs: [checkedAddition] }), checked);
    prepared.set(id, { evaluate, variables: context.variables, resultType: context.resultType,
      guardedResults: context.guardedResults });
  }
  return { expressions: prepared, registry, totalNodes };
}

export function evaluateBinding(binding, state, dcm, prepared, path) {
  if (Object.hasOwn(binding, 'literal')) return binding.literal;
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
      validateValue(dcm, type, state[name], path, 'EXPRESSION_FAILED');
    }
    const schema = type === 'Timestamp' ? TimestampSchema : dcm.types[type]?.kind === 'record'
      ? prepared.registry.getMessage('keir.read.' + type) : undefined;
    variables[name] = schema ? fromJson(schema, state[name]) : state[name];
  }
  let result;
  try { result = expression.evaluate(variables); }
  catch { throw new Fault('EXPRESSION_FAILED', path); }
  if (isCelError(result)) throw new Fault(result.exprId === timestampOverflowId ? 'BUDGET_EXCEEDED' : 'EXPRESSION_FAILED', path);
  const type = expression.resultType;
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
  return validateValue(dcm, expression.resultType, result, path, 'EXPRESSION_FAILED');
}
