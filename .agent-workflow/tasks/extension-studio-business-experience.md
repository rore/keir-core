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

## Evidence

Pending implementation.

## Result review

Pending; broader feature and user design acceptance remain open.
