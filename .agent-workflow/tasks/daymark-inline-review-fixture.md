# Render the Customization Studio design for review

<!-- agent-workflow:start -->
**Outcome:** Two fresh reviewers can walk through the proposed in-chat customization review on desktop and phone, with revisions between rounds.

**Target:** keir-demo-app

**Scope:** Build an isolated browser-test fixture from the written design, capture a baseline, apply design corrections, and capture the revised states for independent review.

**Constraints:** Use synthetic state and the normal browser-test setup. Keep changes under the fixture and its focused test. Keep the blocked standalone file out of the fixture. Do not access saved user data or change the running preview, production routes, or approval authority.

**Completion criteria:** Reviewers can open the exact fixture build, exercise the main path and failed and stale evidence, and compare before and after desktop and phone renders. Record findings about flow and visual polish and the changes made between rounds.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Two fresh reviewers can walk through the proposed in-chat customization review on desktop and phone, with revisions between rounds.","scope":"Build an isolated browser-test fixture from the written design, capture a baseline, apply design corrections, and capture the revised states for independent review.","constraints":"Use synthetic state and the normal browser-test setup. Keep changes under the fixture and its focused test. Keep the blocked standalone file out of the fixture. Do not access saved user data or change the running preview, production routes, or approval authority.","completion_criteria":"Reviewers can open the exact fixture build, exercise the main path and failed and stale evidence, and compare before and after desktop and phone renders. Record findings about flow and visual polish and the changes made between rounds."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The fixture has no real data or decision authority. Responsive interactions and two independent rounds need reproducible evidence and review.

**Discovery:** The existing written design specifies the saved behavior card, example checks, and separate approval and turn-on. The developer prepared a fixture-only checkout and proposed using React, shared Daymark styles, a separate build and the repository's browser-test setup.

**Material assumptions:** The fixture can use existing project tooling and a local browser test without touching application data or production entry points.

**Plan:** Implement the written design with synthetic examples. Capture the initial task pair, collapsed and expanded checks, unchecked coverage, confusion and proceed, approval and turn-on, and failed and stale evidence. Let the first fresh reviewer assess the rendered flow, typography, spacing, hierarchy and mobile wrapping. Revise the relevant states and send them to a different reviewer without sharing the first reviewer's conclusions.

**Verification plan:**
- Desktop and phone readability → Review and capture corresponding states on the exact builds at desktop and at 390 and 320 CSS pixels.
- Visible coverage and one decision per state → Exercise the main, failed and stale paths with pointer and keyboard and inspect focus and result labels.
- Synthetic fixture isolation → Verify absence of service calls or persistent effects and compare normal production bundle hashes.
- Remaining visual and interaction issues → Record limitations and recheck the integrated application separately.

**Plan review:** Agent technical review: [Fixture boundary review](#fixture-boundary-review).

**Approvals:** Isolated design and browser testing are authorized.

**State:** Ready for review
<!-- agent-workflow:end -->

## First rendered review

The frozen Daymark fixture at `915d9d1` passed six browser cases. The designer walked through desktop and two phone widths, including failed and stale checks, keyboard navigation, and separate approval and turn-on. The first independent reviewer found that the first review action fell below the phone viewport, the clarification reply could appear offscreen, and the approval copy sounded like test setup. The developer revised the fixture and a different reviewer received the revised build with neutral instructions.

## Fixture boundary review

The manager reviewed the developer's plan before implementation. Use a separate fixture entry and synthetic state, reuse Daymark styles and browser tooling, and verify the production build is unaffected. Retain the first exact render and show the next build to a different reviewer without sharing the first review's conclusions.

## Result review

Agent technical review: [Independent reviews of the rendered fixture](#independent-reviews-of-the-rendered-fixture).

Reviewed revision: `28aa946c6413795b74771fb42ac7a6f221163c14`.

Verification adequacy: Pass for the bounded fixture and its two independent review rounds. The designer verified the revised phone layout, keyboard controls and accessibility tree labels. Browser zoom and screen-reader verification remain open.

### Independent reviews of the rendered fixture

The first revision brought the review action into view on a phone, scrolled to clarification and clarified the approval copy. The second fresh reviewer found that failed and unavailable scenarios still hid their recovery actions and that a typed long-title question got a generic reply. The reviewer verified that the polished build fixed both problems. The manager checked the desktop rule and phone states for approval and wrong-project failure. The exact task pair is now an example below the general rule, mismatched project names are emphasized, and the unchecked long-title condition remains visible beside turn-on.

Six Chromium cases pass at desktop and at 390 and 320 CSS pixels. The fixture type check and production build pass. Production bundle hashes match the base. The designer verified all five requested visual and interaction corrections on the polished fixture. Keyboard approval, turn-on and turn-off preserve focus and show distinct states. The accessibility tree exposes names and expansion state. The first action fits at 390 by 844 with only two pixels to spare. Browser zoom at 200 percent remains open because the browser tool could not verify the selected window's URL. A screen-reader walkthrough and verification of the working application remain open.
