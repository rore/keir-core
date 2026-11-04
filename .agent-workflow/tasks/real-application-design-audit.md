# Real application design audit

This record owns the user-requested audit and durable architect guidance, not implementation of the proposed runtime/application corrections. The manager owns the two affected roadmap items and subsequent implementation release.

<!-- agent-workflow:start -->
**Outcome:** Reconcile the real-application design with the project's guiding principles and deliver a concrete correction plan to the manager before further affected implementation.
**Target:** Keir architectural contracts and architect guidance.
**Scope:** Read baseline/current contracts and relevant core/demo implementation; write docs/internal/real-application-design-audit.md and a concise baseline-check rule in roles/architect.md; coordinate the hold and correction plan.
**Constraints:** No product code, public API, migration, installed tool, application, settings or data changes. Preserve useful safeguards and accepted scope reductions. Manager alone edits canonical roadmap and releases implementation. No invented verification results or private operational details in tracked content.
**Completion criteria:** Findings cite baseline and current evidence, distinguish errors from legitimate host obligations and accepted limits, specify fixes/owners/order/acceptance, reach the manager, and persist a source-linked architect check. Review the report and guidance for unsupported conclusions and privacy.
**Requirement baseline:**
{"source":"direct-user-request","outcome":"Reconcile the real-application design with the project's guiding principles and deliver a concrete correction plan to the manager before further affected implementation.","scope":"Read baseline/current contracts and relevant core/demo implementation; write docs/internal/real-application-design-audit.md and a concise baseline-check rule in roles/architect.md; coordinate the hold and correction plan.","constraints":"No product code, public API, migration, installed tool, application, settings or data changes. Preserve useful safeguards and accepted scope reductions. Manager alone edits canonical roadmap and releases implementation. No invented verification results or private operational details in tracked content.","completion_criteria":"Findings cite baseline and current evidence, distinguish errors from legitimate host obligations and accepted limits, specify fixes/owners/order/acceptance, reach the manager, and persist a source-linked architect check. Review the report and guidance for unsupported conclusions and privacy."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:real-effect-admission"},{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:demo-task-integration"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** The report is proposed documentation, but roles/architect.md is an unclassified guidance path (gray floor). Cross-repository architectural conclusions require source-backed review; no runtime behavior changes in this task.
**Discovery:** Baseline architecture sections 3-4, DCM sections 3/6, host integration sections 1/5/11/12 and idempotency sections 3-7 require host-supported guarantees while keeping storage/transport host-owned. Current real-effect spec and both runtimes impose a closed deployment evidence record. Demo qualification hashes installed files and runtime/database tuple; dispatcher binds/prepares per event. Existing host-model-binding already requires current host authorization/revocation. Product assistant guides retain outdated simulation-only status.
**Material assumptions:** Historical baseline guides principles, not automatic restoration of superseded Java-later/key-derivation/example requirements. Current decisions and user corrections must be reconciled explicitly. Tests of implementation correctness do not establish proportionality. If manager provides newer source, qualify or revise affected findings.
**Plan:** Audit source ownership boundaries and trace qualification through core/demo. Write a proposed report with retain/remove/simplify/defer distinctions, bounded corrective phases and behavioral acceptance. Amend only architect guidance with source-linked pre-design checks: host versus core ownership, untrusted author versus trusted host, semantic pins versus deployment mechanism, prepared reuse, and explicit traceability for additions. Do not rewrite normative APIs now. Send findings and hold request to manager; send concrete coordination lesson to designated reviewer. Obtain independent plan/result review for this guidance/report change.
**Verification plan:**
- Source-backed findings and bounded fixes -> independent read-only baseline/current-code comparison.
- Durable guidance and readable report -> inspect relative links, diff and source references.
- Public privacy -> scan all task files for local paths, identities and session references.
- Valid recovery record and classification -> Work Record checker and fresh Redline task-path report.
- No executable-change claims -> report that runtime suites were not run for documentation/guidance edits.
**Plan review:** Agent technical review: independent audit_plan_review at f845fbe passes this bounded plan with a wording correction preserving declared contracts, semantic pins, current host approval and enforcement. Correction adopted in guide/report. Reviewer confirmed Elevated/Moderate and documentation checks; no runtime changes approved.
**Approvals:** User explicitly requested baseline audit, concrete correction plan, manager hold and durable guiding principles. No live changes or runtime correction implementation authorized by this record.
**State:** Ready for review
<!-- agent-workflow:end -->

## Durable guide change

The source-linked check is now in roles/architect.md. It preserves declared contracts, semantic pins, current host approval and enforcement while assigning implementation and evidence of native guarantees to the host. It requires necessity/ownership review before turning a demo mechanism into a core prerequisite. The report remains proposed, not accepted runtime policy.

## Progress

Audit completed against baseline/current contracts and selected connected implementation. Manager explicitly adopted the hold and sent it to both developers; worker-specific adoption is manager-owned. Concrete report sent to manager. Only this record, the report and architect guide changed. Core main initially inspected at f845fbe; demo main f02932b and integration source based at aa2126a inspected read-only. No product changes or tests executed; live applications/data/settings untouched.

## Evidence

Local relative-link target and private-content scans pass for all three task files. Git diff whitespace check passes. Fresh Redline task-path report is GRAY solely for roles/architect.md, with no boundary/API/schema/security/runtime-config changes; shadow-mode review warning (exit 1) is covered by the Elevated classification and independent reviews. Executable behavior tests are not applicable to these documentation/guidance changes. Final Work Record validation is clean (exit 0, no failed predicates) after correcting a verification-mapping formatting advisory. Coordination feedback sent to the designated reviewer; manager receives the final report and owns acceptance/resumption.

## Result review

Agent technical review: independent audit_plan_review passes the report and architect guide. It separately inspected the demo preparation path, execution-build hashing and native service qualification dependencies. No substantive blocker; its recovery-note correction is applied here.

Reviewed revision: task working-tree report and architect guide after the source-preserving plan correction, based on core f845fbe.

Verification adequacy: source comparison, link/privacy/diff checks and workflow validation suffice for this report/guide change. This is not approval to remove runtime checks; the concrete corrected contract, compatibility and equivalent enforcement require review before implementation resumes.
