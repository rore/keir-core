# Run Keir 2 task workflows in Daymark

<!-- agent-workflow:start -->
**Outcome:** A Daymark user can author, simulate, approve and activate a task workflow for task creation or completion. Daymark runs the approved artifact against its real tasks.

**Target:** keir-demo-app

**Scope:** Consume the qualified Keir 2 packages, publish Daymark's host model, wire the task operations and two triggers, and implement the reviewed in-conversation Studio design. Rehearse the cutover on disposable data.

**Constraints:** Keep Core domain-independent. Preserve host authority, the original request key, the single-write limit, independent trigger selections and the separate approval and activation decisions. Use the verified package digests in isolated work. Final dependency integration requires the official GitHub assets. Live deployment and database changes require separate authorization.

**Completion criteria:** The same checked artifact runs in simulation and in the real dispatcher. The four agreed workflows produce the expected results on native task rows. A user can understand the rule, review representative simulations and activate it through the Studio. Old records and receipts remain readable and the restore rehearsal succeeds.

**Risk:** Elevated

**Complexity:** Large

**Plan:** Build from Daymark Stage A. Use optional `tagsText` on `tasks.create@2`. Simulation and native execution share the existing comma-separated parser, normalization and native limits. Omission or whitespace-only input uses empty tags. Empty segments are invalid. The Keir String limit is 256 UTF-8 bytes across the entire input. Reads retain a typed list of up to eight tags.

**Plan review:** The architect accepted the host contract and migration plan with the requirements below. Review the implementation separately before final integration.

- Admit only `tasks.create@1`, `tasks.create@2` and `tasks.update@1` receipt pairs. Preserve old row IDs, payload and result bytes, event keys, owner, affected task, and effect evidence. Keep uniqueness, foreign keys and immutability. Authenticate the event and original identity before lookup. Compare action, version and canonical authored input before mutation. Preserve authored presence and values before native normalization. An exact historical replay returns the saved receipt and never reexecutes a write.
- Settle eligible pending events through the old runner before cutover. The cutover transaction must hold the write lock, check every pending admission, and roll back if any remains. Keep old source, artifacts, approval and event evidence, and snapshot old selections and their generation before clearing them. Increment the generations with overflow checks. Start both Keir 2 selections off and show the old selections as historically unavailable. Preserve started and uncertain outcomes for reconciliation. No translation, carried approval, replay or backfill.

**Verification plan:** Verify offline installation and exact format and asset pins. Test tag normalization, duplicate and empty entries, both size limits and atomic rejection. Compile a tagged-create example and use the same artifact in both execution modes. Test owner and trigger isolation, deadline and failure paths, exact revisions, receipt identity and stale selection generation. Inject a pending admission after initial inventory to prove the transaction refuses cutover. Run the four workflows through simulation and native storage. Exercise desktop and narrow browser layouts with saved conversations, focused controls and distinct simulated and confirmed outcomes. Compare old records byte for byte and rehearse rollback, reopening and old-package restore on separate copies of the Stage A database.

**Implementation checkpoint:** The isolated worktree has the Keir 2 host model, both trigger selections, native reads and writes, authoring and Studio changes, and the v13 cutover guard. A focused integration test compiled and simulated both rules, approved and activated them independently, then ran those exact artifacts against native tasks and checked their distinct receipts. The build, authoring checks, 16 scenario checks and two focused Studio browser checks passed. The wider regression suite has failures under investigation. Four-workflow native verification and migration and restore rehearsals remain open.

**Approvals:** Isolated implementation and tests are authorized. Contract and durable migration details require independent technical review before final integration.

**State:** Ready to implement
<!-- agent-workflow:end -->
