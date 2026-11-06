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

**Verification plan:** Test fresh schema and upgrades using a disposable database created by the shipped Daymark revision `52d81be`. Verify prior values and extension records, defaults, round trips for UTC millisecond due times, invalid times, local display across daylight-saving transitions, owner isolation, failed-migration rollback and reopening. Verify that an unsupported schema is unchanged and an old backup opens under the old app. Run the affected app and browser tests.

**Plan review:** Agent technical review: The architect approved isolated implementation after reviewing exact schema admission, transactional schema and version updates, rollback, idempotent reopening and reset refusal. The approved revision uses nullable UTC millisecond due times and requires a fixture created by the shipped app. Review implementation and preservation evidence before integrating.

**Approvals:** Implementation and isolated verification are authorized. Any live database change is outside this record.

**State:** Ready for review
<!-- agent-workflow:end -->

## Result review

The architect approved the isolated candidate after reviewing the editor, save behavior, tests, and pinned Temporal fallback. Daymark [59c819a](https://github.com/rore/keir-demo-app/commit/59c819a1f801a50183d2fc232b433c160da03e31) integrates the exact reviewed tree. The build passed. The native suite passed 349 tests with 13 platform skips. Chromium passed 22 native browser tests plus a separate year-boundary case. The shipped schema 10 rehearsal preserved prior data across all 20 tables and round-tripped a millisecond due time after reopening. The browser suite covered daylight-saving gaps and repeated hours with native Temporal and the fallback.

The [hosted native app workflow](https://github.com/rore/keir-demo-app/actions/runs/42191461455) passed on the integrated revision. Host workflow integration remains a separate task.

An isolated Vite 8.3.5 comparison of the base and integrated editor trees measured emitted JavaScript increasing from 360,427 to 418,118 bytes. The same files compressed with identical gzip settings increased from 105,251 to 124,428 bytes. Daymark's entry point imports the pinned Temporal fallback statically. The comparison measures bundle size and does not measure page load time.
