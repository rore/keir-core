# Studio working experience

Canonical task: `agent-workflow:extension-studio-working-experience`. Owning feature: [extension-studio-design](../../roadmap/features/extension-studio-design.md). Follow-up to [the prototype](extension-studio-prototype.md); the earlier assessment is reused.

<!-- agent-workflow:start -->
**Outcome:** The user can review a polished application-like Studio conversation and canvas through one realistic scripted working journey.
**Target:** keir-demo-app
**Scope:** Demo docs/extension-studio-design.md and docs/extension-studio/ prototype sources/tests; this core-owned Work Record only.
**Constraints:** Keep chat primary, canvas input explicit in chat, user scope visible and human approval separate from activation. Scripted examples only: no AI, external APIs, credentials, runtime or native mutations. Preserve native app, dependencies, services and environment. Manager owns roadmap, integration and push. Conversation lifetime and canvas attention are proposed assumptions, not accepted requirements.
**Completion criteria:** A runnable populated screen demonstrates clarification, behavior inspection, scenario mismatch, explicit correction and exact review; earlier evidence reopens its own revision, draft edits preserve active selection, and interrupted work can resume. Focused state tests and actual desktop, narrow and keyboard checks support a reviewable proposal with honest limits.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"The user can review a polished application-like Studio conversation and canvas through one realistic scripted working journey.","scope":"Demo docs/extension-studio-design.md and docs/extension-studio/ prototype sources/tests; this core-owned Work Record only.","constraints":"Keep chat primary, canvas input explicit in chat, user scope visible and human approval separate from activation. Scripted examples only: no AI, external APIs, credentials, runtime or native mutations. Preserve native app, dependencies, services and environment. Manager owns roadmap, integration and push. Conversation lifetime and canvas attention are proposed assumptions, not accepted requirements.","completion_criteria":"A runnable populated screen demonstrates clarification, behavior inspection, scenario mismatch, explicit correction and exact review; earlier evidence reopens its own revision, draft edits preserve active selection, and interrupted work can resume. Focused state tests and actual desktop, narrow and keyboard checks support a reviewable proposal with honest limits."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}
**Risk:** Routine
**Complexity:** Moderate
**Reason:** All product changes are blue documentation paths; the record is excluded by policy. Self-contained scripted state and several connected UI states require Moderate complexity. No production contract or persistence surface changes.
**Discovery:** Existing pure transition model and sixteen tests preserve approval, user scope, mismatch and active snapshots. Chat messages currently contain text only and canvas always renders the mutable current draft. Native app supplies project rail, green actions, user switcher and task vocabulary; oversized editorial headings are not carried forward. Existing server allowlists two assets and must remain running.
**Material assumptions:** One named customization conversation per example user and offering fresh evidence while an older view is held are explicit design proposals. User review may replace either. Existing tooling and loopback preview can support verification without global changes; tool failures limit only affected checks.
**Plan:** Commit recovery record first. Reuse the pure model and existing exports; add immutable evidence receipts, current versus held canvas selection, exact review guards and scripted operation pause/resume. Replace the prototype presentation with compact app chrome, legible sans-serif type, clear draft/active strip, continuous conversation and contextual working canvas. Seed a realistic continuing customization with an active version; retain reset and empty-user coverage. Update the UX document with proposal decisions, journey and limits. Add focused tests; inspect rendered desktop/narrow/keyboard behavior; commit scoped artifacts and hand off for manager/user review without push.
**Verification plan:**
- Continuity and authority -> existing tests plus exact evidence reopen, held-canvas offer, stale review guard, active snapshot preservation and user isolation tests.
- Interruption -> scripted running/paused/resumed lifecycle, reload recovery and stale completion checks.
- Application quality -> actual desktop and narrow rendering, no horizontal overflow, primary keyboard journey and honest prototype labeling.
- Scope/privacy -> fresh complete-path Redline, explicit Work Record check, whitespace and public-content inspection; native source unchanged.
**Plan review:** Routine self-review: reuse the model and two-asset server; keep evidence snapshots inspectable but historical controls read-only. No new dependencies or production abstractions. Proposal assumptions do not block this bounded exploration or constitute acceptance.
**Approvals:** Not required at this risk level. The manager assigned the concrete local prototype within the user-authorized interaction direction; user design review remains pending.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Initial recovery context: core main at `f7fa4ce`, demo main at `b6f2b03`; both clean at pickup. Scope reserved to this task. No worktree created.
- Classification directly applies the current policy: demo documentation paths BLUE, Work Record excluded; no boundary, contract, dependency or real-effect changes.
- Existing desktop assessment reused. Native working-screen source inspected for project/user structure and control vocabulary; native application stays unchanged.

## Evidence

Pending implementation and focused verification.

## Result review

Pending. This contribution will remain a proposal until actual user interaction review; manager retains feature reconciliation.
