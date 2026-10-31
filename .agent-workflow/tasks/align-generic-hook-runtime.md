# Align the implementation with the reviewed generic-extension design

<!-- agent-workflow:start -->
**Outcome:** The compiler, Java/Node runtimes and runnable hosts execute the corrected generic-extension scenario using the same reference artifact and host-bound model.
**Target:** Keir core compiler, runtimes and shared conformance.
**Scope:** Implement reviewed `/2` profiles, optional narrowing policy, authored nominal constants, exact shared DCM/invoker binding, reference artifacts, generic task creation, fixtures/hosts and affected documentation.
**Constraints:** Preserve `/1` APIs and goldens, default/hard maximum one WRITE, fresh host authority, protected original key/event context and honest effects. No real-write qualification, multi-write, AI authoring/activation service, demo application, registry service, global environment changes or PR requirement.
**Completion criteria:** Compile the corrected task program once and run identical bytes in Java and Node with a complete bound DCM/invoker catalog; project/title choices stay in source; tasks.create remains generic; shared positive/negative conformance and existing fast regression pass; specs/examples accurately describe measured support.
**Requirement baseline:**
{"source":"roadmap/features/generic-hook-capability-contract.md@85733de","outcome":"The compiler, Java/Node runtimes and runnable hosts execute the corrected generic-extension scenario using the same reference artifact and host-bound model.","scope":"Implement reviewed `/2` profiles, optional narrowing policy, authored nominal constants, exact shared DCM/invoker binding, reference artifacts, generic task creation, fixtures/hosts and affected documentation.","constraints":"Preserve `/1` APIs and goldens, default/hard maximum one WRITE, fresh host authority, protected original key/event context and honest effects. No real-write qualification, multi-write, AI authoring/activation service, demo application, registry service, global environment changes or PR requirement.","completion_criteria":"Compile the corrected task program once and run identical bytes in Java and Node with a complete bound DCM/invoker catalog; project/title choices stay in source; tasks.create remains generic; shared positive/negative conformance and existing fast regression pass; specs/examples accurately describe measured support."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:generic-hook-capability-contract"}],"pull_requests":[]}
**Risk:** High
**Complexity:** Large
**Reason:** Artifact/public API and authority-boundary changes span compiler and two runtimes. Redline gray paths set an Elevated floor; host authority and execution safety justify High.
**Discovery:** Reviewed target contracts exist in generic-hook-capability-contract.md, host-model-binding.md and generic-hook-example.md. Current code still implements only `/1`; the corrected DCM is 1,534 canonical bytes and uses generic tasks.create. No implementation has started in this task.
**Material assumptions:** The reviewed contract is sufficient for bounded implementation without new product semantics; architect must identify gaps before code. Existing isolated Java/Node tooling and regression evidence can be reused; do not change everyday installations. Developers must verify current assignments before accepting isolated work.
**Plan:** Implement the [technical freeze](../../docs/internal/generic-runtime-alignment-plan.md) without product redesign. A owns compiler/Java/Java host and all shared conformance; B owns Node runtime/host. Deliver compiler/reference fixtures first while Node begins API/schema unit work, then complete both runtime/host paths using existing safety algorithms. Preserve legacy APIs/goldens, explicit profile dispatch and fresh host authority. Manager integrates selected commits on main, runs focused then integrated fast regression/conformance/coverage/packaged-host verification, obtains independent result review and reconciles docs/feature state. Stop on contract conflict or uncovered product decisions; no developer independently changes the frozen wire/API contract.
**Verification plan:**
- Corrected scenario runs end to end -> same canonical artifact/model pair through compiler, both runtimes and packaged hosts; CREATED/NO_OP/rejection/uncertainty traces and create arguments match.
- Authority and model binding remain sound -> missing/wrong/extra invokers, digest/environment/scope/version mismatch, registration mutation, current revocation, optional-policy widening, nominal forgery and resource-limit negative cases.
- Customer ownership stays in source -> change project/title without changing hook/DCM/host code; generic creation arguments exclude follow-up-specific fields.
- Compatibility remains stable -> all `/1` golden bytes and fast compiler/Java/Node/shared regressions pass.
- Evidence and review are adequate -> independent technical plan/result review, recorded user scope authorization, current Work Record, public-content checks and exact delivery revisions.
**Plan review:** Agent technical review: independent clean-context plan review in Plan review below, approved at 4a3af9d. Architect gap review and technical freeze found no new product decision; implementation may proceed within the recorded approved scope.
**Approvals:** Approved by user 2026-10-31: "ok, so can we now align what deveoped so far?" Scope is implementation of the already reviewed design and user-corrected tour at revision 85733de; this does not approve new behavior or contract departures. Any uncovered product decision requires renewed approval before dependent implementation.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- User requested implementation alignment after reviewing the tour and correcting ownership, model binding, task creation and documentation/process scope. Current main is 85733de; working tree was clean before creating this record.
- Planning checkpoint: architect completed the bounded gap review and technical freeze; both core developers confirmed availability and no competing assignment. Fresh isolated code checkouts are prepared; historical checkouts stay intact. Independent plan review approved the frozen plan. Exact A/B implementation offers have been sent against the reviewed base; acceptance/start evidence is pending. No compiler/runtime edit is claimed yet.
- Canonical owning feature: [generic hook/capability correction](../../roadmap/features/generic-hook-capability-contract.md). Manager alone updates its shared roadmap state.

## Evidence

Pending implementation. Existing `/1` results are regression input, not proof of `/2` support. A focused baseline check independently verified the artifact/model digests and model contents of the read, two-read and single-effect golden fixtures. No build or fixture edit was needed; complete regression will run after implementation.

## Plan review

Architect reviewed the implementation seams and published contracts at 65a0cf4. It found no new product decision and retained High/Large classification. This design-author assessment does not replace the required independent clean-context plan review.

Proposed disjoint ownership:

- Developer A: compiler-java, runtime-java, Java host and all shared conformance fixtures, scenario formats, generated artifacts and verification scripts. Deliver the compiler/reference fixture checkpoint first. New generated fixtures belong under conformance/generic-hooks; legacy goldens remain unchanged.
- Developer B: runtime-node and Node host, including package metadata and installed-package checks. Consume A's exact fixtures and request shared changes from A; do not independently generate conformance goldens.
- Manager: canonical Work Record, roadmap, documentation/status reconciliation, selected-commit integration and combined verification/review.

Sequence: freeze exact Java/Node API wrappers, closed fields, diagnostic precedence and data-only scenario formats; obtain independent plan review; deliver compiler/fixture checkpoint while Node starts contract-based unit work; implement both bound-model/plan-validation/execution paths by reusing existing safety algorithms; update packaged hosts and shared verifier; run focused then combined fast regression and independent result review. Every `/2` read/effect profile must pass, not only the task example.

The [technical appendix](../../docs/internal/generic-runtime-alignment-plan.md) now fixes Java/Node signatures and wrappers, closed host records, diagnostic ordering, shared fixture schema/IDs, exact model pins and finite host-harness budgets without a runtime registry or revocation API. Manager made the shared/effect fixture applicability table explicit from the architect's profile intent. Fresh authorizers enforce revocation; existing approved snapshots remain usable. Independent review found no blocking appendix/contract conflict and approved implementation under the existing user authorization.

Agent technical review: independent clean-context implementation-plan reviewer, 2026-10-31.
Reviewed revision: 4a3af9d076e7dd67d3ea56f96b23211b8948b2d1.
Verification adequacy: approved after inspecting this record, the appendix, three published contracts, testing guidance, compiler/Java/Node seams and existing runners/verifier. Required component negatives remain part of acceptance: policy/nominal forgery, registration mutation, scopes/model coexistence, revocation, capacity boundary/+1 and late effect evidence. No new product decision or required technical fix was identified. Implementation correctness and measured enforcement remain for result review.

Worker slices share this record. Each records its own implementation/evidence in its isolated copy and reports selected code commits separately from record bookkeeping; manager consolidates the canonical copy. Global State stays Ready to implement until integrated verification passes; a finished slice records its completion and remaining dependency in prose. Final acceptance requires independent technical result review and the High-risk human result review of the concrete tested outcome.

## Result review

Pending independent review of integrated implementation and execution evidence.
