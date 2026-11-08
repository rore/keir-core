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
Core final review and host native operation identity/recovery decision remain pending.

**Approvals:**
User authorized this slice, generic framework separation, proper QA and completion. No new environment changes or public visibility change are authorized.

**State:**
Blocked
<!-- agent-workflow:end -->

## Implementation

Planning initialized before semantic implementation. Final design review resolves the implementation dependency; this is not a new user approval request.

