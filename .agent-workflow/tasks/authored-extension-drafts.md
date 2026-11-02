# Authored extension drafts

Canonical task: `agent-workflow:authored-extension-drafts`. Owning feature: [Approval lifecycle](../../roadmap/features/extension-approval-lifecycle.md). This delivers the source-authoring foundation for [conversational authoring](../../roadmap/features/conversational-authoring.md).

<!-- agent-workflow:start -->
**Outcome:** A customer-authored read-only Keir program can be compiled, retained, simulated, explicitly approved and activated for one Daymark user, with its behavior visible on saved tasks.
**Target:** keir-demo-app
**Scope:** Demo-owned source compilation integration using an independently packaged compiler, immutable authored revisions and dependency identities, independently reviewable scenario expectations, existing owner-scoped lifecycle and task-read integration, minimal Studio affordances, regression tests and usage documentation.
**Constraints:** Keep the fixed host-owned DCM, invokers, permissions and supported read/2 language; source cannot change host authority. Preserve prepared examples, native task data and unsaved edits, owner/session/generation fencing, exact evidence, separate human approval and activation, disable/restore and honest uncertainty. No write effects, generated application code, new core semantics, SDK extraction, global tool changes or unbounded AI spend. Use isolated test data and keep public artifacts free of private operational details.
**Completion criteria:** A newly authored program beyond selecting the two prepared templates follows source -> actual compiler -> immutable revision -> independent scenario expectations -> actual simulation -> exact approval -> activation -> saved-task behavior; it survives restart and remains owner-isolated. Invalid or unsupported source yields safe diagnostics without an eligible artifact. A mismatching scenario prevents approval; editing requires new exact evidence and approval. Existing prepared, native, lifecycle, late-response and responsive interaction regressions pass. Independently reviewed source is integrated and pushed with reproducible local setup and hosted qualification.
**Requirement baseline:**
{"source":"Direct user approval of overnight authored-extension and AI delivery, 2026-11-02","outcome":"A customer-authored read-only Keir program can be compiled, retained, simulated, explicitly approved and activated for one Daymark user, with its behavior visible on saved tasks.","scope":"Demo-owned source compilation integration using an independently packaged compiler, immutable authored revisions and dependency identities, independently reviewable scenario expectations, existing owner-scoped lifecycle and task-read integration, minimal Studio affordances, regression tests and usage documentation.","constraints":"Keep the fixed host-owned DCM, invokers, permissions and supported read/2 language; source cannot change host authority. Preserve prepared examples, native task data and unsaved edits, owner/session/generation fencing, exact evidence, separate human approval and activation, disable/restore and honest uncertainty. No write effects, generated application code, new core semantics, SDK extraction, global tool changes or unbounded AI spend. Use isolated test data and keep public artifacts free of private operational details.","completion_criteria":"A newly authored program beyond selecting the two prepared templates follows source -> actual compiler -> immutable revision -> independent scenario expectations -> actual simulation -> exact approval -> activation -> saved-task behavior; it survives restart and remains owner-isolated. Invalid or unsupported source yields safe diagnostics without an eligible artifact. A mismatching scenario prevents approval; editing requires new exact evidence and approval. Existing prepared, native, lifecycle, late-response and responsive interaction regressions pass. Independently reviewed source is integrated and pushed with reproducible local setup and hosted qualification."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-approval-lifecycle"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** Provisional classification: coordinated compiler subprocess, immutable source/artifact storage, scoped lifecycle and UI changes require meaningful verification and independent review. Classify the complete intended paths before implementation and raise risk if discovery warrants.
**Discovery:** Not started. Inspect the accepted application and current contracts; architecture and compiler packaging discovery are offered separately as read-only assistance.
**Material assumptions:** Existing read/2 compiler and both runtimes can admit newly authored programs without language changes. Host-owned scenario authority and independent expectations can be preserved. Disproof returns the affected plan to the task owner before implementation.
**Plan:** Planning only. Resolve compiler distribution/invocation, persisted authored identities, simulation expectations and minimal UI; enumerate exact paths and resource limits. Obtain independent technical plan review before implementation.
**Verification plan:** Plan actual compiler integration and safety checks; source/artifact/owner/revision binding; failed scenario and approval invalidation; restart/migration and prepared compatibility; real saved-task invocation; fast regression plus explicit browser journeys. No checks have run for new behavior.
**Plan review:** Pending independent clean-context technical review of the concrete plan.
**Approvals:** User authorized these overnight slices and necessary delivery in the manager conversation. No high-risk concrete-plan approval is asserted by this preliminary record.
**Exceptions:** —
**State:** Blocked
<!-- agent-workflow:end -->

## Implementation

Planning dependency only: settle the concrete architecture and plan; no source changes authorized before the required technical review. Manager owns canonical roadmap and integration. Developer planning ownership is handed off only after the initial baseline is committed.

## User authorization

Direct user instruction, 2026-11-02: "do those and if possible and we have another possible slice continue to it also. when finished give a status update with what we have. blanket approval to do all that is needed is given". Scope refers to the presented authored-draft and AI-authoring slices, followed by a useful additional slice if feasible. Preserve the user's environment and credentials; no unrelated installation or data changes are implied.

## Evidence

No new implementation, test pass, provider qualification or delivery is claimed.

## Result review

Pending implementation and independent review.

