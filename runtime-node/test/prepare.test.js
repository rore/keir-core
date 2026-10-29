import test from 'node:test';
import assert from 'node:assert/strict';
import { fromBinary, fromJson, toBinary } from '@bufbuild/protobuf';
import { FileDescriptorSetSchema } from '@bufbuild/protobuf/wkt';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { prepare } from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { admission, adapters, artifact, artifactBytes, altered } from './helpers.js';

function reject(bytes, policy = admission({}, bytes), registrations = adapters()) {
  let calls = 0;
  const registry = structuredClone({});
  for (const [id, registration] of Object.entries(registrations)) registry[id] = { ...registration, invoke: () => { calls++; return registration.invoke(); } };
  const result = prepare(bytes, policy, registry);
  assert.equal(typeof result.code, 'string');
  assert.match(result.path, /^\/(artifact|admission)(\/|$)/);
  assert.equal(calls, 0);
  assert.deepEqual(Object.keys(result).sort(), ['code', 'message', 'path']);
  return result;
}
function selected(mutate) {
  const value = structuredClone(artifact); mutate(value);
  value.dcmDigest = digest(canonicalBytes(value.dcm));
  const bytes = canonicalBytes(value);
  return { bytes, policy: admission({ expectedDcmDigest: value.dcmDigest }, bytes) };
}
function mutateChecked(value, id, mutate) {
  const checked = fromBinary(CheckedExprSchema, Buffer.from(value.expressions[id].checked, 'base64'));
  mutate(checked);
  value.expressions[id].checked = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');
}

test('prepares actual Java artifact without dispatch and exposes an immutable opaque handle', () => {
  let calls = 0;
  const handle = prepare(artifactBytes, admission(), adapters(() => { calls++; }));
  assert.equal(handle.code, undefined);
  assert.equal(Object.isFrozen(handle), true);
  assert.deepEqual(Reflect.ownKeys(handle), []);
  assert.equal(calls, 0);
});

test('rejects record-valued conditional branches even beneath a scalar field selection', () => {
  const checked = fromJson(CheckedExprSchema, {
    expr: { id: '5', selectExpr: {
      operand: { id: '4', callExpr: { function: '_?_:_', args: [
        { id: '1', constExpr: { boolValue: true } },
        { id: '2', identExpr: { name: 'input' } },
        { id: '3', identExpr: { name: 'input' } },
      ] } }, field: 'taskId',
    } },
    sourceInfo: { location: 'keir-read/1' },
    typeMap: {
      1: { primitive: 'BOOL' }, 2: { messageType: 'keir.read.ClassifyInput' },
      3: { messageType: 'keir.read.ClassifyInput' }, 4: { messageType: 'keir.read.ClassifyInput' },
      5: { primitive: 'STRING' },
    },
    referenceMap: { 2: { name: 'input' }, 3: { name: 'input' }, 4: { overloadId: ['conditional'] } },
  });
  // Re-pin the complete altered fixture so rejection reaches the checked-profile gate.
  const item = selected(value => { value.expressions.e0.checked = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64'); });
  const result = reject(item.bytes, item.policy);
  assert.equal(result.code, 'ARTIFACT_INVALID');
  assert.equal(result.path, '/artifact/expressions');
});

test('external digest and independent model/consumer/entrypoint/compiler/environment selectors are required', () => {
  for (const overrides of [
    { expectedArtifactDigest: 'sha256:' + '0'.repeat(64) },
    { expectedDcmDigest: 'sha256:' + '0'.repeat(64) },
    { entrypoint: 'OtherEntrypoint' }, { compiler: 'other-compiler' },
    { consumer: { id: 'other.consumer', version: '1', kind: 'DISPLAY' } },
    { environment: { ...artifact.environment, celNode: '^0.6.1' } }
  ]) assert.equal(reject(artifactBytes, admission(overrides)).code, 'PIN_MISMATCH');
  const value = altered(a => { a.expectedArtifactDigest = digest(artifactBytes); });
  reject(value.bytes, value.admission);
});

test('raw size is bounded before parsing and noncanonical/duplicate/malformed UTF-8 data is rejected', () => {
  reject(Buffer.alloc(1048577));
  const original = artifactBytes.toString();
  for (const bytes of [
    Buffer.from(original + '\n'), Buffer.from(' ' + original),
    Buffer.from(original.replace('{"compiler":', '{"compiler":"ignored","compiler":')),
    Buffer.concat([Buffer.from([0xef, 0xbb, 0xbf]), artifactBytes]),
    Buffer.from([0xc3, 0x28]), Buffer.from('['.repeat(17) + '0' + ']'.repeat(17)),
    Buffer.from(original.replace('"tasks.get":1', '"tasks.get":1.0'))
  ]) reject(bytes);
});

test('unknown instructions, fields, capabilities and protected-argument overrides never dispatch', () => {
  for (const mutate of [
    a => { a.steps[0].op = 'WRITE'; },
    a => { a.steps[0].unknown = true; },
    a => { a.steps[0].capability = 'tasks.unknown'; },
    a => { a.steps[0].version = 'other'; },
    a => { a.steps[0].args.actorId = { literal: 'untrusted', type: 'UserId' }; },
    a => { a.return.type = 'Task'; },
    a => { a.return.fields.unknown = a.return.fields.classification; },
    a => { a.expressions.e0.variables.future = 'String'; },
    a => { a.expressions.e0.resultType = 'UserId'; },
    a => { a.steps[1].id = 'input'; },
    a => { a.steps[1].id = 'Boolean'; },
    a => { a.steps[0].args.taskId = { literal: 'raw-nominal', type: 'TaskId' }; }
  ]) { const item = selected(mutate); reject(item.bytes, item.policy); }
});

test('whole DCM rejects unsupported writes/types/records/grants and closed-schema violations', () => {
  for (const mutate of [
    a => { a.dcm.capabilities['tasks.get'].effect = 'WRITE'; },
    a => { a.dcm.types.Unused = { kind: 'list', items: 'String' }; },
    a => { a.dcm.types.int = { kind: 'nominal', base: 'String' }; },
    a => { a.dcm.types.Task.fields.nested = 'ClassifyInput'; },
    a => { a.dcm.types.TaskStatus.values.push('OPEN'); },
    a => { a.dcm.types.TaskStatus.values = Array.from({ length: 17 }, (_, i) => 'V' + i); },
    a => { a.dcm.types.Task.extra = true; },
    a => { a.dcm.capabilities['tasks.get'].parameters.actorId.binding = 'source'; },
    a => { a.dcm.entrypoints.ClassifyTask.grants['tasks.get'] = 5; },
    a => { a.dcm.entrypoints.ClassifyTask.trigger = 'EVENT'; },
    a => { a.dcm.entrypoints.ClassifyTask.consumer.kind = 'WRITE'; },
    a => { for (let i = 0; i < 17; i++) a.dcm.types.Task.fields['field' + i] = 'String'; },
    a => { for (let i = 0; i < 17; i++) a.dcm.types['Extra' + i] = { kind: 'nominal', base: 'String' }; }
  ]) { const item = selected(mutate); reject(item.bytes, item.policy); }
});

test('descriptor and checked protobuf metadata are validated even for host-selected malformed bytes', () => {
  for (const mutate of [
    a => { a.descriptors = Buffer.alloc(65537).toString('base64'); },
    a => { a.descriptors = '!!!!'; },
    a => {
      const descriptor = fromBinary(FileDescriptorSetSchema, Buffer.from(a.descriptors, 'base64'));
      descriptor.file[0].messageType[0].field[0].jsonName = 'different';
      a.descriptors = Buffer.from(toBinary(FileDescriptorSetSchema, descriptor)).toString('base64');
    },
    a => { a.expressions.e0.checked = Buffer.alloc(16385).toString('base64'); },
    a => { a.expressions.e0.checked = Buffer.from([255]).toString('base64'); },
    a => { mutateChecked(a, 'e0', c => { c.typeMap = {}; }); },
    a => { mutateChecked(a, 'e0', c => { c.referenceMap = {}; }); },
    a => { mutateChecked(a, 'e1', c => { c.referenceMap[c.expr.id].overloadId = ['logical_and']; }); },
    a => { mutateChecked(a, 'e0', c => { c.expr.exprKind.value.testOnly = true; }); },
    a => { mutateChecked(a, 'e0', c => { c.expr.exprKind.value.field = 'actorId'; }); },
    a => { mutateChecked(a, 'e0', c => { c.sourceInfo.location = 'unrecognized-source'; }); },
    a => { mutateChecked(a, 'e2', c => {
      const [condition, branch] = c.expr.exprKind.value.args;
      branch.exprKind = structuredClone(condition.exprKind);
      c.typeMap[String(branch.id)] = structuredClone(c.typeMap[String(condition.id)]);
      c.referenceMap[String(branch.id)] = structuredClone(c.referenceMap[String(condition.id)]);
    }); }
  ]) { const item = selected(mutate); reject(item.bytes, item.policy); }
});

test('plan quotas and host restrictions are narrowing-only', () => {
  for (const mutate of [
    a => { a.steps = Array.from({ length: 9 }, () => a.steps[0]); },
    a => { a.steps[1].value.expr = 'e2'; },
    a => { a.expressions.e3 = a.expressions.e0; }
  ]) { const item = selected(mutate); reject(item.bytes, item.policy); }
  for (const limits of [{ steps: 1 }, { totalAstNodes: 9 }, { calls: 5 }, { traceEvents: 65 }, { traceEvents: 1 }, { unknown: 1 }]) reject(artifactBytes, admission({ limits }));
  reject(artifactBytes, admission({ grants: {} }));
  reject(artifactBytes, admission({ grants: { 'tasks.get': 2 } }));
  reject(artifactBytes, admission(), { 'tasks.get': { version: '1', mode: 'real', invoke() {} } });
  reject(artifactBytes, admission(), { 'tasks.get': { version: '2', mode: 'simulation', invoke() {} } });
  reject(artifactBytes, admission(), {});
  reject(artifactBytes, admission({ grants: { 'tasks.get': 0 } }));
  reject(artifactBytes, admission(), { ...adapters(), 'tasks.unknown': { version: '1', mode: 'simulation', invoke() {} } });
});
