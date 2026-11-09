import { consumeDecision } from './decision-consumer.mjs';

// A single-attempt teaching host. These mutable facts and callbacks belong to the
// simulation; a real host rechecks local facts in its native atomic transition.
export function createDecisionHost(api, modelBytes, artifactBytes) {
  const state = {
    selection: Object.freeze({ kind: 'SELECTED', id: 'approved-program' }),
    available: true, approved: true, approvalVersion: 1,
    actorId: 'user1', permitted: true, cancelled: false, closed: false,
    nowMs: 0, factVersion: 1, restricted: false,
    readResponse: undefined, readError: undefined,
    afterRead: undefined, beforeCommit: undefined, nativeFailure: false,
  };
  const operationDeadline = 1000, reserveMs = 100;
  const clock = { nowMs: () => state.nowMs };
  const requestKey = Object.freeze({});
  const authority = new WeakMap([[requestKey, { actorId: 'user1', id: 'attempt1', source: 'scope1' }]]);
  const counts = { invocations: 0, reads: 0, writes: 0, nativeAttempts: 0, nativeResults: 0 };
  function authorized(invocation, actorId = state.actorId) {
    const owner = authority.get(invocation.requestKey);
    return owner && state.permitted && state.actorId === owner.actorId && actorId === owner.actorId
      && invocation.event?.id === owner.id && invocation.event?.source === owner.source;
  }
  let readVersion, started = false;
  const implementations = {
    'policy.get': {
      version: '1', mode: 'simulation', keyType: 'sample.OperationKey/1',
      async invoke(_args, protectedArgs, _budget, invocation) {
        counts.reads++;
        if (!authorized(invocation, protectedArgs.actorId)) return { kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED' };
        readVersion = state.factVersion;
        const response = state.readResponse ?? { kind: 'SUCCESS', value: { restricted: state.restricted } };
        await state.afterRead?.();
        if (state.readError) throw state.readError;
        return response;
      },
    },
    'native.write': {
      version: '1', mode: 'simulation', keyType: 'sample.OperationKey/1',
      invoke() {
        counts.writes++;
        throw new Error('A decision must never dispatch WRITE.');
      },
    },
  };
  const binding = api.bindModel(modelBytes, implementations);
  const preparation = binding.boundModel ? api.prepareProgram(artifactBytes, binding.boundModel) : null;
  const prepared = preparation?.prepared;
  const usesRead = prepared && JSON.parse(Buffer.from(artifactBytes).toString('utf8')).steps.some(step => step.op === 'CALL');
  const context = {
    bindings: usesRead ? { actorId: 'user1' } : {},
    requestKey, event: { id: 'attempt1', source: 'scope1' },
    deadline: { clock, expiresAtMs: operationDeadline },
  };

  function inputValue(input) {
    if (!input || typeof input !== 'object' || Reflect.ownKeys(input).length !== 1) return undefined;
    const field = Object.getOwnPropertyDescriptor(input, 'restricted');
    return field?.enumerable && Object.hasOwn(field, 'value') && typeof field.value === 'boolean' ? field.value : undefined;
  }

  async function attempt(input, invocation) {
    let outcome = null, consumption = state.selection?.kind === 'ABSENT' ? null : { status: 'BLOCKED' };
    const blocked = () => ({ outcome, consumption, nativeResult: { status: 'BLOCKED' } });
    if (started || state.closed) return blocked();
    started = true;
    try {
      const selection = state.selection, approvalVersion = state.approvalVersion;
      const proposed = inputValue(input);
      const originalKey = invocation.requestKey;
      const event = { ...invocation.event };
      const start = clock.nowMs();
      const current = () => !state.cancelled && !state.closed && state.selection === selection
        && (selection?.kind === 'ABSENT' || (state.approved && state.approvalVersion === approvalVersion && state.available))
        && inputValue(input) === proposed && invocation.requestKey === originalKey
        && invocation.event?.id === event.id && invocation.event?.source === event.source
        && invocation.deadline?.clock === clock && invocation.deadline?.expiresAtMs === operationDeadline
        && (selection?.kind === 'ABSENT' || !usesRead || invocation.bindings?.actorId === state.actorId)
        && authorized(invocation) && Number.isSafeInteger(state.nowMs) && state.nowMs >= 0 && state.nowMs >= start
        && state.nowMs < operationDeadline && (readVersion === undefined || readVersion === state.factVersion);
      if (proposed === undefined || !current()) return blocked();
      if (selection?.kind !== 'ABSENT') {
        consumption = { status: 'BLOCKED' };
        const guardDeadline = operationDeadline - reserveMs;
        if (selection?.kind !== 'SELECTED' || !prepared || reserveMs <= 0 || state.nowMs >= guardDeadline) return blocked();
        counts.invocations++;
        try {
          outcome = await api.invokeProgram(prepared, Object.freeze({ restricted: proposed }), {
            ...invocation, deadline: { clock, expiresAtMs: guardDeadline },
          });
        } catch (error) {
          return { outcome, consumption, nativeResult: { status: 'BLOCKED' }, invocationError: error };
        }
        consumption = consumeDecision(outcome);
        if (consumption.status !== 'ALLOWED') return {
          outcome, consumption, nativeResult: { status: consumption.status === 'DENIED' ? 'DENIED' : 'BLOCKED' },
        };
      }
      await state.beforeCommit?.();
      if (!current()) return blocked();
      // No await between the final host checks and this simulated native transition.
      counts.nativeAttempts++;
      if (state.nativeFailure) return { outcome, consumption, nativeResult: { status: 'FAILED' } };
      counts.nativeResults++;
      return { outcome, consumption, nativeResult: { status: 'COMMITTED' } };
    } catch (error) {
      return { ...blocked(), hostError: error };
    } finally {
      state.closed = true;
    }
  }
  return { state, context, attempt, counts: () => ({ ...counts }),
    diagnostic: binding.diagnostic ?? preparation?.diagnostic ?? null };
}
