# Host capability binding: correction proposal

Status: **proposed; no source or normative API change released**. This replaces the architect's earlier suggestion to retain a separate binding-currentness callback. The user's clarified architecture is the starting point, not the qualification protocol being removed. Owning record: [host-contract-alignment](../../../.agent-workflow/tasks/host-contract-alignment.md).

## The design

1. The host defines capabilities in its DCM and supplies their implementations when initializing Keir.
2. Keir validates the DCM and checks that every required implementation is present and compatible. Invalid setup fails before a usable binding exists.
3. Keir prepares an extension against that binding and reuses the prepared extension.
4. On a host-selected invocation, Keir evaluates the extension within the declared types, operations and limits, calls the registered implementations and reports the outcome.
5. The host owns permissions, business rules, persistence, transport and the decision to run an extension. Its implementations enforce their actual contracts.

Conceptual pseudocode, not a claim about current exported signatures:

```javascript
runtime = Keir.bind(dcm, {
  "tasks.get": (args, context) =>
    taskService.get(context.user, args.taskId),
  "tasks.create": (args, context) =>
    taskService.create(context.user, args.projectId, args.title, context.requestId)
});

extension = runtime.prepare(approvedArtifact);

// The host chooses the extension and constructs fresh trusted context.
result = await extension.run({ taskId }, { user, requestId, deadline });
```

Keep the existing bind/prepare/invoke API families where they implement these steps. Existing static identity/version/mode metadata may remain where it prevents a concrete mismatch; it must not become another approval service or deployment certificate. This proposal does not require a new fluent wrapper, registry, cache framework or source language.

## Concrete corrected API proposed for review

These are proposed signatures, not currently exported functionality. Keep the existing function names, opaque `BoundModel`/`Prepared`, diagnostic result wrappers and execution engine. Select the corrected contract explicitly with `keir-effect/4`, DCM format `keir-dcm/effect-4`, artifact format `keir-artifact/effect-4`, and DCM assurance `HOST_BOUND_SINGLE_WRITE`. The number distinguishes incompatible host API semantics from distributed `/3`; it does not add execution features. Keep the existing single-write language, descriptor recipe, CEL environment, event metadata and result/receipt vocabulary. Compiler selection and generated artifact profile must use the same `/4` identity. Never recognize this contract by the absence of a qualification field.

### Node

```typescript
bindModel(dcmBytes: Uint8Array, implementations: Record<string, {
  version: string, mode: "simulation" | "real", keyType: string,
  invoke: (args: Record<string, unknown>, protectedArgs: Record<string, unknown>,
           remainingMs: number, context: {
             requestKey: unknown, event: { id: unknown, source: unknown },
             deadline: { clock: { nowMs(): number }, expiresAtMs: number }
           }) => unknown | Promise<unknown>
}>): { boundModel: BoundModel | null, diagnostic: Diagnostic | null }

prepareProgram(artifactBytes: Uint8Array, boundModel: BoundModel):
  { prepared: Prepared | null, diagnostic: Diagnostic | null }

invokeProgram(prepared: Prepared, businessInput: unknown, invocation: {
  bindings: Record<string, unknown>,
  deadline: { clock: { nowMs(): number }, expiresAtMs: number },
  requestKey: unknown, event: { id: unknown, source: unknown }
}): Promise<Outcome>
```

`Diagnostic` and `Outcome` denote the existing result shapes. The corrected API has no binding-approval or preparation-admission object, repeated scope/profile fields, or per-preparation/per-invocation policy knobs. Derive the profile from the DCM. Enforce the DCM's operation/effect ceilings and existing runtime resource limits internally. All implementation registrations must have the same mode. A bound handle identifies its own binding; a caller-supplied scope string adds no authentication. These two-argument bind/prepare forms accept only the corrected profile; legacy forms remain explicit legacy APIs. Reject extra arguments rather than ignoring old credentials.

### Java

Use two-argument forms and the existing result classes:

```java
public static ModelBinding bindModel(byte[] dcmBytes,
    Map<String,BoundEffectAdapter> implementations);
public static Preparation prepareProgram(byte[] artifactBytes,
    BoundModel boundModel);
```

Add the following typed equivalents for the corrected call contract, alongside unchanged legacy types. Names describe their role, not another framework:

```java
@FunctionalInterface
public interface BoundEffectFunction {
    CompletionStage<?> invoke(Map<String,Object> args,
        Map<String,Object> protectedArgs, long remainingMs, BoundEffectContext context);
}
public record BoundEffectAdapter(String version, String mode,
    String keyType, BoundEffectFunction invoke) {}
public record BoundEffectContext(Object requestKey,
    Map<String,Object> event, Deadline deadline) {}
public record BoundEffectInvocation(Map<String,Object> bindings,
    Deadline deadline, Object requestKey, Map<String,Object> event) {}
public static CompletionStage<Outcome> invokeBoundEffectProgram(Prepared prepared,
    Object businessInput, BoundEffectInvocation invocation);
```

Reuse the existing `Deadline(LongSupplier clock, long expiresAtMs)`. `/4` implementation-map values must be `BoundEffectAdapter`, including READ capabilities in the effect catalog. Existing `invokeEffectProgram` must reject a `/4` prepared handle; `invokeBoundEffectProgram` must reject older handles. A distinct Java method name preserves existing calls with null invocation arguments, which an unrelated record overload would make ambiguous. Both methods use the same execution engine. Legacy binding/preparation forms reject `/4`. No old record constructor or legacy callback contract changes. Context event maps are immutable snapshots, while the original opaque request key is passed by reference. The implementation returns the existing adapter response envelope through a completion stage; it does not return a new receipt type.

### What each field does

| Field or group | Requirement and behavior |
| --- | --- |
| DCM bytes | Validate the complete catalog/types. Derive profile, canonical model digest, domain identity and descriptor recipe from the DCM; no repeated binding metadata. |
| Registration `mode` | Separate fake and real implementations. Reject mixed registrations; never switch a prepared binding's mode. The same artifact can be prepared separately against fake and real bindings. |
| Registration ID, `version`, `invoke` | Exact complete DCM catalog, matching declared versions and actual callables, including unused capabilities. Missing or extra registrations fail initialization. Runtime cannot prove a callable's business semantics from its function signature. |
| Registration `keyType` | Match DCM `hostKey.type`; catches a miswired implementation. No repeated `keyBinding` object and no authenticity callback. A non-null opaque key is required on invocation; its actual authenticity belongs to the host implementation. |
| Artifact bytes | Read entrypoint, consumer contract and compiler identity from the artifact/DCM; check supported compiler/environment and exact bound-model compatibility internally. Compute digests internally. No repeated host-supplied metadata. A host that verifies previously approved stored bytes may compare its own stored digest before calling Keir; that is not a required preparation input. |
| Internal policy/limits/effect allowance | Enforce DCM operation ceilings and runtime bounds, statically and at reached calls. No additional configuration knobs in the corrected API without a demonstrated requirement. |
| Business input vs `bindings` | Customer program input stays separate from trusted host-bound parameters. Preserve existing DCM types and exact required binding-field validation; `protectedArgs` contains the reached capability's host-bound arguments. |
| Event and request key | Preserve typed event identity/source and the exact original opaque key, separate from authored arguments. Do not serialize or copy the key into CEL, traces or outcome documents. |
| Deadline and `remainingMs` | Runtime keeps its existing bounded wait/dispatch/settlement checks. Give the host the same absolute deadline and monotonic clock so native work can recheck before commit; a duration captured at dispatch alone is insufficient. Snapshot deadline fields and retain the supplied trusted clock, rather than creating a new callback protocol. |

Bind failure returns a diagnostic and no usable handle; callers must stop initialization on failure. Preparation similarly returns no prepared handle on failure. Keep existing error codes (`ADMISSION_INVALID`, `PIN_MISMATCH`, `HOST_CONTEXT_INVALID`, resource/type errors) and result wrappers rather than adding an exception API. No authorizer, key validator, qualification, report hash or liveness function appears in the corrected shapes. Structural preflight still occurs before any host call. Permission denial inside an implementation must use a declared adapter rejection with truthful no-effect evidence; an exception or timeout is not automatically definite NONE.

The only added per-call information is the already supplied absolute deadline made available to the implementation. Event, key, protected arguments and remaining duration already exist. Stable implementations close over the host service, not an event or user; each call receives fresh invocation data. The host can use its existing native key lookup and permission checks directly. No mandatory ambient context, middleware, cache or registry is introduced.

## Responsibilities

| Keir checks/enforces | Host implements/enforces |
| --- | --- |
| Valid DCM; complete version-compatible implementations; exact model/artifact compatibility | Which catalog and extension are trusted and selected |
| Program types, permitted operations, effect/count/data/deadline limits | User/resource permission and business invariants at the actual operation |
| Separation of program arguments from trusted host context | Construction of that context; authenticated identity and credentials |
| Passing the original host request key unchanged when the selected effect contract requires one | Key authenticity, duplicate/payload semantics and retention promised by that capability |
| Declared result/error validation; honest confirmed, no-effect or unknown outcomes; no hidden retry | Actual operation, commit evidence and any read-only recovery |

The host must test its guarantees. Keir does not prescribe a test-report format or certify the host by reading hashes. An implementation can call a local service or HTTP/RPC; the runtime does not need to know its database or transport.

## Remove, retain and relocate

- **Remove:** mandatory qualification object, report/build digests, platform/database fingerprints, deployment bundle parser and separate qualification/currentness callback from the corrected core contract. Do not replace them with a renamed mandatory callback or certificate.
- **Retain:** static compatibility checks, explicit simulation/real separation, untrusted-program validation, limits, original-key forwarding and honest outcomes. A passing simulation still does not prove native behavior.
- **Host-owned:** current permission and extension lifecycle checks. The host checks before invoking; capability implementations check before sensitive operations and native commit. Existing host middleware can do this without Keir requiring an additional approval callback.
- **Simplify:** bind stable implementations once and prepare an exact extension once. Fresh user/event/key/deadline state belongs to each invocation, never a mutable global user or a captured previous event. Use existing host facilities, not a new context framework.

## Be explicit about the changed checks

| Old protocol behavior | Corrected responsibility |
| --- | --- |
| Missing/wrong report or installed-build digest blocks binding | No such input. Invalid DCM, missing implementation, wrong version/mode/model still block binding. |
| Qualification revoked at bind/prepare blocks a handle | Bind/prepare validate compatibility; they do not query live business permission. A prepared handle is not permission to execute. Host controls selection and execution. |
| Separate currentness callback denies after an async authorizer returns stale true | Host permission code must make its decision after its awaited work; the capability checks again before sensitive work/commit. Keir keeps deadline checks. It cannot detect a host implementation that falsely reports permission. No second generic callback is introduced to compensate for that. |
| Currentness denies a later call after revocation | Host capability code rejects a now-forbidden operation; host lifecycle stops new invocations. Test the host boundary and absence of unauthorized effects. Do not claim identical internal callback timing or zero adapter entries where checks have moved inside the adapter. |
| Qualification tuple in persisted events controls execution | Host retains the selected program/semantic version and durable revocation. Old records remain evidence; removing the tuple requirement never resurrects revoked or interrupted work. |

Keep pending-authorization/revocation barrier tests, but exercise the retained host checks and assert no unauthorized operation. Do not silently delete them or describe rewritten vectors as unchanged runtime guarantees. Earlier profiles keep their documented behavior unless explicitly migrated.

For the corrected API, do not require separate `authorize` or `keyBinding.validate` callbacks. Registered implementations receive fresh trusted context and enforce current permission and key authenticity through the host's existing services. Keir checks declared key-type compatibility and forwards the original opaque key. Retain the old callbacks only on explicitly supported legacy APIs; do not silently change those callers.

This changes observable denial timing: host rejection inside a called implementation is an attempted call with a declared rejection and definite no-effect outcome, rather than the old pre-dispatch `AUTHORITY_DENIED` with no receipt. Test both contracts where supported. Runtime deadline, trace-capacity and dispatch limits remain independent checks; a timeout or lost response cannot be reported as definite no effect merely because permission was being checked.

Bind and prepare never execute registered capability functions or authenticate a concrete invocation key. A structurally valid `/4` invocation that reaches no capability may finish without any native permission or key-authenticity check; it still passes input/context/type/deadline and execution-limit checks. The host decides whether to invoke it at all. Test this deliberately, rather than retaining an entry authorization callback solely to reproduce old zero-call denial behavior. When a capability is reached, its implementation owns permission and key authenticity; entering that function to reject an invalid key is compatible with zero unauthorized native effects.

## Preservation and compatibility

Developer inventory establishes that demo event pins are immutable JSON; their old fields can remain without rewriting shipped schema migration 8. Historical source reads currently depend on the executable definition, so retain an old-definition/raw-byte read path before replacing that definition. Old approvals must never approve new write semantics. Old PENDING work remains held, STARTED work is not replayed, revoked admissions stay revoked, and saved outcomes remain readable under current host permission.

**Recommendation: introduce the explicit `/4` contract above and retain existing `/1`–`/3` core behavior.** Keep `/3` qualification code reachable only through its explicit legacy profile; it is not a prerequisite or default for `/4`. Retain its tests rather than presenting changed authorization timing as equivalent. Do not add new legacy features or a compatibility translation service. Corrected examples and demo use `/4` directly.

The alternative is a coordinated experimental replacement that rejects old `/3` bytes/callers outright and preserves their demo records only for reading. It would delete the old qualification parser/helper and its protocol-only tests, but breaks distributed packages/callers and requires explicit retirement of the four-argument API. Tags/releases are absent and known demo consumption is held/test-only, but source and packages have already been distributed; metadata does not prove there are no other consumers. The recommendation pays the bounded cost of retaining existing code/tests plus profile dispatch and fixtures to avoid that break. It does not require a second evaluator, another qualification design, or investigation until every possible consumer is found. Manager and independent review can accept or reject this concrete recommendation; compatibility is no longer left as an unspecified implementation choice.

In the corrected demo, `/3` and earlier selections are historical/ineligible. Only `/4` can create new real admissions, with newly compiled bytes, independent checks, fresh customer approval and explicit activation. The trusted definition identifier must also change from `daymark.task-completed/1` to `daymark.task-completed/2`; existing definition/pins identify old rows without new DDL. Preserve old source/model/consumer bytes or a validated raw-byte historical reader, old evidence and receipts. Hold old PENDING before claim, never replay STARTED, retain truthful FINISHED/UNKNOWN, and never clear revocation. Do not convert an old approval, key or event into a new admission. Core `/3` support does not make it executable in the corrected demo. Preserve `/1`–`/2` byte/API behavior and real effect/2 rejection.

Use the demo's existing lifecycle/native service as the owner of durable disable and revocation: retain selection invalidation and monotonic event revocation, without a replacement report registry. Preserve each event's captured admission; ordinary selection replacement must not silently redefine it. Native key lookup stays inside the host implementation, scoped to its database/service and current invocation. Reuse one binding and an exact prepared extension with fresh context; do not introduce a generalized cache. Historical readers must validate stored bytes independently of the currently executable definition. Existing JSON pins can distinguish contracts; no new schema migration is justified by the inventories.

## Bounded verification and next step

At the existing independent plan and result reviews, apply the [shared simplicity rule](../../../roles/shared.md#simplicity) to the actual host example and signatures. Each additional public input, callback, abstraction or service must have a current requirement, a reason existing DCM/artifact/host code cannot satisfy it, and a concrete failure prevented. Remove additions without those answers before acceptance. Existing code/tests and hypothetical future use are not sufficient justification. Record the short verdict in the owning Work Record; do not create another form or approval process. The architect's self-review does not satisfy this check.

Corrected shared scenarios use `keir-scenario/effect-4`, preserving the existing scenario grammar while selecting corrected invocation types and host-implementation permission checks. Reject mixed `/3`–`/4` scenario, model, artifact and invocation identities; retain the legacy corpus. Generate corrected artifacts with the compiler, never by relabeling old fixture bytes.

Verify complete/missing/incompatible binding; same artifact under fake and real implementations; reused preparation with distinct user/key contexts; permission denial and revocation during awaited work; original-key and duplicate behavior; deadline/response-loss UNKNOWN; and retained historical records. Run shared Java/Node cases plus actual demo service tests. A loopback service on the existing machine can test transport failure without another host or a transport framework.

Manager consolidates the developers' inventories with this proposal into the concrete correction plan and independent review. Then align the core admission/binding specifications, runtime/compiler fixtures and examples, demo integration and setup, and product assistant guides together. No implementation release is implied by this document.

Sources: [baseline ownership](../design-baseline/public/host-integration-and-hook-contracts-v0.5.md#1-scope-and-ownership), [prepared execution](../design-baseline/public/keir-architecture.md#63-runner-responsibility-and-performance), [host-owned keys](../design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md#3-responsibility-boundaries), [audit](real-application-design-audit.md). Owner-reported inventories are read-only evidence, not newly executed tests by the architect.
