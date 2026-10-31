import test from 'node:test';
import assert from 'node:assert/strict';
import { bindModel, prepareProgram, invokeProgram } from '../src/index.js';
import { canonicalBytes, digest } from '../src/canonical.js';
import { fixture, approval, admission, registrations, bind, prepared, context } from './reference-helpers.js';

function rejected(f, artifact, extra, code, path, model = bind(f)) {
  const bytes = canonicalBytes(artifact);
  const result = prepareProgram(bytes, model, admission(f, { expectedArtifactDigest: digest(bytes), ...extra }));
  assert.equal(result.prepared, null);
  assert.deepEqual({ code: result.diagnostic.code, path: result.diagnostic.path }, { code, path });
}

test('known-call admission ceilings precede malformed binding metadata in both reference profiles', () => {
  for (const profile of ['read', 'effect']) {
    const f = fixture(profile), artifact = structuredClone(f.artifact);
    const call = artifact.steps.find(step => step.op === 'CALL' && (profile === 'read' || step.capability === 'tasks.create'));
    for (const arg of Object.keys(call.args)) call.args[arg] = {};
    const denied = { capabilityPolicy: { maxCalls: { [call.capability]: 0 } } };
    rejected(f, artifact, {}, 'ARTIFACT_INVALID', profile === 'read' ? '/artifact/steps/0/args/taskId' : '/artifact/steps/3/args/projectId');
    rejected(f, artifact, { ...denied, limits: { calls: profile === 'read' ? 0 : 1 }, ...(profile === 'effect' ? { effectGrant: 0 } : {}) }, 'BUDGET_EXCEEDED', '/artifact/steps');
    rejected(f, artifact, denied, 'AUTHORITY_DENIED', '/admission/capabilityPolicy');
    rejected(f, artifact, { limits: { steps: 0 } }, 'BUDGET_EXCEEDED', '/artifact/steps');
    if (profile === 'effect') rejected(f, artifact, { ...denied, effectGrant: 0 }, 'AUTHORITY_DENIED', '/admission/effectGrant');
  }
});

test('known-call safety and hook policy precede bindings and onError metadata', () => {
  const f = fixture('effect'), artifact = structuredClone(f.artifact);
  const write = artifact.steps.at(-1);
  write.onError = { CREATE_REJECTED: {} };
  rejected(f, artifact, { effectGrant: 0 }, 'AUTHORITY_DENIED', '/admission/effectGrant');
  artifact.steps.push({ ...structuredClone(write), id: 'anotherWrite', args: { projectId: {}, title: {} } });
  delete write.onError;
  rejected(f, artifact, {}, 'BUDGET_EXCEEDED', '/artifact/steps/4');

  const read = fixture('read'), deniedDcm = structuredClone(read.dcm);
  deniedDcm.entrypoints.ClassifyTask.capabilityPolicy = { deny: ['tasks.get'] };
  const dcmBytes = canonicalBytes(deniedDcm), dcmPin = digest(dcmBytes);
  const selected = { ...read, dcm: deniedDcm, dcmBytes, dcmPin };
  const bad = structuredClone(read.artifact); bad.modelRef = approval(selected).modelRef; bad.steps[0].args.taskId = {};
  rejected(selected, bad, {}, 'ENTRY_GRANT_DENIED', '/artifact/steps/0');

  const writeOnRead = structuredClone(read.artifact);
  Object.assign(writeOnRead.steps[0], { capability: 'tasks.create', args: { projectId: {}, title: {} } });
  rejected(read, writeOnRead, {}, 'ENTRY_GRANT_DENIED', '/artifact/steps/0');

  const tooMany = structuredClone(read.artifact);
  tooMany.steps = Array.from({ length: 5 }, (_, i) => ({ ...structuredClone(read.artifact.steps[0]), id: `call${i}`, args: { taskId: i === 4 ? {} : { type: 'TaskId', literal: 'task1' } } }));
  rejected(read, tooMany, {}, 'BUDGET_EXCEEDED', '/artifact/steps/4');
});

test('read policies may name unused catalog WRITEs at hook, admission and invocation', async () => {
  const f = fixture('read');
  for (const policy of [{ allow: ['tasks.get', 'tasks.create'] }, { maxCalls: { 'tasks.create': 4 } }]) {
    const changed = structuredClone(f.dcm); changed.entrypoints.ClassifyTask.capabilityPolicy = policy;
    const dcmBytes = canonicalBytes(changed), selected = { ...f, dcm: changed, dcmPin: digest(dcmBytes) };
    const result = bindModel(dcmBytes, approval(selected), registrations(f));
    assert.equal(result.diagnostic, null); assert.ok(result.boundModel);
    const handle = prepared(f, undefined, { capabilityPolicy: policy });
    const out = await invokeProgram(handle, { taskId: 'task1' }, context('read', { capabilityPolicy: policy }));
    assert.equal(out.classification, 'COMPLETED');
    assert.deepEqual(out.result, { classification: 'DONE' });
  }
});

test('invalid invocation policy precedes invalid input and bindings without callbacks', async () => {
  for (const profile of ['read', 'effect']) {
    const f = fixture(profile), handle = prepared(f);
    for (const capabilityPolicy of [null, undefined, { allow: ['unknown'] }, { maxCalls: { 'tasks.get': 5 } }]) {
      for (const [input, bindings] of [[{}, { actorId: 'user1' }], [{ taskId: 'task1' }, {}], [{}, {}]]) {
        let callbacks = 0;
        const out = await invokeProgram(handle, input, context(profile, { capabilityPolicy, bindings, authorize: () => { callbacks++; return true; } }));
        assert.deepEqual(out.error, { code: 'HOST_CONTEXT_INVALID', path: '' });
        assert.equal(callbacks, 0);
        assert.deepEqual(out.effects ?? [], []);
      }
    }
  }
});
