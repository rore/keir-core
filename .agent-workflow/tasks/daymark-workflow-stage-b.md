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
- Settle eligible pending events through the old runner before cutover. The cutover transaction must hold the write lock and roll back if any pending event lacks a durable revocation. Shipped v12 makes an existing revocation irreversible and prevents dispatch. Keep revoked pending rows byte for byte as historical evidence and show their revoked status. Keep old source, artifacts, approval and event evidence, and snapshot old selections and their generation before clearing them. Increment the generations with overflow checks. Start both Keir 2 selections off and show the old selections as historically unavailable. Preserve started and uncertain outcomes for reconciliation. No translation, carried approval, replay or backfill.

**Verification plan:** Verify offline installation and exact format and asset pins. Test tag normalization, duplicate and empty entries, both size limits and atomic rejection. Compile a tagged-create example and use the same artifact in both execution modes. Test owner and trigger isolation, deadline and failure paths, exact revisions, receipt identity and stale selection generation. Inject a pending admission after initial inventory to prove the transaction refuses cutover. Run the four workflows through simulation and native storage. Exercise desktop and narrow browser layouts with saved conversations, focused controls and distinct simulated and confirmed outcomes. Compare old records byte for byte and rehearse rollback, reopening and old-package restore on separate copies of the Stage A database.

**Implementation checkpoint:** Isolated Daymark candidate `2ea470d3de202c018e3954fdcc71a918ba57ea68` implements the Keir 2 host model, two trigger selections, native operations, and the in-conversation Studio. All four workflows passed in simulation and against native task rows using the same compiled artifact for each workflow. The shipped v12 fixture verified preservation, rollback, a concurrent pending event guard, reopening and restore with the old package. Independent host review rechecked the fixes for malformed lookups, title comparison involving a leading NUL and expiry during final proof checks.

**Verification checkpoint:** The build passed. The full Node run passed 373 tests and skipped 13. Twelve opt-in process tests passed separately, and one test requires POSIX. The last focused Node run passed 21 cases after the disposition change. All 181 Chromium cases passed across grouped runs. Local verification used staged package bytes. Official package provenance and hosted verification on the final commit remain open.

**Result review checkpoint:** Independent review of `2ea470d` and its test-only follow-up `a3ee02d53632cc0b807ddb5e856c4479366ceb9c` found no blocking contract or migration defect. All 17 focused cases passed. The follow-up tested rollback during selection archival and receipt rebuild, verified unchanged replay of confirmed and no-effect receipts, and exercised tag normalization, request identity and limits. The old Daymark package at `e007019` reopened the v12 backup byte for byte. The separate upgraded v13 copy was retained.

**Studio review checkpoint:** The designer exercised the four inline rules and their simulations on desktop and narrow screens with disposable task data. Approval and activation were distinct, and uncertain writes did not appear confirmed. Successor `7ccb182bf4fb46606c8055445a07a082fd8c6a90` ties no-change explanations to the exact saved rule and skipped condition, shows normalized tags, simplifies outcome and time details, and removes duplicated open-match text. The focused Chromium run passed all 30 cases, including checks at desktop and 390 and 320 pixels. Independent code review approved the successor. The designer's focused visual recheck is pending.

**Approvals:** Isolated implementation and tests are authorized. Contract and durable migration details require independent technical review before final integration.

**State:** Ready to implement
<!-- agent-workflow:end -->
