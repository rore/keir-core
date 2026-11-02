import { bindModel, prepareProgram, invokeProgram, ENVIRONMENT } from './index.js';
import { canonicalBytes, parseCanonical, parseModelJson } from './canonical.js';
import { keys, shape, text, integer, validateValue, freeze, requireThat as ok } from './validation.js';

const own = (value, name) => Object.hasOwn(value, name);
const same = (left, right) => canonicalBytes(left).equals(canonicalBytes(right));
const token = value => ok(typeof value === 'string' && /^[A-Z][A-Z0-9_]{0,63}(?![\s\S])/.test(value), '');
const stepName = value => ok(typeof value === 'string' && /^[A-Za-z][A-Za-z0-9]{0,63}(?![\s\S])/.test(value), '');
function closed(value, required, optional = []) {
  const names = keys(value, '', required.length + optional.length);
  ok(required.every(name => own(value, name)) && names.every(name => required.includes(name) || optional.includes(name)), '');
}
function array(value, max, min = 0) {
  ok(Array.isArray(value) && Object.getPrototypeOf(value) === Array.prototype && value.length >= min && value.length <= max, '');
  ok(Reflect.ownKeys(value).length === value.length + 1, '');
  for (let i = 0; i < value.length; i++) {
    const field = Object.getOwnPropertyDescriptor(value, String(i));
    ok(field && own(field, 'value') && field.enumerable, '');
  }
}
function data(value, max = 8192) {
  const bytes = canonicalBytes(value); ok(bytes.length <= max, '');
  return freeze(JSON.parse(bytes));
}
function synchronous(value) {
  if (value instanceof Promise) {
    // Reject async fixture callbacks without leaving a rejected promise unobserved.
    Promise.prototype.then.call(value, () => {}, () => {});
    throw new Error('Synchronous fixture callback required');
  }
  return value;
}
function receipt(value) {
  keys(value, '', 2);
  closed(value, ['status'], value?.status === 'DISPLAYED' ? ['code'] : []);
  if (value.status === 'DISPLAYED') { ok(own(value, 'code'), ''); token(value.code); }
  else ok(value.status === 'NOT_DISPLAYED', '');
  return data(value);
}
function report(status, identity, mismatches, consumption) {
  const value = { format: 'keir-read-simulation-report/1', status, identity, mismatches };
  if (consumption !== undefined) value.consumption = consumption;
  ok(canonicalBytes(value).length <= 16384, '');
  return freeze(value);
}
const failed = (status, stage, identity = {}) => report(status, identity, [{ stage, code: status, ordinal: 0 }]);

function call(value, dcm, stub) {
  shape(value, ['step', 'capability', 'version', 'args', ...(stub ? ['response'] : [])], '');
  stepName(value.step);
  ok(typeof value.capability === 'string' && own(dcm.capabilities, value.capability), '');
  const cap = dcm.capabilities[value.capability];
  ok(cap.effect === 'READ' && value.version === cap.version, '');
  const names = Object.keys(cap.parameters).filter(name => cap.parameters[name].binding === 'agent');
  shape(value.args, names, '');
  for (const name of names) validateValue(dcm, cap.parameters[name].type, value.args[name], '', 'INPUT_INVALID');
  if (!stub) return;
  const response = value.response;
  closed(response, ['kind'], response?.kind === 'SUCCESS' ? ['value'] : response?.kind === 'BUSINESS_REJECTION' ? ['code'] : []);
  if (response.kind === 'SUCCESS') {
    ok(own(response, 'value'), ''); validateValue(dcm, cap.result, response.value, '', 'ADAPTER_INVALID');
  } else if (response.kind === 'BUSINESS_REJECTION') {
    ok(own(response, 'code') && cap.errors.includes(response.code), '');
  } else ok(response.kind === 'TRANSIENT_FAILURE', '');
}

function outcome(value, dcm, entry) {
  const completed = value?.classification === 'COMPLETED';
  ok(['COMPLETED', 'BUSINESS_REJECTION', 'FAILED', 'LIMIT_EXCEEDED'].includes(value?.classification), '');
  closed(value, ['classification', 'trace', completed ? 'result' : 'error'],
    value.classification === 'BUSINESS_REJECTION' ? ['result'] : []);
  if (own(value, 'result')) validateValue(dcm, entry.output, value.result, '', 'INPUT_INVALID');
  if (!completed) {
    closed(value.error, ['code', 'path'], ['cause']); token(value.error.code); text(value.error.path, '', false, 1024);
    if (own(value.error, 'cause')) {
      shape(value.error.cause, ['code', 'path'], ''); token(value.error.cause.code); text(value.error.cause.path, '', false, 1024);
    }
  }
  array(value.trace, 64, 2);
  const fields = { INVOCATION_START: [], STEP_START: ['step'], VALUE_BOUND: ['step'], CALL_DISPATCHED: ['step', 'capability'],
    CALL_SUCCEEDED: ['step', 'capability'], GUARD_EVALUATED: ['step', 'taken'], STEP_SKIPPED: ['step'], RETURNED: [], TERMINATED: [] };
  value.trace.forEach((event, sequence) => {
    ok(event && typeof event.event === 'string' && own(fields, event.event), '');
    closed(event, ['sequence', 'event', ...fields[event.event]], event.event === 'TERMINATED' ? ['code'] : []);
    ok(event.sequence === sequence, '');
    if (own(event, 'step')) stepName(event.step);
    if (own(event, 'capability')) ok(typeof event.capability === 'string' && own(dcm.capabilities, event.capability), '');
    if (own(event, 'taken')) ok(typeof event.taken === 'boolean', '');
    if (own(event, 'code')) token(event.code);
    if (event.event === 'INVOCATION_START') ok(sequence === 0, '');
    if (event.event === 'TERMINATED') ok(sequence === value.trace.length - 1, '');
  });
  ok(value.trace[0].event === 'INVOCATION_START' && value.trace.at(-1).event === 'TERMINATED', '');
  ok(completed ? !own(value.trace.at(-1), 'code') : value.trace.at(-1).code === value.error.code, '');
}

export async function runReadSimulation(selection, fixture, caseBytes) {
  const mismatches = [], captures = [];
  const add = (stage, code, ordinal = 0) => { if (mismatches.length < 8) mismatches.push({ stage, code, ordinal }); };
  let row, used = 0, dcm, entry, prepared, identity, bindings, deadline, authorize, consume, responseCheck, allowed;
  try {
    shape(selection, ['dcmBytes', 'artifactBytes', 'modelDigest', 'artifactDigest', 'compiler', 'entrypoint', 'consumer'], '');
    closed(fixture, ['scope', 'bindings', 'deadline', 'authorize', 'consume', 'allowedReceipts'], ['responseCheck']);
    for (const field of ['modelDigest', 'artifactDigest']) ok(typeof selection[field] === 'string' && /^sha256:[0-9a-f]{64}(?![\s\S])/.test(selection[field]), '');
    text(selection.compiler, '', true); text(selection.entrypoint, '', true); text(fixture.scope, '', true);
    ok(typeof fixture.authorize === 'function' && typeof fixture.consume === 'function', '');
    if (own(fixture, 'responseCheck')) ok(typeof fixture.responseCheck === 'function', '');
    authorize = fixture.authorize; consume = fixture.consume; responseCheck = fixture.responseCheck;
    shape(fixture.deadline, ['clock', 'expiresAtMs'], ''); shape(fixture.deadline.clock, ['nowMs'], '');
    ok(typeof fixture.deadline.clock.nowMs === 'function', ''); integer(fixture.deadline.expiresAtMs, Number.MAX_SAFE_INTEGER, '', 0);
    deadline = { clock: { nowMs: fixture.deadline.clock.nowMs.bind(fixture.deadline.clock) }, expiresAtMs: fixture.deadline.expiresAtMs };
    bindings = data(fixture.bindings, 1048576);
    array(fixture.allowedReceipts, 16, 1); allowed = fixture.allowedReceipts.map(receipt);
    ok(new Set(allowed.map(value => canonicalBytes(value).toString('utf8'))).size === allowed.length, '');
    ok(selection.dcmBytes instanceof Uint8Array && selection.dcmBytes.byteLength <= 65536, '');
    ok(selection.artifactBytes instanceof Uint8Array && selection.artifactBytes.byteLength <= 1048576, '');
    const dcmBytes = Buffer.from(selection.dcmBytes), artifactBytes = Buffer.from(selection.artifactBytes);
    dcm = parseModelJson(dcmBytes); ok(dcm.format === 'keir-dcm/read-2', '');
    const invokers = Object.fromEntries(Object.entries(dcm.capabilities).map(([capability, cap]) => [capability, {
      version: cap.version, mode: 'simulation',
      invoke(args, protectedArgs, remainingMs) {
        const ordinal = captures.length + 1;
        const captured = { capability, version: cap.version, args };
        captures.push(captured);
        const stub = row.stubs[used];
        if (!stub) { add('STUB', 'MISSING_STUB', ordinal); throw new Error('Simulation mismatch'); }
        try {
          ok(cap.effect === 'READ' && Number.isSafeInteger(remainingMs) && remainingMs > 0, '');
          const expectedProtected = Object.fromEntries(Object.entries(cap.parameters).filter(([, param]) => param.binding === 'host').map(([name]) => [name, bindings[name]]));
          ok(same(captured, { capability: stub.capability, version: stub.version, args: stub.args }) && same(protectedArgs, expectedProtected), '');
          if (responseCheck) ok(synchronous(responseCheck(capability, args, stub.response)) === true, '');
        } catch { add('STUB', 'STUB_MISMATCH', ordinal); throw new Error('Simulation mismatch'); }
        used++;
        return stub.response;
      },
    }]));
    const binding = bindModel(dcmBytes, { scope: fixture.scope, modelRef: { domainId: dcm.domain.id, domainVersion: dcm.domain.version,
      format: dcm.format, digest: selection.modelDigest, descriptorRecipe: 'keir-record-descriptors/1' },
    profile: 'keir-read/2', environment: ENVIRONMENT, mode: 'simulation' }, invokers);
    ok(binding.diagnostic === null, '');
    const preparation = prepareProgram(artifactBytes, binding.boundModel, { expectedArtifactDigest: selection.artifactDigest,
      expectedDcmDigest: selection.modelDigest, compiler: selection.compiler, entrypoint: selection.entrypoint,
      consumer: selection.consumer, environment: ENVIRONMENT, mode: 'simulation' });
    ok(preparation.diagnostic === null, ''); prepared = preparation.prepared;
    const artifact = parseCanonical(artifactBytes), hostTypes = new Map();
    for (const step of artifact.steps) if (step.op === 'CALL') for (const [name, param] of Object.entries(dcm.capabilities[step.capability].parameters)) if (param.binding === 'host') {
      if (!hostTypes.has(name)) hostTypes.set(name, []); hostTypes.get(name).push(param.type);
    }
    shape(bindings, [...hostTypes.keys()], '');
    for (const [name, types] of hostTypes) for (const type of types) validateValue(dcm, type, bindings[name], '', 'HOST_BINDING_INVALID');
    entry = dcm.entrypoints[selection.entrypoint];
    identity = { modelDigest: selection.modelDigest, artifactDigest: selection.artifactDigest };
    fixture = { scope: fixture.scope };
  } catch { return failed('SETUP_ERROR', 'SETUP'); }
  try {
    ok(caseBytes instanceof Uint8Array && caseBytes.byteLength <= 65536, '');
    row = parseModelJson(Buffer.from(caseBytes));
    shape(row, ['format', 'id', 'input', 'stubs', 'expect'], ''); ok(row.format === 'keir-read-simulation/1', '');
    ok(typeof row.id === 'string' && /^[A-Za-z][A-Za-z0-9_.-]{0,63}(?![\s\S])/.test(row.id), '');
    validateValue(dcm, entry.input, row.input, '', 'INPUT_INVALID');
    array(row.stubs, 4); row.stubs.forEach(value => call(value, dcm, true));
    shape(row.expect, ['calls', 'outcome', 'consumption'], ''); array(row.expect.calls, 4);
    row.expect.calls.forEach(value => call(value, dcm, false)); outcome(row.expect.outcome, dcm, entry);
    const expectedReceipt = receipt(row.expect.consumption); ok(allowed.some(value => same(value, expectedReceipt)), '');
    freeze(row);
  } catch { return failed('INVALID_SCENARIO', 'SCENARIO', identity); }
  let actual;
  try {
    actual = await invokeProgram(prepared, row.input, { scope: fixture.scope, bindings, deadline, authorize });
    outcome(actual, dcm, entry);
  } catch { return failed('SETUP_ERROR', 'SETUP'); }
  if (used !== row.stubs.length) add('STUB', 'EXTRA_STUB', used + 1);
  const dispatches = actual.trace.filter(event => event.event === 'CALL_DISPATCHED');
  if (dispatches.length !== captures.length) add('CALLS', 'TRACE_MISMATCH');
  const calls = captures.map((capture, index) => {
    const event = dispatches[index], stub = row.stubs[index];
    if (!event || event.capability !== capture.capability || !stub || event.step !== stub.step) add('CALLS', 'TRACE_MISMATCH', index + 1);
    return { step: event?.step ?? '', ...capture };
  });
  if (!same(calls, row.expect.calls)) add('CALLS', 'CALLS_MISMATCH');
  if (!same(actual, row.expect.outcome)) add('OUTCOME', 'OUTCOME_MISMATCH');
  let consumption;
  try {
    consumption = receipt(synchronous(consume(actual)));
    ok(allowed.some(value => same(value, consumption)), '');
  } catch { add('CONSUMER', 'CONSUMER_ERROR'); return report('CONSUMER_ERROR', identity, mismatches); }
  if (!same(consumption, row.expect.consumption)) add('CONSUMER', 'CONSUMPTION_MISMATCH');
  return report(mismatches.length ? 'MISMATCH' : 'PASS', identity, mismatches, consumption);
}
