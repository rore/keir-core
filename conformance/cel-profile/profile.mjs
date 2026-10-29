// Qualification of trusted local Java output; this is not an artifact admission API.
import { createRequire } from 'node:module';
const require = createRequire(new URL('../../runtime-node/package.json', import.meta.url));
export const pb = require('@bufbuild/protobuf');
export const { FileDescriptorSetSchema } = require('@bufbuild/protobuf/wkt');
export const { CheckedExprSchema, Type_PrimitiveType } = require('@bufbuild/cel-spec/cel/expr/checked_pb.js');
export const cel = require('@bufbuild/cel');

export const limits = Object.freeze({ sourceBytes: 512, checkedBytes: 16384, descriptorBytes: 65536, nodes: 64, depth: 16, stringBytes: 256, inputBytes: 8192 });
const fields = ['enabled', 'id', 'projectId', 'status', 'title'];
const arities = { '_==_': 2, '_!=_': 2, '_&&_': 2, '_||_': 2, '!_': 1, '_?_:_': 3 };
const overloads = new Set(['equals', 'not_equals', 'logical_and', 'logical_or', 'logical_not', 'conditional']);
const classifications = new Set(['ACTIVE', 'COMPLETED']);
const fail = message => { throw new Error(message); };
const text = value => {
  if (typeof value !== 'string' || !value.isWellFormed() || Buffer.byteLength(value) > limits.stringBytes) fail('invalid bounded string');
  return value;
};

export function validateChecked(checked) {
  let nodes = 0;
  function visit(expr, depth = 1) {
    if (++nodes > limits.nodes || depth > limits.depth) fail('AST bound');
    const kind = expr?.exprKind;
    const v = kind?.value;
    switch (kind?.case) {
      case 'constExpr':
        if (v.constantKind.case === 'stringValue') text(v.constantKind.value);
        else if (v.constantKind.case !== 'boolValue') fail('constant kind');
        break;
      case 'identExpr':
        if (v.name !== 'task') fail('identifier');
        break;
      case 'selectExpr':
        if (v.testOnly || !fields.includes(v.field) || v.operand?.exprKind.case !== 'identExpr') fail('field selection');
        visit(v.operand, depth + 1);
        break;
      case 'callExpr': {
        if (!Object.hasOwn(arities, v.function) || v.target || v.args.length !== arities[v.function]) fail('call/arity');
        if (v.function === '_?_:_') {
          for (const branch of v.args.slice(1)) {
            const literal = branch.exprKind;
            if (literal.case !== 'constExpr' || literal.value.constantKind.case !== 'stringValue' || !classifications.has(literal.value.constantKind.value)) fail('literal classification');
          }
        }
        for (const arg of v.args) visit(arg, depth + 1);
        const reference = checked.referenceMap[expr.id.toString()];
        if (!reference?.overloadId.length || reference.overloadId.some(id => !overloads.has(id))) fail('checked overload');
        break;
      }
      default: fail('AST kind');
    }
    const type = checked.typeMap[expr.id.toString()]?.typeKind;
    if (!type || !(type.case === 'messageType' && type.value === 'keir.read.Task') && !(type.case === 'primitive' && [Type_PrimitiveType.BOOL, Type_PrimitiveType.STRING].includes(type.value))) fail('checked type');
  }
  if (!checked.expr) fail('missing expression');
  visit(checked.expr);
  if (checked.typeMap[checked.expr.id.toString()]?.typeKind.case !== 'primitive') fail('root type');
  return nodes;
}

export function prepare(bundle) {
  const bytes = Buffer.from(bundle.descriptorBase64, 'base64');
  if (bytes.length > limits.descriptorBytes) fail('descriptor bytes');
  const registry = pb.createFileRegistry(pb.fromBinary(FileDescriptorSetSchema, bytes));
  const descriptor = registry.getMessage('keir.read.Task');
  if (!descriptor) fail('task descriptor');
  const env = cel.celEnv({ registry, variables: { task: cel.objectType(descriptor) } });
  const expressions = new Map();
  for (const entry of bundle.expressions) {
    if (Buffer.byteLength(entry.source) > limits.sourceBytes) fail('source bytes');
    const checkedBytes = Buffer.from(entry.checkedBase64, 'base64');
    if (checkedBytes.length > limits.checkedBytes) fail('checked bytes');
    const checked = pb.fromBinary(CheckedExprSchema, checkedBytes);
    const nodes = validateChecked(checked);
    expressions.set(entry.id, { eval: cel.plan(env, checked), checked, nodes });
  }
  return { descriptor, env, expressions };
}

export function input(task, descriptor) {
  if (!task || Object.getPrototypeOf(task) !== Object.prototype || Object.keys(task).length !== 5 || !['id', 'projectId', 'status', 'title', 'enabled'].every(k => Object.hasOwn(task, k))) fail('required fields');
  if (Buffer.byteLength(JSON.stringify(task)) > limits.inputBytes) fail('input bytes');
  for (const field of ['id', 'projectId', 'status', 'title']) text(task[field]);
  if (!task.id || !task.projectId || !['OPEN', 'DONE'].includes(task.status) || typeof task.enabled !== 'boolean') fail('domain value');
  return { task: pb.create(descriptor, task) };
}

export function evaluate(prepared, id, vars) {
  const value = prepared.expressions.get(id).eval(vars);
  if (cel.isCelError(value)) return { error: true, message: value.message };
  if (typeof value !== 'boolean' && typeof value !== 'string') fail('result type');
  return value;
}
