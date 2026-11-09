# Pre-operation decisions

Status: **released in [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0)**. Original design baseline: Core `a79ceba`. This contract serves [pre-operation-guard](../../roadmap/features/pre-operation-guard.md). The separate [Daymark integration proposal](../internal/daymark-pre-operation-integration.md) is host-specific, not part of this contract.

## Smallest contract

The host binds its DCM and implementations, prepares an approved program, and invokes it before a native operation. The program returns one typed decision: `ALLOW` or `DENY`. The host consumes the decision and retains its existing validation, authorization and commit path. Keir does not perform the native operation through a new callback.

Reuse the current [binding and invocation APIs](host-model-binding.md#api-shapes), [Keir 2 expressions](workflow-expressions.md), adapter responses, limits and outcome envelope. No new source instruction, runtime method, registration layer, policy engine, database, patch or multiple-binding composition is needed. Historical REQUEST/DISPLAY is advisory and must not be silently relabeled as a guard. Existing EVENT/AFTER_COMMIT behavior remains unchanged.

At the inspected baseline, existing custom enums/records can already express ALLOW/DENY and additional bounded output fields; `effectGrant:0` can already prohibit writes; Outcome validation can distinguish completion from failure. What they cannot express is a supported pre-operation lifecycle: `compiler-java/.../Dcm.java`, Java `Artifact.java` and Node `validation.js` explicitly require EVENT/AFTER_COMMIT/EVENT_REPORT. Invoking that declared event before commit would contradict its contract. The change is this lifecycle/consumer alternative plus its validation, not a new execution mechanism. The artifact schema also fixes `modelRef.format` to `keir-dcm/2`; a new DCM contract therefore changes the artifact's accepted model-reference structure. The scenario schema only admits event-report consumption. These concrete constraints justify the version changes below; status labels alone would not.

## DCM delta

A new entrypoint alternative uses the existing fields:

| Field | Guard value and reason |
| --- | --- |
| `trigger` | `REQUEST`: synchronous interception of an operation attempt, not an already committed event. |
| `placement` | `BEFORE_COMMIT`: output must be consumed before the native effect. Existing AFTER_COMMIT cannot prevent it. |
| `consumer.kind` | `DECISION`: distinguishes authoritative permission to continue from reporting/display. Keep existing host-owned consumer `id` and `version`. |
| `output` | Host-named closed record with required `decision`, whose named enum has exactly the two distinct values `ALLOW` and `DENY`, in either order. Other host-declared bounded scalar fields, required or optional under existing rules, are permitted; for example `message:String`. Existing enum/record machinery suffices; no built-in business type. |
| `effectGrant` | Omitted means **0** for DECISION; explicit 0 is valid, 1 is invalid. The lifecycle itself forbids extension writes. Event defaults stay unchanged. |
| `event` | Retain existing `{idType,sourceType}` nominal declarations and invocation `{id,source}` transport. For REQUEST this identifies the host operation attempt and its host-defined scope; it does not assert a committed event or an existing output entity. |

Only the exact REQUEST/BEFORE_COMMIT/DECISION combination is added. Mixed combinations fail DCM validation. The compiler and both binders validate the output shape and effect ceiling. Preparation independently rejects every potential WRITE, even behind a false guard, with `ENTRY_GRANT_DENIED`; runtime dispatch retains its zero-write check. A catalog may still publish WRITE operations for other entrypoints. No special capability allow-list is required; existing optional policy controls still apply.

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
## Compatibility and releases

Identities version independently. Core 0.3.0 and companion edition 5 are published together on the [Core 0.3.0 release](https://github.com/rore/keir-core/releases/tag/v0.3.0). Their contract identities are:

| Contract | Value | Reason |
| --- | --- | --- |
| Source | Keep `keir/2` | No source syntax/type/step change. |
| DCM | `keir-dcm/3` | New lifecycle/consumer semantics and conditional effect default. |
| Artifact | `keir-artifact/3` | Newly admitted entrypoint execution meaning; retain envelope/step representation. |
| Descriptor recipe | Keep `keir-record-descriptors/2` | No value or protobuf representation change. |
| Scenario | `keir-scenario/3` | New decision-consumption variants; retain outcome/call envelope. |
| Compiler/packages | Core `0.3.0`, producer `keir-compiler-java/0.3.0` | Released compiler/runtime pair for this contract. |
| Authoring companion | Edition `5` | Published with verified packages; covers pre-operation decisions and failure boundaries. |

Keep dependency pins unchanged unless implementation evidence requires a separate change. Compiler 0.3.0 is the single current authoring compiler. It accepts source `keir/2` with DCM `/3` only, producing artifact `/3` with its truthful 0.3.0 identity. It rejects DCM `/2`; it never stamps a 0.2.0 producer. Old `/2` artifacts are preserved for execution/history only, not a parallel authoring path. Runtime 0.3.0 admits exactly (artifact `/2`, DCM `/2`, producer 0.2.0, descriptors `/2`) and (artifact `/3`, DCM `/3`, producer 0.3.0, descriptors `/2`), with the existing exact environment pins in both. Reject mixed tuples and guard semantics in `/2`. This needs two explicit supported cases, not a compatibility framework.

Existing approved post-commit artifacts/model snapshots stay on their old exact pair when the runtime package is upgraded. Do not rewrite approvals or receipts. Publish the current host authoring catalog as `/3`, including unchanged EVENT/AFTER_COMMIT/EVENT_REPORT semantics alongside the new guard alternative. New or edited programs compile against that current catalog and require normal approval before replacing an old selected rule. The host may retain exact old model bindings solely to execute existing approved rules; this reuses existing model binding and is not multiple selected programs at one hook. Historical source can seed a new draft, but old approval never transfers to its recompiled bytes. Package/provenance validation must explicitly recognize the new runtime's support for the old pair rather than fabricate old package integrity. No silent deactivation or recompilation of selected rules during package installation.

Update domain-model, host-model-binding, runtime, compiler-artifact, spec index, schemas, example consumers and release compatibility matrix together during implementation. Update `docs/agents/host-integration-assistant.md`, `extension-assistant.md`, the new customer guide and companion compatibility record: discover the exact hook, compose READs, return a decision, test DENY/failure and native consumption, and never infer native commit from ALLOW. Core 0.3.0 includes these reference and package-record updates.

## Acceptance matrix and implementation split

Each runtime case uses one compiler-produced artifact/model pair in both runtimes; compare classification, result, effects, ordered calls and consumption. Forged artifacts are tested at preparation, not only compiler rejection.

| Cases | Required evidence |
| --- | --- |
| Constant/input-only ALLOW and DENY; READ-derived ALLOW/DENY | Same typed outcome and consumer status in Java/Node; effects empty. |
| All existing `/2` event cases | Unchanged result, call/write ceilings and receipts, including failed-after-CONFIRMED and UNKNOWN. |
| Wrong lifecycle tuple, wrong enum/output fields, effectGrant 1 | DCM/compiler/binding rejection. Both ALLOW/DENY enum orders are accepted; missing, duplicate or extra values reject. Reordering still changes the exact model digest. |
| WRITE, including false guarded WRITE, forged prepared plan | Reject before any adapter dispatch. |
| Denied/transient/malformed READ; handled rejection with ALLOW | BLOCKED; zero native mutation attempts. |
| Missing/extra/mistyped result, unknown decision, undeclared message, forged trace or effects | Consumer BLOCKED. Declared message round-trips safely as data in both runtimes. |
| Expiry before start/during READ/after result; regressing clock; cancellation and late ALLOW | No native continuation; late settlement cannot change terminal outcome. |
| Missing/mistyped input, required protected binding or event shape; null original key | Core validation error before dispatch. No-call plans legitimately have no actor binding. |
| Forged/non-null native key, wrong owner or unauthenticated provenance, including pure ALLOW | Host authentication/commit gate blocks native continuation; do not expect Core to authenticate them. |
| Compiler 0.3 with DCM `/2`; mixed artifact/model/producer pair; `/2` guard | Reject. Existing compiler 0.2 `/2` artifacts and new compiler 0.3 `/3` artifacts both run on runtime 0.3 with their exact models. |
| Explicit no binding versus selected-but-unavailable | Native path only for verified absence; unavailable blocks. |
| Changed input, approval, selection, native permission or read fact before commit | Host blocks; race tests prove no native commit. |
| Native failure after ALLOWED | ALLOWED retained only as decision consumption; native failure reported separately. |
| Nested read-only guard under a post-commit WRITE | Bounded host work/depth; one native write; outer evidence preserved. |

The generic host harness supplies an independently observed native-operation counter/result, separately from the consumer status. Tests assert DENIED/BLOCKED cause zero attempts and ALLOWED reaches native validation exactly once. Simulation proves the harness behavior only. Real-host acceptance must additionally observe native persistence/non-persistence and unchanged authorization, concurrency and replay semantics. No new Core durable receipt format is required.

Compiler/Java owner: DCM validation, compiler identity selection, shared fixtures/schema updates, Java binding/preparation/runtime checks and Java consumer/harness. Node owner: same exact model/artifact pairs, validation and runtime checks, Node consumer/harness, parity and packaged execution. Host owner: activation/absence semantics, creation paths, consistent READ adapters, deadline reserve, replay/nesting, real persistence and UI/API tests. Manager owns independent review, package allocation and combined release acceptance.

Core 0.3.0 has passed contract review and packaged Java/Node verification. Each host integration must still qualify its read-consistency mechanism, time reserved for native checks and commit, cancellation and native persistence. Shared simulation and packaged examples establish Core and harness behavior; real-host guarantees require host-specific evidence.
