# Keir decisions, evidence, and remaining gates

**Architecture v0.4 | 28 October 2026**

This register distinguishes **selected directions**, **tested results**, **proposals**, and **unverified obligations**. Nothing here represents full-framework or production deployment certification.

## Decision log

| ID | Decision | Status / rationale |
|---|---|---|
| D01 | Human/agent authors a **closed structural IR**, not arbitrary source | Selected v0. Generic composition of trusted capabilities is the value, not a fixed-rule expression engine. |
| D02 | Domain owner curates a typed **Domain Capability Model** with effects, grants, host-only bindings and real contracts | Selected boundary. API schema import alone grants nothing. |
| D03 | **Custom IR + CEL** rather than restricted Open Workflow 1.0.3 | Selected v0 based on simpler immutable dataflow, smaller semantic surface and terminal business-error handling; Open Workflow syntax was evaluated and retained only as background. |
| D04 | Keep **two formats**: human/agent IR and compiler-generated portable executable artifact | Selected for one authoritative compiler and multiple lighter embedded runners. A second source-language compiler in every runtime is not wanted. |
| D05 | Initially use **Java compiler CLI** and a **Node/TypeScript runner** | Selected for the first POC. Java is bundled only for online authoring; normal task execution is Node-only. |
| D06 | **One Node SaaS deployment**, including web agent loop and local Java CLI subprocess; **no extra compiler service** | Demo deployment constraint, not a framework-wide restriction. |
| D07 | CEL-Java **0.14.0** checked protobuf -> CEL-ES **0.6.1** Node evaluator for the **tested expression subset** | **Tested in real CI; GO for POC**. The format includes checked protobuf and pinned protobuf descriptors. |
| D08 | Signed immutable activation binds exact compiled artifact, DCM, policies, reviewed scenarios and user authorization | Selected trust design, **not yet implemented**. The agent cannot approve itself. |
| D09 | Simulation and production execute the **same compiled artifact and same language-specific interpreter** with fake vs real adapters | Selected testing principle, **not yet implemented end-to-end**. |
| D10 | Synchronous bounded invocation; no durable wait/resume, bulk effectful loops, compensation or automatic retries | Selected scope. First demo permits multiple reads and **at most one write per invocation**. |
| D11 | Compiled expression is prepared once on artifact load; no per-request JSON parsing/CEL type checking | Selected hot-path design; expression-only preparation and warm evaluation have preliminary CI measurements. |
| D12 | **Public task-management SaaS** is the demo and dogfooding host | Selected fictional domain; basic task app must be useful without the extension framework. |
| D13 | Optional future Java runner consumes the **same artifact and conformance suite** | Target, not implemented. |
| D14 | **Build the minimal v0 instruction interpreter; reuse surrounding open-source libraries** | Provisional engineering choice, not a completed code-size or performance comparison. CEL-Java, CEL-ES, Protobuf and existing parsers/cryptography are reused; no broad workflow engine in v0. |
| D15 | Revisit embedded workflow engines only for **proven additional execution requirements or measurable implementation savings** | Triggers include durable waits, parallelism, substantial branching/recovery or a candidate engine preserving our exact semantics with less code. |
| D16 | **Keir** is the public project name; **Controlled Extension IR** is its description, not a forced acronym | Selected naming. Draft source language uses `keir/v0` (previous `business-extension/v0` was pre-release only). |
| D17 | **Agent-independent core, agent-first optional SaaS authoring** | Selected architectural boundary. AI helps business users clarify requirements, propose scenarios and generate IR; host owns the agent and customer UX. Manual IR authoring remains valid. |
| D18 | **Host-owned lifecycle hooks and typed result-consumer contracts** | Selected direction: pre-operation allow/reject/patch, post-commit reaction, response transformation and on-demand evaluation. The extended DCM shape is a **proposal**, not implemented in the existing fixture. |
| D19 | **Shared DCM + optional host-approved tenant capability catalog** | Selected direction: compiled effective catalog is tenant-bound and its digest is pinned. The host owns endpoint trust/registration, security, credentials and transport. No raw URL in IR. |
| D20 | **Endpoint-registration proposals are conditional host features** | The core library/IR cannot register capabilities or decide if a proposal is allowed. No external tenant endpoint is required for the first demo. |
| D21 | Add a proposed `BeforeTaskCreate` interception example to the public task-manager POC | Follow-on integration test, **not included** in first `TaskCompleted` DCM fixture. Compare with a simpler CEL-only hook at equal integration cost. |

## Exact CEL interoperability evidence

**Run:** [Successful GitHub Actions #40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668), [temporary branch](https://github.com/rore/bear-account-demo/tree/spike/cel-java-node-interop-20261028), commit `3023f3e14199cb555f6ba247991438ba0358e5ea`. Environment: Ubuntu 24.04, Java 21, Node 22, CEL-Java `dev.cel:cel:0.14.0`, CEL-ES `@bufbuild/cel:0.6.1`, actual published Maven/npm libraries.

**Observed in CI:** Three initial success expressions and selected error/rejection cases; full existing semantic fixture suite **25/25**; **46/46** harness checks; **zero observed mismatches** in the admitted fixtures. The main fixture suite compiled **19 checked CEL expressions** and Node consumed their protobuf ASTs. Exact int64 beyond JS safe number range, Unicode, protobuf fields, collection macros, short circuit, timestamp/duration and selected error classes were exercised. The initial CI failures were harness defects later corrected (protobuf field mapping and macro registration); the final run is successful.

| CI microbenchmark | Observation | Interpretation limit |
|---|---:|---|
| Java compile/check | ~12.8 ms/expression over 19 | Includes harness overhead; not general compiler throughput |
| Checked protobuf size | 5,160 bytes / 19 expressions | Does not include whole artifact or descriptors |
| Node load/prepare | ~10 ms / 19 expressions | One CI run, not a production startup distribution |
| Warm Node evaluation | ~2.9 microseconds for a simple Boolean | Not the latency of complex macros or business calls |
| Additional Node heap | ~333 KB for the measured process | Not a scaling limit for thousands of active extensions |

**One key architectural caveat:** In CEL-ES 0.6.1, `plan(env, CheckedExpr)` extracts the AST and resolves functions from its **own** environment, without using Java-produced `typeMap`/`referenceMap` as runtime semantic authority. Its independent checker is incomplete and not publicly exported. We therefore use **Java as authoritative type checker**, restrict CEL syntax/functions/overloads in the compiler, pin descriptors/runtime versions and maintain real Java/Node conformance fixtures for the selected subset. Do not generalize from 25 fixtures to all CEL overloads.

**Test repository limitation:** The temporary public branch packages the source harness as `harness.tar.xz` and verifies a SHA-256 in CI. This is reproducible at the run level, but a maintained repository should unpack source and fixture files into reviewable version-controlled paths and pin Maven/npm transitive dependencies with suitable lock information.

## Prior investigation, without overclaiming

- **Open Workflow 1.0.3 vs custom IR:** The language-design agent compared complete illustrative programs and selected a custom structural IR. Restricted Open Workflow remains conceptually possible, but its context replacement, flow directives and 1.0.3 error-handling constraints add an adaptation layer. No working OWS runtime is part of the architecture.
- **Expression alternatives:** JsonLogic, JSONata and other options were considered for portability; no evidence justified replacing CEL after the checked-protobuf real-CI test. No custom expression VM is planned.
- **Generic framework vs deployment host:** A Node task-manager application can bundle a Java compiler CLI for online authoring without separately deploying Java as a service. The compiler is absent from normal business-event execution.
- **Build versus reuse:** The v0 instruction set is a short acyclic `Bind`/`Guard`/`CapabilityCall`/`Return` model with typed outcomes, not a durable workflow. Existing embeddable engines were reviewed at a high level; no engine has yet shown a clear net integration advantage over a small interpreter, especially with future Java parity. This judgment is provisional, not benchmarked. Keep using existing foundational libraries. See [architecture, section 6.4](keir-architecture.md#64-build-versus-reuse-decision-for-the-execution-runner).
- **Fictional task-domain examples:** Two follow-up automation variants reuse the same bounded reads, conditional logic and one task-create capability. A read-only focus-list projection is a later test. These are design examples, **not** evidence of customer demand or commercial ROI.

## Still unproven before a real framework demo

1. **Full IR Java compiler:** Strict grammar/schema, checked CEL, DCM semantic resolution, nominal and unit overlay, effect/authorization/dataflow validation and deterministic diagnostics.
2. **Full portable plan wire format:** Resolved step bindings, typed result slots, error/return transitions, versioning, descriptor fingerprints and canonical manifest encoding.
3. **Node runner:** Trusted artifact admission, plan preparation, runtime authorization, complete quota accounting, correct error and effect trace semantics.
4. **Resource limits:** Hard CPU/instruction bounds for CEL macros and a safe execution strategy if arbitrary expression inputs could otherwise block the Node event loop.
5. **Real adapter guarantees:** Atomic follow-up uniqueness and replay under concurrency, result completeness, scoped authorization and ambiguous outcome evidence. Fake success does not certify these.
6. **End-to-end scenario engine and customer approval:** Independent review of agent-proposed expected outcomes, real artifact signing, activation, audit, disable and version rollback.
7. **Java runner conformance:** Only after the Node POC; share the exact compiled artifact and scenario suite.

## Recommended immediate implementation sequence

**Do not run another generic expression-language survey.** Preserve the now-passing CEL CI tests. Implement a narrow vertical slice: handwritten fictional task DCM -> one complete IR program -> Java compiler emits a portable plan + checked protobuf -> Node simulates the exact plan with fake adapters -> Node executes it through a real `tasks.create` adapter -> customer reviews/approves/activates an immutable revision.

If the full IR compiler cannot enforce a claimed contract, make that contract **an explicit rejection or host prerequisite**, rather than expanding the language or inventing a general workflow engine. Keep the demo single-deployment throughout.

**v0.4 note:** Keir naming, agent/core separation, hook result-consumer contracts and optional tenant-scoped catalogs do not change the passing CEL interoperability evidence. No implementation of hooks, tenant endpoint registration or cross-tenant artifact enforcement has yet been tested.
