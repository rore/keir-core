import { createHash, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram, invokeProgram } from '@keir/runtime-node';

if (process.argv.length > 3) throw new Error('Usage: node examples/node-host/follow-up/run.mjs [artifact.json]');

const dcm = readFileSync(new URL('./dcm.json', import.meta.url));
const fixture = new URL('../../../conformance/keir-2/', import.meta.url);
const artifact = readFileSync(process.argv[2] ?? new URL('artifact.json', fixture));
if (process.argv[2] === undefined) {
  const expected = readFileSync(new URL('artifact.sha256', fixture), 'utf8').trim();
  const actual = 'sha256:' + createHash('sha256').update(artifact).digest('hex');
  if (actual !== expected) throw new Error('Pinned example artifact digest mismatch');
}
const tasks = {
  task1: { id: 'task1', projectId: 'project1', status: 'COMPLETED' },
  task2: { id: 'task2', projectId: 'project2', status: 'COMPLETED' },
  task3: { id: 'task3', projectId: 'project1', status: 'COMPLETED' }
};
const registration = { version: '1', mode: 'simulation', keyType: 'sample.RequestUuid/1' };
let createdTitle;

const { boundModel, diagnostic: bindError } = bindModel(dcm, {
  'tasks.get': {
    ...registration,
    invoke: ({ taskId }) => Object.hasOwn(tasks, taskId)
      ? { kind: 'SUCCESS', value: tasks[taskId] }
      : { kind: 'BUSINESS_REJECTION', code: 'TASK_NOT_FOUND' }
  },
  'tasks.create': {
    ...registration,
    invoke: ({ title }, _protected, _remainingMs, { event }) => {
      if (event.source === 'task3') return { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' };
      createdTitle = title;
      return { kind: 'SUCCESS', value: { id: 'follow-up' }, effect: 'CONFIRMED' };
    }
  }
});
if (bindError) throw new Error(bindError.code);
const { prepared, diagnostic: prepareError } = prepareProgram(artifact, boundModel);
if (prepareError) throw new Error(prepareError.code);

const clock = { nowMs: () => Math.floor(performance.now()) };
for (const taskId of Object.keys(tasks)) {
  createdTitle = undefined;
  const outcome = await invokeProgram(prepared, { taskId }, {
    bindings: { actorId: 'demo-user' },
    deadline: { clock, expiresAtMs: clock.nowMs() + 1000 },
    requestKey: randomUUID(),
    event: { id: `completion-${taskId}`, source: taskId }
  });
  const receipt = outcome.effects[0]?.state ?? '-';
  if (outcome.classification === 'UNCERTAIN') {
    console.log(`${taskId} UNCERTAIN ${receipt}`);
    continue;
  }
  if (outcome.classification !== 'COMPLETED') throw new Error(outcome.error.code);
  const title = createdTitle === undefined ? '' : ` | ${createdTitle}`;
  console.log(`${taskId} ${outcome.result.status} ${receipt}${title}`);
}
