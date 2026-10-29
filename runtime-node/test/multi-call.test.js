import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { canonicalBytes, digest } from '../src/canonical.js';
import { prepare, invoke } from '../src/index.js';
import { admission, adapters, context } from './helpers.js';

const fixture = new URL('../../conformance/read-and-decide/multi-call/', import.meta.url);
const bytes = readFileSync(new URL('artifact.json', fixture));
const expectedArtifactDigest = readFileSync(new URL('artifact.sha256', fixture), 'utf8').trim();
const expectedDcmDigest = readFileSync(new URL('dcm.sha256', fixture), 'utf8').trim();
const invocationRequest = { entrypoint: 'ClassifyTask' };
const callRequest = { entrypoint: 'ClassifyTask', capability: 'tasks.get', version: '1' };
const firstReadTrace = [
  { sequence: 0, event: 'INVOCATION_START' },
  { sequence: 1, event: 'STEP_START', step: 'first' },
  { sequence: 2, event: 'CALL_DISPATCHED', step: 'first', capability: 'tasks.get' },
  { sequence: 3, event: 'CALL_SUCCEEDED', step: 'first', capability: 'tasks.get' },
];

test('two-call Java artifact matches its independently held artifact and model pins', () => {
  const artifact = JSON.parse(bytes);
  const model = JSON.parse(readFileSync(new URL('dcm.json', fixture)));
  assert.equal(digest(bytes), expectedArtifactDigest);
  assert.equal(digest(canonicalBytes(model)), expectedDcmDigest);
  assert.equal(artifact.dcmDigest, expectedDcmDigest);
  assert.deepEqual(artifact.dcm, model);
  assert.equal(model.entrypoints.ClassifyTask.grants['tasks.get'], 2);
  assert.deepEqual(artifact.steps.map(({ id, op, capability }) => ({ id, op, capability })), [
    { id: 'first', op: 'CALL', capability: 'tasks.get' },
    { id: 'second', op: 'CALL', capability: 'tasks.get' },
  ]);
});

test('fresh grants exhaust before second-call authority, reset on reuse, and stop after first-read failure', async () => {
  const calls = [], requests = [];
  let responses = [{ kind: 'SUCCESS', value: { id: 'task1', status: 'COMPLETED' } }];
  const handle = prepare(bytes, admission({ expectedArtifactDigest, expectedDcmDigest, grants: { 'tasks.get': 2 } }, bytes),
    adapters(async (agentArgs, protectedArgs, remainingMs) => {
      calls.push({ agentArgs: { ...agentArgs }, protectedArgs: { ...protectedArgs }, remainingMs });
      return responses.shift();
    }));
  assert.equal(handle.code, undefined);
  const run = (taskId, actorId, count) => invoke(handle, { taskId }, context({
    bindings: { actorId }, grants: { 'tasks.get': count },
    deadline: { clock: { nowMs: () => 0 }, expiresAtMs: 10000 },
    authorize: async request => { requests.push({ ...request }); return true; },
  }));

  assert.deepEqual(await run('task1', 'user1', 1), {
    classification: 'FAILED', error: { code: 'AUTHORITY_DENIED', path: '/steps/1' },
    trace: [...firstReadTrace,
      { sequence: 4, event: 'STEP_START', step: 'second' },
      { sequence: 5, event: 'TERMINATED', code: 'AUTHORITY_DENIED' },
    ],
  });
  assert.deepEqual(calls, [{ agentArgs: { taskId: 'task1' }, protectedArgs: { actorId: 'user1' }, remainingMs: 10000 }]);
  assert.deepEqual(requests, [invocationRequest, callRequest]);
  assert.deepEqual(responses, []);

  calls.length = 0; requests.length = 0;
  responses = [
    { kind: 'SUCCESS', value: { id: 'task2', status: 'COMPLETED' } },
    { kind: 'SUCCESS', value: { id: 'task2', status: 'OPEN' } },
  ];
  assert.deepEqual(await run('task2', 'user2', 2), {
    classification: 'COMPLETED', result: { classification: 'PENDING' },
    trace: [...firstReadTrace,
      { sequence: 4, event: 'STEP_START', step: 'second' },
      { sequence: 5, event: 'CALL_DISPATCHED', step: 'second', capability: 'tasks.get' },
      { sequence: 6, event: 'CALL_SUCCEEDED', step: 'second', capability: 'tasks.get' },
      { sequence: 7, event: 'RETURNED' },
      { sequence: 8, event: 'TERMINATED' },
    ],
  });
  assert.deepEqual(calls, [
    { agentArgs: { taskId: 'task2' }, protectedArgs: { actorId: 'user2' }, remainingMs: 10000 },
    { agentArgs: { taskId: 'task2' }, protectedArgs: { actorId: 'user2' }, remainingMs: 10000 },
  ]);
  assert.deepEqual(requests, [invocationRequest, callRequest, callRequest]);
  assert.deepEqual(responses, []);

  calls.length = 0; requests.length = 0;
  responses = [{ kind: 'BUSINESS_REJECTION', code: 'TASK_NOT_FOUND' }];
  assert.deepEqual(await run('task3', 'user3', 2), {
    classification: 'BUSINESS_REJECTION', error: { code: 'TASK_NOT_FOUND', path: '/steps/0' },
    trace: [
      { sequence: 0, event: 'INVOCATION_START' },
      { sequence: 1, event: 'STEP_START', step: 'first' },
      { sequence: 2, event: 'CALL_DISPATCHED', step: 'first', capability: 'tasks.get' },
      { sequence: 3, event: 'TERMINATED', code: 'TASK_NOT_FOUND' },
    ],
  });
  assert.deepEqual(calls, [{ agentArgs: { taskId: 'task3' }, protectedArgs: { actorId: 'user3' }, remainingMs: 10000 }]);
  assert.deepEqual(requests, [invocationRequest, callRequest]);
  assert.deepEqual(responses, []);
});
