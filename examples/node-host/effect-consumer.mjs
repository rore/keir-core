const classifications = new Set(['COMPLETED', 'BUSINESS_REJECTION', 'FAILED', 'LIMIT_EXCEEDED', 'UNCERTAIN']);
const resultStatuses = new Set(['CREATED', 'NO_OP', 'SOURCE_MISSING', 'REJECTED']);
const effectStates = new Set(['CONFIRMED', 'NONE', 'UNKNOWN']);
const traceFields = {
  INVOCATION_START: [], STEP_START: ['step'], GUARD_EVALUATED: ['step', 'taken'], STEP_SKIPPED: ['step'], VALUE_BOUND: ['step'],
  EFFECT_INTENT: ['step', 'capability'], CALL_DISPATCHED: ['step', 'capability'], EFFECT_RECORDED: ['step', 'capability', 'state'],
  CALL_SUCCEEDED: ['step', 'capability'], RETURNED: [], TERMINATED: [],
};
const own = (value, key) => Object.hasOwn(value, key);
const record = (value, required, optional = []) => value !== null && typeof value === 'object' && !Array.isArray(value)
  && required.every(key => own(value, key)) && Object.keys(value).every(key => required.includes(key) || optional.includes(key));
const bounded = (value, max = 256) => typeof value === 'string' && value.length > 0 && value.isWellFormed() && Buffer.byteLength(value) <= max;
const errorShape = (value, causeAllowed = true) => record(value, ['code', 'path'], causeAllowed ? ['cause'] : [])
  && bounded(value.code) && typeof value.path === 'string' && value.path.isWellFormed() && Buffer.byteLength(value.path) <= 1024
  && (!own(value, 'cause') || errorShape(value.cause, false));
const resultShape = value => record(value, ['status']) && resultStatuses.has(value.status);
const effectShape = value => record(value, ['step', 'capability', 'state']) && bounded(value.step) && bounded(value.capability) && effectStates.has(value.state);
const traceShape = value => {
  if (!Array.isArray(value) || value.length < 2 || value.length > 64 || value[0]?.event !== 'INVOCATION_START' || value.at(-1)?.event !== 'TERMINATED') return false;
  return value.every((event, index) => {
    const attributes = typeof event?.event === 'string' && Object.hasOwn(traceFields, event.event) ? traceFields[event.event] : undefined;
    if (!attributes || !record(event, ['sequence', 'event', ...attributes], event.event === 'TERMINATED' ? ['code'] : []) || event.sequence !== index) return false;
    if (event.event === 'INVOCATION_START' && index !== 0 || event.event === 'TERMINATED' && index !== value.length - 1) return false;
    if (own(event, 'step') && !bounded(event.step)) return false;
    if (own(event, 'capability') && !bounded(event.capability)) return false;
    if (own(event, 'state') && !effectStates.has(event.state)) return false;
    if (own(event, 'taken') && typeof event.taken !== 'boolean') return false;
    if (own(event, 'code') && (!bounded(event.code) || event.event !== 'TERMINATED')) return false;
    return true;
  });
};

const invalid = () => ({ status: 'NOT_REPORTED', code: 'OUTCOME_INVALID' });

function priorConfirmation(effect, outcome) {
  let reached = 0, confirmed = -1, lastStep, lastDispatch;
  for (const event of outcome.trace) {
    if (event.event === 'STEP_START') { reached++; lastStep = event; }
    if (event.event === 'EFFECT_RECORDED' && event.step === effect.step && event.capability === effect.capability && event.state === 'CONFIRMED') confirmed = event.sequence;
    if (event.event === 'CALL_DISPATCHED') lastDispatch = event;
  }
  return confirmed >= 0 && confirmed < lastDispatch?.sequence && outcome.error.path === '/steps/' + (reached - 1)
    && lastDispatch.step === lastStep?.step && lastDispatch.step !== effect.step && lastDispatch.capability !== effect.capability;
}

export function consumeEffect(outcome) {
  if (!record(outcome, ['classification', 'effects', 'trace'], ['result', 'error'])
      || !classifications.has(outcome.classification) || !Array.isArray(outcome.effects)
      || outcome.effects.length > 1 || !outcome.effects.every(effectShape) || !traceShape(outcome.trace)) return invalid();
  const hasResult = own(outcome, 'result'), hasError = own(outcome, 'error');
  if ((hasResult && !resultShape(outcome.result)) || (hasError && !errorShape(outcome.error, ['FAILED', 'LIMIT_EXCEEDED'].includes(outcome.classification)))) return invalid();
  if (hasError && own(outcome.error, 'cause')) {
    const cause = outcome.error.cause;
    const handler = cause.path + '/onError/' + cause.code.replaceAll('~', '~0').replaceAll('/', '~1');
    if (!/^\/steps\/[0-7]$/.test(cause.path) || outcome.error.path !== handler && !outcome.error.path.startsWith(handler + '/')) return invalid();
  }
  if (hasError ? outcome.trace.at(-1).code !== outcome.error.code : own(outcome.trace.at(-1), 'code')) return invalid();
  const { classification, effects } = outcome;
  if (classification === 'COMPLETED' && (!hasResult || hasError || effects.some(effect => effect.state !== 'CONFIRMED'))) return invalid();
  if (classification === 'BUSINESS_REJECTION' && (!hasError || effects.some(effect => effect.state === 'UNKNOWN' || effect.state === 'CONFIRMED' && !priorConfirmation(effect, outcome)))) return invalid();
  if (['FAILED', 'LIMIT_EXCEEDED'].includes(classification) && (!hasError || hasResult || effects.some(effect => effect.state === 'UNKNOWN'))) return invalid();
  if (classification === 'UNCERTAIN' && (!hasError || hasResult || effects.length !== 1 || effects[0].state !== 'UNKNOWN')) return invalid();
  return {
    status: 'REPORTED', classification,
    ...(hasResult ? { result: outcome.result } : {}),
    ...(hasError ? { error: outcome.error } : {}),
    effects
  };
}
