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
**Discovery:** Both installed mirrors contain 77 tracked files, all byte-identical to upstream `9e05090`; no local/newer payload, extra manifest entry or manifest size mismatch was found. Target distribution has 78 files: ten modified files and one new checkpoint per mirror. The update includes reviewed PRs 58–63; hook payloads, integrations, policy and configuration are unchanged. Exact final file groups match their independent upstream result reviews. The separate frozen active record checkout still carries older guidance and will be reported for deferred owner-managed adoption. Root CI/checker scripts remain outside this scope.
**Material assumptions:** The reviewed distribution is compatible with the current advisory consumer setup. A local/newer conflict or uncovered governance change stops the affected refresh for task-owner resolution. Frozen consumer checkouts remain owner-managed and may require deferred adoption.
**Plan:** Record context, inventory exact committed package bytes versus both installed mirrors, verify applicable upstream review identities, then update only stale uncustomized package content and corresponding manifest entries. Preserve exclusions and extra files. Verify mirror parity, package bytes, internal references, scoped risk and focused checker behavior. Report exact commits, hashes and deferred consumers for task-owner integration; do not merge or push canonical main.
**Verification plan:**
- Reviewed guidance adoption -> exact upstream Git-object comparison and task-ownership/handoff passage assertions in both mirrors.
- Customization and compatibility -> full package/mirror inventory, unchanged-exclusion and manifest checks, no hook/config/policy changes, focused Workflow validation.
- Consumer and delivery scope -> read-only active-checkout inventory, privacy/whitespace/diff inspection and exact revision/hash handoff.
**Plan review:** Agent technical review: reuse the exact independent upstream plan/result reviews for unchanged application, as permitted by installed operating-mode, Clean-context delegation. Reviewed file groups are unchanged at the target from `a87de2378b68b8808878076c5454574620bda215` (PR 62 ownership), `268b302a62e7146d8be66479b24f0b99bdd1e0a4` (PR 63 handoff) and `e1003351a169e1bf8ce350dd964e8d1cd49b8a73` (PR 61 privacy/checker carryforward, including prior reviewed changes). Primary PR metadata confirms merged identity and tested final heads. Destination keeps the existing documentation exemption, advisory policy, task-specific identity and absence of live hooks; optional migration support grants no migration approval. This is review reuse, not a new independent consumer review. See Plan review.
**Approvals:** The task owner verified the user's consumer-refresh request and authorized this isolated repository scope. Not required at Elevated risk; no live-environment change is authorized.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Context and immutable baseline recorded before distribution discovery or refresh. Isolated branch starts at `951676197bae975c348bf782604501ec4affc51c`; only the two installed mirror trees and this record are writable task sources. Inventory and review-reuse evidence are the next checkpoint. No package files have changed.
- Inventory completed against committed Git archives. All ten stale files per mirror match the prior upstream package exactly; the new checkpoint is absent in both. Refresh the complete reviewed package delta, including its optional checker/decoder support and size manifest, without invoking bootstrap or changing deployed root tooling. No customization needs merging. A baseline privacy receipt would require separately bound human approval; no receipt or existing task record is changed here. The local shared-record instruction remains applicable to same-task contributors; the refreshed ownership guidance distinguishes independently delivered tasks without changing the local task's owner.

## Plan review

- Verified unchanged reviewed application with empty Git diffs for each deployed file group: operating-mode against PR 62 reviewed result `a87de2378b68b8808878076c5454574620bda215`; plan-and-review against PR 63 reviewed parity repair `268b302a62e7146d8be66479b24f0b99bdd1e0a4`; remaining checker/decoder/checkpoint/template payload against PR 61 accepted carryforward `e1003351a169e1bf8ce350dd964e8d1cd49b8a73`. Manifest changes are mechanical file-size inventory for those same bytes.
- Reviewed compatibility: current consumer configuration remains local/advisory, requires explicit task identity, and deliberately excludes documentation-only work. These consumer choices retain priority. New optional privacy migration controls fail closed without bound approval/history; installation neither executes migrations nor authorizes retrospective changes. Deployed root checker/hooks and frozen checkouts remain unchanged and their version gap will be disclosed. No material uncovered governance or environment change is needed.
- Readiness is limited to copying the unchanged reviewed distribution into the two isolated mirror trees, validating it and reporting owner-managed integration/adoption. No broad product suite or duplicate upstream review is justified for this exact package application.

## Evidence

- Upstream PR 62 is merged as `29c508a350942d2127c337655907fb02cc8cfdb0`; PR 63 is merged as `c0d69150ca05cdfab7951f741c16f7a477c7630a`. Primary repository metadata identifies their reviewed source and final tested heads. Detailed applicability and installation evidence follow after inventory.
