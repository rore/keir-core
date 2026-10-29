# Keir compiler, portable artifact, and runners v0

**Package baseline:** v0.4 (public working draft, 28 October 2026). The language remains at semantic profile v0; the pre-release source identifier is now `keir/v0`. No new execution instruction is introduced.

**Technical architecture and draft execution contract | 28 October 2026 | v0.4**

**Status:** Target design for a first implementation. The Java-to-Node **CEL expression** path has been tested with real libraries, but the full IR compiler, portable instruction-plan format, embedded business runner, activation signature protocol, and end-to-end simulation are **not implemented**. Terms such as MUST below define intended v0 behavior, not achieved certification.

## 1. Why IR and compiled artifact are separate

We intentionally maintain two representations because Java and Node are real execution targets:

- **Extension IR (agent-authored):** Small YAML/JSON source with `let`, `call`, `when`, `onError`, `return`, CEL source and DCM references. It is optimized for reliable authoring, diagnostics, customer review, and constrained composition. It is **untrusted**.
- **Compiled artifact (compiler-authored):** A versioned, immutable, typed instruction program whose capabilities have been resolved, effects checked, bindings assigned, and CEL expressions converted to standard checked protobuf. It is designed to be efficiently prepared and executed in **different languages**. Only a trusted compiler can create a valid compiled program, and only a separately approved one can be activated.

A JSON plan does not mean JSON parsing or CEL compilation per request. JSON is an *artifact serialization choice*. A runner deserializes/verifies/prepares once, then executes cached instructions repeatedly. A separate source-code generator may be a later optimization but is not needed for v0.

## 2. Authoring, compilation, simulation, production

```text
Customer web UI
   |  natural-language change request
   v
Node agent loop (part of host SaaS)
   |  generates/revises untrusted custom IR and test proposals
   v
Local Java compiler CLI  [only when authoring/revising]
   |  strict parse + DCM + CEL-Java checking + effects/types/budgets
   |-- deterministic diagnostics -> bounded agent repair loop
   `-- portable compiled artifact, NOT automatically approved
                         |
                         v
             Node runner + fake adapters
                         |
                         v
            user-reviewed scenarios/trace
                         |
            host approval + signing + activation
                         |
                         v
              embedded Node production runner
              prepared artifact + real adapters
```

**Deployment:** One Node.js/TypeScript web app and its data store in one deployment. The Java CLI/JAR and Java runtime are bundled in the same container or installation and invoked locally from Node at authoring time. There is **no compiler HTTP service**, queue, or separate workflow server. Java is **not** needed during ordinary extension invocation. Future hosted compiler packaging is possible without changing the IR or artifact contract.

## 3. Authoritative Java compilation pipeline

The compiler's input is untrusted Keir IR plus the **authoritative shared DCM and optional tenant-scoped effective capability catalog selected by the host**, never a customer-supplied catalog, endpoint registration request or certification flag.

1. **Strict parse:** Reject duplicate YAML keys, aliases/merge behavior that changes meaning, unknown fields, unbounded payloads and unsupported constructs. Apply the draft [closed IR schema](schemas/extension-ir-v0.schema.json).
2. **Capability and hook binding:** Resolve domain/entrypoint/capability IDs and versions against the exact shared DCM plus host-approved tenant catalog, if any. Pin the effective digest and **hook result-consumer/failure contract** before type/effect validation. Resolve each allowed parameter's binding origin (`agent` vs `host`), field visibility, result/error types and capability contracts.
3. **CEL compile/check:** Use **CEL-Java 0.14.0** with a fresh, statically typed environment for `input` and earlier definitely-available step results. Convert approved expression ASTs to `dev.cel.expr.CheckedExpr`, with checked type/reference maps, and pin the required protobuf descriptors.
4. **Static analysis:** Enforce type/nominal/unit compatibility, optional-result availability, no future references, CEL AST/function/overload allowlist, effect upper bounds, step/call/write ceilings, typed terminal business errors and DCM contract obligations. Compilation MUST NOT claim a real host guarantee is certified merely because it is declared.
5. **Lowering:** Emit fixed operation nodes, slot/type descriptors, checked expression IDs, capability/adapter bindings, host-only parameter slots, resource limits, typed error/return transitions, compiler policy/environment fingerprint and source/DCM digests.
6. **Diagnostics:** Failure emits no runnable artifact. Codes/paths are stable and safe for feeding back to an AI agent: `CAPABILITY_UNKNOWN`, `ENTRY_GRANT_DENIED`, `NOMINAL_MISMATCH`, `DATAFLOW_UNAVAILABLE`, `EFFECT_DENIED`, `CEL_POLICY_DENIED`, `CONTRACT_UNCERTIFIED`, etc.

**Compilation is not activation.** Host review, real contract certification, policy/version compatibility, scenario evidence and an approval record are separate gates.

## 4. Portable artifact v0: proposed layout

```text
compiled-extension/
  plan.json             # resolved, versioned instructions + typed bindings
  types.desc            # pinned protobuf FileDescriptorSet for CEL records
  checked/
    guard-1.pb          # canonical dev.cel.expr.CheckedExpr protobuf
    due-date.pb
    return-status.pb
  manifest.json         # content digests, source/DCM/profile/runner/version pins
  manifest.sig          # host signature added only after approval
```

The **wire schema is not finalized**. The existing real CEL harness demonstrates the `CheckedExpr` and descriptor part, not the full `plan.json` instruction format. A minimal *illustrative* plan node sequence could be:

```json
{
  "format": "keir-plan/v0-draft",
  "entrypoint": "TaskCompleted",
  "nodes": [
    {"op": "CALL", "id": "source", "capability": "tasks.get@1.0.0", "args": {"taskId": {"expr": "input-task-id"}}, "resultSlot": 0},
    {"op": "CALL", "id": "existing", "capability": "tasks.search@1.0.0", "resultSlot": 1},
    {"op": "BIND", "id": "eligible", "checkedExpr": "checked/guard-1.pb", "resultSlot": 2},
    {"op": "CALL_IF", "id": "created", "guardSlot": 2, "capability": "tasks.create@1.0.0", "effects": ["task.create"]},
    {"op": "RETURN", "valueType": "FollowupResult"}
  ]
}
```

This sketch **omits complete resolved argument bindings and terminal error branches** and is not an executable plan. The real implementation MUST include every argument/return binding, optional-value typing, business-error transitions and quotas. Compiled nodes are fixed and acyclic; the runner MUST NOT read or reinterpret the original agent IR.

The artifact must bind at least: `artifactFormatVersion`, `sourceIRDigest`, `dcmDigest`, `effectiveCatalogDigest` (if a tenant overlay is present), `tenantScopeBinding` (where relevant), `entrypoint`, `hookConsumerContractDigest`, `compilerVersion`, `celPolicyVersion`, `typeEnvironmentDigest`, `protobufDescriptorsDigest`, all checked expression digests, `capabilityIdsAndVersions`, `requiredHostContracts`, `allowedEffects`, `limits`, `minimumRunnerFeatureSet`, `planDigest`, and the separate approval record/revision. Compiler and runner versions must be pinned conservatively; no silent upgrades or fallback reinterpretation.

## 5. What the CEL interoperability spike actually proved

A successful real-library CI run on 28 October 2026, [GitHub Actions #40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668), commit `3023f3e14199cb555f6ba247991438ba0358e5ea` on the [temporary branch](https://github.com/rore/bear-account-demo/tree/spike/cel-java-node-interop-20261028), used **Java 21, Node 22, CEL-Java 0.14.0 and CEL-ES 0.6.1**.

- **25/25** semantic fixtures and **46/46** harness checks passed, with no reported semantic mismatch.
- **19 expressions** were compiled in the main suite; actual binary CEL `CheckedExpr` protobufs were exported, loaded and evaluated by Node.
- Tested behaviors included Boolean/arithmetic/conditional logic, typed protobuf field selection, Unicode, signed int64 beyond JS safe number range, collection macros (`exists`, `all`, `filter`, `map`), timestamps/durations, short-circuiting and selected error/compile-rejection cases.
- Expression-only CI microbenchmarks: roughly **12.8 ms Java compilation/checking per expression** (including harness overhead), **10 ms Node preparation for 19 expressions**, **2.9 microseconds** warm evaluation for *one simple Boolean expression*, **5,160 bytes** of checked protobuf across the 19 expressions, and **333 KB** additional Node heap in the measured process. These are not application-level latency, scaling, or production-grade resource-isolation figures.

**Crucial implementation detail:** CEL-ES `plan()` accepts `CheckedExpr` but uses its `.expr` AST, **not** the checked Java type and reference maps, when planning. CEL-ES selects function implementations using its own environment. The version 0.6.1 checker is incomplete and not a publicly exported function. Therefore the **authoritative Java checker** must apply a strict admitted AST/function/overload policy, and Java/Node conformance fixtures are mandatory for every new admitted construct or dependency change. Treat checked protobuf as verified *only after* checking provenance, feature support, and compatible local environment; it is not a general proof of semantic parity for all CEL expressions.

**Not proven by this CI:** DCM nominal/unit overlay, full IR compilation, complete runner, correct host adapters, enforced CPU/instruction gas for arbitrary nested CEL macros, approval UX, or customer intent correctness. The result is **GO for the constrained Node demo**, not GO for untrusted production extensions.

## 6. Node runner phases and trusted boundary

**`loadApprovedArtifact()`**, once per artifact/version:

1. Verify the artifact is from a host-controlled trust root (e.g. an Ed25519-signed, immutable approval manifest). The public verification key is configured **outside** the artifact and never chosen by the agent. Verify all content digests, sizes and signatures before parsing nested files.
2. Match exact artifact format, domain/entrypoint/shared-DCM digest, **effective tenant catalog and host result-consumer binding**, compiler/CEL policy version, descriptors, capability versions, required host contracts and registered adapter versions. Never allow a cross-tenant binding by name alone. Reject unsupported or ambiguous compatibility.
3. Validate the binary instruction envelope, approved node/AST opcodes and resource limits. This is artifact **integrity and compatibility verification**, not full reanalysis of the original IR. The Node runner MUST NOT trust arbitrary unsigned `plan.json` files.
4. Build CEL-ES type/descriptor environment from **approved pinned types**, prepare the checked expressions with `plan()` once, and resolve allowed capability adapters once. Cache immutable `PreparedExtension` by artifact+environment fingerprint.

**`execute(PreparedExtension, InvocationContext)`**, on each host event/request:

1. The host validates current tenant/actor/subject scope and the entrypoint's event admission, independently of artifact approval.
2. The runner validates business input types, uses immutable result slots and guarded sequential instructions, and checks each capability's quota and effect authorization.
3. Trusted adapters inject host-only fields (actor, tenant, stable replay identity) and enforce domain authorization, result bounds, commit preconditions, per-call timeouts and error classification. For registered external endpoints, the **host adapter**, not Keir, performs the network call with host-managed destination, credentials and egress controls. The agent never sees credentials or gains authority via CEL values.
4. A confirmed success advances; a declared `BUSINESS_REJECTION` terminates or reaches a terminal `onError` result while preserving rejection classification; a `TRANSIENT_FAILURE` stops; `UNKNOWN_COMMIT` stops as `UNCERTAIN` with **no automatic retry or success claim**.
5. The runner records redacted ordered calls, guards, effect intent, confirmed/uncertain effects and termination classification; the same trace model applies to simulation. The **host hook consumer** interprets the typed result and applies `ALLOW`, `REJECT`, approved `PATCH` or reaction semantics with its own failure policy. The runner does not choose how to continue a paused business operation.

**Resource safety:** For every call, use a host-approved deadline no longer than the remaining invocation/hook budget, and require the adapter to enforce network timeouts. A remote mutation that times out must remain `UNKNOWN_COMMIT` if an effect may have occurred; it cannot be retried blindly. Count total calls/steps, result bytes/items, strings, AST/macro structural costs and elapsed time. A plain timeout callback cannot preempt synchronous JavaScript execution. CEL-ES 0.6.1 **hard CPU/instruction metering is unverified**. To claim a strict wall deadline, use isolation that can actually be terminated or admit only an analyzably bounded expression subset. Fail closed where a bound cannot be guaranteed.

## 7. Multi-language runners and performance

Node is the first runner. A later Java runner should consume the **same compiled artifact and conformance fixtures**, with identical input/output values, branch behavior, operation ordering and error classifications. A Java runner must not silently reinterpret the Java authoring IR either. Common numeric/protobuf semantics are part of the wire contract: exact signed int64 (Java long/Node bigint), UTC instants with required precision, explicit field presence, bounded lists, no implicit money/points coercion and no locale/timezone-dependent transformations unless explicitly specified.

Preparation is not performed on the business request path. A small synchronous plan interpreter is expected to be adequate for the sample read/guard/create flow; benchmark real adapter latency separately from no-op adapter interpreter overhead. Generating Java/Node source or bytecode is a deferred optimization and must pass the same conformance and simulation tests before replacing the interpreter.

## 8. Minimal Java CLI / Node host interface

The compiler is reusable as a Java library, exposed initially as a CLI. Example invocation (API not implemented):

```bash
java -jar keir-compiler.jar compile \
  --input candidate-extension.yaml \
  --trusted-domain-ref demo.tasks@0.1.0 \
  --output-dir compiled-extension/
```

A Node backend uses `child_process.execFile` or `spawn` **without a shell**, with bounded input/output size, timeout and concurrency; the DCM reference is selected and resolved from trusted host configuration, not from untrusted agent input. The compiler returns machine-readable diagnostics for a bounded agent repair loop, or a newly compiled unsigned artifact. Node then simulates and requests customer approval. On activation the host records the immutable approval evidence and signs the content manifest.

**The core framework does not embed an LLM or manage a web conversation.** The demo's Node app owns the agent loop, storage, UI, test review and actual adapters.

## 9. Compatibility, versioning and explicit exclusions

A compiled artifact is invalidated or requires formal compatibility review when types/fields, grants/effects, required contracts, host-only parameter bindings, function-overload policy, CEL engine version, replay/uncertain-outcome semantics or entrypoint result consumer changes. Semantic widening is not automatically backward-compatible. Old active versions may remain pinned only if the host still supports and certifies their exact bindings; otherwise disable and recompile/reapprove.

v0 deliberately excludes agent-authored scripts, arbitrary HTTP/SQL, dynamically selected operations, executable source generation, user-defined CEL functions, unbounded loops, timers, `wait`/`resume`, workflow compensation, automatic retries and distributed multi-operation transactions. The first demo additionally imposes **at most one mutation per invocation**. Anything that cannot be guaranteed by a host contract must be rejected, not hand-waved into the compiler.

## 10. Agent and external-capability independence (v0.4 clarification)

The compiler and runner are usable through direct APIs or CLI with manually authored IR. Agent prompting, endpoint registration, tenant credential storage, and approval UI are **hosting-product features**, not Keir runtime components. An optional Keir agent skill, CLI/MCP tool wrapper, and example SaaS agent loop may help with discovery/authoring but must not be prerequisites.

A tenant endpoint is invoked through a registered capability adapter, never by a URL compiled from IR. The compiler must freeze a tenant-specific **effective capability binding** and hook contract; runtime host authorization and policy remain current and must not be inferred from a stale signature. Endpoint proposal by an AI author is a host-conditional workflow, not a language operation. See [Hooks and tenant capabilities](hooks-and-tenant-capabilities.md).
