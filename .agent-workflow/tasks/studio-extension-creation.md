# Complete extension creation, editing and recovery

Owning features: [conversational authoring](../../roadmap/features/conversational-authoring.md) and [Studio design](../../roadmap/features/extension-studio-design.md). The user reported that New reopens old chat, requested Enter-to-send, rejected the advice-only product model, and required coherent design and QA of the complete journey.

<!-- agent-workflow:start -->
**Outcome:** A business user creates distinct extensions in a general customization Studio, edits the intended extension safely, and resumes exactly the intended work.

**Target:** keir-demo-app; shared design and planning in keir-core.

**Scope:** Demo extension identity and owner-scoped storage/migration, lifecycle and authoring APIs/context, customization collection and Studio navigation, keyboard behavior, interaction design, regression and rendered QA; core-owned records and roadmap.

**Constraints:** Preserve existing extensions, conversations, immutable evidence, independent expectations, task data and current activation. Enforce owner/extension/revision/generation fences, cumulative budgets and separate approval/activation. No invented hook support, arbitrary UI/code, real task writes, dependency framework or global environment changes. Preserve the existing paid qualification ledger and held allowance; isolated tests must not alter operator data/settings.

**Completion criteria:** New opens a fresh conversation despite existing work and creates a distinct saved extension; A and B can be reopened/edited without overwriting each other. Editing active behavior leaves the current selection on until exact checked approval and separate activation. Explicit continue, reload, interruption and ambiguous responses recover the correct operation without automatic paid replay. Enter sends once, Shift+Enter adds a line and composing Enter does not submit. General intent discovery uses actual host contracts and explains unsupported requests without silent advice substitution. Actual app-entry journeys verify prior data, owner switching, stale/late responses, desktop/narrow/short screens and native-task preservation.

**Requirement baseline:**
{"source":"user-request","outcome":"A business user creates distinct extensions in a general customization Studio, edits the intended extension safely, and resumes exactly the intended work.","scope":"Demo extension identity and owner-scoped storage/migration, lifecycle and authoring APIs/context, customization collection and Studio navigation, keyboard behavior, interaction design, regression and rendered QA; core-owned records and roadmap.","constraints":"Preserve existing extensions, conversations, immutable evidence, independent expectations, task data and current activation. Enforce owner/extension/revision/generation fences, cumulative budgets and separate approval/activation. No invented hook support, arbitrary UI/code, real task writes, dependency framework or global environment changes. Preserve the existing paid qualification ledger and held allowance; isolated tests must not alter operator data/settings.","completion_criteria":"New opens a fresh conversation despite existing work and creates a distinct saved extension; A and B can be reopened/edited without overwriting each other. Editing active behavior leaves the current selection on until exact checked approval and separate activation. Explicit continue, reload, interruption and ambiguous responses recover the correct operation without automatic paid replay. Enter sends once, Shift+Enter adds a line and composing Enter does not submit. General intent discovery uses actual host contracts and explains unsupported requests without silent advice substitution. Actual app-entry journeys verify prior data, owner switching, stale/late responses, desktop/narrow/short screens and native-task preservation."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"},{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Demo implementation paths have the unclassified GRAY floor. Extension identity, persistence compatibility, asynchronous navigation and evidence ownership require coordinated design and regression coverage; no policy relaxation is proposed.

**Discovery:** At demo 0822ffb, Studio mount unconditionally loads the latest conversation, overriding explicit start intent. Draft/selection state is keyed by owner rather than a durable extension identity; immutable revision IDs and conversation IDs do not identify a saved extension. Agent context and collection/Studio copy assume advice. Earlier accepted tests qualified a resumed supported example, not this complete product journey. Developer diagnosis and architect/designer contracts are in progress.

**Material assumptions:** Existing compiler/runtime semantics can support multiple saved extensions with one active selection for the currently supported hook. If ownership, retention, migration or execution cannot preserve the stated guarantees, stop affected implementation and return to design. Unknown legacy conversation lineage must remain readable without guessing an extension binding. A fresh chat must not treat another draft's source/cases as the new customer's intent.

**Plan:**
1. Consolidate one interaction and technical design before production edits: collection, New, save/continue/edit, active-versus-draft divergence, unsupported requests, keyboard, pending jobs, reload, lost responses and owner changes. Designer owns the maintained interaction document; architect owns identity/lifecycle/compatibility decisions; developer supplies exact source/test proposal.
2. Add the smallest host-generated owner-scoped extension identity and persistence/API binding needed for separate saved work. Preserve immutable revisions/evidence and legacy data; create nothing on entry and deduplicate first accepted submission. Retain explicit generation-CAS replacement of today's single active selection. Publish the exact migration and recovery contract before implementing it.
3. Wire actual app navigation and authoring context to explicit fresh/edit/continue intent and extension identity. Keep catalog-driven general creation and truthful support limits. Enter uses the same guarded submit; Shift+Enter is multiline and IME composition is protected.
4. Implement against the reviewed shared design in isolated checkouts with clear file ownership. Keep manager ownership of this record/roadmap and integration. Do not reopen the completed previous delivery record or edit its archived worktree.
5. Reproduce the reported top-level failure before the fix. Verify each completion criterion through real-service browser journeys, API/storage regressions and independent rendered interaction loops; fix failures before acceptance. Rehearse migration on isolated legacy fixtures, including ambiguous lineage and rollback/source compatibility. Keep fast suites fast and select broader browser runs after the combined change settles.
6. Review exact combined source and evidence independently, then deliver directly on main with privacy/identity checks. Prepare a recoverable backup and concrete reviewed impact before any live data upgrade; follow the standing environment-approval directive. Preserve the same user preview URL, completed evidence, existing qualification accounting and unrelated work. Normally archive/remove task worktrees after accepted delivery.

**Verification plan:**
- New versus continue: app collection entry with existing conversations/active state, fresh before first submit, exact saved conversation and empty-history ownership; demonstrate the reported failure before correction.
- Extension identity: save A and B, reopen/edit each, concurrent same/different targets, foreign IDs, stale CAS, history limits, original byte/evidence retention and migration of existing data without guessed lineage.
- Edit/activation: actual active target versus differing draft, original behavior during edits/checks, mismatches/missing receipts prevent approval, exact approval and separate generation-CAS activation, other saved extension remains intact.
- Recovery: reload before/after first accepted submit, lost POST response, same operation receipt/readback, cancelled/interrupted job, late responses and owner switch; no automatic provider replay or cross-owner disclosure.
- Keyboard and generality: Enter/Shift+Enter/IME/229, empty/disabled/running state and duplicate submit; supported intent and unavailable intent with explicit alternative consent, no manual editor or advice-only front door.
- Experience: designer and manager exercise the actual navigation on desktop, phone and short window with configured synthetic and unconfigured modes, preserving unsaved native task edits. Distinguish scripted transport checks from any separately permitted paid qualification.
- Regression/delivery: focused and full fast tests, production build, combined browser suite and independent exact result review; hosted checks, disclosure scan, source/data backup and normal worktree cleanup.

**Plan review:** Pending the consolidated interaction/technical design and exact developer proposal; no production implementation release yet.

**Approvals:** Not required at this risk level for isolated repository development. Existing user authorization covers this coherent slice. Any live environment/data upgrade follows the standing explicit-approval boundary after the reviewed result is concrete.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

Discovery and design only. Developer has accepted read-only diagnosis; architect has identified durable extension identity as necessary. Designer assignment is offered. Production edits are not released. The previous contribution remains delivered; this record owns the newly authorized complete creation/edit/recovery correction.

## Evidence

Pending actual reproduction and implementation checks. No new provider call, operator-data edit or live upgrade is claimed.

## Result review

Pending exact combined implementation and verified user-level acceptance.

