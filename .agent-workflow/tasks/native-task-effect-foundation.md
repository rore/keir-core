# Build the native foundation for real task customizations

Owning feature: [Real task integration](../../roadmap/features/demo-task-integration.md).

<!-- agent-workflow:start -->
**Outcome:** Native task services provide the durable event and keyed creation guarantees needed by the first real completion-driven customization.

**Target:** keir-demo-app

**Scope:** Native completion/event persistence, generic idempotent task creation, host-owned owner/key/selection evidence and focused service/persistence regression. Begin with discovery and a concrete implementation plan; no production edits before contract and plan review.

**Constraints:** Preserve native validation, owner isolation, existing tasks/history/selection and spending records. Keep completion successful independently of reaction failure. No real Keir WRITE activation until core admission is qualified, no simulation flag promotion, no multi-write, blind replay, derived keys, new infrastructure, provider/settings/Git configuration/history or agent-guide changes.

**Completion criteria:** A reviewed host foundation records first completion and its event atomically, excludes duplicate event/create effects, preserves exact originating owner and admission facts, and exposes honest confirmed/rejected/unknown evidence under concurrency and restart tests. Existing native operations and data remain valid; no unqualified execution is exposed as working.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Native task services provide the durable event and keyed creation guarantees needed by the first real completion-driven customization.","scope":"Native completion/event persistence, generic idempotent task creation, host-owned owner/key/selection evidence and focused service/persistence regression. Begin with discovery and a concrete implementation plan; no production edits before contract and plan review.","constraints":"Preserve native validation, owner isolation, existing tasks/history/selection and spending records. Keep completion successful independently of reaction failure. No real Keir WRITE activation until core admission is qualified, no simulation flag promotion, no multi-write, blind replay, derived keys, new infrastructure, provider/settings/Git configuration/history or agent-guide changes.","completion_criteria":"A reviewed host foundation records first completion and its event atomically, excludes duplicate event/create effects, preserves exact originating owner and admission facts, and exposes honest confirmed/rejected/unknown evidence under concurrency and restart tests. Existing native operations and data remain valid; no unqualified execution is exposed as working."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:demo-task-integration"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Provisional Gray floor for the intended host source/tests; persistence and owner/key boundaries require explicit reassessment against the concrete patch before implementation release.

**Discovery:** Architect read-only assessment finds native owner-scoped create/edit/complete services already exist, while durable completion admission and keyed creation receipts do not. Current Studio binds only read/2; effect/2 remains simulation-only. Generic UI improvements are preserved in a separate task and can be reviewed independently.

**Material assumptions:** Reuse the existing process, SQLite and native service checks. First scenario uses existing project/title/status facts and one generic creation, with customer constants in source. Do not require search/tags/dates, a scheduler, generic workflow engine or arbitrary transport qualification. Return affected work to planning if the native guarantees or core admission contract contradict these assumptions.

**Plan:** The assigned developer inspects actual native/lifecycle callers, transaction/selection seams, migration conventions and relevant tests in an isolated checkout. Produce the smallest concrete event/receipt/service patch plan and regression cases; resolve exact event/key/selection/retention/revocation/recovery interface with the architect's bounded real-effect contract. Manager records final risk and implementation plan, obtains independent technical review and any actually required approval, then releases code. Do not block this discovery on UI review or add semantics to the preserved UI checkout. Keep core admission implementation and eventual Studio wiring separately owned; never expose a fake successful effect.

**Verification plan:** Real service/persistence tests will cover first versus repeated completion, same-key same/different payload, concurrent duplicate claims/creates, owner A versus browser B, changed selection/revocation/deletion, failure before/after commit, retained receipts after restart and uncertain outcomes without blind replay. Rehearse schema migration on copied data and preserve legacy records/accounting. Select fast affected checks, reusing reliable evidence; qualification and exact runtime/Studio end-to-end checks follow their own reviewed prerequisites.

**Plan review:** Pending concrete developer plan and architect interface; discovery only is released.

**Approvals:** User authorizes advancing real task-management customization and managing parallel open work. No new live environment or production effect activation is authorized by this discovery record.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

The manager accepts the architect's first scenario recommendation: completing a task in a customer-chosen existing project creates a task with a customer-chosen title. Actual core admission and host durability are missing; this is not an advice rename. The assigned developer has resumed separate isolated host discovery while the architect details the core/host interface. Generic UI technical/rendered review and the small compiler bug proceed independently. Production edits remain gated on the concrete reviewed plan, rather than on completion of unrelated UI work.
