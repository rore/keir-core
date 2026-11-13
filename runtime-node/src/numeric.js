// Internal exact numeric values. Only String normalizers are exported at the package root.
const minimum = -(1n << 63n), maximum = (1n << 63n) - 1n;
const modes = new Set(['UNNECESSARY', 'DOWN', 'FLOOR', 'CEILING', 'HALF_UP', 'HALF_EVEN']);
const requireThat = valid => { if (!valid) throw new TypeError('Invalid numeric value or operation.'); };
const abs = n => n < 0n ? -n : n;
const power = n => { requireThat(Number.isInteger(n) && n >= 0 && n <= 36); return 10n ** BigInt(n); };

function value(coefficient, scale) {
  if (coefficient === 0n) scale = 0;
  while (scale > 0 && coefficient % 10n === 0n) { coefficient /= 10n; scale--; }
  requireThat(scale <= 18 && abs(coefficient).toString().length <= 38);
  return Object.freeze({ coefficient, scale });
}

export function text({ coefficient, scale }) {
  const digits = abs(coefficient).toString().padStart(scale + 1, '0');
  return (coefficient < 0n ? '-' : '') + (scale ? digits.slice(0, -scale) + '.' + digits.slice(-scale) : digits);
}

function parse(raw, normalize) {
  requireThat(typeof raw === 'string' && raw.length <= (normalize ? 256 : 40));
  const pattern = normalize ? /^[+-]?[0-9]+(?:\.[0-9]+)?$/ : /^-?(?:0|[1-9][0-9]*)(?:\.[0-9]*[1-9])?$/;
  requireThat(pattern.exec(raw)?.[0] === raw); // Full match also rejects a final newline before JS's $ anchor.
  const negative = raw.startsWith('-'), unsigned = raw.replace(/^[+-]/, '');
  const [whole, fraction = ''] = unsigned.split('.');
  const normalizedFraction = fraction.replace(/0+$/, '');
  const digits = (whole + normalizedFraction).replace(/^0+/, '') || '0';
  requireThat(digits.length <= 38 && normalizedFraction.length <= 18);
  const result = value(BigInt((negative ? '-' : '') + digits), normalizedFraction.length);
  if (!normalize) requireThat(text(result) === raw);
  return result;
}

export const decimal = raw => parse(raw, false);
export const normalizeDecimal = raw => text(parse(raw, true));
export const normalizeInteger = raw => toInteger(parse(raw, true)).toString();

function int64(value) {
  requireThat(typeof value === 'bigint' && value >= minimum && value <= maximum);
  return value;
}

export function integer(raw) {
  requireThat(typeof raw === 'string' && raw.length <= 20 && /^(?:0|-?[1-9][0-9]*)$/.exec(raw)?.[0] === raw);
  return int64(BigInt(raw));
}

export const fromInteger = n => value(int64(n), 0);
export function toInteger(n) { requireThat(n.scale === 0); return int64(n.coefficient); }
export const addInteger = (a, b) => int64(int64(a) + int64(b));
export const subtractInteger = (a, b) => int64(int64(a) - int64(b));
export const multiplyInteger = (a, b) => int64(int64(a) * int64(b));
export const negateInteger = a => int64(-int64(a));
export const divideInteger = (a, b) => int64(quotient(int64(a), int64(b), 'UNNECESSARY'));

export function add(a, b) {
  const scale = Math.max(a.scale, b.scale);
  return value(a.coefficient * power(scale - a.scale) + b.coefficient * power(scale - b.scale), scale);
}
export const subtract = (a, b) => add(a, negate(b));
export const negate = a => value(-a.coefficient, a.scale);
export const multiply = (a, b) => value(a.coefficient * b.coefficient, a.scale + b.scale);
export const divide = (a, b) => value(quotient(a.coefficient * power(18 + b.scale - a.scale), b.coefficient, 'UNNECESSARY'), 18);
export function compare(a, b) {
  const scale = Math.max(a.scale, b.scale);
  const delta = a.coefficient * power(scale - a.scale) - b.coefficient * power(scale - b.scale);
  return delta < 0n ? -1 : delta > 0n ? 1 : 0;
}

function rounding(scale, mode) {
  requireThat(Number.isInteger(scale) && scale >= 0 && scale <= 18 && modes.has(mode));
}

export function round(a, scale, mode) {
  rounding(scale, mode);
  return a.scale <= scale ? a : value(quotient(a.coefficient, power(a.scale - scale), mode), scale);
}

export function multiplyRounded(a, b, scale, mode) {
  rounding(scale, mode);
  const product = a.coefficient * b.coefficient, productScale = a.scale + b.scale;
  return productScale <= scale ? value(product, productScale) : value(quotient(product, power(productScale - scale), mode), scale);
}

export function divideRounded(a, b, scale, mode) {
  rounding(scale, mode);
  const exponent = scale + b.scale - a.scale;
  return value(quotient(a.coefficient * power(Math.max(0, exponent)), b.coefficient * power(Math.max(0, -exponent)), mode), scale);
}

function quotient(numerator, denominator, mode) {
  requireThat(denominator !== 0n);
  if (denominator < 0n) { numerator = -numerator; denominator = -denominator; }
  let quotient = abs(numerator) / denominator;
  const remainder = abs(numerator) % denominator;
  if (mode === 'UNNECESSARY') requireThat(remainder === 0n);
  const up = remainder !== 0n && (mode === 'FLOOR' && numerator < 0n || mode === 'CEILING' && numerator > 0n ||
    (mode === 'HALF_UP' || mode === 'HALF_EVEN') && (2n * remainder > denominator ||
      2n * remainder === denominator && (mode === 'HALF_UP' || quotient % 2n === 1n)));
  if (up) quotient++;
  return numerator < 0n ? -quotient : quotient;
}
