import { create, createFileRegistry, fromBinary, fromJson, toBinary } from '@bufbuild/protobuf';
import { FileDescriptorSetSchema } from '@bufbuild/protobuf/wkt';
import { BinaryReader } from '@bufbuild/protobuf/wire';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { celEnv, objectType, CelScalar, plan, isCelError } from '@bufbuild/cel';
import { Fault, requireThat as ok, validateValue } from './validation.js';

const pairs = { '_==_': ['equals', 2], '_!=_': ['not_equals', 2], '!_': ['logical_not', 1], '_&&_': ['logical_and', 2], '_||_': ['logical_or', 2], '_?_:_': ['conditional', 3] };
const p = '/artifact/expressions';
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

export function prepareExpressions(artifact, contexts) {
  const dcm = artifact.dcm;
  const records = Object.keys(dcm.types).filter(k => dcm.types[k].kind === 'record').sort();
  const descriptorBytes = base64(artifact.descriptors, 65536, '/artifact/descriptors');
  const expected = create(FileDescriptorSetSchema, { file: [{ name: 'keir_read.proto', package: 'keir.read', syntax: 'proto3', messageType: records.map(name => ({ name, field: Object.keys(dcm.types[name].fields).sort().map((field, i) => ({ name: field, jsonName: field, number: i + 1, label: 1, type: dcm.types[name].fields[field] === 'Boolean' ? 8 : 9 })) })) }] });
  const descriptor = decode(FileDescriptorSetSchema, descriptorBytes);
  ok(Buffer.from(toBinary(FileDescriptorSetSchema, expected)).equals(descriptorBytes), '/artifact/descriptors');
  const registry = createFileRegistry(descriptor);
  const prepared = new Map();
  let totalNodes = 0;
  const scalar = type => type === 'Boolean' || type === 'String' || ['nominal', 'enum'].includes(dcm.types[type]?.kind);
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
  for (const [id, context] of contexts) {
    const checkedBytes = base64(context.checked, 16384, p);
    const checked = decode(CheckedExprSchema, checkedBytes);
    ok(checked.expr && checked.exprVersion === '' && checked.sourceInfo && checked.sourceInfo.syntaxVersion === '' && checked.sourceInfo.extensions.length === 0 && Object.keys(checked.sourceInfo.macroCalls).length === 0, p);
    ok(['<input>', 'keir-read/1'].includes(checked.sourceInfo.location), p);
    // CEL-Java records the final line boundary one past the source length.
    ok(checked.sourceInfo.lineOffsets.every(n => Number.isSafeInteger(n) && n >= 0 && n <= 513) && Object.values(checked.sourceInfo.positions).every(n => Number.isSafeInteger(n) && n >= 0 && n <= 512), p);
    const ids = new Set(), references = new Set();
    function visit(expr, depth = 1) {
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
        inferred = { type: context.variables[value.name] };
      } else if (kind.case === 'selectExpr') {
        ok(!value.testOnly, p);
        const operand = visit(value.operand, depth + 1);
        const record = dcm.types[operand.type];
        ok(record?.kind === 'record' && Object.hasOwn(record.fields, value.field), p);
        inferred = { type: record.fields[value.field] };
      } else if (kind.case === 'callExpr') {
        const pair = pairs[value.function];
        ok(Object.hasOwn(pairs, value.function) && !value.target && value.args.length === pair[1], p);
        const ref = checked.referenceMap[node];
        ok(ref && ref.name === '' && ref.value === undefined && ref.overloadId.length === 1 && ref.overloadId[0] === pair[0], p); references.add(node);
        const args = value.args.map(arg => visit(arg, depth + 1));
        if (['_==_', '_!=_'].includes(value.function)) { equality(args[0], args[1]); inferred = { type: 'Boolean' }; }
        else if (value.function === '_?_:_') { ok(args[0].type === 'Boolean', p); inferred = joinBranches(args[1], args[2]); }
        else { ok(args.every(arg => arg.type === 'Boolean'), p); inferred = { type: 'Boolean' }; }
      } else throw new Fault('ARTIFACT_INVALID', p);
      const type = checked.typeMap[node]?.typeKind;
      const record = dcm.types[inferred.type]?.kind === 'record';
      ok(type && (record ? type.case === 'messageType' && type.value === 'keir.read.' + inferred.type : type.case === 'primitive' && type.value === (inferred.type === 'Boolean' ? 1 : 5)), p);
      return inferred;
    }
    const result = visit(checked.expr);
    ok(scalar(context.resultType) && assignable(result, context.resultType), p);
    ok(Object.keys(checked.typeMap).length === ids.size && Object.keys(checked.typeMap).every(k => ids.has(k)), p);
    ok(Object.keys(checked.referenceMap).length === references.size && Object.keys(checked.referenceMap).every(k => references.has(k)), p);
    totalNodes += ids.size; ok(totalNodes <= 512, p, 'BUDGET_EXCEEDED');
    const variables = Object.fromEntries(Object.entries(context.variables).map(([name, type]) => [name, dcm.types[type]?.kind === 'record' ? objectType(registry.getMessage('keir.read.' + type)) : type === 'Boolean' ? CelScalar.BOOL : CelScalar.STRING]));
    const evaluate = plan(celEnv({ registry, variables }), checked);
    prepared.set(id, { evaluate, variables: context.variables, resultType: context.resultType });
  }
  return { expressions: prepared, registry, totalNodes };
}

export function evaluateBinding(binding, state, dcm, prepared, path) {
  if (Object.hasOwn(binding, 'literal')) return binding.literal;
  const expression = prepared.expressions.get(binding.expr);
  const variables = Object.fromEntries(Object.entries(expression.variables).map(([name, type]) => [name, dcm.types[type]?.kind === 'record' ? fromJson(prepared.registry.getMessage('keir.read.' + type), state[name]) : state[name]]));
  let result;
  try { result = expression.evaluate(variables); }
  catch { throw new Fault('EXPRESSION_FAILED', path); }
  if (isCelError(result)) throw new Fault('EXPRESSION_FAILED', path);
  return validateValue(dcm, expression.resultType, result, path, 'EXPRESSION_FAILED');
}
