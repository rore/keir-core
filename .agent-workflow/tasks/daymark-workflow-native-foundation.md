# Add priority and due dates to Daymark tasks

<!-- agent-workflow:start -->
**Outcome:** Daymark tasks have priority and an optional due date, including after an upgrade from the currently shipped database schema.

**Target:** keir-demo-app

**Scope:** Native task schema, storage, API, task UI and focused upgrade tests. Record event and authority requirements for later Keir integration.

**Constraints:** Use an isolated checkout and disposable data. Preserve existing rows, relationships and owner scoping. Maintain exact schema checks and keep extension behavior separate during this increment.

**Completion criteria:** Fresh and upgraded databases support the fields. Existing tasks retain their values and receive the documented defaults. Invalid values, unauthorized changes and unsupported schema states are rejected.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Daymark tasks have priority and an optional due date, including after an upgrade from the currently shipped database schema.","scope":"Native task schema, storage, API, task UI and focused upgrade tests. Record event and authority requirements for later Keir integration.","constraints":"Use an isolated checkout and disposable data. Preserve existing rows, relationships and owner scoping. Maintain exact schema checks and keep extension behavior separate during this increment.","completion_criteria":"Fresh and upgraded databases support the fields. Existing tasks retain their values and receive the documented defaults. Invalid values, unauthorized changes and unsupported schema states are rejected."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:rich-task-customizations"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The storage format and task API change. Qualification uses disposable copies and existing owner boundaries.

**Discovery:** Daymark validates the exact schema before opening an existing database. The current schema must remain an explicit migration source.

**Material assumptions:** The shipped schema can be upgraded transactionally while preserving task data and extension records. If the rehearsal disproves this, revise the migration before integrating.

**Plan:** Add native priority and nullable due date with explicit defaults, validation and storage constraints. Implement a transactional upgrade from the exact shipped schema. Extend ordinary task controls. Coordinate event and model semantics with the architect before any Keir-facing implementation.

**Verification plan:** Test fresh schema and isolated upgrades populated with prior tasks and extension state. Verify defaults, read and update round trips, invalid values, owner isolation, unsupported source schema and failed-migration recovery. Run the affected app and browser tests.

**Plan review:** Agent technical review: The manager checked the developer's exact-source migration approach and isolated scope. Review the implemented upgrade and preservation evidence before integrating.

**Approvals:** Implementation and isolated verification are authorized. Any live database change is outside this record.

**State:** Ready to implement
<!-- agent-workflow:end -->
