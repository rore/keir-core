import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram, invokeProgram } from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { fixture, bind, approval, admission, registrations, context } from './reference-helpers.js';

const root = new URL('../../conformance/read2-structural/', import.meta.url);
const rows = JSON.parse(readFileSync(new URL('residuals.json', root)));

test('structural artifact positives still require native admission and current authority', async () => {
  const f = fixture('read'); let dispatched = 0, authorized = 0;
  const invokers = registrations(f);
  for (const row of Object.values(invokers)) row.invoke = () => { dispatched++; throw new Error('Unexpected dispatch'); };
  const model = bind(f, invokers);
  for (const row of rows.filter(row => row.stage !== 'compile')) {
    const bytes = row.raw ? Buffer.from(row.raw) : canonicalBytes(row.document);
    if (row.stage === 'bind') {
      const result = bindModel(bytes, approval(f), invokers);
      assert.equal(result.boundModel, null, row.id);
      assert.deepEqual({ code: result.diagnostic.code, path: result.diagnostic.path }, row.expected, row.id);
      assert.equal(authorized, 0, row.id); assert.equal(dispatched, 0, row.id); continue;
    }
    // Select mutated bytes only in this trusted negative fixture; do not trust their self-description.
    const selected = admission(f, { expectedArtifactDigest: row.badPin ? 'sha256:' + '0'.repeat(64) : digest(bytes), ...row.admission });
    const preparation = prepareProgram(bytes, model, selected);
    if (row.stage === 'invoke') {
      assert.equal(preparation.diagnostic, null, row.id);
      const out = await invokeProgram(preparation.prepared, { taskId: 'task1' }, context('read', { authorize: () => { authorized++; return false; } }));
      assert.deepEqual(out.error, row.expected, row.id); assert.equal(authorized, 1, row.id);
    } else {
      assert.equal(preparation.prepared, null, row.id);
      assert.deepEqual({ code: preparation.diagnostic.code, path: preparation.diagnostic.path }, row.expected, row.id);
      assert.equal(authorized, 0, row.id);
    }
    assert.equal(dispatched, 0, row.id); authorized = 0;
  }
});

test('literal-only positive consumes exactly the same Java compiler artifact', async () => {
  const f = fixture('read');
  const bytes = readFileSync(new URL('artifact-literals.json', root));
  const pin = readFileSync(new URL('artifact-literals.sha256', root), 'utf8').trim();
  assert.equal(digest(bytes), pin);
  let dispatched = 0;
  const invokers = registrations(f);
  for (const row of Object.values(invokers)) row.invoke = () => { dispatched++; };
  const result = prepareProgram(bytes, bind(f, invokers), admission(f, { expectedArtifactDigest: pin }));
  assert.equal(result.diagnostic, null);
  const out = await invokeProgram(result.prepared, { taskId: 'task1' }, context('read', { bindings: {} }));
  assert.equal(out.classification, 'COMPLETED'); assert.deepEqual(out.result, { classification: 'DONE' });
  assert.equal(dispatched, 0);
});
