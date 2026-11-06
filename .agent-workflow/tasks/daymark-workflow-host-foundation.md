# Prepare Daymark task workflow host storage

<!-- agent-workflow:start -->
**Outcome:** Daymark records both native task triggers durably and preserves existing completion behavior while preparing its host for verified workflow packages.

**Target:** keir-demo-app

**Scope:** Isolated Daymark host storage, trigger capture, exact open-task lookup and native transaction seams for the workflow adapters.

**Constraints:** Preserve existing completion admissions, approval evidence, keys, receipts, and owner boundaries. Run migrations on disposable data. Keep new workflow dispatch gated on the verified Core package.

**Completion criteria:** Fresh and upgraded databases preserve existing records and support both trigger selections. Human events are captured atomically. The scoped lookup reports false only after a complete authorized read. Existing completion behavior passes its regression suite.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Daymark records both native task triggers durably and preserves existing completion behavior while preparing its host for verified workflow packages.","scope":"Isolated Daymark host storage, trigger capture, exact open-task lookup and native transaction seams for the workflow adapters.","constraints":"Preserve existing completion admissions, approval evidence, keys, receipts, and owner boundaries. Run migrations on disposable data. Keep new workflow dispatch gated on the verified Core package.","completion_criteria":"Fresh and upgraded databases preserve existing records and support both trigger selections. Human events are captured atomically. The scoped lookup reports false only after a complete authorized read. Existing completion behavior passes its regression suite."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:rich-task-customizations"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The migration changes durable selections, event admission and effect receipts. Existing approval and replay guarantees must survive the change.

**Discovery:** Daymark already captures completion and selection in one native transaction and stores definitive create receipts. An isolated query probe confirmed that a partial covering index can distinguish exact titles, owners, and task state.

**Material assumptions:** The existing per-owner trigger selection key supports a second trigger. The exact shipped schema can be migrated transactionally with all prior rows and execution pins preserved. Verify both assumptions with disposable data.

**Plan:** Reuse the queue and receipt path. Rebuild tables and their dependent objects in one transaction from each exact shipped schema, preserving event row order and bytes. Move every renamed-table consumer with the schema. Keep the existing completion route and exact executable /1 bindings. Capture creation events without bindings and exclude them from execution and later backfill. Look up receipts by owner and original key and reject capability, version or payload mismatches before mutation. Run the owner-scoped Boolean lookup with deadline checks before and after the indexed read.

**Verification plan:** Rehearse upgrades from shipped schemas, backup restore and rollback at each rebuild boundary. Compare preserved text as bytes, including embedded NUL. Verify both trigger selections, create followed by complete, execution of a pre-upgrade pending /1 event, recovery without re-executing a started event, unchanged old receipt replay, rejection of cross-action key reuse, and explicit busy and expiry failures from the lookup.

**Plan review:** The architect accepted the direction after inspecting the shipped schema and consumers. The plan and verification above incorporate the required migration, execution and receipt corrections. The published Core package gates the separate workflow adapter and Studio phase.

**Approvals:** Isolated implementation and verification are authorized. Live startup and migration are outside this record.

**State:** Ready to implement
<!-- agent-workflow:end -->
