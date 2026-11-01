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

- [x] Design/spec: review scoped reads, current authorization, resource identity and the host result-consumer contract.
- [ ] Implementation: the same artifact classifies a real completed/open task and the app consumes the validated result.
- [ ] Verification: wrong-resource, cross-user selection/data access, denied, revoked, missing and malformed-result cases fail safely; two demo users show different configured behavior at the same hook and native CRUD remains usable.

## Notes

**Dependencies:** [native-task-app](../features/native-task-app.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Current work:** Architecture consultation and independent technical plan review approved the scoped read/consumer contract. The developer committed candidate `45499ae` in an isolated checkout: pinned runtime archive/provenance, two compiled programs, real scoped reads and native task-detail advice. Initial evidence reports 18 fast checks, two new Chromium journeys and eight Java artifact probe combinations passing. Final regression/standalone qualification is running; independent code review is requested against the committed candidate. [The Work Record](../../.agent-workflow/tasks/real-task-reads.md) retains measured evidence and remaining acceptance. This checkpoint does not complete or deliver the feature; Studio review, live AI and extension writes are outside the increment.

**Design input:** [Task catalog, section 3](../../docs/internal/design-baseline/public/task-management-poc.md); [Host consumption and conformance, sections 4 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
