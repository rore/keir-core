# Preserve the application frame when opening Studio

Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md). Baseline: Daymark `7925fb06e07057d3d4fde1392e81002945c1259a`.

<!-- agent-workflow:start -->
**Outcome:**
Opening a customization keeps the same application frame, sidebar and top bar as the task and customization-list screens.

**Target:**
Daymark Studio navigation and the retained demo.

**Scope:**
Move the Studio reading-width limit off the shared application shell and onto Studio content; add a cross-route geometry regression and correct the layout guidance.

**Constraints:**
No authoring, lifecycle, data, settings, dependencies or task behavior changes. Preserve readable Studio content, compact target/status, one Send, inline review, input and mobile usability. No paid AI calls. Preserve unrelated changes and use isolated tooling. The retired handoff file is not used.

**Completion criteria:**
Tasks, customization list, existing Studio, new Studio and return navigation have identical shell/sidebar/top-bar geometry at the same viewport. Studio content remains bounded and responsive. The regression fails on the prior CSS and passes with the fix. Build and affected browser checks pass, required reviews are accepted, and the existing demo is updated with a fresh backup and unchanged-data verification.

**Requirement baseline:**
{"source":"reported cross-route application-frame regression","outcome":"Opening a customization keeps the same application frame, sidebar and top bar as the task and customization-list screens.","scope":"Move the Studio reading-width limit off the shared application shell and onto Studio content; add a cross-route geometry regression and correct the layout guidance.","constraints":"No authoring, lifecycle, data, settings, dependencies or task behavior changes. Preserve readable Studio content, compact target/status, one Send, inline review, input and mobile usability. No paid AI calls. Preserve unrelated changes and use isolated tooling. The retired handoff file is not used.","completion_criteria":"Tasks, customization list, existing Studio, new Studio and return navigation have identical shell/sidebar/top-bar geometry at the same viewport. Studio content remains bounded and responsive. The regression fails on the prior CSS and passes with the fix. Build and affected browser checks pass, required reviews are accepted, and the existing demo is updated with a fresh backup and unchanged-data verification."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated
**Complexity:** Simple
**Reason:**
Unclassified CSS/test paths have an Elevated floor. The change is presentation-only and does not affect authority or persistence.

**Discovery:**
The added `.workspace:has(.studio-main)` rule caps the shared shell at 1060px and adds automatic side margins whenever Studio mounts. The same shell contains the sidebar and top bar. Studio itself has no local width cap. All Studio entry paths trigger the same selector; changing individual navigation handlers would miss the root cause.

**Material assumptions:**
The existing approximately 850px Studio reading surface can keep its width inside the normal full-width main pane without changing the application frame or any interaction state.

**Plan:**
1. Add a fixture-backed cross-route geometry test covering task/list/existing/new/return navigation at wide desktop, ordinary desktop and mobile sizes; run it against the old bundle.
2. Replace the shell-scoped rule with a local `.studio-main > .read-studio` width limit. Correct the existing design paragraph; change no other styling or behavior.
3. Build and run the new geometry regression plus existing Studio interaction/layout checks. Compare actual rendered task/list/editor frames, not only isolated editor captures. Obtain bounded plan/result and documentation review.
4. Publish and update the retained demo with fresh backup, exact asset and unchanged-data checks. Preserve compact evidence and remove only completed task checkouts.

**Verification plan:**
Cross-route frame stability → compare same-viewport shell, visible sidebar and top-bar rectangles across navigation; require full viewport width and local Studio width at most 850px with no horizontal overflow. Assert exact editing identity and one Send. Inspect a wide paired render.
Interaction preservation → run existing lifecycle/reading/focus browser assertions and build; reuse unchanged semantic/Node qualification for this CSS-only delta.

**Plan review:**
Agent technical review: independent architect PLAN GO for the exact single-selector correction and cross-route geometry checks. Confirmed Studio is a direct child of `.studio-main`; existing flex/min-size/overflow behavior supports the local width cap. Elevated/Simple accepted. Unchanged semantic and Node evidence can be reused; result acceptance waits for implementation and rendered checks.

**Approvals:**
The reported regression directs correction of the shipped layout. Existing delivery/demo authority applies; no paid calls or unrelated environment changes are authorized.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Evidence

Prior tests checked Studio in isolation and missed movement of the surrounding application frame. The new cross-route invariant is the regression check for that acceptance failure.

The production diff replaces one CSS rule: the 1060px limit and margins no longer apply to `.workspace`; the direct Studio content keeps its approximately 850px reading width. The new test failed against the prior bundle at all three viewport sizes, including the mobile border shift. With the correction it passes tasks/list/existing/new/return geometry and unsent-input preservation at 2000×1040, 1440×900 and 390×844. Manager inspected paired wide collection/editor renders and confirmed the unchanged sidebar/top bar.

Build passed. Targeted browser checks pass across the three new geometry cases, three real authoring journeys, six rename cases and 25 layout/business cases. One initial rename teardown failed because concurrent test runs shared an artifact directory; its isolated rerun passed with a separate output directory. No behavior assertion was weakened. These are 37 targeted passes across runs, not a new full-suite claim. The previous 649-Node/272-browser hosted evidence remains applicable to unchanged authoring/lifecycle code.

Documentation review: GO for the single corrected layout paragraph; other human guides and instructions are unchanged. The live correction can replace static assets without restarting the server, preserving active sessions; fresh data/asset backup and exact served-asset/data readback remain required.

## Result review

Agent technical review: independent architect RESULT GO, no required corrections; fixed candidate manifest SHA-256 `c8f852e36645df7232429e86be6dae31ebf8be6e0730bb0dfd8aa0c8f6ef97b1`.

Reviewed revision: baseline `7925fb06e07057d3d4fde1392e81002945c1259a` plus the exact one-rule CSS replacement, regression and guide paragraph; stylesheet SHA-256 `e42cd6a86506a22ccfc859179ba2695a82c844bb598f2e74d404ddecf3512f78`.

Verification adequacy: reviewer inspected the complete delta, red/green and affected logs, and paired wide renders. Strict cross-route frame geometry, local width, unsent input, one Send and zero provider requests are covered. The 37 affected passes adequately qualify this CSS-only repair; unchanged semantic/authority/Node evidence is reused. Normal hosted CI will run but is not yet certified green. No additional full-suite waiting gate is required before the already-authorized data-preserving demo correction.

## Delivery

Published as [Daymark `8a2dc5e`](https://github.com/rore/keir-demo-app/commit/8a2dc5ea01007996a44fa4ffc236c1e4237a6cb0). The retained demo serves the exact tested static assets without restarting the server or changing active sessions. A fresh database/settings/lock and prior-asset snapshot was taken before the update. All 22 tables/schema, database bytes, settings and 12 tasks remain unchanged after live browser readback. No authoring/provider request occurred.

Live paired collection/existing-customization checks at 2000×1040 and 390×844 confirm identical workspace/sidebar/top-bar rectangles, with the workspace at the left edge and full viewport width. Manager inspected the actual saved conversation in the corrected frame. [Normal hosted CI](https://github.com/rore/keir-demo-app/actions/runs/43615521351) is running; its outcome is not yet claimed. Only this layout regression is resolved; the earlier whole-UI completion claim did not cover this invariant.

Workflow validation has no blocking findings. Its single-checkout commit-order advisory counts the owning feature documentation as code; the core Work Record commit `353b5d3` actually preceded application code commit `8a2dc5e` in the separate repository.
