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

**Plan review:** Agent technical review: independent clean-context review under Plan review below passes the smallest existing-limit correction and boundary verification. The manager verified actual compiler/Java/Node reproduction at unchanged source 9b4ae91; implementation is released under the reviewed plan.

**Approvals:** Existing authorization covers repository bug fixes and verification. No separate human approval is required at this risk level.

**Exceptions:** —

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Source inspection and executable reproduction confirm the omitted generic condition. Independent technical plan review passes and the manager releases isolated implementation. The defect fails closed at runtime; no current demo impact or completed fix is claimed.

The developer completed frozen revision 878e6c9c23843d6b96a69e21af952f02029f463f. The manager accepted independent result review and integrated the four exact reviewed file blobs on main. Hosted delivery verification and recoverable task cleanup remain before closing the feature.

## Verification and result review

Agent technical review: independent clean-context non-implementer PASS for 878e6c9 against 9b4ae91, with no blockers. Reviewed revision: 878e6c9c23843d6b96a69e21af952f02029f463f. Verification adequacy: sufficient for the bounded compiler parity fix; source/evidence hashes and actual before/after failure were checked without unnecessary reruns.

The new compiler regression fails before the fix and passes afterward. Full Maven verification passes 233 tests with no failures/skips in 23.006 seconds; Node regression/coverage passes all 391 tests in 2.884 seconds. Shared/packaged conformance preserves 61 legacy read cases, 25 legacy effect scenarios, nine generic read and 19 generic effect scenarios, with 16 fresh compiler runs and eight packaged host batches retaining exact golden bytes. Changed compiler line 147 has all four branches covered. Three additive boundary tests preserve legacy behavior and verify zero runtime dispatch on invalid models. All 25 frozen evidence files and four source hashes match; no fixtures, schemas, runtime production logic, dependencies or unrelated behavior changed.

## Plan review

Independent non-implementer review inspected the record, policy/workflow, compiler validation and boundary/reference tests, both runtime validators/tests and read/2 schema at core 6636171. Verdict: PASS, Elevated/Simple. Apply the existing limit when effectProfile or generic is true; preserve read/1's intentional larger error lists, existing effect limits and runtime diagnostic differences. Boundary tests must isolate the error ceiling using valid bound-model identities and reject 17 without artifact/digest. Existing reference-byte tests protect valid artifacts; no fixture regeneration is warranted. The unused WRITE definition in the read/2 catalog supplies a small additional invalid-definition case. No tests or edits were performed by the reviewer; executable reproduction is still pending.

## Reproduction evidence

At unchanged product source 9b4ae911a2453a9f22a315c4f1e1164554c4d18a, isolated probes invoke actual KeirCompiler.compile, public Java KeirRuntime.bindModel and Node model validation. For both the used READ definition and unused WRITE catalog definition, 16 declared errors compile and both runtimes accept. With 17, the compiler still emits a 1,799-byte artifact; Java rejects BUDGET_EXCEEDED and Node rejects ARTIFACT_INVALID before dispatch. The unchanged reference model/source still reproduces its exact golden artifact. Maven package passes in 21.879 seconds; all 18 existing DcmTest cases pass, preserving effect and legacy read/1 behavior. The manager inspected the source identity, clean tracked state and actual logs; no rerun or fixture change was needed. Detailed private probes remain outside tracked content.
