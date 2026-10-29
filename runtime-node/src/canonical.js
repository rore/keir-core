import { createHash } from 'node:crypto';

const MAX_BYTES = 1_048_576;
const MAX_DEPTH = 16;

function compareScalars(left, right) {
  const a = Array.from(left, char => char.codePointAt(0));
  const b = Array.from(right, char => char.codePointAt(0));
  for (let i = 0; i < Math.min(a.length, b.length); i++) if (a[i] !== b[i]) return a[i] - b[i];
  return a.length - b.length;
}

export function canonicalBytes(value) {
  let size = 0;
  const chunks = [];
  const write = chunk => {
    size += Buffer.byteLength(chunk, 'utf8');
    if (size > MAX_BYTES) throw new Error('Canonical JSON exceeds byte limit');
    chunks.push(chunk);
  };
  const writeString = value => {
    if (!value.isWellFormed()) throw new Error('Invalid Unicode string');
    if (Buffer.byteLength(value, 'utf8') + 2 > MAX_BYTES) throw new Error('Canonical JSON exceeds byte limit');
    const escaped = value.replace(/["\\\u0000-\u001f]/g, char => {
      if (char === '"') return '\\"';
      if (char === '\\') return '\\\\';
      return `\\u00${char.charCodeAt(0).toString(16).padStart(2, '0')}`;
    });
    write(`"${escaped}"`);
  };
  const visit = (item, depth) => {
    if (depth > MAX_DEPTH) throw new Error('Canonical JSON exceeds depth limit');
    if (item === null) throw new Error('Unsupported canonical JSON value');
    if (typeof item === 'boolean') return write(item ? 'true' : 'false');
    if (typeof item === 'number') {
      if (!Number.isSafeInteger(item) || item < 0 || Object.is(item, -0)) throw new Error('Invalid metadata integer');
      return write(String(item));
    }
    if (typeof item === 'string') return writeString(item);
    if (Array.isArray(item)) {
      if (Object.getPrototypeOf(item) !== Array.prototype) throw new Error('Unsupported canonical JSON array');
      const keys = Reflect.ownKeys(item);
      if (keys.length !== item.length + 1 || keys.some(key => key !== 'length' && (!/^(0|[1-9]\d*)$/.test(key) || Number(key) >= item.length))) {
        throw new Error('Unsupported canonical JSON array');
      }
      write('[');
      for (let i = 0; i < item.length; i++) {
        const descriptor = Object.getOwnPropertyDescriptor(item, String(i));
        if (!descriptor || !Object.hasOwn(descriptor, 'value')) throw new Error('Unsupported canonical JSON array');
        if (i) write(',');
        visit(descriptor.value, depth + 1);
      }
      return write(']');
    }
    if (typeof item !== 'object' || (Object.getPrototypeOf(item) !== Object.prototype && Object.getPrototypeOf(item) !== null)) {
      throw new Error('Unsupported canonical JSON value');
    }
    const keys = Reflect.ownKeys(item);
    if (keys.some(key => typeof key !== 'string')) throw new Error('Unsupported canonical JSON property');
    keys.sort(compareScalars);
    write('{');
    for (let i = 0; i < keys.length; i++) {
      const key = keys[i];
      const descriptor = Object.getOwnPropertyDescriptor(item, key);
      if (!descriptor || !descriptor.enumerable || !Object.hasOwn(descriptor, 'value')) throw new Error('Unsupported canonical JSON property');
      if (i) write(',');
      writeString(key); write(':'); visit(descriptor.value, depth + 1);
    }
    write('}');
  };
  visit(value, 1);
  return Buffer.from(chunks.join(''), 'utf8');
}

function checkDepth(text) {
  let depth = 0, quoted = false, escaped = false;
  for (const char of text) {
    if (quoted) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === '"') quoted = false;
    } else if (char === '"') quoted = true;
    else if (char === '{' || char === '[') {
      if (++depth > MAX_DEPTH) throw new Error('Canonical JSON exceeds depth limit');
    } else if (char === '}' || char === ']') depth--;
  }
}

export function parseCanonical(bytes, maxBytes = MAX_BYTES) {
  if (!(bytes instanceof Uint8Array) || !Number.isSafeInteger(maxBytes) || maxBytes < 0 || bytes.byteLength > maxBytes || bytes.byteLength > MAX_BYTES) throw new Error('Invalid canonical JSON bytes');
  let text;
  try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch { throw new Error('Invalid canonical JSON UTF-8'); }
  checkDepth(text);
  let value;
  try { value = JSON.parse(text); }
  catch { throw new Error('Invalid canonical JSON'); }
  if (!canonicalBytes(value).equals(Buffer.from(bytes))) throw new Error('Noncanonical JSON');
  return value;
}

export function digest(bytes) {
  return `sha256:${createHash('sha256').update(bytes).digest('hex')}`;
}
