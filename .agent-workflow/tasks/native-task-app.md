# Local task application foundation

Canonical task: `agent-workflow:native-task-app`. Owning feature: [native-task-app](../../roadmap/features/native-task-app.md).

<!-- agent-workflow:start -->
**Outcome:** Visitors can run a polished local web task app as any of three isolated demo users and retain their projects and tasks across restarts.
**Target:** keir-demo-app
**Scope:** Native web UI, local server/services/storage, seeded demo sign-in, per-user project/task create/edit/list/complete, setup/reset documentation and focused tests; core-owned task record and roadmap evidence.
**Constraints:** No Keir/AI activation or production identity platform; no global tooling/environment/service changes, paid calls, real credentials or live data migration. Preserve isolated user ownership, native validation, local persistence and a useful unextended app. Studio design artifacts are separately owned.
**Completion criteria:** From a clean checkout, documented setup/seed/start opens the local app; each of three users can manage projects/tasks, switch/sign out and restart without data loss or cross-user access. Direct API ownership, stale UI/pending-request boundaries and desktop/mobile empty/error states are verified with fast regression and an end-to-end browser journey.
**Requirement baseline:**
{"source":"https://github.com/rore/keir-core/blob/341448d/roadmap/features/native-task-app.md","outcome":"Visitors can run a polished local web task app as any of three isolated demo users and retain their projects and tasks across restarts.","scope":"Native web UI, local server/services/storage, seeded demo sign-in, per-user project/task create/edit/list/complete, setup/reset documentation and focused tests; core-owned task record and roadmap evidence.","constraints":"No Keir/AI activation or production identity platform; no global tooling/environment/service changes, paid calls, real credentials or live data migration. Preserve isolated user ownership, native validation, local persistence and a useful unextended app. Studio design artifacts are separately owned.","completion_criteria":"From a clean checkout, documented setup/seed/start opens the local app; each of three users can manage projects/tasks, switch/sign out and restart without data loss or cross-user access. Direct API ownership, stale UI/pending-request boundaries and desktop/mobile empty/error states are verified with fast regression and an end-to-end browser journey."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:native-task-app"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** New app paths have an unclassified gray floor; local persistence and service-enforced demo-user scope need independent review. No production credentials, shared service or existing customer data is changed.
**Discovery:** Initial repository is a scaffold; maintained feature and demo scope define requirements. Architecture consultation and implementation discovery are underway; no app implementation yet.
**Material assumptions:** Existing supported local tooling is usable without changing installed versions or global settings. If false, stop affected work and propose an isolated supported alternative. Demo users and data are fictional; no production authentication or live data migration is authorized.
**Plan:** Pending architecture/developer recommendations. Select a minimal supported local web stack and explicit user/session/storage contract, establish visual direction and tests, obtain clean-context technical plan review, then implement in an isolated assigned checkout. Review actual code/UI evidence before manager integration.
**Verification plan:** Clean setup/start/reset and restart persistence; fast native service/API validation and isolation tests; stale request/user-switch regression; browser create/edit/complete journey for seeded users; rendered desktop/mobile and empty/error review; fresh Redline/explicit record checks and public-content scan.
**Plan review:** Pending concrete plan and risk-appropriate review; implementation is not authorized at this checkpoint.
**Approvals:** Not required at this risk level. User authorized the described two-track start: "ok, so let's go" (2026-11-01); prototype UI review remains a separate future checkpoint.
**Exceptions:** —
**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

- Initial context recorded before implementation. Core owns this record; app artifacts and tests stay in demo. Direct-main integration remains manager-owned; no PR requirement.
- Baselines: core `341448d`, demo `0b47b3e`. No implementation delivered. Next action: complete discovery/plan and required review, then explicitly assign isolated implementation work.

## Evidence

Pending. No test, working UI, real AI or native guarantee is claimed.

## Result review

Pending implementation and verification.

