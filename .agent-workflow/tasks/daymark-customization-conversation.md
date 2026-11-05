# Keep one visible conversation per customization

<!-- agent-workflow:start -->
**Outcome:** Opening a customization shows one continuous conversation with its saved messages and a place to continue.

**Target:** keir-demo-app

**Scope:** Use the existing owner-scoped conversation links and message store to render one paginated Studio transcript and renew internal authoring runs when required.

**Constraints:** Preserve existing history, provider receipts, scope and authority fences, exact draft approval and activation, recovery, settings and accounting. No data rewrite or paid call on open.

**Completion criteria:** An upgraded item opens its previous messages without a provider request, continues in place on explicit send and shows both sets of messages after reopen. Wrong-owner access and stale approval or operation reuse fail.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Opening a customization shows one continuous conversation with its saved messages and a place to continue.","scope":"Use the existing owner-scoped conversation links and message store to render one paginated Studio transcript and renew internal authoring runs when required.","constraints":"Preserve existing history, provider receipts, scope and authority fences, exact draft approval and activation, recovery, settings and accounting. No data rewrite or paid call on open.","completion_criteria":"An upgraded item opens its previous messages without a provider request, continues in place on explicit send and shows both sets of messages after reopen. Wrong-owner access and stale approval or operation reuse fail."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Transcript selection and provider context cross the service and Studio boundaries. The current draft, exact recovery receipts and owner scope must stay authoritative.

**Discovery:** The authoring list selects only the current model. Studio also requires an exact draft match. An earlier linked discussion remains stored but the default item path opens without it.

**Material assumptions:** The existing owner and customization links can support bounded pagination and stable transcript identity without changing the schema. Verify the query plan and row ownership before extending the read path.

**Plan:** Read one owner and item transcript from existing linked messages with deterministic keyset pagination. Keep the newest compatible internal run as the write target. On explicit send, a fresh run may carry bounded prior user text as historical context and use the exact current draft. Preserve raw input identity, exact recovery and current review predicates. Remove the routine conversation picker and keep linked navigation on the customization.

**Verification plan:** Reopen and continuation → browser journey with an earlier linked discussion and an explicit new send.
History and owner scope → paginated API cases with cross-item, cross-owner and deleted anchor checks.
Approval and recovery → existing exact lifecycle, lost-response and settings regressions.
Provider context → offline checks on both adapters for bounded prior user data and unchanged request identity.

**Plan review:** Agent technical review: independent static review at b0e5e429 accepted the plan with stable segment ordering, latest compatible target only, exact authority recheck, bounded historical data and explicit recovery after ambiguous responses.

**Approvals:** Not required at this risk level.

**State:** Ready to implement
<!-- agent-workflow:end -->
