---
id: "extension-approval-lifecycle"
title: "Review, activate, disable and roll back exact extension revisions"
status: "queued"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Provide a manual source-to-simulation-to-approval-to-activation workflow before adding an AI author.

## In Scope

- Retain linked source/artifact/model/environment/adapter/consumer/scenario identities and independent expected outcomes.
- Choose trustworthy selection storage or signing from an explicit threat model; content hashes alone are not authority.
- Add scoped active selection, disable/revoke, rollback for new invocations and retention of approved dependencies/in-flight selections.

## Out of Scope

An LLM requirement, a generic registry service in core or implicit migration to the latest model.

## Done When

- [ ] Design/spec: define approval authority, invalidation/reapproval and lifecycle behavior, including selection versus event admission.
- [ ] Implementation: manually compile, simulate, inspect actual behavior, approve and activate an exact revision; disable and roll back it.
- [ ] Verification: tampering, self/unauthorized approval, edits after review, restart, revoked dependencies and old snapshot coexistence behave as specified.

## Notes

**Dependencies:** [real-task-reads](../features/real-task-reads.md); [reusable-simulation](../features/reusable-simulation.md).

**Design input:** [Review and immutable activation, sections 1 and 6](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Selection/lifecycle, sections 5, 7 and 11](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).

The first scalar host integrations may use reviewed operator configuration for exact selection. A full customer approval UI is not a prerequisite to that early demonstration.
