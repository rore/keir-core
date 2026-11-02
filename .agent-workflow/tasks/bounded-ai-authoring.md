# Bounded AI authoring in Studio

Canonical task: `agent-workflow:bounded-ai-authoring`. Owning feature: [Conversational authoring](../../roadmap/features/conversational-authoring.md). Dependency: [Authored drafts](authored-extension-drafts.md); provider/tool modules may be planned independently, but integrated authoring uses the accepted real pipeline.

<!-- agent-workflow:start -->
**Outcome:** A Daymark user can describe a supported read-only customization in Studio chat, clarify it, generate and repair real source and independently review scenarios, then use the existing human review and activation path.
**Target:** keir-demo-app
**Scope:** Host-owned bounded agent/tool loop, at least two explicit provider adapters, server-side local provider/model configuration, owner-scoped conversation and lifecycle tool integration, minimal Studio progress/clarification/recovery interactions, deterministic provider regression, opt-in live qualification and operator documentation.
**Constraints:** Use the authored-draft compiler/simulation/lifecycle tools and fixed host-approved read/2 domain; no generated application code, host catalog edits, write effects or agent approval/activation authority. Preserve per-user session/generation and stale-response guards, exact evidence, manual/native operation without AI, cancellation honesty and reading continuity. Keys stay server-side and out of logs/artifacts. Bound calls, turns, bytes, concurrency and spend; use fake transports by default and do not make unbounded paid calls or change global tooling.
**Completion criteria:** A real provider-driven request can clarify intent, submit newly authored source through the actual compiler, repair a rejected candidate, propose independently reviewable expectations and simulate an exact draft; only the user can approve and activate it. Deterministic fixtures exercise two supported provider protocols, invalid tool arguments, injection, provider/credential failures, cancellation, stale owner/draft changes and exhausted budgets. Configuration absence leaves native and manual workflows usable. Actual live provider/model checks are opt-in, separately reported and bounded; no fake qualification is presented as live success. The integrated Studio journey passes independent technical and rendered review, affected fast/browser regression and hosted CI.
**Requirement baseline:**
{"source":"Direct user approval of overnight authored-extension and AI delivery, 2026-11-02","outcome":"A Daymark user can describe a supported read-only customization in Studio chat, clarify it, generate and repair real source and independently review scenarios, then use the existing human review and activation path.","scope":"Host-owned bounded agent/tool loop, at least two explicit provider adapters, server-side local provider/model configuration, owner-scoped conversation and lifecycle tool integration, minimal Studio progress/clarification/recovery interactions, deterministic provider regression, opt-in live qualification and operator documentation.","constraints":"Use the authored-draft compiler/simulation/lifecycle tools and fixed host-approved read/2 domain; no generated application code, host catalog edits, write effects or agent approval/activation authority. Preserve per-user session/generation and stale-response guards, exact evidence, manual/native operation without AI, cancellation honesty and reading continuity. Keys stay server-side and out of logs/artifacts. Bound calls, turns, bytes, concurrency and spend; use fake transports by default and do not make unbounded paid calls or change global tooling.","completion_criteria":"A real provider-driven request can clarify intent, submit newly authored source through the actual compiler, repair a rejected candidate, propose independently reviewable expectations and simulate an exact draft; only the user can approve and activate it. Deterministic fixtures exercise two supported provider protocols, invalid tool arguments, injection, provider/credential failures, cancellation, stale owner/draft changes and exhausted budgets. Configuration absence leaves native and manual workflows usable. Actual live provider/model checks are opt-in, separately reported and bounded; no fake qualification is presented as live success. The integrated Studio journey passes independent technical and rendered review, affected fast/browser regression and hosted CI."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** Provisional classification for untrusted provider responses, scoped tool dispatch and asynchronous UI integration. Review concrete paths, credential handling and resource/spend boundaries before implementation; raise classification if warranted.
**Discovery:** Read-only provider/tool discovery offered. No implementation or provider compatibility result exists yet.
**Material assumptions:** Existing supported source/compiler/simulation contracts can be exposed as narrow host tools and two provider protocols can support them without a new agent platform. The authored-draft integration must preserve exact host authority; unresolved dependency or provider mismatch returns affected work to planning.
**Plan:** Plan bounded loop, tool protocol, provider transport/configuration, owner/cancellation fencing and minimal Studio changes. Assign independent file ownership and review the concrete plan before code. Do not treat a module-only result as the completed integrated journey.
**Verification plan:** Independently predicted deterministic provider/tool conversations with real compiler/lifecycle integration; two adapter protocol fixtures; invalid and forbidden actions, injection, quotas/cancel/races; missing configuration; real Studio browser journey, regression, independent review and bounded opt-in live checks when available.
**Plan review:** Pending independent clean-context technical review.
**Approvals:** Direct user authorized the presented overnight AI slice and necessary delivery. No high-risk concrete-plan approval is asserted by this preliminary record.
**Exceptions:** —
**State:** Blocked
<!-- agent-workflow:end -->

## Implementation

Planning only. Provider/tool discovery is separate from authored-draft storage implementation. Manager assigns isolated source/record checkouts before writes and reserves shared files; the record owner coordinates contributions and integration.

## User authorization

Direct user instruction, 2026-11-02: "do those and if possible and we have another possible slice continue to it also. when finished give a status update with what we have. blanket approval to do all that is needed is given". This follows the presented authored-draft and AI-authoring plan. No unrelated environment change or unlimited paid usage is implied.

## Evidence

No new implementation, live provider success or end-to-end delivery claimed.

## Result review

Pending implementation and independent review.

