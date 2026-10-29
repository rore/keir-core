import { parentPort, workerData } from 'node:worker_threads';
import { prepare, input, evaluate } from './profile.mjs';
const prepared = prepare(workerData.bundle);
const vars = input(workerData.task, prepared.descriptor);
parentPort.postMessage({ ready: true });
parentPort.on('message', ({ id, iterations = 1 }) => {
  let value;
  // A bounded batch of real CEL calls measures supervision; it is not one CEL invocation.
  for (let i = 0; i < iterations; i++) value = evaluate(prepared, id, vars);
  parentPort.postMessage({ value, memory: process.memoryUsage() });
});
