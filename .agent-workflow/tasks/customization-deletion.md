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

**Discovery:** Current demo revision 20fb2e4 has collection editing and exact lifecycle selection but no customization delete endpoint or UI. Database schema 6 retains linked revisions, conversations, exact operation receipts and dispatch accounting. Architecture review identifies early receipt returns and global conversation lists that bypass item-state checks, plus capture/eligibility reads that bypass the draft lookup. Raw internal conversation access must remain available to settle accounting. Existing positional extension inserts need explicit column lists after an additive column migration. Core semantics are unaffected.

**Material assumptions:** Deletion means removing a customization from normal customer use while retaining internal audit/accounting, rather than physically purging immutable records. Prefer rejecting deletion during running authoring with a clear Stop-first explanation; do not introduce asynchronous cancellation or refund logic. Inspect all lifecycle/authoring lookup and replay paths before settling the storage mechanism.

**Plan:**
1. Add schema 7 with nullable nonnegative deleted_at on extensions. Retain item identity/creation uniqueness, revision/conversation links, content, proof, approvals, history, operation receipts, messages, dispatches and held accounting. Use explicit insert columns at post-migration writers. No trash browser, undelete API, physical purge or new deletion-operation registry.
2. Add an owner/session/origin-checked exact-body delete mutation with expected name, draft version and selection generation from the confirmation snapshot. Compare the legacy-safe exact name and versions before first mutation. In one synchronous transaction, reject running/cancelling or tracked unsettled work for this item; if its revision is selected, use the existing disable transition exactly once; mark the item deleted. Leave unrelated selection/generation untouched. A repeated owner-scoped delete returns the stable item/deletion receipt before obsolete version checks, without another event or spending. Refresh current collection/selection separately from that receipt.
3. Centralize item liveness in existing lifecycle lookups and commit-time checks. Filter normal collection/conversation lists before page limits, preserve tombstones as valid cursor anchors, and explicitly fence capture/eligibility/revision-only paths. Reject public old links, authoring admission/dispatch and late manual saves. Keep internal raw-row access for settlement and immutable accounting. Running authoring must Stop and settle before Delete; do not create automatic cancellation, deletion queues or paid replay.
4. Preserve exact creation/turn receipt identity and mismatched-operation conflict behavior. Replays and recovery for a tombstoned linked item return an explicit minimal owner-scoped CUSTOMIZATION_DELETED outcome rather than an actionable conversation or a replacement item. New explicit creation with a new operation is allowed. Frontend recovery clears only the matching local pending entry on terminal deletion, offers return to the collection and never retries or creates automatically. Legacy ambiguous unlinked discussions retain their current read-only behavior.
5. Add a simple confirmed Delete action to each customization card. Identify the item and explain that deletion turns it off when selected; Cancel sends no mutation. On success refresh selection/list, preserve pagination/owner/lifetime fences and move focus sensibly. Busy feedback points to the existing discussion/Stop; stale state requires refresh and confirmation again. Keep the existing application visual conventions and add no AI deletion tool.
6. Obtain independent clean-context technical plan review, implement in the accepted isolated checkout, verify the affected fast suite and focused real-service browser journeys, then review the exact result. Integrate on main, verify hosted CI, rehearse any migration on a current snapshot, back up and update the existing demo, and recoverably clean the task worktree. Manager owns this record/roadmap; one developer owns the coherent implementation.

**Verification plan:**
- Confirmed/cancelled deletion and selected-item behavior → service/API assertions plus browser confirmation, list disappearance and native behavior checks.
- Owner and stale-state safety → cross-owner, stale revision/generation, other selected item and running-authoring regressions.
- Durable deletion → replayed creation/turn receipts, direct links, restore, late result and reopen tests prove no resurrection or duplicate spending.
- Race and paging behavior → held provider/compiler/simulator checks cover both delete/admission orders, Stop-but-unsettled denial, late-commit rejection, unchanged UNKNOWN holds and deleted pagination anchors.
- Data preservation → if a migration is needed, consistent snapshot rehearsal compares native/history/accounting records, integrity and foreign keys; qualified rollout compares the live result with the expected migration.
- Delivery and usability → production build, affected fast regression, focused keyboard/browser walkthrough, independent result review and exact served-asset verification at the existing demo address.

**Plan review:** Architecture contract advice is accepted against 20fb2e4 and incorporated above. Independent clean-context technical plan review is pending; no product implementation released yet.

**Approvals:** The user reports that a customization cannot be deleted and authorizes fixing it through the ongoing development task. Standing direction authorizes keeping the normal local demo current after verified changes, including routine restarts and preserving migrations. No physical audit purge, accounting reset or unrelated environment change is authorized.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

Discovery confirms the reported missing behavior. The developer accepts planning/discovery with verified role and feature participation; the architect provides the concrete additive-storage, atomic-selection and terminal-receipt contract above. Independent plan review remains before implementation. This planning dependency does not require human approval.
