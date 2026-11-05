# Present the customization rule and simulation clearly in Daymark

Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md).
Earlier delivery: [Review inside the conversation](daymark-inline-review.md).

<!-- agent-workflow:start -->
**Outcome:** A Daymark user can explain what a customization does, see what happened in each simulation case, and find the next decision from the conversation.

**Target:** keir-demo-app

**Scope:** Adapt the deck’s simulation layout to the Studio rule summary and case results. Update the affected wording and presentation in Daymark.

**Constraints:** Preserve saved conversations, exact evidence, separate approval and activation, ownership, focus, and unsent text. Use isolated test data. Leave the compiler, runtime, and native behavior unchanged.

**Completion criteria:** The rendered desktop and mobile flow distinguishes rule intent from simulated outcomes, shows each case result without expansion, and makes the next action clear. A designer critique of the working app leads to a refinement and a second rendered walkthrough.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A Daymark user can explain what a customization does, see what happened in each simulation case, and find the next decision from the conversation.","scope":"Adapt the deck’s simulation layout to the Studio rule summary and case results. Update the affected wording and presentation in Daymark.","constraints":"Preserve saved conversations, exact evidence, separate approval and activation, ownership, focus, and unsent text. Use isolated test data. Leave the compiler, runtime, and native behavior unchanged.","completion_criteria":"The rendered desktop and mobile flow distinguishes rule intent from simulated outcomes, shows each case result without expansion, and makes the next action clear. A designer critique of the working app leads to a refinement and a second rendered walkthrough."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The interface presents evidence for a draft that can perform a native write after separate approval and activation.

**Discovery:** The current summary illustrates a saved scenario. Case headings can show a sample task title without context, and results require expanding individual cases.

**Material assumptions:** Existing saved rule and evidence data can support the first design without changing authorization or persistence. Where exact behavior cannot be summarized, keep the presentation explicit about that limit.

**Plan:** Run evidence presentation and visual design in parallel. The Daymark developer uses exact revision evidence for compact simulation rows, with a conservative rule summary and an explicit fallback. The designer revises the whole Studio screen after feedback on the first studies. Combine the two in the isolated implementation checkout, review the working app, and refine before delivery.

Apply the revised teal Studio direction at the functional candidate. Update the shared shell and Studio styles in `src/web/styles.css` and `src/web/Studio.css`. Limit markup changes to speaker and case styling, the composer prompt, and a local correction control with the same gate as the existing footer. Preserve the live mobile destination control, focus and scroll ownership, and approval and activation. Verify the rendered desktop, narrow, and short views, native Tasks and Customizations, and the synthetic provider journeys. Request a designer critique and a fresh business walkthrough on the rendered app before refinement and final review.

**Verification plan:** Walk through rule, simulation, and next action as a first-time business user at desktop and mobile widths. Check matching, no action, mismatch, unavailable evidence, earlier results, separate approval and activation, and keyboard and focus behavior. Verify that an expected uncertain outcome can match the test without implying task creation or changing existing approval eligibility. Compare the first and refined working renders.

**Plan review:** Agent technical review: approved the functional slice at Daymark `e496f82c4b3ef97b3eaf0e946e256002960e0e41`. Bind the summary to the exact artifact and explicit suite identity, describe only supported artifact facts, show saved mismatch reasons, and preserve approval eligibility for matching expected uncertainty. Two rendered studies established the interaction. Review the new visual direction and combined implementation before delivery.

Independent plan review approved the whole-screen changes at Daymark `ca7307b` after verifying the revised brief. Preserve the existing mobile control and local correction gate. Keep expected uncertainty visibly distinct from confirmed matches and mismatches.

**Approvals:** The user assigned this work to the designer and developer and requested visual iteration. Approval and activation of each customization stay with the person using Daymark.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The functional candidate changes the saved rule and inline simulation rendering in `src/web/Studio.tsx` and `src/web/Studio.css`, plus affected Studio browser assertions and their shared review helper. The isolated app branch starts at released Daymark `e496f82c4b3ef97b3eaf0e946e256002960e0e41`.

The first functional candidate is Daymark `ca7307bdde05d10d1eb142ae23546845fb72d2ed`. The TypeScript and production build passed. The candidate passed 31 focused and 60 remaining impacted browser cases, including real exact-review journeys driven by synthetic OpenAI and Anthropic providers, a mutated-artifact check, mismatch and uncertainty cases, and stale-evidence races. Two additional capture runs at 1440×1000 and 390×844 passed. Independent review approved the revised functional slice.

The first combined render at Daymark `6c820e2ba86d977354f87d2bd56ec50a0d9ac53b` applied the teal shell and Studio layout. The designer's app-capture critique identified the phone header height, repeated headings, and inconclusive check language. A fresh business walkthrough also identified ambiguity between the version in use and the pending change. The second rendered walkthrough confirmed the distinction between the live version, the saved change, and the two decisions. The summary deliberately leaves the full project filter unavailable when the artifact does not support a reliable description.

The designer's interactive review passed the main Studio at desktop and phone sizes and identified cramped columns in the secondary comparison. Final Daymark candidate `bfed69e67a22b32407d1064b062ef69193138dd7` applies the Studio palette there, stacks results based on the actual pane width, and keeps the active and proposed versions distinct. The exact production build passed 35 Chromium cases and 22 focused unit cases. Browser verification includes separate approval and activation, matched uncertainty, mismatch repair, retained unsent text, return focus, delayed simulation/approval/activation reading position, independent scrolling, and native Tasks and Customizations.

Independent technical review approved the final delta. The designer verified the exact served build in the isolated app at desktop, 390px and 320px and accepted the comparison correction. The designer checked pane resizing, readable stacked results, phone overflow, return focus, and retained unsent text. Browser zoom and screen-reader use remain unverified. This is ready for manager integration review and human visual acceptance.

The integration commit [Daymark `2e4befd`](https://github.com/rore/keir-demo-app/commit/2e4befd490bcf3a5aa7135b24380d71d64de891b) contains the exact reviewed tree. A clean build reproduced the reviewed JavaScript and CSS byte for byte. The existing demo now serves that bundle with its current database and assistant settings.
