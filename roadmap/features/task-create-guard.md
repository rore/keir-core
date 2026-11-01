---
id: "task-create-guard"
title: "Make a denied task-create decision prevent persistence"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Enforce the new guard at actual task-creation routes, preserving all native validation and authority checks.

## In Scope

- Use a customer program that reads approved project data and decides whether a proposed task is allowed.
- Cover applicable UI/API/internal routes; define re-entry, fallback and commit-time invariant handling.
- Keep remote decision evaluation outside a long-held database transaction; do not confuse prior reads with atomic consistency.

## Out of Scope

Independent extension writes before commit, patches and new customer-specific host handlers.

## Done When

- [ ] Design/spec: settle hook placement, failure policy, native revalidation and bypass/re-entry behavior.
- [ ] Implementation: a denial leaves no task persisted; an allowance continues the normal authorized create path.
- [ ] Verification: real persistence tests cover denial, invalid result, unavailable decision, concurrent/stale facts, bypass attempts and unchanged native checks.

## Notes

**Dependencies:** [native-task-app](../features/native-task-app.md); [pre-operation-guard](../features/pre-operation-guard.md).

**Design input:** [Guard lifecycle and conformance, sections 6, 12 and 13A](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Pre-create task example, section 4D](../../docs/internal/design-baseline/public/task-management-poc.md).
