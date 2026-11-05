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

**Verification plan:** Verify the exact build on desktop and at 390 and 320 CSS pixels. Check keyboard and focus, visible coverage, one clear decision per state, and absence of service calls or persistent effects. Capture corresponding before and after states. Report any unresolved reading or interaction issues. This fixture does not substitute for later verification of the working application.

**Plan review:** Manager reviewed the developer's plan. Separate fixture entry and synthetic state are appropriate. Reuse the app's styles and normal browser tooling, keep fixture-only paths and verify no production bundle or server changes.

**Approvals:** Isolated design and browser testing are authorized.

**State:** Ready to implement
<!-- agent-workflow:end -->
