# Extension Studio experience design and prototype

Canonical task: `agent-workflow:extension-studio-prototype`. Owning feature: [extension-studio-design](../../roadmap/features/extension-studio-design.md).

<!-- agent-workflow:start -->
**Outcome:** The user can review a polished interactive Studio prototype showing a complete customization journey and recovery paths before Studio UI implementation.
**Target:** keir-demo-app
**Scope:** Demo-owned Studio UX specification, screen/state inventory, self-contained interactive prototype and bounded interaction checks; core-owned task record and roadmap evidence.
**Constraints:** Prototype actions are explicitly scripted/simulated; no real AI calls, runtime certification, native task mutations or credentials. Preserve per-user draft/active concepts, independent simulation expectations and exact human approval. Do not implement the production Studio or alter native app files.
**Completion criteria:** A runnable prototype demonstrates intent/clarification/behavior review/scenarios/simulation/exact approval and activation/visible illustrative task behavior, plus failure recovery, revision/disable/rollback and different user customizations. The UX specification explains navigation, accessibility, setup/failure states and technical seams; the actual prototype is ready for user review, with unresolved feedback retained.
**Requirement baseline:**
{"source":"https://github.com/rore/keir-core/blob/341448d/roadmap/features/extension-studio-design.md","outcome":"The user can review a polished interactive Studio prototype showing a complete customization journey and recovery paths before Studio UI implementation.","scope":"Demo-owned Studio UX specification, screen/state inventory, self-contained interactive prototype and bounded interaction checks; core-owned task record and roadmap evidence.","constraints":"Prototype actions are explicitly scripted/simulated; no real AI calls, runtime certification, native task mutations or credentials. Preserve per-user draft/active concepts, independent simulation expectations and exact human approval. Do not implement the production Studio or alter native app files.","completion_criteria":"A runnable prototype demonstrates intent/clarification/behavior review/scenarios/simulation/exact approval and activation/visible illustrative task behavior, plus failure recovery, revision/disable/rollback and different user customizations. The UX specification explains navigation, accessibility, setup/failure states and technical seams; the actual prototype is ready for user review, with unresolved feedback retained."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}
**Risk:** Routine
**Complexity:** Moderate
**Reason:** Self-contained illustrative UX artifacts with no external calls or real effects live under documentation paths; multiple states and interaction checks make complexity Moderate.
**Discovery:** Demo remains a scaffold. Current authoring/lifecycle contracts separate source intent, compiled behavior, independently reviewed scenario expectations and exact human activation; live provider and real-effect admission are future work. Fresh intended-path Redline reports BLUE with no checkpoints. The prototype has no backend or provider dependency.
**Material assumptions:** Existing supported local tooling is usable without changing installed versions or global settings. If false, stop affected work and propose an isolated supported alternative. Demo users and data are fictional; no production authentication or live data migration is authorized.
**Plan:** Define journey and state/screen inventory against current contracts; consult architect on authority/outcome seams; create representative visual direction and a self-contained local prototype under demo docs with no backend coupling; verify interaction transitions and actual browser rendering; present the prototype for user review. Production Studio UI waits for that review.
**Verification plan:** Assert interaction transitions and revision/approval invalidation; inspect actual desktop/mobile renders, keyboard navigation and representative error/setup states; walk both user scopes and draft/active revisions through the scripted journey; check no network/provider keys or real activation claims; record user review as pending until received.
**Plan review:** Self-review for Routine risk: the plan preserves explicit scripted status, isolates all prototype files under docs/extension-studio and provides transition checks plus actual browser review. User approval of the experience remains pending and is required before production Studio UI implementation.
**Approvals:** Not required at this risk level. User authorized the described two-track start: "ok, so let's go" (2026-11-01); prototype UI review remains a separate future checkpoint.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Initial context recorded before implementation. Core owns this record; app artifacts and tests stay in demo. Direct-main integration remains manager-owned; no PR requirement.
- Baselines: core `341448d`, demo `0b47b3e`. Prototype targets: `docs/extension-studio-design.md`, `docs/extension-studio/index.html`, `docs/extension-studio/studio.mjs`, `docs/extension-studio/studio.test.mjs`. These are manager-owned and disjoint from native app implementation. Use a small pure state model with Node transition tests and a static browser UI; no dependencies or production component API are introduced.

## Evidence

Pending. No test, working UI, real AI or native guarantee is claimed.

## Result review

Pending implementation and verification.

