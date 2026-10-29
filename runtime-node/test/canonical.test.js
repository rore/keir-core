import test from 'node:test';
import assert from 'node:assert/strict';
import { canonicalBytes, parseCanonical, digest } from '../src/canonical.js';

const bytes = text => Buffer.from(text, 'utf8');

test('canonical JSON sorts keys by Unicode scalar value and preserves arrays and Unicode', () => {
  const input = { '\u{10000}': 'é', '\ue000': 'e\u0301', a: [true, 0] };
  assert.equal(canonicalBytes(input).toString(), '{"a":[true,0],"":"é","𐀀":"é"}');
  assert.notEqual(canonicalBytes({ x: 'é' }).toString(), canonicalBytes({ x: 'e\u0301' }).toString());
});

test('escapes quote, slash and all controls with lowercase hex', () => {
  const value = Array.from({ length: 32 }, (_, i) => String.fromCharCode(i)).join() + '"\\';
  const encoded = canonicalBytes(value).toString();
  assert.match(encoded, /\\u0000.*\\u001f/);
  assert.ok(encoded.endsWith('\\"\\\\"'));
});

test('rejects non-JSON values, unsafe numbers, malformed strings and accessors', () => {
  for (const value of [null, -0, -1, 1.5, NaN, Infinity, 2 ** 53, undefined, new Date(), '\ud800']) {
    assert.throws(() => canonicalBytes(value));
  }
  const accessor = Object.defineProperty({}, 'x', { enumerable: true, get() { throw Error('ran'); } });
  assert.throws(() => canonicalBytes(accessor), /property/);
  const symbol = { [Symbol('x')]: 1 };
  assert.throws(() => canonicalBytes(symbol), /property/);
  const hidden = {};
  Object.defineProperty(hidden, 'x', { value: 1, enumerable: false });
  assert.throws(() => canonicalBytes(hidden), /property/);
  const arrayAccessor = [];
  Object.defineProperty(arrayAccessor, '0', { get() { throw Error('ran'); }, enumerable: true });
  assert.throws(() => canonicalBytes(arrayAccessor), /array/);
  const arrayExtra = [];
  arrayExtra.extra = true;
  assert.throws(() => canonicalBytes(arrayExtra), /array/);
});

test('supports own __proto__ data property without prototype mutation', () => {
  const value = JSON.parse('{"__proto__":true}');
  assert.equal(canonicalBytes(value).toString(), '{"__proto__":true}');
  assert.equal(Object.getPrototypeOf(value), Object.prototype);
});

test('parser rejects duplicate keys, BOM, invalid UTF-8, trailing content and alternate formatting', () => {
  for (const input of ['{"a":1,"a":1}', '\ufeff{"a":1}', '{ "a":1}', '{"a":1}\n', '{"a":1.0}', '{"a":1e0}', '{"a":-0}', '{"a":9007199254740992}', '{"a":1}x']) {
    assert.throws(() => parseCanonical(bytes(input)));
  }
  assert.throws(() => parseCanonical(Buffer.from([0xc3, 0x28])));
});

test('enforces inclusive depth and byte bounds', () => {
  const atDepth = '['.repeat(15) + '0' + ']'.repeat(15);
  const overDepth = '['.repeat(16) + '0' + ']'.repeat(16);
  assert.deepEqual(parseCanonical(bytes(atDepth)), JSON.parse(atDepth));
  assert.throws(() => parseCanonical(bytes(overDepth)));
  const exact = '"' + 'a'.repeat(10) + '"';
  assert.equal(parseCanonical(bytes(exact), 12), 'a'.repeat(10));
  assert.throws(() => parseCanonical(bytes(exact), 11));
  assert.throws(() => canonicalBytes('a'.repeat(1_048_575)));
});

test('digest uses the sha256 prefix and lowercase hex', () => {
  assert.equal(digest(bytes('abc')), 'sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad');
});
