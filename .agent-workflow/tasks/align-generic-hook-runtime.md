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
**Plan:** Obtain architect technical review of the published target and exact implementation order/file ownership; reuse accepted design rather than redesigning it. Freeze cross-runtime fixture/API details, then split independent compiler/Java and Node work between the two core developers, with shared fixtures owned explicitly. Integrate selected commits on main, run focused checks then combined fast regression/conformance and packaged hosts; obtain independent result review and reconcile the feature/docs. Detailed allocation and review evidence will be recorded before implementation.
**Verification plan:**
- Corrected scenario runs end to end -> same canonical artifact/model pair through compiler, both runtimes and packaged hosts; CREATED/NO_OP/rejection/uncertainty traces and create arguments match.
- Authority and model binding remain sound -> missing/wrong/extra invokers, digest/environment/scope/version mismatch, registration mutation, current revocation, optional-policy widening, nominal forgery and resource-limit negative cases.
- Customer ownership stays in source -> change project/title without changing hook/DCM/host code; generic creation arguments exclude follow-up-specific fields.
- Compatibility remains stable -> all `/1` golden bytes and fast compiler/Java/Node/shared regressions pass.
- Evidence and review are adequate -> independent technical plan/result review, recorded user scope authorization, current Work Record, public-content checks and exact delivery revisions.
**Plan review:** Pending independent architect implementation-plan review against the accepted contracts.
**Approvals:** Approved by user 2026-10-31: "ok, so can we now align what deveoped so far?" Scope is implementation of the already reviewed design and user-corrected tour at revision 85733de; this does not approve new behavior or contract departures. Any uncovered product decision requires renewed approval before dependent implementation.
**Exceptions:** —
**State:** Blocked
<!-- agent-workflow:end -->

## Implementation

- User requested implementation alignment after reviewing the tour and correcting ownership, model binding, task creation and documentation/process scope. Current main is 85733de; working tree was clean before creating this record.
- Planning checkpoint: architect offer and developer availability checks are being prepared. No compiler/runtime edit or implementation assignment has begun.
- Canonical owning feature: [generic hook/capability correction](../../roadmap/features/generic-hook-capability-contract.md). Manager alone updates its shared roadmap state.

## Evidence

Pending implementation. Existing `/1` results are regression input, not proof of `/2` support.

## Plan review

Pending architect assessment of the implementation plan, risk, verification and any uncovered decisions.

## Result review

Pending independent review of integrated implementation and execution evidence.
