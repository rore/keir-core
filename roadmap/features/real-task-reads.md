---
id: "real-task-reads"
title: "Run packaged read extensions against real task data"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Connect the existing read/2 compiler/runtime path to real scoped task services and a host-owned display consumer.

## In Scope

- Consume pinned core packages/tarballs; do not import sibling runtime source.
- Publish a small task DCM, bind its complete invoker catalog and select an independently approved artifact/model/consumer.
- Invoke an on-demand extension over actual task data; compare its simulation and live host consumption.
- Derive user scope from the trusted demo session and select that user's configured extension. The same hook can run different programs for different demo users; cross-user resource IDs or submitted owner IDs cannot widen access.

## Out of Scope

Writes, a transaction guard, AI, public registry publication and a complete activation UI.

## Done When

- [ ] Design/spec: review scoped reads, current authorization, resource identity and the host result-consumer contract.
- [ ] Implementation: the same artifact classifies a real completed/open task and the app consumes the validated result.
- [ ] Verification: wrong-resource, cross-user selection/data access, denied, revoked, missing and malformed-result cases fail safely; two demo users show different configured behavior at the same hook and native CRUD remains usable.

## Notes

**Dependencies:** [native-task-app](../features/native-task-app.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Current work:** Architecture consultation confirms the existing read/2 API and scoped native service support this increment. [The implementation plan](../../.agent-workflow/tasks/real-task-reads.md) covers standalone integrity-pinned packaging, trusted per-user selection, projected task reads, advisory result consumption and a minimal native task-detail action. Independent technical plan review is active; implementation preparation is assigned. Source changes wait for that internal review, independently of deferred Studio UX. Keep existing runtime semantics, native ownership checks, Studio presentation, AI and writes outside the change.

**Design input:** [Task catalog, section 3](../../docs/internal/design-baseline/public/task-management-poc.md); [Host consumption and conformance, sections 4 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
