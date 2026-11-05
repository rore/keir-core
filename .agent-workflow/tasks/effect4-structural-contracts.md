# Publish structural schemas for effect/4

<!-- agent-workflow:start -->
**Outcome:** Authoring and integration tools can check decoded effect/4 source, DCM and artifact structure against versioned schemas.

**Target:** keir-core

**Scope:** Add three effect/4 schemas, minimal shared fixtures and focused checks, using the existing offline schema validator and native conformance APIs.

**Constraints:** Preserve native acceptance, artifact bytes, legacy schemas and runtime authority. Keep the existing pinned development dependency and CI commands. Schema validity grants no permission to prepare, activate or execute an artifact.

**Completion criteria:** Current compiler-produced documents validate structurally. Representative malformed documents fail schema checks and structural-pass residuals fail through the intended native boundary in Java and Node. The read/2 command remains compatible and CI runs both structural suites.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Authoring and integration tools can check decoded effect/4 source, DCM and artifact structure against versioned schemas.","scope":"Add three effect/4 schemas, minimal shared fixtures and focused checks, using the existing offline schema validator and native conformance APIs.","constraints":"Preserve native acceptance, artifact bytes, legacy schemas and runtime authority. Keep the existing pinned development dependency and CI commands. Schema validity grants no permission to prepare, activate or execute an artifact.","completion_criteria":"Current compiler-produced documents validate structurally. Representative malformed documents fail schema checks and structural-pass residuals fail through the intended native boundary in Java and Node. The read/2 command remains compatible and CI runs both structural suites."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:machine-readable-contracts"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Schema, fixture and verifier paths are Gray under Redline. Public structural guidance must agree with three native validators without implying semantic or authority checks.

**Discovery:** At `673ca5c9`, the effect/4 DCM and artifact have distinct identities, and source remains `keir/v0` with an externally selected profile. The Node and Java loaders both reject an undeclared handler but report different diagnostic paths. They also differ in the diagnostic code for more than 16 handlers.

**Material assumptions:** The existing offline Draft 2020-12 checker can run a second suite without another dependency. Reuse compiler-produced fixtures and native harnesses wherever they cover the intended boundary. Keep phase-specific diagnostics when acceptance agrees.

**Plan:** Add independent source, DCM and artifact schemas in `schemas/effect-4/` using versioned URNs, closed shapes and local bounds. Source remains `keir/v0` with external profile selection. Only CALL admits `when` (step ID) and `onError` (1–16 uppercase error-code keys), with source `{record}` and lowered `{type,fields}` handlers. Keep earlier Boolean BIND references, error membership and output compatibility native. The DCM requires `HOST_BOUND_SINGLE_WRITE`, `hostKey.source` of `requestKey`, and an `EVENT`/`AFTER_COMMIT`/`EVENT_REPORT` hook. `capabilityPolicy` and `effectGrant` are optional. An explicit grant is 0 or 1. Artifact format/profile are `keir-artifact/effect-4` and `keir-effect/4`, with `modelRef.format` `keir-dcm/effect-4` and descriptor recipe `keir-record-descriptors/1`. Pin `compiler` to `keir-compiler-java/0.1.0-SNAPSHOT` as both effect/4 loaders require. Pin the four current environment literals using the read/2 snapshot convention. Native preparation retains all compatibility checks. Keep raw parsing, byte and Unicode rules, model identity, checked CEL, dataflow, effect counts, authority and receipts native. Extend `scripts/check-read2-contracts.py` with `--profile keir-effect/4` for single-document checks. Preserve the existing read/2 default, result envelope, input bounds and exit status. The existing `keir-contract` command without arguments runs both suites and fails on either, with the new suite using `test_effect4_contracts.py`. Link profile guidance from the relevant authoring docs. Stop for accepted native documents excluded by the schema, native acceptance disagreement, or any need to alter production behavior, dependencies or CI policy.

**Verification plan:**
- Structural scope → current shared fixtures and decoded source equivalents validate. Closed-envelope, legacy authority, handler-shape and representative local bound negatives fail.
- Native authority → add one compiler-produced positive with a guarded CALL and a declared handler, which is absent from current effect/4 fixtures. Validate and prepare the same exact bytes in both runtimes. Use `GenericHookCompilerTest`, `BoundEffectRuntimeTest` and `runtime-node/test/bound-effect.test.js` as existing evidence or extension points. Reuse model identity and second-WRITE tests. Add only missing shared residuals for an invalid guard reference, undeclared handler and malformed checked CEL. Assert rejection at preparation with zero dispatch. An undeclared handler is `ARTIFACT_INVALID` at the per-code path in Java and the handler map in Node. Handler overflow is a schema negative, with Java and Node retaining their different native codes.
- Compatibility → existing read/2 CLI and tests, focused compiler/runtime tests and the normal product contract checks pass with pinned offline schema dependencies.

**Plan review:** Agent technical review: see [Plan review](#plan-review) below.

**Approvals:** Not required at this risk level.

**State:** Ready to implement
<!-- agent-workflow:end -->

## Plan review

Clean-context review of `673ca5c9` requested exact format and handler rules, explicit checker compatibility, and minimal missing native proof. The reviewer accepted the revised plan and recommended pinning the compiler literal that both effect/4 loaders already enforce. The manager verified that literal in both loaders. The current base leaves the reviewed product paths unchanged.
