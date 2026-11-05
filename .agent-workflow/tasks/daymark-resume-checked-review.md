# Resume review of a checked Daymark draft

<!-- agent-workflow:start -->
**Outcome:** A person can ask Studio to make an already checked customization work and get back to its saved review in the same conversation.

**Target:** keir-demo-app

**Scope:** Repair the resumed `offer_review` path and add a regression for creating, discussing and reopening a checked draft. Keep the existing review UI usable while in-chat visuals are designed separately.

**Constraints:** Use only the same owner's current revision and exact saved suite evidence. Preserve stale and ineligible evidence handling, isolated tests, and explicit approval and activation. A chat message cannot authorize either action.

**Completion criteria:** A follow-up message after review reopens matching saved evidence without recompiling or running examples again. Missing, foreign or stale evidence cannot be presented as current approval evidence. The user sees an actionable next step and the draft and conversation remain intact.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A person can ask Studio to make an already checked customization work and get back to its saved review in the same conversation.","scope":"Repair the resumed offer_review path and add a regression for creating, discussing and reopening a checked draft. Keep the existing review UI usable while in-chat visuals are designed separately.","constraints":"Use only the same owner's current revision and exact saved suite evidence. Preserve stale and ineligible evidence handling, isolated tests, and explicit approval and activation. A chat message cannot authorize either action.","completion_criteria":"A follow-up message after review reopens matching saved evidence without recompiling or running examples again. Missing, foreign or stale evidence cannot be presented as current approval evidence. The user sees an actionable next step and the draft and conversation remain intact."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The review response can make exact evidence available for later approval. The host must verify ownership and evidence identity before showing that decision.

**Discovery:** The reported resumed turn ended with `HOST_RESULT_INVALID` after two successful provider calls and an unresolved second host tool call. The draft still matched the conversation and retained passing evidence. The service clears the per-turn evidence handle, while `offer_review` requires it before asking the host to read evidence. Initial live qualification ended at the first review and missed this path.

**Material assumptions:** The current lifecycle can resolve the exact draft's saved evidence without starting another simulation. Verify that boundary and return to planning if it requires persisted shape or authority changes.

**Plan:** Keep resetting provider-native continuation. Reuse the lifecycle's owner-bound current draft and exact suite evidence to resolve review on a subsequent turn. Retain exact in-turn handle checks for freshly simulated evidence. Treat a missing review as a recoverable result with an actionable next step. Keep the assistant's description separate from host proof and retain all approval and activation checks.

**Verification plan:** Reproduce the user journey with a controlled provider and real compilation and simulation. Continue the same conversation through an explanation and a request to proceed. Check the same saved revision and evidence are presented, with no new compile, simulation, approval, activation or task write. Include a stale or mismatched proof and exercise the existing browser review entry. Run a scoped live provider walkthrough on isolated data before preview rollout if the provider is available.

**Plan review:** Independent technical review requested before implementation.

**Approvals:** No additional approval required for isolated development.

**State:** Ready to implement
<!-- agent-workflow:end -->

## Progress

Discovery is complete. Implementation awaits independent plan review. The separate in-chat design remains in discovery.
