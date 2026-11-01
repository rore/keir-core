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

Add the customer persona on top of the manual exact-revision review/activation flow; the customer need not write IR.

## In Scope

- Discover only the approved scoped catalog, clarify intent and generate/repair source using compiler diagnostics.
- Propose scenarios but keep expected outcomes independently reviewable; explain artifact-derived behavior and simulation differences.
- Request exact activation through host policy, with bounded calls/cost and renewed approval after changes.

## Out of Scope

Generated UI, agent certification, automatic approval, endpoint registration and an AI provider dependency in core.

## Done When

- [ ] Design/spec: define discovery/compile/simulate/review tools, prompt/data trust boundaries and provider/spend policy.
- [ ] Implementation: a live business-user request produces a candidate, simulation and approved exact activation; the manual path remains available.
- [ ] Verification: prompt/data injection, forbidden catalog edits, self-approval, stale revisions and exhausted budgets are rejected; activation can be disabled or rolled back through the lifecycle flow.

## Notes

**Dependencies:** [extension-approval-lifecycle](../features/extension-approval-lifecycle.md); [machine-readable-contracts](../features/machine-readable-contracts.md); [reusable-simulation](../features/reusable-simulation.md).

**Design input:** [Customer authoring and approval, sections 1 and 6-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Application authoring UX, section 5](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing feature ID. Manual immutable approval, activation/disable/rollback and its storage tests are owned by extension-approval-lifecycle; reusable scenario/report tooling is owned by reusable-simulation. This card owns the additional live AI behavior, not duplicate completion promises.
