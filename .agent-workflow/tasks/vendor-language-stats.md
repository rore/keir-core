<!-- agent-workflow:start -->
**Outcome:**
GitHub language classification distinguishes imported agent tooling from Keir implementation without hiding Keir-authored tooling.

**Target:**
keir-core repository language metadata.

**Scope:**
Add root .gitattributes for the imported Agent Workflow and Minimap packages and their supplied workflow entry-point scripts; maintain this task record. Do not change package contents or consolidate installations in this task.

**Constraints:**
Preserve all implementation, package bytes, runtime behavior, text normalization and existing working changes. Do not mark all Python or all scripts vendored. No installed-tool, environment, symlink, Git configuration, commit, push or release changes.

**Completion criteria:**
Explicit imported paths receive linguist-vendored; representative Keir sources and authored scripts remain unspecified. Check tracked-file coverage, unchanged imported bytes, attribute behavior, whitespace and public-content privacy. Record independent review and distinguish local verification from GitHub recomputation after authorized publication.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"GitHub language classification distinguishes imported agent tooling from Keir implementation without hiding Keir-authored tooling.","scope":"Add root .gitattributes for the imported Agent Workflow and Minimap packages and their supplied workflow entry-point scripts; maintain this task record. Do not change package contents or consolidate installations in this task.","constraints":"Preserve all implementation, package bytes, runtime behavior, text normalization and existing working changes. Do not mark all Python or all scripts vendored. No installed-tool, environment, symlink, Git configuration, commit, push or release changes.","completion_criteria":"Explicit imported paths receive linguist-vendored; representative Keir sources and authored scripts remain unspecified. Check tracked-file coverage, unchanged imported bytes, attribute behavior, whitespace and public-content privacy. Record independent review and distinguish local verification from GitHub recomputation after authorized publication."}

**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Simple

**Reason:**
The root attributes file is an unclassified path under the existing policy, giving an Elevated floor. The change affects language statistics only, not execution or repository policy.

**Discovery:**
Tracked imported packages are Agent Workflow and Minimap under both .agents/skills and .claude/skills. Five supplied agent-workflow/agent-redline scripts are separate entry points. Real Keir compiler/runtime sources and authored helper scripts must remain visible. Root .gitattributes and this task record were absent at pickup.

**Material assumptions:**
Only linguist-vendored is needed. If attribute checks show text/eol/filter changes, an unintended match or an unverified provenance, stop and narrow the rule rather than alter tooling or global settings.

**Plan:**
Classify the two intended paths with fresh Redline evidence; obtain independent review of this plan. Add four anchored recursive package rules and five anchored entry-point rules. Validate every tracked imported file and representative non-vendor files, prove package/source bytes unchanged, then obtain focused result review. Keep upstream consolidation separate.

**Verification plan:**
- Imported paths and authored-code visibility -> exhaustive native Git check-attr inventory and synthetic negative controls.
- Unchanged bytes and normalization -> pre/post tracked-file hashes and text/eol/filter/working-tree-encoding/ident comparison.
- Task classification and structure -> existing Redline reporter and workflow checker, with no dependency installation and only a validated non-error verdict accepted.
- Public-content safety -> inspect actual new files for whitespace and private operational data; do not claim live GitHub recalculation before publication.

**Plan review:**
Agent technical review: independent documentation-role PLAN GO with no mandatory findings; see Plan review evidence below. The complete planned scope and verification approach are accepted; implementation may begin.

**Approvals:**
The owner requested accurate vendored marking. On 2026-11-12 the owner approved reviewed local commits and candidate publication, conditional on independent review and privacy checks, under the bounded finish plan. This supersedes the original preparation-only publication restriction for this reviewed result; installed-environment changes, releases and upstream consolidation remain excluded. The original requirement baseline is preserved.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

- Established the bounded language-metadata task separately from guarded-result capability delivery and upstream tooling consolidation. Existing work is preserved.
- Fresh classification and independent plan review are complete. Beginning the accepted attributes-only implementation; imported package contents remain untouched.
- Added the nine accepted root-anchored linguist-vendored rules. Focused native Git and byte-preservation checks pass; preparing the fixed result for independent review.

## Plan review

The independent documentation role inspected the canonical task plan, imported package/script provenance and existing nested attributes against base c4b61e94873fdcf2b64d7c2cfb34b9475ab4f0fd. Verdict: PLAN GO, no mandatory findings. Four anchored package rules and five exact supplied-script rules avoid blanket language/folder vendoring. Exhaustive tracked-file checks, authored-code negative controls, unchanged bytes/normalization and privacy inspection are adequate for the planned local configuration result. Online extraction was unavailable to the reviewer; native Git behavior must still be verified. This approval does not claim implementation, publication or live GitHub recomputation.

## Evidence

Fresh pre-edit and final Redline reports are GRAY in shadow mode for .gitattributes, with no red paths or boundary violations and no API/schema/security/runtime-configuration change. The final report counts 11 changed attributes lines and excludes the task record. Exit 1 is the recorded review warning, not an execution error. The explicit task resolver found the initial Blocked record; it is now Ready for review.

The actual post-edit check passes for all 1,105 tracked baseline files: 279 imported files receive linguist-vendored, 826 other tracked paths retain their previous attributes, and all 1,105 working-file byte hashes remain unchanged. Six synthetic negative controls also remain unspecified. Existing text, eol, filter, working-tree-encoding and ident attributes are unchanged. Actual new-file privacy and trailing-whitespace checks pass. No GitHub-statistics result is claimed before publication. Operational paths, raw evidence and the complete fixed inventory belong in ignored local records.

The workflow structural check was clean before the result-review transition, with no blocking failures or advisories. Independent result review subsequently passed as recorded below. An intermediate generated diff used the wrong line-ending serialization and was rejected; that invalid report is not accepted evidence. The final reporter input uses canonical LF and its non-error verdict is validated before the checker is invoked. No supplied tool was edited to pass verification.

## Result review

Agent technical review: independent documentation-role RESULT GO on 2026-11-12; no mandatory findings.

Reviewed revision: base `c4b61e94873fdcf2b64d7c2cfb34b9475ab4f0fd` plus the fixed two-path snapshot identified by the attributes and record hashes below.

Verification adequacy: exhaustive native Git attribute and byte-preservation checks cover the full 1,105-file baseline, authored-code near misses and normalization invariants; privacy, whitespace and valid non-error classification evidence are sufficient for this local metadata-only change, not a claim of live GitHub recomputation.

Independent documentation-role RESULT GO on 2026-11-12, with no mandatory findings. The reviewer inspected the exact frozen two-path result, verified all 1,105 baseline tracked files independently, confirmed 279 imported files are vendored while 826 other paths and all baseline bytes retain their prior values, checked six near-miss controls and preserved normalization attributes, and inspected privacy, whitespace and the valid non-error Redline evidence. The reviewed attributes SHA-256 is `89b89e10db0eeb6e6da7f9bc930e3c9d23b2a73a02cfa6d5ecd0ee1cafcbf74c`; the pre-verdict record SHA-256 is `a12ff1c4c3549e7d926df1ef16e65f5f5c0d7a5ca6285a635d8b9e444ba41758`. Subsequent record-only edits capture that verdict and the owner's delivery approval; the attributes and imported bytes are unchanged. The post-review structural check passed with exit 0 and no failed predicates. Manager accepts this reviewed result for authorized publication, whose actual commit and remote readback belong to delivery evidence. This review does not claim upstream consolidation or live GitHub language recalculation.
