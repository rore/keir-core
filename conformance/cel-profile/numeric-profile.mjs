// Experimental probe only. Import-safe; no production API or numeric contract.
import assert from 'node:assert/strict';
import { cpus, availableParallelism, platform, arch } from 'node:os';
import { createHash } from 'node:crypto';
import { pb, cel, CheckedExprSchema, FileDescriptorSetSchema } from './profile.mjs';

const D = 'keir.numeric.Decimal', INPUT = 'keir.read.Input';
const modes = new Set(['UNNECESSARY', 'DOWN', 'FLOOR', 'CEILING', 'HALF_UP', 'HALF_EVEN']);
const minimum = -(1n << 63n), maximum = (1n << 63n) - 1n;
const timeErrorId = -1n;
const pow = n => { assert(Number.isInteger(n) && n >= 0 && n <= 36); return 10n ** BigInt(n); };
const abs = n => n < 0n ? -n : n;
export const hash = bytes => createHash('sha256').update(bytes).digest('hex');
export const check = (actual, expected) => assert.deepStrictEqual(actual, expected, 'Independent expectation mismatch');
export const progress = { phase: 'not started', checkedCases: 0, checkedTimedCases: 0 };

function reduced(c, s) {
  if (c === 0n) return { c: 0n, s: 0 };
  while (s > 0 && c % 10n === 0n) { c /= 10n; s--; }
  return { c, s };
}
function spelling({ c, s }) {
  const digits = abs(c).toString().padStart(s + 1, '0');
  return (c < 0n ? '-' : '') + (s ? digits.slice(0, -s) + '.' + digits.slice(-s) : digits);
}
function bounded(c, s, candidate) {
  const value = reduced(c, s), scale = candidate === 'fixed9' ? 9 : 18;
  if (value.s > scale || abs(value.c).toString().length > 38) throw Error('numeric domain');
  if (candidate !== 'normalized18' && abs(value.c * pow(scale - value.s)).toString().length > 38) throw Error('fixed range');
  return value;
}
function parseText(value, candidate, normalize = false) {
  if (typeof value !== 'string' || value.length > (normalize ? 256 : 40) ||
      !(normalize ? /^[+-]?[0-9]+(?:\.[0-9]+)?$/ : /^-?(?:0|[1-9][0-9]*)(?:\.[0-9]*[1-9])?$/).test(value)) throw Error('numeric text');
  const [whole, fraction = ''] = value.split('.');
  const result = reduced(BigInt(whole + fraction), fraction.length);
  // Normalize redundant textual zeros before constructing powers or checking the candidate.
  if (result.s > 18 || abs(result.c).toString().length > 38) throw Error('numeric text range');
  const checked = bounded(result.c, result.s, candidate);
  if (!normalize && spelling(checked) !== value) throw Error('noncanonical numeric text');
  return checked;
}
export function normalize(value, kind) {
  const decimal = parseText(value, 'normalized18', true);
  if (kind === 'integer') {
    if (decimal.s || decimal.c < minimum || decimal.c > maximum) throw Error('integer range');
  } else if (kind !== 'decimal') throw Error('helper kind');
  return spelling(decimal);
}
function integer(value) {
  if (typeof value !== 'string' || value.length > 20 || !/^(?:0|-?[1-9][0-9]*)$/.test(value)) throw Error('integer text');
  const n = BigInt(value);
  if (n < minimum || n > maximum) throw Error('integer range');
  return n;
}
function quotient(n, d, mode) {
  if (!d || !modes.has(mode)) throw Error('division/mode');
  if (d < 0n) { n = -n; d = -d; }
  let q = abs(n) / d;
  const r = abs(n) % d;
  if (r && mode === 'UNNECESSARY') throw Error('inexact');
  const up = r && (mode === 'FLOOR' && n < 0n || mode === 'CEILING' && n > 0n ||
    (mode === 'HALF_UP' || mode === 'HALF_EVEN') && (2n * r > d || 2n * r === d && (mode === 'HALF_UP' || q % 2n === 1n)));
  if (up) q++;
  return n < 0n ? -q : q;
}

export function kernel(candidate, registry) {
  const descriptor = registry.getMessage(D), type = cel.objectType(descriptor);
  assert(descriptor);
  const counters = Object.create(null);
  const hit = name => { counters[name] = (counters[name] ?? 0) + 1; };
  const message = value => pb.create(descriptor, { value: spelling(value) });
  const read = value => {
    hit('decimalOperandOrResultParses');
    if (value?.desc?.typeName !== D || value.message?.$typeName !== D || value.message.$unknown?.length) throw Error('Decimal message');
    return parseText(value.message.value, candidate);
  };
  const result = (c, s) => message(bounded(c, s, candidate));
  const funcs = [], names = new Map();
  function add(name, args, output, id, action) {
    names.set(id, name);
    funcs.push(cel.celFunc(name, args, output, (...values) => {
      hit(id);
      try { return action(...values); } catch (error) { throw cel.celError(error); }
    }));
  }
  const I = cel.CelScalar.INT, S = cel.CelScalar.STRING, B = cel.CelScalar.BOOL;
  add('decimal', [S], type, 'keir_decimal_from_string', text => message(parseText(text, candidate)));
  add('toDecimal', [I], type, 'keir_decimal_from_integer', n => result(n, 0));
  add('toInteger', [type], I, 'keir_integer_from_decimal', value => {
    const n = read(value); if (n.s) throw Error('fraction'); return integer(n.c.toString());
  });
  add('_/_', [I, I], I, 'divide_int64', (a, b) => integer(quotient(a, b, 'UNNECESSARY').toString()));
  for (const [name, id, sign] of [['_+_', 'add', 1n], ['_-_', 'subtract', -1n]]) {
    add(name, [type, type], type, 'keir_decimal_' + id, (left, right) => {
      const a = read(left), b = read(right), s = Math.max(a.s, b.s);
      return result(a.c * pow(s - a.s) + sign * b.c * pow(s - b.s), s);
    });
  }
  add('_*_', [type, type], type, 'keir_decimal_multiply', (left, right) => {
    const a = read(left), b = read(right); return result(a.c * b.c, a.s + b.s);
  });
  add('_/_', [type, type], type, 'keir_decimal_divide', (left, right) => {
    const a = read(left), b = read(right); return result(quotient(a.c * pow(18 + b.s - a.s), b.c, 'UNNECESSARY'), 18);
  });
  add('-_', [type], type, 'keir_decimal_negate', value => { const a = read(value); return result(-a.c, a.s); });
  for (const [name, id, predicate] of [['_<_', 'less', n => n < 0n], ['_<=_', 'less_equal', n => n <= 0n],
    ['_>_', 'greater', n => n > 0n], ['_>=_', 'greater_equal', n => n >= 0n]]) {
    add(name, [type, type], B, 'keir_decimal_' + id, (left, right) => {
      const a = read(left), b = read(right), s = Math.max(a.s, b.s);
      return predicate(a.c * pow(s - a.s) - b.c * pow(s - b.s));
    });
  }
  const scale = n => { if (n < 0n || n > 18n) throw Error('scale'); return Number(n); };
  add('round', [type, I, S], type, 'keir_decimal_round', (value, digits, mode) => {
    const a = read(value), s = scale(digits);
    return a.s <= s ? result(a.c, a.s) : result(quotient(a.c, pow(a.s - s), mode), s);
  });
  add('multiplyRounded', [type, type, I, S], type, 'keir_decimal_multiply_rounded', (left, right, digits, mode) => {
    const a = read(left), b = read(right), s = scale(digits), product = a.c * b.c;
    return a.s + b.s <= s ? result(product, a.s + b.s) : result(quotient(product, pow(a.s + b.s - s), mode), s);
  });
  add('divideRounded', [type, type, I, S], type, 'keir_decimal_divide_rounded', (left, right, digits, mode) => {
    const a = read(left), b = read(right), s = scale(digits), exponent = s + b.s - a.s;
    return result(quotient(a.c * pow(Math.max(0, exponent)), b.c * pow(Math.max(0, -exponent)), mode), s);
  });
  const timestamp = cel.objectType(registry.getMessage('google.protobuf.Timestamp'));
  const duration = cel.objectType(registry.getMessage('google.protobuf.Duration'));
  add('_+_', [timestamp, duration], timestamp, 'add_timestamp_duration', (a, b) => {
    const seconds = a.message.seconds + b.message.seconds;
    if (seconds < -62135596800n || seconds > 253402300799n) throw cel.celError('Timestamp overflow', timeErrorId);
    return pb.create(registry.getMessage('google.protobuf.Timestamp'), { seconds, nanos: a.message.nanos });
  });
  function project(raw, model) {
    hit('projections');
    const base = { a: '1.25', b: '2', d: '1', i: '6', j: '2', flag: true, text: 'tenant',
      input: model === 'numeric' ? {} : { flag: false, text: '', tags: [] }, ...raw };
    const vars = { flag: base.flag, text: base.text, i: integer(base.i), j: integer(base.j) };
    if (typeof vars.flag !== 'boolean' || typeof vars.text !== 'string') throw Error('activation');
    for (const name of ['a', 'b', 'd']) vars[name] = message(parseText(base[name], candidate));
    const input = base.input;
    if (!input || Object.getPrototypeOf(input) !== Object.prototype) throw Error('input');
    let projected;
    if (model === 'numeric') {
      if (Object.keys(input).some(k => !['maybe', 'zero'].includes(k))) throw Error('input fields');
      projected = {};
      if (Object.hasOwn(input, 'maybe')) projected.maybe = { value: spelling(parseText(input.maybe, candidate)) };
      if (Object.hasOwn(input, 'zero')) projected.zero = integer(input.zero).toString();
    } else {
      if (Object.keys(input).sort().join() !== 'flag,tags,text' || typeof input.flag !== 'boolean' || typeof input.text !== 'string' ||
          !Array.isArray(input.tags) || input.tags.length > 16 || input.tags.some(x => typeof x !== 'string' || x.length > 256)) throw Error('plain input');
      projected = input;
    }
    vars.input = pb.fromJson(registry.getMessage(INPUT), projected);
    return vars;
  }
  function output(value, typeName) {
    if (cel.isCelError(value)) return value.exprId === timeErrorId ? 'TIME_ERROR' : 'ERROR';
    if (typeName === 'Decimal') return spelling(read(value));
    if (typeName === 'Integer') return integer(typeof value === 'bigint' ? value.toString() : null).toString();
    if (typeName === 'Boolean' && typeof value === 'boolean' || typeName === 'String' && typeof value === 'string') return value;
    throw Error('result type');
  }
  const variables = { a: type, b: type, d: type, i: I, j: I, flag: B, text: S, input: cel.objectType(registry.getMessage(INPUT)) };
  return { candidate, registry, counters, project, output, read, message, names, env: cel.celEnv({ registry, variables, funcs }) };
}

const types = { Boolean: 1, Integer: 2, String: 5 };
function wire(name) {
  if (types[name]) return { case: 'primitive', value: types[name] };
  if (name === 'Timestamp' || name === 'Duration') return { case: 'wellKnown', value: name === 'Timestamp' ? 2 : 3 };
  if (name === 'Tags') return { case: 'listType', value: { elemType: { typeKind: wire('String') } } };
  return { case: 'messageType', value: name === 'Decimal' ? D : INPUT };
}
// Probe gate, deliberately narrower than CEL; never an artifact admission API.
export function gate(checked, model) {
  const ids = new Set(), refs = new Set(), declarations = { a: 'Decimal', b: 'Decimal', d: 'Decimal', i: 'Integer', j: 'Integer', flag: 'Boolean', text: 'String', input: 'Input' };
  function visit(expr, depth = 1, present = new Set()) {
    assert(expr && expr.id > 0n && !ids.has(String(expr.id)) && depth <= 16 && ids.size < 64);
    const id = String(expr.id); ids.add(id);
    const { case: kind, value } = expr.exprKind;
    let type;
    if (kind === 'constExpr') {
      const c = value.constantKind;
      type = { boolValue: 'Boolean', int64Value: 'Integer', stringValue: 'String' }[c.case]; assert(type);
      if (type === 'String') assert(c.value.isWellFormed() && Buffer.byteLength(c.value) <= 256);
    } else if (kind === 'identExpr') {
      type = declarations[value.name]; assert(type);
      const ref = checked.referenceMap[id]; assert(ref && ref.name === value.name && !ref.value && ref.overloadId.length === 0); refs.add(id);
    } else if (kind === 'selectExpr') {
      check(visit(value.operand, depth + 1, present), 'Input');
      const fields = model === 'numeric' ? { maybe: 'Decimal', zero: 'Integer' } : { flag: 'Boolean', text: 'String', tags: 'Tags' };
      type = fields[value.field]; assert(type);
      if (value.testOnly) { assert(model === 'numeric'); type = 'Boolean'; }
      else if (model === 'numeric') assert(present.has(value.field));
    } else {
      assert(kind === 'callExpr' && !value.target);
      const f = value.function, args = value.args;
      const expectedArity = f === '_?_:_' ? 3 : ['round'].includes(f) ? 3 : ['multiplyRounded', 'divideRounded'].includes(f) ? 4 :
        ['!_', '-_', 'decimal', 'toDecimal', 'toInteger', 'timestamp', 'duration', 'size'].includes(f) ? 1 : 2;
      check(args.length, expectedArity);
      let proof = args[0]?.exprKind;
      const negated = proof?.case === 'callExpr' && proof.value.function === '!_';
      if (negated) proof = proof.value.args[0]?.exprKind;
      const proofField = proof?.case === 'selectExpr' && proof.value.testOnly ? proof.value.field : undefined;
      const a = args.map((arg, i) => visit(arg, depth + 1, proofField && (f === '_&&_' && i === 1 && !negated || f === '_?_:_' && i === (negated ? 2 : 1)) ? new Set([...present, proofField]) : present));
      let overload;
      const pair = t => a.length === 2 && a.every(x => x === t);
      const literal = i => args[i].exprKind.case === 'constExpr' ? args[i].exprKind.value.constantKind : {};
      if (['_==_', '_!=_'].includes(f)) { assert(a[0] === a[1] && !['Input', 'Tags', 'Duration'].includes(a[0])); type = 'Boolean'; overload = f === '_==_' ? 'equals' : 'not_equals'; }
      else if (f === '_?_:_') { assert(a[0] === 'Boolean' && a[1] === a[2] && !['Input', 'Tags'].includes(a[1])); type = a[1]; overload = 'conditional'; }
      else if (['!_', '_&&_', '_||_'].includes(f)) { assert(a.every(x => x === 'Boolean')); type = 'Boolean'; overload = { '!_': 'logical_not', '_&&_': 'logical_and', '_||_': 'logical_or' }[f]; }
      else if (f === 'decimal') { assert(literal(0).case === 'stringValue'); parseText(literal(0).value, 'normalized18'); type = 'Decimal'; overload = 'keir_decimal_from_string'; }
      else if (f === 'toDecimal' || f === 'toInteger') { check(a[0], f === 'toDecimal' ? 'Integer' : 'Decimal'); type = f === 'toDecimal' ? 'Decimal' : 'Integer'; overload = f === 'toDecimal' ? 'keir_decimal_from_integer' : 'keir_integer_from_decimal'; }
      else if (f === 'size') { check(a[0], 'Tags'); type = 'Integer'; overload = 'size_list'; }
      else if (f === 'timestamp' || f === 'duration') { assert(literal(0).case === 'stringValue'); type = f === 'timestamp' ? 'Timestamp' : 'Duration'; overload = 'string_to_' + f; }
      else if (['round', 'multiplyRounded', 'divideRounded'].includes(f)) {
        const i = args.length - 2, scale = literal(i), mode = literal(i + 1);
        assert(a.slice(0, i).every(t => t === 'Decimal') && scale.case === 'int64Value' && scale.value >= 0n && scale.value <= 18n && mode.case === 'stringValue' && modes.has(mode.value));
        type = 'Decimal'; overload = { round: 'keir_decimal_round', multiplyRounded: 'keir_decimal_multiply_rounded', divideRounded: 'keir_decimal_divide_rounded' }[f];
      } else {
        const operation = { '_+_': 'add', '_-_': 'subtract', '_*_': 'multiply', '_/_': 'divide', '-_': 'negate', '_<_': 'less', '_<=_': 'less_equals', '_>_': 'greater', '_>=_': 'greater_equals' }[f]; assert(operation);
        if (f === '_+_' && pair('String')) { type = 'String'; overload = 'add_string'; }
        else if (f === '_+_' && a.join() === 'Timestamp,Duration') { type = 'Timestamp'; overload = 'add_timestamp_duration'; }
        else {
          assert(a.every(t => t === a[0]) && ['Integer', 'Decimal'].includes(a[0]));
          type = ['less', 'less_equals', 'greater', 'greater_equals'].includes(operation) ? 'Boolean' : a[0];
          overload = a[0] === 'Integer' ? operation + '_int64' : 'keir_decimal_' + operation.replace('_equals', '_equal');
        }
      }
      const ref = checked.referenceMap[id]; assert(ref && ref.name === '' && !ref.value); check(ref.overloadId, [overload]); refs.add(id);
    }
    const actualType = checked.typeMap[id]?.typeKind;
    if (type === 'Tags') { assert(actualType?.case === 'listType'); check(actualType.value.elemType.typeKind, wire('String')); }
    else check(actualType, wire(type));
    return type;
  }
  const type = visit(checked.expr);
  check(Object.keys(checked.typeMap).sort(), [...ids].sort()); check(Object.keys(checked.referenceMap).sort(), [...refs].sort());
  return type;
}

export function inventory(fixtures) {
  assert(fixtures && fixtures.helpers.length <= 64 && fixtures.cases.length <= 128 && fixtures.rejectSources.length <= 32 && fixtures.concurrent.length === 3);
  assert(fixtures.cases.filter(x => x.timed).length <= 8);
  const ids = new Set();
  for (const spec of [...fixtures.cases, ...fixtures.concurrent]) {
    assert(/^[a-z][a-z0-9-]{0,63}$/.test(spec.id) && !ids.has(spec.id)); ids.add(spec.id);
    assert(Buffer.byteLength(spec.source) <= 512 && (!spec.bind || Buffer.byteLength(spec.bind) <= 512));
  }
  for (const spec of fixtures.helpers) if (spec.repeat) assert(spec.repeat === '0' && Number.isInteger(spec.count) && spec.count >= 0 && spec.count <= 257);
}
function registry(bytes) {
  assert(bytes.length <= 65536);
  return pb.createFileRegistry(pb.fromBinary(FileDescriptorSetSchema, bytes));
}
function prepared(bundle, candidate) {
  for (const [name, bytes] of Object.entries(bundle.descriptors)) check(hash(Buffer.from(bytes, 'base64')), bundle.descriptorHashes[name]);
  const kernels = Object.fromEntries(Object.entries(bundle.descriptors).map(([name, value]) => [name, kernel(candidate, registry(Buffer.from(value, 'base64')))]));
  const expressions = new Map();
  for (const row of bundle.expressions) {
    const k = kernels[row.model], bytes = Buffer.from(row.checked, 'base64'); assert(bytes.length <= 16384); check(hash(bytes), row.sha256);
    const checked = pb.fromBinary(CheckedExprSchema, bytes); check(gate(checked, row.model), row.type);
    expressions.set(row.id, { row, checked, k, evaluate: cel.plan(k.env, checked) });
  }
  return { kernels, expressions };
}
function evaluate(p, spec, projected) {
  const entry = p.expressions.get(spec.id), { k } = entry;
  let vars = projected ?? k.project(spec.vars ?? {}, spec.model ?? 'plain');
  for (const name of spec.omit ?? []) { assert(name === 'a'); delete vars[name]; }
  if (spec.bind) {
    const first = p.expressions.get(spec.id + '-bind'), value = first.evaluate(vars);
    if (cel.isCelError(value)) return k.output(value, 'Decimal');
    k.read(value);
    vars = { ...vars, d: value.message };
  }
  return k.output(entry.evaluate(vars), spec.type);
}
function distribution(samples, elapsedNs) {
  const sorted = [...samples].sort((a, b) => a - b);
  return { checked: samples.length, elapsedNs, throughputPerSecond: samples.length * 1e9 / elapsedNs,
    p50Ns: sorted[Math.ceil(sorted.length * .5) - 1], p95Ns: sorted[Math.ceil(sorted.length * .95) - 1], p99Ns: sorted[Math.ceil(sorted.length * .99) - 1], maxNs: sorted.at(-1) };
}
function measure(action, expected, count = 1000, warmups = 100) {
  for (let i = 0; i < warmups; i++) check(action(), expected);
  const samples = [], start = process.hrtime.bigint();
  for (let i = 0; i < count; i++) { const t = process.hrtime.bigint(), value = action(); samples.push(Number(process.hrtime.bigint() - t)); check(value, expected); }
  return distribution(samples, Number(process.hrtime.bigint() - start));
}
function measureEngine(entry, vars, expected) {
  for (let i = 0; i < 100; i++) check(entry.k.output(entry.evaluate(vars), entry.row.type), expected);
  const samples = [], start = process.hrtime.bigint();
  for (let i = 0; i < 1000; i++) {
    const t = process.hrtime.bigint(), value = entry.evaluate(vars);
    samples.push(Number(process.hrtime.bigint() - t));
    check(entry.k.output(value, entry.row.type), expected);
  }
  return distribution(samples, Number(process.hrtime.bigint() - start));
}
const expectFor = (spec, candidate) => typeof spec.expected === 'object' ? spec.expected[candidate] : spec.expected;

export async function replay(fixtures, bundle) {
  inventory(fixtures);
  const report = { status: 'running', node: process.version, platform: platform(), arch: arch(), cpu: cpus()[0]?.model,
    availableParallelism: availableParallelism(), counters: {}, cases: [], helpers: [], negativeChecks: [], timings: [], concurrency: [],
    allocation: { supported: false, reason: 'Heap/RSS observations are not an allocation counter.' },
    projectionWorkload: 'common eight-declaration activation for all families, not a legacy nonnumeric Core invocation',
    adaptation: 'value:string is reparsed per Decimal operand; no parse-once or tenant cache claim' };
  for (const spec of fixtures.helpers) {
    progress.phase = 'normalization helpers';
    let actual; try { actual = normalize(spec.repeat ? spec.repeat.repeat(spec.count) : spec.input, spec.kind); } catch { actual = 'ERROR'; }
    check(actual, spec.expected); report.helpers.push({ kind: spec.kind, actual });
  }
  let p;
  for (const candidate of ['fixed9', 'fixed18', 'normalized18']) {
    const start = process.hrtime.bigint(); p = prepared(bundle, candidate);
    const preparationNs = Number(process.hrtime.bigint() - start);
    for (const spec of fixtures.cases) {
      progress.phase = candidate + '/' + spec.id;
      const entry = p.expressions.get(spec.id), counter = entry.k.counters[spec.counter] ?? 0;
      const actual = evaluate(p, spec); check(actual, expectFor(spec, candidate));
      if (spec.counter) assert((entry.k.counters[spec.counter] ?? 0) > counter, 'Custom binding was not reached');
      report.cases.push({ id: spec.id, candidate, actual });
      progress.checkedCases++;
    }
    report.counters[candidate] = { preparationNs, byModel: Object.fromEntries(Object.entries(p.kernels).map(([id, k]) => [id, { ...k.counters }])) };
  }
  for (const [model, k] of Object.entries(p.kernels)) {
    for (const invalid of [0, 1n, NaN, Infinity, null, '100.00', '-0']) {
      assert.throws(() => k.project({ a: invalid }, model)); report.negativeChecks.push('native/noncanonical-' + model);
    }
    assert.throws(() => k.read({ desc: { typeName: D }, message: { $typeName: D, value: '' } }));
  }
  const donor = p.expressions.get('decimal-add');
  for (const mutate of [c => { c.typeMap = {}; }, c => { c.referenceMap[c.expr.id].overloadId = ['forged']; },
    c => { c.typeMap['99999'] = c.typeMap[c.expr.id]; }, c => { c.expr.exprKind.value.target = c.expr.exprKind.value.args[0]; }]) {
    const changed = structuredClone(donor.checked); mutate(changed); assert.throws(() => gate(changed, 'plain')); report.negativeChecks.push('forged-metadata');
  }
  const floating = structuredClone(p.expressions.get('integer-max').checked);
  floating.expr.exprKind.value.constantKind = { case: 'doubleValue', value: 1.5 };
  assert.throws(() => gate(floating, 'plain')); report.negativeChecks.push('forged-double');
  const oracle = fixtures.cases.find(spec => spec.id === 'decimal-add');
  const wrongOracle = { ...oracle, expected: 'deliberately-wrong' }, oracleActual = evaluate(p, wrongOracle);
  check(oracleActual, expectFor(oracle, 'normalized18')); // Evaluation/setup failures cannot satisfy this negative check.
  assert.throws(() => check(oracleActual, expectFor(wrongOracle, 'normalized18')), assert.AssertionError);
  report.negativeChecks.push('wrong-expectation');
  // An error may not poison the next invocation of the same prepared plan.
  const recovery = fixtures.cases.find(x => x.id === 'integer-exact-divide');
  check(evaluate(p, { ...recovery, vars: { j: '0' } }), 'ERROR'); check(evaluate(p, recovery), '3');
  const decimalRecovery = fixtures.cases.find(x => x.id === 'decimal-divide');
  check(evaluate(p, { ...decimalRecovery, vars: { b: '0' } }), 'ERROR'); check(evaluate(p, decimalRecovery), '0.625');
  for (const spec of fixtures.cases.filter(x => x.timed)) {
    progress.phase = 'timing/' + spec.id;
    const entry = p.expressions.get(spec.id), vars = entry.k.project(spec.vars ?? {}, spec.model ?? 'plain');
    const before = process.memoryUsage(), cpu = process.cpuUsage();
    const engine = measureEngine(entry, vars, spec.expected);
    const warm = measure(() => entry.k.output(entry.evaluate(vars), spec.type), spec.expected);
    const complete = measure(() => evaluate(p, spec), spec.expected);
    report.timings.push({ id: spec.id, preparedEngineEvaluation: engine, preparedEvaluationAndResultValidation: warm, projectionEvaluationAndResultValidation: complete,
      cpuUs: process.cpuUsage(cpu), before, after: process.memoryUsage() });
    progress.checkedTimedCases++;
  }
  report.normalization = measure(() => normalize('100.00', 'decimal'), '100');
  const k = p.kernels.plain;
  report.projection = measure(() => { const v = k.project({}, 'plain'); return typeof v.i; }, 'bigint');
  report.warmPreparations = [];
  for (const id of ['scalar', 'integer-add', 'decimal-add']) {
    const row = bundle.expressions.find(x => x.id === id), preparations = [];
    const subset = { ...bundle, descriptors: { [row.model]: bundle.descriptors[row.model] }, expressions: [row] };
    for (let i = 0; i < 30; i++) { const t = process.hrtime.bigint(); prepared(subset, 'normalized18'); preparations.push(Number(process.hrtime.bigint() - t)); }
    report.warmPreparations.push({ id, ...distribution(preparations, preparations.reduce((a, b) => a + b, 0)) });
  }
  for (const spec of fixtures.concurrent) {
    progress.phase = 'concurrency/' + spec.id;
    const entry = p.expressions.get(spec.id), samples = [], cpu = process.cpuUsage();
    function one(index) {
      const tenant = 'tenant-' + index, t = process.hrtime.bigint();
      const vars = entry.k.project({ i: String(index), text: tenant }, 'plain'), value = entry.k.output(entry.evaluate(vars), spec.type);
      samples.push(Number(process.hrtime.bigint() - t));
      check(value, spec.type === 'String' ? tenant + '-ok' : spec.type === 'Integer' ? String(index + 1) : index + '.25');
    }
    for (let i = 0; i < 100; i++) one(i); samples.length = 0;
    let tickNs;
    const tickStart = process.hrtime.bigint(), tick = new Promise(resolve => setTimeout(() => { tickNs = Number(process.hrtime.bigint() - tickStart); resolve(); }, 0));
    const start = process.hrtime.bigint();
    await Promise.all(Array.from({ length: 4 }, async (_, lane) => { for (let i = 0; i < 250; i++) { await Promise.resolve(); one(lane * 250 + i); } }));
    const elapsed = Number(process.hrtime.bigint() - start); await tick;
    report.concurrency.push({ id: spec.id, ...distribution(samples, elapsed), cpuUs: process.cpuUsage(cpu), eventLoopTickNs: tickNs,
      scheduling: 'four interleaved same-isolate lanes; synchronous CPU is serialized, not worker parallelism' });
  }
  report.finalCounters = Object.fromEntries(Object.entries(p.kernels).map(([id, value]) => [id, value.counters]));
  report.retainedHeapBytes = (() => { global.gc(); return process.memoryUsage().heapUsed; })();
  report.memory = process.memoryUsage(); report.processPeakRssBytes = process.resourceUsage().maxRSS * 1024;
  assert(report.processPeakRssBytes <= 512 * 1024 * 1024);
  progress.phase = 'complete'; report.status = 'passed'; return report;
}
