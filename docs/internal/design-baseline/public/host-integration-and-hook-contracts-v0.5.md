# Keir: Host Integration and Hook Contracts

**Design proposal v0.5 | 29 October 2026**  
**Status:** Proposed architecture delta to Keir v0.4. Not implemented, tested, or production certified.  
**Audience:** Keir compiler/runner implementers and SaaS host integrators.  
**Publication scope:** Public-safe. Fictional domain examples only. No private customer material or internal service endpoints.

## 0. Executive decision

**Keir is an execution boundary for customer-authored customization, not the owner of an application's business operation.** A SaaS host publishes extension points, typed capabilities, and result-consumer contracts. Keir validates and runs a customer's selected composition. The host enforces the effect of the result in its native flow. An optional host-owned AI agent makes authoring accessible to business users.

The existing v0.4 architecture already recognized hooks, tenant capabilities, results, and failure policies. The new assessment does **not** justify a different IR. It makes several host-interface obligations important enough to specify rather than leave implicit.

### Recommended changes to v0.4

| Concern | v0.4 baseline | Proposed v0.5 clarification |
|---|---|---|
| Hook definition | Phase plus input/output | Add **placement**, **trigger**, **consumer kind**, **native effect owner**, and execution-mode restrictions as distinct fields |
| Effects near a native operation | Allowed effects listed per hook | By default, **pre-commit intercepts may read/decide but may not perform independent business mutations**; the host applies the native effect once |
| Result handling | Typed `ALLOW`/`REJECT`/`PATCH` or view | Define an explicit **result-consumer contract** and classify runner completion separately from business decisions |
| Multiple customer extensions | Not defined | Admit explicit **EXCLUSIVE**, **ALL_MUST_CONTINUE**, or **INDEPENDENT_FANOUT** composition modes; no implicit last-writer-wins |
| Invocation identity | Trusted host-bound context | Specify activation selection, input provenance, state versions, deadlines, and immutable execution receipts |
| Event reaction | `POST_COMMIT` phase | Define **durable admission, replay identity, causal re-entry controls, ordering needs, and uncertain-effect reconciliation** |
| Host correctness | Host certifies real guarantees | Define a small **host integration conformance suite** checking result consumption and all routed paths |
| External endpoints | Host-approved tenant capabilities | Define explicit **data disclosure/egress projections** and strict effect classification, separate from CEL arguments |

**Status of these recommendations:** Design decisions for the proposed contract, not claims about implemented Keir APIs. The precise schema and language bindings shown below are illustrative; they must be reconciled with the real v0 DCM schema before implementation.

## 1. Scope and ownership

### 1.1 Responsibilities

**SaaS host** owns the application lifecycle, where and when hooks fire, authenticated scope, native authorization and invariants, data projection, current capability implementations, the hook's result consumer, transaction boundaries, actual event producer/delivery, approved endpoint registration, and activation of customer programs.

**Keir compiler** owns static verification of the closed IR against the **effective host-approved catalog**: typed data flow, bounded expressions, operation/effect grants, hook output contract, declared failure modes, and artifact provenance. It does not certify underlying host guarantees by declaration alone.

**Keir runner** owns executing the approved program within runtime budgets, dispatching permitted calls through trusted host adapters, producing a typed result and execution receipt, preserving uncertainty, and providing identical execution semantics with real or fake adapters. Its Node and Java implementations must follow one portable artifact and CEL profile.

**Host-integrated AI agent** is an optional authoring interface. It can clarify intent, select admitted operations, create IR, propose scenarios, and explain diagnostics. It cannot create capabilities, choose failure policy, define hook placement, certify adapter correctness, or authorize activation.

### 1.2 What remains unchanged

- Keir IR v0 remains ordered `let` and static `call` steps, optional `when`, terminal typed `onError`, and typed `return`; CEL is pure computation inside bindings.
- No arbitrary source code, arbitrary HTTP destinations, side-effect loops, long-lived waits, distributed transactions, or generated business APIs.
- The host may expose native capabilities and host-approved tenant endpoints through the **same static `call`** instruction.
- One authoritative Java compiler creates a portable artifact consumed by Node first and Java later. Java and Node runner contracts must not diverge semantically.
- The core runtime can remain a small library without a broker, database, agent, or HTTP compiler service.

## 2. Distinguish the three interfaces

```text
          Host publishes (design / integration time)
   HookDefinition + CapabilityCatalog + AdapterGuarantees
                            |
                            v
          Keir compiler validates customer IR
                            |
                  Approved compiled artifact
                            |
  Host selects activation, creates invocation, invokes runner
                            |
            +---------------+----------------+
            |                                |
       Keir runner                    Host adapters
       evaluates CEL          trusted reads / approved calls
            |                                |
            +----------- typed result -------+
                            |
                Host result consumer
         validates, applies, persists, or responds
                            |
                   Native SaaS behavior
```

There are **three independent contracts**:

1. **Definition contract:** what the extension is allowed to know, call, and return. Published in the DCM plus host-approved tenant catalog.
2. **Invocation contract:** which approved artifact runs for which protected tenant/workspace/entity/actor/event, with which authoritative inputs, budgets, and current permission checks.
3. **Consumption contract:** what the host does with the result, when it must do it relative to native validation/commit, and what happens on error/timeout/no binding.

A correctly typed result is not sufficient if the consumption contract is absent or incorrect. Likewise, declaring a read or effect in the catalog does not prove the adapter implements its promised business semantics.

## 3. Hook definition model

### 3.1 Orthogonal dimensions, not a growing list of special-purpose hook types

A hook has four important dimensions:

- **Trigger:** `INTERCEPT` (synchronous host call), `EVENT` (independent post-commit invocation), or `REQUEST` (explicit query/decision).
- **Placement:** a stable, host-defined lifecycle location, such as `BEFORE_NATIVE_VALIDATION`, `BEFORE_PERSISTENCE`, `BEFORE_EFFECT`, `BEFORE_NATIVE_MAPPING`, `BEFORE_RESPONSE`, or `AFTER_COMMIT`. Placement is a host-owned semantic contract, **not** an instruction authored by the customer.
- **Consumer:** `GUARD`, `PATCH`, `SELECT_ACTION`, `RESPONSE_PROJECTION`, `OBSERVE`, or a precisely typed combination approved by the host. Consumers are **behavioral contracts** with explicit native validation; they are not commands to execute arbitrary code.
- **Capability policy:** which statically named native/tenant operations are callable, what data may be disclosed, the maximum effects and calls, and the permitted failure outcomes.

Do not assume `AFTER_COMMIT` always means a synchronous callback. A durable event reaction has different delivery semantics from a best-effort immediate observation, even if both occur after native commit. Encode that difference explicitly.

### 3.2 Proposed `HookDefinition` fields

A portable host definition needs at least:

| Field | Purpose |
|---|---|
| `id`, `version` | Immutable semantic identity; major changes do not masquerade as compatible |
| `trigger`, `placement` | When the host invokes Keir relative to authorization, validation, effects, and commit |
| `inputType`, `outputType` | Closed, projected types; business data available to CEL is explicit |
| `resultConsumer` | Recognized result variants; exact application and native revalidation obligations |
| `effectOwner` | Whether the final native business effect is performed by the **HOST** or by an approved **EXTENSION_CALL** after commit |
| `capabilityGrants` | Admitted calls, counts, effect classes, and remote disclosure allowances |
| `bindingScope` | Host-selected tenant/workspace/organization/program/entity constraints and routing coverage |
| `activationMode` | One or multiple programs and the only legal combination semantics |
| `failurePolicy` | Host-owned behavior on missing binding, invalid result, failure, deadline and uncertain effect |
| `budgets` | Total deadline including queueing/calls, per-call deadlines, CEL cost, input/output and fan-out limits |
| `consistency` | Source completeness, source versions, freshness/snapshot requirements and commit revalidation obligations |
| `guarantees` | Host-verifiable or certified properties actually required for use; never unverified author claims |

`resultConsumer` is a separately versioned contract because two hooks can accept the same output schema but interpret it at materially different lifecycle locations.

### 3.3 Authoring and discovery are also part of the interface

The host should make the same hook and capability contract discoverable to an AI authoring agent **and** to a technical author: short business purpose, phase, supported outputs, available fields and operations, effect consequences, missing-data choices, limits, and a few neutral test examples. Do not expose arbitrary internal APIs simply because they appear in OpenAPI.

A minimal authoring-facing facade should answer: `listAuthorableHooks(scope)`, `describeHook(hookId)`, `listAdmittedCapabilities(hookId, scope)`, `compileProposal`, `simulateProposal`, and `requestActivation`. These are **host/application operations** over the Keir compiler and simulator, not program-executable `call` capabilities. Descriptions and generated proposals are untrusted; only the underlying approved DCM and binding authority determine execution permission.

This is important for Keir's intended business-user experience: a powerful runtime is not sufficient if an AI agent cannot discover the actual safe customization surface or explain the effects of a proposed activation.

### 3.4 Proposed example: pre-create policy

Illustrative host-side DCM sketch, **not valid Keir program syntax**:

```yaml
id: tasks.beforeCreate
version: 1
trigger: INTERCEPT
placement: AFTER_AUTH_BEFORE_PERSISTENCE
inputType: ProposedTaskCreate
outputType: TaskCreateDecision
resultConsumer:
  kind: GUARD
  version: 1
  allowedDecisions: [CONTINUE, DENY]
  enforceBefore: tasks.create.commit
  nativeAuthorization: REQUIRED
  nativeValidation: REQUIRED
  onContinue: PERFORM_ORIGINAL_OPERATION
  onDeny: RETURN_DECLARED_BUSINESS_ERROR
effectOwner: HOST
activationMode: ALL_MUST_CONTINUE
capabilityGrants:
  projects.get:
    effectClass: READ
    maxCalls: 1
failurePolicy:
  noBinding: CONTINUE_NATIVE
  invalidResult: REJECT_OPERATION
  executionFailure: REJECT_OPERATION
  deadlineExceeded: REJECT_OPERATION
budgets:
  totalDeadlineMs: 300
  maxCallCount: 1
  maxInputBytes: 16384
  maxResultBytes: 2048
consistency:
  nativeTaskWrite: HOST_ATOMIC
  dependencyReads: RECHECK_IF_POLICY_REQUIRES_CURRENT_STATE
```

Here a customer could select `projects.get`, evaluate the project and task details using CEL, and return `CONTINUE` or `DENY`. The host does not implement that customer's project-specific rule. It **does** implement `tasks.beforeCreate`, `projects.get`, and the rule-independent result consumer.

`CONTINUE` means **continue to the native authorization and business checks**; it does not mean “authorized”. `DENY` is an additional host-enforced restriction, not a replacement for native security.

## 4. Hook result and native-effect ownership

### 4.1 Fundamental distinction: computing a result versus performing a native effect

The extension may **call approved operations**, including multiple reads and tenant-provided decision endpoints. That is Keir's expressive value. But there must be **one identified owner** of each final native business effect.

For a synchronous intercept of an existing native operation, the recommended default is:

1. Host authenticates and resolves protected inputs.
2. Keir may execute approved reads/decision operations; CEL computes the policy.
3. Keir returns `CONTINUE`, `DENY`, a bounded `PATCH`, or a declared `SELECT_ACTION`.
4. Host validates/revalidates the result and performs **its original native operation once**.

Keir must not independently perform that same native effect through a second `call`. Otherwise an innocent-looking customization can duplicate credits, payments, writes, or other irreversible effects.

**Policy:** `INTERCEPT` hooks with `effectOwner: HOST` reject effectful `call`s that could mutate business state. Remote decision endpoints can be allowed only if the host declares them non-mutating from the business perspective and accepts their external network/data-disclosure risk. `READ` is not synonymous with free or side-effect-free infrastructure use.

An approved event reaction is different. There the host's original operation has already committed, and Keir may invoke a **separate approved follow-on effect** through an adapter. That effect has its own identity, idempotency, authorization, success/uncertainty status, and accounting semantics.

### 4.2 Result classes and what they mean

| Consumer kind | Typical value | Host effect | Key restriction |
|---|---|---|---|
| `GUARD` | `CONTINUE` or declared `DENY(reason)` | Continue native operation or block it | `CONTINUE` never overrides host authz or validation |
| `PATCH` | Bounded typed changes to approved paths | Apply to the **proposed** state, then validate and commit natively | No writes to protected IDs, accounting references, immutable state |
| `SELECT_ACTION` | Choice among already admitted native alternatives plus typed parameters | Host validates selection and executes the selected native action | No arbitrary operation name, recipient, or funding source |
| `RESPONSE_PROJECTION` | Bounded permitted filter, ordering, or view | Host checks result integrity and returns/transforms response | Cannot undo a previously committed effect; must honor completeness and protected fields |
| `OBSERVE` | Typed status/metadata only | Host records outcome; original operation is already completed | No retroactive veto or implied transaction |
| `EVENT_REACTION` | Typed result plus explicit approved capability effects | Adapter performs separate admitted effects | Replay, uncertainty and re-entry controls mandatory for mutations |

`PATCH` is proposed data, not authority. The host must reject unknown fields, invalid units/amounts, mismatched references, violations of schema and invariants, or disallowed field changes. `SELECT_ACTION` is often a better integration than asking Keir to call an existing native action while the host is itself about to call it.

### 4.3 Execution classification must be separate from business decision

Do not overload `ALLOW`, `REJECT`, or HTTP status to mean that Keir executed successfully.

**Runner execution outcome:** `COMPLETED`, `BUSINESS_REJECTION`, `FAILED`, `LIMIT_EXCEEDED`, `UNCERTAIN`.

**Hook business result:** e.g. `CONTINUE`, `DENY`, `PATCH`, `ACTION_SELECTION`, `PROJECTION`, `ACK`.

The host consumes a business result only if the execution classification permits it. A deliberate typed `DENY` may be a normally completed computation. For an `onError` handler that returns a safe typed `DENY`, the runtime may still classify the underlying capability failure as `BUSINESS_REJECTION`; the consumer may accept **only that explicitly declared deny outcome**, not a patch, approval, or success effect. `TRANSIENT_FAILURE` and `UNKNOWN_COMMIT` must never be rewritten as successful business outcomes.

A `HookExecutionReceipt` should record the artifact/binding revision, invocation identity (redacted for user-visible logs), execution classification, result kind, call/effect outcomes, timeout, and whether the host consumer **accepted/applied** the result. A Keir runtime `COMPLETED` receipt is not evidence that the host actually committed or applied anything.

## 5. Invocation contract and proposed SDK surface

### 5.1 Separate author-visible `input` from protected invocation context

Keir IR/CEL sees only the typed `input` projection plus earlier step results. Protected identity, credentials, tenant/workspace, scope resolution, stable operation keys, and endpoint configuration are **not** general CEL variables. An authorized business fact (such as an actor's approved role category) may be explicitly projected into the input if the hook needs it, with a schema and provenance contract.

Proposed host-generated invocation (conceptual JSON):

```json
{
  "hook": {"id": "tasks.beforeCreate", "version": 1},
  "activation": {"bindingId": "host-selected-binding", "artifactDigest": "sha256:..."},
  "input": {"title": "Review invoice", "projectId": "project-42", "priority": "URGENT"},
  "trustedContext": {
    "tenantId": "host-bound",
    "workspaceId": "host-bound",
    "actorRef": "host-bound",
    "invocationId": "host-generated",
    "requestOrEventRef": "host-generated",
    "entityVersion": "host-observed",
    "deadline": "host-monotonic-deadline"
  }
}
```

This is a **trusted host SDK object**, not an untrusted JSON request accepted verbatim from the customer or agent. An embedded library cannot authenticate its caller merely because an object is named `TrustedHookInvocation`: the host must restrict who can construct/submit it, and the runner must verify binding/catalog/grant consistency and runtime scope. If the runner is ever exposed over a process boundary, that boundary needs its own authenticated transport. Values such as `invocationId` cannot be changed through author-supplied IR, CEL, or adapter arguments.

### 5.2 Suggested portable API boundaries (signatures illustrative)

```typescript
// Host integration, not a new language instruction.
interface KeirHostRuntime {
  prepare(artifact: VerifiedArtifact, catalog: EffectiveCatalog,
          adapters: HostAdapterRegistry): PreparedExtension;
}

interface PreparedExtension {
  execute(invocation: TrustedHookInvocation): Promise<KeirExecutionOutcome>;
}

interface HookConsumer<Input, Result> {
  validateAndApply(input: Input, outcome: KeirExecutionOutcome<Result>,
                   hostOperation: NativeOperationContext): Promise<ConsumptionReceipt>;
}
```

The Java integration should expose the **same data contracts and semantics**, with Java-idiomatic asynchronous results where adapters may perform I/O. A conceptual Java equivalent is:

```java
public interface PreparedExtension {
    CompletionStage<KeirExecutionOutcome> execute(TrustedHookInvocation invocation);
}

public interface HookConsumer<I, R> {
    CompletionStage<ConsumptionReceipt> validateAndApply(
        I input, KeirExecutionOutcome<R> outcome, NativeOperationContext operation);
}
```

These are illustrative interfaces, not an implemented SDK. Java is not required at Node runtime. The `HookConsumer` belongs to the host, although Keir can supply reusable validators and conformance tests. The effect of a hook must not depend on which runner language executes the compiled artifact.

A consumer must never trust a caller-provided `KeirExecutionOutcome` without verifying it originated from a selected approved invocation and passed schema, scope, and result-classification checks. An API method name is not a security boundary.

### 5.3 Activation selection and scope

Activation is an independent, host-authorized decision:

```text
authenticated host operation or durable event
  -> resolve tenant/workspace/program/entity scope
  -> select pinned active hook binding(s)
  -> verify artifact + effective catalog + consumer version
  -> run with host-owned context and current adapter authorization
  -> consume result with the matched host contract
```

The customer must not select another tenant's binding, a broader workspace, or a stronger hook merely by setting IR fields. Scope overlap and binding precedence must have deterministic rules. The smallest v0 rule is that the host rejects ambiguous overlapping `EXCLUSIVE` bindings rather than silently choosing whichever sorts first.

## 6. Synchronous hook lifecycle and consistency

### 6.1 Correct integration sequence

For a `PATCH` or `GUARD` intercept:

```text
Host: authenticate + authorize + resolve entity
    -> construct authoritative proposed state or bounded input
    -> select active Keir artifact(s)
    -> execute approved reads/decisions without unsafe host locks
    -> verify Keir outcome and result variant
    -> apply patch / deny / selection
    -> rerun native validation and invariant checks
    -> revalidate relevant mutable facts or reject stale decision
    -> perform native effect and commit exactly through host's original path
    -> record separate host-consumption outcome
```

**The placement must precede the effect it intends to control.** A post-booking veto cannot prevent booking; a post-response filter cannot reverse settlement. Native validation may need to run both before and after an extension patch. A before-persistence derived value must be based on the merged proposed state, not a disconnected stale read.

### 6.2 Time-of-check/time-of-use and remote reads

Suppose a pre-create rule reads a related project and denies creation if it is locked. The rule may be well-typed and the lookup may be authorized, yet the project can change before commit.

The hook contract must declare whether the decision is:

- **Advisory:** a stale check is acceptable for the product semantics, with explicitly limited guarantees; or
- **Authoritative:** the host must recheck a version/predicate within an appropriate atomic transition or enforce another proven serialization/locking mechanism.

A host cannot declare an authoritative invariant simply because Keir checked it earlier. If an authoritative cross-entity recheck cannot be made, that use case is **unsupported by the host contract**, not “solved by Keir”.

**Do not keep a database transaction open while calling an untrusted tenant endpoint.** The hook can read remotely, return a proposal, and then the host must revalidate whatever can change before committing. If exact atomicity across systems is required, a separate host business protocol is needed.

### 6.3 Correct failure policy

Every hook must define distinct behavior for:

- No active extension (optional enhancement versus mandatory gate).
- Invalid or incompatible artifact / schema / result.
- Explicit business `DENY`.
- Capability `BUSINESS_REJECTION`.
- `TRANSIENT_FAILURE`, missing facts or stale facts.
- Deadline/CPU/input/output budget exceeded.
- `UNKNOWN_COMMIT` after an approved effectful call.

**Fail-open is permissible only where the host explicitly treats customization as optional.** A mandatory financial/security guard must not turn absence, timeout, or malformed output into `CONTINUE`. Fail-closed is also not a replacement for correct routing across all paths. Unknown effects require reconciliation; a simple fallback may be unsafe.

### 6.4 Nested hooks and capability-mediated re-entry

An approved capability may call a host service that itself has Keir hooks. If the capability adapter simply calls a public API, a read can accidentally trigger a response-transformation hook, which in turn calls that same read again. An effectful event reaction may also legitimately invoke a new native operation subject to its own mandatory guards.

The host must define **which hook boundaries are crossed by each adapter call**. For ordinary domain reads, prefer a dedicated authorized adapter returning an authoritative typed projection rather than invoking an externally personalized response pipeline. Never bypass underlying native authorization. For effectful calls, preserve mandatory native validations and guard hooks; a “trusted internal caller” must not silently skip them. Pass trusted causation/re-entrancy context, reject undeclared synchronous hook cycles, and cap allowed invocation depth and total work. This rule is separate from asynchronous event-cycle protection.

### 6.5 Response projection and completeness

The host contract must specify whether it supplies a complete bounded candidate set, a stable snapshot, a page, or a deliberately partial view. An extension that orders an entire benefit list cannot claim global ordering when it sees only page one. The runner validates item count and output shape, while the host validates the meaning of completeness and permitted field changes.

Support separate declared consumers for filtering, selection, reordering, and field projection where they have different invariants. A general `replaceResponse(any JSON)` is not a safe interface.

## 7. Event-triggered extensions require a delivery contract

### 7.1 Event is a new invocation, not a suspended program

A committed host operation may produce a durable event. A selected Keir extension receives an admitted **one-shot** invocation and may perform permitted follow-on calls. The host is responsible for event production, durable delivery, scheduling/backpressure, and endpoint availability; Keir does not become a broker or workflow system.

Recommended host sequence:

```text
native transaction commits + durable outbox record
    -> event dispatcher (possibly at-least-once)
    -> select activation and persist binding/artifact at admission
    -> each bound extension gets its own stable effect identity
    -> Keir runs bounded composition against trusted adapters
    -> persist COMPLETE / FAILED / UNCERTAIN and confirmed effects
    -> acknowledge, reconcile, or route to operator action
```

Use the existing host broker/outbox where available. CloudEvents can supply a standard event envelope (`source`, `id`, `type`, optional `subject` and `time`), but it does **not** itself supply durable delivery, an authorization principal, idempotency, or a Keir activation identity.

### 7.2 Replay and deduplication

A delivery key should identify at least `(event source, event ID, hook ID/version, activation binding revision)`. Each distinct effectful step gets a stable **host-generated operation identity** derived from that admitted delivery and step, not an agent-provided key.

After a crash, a retry must preserve the prior admission's artifact version and effect identities. The system must distinguish `NOT_STARTED`, `RUNNING/LEASED`, `CONFIRMED_EFFECT`, `FAILED`, and `UNCERTAIN` rather than merely marking the message read. If an effect may have committed without confirmation, do not blindly reissue it. Require adapter-specific idempotent replay or reconciliation evidence.

Event ordering must be defined per relevant entity when newer state should supersede older events. If ordering is not available, use entity versions/current-state checks and a declared stale-event policy. A `ProfileChanged` event containing old/new facts needs a trustworthy source version if its result changes lasting membership or rewards.

### 7.3 Activation-time semantics

Two legitimate policies exist:

- **Bind at event commit:** the policy active when the business event occurred is captured into the outbox/admission record. Stronger historical reproducibility; more host integration.
- **Bind at delivery admission:** the policy active when the event first enters extension processing is pinned and used on all retries. Easier integration, but delayed events may use newer rules.

The host must **choose and document one per event source**. The v0 default recommendation is **at first durable delivery admission**, pinned on retry. Use commit-time binding where event-time rule correctness is a business requirement. No silent re-resolution of the latest extension after a failed attempt.

### 7.4 Causation and re-entry

A follow-on call may itself emit another event. The host should propagate a trusted causation ID/root invocation and bound re-entry depth. Default self-triggering paths should be rejected or suppressed unless explicitly approved. Protect against cycles across **multiple different extensions**, not only a rule calling itself. Supply per-event and per-tenant fan-out and aggregate-effect quotas. Ordinary source events must still be publishable when an extension is disabled.

## 8. Multiple extensions at one hook

This is a genuine missing decision in v0.4. Letting users create several extensions without defining aggregation produces unpredictable behavior.

### 8.1 Proposed activation modes

| Mode | Suitable hooks | Semantics |
|---|---|---|
| `EXCLUSIVE` | Patch, action selection, response ordering, complex transformations | Exactly one effective binding for a hook/scope; ambiguous overlaps rejected |
| `ALL_MUST_CONTINUE` | Pure additional-deny policies | Execute bounded policies against the same input; **any** denial or host-configured failure blocks; no mutations or result patch merge |
| `INDEPENDENT_FANOUT` | Post-commit event reactions | Each activation executes separately with its own receipt, grants and replay identity; total fan-out bounded |

**Default to `EXCLUSIVE` for v0 interception hooks.** Add `ALL_MUST_CONTINUE` only if the host explicitly offers a pure veto consumer and can enforce an aggregate deadline. `INDEPENDENT_FANOUT` is needed if multiple post-commit customer reactions can subscribe to one source event.

Defer arbitrary ordered chains of patches. Two authors changing the same amount must not silently use last-writer-wins; they may need an explicitly authored combined extension or a future, schema-specific associative merge rule with conflict detection. Combining all-deny decisions is safe only when the host's fail policy and native authorization remain intact.

### 8.2 Binding governance

The host decides who can author/approve policies at each scope, whether one workspace overrides an account-level policy, whether a group owner may issue an additional deny, and whether a policy applies to public/admin/bulk paths. Tenant administrator authorization does not automatically grant entity-owner authority. The author must not choose binding priority or bypass a higher-priority mandatory policy through source IR.

## 9. Capabilities, external endpoints and data exposure

### 9.1 Capability declaration must describe real use, not just HTTP method

For each `call`, the effective catalog needs: semantic name/version, typed inputs/outputs, host-bound parameters, **effect class**, recipient/target scope, per-call deadline, cardinality/bytes, declared business errors, idempotency/reconciliation behavior, and evidence for current authorization and other guarantees.

Useful effect classes are `READ_LOCAL`, `READ_REMOTE`, `EXTERNAL_DECISION`, `BUSINESS_WRITE`, and `EXTERNAL_WRITE`. A nominally HTTP GET request is not automatically a trustworthy or complete read, and a remote decision call still creates outbound traffic and can expose customer data.

At compile time reject a `BUSINESS_WRITE` or `EXTERNAL_WRITE` inside a hook whose effect policy disallows it. At runtime independently enforce current grants, scope, capability/tenant registry digest and adapter classification. A valid static catalog is never permission to access an object owned by another tenant.

### 9.2 Tenant-registered endpoints

Keir does not register, discover, or invoke arbitrary endpoint URLs from customer IR. The host registers and approves a **typed capability**, owning endpoint verification, TLS, allowlisted destinations, DNS/IP checks, redirects, credentials, egress policy, rate limits, and transport; the Keir runner sees only a named adapter.

The host must publish which **specific input fields** may be sent to each tenant endpoint. A generic `input` object, unfiltered native response, or credentials map must not be passable wholesale. Field-level information-flow policy is hard to prove across arbitrary derived expressions: for v0, use closed, sanitized adapter argument schemas plus explicit data-disclosure grants; never claim comprehensive declassification just from CEL static checking.

Recommended first tenant endpoint profile: bounded read-only verification/decision, deterministic enough for simulation, with explicit timeout and failure semantics. Remote mutating effects require separate idempotency/reconciliation certification and should be deferred from the first intercept POC.

## 10. Budgets and portability across Node and Java

The **host request deadline** covers the entire native operation, including the hook and its final consumption/commit path. The **Keir hook sub-deadline** covers activation lookup, queueing, CEL computation, trusted adapter calls and result validation, not merely expression evaluation. Reserve a separate native-consumer/commit budget rather than allowing Keir to consume the entire request deadline. A per-call timeout cannot exceed the remaining hook sub-deadline.

Both runners must apply the same Keir-approved CEL subset, typed inputs, checked-expression artifact, worst-case list/cardinality assumptions, and invocation budget semantics. Language-specific containment mechanisms may differ:

- **Java runner:** CEL-Java 0.14.0 supports a bounded total number of comprehension iterations, including nested loops. This is useful but is not a proven hard limit on all CPU/memory work or a thread-kill mechanism.
- **Node runner:** CEL-ES 0.6.1 does not expose an equivalent comprehension iteration counter in the evaluated execution path. Static admission bounds are therefore essential; a prewarmed worker may provide a practical termination boundary when stronger time isolation is required. Worker heap limits are not complete process isolation.

**Do not infer Java/Node production equivalence from the existing expression-only interoperability tests.** Host contract conformance, data representations, cancellation/timeout outcomes, overload allowlisting, costs, and adapter effects need independent cross-runtime tests.

Runtime limits cannot make an unreliable remote effect atomic, and hard timeouts cannot establish whether a timed-out write committed. Keep those failure classes distinct.

## 11. Artifact, DCM and lifecycle compatibility

A compiled artifact should pin:

- Keir IR/compiler/portable-artifact/CEL-profile versions.
- Shared DCM and effective tenant capability catalog digests.
- Hook ID/version, **result-consumer ID/version**, effect-ownership policy, required host certifications.
- Approved binding scope, capability grants, data disclosure restrictions, resource limits.
- Source/simulation/test revision approved by the host; artifact digest and signing/integrity metadata.

On activation and every loading boundary, the host verifies the exact compatible catalog, adapter contracts, binding authority and consumer version. A syntactically unchanged return record is not sufficient if the consumer's financial or transactional meaning changed. A newly widened field, action, or endpoint may expand authority even without altering the method name.

Rollbacks change **which artifact is selected for new admissions**, not the artifact used by an in-flight operation or pinned event replay. For urgent kill switches, the host may prohibit subsequent effect calls even for a previously admitted artifact; resulting stopped/incomplete executions must be recorded honestly.

## 12. Simulation and host conformance

### 12.1 The simulation boundary must include the consumer

Running the same artifact with fake adapters is necessary but no longer sufficient. The POC simulator should also have a **fake host consumer** that takes Keir's typed result and applies the actual consumer rules: veto, legal patch application, revalidation, native commit simulation, response projection, or event effect recording.

Business users should see both:

1. **Program trace:** which approved capabilities were called, which conditions executed, and what result was returned.
2. **Host outcome trace:** which patch/decision the host accepted, whether native validation succeeded, whether a business effect was committed, and whether the outcome was confirmed or uncertain.

An agent-created test is not an independently verified business oracle. Users or authorized domain owners must approve the expected outcomes.

### 12.2 Minimal host conformance suite

The host integration should pass tests for:

1. **Routing:** an extension bound to a mandatory guard is invoked on every declared public/internal/bulk entry path; uncovered paths are rejected from the hook's coverage claim.
2. **Native authority:** returning `CONTINUE` never bypasses authentication, authorization, domain validation or financial invariants.
3. **Result consumption:** a valid `DENY` actually blocks; a legal patch is applied before commit; an illegal/protected patch is rejected; a projection is not silently ignored.
4. **Placement:** a pre-effect hook fires before the first irreversible effect. A post-commit event cannot retroactively prevent the original action.
5. **Consistency:** stale dependency checks are detected or explicitly classified advisory; merged-state patches use the proposed write state.
6. **Failure:** absent mandatory binding, malformed output, incompatible artifact, budget exceed, transient adapter failure and `UNKNOWN_COMMIT` follow the declared host policy.
7. **Replay:** duplicate events preserve binding revision and stable effect identity; uncertain effects are not blindly replayed.
8. **Causation:** a customer-defined follow-on effect does not create unbounded self/cross-extension loops.
9. **Data disclosure:** protected IDs and credentials never appear as agent-bindable fields; tenant endpoints receive only approved projected data.
10. **Multi-binding and re-entry:** simultaneous denial policies combine predictably; conflicting patch bindings cannot both activate; nested adapter calls cannot create undeclared hook cycles or bypass mandatory native checks.
11. **Compatibility:** old artifact with changed consumer/effective catalog fails to load unless explicitly certified compatible.
12. **Node/Java parity:** identical fixtures and approved failure classes for both runners once Java exists.

Keir can provide portable fixtures, a sample consumer, and a conformance-kit interface. Host teams are responsible for proving their actual integration paths and guarantees.

## 13. Illustrative end-to-end patterns

### Pattern A: Synchronous guard with customer-selected read

A user asks: “In projects marked as restricted, don't allow urgent tasks without a justification.”

```text
Host tasks.beforeCreate
  -> Keir projects.get(input.projectId) [approved READ]
  -> CEL tests project setting and task justification
  -> Keir returns CONTINUE or DENY
  -> Host enforces DENY or continues original create path after native validation
```

No customer-specific task-service code is added. The host must still publish the hook, project-read capability, trusted project/actor scope and result consumer.

### Pattern B: Synchronous proposed-value patch

A user asks: “Use the project default priority when a new task has no explicit priority.”

```text
Host constructs merged proposed task
  -> Keir projects.get(input.projectId) [approved READ]
  -> CEL computes permitted default priority
  -> Keir returns PATCH(priority)
  -> Host validates modified task and persists it natively
```

The extension does **not** call `tasks.create` a second time. Patch rights apply to a narrow allowlist, and a later native validation failure means no task was created.

### Pattern C: Post-commit event reaction

A user asks: “Whenever a completed task has a specific tag, create an approved follow-up task.”

```text
Host commits task completion and records event
  -> durable event admission pins approved Keir version
  -> Keir tasks.get / tasks.search / CEL condition
  -> Keir tasks.create [separate approved effect with host replay identity]
  -> Host adapter confirms or records uncertain outcome
```

The original task completion is unaffected. A duplicate event cannot create another follow-up if the host-native task-create operation enforces stable origin uniqueness. A preceding search is not an atomic duplicate guard.

### Pattern D: Bounded response transformation

A user asks: “Show the highest-priority available items first within the complete selected view.”

```text
Host supplies complete bounded view or explicitly typed partial view
  -> Keir performs pure sorting/selection using typed CEL
  -> Keir returns a projected view
  -> Host verifies IDs/protected fields/completeness and responds
```

For global ordering, an incomplete page must be rejected or the consumer must explicitly declare partial ordering; it cannot silently pretend to sort all items.

## 14. Decisions, deferrals and recommended implementation slice

### Proposed for v0.5

1. Add a versioned `HookDefinition`/`ResultConsumer` contract to DCM. No new customer-facing IR syntax.
2. Default intercept effect ownership to the host. Pre-effect Keir calls may read/decide but not independently mutate the same native business state.
3. Distinguish runner completion from business decisions and host consumption receipts.
4. Default sync binding composition to `EXCLUSIVE`, with optional pure `ALL_MUST_CONTINUE`. Event reactions may use bounded `INDEPENDENT_FANOUT`.
5. Require explicit no-binding/failure/timeout behavior and declarative routing/consistency claims.
6. Treat async source events as independent invocations, using host-owned durable admission and replay tracking when effects matter.
7. Include a simulated result consumer and host conformance fixtures in the POC.

### Not for v0

- General workflow scheduling, durable waits, compensating transactions, or cross-system atomicity.
- Arbitrary ordered policy chains, automatic patch conflict resolution, or unrestricted response mutation.
- A Java/Node-specific hook language or JavaScript/Java callbacks authored by customers.
- Automatic endpoint registration or trust established by YAML declarations.
- A general IAM/policy engine replacing native authorization.
- Universal collection completeness, data freshness, or current-state consistency guarantees.

### First practical integration

Keep the existing **fictional Node task-management host** and its `TaskCompleted` post-commit extension. Add **one** synchronous `tasks.beforeCreate` guard/patch consumer. This exercises two genuinely different result paths without adding a second deployed service or changing the IR language. Build the host conformance suite and simulation of consumer behavior before broadening the catalog. A Java runner should later run the same contract fixtures and compiled artifacts; do not invent a Java-only integration model.

## 15. Open decisions to validate by implementation

1. How should host bindings across account/workspace/group scopes inherit and conflict? Start with no ambiguous overlapping exclusive bindings; define finer inheritance only after a real use case.
2. Which hooks merit authoritative cross-entity decisions? Those require concrete host version/revalidation evidence, not an IR flag.
3. Do any real host consumers need **multiple independent patches** in the same operation? If yes, design an explicit typed merge algebra; do not implicitly sequence them.
4. What durable delivery store, ordering guarantees and policy-at-event-time semantics does a host actually have?
5. How do the Node and Java runners enforce worst-case CEL cost and cancellation under the configured hook budget? Prior CEL-Java/CEL-ES expression interoperability does not answer this.
6. Which endpoint data can be disclosed to a tenant-registered decision provider, and who approves that projection?

These are **host-contract and runtime engineering questions**, not evidence that the Keir programming language needs more control-flow constructs.

## References and evidence boundaries

**Design baseline:** Keir Architecture v0.4; Keir Hooks and Tenant Capabilities v0.4; Keir Extension IR v0; Keir Task Management POC v0.4 (existing local documents). This document is an explicit proposed revision, not a replacement for those language/runtime specifications.

**Motivation:** A separate, private October 2026 SaaS requirements assessment evaluated synchronous policy hooks, derived-field updates, event-triggered reward reactions, native financial preparation, bounded response transformation, and reliable event semantics. It is not reproduced here; the technical principles are generalized and fictionalized.

**Public technical references:**

- CloudEvents v1.0.2, standard event envelope and unique source + ID semantics: https://github.com/cloudevents/spec
- AWS Transactional Outbox Pattern, duplicate delivery and ordering considerations: https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html
- OWASP SSRF Prevention Cheat Sheet, registered external endpoint destination and DNS protections: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
- CEL-Java 0.14.0 options, comprehension iteration limits: https://github.com/cel-expr/cel-java/blob/v0.14.0/common/src/main/java/dev/cel/common/CelOptions.java
- CEL-ES 0.6.1 planner/evaluator: https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/src/planner.ts

**No certification claim:** All host schemas, SDK types, binding policies, event admission rules and examples in this design are proposals. Neither host consumption correctness, hard runtime limits, nor reliable delivery has yet been proven end-to-end in Keir.
