# Studio host awareness

Owning feature: [conversational authoring](../../roadmap/features/conversational-authoring.md). Daymark baseline: `95a0873205304bbd112da62388ad7a7256527c72`. Core baseline: `2cb21a8479d6dd85763063873438fe9c2032abe7`.

<!-- agent-workflow:start -->
**Outcome:**
The Studio assistant can discuss the signed-in person's existing customizations and relevant tasks, ground suggestions in inspected evidence, and guide an explicit choice of editing target without changing rules through discussion.

**Target:**
Daymark host-side conversational assistant.

**Scope:**
Bounded read-only customization discovery/details and targeted task context; assistant instructions and Studio target handoff; affected tests and existing user/data-flow documentation. Core changes are limited to this record and the owning roadmap.

**Constraints:**
No Core semantics, new dependencies, schema migration, general memory store, cross-user data, automatic target switch, task write, approval or activation. Preserve existing compiler, simulation, review and separate activation. No live-provider requests without separately approved provider/model, credential and spend cap. Preserve running demo, saved data/settings and unrelated work.

**Completion criteria:**
A new chat can discover two existing items or an empty collection accurately; distinguish a differing draft, approval and active revision; inspect bounded relevant task evidence and disclose missing/partial data; disambiguate names; offer an explicit existing-item choice without retargeting writes. Owner isolation, untrusted content and discussion-only nonmutation pass tests. Business-user review explains knowledge, evidence and next action. Report actual scripted/live evidence and a PR under existing review/merge rules.

**Requirement baseline:**
{"source":"user request: Studio customization and task awareness","outcome":"The Studio assistant can discuss the signed-in person's existing customizations and relevant tasks, ground suggestions in inspected evidence, and guide an explicit choice of editing target without changing rules through discussion.","scope":"Bounded read-only customization discovery/details and targeted task context; assistant instructions and Studio target handoff; affected tests and existing user/data-flow documentation. Core changes are limited to this record and the owning roadmap.","constraints":"No Core semantics, new dependencies, schema migration, general memory store, cross-user data, automatic target switch, task write, approval or activation. Preserve existing compiler, simulation, review and separate activation. No live-provider requests without separately approved provider/model, credential and spend cap. Preserve running demo, saved data/settings and unrelated work.","completion_criteria":"A new chat can discover two existing items or an empty collection accurately; distinguish a differing draft, approval and active revision; inspect bounded relevant task evidence and disclose missing/partial data; disambiguate names; offer an explicit existing-item choice without retargeting writes. Owner isolation, untrusted content and discussion-only nonmutation pass tests. Business-user review explains knowledge, evidence and next action. Report actual scripted/live evidence and a PR under existing review/merge rules."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"}],"pull_requests":["https://github.com/rore/keir-demo-app/pull/11"]}

**Risk:** Elevated
**Complexity:** Moderate
**Reason:**
New owner-scoped read tools extend existing provider context. Paths are gray under the shared Redline policy; auth, persistence schema and approval authority remain unchanged. Meaningful privacy, grounding and target-confusion risks require independent technical review and isolation/nonmutation checks.

**Discovery:**
The agent currently exposes contract/project discovery but no collection/task-read tools. Its discovery is bound to one item; currentUse intentionally omits details for another selected item. The service binds writes to ctx.extensionId and fences owner/session/run before tools. Each explicit turn refreshes native context. A new chat admits an empty item, which must not be mistaken for an empty saved collection.

**Material assumptions:**
Existing lifecycle state/source/evidence and native task services can supply bounded owner-scoped reads without schema changes. Stop and revisit if they cannot. Provider text remains fallible: trust-boundary instructions and deterministic read-only tools prevent authority expansion, not a claim of infallible answers. No existing live-qualification allowance is assumed valid for this new feature.

**Plan:**
1. Inspect lifecycle projections and Studio navigation; independently review exact context/target design before code.
2. Add small Daymark-owned read bindings using existing lifecycle/native services. Discover bounded item metadata, inspect actual revision behavior/evidence with distinct draft/approval/current-use facts, and query bounded task summaries/results with scope/coverage. Keep owner IDs host-bound and reject extra/invalid arguments.
3. Give each turn a small fresh host-awareness context, not a task dump. Teach natural grounded replies, uncertainty and untrusted task text. New read tools cannot mutate or replace the bound editing target.
4. Reuse existing item navigation for explicit review/change choice. If a model offers a specific item, expose a host-validated user action; clicking, not discussion, selects it. Preserve normal draft/check/approval/activation flow.
5. Add focused service/agent/API/browser cases and update existing data-flow guidance. Run affected and full regression/build where practical; obtain independent technical, documentation and business-user UX result reviews.
6. Create the requested PR, reconcile review findings, follow existing delivery authority, update the owning roadmap and preserve or clean isolated checkouts appropriately.

Settled caller-visible behavior for review:
- Send/Enter discusses only. A separate Prepare changes action on the same composer explicitly permits draft preparation/checks for the currently opened item. Both retain one conversation. Existing exact pending request bodies remain replayable; omitted intent on legacy API requests retains its former preparation meaning. New UI requests always declare discuss or prepare intent. No keyword heuristic grants mutation authority.
- Discussion exposes discovery, clarification and exact item-opening offers, but not compile, scenario replacement or simulation. Dispatch rejects unsolicited mutation calls even if a provider returns a non-advertised tool. Tool bindings keep owner/session/run and ctx.extensionId fences. Conversation messages, durable budget receipts and the existing new-chat placeholder admission are permitted bookkeeping, not rule changes.
- Each explicit turn gets only fresh editing-target identity, intent and a count of other saved nondeleted items. New empty conversational placeholders do not prove no earlier saved work. Names/behavior/task rows are fetched on demand, not included as a database dump.
- List customization metadata with the existing owner-bound collection cursor: at most 20 rows and 16 KiB per tool result. Inspect one exact item: separate draft approval, exact selected revision (possibly another item), actual eligibility and version-bound artifact-derived behavior/check summaries; at most 32 KiB. No automatic source/artifact/history or conversation-excerpt forwarding. Unavailable/partial projections are labeled, never substituted from names or a different revision.
- Search one owned project through native bounded search, at most 25 tasks. Allow a blank search only as an explicit project-scoped recent/sample read; preserve the public search endpoint's existing nonempty-filter rule. Return title, status, tags, priority, dates and hasNotes. Notes are omitted by default; an explicit includeNotes choice returns a bounded excerpt of at most 256 Unicode code points with a truncation flag. Whole result at most 16 KiB; expose returned count, filters, next cursor and partial coverage, not a database-wide pattern claim. Reject unknown/foreign projects, cursors and extra authority fields without leaking content.
- An opening offer carries a host-validated item identity/name, not a model-supplied URL or an instruction to replay elsewhere. Its button opens that item only after a fresh owner/access check, creates a fresh navigation entry, preserves the previous chat/unsent input, and never resends the old request. Deleted/foreign/stale responses cannot select an item. The destination starts from its normal fresh handles.
- Keep existing per-request provider byte preflight and cumulative continuation checks. Repeated read pages count against those same limits and reservations. Task/name/notes strings remain untrusted data in structured tool output, not policy.

**Verification plan:**
- Two saved items, empty collection, ambiguous names, differing draft/active/approved → scripted provider through actual host service and lifecycle fixtures.
- Relevant versus insufficient task evidence, bounded filters/pages/bytes and truthful coverage → native-data/read-tool tests and provider-boundary assertions.
- Foreign IDs/cursors, user switch and prompt-like task/name content → owner/session isolation and data-trust tests; no auth change.
- Read-only discussion and target handoff → snapshot native rows, drafts, approvals and selections before/after; assert no compile/simulate/write tool from read requests and no automatic UI target switch.
- Business journey → browser assertions and independent rendered review of knowledge/evidence/next choice, explicit item opening, preserved draft and approval/activation controls.
- Adversarial provider, stale target and oversized data → unsolicited compile/scenario/simulate rejection; selected-other-item and exact-proof cases; deletion/owner-switch/delayed response; pending/unsent preservation; repeated pages under result/cumulative request limits.
- Combined acceptance → build, focused and full Node/browser regression plus independent result review. Live interaction only if separately authorized; otherwise label scripted evidence and the live gap explicitly.

**Plan review:**
Agent technical review: independent architect PLAN GO for the settled contracts, record SHA-256 `b8d170610bc8cc070d3915cd5f0af3731d7f5a36fa397b2b9fa74ebe445615ba`, at the named baseline revisions. All four initial corrections resolved. Caller/host-owned intent, bounded scope, cumulative limits, exact revision meaning, adversarial tests and open-only handoff accepted. This is plan acceptance, not result evidence.

**Approvals:**
Not required at this risk level. The user explicitly requested implementation and a PR; paid live-provider qualification remains separately unapproved.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Discovery and independent plan review complete. The manager owns this core record/roadmap and Daymark agent/UI integration. The demo developer contributes backend read bindings and focused service tests in the same isolated task checkout, with disjoint file ownership. Unrelated dirty canonical checkouts and the requested running demo are untouched.

Implementation now covers bounded owner-scoped reads, exact revision projections, read-only discussion dispatch/binding fences, explicit Open navigation, provider disclosure and focused tests. The backend contribution passed 94 focused checks. Combined testing found and corrected test-provider parsing of the new structured message envelope without changing transcript/refusal assertions. Existing request-size regression also caught prompt overhead; repeated host coaching/tool descriptions were shortened while retaining the pinned guide, scenario coverage and the unchanged 131,072-byte request ceiling.

The application change is `0151422ad6ebc8f4a0b7b7dfe786fe7c0f6d77c0`, submitted as [PR 11](https://github.com/rore/keir-demo-app/pull/11). Follow-up `1844973e91013c660f55f2c898da72622712f12b` changes only four existing browser-test files: strict expectations now include discussion intent, and an intended preparation journey uses Prepare changes. All 24 originally reviewed files remain byte-identical. Independent narrow review accepted that test delta without correction. Final hosted regression passed. Review summaries, top-level comments and inline threads were empty on fresh readback. The PR was marked ready and merged as `eb31d2541c350320fe99b2869c3e0bde42f6e985`; the merge tree equals the reviewed candidate.

Documentation review found generic Continue/Try again incorrectly granted preparation intent; it now resumes discussion only, with exact pending-request retry semantics retained. Independent rendered/business-user review found mobile target ambiguity; the composer now names the bound editing target and explains that another item must be opened first. Both have focused browser assertions and independent correction-readback GO. No live-provider requests have been authorized or made. Live-provider qualification requires separate approval for this increment.

Trigger 3 dropped: invocation mistakes, not a workflow defect. Existing workflow commands and templates worked after correction; no policy or skill change was needed.

## Evidence

Combined build/type check passed. Full local Node regression: 636 tests, 623 passed, 13 skipped, zero failures. Six new browser scenarios passed, including both review corrections. The full local browser run passed 256 cases and exposed 11 stale test expectations/interactions. All 11 passed after the test-only correction, covering all 267 cases on unchanged product code. No cases were removed or skipped. Final [hosted regression](https://github.com/rore/keir-demo-app/actions/runs/43588156233) passed on `1844973`: build, 624 Node passes, 12 opt-in skips, zero failures, and all 267 browser cases in one run. The complete job took 11 minutes 12 seconds within its 25-minute ceiling. All provider decisions/transports in these checks are scripted; host reads, lifecycle, compiler, simulation and browser operations are real.

The retained demo serves the merge-equivalent reviewed tree. Fresh backup, exact HTML/two served assets, new discussion/preparation controls, and readback across all three demo accounts passed. All 22 table contents/schema, database bytes and provider settings remained unchanged across the update and browser verification. Existing four projects and 12 tasks remain. No migration, paid provider call, allowance change or data reset occurred. A verification-script read initially omitted the required session-context header and failed safely; corrected authenticated readback passed without product changes.

## Result review

Agent technical review: independent non-implementer architect static GO against the exact 24-file source freeze at `0151422`. Owner-bound reads, strict argument/scope checks, both discussion nonmutation fences, exact evidence attribution, fresh Open navigation, legacy replay, public search compatibility and implementation size were accepted without required correction.

Reviewed revision: `0151422ad6ebc8f4a0b7b7dfe786fe7c0f6d77c0`. Its 24 changed files remain byte-identical at `1844973e91013c660f55f2c898da72622712f12b`. Independent narrow review verified the clean final 28-file manifest and accepted the four additional test-only files. Strict assertions and all cases remain intact; the split local browser evidence is adequate for the correction, but is not represented as one final green full-suite run.

Verification adequacy: full Node coverage and real-host/scripted-model end-to-end checks exercise the reported false-empty-collection case, exact active/draft/approval distinctions, relevant/insufficient/partial task evidence, malformed and foreign reads, hostile content, unsolicited mutations, cumulative limits, explicit target navigation, stale/deleted/late responses and preserved drafts/approval/activation. Independent documentation and rendered business-user correction reviews pass. Final full hosted regression and destination verification pass. The manager accepts this increment as delivered; the owning roadmap returns to queued for its separate unassigned provider-compatibility residual. No live-model quality or provider-compatibility claim is made.

The completed application task worktree and replaced preview were normally removed after preserving committed source, compact test/review evidence and the fresh recovery snapshot. The current preview is retained because it serves the demo. The isolated record checkout has no further assignment and will be normally removed after this record and roadmap are pushed; unrelated dirty checkouts remain untouched. Collaborators have removed completed participation; only the manager's closing association remains until delivery reconciliation finishes.
