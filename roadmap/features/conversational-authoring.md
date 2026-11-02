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

- [ ] Design/spec: define discovery/compile/simulate/review tools, prompt/data trust boundaries and provider/spend policy; select a small provider/model support matrix and document local configuration, outbound AI data and limits.
- [ ] Implementation: a live business-user request produces a candidate, simulation and approved exact activation; the manual path remains available.
- [ ] Verification: prompt/data injection, forbidden catalog edits, self-approval, stale revisions and exhausted budgets are rejected; activation can be disabled or rolled back through the lifecycle flow.
- [ ] Provider compatibility: the same create/clarify/compile-repair/simulate/review flow works with at least two explicitly qualified provider/model configurations. Fast fake-provider regression covers errors, invalid tool arguments, cancellation and absent/invalid credentials; live provider checks are opt-in with the operator's own keys and explicit spend limits. No provider code enters core.

## Notes

**Dependencies:** [extension-studio-design](extension-studio-design.md); [extension-approval-lifecycle](../features/extension-approval-lifecycle.md); [machine-readable-contracts](../features/machine-readable-contracts.md); [reusable-simulation](../features/reusable-simulation.md).

**Design input:** [Customer authoring and approval, sections 1 and 6-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Application authoring UX, section 5](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing feature ID. Manual immutable approval, activation/disable/rollback and its storage tests are owned by extension-approval-lifecycle; reusable scenario/report tooling is owned by reusable-simulation. This card owns the additional live AI behavior, not duplicate completion promises.

Qualify a provider-neutral adapter against the supported configurations before selecting a dependency.

**Authorized sequence:** First deliver [real authored drafts](../../.agent-workflow/tasks/authored-extension-drafts.md) through compilation, simulation and exact activation; then connect bounded AI tools and provider configuration to the accepted conversation/review experience. Continue to another useful increment if the completed dependencies support it. Deterministic provider fixtures qualify offline behavior; real provider compatibility requires explicit configured models and bounded live calls. No AI implementation is claimed yet.

**Implementation underway:** The [bounded AI authoring record](../../.agent-workflow/tasks/bounded-ai-authoring.md) has an independently approved overall plan and focused integration plan. The reviewed provider/configuration/tool-loop checkpoint is integrated at demo `debb222`: 30 focused tests and the complete 69-test fast regression pass. Backend integration is released against the reviewed authored foundation: persistent conversation/run state, shared finite reservations, cancellation/recovery and actual compiler/simulation tools. Studio work follows acceptance of the corrected authored UI. No live-provider compatibility or completed AI journey is claimed.
