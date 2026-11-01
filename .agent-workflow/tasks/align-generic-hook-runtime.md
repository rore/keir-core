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
**Discovery:** At planning base 85733de, reviewed target contracts existed in generic-hook-capability-contract.md, host-model-binding.md and generic-hook-example.md; code implemented only `/1`. The corrected DCM is 1,534 canonical bytes and uses generic tasks.create. Final implementation and verification are recorded below.
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
**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

- User requested implementation alignment after reviewing the tour and correcting ownership, model binding, task creation and documentation/process scope. Current main is 85733de; working tree was clean before creating this record.
- Implementation checkpoint: architect freeze and independent plan review are complete. Both core developers explicitly accepted their disjoint slices and verified clean isolated checkouts at 0120145; implementation work is underway. A starts the compiler/reference-fixture checkpoint, B starts Node API/binding work against the freeze. Historical checkouts stay intact. Runtime/host acceptance is still pending; compiler checkpoint results are recorded below.
- Canonical owning feature: [generic hook/capability correction](../../roadmap/features/generic-hook-capability-contract.md). Manager alone updates its shared roadmap state.
- Java/runtime/shared checkpoint: selected source `64a4ef7` integrated locally as `476c5f5`; the compiler checkpoint was already present and was not reapplied. This adds direct model binding, reference-program admission and fresh invocation, Java packaged hosts, nine read/nineteen effect scenarios and the additive combined verifier. Node confirmed adoption of the compiler dependency; the shared corpus was routed next. Independent source review and canonical verification are underway. No combined acceptance or push is claimed.

## Evidence

Final implementation: `933c5c8c2ac587d588a9ee83052905b9e6b2d024`, locally verified on 2026-10-31. All required local checks pass; final human result review and delivery remain pending.

| Check | Measured result |
| --- | --- |
| Clean Java coverage build | 216 tests: 58 compiler, 128 runtime, 30 host; zero failures/errors/skips; 52.470 seconds. Java source is unchanged by the final Node correction. |
| Default Node coverage | 367 tests; zero failures/skips; 4.175 seconds; 100% lines/97.73% branches. Includes legacy, generic shared rows and combined-invalid regressions. |
| Independent installed Node hosts | Three tests pass in 0.967 seconds; legacy read/effect and both generic batches use the installed package. |
| Complete cross-process verifier | 61 legacy read cases/52 invocations, 25 legacy effect scenarios, nine generic read/nineteen generic effect rows; sixteen fresh compiler processes/eight packaged-host batches. Full outcomes/calls/traces/effects/consumption match. |
| Package integrity | All seven Node source/archive/installed files equal; archive/metadata/lock SHA-512 matches; registry pins unchanged. 21,476 packed/79,140 unpacked bytes. All 51 packaged Java runtime/host classes match; compiler/parser exclusion and legacy manifest pass. |
| Compatibility | Existing `/1` golden bytes unchanged; explicit `/2` source/profile/reference fixtures deterministic. Project/title changes remain source-only; generic create arguments remain projectId/title. |

Coverage: compiler 848/893 lines and 724/835 branches; Java runtime 1117/1129 lines and 1043/1223 branches; Java host 490/494 lines and 678/784 branches. Coverage and simulation do not certify real authorization, native idempotency, remote cancellation, decoded-heap containment or production readiness. Effects remain simulation-only. Main-push [product CI](https://github.com/rore/keir-core/actions/runs/41280457363) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41280457420) pass on `55c0f27`.

Selected source deltas and attributable integration:

| Contribution | Selected source | Integrated revision |
| --- | --- | --- |
| Compiler/reference fixtures | `8008ff8` | `db8944f` |
| Java runtime/host/shared corpus | `64a4ef7` | `476c5f5` |
| Bounded context and exact LF/CRLF pins | `c04d6d1` | `8a2d390` |
| Java unknown-capability diagnostic | `2f2adcb` | `9999766` |
| Existing deadline-test preparation order | `73c736b` | `29bccb8` |
| Compiler-first envelope coverage | `bcf4cc8` | `8d484d0` |
| Node runtime/host | `62c7e06` | `95e4eb6` |
| Five Node review corrections | `4ea4de2` | `933c5c8` |

Each correction retains the triggering regression: malformed/cyclic/deep contexts; exact digest-line endings; unknown versus malformed capabilities; unchanged 150 ms late-receipt test; safety/policy-before-binding preflight; unused WRITE catalog selection without WRITE authority; approval leaf/format diagnostics; zero profile-getter execution. Worker bookkeeping was consolidated, not replayed. Earlier checkpoint evidence remains in this record's Git history.

Final governance/content checks: fresh full-task Redline is GRAY in shadow mode, with no boundary violation or required checkpoint. Explicit-identity Work Record validation is clean (exit 0), including committed baseline, approval/relationship preservation and result-review evidence. Changed documentation links, public-content scan, whitespace and public commit identity pass. Documentation reconciles measured support without changing runtime source, tests, CI wiring, installed skills or hooks.

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

Current record ownership: the manager alone maintains canonical requirements, immutable baseline, approval evidence, global State, integration and final acceptance. A and B supply separately attributable source revisions and component evidence; neither owns an independent workflow or delivery acceptance. The published [Workflow task-ownership clarification](https://github.com/rore/agent-workflow/blob/29c508a350942d2127c337655907fb02cc8cfdb0/docs/SPEC.md#6-work-record) supports this single managed task with parallel contributions. Independently progressing delivery tasks require distinct linked records; contributor/worktree count alone does not. Preserve this record and approvals; no migration or mandatory parent tracker is needed. Resolver identity does not override a branch-derived edit guard: task-named contributor branches are required if that guard is adopted. No hook or vendored-skill change is part of this feature.

## Result review

Agent technical review: independent clean-context non-implementer, final source and verification-adequacy review, 2026-10-31.
Reviewed revision: `933c5c8c2ac587d588a9ee83052905b9e6b2d024`.
Verification adequacy: **APPROVED**. All earlier Java and Node findings are resolved and covered by regressions. Reviewer inspected the final source, manager-produced coverage/package/parity evidence and reused unchanged prior reviews without rerunning passing suites. Exact model/invoker binding, narrowing, nominal provenance, fresh authority/native context, effect evidence and finite harness behavior satisfy the recorded local scope; `/1` compatibility is preserved. No remaining technical blocker was found.

Human result review: approved by user on 2026-11-01: "You can merge". This approves delivery of reviewed implementation `933c5c8` and reconciled documentation `18eee0a` on main under the existing no-PR instruction. Public status documentation describes measured experimental local support; no real-write or production guarantee is claimed.

Delivery: approved implementation and documentation pushed to main at `55c0f27`; product and workflow CI both passed. Both completed contributor worktrees were recoverably archived after verifying clean state, recovery references, a complete Git bundle and checksummed preservation of needed ignored evidence. The owning feature is complete; broader production and native-host qualification remain outside this task.
