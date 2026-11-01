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
**Discovery:** Clean demo scaffold at 0b47b3e; architect and demo developer separately inspected current scope. Installed Node 24.14.1 can execute built-in SQLite; its versioned API is experimental, accepted explicitly for this isolated local demo. Fresh intended-path Redline is GRAY with no boundary violation or checkpoint. Developer proposed browser-native modules; manager selects the architect's small React/TypeScript/Vite and Fastify approach because the known major Studio UI will need the same component/testing foundation. No ORM, SSR framework or production identity stack is needed.
**Material assumptions:** Existing supported local tooling is usable without changing installed versions or global settings. If false, stop affected work and propose an isolated supported alternative. Demo users and data are fictional; no production authentication or live data migration is authorized.
**Plan:** Use a React/TypeScript/Vite frontend, small Fastify same-origin API/static host, parameterized node:sqlite services and opaque HttpOnly SameSite server sessions. Three seeded fictional identities and owner-qualified project/task queries; bounded fields and bodies, host/origin and per-session request context checks. Revalidate revoked sessions before writes after async input; admitted work remains under its original identity. Clear UI scope on switch, abort old work and ignore old-generation responses. Keep sessions in memory (restart requires sign-in), data in one ignored local DB, seed idempotently and reset only offline with explicit confirmation/backup. Package scripts provide clean setup/build/start, fast node:test and isolated Playwright journeys; no global changes. Target native server/service/session modules, frontend components/styles, package/lock/config, focused tests, setup/reset scripts and native docs. Stop on unknown native guarantees or scope change. Isolated worker commits source only; manager integrates selected commits on main and owns core record/roadmap. No Studio files or real Keir/AI integration in this task.
**Verification plan:**
- Clone/run and persistence -> pinned clean npm setup/build/start, three-user native journeys, restart against the same DB without duplicated seeds; explicit reset backup and active-server refusal.
- Native correctness and scope -> node:test/Fastify inject with real temporary SQLite for validation, missing/foreign IDs, project ownership, forged owner fields, transaction failure and current authorization.
- Switching boundary -> delayed body/response and old-session/new-cookie tests; frontend generation checks and browser user-switch/sign-out/refresh journeys, preserving original-scope writes already committed.
- Polish/accessibility -> actual desktop/mobile render and keyboard/detail-panel/empty/loading/error review plus focused Playwright journeys with test-owned process/port/DB and no reuse of a user's running app.
- Delivery integrity -> exact lock/runtime support, no global installation or credential use, complete task diff/privacy/commit-identity scan, fresh demo/core Redline and explicit Work Record validation, independent result review.
**Plan review:** Agent technical review: approved revised plan; see Plan review below. Five initial findings are resolved in demo docs/native-task-app-design.md at 6973a19. Independent confirmation covers scope, risk and verification adequacy; no further broad plan review is needed for unchanged implementation of that contract.
**Approvals:** Not required at this risk level. User authorized the described two-track start: "ok, so let's go" (2026-11-01); prototype UI review remains a separate future checkpoint.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Initial context recorded before implementation. Core owns this record; app artifacts and tests stay in demo. Direct-main integration remains manager-owned; no PR requirement.
- Baselines: core `341448d`, demo `0b47b3e`. No implementation delivered. Next action: complete discovery/plan and required review, then explicitly assign isolated implementation work.
- Discovery checkpoint: architect and developer findings reconciled into the concrete plan. Manager chose the shared React/Fastify foundation; the no-runtime-dependency alternative was considered but would require hand-maintaining transport handling and a separate future Studio UI foundation. Native scope and acceptance are unchanged. Plan review is pending; no app code has begun.
- Plan-review checkpoint: the demo-owned native design contract now specifies per-session request tokens matched to the live cookie, exact Host/Origin checks, serialized cookie-writing auth transitions, revalidation after async input, owner-consistent schema constraints, shared exclusive startup/reset lock with verified backup/transactional reset and desktop/mobile visual direction. Added verification cases refine the existing obligations; task-context baseline is unchanged. Await reviewer confirmation, then hand off the reserved isolated checkout.
- Implementation handoff: revised plan independently approved on 2026-11-01. Demo developer owns native app source/config/tests/docs in an isolated task checkout; manager owns this core record, canonical roadmap and separate Studio prototype files. Target paths follow the concrete plan and native design contract. No app code has been accepted yet.

## Plan review

Agent technical review: independent clean-context non-implementer reviewed the initial plan and the revised [native contract](https://github.com/rore/keir-demo-app/blob/6973a19/docs/native-task-app-design.md). All five findings were resolved: session-context matching, cookie/revocation races, data invariants, exclusive reset/backup, and native visual direction. Verdict: approve; Elevated/Moderate and the selected small stack are proportionate. Verification explicitly covers rejection/race/persistence/reset with isolated data and test-owned processes. Across-tab authentication serialization remains an implementation obligation; per-tab disabled buttons alone are insufficient. Review did not execute code or certify native guarantees.

## Evidence

Pending. No test, working UI, real AI or native guarantee is claimed.

## Result review

Pending implementation and verification.

