import test from 'node:test';
import assert from 'node:assert/strict';
import { consumeEffect } from './effect-consumer.mjs';
import { typed } from './generic-simulation.mjs';

const trace = code => [{ sequence: 0, event: 'INVOCATION_START' }, { sequence: 1, event: 'TERMINATED', ...(code ? { code } : {}) }];
const base = { classification: 'COMPLETED', result: { status: 'CREATED' }, effects: [], trace: trace() };
const err = { code: 'BUSINESS_RULE', path: '/steps/0' };
const receipt = state => [{ step: 'write', capability: 'tasks.create', state }];
const confirmedReadTrace = () => [
  { event: 'INVOCATION_START' }, { event: 'STEP_START', step: 'write' },
  { event: 'EFFECT_INTENT', step: 'write', capability: 'tasks.create' },
  { event: 'CALL_DISPATCHED', step: 'write', capability: 'tasks.create' },
  { event: 'EFFECT_RECORDED', step: 'write', capability: 'tasks.create', state: 'CONFIRMED' },
  { event: 'CALL_SUCCEEDED', step: 'write', capability: 'tasks.create' },
  { event: 'STEP_START', step: 'read' }, { event: 'CALL_DISPATCHED', step: 'read', capability: 'tasks.get' },
  { event: 'TERMINATED', code: 'ACCESS_DENIED' },
].map((event, sequence) => ({ sequence, ...event }));
const consume = outcome => {
  const value = { ...base, ...outcome };
  for (const key of ['result', 'error']) if (outcome[key] === undefined) delete value[key];
  if (outcome.trace === undefined) value.trace = trace(outcome.error?.code);
  return consumeEffect(value);
};
const invalid = outcome => assert.deepEqual(consume(outcome), { status: 'NOT_REPORTED', code: 'OUTCOME_INVALID' });

test('selected-model reports preserve the default result restriction and receipt checks', () => {
  const model = { format: 'keir-dcm/4', types: {
    Status: { kind: 'enum', values: ['REQUESTED', 'NO_OP'] },
    Report: { kind: 'record', fields: { status: 'Status', ordered: 'Integer' } },
  } };
  const validResult = value => { try { typed(value, 'Report', model); return true; } catch { return false; } };
  const value = { ...base, result: { status: 'REQUESTED', ordered: '3' } };
  const rejected = { status: 'NOT_REPORTED', code: 'OUTCOME_INVALID' };
  assert.deepEqual(consumeEffect(value), rejected);
  assert.equal(consumeEffect(value, validResult).status, 'REPORTED');
  assert.deepEqual(consumeEffect(value, () => { throw new Error('Invalid declared result'); }), rejected);
  for (const ordered of ['03', 3, 3n, '9223372036854775808'])
    assert.deepEqual(consumeEffect({ ...value, result: { status: 'REQUESTED', ordered } }, validResult), rejected);
  assert.deepEqual(consumeEffect({ ...value, effects: receipt('NONE') }, validResult), rejected);
});

test('reports completed created and no-op results without deriving effects', () => {
  assert.equal(consumeEffect(base).result.status, 'CREATED');
  assert.deepEqual(consume({ result: { status: 'NO_OP' } }).effects, []);
});

test('keeps handled rejection visibly a rejection and accepts unhandled rejection', () => {
  assert.deepEqual(consume({ classification: 'BUSINESS_REJECTION', result: { status: 'SOURCE_MISSING' }, error: err, effects: receipt('NONE') }), {
    status: 'REPORTED', classification: 'BUSINESS_REJECTION', result: { status: 'SOURCE_MISSING' }, error: err, effects: receipt('NONE')
  });
  const unhandled = consume({ classification: 'BUSINESS_REJECTION', error: err, effects: [] });
  assert.equal(unhandled.status, 'REPORTED');
  assert.equal(Object.hasOwn(unhandled, 'result'), false);
});

test('a business handler may return any declared output status while remaining a rejection', () => {
  for (const status of ['CREATED', 'NO_OP', 'SOURCE_MISSING', 'REJECTED']) {
    assert.deepEqual(consume({ classification: 'BUSINESS_REJECTION', result: { status }, error: err, effects: receipt('NONE') }), {
      status: 'REPORTED', classification: 'BUSINESS_REJECTION', result: { status }, error: err, effects: receipt('NONE'),
    });
  }
});

test('a later read rejection retains an already confirmed write', () => {
  const error = { code: 'ACCESS_DENIED', path: '/steps/1' };
  for (const result of [undefined, { status: 'NO_OP' }]) {
    const events = confirmedReadTrace();
    if (result) events.splice(-1, 0, { event: 'RETURNED' });
    const trace = events.map((event, sequence) => ({ ...event, sequence }));
    assert.deepEqual(consume({ classification: 'BUSINESS_REJECTION', error, effects: receipt('CONFIRMED'), trace, result }), {
      status: 'REPORTED', classification: 'BUSINESS_REJECTION', error, effects: receipt('CONFIRMED'), ...(result ? { result } : {}),
    });
  }
  invalid({ classification: 'BUSINESS_REJECTION', error, effects: receipt('UNKNOWN') });
});

test('confirmed business rejection needs an earlier confirmed effect and a distinct later rejected dispatch', () => {
  const error = { code: 'ACCESS_DENIED', path: '/steps/1' };
  const outcome = { classification: 'BUSINESS_REJECTION', error, effects: receipt('CONFIRMED'), trace: confirmedReadTrace() };
  invalid({ ...outcome, trace: trace(error.code) });
  invalid({ ...outcome, error: { ...error, path: '/steps/0' } });
  for (const mutate of [
    events => { events[4].state = 'NONE'; },
    events => { events[7].step = 'write'; },
    events => { events[7].capability = 'tasks.create'; },
    events => { events[6].step = 'different'; },
    events => { const [confirmed] = events.splice(4, 1); events.splice(-1, 0, confirmed); },
  ]) {
    const events = confirmedReadTrace(); mutate(events);
    invalid({ ...outcome, trace: events.map((event, sequence) => ({ ...event, sequence })) });
  }
});

test('preserves confirmed effects beside failures and only unknown effects beside uncertainty', () => {
  assert.deepEqual(consume({ classification: 'FAILED', error: err, effects: receipt('CONFIRMED') }).effects, receipt('CONFIRMED'));
  assert.deepEqual(consume({ classification: 'LIMIT_EXCEEDED', error: err, effects: receipt('NONE') }).classification, 'LIMIT_EXCEEDED');
  assert.deepEqual(consume({ classification: 'UNCERTAIN', error: err, effects: receipt('UNKNOWN') }).classification, 'UNCERTAIN');
  invalid({ classification: 'UNCERTAIN', error: err, effects: [] });
});

test('rejects malformed, extra, contradictory and recursive cause shapes', () => {
  for (const value of [null, false, [], {}, { ...base, trace: [] }]) assert.deepEqual(consumeEffect(value), { status: 'NOT_REPORTED', code: 'OUTCOME_INVALID' });
  invalid({ result: { status: 'CREATED', effect: 'CONFIRMED' } });
  invalid({ unexpected: true });
  invalid({ classification: 'COMPLETED', error: err });
  invalid({ classification: 'FAILED', error: err, effects: receipt('UNKNOWN') });
  invalid({ classification: 'UNCERTAIN', error: err, effects: receipt('CONFIRMED') });
  invalid({ classification: 'BUSINESS_REJECTION', error: err, result: { status: 'UNDECLARED' } });
  invalid({ classification: 'FAILED', error: { ...err, cause: { ...err, cause: err } } });
  invalid({ classification: 'BUSINESS_REJECTION', error: { ...err, cause: err } });
  invalid({ trace: [{ sequence: 0, event: 'EFFECT_RECORDED', step: 'x', capability: 'y' }] });
  invalid({ trace: [{ sequence: 0, event: 'INVOCATION_START', type: 'INVOCATION_START' }] });
  invalid({ trace: [{ sequence: 1, event: 'INVOCATION_START' }, { sequence: 2, event: 'TERMINATED' }] });
  invalid({ trace: [{ sequence: 0, event: 'INVOCATION_START', code: 'X' }] });
  invalid({ trace: [trace()[0], { sequence: 1, event: 'CALL_DISPATCHED' }, { sequence: 2, event: 'TERMINATED' }] });
  invalid({ trace: [trace()[0], { sequence: 1, event: 'constructor' }, { sequence: 2, event: 'TERMINATED' }] });
  invalid({ classification: 'FAILED', error: err, trace: trace('DIFFERENT') });
});

test('trace event names must be strings, never property-coercible arrays', () => {
  invalid({ trace: [trace()[0], { sequence: 1, event: ['RETURNED'] }, { sequence: 2, event: 'TERMINATED' }] });
  invalid({ trace: [trace()[0], { sequence: 1, event: ['STEP_START'], step: 'source' }, { sequence: 2, event: 'TERMINATED' }] });
});

test('an original business cause belongs only to its matching handler subtree', () => {
  const cause = { code: 'CREATE_REJECTED', path: '/steps/2' };
  invalid({ classification: 'FAILED', error: { code: 'EXPRESSION_FAILED', path: '/input', cause } });
  invalid({ classification: 'FAILED', error: { code: 'EXPRESSION_FAILED', path: '/steps/2/onError/ACCESS_DENIED', cause } });
  const failure = { code: 'BUDGET_EXCEEDED', path: '/steps/2/onError/CREATE_REJECTED', cause };
  assert.equal(consume({ classification: 'LIMIT_EXCEEDED', error: failure, effects: receipt('NONE') }).status, 'REPORTED');
  assert.equal(consume({ classification: 'FAILED', error: { ...failure, path: failure.path + '/status' }, effects: receipt('NONE') }).status, 'REPORTED');
});
