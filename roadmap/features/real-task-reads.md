---
id: "real-task-reads"
title: "Run packaged read extensions against real task data"
status: "done"
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
- [x] Implementation: the same artifact classifies a real completed/open task and the app consumes the validated result.
- [x] Verification: wrong-resource, cross-user selection/data access, denied, revoked, missing and malformed-result cases fail safely; two demo users show different configured behavior at the same hook and native CRUD remains usable.

## Notes

**Dependencies:** [native-task-app](../features/native-task-app.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Delivered:** Demo main `45499ae` contains the reviewed pinned runtime, task DCM, two compiled user programs, scoped real reads and native task-detail advice. Twenty-one Node regressions, nine Chromium journeys, eight Java artifact probes and a standalone locked install/build/server check without Java or a core checkout pass. Independent result review approved the exact integrated commit; [demo CI passes](https://github.com/rore/keir-demo-app/actions/runs/41334801825) and [core review-record CI passes](https://github.com/rore/keir-core/actions/runs/41334925661). Final workflow validation is clean; GRAY Redline has no boundary/checkpoint. The owner released the assignment and detached its association; the task-created checkout was removed after preserving a verified source bundle and 36 evidence files. Existing previews/data were not restarted or altered. [The Work Record](../../.agent-workflow/tasks/real-task-reads.md) retains evidence and local-demo limits. Studio, live AI and extension writes remain separate work.

**Design input:** [Task catalog, section 3](../../docs/internal/design-baseline/public/task-management-poc.md); [Host consumption and conformance, sections 4 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
