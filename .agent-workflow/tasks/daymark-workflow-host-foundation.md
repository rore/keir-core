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

**Plan:** Start the owner-scoped Boolean lookup and isolated fixtures. Reuse the existing queue and receipt path. After the architect reviews the storage and authority plan, implement exact schema migration, atomic trigger capture and replay boundaries. Keep current completion registrations and external responses through this phase.

**Verification plan:** Rehearse upgrades from shipped schemas, backup restore and failed migration rollback. Verify exact row preservation, both trigger selections, atomic capture, origin, owner isolation, deadline and query failures, one receipt per key, and current completion regressions.

**Plan review:** The manager reviewed the scoped lookup and isolated preparation. Independent architecture review is required before editing migration or execution authority. The published Core package gates the separate workflow adapter and Studio phase.

**Approvals:** Isolated implementation and verification are authorized. Live startup and migration are outside this record.

**State:** Ready to implement
<!-- agent-workflow:end -->
