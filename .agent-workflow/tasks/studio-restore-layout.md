# Restore Studio, then trim spacing

Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md). Change baseline: Daymark `8a2dc5ea01007996a44fa4ffc236c1e4237a6cb0`. Layout reference: `eb31d2541c350320fe99b2869c3e0bde42f6e985`, before the rejected visual rewrite.

<!-- agent-workflow:start -->
**Outcome:**
The previous Studio layout and navigation are restored, with one Send, preserved saved work and less excess spacing.

**Target:**
Daymark Studio and the retained demo.

**Scope:**
Restore the previous component layout, management/current-use inspection and responsive pane behavior. Retain single-Send auto intent, exact pending recovery and correctness fixes. After restoration, reduce padding/margins only. Align existing navigation coaching, docs and affected tests.

**Constraints:**
No new layout, control relocation, interaction design, dependencies, Core semantics, schema, provider replay or memory. Do not roll back the authoring/authority backend, owner isolation, exact approval/activation, saved history/input or active/draft proof binding. Keep readable type and the regular full-width application frame. Preserve current data/settings and unrelated work. Do not use the retired handoff file.

**Completion criteria:**
More opens the previous separate management view, never inside the conversation log. Closing it restores conversation reading/focus and unsent input. Existing review, current-use, history, rename and disable locations/behavior match the previous layout at desktop, short/mobile and zoom. One Send still submits auto intent and exact recovery accepts old/new bodies. Spacing changes do not move controls or alter behavior. Build, affected regression and independent technical/docs review pass; the backed demo update preserves all saved work/settings.

**Requirement baseline:**
{"source":"authorized restoration of previous Studio with spacing-only polish","outcome":"The previous Studio layout and navigation are restored, with one Send, preserved saved work and less excess spacing.","scope":"Restore the previous component layout, management/current-use inspection and responsive pane behavior. Retain single-Send auto intent, exact pending recovery and correctness fixes. After restoration, reduce padding/margins only. Align existing navigation coaching, docs and affected tests.","constraints":"No new layout, control relocation, interaction design, dependencies, Core semantics, schema, provider replay or memory. Do not roll back the authoring/authority backend, owner isolation, exact approval/activation, saved history/input or active/draft proof binding. Keep readable type and the regular full-width application frame. Preserve current data/settings and unrelated work. Do not use the retired handoff file.","completion_criteria":"More opens the previous separate management view, never inside the conversation log. Closing it restores conversation reading/focus and unsent input. Existing review, current-use, history, rename and disable locations/behavior match the previous layout at desktop, short/mobile and zoom. One Send still submits auto intent and exact recovery accepts old/new bodies. Spacing changes do not move controls or alter behavior. Build, affected regression and independent technical/docs review pass; the backed demo update preserves all saved work/settings."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated
**Complexity:** Moderate
**Reason:**
Restoring view composition touches navigation and exact-review presentation; gray source/test paths have an Elevated floor. Backend authority and storage remain unchanged.

**Discovery:**
The rejected rewrite moved the existing preview/management section inside the conversation log, hid the previous pane behavior and relocated controls. The earlier component remains in Git. The same rewrite also introduced single-Send compatibility and some correctness fixes, so a blanket revert would lose required behavior. Navigation coaching names the rejected controls and must match the restoration.

**Material assumptions:**
The existing pre-rewrite component is the restoration source, not a starting point for another design. Current single-Send/recovery and proof-freshness changes can be retained as small explicit exceptions. Density needs only a few spacing rules, not new wrappers or state.

**Plan:**
1. Review the exact old/current component and callers. Restore the previous DOM/pane/control arrangement while retaining auto-intent request/recovery support and independent ownership/proof fixes. Keep backend resolution, dispatch and persistence unchanged; alter coaching only where control labels/locations change.
2. Verify the restored More/open/close path outside the conversation before polishing. Then trim only existing padding, margins and message gaps; retain readable text and existing responsive behavior. No designer/prototype round or new interaction is needed.
3. Preserve meaningful tests and scripted intent accounting. Add a direct More-not-in-log regression with input/reading/focus preservation. Exercise current-use versus draft, separate approval/activation, owner switches and frame stability at representative sizes. Update affected existing guides, not new pages.
4. Review the fixed contribution, publish and update the existing demo after fresh backup and exact assets/data/settings verification. Reuse unchanged qualified backend evidence. Keep CI findings advisory; do not invent an extra approval gate.

**Verification plan:**
Restored interaction → real-host More is outside the log, separate pane and return work at desktop/mobile, and old control locations remain.
Single Send and safety → new free text submits auto, pending auto/legacy requests recover exactly, current-use proof remains read-only and owner-bound, approval and activation stay separate.
Spacing only → compare restored baseline with final CSS diff/renders; no control movement, reduced padding/gaps and unchanged readable type.
Preservation and delivery → build, affected/full regression as needed, bounded technical/docs review, fresh demo backup and unchanged saved-data/settings readback.

**Plan review:**
Agent technical review: independent architect PLAN GO, Elevated/Moderate, after comparing `eb31d25` and `8a2dc5e` component/CSS/coaching and reading the exact record. Restore sibling chat/divider/inspection, resizer/mobile switching and previous control locations; remove the local 850px cap while preserving the full application frame. Retain auto/legacy recovery, one Send, composer growth, exact owner/source/proof fences and separate decisions. Bind timing coverage to each reviewed active/draft revision. Restore active-pane opening and adapt inline-only scrolling so it cannot move the sibling conversation or overwrite its return anchor. Verification plan accepted; unchanged backend contracts are reused. No new design or user-approval gate.

**Approvals:**
The visual rollback, retention of single Send/saved work and subsequent spacing-only polish are explicitly authorized. No paid provider calls or unrelated environment changes are authorized.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The earlier redesign is rejected. Passing tests did not validate the requested interaction. Restoration is the deliverable; spacing is a later CSS-only step within the restored structure. No additional design or generic permission round is planned.

The developer accepted source/test ownership and traced the old/current flows read-only while the architect reviewed the narrow plan. Plan GO clears implementation. Parent owns the three affected existing human-guide updates and all core records; no concurrent source edits are planned.

Documentation-role review accepted the three prose deltas with one mechanical correction: retain the existing Go to approval instruction before Approve this change in the walkthrough. That correction is applied. Other guides remain valid because single Send, model data/cost, recovery and authority contracts are unchanged. This prose review does not certify source, tests or delivery.

The first restored More journey established pre-density real-host evidence at desktop, mobile and short sizes. Parent inspected actual desktop/mobile captures and the regression assertions: management is outside the log, chat/divider/inspection are siblings, the desktop resizer remains visible, mobile switches panes, and closing restores exact reading position, focus and unsent text without changing shell geometry or saved customization state. The restored CSS and source hashes were preserved before density changes. Final verification and delivery below supersede this early checkpoint.

The density diff is exactly four CSS spacing rules: header padding, message bottom margin, inspection-sheet padding and inline decision-card padding. The other four changed source files match the pre-density hashes. The focused run passes 21 browser checks, including active/draft timing in both directions, stale/owner proof fences, the three single-Send journeys, More return behavior and full-width shell geometry. The focused Node run passes 60 checks. These scripted results do not qualify live-model interpretation.

Independent architect RESULT GO found no source blockers. All five source hashes matched the result manifest at review start and end. The review confirms restoration against the previous layout, retained recovery/proof/timing fences and exactly four spacing-only rules. Parent subsequently closed the identified combined internal-inspection return-state coverage gap. The initial compatibility run passed 51 of 52 cases; its failed-review navigation test was corrected without weakening mismatch assertions. The corrected group and final hosted suite pass.

## Evidence

- Fixed production source identity: `522c4d95ed283104bf4f1b0324e6ed194983f0e8fbcd1c175777fd7c08e064ae`, based on Daymark `8a2dc5e`. All five reviewed source-file hashes remain unchanged.
- Parent rebuilt successfully: TypeScript and Vite pass, producing `index-CWuwDG5l.js` and `index-PpDPp7LF.css`.
- Full local Node run: 648 passed, 13 skipped, zero failures. Skips are explicit platform/slow qualification exclusions, not removed checks.
- Full local browser run: 278 passed and one test failed because it still opened More and attempted to scroll the now-hidden mobile conversation. The test now uses the restored status-strip disable action and actual mobile page scroller. Its complete five-test exact-review group passes afterward. Production source did not change. This is full-run-plus-targeted-correction evidence, not a claimed single green 279-case run.
- The internal-inspection coverage limit is closed by extending the existing scroll/splitter journey through More, saved source and saved results, then asserting exact nonzero reading position, original focus, unsent text and unchanged saved state. The focused run and full-run instance both pass.
- The developer's focused batches also cover native 200% browser zoom, one Send/Enter/legacy recovery, all supported triggers, current-use isolation, rename/history and separate exact approval/activation. No paid provider qualification occurred.
- Final hosted verification passes on Daymark `4f74a4a`: build, 649 Node checks with 12 explicit skips, and all 279 browser cases in one run. The local-only extra skip concerns POSIX file modes and makes no Windows ACL claim.
- Fresh preservation backup and read-only checks pass: 22 tables, valid integrity/foreign keys, no active authoring or pending native work, exact current database/settings bytes captured. The user approved restarting the existing demo after confirming transient edits were saved.

## Result review

Agent technical review: independent architect RESULT GO, reviewing the fixed five-source-file manifest against the previous layout and the recorded plan. No source blockers or concrete sibling-scroll ownership defect were found.

Reviewed revision: Daymark `4f74a4a`, with production source identity `522c4d95ed283104bf4f1b0324e6ed194983f0e8fbcd1c175777fd7c08e064ae` unchanged from the reviewed `8a2dc5e`-based contribution.

Verification adequacy: the restored navigation, controls, single Send and authority/proof/timing contracts have real-host regression coverage. Parent closed the identified combined internal-navigation return-state gap and corrected the remaining test-only mobile-navigation assumption without changing source. Prose review is accepted with its walkthrough correction. Passing tests and rendered inspection are bounded technical evidence, not general human usability acceptance.

Trigger 2 dropped: scope/progress misjudgments were agent execution errors, not Agent Workflow defects.

## Delivery

[Daymark `4f74a4a`](https://github.com/rore/keir-demo-app/commit/4f74a4ac7b7346859ee02d73316642edda4c85ff) is published on main and running at the existing demo URL after the approved backed restart. Served HTML/JavaScript/CSS match the tested build; the five installed source files match the reviewed manifest. The unchanged database and settings bytes, all 22 table snapshots, schema, integrity and foreign keys were verified again after restart.

Actual saved-conversation readback passes at desktop and mobile: More opens outside the log, the desktop divider and mobile pane switch work, closing restores input/reading/focus, and the application frame is unchanged. Only local demo sign-in was posted; zero authoring turns or domain writes occurred. Parent inspected the actual saved-conversation and More renders. This is bounded restoration acceptance, not a general human usability study.

[Hosted verification](https://github.com/rore/keir-demo-app/actions/runs/43626858751) passed build, 649 Node checks with 12 skips, and all 279 browser cases. The live stage's Git revision is current and its reviewed source bytes and tested assets remain exact. Compact private preservation/verification evidence is retained outside the task checkouts. Only normal task-checkout retirement follows publication of this final record; no product redesign, implementation or approval step remains.
