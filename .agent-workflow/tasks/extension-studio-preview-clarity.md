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
**Verification plan:**
- Adjustable divider -> measure pane widths after pointer drag, keyboard movement, both limits and reload.
- Business-user clarity -> inspect the rendered task sheet and current exchange on desktop and mobile.
- Continuity -> reopen earlier examples, verify unsent input, and complete the separate confirmation and turn-on decisions.
- Regression and scope -> run the existing prototype suite, Redline, workflow, privacy and whitespace checks.
**Plan review:** Reuse the existing renderers, state and example server. Resizing changes presentation independently of conversation state.
**Approvals:** The user's direct review authorizes the proposal iteration.
**Exceptions:** —
**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Demo baseline `45499ae`. Core baseline `8b0c6f6`. Existing agent design brief and research guide this iteration.

Committed the recovery plan before editing the prototype. Demo revision `2a21c3e` changes the prototype HTML and design brief. The single header and folded earlier messages leave the current question in focus. The canvas shows the completed task and proposed follow-up on an ivory task sheet, with the proposed addition highlighted in mustard. Its slate-grey surrounding surface distinguishes it from the white conversation. Charcoal and oxblood complete the proposed palette. Example results and review wording use the project, person and task names. The user endorsed the interaction direction and delegated palette selection. The revised visuals await their review.

## Evidence

- All 30 existing prototype tests pass, with zero failures or skips, in 0.243 seconds.
- An isolated headless Chrome session exercised the actual loopback preview. Mouse dragging changed the measured pane widths and preserved unsent composer input. Arrow keys adjusted width, Home and End kept both panes at least 320 pixels wide, and a fresh page load restored the chosen split.
- Earlier messages expanded and opened their original read-only example. The current conversation proceeded through project selection, examples, both review confirmations and separate turn-on. The 390-pixel viewport stacked the panes, removed the splitter from keyboard navigation, stayed within viewport width and moved focus using both pane links. The page reported no JavaScript errors.
- Inspected actual desktop and mobile screenshots. The final desktop capture shows one current exchange and one task sheet. Local screenshots and the isolated interaction check are retained in ignored evidence.

## Result review

Routine self-review at demo `2a21c3e`. The changes meet the bounded feedback request. Pointer handling, keyboard focus, tab-local width, read-only history and the decision flow were verified through the rendered interface. The prototype still uses scripted replies. User acceptance of the revised palette and visual clarity remains open. The manager owns roadmap reconciliation and delivery.

## Manager integration

Manager reviewed exact demo `2a21c3e22a3cc87e3b71e4dfc0a30637cc058a75`, the changed rendering and divider code, saved desktop/mobile screenshots and the isolated browser-check procedure. Verification is adequate for this bounded scripted proposal: actual drag/keyboard bounds, refresh persistence, unsent input, historical previews, separate confirmation/turn-on and mobile navigation are exercised. The unchanged 30-test prototype result and rendered evidence are reused; no visual acceptance or production behavior is inferred.

Complete-path Redline is BLUE without checkpoints or boundary violations; the core-owned Work Record check is CLEAN. Scope, commit identity, privacy and whitespace checks pass. Exact source revision is published on demo main and [native-app CI passes](https://github.com/rore/keir-demo-app/actions/runs/41416186002), including build, native regression and browser journeys. Core planning/verification records and roadmap are reconciled. No task worktree was created for this revision; canonical repositories, preview and private evidence remain in place. The requested proposal iteration is delivered; user visual judgment remains open.
