# Resume review of a checked Daymark draft

<!-- agent-workflow:start -->
**Outcome:** A person can ask Studio to make an already checked customization work and get back to its saved review in the same conversation.

**Target:** keir-demo-app

**Scope:** Repair the resumed offer_review path and add a regression for creating, discussing and reopening a checked draft. Keep the existing review UI usable while in-chat visuals are designed separately.

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

**Verification plan:**
- Reopen the same saved evidence without recompiling or running examples again → Reproduce the browser journey with synthetic transport and the real compiler and simulator. Check exact evidence identity and unchanged operation counts after explanation and proceed.
- Keep missing, foreign and stale evidence outside approval → Test the service and agent boundaries with absent, mismatched and superseded proof and replacement during the read.
- Keep an actionable next step and preserve draft and conversation → Open the exact saved proof in the browser and confirm approval, selection and task state are unchanged. Run a scoped live provider walkthrough on isolated data before preview rollout if the provider is available.

**Plan review:** Agent technical review: [Independent architecture review](#independent-architecture-review).

**Approvals:** No additional approval required for isolated development.

**State:** Ready for review
<!-- agent-workflow:end -->

## Progress

Independent plan review is complete. The regression covers the same conversation through review, explanation and a request to proceed. Exercise missing, failed, foreign and stale proof, including replacement during the evidence read. The separate in-chat design remains in discovery.

The developer reproduced the browser failure on `8345a174` using synthetic provider transport and the real compiler and simulator. Initial review and an explanation succeeded. The subsequent request to proceed reached `offer_review` and ended with `HOST_RESULT_INVALID`. The fix must pass that same browser journey before the running preview is updated.

## Independent architecture review

The architect accepted the plan on Daymark `8345a174`. Keep the exact checks for an in-turn proof. Resolve saved proof only when that handle is absent, from the owner and extension's current draft and latest evidence. Check the authored suite and current dependency identity through the lifecycle. Keep failed checks visible with approval disabled and return missing or stale evidence as a recoverable result. Recheck current draft identity after the read.

## Candidate evidence

Daymark `d4e4969` passed 100 focused tests and 24 Chromium tests. The browser regression reopens the exact saved evidence after an explanation and a new request in the same conversation. No examples rerun and no native writes, approval or activation occur on reopen. Independent result review accepted the final candidate after checking neutral host copy and a regression where newer mismatched evidence must never fall back to older passing proof. The production bundle passed. Integration `26a6afb` adds only the intervening public documentation changes to the reviewed candidate.

## Result review

Agent technical review: [Saved review and evidence checks](#saved-review-and-evidence-checks).

Reviewed revision: `d4e49698e72633d2443e02a943a0de9b67f36ae0`.

Verification adequacy: Pass for the reported same-conversation failure and the saved-evidence trust boundary.

### Saved review and evidence checks

The independent reviewer accepted the final neutral host message and the regression against falling back to older evidence. The manager checked the candidate tree, scoped diff, build hashes, and results of the browser replay. The integrated revision differs only by three reader documentation files.

## Preview and live-provider check

Daymark `26a6afb` is on main and the existing preview serves the reviewed build. The saved database and assistant settings matched the rollback copy byte for byte after startup. No migration was needed.

The configured OpenAI provider completed drafting and simulation on synthetic tasks, then replied to the explanation request in the same conversation. Its next response timed out after catalog discovery, before invoking the review tool. The saved draft and examples remained unchanged and nothing was approved or turned on. The controlled browser replay verifies the exact defect. Completion of the same path with the live provider is still open.
