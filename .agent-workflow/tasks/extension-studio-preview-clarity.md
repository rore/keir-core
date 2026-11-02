# Refine the Studio conversation and preview

Canonical task: `agent-workflow:extension-studio-preview-clarity`. Owning feature: [Extension Studio](../../roadmap/features/extension-studio-design.md).

<!-- agent-workflow:start -->
**Outcome:** Make the conversation easy to read and give its adjustable canvas a distinct, concrete presentation for business users.
**Target:** keir-demo-app
**Scope:** Studio design brief and existing scripted prototype in the demo repository; this core Work Record.
**Constraints:** Preserve conversation history, exact review, user scope and separate turn-on. Use existing project tools and preview. Manager owns roadmap and integration.
**Completion criteria:** Desktop divider works with pointer and keyboard and both panes remain usable on narrow screens. Preview communicates the proposed outcome and current setup. Inspect actual rendering and preserve existing prototype checks.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"Make the conversation easy to read and give its adjustable canvas a distinct, concrete presentation for business users.","scope":"Studio design brief and existing scripted prototype in the demo repository; this core Work Record.","constraints":"Preserve conversation history, exact review, user scope and separate turn-on. Use existing project tools and preview. Manager owns roadmap and integration.","completion_criteria":"Desktop divider works with pointer and keyboard and both panes remain usable on narrow screens. Preview communicates the proposed outcome and current setup. Inspect actual rendering and preserve existing prototype checks."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}
**Risk:** Routine
**Complexity:** Moderate
**Reason:** Self-contained presentation and input handling under blue documentation paths.
**Discovery:** The user reviewed the running proposal and endorsed chat as the main interaction with a supporting canvas. They found both panes too similar, the screen cluttered and the canvas hard to interpret. They rejected the previous palette.
**Material assumptions:** The user delegated palette selection and asked for a distinctive design. Visual acceptance follows review of the revised proposal.
**Plan:** Reduce repeated navigation and labels. Give the canvas one sample task list on a contrasting surface. Simplify examples and review. Add a bounded desktop splitter that supports pointer and keyboard input, retaining the stacked narrow view.
**Verification plan:** Check drag, keyboard, limits, responsive layout and a representative conversation through review in a browser. Run existing prototype tests and scope checks.
**Plan review:** Reuse the existing renderers, state and example server. Resizing changes presentation independently of conversation state.
**Approvals:** The user's direct review authorizes the proposal iteration.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

Demo baseline `45499ae`. Core baseline `8b0c6f6`. Existing agent design brief and research guide this iteration.
