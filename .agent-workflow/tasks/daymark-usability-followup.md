# Daymark usability correction

Identity: `agent-workflow:daymark-usability-followup`. Owning feature: [Daymark visual polish](../../roadmap/features/daymark-visual-polish.md).
App baseline: `c4d503a4c3f3c4898f1df30cdecb05fd7f3b2d25`. Core baseline: `422d7b508306eacb3dc0bce4b77bf081f12fc2cf`.

<!-- agent-workflow:start -->
**Outcome:**
Person switching is direct, only task content scrolls in Tasks, and chat has demonstrably useful space.

**Target:**
Daymark workspace UI and its illustrated documentation.

**Scope:**
Correct person selection, Tasks scroll ownership and scrollbar styling, and chat allocation in existing components/styles. Add focused regression and refresh affected documentation captures.

**Constraints:**
Commit and push the verified correction and documentation in both repositories. No deployment, live app change, installed-tool change, paid call, new dependency, backend or schema change. Preserve supported controls, complete evidence, separate approval/activation, keyboard/focus, input, reading position and saved work. Preserve the existing documentation refresh.

**Completion criteria:**
Person switching has no nested picker. Scrolling a long task list leaves project heading, project tools, quick-add and search stationary. Scrollbars fit the existing visual language and remain usable. Identical conversation, viewport and reading position measurements compare corrected chat with published and pre-redesign builds, including short and narrow windows. Focus/input/reading and task search/paging/navigation regressions pass. Affected screenshots and text match the corrected UI, with independent technical and rendered review.

**Requirement baseline:**
{"source":"Owner usability feedback following the screenshot refresh","outcome":"Person switching is direct, only task content scrolls in Tasks, and chat has demonstrably useful space.","scope":"Correct person selection, Tasks scroll ownership and scrollbar styling, and chat allocation in existing components/styles. Add focused regression and refresh affected documentation captures.","constraints":"Repository edits and isolated tests only. No commit, push, deployment, live app change, installed-tool change, paid call, new dependency, backend or schema change. Preserve supported controls, complete evidence, separate approval/activation, keyboard/focus, input, reading position and saved work. Preserve the existing uncommitted documentation refresh.","completion_criteria":"Person switching has no nested picker. Scrolling a long task list leaves project heading, project tools, quick-add and search stationary. Scrollbars fit the existing visual language and remain usable. Identical conversation, viewport and reading position measurements compare corrected chat with published and pre-redesign builds, including short and narrow windows. Focus/input/reading and task search/paging/navigation regressions pass. Affected screenshots and text match the corrected UI, with independent technical and rendered review."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:daymark-visual-polish"}],"pull_requests":[]}

**Behavior changes:**
[{"target":"task-context.constraints","classification":"requirement-change","before":"Repository edits and isolated tests only. No commit, push, deployment, live app change, installed-tool change, paid call, new dependency, backend or schema change. Preserve supported controls, complete evidence, separate approval/activation, keyboard/focus, input, reading position and saved work. Preserve the existing uncommitted documentation refresh.","after":"Commit and push the verified correction and documentation in both repositories. No deployment, live app change, installed-tool change, paid call, new dependency, backend or schema change. Preserve supported controls, complete evidence, separate approval/activation, keyboard/focus, input, reading position and saved work. Preserve the existing documentation refresh.","reason":"The owner authorized publication after completion of local qualification.","impact":"Allows commits and pushes in both repositories; does not authorize a live update or other environment changes.","alternatives":"Leave the verified changes uncommitted, as under the earlier boundary.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"Owner reply to the explicit question about committing and pushing both repositories","verbatim":"of course"}}]

**Risk:** Elevated
**Complexity:** Moderate
**Reason:**
Fresh intended-path Redline is GRAY in shadow mode. Shared scroll, selection and focus presentation need independent review; no domain authority or persistence contract change is intended.

**Discovery:**
The person control is a select inside an account disclosure, not customization Rename. Tasks puts its scroll ref and persisted offset on main. The task surface has no bounded results scroller. Studio reserves 280px for its behavior aside, adds substantial header/message padding, and retains a 600px minimum in short windows. Reuse the existing eight-message visual fixture and preserved pre-redesign assets for a matched comparison.

**Material assumptions:**
The existing working-document direction remains accepted. These are bounded corrections, not another broad redesign. Compare actual content geometry before choosing chat changes. If behavior or authority must change, return that part to planning.

**Plan:**
1. Preserve reviewed documentation and reproduce all findings in isolated fictional fixtures.
2. Measure published and pre-redesign chat with identical content, viewport and reading position. Trace all selection, scroll restoration, filtering/paging and focus callers.
3. Apply the reviewed minimal route: retain the centered canvas width and app-bar alignment, remove the duplicate permanent behavior aside, compact header/message padding and cap reading lines. Full Summary remains directly available from the header. Remove ordinary chat's short-window minimum-height overflow; verify full-document/More/restart reachability and use an accessible overflow fallback if necessary.
4. Move the existing task scroll ref/offset handler to a bounded task-content container, retaining project-reset/navigation semantics and stationary tools. Replace nested person select with direct choices using the existing switch callback. Choose the smallest measured chat allocation correction.
5. Build, run focused interaction/geometry regressions, review actual rendered results, and recapture affected docs. Commit and push both repositories under the owner's later publication authorization; leave the live app unchanged.

**Verification plan:**
Direct person selection → keyboard and pointer switch/sign-out checks with existing session isolation.
Task-only scrolling → long open/completed/search lists, paging, heading/tools bounds, navigation restoration, project reset and task-dialog focus checks at desktop/narrow/short sizes.
Useful chat space → same eight messages at identical top/middle/latest anchors, transcript/content width and height, visible content, chrome/composer geometry, no clipping or horizontal overflow.
Preservation → existing Studio visual/reading/focus and workspace/search suites plus build; complete evidence and separate decisions remain accessible.
Documentation → affected real-flow recapture, copied-image hashes/provenance, link/image rendering and independent review.

**Plan review:**
Agent technical review: architect bounded person/task PLAN GO and [CSS-first candidate PLAN GO](#plan-review). Designer directly inspected all three original images and matched renders. Their short-state reachability obligations are included below. Manager selected the designer's smaller centered-width/no-duplicate-aside route rather than the wider candidate; no new presentation state or evidence path is introduced.

**Approvals:**
Not required at this risk level. Following completed qualification, the owner answered “of course” to “Do you want me to commit and push both repositories?” This authorizes that publication only. The live app remains unchanged.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Repository correction, documentation alignment and qualification are complete. The following notes preserve the diagnosis and review sequence; final acceptance is recorded under Result review.
The six app captures, four Core copies and four reader/provenance texts remain preserved. Their completed documentation review applies to rendered app `c4d503a` only. Reuse unchanged evidence, but do not claim those pixels represent a later UI revision.

Matched measurements are complete on the same saved rule and eight messages. At 1440×1000, pre-redesign transcript geometry is 1120×691px, published geometry is 678×610px, and the isolated CSS candidate is 1126×724px. Candidate header height is 101px versus published 175px. It retains the 280px behavior aside and readable body text. At 1440×500 its composer stays within the window; the published layout can place it outside. Narrow transcript height improves from 423px to 485px.

The comparison now aligns the same message IDs at top, middle and latest, rather than comparing fractions of logs containing different inline documents. Pre-redesign narrow/short layouts scroll the outer page and put the composer outside the window when the selected message is brought into view. Do not count that as a larger bounded chat area. At 1440px the candidate still shows six complete latest messages versus seven before redesign and three in the published build; no universal message-count win is claimed.

All three original owner attachments were recovered byte-for-byte into ignored local evidence and directly inspected. Existing architect and designer received bounded read-only offers with the owning record, actual source and renders. Manager owns production implementation and all record writes. Production edits began only after the recorded plan review.

## Plan review

Independent architect source-traced plan GO covers direct person choices with current-person no-op, task scroll ownership/restoration/focus/paging, and the CSS-first chat correction. Candidate review requires real short/dynamic-state checks, especially 683×384, not just ordinary-chat screenshots. Exact review receipts remain in ignored local evidence.

Designer inspected originals and current/candidate renders. Recommendations accepted: direct person buttons in the existing disclosure; one task-results scroller; centered width with duplicate aside removed; compact header/message padding; readable line caps; no blanket short-state clipping. Full Summary/Simulation/evidence and all controls remain. This is a bounded correction to the accepted direction, not a new broad prototype cycle.

Before result acceptance verify actual hit targets and scrollable content for long names, active/current-use controls, setup/error/recovery notices, More, restart, full Summary/Simulation and decision actions at short sizes and breakpoint crossings. Preserve input and reading position, including asynchronous proof responses. Geometric message counts do not establish unobscured text or working product navigation.

## Evidence

Fresh intended-path report: GRAY, no detected API/schema/security/runtime-configuration change. Report used cached offline Python tooling; policy-schema validation was unavailable in that invocation. No installed tools or global configuration changed.

Build passes. Six new account/task checks pass, including actual task scrolling with stationary heading/tools/quick-add/search and retained navigation position at 1440×1000, 626×500, 390×844, 390×500 and 683×384. Four new chat-space/input/More checks and one short active/setup-notice check pass. Existing shell and visual/reading/short-document checks passed before the final small short-height adjustment; the complete browser suite is running against the frozen final build.

Node regression: 691 tests, 678 pass, 13 skips, zero failures. An initial invocation exposed the test runner's temporary-path separator assumption; rerunning with the native Windows path form passes without source or environment-installation changes.

The final centered-width implementation deliberately differs from the wider experimental candidate. At 1440×1000 its transcript is 958×724px versus the published 678×610px and pre-redesign 1120×691px. Header is 101px versus published 175px. It keeps readable line caps rather than claiming every pre-redesign width or message-count metric is beaten. At the same latest-message anchor it shows five complete messages versus published three and pre-redesign seven. At 390×844 transcript height is 487px versus published 423px. The 500px-high ordinary-chat composer stays within the window.

All six follow-up screenshots were recaptured against the frozen corrected build; the four Core copies match. Real compilation/simulation, explicit Rename/Save, separate approval/activation, unchanged tasks before activation, native Follow up creation/readback, no new AI during execution and zero page errors pass. Provenance explicitly identifies an uncommitted correction based on the published revision, not the published revision itself. All ten image hashes, 89 local links, 14 fragments and desktop/390px responsive image embeds pass. Hosted GitHub rendering remains unverified because the work is unpublished.

### Regression correction

The first full browser attempt exposed short full-document decision visibility and historical reading-position failures. It was stopped rather than running a known-broken build to completion; its log and failure traces were preserved. No failed check is counted as passing.

Root correction: keep the compact grid header and viewport-fit minimum for ordinary chat, but retain a stable stacked header and the established short-document scroll extent for Summary, Simulation and management. This avoids conditional actions changing title wrapping above an inspected example and keeps the existing focus/scroll-inset routes intact. No focus helper, authority logic or assertion was weakened.

Independent result review also found that stretching the task sidebar changed the project popup's containing block. Anchor the popup to its disclosure. Reserve popup clearance below the independently scrolling long project list and open it upward in the short sidebar arrangement. New checks cover forty projects, both New/Rename actions fully in view and hit-testable without corrective scrolling, stationary controls, zero outer task scroll and Escape focus return.

All 17 targeted failure/recovery and popup checks now pass. This includes the previously failing short approval/activation/history paths and held historical reading, ordinary short chat, active setup notices, document return and resize/prepend anchors. A fresh 312-case browser run uses two isolated workers and a separate output directory; final totals are pending. The source/build/capture freeze is being advanced only after the targeted correction passes.

The documentation reviewer identified two stale overview phrases in `tests/README.md`; both are corrected. The obsolete logged-out select assertion now checks the actual person-choice buttons rather than an always-absent combobox.

Final capture identity is frozen after those fixes, including changed test bytes. Only Summary/Simulation/approved screenshots changed from the first correction capture; request/native-outcome/saved-task images are byte-identical. All four Core copies are synchronized. Both provenance pages name the final stylesheet hash. Expanded verification passes 98 local links, 15 fragments, 11 text files and all ten image/copy hashes and responsive embeds.

Documentation delta review also found an inaccurate Core image description claiming that the creation outcome was visible behind the Task details modal. The alt text now describes only the visible saved-task form. The preceding native-outcome image remains the evidence of task creation; no unnecessary recapture was added for that wording.

### Final qualification reconciliation

The complete 312-case run finished with 311 passes and one failure in 8.1 minutes. The remaining failure was the existing narrow-screen project-popup bounds assertion: anchoring to the narrow disclosure placed the popup beyond the viewport.

The final two-declaration correction anchors the ordinary mobile-row popup to its full-width project context. The short sidebar-column arrangement explicitly retains the positioned disclosure and upward opening. Desktop, ordinary chat, full-document behavior and JavaScript are unchanged. Two added popup cases cover 390×844 and 390×500.

The complete affected native, visual-polish and workspace-usability files then pass 51/51 in one minute, including the formerly failing case, four popup viewports, project CRUD, search paging, person/authentication isolation, forms, dialogs, focus, reading and short-document behavior. The broad run's 263 cases outside those files are reused. Combined acceptance covers all 314 current browser cases: 263 unchanged broad passes plus 51 final affected passes. This is a reconciled qualification, not a claim that one clean 314-case run occurred.

Final real-flow recapture passes against the new frozen assets. All six images are byte-identical to the documentation-approved set, and all four Core copies still match. Only the inactive-at-desktop mobile popup CSS changed; both provenance stylesheet hashes are updated. Node's 678 passes and 13 skips are reused because backend and JavaScript bytes are unchanged.

## Result review

The independent designer gives scoped rendered GO for the centered correction, based on direct original-image and actual-render inspection. This covers direct person choices, stationary task composition, reclaimed ordinary chat space, readable line caps and short/narrow composer visibility. It excludes universal message-count superiority, full regression, publication and owner acceptance. Later changes affect popup placement and full-document stability, not those ordinary-chat renders; their exact new asset identity is separately frozen.

Agent technical review: independent architect result review, bounded popup/document correction GO and final mobile-popup/reconciliation DELTA GO. Exact receipts remain in ignored evidence. The final reviewer independently verified that reversing only the two mobile CSS declarations and two new viewport entries reproduces the previously reviewed source/test hashes. No unresolved source or coverage blocker remains.

Reviewed revision: app base `c4d503a4c3f3c4898f1df30cdecb05fd7f3b2d25` plus uncommitted correction, source aggregate `d7bfd90682ac80146df3c5ff9e093a156700620544828699ca3a9ebd864b5e30`, stylesheet `2381ad1877ea37437d5ec93ba1a99481343fe34ed66d48b53578037440947378`. All six source, three asset and 31 changed-test pins matched the review snapshot.

Verification adequacy: accepted for the bounded final delta. The reviewer independently reconciled log rows by file and test title: 263 unaffected broad passes plus 51 final affected passes cover all 314 current cases with zero unresolved failures. Preserve the stated combined-run limit. Earlier unit, document/reading/authority and rendered evidence is reused only where unchanged. Build, final real-flow capture, source/test/asset identity, copied screenshots, links, responsive image rendering, whitespace and privacy checks pass.

Documentation review approves the final image/provenance/navigation delta. Both text findings are corrected and read back. A final recapture confirms all six approved image hashes are unchanged after the mobile-only CSS fix. Both guides use the corresponding local images and exact Core copies. Hosted GitHub rendering and physical-device/other-browser qualification are not claimed.

Manager accepts the combined outcome. Supported behavior, exact evidence, separate decisions, input, focus, reading and saved work remain covered. The initial edit-only scope was complete before the owner authorized the publication phase below.

## Publication

The owner explicitly authorized committing and pushing both repositories after qualification. Both remote main branches still match the reviewed task bases. Commit identity is verified as the personal repository identity. Publish the app correction first, then pin its exact commit in Core's image provenance and completion record. Do not force push or alter unrelated checkouts. No deployment or live update is authorized.

The app correction is committed and pushed to main as [`79a4b885a2c657e5b17aaddf8fd32077f523fa53`](https://github.com/rore/keir-demo-app/commit/79a4b885a2c657e5b17aaddf8fd32077f523fa53). Remote-head readback matches. Core's four image copies and reader provenance now pin that exact published source. This record accompanies the synchronized Core documentation publication. The live app remains unchanged.

Skill feedback trigger 2 dropped: the reviewed corrections concern consumer UI layout and coverage, not Agent Workflow behavior.
