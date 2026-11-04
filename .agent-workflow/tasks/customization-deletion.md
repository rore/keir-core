# Delete a customer customization

Owning feature: [extension approval lifecycle](../../roadmap/features/extension-approval-lifecycle.md).

<!-- agent-workflow:start -->
**Outcome:** A signed-in demo user can delete an unwanted customization through a clear confirmed action.

**Target:** keir-demo-app

**Scope:** Customization deletion across storage, owner-scoped lifecycle/authoring APIs, the customization list and affected callers, documentation and regression tests.

**Constraints:** Preserve native tasks, other users and unrelated selections; retain audit history, operation identity and all spending/unknown-dispatch accounting. No arbitrary code, new Keir capability, provider call or global environment change. Use existing project tooling and update the existing local demo after verified delivery.

**Completion criteria:** Confirmed deletion removes the item from normal use; deleting the selected item turns it off atomically. Cancellation changes nothing. Foreign ownership, stale confirmation and active authoring fail safely. Saved links, replayed operations, later results, restore and restart cannot resurrect deleted work. Current data migrates without loss if storage changes are necessary.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A signed-in demo user can delete an unwanted customization through a clear confirmed action.","scope":"Customization deletion across storage, owner-scoped lifecycle/authoring APIs, the customization list and affected callers, documentation and regression tests.","constraints":"Preserve native tasks, other users and unrelated selections; retain audit history, operation identity and all spending/unknown-dispatch accounting. No arbitrary code, new Keir capability, provider call or global environment change. Use existing project tooling and update the existing local demo after verified delivery.","completion_criteria":"Confirmed deletion removes the item from normal use; deleting the selected item turns it off atomically. Cancellation changes nothing. Foreign ownership, stale confirmation and active authoring fail safely. Saved links, replayed operations, later results, restore and restart cannot resurrect deleted work. Current data migrates without loss if storage changes are necessary."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-approval-lifecycle"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Unclassified demo storage/API/UI paths establish the Gray risk floor. Deletion changes selection and availability while durable replay and spending records must remain intact.

**Discovery:** Current demo revision 20fb2e4 has collection editing and exact lifecycle selection but no customization delete endpoint or UI. Database schema 6 retains linked revisions, conversations, exact operation receipts and dispatch accounting. Core semantics are unaffected.

**Material assumptions:** Deletion means removing a customization from normal customer use while retaining internal audit/accounting, rather than physically purging immutable records. Prefer rejecting deletion during running authoring with a clear Stop-first explanation; do not introduce asynchronous cancellation or refund logic. Inspect all lifecycle/authoring lookup and replay paths before settling the storage mechanism.

**Plan:** Obtain bounded architectural advice on the smallest deletion contract, then independent clean-context technical review of the concrete plan. Implement owner-scoped confirmed logical deletion using existing conventions, atomic selection handling and stale-state fences; cover all readers, mutators, replay/restore and late-result paths. Add meaningful fast tests and focused real-service browser journeys. Review the exact result, integrate on main, verify hosted CI, back up and update the existing demo, then recoverably clean the task worktree. The manager owns this record and roadmap; one developer owns the coherent implementation.

**Verification plan:**
- Confirmed/cancelled deletion and selected-item behavior → service/API assertions plus browser confirmation, list disappearance and native behavior checks.
- Owner and stale-state safety → cross-owner, stale revision/generation, other selected item and running-authoring regressions.
- Durable deletion → replayed creation/turn receipts, direct links, restore, late result and reopen tests prove no resurrection or duplicate spending.
- Data preservation → if a migration is needed, consistent snapshot rehearsal compares native/history/accounting records, integrity and foreign keys; qualified rollout compares the live result with the expected migration.
- Delivery and usability → production build, affected fast regression, focused keyboard/browser walkthrough, independent result review and exact served-asset verification at the existing demo address.

**Plan review:** Pending concrete architecture advice and independent clean-context technical plan review. No product implementation released yet.

**Approvals:** The user reports that a customization cannot be deleted and authorizes fixing it through the ongoing development task. Standing direction authorizes keeping the normal local demo current after verified changes, including routine restarts and preserving migrations. No physical audit purge, accounting reset or unrelated environment change is authorized.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

Discovery confirms the reported missing behavior. A bounded architecture offer and developer planning offer are pending; concrete deletion, concurrency and replay semantics must be recorded before implementation. This planning dependency does not require human approval.
