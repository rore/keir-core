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

**Plan review:** Agent technical review: independent clean-context plan review passes core 604158c against demo 20fb2e4 with no required corrections. It confirms the additive tombstone, existing selection transition, early terminal receipt handling, raw settlement separation, cursor anchors and migration/reset coverage are the smallest complete approach. Architecture contract advice is accepted and incorporated above. Implementation is released within this plan; final result review remains required.

**Approvals:** The user reports that a customization cannot be deleted and authorizes fixing it through the ongoing development task. Standing direction authorizes keeping the normal local demo current after verified changes, including routine restarts and preserving migrations. No physical audit purge, accounting reset or unrelated environment change is authorized.

**Exceptions:** —

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Discovery confirms the reported missing behavior. The developer accepts the assignment with verified role and feature participation; architecture advice and independent clean-context plan review pass. Implementation is released under the recorded plan, including reachable terminal recovery even if the preliminary item-scoped list request fails. A consistent isolated schema-6 snapshot passes integrity and foreign-key checks for all 18 tables; credentials are not copied and the running demo is unchanged. No new human approval is required for implementation or the verified local demo update.

Implementation is in progress in the accepted isolated checkout. Target files are demo `src/data.mjs`, `src/extension-lifecycle.mjs`, `src/authoring-service.mjs`, `src/app.mjs`, `src/web/Customizations.tsx`, `src/web/Studio.tsx`, `src/web/main.tsx`, `src/web/styles.css`, affected lifecycle/authoring docs and deletion storage/lifecycle/API/authoring/browser regressions plus existing schema-version fixtures. The developer owns backend integration and bounded same-task frontend/storage contributions under this record. The developer reports 27 passing storage cases covering fresh databases, populated schema-6 migration, reopen, rollback and reset backup with records and UNKNOWN holds retained; combined verification and independent result review are still pending.

Backend, storage, API, replay and race implementation now passes the developer's full fast checkpoint: 264 passes, one expected platform-specific skip, zero failures in about 12.3 seconds; production build passes. Fourteen focused browser cases pass, including ten deletion and four existing recovery cases; an additional all-UI real compiler/runtime creation-to-activation-to-deletion journey is being finalized before the source is frozen. An intermediate test-fixture synchronization stall was isolated and corrected before the final passing run; no live app or provider was touched. Exact source identity, independent result review, hosted CI and normal rollout remain pending.

## Evidence

Frozen candidate: demo `8c314a68bf0286006b5c6adbc860afc150f6d41b`, based on `20fb2e4`, with clean tracked source and generic public attribution. The final fast suite passes 264 tests with one expected Windows platform skip and zero failures; type checking and production build pass. Eleven deletion browser cases pass, including the actual no-AI guided create/check/approve/use/delete journey with the real compiler/runtime; four existing recovery cases also pass. Twenty-seven storage/migration checks pass. Busy browser feedback is modeled; actual busy/settlement and both race orders are covered by service/API tests. Exact build assets, logs and desktop/mobile captures are preserved. Independent result review passes below. [Hosted verification](https://github.com/rore/keir-demo-app/actions/runs/41825769466) and normal delivery remain pending; no live data or provider settings have changed.

## Result review

Agent technical review: Independent non-implementer review passes the exact candidate against the accepted plan, including connected readers/mutators, receipts, settlement, paging, migration and UI recovery, with no required corrections.

Reviewed revision: 0b4f6d9060c95a458824db84cac3e7e52a65c671

Verification adequacy: The fast/API/storage/race suite, actual compiler/runtime browser journey, existing recovery checks and independently rehearsed current-data migration adequately cover this contribution. All 18 original tables and held accounting are preserved in the migration rehearsal, with null initial tombstones, idempotent reopen, clean integrity/foreign keys and zero provider calls. A separate actual compiled-UI check verifies the named selected-item confirmation, turn-off warning, Cancel and returned focus on an isolated copy. It does not substitute for the automated deletion execution or claim fresh human visual acceptance. Code is integrated and pushed; hosted combined CI and the normal backed-up rollout remain pending.

The copied UI's 18 tables remain identical after confirmation and Cancel. Final shared Workflow checking has zero blocking predicates; its commit-order advisory is checked against the actual histories: this core record was created at `6e0eb02`, before demo implementation `8c314a6`. No policy exception or weakened check is used.

The first hosted combined run fails two existing browser cases, with 129 passing and two failing; build and fast checks pass. One assertion reads the newly introduced actions wrapper instead of the containing customization article. Another recovery fixture waits for the old preliminary index request, while the corrected recovery path now checks the exact operation receipt first. The developer is released for the bounded existing-test corrections, preserving the original selection, blocked-retry, same-operation and unsent-text assertions. No application behavior, timeout or retry setting is weakened. Final acceptance, normal rollout and worktree cleanup are held until the corrected exact revision passes review and hosted CI; unchanged application reviews and migration/UI evidence remain valid.

The test-only correction is frozen at `0b4f6d9060c95a458824db84cac3e7e52a65c671`, with only the two identified existing browser test files changed. Both affected cases pass together in 6.1 seconds. Selection now scopes to the containing card; the recovery fixture uses its existing exact-operation recovery hook and adds payload equality assertions while retaining blocked retry, identical submitted operation and unsent follow-up checks. Application source, timeouts and retry settings are unchanged, and all three build asset hashes match the accepted 8c build. Unchanged application result review, migration rehearsal, fast and deletion/browser evidence are reused. Independent scoped review passes with no required correction; the corrected revision is integrated and pushed. Replacement hosted verification remains pending before rollout.

Final delivery: [replacement hosted CI](https://github.com/rore/keir-demo-app/actions/runs/41827591355) passes all 265 fast tests and 131 browser cases on the corrected revision. The normal demo now serves that exact qualified build at its existing address after a fresh consistent current-data backup and schema-6-to-7 migration. All 18 tables match the expected migration; provider settings and current held accounting are unchanged, integrity/foreign keys pass, and all three served assets match. No customer customization is deleted by rollout. The developer's final source/evidence archive is verified and the completed task worktree/dependency junction is removed without changing canonical dependencies; explicit developer and architect participation is detached and verified. The existing approval-contract design work remains open under the owning feature.
