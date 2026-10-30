# Read-and-decide runtime

Status: implemented experimental checkpoint with local shared conformance. Java and Node share this behavior and [artifact contract](compiler-artifact.md). APIs below define data/ownership semantics; Java futures and Node promises implement the same asynchronous adapter boundary. Production containment qualification remains open.

## Prepare and invoke

These APIs and outcomes remain the read-profile contract. The planned [single-effect runtime delta](single-effect.md#host-api-and-native-key) adds a separate protected key/event channel, current effect grant, terminal handler results and retained effect receipts only under its new profile. It does not weaken the read loader or change read adapter signatures.

`prepare(artifactBytes, admission, adapters) -> Prepared | Diagnostic` validates the artifact before resolving adapters and preparing CEL. Admission is the closed host-owned record `{expectedArtifactDigest,expectedDcmDigest,entrypoint,consumer,compiler,environment,mode,grants,limits?}`. Consumer, compiler and environment must exactly match the selected artifact contract. `mode` is `simulation` or `real`. Preparing calls neither authorization callbacks nor adapters. A Prepared handle is immutable and caches only plan/environment and admission ceilings, not user identity or mutable invocation data.

`adapters` is a host-supplied map from capability ID to the closed registration `{version,mode,invoke}`. Every called capability needs a registration with the exact DCM version and admission mode; unknown IDs, invalid registrations, missing required registrations and version/mode mismatches fail preparation. Registered but unused DCM capabilities grant no authority. Snapshot validated registration metadata/function references during preparation; later map mutation must not change a prepared binding. This is an in-process API, not a registry service or customer configuration format.

`invoke(prepared, businessInput, trustedInvocation) -> Outcome` gets a fresh closed input record and trusted `{bindings,deadline,authorize,grants}` context. `bindings` supplies declared protected scalar parameters by name; `deadline` uses the host monotonic clock; `authorize` checks current invocation/capability scope immediately before each dispatch. The host constructs this context; it is not deserialized verbatim from customer JSON. No CEL variable exposes it. Validate input, host bindings and the current host grant before any call. A host may narrow grants, never widen artifact/DCM rights.

Both admission and invocation `grants` are required maps from capability ID to positive integer call ceilings, shaped like entrypoint grants. Omission of an ID means zero permission, never inheritance. Admission grants may only narrow the selected DCM entrypoint; fresh invocation grants may only narrow admission. Reject unknown IDs, invalid counts or widening during preparation, or as FAILED/`HOST_CONTEXT_INVALID` for invocation context. Snapshot invocation grants for that invocation and count every dispatch; a missing/exhausted grant terminates as FAILED/`AUTHORITY_DENIED` before dispatch. Current authorization remains required even with a positive remaining count. This explicit fresh map supersedes the earlier consultation suggestion that admission grants alone suffice; no additional per-user policy framework is required.

Authorization receives an immutable closed request `{entrypoint}` once for the invocation, then `{entrypoint,capability,version}` immediately before each call. The host callback captures current identity/resource context; protected values are not copied into this request or CEL. Check grants before calling the per-call authorization function.

`authorize` returns an asynchronous Boolean for the selected invocation or call: `true` permits continuing validation/dispatch, `false` terminates as FAILED/`AUTHORITY_DENIED`. A thrown exception, rejected future/promise or non-Boolean result terminates as FAILED/`AUTHORITY_CHECK_FAILED`; it is not a business denial or an adapter exception. No adapter dispatch follows either failure. Authorization consumes the invocation deadline; if the deadline expires while it is pending, classify LIMIT_EXCEEDED/`BUDGET_EXCEEDED` and discard its eventual result. Check expiry again after a successful callback before dispatch. Never expose the callback's exception details.

`deadline` is a host-owned pair `{clock,expiresAtMs}`, where `clock.nowMs()` returns nondecreasing integer milliseconds from an arbitrary monotonic origin and `expiresAtMs` is in that same clock domain. Both are nonnegative exact integers no larger than 2^53-1; reject invalid values/clock regression as FAILED/`HOST_CONTEXT_INVALID`. Expired means `nowMs() >= expiresAtMs`; remaining budget is `max(0, expiresAtMs - nowMs())` milliseconds. Java hosts may derive integer milliseconds from their monotonic timer; Node hosts do likewise. Never serialize these readings as UTC/wallclock or compare origins across processes. Shared fixtures inject identical logical clock readings and expiry values; real hosts independently own their clocks and native timeout scheduling. This context is not part of the artifact or business input.

Adapter call is `invoke(agentArgs, protectedArgs, remainingDeadline) -> AdapterOutcome`, with separately validated immutable argument records. Do not merge into a caller-controlled object. The adapter still checks current object/resource access and must enforce its actual I/O deadline. Generic runtime cannot prove that a task service returned the requested task or that its authorization implementation is correct.

Adapter outcomes: `{kind:"SUCCESS",value:record}`, `{kind:"BUSINESS_REJECTION",code:declaredCode}`, or `{kind:"TRANSIENT_FAILURE"}`. Undeclared codes, invalid shapes, extra fields, uncertain/effectful responses or thrown exceptions produce `FAILED` with `ADAPTER_INVALID` or `ADAPTER_EXCEPTION`, never an ordinary result. No automatic retry or handler. READ operations must not claim mutation evidence; uncertainty about a write is outside this profile.

## Outcomes and trace

Outcome is `{classification,result?,error?,trace}`. Classification is `COMPLETED`, `BUSINESS_REJECTION`, `FAILED`, or `LIMIT_EXCEEDED`. Only COMPLETED carries `result`, exactly the output record. All other classifications carry `error:{code,path}` and no result. Business rejection uses its declared business code; transient read failure uses `ADAPTER_TRANSIENT`; scope denial uses `AUTHORITY_DENIED`; validation uses `INPUT_INVALID`, `HOST_BINDING_INVALID`, or `ADAPTER_INVALID`; expression evaluation errors use `EXPRESSION_FAILED`; limits use `BUDGET_EXCEEDED`. No host exception text is returned. Loader/compile failures are diagnostics, not execution outcomes. Runtime paths use escaped JSON pointers: `/input` (optionally a safe schema field), `/hostBindings` (no protected values), `/steps/<zero-based-index>` or `/return`; invocation-wide failures use the empty pointer. Loader paths are rooted at `/artifact` or `/admission`.

Value-size violations use LIMIT_EXCEEDED/`BUDGET_EXCEEDED` at every typed boundary: business input, protected bindings, evaluated arguments, adapter results and returned output. A string above 256 UTF-8 bytes or a bounded serialized record above 8,192 bytes is a limit violation. Wrong types, missing/null/unknown fields and invalid enum members use the boundary's validation code instead. Validate scalar type/Unicode before its byte bound, then enum membership; validate record shape and fields before its serialized bound. No failure supplies a normal result or permits a later dispatch.

Trace is an ordered array of `{sequence,event,step?,capability?,code?}`; sequence starts at 0. Emit `INVOCATION_START`, then `STEP_START` for each reached step; for CALL emit `CALL_DISPATCHED` only after authorization/validation and immediately before dispatch, then `CALL_SUCCEEDED` on valid success. For successful BIND emit `VALUE_BOUND`. Finish with `RETURNED` then `TERMINATED` on success, otherwise `TERMINATED` with error code. No later step starts after failure. No values, arguments, host bindings, raw CEL, credentials or exception stacks appear. A trusted conformance adapter records exact arguments separately for assertions. Trace capacity is bounded and exceeding it terminates instead of silently dropping audit events; reserve the terminal event slot.

The host consumer receives only an outcome from its selected invocation, validates output type/classification and displays a result only on COMPLETED. Consumption receipt is separately `{status:"DISPLAYED",result:record}` or `{status:"NOT_DISPLAYED",code:string}`. It is not a runtime event or evidence of a native transaction.

## Limits

Optional `admission.limits` is a closed narrowing-only record with optional `steps`, `calls`, `totalAstNodes`, `traceEvents`. Omitted fields retain the profile ceilings below. Values must be integers within 0..8, 0..4, 0..512 and 2..64 respectively; unknown fields or widening fail preparation. Preparation rejects a plan exceeding narrowed static step/call/aggregate-AST limits; runtime also enforces dispatch/step ceilings. Trace quota is enforced during invocation: reserve termination capacity before emitting events and, before adapter dispatch, enough capacity for its dispatch/success events and termination. If capacity is insufficient, terminate with LIMIT_EXCEEDED/`BUDGET_EXCEEDED` before dispatch. Do not drop events or report a completed call whose required trace cannot fit. These four quotas do not add new source/DCM knobs or weaken any other hard cap.

These are conservative **candidate admission ceilings**, not measured performance guarantees. Qualification must confirm or revise them before the profile is accepted; a change alters the profile/environment contract and fixtures. Implementations must not accept unbounded values while evidence is pending.

| Quantity | Candidate ceiling |
| --- | ---: |
| Raw source / DCM document bytes, each | 65,536 |
| Canonical embedded DCM UTF-8 bytes | 65,536 |
| Artifact bytes | 1,048,576 |
| JSON/YAML structural nesting depth | 16 |
| DCM named types / capabilities / entrypoints | 16 / 8 / 4 |
| Fields per record / parameters per capability / enum cases | 16 / 16 / 16 |
| UTF-8 bytes in each business/model value string | 256 |
| Source steps / total calls | 8 / 4 |
| CEL source bytes per expression | 512 |
| Expanded AST nodes / depth per expression | 64 / 16 |
| Total expression AST nodes per artifact | 512 |
| Decoded descriptors / each CheckedExpr bytes | 65,536 / 16,384 |
| Serialized business input or adapter result bytes | 8,192 |
| Trace events | 64 |

Identifier and metadata strings also obey their schema bounds; artifact base64 and CEL strings use their dedicated bounds. All declared caps are enforced inclusively, with boundary+1 rejection. Limit collection sizes before materializing their contents; bound YAML parsing and protobuf decoding as well as invocation input. No attacker-controlled descriptor recursion or external resolver is allowed. Reject missing required fields and unknown fields; never truncate.

Check remaining deadline before every step, expression and adapter dispatch and after completion; never dispatch a later call after expiry. Host deadline budgets include queueing/preparation if preparation occurs in its request path; prefer prepare once outside it. Adapter and evaluator cancellation are separate. A timer around synchronous CEL or a Java interrupted thread does not demonstrate stopped computation. Pure Node expression workers may supply termination, but must not own adapter authority. Document actual qualified enforcement; do not advertise a hard deadline, complete memory isolation or production multi-tenant containment without evidence. This profile promises no CPU gas counter equivalence between libraries. Deterministic clock/budget tests compare behavior; measured wall latency is not a cross-runtime equality assertion.

For this controlled scalar checkpoint, in-process CEL with enforced checked-AST/data ceilings and before/after deadline checks is permitted; a worker pool is not required. Asynchronous authorization and adapter waits must race the remaining deadline and discard late results. This stops waiting, not necessarily the underlying work. It cannot preempt synchronous CEL and establishes no hard-deadline guarantee.

Each invocation uses fresh immutable data. A late adapter response after a deadline is discarded; it cannot append a successful result to a terminated invocation. No state/identity survives in the Prepared handle. Compilation and preparation have host-owned finite time/concurrency budgets too; CLI cancellation must reap its child rather than leave abandoned work.
