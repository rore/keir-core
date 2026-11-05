# Align public DCM binding in Java and Node

Owning feature: [Public package release](../../roadmap/features/public-package-release.md).

<!-- agent-workflow:start -->
**Outcome:** Java and Node agree on the public DCM binding boundary for a UTF-8 BOM and declared capability errors.

**Target:** keir-core

**Scope:** Correct Node public model parsing and the declared error budget diagnostic. Verify both runtimes through bindModel with one shared set of exact byte cases.

**Constraints:** Preserve valid model bytes, compiler behavior, host authority, versioned formats and the frozen scenario corpus. Keep Java production behavior and avoid changing diagnostic precedence for inputs with multiple defects.

**Completion criteria:** Both runtimes bind the baseline and 16 distinct errors, reject a BOM with ADMISSION_INVALID /binding/dcm, and reject 17 distinct errors with BUDGET_EXCEEDED /binding/dcm. No binding case dispatches. Rebuild and verify the exact release packages and coordinate new pins with Daymark.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Java and Node agree on the public DCM binding boundary for a UTF-8 BOM and declared capability errors.","scope":"Correct Node public model parsing and the declared error budget diagnostic. Verify both runtimes through bindModel with one shared set of exact byte cases.","constraints":"Preserve valid model bytes, compiler behavior, host authority, versioned formats and the frozen scenario corpus. Keep Java production behavior and avoid changing diagnostic precedence for inputs with multiple defects.","completion_criteria":"Both runtimes bind the baseline and 16 distinct errors, reject a BOM with ADMISSION_INVALID /binding/dcm, and reject 17 distinct errors with BUDGET_EXCEEDED /binding/dcm. No binding case dispatches. Rebuild and verify the exact release packages and coordinate new pins with Daymark."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Simple

**Reason:** Public model admission is a runtime trust boundary. The change aligns two checks with the existing model contract.

**Discovery:** On Core `db0a4a3`, a public Node binding probe accepted the baseline, a BOM-prefixed baseline and 16 distinct errors. It rejected 17 errors with `ADMISSION_INVALID` at `/binding/dcm`. Java source rejects the BOM and reports the over-budget list as `BUDGET_EXCEEDED`. The current compiler already enforces the 16-error ceiling.

**Material assumptions:** The cases differ from a valid model only by the stated byte prefix or distinct valid error count. Java's current public behavior is the target.

**Plan:** Keep the BOM visible to the Node model parser or reject it before decoding. Report the existing declared-error ceiling as a budget failure. Add one shared binding matrix used by both runtime test suites. Keep fixture ownership with the Java and shared-conformance owner and Node production and test ownership with the Node owner.

**Verification plan:** Reproduce the two Node differences before the fix. Assert exact public outcomes and zero dispatch in both runtimes using the same cases. Verify the ordinary runtime suites and isolated installed packages before replacing draft bytes or qualifying Daymark.

**Plan review:** Manager reviewed the exact public Node path and Java diagnostic mapping at `db0a4a3` independently of implementation. The bounded plan preserves the admitted limit and Java behavior. Confirm against executable Java binding before acceptance.

**Approvals:** The user authorized the public-format cutover and resolution of release blockers. The manager authorized this scoped parity fix and its isolated verification.

**State:** Ready for review
<!-- agent-workflow:end -->

## Packaged Java baseline

The extracted Java runtime from the Core 0.1.0 draft passed all four public binding cases. The baseline and 16 declared errors bound successfully. The BOM returned `ADMISSION_INVALID` and 17 declared errors returned `BUDGET_EXCEEDED`, both at `/binding/dcm`. The registered adapters fail the probe if binding dispatches either one.

## Combined verification

The independently reviewed Java matrix at `656ab97` and the Node fix at `2f0f5ba` share the exact four model byte cases. All 70 Java runtime tests and all 50 Node runtime tests passed, including loopback. The seven installed Node teaching tests passed, and the installed files match the rebuilt tarball byte for byte. A separate install of the corrected Node 0.1.0 package passed all four public binding cases with zero dispatch. The frozen conformance fixtures and Java production code were unchanged. [Integration PR](https://github.com/rore/keir-core/pull/5)
