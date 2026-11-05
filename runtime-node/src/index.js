import { parseCanonical } from './canonical.js';
import { prepareExpressions, evaluateBinding } from './cel.js';
import { ENVIRONMENT, LIMITS, Fault, requireThat as ok, keys, shape, freeze, pointer, validateEnvelope, validatePlan, validateValue } from './validation.js';
import { modelFor } from './model.js';

export { ENVIRONMENT };
export { bindModel } from './model.js';
const handles = new WeakMap();
const diagnostic = (code, path) => Object.freeze({ code, path, message: 'The selected artifact or host configuration is invalid.' });

export function prepareProgram(artifactBytes, boundModel) {
  let path = '/binding', code = 'ADMISSION_INVALID';
  try {
    const bound = modelFor(boundModel); ok(bound, path, code);
    path = '/admission';
    ok(arguments.length === 2, path, code);
    path = '/artifact'; code = 'ARTIFACT_INVALID';
    ok(artifactBytes instanceof Uint8Array, path, code);
    ok(artifactBytes.byteLength <= LIMITS.artifactBytes, path, 'BUDGET_EXCEEDED');
    const artifact = parseCanonical(Buffer.from(artifactBytes));
    validateEnvelope(artifact, bound);
    const { contexts, calls, entrypoint, policy, effectGrant } = validatePlan(artifact, bound);
    const cel = prepareExpressions(contexts, bound);
    ok(cel.totalNodes <= LIMITS.totalAstNodes, '/artifact/expressions', 'BUDGET_EXCEEDED');
    const hostFields = Object.create(null);
    for (const id of Object.keys(calls)) for (const [name, parameter] of Object.entries(bound.dcm.capabilities[id].parameters)) if (parameter.binding === 'host') {
      if (!Object.hasOwn(hostFields, name)) hostFields[name] = [];
      hostFields[name].push(parameter.type);
    }
    const prepared = Object.freeze(Object.create(null));
    handles.set(prepared, { artifact: freeze(artifact), dcm: bound.dcm, entrypoint, grants: freeze(policy), cel,
      adapters: bound.adapters, hostFields, effectGrant });
    return Object.freeze({ prepared, diagnostic: null });
  } catch (error) {
    const fault = error instanceof Fault;
    const selectedCode = fault && ['PIN_MISMATCH', 'BUDGET_EXCEEDED', 'ENTRY_GRANT_DENIED', 'CAPABILITY_UNKNOWN'].includes(error.code) ? error.code : code;
    const selectedPath = code === 'ARTIFACT_INVALID' && fault ? error.path : path;
    return Object.freeze({ prepared: null, diagnostic: diagnostic(selectedCode, selectedPath) });
  }
}

function deadline(context) {
  shape(context, ['clock', 'expiresAtMs'], '');
  ok(context.clock && typeof context.clock.nowMs === 'function' && Number.isSafeInteger(context.expiresAtMs) && context.expiresAtMs >= 0, '', 'HOST_CONTEXT_INVALID');
  const expires = context.expiresAtMs;
  const nowMs = context.clock.nowMs.bind(context.clock);
  let last = -1;
  function remaining(path) {
    let now;
    try { now = nowMs(); } catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    ok(Number.isSafeInteger(now) && now >= 0 && now >= last, '', 'HOST_CONTEXT_INVALID'); last = now;
    ok(now < expires, path, 'BUDGET_EXCEEDED');
    return expires - now;
  }
  function wait(action, path, failureCode, onDispatch, onResponse) {
    return new Promise((resolve, reject) => {
      let active = true, timer;
      const finish = (fn, value) => { if (active) { active = false; clearTimeout(timer); fn(value); } };
      const rejected = () => {
        if (!active) return;
        try { remaining(path); finish(reject, new Fault(failureCode, path)); } catch (error) { finish(reject, error); }
      };
      const arm = left => {
        const chunk = Math.min(left, 2147483647);
        timer = setTimeout(() => {
          if (!active) return;
          try { remaining(path); } catch (error) { finish(reject, error); return; }
          if (left <= chunk) finish(reject, new Fault('BUDGET_EXCEEDED', path));
          else arm(left - chunk);
        }, chunk);
      };
      let pending;
      try {
        const budget = remaining(path);
        onDispatch?.();
        arm(budget);
        try { pending = action(budget); } catch { rejected(); return; }
      } catch (error) { finish(reject, error); return; }
      Promise.resolve(pending).then(value => {
        if (!active) return;
        try {
          // A winning WRITE response establishes evidence before a completion-clock failure can discard its value.
          const settled = onResponse ? onResponse(value) : value;
          remaining(path); finish(resolve, settled);
        } catch (error) { finish(reject, error); }
      }, rejected);
    });
  }
  return { remaining, wait };
}

function effectEvidence(response) {
  try {
    keys(response, '');
    if (response.kind === 'SUCCESS') {
      shape(response, ['kind', 'value', 'effect'], '');
      return response.effect === 'CONFIRMED' ? 'CONFIRMED' : 'UNKNOWN';
    }
    if (response.kind === 'BUSINESS_REJECTION' || response.kind === 'TRANSIENT_FAILURE') {
      shape(response, response.kind === 'BUSINESS_REJECTION' ? ['kind', 'code', 'effect'] : ['kind', 'effect'], '');
      return response.effect === 'NONE' ? 'NONE' : 'UNKNOWN';
    }
  } catch { /* Malformed outer evidence cannot establish a definite effect. */ }
  return 'UNKNOWN';
}

export async function invokeProgram(prepared, businessInput, trustedInvocation) {
  const plan = handles.get(prepared);
  const trace = [];
  const effects = [];
  let businessCause;
  const limit = LIMITS.traceEvents;
  function emit(event, attributes = {}, path = '') {
    ok(trace.length < limit - 1, path, 'BUDGET_EXCEEDED');
    trace.push({ sequence: trace.length, event, ...attributes });
  }
  function terminate(classification, error, result) {
    if (effects.some(effect => effect.state === 'UNKNOWN')) { classification = 'UNCERTAIN'; result = undefined; }
    trace.push({ sequence: trace.length, event: 'TERMINATED', ...(error ? { code: error.code } : {}) });
    return freeze({ classification, ...(error ? { error, ...(plan && result !== undefined ? { result } : {}) } : { result }),
      ...(plan ? { effects } : {}), trace });
  }
  emit('INVOCATION_START');
  try {
    ok(plan && arguments.length === 3, '', 'HOST_CONTEXT_INVALID');
    try { shape(trustedInvocation, ['bindings', 'deadline', 'requestKey', 'event'], ''); }
    catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    let clock, invocationDeadline;
    try {
      shape(trustedInvocation.deadline, ['clock', 'expiresAtMs'], '');
      const sourceClock = trustedInvocation.deadline.clock, nowMs = sourceClock?.nowMs;
      ok(typeof nowMs === 'function', '', 'HOST_CONTEXT_INVALID');
      invocationDeadline = Object.freeze({ clock: Object.freeze({ nowMs: nowMs.bind(sourceClock) }),
        expiresAtMs: trustedInvocation.deadline.expiresAtMs });
      clock = deadline(invocationDeadline);
    } catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    clock.remaining('');
    const { artifact, entrypoint, adapters, hostFields, dcm, grants, effectGrant } = plan;
    const state = Object.fromEntries([['input', validateValue(dcm, entrypoint.input, businessInput, '/input', 'INPUT_INVALID')]]);
    const bindings = Object.create(null);
    try {
      shape(trustedInvocation.bindings, Object.keys(hostFields), '/hostBindings');
      for (const [name, types] of Object.entries(hostFields)) for (const type of types) bindings[name] = validateValue(dcm, type, trustedInvocation.bindings[name], '/hostBindings', 'HOST_BINDING_INVALID');
    } catch (error) { throw new Fault(error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'HOST_BINDING_INVALID', '/hostBindings'); }
    let hostContext;
    try {
      shape(trustedInvocation.event, ['id', 'source'], '');
      const event = Object.freeze({
        id: validateValue(dcm, entrypoint.event.idType, trustedInvocation.event.id, '', 'HOST_CONTEXT_INVALID'),
        source: validateValue(dcm, entrypoint.event.sourceType, trustedInvocation.event.source, '', 'HOST_CONTEXT_INVALID'),
      });
      const requestKey = trustedInvocation.requestKey;
      ok(requestKey !== null && requestKey !== undefined, '', 'HOST_CONTEXT_INVALID');
      hostContext = Object.freeze({ requestKey, event, deadline: invocationDeadline });
    } catch (error) { throw new Fault(error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'HOST_CONTEXT_INVALID', ''); }
    clock.remaining('');
    const counts = Object.create(null);
    let totalCalls = 0, totalEffects = 0;
    const evaluate = (binding, path) => {
      clock.remaining(path);
      // shortcut: scalar CEL is synchronous; qualify interruption before promising hard deadlines.
      const value = evaluateBinding(binding, state, dcm, plan.cel, path);
      clock.remaining(path);
      return value;
    };
    for (const [index, step] of artifact.steps.entries()) {
      const path = '/steps/' + index;
      clock.remaining(path);
      ok(index < LIMITS.steps, path, 'BUDGET_EXCEEDED');
      emit('STEP_START', { step: step.id }, path);
      if (step.op === 'BIND') {
        state[step.id] = evaluate(step.value, path);
        emit('VALUE_BOUND', { step: step.id }, path);
        continue;
      }
      if (Object.hasOwn(step, 'when')) {
        const taken = state[step.when];
        emit('GUARD_EVALUATED', { step: step.id, taken }, path);
        if (!taken) { emit('STEP_SKIPPED', { step: step.id }, path); continue; }
      }
      const cap = dcm.capabilities[step.capability];
      const write = cap.effect === 'WRITE';
      counts[step.capability] = (counts[step.capability] ?? 0) + 1; totalCalls++;
      ok(totalCalls <= LIMITS.calls, path, 'BUDGET_EXCEEDED');
      ok(counts[step.capability] <= (grants[step.capability] ?? 0), path, 'AUTHORITY_DENIED');
      if (write) { totalEffects++; ok(totalEffects <= effectGrant, path, 'AUTHORITY_DENIED'); }
      const agentArgs = Object.create(null), protectedArgs = Object.create(null);
      for (const [name, parameter] of Object.entries(cap.parameters)) {
        if (parameter.binding === 'host') protectedArgs[name] = validateValue(dcm, parameter.type, bindings[name], '/hostBindings', 'HOST_BINDING_INVALID');
        else agentArgs[name] = validateValue(dcm, parameter.type, evaluate(step.args[name], path), path, 'INPUT_INVALID');
      }
      clock.remaining(path);
      freeze(agentArgs); freeze(protectedArgs);
      let receipt, recorded = false;
      const settled = response => {
        if (!receipt || recorded) return;
        recorded = true;
        let envelope;
        try { keys(response, path); envelope = { ...response }; } catch { /* Invalid outer shape remains unknown. */ }
        receipt.state = effectEvidence(envelope);
        emit('EFFECT_RECORDED', { step: step.id, capability: step.capability, state: receipt.state }, path);
        return envelope;
      };
      let response;
      try {
        response = await clock.wait(budget => {
          if (write) {
            receipt = { step: step.id, capability: step.capability, state: 'UNKNOWN' }; effects.push(receipt);
            emit('EFFECT_INTENT', { step: step.id, capability: step.capability }, path);
            emit('CALL_DISPATCHED', { step: step.id, capability: step.capability }, path);
          }
          return adapters[step.capability].invoke(agentArgs, protectedArgs, budget, hostContext);
        }, path, 'ADAPTER_EXCEPTION', () => {
          ok(trace.length + (write ? 5 : 3) <= limit, path, 'BUDGET_EXCEEDED');
          if (!write) emit('CALL_DISPATCHED', { step: step.id, capability: step.capability }, path);
        }, write ? settled : undefined);
      } catch (error) { settled(undefined); throw error; }
      try {
        if (response?.kind === 'SUCCESS') {
          shape(response, ['kind', 'value', ...(write ? ['effect'] : [])], path);
          if (write) ok(response.effect === 'CONFIRMED', path, 'ADAPTER_INVALID');
          const value = validateValue(dcm, cap.result, response.value, path, 'ADAPTER_INVALID');
          if (!Object.hasOwn(step, 'when')) state[step.id] = value;
        } else if (response?.kind === 'BUSINESS_REJECTION') {
          shape(response, ['kind', 'code', ...(write ? ['effect'] : [])], path);
          if (write) ok(response.effect === 'NONE', path, 'ADAPTER_INVALID');
          const code = response.code;
          ok(cap.errors.includes(code), path, 'ADAPTER_INVALID');
          clock.remaining(path);
          if (Object.hasOwn(step.onError ?? {}, code)) {
            businessCause = { code, path };
            const handlerPath = path + '/onError/' + pointer(code);
            const result = Object.fromEntries(Object.entries(step.onError[code].fields).map(([field, binding]) => [field, evaluate(binding, handlerPath + '/' + pointer(field))]));
            const validated = validateValue(dcm, entrypoint.output, result, handlerPath, 'EXPRESSION_FAILED');
            clock.remaining(handlerPath); emit('RETURNED', {}, handlerPath);
            return terminate('BUSINESS_REJECTION', businessCause, validated);
          }
          return terminate('BUSINESS_REJECTION', { code, path });
        } else if (response?.kind === 'TRANSIENT_FAILURE') {
          shape(response, ['kind', ...(write ? ['effect'] : [])], path);
          if (write) ok(response.effect === 'NONE', path, 'ADAPTER_INVALID');
          throw new Fault('ADAPTER_TRANSIENT', path);
        } else if (write && response?.kind === 'UNKNOWN_COMMIT') {
          shape(response, ['kind', 'effect'], path); ok(response.effect === 'UNKNOWN', path, 'ADAPTER_INVALID');
          throw new Fault('UNKNOWN_COMMIT', path);
        } else throw new Fault('ADAPTER_INVALID', path);
      } catch (error) {
        if (businessCause || error instanceof Fault && ['ADAPTER_TRANSIENT', 'UNKNOWN_COMMIT', 'BUDGET_EXCEEDED', 'HOST_CONTEXT_INVALID'].includes(error.code)) throw error;
        throw new Fault('ADAPTER_INVALID', path);
      }
      clock.remaining(path);
      emit('CALL_SUCCEEDED', { step: step.id, capability: step.capability }, path);
    }
    const result = Object.fromEntries(Object.entries(artifact.return.fields).map(([field, binding]) => [field, evaluate(binding, '/return')]));
    const validated = validateValue(dcm, entrypoint.output, result, '/return', 'EXPRESSION_FAILED');
    clock.remaining('/return');
    emit('RETURNED', {}, '/return');
    return terminate('COMPLETED', undefined, validated);
  } catch (error) {
    const failure = error instanceof Fault ? error : new Fault('HOST_CONTEXT_INVALID', '');
    return terminate(failure.code === 'BUDGET_EXCEEDED' ? 'LIMIT_EXCEEDED' : 'FAILED', {
      code: failure.code, path: failure.path, ...(businessCause ? { cause: businessCause } : {}),
    });
  }
}
