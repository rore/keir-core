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

Task CRUD, projects, completion, tags, priority, due dates and search; `TaskCompleted` entrypoint; scoped reads; one protected create capability; exact-artifact simulation, review and activation; real adapter tests. Add representative hook and composition examples as implementation needs them; no frozen-catalog evaluation is required first.

## Out of Scope

A second Java task app, scheduling, arbitrary tenant endpoints and customer-specific handlers for each extension.

## Done When

- Basic task management works with extensions disabled.
- An approved artifact runs with the Node runtime and real adapters; simulation uses the same artifact and runner.
- Concurrent uniqueness, replay and authorization pass real persistence tests; unknown commit is not called success.
- Integration examples exercise declared hook/capability contracts and verify the host consumes their results correctly.

## Notes

Implementation and tests live in `keir-demo-app`; status stays in this core roadmap. See the demo repository's `docs/scope.md` for product scope.
