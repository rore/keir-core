import { createHash } from 'node:crypto';

const MAX_BYTES = 1_048_576;
const MAX_DEPTH = 16;

export class CanonicalLimitError extends Error {
  constructor() { super('Canonical JSON exceeds byte limit'); this.name = 'CanonicalLimitError'; }
}

function compareScalars(left, right) {
  const a = Array.from(left, char => char.codePointAt(0));
  const b = Array.from(right, char => char.codePointAt(0));
  for (let i = 0; i < Math.min(a.length, b.length); i++) if (a[i] !== b[i]) return a[i] - b[i];
  return a.length - b.length;
}

export function canonicalBytes(value, maxBytes = MAX_BYTES) {
  const requestedLimit = arguments.length > 1;
  if (!Number.isSafeInteger(maxBytes) || maxBytes < 0 || maxBytes > MAX_BYTES) throw new Error('Invalid canonical JSON byte limit');
  let size = 0;
  const chunks = [];
  let pending = [];
  let pendingBytes = 0;
  const write = chunk => {
    const bytes = Buffer.byteLength(chunk, 'utf8');
    if (size + bytes > maxBytes) {
      if (!requestedLimit) throw new Error('Canonical JSON exceeds byte limit');
      throw new CanonicalLimitError();
    }
    size += bytes;
    pending.push(chunk);
    pendingBytes += bytes;
    if (pendingBytes >= 1024) {
      chunks.push(pending.join(''));
      pending = [];
      pendingBytes = 0;
    }
  };
  const writeString = value => {
    if (!value.isWellFormed()) throw new Error('Invalid Unicode string');
    write('"');
    for (const char of value) {
      if (char === '"') write('\\"');
      else if (char === '\\') write('\\\\');
      else if (char.charCodeAt(0) < 32) write(`\\u${char.charCodeAt(0).toString(16).padStart(4, '0')}`);
      else write(char);
    }
    write('"');
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
  if (pending.length) chunks.push(pending.join(''));
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

// Model input may be formatted JSON; detect duplicate keys before JSON.parse loses them.
export function parseModelJson(bytes) {
  const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
  let offset = 0;
  const whitespace = () => { while (offset < text.length && /[ \t\r\n]/.test(text[offset])) offset++; };
  const take = token => { whitespace(); if (text[offset++] !== token) throw new Error('Invalid model JSON'); };
  function string() {
    whitespace(); const start = offset;
    take('"');
    let escaped = false;
    while (offset < text.length) {
      const char = text[offset++];
      if (!escaped && char === '"') return JSON.parse(text.slice(start, offset));
      if (!escaped && char === '\\') escaped = true; else escaped = false;
    }
    throw new Error('Invalid model string');
  }
  function value(depth = 1) {
    if (depth > MAX_DEPTH) throw new Error('Model JSON exceeds depth limit');
    whitespace(); const token = text[offset];
    if (token === '{') {
      offset++; const names = new Set(); whitespace();
      if (text[offset] === '}') { offset++; return; }
      while (true) {
        const key = string(); if (names.has(key)) throw new Error('Duplicate model key'); names.add(key);
        take(':'); value(depth + 1); whitespace();
        if (text[offset] === '}') { offset++; return; }
        take(',');
      }
    } else if (token === '[') {
      offset++; whitespace(); if (text[offset] === ']') { offset++; return; }
      while (true) {
        value(depth + 1); whitespace(); if (text[offset] === ']') { offset++; return; }
        take(',');
      }
    } else if (token === '"') string();
    else {
      const match = /^(?:true|false|0|[1-9][0-9]*)/.exec(text.slice(offset));
      if (!match) throw new Error('Invalid model value'); offset += match[0].length;
    }
  }
  value(); whitespace(); if (offset !== text.length) throw new Error('Trailing model JSON');
  const result = JSON.parse(text); canonicalBytes(result); return result;
}
