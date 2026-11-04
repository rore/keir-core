# Align declared-error validation across compiler and runtimes

Owning feature: [Declared-error limit parity](../../roadmap/features/declared-error-limit-parity.md).

<!-- agent-workflow:start -->
**Outcome:** read/2 compilation rejects capability error catalogs that exceed the existing runtime limit.

**Target:** keir-core

**Scope:** Compiler DCM error-count validation and focused compiler, Java and Node boundary regression coverage.

**Constraints:** Preserve read/1 compatibility, effect-profile limits, runtime admission, diagnostics privacy and valid artifact bytes. No schema/profile/version widening, demo changes, installed tooling, global configuration or Git identity/history changes.

**Completion criteria:** read/2 accepts 16 declared errors and rejects 17 during compilation; Java and Node agree at the boundary; read/1 and effect compatibility tests remain valid.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"read/2 compilation rejects capability error catalogs that exceed the existing runtime limit.","scope":"Compiler DCM error-count validation and focused compiler, Java and Node boundary regression coverage.","constraints":"Preserve read/1 compatibility, effect-profile limits, runtime admission, diagnostics privacy and valid artifact bytes. No schema/profile/version widening, demo changes, installed tooling, global configuration or Git identity/history changes.","completion_criteria":"read/2 accepts 16 declared errors and rejects 17 during compilation; Java and Node agree at the boundary; read/1 and effect compatibility tests remain valid."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:declared-error-limit-parity"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Simple

**Reason:** Compiler/test paths have the unclassified Gray floor. The change tightens one omitted existing profile check without changing runtime authority or the accepted limit.

**Discovery:** Dcm.java passes a 16-element limit only when effectProfile is true. Its generic read/2 path uses the legacy unbounded read limit. Java Artifact.java and Node validation.js enforce 16 for effect or generic profiles. The read/2 schema conformance already checks this ceiling. DcmTest explicitly preserves read/1 acceptance of 17 errors, so applying 16 universally would break compatibility.

**Material assumptions:** This is an omitted generic-profile condition, not a new limit decision. Verify executable boundary behavior and existing schema/runtime contracts before editing; return to design if they contradict this interpretation.

**Plan:** Reproduce read/2 compiler acceptance of 17 errors and existing runtime rejection in isolated tests. Extend the existing Dcm error-list limit condition to generic profiles. Add focused compiler and both-runtime boundary regressions using existing fixtures/helpers; assert accepted 16, rejected 17, no emitted artifact on compilation failure and preserved read/1/effect behavior. Cover invalid unused catalog definitions when existing helpers make this a small additional case. Preserve current runtime validation. Obtain clean-context plan review before edits and independent result review after verification. Manager owns record/roadmap; assigned core developer owns isolated implementation. Stop if a different profile/limit contract or broader defect is discovered.

**Verification plan:** Compiler boundary regression fails before the fix and passes afterward; diagnostics identify the oversized capability errors without an artifact. Both runtimes admit a valid boundary model and reject an oversized one through existing preparation/loading APIs. Existing compiler, Java/Node validation and affected shared structural conformance preserve read/1/effect and valid reference artifacts. Run affected fast checks once; broaden only for changes/failures. Inspect public diff/metadata before integration and recoverably clean the task checkout afterward.

**Plan review:** Pending clean-context technical review; reproduction/discovery only before implementation release.

**Approvals:** Existing authorization covers repository bug fixes and verification. No separate human approval is required at this risk level.

**Exceptions:** —

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

Source inspection confirms the omitted generic condition. An executable reproduction and independent technical plan review remain pending. The defect fails closed at runtime; no current demo impact or completed fix is claimed.
