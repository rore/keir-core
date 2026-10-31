import test from 'node:test';
import assert from 'node:assert/strict';
import { narrowPolicy, Fault } from '../src/validation.js';

const dcm = { capabilities: Object.fromEntries(['tasks.get', 'tasks.getSummary', 'tasks.create'].map(id => [id, {}])) };
const all = { 'tasks.get': 4, 'tasks.getSummary': 4, 'tasks.create': 4 };
const denied = { 'tasks.get': 4, 'tasks.getSummary': 4, 'tasks.create': 0 };
const narrow = (policy, previous = all, catalog = dcm) => narrowPolicy(catalog, policy, previous, '/policy');
function invalid(policy, previous = all, catalog = dcm) {
  assert.throws(() => narrow(policy, previous, catalog), error => error instanceof Fault && error.code === 'ARTIFACT_INVALID' && error.path.startsWith('/policy'));
}

test('omitted and empty policies inherit all previous ceilings', () => {
  assert.deepEqual(narrow(undefined), all);
  assert.deepEqual(narrow({}), all);
  assert.deepEqual(narrow({ maxCalls: {} }), all);
});

test('allow, deny and quotas only narrow the preceding map', () => {
  assert.deepEqual(narrow({ allow: ['tasks.get'] }), { 'tasks.get': 4, 'tasks.getSummary': 0, 'tasks.create': 0 });
  assert.deepEqual(narrow({ allow: [] }), { 'tasks.get': 0, 'tasks.getSummary': 0, 'tasks.create': 0 });
  assert.deepEqual(narrow({ allow: ['tasks.get'], deny: ['tasks.get'] }), { 'tasks.get': 0, 'tasks.getSummary': 0, 'tasks.create': 0 });
  assert.deepEqual(narrow({ deny: ['tasks.getSummary'] }), { 'tasks.get': 4, 'tasks.getSummary': 0, 'tasks.create': 4 });
  assert.deepEqual(narrow({ deny: ['tasks.getSummary'] }, { ...all, 'tasks.getSummary': 0 }), { ...all, 'tasks.getSummary': 0 });
  assert.deepEqual(narrow({ maxCalls: { 'tasks.get': 2 } }), { 'tasks.get': 2, 'tasks.getSummary': 4, 'tasks.create': 4 });
  for (const n of [0, 1, 2, 3, 4]) assert.equal(narrow({ maxCalls: { 'tasks.get': n } })['tasks.get'], n);
});

test('later omission preserves earlier denial and quota; allow cannot restore either', () => {
  const prior = narrow({ allow: ['tasks.get', 'tasks.create'], deny: ['tasks.create'], maxCalls: { 'tasks.get': 2 } });
  assert.deepEqual(narrow(undefined, prior), { 'tasks.get': 2, 'tasks.getSummary': 0, 'tasks.create': 0 });
  assert.deepEqual(narrow({}, prior), { 'tasks.get': 2, 'tasks.getSummary': 0, 'tasks.create': 0 });
  assert.deepEqual(narrow({ allow: ['tasks.get'] }, prior), { 'tasks.get': 2, 'tasks.getSummary': 0, 'tasks.create': 0 });
  assert.deepEqual(narrow({ deny: ['tasks.create'] }, prior), { 'tasks.get': 2, 'tasks.getSummary': 0, 'tasks.create': 0 });
  invalid({ allow: ['tasks.create'] }, prior);
});

test('quotas cannot widen ceilings or grant positive quota to denied capabilities', () => {
  const prior = { ...all, 'tasks.get': 2, 'tasks.create': 0 };
  assert.deepEqual(narrow({ maxCalls: { 'tasks.get': 1, 'tasks.create': 0 } }, prior), { ...prior, 'tasks.get': 1 });
  invalid({ maxCalls: { 'tasks.get': 3 } }, prior);
  assert.deepEqual(narrow({ allow: ['tasks.get'], maxCalls: { 'tasks.get': 1 } }, prior), { 'tasks.get': 1, 'tasks.getSummary': 0, 'tasks.create': 0 });
  invalid({ maxCalls: { 'tasks.create': 1 } }, prior);
  invalid({ maxCalls: { 'tasks.get': 5 } });
});

test('a preceding denial prevents allowing or widening that capability', () => {
  assert.deepEqual(narrow(undefined, denied), denied);
  invalid({ allow: ['tasks.get', 'tasks.create'] }, denied);
  invalid({ maxCalls: { 'tasks.create': 1 } }, denied);
  invalid({ allow: ['tasks.create'] }, denied);
});

test('rejects malformed policies, unknown and duplicate IDs, and catalog overflow', () => {
  for (const policy of [null, [], { typo: true }, { allow: null }, { allow: 'tasks.get' }, { deny: null }, { deny: ['tasks.get', 'tasks.get'] }, { allow: ['unknown.capability'] }, { maxCalls: null }, { maxCalls: { unknown: 1 } }, { maxCalls: { 'tasks.get': null } }, { maxCalls: { 'tasks.get': 1.5 } }, { maxCalls: { 'tasks.get': -1 } }]) invalid(policy);
  const eight = { capabilities: Object.fromEntries(Array.from({ length: 9 }, (_, i) => [`cap.${i}`, {}])) };
  invalid({}, Object.fromEntries(Object.keys(eight.capabilities).map(id => [id, 4])), eight);
  invalid({ allow: Object.keys(eight.capabilities) }, Object.fromEntries(Object.keys(eight.capabilities).map(id => [id, 4])), eight);
  invalid({ maxCalls: Object.fromEntries(Object.keys(eight.capabilities).map(id => [id, 1])) }, Object.fromEntries(Object.keys(eight.capabilities).map(id => [id, 4])), eight);
});

test('returns a new map without mutating policy or previous ceilings', () => {
  const previous = { ...all };
  const policy = { allow: ['tasks.get'], maxCalls: { 'tasks.get': 1 } };
  const result = narrow(policy, previous);
  assert.deepEqual(previous, all);
  assert.deepEqual(policy, { allow: ['tasks.get'], maxCalls: { 'tasks.get': 1 } });
  assert.notEqual(result, previous);
  result['tasks.get'] = 0;
  assert.equal(previous['tasks.get'], 4);
});

test('quotas remain separate from aggregate call budgets', () => {
  assert.equal(narrow({ maxCalls: { 'tasks.get': 4 } })['tasks.get'], 4);
  // The helper computes only per-capability ceilings; aggregate admission limits are enforced by the caller.
});
