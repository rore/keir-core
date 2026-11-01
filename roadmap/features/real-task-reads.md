---
id: "real-task-reads"
title: "Run packaged read extensions against real task data"
status: "queued"
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

**Next delivery:** Continue while Studio UX review is deferred. Architecture consultation is requested on the smallest real-data read integration: packaged runtime, host-held model/invokers, trusted per-user program selection and validated host result consumption. Reuse current read/2 semantics and native ownership checks; keep Studio presentation, AI and writes outside this increment. Record the concrete implementation plan after the bounded contract review; a queued card or consultation request alone does not assign code work.

**Design input:** [Task catalog, section 3](../../docs/internal/design-baseline/public/task-management-poc.md); [Host consumption and conformance, sections 4 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
