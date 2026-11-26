# Consolidate role guidance on main

<!-- agent-workflow:start -->
**Outcome:** New Core worktrees based on current main inherit the team's current role guidance.

**Target:** Core owns the role guides used by Core and Demo.

**Scope:** Reconcile the existing role edits with current main, retain published improvements, and make the Core role-loading and new-worktree rules explicit. No product code or Demo release changes.

**Constraints:** Preserve unrelated changes and active worktrees. Keep existing identities, model settings, permissions, and review gates. No force updates, checkout moves, or installed-tool changes.

**Completion criteria:** Main contains one coherent set of guides with valid mappings, no hardcoded model settings, and the owner's simplification, blocker recovery and follow-through requirements. A clean checkout of the resulting revision has those exact files. New worktrees default to current main; intentional older bases use current role guidance rather than silently falling back.

**Requirement baseline:** {"source":"owner role-source consolidation request","outcome":"New Core worktrees based on current main inherit the team's current role guidance.","scope":"Reconcile the existing role edits with current main, retain published improvements, and make the Core role-loading and new-worktree rules explicit. No product code or Demo release changes.","constraints":"Preserve unrelated changes and active worktrees. Keep existing identities, model settings, permissions, and review gates. No force updates, checkout moves, or installed-tool changes.","completion_criteria":"Main contains one coherent set of guides with valid mappings, no hardcoded model settings, and the owner's simplification, blocker recovery and follow-through requirements. A clean checkout of the resulting revision has those exact files. New worktrees default to current main; intentional older bases use current role guidance rather than silently falling back."}

**Risk:** Elevated

**Complexity:** Simple

**Reason:** Nine intended paths checked against Core Redline; GRAY, with no boundary violation. This is one guidance reconciliation, not a product or runtime change.

**Discovery:** Primary Core main has uncommitted role edits overlapping published changes. A separate Daymark worktree has two additional role proposals. Current published main is `5223178ad25dc713bf787fbf3f4ca877d9a246b9`. Some local role changes have focused reviews, while the combined snapshot needs independent review.

**Material assumptions:** Current role names and responsibilities are preserved. A worktree inherits files from its start commit, not from a working directory or the newest remote ref automatically.

**Plan:** Prepare a role-only delta on current main in an isolated checkout. Reconcile the seven changed role files, preserving upstream additions and the manager's follow-through and the shared authoring boundary. In Core `AGENTS.md`, require fresh role resolution and a current main base for new worktrees; for an intentional older base, read current guidance separately. Do not overwrite active or dirty checkouts. Verify the exact review delta before check-in.

**Verification plan:**

- Coherent guidance and valid mappings → compare the delta against published and local versions, validate links and mappings, scan for fixed model settings and private details, and run diff, Redline and Work Record checks.
- Exact role files on main → compare the commit's files in a fresh worktree and verify main contains that commit after integration.

**Plan review:** Agent technical review: `keir-role-guidance-main-plan-review`, PLAN GO recovered from the documentation reviewer's final response. Preserve published visual-review duties, mapping and Collection dispatch edits, manager follow-through, and shared host authority. Verify the exact merged files in a fresh worktree and the primary role source separately.

**Approvals:** The owner requested check-in on main. No force update, existing worktree rewrite, runtime change or Daymark publication.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Created an isolated checkout at the verified current main revision. The primary Core checkout and Daymark worktrees remain unchanged. The seven-file role delta overlaps published changes in designer, manager and shared guidance; the published documentation guide needs no edit. The independent reviewer confirmed the primary role source but did not see the review brief in the wake. Supplied the original references to resume the same review without resending. Await the verdict before editing the combined guides.

Recovered PLAN GO from the original documentation reviewer. The owner also requested repair of the primary checkout. Before any checkout change, verified a private byte-for-byte archive of the 14 tracked edits and ten untracked files. After integrating the reviewed role commit, preserve any remaining local changes separately and fast-forward the primary checkout; do not publish unrelated edits.

## Evidence

The isolated eight-file guidance diff over `5223178` originally had SHA-256 `4c80605c94d5b662de3cd78691fef50dfb14f569bf49a0130c8a5ae39cb07092`. All eight unique mappings match the index and readable guides. The published documentation guide is unchanged. The diff check and scoped check for local identifiers and fixed model settings passed. No product tests were needed.

The independent reviewer accepted the guide changes and found one ambiguity in `AGENTS.md`: the active source must be the primary Core roles for every task, not just older worktrees. Clarified source and base selection separately. The final guidance diff (`bfbf7814b52527a9448c699de39f1610857639a9c1e56bc1845bb8922d73909b`) passed diff check and was sent for a one-finding recheck. Separately parked 17 unrelated local files in a private, verified recovery stash backed by the byte archive. The primary working copy still serves the existing role files until integration.

## Result review

Agent technical review: `keir-role-guidance-main-result-review` and `keir-role-guidance-main-wording-recheck`, independent documentation reviewer. Final GO recovered from the review session.

Reviewed revision: `5223178` plus guidance diff SHA-256 `bfbf7814b52527a9448c699de39f1610857639a9c1e56bc1845bb8922d73909b`.

Verification adequacy: The reviewer matched the exact diff, confirmed that all seven role files were unchanged across the wording correction, and cleared the ambiguity. Primary Core is the only active role source, including for pinned historical worktrees. The reviewer reran diff check; worktree inheritance and primary checkout reconciliation are verified separately during delivery.
