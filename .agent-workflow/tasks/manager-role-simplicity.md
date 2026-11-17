# Prioritize simplicity in the manager role

Identity: `agent-workflow:manager-role-simplicity`. Base: `2900a568ed9de93b0b4126da208a4d216b62e325`.

<!-- agent-workflow:start -->
**Outcome:**
The manager prioritizes the simplest sufficient code and process, cost, time and valid evidence reuse without weakening safeguards.

**Target:**
Keir manager role guidance.

**Scope:**
Update `roles/manager.md` and this required Work Record only.

**Constraints:**
Preserve correctness, safety, privacy, required verification, authority boundaries and repository workflow. No shared-policy, runtime, application or environment changes. Commit and push the verified role improvement under the owner's delivery instruction.

**Completion criteria:**
The role leads with the requested primary objective, removes overlapping wording, retains required responsibilities and links, and introduces no new process gate.

**Requirement baseline:**
{"source":"Owner request to improve the manager role with the supplied simplicity objective","outcome":"The manager prioritizes the simplest sufficient code and process, cost, time and valid evidence reuse without weakening safeguards.","scope":"Update `roles/manager.md` and this required Work Record only.","constraints":"Preserve correctness, safety, privacy, required verification, authority boundaries and repository workflow. No shared-policy, runtime, application, environment, commit or push changes.","completion_criteria":"The role leads with the requested primary objective, removes overlapping wording, retains required responsibilities and links, and introduces no new process gate."}

**Relationships:**
{"schema_version":1,"sources":[],"pull_requests":[]}

**Behavior changes:**
[{"target":"task-context.constraints","classification":"requirement-change","before":"Preserve correctness, safety, privacy, required verification, authority boundaries and repository workflow. No shared-policy, runtime, application, environment, commit or push changes.","after":"Preserve correctness, safety, privacy, required verification, authority boundaries and repository workflow. No shared-policy, runtime, application or environment changes. Commit and push the verified role improvement under the owner's delivery instruction.","reason":"The owner corrected the repeated failure to publish requested edits without a separate reminder.","impact":"Publish the verified role improvement; unrelated changes and environment operations remain excluded.","alternatives":"Keep the requested improvement local and continue requiring a separate push reminder.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"Owner correction after requesting commit/push as part of finishing repository changes","verbatim":"i don't need to remind you to push such things, it should be obvious"}}]

**Risk:** Elevated
**Complexity:** Simple
**Reason:**
Fresh Redline classifies the unlisted role path GRAY. This is a narrow prose change, not a runtime or permission change.

**Discovery:**
Shared guidance already requires simple solutions and evidence reuse. The manager guide lacks a clear primary objective covering coordination overhead. Its simplification-tracking bullet and final premature-gates sentence overlap with the requested objective.

The owner later identified a concrete finish failure: publication was called complete while the existing local demo remained stale. Shared guidance already requires keeping it current. Add one manager-specific completion sentence to the existing delivery bullet, without adding a new gate or granting environment permission.

**Material assumptions:**
The supplied objective complements existing safeguards; it does not waive required workflow, verification or approval.

**Plan:**
Add the following paragraph immediately after the title. Keep the existing responsibility introduction. Delete only the overlapping simplification-tracking bullet and premature-gates sentence; shared guidance retains concrete simplification follow-up obligations.
Primary objective: keep product work moving with the simplest code and process that meet current requirements. Treat money, tokens and elapsed time as constraints. Target only what is needed and reuse valid evidence. Prefer reuse and deletion over added machinery. Remove needless code, agents, handoffs, approval loops, repeated status updates and bookkeeping. Preserve correctness, safety, privacy and required verification. Do not make avoidable work a prerequisite.

Bounded follow-up: append “Do not call a demo release finished until its authorized local rollout is verified, including served assets and preserved saved work, or report the specific blocker.” to the existing delivery bullet. Reuse the accepted objective/deduplication reviews; review only this new sentence.

Delivery correction: add “Carry requested repository edits through verification, commit and push within existing authority. Do not wait for a separate push reminder.” after the opening sentence of the same delivery bullet. Reuse accepted unchanged guidance reviews. Review only this delta, then publish the verified role and existing release-completion notes.

**Verification plan:**
Requested objective and deduplication → inspect the exact diff against the supplied paragraph.
Safeguards and responsibilities → independent bounded plan/result review against existing shared guidance.
Scope and formatting → changed-path, local-link, whitespace and workflow checks.

**Plan review:**
Agent technical review: independent read-only reviewer gives PLAN GO for the exact paragraph and two deletions. Safeguards, duties and shared guidance remain intact; no new process gate is introduced.

Follow-up delta PLAN GO: the independent reviewer approves the exact release-finish sentence. It makes the existing shared delivery obligation explicit. “Authorized” preserves permission boundaries; reporting a blocker is honest completion reporting, not a new gate.

Publication delta PLAN GO: the independent reviewer approves the exact verification/commit/push wording. “Within existing authority” preserves local-only requests and other authorization boundaries. No new gate or global policy is introduced.

**Approvals:**
Owner requested this role improvement. No separate high-risk approval applies.

The owner's later delivery correction requires commit/push as part of finishing requested changes. This covers the verified role improvement and existing release-completion notes, not unrelated work or environment changes.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Added the requested primary objective and removed exactly the two overlapping passages. Existing responsibilities and shared guidance remain unchanged. The updated role applies to this session; the repository edit remains uncommitted.

## Result review

Agent technical review: independent read-only PLAN GO and RESULT GO. The reviewer confirms that safeguards, duties, required verification and authority remain intact, with no new process gate.

Reviewed revision: base `2900a568ed9de93b0b4126da208a4d216b62e325` plus the role edit, SHA-256 `50e9fcdf8cbcc1c435206caf602e22a039a7276dccb0d3d1840ef0875ca88090`.

Verification adequacy: exact reviewed-edit comparison, local links and whitespace pass. Runtime tests are unnecessary for this prose-only change. Only the role file and required task record changed.

Skill feedback trigger 2 dropped: this is a requested repository-local role clarification, not an Agent Workflow defect.

## Release-finish correction

The owner identified the omitted local rollout. Added the single reviewed sentence to the existing delivery bullet: do not call a demo release finished until its authorized local rollout is verified, including served assets and preserved saved work, or report the specific blocker. No authority or shared-policy change is introduced.

Independent delta PLAN GO and RESULT GO confirm the exact sentence and preserved permission boundaries. Prior primary-objective/deduplication review remains valid. Updated role SHA-256: `84a259aa83cb64de571c4b787d1e1fe55c1317ca6425687a0f726cc18a797044`. The role and record remain uncommitted.

Final verification passes the exact objective/deletions/finish-sentence comparison, local links, privacy and whitespace. Fresh shadow Redline is GRAY for the role path with no boundary violation. Both this record and the local-delivery record pass the workflow checker.

## Commit and push follow-through

The owner corrected the repeated need for a separate push reminder. The delivery bullet now requires verification, commit and push for requested repository edits within existing authority. Explicit local-only requests and environment boundaries remain unchanged.

Independent delta PLAN GO and RESULT GO confirm the exact two sentences and preserved prior guidance. Final role SHA-256: `a0ae6b462c2d3804265fb7b9966052d1a372f6065f19f7ea6f74f3a3b011a286`. Exact-diff, local-link, privacy and whitespace checks pass. No runtime tests are needed for this prose-only change.

This record accompanies publication of the verified role improvement and the existing local-release completion notes. Earlier “uncommitted” statements describe the edit/review phases, not an instruction to leave the completed changes local. Application source, saved data and installed tools are not changed by this publication.
