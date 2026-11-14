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

**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

The earlier redesign is rejected. Passing tests did not validate the requested interaction. Restoration is the deliverable; spacing is a later CSS-only step within the restored structure. No additional design or generic permission round is planned.

The developer accepted source/test ownership and traced the old/current flows read-only while the architect reviewed the narrow plan. Plan GO clears implementation. Parent owns the three affected existing human-guide updates and all core records; no concurrent source edits are planned.
