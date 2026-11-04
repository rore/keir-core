# Keep Studio conversation clear and unobstructed

Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md).

<!-- agent-workflow:start -->
**Outcome:** A business user can understand the saved customization's status and keep chatting without an oversized persistent decision box obstructing the conversation.

**Target:** keir-demo-app

**Scope:** Studio decision presentation, conversation/composer layout, actual UI-control coaching and affected browser/interaction regression coverage.

**Constraints:** Preserve exact saved evidence, independent approval and explicit activation, stale-state and owner fences, history, unsent text and current runtime selection. No backend authority, compiler/runtime, capabilities, provider settings, Git configuration/history or agent-guide changes. Keep the existing local demo current after verified delivery.

**Completion criteria:** Pending review or activation is understandable and optional while chatting. Its controls do not permanently consume the conversation's viewport or force the user to act. The user can deliberately review and activate the exact saved change, continue discussion or revise it, and return without losing place/text. Desktop, phone and short-window conversation/composer remain usable, with no accidental approval or activation.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A business user can understand the saved customization's status and keep chatting without an oversized persistent decision box obstructing the conversation.","scope":"Studio decision presentation, conversation/composer layout, actual UI-control coaching and affected browser/interaction regression coverage.","constraints":"Preserve exact saved evidence, independent approval and explicit activation, stale-state and owner fences, history, unsent text and current runtime selection. No backend authority, compiler/runtime, capabilities, provider settings, Git configuration/history or agent-guide changes. Keep the existing local demo current after verified delivery.","completion_criteria":"Pending review or activation is understandable and optional while chatting. Its controls do not permanently consume the conversation's viewport or force the user to act. The user can deliberately review and activate the exact saved change, continue discussion or revise it, and return without losing place/text. Desktop, phone and short-window conversation/composer remain usable, with no accidental approval or activation."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Unclassified client/UI paths establish the Gray floor. Presentation changes span review, activation, recovery, focus and responsive layout, while preserving authority and saved identities.

**Discovery:** At demo 0b4f6d9, the current-decision article is a sibling outside the scrollable message log. Its nonshrinking layout and large maximum height can consume most of the conversation above the composer. An approved inactive draft keeps this article visible during unrelated chat. The existing compact status strip already reports draft/use state. The user's screenshot rejects this persistent presentation.

**Material assumptions:** The correction changes how and when decisions are presented, not whether approval and activation are required. Keep chat primary and use the accepted contextual canvas only when useful and understandable. Do not introduce another dashboard, forced wizard, detached prototype approval gate or backend state machine. Settle the bounded interaction design with the standing designer before implementation.

**Plan:** Obtain a focused designer recommendation on the actual screenshot and working source, then record the smallest interaction/layout plan and independent technical plan review. Implement in an isolated checkout, preserving the existing exact review/activation handlers and explicit user intent. Verify interaction and authority regression together, inspect actual rendered states, test a realistic business-user conversation when coaching changes, and fix discovered problems. Independently review the frozen result, run exact hosted verification, refresh the normal demo at its existing address with data preserved, and recoverably clean the task worktree. Manager owns this record/roadmap; one developer owns the coherent implementation.

**Verification plan:**
- Unobstructed ongoing conversation → actual browser journeys with approved/not-in-use and checked/not-approved saved work, unrelated questions, continued edits and reload.
- Deliberate exact decisions → existing/new browser assertions for explicit review, unchanged approval/activation identities, stale responses and no implicit mutation from navigation or chat.
- Continuity and responsive clarity → desktop, phone and short-window rendered checks for useful conversation space, visible composer/latest reply, keyboard/focus, history and unsent text through review/canvas navigation.
- Agent/UI agreement → focused coaching checks and a business-persona interaction against actual available controls; use existing approved provider testing safely if live calls are needed, preserving prior accounting.
- Delivery → affected fast regression, build, combined browser CI, independent result review and exact served-asset/data-preservation checks at the existing demo address.

**Plan review:** Pending bounded designer recommendation and independent technical plan review; no implementation released yet.

**Approvals:** The user rejects the persistent decision box as unclear and obstructive and requests a simple business-user experience. Existing authorization covers actual interaction/UX iterations and updating the normal demo after verification. No guide, Git identity/configuration or history change is part of this correction.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

The screenshot and source confirm the obstruction. Focused design and developer discovery are next; the current demo and customer state remain unchanged during planning.
