# Bring saved behavior and example checks into the Daymark conversation

Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md).
Design reference: [Review inside the conversation](../../docs/internal/studio-inline-review-proposal.md).

<!-- agent-workflow:start -->
**Outcome:** A business user can understand the saved customization, its checked examples and the next decision inside the real Daymark conversation.

**Target:** keir-demo-app

**Scope:** Integrate the reviewed inline behavior and example review into the working Studio. Preserve access to history and secondary details and align assistant wording and focused tests.

**Constraints:** Use existing owner-bound evidence and approval and activation handlers. Preserve the composer, typed text, saved conversations, stale-state handling and the version in use. Keep compiler, runtime, native task behavior, provider settings and the normal preview data outside isolated implementation.

**Completion criteria:** Desktop and narrow-screen users can read what will happen, see expected and observed results and any unchecked coverage, reopen the same saved evidence, approve and separately turn on the exact revision. Failed, unavailable, stale and historical results cannot offer an invalid decision.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A business user can understand the saved customization, its checked examples and the next decision inside the real Daymark conversation.","scope":"Integrate the reviewed inline behavior and example review into the working Studio. Preserve access to history and secondary details and align assistant wording and focused tests.","constraints":"Use existing owner-bound evidence and approval and activation handlers. Preserve the composer, typed text, saved conversations, stale-state handling and the version in use. Keep compiler, runtime, native task behavior, provider settings and the normal preview data outside isolated implementation.","completion_criteria":"Desktop and narrow-screen users can read what will happen, see expected and observed results and any unchecked coverage, reopen the same saved evidence, approve and separately turn on the exact revision. Failed, unavailable, stale and historical results cannot offer an invalid decision."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Studio displays evidence for changes that can perform native writes after explicit activation. Moving decisions must preserve the exact evidence and saved state used by the host.

**Discovery:** Daymark base `36ade85` uses the split preview for current decisions. The separate rendered fixture at `28aa946` completed two design iterations. The live code already loads the exact saved review, compares evidence and selection, and sends separate explicit approval and activation requests.

**Material assumptions:** The current evidence and saved source projections can support a truthful behavior summary and example rows. When metadata is incomplete, explain the gap and retain exact evidence. Do not infer unchecked conditions or successful behavior from assistant prose.

**Plan:** Reuse the current saved-review controller and authorization guards. Render one current review inside the conversation with separate behavior and checked-example surfaces. Route explicit review links and assistant offers to the same saved snapshot without duplicating controls, interrupting typing or opening a split view. Keep history and versions in use available as read-only inspection. Use the existing task-shaped evidence and clear states for approved, off, on, failed and unchecked. Align the workspace and speaker labels with the proposal. Return to planning if this requires new authority or a new persistence model.

**Verification plan:**
- Check the real application at desktop and narrow widths. Walk explain, example checks, confusion, reopen, approve, turn on, turn off and later edits. Check focus, scroll, composer, readable coverage and no horizontal overflow.
- Exercise stale, missing, failed and historical evidence; another active version; interrupted and duplicate actions. Assert that chat and opening evidence do not approve, activate or rerun checks.
- Preserve earlier results and typed text through reload and later conversation turns. Use independent technical and rendered reviews, revise concrete findings and recheck the working app before normal preview delivery.

**Plan review:** Agent technical review: [Client plan and exact-proof activation review](#client-plan-and-exact-proof-activation-review). Both reviewed plans passed.

**Approvals:** Approved to integrate the reviewed in-conversation design in Daymark. Existing approval and activation requirements remain in force for each customization.

**State:** Ready to implement
<!-- agent-workflow:end -->

## Delivery

The user approved moving the reviewed design into the working demo. The Daymark developer accepted implementation ownership and verified the clean base at `36ade85`. The split preview remains the deployed baseline.

## Client plan and exact-proof activation review

The independent reviewer checked the client plan at SHA-256 `876bab48fa3fbbaad831901799a94030950280e10f62ac6e8beb8c81a43b8ca8`. It passes with a fresh owner-bound state read after loading the saved evidence and source. That read must be independent of active-version comparison. Scroll and focus intent are separate from decision freshness.

The current host can replace passing approval evidence for the same revision without changing the selection generation. The developer reproduced the race with two passing proofs. The reviewed addendum at SHA-256 `b6a69b61411eb0caff97fc52dd9a60f7f8c291b32289591e7840064e7fdcd111` requires Studio to send its reviewed evidence ID on activation. Daymark compares it to the approved ID inside the existing transaction and rejects a mismatch before selection or history writes. Preserve authorization, revision, draft, suite, generation and uncertain-response recovery. Test an intervening approval, explicit turn-on with the matching proof, and a committed response lost in transit. Deploy matching web and server assets together because older cached clients omit the new required field.
