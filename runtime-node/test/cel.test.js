import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fromBinary, toBinary } from '@bufbuild/protobuf';
import { CheckedExprSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { evaluateBinding, prepareExpressions, prepareModelTypes } from '../src/cel.js';

const here = new URL('../../conformance/cel-profile/', import.meta.url);
const bundle = JSON.parse(readFileSync(new URL('evidence/bundle.json', here), 'utf8'));
const fixtures = JSON.parse(readFileSync(new URL('fixtures.json', here), 'utf8'));
const dcm = { types: { Task: { kind: 'record', fields: { enabled: 'Boolean', id: 'String', projectId: 'String', status: 'String', title: 'String' } } } };
const contexts = new Map(bundle.expressions.map(({ id, checkedBase64, javaOutputType }) => [id, {
  checked: checkedBase64,
  variables: { task: 'Task' },
  resultType: javaOutputType === 'bool' ? 'Boolean' : 'String',
}]));
const qualifiedIds = new Set(fixtures.expressions.map(expression => expression.id));
const qualifiedContexts = new Map([...contexts].filter(([id]) => qualifiedIds.has(id)));
const model = { dcm, types: prepareModelTypes(dcm) };
const tasks = Object.fromEntries(fixtures.inputs.map(input => [input.name, input.task]));

function prepare(selected = contexts) {
  return prepareExpressions(selected, model);
}

test('accepts Java qualification checked expressions and matches their fixture results', () => {
  const prepared = prepare(qualifiedContexts);
  for (const expression of fixtures.expressions.filter(({ id }) => qualifiedIds.has(id))) {
    const context = contexts.get(expression.id);
    for (let i = 0; i < expression.expected.length; i++) {
      const input = fixtures.inputs[i];
      assert.deepEqual(evaluateBinding({ expr: expression.id, type: context.resultType }, { task: input.task }, dcm, prepared, '/test'), expression.expected[i], expression.id);
    }
  }
  assert.equal(prepared.expressions.get('status').resultType, 'Boolean');
});

test('accepts declared variables that are available, including an unused scalar', () => {
  const selected = new Map([['status', { ...contexts.get('status'), variables: { task: 'Task', unused: 'Boolean' } }]]);
  const prepared = prepare(selected);
  assert.equal(evaluateBinding({ expr: 'status', type: 'Boolean' }, { task: tasks.open, unused: true }, dcm, prepared, '/test'), false);
});

test('rejects forged overload metadata even when the function itself is allowed', () => {
  const context = contexts.get('boolean');
  const checked = fromBinary(CheckedExprSchema, Buffer.from(context.checked, 'base64'));
  const callId = Object.keys(checked.referenceMap).find(key => checked.referenceMap[key].overloadId.includes('logical_and'));
  assert.ok(callId);
  checked.referenceMap[callId].overloadId = ['logical_or'];
  assert.throws(() => prepare(new Map([['forged', { ...context, checked: Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64') }]])));
});

test('rejects checked maps with missing or extra type/reference entries', () => {
  const context = contexts.get('status');
  for (const mutate of [
    checked => { delete checked.typeMap[Object.keys(checked.typeMap)[0]]; },
    checked => { checked.typeMap['999'] = checked.typeMap[Object.keys(checked.typeMap)[0]]; },
    checked => { delete checked.referenceMap[Object.keys(checked.referenceMap)[0]]; },
    checked => { checked.referenceMap['999'] = checked.referenceMap[Object.keys(checked.referenceMap)[0]]; },
  ]) {
    const checked = fromBinary(CheckedExprSchema, Buffer.from(context.checked, 'base64'));
    mutate(checked);
    const encoded = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');
    assert.throws(() => prepare(new Map([['mutated', { ...context, checked: encoded }]])));
  }
});

test('rejects unknown variables and malformed checked bytes', () => {
  assert.throws(() => prepare(new Map([['unknown', { ...contexts.get('status'), variables: { other: 'Task' } }]])));
  assert.throws(() => prepare(new Map([['bad', { ...contexts.get('status'), checked: '!!!!' }]])));
  assert.throws(() => prepare(new Map([['bad', { ...contexts.get('status'), checked: Buffer.from([0xff]).toString('base64') }]])));
});

test('requires the current checked expression source location', () => {
  const context = contexts.get('status');
  for (const location of ['keir-read/1', 'untrusted']) {
    const checked = fromBinary(CheckedExprSchema, Buffer.from(context.checked, 'base64'));
    checked.sourceInfo.location = location;
    const encoded = Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');
    assert.throws(() => prepare(new Map([['status', { ...context, checked: encoded }]])));
  }
});

test('rejects checked-expression size above the limit and accepts qualified AST boundaries', () => {
  assert.throws(() => prepare(new Map([['large', { ...contexts.get('status'), checked: Buffer.alloc(16385).toString('base64') }]])));
  assert.ok(prepare(new Map([['nodes', contexts.get('max_nodes')]])).expressions.has('nodes'));
  assert.ok(prepare(new Map([['depth', contexts.get('max_depth')]])).expressions.has('depth'));
});

test('nominal wire equality is only a scalar bridge check, not a Keir overlay claim', () => {
  const prepared = prepare(new Map([['nominal_erasure', contexts.get('nominal_erasure')]]));
  assert.equal(evaluateBinding({ expr: 'nominal_erasure', type: 'Boolean' }, { task: tasks.open }, dcm, prepared, '/test'), true);
});
