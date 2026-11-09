---
id: "task-create-guard"
title: "Make a denied task-create decision prevent persistence"
status: "done"
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

- [x] Design/spec: settle hook placement, failure policy, native revalidation and bypass/re-entry behavior.
- [x] Implementation: a denial leaves no task persisted; an allowance continues the normal authorized create path.
- [x] Verification: real persistence tests cover denial, invalid result, unavailable decision, concurrent/stale facts, bypass attempts and unchanged native checks.

## Delivery

Delivered in Daymark source `7ad6d36` with the [published Core 0.3.0 packages and guide edition 5](https://github.com/rore/keir-core/releases/tag/v0.3.0). All three rules use ordinary programs against the published model; the framework contains no task-specific capability or policy.

[Hosted demo verification](https://github.com/rore/keir-demo-app/actions/runs/42579777651) passed 515 Node tests with 12 explicit opt-in skips, and all 229 browser cases in one run. Earlier explicit process/crash checks passed all 12 cases. Focused official-package checks passed distribution/compiler verification, 183 native and binding tests, 69 authoring tests and nine real guard browser journeys. The seven-example repair regression also passes with extra user wording after removing duplicated navigation coaching; the context limit and requested examples are unchanged.

All three actual provider-authored rules compiled, passed their simulations and reached eligible review offers. A final live journey repeated this on the official packages and guide. No real tasks, approvals or selections were changed by qualification.

The running demo now serves the exact reviewed build. The schema-13 to schema-14 rollout preserved all 21 original tables' data and the provider settings byte for byte. Verified backups and the previous app remain available; restoration was rehearsed. Existing approved event selections remain unchanged and new before-save checks start off. Independent post-rollout review passed at desktop, narrow and short-screen sizes, using a disposable browser context without native writes or provider requests.

## Notes

The checkpoints below record development and earlier dependencies; the delivery above is the final accepted state.

Design and host-entry-point discovery are active. The host design must cover interactive, API and internal task creation, including task creation requested by existing post-event extensions. No active guard preserves ordinary native task behavior. An active guard that cannot establish an allowed result prevents persistence with an understandable recovery path. Existing native validation, current permissions, receipt identity and commit-time checks remain authoritative.

Completion includes independent design and result review, real compiler/simulator/native tests, cross-owner and stale-selection cases, desktop and narrow/short-screen walkthroughs, matching agent guidance and release pins, and updating the running demo after verification.

The entry-point audit found two live insertion paths: native task creation and the `tasks.create` adapter used by post-event rules. Both must reach the same guard coordinator before their existing short write transaction. Existing receipt replay and automation event suppression remain intact. Quick-add currently accepts only a title, so the designer is adding a pre-save details and correction flow using the existing native fields. The first design review requires retaining all three scoped examples rather than narrowing the feature to one project/priority condition. The demo developer owns native integration and UI implementation; the designer reviews actual rendered behavior and the architect reviews the consistency boundary.

The focused native recovery design passed independent review: one owner-scoped terminal result commits atomically with task and event, and explicit same-attempt retries compare the complete normalized candidate. Absent readback remains unresolved. Recovered creation snapshots never replace current task state, and readback/replay recheck current session and original-project access. This host-only mechanism adds no Core idempotency API. Its real concurrency, lost-response/restart, access-loss and reset tests remain implementation acceptance work.

Model review resolved the 18-type draft against Core's existing 16-type ceiling: the current catalog shares the identical task-result record and uses the already projected project ID instead of adding an unnecessary project lookup. All three business rules, existing capabilities and required event time remain available; no framework limit changes. READ qualification uses the existing task operations. Read facts are checked before insertion under the transaction lock, while post-insert checks retain authority and deadline protection without rejecting the operation's own expected changes.

The independent native/recovery slice passed 204 browser cases and all 12 explicit process/crash tests. These results precede real Core 0.3 integration and do not qualify the new authored guard end to end. Both designer recovery findings are now closed by a fresh rendered recheck: pending/unresolved saves use truthful Close wording, and recovered success keeps a readable in-form confirmation with appropriate focus while preserving newer edits. The review used real native persistence/readback in an isolated fixture at desktop, 320-pixel and short-screen sizes; it verified the original tasks were saved while newer drafts remained unsaved. Final acceptance still requires all three authored business rules, retained approved event execution, the actual guard/Studio rendered walkthrough and the verified running-demo update.

The final 83-file candidate has now passed those real compiler/runtime/native rule journeys and is integrated with exact file-hash verification. Existing approved generation-2 event programs execute alongside the generation-3 guard without transferring approvals. The owner reports 513 Node passes, 12 explicit process passes and 229 unique browser cases across completed split runs; all 16 focused guard/Studio tests pass after the last heading correction. Independent combined Studio review closed that final presentation finding. A fresh offline integration build reproduces the reviewed browser bundle. These are development-candidate results: a bounded live-provider authoring check, official release assets and pins, preserved-data rollout and verification of the running demo remain required before closure.

The additional live-provider check was blocked before launch by automatic approval review, which requires direct authorization for credential use and billable requests. No credential read, external request or spend occurred. The prepared isolated check and completed implementation evidence are retained while the integration owner requests that exact scope; existing release provenance remains a separate dependency.

The user approved the bounded live-provider check and release commit metadata. Both approval holds are cleared. The isolated provider check, official package build and preserved-data rollout are proceeding; their results remain required for completion.

After removing the validation-only cost ceiling as requested, all three actual provider-authored rules compiled, passed their real simulations and reached eligible review offers. The normal authoring loop recovered from compiler rejections in two journeys. Native task rows, approvals, selections and live settings were unchanged. The schema-13 to schema-14 migration rehearsal preserved all 21 existing tables' data, passed integrity and foreign-key checks, and verified restoration with the previous app. Official package adoption, hosted demo CI and the final running-demo update remain open.

**Dependencies:** [native-task-app](../features/native-task-app.md); [pre-operation-guard](../features/pre-operation-guard.md).

**Design input:** [Guard lifecycle and conformance, sections 6, 12 and 13A](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Pre-create task example, section 4D](../../docs/internal/design-baseline/public/task-management-poc.md).
