# Use the packaged Keir guide in Daymark

<!-- agent-workflow:start -->
**Outcome:** Daymark supplies the verified Keir authoring guide to its assistant alongside the application's own rules and scoped tools.

**Target:** keir-demo-app

**Scope:** Vendor the verified second edition guide from the Core authoring companion and use it in the existing authoring request assembly.

**Constraints:** Verify the archive against its trusted pin at vendoring. Use the exact guide bytes. Keep the Daymark DCM, tool definitions and approval authority separate. Preserve native tasks and saved work if authoring cannot load the guide. Leave the review design and runtime packages outside this change.

**Completion criteria:** The actual provider request contains the pinned guide and the Daymark instructions. A missing or changed guide prevents authoring before provider dispatch. Same-project authoring and saved review continuity pass without changing user authority.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Daymark supplies the verified Keir authoring guide to its assistant alongside the application's own rules and scoped tools.","scope":"Vendor the verified second edition guide from the Core authoring companion and use it in the existing authoring request assembly.","constraints":"Verify the archive against its trusted pin at vendoring. Use the exact guide bytes. Keep the Daymark DCM, tool definitions and approval authority separate. Preserve native tasks and saved work if authoring cannot load the guide. Leave the review design and runtime packages outside this change.","completion_criteria":"The actual provider request contains the pinned guide and the Daymark instructions. A missing or changed guide prevents authoring before provider dispatch. Same-project authoring and saved review continuity pass without changing user authority."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The guide affects proposed rules that may later write through Daymark. Authorization and review decisions stay with existing host checks.

**Discovery:** Core v2 provides a guide with exact source and compatibility pins. Daymark currently assembles its prompt in the authoring agent and exposes its DCM and capabilities through scoped discovery.

**Material assumptions:** The existing authoring configuration and request assembly can use a verified local guide while keeping ordinary tasks and review available on guide failure.

**Plan:** Verify the archive and vendor only the exact guide bytes with a narrow checkout rule. Check the bounded local file against the trusted digest before decoding and use one verified instruction snapshot per admitted turn. Reuse that check for readiness and submission. Preserve existing receipt replay before the guide check. Keep Daymark policy, DCM discovery, tools, approval and activation authority. In Studio, handle only the explicit pre-receipt `503 AUTHORING_GUIDE_UNAVAILABLE` as a rejected submission. Clear the pending submission, keep the typed text and saved review available, and refresh authoring availability. Ambiguous responses still use receipt recovery.

**Verification plan:**
- Exact guide and scoped Daymark context → Capture both provider payloads after discovery. Check the guide occurs once, Daymark policy and scoped tools remain present, and serialized requests stay within the current size limit.
- Missing or changed guide → Check startup, submission after a successful readiness read, and exact duplicate receipt replay. Assert no new admission, dispatch, budget or persistence changes on rejection. In the browser, verify that text and saved review remain usable and that the explicit rejection skips receipt recovery.
- Same-project rule and saved review → Run the existing controlled compiler, simulation and browser journeys and check unchanged approval and activation boundaries.

**Plan review:** Agent technical review: [Reviewed implementation plan](#reviewed-implementation-plan).

**Approvals:** Isolated implementation and verification are authorized within scope.

**State:** Ready for review
<!-- agent-workflow:end -->

## Reviewed implementation plan

The independent review passed against Daymark base `26a6afb60e37da50ae435198249ebb25a2218e36` and plan SHA-256 `79e3f4e4fdb145a3b2501b0dadbee824c8ab233b44d194fb95eeb7fe8e085dbd`. Review found that a guide failure after a successful readiness check would enter receipt recovery and could lock the saved review controls. The revised plan handles the explicit rejection before any receipt, preserves ambiguous response recovery, and adds a browser regression. Core companion v2 verification passed against its trusted archive digest.

## Result review and delivery

Independent result review passed for `3577695d82691bf6abf07f99b021c37e1e518b02` after a delayed availability refresh was fixed so it does not keep review controls locked. A browser regression keeps the refresh pending while the user reviews, approves, and separately turns on the saved revision. Manager checked the reviewed diff and confirmed the follow-up at `36ade85eeec5cc905c049f05aa662009470ecafc` adds only release provenance. Daymark main was fast-forwarded to that revision.

The vendor file matches the verified [authoring companion release](https://github.com/rore/keir-core/releases/tag/authoring-v2). Both provider request captures include the guide and scoped host context and stay under the existing request limit. The qualified suites recorded 361 tests passed, 13 skipped, 11 browser checks passed, and a successful build. Same-project authoring, exact receipt replay, and independent approval and activation passed. Tests used recorded provider responses and isolated data.
