# Run Keir 2 task workflows in Daymark

<!-- agent-workflow:start -->
**Outcome:** A Daymark user can author, simulate, approve and activate a task workflow for task creation or completion. Daymark runs the approved artifact against its real tasks.

**Target:** keir-demo-app

**Scope:** Consume the qualified Keir 2 packages, publish Daymark's host model, wire the task operations and two triggers, and implement the reviewed in-conversation Studio design. Rehearse the cutover on disposable data.

**Constraints:** Keep Core domain-independent. Preserve host authority, the original request key, the single-write limit, independent trigger selections and the separate approval and activation decisions. Use the verified package digests in isolated work. Final dependency integration requires the official GitHub assets. Live deployment and database changes require separate authorization.

**Completion criteria:** The same checked artifact runs in simulation and in the real dispatcher. The four agreed workflows produce the expected results on native task rows. A user can understand the rule, review representative simulations and activate it through the Studio. Old records and receipts remain readable and the restore rehearsal succeeds.

**Requirement baseline:**
{"source": "git:4c0a580076f5ff96a0fb290cc6225c370ff4153d:.agent-workflow/tasks/daymark-workflow-stage-b.md", "outcome": "A Daymark user can author, simulate, approve and activate a task workflow for task creation or completion. Daymark runs the approved artifact against its real tasks.", "scope": "Consume the qualified Keir 2 packages, publish Daymark's host model, wire the task operations and two triggers, and implement the reviewed in-conversation Studio design. Rehearse the cutover on disposable data.", "constraints": "Keep Core domain-independent. Preserve host authority, the original request key, the single-write limit, independent trigger selections and the separate approval and activation decisions. Use the verified package digests in isolated work. Final dependency integration requires the official GitHub assets. Live deployment and database changes require separate authorization.", "completion_criteria": "The same checked artifact runs in simulation and in the real dispatcher. The four agreed workflows produce the expected results on native task rows. A user can understand the rule, review representative simulations and activate it through the Studio. Old records and receipts remain readable and the restore rehearsal succeeds."}

**Relationships:**
{"schema_version": 1, "sources": [{"provider": "minimap", "scope_ref": "roadmap:v1:git:github.com/rore/keir-core#roadmap", "local_ref": "item:v1:rich-task-customizations"}], "pull_requests": []}

**Risk:** Elevated

**Complexity:** Large

**Reason:** The task changes host-bound native mutations, immutable receipts, customer approval and database migration. Shared artifacts, independent review, native persistence checks and a recoverable rollout are required.

**Discovery:** The released Keir 2 packages and four integrated workflows were already verified. Closure inspection identified missing focused snapshot-concurrency evidence, stale qualification descriptions and a valid YAML-authored follow-up whose title and destination were absent from its visual summary. The existing compiler, dispatcher, lifecycle, browser fixture and checked-expression decoder provide the necessary verification and presentation boundaries.

**Material assumptions:** Core remains domain-independent. The DCM and released compiler/runtime bytes define accepted functionality. Studio summaries describe only recognized verified dataflow and fall back for unsupported expressions. The open-task read is a snapshot and grants no atomic business uniqueness. Live model observations qualify the observed journey, while scripted provider tests remain regression evidence.

**Plan:** Build from Daymark Stage A. Use optional `tagsText` on `tasks.create@2`. Simulation and native execution share the existing comma-separated parser, normalization and native limits. Omission or whitespace-only input uses empty tags. Empty segments are invalid. The Keir String limit is 256 UTF-8 bytes across the entire input. Reads retain a typed list of up to eight tags.

**Plan review:** Agent technical review: independent architect review of the host contract and migration plan recorded in this task. The architect accepted the host contract and migration plan with the requirements below. Review the implementation separately before final integration.

- Admit only `tasks.create@1`, `tasks.create@2` and `tasks.update@1` receipt pairs. Preserve old row IDs, payload and result bytes, event keys, owner, affected task, and effect evidence. Keep uniqueness, foreign keys and immutability. Authenticate the event and original identity before lookup. Compare action, version and canonical authored input before mutation. Preserve authored presence and values before native normalization. An exact historical replay returns the saved receipt and never reexecutes a write.
- Settle eligible pending events through the old runner before cutover. The cutover transaction must hold the write lock and roll back if any pending event lacks a durable revocation. Shipped v12 makes an existing revocation irreversible and prevents dispatch. Keep revoked pending rows byte for byte as historical evidence and show their revoked status. Keep old source, artifacts, approval and event evidence, and snapshot old selections and their generation before clearing them. Increment the generations with overflow checks. Start both Keir 2 selections off and show the old selections as historically unavailable. Preserve started and uncertain outcomes for reconciliation. No translation, carried approval, replay or backfill.

**Verification plan:**

- Package pins, tag limits and native authority -> Verify offline installation and exact format and asset pins. Test tag normalization, duplicate and empty entries, both size limits and atomic rejection. Compile a tagged-create example and use the same artifact in both execution modes. Test owner and trigger isolation, deadline and failure paths, exact revisions, receipt identity and stale selection generation. Inject a pending admission after initial inventory to prove the transaction refuses cutover. Run the four workflows through simulation and native storage. Exercise desktop and narrow browser layouts with saved conversations, focused controls and distinct simulated and confirmed outcomes. Compare old records byte for byte and rehearse rollback, reopening and old-package restore on separate copies of the Stage A database.

**Approvals:** Isolated implementation and tests are authorized. Contract and durable migration details require independent technical review before final integration.

**State:** Ready for review
<!-- agent-workflow:end -->

**Implementation checkpoint:** Isolated Daymark candidate `2ea470d3de202c018e3954fdcc71a918ba57ea68` implements the Keir 2 host model, two trigger selections, native operations, and the in-conversation Studio. All four workflows passed in simulation and against native task rows using the same compiled artifact for each workflow. The shipped v12 fixture verified preservation, rollback, a concurrent pending event guard, reopening and restore with the old package. Independent host review rechecked the fixes for malformed lookups, title comparison involving a leading NUL and expiry during final proof checks.

**Verification checkpoint:** Local verification used staged package bytes and included 12 separately passing process tests and the four workflow cases. Final verification used the official release downloads. The first hosted run exposed four stale browser assertions. Their correction changed only tests. The final hosted candidate passed the build, 375 Node tests and all 182 Chromium tests, with 12 opt-in Node skips. [Passing hosted run](https://github.com/rore/keir-demo-app/actions/runs/42261861370)

**Result review checkpoint:** Independent review of `2ea470d` and its test-only follow-up `a3ee02d53632cc0b807ddb5e856c4479366ceb9c` found no blocking contract or migration defect. All 17 focused cases passed. The follow-up tested rollback during selection archival and receipt rebuild, verified unchanged replay of confirmed and no-effect receipts, and exercised tag normalization, request identity and limits. The old Daymark package at `e007019` reopened the v12 backup byte for byte. The separate upgraded v13 copy was retained.

**Studio review checkpoint:** The designer exercised the four inline rules and their simulations on desktop and narrow screens with disposable task data. Approval and activation were distinct, and uncertain writes did not appear confirmed. Successor `7ccb182bf4fb46606c8055445a07a082fd8c6a90` ties no-change explanations to the exact saved rule and skipped condition, shows normalized tags, simplifies time details, and removes duplicated open-match text. All 30 focused Chromium cases passed, including checks at desktop and 390 and 320 pixels. Independent code review approved the successor. The designer verified the visual changes and identified one remaining copy issue. Follow-up `087ce64f97f52fea2b986d0ab4694fcf00adbd39` closes it with plain-language uncertainty and unavailable outcomes, a separate indication when the example matched, 11 passing focused Chromium cases and visual inspection of both rendered rows.

**Integration:** Private Daymark `main` advanced from Stage A `e007019` to the exact hosted candidate `77515417e74f962dd09a95c68f4a6b7306877e81`. The vendored compiler, runtime, notices, guide and scenario schema match the official Core and companion downloads. [Core 0.2.0](https://github.com/rore/keir-core/releases/tag/v0.2.0) · [Authoring companion v4](https://github.com/rore/keir-core/releases/tag/authoring-v4)

**Demo rollout:** The running demo serves `7751541` with Keir `0.2.0`. The rehearsed schema migration preserved existing records, and a verified copy of the original database was retained for rollback. The live browser asset matched the checked build by SHA-256, and the migrated database passed its integrity check.

**Closure verification:** A live OpenAI authoring run on the deployed build saved a same-project follow-up with a copied title and fixed suffix. Four independent simulations passed. A subsequent conversational request sent with Enter completed normally, and reopening retained the exact draft, evidence and enabled approval control. The same artifact was reproduced byte for byte and exercised in a disposable native database. Approval and activation were separate, activation changed no task, two different projects received the expected titles, and repeated completion retained the original receipts without another task. This qualifies that observed provider journey. It does not establish compatibility with every model.

The focused native concurrency regression in `6e90b87` confirms both outcomes of the snapshot contract. Two different events can both observe no open match and each create a task. Concurrent dispatch and replay of the same event retain one created task and its stable receipt. Combined native, simulation and authoring checks passed all 19 cases. The summary correction in `74a67d8` reads the verified artifact for the observed same-project title-suffix rule and preserves fallback for unsupported expressions. Independent review found no blocker.

The first full closeout browser run passed 182 cases on Windows and exposed a sign-in synchronization race in the new test. Hosted verification exposed the same race and a short-viewport approval button that was not fully visible. The test now waits for the signed-in owner. The layout correction applies the existing compact header to inline review below 500 pixels. A deterministic 683-by-384 regression holds the real simulation response while the reader scrolls up, then verifies the unread indicator, setup notice, full approval action and composer together. Restoring only the old CSS selectors makes this regression fail because the log is 6.55 pixels shorter than the action. The corrected case passes with both 0.99 visibility assertions unchanged.

The accepted closeout is `16c7f5c54548058da6f509f20ed00835b46720e1`. Hosted qualification passed the build, 379 Node tests and all 183 Chromium tests, with 12 opt-in process tests skipped. The running demo serves the same application bytes. Restart preserved all 21 database tables, and the served browser asset matched the checked build by SHA-256. The saved live-provider discussion, four simulations and enabled approval control survived the restart. The temporary unapproved qualification customization was then deleted through the UI, preserving other items and trigger selections. The current slice is complete. [Closeout verification](https://github.com/rore/keir-demo-app/actions/runs/42468891600)

## Result review

Agent technical review: independent review of the concurrency and summary changes found no blockers. [Reviewed Daymark changes](https://github.com/rore/keir-demo-app/compare/77515417e74f962dd09a95c68f4a6b7306877e81...16c7f5c54548058da6f509f20ed00835b46720e1)

Reviewed revision: 16c7f5c54548058da6f509f20ed00835b46720e1

Verification adequacy: the regression uses actual compiler artifacts and native SQLite operations. The summary recognizes only the verified completion, same-project and title-suffix dataflow and preserves fallback for unsupported expressions and changed identities. The concurrency regression demonstrates asynchronous interleaving on one database connection. It adds no cross-process durability or atomic business-uniqueness claim. Independent follow-up review accepted the sign-in synchronization, short-height CSS and deterministic unread regression. The rendered 683-by-384 capture shows all required controls fully visible. Full closeout test and rollout results are recorded above.
