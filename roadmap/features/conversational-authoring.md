---
id: "conversational-authoring"
title: "Author customer extensions through a bounded AI workflow"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Deliver the in-app AI-driven extension creation UI on top of the manual exact-revision review/activation flow. This is part of the complete demo; the customer need not write IR. The person running the app supplies their own AI provider configuration and credentials.

## In Scope

- Discover only the approved scoped catalog, clarify intent and generate/repair source using compiler diagnostics.
- Implement the reviewed Extension Studio as primary agent chat plus a contextual canvas. The agent shows useful visual/interactive components as the conversation evolves; canvas edits and confirmations return explicit user input to chat. Preserve structured behavior/scenario review, draft/active status and recovery without a forced workflow wizard or arbitrary generated application code.
- Bind conversations, tool access, scenarios and activation requests to the signed-in demo user; switching users must not expose another user's customization or conversation state.
- Propose scenarios but keep expected outcomes independently reviewable; explain artifact-derived behavior and simulation differences.
- Request exact activation through host policy, with bounded calls/cost and renewed approval after changes.
- Support configurable provider/model selection through one host-owned authoring workflow; evaluate existing multi-provider libraries versus a small adapter boundary before choosing dependencies. Qualify at least two providers rather than assuming every model or compatible endpoint supports the required tools and structured outputs.
- Keep operator-supplied keys server-side in local environment/ignored configuration; ship placeholders only. Never include keys in browser bundles, source, fixtures, logs or exported scenarios. Missing configuration shows a clear setup state; native tasks and already activated extensions remain usable.

## Out of Scope

Arbitrary generated application UI/code, agent certification, automatic approval, endpoint registration and an AI provider dependency in core. Contextual canvas components are part of the Studio.

## Done When

- [x] Design/spec: define discovery/compile/simulate/review tools, prompt/data trust boundaries and provider/spend policy; select a small provider/model support matrix and document local configuration, outbound AI data and limits.
- [ ] Implementation: a live business-user request produces a candidate, simulation and approved exact activation; the manual path remains available.
- [x] Verification: prompt/data injection, forbidden catalog edits, self-approval, stale revisions and exhausted budgets are rejected; activation can be disabled or rolled back through the lifecycle flow.
- [ ] Provider compatibility: the same create/clarify/compile-repair/simulate/review flow works with at least two explicitly qualified provider/model configurations. Fast fake-provider regression covers errors, invalid tool arguments, cancellation and absent/invalid credentials; live provider checks are opt-in with the operator's own keys and explicit spend limits. No provider code enters core.

## Notes

**Dependencies:** [extension-studio-design](extension-studio-design.md); [extension-approval-lifecycle](../features/extension-approval-lifecycle.md); [machine-readable-contracts](../features/machine-readable-contracts.md); [reusable-simulation](../features/reusable-simulation.md).

**Design input:** [Customer authoring and approval, sections 1 and 6-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Application authoring UX, section 5](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing feature ID. Manual immutable approval, activation/disable/rollback and its storage tests are owned by extension-approval-lifecycle; reusable scenario/report tooling is owned by reusable-simulation. This card owns the additional live AI behavior, not duplicate completion promises.

Qualify a provider-neutral adapter against the supported configurations before selecting a dependency.

**Read-only implementation delivered:** Demo [435d509](https://github.com/rore/keir-demo-app/commit/435d509f0a6d77a28d47dbda4de742510a2ac76d) connects Studio chat to two native provider adapters, durable conversations and shared finite reservations, actual compilation and independent scenarios, simulation, exact human review and separate activation. Cancellation, lost responses, restart and user changes preserve honest recovery. Native/manual flows work without AI configuration. The production build, 137 fast tests, 61 browser journeys and [hosted CI](https://github.com/rore/keir-demo-app/actions/runs/41534269475) pass. Independent technical review approves the exact source; rendered checks validate the scoped corrections. The [single Work Record](../../.agent-workflow/tasks/bounded-ai-authoring.md) records the component ownership and combined acceptance.

**Remaining live acceptance:** Automated journeys use scripted provider responses with the real compiler, simulator, storage and browser. Neither provider/model configuration has been qualified through paid live calls, and the live-request completion and provider-compatibility checks above remain open. An operator must supply their own server-side key and finite allowance, following the [configuration and qualification guide](https://github.com/rore/keir-demo-app/blob/main/docs/ai-authoring.md). No real provider compatibility or full feature completion is claimed.
