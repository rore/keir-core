import { types as nodeTypes } from 'node:util';

const own = (value, key) => Object.hasOwn(value, key);
const requireValid = condition => { if (!condition) throw new Error('Invalid decision outcome'); };
const bounded = value => typeof value === 'string' && value.length <= 256
  && value.isWellFormed() && Buffer.byteLength(value) <= 256;

function record(value, required, optional = []) {
  requireValid(value !== null && typeof value === 'object' && !nodeTypes.isProxy(value)
    && [Object.prototype, null].includes(Object.getPrototypeOf(value)));
  const keys = Reflect.ownKeys(value);
  requireValid(required.every(key => own(value, key))
    && keys.every(key => required.includes(key) || optional.includes(key)));
  return Object.fromEntries(keys.map(key => {
    const descriptor = Object.getOwnPropertyDescriptor(value, key);
    requireValid(descriptor.enumerable && own(descriptor, 'value'));
    return [key, descriptor.value];
  }));
}

function array(value, max) {
  requireValid(Array.isArray(value) && !nodeTypes.isProxy(value) && Object.getPrototypeOf(value) === Array.prototype
    && value.length <= max);
  const descriptors = Object.getOwnPropertyDescriptors(value);
  const length = descriptors.length.value;
  requireValid(length <= max && Reflect.ownKeys(descriptors).length === length + 1);
  return Array.from({ length }, (_, index) => {
    const descriptor = descriptors[index];
    requireValid(descriptor?.enumerable && own(descriptor, 'value'));
    return descriptor.value;
  });
}

function validateTrace(value) {
  const events = array(value, 64);
  let index = 0, calls = 0;
  const take = (name, fields = []) => {
    const event = record(events[index], ['sequence', 'event', ...fields]);
    requireValid(event.sequence === index && !Object.is(event.sequence, -0) && event.event === name);
    index++;
    return event;
  };
  const next = () => index < events.length ? record(events[index], ['sequence', 'event'], ['step', 'capability', 'taken']).event : undefined;
  const steps = new Set();
  take('INVOCATION_START');
  while (next() === 'STEP_START') {
    const { step } = take('STEP_START', ['step']);
    requireValid(typeof step === 'string' && /^[A-Za-z][A-Za-z0-9]{0,63}$/.test(step) && !steps.has(step) && steps.size < 8);
    steps.add(step);
    const sameStep = (name, fields = []) => {
      const event = take(name, ['step', ...fields]);
      requireValid(event.step === step);
      return event;
    };
    if (next() === 'VALUE_BOUND') {
      sameStep('VALUE_BOUND');
      continue;
    }
    let taken = true;
    if (next() === 'GUARD_EVALUATED') {
      ({ taken } = sameStep('GUARD_EVALUATED', ['taken']));
      requireValid(typeof taken === 'boolean');
    }
    if (!taken) {
      sameStep('STEP_SKIPPED');
      continue;
    }
    const { capability } = sameStep('CALL_DISPATCHED', ['capability']);
    requireValid(typeof capability === 'string' && /^[A-Za-z][A-Za-z0-9.]{0,99}$/.test(capability) && ++calls <= 4);
    requireValid(sameStep('CALL_SUCCEEDED', ['capability']).capability === capability);
  }
  requireValid(steps.size > 0);
  take('RETURNED');
  take('TERMINATED');
  requireValid(index === events.length);
}

// This host knows its selected model's closed decision/message output at setup.
export function consumeDecision(outcome) {
  try {
    const snapshot = record(outcome, ['classification', 'result', 'effects', 'trace']);
    requireValid(snapshot.classification === 'COMPLETED');
    const result = record(snapshot.result, ['decision'], ['message']);
    requireValid(result.decision === 'ALLOW' || result.decision === 'DENY');
    requireValid(!own(result, 'message') || bounded(result.message));
    array(snapshot.effects, 0);
    validateTrace(snapshot.trace);
    return { status: result.decision === 'ALLOW' ? 'ALLOWED' : 'DENIED', result };
  } catch {
    return { status: 'BLOCKED' };
  }
}
