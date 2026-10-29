import { canonicalBytes, digest, parseCanonical } from './canonical.js';
import { prepareExpressions, evaluateBinding } from './cel.js';
import { ENVIRONMENT, LIMITS, Fault, requireThat as ok, keys, shape, integer, freeze, validatePlan, validateValue } from './validation.js';

export { ENVIRONMENT };
const handles = new WeakMap();
const same = (left, right) => canonicalBytes(left).equals(canonicalBytes(right));
const diagnostic = (code, path) => Object.freeze({ code, path, message: 'The selected artifact or host configuration is invalid.' });

export function prepare(artifactBytes, admission, adapters) {
  try {
    shape(admission, ['expectedArtifactDigest', 'expectedDcmDigest', 'entrypoint', 'consumer', 'compiler', 'environment', 'mode', 'grants', ...(Object.hasOwn(admission ?? {}, 'limits') ? ['limits'] : [])], '/admission');
    for (const field of ['expectedArtifactDigest', 'expectedDcmDigest']) ok(typeof admission[field] === 'string' && /^sha256:[0-9a-f]{64}$/.test(admission[field]), '/admission/' + field, 'ADMISSION_INVALID');
    ok(artifactBytes instanceof Uint8Array && artifactBytes.byteLength <= LIMITS.artifactBytes, '/artifact', 'BUDGET_EXCEEDED');
    const bytes = Buffer.from(artifactBytes);
    ok(digest(bytes) === admission.expectedArtifactDigest, '/artifact', 'PIN_MISMATCH');
    const artifact = parseCanonical(bytes);
    const plan = validatePlan(artifact);
    ok(artifact.dcmDigest === admission.expectedDcmDigest && artifact.entrypoint === admission.entrypoint && artifact.compiler === admission.compiler && same(artifact.environment, admission.environment) && same(plan.entrypoint.consumer, admission.consumer), '/admission', 'PIN_MISMATCH');
    ok(['simulation', 'real'].includes(admission.mode), '/admission/mode', 'ADMISSION_INVALID');
    const grants = Object.fromEntries(keys(admission.grants, '/admission/grants', 8).map(id => {
      ok(Object.hasOwn(plan.entrypoint.grants, id), '/admission/grants', 'ADMISSION_INVALID');
      integer(admission.grants[id], plan.entrypoint.grants[id], '/admission/grants');
      return [id, admission.grants[id]];
    }));
    const limits = { steps: LIMITS.steps, calls: LIMITS.calls, totalAstNodes: LIMITS.totalAstNodes, traceEvents: LIMITS.traceEvents };
    if (admission.limits !== undefined) for (const key of keys(admission.limits, '/admission/limits', 4)) {
      ok(Object.hasOwn(limits, key), '/admission/limits', 'ADMISSION_INVALID');
      integer(admission.limits[key], limits[key], '/admission/limits', key === 'traceEvents' ? 2 : 0);
      limits[key] = admission.limits[key];
    }
    ok(artifact.steps.length <= limits.steps && plan.callCount <= limits.calls, '/artifact/steps', 'BUDGET_EXCEEDED');
    for (const [id, count] of Object.entries(plan.calls)) ok(count <= (grants[id] ?? 0), '/admission/grants', 'AUTHORITY_DENIED');
    const cel = prepareExpressions(artifact, plan.contexts);
    ok(cel.totalNodes <= limits.totalAstNodes, '/artifact/expressions', 'BUDGET_EXCEEDED');
    const selectedAdapters = Object.create(null);
    const hostFields = Object.create(null);
    for (const id of keys(adapters, '/admission/adapters', 8)) {
      ok(Object.hasOwn(artifact.dcm.capabilities, id), '/admission/adapters', 'ADMISSION_INVALID');
      const adapter = adapters[id]; shape(adapter, ['version', 'mode', 'invoke'], '/admission/adapters');
      ok(adapter.version === artifact.dcm.capabilities[id].version && adapter.mode === admission.mode && typeof adapter.invoke === 'function', '/admission/adapters', 'ADMISSION_INVALID');
      selectedAdapters[id] = { invoke: adapter.invoke };
    }
    for (const id of Object.keys(plan.calls)) {
      ok(Object.hasOwn(selectedAdapters, id), '/admission/adapters', 'ADMISSION_INVALID');
      for (const [name, parameter] of Object.entries(artifact.dcm.capabilities[id].parameters)) if (parameter.binding === 'host') {
        if (!Object.hasOwn(hostFields, name)) hostFields[name] = [];
        hostFields[name].push(parameter.type);
      }
    }
    const handle = Object.freeze(Object.create(null));
    handles.set(handle, { artifact: freeze(artifact), entrypoint: plan.entrypoint, grants: freeze(grants), limits: freeze(limits), cel, adapters: selectedAdapters, hostFields });
    return handle;
  } catch (error) {
    const path = error instanceof Fault ? error.path : '/artifact';
    const code = error instanceof Fault ? error.code : 'ARTIFACT_INVALID';
    return diagnostic(code === 'ARTIFACT_INVALID' && path.startsWith('/admission') ? 'ADMISSION_INVALID' : code, path);
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
  function wait(action, path, failureCode, onDispatch) {
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
        try { remaining(path); finish(resolve, value); } catch (error) { finish(reject, error); }
      }, rejected);
    });
  }
  return { remaining, wait };
}

export async function invoke(prepared, businessInput, trustedInvocation) {
  const plan = handles.get(prepared);
  const trace = [];
  const limit = plan?.limits.traceEvents ?? LIMITS.traceEvents;
  function emit(event, attributes = {}, path = '') {
    ok(trace.length < limit - 1, path, 'BUDGET_EXCEEDED');
    trace.push({ sequence: trace.length, event, ...attributes });
  }
  function terminate(classification, error, result) {
    trace.push({ sequence: trace.length, event: 'TERMINATED', ...(error ? { code: error.code } : {}) });
    return freeze({ classification, ...(error ? { error } : { result }), trace });
  }
  emit('INVOCATION_START');
  try {
    ok(plan, '', 'HOST_CONTEXT_INVALID');
    try { shape(trustedInvocation, ['bindings', 'deadline', 'authorize', 'grants'], ''); }
    catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    ok(typeof trustedInvocation.authorize === 'function', '', 'HOST_CONTEXT_INVALID');
    let clock;
    try { clock = deadline(trustedInvocation.deadline); } catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    clock.remaining('');
    const { artifact, entrypoint, adapters, hostFields } = plan;
    const dcm = artifact.dcm;
    const state = Object.fromEntries([['input', validateValue(dcm, entrypoint.input, businessInput, '/input', 'INPUT_INVALID')]]);
    const bindings = Object.create(null);
    try {
      shape(trustedInvocation.bindings, Object.keys(hostFields), '/hostBindings');
      for (const [name, types] of Object.entries(hostFields)) for (const type of types) bindings[name] = validateValue(dcm, type, trustedInvocation.bindings[name], '/hostBindings', 'HOST_BINDING_INVALID');
    } catch (error) { throw new Fault(error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'HOST_BINDING_INVALID', '/hostBindings'); }
    const grants = Object.create(null);
    try {
      for (const id of keys(trustedInvocation.grants, '', 8)) {
        ok(Object.hasOwn(plan.grants, id), '', 'HOST_CONTEXT_INVALID');
        integer(trustedInvocation.grants[id], plan.grants[id], '');
        grants[id] = trustedInvocation.grants[id];
      }
    } catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    const authorize = trustedInvocation.authorize;
    async function authorized(request, path) {
      clock.remaining(path);
      const allowed = await clock.wait(() => authorize(freeze(request)), path, 'AUTHORITY_CHECK_FAILED');
      ok(typeof allowed === 'boolean', path, 'AUTHORITY_CHECK_FAILED');
      ok(allowed, path, 'AUTHORITY_DENIED');
    }
    await authorized({ entrypoint: artifact.entrypoint }, '');
    const counts = Object.create(null);
    let totalCalls = 0;
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
      ok(index < plan.limits.steps, path, 'BUDGET_EXCEEDED');
      emit('STEP_START', { step: step.id }, path);
      if (step.op === 'BIND') {
        state[step.id] = evaluate(step.value, path);
        emit('VALUE_BOUND', { step: step.id }, path);
        continue;
      }
      const cap = dcm.capabilities[step.capability];
      counts[step.capability] = (counts[step.capability] ?? 0) + 1; totalCalls++;
      ok(totalCalls <= plan.limits.calls, path, 'BUDGET_EXCEEDED');
      ok(counts[step.capability] <= (grants[step.capability] ?? 0), path, 'AUTHORITY_DENIED');
      const agentArgs = Object.create(null), protectedArgs = Object.create(null);
      for (const [name, parameter] of Object.entries(cap.parameters)) {
        if (parameter.binding === 'host') protectedArgs[name] = validateValue(dcm, parameter.type, bindings[name], '/hostBindings', 'HOST_BINDING_INVALID');
        else agentArgs[name] = validateValue(dcm, parameter.type, evaluate(step.args[name], path), path, 'INPUT_INVALID');
      }
      await authorized({ entrypoint: artifact.entrypoint, capability: step.capability, version: step.version }, path);
      clock.remaining(path);
      const response = await clock.wait(budget => adapters[step.capability].invoke(freeze(agentArgs), freeze(protectedArgs), budget), path, 'ADAPTER_EXCEPTION', () => {
        ok(trace.length + 3 <= limit, path, 'BUDGET_EXCEEDED');
        emit('CALL_DISPATCHED', { step: step.id, capability: step.capability }, path);
      });
      try {
        if (response?.kind === 'SUCCESS') {
          shape(response, ['kind', 'value'], path);
          state[step.id] = validateValue(dcm, cap.result, response.value, path, 'ADAPTER_INVALID');
        } else if (response?.kind === 'BUSINESS_REJECTION') {
          shape(response, ['kind', 'code'], path);
          const code = response.code;
          ok(cap.errors.includes(code), path, 'ADAPTER_INVALID');
          clock.remaining(path);
          return terminate('BUSINESS_REJECTION', { code, path });
        } else if (response?.kind === 'TRANSIENT_FAILURE') {
          shape(response, ['kind'], path); throw new Fault('ADAPTER_TRANSIENT', path);
        } else throw new Fault('ADAPTER_INVALID', path);
      } catch (error) {
        if (error instanceof Fault && ['ADAPTER_TRANSIENT', 'BUDGET_EXCEEDED', 'HOST_CONTEXT_INVALID'].includes(error.code)) throw error;
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
    return terminate(failure.code === 'BUDGET_EXCEEDED' ? 'LIMIT_EXCEEDED' : 'FAILED', { code: failure.code, path: failure.path });
  }
}
