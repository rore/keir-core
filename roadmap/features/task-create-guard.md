---
id: "task-create-guard"
title: "Make a denied task-create decision prevent persistence"
status: "active"
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
- Demonstrate rules requiring due dates for client tasks, notes for high-priority tasks and a review tag in a chosen project, using the host-published model and ordinary authored conditions.
- Keep the user's unsaved fields after denial or an unavailable active check. Present the rule and allowed, blocked and unavailable simulations inside the existing conversation.

## Out of Scope

Independent extension writes before commit, patches and new customer-specific host handlers.

## Done When

- [ ] Design/spec: settle hook placement, failure policy, native revalidation and bypass/re-entry behavior.
- [ ] Implementation: a denial leaves no task persisted; an allowance continues the normal authorized create path.
- [ ] Verification: real persistence tests cover denial, invalid result, unavailable decision, concurrent/stale facts, bypass attempts and unchanged native checks.

## Notes

Design and host-entry-point discovery are active. The host design must cover interactive, API and internal task creation, including task creation requested by existing post-event extensions. No active guard preserves ordinary native task behavior. An active guard that cannot establish an allowed result prevents persistence with an understandable recovery path. Existing native validation, current permissions, receipt identity and commit-time checks remain authoritative.

Completion includes independent design and result review, real compiler/simulator/native tests, cross-owner and stale-selection cases, desktop and narrow/short-screen walkthroughs, matching agent guidance and release pins, and updating the running demo after verification.

**Dependencies:** [native-task-app](../features/native-task-app.md); [pre-operation-guard](../features/pre-operation-guard.md).

**Design input:** [Guard lifecycle and conformance, sections 6, 12 and 13A](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Pre-create task example, section 4D](../../docs/internal/design-baseline/public/task-management-poc.md).
