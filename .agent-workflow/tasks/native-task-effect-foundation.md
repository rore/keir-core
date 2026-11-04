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

**Plan review:** Agent technical review: clean-context non-implementer PASS under Plan review below, including exact developer packet and corrected Unicode boundary. Elevated/Moderate remains appropriate for the dormant two-file slice. Required transaction, historical migration, permanent revocation, retained receipt and actual crash/concurrency conditions are recorded and accepted; implementation is released.

**Approvals:** User authorizes advancing real task-management customization and managing parallel open work. No new live environment or production effect activation is authorized by this discovery record.

**Exceptions:** —

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The manager accepts the architect's first scenario recommendation: completing a task in a customer-chosen existing project creates a task with a customer-chosen title. Actual core admission and host durability are missing; this is not an advice rename. The assigned developer has resumed separate isolated host discovery while the architect details the core/host interface. Generic UI technical/rendered review and the small compiler bug proceed independently. Production edits remain gated on the concrete reviewed plan, rather than on completion of unrelated UI work.

Concrete developer plan is complete at unchanged demo 0b4f6d9, with exactly two production-file owners. It aligns with reviewed admission/handoff plus the architect's narrow Unicode correction. Manager inspected the clean source and exact plan evidence, recorded scope/risk/test seams, and requested independent review. No schema8, native authority guarantee, real runner or public activation is implemented yet.

## Plan review

Independent clean-context technical review passes the exact plan and affected native/API/lifecycle/migration callers. The reviewer confirms Elevated/Moderate for additive local stores, small selection migration and an unconstructed service with no public runner/activation. The manager accepts the following conditions and releases production implementation in the assigned isolated checkout:

- Use a single nonnested transaction owner and internal validation/insertion helpers. Required authority callbacks are trusted synchronous non-I/O strict Booleans; missing-receipt writes recheck authority immediately before commit.
- Resolve branded key and owner before disclosure. Matching receipts retain a separately read-authorized path after finish/revocation; missing receipts require STARTED and exact current authority. Runtime outcome is immutable, reconciliation separate; transient/UNKNOWN never becomes durable rejection or replay.
- Preserve seedLifecycle's historical V1-to-V2 use before the V8 selection shape exists. Keep genuine V4 fixtures intact and explicitly adjust current-schema teardown/upgrade assertions. Schema8 remains exclusively owned here.
- Disable/delete revokes admissions item-wide, including old replaced revisions and an unselected deleted item; replacement alone does not revoke and re-enable never clears it. Any required durable revocation observation must survive denial rather than roll back with it.
- Deliver actual separate-process file-backed contention and abrupt termination/reopen evidence at event, claim, create/receipt and outcome boundaries. Include legacy/spending/UNKNOWN preservation, corrupt backup and failed reset, old-key rejection, retained audit and every affected selection fixture/browser suite.
- Apply malformed-Unicode rejection only to the new keyed project/title boundary before canonicalization, preserve ordinary CRUD and the 120 UTF-16-unit title bound, and test lone surrogates plus valid supplementary characters. The corrected handoff pin is 97397650b6bde792678aa468c930d34f24f9b5f1.

No tests, edits or live actions were performed by the reviewer. This accepts the implementation plan; actual native guarantees and real-execution/deployment qualification remain unproven until implementation review.

## Frozen implementation and verification

Developer result f02932bed14980e05d6843cbfff5370b247f3258 against demo 0b4f6d9 is clean and frozen. Production scope remains data.mjs and extension-lifecycle.mjs, with 17 affected test/fixture files; no frontend, public runner, package/core or live changes. The manager verified source identity, tracked cleanliness and exact result-manifest hash and requested independent frozen result review before integration.

Reported verification: fast regression passes 284 tests with no failures; its initial 11 skips comprise one existing Windows permission case and ten gated physical cases. Two additional physical-only ordering cases were subsequently added; the final default process file confirms all 12 physical cases remain gated. The final selected physical suite passes all 12 with no skips, including real separate-process contention, abrupt termination around commit boundaries and both selection/completion orders. Focused native/storage/API passes 23 cases, assigned migration/context passes 60, all 42 affected browser cases pass, and build/typecheck passes. Raw logs, source/test/build hashes and original setup-failure evidence are retained privately for review.

Implementation evidence covers owner/key/authority/revocation, separately read-authorized retained receipts, immutable outcomes/reconciliation, exact populated migration/backup/reset/legacy/accounting preservation, current keyed Unicode/native bounds and non-replay of interrupted STARTED work. Power-loss/fsync guarantees, actual runtime binding, deployment qualification and whole-product UX remain outside this dormant result. Original logs were retained when scoped test elevation and an isolated dependency junction resolved setup failures; no global tooling was changed. The checkout/evidence/participation remain pinned for review and possible fixes.

## Result review and integration

Agent technical review: independent clean-context non-implementer PASS for f02932bed14980e05d6843cbfff5370b247f3258 against 0b4f6d9. Reviewed revision: f02932bed14980e05d6843cbfff5370b247f3258. Verification adequacy: sufficient for the bounded dormant foundation. The reviewer independently verified all 21 manifest-listed files and actual native/API/lifecycle/migration/backup/reset/seed/concurrency fixtures, with no blocker or weakened behavioral contract. It confirms permanent revocation, current commit authority, separate read-authorized retained receipts, exact legacy preservation, actual child-process barriers/termination/reopen proof and unchanged public DTOs/ordinary CRUD. The final physical-only additions are covered by the selected final run and default gate check.

Manager accepted this gate and fast-forwarded demo main to the exact frozen f02932b source, then pushed it. Hosted full-suite delivery verification and recoverable cleanup remain. No live database migration, constructed effect service, real runner or public activation was performed. Native runtime/deployment qualification and power-loss/fsync guarantees remain separate from this result.
