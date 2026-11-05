# Clarify Studio capability and source guidance

<!-- agent-workflow:start -->
**Outcome:** Studio accurately describes current task customization and can recover from the observed malformed source.

**Target:** keir-demo-app

**Scope:** Change only the capability discovery and source formatting guidance in the existing Daymark authoring prompt.

**Constraints:** Preserve the host catalog, source validation, approval and activation boundaries, existing conversations and demo data.

**Completion criteria:** Describe a completion-driven single creation in accessible projects. Reproduce the rejected source and compile the same intended rule with valid source. Run focused authoring and compiler regressions.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Studio accurately describes current task customization and can recover from the observed malformed source.","scope":"Change only the capability discovery and source formatting guidance in the existing Daymark authoring prompt.","constraints":"Preserve the host catalog, source validation, approval and activation boundaries, existing conversations and demo data.","completion_criteria":"Describe a completion-driven single creation in accessible projects. Reproduce the rejected source and compile the same intended rule with valid source. Run focused authoring and compiler regressions."}

**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Simple

**Reason:** The shared path policy classifies the prompt source as gray. The change alters guidance while existing host checks still control execution.

**Discovery:** The current catalog exposes one task completion trigger, a scoped task read and one task creation. Both saved compile attempts for the accepted fixed-title alternative omitted the closing JSON quote in the return expression. The compiler reported SYNTAX_INVALID at /source in both attempts.

**Material assumptions:** The fixed-title rule fits the current catalog. Check it with the packaged compiler and revisit the scope if valid source is rejected.

**Plan:** Describe the trigger and one-task outcome in plain language. Present discovered projects as choices, and describe example checks briefly. Give the assistant precise source quoting and syntax recovery guidance.

**Verification plan:** Source failure and recovery → compile the captured malformed and corrected shapes offline.
Existing authoring and compiler behavior → run the focused suites.
Capability and authority claims → review the exact prompt diff against the host contract.

**Plan review:** Agent technical review: independent source review against the Daymark task model and authoring context accepted the capability description and recovery within existing compiler and tool boundaries.

**Approvals:** Not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation and verification

The prompt now presents the current customization in terms of a task completion, the project condition and at most one new task with a fixed title. It describes example checks briefly and explains how to recover a whole-source syntax rejection while keeping the rule and draft version.

Both captured attempts reproduced the same whole-source syntax diagnostic. Adding the missing closing quote compiled both, and block YAML with single-quoted expressions produced byte-identical artifacts. All 73 focused authoring and compiler tests passed against the isolated installed runtime.

## Result review

Agent technical review: Approved both prompt additions against the current host contract, authoring boundaries and exact source failure.

Reviewed revision: [e76d5f1](https://github.com/rore/keir-demo-app/commit/e76d5f1a40773a75e5044a7a920da2560704e52c).

Verification adequacy: Reproduction and artifact equality verify the source diagnosis and repair format. Provider adherence requires a live conversation.

The verified prompt is published and serves the existing preview. Database integrity, saved conversation count and assistant settings matched the pre-update checks.
