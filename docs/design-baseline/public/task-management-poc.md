# Keir Task Management App POC

**Working product and demo definition | v0.4 | 28 October 2026**  
**Status:** Proposed pet-project POC; not implemented or committed to a roadmap.  
**Relationship:** Public, domain-neutral demonstration of **Keir**, the agent-independent controlled extension language/compiler/runner.

> **Core idea:** Build a genuinely usable, lightweight personal task manager and let users request new task behavior in natural language. An AI agent composes only approved task capabilities into a closed extension program. A trusted compiler validates and lowers it to a portable artifact. The embedded Node runner simulates and executes that same artifact through trusted application adapters.

## 1. Why this application

The task manager is **both a useful application and an ongoing dogfooding environment** for the generic framework. Unlike a synthetic rules demo, it can be used every day, and its domain and examples are suitable for public development. The app should stand on its own without AI or extensions. The web agent is a **host application feature** used to make Keir customizations accessible to nontechnical users, not part of the core Keir library.

The technical demonstration is **customer-authored capability composition**, not merely an AI-written condition in an existing automation. Different extensions can select reads, apply calculations and conditions, and invoke permitted operations without requiring a new application handler for each rule.

Keep the two codebases conceptually separate:

- **Task application:** Owns its task model, UI, persistence, application events, API operations, authorization, and lifecycle of task changes.
- **Keir framework:** Owns the domain-capability format, restricted extension language, deterministic validation, simulation, execution plans, and generic adapter interfaces. It must have no task-specific logic.

## 2. The useful task-management MVP

The ordinary application provides projects or lists; tasks with title, notes, status, priority, tags, due date and completion time; and basic create, edit, complete, search, and filter experiences. A simple desktop-friendly web UI is sufficient. Local-first or single-user deployment is acceptable for the first POC; shared accounts and mobile applications are not prerequisites.

**Initial task model:** `Task{id, projectId, title, notes?, status, priority, tags[], dueAt?, completedAt?, createdAt, origin?}`. `origin` is protected metadata for identifying extension-created tasks, not a free-form agent-controlled privilege.

The app must offer real daily utility without Keir: a usable task list, completion workflow, quick editing, and due/priority filters. Extension failures must never break basic task CRUD.

**Explicit non-goals for v0:** calendars and notifications, background timers, recurring scheduling engine, external integrations, collaborative permissions, dependency graphs, complicated reports, full chat product, or AI-generated UI. A *due date on a newly created task* is not a durable wait.

## 3. Approved task-domain capabilities

The app defines ordinary APIs first; the host then **explicitly selects** the subset available to extensions. An OpenAPI importer could supply types later, but does not automatically grant capabilities.

**v0 entry point:** `TaskCompleted`, with the completed task ID and host-bound execution context. The host emits this only after its own successful task-completion transaction. The extension does not control event admission or the acting user.

**v0 capability catalog:**

- `tasks.get(taskId) -> Task`: read a task visible to the current host-bound user; missing task is a typed `TASK_NOT_FOUND` business rejection.
- `tasks.search(projectId?, originSourceTaskId?, originKind?, status?, maxResults) -> TaskSearchResult`: bounded, authorized search over individually typed optional filters, returning `{items: Task[]}` with no silent truncation of the narrow origin query. The exact fictional v0 parameters are in `examples/task-domain.yaml`; broader tag/priority filters can be added only through DCM revision.
- `tasks.create(projectId, title, originKind, dueAt) -> Task`: create one task through the normal service, subject to field constraints, quota, atomic origin-key uniqueness, and a stable replay identity **bound by the host** to the extension/version/event/step. Host-only source task ID, actor, tenant and operation-key parameters are never agent arguments.

The catalog also exposes safe clock and date arithmetic as pure expressions; the host supplies a fixed invocation time. Task IDs, project IDs and due dates have explicit types. The agent cannot select arbitrary URLs, run SQL, choose another user, or override the host context.

**Next read-only entry point:** `FocusListRequested`, returning a typed list/view model. It can reuse `tasks.search` with an appropriately expanded approved query/result contract (e.g. priority and enough bounded candidates) and no mutation. **That entrypoint and the extra query fields are not yet in the included v0 DCM fixture.** This is a second demonstration slice, not a prerequisite for initial completion-event automation.

A later version may expose `tasks.update` with explicitly permitted fields and version checks. Deletion, sharing, arbitrary reassignment and bulk mutation are excluded initially.

## 4. Customer-generated extension examples

All examples use the **same frozen v0 catalog**; the customer changes the program, not the application's Node domain implementation. Example prompts are product scenarios; the selected structural IR remains a normative draft, not a running implementation.

### A. Payment follow-up (first end-to-end demonstration)

> When I complete a task tagged `invoice-sent`, create a payment follow-up due seven days later, unless a follow-up for that source task already exists.

Composition: `TaskCompleted -> tasks.get -> conditional tasks.search -> tasks.create`.

The created task inherits the project and refers to the original task. Simulation verifies qualifying/nonqualifying tags, existing follow-up, due-date calculation, duplicate event replay, forbidden project selection and task-creation failure. A search-before-create guard is useful for preview, **but it is not an atomic deduplication guarantee**; `tasks.create` must enforce the host-owned operation identity.

### B. Next recurring review (same catalog, different behavior)

> When I finish a task tagged `weekly-review`, create next week's review with the same project and a due date seven days after the completed review's due date, or after completion if it had no due date.

Composition: `TaskCompleted -> tasks.get -> date/conditional calculation -> tasks.search -> tasks.create`.

No scheduler is involved: completing the current review immediately creates a future-dated task. This tests optional dates, value propagation, a different condition and a different created-task shape without new domain adapters.

### C. Personal focus list (second entry point, read-only)

> Show my five most important open tasks, prioritizing overdue items, excluding completed tasks, and preferring the current project.

Composition: `FocusListRequested -> tasks.search -> bounded filtering/scoring -> typed return`.

This tests a different result consumer using the same generic evaluator. It should be compared honestly with a predefined filter/sort UI or a CEL-only derived view; it may not require the richer composition language.

**Optional later stress case:** When a task completes, check bounded dependent tasks and mark newly unblocked tasks ready. This would require trusted update capabilities and bounded multi-effect semantics, so it is **not** v0 scope.

### D. Before-create synchronous policy hook (proposed second integration pattern)

A host-defined `BeforeTaskCreate` hook can let a customer customize validation of an operation *before* it commits. Example business request:

> In projects marked as requiring justification, reject a new urgent task unless its notes explain the reason.

The host supplies a projected proposed-task request and grants a bounded `projects.get` read. The extension calculates the condition and returns a typed `ALLOW` or `REJECT` with an approved reason code. The task service must enforce a rejection before creation and revalidate any allowed request against native invariants. There is no extra write by the extension and no durable workflow. This is **not currently included in the sample DCM fixture**: the host must define the hook, typed result consumer and project policy field through a future approved DCM revision. It is a design follow-on, not a claim that the v0 fixture already compiles the example.

This gives the POC a stronger test of Keir's value beyond a fixed post-completion workflow. Compare it with a simple pre-create CEL predicate and with hard-coded validation at the **same host integration cost**. It earns its richer IR only where the author can select approved domain reads/conditions and deliver genuinely different compositions.

### Optional future tenant integration, not part of first POC

A host may later register a tenant-specific validation/data endpoint, publish it as a scoped typed capability, and admit it into the effective catalog for that tenant. Keir would treat it as an ordinary `call` with a declared deadline/error contract. The host alone registers/trusts the endpoint, handles transport and credentials, and decides whether an AI user can propose such a registration. No arbitrary endpoint URLs are accepted in the IR. [Detailed contract](hooks-and-tenant-capabilities.md).

## 5. Online SaaS authoring and approval

Design time is an **interactive web-app workflow**, not an offline CI or developer-only process:

**Step 1 - Request:** The user describes new task behavior in the web UI.

**Step 2 - Generate:** The **host-owned, optional** backend agent loop receives the approved task DCM and produces **custom structural IR + CEL** and draft scenario expectations.

**Step 3 - Compile and repair:** Node invokes the **bundled Java compiler CLI** with the IR and trusted DCM binding. The compiler emits deterministic diagnostics or a portable compiled artifact. Diagnostics feed back into the agent for a **bounded** correction loop.

**Step 4 - Simulate:** The user reviews proposed scenarios and expected effects. The **Node runner** executes the **same compiled artifact** with fake adapters, a fixed clock and declared fault outcomes. Actual results, branch/effect traces and discrepancies are shown.

**Step 5 - Approve:** The user approves or rejects the **exact artifact and reviewed scenarios**. The app stores immutable artifact/definition/DCM versions, test evidence and activation status. Any edit requires recompilation, rerun and reapproval.

**Step 6 - Activate:** Actual `TaskCompleted` events execute the approved artifact with the same Node runner and real task adapters. Extensions can be disabled or rolled back by version.

The agent can propose and revise, but **cannot grant capabilities, certify atomicity, or approve activation**. Its own expected test outcomes are not independently trustworthy. The app must bound the agent loop's iterations and spend.

**Required scenarios:** qualifying and nonmatching tag; missing task; existing follow-up; optional due date; event replay; task-create rejection; `UNKNOWN_COMMIT`; malformed/forbidden extension. Simulation checks exact ordered calls, effects and terminal classifications, not only the final value. For the first program the term "seven days later" is explicitly defined as **168 elapsed hours**; user-calendar-day semantics require a later trusted calendar/timezone binding.

## 6. One deployment, two build/runtime technologies

**Deployment target:** **one Node.js/TypeScript SaaS web application**, with UI, agent orchestration backend, task persistence, scenario storage, simulation mode and an embedded Node extension runner. No additional HTTP compiler service, separate workflow engine or second deployed application. For v0, implement only the minimal bounded interpreter and **reuse existing CEL, Protobuf, parser and cryptographic libraries**; this build-versus-reuse decision is provisional pending an actual implementation comparison.

When an extension is authored or revised, the Node process invokes an adjacent **Java compiler CLI/JAR as a local subprocess**. A Java runtime is bundled in the container or installation; it does **not** participate in ordinary task execution. Keeping the compiler as an independently callable CLI leaves room for future service packaging without requiring it in the POC.

The compiler receives untrusted agent-authored IR plus **trusted DCM identity**, validates grammar/CEL/types/effects/grants/budgets and produces a portable, versioned artifact. **Expression stack proven for the selected subset:** Java 21 + `dev.cel:cel:0.14.0` emits `CheckedExpr` protobufs; Node 22 + `@bufbuild/cel:0.6.1` loads and executes them. The [real-library CI run](https://github.com/rore/bear-account-demo/actions/runs/40698821668) passed 25 semantic fixtures and 46 harness checks. This was not an end-to-end task application run; cost isolation and nominal/unit checking remain open. The **Node runner** verifies artifact provenance, version/DCM compatibility and adapter registration, prepares its expressions **once** at load time, then executes many short invocations. It never treats arbitrary JSON labeled `compiled` as approved authority.

**Interop status:** The Java-to-Node `CheckedExpr` path is now **verified for the tested restricted subset** in actual published-library CI (25 semantic fixtures and 46 harness checks). It does not validate every CEL construct or the complete extension engine. **Remaining gates** are compiler-level nominal/unit checks; strict AST/overload allowlisting; cost, time and input-size enforcement; approved-artifact provenance; and real adapter authorization, result and error contracts. The runner must still enforce runtime authorization, type/cardinality limits and error classification.

The task host owns authenticated identity, object/project access, event admission, task persistence, transaction boundaries, protected replay identity and a **real atomic uniqueness contract** for `createTask`; a search-then-create decision alone cannot prevent duplicate follow-ups under concurrency. First POC stays synchronous: multiple reads, bounded CEL, at most **one mutation per invocation**. No durable waits, arbitrary effect loops, compensation or distributed transactions.

## 7. Testing model and implementation slices

**The test model has four layers:** compiler rejection/negative tests; identical Java/Node runner conformance once both exist; user-reviewed business simulations with the Node runner and fake adapters; and **real task-adapter** integration/concurrency tests for uniqueness, replay, permissions and uncertain outcomes. Passing mocked scenarios does not certify real database behavior.

**Slice 0 - Expression interoperability (completed for supported subset):** Java 21 + CEL-Java 0.14.0 exported checked protobuf consumed by Node 22 + CEL-ES 0.6.1. Keep the passing conformance suite as a dependency-upgrade gate. **Not completed:** full IR program execution, nominal/unit overlay and hard expression resource isolation.

**Slice 1 - Useful app:** Implement task CRUD, completion, lists/projects and search; verify task management works without AI/extensions.

**Slice 2 - Thin framework:** Hand-author one or two extensions; freeze the task DCM; compile through local Java CLI; execute the portable artifact with Node runner using simulated and real adapters. Demonstrate replay and `UNKNOWN_COMMIT` handling.

**Slice 3 - SaaS authoring:** Connect a bounded backend agent loop, diagnostics-based repair, proposed scenarios, review/approve UI, immutable versions, and activate/disable/rollback.

**Slice 4 - Demonstrate composition and new hook surface:** Run both A and B without adding a new host adapter or domain algorithm. Optionally add read-only `FocusListRequested` (C) or the `BeforeTaskCreate` intercept (D) through an explicit new host DCM revision, not a silent change to the frozen initial catalog. Measure cold load, warm runtime overhead and app latency separately.

The first POC **does not** require a Java runner, generated Java/Node source, automatic OpenAPI imports, a timer engine or a distributed compiler service.

## 8. Acceptance criteria

- The app is useful for ordinary task management, even with the extension system disabled.
- The **web conversation -> IR -> Java compiler -> artifact -> Node simulator -> review -> activation** loop works end-to-end in one deployment.
- Two materially different task automations use **the same frozen DCM, artifact format, runner and task adapters**, without per-customer Node business code. Their incremental benefit is compared with a fixed-hook CEL solution.
- A later `BeforeTaskCreate` hook demonstrates a typed host-enforced `ALLOW`/`REJECT` decision, without embedding a task-specific instruction in the Keir runner.
- The exact compiled artifact runs with fake or real adapters, producing reviewable ordered calls, effects, branch decisions and error outcomes.
- Runtime trust is enforced: forged plan/host context, forbidden effects and operations, unbounded work, type violations and unsafe error handling are rejected or blocked.
- Real host tests demonstrate atomic follow-up uniqueness, replay safety and current authorization; simulated evidence is not misrepresented as production guarantees.
- Performance of the prepared Node runner is measured rather than assumed; the test distinguishes CEL/dispatch overhead from real task/database latency.

**Stop or narrow the hypothesis** if portable expression execution proves impractical, most extensions demand new bespoke adapters, native task configuration is simpler, or the infrastructure costs outweigh useful customization.

## 9. Design assets and integration references

The [Keir Extension IR v0 specification](keir-language-v0.md) defines the actual agent-authorable YAML grammar, `let`/`call`/`when`/`onError`/return semantics and a full follow-up example. [Domain Capability Model](domain-model-v0.md) defines available host calls and guarantees; [compiler/artifact/runner](compiler-artifact-and-runners-v0.md) defines the portable plan and cross-language CEL; [simulation and authoring](simulation-and-authoring-v0.md) defines agent-loop steps, scenario structure and approval. A small set of illustrative files is provided in `examples/`, with a draft structural schema in `schemas/`.

**Do not use agent-supplied DCM or signing keys as the trust root.** The Node host resolves the approved DCM itself and records a genuine user authorization separately from the agent. The sample YAML/schema exercise language structure and are not a substitute for a complete compiler or proven adapter guarantees.

## 10. Decisions, open questions and maintenance

**Selected v0 design:** Keir (Controlled Extension IR), small custom structural IR + CEL; a Java CLI as authoritative compiler; portable compiled artifact; an embedded TypeScript/Node runner; one Node web deployment; first-class customer-reviewed simulation; short invocations with one write in the first POC. Restricted Open Workflow remains a documented comparison, not the v0 authoring language.

**Open and gating:** The tested CEL-Java 0.14.0 to CEL-ES 0.6.1 subset passed real CI; **full IR compilation and runtime safety remain open**. Remaining work includes exact portable artifact encoding/provenance, DCM nominal/unit overlay, enforceable CEL expression-cost limits, local Java packaging/startup cost, safe activation and schema migration, certified typed task API guarantees, and the web authoring UI. These are proposals, not verified end-to-end implementation claims.

**Related source of truth:** The full generic architecture is in [Keir architecture](keir-architecture.md). This document covers only the fictional task-management demonstration. Keep both documents synchronized when decisions affect their scope.

**Change log:** v0.3.1 (28 October 2026): removed domain-specific research references, aligned the existing runner reuse decision, and corrected CEL interoperability status and implementation milestones. v0.3 (28 October 2026): linked complete language/DCM/artifact/scenario definitions and incorporated actual CEL-Java/Node CI parity, while retaining the one-deployment, local-CLI demo design. v0.2 (28 October 2026): aligned language choice, two-format compiler/runner design, single Node SaaS deployment with local Java CLI, online agent authoring/revision/testing/approval, CEL portability gate and layered tests. v0.1 (28 October 2026): initial task-management POC definition.

**v0.4 changes:** Named Keir, clarified the optional host-owned AI business-user interface, and specified a second synchronous `BeforeTaskCreate` integration pattern plus tenant capability ownership as future capabilities. The first v0 task DCM and CEL interoperability results remain unchanged.
