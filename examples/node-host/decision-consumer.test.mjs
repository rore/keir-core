import test from 'node:test';
import assert from 'node:assert/strict';
import { consumeDecision, consumeQuote } from './decision-consumer.mjs';

const sequence = events => events.map((event, sequence) => ({ sequence, ...event }));
const completed = (decision = 'ALLOW') => ({
  classification: 'COMPLETED', result: { decision }, effects: [],
  trace: sequence([
    { event: 'INVOCATION_START' }, { event: 'STEP_START', step: 'restricted' },
    { event: 'VALUE_BOUND', step: 'restricted' }, { event: 'RETURNED' }, { event: 'TERMINATED' },
  ]),
});
const read = (taken = true) => sequence([
  { event: 'INVOCATION_START' }, { event: 'STEP_START', step: 'read' },
  { event: 'GUARD_EVALUATED', step: 'read', taken },
  ...(taken ? [
    { event: 'CALL_DISPATCHED', step: 'read', capability: 'operation.check' },
    { event: 'CALL_SUCCEEDED', step: 'read', capability: 'operation.check' },
  ] : [{ event: 'STEP_SKIPPED', step: 'read' }]),
  { event: 'RETURNED' }, { event: 'TERMINATED' },
]);
const blocked = value => assert.deepEqual(consumeDecision(value), { status: 'BLOCKED' });

test('quote consumer checks every output field and canonical amount before consuming a decision', () => {
  for (const decision of ['ALLOW', 'DENY']) {
    const outcome = { ...completed(decision), result: { decision, message: '', amount: '92.5' } };
    const consumed = consumeQuote(outcome);
    assert.deepEqual(consumed, { status: decision === 'ALLOW' ? 'ALLOWED' : 'DENIED', result: outcome.result });
    outcome.result.amount = '0';
    assert.equal(consumed.result.amount, '92.5');
    assert.deepEqual(consumeDecision(outcome), { status: 'BLOCKED' });
  }
  for (const amount of [undefined, null, 92.5, 1n, '92.50', '+1', '-0', '1e2', '0.0000000000000000001', '1'.repeat(39)]) {
    const outcome = { ...completed(), result: { decision: 'ALLOW', message: '', amount } };
    assert.deepEqual(consumeQuote(outcome), { status: 'BLOCKED' });
  }
  for (const field of ['decision', 'message', 'amount']) {
    const outcome = { ...completed(), result: { decision: 'ALLOW', message: '', amount: '1' } };
    delete outcome.result[field];
    assert.deepEqual(consumeQuote(outcome), { status: 'BLOCKED' });
  }
  const outcome = { ...completed(), result: { decision: 'ALLOW', message: '', amount: '1' } };
  outcome.effects = [{ state: 'CONFIRMED' }];
  assert.deepEqual(consumeQuote(outcome), { status: 'BLOCKED' });
  let accessed = false;
  Object.defineProperty(outcome.result, 'amount', { get() { accessed = true; return '1'; } });
  assert.deepEqual(consumeQuote(outcome), { status: 'BLOCKED' });
  assert.equal(accessed, false);
});

test('allows and denies completed pure, read-derived and skipped-read decisions', () => {
  for (const decision of ['ALLOW', 'DENY']) {
    for (const trace of [completed().trace, read(), read(false)]) {
      assert.deepEqual(consumeDecision({ ...completed(decision), trace }), {
        status: decision === 'ALLOW' ? 'ALLOWED' : 'DENIED', result: { decision },
      });
    }
    const unguarded = read().filter(event => event.event !== 'GUARD_EVALUATED');
    assert.equal(consumeDecision({ ...completed(decision), trace: sequence(unguarded.map(({ sequence, ...event }) => event)) }).result.decision, decision);
  }
});

test('preserves the complete bounded message as independent plain data', () => {
  for (const message of ['', '<script>deny()</script>\n"plain text"', 'é'.repeat(128), '😀'.repeat(64)]) {
    const outcome = completed('DENY');
    outcome.result.message = message;
    const consumed = consumeDecision(outcome);
    assert.deepEqual(consumed, { status: 'DENIED', result: { decision: 'DENY', message } });
    assert.notEqual(consumed.result, outcome.result);
    assert.equal(Object.getPrototypeOf(consumed.result), Object.prototype);
    outcome.result.message = 'changed';
    assert.equal(consumed.result.message, message);
  }
  const outcome = completed();
  Object.setPrototypeOf(outcome.result, null);
  Object.freeze(outcome.result);
  Object.freeze(outcome.trace);
  assert.deepEqual(consumeDecision(outcome), { status: 'ALLOWED', result: { decision: 'ALLOW' } });
});

test('blocks every non-completed outcome, including handled READ rejection returning ALLOW', () => {
  for (const classification of ['BUSINESS_REJECTION', 'FAILED', 'LIMIT_EXCEEDED', 'UNCERTAIN', 'UNKNOWN', null]) {
    const outcome = { ...completed(), classification };
    blocked(outcome);
    outcome.error = { code: 'ACCESS_DENIED', path: '/steps/0' };
    outcome.trace = sequence(read().filter(event => event.event !== 'CALL_SUCCEEDED').map(({ sequence, ...event }) => event));
    outcome.trace.at(-1).code = 'ACCESS_DENIED';
    blocked(outcome);
  }
});

test('blocks missing, unknown and mistyped outcome/result fields and malformed strings', () => {
  for (const value of [undefined, null, false, [], {}, () => {}, new Date(), new Map()]) blocked(value);
  for (const field of ['classification', 'result', 'effects', 'trace']) {
    const outcome = completed(); delete outcome[field]; blocked(outcome);
  }
  for (const result of [null, [], {}, 'ALLOW', { decision: 'allow' }, { decision: 'UNKNOWN' },
    { decision: null }, { decision: ['ALLOW'] }, { decision: 'ALLOW', extra: true }]) {
    blocked({ ...completed(), result });
  }
  for (const message of [null, false, 0, {}, [], undefined, 'a'.repeat(257), 'é'.repeat(129), '\uD800', '\uDC00']) {
    blocked({ ...completed(), result: { decision: 'ALLOW', message } });
  }
  blocked({ ...completed(), extra: true });
  blocked({ ...completed(), error: { code: 'FAILED', path: '' } });
});

test('blocks every effect and every effect event even beside a valid decision', () => {
  for (const effects of [null, {}, [null], [{ step: 'read', capability: 'operation.check', state: 'CONFIRMED' }],
    [{ step: 'read', capability: 'operation.check', state: 'NONE' }], [{ state: 'UNKNOWN' }]]) {
    blocked({ ...completed(), effects });
  }
  for (const event of ['EFFECT_INTENT', 'EFFECT_RECORDED']) {
    const trace = read();
    trace.splice(3, 0, { event, step: 'read', capability: 'operation.check', state: 'NONE' });
    blocked({ ...completed(), trace: sequence(trace.map(({ sequence, ...item }) => item)) });
  }
});

test('blocks forged trace fields, sequence, ordering, step and capability relationships', () => {
  const mutations = [
    trace => { trace[0].sequence = -0; },
    trace => { trace[1].sequence = 9; },
    trace => { trace[0].event = 'RETURNED'; },
    trace => { trace[1].event = ['STEP_START']; },
    trace => { trace[1].extra = true; },
    trace => { trace[1].step = ''; },
    trace => { trace[1].step = '\uD800'; },
    trace => { trace[2].taken = 'true'; },
    trace => { trace[2].taken = false; },
    trace => { trace[2].step = 'other'; },
    trace => { trace[3].capability = ''; },
    trace => { trace[3].capability = 'é'.repeat(129); },
    trace => { trace[4].capability = 'other'; },
    trace => { trace[4].step = 'other'; },
    trace => { trace[4].event = 'RETURNED'; },
    trace => { trace.at(-1).code = 'ACCESS_DENIED'; },
    trace => { trace.at(-1).event = 'constructor'; },
    trace => { trace.splice(4, 1); },
    trace => { trace.splice(-2, 1); },
    trace => { trace.push({ event: 'TERMINATED' }); },
  ];
  for (const mutate of mutations) {
    const trace = read(); mutate(trace);
    blocked({ ...completed(), trace });
  }
  blocked({ ...completed(), trace: [] });
  blocked({ ...completed(), trace: sequence([{ event: 'INVOCATION_START' }, { event: 'RETURNED' }, { event: 'TERMINATED' }]) });
  blocked({ ...completed(), trace: read(false).map(event => event.event === 'STEP_SKIPPED' ? { ...event, step: 'other' } : event) });
  for (const step of ['with.dot', 's'.repeat(65)]) {
    blocked({ ...completed(), trace: read().map(event => event.step ? { ...event, step } : event) });
  }
  for (const capability of ['with-hyphen', 'c'.repeat(101)]) {
    blocked({ ...completed(), trace: read().map(event => event.capability ? { ...event, capability } : event) });
  }
});

test('enforces bounded steps, calls and trace length with no repeated reached step', () => {
  const many = (count, calls = false) => sequence([
    { event: 'INVOCATION_START' },
    ...Array.from({ length: count }, (_, index) => [
      { event: 'STEP_START', step: `step${index}` },
      ...(calls ? [{ event: 'CALL_DISPATCHED', step: `step${index}`, capability: 'operation.check' },
        { event: 'CALL_SUCCEEDED', step: `step${index}`, capability: 'operation.check' }]
        : [{ event: 'VALUE_BOUND', step: `step${index}` }]),
    ]).flat(),
    { event: 'RETURNED' }, { event: 'TERMINATED' },
  ]);
  assert.equal(consumeDecision({ ...completed(), trace: many(8) }).status, 'ALLOWED');
  assert.equal(consumeDecision({ ...completed(), trace: many(4, true) }).status, 'ALLOWED');
  blocked({ ...completed(), trace: many(9) });
  blocked({ ...completed(), trace: many(5, true) });
  blocked({ ...completed(), trace: many(32) });
  const repeated = many(2).map(event => event.step === 'step1' ? { ...event, step: 'step0' } : event);
  blocked({ ...completed(), trace: repeated });
});

test('rejects accessors, proxies and uncloneable values without evaluating them or throwing', () => {
  let evaluated = 0;
  const accessor = target => Object.defineProperty(target, 'decision', { enumerable: true, get() { evaluated++; throw Error('getter'); } });
  const outcomeGetter = Object.defineProperty(completed(), 'classification', { enumerable: true, get() { evaluated++; throw Error('getter'); } });
  blocked(outcomeGetter);
  blocked({ ...completed(), result: accessor({}) });
  const arrayGetter = completed();
  Object.defineProperty(arrayGetter.trace, '1', { enumerable: true, get() { evaluated++; throw Error('getter'); } });
  blocked(arrayGetter);
  const eventGetter = completed();
  Object.defineProperty(eventGetter.trace[1], 'event', { enumerable: true, get() { evaluated++; throw Error('getter'); } });
  blocked(eventGetter);
  const proxy = value => new Proxy(value, { get() { evaluated++; throw Error('proxy'); },
    getPrototypeOf() { evaluated++; throw Error('proxy'); }, ownKeys() { evaluated++; throw Error('proxy'); } });
  blocked(proxy(completed()));
  blocked(new Proxy(completed(), {}));
  for (const field of ['result', 'effects', 'trace']) blocked({ ...completed(), [field]: proxy(completed()[field]) });
  const eventProxy = completed(); eventProxy.trace[1] = proxy(eventProxy.trace[1]); blocked(eventProxy);
  const revoked = Proxy.revocable({}, {}); revoked.revoke(); blocked(revoked.proxy);
  blocked({ ...completed(), result: { decision: 'ALLOW', message: () => {} } });
  blocked({ ...completed(), result: { decision: 'ALLOW', message: Symbol('message') } });
  const cycle = completed(); cycle.result.message = cycle; blocked(cycle);
  assert.equal(evaluated, 0);
});

test('rejects hidden, symbolic, inherited, sparse and custom-prototype data', () => {
  for (const target of ['outcome', 'result', 'event', 'effects', 'trace']) {
    const outcome = completed();
    const value = target === 'outcome' ? outcome : target === 'event' ? outcome.trace[1] : outcome[target];
    Object.defineProperty(value, 'hidden', { value: true });
    blocked(outcome);
  }
  const symbolic = completed(); symbolic.result[Symbol('extra')] = true; blocked(symbolic);
  const inherited = completed(); inherited.result = Object.create({ decision: 'ALLOW' }); blocked(inherited);
  const sparse = completed(); delete sparse.trace[1]; blocked(sparse);
  const customArray = completed(); Object.setPrototypeOf(customArray.effects, null); blocked(customArray);
  const hiddenDecision = completed(); Object.defineProperty(hiddenDecision.result, 'decision', { enumerable: false }); blocked(hiddenDecision);
});
