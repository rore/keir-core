# Refresh repository-local Workflow guidance

Canonical task: `agent-workflow:workflow-guidance-refresh`. This is a bounded repository maintenance request, with no assigned roadmap item. Upstream [PR 62](https://github.com/rore/agent-workflow/pull/62) and [PR 63](https://github.com/rore/agent-workflow/pull/63) supply reviewed dependencies rather than a separate Keir task identity.

<!-- agent-workflow:start -->
**Outcome:** Keir's repository-local Agent Workflow mirrors provide the reviewed task-ownership and settled-decision handoff guidance.
**Target:** keir-core
**Scope:** Audit and refresh `.agents/skills/agent-workflow/` and `.claude/skills/agent-workflow/` against the reviewed upstream distribution at `c0d69150ca05cdfab7951f741c16f7a477c7630a`, preserving local customization and reporting active consumer checkout gaps; maintain this separate Work Record.
**Constraints:** Preserve local or newer changes and manifest customization. Do not change hooks, CI, policy, product runtime, services, global installations or configuration; do not rewrite completed task records. Do not edit frozen UI source or record checkouts. The task owner retains canonical main, roadmap, integration and delivery.
**Completion criteria:** Both mirrors contain the applicable reviewed upstream guidance with byte/manifest/link validation; local customization remains intact; focused Workflow checks pass or explain a concrete compatibility gap; active consumer checkout adoption and exact revision/hash evidence are reported for owner-managed delivery.
**Requirement baseline:**
{"source":"Task-owner request to refresh repository-local Agent Workflow consumers against reviewed upstream PRs 62 and 63.","outcome":"Keir's repository-local Agent Workflow mirrors provide the reviewed task-ownership and settled-decision handoff guidance.","scope":"Audit and refresh `.agents/skills/agent-workflow/` and `.claude/skills/agent-workflow/` against the reviewed upstream distribution at `c0d69150ca05cdfab7951f741c16f7a477c7630a`, preserving local customization and reporting active consumer checkout gaps; maintain this separate Work Record.","constraints":"Preserve local or newer changes and manifest customization. Do not change hooks, CI, policy, product runtime, services, global installations or configuration; do not rewrite completed task records. Do not edit frozen UI source or record checkouts. The task owner retains canonical main, roadmap, integration and delivery.","completion_criteria":"Both mirrors contain the applicable reviewed upstream guidance with byte/manifest/link validation; local customization remains intact; focused Workflow checks pass or explain a concrete compatibility gap; active consumer checkout adoption and exact revision/hash evidence are reported for owner-managed delivery."}
**Relationships:**
{"schema_version":1,"sources":[],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Simple
**Reason:** Installed governance guidance and bundled tooling are unclassified GRAY paths. This is one bounded distribution refresh without policy or live integration changes.
**Discovery:** Canonical core is clean at `951676197bae975c348bf782604501ec4affc51c`. The exact upstream target object exists; its normal merge and prior ownership merge were verified through the primary repository. The upstream working checkout has unrelated modifications and will remain untouched. Mirror/customization inventory is pending.
**Material assumptions:** The reviewed distribution is compatible with the current advisory consumer setup. A local/newer conflict or uncovered governance change stops the affected refresh for task-owner resolution. Frozen consumer checkouts remain owner-managed and may require deferred adoption.
**Plan:** Record context, inventory exact committed package bytes versus both installed mirrors, verify applicable upstream review identities, then update only stale uncustomized package content and corresponding manifest entries. Preserve exclusions and extra files. Verify mirror parity, package bytes, internal references, scoped risk and focused checker behavior. Report exact commits, hashes and deferred consumers for task-owner integration; do not merge or push canonical main.
**Verification plan:**
- Reviewed guidance adoption -> exact upstream Git-object comparison and task-ownership/handoff passage assertions in both mirrors.
- Customization and compatibility -> full package/mirror inventory, unchanged-exclusion and manifest checks, no hook/config/policy changes, focused Workflow validation.
- Consumer and delivery scope -> read-only active-checkout inventory, privacy/whitespace/diff inspection and exact revision/hash handoff.
**Plan review:** Pending inventory and verification of exact upstream independent reviews before implementation. Reuse only unchanged applicable reviewed content; uncovered compatibility or behavioral changes require review.
**Approvals:** The task owner verified the user's consumer-refresh request and authorized this isolated repository scope. Not required at Elevated risk; no live-environment change is authorized.
**Exceptions:** —
**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

- Context and immutable baseline recorded before distribution discovery or refresh. Isolated branch starts at `951676197bae975c348bf782604501ec4affc51c`; only the two installed mirror trees and this record are writable task sources. Inventory and review-reuse evidence are the next checkpoint. No package files have changed.

## Evidence

- Upstream PR 62 is merged as `29c508a350942d2127c337655907fb02cc8cfdb0`; PR 63 is merged as `c0d69150ca05cdfab7951f741c16f7a477c7630a`. Primary repository metadata identifies their reviewed source and final tested heads. Detailed applicability and installation evidence follow after inventory.
