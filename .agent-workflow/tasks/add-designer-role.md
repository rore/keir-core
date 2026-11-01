# Add the designer position

Canonical task: `agent-workflow:add-designer-role`. User requested an additional UX/UI designer using Astra high, then clarified that the result must feel like a proper application rather than default AI-generated UI. This is team setup; the first Studio assessment is read-only and separately scoped.

<!-- agent-workflow:start -->
**Outcome:** Keir has a concise designer role using Astra high, with clear UX/UI ownership and a bounded first design review.
**Target:** keir-core
**Scope:** Designer role guide, exact role mapping, team index and manager/architect collaboration guidance; Studio design ownership and this task record.
**Constraints:** Preserve existing agent settings, runtime/approval authority and shared guidance. No global installation, alias takeover, separate chat creation, production UI implementation or automatic design approval. Use native delegation for the first bounded designer task.
**Completion criteria:** Role mapping resolves uniquely to a readable concise guide; team docs state Astra high and design/architecture/development boundaries; a native Astra-high designer accepts the bounded Studio review; no Relay registration or separate chat is claimed without evidence.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"Keir has a concise designer role using Astra high, with clear UX/UI ownership and a bounded first design review.","scope":"Designer role guide, exact role mapping, team index and manager/architect collaboration guidance; Studio design ownership and this task record.","constraints":"Preserve existing agent settings, runtime/approval authority and shared guidance. No global installation, alias takeover, separate chat creation, production UI implementation or automatic design approval. Use native delegation for the first bounded designer task.","completion_criteria":"Role mapping resolves uniquely to a readable concise guide; team docs state Astra high and design/architecture/development boundaries; a native Astra-high designer accepts the bounded Studio review; no Relay registration or separate chat is claimed without evidence."}
**Relationships:**
{"schema_version":1,"sources":[],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Simple
**Reason:** New persistent agent guidance and mapping have a gray risk floor; no runtime, security policy or human authority changes.
**Discovery:** Team docs lack a designer and still describe demo work as deferred although the native slice is delivered. Existing persistent-agent-roles requires exact mappings before using names; mapping alone does not register an agent or grant authority. Desktop chat creation is not part of this user request, so use native delegation and disclose its scope.
**Material assumptions:** Astra high is available through native delegation as explicitly requested; if unavailable, report instead of substituting. A proposed Relay name may remain unregistered; never take over another session.
**Plan:** Add roles/designer.md (about 150 words) with product UX/UI ownership, user-aligned interaction model, professional task-focused app design, state/recovery/accessibility and actual prototype review. Add the exact @keir-designer mapping and team row; remove obsolete demo-developer deferral. Manager delegates UX to designer; architect remains responsible for technical contracts. Update Studio feature ownership without marking design approved. Obtain independent review of this narrow governance plan before edits, validate the exact mapping/guide paths and absence of broader authority, then onboard one native Astra-high designer for a bounded read-only assessment of current Studio artifacts. Keep role/session adoption truthful and preserve user-selected settings.
**Verification plan:**
- Mapping integrity -> reject duplicate YAML keys, missing/escaping/non-Markdown targets; read back the exact designer mapping.
- Responsibility/model scope -> independent review of the plan and resulting guide/index/collaboration lines against the user request and unchanged authority.
- Actual delegation -> native spawn configured gpt-6-astra/high, acceptance/read-only design report, no claim of separate chat/Relay identity.
- Delivery -> fresh complete-path Redline and explicit Work Record validation, privacy/links/whitespace checks, normal direct-main commit/push.
**Plan review:** Agent technical review: independent clean-context reviewer approved the proportional plan, with explicit readiness after plan approval and no implied Studio approval. See Plan review below.
**Approvals:** Not required at this risk level. User explicitly requested the designer and Astra high; existing sessions must not be reconfigured.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Context/baseline recorded before role edits. Base core `dc867dd`; existing team/session settings remain unchanged.

## Plan review

Agent technical review: independent non-implementer approved guide/mapping/team/collaboration scope. Transition to Ready to implement now that review is complete; Studio ownership is not design acceptance. Existing globally available role guidance is sufficient; no repo-local skill installation or environment change is required.

## Evidence

Pending role setup and first designer acceptance. No runtime or UX approval is claimed.

## Result review

Pending narrow guide/mapping review and verification.
