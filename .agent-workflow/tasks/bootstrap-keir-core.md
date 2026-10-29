# Bootstrap Agent Workflow in keir-core

<!-- agent-workflow:start -->
**Outcome:** Prepare an advisory Agent Workflow setup for keir-core.

**Target:** keir-core, main.

**Scope:** Inspect the existing repository, prepare the exact local policy/configuration draft, then install the approved advisory files and verify them.

**Constraints:** Preserve direct pushes to main. No required PR, status check, branch protection, native edit/push hook, service restart, global tool change or demo-repository installation. Preserve existing AGENTS.md and CLAUDE.md guidance.

**Completion criteria:** The developer reviews the draft; approved advisory configuration validates, local checks report their actual evidence, and a clear summary distinguishes installed parts from deferred enforcement.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Prepare an advisory Agent Workflow setup for keir-core.","scope":"Inspect the existing repository, prepare the exact local policy/configuration draft, then install the approved advisory files and verify them.","constraints":"Preserve direct pushes to main. No required PR, status check, branch protection, native edit/push hook, service restart, global tool change or demo-repository installation. Preserve existing AGENTS.md and CLAUDE.md guidance.","completion_criteria":"The developer reviews the draft; approved advisory configuration validates, local checks report their actual evidence, and a clear summary distinguishes installed parts from deferred enforcement."}

**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Simple

**Reason:** This changes repository governance configuration, even though enforcement is deferred.

**Discovery:** No Git commit, remote, build/runtime implementation, CI, CODEOWNERS or prior workflow configuration. The Java and Node component directories contain README placeholders. Agent Workflow skill package is already copied locally for Codex and Claude. The current user requested direct-to-main work without enforced process.

**Material assumptions:** The user wants an advisory configuration before an enforced PR workflow. Disproved by a request for immediate hooks or CI; then revise the proposal before installing those integrations.

**Plan:** Confirm the Phase 1 inspection with the developer, then review and adapt `docs/agent-workflow-advisory-proposal.md`. After policy sign-off, write only the approved local files, validate schemas and run bounded local checks.

**Verification plan:**
- Approved policy fidelity -> compare parsed configs with signed-off YAML blocks and validate both schemas.
- No automatic enforcement -> check absence of hook, CI and branch-protection files.
- Local tool reachability -> resolve the bootstrap Work Record and run Redline reporter on representative changed-path lists.
- Honest limits -> record that no HEAD, remote or full Git-diff probe is available.

**Plan review:** Agent technical review: Codex subagent /root/bootstrap_review (2026-10-29, retrospective read-only review). Phase 1 inspection and Phase 2 documentation scope were confirmed by the developer. The adapted Phase 3 policies in `docs/agent-workflow-advisory-proposal.md` received explicit sign-off on 2026-10-29. The earlier draft was presented before the required Phase 1 checkpoint and superseded. The clean-context review occurred after initial installation; no pre-implementation review is claimed.

**Approvals:** Not required at this risk level; developer explicitly signed off on the adapted Phase 3 policies on 2026-10-29.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Installed the signed-off YAML policies, copied the local checker, Redline reporter, runtime adapters and policy schema from the existing skill package, then updated the repository guidance and Work Record README. Left hooks, CI and host settings untouched.

## Evidence

Git HEAD was unavailable because the repository has no commits. The developer confirmed the Phase 1 inspection, temporary lightweight flow and final Phase 3 policies. No policy or runtime hook was activated before sign-off.

- `docs/agent-workflow-bootstrap-summary.md` records installed files, deferred controls and probe results.
- Both active policies matched the approved YAML draft by parsed content and passed schema validation.
- Copied scripts and schema matched source SHA-256; the checker resolved this Work Record.
- The checker had no blocking predicate failures. It returned advisory exit code 1 because no Redline verdict was supplied.
- Synthetic Redline probes returned BLUE/0 for a roadmap path and RED/1 for a roadmap plus decision path. No real Git-diff probe was possible.
- No CI workflow, native hook, CODEOWNERS or branch-protection setting was added.

## Result review

Agent technical review: Codex subagent /root/bootstrap_review (2026-10-29, read-only review of plan and installed files).
Reviewed revision: uncommitted `keir-core` working tree on 2026-10-29; no Git HEAD exists.
Verification adequacy: schema, hash, resolver and synthetic Redline probes cover the advisory files and path classification; full Git-diff, CI and branch-protection behavior remain unverified until commits and a remote exist.

The reviewer found no mismatch with the user's opt-in/direct-main requirement. It found stale proposal and Work Record status, which were corrected. The review occurred after initial writes, so it does not establish a pre-implementation plan review.
