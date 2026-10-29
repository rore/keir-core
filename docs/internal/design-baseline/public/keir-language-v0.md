# Keir Extension IR v0 - normative draft (architecture v0.4)

**Package baseline:** v0.4 (public working draft, 28 October 2026). The language remains at semantic profile v0; the pre-release source identifier is now `keir/v0`. No new execution instruction is introduced.

**Status:** Proposed normative **human- or agent-authoring** format, updated 28 October 2026. **Selected v0 language:** closed structural YAML/JSON IR with a restricted CEL subset, compiled by one authoritative Java compiler into a separate portable executable artifact. The Open Workflow 1.0.3 profile remains an assessed alternative, not the chosen syntax and never the runtime. **The IR compiler and DCM overlay have not yet been implemented.** A separate Java/Node expression-interoperability harness has passed for the tested CEL subset; see [decisions and evidence](decisions-and-evidence.md).

Normative terms **MUST**, **MUST NOT**, **SHOULD**, **MAY** apply to a conforming v0 compiler or host. Implementations MUST reject any unsupported construct rather than ignore it.

## 1. Execution and trust scope

An extension runs once within a host-supplied entrypoint. It is an **acyclic, bounded list of pure bindings and statically identified capability calls**. The host supplies the invocation, trusted adapters, effective capability catalog and hook result-consumer semantics. The Keir core has no agent dependency. The program cannot schedule itself, create event consumers, run scripts, or supply a tenant/caller identity. The executor receives **only a `ValidatedPlan`**, never YAML directly.

The encoded format is UTF-8 YAML or JSON. Parsed mappings MUST reject duplicate keys; unknown properties MUST fail validation. Arrays preserve order. The compiler MUST canonicalize the typed representation independently of YAML key order.

```yaml
language: keir/v0
domain:
  id: demo.tasks
  version: '0.1.0'
  fingerprint: sha256:a48d02c0070212a5186f2a06f59c625462a8e24991b16cdcc0dfc0f64ab63caf
entrypoint: TaskCompleted
name: read-completed-task
steps:
  - id: source
    call: tasks.get
    args:
      taskId: {cel: 'input.taskId'}
return:
  record:
    status: {literal: NO_OP}
```

The fenced block above is illustrative. The updated package contains a complete [task-management follow-up IR](examples/followup-extension.yaml); the previous language-design agent drafted additional assignment/dependency cases not included here. Machine-readable structural syntax in this updated package: [draft `schemas/extension-ir-v0.schema.json`](schemas/extension-ir-v0.schema.json). The schema validates shape only; semantic checks are separate.

## 2. Document fields

| Field | Rule |
|---|---|
| `language` | Literal `keir/v0`. No implicit future-version fallback. |
| `domain.id/version/fingerprint` | MUST match one immutable host-approved Domain Capability Model (DCM), including its fingerprint. |
| `entrypoint` | MUST exist in the DCM. The host controls when it runs and how the result is consumed; the program does not declare a listener, choose a hook phase or set its fail-open/fail-closed policy. |
| `name` | Lowercase alphanumeric and hyphens, unique per domain/entrypoint deployment. |
| `steps` | 1–16 ordered steps, further capped by entrypoint `maxSteps`. IDs unique across the entire program. |
| `return.record` | Typed record produced on successful completion. MUST exactly match the entrypoint output type; no extra keys. |

A deployment identifier and revision are assigned by the host; they are not author authority. A tenant-specific capability is resolved only from the **trusted compile context**, never from a tenant declaration or URL added to the IR. A production plan MUST bind the compiler version, canonical program digest, DCM digest, entrypoint, granted effects, and runtime quotas.

## 3. Exact step grammar and execution semantics

A step has a unique `id` and exactly one of the following variants.

**Pure binding (`let`)**

```yaml
- id: eligible
  let: {cel: 'task.status == "COMPLETED"'}
```

The CEL expression MUST be pure and statically typed. Its result is bound immutably under `eligible`. A pure literal is also valid: `let: {literal: 10}`. A `let` is always executed and cannot be conditional in v0.

**Capability call (`call`)**

```yaml
- id: update
  when: eligible
  call: updateTask
  args:
    taskId: {cel: task.id}
    expectedVersion: {cel: task.version}
    assigneeId: {cel: chosenAssignee}
```

`call` MUST be the exact static name of a granted, pinned capability in the effective catalog supplied by the host (shared DCM plus optional tenant-scoped capabilities). `args` MUST contain every required **agent-bound** argument, MAY contain a declared optional agent-bound argument, and MUST NOT contain undeclared or host-bound arguments. Every argument MUST be a binding object of exactly one shape: `{literal: <JSON value>}` or `{cel: <CEL source string>}`. No partially interpolated strings, arbitrary expression-language identifiers, nested source code, endpoint names, SQL or URLs may be introduced as executable operations. Ordinary text values are data, never an execution selector.

`when` is optional and MUST name an earlier, unconditionally bound Boolean `let` step. When false, the call is not made and the result binding is `Absent<CapabilityResult>`; it creates no effect. A later expression MAY reference a conditional call's output only through a compiler-proven guarded flow; if safety cannot be proven the program MUST be rejected. v0 examples deliberately avoid this complexity.

Calls execute sequentially, **after** the preceding call's declared success has been confirmed. The evaluator MUST NOT treat a timeout or uncertain response as success or absence of effect. The adapter is the only authority for effect results.

**Terminal typed business handling (`onError`)**

```yaml
- id: lookup
  call: getTask
  args:
    taskId: {cel: input.taskId}
  onError:
    TASK_NOT_FOUND:
      record:
        status: {literal: TASK_NOT_FOUND}
        candidateTaskIds: {literal: []}
```

`onError` is a map from *declared, handleable business error code* to a terminal result record with the same type as the entrypoint output. It is **not** a general-purpose exception handler. It cannot call other capabilities, retry, or convert `UNKNOWN_COMMIT` or `TRANSIENT_FAILURE` into business success. An unhandled business failure terminates as `BUSINESS_REJECTION`. A handled business failure returns the declared record with execution classification `BUSINESS_REJECTION`, preserving error provenance. This is the simplest v0 alternative to Open Workflow's `try/catch` and `raise` machinery.

**Return**

`return.record` is evaluated only after all required steps have succeeded or been skipped. Each field uses a binding object; extra fields, missing required fields, wrong nominal units or unbounded collections fail validation. Return evaluation has no effects. Root-level `return` MUST NOT access a result absent on a possible path.

## 4. Control flow and bounds

v0 has **ordered steps + guarded calls + CEL conditional expressions**. There is no general `if` block, `switch`, `for`, `while`, recursion or mutable local variables. CEL bounded list `all`, `exists`, `filter`, and `map` may compute *pure* projections over lists with a DCM-imposed maximum cardinality. No call may occur inside CEL. In particular, a list of candidate IDs can be returned, but v0 cannot dynamically call `updateTask` for every candidate.

The compiler MUST calculate the worst-case call count as the number of `call` nodes (guards never increase this bound), check per-capability grants, and accumulate a conservative effect set. The runtime MUST independently enforce step count, calls per capability, output item count, object/string size, CEL evaluation cost, elapsed time, and adapter-provided result caps. Passing static analysis is not a substitute for runtime metering.

The DCM decides whether a call is legal in a given entrypoint, which write effects are admissible, and which required host certifications must be active. Any missing certification blocks activation. An individually valid read followed by a write does not create a transaction.

## 5. CEL binding contract

Every expression sees only:

- `input`: immutable typed **business invocation input** from the declared entrypoint. No tenant credentials, arbitrary headers, `principal`, tokens, or host implementation objects.
- Earlier step IDs: immutable typed `let` values or **confirmed** capability results. A conditional call result is optional.
- Locally introduced CEL macro iterators inside an expression.

There is no arbitrary `context` map in the selected language, no host-object reflection, and no variable shadowing of `input` or any other step ID. Step IDs are unique and must not collide with reserved identifiers or CEL names. The compiler MUST build a fresh checked CEL environment for each step, containing **only earlier definitely available bindings**. It MUST reject unknown names, opaque `dyn` values, forbidden functions, nullable access without a valid guard, and static output types inconsistent with DCM.

Permitted CEL primitives/operators are Boolean, string, signed 64-bit integer, timestamp, duration, typed record/list selection, `==`, `!=`, comparisons of same types, Boolean operators, `?:`, checked integer `+/-/*`, timestamp + duration, and macros `all/exists/filter/map` over statically bounded lists. Permit `size()` on strings/lists, and `duration()` on trusted literal durations. `timestamp()` may parse explicitly typed input strings only when the DCM declares that conversion. **No user functions, external lookups, regular expressions, reflection, `dyn`, arbitrary custom bindings, floating-point rounding, random/time functions, or unbounded comprehensions.** The compiler MUST inspect the checked CEL AST, not merely grep text.

Nominal IDs and units are **DCM compiler types**, not automatically a built-in CEL-Java nominal-type system. CEL-Java provides the checked expression kernel; a separate type overlay MUST prevent, for example, `TaskId` being assigned to `UserId` even if their wire representation is `string`. The evaluator MUST preserve these tags across adapter boundaries. Nullable values must be explicitly checked before dereference (or rejected); the v0 fixtures avoid optional dereference.

Exact money/points arithmetic MUST use checked signed integer minor units with declared unit and scale, avoiding `double`. There is **no v0 general decimal operator**. Cross-unit arithmetic is rejected unless the DCM supplies an approved, pure conversion with explicit rounding semantics, added as a future profile change. Units and overflow MUST be checked again by the trusted adapter.

CEL compilation and execution require separate budgets. Each expression MUST fit its entrypoint's AST size/depth and runtime cost limits; aggregate cost across the invocation MUST be bounded. Implementation-specific CEL-Java cost enforcement MUST be proven in the Java POC; Java thread interruption alone is not a demonstrated CPU/resource bound.

## 6. Outcome semantics

Each capability returns one tagged outcome:

1. `SUCCESS(value, verifiedEffectEvidence?)`: downstream steps may execute.
2. `BUSINESS_REJECTION(code, data?)`: terminal by default; a declared `onError` may provide a typed **business-rejection** return.
3. `TRANSIENT_FAILURE(code)`: terminal in v0. **No automatic retry**, even for reads, until a versioned host retry contract is introduced.
4. `UNKNOWN_COMMIT(code, evidence?)`: terminal **UNCERTAIN** result. No subsequent steps, no automatic retry, no assumed rollback, and no ordinary successful output. The host retains the stable operation identity and performs out-of-band reconciliation if supported.

For writes, idempotency keys and authoritative principal/tenant context MUST be constructed by trusted host adapters. The program cannot construct replay identity, even through a typed string parameter. A response with HTTP status `2xx` is not automatically `SUCCESS`; the adapter performs operation-specific mapping, including partial results.

## 7. Rejected constructs

Any unknown field or construct MUST fail. In particular: Java, JavaScript, SQL, shell, HTTP destination selection, arbitrary URL function invocation, **tenant endpoint registration**, dynamic capability names, external resource imports, loops, parallelism, waits, durable event delivery, scheduling, custom macros/functions, host context override, user-selected idempotency keys, compensations, retry policies, external mutation of local state, branch-to-task labels, and script execution.

## 8. Activation & compatibility

A plan is compiled with **the exact** DCM version and canonical digest, approved entrypoint, granted capabilities/effects, adapter contract certifications, CEL policy version, compiler version and required runtime quotas. Changing an operation's signature, errors, effects, units, host binding, retry/uncertain behavior, security scope, or a relevant guarantee requires revalidation and normally a new DCM version. A schema widening may still change effect authority and is **not automatically backward compatible**.

Never reuse a plan against an updated DCM based only on matching operation names. Old active plans must either retain their pinned verified catalog and adapters or undergo explicit recompile/approval; reject if neither is possible.

## 9. Conformance evidence

The original 76-file language-design package had limited schema/fixture checks, not a working compiler. **Separate, later real-library CI evidence** demonstrated CEL-Java `0.14.0` checked-expression export and CEL-ES `0.6.1` execution for 25 semantic fixtures and 46 harness checks ([run #40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668)). That verifies a subset of *expressions*, not this complete IR grammar, effect enforcement, nominal/unit overlay, or end-to-end runner. A companion [structural JSON Schema](schemas/extension-ir-v0.schema.json) and a complete [follow-up sample](examples/followup-extension.yaml) are included as **draft, non-certified implementation input**.


## 10. Complete v0 IR example: invoice payment follow-up

The following sample is **concrete authoring syntax**, not a claim that the Java IR compiler already exists. Its DCM fingerprint is tied to the included fictional [task domain fixture](examples/task-domain.yaml) by this package's documented example digest algorithm. Runtime activation would additionally require actual trusted host certifications and approval.

```yaml
# Illustrative agent-authored v0 extension. DCM contracts are pending certification.
language: keir/v0
domain:
  id: demo.tasks
  version: '0.1.0'
  fingerprint: sha256:a48d02c0070212a5186f2a06f59c625462a8e24991b16cdcc0dfc0f64ab63caf
entrypoint: TaskCompleted
name: invoice-payment-followup
steps:
  - id: source
    call: tasks.get
    args:
      taskId: {cel: 'input.taskId'}
    onError:
      TASK_NOT_FOUND:
        record:
          status: {literal: SOURCE_MISSING}
  - id: existing
    call: tasks.search
    args:
      projectId: {cel: 'source.projectId'}
      originSourceTaskId: {cel: 'source.id'}
      originKind: {literal: payment-followup}
      maxResults: {literal: 5}
  - id: eligible
    let:
      cel: 'source.status == "COMPLETED" && source.tags.exists(t, t == "invoice-sent") && size(existing.items) == 0'
  - id: created
    when: eligible
    call: tasks.create
    args:
      projectId: {cel: 'source.projectId'}
      originKind: {literal: payment-followup}
      title: {literal: 'Follow up on payment'}
      dueAt: {cel: 'input.completedAt + duration("168h")'}
    onError:
      FOLLOWUP_ALREADY_EXISTS:
        record:
          status: {literal: NO_OP}
return:
  record:
    status: {cel: 'eligible ? "CREATED" : "NO_OP"'}
```

**Meaning:** On a host-delivered `TaskCompleted`, read the completed task, search for existing matching follow-up tasks, compute eligibility, and invoke `tasks.create` at most once. `tasks.create` is an ordinary generic task creation capability, not a vendor-written `createPaymentFollowup` routine. The creation request uses exactly the **agent-bound** arguments; source task ID, actor, tenant and stable operation key are **host-bound**. On successful creation the returned `CREATED` status is valid; if creation is skipped it returns `NO_OP`. A handled duplicate or missing-task business error returns a typed terminal record but keeps the run classified as a business rejection, not ordinary success.

**Important limitations:** `tasks.search` is not proof against concurrent duplicates. A host-side uniqueness constraint on `(tenant, sourceTaskId, originKind)` and replay-safe operation identity are mandatory for real activation. The duration is specifically **168 elapsed hours**, not a timezone-sensitive seven calendar days. This sample does not prove its compiler or adapter contracts until implemented and tested.

## 11. Exact language rules that matter for portable compilation

| Construct | Authoring rule | Portable lowering |
|---|---|---|
| `steps` | Ordered list; names unique; at most 16 before DCM tighter bounds | Immutable instruction sequence and result slots |
| `let` | Pure typed `{cel:...}` or `{literal:...}`; no guard, no effects | `BIND` from checked expression/literal |
| `call` | Exact DCM capability, static ID; arguments individually bound | Resolved capability ID/version, typed input bindings and result slot |
| `when` | ID of a preceding unconditionally bound Boolean `let` | Guarded call; absent result if skipped |
| `onError` | Only DCM-declared handleable business codes; **terminal** record | Typed terminal business-rejection branch, not retry |
| `return.record` | Exact typed record; always evaluated after successful/skipped steps | Pure terminal return bindings |

**Enum literal refinement:** If a DCM parameter or return field has an enum type, an IR `{literal: ...}` value must be one of its declared cases. For `{cel: ...}` producing a string to an enum field, the compiler MUST prove all possible string values belong to that enum (for example, both branches of a conditional expression are declared enum literals); otherwise reject. Comparing an enum field with an undeclared arbitrary string is forbidden. This requirement is part of the planned DCM overlay and has **not** been implemented by the expression-only spike.

**CEL value and type rules:** The compiler MUST produce a checked AST using CEL-Java **0.14.0**, then independently apply a pinned AST/function/overload allowlist and a DCM nominal/unit overlay. The tested Node CEL-ES **0.6.1** consumes the resulting canonical `CheckedExpr` protobuf. CEL-ES does **not** use the checked type/reference maps to plan calls, so the compiler must reject unverified overloads/semantics and the integration must run cross-language conformance tests whenever the admitted subset or pinned libraries change. No arbitrary user functions or host adapters are callable from CEL. Java int64 / Node bigint, protobuf field presence, exact durations and typed records have explicit adapter-boundary rules in the [compiled artifact/runner specification](compiler-artifact-and-runners-v0.md).

**Runtime resource safety is unresolved:** A finite declared list length and AST size do not automatically prove an enforceable CPU bound for nested macros. Until proven, the compiler/runtime must refuse expressions beyond a conservative verified profile and should isolate expensive synchronous evaluation from the Node application event loop where required.

## 12. Rejection examples and diagnostic requirements

These are **illustrative**, not a full negative fixture set:

```yaml
# Rejected: unknown dynamically resolved operation
- id: bad
  call: "${input.operationName}"
  args: {}

# Rejected: caller/tenant and replay key are host-bound, never agent arguments
- id: unsafe
  call: tasks.create
  args:
    tenantId: {cel: input.tenantId}
    operationKey: {literal: random-key}

# Rejected: unsupported persistent workflow construct
- id: later
  wait: {days: 7}
```

Compiler rejection MUST be fail-closed and deterministic. Diagnostics have `{code, path, stepId?, expected?, actual?, remediation}` but MUST NOT expose secrets or unredacted customer data. Important classes: `PROFILE_UNSUPPORTED`, `CAPABILITY_UNKNOWN`, `ENTRY_GRANT_DENIED`, `HOST_BINDING_OVERRIDE`, `TYPE_MISMATCH`, `NOMINAL_MISMATCH`, `UNIT_MISMATCH`, `DATAFLOW_UNAVAILABLE`, `EFFECT_DENIED`, `CONTRACT_UNCERTIFIED`, `BUDGET_EXCEEDED`, `ERROR_NOT_HANDLEABLE`, `CEL_POLICY_DENIED` and `PIN_MISMATCH`.

**Versioning:** `language: keir/v0` identifies this authoring format. The independent compiled artifact format is versioned separately and is **not** user-authorable. Major semantic/profile changes require recompile and explicit reapproval. The POC may restrict activation to **one mutation per invocation** even though the structural syntax can represent sequential static calls; multi-write crash recovery is deferred.

## 13. Host hooks and tenant capabilities (v0.4 integration clarification)

The step language is unchanged: a `call` to a host-approved native capability and a `call` to a host-approved tenant-backed endpoint have the **same grammar**, capability resolution and effect checks. Untrusted source IR cannot register URLs, credentials, tenant identities or capabilities. The compiler receives the trusted tenant catalog separately, binds its digest into the compiled artifact, and rejects unavailable names.

A pre-operation hook can use the same `return.record` mechanism to produce a typed decision (`ALLOW`, `REJECT`) or permitted patch when its **host-defined entrypoint output** declares those variants. A post-commit hook does not thereby gain the right to reject an already-completed operation. Timeout/failure behavior is set by the host's hook policy; the IR cannot select it. The host is responsible for applying returned decisions, validating patches and enforcing no bypass.

This version of the IR **does not introduce new instructions**, an agent subsystem or a dynamic external-call operator. The existing example DCM does not yet publish `BeforeTaskCreate`; refer to [hook and tenant-capability contract](hooks-and-tenant-capabilities.md) for the proposed host integration and to [Domain Capability Model](domain-model-v0.md) for the shared/tenant catalog boundary.
