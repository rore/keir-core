import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { create, fromBinary, toBinary } from '@bufbuild/protobuf';
import { CheckedExprSchema, ReferenceSchema } from '@bufbuild/cel-spec/cel/expr/checked_pb.js';
import { ExprSchema } from '@bufbuild/cel-spec/cel/expr/syntax_pb.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { bindModel, invokeProgram, prepareProgram } from '../src/index.js';

const read = path => readFileSync(new URL('../../conformance/keir-2/' + path, import.meta.url));
function pinned(name) {
  const bytes = read(name + '.json');
  assert.equal(digest(bytes), read(name + '.sha256').toString('ascii').trim());
  return bytes;
}

function fixture(response) {
  const dcm = pinned('dcm-inventory'), artifact = JSON.parse(pinned('artifact-inventory-algebra'));
  const row = JSON.parse(read('scenarios/inventory-algebra/inventory-confirmed-then-overflow.json'));
  const calls = [];
  const { capabilities, hostKey } = JSON.parse(dcm);
  const implementations = Object.fromEntries(Object.entries(capabilities).map(([id, capability]) => [id, {
    mode: 'simulation', keyType: hostKey.type, version: capability.version,
    invoke(args) { calls.push(id); return response(args); },
  }]));
  const bound = bindModel(dcm, implementations);
  assert.equal(bound.diagnostic, null);
  const expression = artifact.expressions[artifact.steps.at(-1).value.expr];
  const prepare = edit => {
    const checked = fromBinary(CheckedExprSchema, Buffer.from(expression.checked, 'base64'));
    edit?.(checked);
    const candidate = structuredClone(artifact);
    candidate.expressions[candidate.steps.at(-1).value.expr].checked =
      Buffer.from(toBinary(CheckedExprSchema, checked)).toString('base64');
    return prepareProgram(canonicalBytes(candidate), bound.boundModel);
  };
  const invoke = async edit => {
    const admission = prepare(edit);
    assert.equal(admission.diagnostic, null);
    calls.length = 0;
    return invokeProgram(admission.prepared, row.given.input, {
      bindings: { actorId: 'user1' }, requestKey: {}, event: { id: 'event1', source: 'item1' },
      deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 1000 },
    });
  };
  return { calls, prepare, invoke };
}

// Retain the compiler-emitted comparison and change only the enclosing Boolean context.
function comparison(checked) {
  const conditional = checked.expr;
  const [predicate, ...branches] = conditional.exprKind.value.args;
  for (const { id } of [conditional, ...branches]) {
    delete checked.typeMap[id];
    delete checked.referenceMap[id];
    delete checked.sourceInfo.positions[id];
  }
  checked.expr = predicate;
}

function wrap(checked, operation, value, constantFirst) {
  const id = BigInt(Math.max(...Object.keys(checked.typeMap).map(Number))) + 1n;
  const constant = create(ExprSchema, { id, exprKind: { case: 'constExpr',
    value: { constantKind: { case: 'boolValue', value } } } });
  const predicate = checked.expr;
  checked.expr = create(ExprSchema, { id: id + 1n, exprKind: { case: 'callExpr',
    value: { function: operation, args: constantFirst ? [constant, predicate] : [predicate, constant] } } });
  checked.typeMap[id] = checked.typeMap[predicate.id];
  checked.typeMap[checked.expr.id] = checked.typeMap[predicate.id];
  checked.referenceMap[checked.expr.id] = create(ReferenceSchema, {
    overloadId: [operation === '_&&_' ? 'logical_and' : 'logical_or'],
  });
}

const confirmed = args => ({ kind: 'SUCCESS', value: { itemId: args.itemId }, effect: 'CONFIRMED' });

test('only escaping Timestamp overflow reaches the budget diagnostic after a confirmed write', async () => {
  const f = fixture(confirmed);
  async function check(edit, masked) {
    const outcome = await f.invoke(edit);
    assert.deepEqual(f.calls, ['inventory.adjust']);
    assert.deepEqual(outcome.effects, [{ step: 'adjusted', capability: 'inventory.adjust', state: 'CONFIRMED' }]);
    if (masked === undefined) {
      assert.equal(outcome.classification, 'LIMIT_EXCEEDED');
      assert.deepEqual(outcome.error, { code: 'BUDGET_EXCEEDED', path: '/steps/6' });
    } else {
      assert.equal(outcome.classification, 'COMPLETED');
      assert.deepEqual({ ...outcome.result }, { eligible: masked });
    }
  }
  await check(); // The original compiler-emitted conditional.
  await check(comparison);
  for (const constantFirst of [false, true]) {
    for (const [operation, neutral] of [['_&&_', true], ['_||_', false]]) {
      await check(checked => { comparison(checked); wrap(checked, operation, neutral, constantFirst); });
      await check(checked => {
        comparison(checked);
        wrap(checked, operation, neutral, constantFirst);
        wrap(checked, operation, neutral, !constantFirst);
      });
      await check(checked => { comparison(checked); wrap(checked, operation, !neutral, constantFirst); }, !neutral);
    }
  }
});

test('artifact-supplied overflow IDs are rejected and masked overflow cannot reclassify an adapter error', async () => {
  const f = fixture(() => { throw new Error('unrelated adapter failure'); });
  const { prepared, diagnostic } = f.prepare(checked => {
    const { id } = checked.expr;
    checked.typeMap['-1'] = checked.typeMap[id];
    checked.referenceMap['-1'] = checked.referenceMap[id];
    delete checked.typeMap[id];
    delete checked.referenceMap[id];
    checked.expr.id = -1n;
  });
  assert.equal(prepared, null);
  assert.equal(diagnostic.code, 'ARTIFACT_INVALID');
  assert.deepEqual(f.calls, []);
  const outcome = await f.invoke();
  assert.deepEqual(f.calls, ['inventory.adjust']);
  assert.equal(outcome.classification, 'UNCERTAIN');
  assert.deepEqual(outcome.error, { code: 'ADAPTER_EXCEPTION', path: '/steps/5' });
});
