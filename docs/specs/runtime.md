<a id="read-and-decide-runtime"></a>
# Runtime execution and limits

This page defines how the runtime executes a prepared program. See [Host model binding](host-model-binding.md#bind-the-dcm-and-prepare-a-compiled-rule) for Java and Node APIs and [Compiler and artifact](compiler-artifact.md) for the artifact format.

Implementation evidence belongs in [testing](../testing.md); pending resource qualification belongs in the [resource roadmap](../../roadmap/features/runtime-resource-qualification.md). Neither changes the limits or guarantees specified here.

<a id="corrected-effect4-invocation-boundary"></a>
<a id="effect4-execution"></a>
<a id="single-write-execution"></a>
## Program execution

1. Validate the [fresh invocation context](host-model-binding.md#invocation-inputs-and-adapter-context), including deadline, input, protected bindings, event and presence of the original key. Start with only the validated business record in the source environment as `input`.
2. Visit steps in source order. A BIND evaluates and saves its value. A CALL with a false guard skips argument evaluation and dispatch, binds no result and consumes no dynamic call or write budget. It still counts as a reached step. Static validation counts every possible call and write regardless of its guard.
3. For a reached call, check limits and the DCM's restrictions, validate arguments and dispatch once. The implementation receives business and protected arguments separately. Validate a successful response's result against the declared type. Retained producers make it available only for unguarded calls. The exact development 0.4.0 producer also retains successful guarded results under the scalar proof rules below.
4. A declared business rejection ends step execution. Evaluate its matching handler, if present, in the environment immediately before the call and validate the handler's output. A handler does not catch malformed responses, runtime failures, limits or uncertain effects. On the normal path, evaluate and validate the root return after all steps. A failure stops further steps and dispatch.

The same artifact and exact model can run against separately bound real or simulated implementations. Simulation reports what its own implementations did. It supplies no evidence of a native commit.

### Guarded-result activation

For producer `keir-compiler-java/0.4.0`, retain each complete validated successful result at most once in invocation state, after existing response/value/deadline checks. A skipped CALL leaves its result absent. Do not fabricate null, empty/default protobuf messages or placeholder records. Availability follows the exact [conditional guard proof](language.md#steps-and-data-flow), not mere declaration.

Build each CEL activation from actually bound values. For a conditional result whose known guard is false, omit the name without reading or converting any supplied value for it. For a true guard on a continuing path, require a validated result, including unused declarations. Required ordinary values and Boolean guard values must also be present and valid. Missing required state fails with `EXPRESSION_FAILED` at the expression binding before CEL presence tests; it is not optional-field absence or a reason to select fallback. Convert only present validated record values. The pinned engines evaluate only the selected conditional arm; static validation still checks every arm.

Fresh invocation state prevents a prepared artifact from leaking a prior result into later true/false or mixed-guard invocations. At most four complete result records are retained, each within the existing 8,192-byte cap, including validation of bounded list fields that guarded expressions cannot select. The existing declaration, AST, value and deadline ceilings remain unchanged. Measured conversion and retention costs are engineering evidence, not an enforced process-memory or CPU-preemption guarantee.

Rejection, transient failure, invalid/oversized response, exception, expiry and unknown commit terminate normal continuation; none becomes an absent result or successful fallback. Preserve receipt settlement before later value/deadline failure. An own-call rejection handler never sees its rejecting result; a later handler can use earlier successful results only with an explicit valid proof, and still terminates as BUSINESS_REJECTION. Result retention adds no trace event or native authority. Older tuples retain their original behavior.

<a id="effect-evidence-and-outcomes"></a>
## Adapter responses

The DCM determines whether a call is a READ or WRITE. Responses are closed records with the fields shown. `record` matches the operation's declared result type. `declaredCode` is one of the operation's declared business errors.

| Kind | READ response | WRITE response |
| --- | --- | --- |
| Success | `{kind:SUCCESS,value:record}` | `{kind:SUCCESS,value:record,effect:CONFIRMED}` |
| Business rejection | `{kind:BUSINESS_REJECTION,code:declaredCode}` | `{kind:BUSINESS_REJECTION,code:declaredCode,effect:NONE}` |
| Transient failure | `{kind:TRANSIENT_FAILURE}` | `{kind:TRANSIENT_FAILURE,effect:NONE}` |
| Unknown commit | Unsupported | `{kind:UNKNOWN_COMMIT,effect:UNKNOWN}` |

An adapter exception or rejected asynchronous response fails with `ADAPTER_EXCEPTION` unless a deadline or clock failure takes precedence. Malformed responses and undeclared business codes use `ADAPTER_INVALID`. A response that exceeds a size limit uses `BUDGET_EXCEEDED`. Transient failure uses `ADAPTER_TRANSIENT`, and an unknown commit uses `UNKNOWN_COMMIT`. The runtime never retries a call or invocation.

## Write evidence

A dispatched write starts with an UNKNOWN receipt. NONE means the implementation established that this attempted dispatch caused no effect. CONFIRMED means the implementation reported a commit or supplied authoritative confirmation of the same native action. UNKNOWN means the outcome of the attempted write cannot be established. An HTTP status, thrown error or local timeout alone cannot establish either definite state.

The adapter response and the timeout settle once. If the response wins, valid outer CONFIRMED or NONE evidence is recorded before the completion-clock check or validation of the returned value or error code. A malformed or oversized payload after confirmation therefore preserves CONFIRMED even though the invocation fails. An undeclared business error code with a valid outer NONE envelope preserves NONE and cannot invoke a handler. Malformed outer evidence leaves the receipt UNKNOWN.

If a timeout wins first, the runtime stops waiting and retains UNKNOWN. A later response cannot revise the outcome or continue the program. Expiry after definite evidence retains that evidence while terminating with the deadline error. The same applies to an invalid or regressing clock. The host owns native reconciliation and any subsequent retry decision.

## Outcome

For a prepared program the outcome is `{classification,result?,error?,effects,trace}`. `effects` is a zero-or-one array of closed `{step,capability,state}` records. A skipped or undispatched write has no record. A decision program has no possible writes and always has empty effects. The receipt captures runtime evidence in memory. Durable storage and native guarantees belong to the host.

| Classification | Result and error | Effect evidence |
| --- | --- | --- |
| `COMPLETED` | Validated result | No receipt or CONFIRMED |
| `BUSINESS_REJECTION` | Original `{code,path}`. A successful matching handler also returns its validated result. | A rejected WRITE has NONE. A later READ rejection preserves a prior CONFIRMED write. |
| `FAILED`, `LIMIT_EXCEEDED` | Error. `LIMIT_EXCEEDED` uses `BUDGET_EXCEEDED`. | Retains any definite receipt. A later failure does not undo a confirmed write. |
| `UNCERTAIN` | Error for the underlying failure | A dispatched write remains UNKNOWN. This classification supersedes another failure classification. |

A failed handler retains the original rejection as `error.cause:{code,path}` beneath the handler's own error. The cause never nests. Handler expression paths are under `/steps/<index>/onError/<escapedCode>/<escapedField>`. A successful handler returns the original error without a cause. No later step or root return runs after a rejection.

<a id="trace-and-consumer"></a>
## Trace and event report

Every trace entry has a zero-based `sequence` and an `event`. The first event is `INVOCATION_START`. The final event is `TERMINATED`, with `code` on non-success.

| Event | Extra fields |
| --- | --- |
| `STEP_START`, `VALUE_BOUND`, `STEP_SKIPPED` | `step` |
| `GUARD_EVALUATED` | `step`, Boolean `taken` |
| `EFFECT_INTENT`, `CALL_DISPATCHED`, `CALL_SUCCEEDED` | `step`, `capability` |
| `EFFECT_RECORDED` | `step`, `capability`, `state` |
| `RETURNED` | None |

A skipped CALL emits `STEP_START`, `GUARD_EVALUATED` and `STEP_SKIPPED`. A WRITE emits `EFFECT_INTENT` immediately before `CALL_DISPATCHED`, then `EFFECT_RECORDED` on settlement, including timeout. A validated successful response emits `CALL_SUCCEEDED`. The normal return or a successful handler emits `RETURNED`. Successful handler execution terminates with the original business rejection code.

Before dispatch reserve room for dispatch, possible success and termination. A WRITE also reserves intent and effect-record events, for five slots total. Insufficient capacity fails before dispatch. Once dispatch begins, the runtime preserves its write receipt through later trace or result failures. Trace records contain approved IDs and safe codes. They exclude raw arguments, keys, event IDs, resource values and exception details.

The host's `EVENT_REPORT` consumer validates the outcome before reporting it. The [shared example consumer](../../examples/node-host/effect-consumer.mjs) returns `{status:REPORTED,classification,result?,error?,effects}` for a valid outcome or `{status:NOT_REPORTED,code:OUTCOME_INVALID}` for an invalid one. Report a handled rejection as a rejection and preserve a confirmed receipt beside a later failure. A result such as `CREATED` is a business value, separate from the receipt. Reporting does not execute an action or undo the original event.

## Decision consumption

For a REQUEST/BEFORE_COMMIT/DECISION entrypoint, the same execution and outcome rules apply with a write ceiling of zero. The compiler and preparation reject every potential WRITE, including a false-guarded call, with `ENTRY_GRANT_DENIED`. Dispatch enforces the ceiling independently.

The host validates the full outcome, its declared closed result type, classification, consistent trace and empty effects before consuming a decision. The example host helper `consumeDecision(outcome)` returns `{status:ALLOWED,result}` for valid COMPLETED ALLOW, `{status:DENIED,result}` for valid COMPLETED DENY, or exactly `{status:BLOCKED}` otherwise.

The [Node](../../examples/node-host/decision-consumer.mjs) and [Java](../../examples/java-host/src/main/java/dev/keir/examples/DecisionConsumer.java) helpers are specialized to the teaching model: required `decision` and optional `message:String`. They reject other fields. Core permits additional declared bounded scalar fields in a decision model; a host using them supplies matching result validation in its own consumer. Capture that validator from the selected model at setup and validate the complete result before reading `decision`. The result-validation block in these helpers shows where the teaching model is checked. This is host code, not a runtime export or a per-call model argument.

A handled BUSINESS_REJECTION with a result containing ALLOW still blocks. Failures, resource limits, uncertain outcomes, malformed results and nonempty effects also block. A selected extension that cannot be invoked is not equivalent to an absent selection. The host never treats a customer-supplied outcome as its invocation result.

ALLOWED permits fresh native checks; it supplies no evidence of a native commit. Selection, approval, authorization, proposed input, read consistency, cancellation and the native deadline remain host obligations. The host records the native result separately. See [pre-operation decisions](pre-operation-decisions.md#consumption-and-failure-policy) and the [integration walkthrough](../decision-walkthrough.md).

## Limits

The compiler and runtime enforce these ceilings at the applicable boundaries. The current binding and invocation APIs use fixed runtime limits. The DCM can [restrict capability use](domain-model.md#call-and-write-policy). Measured performance belongs in the [verification record](../testing.md).

| Quantity | Ceiling |
| --- | ---: |
| Raw source / DCM document bytes, each | 65,536 |
| Canonical DCM UTF-8 bytes | 65,536 |
| Artifact bytes | 1,048,576 |
| JSON/YAML structural nesting depth | 16 |
| DCM named types / capabilities / entrypoints | 16 / 8 / 4 |
| Fields per record / parameters per capability / enum cases | 16 / 16 / 16 |
| UTF-8 bytes in each business/model value string | 256 |
| Source steps / total calls | 8 / 4 |
| Possible writes | 1 for event extensions; 0 for decisions |
| CEL source bytes per expression | 512 |
| Expanded AST nodes / depth per expression | 64 / 16 |
| Total expression AST nodes per artifact | 512 |
| Decoded descriptors / each CheckedExpr bytes | 65,536 / 16,384 |
| Serialized business input or adapter result bytes | 8,192 |
| Trace events | 64 |

Identifier and metadata strings also obey their schema bounds; artifact base64 and CEL strings use their dedicated bounds. All declared caps are enforced inclusively, with boundary+1 rejection. Limit collection sizes before materializing their contents; bound YAML parsing and protobuf decoding as well as invocation input. No attacker-controlled descriptor recursion or external resolver is allowed. Reject missing required fields and unknown fields; never truncate.

## Deadlines

The clock and expiry use the same millisecond time base. Both are nonnegative integers at most `9,007,199,254,740,991`. Clock readings must be nondecreasing within each invocation. A thrown, invalid or regressing clock is `HOST_CONTEXT_INVALID`. Equality at expiry is expired and reports `BUDGET_EXCEEDED`.

Check time at invocation entry and before each step, expression and dispatch, after evaluating expressions and when an adapter completes. The runtime races asynchronous adapter waiting against the remaining budget. Expiry stops waiting and prevents later dispatch. Synchronous CEL and host work already in progress may continue. The host is responsible for its transport and native commit deadline, including time spent in its own queues. The [limits](#limits) bound program structure and values. CPU preemption and process isolation require separate controls.

<a id="legacy-read1-execution-and-profile-extensions"></a>
<a id="implementation-reference-earlier-execution-contracts"></a>
<a id="prepare-and-invoke"></a>
<a id="outcomes-and-trace"></a>

[Earlier runtime execution contracts](../internal/history/compatibility-reference.md#earlier-runtime).

In the earlier admission APIs, optional `admission.limits` is a closed narrowing-only record with optional `steps` (0..8), `calls` (0..4), `totalAstNodes` (0..512) and `traceEvents` (2..64). Omitted fields retain the profile's ceilings. Preparation enforces narrowed static step, call and AST limits. Execution checks the narrowed step, call and trace limits, with the required trace capacity reserved before dispatch. These quotas leave every other hard cap in force.
