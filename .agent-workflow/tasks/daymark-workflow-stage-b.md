# Run Keir 2 task workflows in Daymark

<!-- agent-workflow:start -->
**Outcome:** A Daymark user can author, simulate, approve and activate a task workflow for task creation or completion. Daymark runs the approved artifact against its real tasks.

**Target:** keir-demo-app

**Scope:** Consume the qualified Keir 2 packages, publish Daymark's host model, wire the task operations and two triggers, and implement the reviewed in-conversation Studio design. Rehearse the cutover on disposable data.

**Constraints:** Keep Core domain-independent. Preserve host authority, the original request key, the single-write limit, independent trigger selections and the separate approval and activation decisions. Use the verified package digests in isolated work. Final dependency integration requires the official GitHub assets. Live deployment and database changes require separate authorization.

**Completion criteria:** The same checked artifact runs in simulation and in the real dispatcher. The four agreed workflows produce the expected results on native task rows. A user can understand the rule, review representative simulations and activate it through the Studio. Old records and receipts remain readable and the restore rehearsal succeeds.

**Risk:** Elevated

**Complexity:** High

**Plan:** Build from Daymark Stage A. Use the optional `tagsText` scalar on the Daymark create adapter if the contract review confirms existing native parsing, normalization and bounds. Keep typed tags on reads. Review the receipt schema change for `tasks.create@2` and `tasks.update@1` before changing durable storage. Keep old `/1` artifacts, approvals, selections, events and receipts as history. Make old selections visibly unavailable after cutover and start both `/2` selections off. Settle eligible pending events only through the existing exact `/1` runner. Check for unresolved pending work under the cutover transaction and refuse to switch while any remains. Preserve started and uncertain outcomes for reconciliation without replay, translation, carried approval or backfill.

**Verification plan:** Verify offline installation and exact format and asset pins. Test adapter boundaries, two-owner/two-trigger isolation, deadline and failure paths, exact rule and evidence revisions, and concurrent or repeated receipts. Run the four workflows through simulation and native storage. Exercise desktop and narrow browser layouts with saved conversations, focused controls and distinct simulated and confirmed outcomes. Rehearse transactional upgrade and restore on copies of the Stage A database.

**Approvals:** Isolated implementation and tests are authorized. Contract and durable migration details require independent technical review before final integration.

**State:** Ready to implement
<!-- agent-workflow:end -->
