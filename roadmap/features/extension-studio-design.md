---
id: "extension-studio-design"
title: "Design and prototype the Extension Studio experience"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

The Extension Studio is the centerpiece of the demo. Maintain the accepted working design baseline and refine each workflow's product, interaction and visual design as real functionality is implemented.

The designer owns UX/UI: a purposeful, polished application with clear hierarchy, usable density and realistic interactions, not a generic AI template. The architect owns technical constraints; the manager coordinates acceptance. The chat-led, resizable preview is the accepted starting direction; production interaction details mature with each functional feature.

## In Scope

- Map intent, clarification, proposed behavior, compilation/repair, scenario design, simulation, review, exact activation, observed application behavior and later revision/disable/rollback. Define how users move back and forth without losing work.
- Make agent chat the primary interaction, with a contextual canvas showing visual or interactive UI as the agent works. The agent selects useful application-owned views: clarification choices, behavior diagrams, scenarios, comparisons, traces and exact-revision review. Canvas interactions feed user input back into the conversation; do not impose a staged wizard or require users to navigate a workflow dashboard.
- Define information hierarchy, conversation continuity, canvas updates, progress/status, action labels and progressive disclosure. Users must understand behavior and consequences without reading IR; technical evidence remains inspectable. Presentation choices never grant the agent approval or activation authority.
- Distinguish user intent, generated proposals, validated behavior, simulated evidence and actual runtime results. Show what a customization reads/changes, when it runs, who owns it and what remains untested or uncertain.
- Specify draft versus active revisions, approval invalidation, scenario expectations, real-effect boundaries and user-scoped state. Cover missing provider setup, clarification, invalid/unsupported requests, mismatches, provider failure, cancellation, reload/resume and login switches.
- Establish a distinctive visual system and polished accessible interactions consistent with the native app. Use representative content and meaningful empty/loading/error/success states, not only a happy-path mockup.
- Build an interactive prototype for a guided end-to-end demo and recovery paths. Mark scripted/simulated behavior honestly; a prototype does not certify AI, runtime or native isolation.

## Out of Scope

A generic agent builder, a second host-design chat, implementation of the complete Studio backend or a claim that visual polish proves correctness.

## Done When

- [ ] Design: maintain the demo-owned UX specification, screen/state inventory, interaction rules and visual direction; resolve consequential product questions with the user and architectural contracts with the architect.
- [ ] Prototype: walk the user through request -> clarification -> behavior review -> scenarios -> simulation -> exact approval/activation -> visible task-app behavior, then edit/retest and disable/rollback. Include a recoverable failed attempt and different customizations for two users.
- [ ] Review: review the actual prototype with the user and record resolved feedback before implementing the Studio UI. Verify business-user clarity, accessible navigation and continuity across draft/active states; do not substitute a static screenshot for interaction review.
- [ ] Handoff: connect each screen/state to host-owned authoring/lifecycle tools and define fast regression, UI journeys, visual checks and separate opt-in live-provider acceptance. Track remaining implementation in the existing owning features.

## Notes

**Current user correction:** The working app must present customization as a general host-app feature, rather than a task-specific advice scenario. Review the whole functional experience: independent host-app branding with secondary Powered by Keir attribution, coherent but distinct Studio identity, discoverable customization navigation, desktop/narrow layouts and correct scroll ownership so content does not drag the sidebar. The previous visual baseline is superseded where this feedback conflicts. Preserve the actual lifecycle underneath and label currently supported examples honestly. The [correction Work Record](../../.agent-workflow/tasks/demo-customization-experience.md) owns design discovery, implementation, regression and independent review; no correction result is claimed yet.

**Correction implementation:** The designer's Daymark working identity, persistent Tasks/Customizations shell and dedicated chat/canvas Studio are defined in the owning record. Native modal return/drafts, owner lifetime, exact lifecycle actions and independent scroll regions remain explicit obligations. Independent technical review approved plan aa5ae22 with no blockers; the demo developer accepted implementation against readiness b7086b7. First functional candidate `1cf19c1` builds and passes 38 fast regressions. Rendered review completed with changes requested; the developer also continues scrolling/navigation regressions. Final verification, resolved designer findings and independent technical result review remain before delivery. No completed correction result is claimed yet. Currently supported read-only examples remain honest, and broader AI authoring is unfinished.

**Rendered correction pass:** The actual review verified the new shell/navigation, real lifecycle, native edit return and ordinary scroll isolation. It requests one bounded pass for short/zoomed clipped controls and selected-tab hover contrast; truthful off/unavailable versus draft/in-use status; business-language expected/actual evidence with technical details secondary and an immediately visible Use action after approval; and consolidated repeated copy, scope highlighting and attribution. These findings belong to the existing Work Record and scope. Recheck corrected screens and triggering paths rather than restarting an unrelated style round.

**Dependencies:** No unfinished runtime prerequisite for design/prototyping. Use maintained specifications; record unsupported behavior rather than invent implementation.

**Related deliveries:** [Authoring workflow](conversational-authoring.md), [approval lifecycle](extension-approval-lifecycle.md), [simulation tools](reusable-simulation.md) and [native app](native-task-app.md).

Design may start before backend completion. This card owns the reviewed experience; the related features own executable behavior and verification. Keep the first native task slice small while reserving deliberate design effort for the Studio.

**Earlier prototype:** The first staged prototype was rejected. A revised primary-chat/contextual-canvas [UX specification](https://github.com/rore/keir-demo-app/blob/main/docs/extension-studio-design.md) and prototype were recorded under [the core-owned task record](../../.agent-workflow/tasks/extension-studio-prototype.md). Sixteen focused checks and a browser walkthrough covered chat-driven canvas changes, explicit canvas input returned to chat and separate human review/activation. That checkpoint did not settle the full conversation/progress/cancellation design or certify live AI/native behavior; later iterations and the accepted baseline below supersede its review status.

**Designer assessment:** Rendered desktop review supports chat plus contextual canvas, but identifies lost continuity when canvas views replace earlier evidence, weak draft/active visibility, oversized editorial headings and competing scroll areas. Progress, cancellation and resume remain undesigned in the prototype. The next bounded design proposal should use a realistic populated working screen, preserve links to earlier evidence, make draft versus active state explicit and cover clarification, a scenario mismatch, correction, exact review and interruption. A continuing conversation per customization and offering new results while the user is occupied are proposal assumptions to review with the user, not accepted requirements or blockers to bounded design exploration. Review visual direction with the user; the current warm-serif treatment is not approved. This assessment does not establish narrow-screen, accessibility or live-provider acceptance.

**Previous proposal:** The standing designer delivered the populated application-like chat/canvas proposal at demo `b3d226b`, with a visible draft/active strip, reopenable evidence, held-canvas continuity and scripted interruption/resume. Twenty-five focused checks and actual desktop/narrow/keyboard walkthroughs pass; the manager inspected the rendered screen and verified the recorded scope/privacy/workflow evidence. Independent state review approved this revision with no actionable findings; the prototype is pushed. [The follow-up record](../../.agent-workflow/tasks/extension-studio-working-experience.md) retains evidence. At that checkpoint user acceptance remained pending; the accepted working baseline below supersedes that gate. Native source, live AI and real execution remain outside this design increment.

**Ready for user review:** The whole-experience revision is pushed at demo `33d5c4e`, following the agent-led design brief at `70a8583`: conversation-first presentation, familiar task/follow-up previews, readable typography/contrast, ordinary-language choices/status/review and secondary technical detail. Thirty focused checks pass. Actual desktop/narrow/keyboard evidence from the unchanged presentation remains applicable. Independent review caught and then approved the corrected turn-off routing; a direct replay confirms the previously failing request disables the simulated setup while turn-on still requires human review. Manager rendered inspection and scope/privacy/workflow readback are complete. [The business-user iteration record](../../.agent-workflow/tasks/extension-studio-business-experience.md) retains evidence. That user-review request is superseded by the accepted baseline below; live AI/native effects remained outside this prototype increment.

**Accepted working baseline:** The user accepted demo `2a21c3e` as a good start and asked to iterate as real functionality is added. Its chat-led task-sheet canvas, focused conversation and pointer/keyboard divider are the starting direction. Thirty prototype checks, rendered interaction checks, manager scope/privacy/workflow review and [hosted native-app CI](https://github.com/rore/keir-demo-app/actions/runs/41416186002) pass; see [the iteration record](../../.agent-workflow/tasks/extension-studio-preview-clarity.md). No further standalone prototype iteration is requested. Remaining production states, lifecycle/tool handoffs and detailed UX mature with their owning functional features; this feature stays active. Scripted results do not certify live AI or task effects.

**Sequence:** Proceed from the accepted baseline with functional Studio/backend slices and feature-specific UX review. A separate visual-polish cycle is not a prerequisite. [Packaged real task reads](real-task-reads.md) and the first [reusable simulation](reusable-simulation.md) increment are delivered.

**First functional Studio:** Demo [df06843](https://github.com/rore/keir-demo-app/commit/df0684301ff4a3909d7b71d019e5c63d93d23193) connects the accepted chat/resizable canvas to actual saved read-only advice, simulation evidence, separate human approval/activation, history and named restore. Desktop/narrow, keyboard, unsaved-edit, conflict and late-response journeys pass; [hosted CI is green](https://github.com/rore/keir-demo-app/actions/runs/41432168250). This prepared-choice flow is explicitly manual and does not claim AI authoring or follow-up creation. Its implementation/evidence belongs to [approval lifecycle](extension-approval-lifecycle.md); remaining authoring, provider and richer conversation states keep this design feature active. Earlier prototype review requests are historical and superseded by the accepted baseline; no standalone visual-review gate blocks functional iteration.
