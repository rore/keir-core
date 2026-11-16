# Keep authoring context management internal

Owning feature: [Conversational authoring](../../roadmap/features/conversational-authoring.md). App baseline: `4f74a4ac7b7346859ee02d73316642edda4c85ff`. Core baseline: `515647c552982a964c18bed4c828b533cf382241`.

<!-- agent-workflow:start -->
**Outcome:**
Ordinary customization authoring completes without making the person manually restart the assistant's internal context.

**Target:**
Daymark's existing single-Send authoring flow and saved Studio work.

**Scope:**
Reproduce the request/context-size boundary with the native adapters and actual host workflow. Keep normal requests/checkpoints bounded through internal packing or renewal in the existing loop. Remove the forced context-recovery interaction for this internal condition while preserving ordinary Send, saved review and genuine failure recovery. Update affected tests and existing guidance; deliver the verified fix to the retained demo.

**Constraints:**
No Studio layout redesign, general memory, workflow engine, new dependency, Core language/runtime change, database schema change, paid provider replay or blanket cap increase. Do not weaken provider reservation/accounting, validation, cancellation, owner/target/draft/proof fences, exact recovery or independent examples. Never replay an uncertain or completed effect to refresh context. Keep approval and activation explicit. Preserve complete saved conversation, source, cases, receipts, settings and native data. Proposed multi-action and pre-save-patch slices remain unstarted. Repository-role trigger rollout is deferred and separate.

**Completion criteria:**
A representative multi-step before-save rule and repeated bounded reads cross the old cumulative boundary and reach the correct answer or exact review without a Continue/context-management action. Both native adapter shapes remain valid. Fresh context preserves the current user request, resolved intent, exact saved target/source/scenarios/proof and required recent results; dropped duplicated/transient data is not mistaken for authority or complete evidence. Same-turn renewal does not replay host mutations, resolve intent again, bypass resource reservations or auto-submit on page mount. Existing blocked work can be opened and continued through ordinary controls without hiding eligible saved review behind an internal context gate. Malformed, irreducibly oversized, cancelled, stale, budget-exhausted and uncertain work remains honestly bounded and safe. Focused/regression checks, independent technical/prose review and preserved-data delivery pass.

**Requirement baseline:**
{"source":"user-authorized correction of internal context-limit interruption","outcome":"Ordinary customization authoring completes without making the person manually restart the assistant's internal context.","scope":"Reproduce the request/context-size boundary with the native adapters and actual host workflow. Keep normal requests/checkpoints bounded through internal packing or renewal in the existing loop. Remove the forced context-recovery interaction for this internal condition while preserving ordinary Send, saved review and genuine failure recovery. Update affected tests and existing guidance; deliver the verified fix to the retained demo.","constraints":"No Studio layout redesign, general memory, workflow engine, new dependency, Core language/runtime change, database schema change, paid provider replay or blanket cap increase. Do not weaken provider reservation/accounting, validation, cancellation, owner/target/draft/proof fences, exact recovery or independent examples. Never replay an uncertain or completed effect to refresh context. Keep approval and activation explicit. Preserve complete saved conversation, source, cases, receipts, settings and native data. Proposed multi-action and pre-save-patch slices remain unstarted. Repository-role trigger rollout is deferred and separate.","completion_criteria":"A representative multi-step before-save rule and repeated bounded reads cross the old cumulative boundary and reach the correct answer or exact review without a Continue/context-management action. Both native adapter shapes remain valid. Fresh context preserves the current user request, resolved intent, exact saved target/source/scenarios/proof and required recent results; dropped duplicated/transient data is not mistaken for authority or complete evidence. Same-turn renewal does not replay host mutations, resolve intent again, bypass resource reservations or auto-submit on page mount. Existing blocked work can be opened and continued through ordinary controls without hiding eligible saved review behind an internal context gate. Malformed, irreducibly oversized, cancelled, stale, budget-exhausted and uncertain work remains honestly bounded and safe. Focused/regression checks, independent technical/prose review and preserved-data delivery pass."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"}],"pull_requests":[]}

**Risk:** Elevated
**Complexity:** Moderate
**Reason:**
The intended app source/tests have a fresh Gray floor. Native continuation and checkpoint handling must change without changing host authority, durable outcome accounting or spending limits.

**Discovery:**
The provider serializes the complete native tool chain into each request and rejects it above 131072 bytes. Agent/service checkpoints also stop at 262144 bytes. The same CONTEXT_LIMIT reaches a visible manual continuation card and gates saved review. Each explicit user turn already clears native continuation and rebuilds current host context; the within-turn chain is the remaining cumulative path. Existing tests deliberately assert that bounded read chains stop at the byte cap. Existing current-proposal, evidence and conversation projections can be reused rather than adding a memory system.

**Material assumptions:**
Normal requests can fit through a smaller current-context projection or safe renewal in the existing loop. Byte limits must not be mislabeled as exact token counts; any model/token-budget claim requires real evidence. An irreducible or uncertain boundary still needs an honest failure, not a hidden infinite retry. The screenshot does not establish which of the shared context guards triggered.

**Plan:**
1. Reproduce and measure native request/checkpoint growth using scripted transports and real before-save source/scenarios. Trace every preflight, continuation, RESULT checkpoint and recovery caller. Identify duplicated/transient payloads versus state and tool evidence that must survive.
2. Obtain a bounded independent technical plan review. Prefer a small shared packing/renewal change in existing modules. Preserve native tool-call/result pairing, frozen intent, current host fences, full saved artifacts and definitive dispatch outcomes; do not add unaccounted provider passes or replay effects.
3. Implement after plan GO. Remove the forced context-management gate while retaining normal Send and safe recovery for real failures. Regress old blocked-state reentry, reading/input preservation and exact review without changing the restored layout.
4. Run focused adapter/agent/service/browser tests and adequate combined regression, align existing guidance, review the fixed contribution, then perform backed delivery and actual-app readback. Keep all paid-provider work and unrelated environment changes excluded.

**Verification plan:**
Ordinary uninterrupted authoring → native OpenAI/Anthropic scripted multi-step before-save and repeated-read journeys across the old boundary reach truthful completion.
Authority and preservation → exact target/source/scenario/proof and resolved intent survive packing; read/discuss cannot gain mutation authority; approval/activation stay separate.
Accounting and recovery → checkpoints retain SUCCESS/REJECTED/UNKNOWN correctly; no duplicate effects, extra intent classification, free provider requests or unsafe replay; stale/owner/cancel/budget fences remain.
Visible behavior → a saved blocked conversation reopens, ordinary Send works, eligible exact review is not context-gated, no automatic provider traffic on mount, and input/reading/layout remain stable.
Delivery → build/regression, independent result/prose review, fresh backup, exact served assets/source and unchanged saved data/settings.

**Plan review:**
Agent technical review: [Architect plan verdict](#architect-plan-verdict).
GO for bounded local packing/renewal in the existing agent loop, native-format handling in the provider, full-row checkpoint protection in the service and removal of the context-specific Studio gate. Keep wire/storage/token guards and reservations unchanged. Renew only from settled outcomes; preserve frozen intent, exact independent expectations and required read facts. Truncated current-proposal projections cannot recreate missing expectations. Require finite renewal/no-progress limits and the decisive checks below.

**Approvals:**
The user explicitly authorized fixing the internal context-management experience. On final-delivery confirmation, the user chose Publish and restart: commit/push the verified fix to main in both repositories and briefly restart the existing Daymark instance after a fresh backup, preserving current database/settings and code-only rollback. In-progress app edits were confirmed saved. No paid provider calls, unrelated role rollout, environment/tool changes or proposed feature implementation are authorized.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The developer accepted source/test ownership. Parent owns core records and existing human guidance. Both chats stay rooted at the shared workspace; source work uses the isolated checkouts. No additional designer round or new agents are needed for this correction.

Implementation is handed off for review. The source/test contribution digest is `a69ba9e5808199d8ad825508e6449d4227a67bb33558a42ef0a9cebfe988e047`. Developer evidence reports build PASS, 666 Node passes with 13 platform/opt-in skips, 73 expanded browser passes and 8 focused recovery passes. It uses scripted transports and real isolated compiler/lifecycle/SQLite, not live-model qualification. Nothing is committed or delivered yet. Documentation surface review completed; its three minimal clarifications are applied pending final fixed-diff review.

An implementation handoff is not completed delivery. Reconcile its fixed evidence, finish reviews and carry authorized publication and installed readback through closure rather than reassigning completed work. Role-trigger rollout stays deferred; this correction does not authorize unrelated guidance changes.

Parent verified all 42 implementation/boundary/build/evidence hashes and reran 117 affected Node checks: all pass, no skips. Full browser regression is running against that fixed contribution. Independent result review found a blocking preservation gap: renewal immediately after a successful recompile can lose the prior independent scenarios because compilation clears the current suite and earlier catalog snapshots are not retained. The existing native test renews only after scenarios are saved. Hold delivery. The same developer is offered a bounded root-cause correction and a real-host/native regression forcing renewal between recompile and scenario restoration, with byte-exact wrong independent expectations remaining review-ineligible. No source edits during the parent browser run; read-only tracing may proceed. This is enforcement of existing scope, not a new feature or cap change.

The parent browser command ended before application assertions: the wrapper selects a missing worktree-local Chromium. Reuse the existing isolated pinned browser with a process-only path and direct Playwright CLI; no browser install or persistent configuration change is needed. Source edit GO is sent for the correction. Prose content review passes; refresh the three parent-owned doc identities in the revised manifest. The final full browser run will use the corrected fixed bytes, not mixed revisions.

The P1 correction is frozen as contribution `75d42642114a398279647746a49f630a07874d8dc26f0cca4270cee4368daa2b`. Before tool effects, the service journals the owned immutable revision/suite identity. Discovery resolves its exact prior scenarios after compilation clears the current suite. Missing, truncated, oversized or mismatched prior suites stop safely. This also works without an explicit discovery call and across two recompilations. The prior expectations remain historical data, never current proof or a request to repeat effects.

Developer verification: 678 Node passes with 13 existing platform/opt-in skips, 165 focused passes, 16 native correction passes, build and diff checks pass. Parent verified 53 exact source/test/doc/build/evidence hashes and independently reran all 16 native correction cases: all pass with no skips. Both adapters retain deliberately wrong expectations byte-exact and review-ineligible, original intent/accounting, native pairing and lost-checkpoint restart safety. The final full browser suite runs against these frozen bytes using the existing isolated pinned Chromium. Architect delta review passes and closes P1. Prose content GO remains applicable; the refreshed manifest now contains current doc identities. No live update is claimed yet.

## Result review

Agent technical review: Independent architect fixed-contribution review and P1 delta verdict.
Reviewed revision: `75d42642114a398279647746a49f630a07874d8dc26f0cca4270cee4368daa2b`.
Verification adequacy: GO for the corrected contribution. The initial P1 is closed by durable immutable scenario provenance before tool effects and exact recovery after recompilation. Both-native real-host regressions cover two recompiles with/without discovery, deliberately wrong byte-exact expectations, missing/truncated/oversized prior suites, lost-checkpoint restart, native pairing, frozen intent, reservations and no replay. The reviewer verified implementation hashes at start/end without edits or test execution. Parent full browser verification passes all 279 cases in 15.6 minutes on the corrected fixed bytes. Source/doc/asset hashes still match the reviewed manifest. Backed live readback remains the final delivery gate.

Documentation review: Content GO for AI setup, lifecycle and verification guidance. Existing Send/navigation, authority, installation and frozen Keir guidance remain applicable. Parent-owned doc metadata is refreshed in the corrected manifest. Scripted-native/real-host evidence does not qualify a live model.

## Delivery continuation

Delivered [app revision `4edcf81e5f1b2be1d61541d5945bcfb24a46cacb`](https://github.com/rore/keir-demo-app/commit/4edcf81e5f1b2be1d61541d5945bcfb24a46cacb) to main and the existing demo after the explicitly approved backed restart. The fresh verified SQLite/settings backup matched current work before stopping the confirmed owning process. Source/assets alone changed; dependencies, data directory and URL stayed the same. Exact served HTML/JS/CSS and installed reviewed source were verified.

Post-restart and post-readback comparisons confirm all 22 tables, schema and settings unchanged. Actual saved-work readback at desktop and mobile preserves the frame, single Send, separate More, unsent input, reading position and focus. It made zero authoring turns and zero native mutations. No paid provider call, schema migration, provider/settings reset or rollback was needed. Any later rollback must restore code/assets only, never overwrite newer work with this backup.

Local verification is complete: build, 678 Node passes with 13 existing platform/opt-in skips, 165 focused passes, parent 16 native correction passes and all 279 browser cases. [Hosted verification](https://github.com/rore/keir-demo-app/actions/runs/43858504777) was triggered by publication and remains in progress at this record update; no hosted PASS is claimed.

The owning feature returns to queued solely for its unassigned second-provider live-qualification residual. Proposed multi-action and pre-save-patch slices remain unstarted. Compact evidence retains the fixed source/test/doc identities, verification logs, representative renders and preservation results without retaining dependencies or a full checkout. The completed app task checkout and its registration are removed. Core's task checkout is removed after publishing this record, with final cleanup readback retained alongside private delivery evidence. Canonical source checkouts and unrelated changes remain untouched.

The scripted-native real-host before-save reproducer reaches REVIEW_AVAILABLE with one post-save discovery read. One additional discovery read instead stops at CONTEXT_LIMIT before simulation in both adapters. Exact source and four independent cases remain saved at draft version 2. All completed dispatches are SUCCESS, with six charged provider reservations and no uncertain effects. Largest sent requests before the stop are 109469 bytes (OpenAI) and 108182 bytes (Anthropic). This is deterministic scripted transport evidence, not paid-provider qualification or a captured live screenshot request.

## Architect plan verdict

Independent architect review: GO, Elevated/Moderate. The reviewer inspected the agent loop, provider serialization, service checkpoints/reentry and Studio recovery callers without edits or test runs. The review does not certify the reproducer's results.

- Keep native call/result pairs and required reasoning blocks together. Never splice opaque reasoning or send orphan results. Measure the actual serialized envelope.
- Renew locally before request preflight and oversized checkpoints, using settled state and existing host projections. Preserve exact source/cases, current user request, frozen intent, handles, repair attempts, read evidence and definitive dispatch receipts. Never renew through UNKNOWN or checkpoint uncertainty, replay tools or refund uncertain reservations.
- Keep 131072-byte request/response guards, 262144-byte saved-row ceiling, token guards and conservative reservations unchanged. Bound renewal/no-progress. Irreducible input stops honestly.
- Verify both native adapters across the old cumulative boundary, exact scenarios/read facts and frozen intent. Exercise provider/tool RESULT and terminal persistence, rejected and unknown outcomes, fault injection, cancellation, stale owner/draft, malformed/irreducible input, exhausted renewal and budget.
- Verify old blocked work allows ordinary Send and independently validated saved review. Preserve lost-response recovery/input. Mount/reload sends no provider requests; running, uncertain, stale, historical and owner protections remain.
