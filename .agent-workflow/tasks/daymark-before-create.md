# daymark-before-create

Owning feature: [task-create-guard](../../roadmap/features/task-create-guard.md).

<!-- agent-workflow:start -->
**Outcome:**
Daymark can block invalid new tasks using customer-authored before-save rules while preserving the person's unsaved details.

**Target:**
keir-demo-app

**Scope:**
Daymark shared create coordinator, approved DCM and local READ adapters, both native/API and automation creation paths, exact selection/replay/consistency checks, Studio authoring/simulation/review and native pre-save details, tests and release integration.

**Constraints:**
No Daymark semantics in Core. Preserve native limits, owner isolation, permissions, event suppression and existing settled receipts. No guard writes or asynchronous work inside native transactions. No blind retry after unknown commit. Preserve existing saved work and provider settings.

**Completion criteria:**
All three requested rules compile and demonstrate allowed, denied, nonmatching and unavailable cases. Denied/blocked UI/API/internal creates persist neither task nor TaskCreated event. Corrected explicit saves create once. All entered fields and newer edits survive rejection. Existing event rules and approvals remain intact; desktop/narrow/short UI and running-demo delivery pass.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Daymark can block invalid new tasks using customer-authored before-save rules while preserving the person's unsaved details.","scope":"Daymark shared create coordinator, approved DCM and local READ adapters, both native/API and automation creation paths, exact selection/replay/consistency checks, Studio authoring/simulation/review and native pre-save details, tests and release integration.","constraints":"No Daymark semantics in Core. Preserve native limits, owner isolation, permissions, event suppression and existing settled receipts. No guard writes or asynchronous work inside native transactions. No blind retry after unknown commit. Preserve existing saved work and provider settings.","completion_criteria":"All three requested rules compile and demonstrate allowed, denied, nonmatching and unavailable cases. Denied/blocked UI/API/internal creates persist neither task nor TaskCreated event. Corrected explicit saves create once. All entered fields and newer edits survive rejection. Existing event rules and approvals remain intact; desktop/narrow/short UI and running-demo delivery pass."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:task-create-guard"}],"pull_requests":[]}

**Risk:**
Elevated

**Complexity:**
Large

**Reason:**
Unclassified contract and persistence integration paths require independent review; separately delivered runtime and host behavior must agree.

**Discovery:**
The host audit identified createTask and createTaskOnce sharing private insertion but separate synchronous transactions, title-only quick-add and absent interactive replay identity. The designer supplied a retained-fields creation form and inline rule/simulation proposal.

**Material assumptions:**
Existing invocation and bounded type machinery are sufficient; if implementation requires a new semantic/API contract, stop affected work for architect review while continuing independent work. Host local READs can be revalidated inside its short transaction; otherwise they cannot justify an authoritative ALLOW.

**Plan:**
Use the independently reviewed Core and host contract. Implement one guarded create coordinator reached by both paths, immutable normalized input, selection/read-fact/native authority rechecks at commit, parent deadlines and truthful receipts. Add pre-save details and plain-text denial messages; preserve approval/activation separation. Settle minimal lost-response readback before retries. Run native persistence/race tests, authoring flow and actual rendered QA before official packages and live update.

**Verification plan:**
No persistence or bypass -> real service/API/adapter tests asserting task/event/receipt rows. Races and replay -> paused evaluation, separate DB connection, selection/permission/read changes and concurrent same-attempt calls. Three rules -> actual compiled artifact simulations and native allow/deny. UX -> denied-corrected-saved walkthrough at desktop/320px/683x384 with setup/unread/composer, retained newer input and owner changes. Delivery -> qualified release pins, backup/migration rehearsal and served asset/native checks.

**Plan review:**
Agent technical review: independent Core/host-seam review passed design 9d2e593dc2215181593d467873e92cd0117bf402; manager inspected and accepted the designer's retained-form and inline simulation direction. The host native identity/readback contract remains a bounded dependency: implement approved UI, projection and coordinator structure independently, but do not enable unknown-save retry or claim recovery until the architect's focused design and review close it.

Independent manager review subsequently accepted the focused host protocol at 1c0f6c34249dad1d7d6f2c2085c664129abd2e93: owner-scoped exact-candidate replay, atomic task/event/terminal result, absent readback as unresolved, current access checks, historical receipt versus current task rendering, and actual reset/session handling. Implementation of this bounded protocol is authorized; its persistence, race and browser verification remains required.

**Approvals:**
User authorized this slice, generic framework separation, proper QA and completion. No new environment changes or public visibility change are authorized.

**State:**
Ready for review
<!-- agent-workflow:end -->

## Implementation

Planning initialized before semantic implementation. Final design review resolves the implementation dependency; this is not a new user approval request.

The final unpublished implementation candidate is integrated from its exact 83-file manifest. The integration owner checked every file hash, confirmed the manifest covers the entire change set, installed dependencies offline and passed the build/typecheck. Its browser bundle digest matches the independently reviewed rendered candidate. Candidate compiler/runtime/guide provenance remains explicitly unpublished; this is not an official asset or rollout claim.

All three requested rules compile, simulate, approve, activate and demonstrate native denial/correction using real Core 0.3 components. Regression coverage includes existing approved generation-2 event execution alongside a generation-3 guard, expected self-change after a negative existence read, independent fact changes, permission/selection/deadline/cancellation failures and preserved native input boundaries. The owner reports 513 Node tests passed, 13 documented default skips, all 12 explicit process tests passed, and 229 unique browser cases passed across completed split runs. After the final display-only heading correction, all 16 focused real guard/Studio tests passed again. The integration owner inspected the summary, final logs and real guard test paths and reused that evidence rather than rerunning the full suites.

Independent native-form and combined Studio presentation reviews are accepted. Both native recovery findings and the technical example-heading finding are closed on exact source hashes. Reviews covered desktop, 320-pixel and short-screen layouts, retained input/focus, separate approval and activation, mixed check/action collections and truthful decision-versus-save results. The final heading preserves exact identifiers in Details.

A bounded configured-provider authoring check is prepared for an isolated database with a small test-local spending limit. Automatic approval review rejected credential use and billable provider calls without direct human authorization; no credential was read and no request or charge occurred. The integration owner holds one explicit approval request for that scope. Live AI authoring is not qualified by the scripted-provider/browser evidence. Final official package/guide pins, actual release smoke, preserved-data rollout and running-demo verification remain open.

## Result review

Final acceptance is complete at Daymark source `7ad6d36`. The user approved both pending scopes and subsequently removed the validation-only cost limit. All three real-provider journeys compiled, simulated and produced eligible review offers; an additional final journey passed on the official compiler, runtime and guide. Native data and human decisions were unchanged during those isolated checks.

The integration owner independently verified every file in the final 85-file manifest and reviewed the official package, compiler, guide and lock pins. The published Core release downloads and guide archive matched their qualified hashes. The added prompt deduplication preserves authoritative host navigation, approval/activation boundaries and all requested examples; both the original seven-example repair and a longer user request pass below the unchanged context ceiling. Focused authoring, native, distribution and browser results are accepted. The [full hosted run](https://github.com/rore/keir-demo-app/actions/runs/42579777651) then passed 515 Node tests, 12 documented opt-in skips and all 229 browser cases.

The approved local rollout is verified: the actual served browser bytes match the reviewed build, all 21 original database tables retain their data after schema 13 to 14, integrity and foreign-key checks pass, and provider settings are unchanged. Backups and the previous app are retained after a successful restore rehearsal. The designer's independent read-only post-rollout review passed all three viewport sizes; the integration owner inspected its report and representative screenshots. No automatic activation or public repository visibility change occurred. This closes the implementation, package, live-authoring and rollout dependencies.

