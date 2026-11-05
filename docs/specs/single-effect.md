<a id="single-effect-profile"></a>
<a id="keir-ir-single-write-behavior"></a>
# Single-write behavior

This page defines the single-write rules for source and data flow, lowered calls, outcomes, traces and event reports. These semantics apply across supported profiles unless a section explicitly gives a compatibility boundary. [Host model binding](host-model-binding.md#effect4-binding-and-preparation) defines the current API; [runtime execution](runtime.md#effect4-execution) defines invocation and limits; [host evidence and key identity](real-single-effect-admission.md#effect-outcomes-and-key-identity) defines the host's obligations. Implementation status and conformance evidence belong in [testing](../testing.md).

<a id="effect4-and-legacy-boundaries"></a>
## Single-write behavior and compatibility boundaries

The current contract uses a reference to the DCM and accepts real or simulated implementations for that model. All implementations in a binding use the same mode. Each invocation supplies fresh bindings, an absolute deadline, the original non-null key, and the typed event. The host implementations check permissions and authenticate the key. [Compatibility details and wire identifiers](../internal/implementation-reference.md#contract-compatibility)

No profile may turn a guarded second WRITE into an admitted program. A skipped write has no effect receipt. Neither a successful simulation nor a program-authored result proves a native commit. A handled business rejection may carry both a validated result and its original error, as specified below.

<a id="versions-and-inherited-rules"></a>
<a id="implementation-reference-inherited-source-and-format-rules"></a>

[Original simulated-write source and artifact](../internal/compatibility-reference.md#original-write-formats).

## Source and definite availability

Keir supports explicit nominal literals and [bounded String concatenation](language.md#types-and-cel-bridge). BIND remains `{id,let}`. CALL is `{id,call,args,when?,onError?}`; no other fields. Optional fields must be absent or valid, never null. `when` is a step identifier naming an earlier unconditional Boolean BIND, not an inline expression or a call result. All BINDs are unconditional. `onError` is a nonempty map of at most 16 declared capability business codes to `{record:{field:binding,...}}`; each record must match the entrypoint output exactly. No handler calls, retries, nested handlers, error variable or dynamic error selector.

An expression's environment contains `input`, earlier BINDs and earlier **unguarded** CALL results. A guarded CALL result is excluded from every later expression and handler environment, even under the same guard or a CEL conditional. Reject references with `DATAFLOW_UNAVAILABLE`; no `has`, optional, null placeholder or path proof is introduced. A guarded successful result is still fully validated before continuing. It need not be placed in CEL state. An unguarded CALL result is available on the continuing success path only. A handler sees the environment immediately **before its own call**, never that call's result or later bindings.

False `when` skips argument evaluation, any legacy per-call authorization callback, dispatch and handler evaluation; it consumes a reached step but no dynamic call/effect count. Invocation input, key, event and protected-binding validation still precede all steps. A true guard follows normal checks. No handler can catch a runtime authority, validation, resource, transient or uncertain failure. A host implementation's declared BUSINESS_REJECTION follows the ordinary terminal-handler rules, including when its business code denotes denied access. A declared BUSINESS_REJECTION terminates: evaluate the matching handler if present, then stop; otherwise return no result. Never execute later steps or the root return after a business rejection.

Whole-document parse/shape, DCM compatibility (and external pins for legacy APIs), and structural/authority/reference preflight precede expression checking. Preflight includes guard references, handler code membership, output shape and conservative call/effect grants. Check expressions in step order: BIND value; CALL arguments by ASCII name, then handlers by ASCII code and their fields by ASCII name; root return fields last. Each expression completes typing/overlay/profile admission before the next. Aggregate AST counts include every handler, regardless of reachability. Existing safe diagnostics apply; use `ENTRY_GRANT_DENIED` for disallowed WRITE/effect counts and `DCM_INVALID` for unsupported lifecycle/key metadata. The one-write hard ceiling uses `BUDGET_EXCEEDED`; all failures emit no artifact.

## Closed DCM additions

[Original simulated-write model and key schema](../internal/compatibility-reference.md#original-write-model).

The [current DCM schema](../../schemas/effect-4/README.md) defines the closed model. A single-write entrypoint uses `EVENT`, `AFTER_COMMIT` and an `EVENT_REPORT` consumer. Its `event.idType` and `event.sourceType` name declared nominal String types. The model declares one `hostKey` with source `requestKey` and a nonempty native key type label of at most 256 UTF-8 bytes. The key type label belongs to the host and is outside the business and CEL type systems. The [hook contract](generic-hook-capability-contract.md#optional-restrictive-controls) defines optional restrictions.

The host supplies an admitted event after the original operation committed. Nothing in the output can veto or roll back that operation. The generic runtime validates event types; the host and adapters enforce the relationship between the event source, business arguments and returned native entities. Source-selected project/title values are data, never authorization. The complete DCM digest covers capability and consumer versions and the event and key declarations.

## Artifact

The [artifact envelope](compiler-artifact.md#effect4-identity) references the bound model. Earlier embedded-model artifacts are documented in the [implementation reference](../internal/compatibility-reference.md#original-write-formats). BIND is unchanged. CALL is `{op:CALL,id,capability,version,args,resultType,when?,onError?}`. `when` retains the checked earlier Boolean BIND ID. Each `onError` value lowers to `{type:outputType,fields:{field:loweredBinding,...}}`. Root return is unchanged. Allocate expression IDs in the source order defined above. Each `variables` map is exactly the definitely available environment for that position, including unused but available variables; guarded results are never inserted. Recompute and check these environments during preparation, rather than trusting artifact declarations.

Preparation independently checks guard/handler references, all type overlays, conservative calls/writes, lifecycle, key metadata and grants. No source interpretation or new opcode is needed. Unknown instructions/fields, forged effect classifications, omitted required checks or incompatible versions fail closed before callbacks or dispatch. Keep old golden bytes and rejection corpus unchanged.

Both loaders distinguish static effect authority from the hard ceiling: a WRITE exceeding the entrypoint's zero effect grant fails `ENTRY_GRANT_DENIED` at its `/artifact/steps/<index>`; a second possible WRITE fails `BUDGET_EXCEEDED`, regardless of guards.

## Host API and native key

Use the [bound host API](host-model-binding.md#api-shapes). The [original-key identity rule](real-single-effect-admission.md#effect-outcomes-and-key-identity) applies to earlier contracts as well. [Original simulated-write host API](../internal/compatibility-reference.md#original-write-host-api).

## Effect evidence and outcomes

READ adapter outcomes are unchanged and reject any `effect` field. WRITE has exactly these tagged shapes:

| Shape | Meaning |
| --- | --- |
| `{kind:SUCCESS,value:record,effect:CONFIRMED}` | Adapter attests a committed effect (including a confirmed duplicate result); validate record before using it. |
| `{kind:BUSINESS_REJECTION,code:declaredCode,effect:NONE}` | Definite rejection with no effect; terminal handler may run. |
| `{kind:TRANSIENT_FAILURE,effect:NONE}` | Adapter establishes no effect; FAILED/ADAPTER_TRANSIENT, no handler. |
| `{kind:UNKNOWN_COMMIT,effect:UNKNOWN}` | Commit unknown; UNCERTAIN/UNKNOWN_COMMIT, no handler/result. |

An HTTP status or local timeout is not effect evidence. After dispatch, throw/rejected future, timeout, malformed envelope or incompatible effect tag leaves UNKNOWN. A valid outer SUCCESS/CONFIRMED envelope establishes confirmation **before** validating its value: malformed/oversized value then fails with CONFIRMED retained, no bound result and no later step. A valid BUSINESS_REJECTION/NONE outer envelope establishes NONE even if its code is undeclared; the invalid code fails validation and cannot invoke a handler. Extra outer fields or invalid kind/effect combinations do not establish evidence. Late responses after timeout/termination cannot revise any result or receipt.

For both runtimes, settle the adapter-response/timeout race exactly once. If the response wins while the call is active, inspect its outer envelope and record valid CONFIRMED or NONE evidence **before** completion deadline/clock checks and result/code validation. This includes NONE from valid BUSINESS_REJECTION or TRANSIENT_FAILURE envelopes. A subsequent clock reading equal to expiry yields LIMIT_EXCEEDED/BUDGET_EXCEEDED; an invalid or regressing clock yields FAILED/HOST_CONTEXT_INVALID. Both retain the settled evidence, with no normal result, handler or later step. A wait helper must not discard the winning response by checking the completion clock before this evidence settlement. If timeout wins first, the receipt remains UNKNOWN and termination is UNCERTAIN/BUDGET_EXCEEDED; a later CONFIRMED or NONE response is ignored. Winning the response race preserves evidence, not permission to continue after expiry. Pre-dispatch deadline checks remain unchanged.

Outcome for this profile is `{classification,result?,error?,effects,trace}`. `effects` is always an array of zero or one closed `{step,capability,state}` records. A skipped or undispatched write has no record. Before invoking a WRITE adapter, record intent and initialize its receipt to UNKNOWN; replace only on validated adapter evidence. Every termination preserves that receipt. INTENT is a trace transition, not a final effect state. This is an in-memory receipt, not crash durability or proof that an external operation committed.

Classification rules:

- COMPLETED has a validated result and no error; any dispatched write must be CONFIRMED. The no-op path may complete with no effect.
- BUSINESS_REJECTION always retains `error:{code,path}` for the rejected call. Only a successfully evaluated matching handler adds a validated `result`; an unhandled rejection has no result. NONE is required for a rejected WRITE.
- FAILED or LIMIT_EXCEEDED has error and no result, retaining any CONFIRMED or NONE receipt. This includes return/handler validation, authorization, clock or trace failures after a confirmed write; never report rollback.
- If a dispatched write remains UNKNOWN, terminal classification is UNCERTAIN, even when the triggering code is BUDGET_EXCEEDED, ADAPTER_EXCEPTION, ADAPTER_INVALID or HOST_CONTEXT_INVALID. Retain that code/path in error and no result. No handler or subsequent step runs.

Handler evaluation failures use their ordinary classification/code/path, with `error.cause:{code,path}` retaining the original business rejection. `cause` is allowed only for this case, never recursive. Successful handlers keep the original error with no cause. Handler field paths are `/steps/<index>/onError/<escapedCode>/<escapedField>`; other paths retain existing conventions. Successful handler evaluation emits RETURNED then TERMINATED with the business error code. Limits, shape/type/Unicode order and safe error text otherwise follow the read contract.

## Trace and consumer

Extend the existing trace shape only with optional Boolean `taken` and effect `state`. A guarded CALL emits STEP_START then GUARD_EVALUATED `{step,taken}`; false adds STEP_SKIPPED and continues. True follows normal dispatch. WRITE emits EFFECT_INTENT `{step,capability}`, CALL_DISPATCHED, then EFFECT_RECORDED `{step,capability,state}` on settlement (including timeout). Valid successful result then emits CALL_SUCCEEDED. READ success and BIND events are unchanged. No skipped result is fabricated.

Before WRITE dispatch reserve five slots: EFFECT_INTENT, CALL_DISPATCHED, EFFECT_RECORDED, possible CALL_SUCCEEDED and TERMINATED. Before READ reserve CALL_DISPATCHED, possible CALL_SUCCEEDED and TERMINATED. No asynchronous work occurs between writing intent and invoking the adapter. If dispatch has not actually begun, local failure establishes no attempted effect; never claim UNKNOWN merely because a guard was true. Settlement updates the ledger before further validation/trace work; terminal capacity and the reserved effect record cannot be consumed by other events while the sequential call is pending. Later trace exhaustion preserves the ledger. Test the exact quota boundary. Trace contains no raw arguments, native keys, event IDs, resource values or exception details.

The EVENT_REPORT consumer returns a separate receipt `{status:REPORTED,classification,result?,error?,effects}` after validating the runtime outcome against this profile. It may display a handled BUSINESS_REJECTION result **as rejection**, never as success. For UNCERTAIN or failure it displays classification/error and retained effects without an ordinary result. It may show a CONFIRMED effect beside failed execution. It never changes the original committed event, creates a write, retries or infers an effect from a source-authored `CREATED` label. The read DISPLAY consumer remains COMPLETED-only. A malformed outcome yields `{status:NOT_REPORTED,code:OUTCOME_INVALID}`.

## Ordered simulation protocol and acceptance

The ordered stub, comparison and consumer rules below apply to all effect profiles. Use the [scenario format for the selected artifact](../internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors). Fixtures provide protected inputs through the host API for that artifact.

Run the exact compiled bytes through the ordinary prepare/invoke path with explicit simulation registrations and the EVENT_REPORT consumer in **both** runtimes. Scenario data cannot supply executable callbacks, authorize real mode or change the plan. Use closed `{format,id,given,stubs,expect}` with the selected versioned format; `given` is `{input,fixture}`. `fixture` selects a trusted test-host fixture ID, not a filesystem path or callback. The host fixture constructs native key/event/bindings and deterministic clock behavior, plus any required legacy grants/authorization for its selected profile. Scenarios cannot override those protected fields. Ceiling: 65,536 raw bytes, four stubs; existing scalar/record bounds apply.

Each stub is `{step,capability,version,args,protectedArgs,response}` in exact dispatch order. `response` is one valid adapter outcome above; negative protocol tests inject malformed outcomes through the trusted test harness, not a permissive production adapter schema. Before consuming a stub, the test adapter checks actual capability/version and exact agent/protected arguments, and separately checks native key identity and protected origin against the selected fixture. The adapter context has no step label: after execution, reconcile captured calls with the trusted `CALL_DISPATCHED` trace entries by dispatch ordinal, require equal counts and capabilities, and compare each actual trace step with its stub's step. Derive reported `calls.step` from the trace, never from the expected stub or a second plan interpreter. A skipped call consumes no stub. Missing, extra, wrong-order, wrong-step, mismatched, or unconsumed stubs produce a failed simulation assertion outside runtime exception handling, even if execution otherwise returned COMPLETED. Do not turn a stub mismatch into a declared business rejection.

`expect` is exactly `{outcome,calls,consumption}`; outcome includes exact classification/result/error/effects/trace, calls is the ordered array of `{step,capability,version,args,protectedArgs}`, and consumption is the exact consumer receipt. Exclude real wall times. Harness diagnostics identify the mismatching field without leaking protected production values. Test clocks and fixture actions model timeout, denial and lost response without sleeps or live I/O. Duplicate delivery is explicitly simulated; it is not certification of real atomic deduplication or retention. No production activation path is added.

The [shared conformance corpus](../../conformance/generic-hooks/README.md) and [testing evidence](../testing.md) identify the versioned vectors. Preserve earlier profile regression fixtures and packaged-host behavior. Simulation does not establish real persistence, authority implementation, duplicate suppression, hard deadlines, exactly-once execution or recovery. Host implementations own native guarantees. Earlier qualified-write artifacts retain their [qualification protocol](real-single-effect-admission.md#legacy-effect3-qualification).
