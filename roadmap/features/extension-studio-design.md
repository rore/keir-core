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

The Extension Studio is the centerpiece of the demo. Produce a detailed product, interaction and visual design that makes Keir's value and complete customization process understandable to a business user before implementing the Studio UI.

The designer owns UX/UI: a purposeful, polished application with clear hierarchy, usable density and realistic interactions, not a generic AI template. The architect owns technical constraints; the manager coordinates acceptance. The current prototype is a proposal to assess, not an approved visual direction or a production UI contract.

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

**Dependencies:** No unfinished runtime prerequisite for design/prototyping. Use maintained specifications; record unsupported behavior rather than invent implementation.

**Related deliveries:** [Authoring workflow](conversational-authoring.md), [approval lifecycle](extension-approval-lifecycle.md), [simulation tools](reusable-simulation.md) and [native app](native-task-app.md).

Design may start before backend completion. This card owns the reviewed experience; the related features own executable behavior and verification. Keep the first native task slice small while reserving deliberate design effort for the Studio.

Current work: the first staged prototype was rejected. A revised primary-chat/contextual-canvas [UX specification](https://github.com/rore/keir-demo-app/blob/main/docs/extension-studio-design.md) and prototype are available under [the core-owned task record](../../.agent-workflow/tasks/extension-studio-prototype.md). Sixteen focused checks pass; an actual browser walkthrough covers chat-driven canvas changes, explicit canvas input returned to chat and separate human review/activation. User review and the full conversation/progress/cancellation design remain pending; no live AI/native behavior is certified.

**Designer assessment:** Rendered desktop review supports chat plus contextual canvas, but identifies lost continuity when canvas views replace earlier evidence, weak draft/active visibility, oversized editorial headings and competing scroll areas. Progress, cancellation and resume remain undesigned in the prototype. The next bounded design proposal should use a realistic populated working screen, preserve links to earlier evidence, make draft versus active state explicit and cover clarification, a scenario mismatch, correction, exact review and interruption. A continuing conversation per customization and offering new results while the user is occupied are proposal assumptions to review with the user, not accepted requirements or blockers to bounded design exploration. Review visual direction with the user; the current warm-serif treatment is not approved. This assessment does not establish narrow-screen, accessibility or live-provider acceptance.

**Active assignment:** The standing designer has resumed the concrete working-experience proposal and representative scripted interaction. Reuse the completed assessment; review the populated application screen and interaction with the user before production Studio implementation. The delivered native app is not a blocker. Keep native source and live integrations outside this design increment.
