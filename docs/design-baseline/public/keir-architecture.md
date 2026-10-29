# Keir: Controlled Extension IR

**Product Definition & Architecture | Working baseline v0.4 | 28 October 2026**  
**Status:** Agreed direction for an experimental public framework, not a completed implementation or security certification.  
**Publication scope:** Domain-independent architecture and fictional task-management examples only. This document does not contain any proprietary customer case studies or non-public product interfaces.

> **Definition.** Keir is an embeddable, agent-independent language, compiler and runtime for constrained application extensions. Humans or AI agents can compose host-approved, typed capabilities in a closed Extension IR rather than generating arbitrary executable code. A deterministic compiler validates that composition and emits a portable artifact; a trusted host runner executes it with real or simulated adapters. Hosts may add AI authoring to make this capability accessible to business users. **Keir** is the product name; **Controlled Extension IR** describes its technical purpose rather than serving as a forced acronym.

## 1. Decision snapshot

- **Core thesis:** Untrusted authors, especially AI agents, create extensions inside a closed, host-defined capability environment. Validation rejects unauthorized operations and effects; it does **not** prove business intent or compensate for incorrect host contracts.
- **Core versus experience:** Keir has no LLM or agent dependency. A hosting product may provide an **agent-first business-user experience** for clarifying requirements, generating IR, proposing tests and explaining simulations; manual/programmatic authoring remains supported.
- **Hook model:** Hosts define lifecycle entrypoints and result consumers, including pre-operation allow/reject, controlled input/output transformations, post-commit reactions and explicit on-demand requests. The host applies results and owns failure policy.
- **Catalog layering:** A domain-wide DCM may be supplemented by host-approved tenant-specific capabilities, including optional registered external endpoints; compilation uses a tenant-scoped **effective catalog**, never agent-supplied grants.
- **External integration ownership:** The host owns endpoint registration, destination trust, credentials, tenant security and transport. Keir enforces admitted typed calls, bounds, outcomes and capability contracts; no arbitrary URL execution.
- **Selected language design:** Small custom structural IR + CEL for v0. Restricted Open Workflow 1.0.3 remains a documented alternative; the custom design is **not yet implemented**.
- **Target execution architecture:** One authoritative compiler lowers IR to a **portable compiled artifact**, consumed by embedded Node and eventually Java runners.
- **Verified expression stack for the POC:** CEL-Java **0.14.0** produces standard CEL `CheckedExpr` protobuf; CEL-ES **0.6.1** loads and evaluates the tested subset in Node 22. Java 21 is used at authoring time only. This is **GO for POC**, not full-CEL or production-security certification.
- **Host-owned agent-first demo experience (not required by Keir):** Customer web conversation and backend agent loop for generation, diagnostics/repair, test proposals, simulation, review and activation.
- **POC deployment constraint:** One Node web application with a bundled Java compiler CLI invoked locally during authoring. **No separate compiler service.**
- **Test model:** The **exact compiled artifact** runs in production and simulation through the same language-specific runner, with real versus fake adapters.
- **Scope:** Short, bounded, synchronous invocations; no durable waits, effectful loops, implicit retries or distributed transactions.
- **Reuse decision:** Implement a minimal runner for the small v0 instruction set; reuse existing CEL, Protobuf, parsing and schema libraries. Adopting a general workflow engine is deferred unless it demonstrably reduces total integration/semantic complexity. This is provisional, not benchmarked.

**Evidence boundary (updated):** The language-design agent produced **draft specifications** and fixture-only checks. A subsequent published-binary interoperability CI run now **passed 25/25 semantic fixtures and 46/46 harness checks** for Java-produced checked CEL evaluated by Node. It was an **expression-only harness**, not a complete Keir IR compiler, capability runner, simulator, DCM nominal/unit checker, or safe resource-metering proof. See [technical evidence and decisions](decisions-and-evidence.md).

## 2. Why this product exists

The starting problem is that AI agents can generate new application behavior as Java or JavaScript, but a SaaS host cannot responsibly execute arbitrary agent-generated code as a tenant customization. Traditional drag-and-drop and rule-builder tools reduce programming requirements, yet still demand technical understanding of conditions, data sources, and workflows from business users.

**Keir changes what can be generated.** The host exposes a **walled garden**: a closed language, selected capabilities, typed invocation inputs, allowed effects, bounded computations, and explicitly controlled outputs. Extensions are rejected if they attempt behavior outside these boundaries. This provides **enforceable authority closure**, not a blanket guarantee of business correctness, safety of every permitted operation, or suitability for an individual customer's intent.

A hosting product can place an AI agent above Keir so that a business user describes the outcome, clarifies ambiguous requirements, reviews proposed examples and effect traces, and approves the exact compiled artifact. That conversational layer is a powerful **usage model**, but is neither part of Keir's runtime nor required to author an extension.

The bet is that a host can expose a **curated capability vocabulary** and let a human or agent choose reads, calculations, conditions, dependencies and permitted effects. Keir supplies the closed programming model and enforcement machinery; the host supplies real domain semantics, hook placement, activation policy, current authorization and actual capability implementations.

This is distinct from a rules engine. If every customization only changes the predicate for an existing fixed operation, a conventional rule or CEL expression may be simpler. The framework earns its complexity when different customer requirements can select and connect **different approved operations** without new vendor-written algorithms per customer.

**Not goals:** Arbitrary generated source code, unrestricted HTTP/SQL, built-in endpoint onboarding, a new IAM/tenancy layer, an LLM/agent framework, automatic creation of missing business hooks, guaranteed cross-service transactions, a durable workflow engine, generated user interfaces, or automatic proof of customer intent.

**Adoption principle:** Demonstrating the framework as a public library does not automatically prove that every SaaS product needs it. Each host should compare capability composition with simpler native configuration, fixed-hook expressions, and ordinary integration code.

## 3. Logical architecture and ownership

```text
Hosting SaaS defines domain-wide DCM and hook/result contracts
                  +
Host-approved tenant capability catalog (optional)
                  |
                  v
       Effective capability catalog
                  |
Manual author OR optional host-owned AI agent --> Extension IR
                  |
                  v
  Java compiler (trusted authoring-time tool)
          diagnostics OR compiled artifact
                  |
        host tests + customer approval
                  |
         Node runner (Java later)
          /                  \
   fake adapters          real host adapters
   (simulation)            (production)
                  |
    Host applies hook decision / patch / reaction
```

**Four enduring stages:**

1. **Capability publication:** The host selects native operations, lifecycle hooks, typed result consumers and real guarantees in a shared DCM. It may augment them with separately approved tenant capabilities. The compiler sees an authenticated, versioned effective catalog; importing an API or having an agent propose an endpoint grants nothing.
2. **Authoring:** A developer or customer may supply IR directly. Optionally, the host integrates an AI agent that conducts a business-user conversation, discovers admitted capabilities and proposes IR/scenarios. The agent is not trusted to create grants, register endpoints, certify contracts or authorize activation.
3. **Compilation and approval:** The trusted compiler resolves the IR against the authoritative DCM, checks safety and semantics, and produces an immutable, portable executable artifact or structured diagnostics. The host coordinates simulation, customer review and activation of the exact artifact.
4. **Execution:** The app's embedded runner loads the authorized artifact, prepares it once, and invokes host-owned capabilities when an approved business entrypoint fires. The same runner executes simulated scenarios with fake adapters.

**Ownership boundary:** Keir does not contain an LLM, conversation loop, endpoint registry, HTTP connector trust policy or SaaS activation UI. Those are host responsibilities. Keir can provide machine-readable DCM discovery, agent-friendly compiler diagnostics, simulation APIs, a sample agent skill and a thin optional CLI/MCP wrapper. The sample Node SaaS demonstrates the host integration without making it mandatory for library consumers.

## 4. Domain Capability Model (DCM)

The shared DCM declares the SaaS domain contract; a hosting application may overlay **tenant-specific approved capabilities** to form an effective catalog. The compiler accepts both only through a **trusted host context**. The compiler and runner enforce declared capability authority; endpoint trust and all real host guarantees remain the hosting application's responsibility.

The DCM and effective catalog are authority boundaries, not merely imported API documentation. They contain:

- **Identity and types:** versioned domain/capability IDs, selected source digests, named records, nominal IDs (e.g. `TaskId` versus `UserId`), enums, bounded lists, nullable/optional fields, units and exact-value representations.
- **Capabilities:** statically named calls, typed agent-supplied arguments and host-only arguments, output projection, declared effects, possible typed errors, explicit replay/uncertain-outcome behavior and resource bounds.
- **Entrypoints and result consumers:** host-owned invocation phases (pre-operation decision/transform, post-commit reaction, response transformation, on-demand), typed input/output, allowed result shapes (`ALLOW`, `REJECT`, bounded approved `PATCH`, view), enforcement placement, total deadlines, failure policy and call/effect grants. An AsyncAPI message declaration is not automatically an active listener.
- **Trusted contracts:** scope/authorization, result completeness/freshness, atomic uniqueness, compare-and-set, domain invariants, idempotency/reconciliation. **Declarations are not evidence**: the application must certify or test the adapter implementations and enforce current authorization on every call.
- **Compatibility:** activation pins the semantic shared DCM, the **effective tenant-scoped catalog**, hook/result-consumer contract and adapter/contract versions. Widening fields, changing effects or retry behavior may require recompilation and reapproval.

For the public demo, the task host can publish `getTask`, `listTasks`, `getProject`, `createTask`, `updateTask` and narrowly selected entrypoints. The initial POC only needs a **small subset**. The host binds tenant, actor, authorization, invocation identity and idempotency information; the agent never supplies these through the IR or a CEL variable.

OpenAPI/AsyncAPI importing can be added after a manually curated DCM works. Schema imports alone do not prove business semantics, authorization, freshness, atomicity or concurrency guarantees.

### 4.1 Host-defined synchronous hooks and consumers

Keir's entrypoint is not just an event name. The host defines **where** execution occurs and **how** its typed result is applied:

- `PRE_OPERATION`: Before a host-controlled operation. An approved `ALLOW` lets the original flow continue; `REJECT` blocks it through an allowed business error; a permitted `PATCH` is applied only after host validation. Timeout/failure fallback is a *host policy*, not IR-authority. The host must prevent bypass and re-check mutable invariants at commit.
- `POST_COMMIT`: Reacts to an already-completed operation. It cannot retroactively reject or roll back the original action. The host chooses delivery/observability and guarantees for follow-on effects.
- `RESPONSE_TRANSFORM`: Before returning a response, applies a **bounded** permitted filter, projection or ordering over trusted complete-enough inputs. It cannot rewrite protected settlement/state or claim global completeness from a truncated page.
- `ON_DEMAND`: Returns a typed decision/view for an explicit host request, with no implied workflow lifecycle.

A host-controlled deadline covers the entire invocation and all permitted calls. In particular, external network latency must not silently extend a business request or hold a database transaction open across an untrusted call. [Hook and tenant-capability design](hooks-and-tenant-capabilities.md) details the proposed contract; the existing `TaskCompleted` fixture is still the normative first-slice example.

### 4.2 Shared domain and tenant-specific capabilities

A host can register tenant-scoped typed operations (such as a customer's decision/data endpoint) alongside the shared domain DCM. The **host**, not Keir or its authoring agent, verifies endpoint ownership and destination, stores credentials, decides which data may leave the SaaS, and owns the actual transport adapter. The compiler treats an approved tenant endpoint as an ordinary, statically named typed capability. The IR **never contains an arbitrary URL or credential**.

Compilation is tenant-bound when tenant capabilities are used: the trusted compile context selects the tenant, supplies the approved tenant catalog and authorizes the effective grants. The artifact pins `dcmDigest` plus `effectiveCatalogDigest` and a tenant-scoped activation binding, so one tenant's program cannot be reused with another tenant's endpoint merely because capability names match. An agent can *propose* an endpoint registration only when the host explicitly provides that authoring feature; proposal is not registration or approval. Contracts include approved timeouts, failure/uncertainty classifications and bounds, while endpoint security enforcement stays with the host.

## 5. Agent-facing language: selected custom IR v0

The language-design review compared real draft programs in restricted Open Workflow 1.0.3 and a small structural IR. **The chosen v0 language is Keir’s custom structural IR with CEL**, not the Open Workflow runtime or the restricted Open Workflow profile.

A v0 program (`language: keir/v0`) has a pinned domain/entrypoint, a short ordered step list and a typed terminal return. The same syntax can call host-approved native or tenant capabilities without special HTTP syntax. Steps are either pure immutable `let` bindings or statically named capability `call`s; calls may use a preceding Boolean `let` as a guard (`when`). Arguments are typed literals or checked CEL expressions. Declared business rejections may use a terminal `onError` return. The language has no arbitrary source code, user functions, generic `switch`, dynamic calls, mutable context, effectful iteration, `wait`, `listen`, timers, scripts, retry policies or compensation.

**Illustrative structural fragment (the complete program and grammar are defined in the companion spec):**

```yaml
language: keir/v0
entrypoint: TaskCompleted
steps:
  - id: source
    call: tasks.get
    args:
      taskId: {cel: 'input.taskId'}
  - id: existing
    call: tasks.search
    args:
      projectId: {cel: 'source.projectId'}
      originSourceTaskId: {cel: 'source.id'}
      originKind: {literal: payment-followup}
      maxResults: {literal: 5}
  - id: eligible
    let: {cel: 'source.tags.exists(t, t == "invoice-sent") && size(existing.items) == 0'}
```

A complete program also pins the DCM, supplies a correctly typed `return`, and may conditionally invoke the approved `tasks.create` with fully typed request bindings. The fragment illustrates **agent-selected composition** without inventing a generic `createFollowup` business API. Full normative examples and schemas are in the design package's `examples/` folder and the companion [IR specification](keir-language-v0.md). Conditional results must be checked for presence before use; a skipped write cannot be reported as confirmed.

**Complexity line:** v0 may statically order multiple calls and pure bounded CEL projections, but it has **no loop over side effects** or long-running workflow state. The *first task-manager POC* should allow multiple reads and at most **one mutation per invocation**, even if later versions of the language allow carefully certified sequences. Adding `WAIT/RESUME` for delayed business actions is not a v0 goal: the host should invoke a new short-lived extension when the relevant authoritative facts become available.

**The canonical IR definition is in [Keir Extension IR v0](keir-language-v0.md)**, including complete permitted fields, types, `let`/`call`/`when`/`onError` semantics, a full follow-up program, negative examples and constraints. The [DCM specification](domain-model-v0.md) defines the typed host capability contract; the [compiler and runner contract](compiler-artifact-and-runners-v0.md) defines the compiled wire artifact. This document is the architectural source of truth, not a replacement for those normative drafts.

**Why not Open Workflow for v0?** Standard sequencing and catalog calls are useful, but its task context replacement/export model, workflow-oriented branch/error machinery and constrained semantics create extra complexity for our small execution model. Custom DCM binding alone is **not** a decisive objection, and OWS 1.0.3's missing `catch.then` is version-specific. Keep the comparative draft as evidence or a future import adapter, but do not carry two authoring languages in v0. The 27 fixture passes did **not** validate the official OWS schema or run CEL-Java binaries.

## 6. Compiler, portable artifact and runner

**We intentionally retain two representations.** The agent-facing **IR** prioritizes authorability and domain language; the compiled **artifact** is the deterministic contract shared by Node and Java execution engines. A separate artifact now has a purpose: one authoritative static compiler and smaller, language-specific runners.

### 6.1 Compiler responsibility

The Java compiler core (offered as a CLI initially) takes untrusted IR and an **authoritative host-approved DCM**, then:

1. Strictly parses the syntax and rejects duplicate/unknown properties and unsupported language constructs.
2. Resolves statically named capabilities and entrypoint grants against the pinned DCM.
3. Parses and **type-checks** CEL, validates nominal/unit semantics, dataflow, optional values and bounded collections.
4. Computes possible effects, call quotas, budget limits, permitted error handlers and required host contracts. It does not assert that an unverified host contract is true.
5. Lowers valid content into a deterministic, versioned compiled program with structured diagnostics on failure.

The compiler's CEL work may itself be called *compilation*. It is **not** executing expressions or calling domain operations. Compilation/validation occurs at customization time, not for each business request.

### 6.2 Portable compiled artifact

Proposed contents (format **not frozen**): format and policy versions; immutable program/entrypoint/shared-DCM identity; **effectiveCatalogDigest**, tenant-scoped binding when needed, hook-result-consumer contract digest; resolved capability IDs/versions and required contracts; ordered operation nodes, argument/result bindings and guards; portable checked expressions and explicit type metadata; effect/limit envelope; typed termination/error policies; canonical digest and verifiable trusted provenance.

**Expression representation for the POC is now selected:** Each CEL expression is parsed and type-checked with CEL-Java **0.14.0** and stored as canonical `dev.cel.expr.CheckedExpr` binary protobuf (including its type/reference metadata) plus approved source for diagnostics, pinned protobuf descriptors and a declared environment/type/policy fingerprint. CEL-ES **0.6.1** accepts that protobuf for preparation and execution in Node. Real GitHub Actions tests verified parity for the **tested restricted CEL subset**. The Node planner nevertheless uses its own function environment and **does not honor Java's checked type/reference maps for planning**. Therefore a signature plus a checked protobuf is not sufficient for arbitrary expressions: the compiler MUST restrict functions/overloads/AST nodes, the host MUST pin versions and descriptors, and every supported expression feature must pass cross-language conformance. Do not rely on CEL-ES 0.6.1's internal incomplete, non-public checker to revalidate arbitrary expressions at activation.

**Cross-language value rules remain part of the compiled contract:** signed int64 uses protobuf/Java `long`/Node `bigint`, never untagged JavaScript numbers; nominal IDs and unit types need a compiler-side overlay and adapter-boundary checks; null versus missing is explicit; timestamps/durations avoid locale-dependent calendar arithmetic. Resource ceilings still require an enforceable solution, since the spike did not prove hard CPU/instruction metering.

A hash binds bytes but does **not** authenticate who approved them. A production runner must accept only artifacts from a trusted compiler/activation channel (e.g. verified signature or controlled trusted storage), pinned to the host's active DCM and runtime compatibility. Do not treat user-supplied `approved: true` metadata as authorization.

### 6.3 Runner responsibility and performance

A runner **does not revalidate the original IR** or need an agent/Java compiler at business-execution time. It verifies artifact provenance, format/policy/entrypoint/DCM compatibility and adapter availability; prepares CEL/native instruction objects once on activation or load; caches a `PreparedExtension`; and executes it on each approved invocation.

At runtime it still enforces **current** authorization and object scope, input/result types, budgets, allowed effects, trusted parameter injection, adapter error classification and audit traces. A trusted compiled artifact does not make an adapter safe by itself.

Because a typical flow is a handful of conditions and reads plus one command, interpreter dispatch is expected to be small relative to actual database/API operations. **An expression-only CI microbenchmark now exists**: 19 checked protobuf expressions occupied 5,160 bytes; Node prepared the 19 in about 10 ms; a simple Boolean expression evaluated in about 2.9 microseconds warm; additional heap in that run was roughly 333 KB. These are *not* full-runner or real SaaS latency measurements. POC benchmarks should separate authoring compilation, cold load/preparation, warm no-op adapter execution, realistic adapter latency, memory across loaded extensions and timeouts. Generating Java/Node source from the compiled artifact is a potential **later optimization**, not v0: it adds two code generators and executable-equivalence testing.

### 6.4 Build-versus-reuse decision for the execution runner

**Decision for v0 (provisional):** Implement a minimal interpreter for the compiled program's `Bind`, `Guard`, statically resolved `CapabilityCall`, terminal return and typed failure transitions. Do **not** adopt a general-purpose workflow engine for the first implementation. Keep this decision independent of the custom IR choice: the compiler may later target a different engine if the requirements change.

**Rationale:** The approved execution model is synchronous and acyclic: a short sequence of prepared CEL expressions and capability calls, bounded reads, at most one mutation in the first POC, and no durable waits, scheduling, parallel branches or compensating transactions. A larger engine may require adapters, state/context conventions and semantics that are more work to restrict and align across Java/Node than a small interpreter. This is an engineering hypothesis, **not** a measured build-versus-reuse comparison.

**Reuse instead of rewriting:** CEL-Java/CEL-ES for expressions, Protobuf for portable checked expressions and values, established YAML/JSON/schema parsers, cryptography libraries for approval verification, and the host's existing task services/persistence. Reuse generic UI/agent SDKs where helpful without coupling the core framework to an agent provider.

**What we examined:** A restricted Open Workflow language/runtime, and embeddable workflow options including Sequential Workflow Machine, BPMN-oriented engines and other Node-specific workflow libraries. None has yet demonstrated a better fit for the **exact** closed, portable v0 instruction set. No existing engine has been benchmarked against a completed reference interpreter.

**Revisit when:** A genuine requirement introduces durable state, effectful iteration, parallelism, complex branching/recovery, or when a candidate engine demonstrates lower total code and maintenance cost while preserving artifact trust, simulation parity and future Java runner equivalence. Don't adopt an orchestration platform just to execute four small instruction categories.

## 7. SaaS web authoring, simulation and activation

**The optional agent-first SaaS experience is online.** A business user describes behavior in a web UI; a *host-owned* backend agent loop clarifies ambiguous intent, generates IR, accepts compiler diagnostics and proposes reviewed examples. This is a usability layer, **not part of Keir**. Developers can also directly author the same IR using Keir's CLI/APIs. Keir should ship an optional agent skill/tool example, not an LLM runtime dependency.

```text
Customer prompt (web) -> Agent proposes IR
                        -> Compile against trusted DCM
                           |-- diagnostics -> agent revises (bounded loop)
                           `-- compiled artifact
                        -> Agent proposes scenarios/expected behavior
                        -> Simulate exact artifact (same Node runner, fake adapters)
                        -> Show diffs, branch/effect traces; user reviews oracle
                        -> Approve exact artifact + tests + DCM/policy versions
                        -> Activate (or reject); audit/disable/rollback by version
                        -> Host entrypoint invokes Node runner + real adapters
```

The agent can generate examples, inspect diagnostics and suggest test refinements. **It cannot certify host contracts, judge its own expected behavior as correct, or approve its own activation.** A modified IR must be recompiled, retested and reapproved. The host should bound agent iterations and computational costs.

**Simulation contract:** One compiled artifact, one runner, different adapters. Scenario input and fixed clock; deterministic stubs with `SUCCESS`, business rejection, transient failure or `UNKNOWN_COMMIT`; expected typed result, exact ordered calls, branch decisions and effects (`NONE`, `CONFIRMED`, `UNCERTAIN`). Unexpected calls and unused stubs fail the scenario. Trace evidence must be redacted and tied to the artifact digest. Scenarios authored by the same agent can repeat its misunderstanding; customer review of expected outcomes is required.

**Testing layers are distinct:**

- **Compiler negative/conformance tests:** Reject forbidden calls, types/units, tenant-binding overrides, dynamic endpoints, over-budget work, invalid error handling and unsound dataflow.
- **Runner conformance tests:** Java and Node (when both exist) run identical compiled artifacts and deterministic scenario fixtures, producing equivalent outputs, calls, effects and termination classes.
- **Scenario/business simulation:** Customer-reviewed expected results and failure-injection tests for each extension, executed by the production runner with fake adapters.
- **Host adapter integration/concurrency tests:** Independently prove real uniqueness/CAS, replay, auth scope and uncertain-outcome classification. Mock results do **not** certify a real remote transaction.

**Failure discipline:** `SUCCESS` advances; an unhandled `BUSINESS_REJECTION` terminates or takes a declared terminal business result; `TRANSIENT_FAILURE` stops in v0 (no automatic retry); `UNKNOWN_COMMIT` stops as **UNCERTAIN** with no success claim or retry. A lost response cannot be treated as proof no effect occurred. Later reconciliation, if needed, is host-owned and out of the first synchronous runner scope.

## 8. Demo architecture: useful Node task-management SaaS

The public demo is a **small usable task manager**, not a synthetic evaluator. It is a demonstration of Keir rather than a dependency of the library. Core task CRUD, projects/lists, priorities, tags, due dates, completion and search work without the framework. The framework becomes a pluggable customization facility.

**Single deployment constraint:** One Node.js/TypeScript web application contains the UI, backend agent loop, task domain, real adapters, extension storage, simulation orchestration and **Node runner**. It bundles the **Java compiler CLI/JAR plus a Java runtime** and invokes the CLI locally from Node **only when authoring/revising an extension**. The compiler runs as a subprocess, **not as a separately deployed HTTP service**. One service/container can still contain two language runtimes and short-lived subprocesses. If CLI startup becomes costly, optimize later without changing the architecture.

First capability slice: `TaskCompleted` + scoped `getTask`, bounded `listTasks`, safe `createTask` with **host-enforced atomic follow-up uniqueness** and host-owned replay identity. Multiple reads, pure calculations, optional dates, one controlled mutation. Two different extensions use the *same* catalog: invoice payment follow-up and next recurring review task creation. A later read-only `FocusListRequested` extension demonstrates another output surface. A proposed `BeforeTaskCreate` interception hook shows a different pattern: read an approved project policy and return a typed `ALLOW` or `REJECT` result that the host must enforce. That hook and its additional project capability are **not present in the frozen first-slice DCM fixture** and must not be presented as implemented. Assignment/dependency examples from the language-design package are useful language stress tests, **not an obligation to implement every operation in the first demo**.

**Distinguish due dates from waits:** Creating a task due in seven days is a synchronous mutation now. No scheduler, durable `WAIT`, resumed workflow or background Java service is needed for that extension.

**Selected expression integration (October 28 CI):** Java 21 + CEL-Java 0.14.0 compiled and checked real expressions, and Node 22 + CEL-ES 0.6.1 loaded the exported checked protobufs. The final successful public CI run is [#40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668) on the temporary [spike branch](https://github.com/rore/bear-account-demo/tree/spike/cel-java-node-interop-20261028), commit `3023f3e14199cb555f6ba247991438ba0358e5ea`. Reported: 25/25 semantic fixtures, 46/46 harness checks, no observed mismatch. The conformance harness covers CEL interoperability only, not arbitrary user-supplied expression safety.

**Recommended implementation slices:**

1. **CEL portability gate, complete for tested subset:** Keep the existing real-library CI harness as a regression gate on every compiler/evaluator upgrade. Complete the still-missing nominal/unit typing overlay, source-to-checked-AST allowlist and enforceable cost policy as part of the core POC. Do not misstate CI parity as full language support.
2. **Thin engine:** Handwritten DCM + custom IR schema/parser + Java compiler CLI that emits a versioned plan and checked CEL protobufs; Node loader/interpreter + fake adapters; one read/guard/create test and deterministic trace.
3. **Usable app and real adapter:** Build task CRUD and atomic follow-up uniqueness; execute the identical artifact against fake and real adapters; replay/error tests.
4. **Agent web workflow:** Bounded IR-repair loop, scenario proposals, customer preview/approval, immutable storage, activation/disable/rollback.
5. **Breadth:** Second distinct automation without new domain implementation; read-only view variation; benchmark, conformance and security negative tests. Consider Java runner after the Node path is proven.

No requirement to deploy a second compiler service or generate Java/Node business source for this POC. The task-specific scope lives in [Task Management App POC](task-management-poc.md).

## 9. Evaluating real host-product value

The framework targets a **general** extensibility problem; its usefulness in a particular SaaS product must be demonstrated rather than inferred from a working compiler. For the public task-management demo, compare several customer-authored behaviors against the same frozen DCM and against simpler alternatives:

- **Invoice follow-up:** Read a completed task, search existing follow-ups, evaluate a condition and invoke one approved `tasks.create` mutation. Atomic uniqueness is enforced by the host, not by the read/check sequence.
- **Recurring weekly review:** Reuse the same `tasks.get`, `tasks.search` and `tasks.create` capabilities but change eligibility and due-date calculation without adding a bespoke handler.
- **Personal focus list (later):** Query a bounded task collection, compute a pure ranking/filter and return a typed view. Compare honestly against a simple built-in list filter, which may be sufficient.
- **Before-create policy hook (proposed):** A host intercepts task creation, Keir reads an approved project policy, and an extension returns a typed `ALLOW`/`REJECT`. The host enforces that result and preserves native validations. This tests a different integration surface and prevents the demo from being only post-event automations.
- **Counterexample:** A hypothetical strict concurrent quota cannot be enforced safely by `read usage -> check -> write` without a trusted atomic reservation/conditional mutation. Reject unsupported guarantees instead of adding unsafe workflow syntax.

For any future host, estimate **repeated unmet needs**, the number of customizations served by each reusable entrypoint and capability, avoided per-customer code, additional support/operational costs, and the value relative to native configuration or a single CEL-enabled fixed hook. Separate host-integration effort from customer-specific logic. No measured ROI or adoption claims are made here.

A generic library should remain worthwhile as an experiment even if the task-management demo shows that some simple use cases are best solved by ordinary product configuration.

## 10. Open engineering questions and decision gates

**What is now demonstrated:** Real library interoperability of **CEL-Java 0.14.0 → checked protobuf → CEL-ES 0.6.1** for 19 compiled expressions and 25 semantic fixtures, with 46 harness checks passing. This supports the **Node demo**. It does not establish semantic equivalence for every CEL operation or stable performance across different workloads.

**Blocking for a credible, bounded POC:**

1. **Implement the whole IR compiler and artifact wire contract:** Strict YAML/JSON parsing, semantic DCM resolution, AST restriction, nominal/unit/dataflow checks, error/effect analysis, deterministic plan serialization and stable diagnostics. The existing CEL harness tests expressions, not these components.
2. **Artifact trust and approval:** Canonical manifest, compiler provenance, immutable DCM and policy pins, descriptors/AST digests, host-signature verification, version/adapter compatibility and explicit customer approval bound to the exact artifact and scenario revision. Compilation alone is *not* activation.
3. **Resource limits:** Enforce AST/macros and per-input list bounds, total expression cost and elapsed-time limits. CEL-ES 0.6.1 hard gas/instruction metering was **not** demonstrated. A synchronous JS expression cannot be interrupted by an ordinary timeout in the same event-loop thread; use a proved small-subset bound or appropriately isolated execution where required.
4. **Real adapter contracts:** Verify concurrent atomic follow-up uniqueness, host-owned replay identity, authorization and access scoping, and `UNKNOWN_COMMIT` handling independently of fake adapters.
5. **End-to-end equivalence:** Execute the **same final compiled program** in the Node runner with fake and real task-management adapters, assert traces and effects, and repeat expression interoperability tests with future Java/Node runner versions.

**Next host-contract design work (not blockers for the initial `TaskCompleted` slice):** Specify the `BeforeTaskCreate` pre-operation result/failure contract, and the trusted effective-catalog overlay for optional tenant endpoints. Neither requires changing the v0 IR step grammar. External tenant endpoints themselves remain deferred.

**Choices to defer:** Java runner implementation; generated Java/Node code as an optimization; HTTP compiler service; broad OpenAPI/AsyncAPI auto-import; multiple mutations requiring recovery; scheduling/resumption, automatic retries, bulk effects, and generalized calendar/timezone support. None is required for the first demonstration.

**Decision boundary:** The current GO is for the **restricted, tested CEL subset in a POC**. Any new CEL function or overloaded operation must be proven against real Java/Node runtimes and explicitly admitted to the compiler policy. If hard resource limits cannot be established for a particular expression shape, reject it rather than silently accepting it.

## 11. Source register and maintenance

**Design-package drafts (28 October 2026):** [keir-language-v0.md](keir-language-v0.md), [domain-model-v0.md](domain-model-v0.md), [compiler-artifact-and-runners-v0.md](compiler-artifact-and-runners-v0.md), [simulation-and-authoring-v0.md](simulation-and-authoring-v0.md), [task-management-poc.md](task-management-poc.md), and [decisions-and-evidence.md](decisions-and-evidence.md). These define the proposed IR, DCM, compiler, runners, simulation, evidence and deployment decisions. The draft schema and example artifacts are included; a full library is not yet implemented.


**Cross-language expression evidence (28 October 2026):** [successful GitHub Actions run #40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668), [temporary source branch](https://github.com/rore/bear-account-demo/tree/spike/cel-java-node-interop-20261028), and original report `expression-interoperability-findings(1).md` (written before the CI results). The published run pins CEL-Java 0.14.0, CEL-ES 0.6.1, Java 21 and Node 22. Include the run/commit/fixture coverage when reporting results; don't cite the earlier unexecuted report as proof of parity.

**Public technical references:** [CEL-Java](https://github.com/cel-expr/cel-java), [CEL-ES 0.6.1 source](https://github.com/bufbuild/cel-es/tree/v0.6.1), [CEL CheckedExpr protobuf](https://github.com/google/cel-spec/blob/master/proto/cel/expr/checked.proto), [CEL language definition](https://github.com/cel-expr/cel-spec/blob/master/doc/langdef.md), [Open Workflow specification 1.0.3](https://github.com/open-workflow-specification/specification/blob/v1.0.3/dsl-reference.md).

**Maintenance rule:** This public working document records **principles, selected designs, tentative deployment choices, implementation evidence and unresolved gates separately**. When research changes one, update the decision snapshot and related sections. Keep rejected alternatives and limitations rather than retroactively treating proposals as validated facts.

**Change log:** v0.3.1 (28 October 2026): public-safe consolidation of the complete architecture, provisional minimal-runner build-versus-reuse decision, and generic host-adoption criteria; removed all domain-specific research discussions and examples. v0.3 (28 October 2026): CI-confirmed Java/Node checked-CEL path, selected compiler/evaluator library pins, revised resource/trust gates, and explicit references to the full IR/DCM/compiled-artifact and simulation specifications. v0.2 (28 October 2026): selected custom IR + CEL; captured initial business-composition feasibility and adoption criteria; defined online SaaS authoring/repair/test/approval lifecycle; adopted a single authoritative compiler and portable compiled artifact with Java/Node runner targets; selected one-service Node task-manager POC with bundled Java compiler CLI; captured portability, performance, testing and trust gates. v0.1 (27 October 2026): initial consolidated product definition.

**v0.4 changes:** Named Keir; articulated the closed, agent-independent programming model and agent-first host experience; specified host-owned hook/result-consumer semantics and optional tenant capability layering/external endpoint boundaries; strengthened the demo distinction from fixed-hook rules. Existing tested CEL and compact-runner decisions remain unchanged.
