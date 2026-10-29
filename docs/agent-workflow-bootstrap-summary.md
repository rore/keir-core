# Agent Workflow advisory bootstrap summary

Date: 2026-10-29

This is the approved advisory subset for `keir-core`. Agent Workflow is opt-in for ordinary work. Updates confined to `roadmap/`, `docs/README.md`, or `docs/roadmap.md` need no Work Record. The `keir-demo-app` repository has no Agent Workflow process.

Follow-up on 2026-10-29: the user approved [advisory CI](agent-workflow-ci-proposal.md) for main pushes and PRs. The findings below describe the original local bootstrap; the CI follow-up adds reporting without required workflow checks.

## Installed

- The previously copied Agent Workflow and Minimap skills remain in `.agents/skills/` and `.claude/skills/`.
- `agent-workflow.yaml` uses local Work Records, makes them optional for branch changes, and makes a Redline verdict optional.
- `agent-redline-policy.yaml` classifies the current policy files and accepted decisions as red, the named roadmap/navigation paths as blue, and runs in shadow mode. It declares no language extension or dependency boundary.
- `scripts/agent-workflow-check.py`, `scripts/agent-redline-report.py`, the three `scripts/agent-workflow-runtime.*` adapters, and `.agent-redline/agent-policy.schema.json` were copied byte-for-byte from the installed skill package.
- `AGENTS.md`, `CONTRIBUTING.md`, and `.agent-workflow/tasks/README.md` describe the advisory use and exact documentation exemption. `CLAUDE.md` still imports `AGENTS.md`.

## Verification

- Both active YAML files parse, validate against the installed schemas, and match the approved Phase 3 draft by parsed content.
- Each copied script and schema matched its source SHA-256 after copying.
- The local checker resolved `agent-workflow:bootstrap-keir-core` to the intended Work Record. A check of that record had no blocking predicates; it returned advisory exit code 1 because no Redline verdict was supplied.
- With a synthetic roadmap-only changed-path list, Redline returned `BLUE`, exit code 0. With synthetic roadmap and decision paths, it classified the decision as red and returned advisory `RED`, exit code 1. These probe lists are not a real Git diff.
- No `.github/workflows/`, CODEOWNERS, Claude/Codex hook settings, or pre-push hook was installed. No global tool or application setting was changed.

## Deferred

At bootstrap time, the repository had no commit, remote, build/runtime implementation, or tests. A full changed-files probe, live default-branch protection check, CI run, runtime-hook coverage check, and language-boundary calibration were unavailable. The normal upstream bootstrap's hook, CI, branch-protection and review-enforcement steps were intentionally deferred; they require a separate decision when implementation and verification exist. Direct pushes to `main` remain subject to whatever rules the remote applies.

The clean-context technical review took place after the initial file writes and found no mismatch with the advisory intent. The Work Record records that sequencing limit; no earlier review is claimed.
