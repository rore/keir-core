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
**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

- Initial recovery context: core main at `f7fa4ce`, demo main at `b6f2b03`; both clean at pickup. Scope reserved to this task. No worktree created.
- Classification directly applies the current policy: demo documentation paths BLUE, Work Record excluded; no boundary, contract, dependency or real-effect changes.
- Existing desktop assessment reused. Native working-screen source inspected for project/user structure and control vocabulary; native application stays unchanged.
- Recovery plan committed as `3a582c3` before prototype changes. Implementation reuses the two-asset server and pure state model; no dependencies or production files changed.
- Implemented compact application shell, populated continuing conversation, visible draft/active selection, immutable evidence receipts and held-canvas offers. Unsubmitted field values survive arriving results. Current-review guards reject stale controls; keyboard approval moves focus to the separate activation control.
- Added scripted delayed run, pause/resume, reload interruption, draft supersession and user-switch interruption. Late results cannot cross users or apply to a changed draft. Production cancellation and durable history remain outside this increment.
- Verification checkpoint: focused Node suite and actual desktop/narrow interaction checks are in progress; public design document records assumptions and the representative journey. Existing isolated workflow tooling was located and reused without installation or configuration changes.

## Evidence

- Demo implementation: `b3d226b` on main. Only the four reserved documentation/prototype files changed; the existing allowlisted server and native source are unchanged. Core recovery commit `3a582c3` preceded all product edits.
- `node --test docs/extension-studio/studio.test.mjs`: 25 checks pass, zero failures/skips, 0.228 seconds including the runner. Existing sixteen checks remain. Added checks cover immutable evidence, held-canvas offers, stale review, separate activation, paused/late completion, reload, user isolation and interruption-state accuracy. The sandbox blocked the loopback test connection; unchanged checks passed with isolated loopback access. Inline browser module and state-module syntax checks pass.
- Actual Chrome desktop walkthrough: populated clarification; running -> paused -> resumed mismatch; unsent canvas title preserved while fresh evidence is offered; explicit expectation correction; exact review; keyboard Space/Tab/Enter through both confirmations and approval, then focus on separate activation; active r2 remains selected while draft r3 changes. Reopening r1 evidence shows its original title/results with no approval control. Reload during a run displays Interrupted and requires Resume.
- Actual 390-pixel narrow layout: 375-pixel document content width, no horizontal overflow; both working areas remain available with keyboard-focusable links between them. Temporary viewport override reset. Saved desktop screenshot is retained in ignored task evidence. One browser asynchronous-listener channel error was observed without an application stack or failed interaction; these checks do not diagnose browser extensions or constitute a full accessibility audit.
- Fresh complete-path Redline reports BLUE for demo and core record, no boundary violations. Changed-content privacy and whitespace checks pass. No private paths or routing details were added to tracked content. Explicit Work Record validation is clean for both path sets, with no failed predicates. The core task diff starts at `c3cbead7`, the parent of the recovery commit; that earlier manager-owned roadmap update is excluded from this task's changes.

## Result review

Routine self-review at demo `b3d226b`: completion criteria are met for this bounded scripted proposal. Existing behavior tests remain intact; new checks exercise actual transition entry points, and rendered checks cover state/focus continuity that pure model tests cannot establish. Historical evidence remains inspectable without granting current approval authority. Two public assets and the existing transition model remain sufficient; no dependency or production abstraction was added.

The conversation lifetime and canvas attention behavior remain proposal assumptions. Production provider operations, durable history, real execution, full accessibility and user design acceptance remain unverified. Native app, runtime semantics and environment were untouched. No changes were pushed. Manager owns roadmap reconciliation, integration and user review; the feature remains active. No worktree was created or requires cleanup. The existing preview service is retained without restart for review.
