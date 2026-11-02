---
id: "extension-approval-lifecycle"
title: "Review, activate, disable and roll back exact extension revisions"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Provide a manual source-to-simulation-to-approval-to-activation workflow before adding an AI author.

Backend lifecycle work can proceed independently; its customer-facing UI follows the reviewed [Extension Studio design](extension-studio-design.md), including clear draft/active identities, approval invalidation and recoverable navigation.

## In Scope

- Retain linked source/artifact/model/environment/adapter/consumer/scenario identities and independent expected outcomes.
- Choose trustworthy selection storage or signing from an explicit threat model; content hashes alone are not authority.
- Add scoped active selection, disable/revoke, rollback for new invocations and retention of approved dependencies/in-flight selections.
- Keep sources, scenarios, approvals and active bindings isolated per demo user. Show the same hook with different customizations for two users; disabling or rolling back one user's binding must leave the other's behavior intact.

## Out of Scope

An LLM requirement, a generic registry service in core or implicit migration to the latest model.

## Done When

- [ ] Design/spec: define approval authority, invalidation/reapproval and lifecycle behavior, including selection versus event admission.
- [ ] Implementation: manually compile, simulate, inspect actual behavior, approve and activate an exact revision; disable and roll back it.
- [ ] Verification: tampering, unauthorized/cross-user approval or selection, edits after review, restart, revoked dependencies and old snapshot coexistence behave as specified. The owning demo user may approve their own extension where host policy permits; the agent cannot self-approve.

## Notes

**Dependencies:** [real-task-reads](../features/real-task-reads.md); [reusable-simulation](../features/reusable-simulation.md).

**First increment:** The user approved a read-only lifecycle slice using existing compiled example programs: simulate a candidate, review and approve exact evidence, activate it for Maya, observe actual task advice, then disable or roll back while Leo stays unchanged. The architect and designer supplied the contract/functional UX; independent technical review approved revised plan `17ed6f3`, and readiness `d4a1086` is recorded in the [Work Record](../../.agent-workflow/tasks/read-extension-lifecycle.md). The demo developer owns implementation and integration in isolated source/record checkouts; a second developer accepted the bounded Studio UI helper task under the same record. Frozen rules cover six-case per-template evidence, explicit operator bootstrap, current-draft approval/activation, selection generations, in-flight and browser stale-response suppression, and finite host limits. Backend implementation and focused tests are underway; migration, restart, API and browser verification remain before independent result review. No completed implementation result is claimed yet. The accepted chat/canvas is the working direction; the unfinished broader simulation kit is not a gate. Live AI, arbitrary source authoring and real effects remain outside this slice.

**Design input:** [Review and immutable activation, sections 1 and 6](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Selection/lifecycle, sections 5, 7 and 11](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).

The first scalar host integrations may use reviewed operator configuration for exact selection. A full customer approval UI is not a prerequisite to that early demonstration.
