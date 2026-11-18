# Companion qualification anchor regression

<!-- agent-workflow:start -->
**Outcome:**
Restore packaging CI while preserving the packaged qualification-link obligation.

**Target:**
rore/keir-core: authoring companion packaging tests.

**Scope:**
Update the stale edition-9 qualification heading assertion and this task record only.

**Constraints:**
No release, runtime, compiler, documentation rewrite, policy change or weakened archive checks.

**Completion criteria:**
Packaged guide still links to the qualification anchor; missing anchor fails; all three packaging suites pass.

**Requirement baseline:**
{"source": "work-record-initial", "outcome": "Restore packaging CI while preserving the packaged qualification-link obligation.", "scope": "Update the stale edition-9 qualification heading assertion and this task record only.", "constraints": "No release, runtime, compiler, documentation rewrite, policy change or weakened archive checks.", "completion_criteria": "Packaged guide still links to the qualification anchor; missing anchor fails; all three packaging suites pass."}

**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}

**Risk:**
Elevated

**Complexity:**
Simple

**Reason:**
Test script is gray under current Redline policy; no product behavior changes.

**Discovery:**
At b6be791e81d1c8446a69db4a9d398dc12c82c7e4, 22/23 companion tests pass. The specification index retains an explicit numeric-qualification-status anchor after its heading was removed; the edition-9 guide still links to it.

**Material assumptions:**
The obligation is a resolvable packaged qualification link, not fixed heading wording. If review finds the old section content is required by packaging, stop and re-plan rather than suppressing the failure.

**Plan:**
Replace the literal heading assertion with explicit guide-link and HTML-anchor assertions. Preserve all other archive, byte, pin, inventory and offline checks. Demonstrate rejection by removing the anchor in a temporary mutation, restore it, then run all three packaging suites.

**Verification plan:**
Qualification link and anchor remain checked -> focused positive and missing-anchor mutation.
Packaging remains intact -> test-authoring-companion.py, test-verify-core-release.py, test-release-core.py.
Scope and process -> diff check, fresh Redline and workflow validation, independent review, exact-commit CI.

**Plan review:**
Agent technical review: independent packaging reviewer, recorded in Plan review below.

**Approvals:**
User authorized fixing and integrating the packaging failure; no release authorized.

**Exceptions:**
—

**State:**
Ready for review
<!-- agent-workflow:end -->

## Implementation

Task base: b6be791e81d1c8446a69db4a9d398dc12c82c7e4 on main. Planned files: `scripts/test-authoring-companion.py` and this record. Persistent role tooling is unavailable; no role identity assumed. No roadmap scope or status changes.

## Plan review

Independent clean-context technical review approved before implementation: the explicit anchor preserves the intentionally retained fragment destination. Checking the guide destination plus the archive anchor is equivalent fragment-resolvability coverage plus inbound-link coverage, not qualification-content certification. Missing-anchor mutation must fail at the assertion, then restore.

## Evidence

Baseline b6be791e: companion suite 22 pass, one stale-heading failure. Updated tree: companion 23/23 (2.281 s), release verifier 7/7 (0.033 s), release orchestration 5/5 (0.012 s). Temporary removal of the explicit anchor fails the focused edition-9 test at the new anchor assertion; original README bytes restored and companion 23/23 rerun passes. No production/archive-generation changes or published assets. This is equivalent link-target coverage plus an added inbound-link assertion.

Implementation completed in the two planned paths only; heading wording is no longer part of the packaging obligation. No roadmap change is needed for this CI maintenance fix. Exact remote CI verification follows integration.

## Result review

Agent technical review: independent packaging reviewer inspected staged diff and actual logs; approved without technical blockers. Reviewer was started with an explicit `fork_turns: none` self-contained brief.

Reviewed revision: staged tree df3376b457ff2594ab5e2266948f853b5dd18289 on base b6be791e81d1c8446a69db4a9d398dc12c82c7e4, before this evidence-only addition.

Verification adequacy: all three packaging suites exercise the affected pipeline; the temporary missing-anchor mutation proves the replacement assertion catches a broken fragment target. Scope stayed at two files, all previous checks remain, and restored documentation has no diff. No product or release claim is inferred. Fresh Redline remains GRAY for the test path, with no boundary/API/schema/security/runtime changes. Direct-main delivery follows the current process; legacy configuration wording does not add publication authority.
