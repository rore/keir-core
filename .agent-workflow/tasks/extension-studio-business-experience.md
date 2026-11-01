# Studio for business users

Canonical task: `agent-workflow:extension-studio-business-experience`. Owning feature: [extension-studio-design](../../roadmap/features/extension-studio-design.md). Follow-up to [working experience](extension-studio-working-experience.md).

<!-- agent-workflow:start -->
**Outcome:** A nontechnical person can understand and use the Studio proposal through an agent-led conversation and a clear, polished visual preview.
**Target:** keir-demo-app
**Scope:** Demo docs/extension-studio-design.md and docs/extension-studio/ prototype sources/tests; this core Work Record.
**Constraints:** Preserve exact review, separate human approval/turn-on, immutable historical examples, current-versus-proposed behavior, interruption and user isolation. Scripted only; no live AI, native changes, dependencies or environment changes. Manager owns roadmap/integration/push. Study design principles without copying another interface.
**Completion criteria:** Main screen uses ordinary work language, readable text/contrast and clear status; chat guides the next decision and canvas shows familiar task outcomes. Existing safety/continuity checks pass, new plain-language paths are checked, and desktop/narrow/keyboard interaction is inspected. Remaining prototype limits and user review stay explicit.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"A nontechnical person can understand and use the Studio proposal through an agent-led conversation and a clear, polished visual preview.","scope":"Demo docs/extension-studio-design.md and docs/extension-studio/ prototype sources/tests; this core Work Record.","constraints":"Preserve exact review, separate human approval/turn-on, immutable historical examples, current-versus-proposed behavior, interruption and user isolation. Scripted only; no live AI, native changes, dependencies or environment changes. Manager owns roadmap/integration/push. Study design principles without copying another interface.","completion_criteria":"Main screen uses ordinary work language, readable text/contrast and clear status; chat guides the next decision and canvas shows familiar task outcomes. Existing safety/continuity checks pass, new plain-language paths are checked, and desktop/narrow/keyboard interaction is inspected. Remaining prototype limits and user review stay explicit."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}
**Risk:** Routine
**Complexity:** Moderate
**Reason:** All product paths are blue docs; record excluded. Connected conversation, presentation and tests require moderate complexity; no production authority changes.
**Discovery:** Prior proposal preserves continuity but exposes engineering vocabulary and small secondary text. Product agent guides place discovery, authoring and repair behind the customer experience. Design research and translations are recorded in the demo brief. Reuse existing state snapshots and transition guards.
**Material assumptions:** The immediate implementation scope is the Studio prototype. General product coherence informs its appearance; native app changes are outside this increment. User review, not aesthetic self-assessment, determines acceptance.
**Plan:** Commit this recovery record. Rework the whole Studio presentation: chat-dominant layout, clearer spacing/type/contrast, concise navigation, recognizable before/after task preview and ordinary-language statuses/actions. Rewrite agent replies and add bounded natural-language routes for visible suggestions; retain underlying identity and approval safeguards. Keep technical detail secondary. Update design brief, run focused checks and actual rendered interaction, then commit/handoff without push.
**Verification plan:**
- Nontechnical journey -> focused tests for visible plain-language prompts and their state transitions, preserving all prior safety tests.
- Clarity and visibility -> rendered desktop/narrow views, readable controls, status visibility, overflow and keyboard path.
- Boundaries -> exact review/turn-on and historical snapshot guards; native source unchanged.
- Scope and handoff -> fresh Redline, explicit Work Record validation, whitespace/privacy checks and scoped commits.
**Plan review:** Routine self-review: presentation and bounded scripted routes change, existing model and authority remain. Task-shaped preview replaces a technical diagram; user can ask for help or change intent throughout. No new abstraction or dependency needed.
**Approvals:** Not required at this risk level. User explicitly requested the whole look be reconsidered for usability and visibility, following the nontechnical agent-led direction. Production implementation remains out of scope.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Baselines: core `a5dc545`, demo `70a8583`; clean at pickup. Scope limited to the Studio proposal and this record.
- Prior research reused from the design brief; no repeated product audit, helper or worktree needed.
- Recovery plan committed as `73e953f` before product edits. The manager's intervening roadmap commit `ba08f29` is the actual core task parent; it is not part of this task's changes.
- Reworked the complete Studio presentation: wider conversation, quieter navigation, larger text, stronger contrast, familiar completed-task/follow-up preview and ordinary-language status, choices and review. Editing fields and technical identities are secondary disclosures. Reused the existing model and allowlisted server without dependencies.
- Added bounded routes for project scope, example checking, explaining/editing a preview, confirmation requests and continue. All-project requests explain the prototype limit without changing intent. Approval remains an explicit human control, followed by separate turn-on.

## Evidence

- Demo implementation: `f2513e7`, four reserved design/prototype files only, on main. Native source, dependencies, services and environment are unchanged. Prior design direction commit `70a8583` remains part of the handoff.
- `node --test docs/extension-studio/studio.test.mjs`: 28 checks pass, zero failures/skips, 0.193 seconds. Existing safety checks remain; three added checks cover plain-language scope and review, visible conversation prompts/pause/continue, and unsupported all-project intent. The canvas receipt assertion now checks the customer-facing word preview; the edit invalidation and current-setup assertions are unchanged. Both state-module and inline-browser-module syntax checks pass.
- Actual desktop browser: reviewed the task-shaped preview, progress and pause; selected Only Launch, inspected the review, confirmed both checkboxes with Space and approval with Enter; focus moved to the separate Turn this on control. Current setup stayed unchanged until that action. Earlier examples reopen their original contents without approval/turn-on controls. An unsent task-name edit remained intact while a new preview was offered.
- Actual 390-pixel narrow view: document width 375 pixels, no horizontal overflow; 15px conversation text and direct links between chat and preview. Rendered task preview inspected; temporary viewport override reset. Final desktop and narrow screenshots retained in ignored local evidence. No console errors were reported for the final task tab.
- Complete changed-path privacy and whitespace checks pass. Fresh core and demo Redline reports are BLUE, with no boundary violations. The core report excludes the Work Record as configured. No private paths or session details appear in tracked additions. Explicit Work Record validation is clean for both path sets, with no failed predicates, using the actual task parent `ba08f29`.

## Result review

Routine self-review at demo `f2513e7`: the bounded prototype meets the iteration criteria. Main decisions use work language, the canvas shows recognizable task outcomes, and the conversation guides choices without changing approval authority. Tests exercise real transition entry points; rendered checks cover visual hierarchy, keyboard review, narrow navigation and unsent-field preservation. No production abstraction or dependency was needed.

This remains a scripted proposal with tab-local history, one supported task-follow-up pattern and no live agent or real task effects. Full accessibility audit, production execution and business-user acceptance are not established. Native app tests were not rerun because native files did not change. User review determines acceptance of the visual direction; the broader feature remains open.

The designer has not pushed. Manager owns roadmap reconciliation, integration and delivery. No worktree was created. The existing preview server remains available without restart.

Manager review: inspected the actual rendered screen and verified exact four-file demo scope, clean checkouts, public commit identity, privacy/whitespace and the saved BLUE Redline/clean workflow reports for both path sets. The demo developer is reviewing only the changed chat routes, visible controls and review/status wording; earlier state-integrity review remains applicable to unchanged model behavior. User design acceptance remains pending.

Independent delta review found one P2 at `f2513e7`: broad turn-on matching intercepts explicit turn-off phrases such as "Turn off this automation" and "Turn it off in Operations", leaving the simulated setup on. Targeted comparison confirms the earlier proposal turned it off. The designer is assigned the narrow route/word-boundary correction and opposite-intent regression checks in the same task; delivery is held for fix verification. Other unchanged model/browser evidence remains applicable.

Designer correction: constrained the turn-on alternative to whole words, retaining the existing turn-off route and all other UI/model behavior. The regression failed against the reviewed implementation before the fix. After correction, both reported turn-off phrases disable the setup; a subsequent "Turn this on" opens review without approving or enabling it. The full prototype suite passes 29 checks, zero failures/skips, in 0.220 seconds. Whitespace check passes. No parser expansion, native test rerun or browser rerun was needed for this one-line route correction; existing rendered evidence remains applicable. Manager review paragraphs and pending roadmap edits were preserved. Narrow independent recheck and user visual acceptance remain pending.

Correction revision: demo `2982f18`, limited to the route and regression test. Refreshed complete-path Redline is BLUE and explicit workflow validation is clean for both core and demo, with no failed predicates. Changed-content privacy checks pass. The manager-owned roadmap edit was included in read-only qualification but is left uncommitted for its owner; this record retains the manager's review notes.

Narrow recheck at `2982f18` confirms the two original phrases are fixed but reproduces the same precedence defect for "Turn off the follow-up on Launch": standalone "on" still routes to review first. Resume the same correction with explicit turn-off handling before turn-on/review, whole-word matching and project-qualified opposite-intent cases. Delivery remains held; no new parser or UI scope is required.

Precedence correction: moved the existing explicit disable/turn-off route before turn-on/review and constrained disable, turn and off to whole words. Existing project/title edits retain their priority, so quoted task names remain editable. No new parser architecture or phrase exceptions were added. The expanded regression failed before correction for the project-qualified turn-off and for the word office. After correction, all four reported turn-off phrases and explicit disable switch the setup off. Positive turn-on requests still open review without approving/enabling; office, onboarding, return and disablement do not create unintended on/off intent. The full suite passes 30 tests, zero failures/skips, in 0.334 seconds. Browser/native reruns are omitted for this routing-only delta; prior unchanged evidence remains applicable. Manager's pending review paragraph is preserved; final independent delta review and user design acceptance remain pending.

Precedence revision: demo `33d5c4e`, only the existing route and focused tests. Fresh complete-path qualification is BLUE and workflow-clean for core and demo, without failed predicates. Changed-content privacy and whitespace checks pass. No push, UI, dependency, native source or environment changes.

Agent technical review: the independent demo developer approved the bounded correction at `33d5c4e` after the initial delta review and first recheck identified the turn-off collision. Explicit whole-word turn-off now precedes review/turn-on without changing approval authority or UI. No unresolved finding remains in the bounded review.

Reviewed revision: `33d5c4e4e4f25786b0fb0e8c87128b1b4ad5449c`, correction delta versus `2982f18`; earlier review covers the rest of the business-user iteration versus `b3d226b`.

Verification adequacy: the reviewer ran one targeted precedence regression (pass, zero failures, 150.9 ms including runner) and directly replayed the previously failing project-qualified request, confirming disabled state and the turn-off response. The reviewer inspected the whole-word cases and reused the designer's 30 passing checks and prior unchanged desktop/narrow/keyboard/state evidence. No broad browser/native suite rerun was needed. The reviewer ended its own association with an empty readback.

Delivery: demo `33d5c4e` is pushed to main. Manager reconciled the roadmap as ready for user review while keeping the broader feature active. Earlier references to pending manager edits describe intermediate checkpoints; those roadmap changes are committed. No task worktree requires cleanup. Preserve the existing preview for user interaction/visual review; production Studio implementation remains outside this contribution.
