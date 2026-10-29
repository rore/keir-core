# Advisory Agent Workflow CI

Approved for installation on 2026-10-29 after the user selected advisory reporting on main pushes and PRs. The installed workflow is `.github/workflows/agent-workflow.yml`.

The workflow runs on pushes to `main`, PRs targeting `main`, and manual dispatch. Both jobs use a read-only repository token. Redline validates both policies, computes complete change evidence and uploads its verdict. The dependent Agent Workflow job downloads that evidence and checks changed Work Records when present. Both jobs publish JSON reports to their job summaries and downloadable artifacts.

Findings with exit codes 1 or 2 are advisory. A policy schema error, missing or malformed verdict, unexpected exit code, or installation failure still fails CI so a broken reporter cannot appear healthy. Work Records remain optional, and the existing roadmap/navigation-document exemptions remain intact.

This adapts the bundled PR workflow for the approved advisory setup. It retains separate jobs, the Redline dependency, complete diff evidence, verdict artifact transfer and exit-code checks. It uses job summaries in place of sticky PR comments, avoiding write permissions and supporting direct pushes. No runtime hooks, CODEOWNERS changes, branch protection or required status checks are installed.

On PRs the diff starts at the merge base. On pushes it starts at the previous branch tip; a new branch uses the empty tree. Manual runs compare the selected commit with its parent, or the empty tree for a root commit. A missing base fails evidence collection.

When executable compiler/runtime code and conformance tests exist, revisit language boundaries and enforcement. Proposed future required checks would be `redline` and `agent-workflow`, but neither is required now. CODEOWNERS and protected branches need a separate explicit decision. Application build/test CI belongs in the demo repository when it has runnable code.
