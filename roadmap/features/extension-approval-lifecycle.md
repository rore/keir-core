---
id: "extension-approval-lifecycle"
title: "Review, activate, disable and roll back exact extension revisions"
status: "blocked"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

**Hold:** The real-task lifecycle iteration is held for the user-directed base-design audit. Preserve exact revisions, approvals, selections, historical receipts and current user data; no new implementation, acceptance or rollout proceeds before corrected scope is reconciled.

Provide a manual source-to-simulation-to-approval-to-activation workflow before adding an AI author.

Backend lifecycle work can proceed independently; its customer-facing UI follows the reviewed [Extension Studio design](extension-studio-design.md), including clear draft/active identities, approval invalidation and recoverable navigation.

## In Scope

- Retain linked source/artifact/model/environment/adapter/consumer/scenario identities and independent expected outcomes.
- Choose trustworthy selection storage or signing from an explicit threat model; content hashes alone are not authority.
- Add scoped active selection, disable/revoke, rollback for new invocations and retention of approved dependencies/in-flight selections.
- Keep sources, scenarios, approvals and active bindings isolated per demo user. Show the same hook with different customizations for two users; disabling or rolling back one user's binding must leave the other's behavior intact.
- Let users delete unwanted customizations through an explicit confirmation, turning off the selected item while retaining audit and spending records.

## Out of Scope

An LLM requirement, a generic registry service in core or implicit migration to the latest model.

## Done When

- [ ] Design/spec: define approval authority, invalidation/reapproval and lifecycle behavior, including selection versus event admission.
- [x] Implementation: manually compile, simulate, inspect actual behavior, approve and activate an exact revision; disable and roll back it.
- [x] Verification: tampering, unauthorized/cross-user approval or selection, edits after review, restart, revoked dependencies and old snapshot coexistence behave as specified. The owning demo user may approve their own extension where host policy permits; the agent cannot self-approve.
- [x] Customization deletion: confirmed removal, selected-item handling, owner isolation and protection against stale work or replay resurrecting the item.

## Notes

**Deletion delivered:** [Customization deletion](https://github.com/rore/keir-demo-app/commit/0b4f6d9060c95a458824db84cac3e7e52a65c671) is reviewed, pushed and running in the normal demo. Each card has a confirmed Delete action; deleting the selected item turns it off atomically. Owner/stale/busy checks, terminal replay guards and retained audit/accounting protect existing work. Independent review, real compiler/runtime browser deletion, desktop/mobile confirmation and current-data migration checks pass. [Hosted CI](https://github.com/rore/keir-demo-app/actions/runs/41827591355) passes all 265 fast tests and 131 browser cases after two existing browser fixtures/assertions were corrected without weakening behavior checks. The backed-up rollout preserves all 18 migrated tables, settings and accounting and serves the exact tested assets. Verified recovery archives and task cleanup are recorded in the [Work Record](../../.agent-workflow/tasks/customization-deletion.md). The broader approval-contract design remains open.

**Dependencies:** [real-task-reads](../features/real-task-reads.md); [reusable-simulation](../features/reusable-simulation.md).

**First increment delivered:** Demo [df06843](https://github.com/rore/keir-demo-app/commit/df0684301ff4a3909d7b71d019e5c63d93d23193) adds the actual read-only Studio lifecycle: choose a prepared program, run six-case simulation, inspect exact evidence, explicitly approve and activate it for one user, observe real task advice, then disable or restore a named previous choice. Native tasks and other users remain unaffected. Transactional migration, operator provenance, owner-scoped evidence/approval, selection generations, stale-response suppression and bounded execution are implemented. Independent result review approved the corrected source; 38 fast checks, nine native browser journeys, eight lifecycle journeys and the standalone smoke pass. [Hosted CI passes](https://github.com/rore/keir-demo-app/actions/runs/41432168250). See the [Work Record](../../.agent-workflow/tasks/read-extension-lifecycle.md) and [usage/contract guide](https://github.com/rore/keir-demo-app/blob/main/docs/read-extension-lifecycle.md).

**Remaining:** The implemented and verified scope is the local read-only host, including prepared and newly authored revisions. This feature stays active for a normative approval contract covering selection versus event admission and broader hosts. Real effects belong to later milestones. AI authoring is tracked separately in [conversational authoring](conversational-authoring.md).

**Design input:** [Review and immutable activation, sections 1 and 6](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Selection/lifecycle, sections 5, 7 and 11](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).

**Result review:** Independent review reproduced and then verified correction of the simulation fixture's frozen clock. Each case now receives a real monotonic one-second checked deadline; an actual-simulator regression proves overdue work cannot produce approvable evidence. No core semantic change or hard-preemption guarantee was introduced.

The first scalar host integrations may use reviewed operator configuration for exact selection. A full customer approval UI is not a prerequisite to that early demonstration.

**Authored increment delivered:** Demo [45a56e0](https://github.com/rore/keir-demo-app/commit/45a56e061cc2192e8c62fce7381d3b22fe952541) adds actual source compilation, immutable source/artifact revisions, independently written scenarios, exact saved-content review, separate approval/activation and visible saved-task behavior. Independent technical and rendered reviews approve the corrected source. The combined build and 97-test fast suite pass; all 44 browser journeys and [hosted qualification](https://github.com/rore/keir-demo-app/actions/runs/41519984857) pass. The [authored-draft Work Record](../../.agent-workflow/tasks/authored-extension-drafts.md) retains scope, limits and evidence. The host remains read-only; the next increment connects the AI author without changing approval authority.
