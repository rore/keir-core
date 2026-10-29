# Keir: Host-Owned Idempotency and Multi-Effect Contracts

**Design proposal v0.7 | 29 October 2026**  
**Applies to:** Keir Architecture v0.4; Host Integration and Hook Contracts v0.5  
**Supersedes:** *Keir: Idempotent Multi-Effect Execution and Host Contracts* v0.6, especially its runner-generated per-step idempotency keys.  
**Status:** Proposed contract, not implemented or production certified. Public-safe, fictional domain examples only.

## 1. Decision

**Keir MAY execute more than one mutating capability within a bounded extension when the host explicitly admits that execution mode and certifies that the operations' real idempotency contracts are compatible.** This replaces the blanket *at most one mutation* POC restriction, not Keir's closed-language or host-authority model.

**The idempotency identity belongs entirely to the host.** The host passes an opaque, typed idempotency value in trusted invocation context. Keir passes that value to approved capability adapters **without generating, concatenating, hashing, encoding, converting, namespace-prefixing, or otherwise changing the key**. Only a host adapter performs the API's required *wire serialization* of the original value according to that API's own contract; it must not silently substitute a different logical identity.

The same host-provided request ID **can be supplied unchanged to multiple mutations** if the respective downstream APIs recognize them as independent idempotent operations. It **cannot** be assumed safe across arbitrary APIs, repeated calls, separate extensions, or distinct business requests. An API might require a UUID, interpret parts of a request ID, or deduplicate across multiple endpoints. Keir has neither the authority nor the knowledge to rewrite those semantics.

**Decision in one line:** `host-owned opaque key + host-certified deduplication scopes + bounded sequence admission + explicit partial outcomes`.

### Invariants

- Customer-authored IR **cannot read, create, select, or override** idempotency identities.
- Keir **never synthesizes downstream idempotency keys**. Its `stepId` and internal execution/correlation IDs are **not** API idempotency keys.
- A capability merely *accepting* an idempotency header is **not sufficient** to be certified as retry-safe.
- Keir does **not** infer atomicity, exactly-once processing of an entire extension, automatic replay, or safe concurrent business-state transitions from idempotency.
- Host-owned pre-commit interception hooks remain **read/decide/return** by default; independent external mutations are not enabled merely because a key exists.
- The identical DCM semantics and protected-key forwarding contract apply to **Node and Java runners**.

## 2. Problem and counterexample

A host initiates an extension with a domain request ID that is already used by its APIs as the idempotency key:

```text
Host requestId: UUID("d7e7af57-0b60-4a75-8118-df19654a5aa9")

AfterTaskCompleted extension:
  A: tasks.createFollowUp(..., requestId = original UUID)
  B: notifications.send(...,  requestId = original UUID)
```

This is **admissible only if** A and B accept the same UUID type/format *and* their implementations treat the two calls as distinct deduplicated operations. If they both use a global `(tenant, requestId)` deduplication entry, B may be discarded as a duplicate of A or rejected as a payload mismatch. If they use independently scoped `(tenant, operation, requestId)` records, the same UUID can work for both.

The previous v0.6 proposal generated `hash(hostRequestId, stepId, ...)` and allowed per-step keys. This is wrong as a general Keir contract: it may violate the underlying API's identifier format, business semantics, lookup rules, or existing deduplication state. **Delete the derived-key requirement; retain static step IDs solely for validation, tracing and receipts.**

A different example is also unsafe:

```text
A: notifications.send(to=Alice, template=X, requestId=original)
B: notifications.send(to=Bob,   template=Y, requestId=original)
```

If `notifications.send` deduplicates by `(tenant, requestId)`, B cannot be admitted just because its Keir step ID differs. **One caller-supplied key does not imply unlimited unique mutations.**

## 3. Responsibility boundaries

| Owner | Responsibility |
|---|---|
| **Host** | Supplies an appropriate stable typed key for the logical invocation; owns its uniqueness, scope, validity, routing, retention expectations, and any alternative key slots; determines hook mode and lifecycle placement; verifies downstream guarantees. |
| **Host capability / adapter** | Accepts and forwards the original host key as required by the API; enforces or delegates atomic duplicate suppression, payload-mismatch behavior, authorization, request validation, and status/result recovery when promised. |
| **DCM / effective catalog** | Describes each effect's required key source/type, **actual deduplication collision boundary**, guarantees, retention, and failure semantics as host-certified contracts. |
| **Keir compiler** | Rejects disallowed effect sequences, incompatible key types/sources, and potential shared-key collisions based on certified DCM semantics and conservative call-graph analysis. |
| **Keir runner** | Carries the host key as protected opaque metadata; forwards it unchanged to the declared adapter; enforces bounded order/count and runtime admission; reports confirmed/partial/unknown effects. |
| **Host activation/delivery** | Checks collisions **across active extensions** and non-Keir host consumers that use the same key namespace; pins policy/artifact and owns any retry, durable journal or reconciliation. |
| **Business-user agent** | May explain that a proposed composition is unsupported, but cannot assign a stronger guarantee, manufacture an idempotency key or change the certified collision scope. |

**Two identities must not be conflated:**

- `hostInvocationId` is a logical execution/admission identity useful for logging, artifact pinning, receipts and event delivery. It might be a string or composite identity; it is *not automatically a valid API idempotency key*.
- `hostIdempotencyKey` is the exact host-owned value accepted by the affected capability's API. It may happen to be the same request ID. If the host supplies only one field, it must explicitly certify that field serves both purposes in that context.

`tenantId`, `workspaceId`, `subject`, `bindingId`, and `stepId` can participate in **admission and tracing**, but Keir must not concatenate them into the key sent to the API. Where a provider actually incorporates these into its deduplication lookup, the host certifies that behavior as the API's collision boundary.

## 4. DCM: certify *how* an API deduplicates

The DCM needs a stronger contract than `idempotent: true`. **The following YAML is a proposed DCM shape, not final schema and not customer-authored IR.** All values are host assertions that require adapter tests or other direct evidence.

```yaml
entrypoints:
  tasks.onCompleted:
    trigger: EVENT
    placement: AFTER_COMMIT
    consumer: EVENT_REACTION
    effectOwner: EXTENSION_CALL
    invocation:
      logicalIdentity: hostContext.eventId
      idempotencyKeyRef: hostContext.requestId  # trusted and opaque
      onMissingKey: REJECT_EFFECT_SEQUENCE
    executionPolicy:
      mode: BOUNDED_EFFECT_SEQUENCE
      maxEffectCalls: 3
      order: SEQUENTIAL
      automaticReplay: DISABLED
      onEffectFailure: STOP_AND_REPORT
      onUnknownCommit: STOP_AND_REPORT_UNCERTAIN

capabilities:
  tasks.createFollowUp:
    effectClass: BUSINESS_WRITE
    idempotency:
      mode: HOST_KEYED
      keyRef: hostContext.requestId
      keyType: host.types.RequestUuid
      keyTransport: HOST_ADAPTER_ONLY
      collisionGroup: task-service.follow-up-create.v1
      collisionBoundary: TENANT_AND_OPERATION_AND_KEY
      duplicateEffectSafe: HOST_CERTIFIED
      onPayloadMismatch: REJECT
      concurrentDuplicate: REJECT_OR_JOIN
      resultRecoverable: true
      retention: HOST_CERTIFIED
      certificationRef: task-adapter-idempotency:v1

  notifications.send:
    effectClass: EXTERNAL_WRITE
    idempotency:
      mode: HOST_KEYED
      keyRef: hostContext.requestId
      keyType: host.types.RequestUuid
      keyTransport: HOST_ADAPTER_ONLY
      collisionGroup: notification-service.send.v1
      collisionBoundary: TENANT_AND_OPERATION_AND_KEY
      duplicateEffectSafe: HOST_CERTIFIED
      onPayloadMismatch: REJECT
      concurrentDuplicate: REJECT_OR_JOIN
      resultRecoverable: true
      retention: HOST_CERTIFIED
      certificationRef: notification-adapter-idempotency:v1
```

**`collisionGroup` is not a namespace Keir creates in a downstream key.** It is a *host-certified declaration* that distinguishes the actual deduplication tables/services/scopes. Two groups may be treated as disjoint **only if the host has verified that their dedupe state cannot collide for the admitted key values**. Distinct labels assigned for convenience are not proof.

For a provider whose key is globally unique across all operations, the host should give all affected capabilities **the same collision group**. If the provider uses `(tenant, endpoint, requestId)` as its actual lookup key, distinct certified endpoint groups are appropriate. If the lookup depends on a resource ID only available at runtime, the compiler must conservatively reject shared-key reuse unless a host-guaranteed disjointness rule is enforceable at runtime.

The DCM must also specify:

- **Key acceptance:** exact host-native type, semantic format and allowed sources; no silent stringification of a structured or numeric key; invalid/missing key blocks the effect before dispatch.
- **Identity stability:** host guarantees the same value for the same logical request across any supported delivery attempts; uniqueness within each certified collision boundary.
- **Duplicate effect safety:** under **concurrent** duplicates the operation does not produce duplicate business effects; the provider's dedupe record must be atomic with the effect or protected by an equivalent domain protocol.
- **Payload matching:** behavior when the same key is reused with different effective arguments. Prefer rejection; a provider that ignores changed payloads silently is insufficient for reliable replay without additional host checks.
- **Result recovery:** can a second call/status check obtain a result adequate for later dependent steps, or only prevent duplicate effects? These are different guarantees.
- **Retention and unknown status:** how long the contract holds, what a timeout means, and whether an authoritative status query or safe same-key retry is supported.
- **Business concurrency:** authorization, entity-version checks, and relevant invariants remain independent of idempotency.

A tenant-registered external write endpoint is subject to the *same* certification standard. A customer statement that it accepts `Idempotency-Key` is not enough.

## 5. Admission rules

### 5.1 Compiler checks for one extension

Keir MAY admit a multi-effect artifact only when all of the following hold:

1. The **hook explicitly permits** extension-owned effects and declares a finite maximum effect count; the compiled IR has only statically known, bounded sequential effect calls.
2. Every possible effectful capability has a **host-certified compatible key contract**. It accepts the host-provided key type/source without modification by Keir and has the required duplicate-effect guarantee.
3. For each possible execution path, **no two distinct business effects can consume the same host key in the same certified deduplication collision group**. For v0.7, use conservative path analysis: when mutual exclusivity cannot be proven, **reject** rather than assume CEL guards won't both match. Static step IDs do not disambiguate keys.
4. Key-based idempotency does not conflict with the hook's lifecycle or consumer. Ordinary pre-native-effect guards and patches cannot perform independent business writes.
5. All existing authority, dataflow, output-contract, effect-count, deadline and resource checks pass. A certified key does not grant mutation permission.

The compiler cannot guarantee the *runtime key value* or the actual external deduplication store. It validates the published DCM contract and encodes the required key source, type, collision groups, versions and certification references into the artifact. The runner and host activation must verify compatibility again at execution time.

### 5.2 Host admission checks across extensions and other callers

A single artifact can be safe while two active extensions are not. Example: two independently activated `TaskCompleted` reactions both invoke `notifications.send` with the *same* host request ID. Their distinct Keir bindings do not make the notifications unique to the API.

Therefore the host's activation/invocation layer MUST either:

- ensure at most one effective mutation uses `(actual collision boundary, original key)` across the admitted set;
- provide **separate host-owned API-compatible key values** in explicitly bound trusted key slots, each stable across its own retries; or
- use a separately certified host-owned atomic/batch operation that implements the required semantics.

It must also account for native host code that calls the same API using that key. Keir cannot inspect external callers or invent disjointness. Where these guarantees are not available, the host rejects the multi-effect admission or allows only the non-conflicting subset by activating a different approved extension; it must not silently skip an IR step.

### 5.3 Optional host-supplied key slots (not required for v0.7 POC)

Some domains genuinely need two calls in the same collision group. A future host may supply **multiple distinct original keys**, one per explicitly authorized slot:

```yaml
trustedInvocation:
  idempotencyKeys:
    mainRequest: <host-supplied-typed-key-A>
    secondAction: <host-supplied-typed-key-B>
```

The host defines the slot, format, uniqueness and binding to specific capabilities. Keir merely forwards the chosen *host-provided* value, unchanged. The customer's IR cannot assign the slot or create a new key. This is optional host functionality, **not** disguised Keir key derivation. If no suitable host key exists, the desired sequence is not admitted under this profile.

## 6. Runtime API: Node and Java

Keep the identity out of the CEL environment and out of customer-controlled arguments. A trusted adapter sees both the typed business arguments and the original host idempotency value through a separate context parameter.

**Node (illustrative TypeScript API):**

```ts
// The host SDK owns the actual representation and validation.
declare const hostOpaqueKeyBrand: unique symbol;
interface HostIdempotencyKey { readonly [hostOpaqueKeyBrand]: true }

interface TrustedEffectContext {
  readonly idempotencyKey: HostIdempotencyKey; // unchanged host value
  readonly logicalInvocationId: string;      // NOT forwarded as API key
  readonly staticStepId: string;              // NOT forwarded as API key
  readonly scope: AuthorizedHostScope;
  readonly deadline: Deadline;
}

interface CapabilityAdapter<A, R> {
  invoke(args: A, ctx: TrustedEffectContext): Promise<CapabilityOutcome<R>>;
}
```

**Java (semantically equivalent sketch):**

```java
// Marker/host SDK type; Keir doesn't construct or transform instances.
interface HostIdempotencyKey {}

record TrustedEffectContext(
    HostIdempotencyKey idempotencyKey,
    String logicalInvocationId,
    String staticStepId,
    AuthorizedHostScope scope,
    Deadline deadline) {}

interface CapabilityAdapter<A, R> {
  CompletionStage<CapabilityOutcome<R>> invoke(A args, TrustedEffectContext ctx);
}
```

The marker types are illustrative; actual Node and Java SDKs need a host-defined interoperable key-type contract. **"Unchanged" means preserving its host-defined logical value and type**, not requiring the same in-memory object identity across separate process invocations. If a host moves execution across a process boundary, it owns the lossless serialization and reconstruction of the original value. Keir must not perform implicit `toString()`, case folding, trimming, UUID conversion, base64 encoding, hashing or per-step suffixing.

The host adapter may perform the API's **specified HTTP/gRPC encoding** (for example, serializing an accepted UUID as a required header string); this is protocol transport, **not key derivation**. Such encoding is host-owned, documented and verified. An adapter cannot quietly convert an incompatible key into a new identity and still claim pass-through semantics.

Keir may use `(logicalInvocationId, bindingId, staticStepId)` **internally for receipts and audit**. These identifiers are never substituted for the API key. The compiled artifact contains **key-source references and type contracts, never a concrete request key or a credential**.

## 7. Execution and failure semantics

Multiple idempotent operations can still leave partial progress:

```text
TaskCompleted (requestId = original UUID)
  -> tasks.createFollowUp(original key): CONFIRMED followUpId=F-32
  -> notifications.send(original key): TIMEOUT / UNKNOWN_COMMIT
  -> tasks.updateMetadata(...): NOT_REACHED
```

The invocation is **not successful** and the first effect is **not rolled back**. A timeout is `UNKNOWN_COMMIT` unless the adapter can establish a definite no-effect outcome. Keir stops before any subsequent effectful call and returns a receipt such as:

```yaml
invocationOutcome: UNCERTAIN
summary: UNCERTAIN_PRESENT
steps:
  createFollowUp: { effect: CONFIRMED, resultRef: F-32 }
  sendNotification: { effect: UNKNOWN_COMMIT }
  updateMetadata: { effect: NOT_REACHED }
```

**Default v0.7 mode:** bounded single attempt, **no Keir-managed automatic replay**. Host event delivery must not re-execute a previously attempted mutation blindly after redelivery. A host may retain a durable admission record, quarantine unknown deliveries, or explicitly reconcile them. No Keir database or workflow engine is required merely to permit bounded multi-effect execution.

**Replayable mode is separately opt-in.** The host must pin the original artifact, activation, input/read snapshot or exact effective mutation arguments, the original host-provided key(s), dedupe scope, result/call records, key-retention horizon and authorization policy. Recovery can reuse the original key unchanged only if the adapter's published behavior permits safe same-key recovery, including payload match and original result/status availability. If the API cannot resolve `UNKNOWN_COMMIT`, stop for reconciliation. Never re-run an already confirmed effect with fresh arguments just because the key is unchanged.

Idempotency does not make later writes serializable, protect against business-state changes, guarantee financially atomic outcomes, or excuse new authorization checks. Where all-or-nothing is required, the host should offer **one atomic domain capability**, not a Keir composition of independent remote writes.

## 8. Hook types and activation composition

- **Pre-commit `INTERCEPT` with host-owned native effect:** Return `ALLOW`, `DENY`, typed `PATCH` or permitted selection. Do not permit independent extension-owned writes by default; they can commit before the eventual native validation fails.
- **Post-commit `EVENT`:** May opt into `BOUNDED_EFFECT_SEQUENCE` if the host supplies a stable valid key and compatible certified effects. Event-source identity and request ID are distinct unless the host says otherwise. If several events share an original request ID, the host must check collision scopes across *those events*, not just within an extension.
- **Explicit `REQUEST` command with extension-owned effects:** May opt into the same bounded mode, accepting partial outcomes.
- **`INDEPENDENT_FANOUT` activations:** Require host-wide collision analysis across active bindings. They are independent execution receipts, not automatically independent dedupe namespaces.
- **Guard/patch aggregators:** Multiple deny-only policies do not need idempotency keys for reads/decisions; the ordinary v0.5 composition semantics still apply.

## 9. Conformance and POC tests

| Test | Expected behavior |
|---|---|
| Host UUID key passed to two certified disjoint collision groups | Both adapters receive the **original unchanged typed value**, both effects may execute. |
| Key has a host-specific format/embedded semantics | The runner does not alter format or use a derived step key; adapter accepts it as documented. |
| Two mutations share the same collision group and key | Rejected at compile/admission, even with different static step IDs. |
| Two fan-out bindings call the same group with the same key | Host admission rejects or requires distinct **host-supplied** approved keys. |
| One capability expects an incompatible key type | Reject; do not coerce or hash. |
| Missing key for a required effect | Reject before any effect dispatch. |
| Provider merely accepts an `Idempotency-Key` header | Not certified for multi-effect execution. |
| Same key and changed request fingerprint on replay | Reject or reconcile according to verified host contract; no new effect. |
| First effect confirmed, second definitively rejected | Report `PARTIAL_CONFIRMED`, stop; don't roll back first. |
| First effect confirmed, second times out | Report `UNCERTAIN_PRESENT`, stop; do not claim second failed harmlessly. |
| Retry after retention expires | Block/reconcile unless independent host-owned business uniqueness proves safety. |
| Re-delivery after activation revision change | Use pinned activation and original key or stop; never silently execute new policy with old request key. |
| Two logical events share one original request ID | Check collision groups across events; stable per-event execution IDs do not make API keys distinct. |
| Pre-commit interception returns patch then native validation fails | No extension-owned mutation occurred; host rejects the native action. |
| Node versus Java runner | Same original host key type/value forwarded for each admitted operation, same effect sequence and outcome classifications. |

**Minimum POC:** A fictional task-management host supplies a UUID request ID; an approved post-commit extension invokes `tasks.createFollowUp` and `notifications.send` using that unchanged UUID. Realistic adapters have **separate certified dedupe scopes**. Include a negative test in which they share a scope and Keir rejects admission, plus a lost-response test that shows an honest partial/uncertain receipt. This demonstrates the concept without building durable recovery.

## 10. Changes required in previous designs

| Artifact | v0.7 change |
|---|---|
| **Host Integration v0.5, §7.2** | Replace “each distinct effectful step gets a stable host-generated operation identity” **as a downstream key rule**. Distinct static step IDs remain for receipts; API key comes unchanged from the host, subject to collision-scope admission. |
| **Multi-Effect v0.6, §§1–6** | **Withdraw** `keyOwner: KEIR_RUNNER`, `operationKey = HMAC(...)`, derived per-step keys, `operationKey` adapter arguments, and all test vectors requiring distinct keys per step. Replace with `keyRef`, `keyType`, `collisionGroup`, host certification and original-key pass-through. |
| **Keir IR v0** | **No change.** `let`, `call`, `when`, `onError`, `return` remain; no key expression, effect loop or retry instruction. |
| **Compiled artifact** | Pin hook/effect policy, *key-source contract and type*, certified collision groups, capability/adapter versions and approved effect count. Never embed the actual request key. |
| **Node/Java runners** | Host-protected key parameter to capability adapters; no key derivation; conservative runtime compatibility and scope checks; accurate partial/uncertain receipts. |
| **Simulation** | Fake APIs must implement both **disjoint-group success** and **shared-group collision**, plus timeout and duplicate request semantics. Fake success is not real provider certification. |

## 11. Open decisions and boundary of evidence

1. **Collision-group certification:** The host must demonstrate each relevant provider's real key lookup scope, request matching and concurrency behavior. Keir cannot infer it from REST paths or operation names.
2. **Cross-binding admission:** A production host must select an implementation for tracking relevant active bindings and their shared-key collision potential. For the POC, limit to a single bound extension and test rejection of conflicting fan-out.
3. **Replay requirements:** Default **none** in Keir v0.7. Add a host-owned operation journal and result recovery only when there is a concrete need for automatic replay; never mistake idempotency for durable workflow state.
4. **Host key slots:** Optional, host-provided additional keys may be valuable later. Do not require them for the initial POC.
5. **Naturally idempotent unkeyed operations:** Not automatically admitted under this keyed multi-effect profile. Assess them separately against concurrency, result-recovery and business-state guarantees.

**No production adapter has been certified by this design.** A host-declared contract must be tested against its actual service/provider. CEL-Java-to-Node interoperability does not establish multi-effect safety or cross-runner adapter correctness.

### References (principles, not Keir certification)

- [AWS Builders' Library, *Making retries safe with idempotent APIs*](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/): caller-provided request IDs, parameter mismatch, recovery semantics, and late arrivals.
- [Stripe, *Idempotent requests*](https://docs.stripe.com/api/idempotent_requests): API-dependent duplicate semantics, parameter comparison and finite key retention. Its API contract must not be generalized to arbitrary host APIs.
- [IETF HTTPAPI `Idempotency-Key` draft-08, 22 October 2026](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/): the resource owner defines key uniqueness and fingerprint/expiry behavior; **still an Internet-Draft, not a finalized RFC**.
- [Azure Service Bus duplicate detection](https://learn.microsoft.com/en-us/azure/service-bus-messaging/duplicate-detection): concrete example where deduplication scope depends on message ID and partition configuration, illustrating why namespace behavior is host-specific.

---

**Implementation decision:** Adopt **host-owned original-key pass-through and collision-aware admission** as v0.7's normative direction. Allow a bounded sequence of certified, independent keyed effects; do **not** introduce a Keir key generator, durable workflow engine, or source-language change.
