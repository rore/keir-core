import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { create, fromBinary, fromJson, isFieldSet, toBinary } from '@bufbuild/protobuf';
import { FileDescriptorProtoSchema } from '@bufbuild/protobuf/wkt';
import { CheckedExprSchema, ReferenceSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { ExprSchema } from '@bufbuild/cel-spec/cel/expr/syntax_pb.js';
import { canonicalBytes } from '../src/canonical.js';
import { prepareModelTypes } from '../src/cel.js';
import { bindModel, prepareProgram } from '../src/index.js';
import { validateValue } from '../src/validation.js';

const dcm = { types: {
  Labels: { kind: 'list', element: 'String', maxItems: 16 },
  Inventory: { kind: 'record', fields: {
    note: { type: 'String', optional: true },
    occurredAt: 'Timestamp',
    labels: 'Labels',
    expiresAt: { type: 'Timestamp', optional: true },
    enabled: { type: 'Boolean', optional: true },
  } },
} };
const base = { labels: [], occurredAt: '2026-03-07T12:34:56.000Z' };
const validate = input => validateValue(dcm, 'Inventory', input, '/input', 'INPUT_INVALID');

test('validates canonical timestamps, optional omission and list bounds before protobuf conversion', () => {
  assert.deepEqual(validate(base), base);
  const exact = { ...base, enabled: false, note: '', expiresAt: '0001-01-01T00:00:00.000Z', labels: Array(16).fill('é'.repeat(128)) };
  assert.deepEqual(validate(exact), exact);
  for (const occurredAt of [
    '0000-01-01T00:00:00.000Z', '2026-02-30T00:00:00.000Z', '2026-01-01T00:00:60.000Z',
    '2026-01-01T00:00:00.000+01:00', '2026-01-01T00:00:00Z', '2026-01-01T00:00:00.000001Z',
  ]) assert.throws(() => validate({ ...base, occurredAt }), { code: 'INPUT_INVALID', path: '/input' });
  for (const field of ['enabled', 'note', 'expiresAt']) {
    assert.throws(() => validate({ ...base, [field]: null }), { code: 'INPUT_INVALID', path: '/input' });
  }
  assert.throws(() => validate({ ...base, labels: Array(17).fill('x') }), { code: 'BUDGET_EXCEEDED', path: '/input/labels' });
  assert.throws(() => validate({ ...base, labels: ['é'.repeat(128) + 'x'] }), { code: 'BUDGET_EXCEEDED', path: '/input/labels/0' });
});

test('enum membership rejects a conditional string needle with two valid enum literals', () => {
  const read = name => readFileSync(new URL('../../conformance/keir-2/' + name + '.json', import.meta.url));
  const bytes = read('dcm-inventory-enum');
  const { capabilities, hostKey } = JSON.parse(bytes);
  const implementations = Object.fromEntries(Object.entries(capabilities).map(([id, capability]) => [id, {
    mode: 'simulation', keyType: hostKey.type, version: capability.version,
    invoke() { assert.fail('Admission dispatched an adapter'); },
  }]));
  const bound = bindModel(bytes, implementations);
  assert.equal(bound.diagnostic, null);
  const artifactBytes = read('artifact-inventory-enum');
  assert.equal(prepareProgram(artifactBytes, bound.boundModel).diagnostic, null);

  const artifact = JSON.parse(artifactBytes);
  const expression = artifact.expressions[artifact.steps[0].value.expr];
  const checked = fromBinary(CheckedExprSchema, Buffer.from(expression.checked, 'base64'));
  let member = checked.expr;
  while (member.exprKind.value.function !== '@in') member = member.exprKind.value.args[0];
  const literal = member.exprKind.value.args[0];
  const id = BigInt(Math.max(...Object.keys(checked.typeMap).map(Number))) + 1n;
  const condition = create(ExprSchema, { id, exprKind: { case: 'constExpr',
    value: { constantKind: { case: 'boolValue', value: true } } } });
  const alternate = create(ExprSchema, { id: id + 1n, exprKind: { case: 'constExpr',
    value: { constantKind: { case: 'stringValue', value: 'HIGH' } } } });
  const needle = create(ExprSchema, { id: id + 2n, exprKind: { case: 'callExpr',
    value: { function: '_?_:_', args: [condition, literal, alternate] } } });
  member.exprKind.value.args[0] = needle;
  checked.typeMap[condition.id] = checked.typeMap[member.id];
  checked.typeMap[alternate.id] = checked.typeMap[literal.id];
  checked.typeMap[needle.id] = checked.typeMap[literal.id];
  checked.referenceMap[needle.id] = create(ReferenceSchema, { overloadId: ['conditional'] });
  expression.checked = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');

  const denied = prepareProgram(canonicalBytes(artifact), bound.boundModel);
  assert.equal(denied.prepared, null);
  assert.equal(denied.diagnostic.code, 'ARTIFACT_INVALID');
});

test('descriptor sorts fields, retains exact JSON names and preserves false and empty presence', () => {
  const { registry } = prepareModelTypes(dcm);
  const inventory = registry.getMessage('keir.read.Inventory');
  const { field, oneofDecl } = inventory.proto;
  assert.deepEqual(field.map(f => [f.name, f.jsonName, f.number]), [
    ['enabled', 'enabled', 1], ['expiresAt', 'expiresAt', 2], ['labels', 'labels', 3],
    ['note', 'note', 4], ['occurredAt', 'occurredAt', 5],
  ]);
  assert.deepEqual(oneofDecl.map(oneof => oneof.name), ['_enabled', '_note']);
  assert.equal(field[0].proto3Optional, true);
  assert.equal(field[1].proto3Optional, false);
  assert.equal(field[1].typeName, '.google.protobuf.Timestamp');
  assert.equal(field[2].label, 3);
  assert.equal(field[3].proto3Optional, true);
  const timestampProto = registry.getFile('google/protobuf/timestamp.proto').proto;
  const bytes = toBinary(FileDescriptorProtoSchema, timestampProto);
  assert.equal(bytes.length, 255);
  assert.equal(createHash('sha256').update(bytes).digest('hex'), '422a163a2d7051465e0a516584b4d96d18c270ec4e7ad3c21ad33e5c440ebecc');
  const parameterOnly = prepareModelTypes({
    types: {}, capabilities: { update: { parameters: { dueAt: { type: 'Timestamp' } } } },
  });
  assert.deepEqual(parameterOnly.registry.getFile('keir_read.proto').proto.dependency, ['google/protobuf/timestamp.proto']);

  const absent = fromJson(inventory, validate(base));
  const present = fromJson(inventory, validate({ ...base, enabled: false, note: '', expiresAt: base.occurredAt }));
  const roundTrip = fromBinary(inventory, toBinary(inventory, present));
  for (const name of ['enabled', 'note', 'expiresAt']) {
    assert.equal(isFieldSet(absent, inventory.field[name]), false, name);
    assert.equal(isFieldSet(roundTrip, inventory.field[name]), true, name);
  }
});
