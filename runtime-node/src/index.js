import { canonicalBytes, digest, parseCanonical } from './canonical.js';
import { prepareExpressions, evaluateBinding } from './cel.js';
import { ENVIRONMENT, LIMITS, Fault, requireThat as ok, keys, shape, text, integer, freeze, pointer, narrowPolicy, validateEnvelope, validatePlan, validateValue } from './validation.js';
import { modelFor } from './model.js';

export { ENVIRONMENT };
export { bindModel } from './model.js';
const handles = new WeakMap();
const same = (left, right) => canonicalBytes(left).equals(canonicalBytes(right));
const diagnostic = (code, path) => Object.freeze({ code, path, message: 'The selected artifact or host configuration is invalid.' });

export function prepare(artifactBytes, admission, adapters) {
  try {
    const effectAdmission = Object.hasOwn(admission ?? {}, 'effectGrant') || Object.hasOwn(admission ?? {}, 'keyBinding');
    shape(admission, ['expectedArtifactDigest', 'expectedDcmDigest', 'entrypoint', 'consumer', 'compiler', 'environment', 'mode', 'grants',
      ...(Object.hasOwn(admission ?? {}, 'limits') ? ['limits'] : []), ...(effectAdmission ? ['effectGrant', 'keyBinding'] : [])], '/admission');
    for (const field of ['expectedArtifactDigest', 'expectedDcmDigest']) ok(typeof admission[field] === 'string' && /^sha256:[0-9a-f]{64}$/.test(admission[field]), '/admission/' + field, 'ADMISSION_INVALID');
    ok(artifactBytes instanceof Uint8Array && artifactBytes.byteLength <= LIMITS.artifactBytes, '/artifact', 'BUDGET_EXCEEDED');
    const bytes = Buffer.from(artifactBytes);
    ok(digest(bytes) === admission.expectedArtifactDigest, '/artifact', 'PIN_MISMATCH');
    const artifact = parseCanonical(bytes);
    const plan = validatePlan(artifact);
    ok(effectAdmission === plan.effect, '/admission', 'ADMISSION_INVALID');
    ok(artifact.dcmDigest === admission.expectedDcmDigest && artifact.entrypoint === admission.entrypoint && artifact.compiler === admission.compiler && same(artifact.environment, admission.environment) && same(plan.entrypoint.consumer, admission.consumer), '/admission', 'PIN_MISMATCH');
    ok(plan.effect ? admission.mode === 'simulation' : ['simulation', 'real'].includes(admission.mode), '/admission/mode', 'ADMISSION_INVALID');
    let keyValidator;
    if (plan.effect) {
      integer(admission.effectGrant, plan.entrypoint.effectGrant, '/admission/effectGrant', 0);
      ok(plan.writeCount <= admission.effectGrant, '/admission/effectGrant', 'AUTHORITY_DENIED');
      shape(admission.keyBinding, ['type', 'validate'], '/admission/keyBinding');
      ok(admission.keyBinding.type === artifact.dcm.hostKey.type && typeof admission.keyBinding.validate === 'function', '/admission/keyBinding', 'ADMISSION_INVALID');
      keyValidator = admission.keyBinding.validate;
    }
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
      const adapter = adapters[id]; shape(adapter, ['version', 'mode', 'invoke', ...(plan.effect ? ['keyType'] : [])], '/admission/adapters');
      ok(adapter.version === artifact.dcm.capabilities[id].version && adapter.mode === admission.mode && typeof adapter.invoke === 'function', '/admission/adapters', 'ADMISSION_INVALID');
      if (plan.effect) ok(adapter.keyType === artifact.dcm.hostKey.type, '/admission/adapters', 'ADMISSION_INVALID');
      selectedAdapters[id] = Object.freeze({ invoke: adapter.invoke });
    }
    for (const id of Object.keys(plan.calls)) {
      ok(Object.hasOwn(selectedAdapters, id), '/admission/adapters', 'ADMISSION_INVALID');
      for (const [name, parameter] of Object.entries(artifact.dcm.capabilities[id].parameters)) if (parameter.binding === 'host') {
        if (!Object.hasOwn(hostFields, name)) hostFields[name] = [];
        hostFields[name].push(parameter.type);
      }
    }
    const handle = Object.freeze(Object.create(null));
    handles.set(handle, { version: 1, artifact: freeze(artifact), dcm: artifact.dcm, entrypoint: plan.entrypoint, grants: freeze(grants), limits: freeze(limits), cel, adapters: selectedAdapters, hostFields,
      effect: plan.effect, effectGrant: admission.effectGrant, keyValidator });
    return handle;
  } catch (error) {
    const path = error instanceof Fault ? error.path : '/artifact';
    const code = error instanceof Fault ? error.code : 'ARTIFACT_INVALID';
    return diagnostic(code === 'ARTIFACT_INVALID' && path.startsWith('/admission') ? 'ADMISSION_INVALID' : code, path);
  }
}

export function prepareProgram(artifactBytes, boundModel, admission) {
  let path = '/binding', code = 'ADMISSION_INVALID';
  try {
    const bound = modelFor(boundModel); ok(bound, path, code);
    path = '/admission';
    shape(admission, ['expectedArtifactDigest', 'expectedDcmDigest', 'entrypoint', 'consumer', 'compiler', 'environment', 'mode',
      ...['capabilityPolicy', 'limits', ...(bound.effect ? ['effectGrant'] : [])].filter(field => Object.hasOwn(admission ?? {}, field))], path);
    path = '/admission/expectedArtifactDigest';
    ok(typeof admission.expectedArtifactDigest === 'string' && /^sha256:[0-9a-f]{64}$/.test(admission.expectedArtifactDigest), path, code);
    path = '/artifact'; code = 'ARTIFACT_INVALID';
    ok(artifactBytes instanceof Uint8Array, path, code); ok(artifactBytes.byteLength <= LIMITS.artifactBytes, path, 'BUDGET_EXCEEDED');
    const bytes = Buffer.from(artifactBytes); ok(digest(bytes) === admission.expectedArtifactDigest, path, 'PIN_MISMATCH');
    const artifact = parseCanonical(bytes); validateEnvelope(artifact, bound);
    const entrypoint = bound.dcm.entrypoints[artifact.entrypoint];
    const limits = { steps: LIMITS.steps, calls: LIMITS.calls, totalAstNodes: LIMITS.totalAstNodes, traceEvents: LIMITS.traceEvents };
    let grants = { ...bound.model.policies[artifact.entrypoint] };
    let effectGrant = bound.effect ? bound.model.effectGrants[artifact.entrypoint] : 0;
    code = 'ADMISSION_INVALID';
    for (const field of Object.keys(admission).sort()) {
      path = '/admission/' + field;
      if (field === 'capabilityPolicy') {
        ok(admission.capabilityPolicy !== undefined, path); grants = narrowPolicy(bound.dcm, admission.capabilityPolicy, grants, path);
      } else if (field === 'compiler') ok(admission.compiler === artifact.compiler, path);
      else if (field === 'consumer') ok(same(admission.consumer, entrypoint.consumer), path);
      else if (field === 'effectGrant') { integer(admission.effectGrant, effectGrant, path, 0); effectGrant = admission.effectGrant; }
      else if (field === 'entrypoint') ok(admission.entrypoint === artifact.entrypoint, path);
      else if (field === 'environment') ok(same(admission.environment, artifact.environment), path);
      else if (field === 'expectedDcmDigest') ok(admission.expectedDcmDigest === bound.modelRef.digest, path);
      else if (field === 'limits') for (const name of keys(admission.limits, path, 4).sort()) {
        ok(Object.hasOwn(limits, name), path); integer(admission.limits[name], limits[name], path, name === 'traceEvents' ? 2 : 0);
        limits[name] = admission.limits[name];
      }
      else if (field === 'mode') ok(admission.mode === bound.mode, path);
    }
    path = '/artifact'; code = 'ARTIFACT_INVALID';
    const plan = validatePlan(artifact, bound);
    ok(artifact.steps.length <= limits.steps && plan.callCount <= limits.calls, '/artifact/steps', 'BUDGET_EXCEEDED');
    for (const [id, count] of Object.entries(plan.calls)) ok(count <= grants[id], '/admission/capabilityPolicy', 'AUTHORITY_DENIED');
    ok(plan.writeCount <= effectGrant, '/admission/effectGrant', 'AUTHORITY_DENIED');
    const cel = prepareExpressions(artifact, plan.contexts, bound);
    ok(cel.totalNodes <= limits.totalAstNodes, '/artifact/expressions', 'BUDGET_EXCEEDED');
    const hostFields = Object.create(null);
    for (const id of Object.keys(plan.calls)) for (const [name, parameter] of Object.entries(bound.dcm.capabilities[id].parameters)) if (parameter.binding === 'host') {
      if (!Object.hasOwn(hostFields, name)) hostFields[name] = [];
      hostFields[name].push(parameter.type);
    }
    const prepared = Object.freeze(Object.create(null));
    handles.set(prepared, { version: 2, artifact: freeze(artifact), dcm: bound.dcm, entrypoint, grants: freeze(grants), limits: freeze(limits), cel,
      adapters: bound.adapters, hostFields, effect: bound.effect, effectGrant, keyValidator: bound.keyValidator, scope: bound.scope, bound });
    return Object.freeze({ prepared, diagnostic: null });
  } catch (error) {
    const fault = error instanceof Fault;
    const selectedCode = fault && ['PIN_MISMATCH', 'BUDGET_EXCEEDED', 'AUTHORITY_DENIED', 'ENTRY_GRANT_DENIED', 'CAPABILITY_UNKNOWN'].includes(error.code) ? error.code : code;
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

export function invoke(prepared, businessInput, trustedInvocation) { return execute(prepared, businessInput, trustedInvocation, 1); }
export function invokeProgram(prepared, businessInput, trustedInvocation) { return execute(prepared, businessInput, trustedInvocation, 2); }

async function execute(prepared, businessInput, trustedInvocation, version) {
  const plan = handles.get(prepared);
  const trace = [];
  const effects = [];
  let businessCause;
  const limit = plan?.limits.traceEvents ?? LIMITS.traceEvents;
  function emit(event, attributes = {}, path = '') {
    ok(trace.length < limit - 1, path, 'BUDGET_EXCEEDED');
    trace.push({ sequence: trace.length, event, ...attributes });
  }
  function terminate(classification, error, result) {
    if (effects.some(effect => effect.state === 'UNKNOWN')) { classification = 'UNCERTAIN'; result = undefined; }
    trace.push({ sequence: trace.length, event: 'TERMINATED', ...(error ? { code: error.code } : {}) });
    return freeze({ classification, ...(error ? { error, ...(plan?.effect && result !== undefined ? { result } : {}) } : { result }),
      ...(plan?.effect ? { effects } : {}), trace });
  }
  emit('INVOCATION_START');
  try {
    ok(plan && plan.version === version, '', 'HOST_CONTEXT_INVALID');
    try {
      shape(trustedInvocation, ['bindings', 'deadline', 'authorize', ...(version === 2 ? ['scope',
        ...(Object.hasOwn(trustedInvocation ?? {}, 'capabilityPolicy') ? ['capabilityPolicy'] : [])] : ['grants']),
      ...(plan.effect ? ['requestKey', 'event', ...(version === 1 || Object.hasOwn(trustedInvocation ?? {}, 'effectGrant') ? ['effectGrant'] : [])] : [])], '');
      if (version === 2) { text(trustedInvocation.scope, '', true); ok(trustedInvocation.scope === plan.scope, ''); }
    }
    catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    ok(typeof trustedInvocation.authorize === 'function', '', 'HOST_CONTEXT_INVALID');
    let clock;
    try { clock = deadline(trustedInvocation.deadline); } catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    clock.remaining('');
    const { artifact, entrypoint, adapters, hostFields } = plan;
    const dcm = plan.dcm;
    const state = Object.fromEntries([['input', validateValue(dcm, entrypoint.input, businessInput, '/input', 'INPUT_INVALID')]]);
    const bindings = Object.create(null);
    try {
      shape(trustedInvocation.bindings, Object.keys(hostFields), '/hostBindings');
      for (const [name, types] of Object.entries(hostFields)) for (const type of types) bindings[name] = validateValue(dcm, type, trustedInvocation.bindings[name], '/hostBindings', 'HOST_BINDING_INVALID');
    } catch (error) { throw new Fault(error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'HOST_BINDING_INVALID', '/hostBindings'); }
    const grants = Object.create(null);
    try {
      if (version === 2) {
        if (Object.hasOwn(trustedInvocation, 'capabilityPolicy')) ok(trustedInvocation.capabilityPolicy !== undefined, '');
        Object.assign(grants, narrowPolicy(dcm, trustedInvocation.capabilityPolicy, plan.grants, ''));
      } else for (const id of keys(trustedInvocation.grants, '', 8)) {
        ok(Object.hasOwn(plan.grants, id), '', 'HOST_CONTEXT_INVALID');
        integer(trustedInvocation.grants[id], plan.grants[id], '');
        grants[id] = trustedInvocation.grants[id];
      }
    } catch { throw new Fault('HOST_CONTEXT_INVALID', ''); }
    let hostContext, effectGrant;
    if (plan.effect) {
      try {
        effectGrant = version === 2 && !Object.hasOwn(trustedInvocation, 'effectGrant') ? plan.effectGrant : trustedInvocation.effectGrant;
        integer(effectGrant, plan.effectGrant, '', 0);
        shape(trustedInvocation.event, ['id', 'source'], '');
        const event = Object.freeze({
          id: validateValue(dcm, entrypoint.event.idType, trustedInvocation.event.id, '', 'HOST_CONTEXT_INVALID'),
          source: validateValue(dcm, entrypoint.event.sourceType, trustedInvocation.event.source, '', 'HOST_CONTEXT_INVALID'),
        });
        const requestKey = trustedInvocation.requestKey;
        ok(requestKey !== null && requestKey !== undefined, '', 'HOST_CONTEXT_INVALID');
        hostContext = Object.freeze({ requestKey, event });
      } catch (error) { throw new Fault(error instanceof Fault && error.code === 'BUDGET_EXCEEDED' ? error.code : 'HOST_CONTEXT_INVALID', ''); }
      clock.remaining('');
      let valid;
      const validateKey = plan.keyValidator;
      try { valid = validateKey(hostContext.requestKey); } catch { valid = false; }
      clock.remaining('');
      ok(valid === true, '', 'HOST_CONTEXT_INVALID');
    }
    const authorize = trustedInvocation.authorize;
    async function authorized(request, path) {
      clock.remaining(path);
      const allowed = await clock.wait(() => authorize(freeze(request)), path, 'AUTHORITY_CHECK_FAILED');
      ok(typeof allowed === 'boolean', path, 'AUTHORITY_CHECK_FAILED');
      ok(allowed, path, 'AUTHORITY_DENIED');
    }
    await authorized({ entrypoint: artifact.entrypoint }, '');
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
      ok(index < plan.limits.steps, path, 'BUDGET_EXCEEDED');
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
      ok(totalCalls <= plan.limits.calls, path, 'BUDGET_EXCEEDED');
      ok(counts[step.capability] <= (grants[step.capability] ?? 0), path, 'AUTHORITY_DENIED');
      if (write) { totalEffects++; ok(totalEffects <= effectGrant, path, 'AUTHORITY_DENIED'); }
      const agentArgs = Object.create(null), protectedArgs = Object.create(null);
      for (const [name, parameter] of Object.entries(cap.parameters)) {
        if (parameter.binding === 'host') protectedArgs[name] = validateValue(dcm, parameter.type, bindings[name], '/hostBindings', 'HOST_BINDING_INVALID');
        else agentArgs[name] = validateValue(dcm, parameter.type, evaluate(step.args[name], path), path, 'INPUT_INVALID');
      }
      await authorized({ entrypoint: artifact.entrypoint, capability: step.capability, version: step.version }, path);
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
          return plan.effect ? adapters[step.capability].invoke(agentArgs, protectedArgs, budget, hostContext)
            : adapters[step.capability].invoke(agentArgs, protectedArgs, budget);
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
