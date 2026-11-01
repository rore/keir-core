---
id: "conversational-authoring"
title: "Author customer extensions through a bounded AI workflow"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Deliver the in-app AI-driven extension creation UI on top of the manual exact-revision review/activation flow. This is part of the complete demo; the customer need not write IR. The person running the app supplies their own AI provider configuration and credentials.

## In Scope

- Discover only the approved scoped catalog, clarify intent and generate/repair source using compiler diagnostics.
- Bind conversations, tool access, scenarios and activation requests to the signed-in demo user; switching users must not expose another user's customization or conversation state.
- Propose scenarios but keep expected outcomes independently reviewable; explain artifact-derived behavior and simulation differences.
- Request exact activation through host policy, with bounded calls/cost and renewed approval after changes.
- Support configurable provider/model selection through one host-owned authoring workflow; evaluate existing multi-provider libraries versus a small adapter boundary before choosing dependencies. Qualify at least two providers rather than assuming every model or compatible endpoint supports the required tools and structured outputs.
- Keep operator-supplied keys server-side in local environment/ignored configuration; ship placeholders only. Never include keys in browser bundles, source, fixtures, logs or exported scenarios. Missing configuration shows a clear setup state; native tasks and already activated extensions remain usable.

## Out of Scope

Generated UI, agent certification, automatic approval, endpoint registration and an AI provider dependency in core.

## Done When

- [ ] Design/spec: define discovery/compile/simulate/review tools, prompt/data trust boundaries and provider/spend policy; select a small provider/model support matrix and document local configuration, outbound AI data and limits.
- [ ] Implementation: a live business-user request produces a candidate, simulation and approved exact activation; the manual path remains available.
- [ ] Verification: prompt/data injection, forbidden catalog edits, self-approval, stale revisions and exhausted budgets are rejected; activation can be disabled or rolled back through the lifecycle flow.
- [ ] Provider compatibility: the same create/clarify/compile-repair/simulate/review flow works with at least two explicitly qualified provider/model configurations. Fast fake-provider regression covers errors, invalid tool arguments, cancellation and absent/invalid credentials; live provider checks are opt-in with the operator's own keys and explicit spend limits. No provider code enters core.

## Notes

**Dependencies:** [extension-approval-lifecycle](../features/extension-approval-lifecycle.md); [machine-readable-contracts](../features/machine-readable-contracts.md); [reusable-simulation](../features/reusable-simulation.md).

**Design input:** [Customer authoring and approval, sections 1 and 6-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Application authoring UX, section 5](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing feature ID. Manual immutable approval, activation/disable/rollback and its storage tests are owned by extension-approval-lifecycle; reusable scenario/report tooling is owned by reusable-simulation. This card owns the additional live AI behavior, not duplicate completion promises.

Feasibility input: [AI SDK providers](https://ai-sdk.dev/docs/foundations/providers-and-models) and [provider management](https://ai-sdk.dev/docs/ai-sdk-core/provider-management) document a shared interface and provider selection. This is an implementation candidate, not a dependency decision or proof of our workflow's compatibility.
