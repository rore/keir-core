import test from 'node:test';
import assert from 'node:assert/strict';
import { prepare, invoke } from '../src/index.js';
import { effectBytes, effectArtifact, effectAdmission, effectAdapters, effectContext, effectInput, alteredEffect, keyType } from './effect-helpers.js';
import { digest } from '../src/canonical.js';
import { artifactBytes as readBytes, admission as readAdmission, adapters as readAdapters } from './helpers.js';

function rejected(bytes = effectBytes, admission = effectAdmission(), adapters = effectAdapters()) {
  let callbacks = 0;
  const counted = Object.fromEntries(Object.entries(adapters).map(([id, adapter]) => [id, {
    ...adapter, invoke: (...args) => { callbacks++; return adapter.invoke(...args); },
  }]));
  let validations = 0;
  const policy = { ...admission };
  if (typeof admission.keyBinding?.validate === 'function') {
    policy.keyBinding = { ...admission.keyBinding, validate: value => { validations++; return admission.keyBinding.validate(value); } };
  }
  const result = prepare(bytes, policy, counted);
  assert.equal(typeof result.code, 'string');
  assert.match(result.path, /^\/(artifact|admission)(\/|$)/);
  assert.equal(callbacks, 0);
  assert.equal(validations, 0);
  assert.deepEqual(Object.keys(result).sort(), ['code', 'message', 'path']);
  assert.equal(result.message.includes('Private opaque key'), false);
  return result;
}

test('admits the effect artifact without invoking host callbacks', () => {
  const admission = effectAdmission();
  let calls = 0, validations = 0;
  admission.keyBinding.validate = () => { validations++; throw new Error('private prepare validator'); };
  const adapters = Object.fromEntries(Object.entries(effectAdapters()).map(([id, adapter]) => [id, { ...adapter, invoke: () => { calls++; } }]));
  const result = prepare(effectBytes, admission, adapters);
  assert.equal(result.code, undefined);
  assert.equal(calls, 0);
  assert.equal(validations, 0);
  assert.equal(Object.isFrozen(result), true);
});

for (const type of ['String', 'Boolean', 'TaskId', 'FollowupInput']) test(`opaque key contract label ${type} stays outside the business type namespace`, async () => {
  const item = alteredEffect(artifact => { artifact.dcm.hostKey.type = type; });
  item.admission.keyBinding = { ...item.admission.keyBinding, type };
  const seen = [];
  const adapters = Object.fromEntries(Object.entries(effectAdapters()).map(([id, adapter]) => [id, {
    ...adapter, keyType: type, invoke: (...args) => { seen.push(args[3].requestKey); return adapter.invoke(...args); },
  }]));
  const handle = prepare(item.bytes, item.admission, adapters);
  assert.equal(handle.code, undefined);
  const context = effectContext();
  const outcome = await invoke(handle, effectInput, context);
  assert.equal(outcome.classification, 'COMPLETED');
  assert.deepEqual(outcome.result, { status: 'CREATED' });
  assert.equal(seen.length, 2);
  for (const key of seen) assert.equal(key, context.requestKey);
  assert.deepEqual(outcome.effects, [{ step: 'created', capability: 'tasks.createFollowUp', state: 'CONFIRMED' }]);
});

for (const [name, mutate] of [
  ['assurance', a => { a.dcm.assurance = 'CERTIFIED'; }],
  ['host key source', a => { a.dcm.hostKey.source = 'input'; }],
  ['empty host key type', a => { a.dcm.hostKey.type = ''; }],
  ['entrypoint trigger', a => { a.dcm.entrypoints[a.entrypoint].trigger = 'REQUEST'; }],
  ['entrypoint placement', a => { a.dcm.entrypoints[a.entrypoint].placement = 'BEFORE_COMMIT'; }],
  ['consumer kind', a => { a.dcm.entrypoints[a.entrypoint].consumer.kind = 'DISPLAY'; }],
  ['static effect grant', a => { a.dcm.entrypoints[a.entrypoint].effectGrant = 0; }],
  ['mixed artifact profile', a => { a.profile = 'keir-read/1'; }],
  ['mixed artifact format', a => { a.format = 'keir-artifact/read-1'; }],
]) test(`rejects invalid effect metadata: ${name}`, () => {
  const { bytes, admission } = alteredEffect(mutate);
  rejected(bytes, admission);
});

for (const [name, overrides] of [
  ['missing effect grant', { effectGrant: undefined }],
  ['widened effect grant', { effectGrant: 2 }],
  ['invalid effect grant', { effectGrant: '1' }],
  ['missing key binding', { keyBinding: undefined }],
  ['wrong key type', { keyBinding: { type: 'String', validate: () => true } }],
  ['nonfunction validator', { keyBinding: { type: keyType, validate: true } }],
  ['real mode', { mode: 'real' }],
]) test(`rejects invalid admission: ${name}`, () => {
  const policy = { ...effectAdmission(), ...overrides };
  if (Object.hasOwn(overrides, 'effectGrant') && overrides.effectGrant === undefined) delete policy.effectGrant;
  if (Object.hasOwn(overrides, 'keyBinding') && overrides.keyBinding === undefined) delete policy.keyBinding;
  rejected(effectBytes, policy);
});

for (const [name, mutate] of [
  ['unknown adapter', registrations => { registrations.unknown = { version: '1', mode: 'simulation', keyType, invoke() {} }; }],
  ['wrong adapter mode', registrations => { registrations['tasks.get'].mode = 'real'; }],
  ['wrong adapter key type', registrations => { registrations['tasks.get'].keyType = 'other'; }],
  ['wrong adapter version', registrations => { registrations['tasks.get'].version = '2'; }],
  ['adapter extra field', registrations => { registrations['tasks.get'].extra = true; }],
]) test(`rejects invalid adapter registration: ${name}`, () => {
  const registrations = effectAdapters(); mutate(registrations); rejected(effectBytes, effectAdmission(), registrations);
});

test('rejects a read-profile artifact containing effect-only fields', () => {
  const result = rejected(readBytes, { ...readAdmission(), effectGrant: 1, keyBinding: { type: keyType, validate: () => true } }, readAdapters());
  assert.equal(result.code, 'ADMISSION_INVALID'); assert.equal(result.path, '/admission');
});

for (const [name, mutate] of [
  ['future guard', step => { step.when = 'future'; }],
  ['CALL result guard', step => { step.when = 'source'; }],
  ['self guard', step => { step.when = 'created'; }],
  ['empty handlers', step => { step.onError = {}; }],
  ['undeclared handler', step => { step.onError.UNDECLARED = structuredClone(step.onError.CREATE_REJECTED); }],
  ['wrong handler output', step => { step.onError.CREATE_REJECTED.type = 'Task'; }],
  ['extra handler field', step => { step.onError.CREATE_REJECTED.fields.extra = step.onError.CREATE_REJECTED.fields.status; }],
  ['handler steps', step => { step.onError.CREATE_REJECTED.steps = []; }],
]) test(`rejects invalid guard/handler structure: ${name}`, () => {
  const item = alteredEffect(a => mutate(a.steps.find(step => step.capability === 'tasks.createFollowUp')));
  rejected(item.bytes, item.admission);
});

test('rejects unavailable guarded records even as unused environment declarations', () => {
  const item = alteredEffect(a => { a.expressions[a.return.fields.status.expr].variables.created = 'CreatedTask'; });
  rejected(item.bytes, item.admission);
});

test('null optional guard/handler fields fail closed even with an independently selected raw digest', () => {
  for (const field of ['when', 'onError']) {
    const artifact = structuredClone(effectArtifact);
    artifact.steps.find(step => step.capability === 'tasks.createFollowUp')[field] = null;
    const bytes = Buffer.from(JSON.stringify(artifact));
    rejected(bytes, effectAdmission({ expectedArtifactDigest: digest(bytes) }));
  }
});

test('a second guarded write exceeds the conservative ceiling even with a constant-false guard', () => {
  const item = alteredEffect(a => {
    const first = a.steps.find(step => step.capability === 'tasks.createFollowUp');
    const other = structuredClone(first); other.id = 'otherWrite';
    a.dcm.entrypoints[a.entrypoint].grants['tasks.createFollowUp'] = 2;
    const root = a.return.fields.status;
    const originalReturn = a.expressions[root.expr];
    let next = Number(root.expr.slice(1));
    for (const name of Object.keys(other.args).sort()) {
      const binding = other.args[name];
      if (Object.hasOwn(binding, 'expr')) {
        const original = a.expressions[binding.expr];
        binding.expr = 'e' + next++; a.expressions[binding.expr] = structuredClone(original);
      }
    }
    root.expr = 'e' + next; a.expressions[root.expr] = originalReturn;
    a.steps.push(other);
    const guard = a.steps.find(step => step.id === first.when);
    const removed = guard.value.expr;
    guard.value = { literal: false, type: 'Boolean' };
    const relabel = id => Number(id.slice(1)) > Number(removed.slice(1)) ? 'e' + (Number(id.slice(1)) - 1) : id;
    a.expressions = Object.fromEntries(Object.entries(a.expressions).filter(([id]) => id !== removed).map(([id, expression]) => [relabel(id), expression]));
    for (const step of a.steps) {
      const bindings = step.op === 'BIND' ? [step.value] : [...Object.values(step.args), ...Object.values(step.onError ?? {}).flatMap(handler => Object.values(handler.fields))];
      for (const binding of bindings) if (Object.hasOwn(binding, 'expr')) binding.expr = relabel(binding.expr);
    }
    root.expr = relabel(root.expr);
  });
  assert.equal(rejected(item.bytes, item.admission).code, 'BUDGET_EXCEEDED');
});
