import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import * as numeric from '../src/numeric.js';
import { normalizeInteger, normalizeDecimal } from '../src/index.js';
import { canonicalBytes } from '../src/canonical.js';

const vectors = JSON.parse(readFileSync(new URL('../../conformance/numeric-values.json', import.meta.url), 'utf8'));

function check(row, action) {
  if (row.expected === 'ERROR') {
    assert.throws(action, { name: 'TypeError', message: 'Invalid numeric value or operation.' });
  } else assert.equal(action(), row.expected);
}

function operation({ kind, op, args }) {
  if (kind === 'integer') {
    const a = numeric.integer(args[0]);
    if (op === 'parse') return a.toString();
    if (op === 'negate') return numeric.negateInteger(a).toString();
    const b = numeric.integer(args[1]);
    switch (op) {
      case 'add': return numeric.addInteger(a, b).toString();
      case 'subtract': return numeric.subtractInteger(a, b).toString();
      case 'multiply': return numeric.multiplyInteger(a, b).toString();
      case 'divide': return numeric.divideInteger(a, b).toString();
      default: throw new Error('Unknown vector operation');
    }
  }
  if (op === 'fromInteger') return numeric.text(numeric.fromInteger(numeric.integer(args[0])));
  const a = numeric.decimal(args[0]);
  if (op === 'parse') return numeric.text(a);
  if (op === 'toInteger') return numeric.toInteger(a).toString();
  if (op === 'negate') return numeric.text(numeric.negate(a));
  if (op === 'round') return numeric.text(numeric.round(a, args[1], args[2]));
  const b = numeric.decimal(args[1]);
  switch (op) {
    case 'compare': return numeric.compare(a, b).toString();
    case 'add': return numeric.text(numeric.add(a, b));
    case 'subtract': return numeric.text(numeric.subtract(a, b));
    case 'multiply': return numeric.text(numeric.multiply(a, b));
    case 'divide': return numeric.text(numeric.divide(a, b));
    case 'multiplyRounded': return numeric.text(numeric.multiplyRounded(a, b, args[2], args[3]));
    case 'divideRounded': return numeric.text(numeric.divideRounded(a, b, args[2], args[3]));
    default: throw new Error('Unknown vector operation');
  }
}

test('shared independent normalization vectors through root helpers', () => {
  for (const row of vectors.normalization) {
    const input = row.repeat ? row.repeat.repeat(row.count) : row.input;
    const normalize = row.kind === 'integer' ? normalizeInteger : normalizeDecimal;
    check(row, () => {
      const result = normalize(input);
      assert.equal(normalize(result), result);
      return result;
    });
  }
});

for (const row of vectors.operations) test('numeric/' + row.id, () => check(row, () => operation(row)));

test('helpers stay opt-in; reject nonstrings without coercion or disclosure', () => {
  const hostile = { toString() { throw Error('Must not coerce'); } };
  for (const input of [hostile, 1n, NaN, Infinity, false, undefined, new String('1')]) {
    for (const normalize of [normalizeInteger, normalizeDecimal]) {
      assert.throws(() => normalize(input), { name: 'TypeError', message: 'Invalid numeric value or operation.' });
    }
  }
  assert.throws(() => numeric.integer('100.00'), TypeError);
  assert.throws(() => numeric.decimal('100.00'), TypeError);
  assert.throws(() => canonicalBytes(-1));
  assert.throws(() => canonicalBytes(1.5));
  assert.throws(() => normalizeDecimal('private-invalid-input'), { message: 'Invalid numeric value or operation.' });
});

test('immutable intermediate values survive failure and reuse', () => {
  const a = numeric.decimal('1.25'), b = numeric.decimal('2'), intermediate = numeric.multiply(a, b);
  assert.throws(() => { a.coefficient = 100n; }, TypeError);
  assert.throws(() => numeric.divide(intermediate, numeric.decimal('0')), TypeError);
  assert.equal(numeric.text(numeric.add(intermediate, a)), '3.75');
  assert.equal(numeric.text(intermediate), '2.5');
  assert.equal(numeric.text(a), '1.25');
  assert.equal(numeric.text(b), '2');
});
