# Pre-operation decisions

Evaluate a rule before the application performs a native operation. The rule may read and return a decision; the application owns authorization, consistency and saving.

## Decision contract

The host binds its DCM and implementations, prepares an approved program, and invokes it before a native operation. The program returns one typed decision: `ALLOW` or `DENY`. The host consumes the decision and retains its existing validation, authorization and commit path. Keir does not perform the native operation through a new callback.

Use the [binding and invocation APIs](host-model-binding.md#api-shapes), [expression rules](workflow-expressions.md), adapter responses and runtime limits. Historical REQUEST/DISPLAY is advisory and must not be relabeled as this authoritative check. EVENT/AFTER_COMMIT behavior remains unchanged.

## Hook and result

A decision entrypoint uses these fields:

| Field | Guard value and reason |
| --- | --- |
| `trigger` | `REQUEST`: synchronous interception of an operation attempt, not an already committed event. |
| `placement` | `BEFORE_COMMIT`: output must be consumed before the native effect. Existing AFTER_COMMIT cannot prevent it. |
| `consumer.kind` | `DECISION`: distinguishes authoritative permission to continue from reporting/display. Keep existing host-owned consumer `id` and `version`. |
| `output` | Host-named closed record with required `decision`, whose named enum has exactly the two distinct values `ALLOW` and `DENY`, in either order. Other host-declared bounded scalar fields, required or optional under existing rules, are permitted; for example `message:String`. Existing enum/record machinery suffices; no built-in business type. |
| `effectGrant` | Omitted means **0** for DECISION; explicit 0 is valid, 1 is invalid. The lifecycle itself forbids extension writes. Event defaults stay unchanged. |
| `event` | Retain existing `{idType,sourceType}` nominal declarations and invocation `{id,source}` transport. For REQUEST this identifies the host operation attempt and its host-defined scope; it does not assert a committed event or an existing output entity. |

Only the exact REQUEST/BEFORE_COMMIT/DECISION combination is valid. Mixed combinations fail DCM validation. The compiler and both binders validate the output shape and effect ceiling. Preparation independently rejects every potential WRITE, even behind a false guard, with `ENTRY_GRANT_DENIED`; runtime dispatch retains its zero-write check. A catalog may still publish WRITE operations for other entrypoints. No special capability allow-list is required; existing optional policy controls still apply.

An ordinary bounded message field can explain a customer-authored denial. It is data, never markup, native authority or executable recovery instructions. No Core reason-code registry, patch, next action or host-verification object is added. Only `decision` controls continuation; other fields cannot override it. The host validates the complete declared output, renders messages as plain text and uses its own text for infrastructure/native errors.

## Domain-neutral example

A host exposes `BeforeOperation` with input `{restricted:Boolean}` and output `DecisionResult`. Its types include:

```json
{
  "Decision": {"kind":"enum","values":["ALLOW","DENY"]},
  "DecisionResult": {"kind":"record","fields":{"decision":"Decision"}}
}
```

The existing source language can select enum literals directly in a typed return:

```yaml
steps:
  - id: restricted
    let: {cel: 'input.restricted'}
return:
  record:
    decision: {cel: 'restricted ? "DENY" : "ALLOW"'}
```

This is a source fragment, not a complete compile-ready artifact. A second shared example reads a host capability returning `{restricted:Boolean}` before selecting the decision. Neither example contains a domain mutation. Both use the same artifact bytes in Java and Node.

## Consumption and failure policy

The host establishes selection state independently of the runtime result. Exactly one selected approved program is supported. Absence is not represented by a null prepared handle, a thrown load error or a fabricated ALLOW result.

| Observed state | Host behavior |
| --- | --- |
| Authoritatively no active binding | Continue ordinary native validation/operation. This hook is an optional customization. |
| Binding exists but artifact, package, approval or implementation is unavailable/incompatible | Block; never reinterpret as no binding. |
| Valid `COMPLETED`, fully typed result with `decision:"ALLOW"`, empty `effects` | Eligible to continue, subject to fresh host checks and deadline. |
| Valid `COMPLETED`, fully typed result with `decision:"DENY"`, empty `effects` | Deny; do not attempt the native mutation. |
| `BUSINESS_REJECTION`, including a handler returning ALLOW | Block. A handled capability rejection is not a successfully completed guard. |
| FAILED, LIMIT_EXCEEDED, UNCERTAIN, malformed result/outcome, nonempty effects, exception | Block. Preserve the actual failure for diagnostics. |
| Cancellation, expired deadline, changed selection or stale read facts | Block; discard the permission to continue. No automatic reevaluation loop. |

A mandatory guard with no-binding-denies semantics is outside this optional-customization slice; it must not be implied by the default above.

The sample consumer is a pure host helper, `consumeDecision(outcome)`, with the same behavior in Java and Node examples. It returns `{status:"ALLOWED",result}`, `{status:"DENIED",result}` or exactly `{status:"BLOCKED"}`. The teaching model declares only required `decision` and optional `message:String`; its helpers reject other result fields. A host with a different output validates the complete record using its already selected model's validator, captured at binding/setup rather than passed anew by the caller. That consumer retains its validated declared fields. It also validates the closed outcome, empty effects, classification and consistent trace using the existing consumer validation pattern. A failed validation returns BLOCKED rather than throwing through to a native fallback. No per-call DCM, digest, entrypoint, consumer identity or compiler argument is added to this helper. This is example host code, not a new runtime export. See [runtime decision consumption](runtime.md#decision-consumption) for the concrete validation blocks.

ALLOWED is permission to proceed, **not evidence that the native operation committed**. Keep consumer output, runtime outcome and native result separate. The host must never consume an outcome supplied by the customer as if it came from its invocation. Do not introduce a signed permission token or new Core receipt service to join these steps: the host's local control flow suffices.

## Host invocation and consistency obligations

1. Authenticate, resolve scope and validate/normalize the proposed operation through existing host code. Snapshot the proposed values; they must not change after guard evaluation. The host chooses the source-visible projection. Protected actor, provenance and original request key remain outside authored arguments.
2. Resolve the authoritative current selection, including explicit absence. Bind/prepare can be cached by their existing exact identities; caller authority, proposed values and deadlines cannot be cached there. Read-only plans, including no-call plans, still require native authorization before commit.
3. Invoke with existing business input, protected bindings, original key, event context and deadline. Host READ adapters authorize every lookup and return their declared bounded projection. They must not cause a native business write or recursively invoke customized response pipelines. A READ declaration is a host promise, not a sandbox around arbitrary host code.
4. Consume the outcome. DENIED/BLOCKED ends this operation attempt without native mutation.
5. Recheck selection/approval, authorization, native invariants and mutable facts used to justify ALLOW at the native commit boundary. A changed fact or selection blocks the attempt. Then perform the existing native operation once. No remote evaluation while holding a long-lived native write transaction.

Keir does not offer a universal snapshot guarantee. A host may revalidate locally read values/predicates inside its native atomic transition. For remote facts it needs an actual host protocol that establishes the required consistency; otherwise that authoritative use is unsupported. A prior successful read or a hash of its result does not establish commit-time truth. The host must specify its consistency boundary and test races against it.

The same original key passes through READ adapters unchanged. Keir checks only that the key is non-null, not its authenticity or native shape. Protected bindings contain only the names required by actual planned calls; a no-call ALLOW requires no actor binding in Core. The host must still authenticate actor, key and provenance before native continuation, including for a no-call plan. Keir does not append step or guard identifiers. The host may allocate an operation-attempt identity before any output entity exists. It must not forge a committed event or accept provenance selected by the extension.

## Time, cancellation and nesting

Use the existing absolute deadline and clock. Let `D` be the enclosing operation expiry and `R > 0` the host's measured reserve for validation and native commit. Guard expiry is at most `D - R`; if there is no positive guard budget, block before invocation. Do not reset a full budget after a read, on nested invocation or before commit. Native work still checks `D`. The reserve is host configuration, not a new DCM field or runtime parameter; it is not a guarantee that an arbitrarily stalled commit finishes.

Cancellation belongs to the host operation lifetime. Mark that attempt closed and prohibit native continuation; cancel host transport where supported. Existing runtime timeout settles once and ignores late adapter output. A late ALLOW must not reopen a cancelled/expired attempt. No new Core cancellation API is needed for this slice. Synchronous trusted code may keep running; do not claim hard CPU preemption.

A post-commit extension may call one native WRITE whose host operation has a guard. That nested guard is READ-only. The host bounds synchronous depth and aggregate work across this chain and passes the enclosing expiry. Ordinary READ adapters do not re-enter guards. This adds no second extension WRITE and does not erase the outer write's evidence: if it times out after dispatch, its receipt can remain UNKNOWN even when the inner guard was read-only. Native reconciliation remains host-owned.

<a id="compatibility-and-release-proposal"></a>
## Compatibility

Select the [exact model/artifact/producer combination](compiler-artifact.md#source-and-artifact-identity). Keep existing approved artifacts, model snapshots and receipts with their original identities. Recompiling creates new bytes that need their own checks and approval; installing a package must not silently recompile, transfer approval or deactivate an existing selection.

<a id="acceptance-matrix-and-implementation-split"></a>
## Verification

Run the same compiled artifact through Java and Node and compare complete outcomes, calls and decision consumption. Exercise ALLOW, DENY, failed and malformed reads, forged plans/outcomes, unavailable selections, cancellation/expiry and changed permissions or facts. Observe native attempts and persistence separately: DENIED/BLOCKED cause no native attempt; ALLOWED reaches fresh native validation once and may still fail there. Check nested read-only guards without losing the outer write's evidence.

The [original acceptance matrix](../internal/history/read-and-decide-design.md#acceptance-matrix-and-implementation-split) retains the initial rollout vectors. Each host must still qualify its own read-consistency mechanism, time reserve, cancellation and native persistence; simulation cannot establish those native guarantees.

<a id="compatibility-and-releases"></a>
<a id="dcm-delta"></a>
<a id="smallest-contract"></a>
