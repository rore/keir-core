---
id: demo-task-integration
title: Build the task app and connect the first real extension
status: queued
priority: high
commitment: committed
milestone: Customize a real task application
repo: keir-demo-app
---

## Summary

Build a useful task manager that works without extensions, then run a reviewed follow-up extension through pinned Keir packages and real task adapters.

## Why

The demo tests the integration boundary and real host guarantees. Fake-adapter success cannot certify authorization, uniqueness or replay.

## In Scope

Grow native task create/list/complete behavior first, then the remaining planned CRUD, projects, tags, priority, due dates and search. Connect packaged core execution after the minimal native path exists, using the read-only contracts and then the scalar follow-up. Add scoped reads, one protected create capability, exact-artifact simulation/review/activation and real adapter tests. Add a pre-create guard consumer with native revalidation so DENY prevents persistence without replacing native authorization. No frozen-catalog evaluation is required.

## Out of Scope

A second Java task app, scheduling, arbitrary tenant endpoints and customer-specific handlers for each extension.

## Done When

- Basic task management works with extensions disabled.
- An approved artifact runs with the Node runtime and real adapters; simulation uses the same artifact and runner.
- Concurrent uniqueness, replay and authorization pass real persistence tests; unknown commit is not called success.
- Integration examples exercise declared hook/capability contracts and verify the host consumes their results correctly.
- A denied pre-create request leaves no task persisted. CONTINUE still passes native authentication, authorization, validation and required consistency checks; no independent extension write occurs before native commit.

## Notes

Implementation and tests live in `keir-demo-app`; status stays in this core roadmap. See the demo repository's `docs/scope.md` for product scope.

The [read](portable-core-first-slice.md) and [simulation-only effect](single-effect-followup.md) checkpoints now provide useful packaged core behavior. The next assignment can give the dedicated demo developer the minimal native create/list/complete path, with extensions disabled. No demo implementation is assigned by this update. The native app need not wait for the full language or [expression enhancement](enrich-followup-expressions.md). Real adapter integration requires reviewed core admission and host guarantees, with database/atomic uniqueness, event admission/version pinning and lost-response behavior decided before claims of safe replay. A local package/tarball with a recorded version/digest is sufficient before registry publication; no sibling source dependency.

The [host integration assistant](host-integration-assistant.md) follows the real host contracts; [customer authoring](conversational-authoring.md) then uses the published approved catalog.
