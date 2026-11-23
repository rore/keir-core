# Daymark Team Processes: AI-authored business capabilities from bounded Keir programs

**Status:** Proposed feature / agent handoff, not yet implemented  
**Date:** November 23, 2026  
**Primary repository:** [rore/keir-demo-app](https://github.com/rore/keir-demo-app)  
**Dependency:** Published [rore/keir-core](https://github.com/rore/keir-core) compiler and runtimes; **no Keir Core change is assumed**  
**Working names:** *Team process* (user-facing), *bundle* (internal versioned set of Keir programs)

## 1. Product outcome and hypothesis

Demonstrate that a small but credible work-management SaaS can expose **stable, trusted domain capabilities** and let a team administrator use AI to create **new business functionality the vendor did not implement**. The business behavior runs as several bounded Keir programs in response to ordinary application events; the host continues to own data, authorization, native operations, concurrency, and recovery.

**Success is not a generated UI or a sophisticated hardcoded approval feature.** Success is that two different team processes compile and run against the **same frozen host capability catalog**, with no process-specific host code.

Keep **Personal** projects and their existing small owner-scoped customizations intact. Introduce a minimal **Team** project mode with a project administrator who can author, simulate, approve, and activate a *single coherent team process* (a bundle) for that project. Do not add a workspace/account hierarchy.

### Required demonstrations

**A. Deliverable review:** In a team project intended for client deliverables, when a member creates a top-level task, a Keir program requests a linked, assigned review child task. The deliverable cannot be completed until its review child has been completed. A different team member can finish the review later; no Keir program waits during that time. This is a **review-completion gate**, not formal approval/rejection or sign-off.

**B. Intake routing:** Under the **same host catalog**, a different generated process applies routing rules to new tasks: for example, high-priority tasks are assigned to a chosen project member and given a due date. It uses bounded reads and one native update, without a native routing engine or review-related logic. Run it on a separate fixture/project, or activate it after the first process while testing existing-version pinning. It need not run simultaneously with A in the first product slice.

Both processes must be implementable without editing the catalog between A and B.

## 2. Current baseline, verified in repository

- Daymark uses React/TypeScript, Node/Fastify, and SQLite. Three seeded demo users are independent owner scopes; projects and tasks are user-owned, not shared.
- Keir currently has `TaskBeforeCreate`, `TaskCreated`, and `TaskCompleted` entrypoints. It does **not** have `TaskBeforeComplete`.
- The DCM exposes `tasks.get`, `tasks.openMatch`, `tasks.create`, and `tasks.update`. The current `tasks.update` targets the triggering task. There is no assignee or task relationship in the existing task model.
- Revisions are owner-scoped and selections are keyed by **owner + hook**. Different hook rules are not approved/activated as one bundle.
- Automation-created tasks currently do **not** emit a further `TaskCreated` workflow event. Preserve this no-cascade behavior in the first slice and document it.
- Keir Core permits before-commit read-only decisions and after-commit programs with at most one possible write, subject to the current call/step/AST limits. Its scenario protocol runs a single pinned artifact; it is **not** an existing multi-event process simulator.

Reference sources: [Daymark model](https://github.com/rore/keir-demo-app/blob/main/extensions/task-effect/numeric/dcm.json), [lifecycle](https://github.com/rore/keir-demo-app/blob/main/src/extension-lifecycle.mjs), [native task service](https://github.com/rore/keir-demo-app/blob/main/src/data.mjs), [integration](https://github.com/rore/keir-demo-app/blob/main/docs/read-extension-lifecycle.md), [Keir Core runtime](https://github.com/rore/keir-core/blob/main/docs/specs/runtime.md).

## 3. Architectural boundaries

**Daymark owns:** Project membership and roles; task persistence, assignments and child relationships; valid task operations; project-scoped authorization; event capture; native transactional commit; host capability adapters; process bundle storage, simulation orchestration, review and activation; operational status and recovery.

**Keir Core owns:** Compile one bounded program against a trusted domain capability model; statically validate types, data flow, allowed operations and effects; prepare and invoke exactly that artifact with an approved host binding. Core does not schedule, wait, retry, call another Keir program, manage bundles, or coordinate a business process.

**AI owns no authority:** It proposes the bundle's programs and examples. It cannot change the trusted host catalog, invent endpoints, approve/activate its own proposal, supply protected actor context, or bypass native authorization. Production execution makes no AI calls.

**Persistent business state connects events.** The review task and its parent relationship, not a suspended workflow, remember progress. A bundle is a lifecycle/approval unit, **not** a single Keir invocation or transaction across events.

### Explicit non-goals

No generic entity or schema generator; no arbitrary custom fields; no approval engine; no arbitrary generated React/HTML/UI; no durable workflow engine, queue-orchestrator replacement or inter-program calls; no multi-write Keir programs; no cross-project processes; no general function composition; no arbitrary iteration; no multiple simultaneously active team bundles per project; no organization/workspace hierarchy; no changes to the personal-customization semantics. No automatic retries of uncertain native writes.

## 4. Minimal native Daymark domain additions

Implement only what is useful as ordinary task-management functionality, regardless of the demo process:

1. **Team projects:** A project has a mode `PERSONAL` or `TEAM`; team projects have explicit members and roles (`ADMIN`, `MEMBER`). Seed one shared team project with Maya as administrator and Leo as member; preserve the existing personal projects and data. Authentication is still the existing demo sign-in, not production IAM.
2. **Task assignment:** Optional `assigneeId`, referencing a current project member. Define and test who can edit/complete assigned tasks; a defensible minimum is assignee or project admin, while permitted members can create and view team tasks.
3. **Parent/child relationship:** Optional immutable `parentTaskId` in the same project, with one parent per child, bounded depth or a simpler no-nesting rule, no cycles, and no cross-project references. Creating a child and recording its link is **one atomic native task-create operation**. Do not create a review-specific relation API.
4. **Domain reads:** A bounded, indexed `tasks.childrenSummary(parentTaskId, tag)` read returns whether at least one matching child exists and whether any matching child remains incomplete. This is a generic child-task query, not `reviewApproved()` or an unbounded list. The tag is a user-authored discriminator (e.g., `review`), not a host-defined approval entity.
5. **Domain writes:** Extend generic `tasks.create` with optional `parentTaskId`, `assigneeId` and existing tag fields; extend `tasks.update` with assignment plus current priority/due-date changes in **one atomic update**. Every operation checks membership, project bounds, types and native invariants at commit.
6. **Extension point:** Add a read-only `TaskBeforeComplete` decision hook around native completion, with fresh revalidation of any read facts before the completion commits. Use Core's existing decision semantics; no new Core opcode.

Do **not** add an approval status, reviewer-signoff entity or process-specific service. For this example, a child task marked `review` being *completed* is all the business state required. Make this limitation explicit in UI wording.

## 5. Proposed capability catalog (one version for both process examples)

Illustrative contract shape; names and DTO fields may be adjusted during the spike, but **freeze the same catalog for both examples before concluding feasibility**.

| Entrypoint / capability | Purpose | Effect |
| --- | --- | --- |
| `TaskCreated` | Trusted event for a newly saved human-created task | Event; program may make one WRITE |
| `TaskBeforeComplete` | Decision on an attempted native completion | Read-only DECISION |
| `tasks.get(taskId)` | Read authorized task fields, including parent/assignee/tags/status | READ |
| `tasks.childrenSummary(parentTaskId, tag)` | Return `{exists:Boolean, anyIncomplete:Boolean}` for scoped children | READ |
| `tasks.create(...)` | Create a task, optionally assigned and linked as a child | WRITE |
| `tasks.update(...)` | Atomically update permitted properties of the triggering task | WRITE |

Retain other existing catalog entries where necessary for compatibility. Protected project/actor/operation identity is host-supplied and cannot be overridden by source. Use typed task, project and user IDs. Bounded response records only; no arbitrary collections or full-text search in Keir.

**Compiler feasibility is a gate:** prove the actual proposed types, optional fields, guarded calls and both hook definitions compile against the installed Core version. If they do not, report exact diagnostics and consider the smallest adjustment to the host model before proposing a Core extension.

## 6. Executable process A: deliverable review

Configure the first team project as a *client deliverables* project. The process applies to newly created **top-level tasks in this project**; generated review children are not themselves deliverables. This removes reliance on mutable tags for identifying governed roots.

**Program A1, `TaskCreated`** (illustrative behavior, not Keir syntax): Read the triggering task. If it is a root task, request one `tasks.create` with `parentTaskId = sourceTask.id`, `assigneeId = Leo`, title `Review: <source title>` within native length limits, and tag `review`. Because `tasks.create` atomically sets the link, this remains **one Keir WRITE**. If the source is already a child or outside the scoped team project, no effect.

**Program A2, `TaskBeforeComplete`:** A governed top-level task can be completed only if `tasks.childrenSummary(taskId, "review")` reports **exists** and **no incomplete child**. Otherwise DENY with a clear business message. A review child has no governed root pin and follows ordinary task-completion rules. An execution failure, missing review, stale evidence, or unavailable runtime must not turn into ALLOW for a governed root.

Flow: create deliverable → persist event and policy version → create review child asynchronously → member attempts early completion and is denied → Leo completes assigned review → original deliverable can be completed. Daymark doesn't know this is a review process; it knows tasks, relationships, hooks, and permissions.

**Race/failure rules:** The deliverable is saved before its review child is created. If creation is pending, fails or has an UNKNOWN effect, the root remains open and completion fails closed until the native linked state is confirmed. Expose a useful pending/error/reconciliation status; do not automatically reissue a possibly committed create. Completion must recheck the child state and current authority in its native transaction, not rely solely on an earlier UI preview or stale read.

## 7. Executable process B: intake routing

Using **the exact same frozen host catalog**, author a separate bundle or test fixture: when a newly created high-priority root task enters a team project, assign it to a configured team member and set a due date derived from the event timestamp. The program reads the task and performs at most one atomic `tasks.update`; a nonmatching task gets no update.

This is different from review gating: it is conditional routing and property assignment, not generation of a related task or denial of completion. No native `routeTask`, `assignUrgent`, or `chooseReviewer` capability is permitted. Use only generic task operations.

A second business process must pass before treating the host capability catalog as meaningfully composable. Test both using identical catalog bytes/digest; do not add a process-specific capability to make the second pass.

## 8. Bundle, identity, lifecycle and version rules (Daymark-owned)

A **team-process bundle revision** is an immutable manifest of: project scope; human-readable intent and name; exact hook-to-artifact bindings; model/compiler/runtime identities and artifact digests; scenario-suite identities; any explicit parameters or reviewer identity; approved evidence reference; and bundle revision digest.

- One **proposal** groups all programs and their expected end-to-end behavior. The admin reviews it as **one business capability**, with individual program/effect details available for inspection.
- One explicit **approval** binds to the exact manifest revision and saved simulation evidence. Approval does not activate.
- One explicit **activation** updates a single project-level selected-bundle pointer/generation in a native transaction. No state may exist in which only half of the new bundle is active for a newly created task.
- For the first slice, **one active team bundle per project for new root tasks**. Older pinned revisions may still govern tasks that started under them. A second bundle can be tried in a separate project, or activated later while version-pinning is tested.
- **Version pinning:** On creating a governed root, Daymark records the selected bundle revision *in the same native transaction* as the task and its event admission. Subsequent events for that task use its original approved bundle revision, even after the admin activates a new revision for **new** tasks. Preexisting tasks with no pin are not retroactively governed. The event worker also executes the exact captured revision, never a later selection.
- **Deactivation:** Stops admitting new process instances; existing pinned instances continue under their captured version unless explicitly revoked. An administrative emergency revoke must prevent later effects and fail closed for covered gates, with an explicit audited native remediation path; never silently bypass a previously applicable gate. The first demo can keep the remediation UI minimal, but the semantics and tests must exist.
- Do not silently migrate open tasks between versions. No general instance-migration engine in this feature.

This is lifecycle composition, **not** Keir program-to-program invocation. A bundle does not make events across hours/days a single transaction.

## 9. Authority and security invariants

- Personal owner-scoped tasks and policies remain private and function as before. A TEAM project is shared **only** with explicit members; every read/search/link and native write checks project membership or the narrowly granted policy execution principal.
- Only the project `ADMIN` may propose/approve/activate/deactivate team bundles; members may perform permitted native task work but cannot change the team policy. AI-generated data is never a permission grant.
- Separate **human initiator**, **assigned worker**, and **project policy executor**. Policy-created tasks run under narrow project-scoped service authority, not by impersonating an administrator or giving every member admin rights. Record the initiating event and exact bundle/artifact in audit evidence.
- Every native mutation checks resource ownership, project membership for assignees/links, idempotency identity, payload match, native invariants and deadline at commit. Keir's compilation cannot substitute for these checks.
- Links cannot cross projects, create cycles, or be forged by passing untrusted IDs. Foreign objects must not leak through not-found or scenario details.
- Simulation never touches the saved Daymark database. Uncertain native effects remain uncertain until the host independently reconciles them.

## 10. Whole-process simulation and business-user review

Keep existing per-program Keir scenario semantics. Add a **Daymark-hosted sequence harness** that invokes the *real compiled artifacts* over one disposable, stateful simulated task store and produces one coherent multi-event report.

Each sequence contains explicit user/native operations, expected changes and decisions, synthetic host responses/faults, and assertions about task state, links, assignments, attempts, effect receipts and no unexpected side effects. Expected business outcomes must be separately authored and visible for human review; generating both implementation and expectations with AI is **not evidence** that the intent is correct.

Required scenarios:

1. Root task creates exactly one assigned, linked review task.
2. Early root completion is denied and leaves root open.
3. Review completed by authorized assignee; root completion then succeeds.
4. Policy-created review child does not recursively create another review.
5. Other personal/unbound project remains unaffected.
6. Child creation rejected, delayed or UNKNOWN: root remains protected; no hidden retry or false success.
7. Missing/invalid reference, unauthorized member, revoked program, stale source data, and failed/stale gate: no unauthorized native commit.
8. Switch bundle version between creation and completion: old root remains governed by its original bundle; new root uses new selection.
9. Intake routing: matching high-priority task is assigned/dated; nonmatching task stays unchanged.
10. Deliberately wrong expected result produces a visible failed simulation, not a pass.

The admin-facing summary should show a **timeline of example business events and observed effects**, not only disjoint unit-test reports. Each event links to the pinned program, calls and trace when inspected. State clearly that these are simulated examples, not proof of every real-world case. Approval identifies exactly which bundle and evidence were reviewed.

## 11. Lean delivery plan and stop gates

### Slice 0 — Architectural feasibility spike, **no production schema or UI changes**

Implement an isolated fake Daymark store and a *proposed* trusted DCM. Hand-author both A and B, compile with the **real** published Keir compiler, and invoke the **real** runtime on the resulting artifacts. Demonstrate sequence simulation and exact expected outcomes, including partial failure and version switch. Prove the same catalog admits both processes within Core limits.

**Deliverables:** two sources and compiled artifact pins; one frozen catalog; fake store/harness; independent sequence fixtures and report; exact diagnostics/performance caveats; a short decision record identifying any genuinely required Core change.

**Gate:** Stop if a program cannot be expressed/compiled within the current Core contract, if the catalog becomes review-specific, or if the second process requires modifying the catalog. Report the failed construct and the smallest alternative; do not automatically redesign Keir Core.

### Slice 1 — Native team-project foundation, no AI

Introduce minimal project membership, assignment, parent/child task support, generic child-status read, scoped domain adapters and `TaskBeforeComplete`. Preserve personal behavior. Add real native authorization/transaction/concurrency tests. Connect a manually assembled bundle to one seeded team project with two existing users. Keep one active bundle per project.

**Gate:** The review and routing scenarios work against real native services; personal regression suites pass; forbidden cross-project access and gate bypasses fail; no review-specific Daymark operation exists.

### Slice 2 — Coherent bundle simulation, approval and activation

Add Daymark-owned immutable bundle revisions, whole-process evidence, project-level approval and atomic activation, version capture on root creation, old-version routing, cancellation/revocation semantics and minimal operational status. Adapt existing Studio's summary/simulation/activation flow, rather than introducing a new UI platform. AI remains scripted/manual.

**Gate:** One proposal is reviewed and activated as one unit; partial activation, stale evidence, and mid-flight version switching cannot change governed behavior silently. The full tested sequence remains inspectable.

### Slice 3 — AI authoring and focused demo polish

Extend the authoring assistant's trusted context to the project catalog and bundle manifest. One admin request produces the program set, bounded scenarios and human-readable explanation. Use the real compiler and simulator, then require explicit human approval and activation. Reuse the normal task UI; add only a minimal child/assignee display and a clear completion-denied explanation.

**Gate:** A business user can request the review process, inspect/repair the proposed behavior, simulate it, approve/activate it and complete the real cross-user journey without an AI call during execution. Show intake routing against the same catalog. Do not represent scripted provider runs as evidence of live-model reliability.

## 12. Acceptance criteria / definition of done

- **Demonstrated novelty:** Two materially different business processes work using the same frozen Daymark DCM and native capability implementations. None is implemented as a domain-specific host action.
- **Core unchanged:** The initial implementation compiles/runs against existing Keir Core. Any proposed Core enhancement is separately justified by an actual failed test and stays out of this feature unless explicitly approved.
- **Personal preserved:** Existing Maya/Leo/Sam personal task scopes, owner-scoped rules, and their tests remain correct; shared team access is explicit, never accidental.
- **Single coherent process:** Review is represented by two separately bounded Keir programs but proposed, simulated, approved and activated as one versioned bundle.
- **Atomic boundaries:** Linked review creation is one native effect; bundled hook selection changes atomically; task completion rechecks current read facts and authority at commit. No cross-event transaction is claimed.
- **Stable version semantics:** Old tasks retain their originally selected bundle; replacement for new tasks does not rewrite in-flight behavior; emergency revocation fails safely.
- **Failures honest:** No review, failed/unknown child creation, unavailable gate or stale authority cannot yield an accidental success. No untracked retry after an uncertain write.
- **User validation:** End-to-end scenarios show expected and observed business outcomes; a mismatch blocks eligibility for approval. Simulation is clearly identified as synthetic.
- **Operational restraint:** No general UI builder, app framework, workflow engine, dynamic entities, unbounded lists or new Core-wide execution semantics are introduced.
- **Verification:** Current repository tests plus new unit/integration/browser tests pass; failures and omissions are reported explicitly, not hidden behind a generic PASS.

## 13. Decisions and unresolved issues for the implementing agent

**Decided for the first slice:** Project is the sharing/policy boundary; user-facing concept is a *team process*; bundle belongs to Daymark; composition is across events; review means completion of a linked task; personal mode stays intact; one active team bundle per project; exact version captured on root creation; no Core changes assumed; no new app-generation UI.

**To resolve in Slice 0 or early Slice 1, with written justification:** Exact native naming and cardinality for child links; whether assignment constraints require an admin override; data-model migrations that preserve preexisting personal rows; how to surface a stuck/unknown review creation without implying it can be retried; semantics of manual admin remediation following emergency revocation; ability to compile and type-check the proposed optional field/summary shapes. Keep choices minimal and domain-natural.

**Do not solve yet:** Multiple concurrent bundles and policy conflict ordering; general helpers/function calls; multi-mutation Keir transactions; distributed workflow orchestration; cross-project routing; general-purpose custom forms, attributes or entities; reusable Core bundle/simulator API; full production IAM/tenant model.

## 14. Implementation handoff instruction

Start with **Slice 0 only**. Inspect current repository state before editing. Create a short design note for the proposed DCM and generic task primitives, then build and run the smallest real-compiler/real-runtime fake-store experiment for both independent processes and the required failure paths. Record exact commands, compiler diagnostics, test results, model/artifact hashes, and gaps. **Do not implement team UI, database migrations, multi-program Core features, or a workflow engine until the feasibility gate passes and the next slice is approved.**
