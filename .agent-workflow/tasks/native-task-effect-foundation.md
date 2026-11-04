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

**Reason:** Gray source/test floor, with bounded local persistence and owner/key risk. The concrete slice changes two existing production files and adds no public API, runner or real activation. Independent review must reassess before implementation release if scope or authority changes.

**Discovery:** Native completion preserves its first timestamp but records no durable event; generic creation has no operation-key receipt. Existing selection storage is owner-only and scoped to ClassifyTask. The reviewed plan reserves schema8 and per-hook selection/revocation writes for this owner. A developer probe exposes malformed-Unicode replacement at the native storage boundary; add a strict check only to the new keyed interface before canonicalization, preserving ordinary CRUD behavior and its 120 UTF-16-unit title bound. Current Studio stays read/2 and effect/2 simulation-only; no public real activation is added.

**Material assumptions:** Reuse the existing process, SQLite and native service checks. First scenario uses existing project/title/status facts and one generic creation, with customer constants in source. Do not require search/tags/dates, a scheduler, generic workflow engine or arbitrary transport qualification. Return affected work to planning if the native guarantees or core admission contract contradict these assumptions.

**Plan:** Change production only in data.mjs and extension-lifecycle.mjs. Data owns one transactional V7-to-V8 migration: additive owner-scoped completion-event/receipt tables and task owner index, plus rebuild only the small selection table to owner/hook keys while preserving ClassifyTask revision/generation exactly and leaving TaskCompleted unselected. Extend verified backup/reset/seed coverage without resetting spending, event backfill or task/authoring rebuilds. completeTask owns one nonnested transaction for first transition plus distinct event/original-key and stored selection/pins; repeated completion emits nothing and its public DTO remains unchanged. Add a dormant trusted service in data.mjs for guarded PENDING-to-STARTED claim, immutable finish, separately dated reconciliation, branded original-key resolution, read-only audit and createTaskOnce. Reuse native validation/insertion helpers, reject malformed new-interface strings before canonicalization, compare exact canonical payload/version, and atomically persist task/receipt only for STARTED nonrevoked work with current source/resource/customization/qualification authority checked at commit. Matching receipts use current read authority after finish/revocation; mismatch or unknown/cross-owner keys never create a second effect. Do not persist transient/UNKNOWN as terminal rejection or replay STARTED. Scope every current lifecycle selection query to ClassifyTask; disable/delete permanently revoke relevant PENDING/STARTED admissions in the same transaction, including old replaced revisions, while replacement alone does not revoke and reactivation never clears revocation. Retain finished outcomes and expose internal revocation for later qualification integration. No runner, frontend, API, package or public effect activation. Update only affected fixtures/tests, reserving schema8 and these data/lifecycle writes against concurrent work. Obtain independent concrete plan review before production edits; return broader changes to planning.

**Verification plan:** New native-task-effect-persistence tests plus affected data/API, lifecycle/deletion and migration suites cover populated V7-to-V8/reopen and injected rollback; exact legacy bytes/selections/generations/held spend/UNKNOWN preservation; corrupt backup and reset/old-key rejection; first/repeated completion and actual file-backed multi-process contention; atomic event/task/receipt rollback and crash windows; one claim, no STARTED replay; stable same-key receipt after native task edits/FINISHED, mismatch/unknown/cross-owner/unbranded keys; deterministic NONE versus transient/UNKNOWN; A's event after browser B; replacement versus disable-reenable/delete and current qualification/resource authority at commit; retained confirmed audit; surrogate rejection, valid supplementary Unicode, trim/native bounds and unchanged public DTOs. Update genuine legacy fixture teardown and direct selection SQL callers explicitly, without IF NOT EXISTS masking. Run focused affected checks then the fast suite; run affected browser fixture suites only if setup changes. Reuse generic UI evidence. No paid provider or live qualification/rollout is needed for this dormant slice.

**Plan review:** Exact developer plan and reviewed core/host interface are recorded; clean-context technical plan review is pending before production edits. The architect's Unicode correction is included explicitly in the keyed boundary and verification.

**Approvals:** User authorizes advancing real task-management customization and managing parallel open work. No new live environment or production effect activation is authorized by this discovery record.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

The manager accepts the architect's first scenario recommendation: completing a task in a customer-chosen existing project creates a task with a customer-chosen title. Actual core admission and host durability are missing; this is not an advice rename. The assigned developer has resumed separate isolated host discovery while the architect details the core/host interface. Generic UI technical/rendered review and the small compiler bug proceed independently. Production edits remain gated on the concrete reviewed plan, rather than on completion of unrelated UI work.

Concrete developer plan is complete at unchanged demo 0b4f6d9, with exactly two production-file owners. It aligns with reviewed admission/handoff plus the architect's narrow Unicode correction. Manager inspected the clean source and exact plan evidence, recorded scope/risk/test seams, and requested independent review. No schema8, native authority guarantee, real runner or public activation is implemented yet.
