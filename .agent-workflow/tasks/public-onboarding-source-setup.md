# Reproduce the documented source-example setup

<!-- agent-workflow:start -->
**Outcome:**
The source quickstart and CI use one reliable setup command that preserves package integrity and the existing locked dependency graph.

**Target:**
Keir Core source-example installation and its CI caller.

**Scope:**
A source-only setup script, its focused Node tests and the existing CI setup caller. Coordinate final command text with the documentation owner. Verify the existing canonical DCM-fingerprint procedure separately without inventing a public interface.

**Constraints:**
Keep integrity checks, npm ci, existing CI assertions, package.json and unrelated lock fields intact. Preserve pre-existing edits and restore both original package.json and package-lock.json bytes on any setup or postcondition failure. No runtime/compiler semantics, dependency upgrades, new public API, registry or visibility changes, global tooling/configuration or live-app/data changes. Use isolated tooling and synthetic examples.

**Completion criteria:**
A clean source checkout uses the published setup command to run both Node examples and the edited-title flow; CI invokes the same command and retains its package/extraction checks. Focused tests detect unexpected lock drift and setup failures. Consumer/fingerprint instructions have actual released-tool evidence and disclose their limits.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"The source quickstart and CI use one reliable setup command that preserves package integrity and the existing locked dependency graph.","scope":"A source-only setup script, its focused Node tests and the existing CI setup caller. Coordinate final command text with the documentation owner. Verify the existing canonical DCM-fingerprint procedure separately without inventing a public interface.","constraints":"Keep integrity checks, npm ci, existing CI assertions, package.json and unrelated lock fields intact. Preserve pre-existing edits and restore original lock content if setup changes an unapproved field. No runtime/compiler semantics, dependency upgrades, new public API, registry or visibility changes, global tooling/configuration or live-app/data changes. Use isolated tooling and synthetic examples.","completion_criteria":"A clean source checkout uses the published setup command to run both Node examples and the edited-title flow; CI invokes the same command and retains its package/extraction checks. Focused tests detect unexpected lock drift and setup failures. Consumer/fingerprint instructions have actual released-tool evidence and disclose their limits."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-onboarding-readiness"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:**
The script changes a local package-lock pin and CI invokes it. Native integrity and failure behavior need independent assessment; package/framework behavior and release assets remain unchanged. The known policy floor for scripts and CI is gray.

**Discovery:**
At source b038bf42baa4b0d2d873e88df2409e5ba123bbeb, the developer reproduced EINTEGRITY from the literal pack-to-ci quickstart. CI already refreshes the local tarball integrity through its own one-off command. Native npm pack, explicit local-tar npm install --package-lock-only, then npm ci succeeds, changing only that integrity leaf. Both Node examples and an edited title pass; retained released compiler and Java runtime assets reproduce the expected results. The developer also verified the fingerprint recipe against the released compiler, including key-order/whitespace equivalence and stale-pin rejection; ordinary JSON serializers alone do not cover the specified control-character escaping.

**Material assumptions:**
Native npm refresh can preserve every field other than the freshly packed local runtime integrity. If it changes anything else, fail and restore the original content rather than widening the allowed changes. Source setup and released-package installation are separate procedures. No supported public hash helper/CLI is assumed; documentation must describe only verified procedures.

**Plan:**
Add scripts/setup-node-example.mjs and scripts/setup-node-example.test.mjs. Derive paths from the script location and validate runtime name/version and exact host declaration, locked version and local-tar path before any write. Pack the current source runtime using native npm, explicitly refresh only its local-tar pin through npm, verify the exact allowed semantic lock delta and unchanged package.json, then retain npm ci. Verify integrity against the freshly packed tar. On any command or postcondition failure restore both original manifest and lock byte buffers; do not claim to restore node_modules replaced by npm ci. Preserve ignore-scripts, no-audit, no-fund and a project-local cache. Replace the existing setup sequence in .github/workflows/keir-contract.yml, include both helper/test paths in both event filters and explicitly run the focused test. Preserve all extraction/source equality, teaching and old/current runtime comparisons. The documentation owner integrates the verified command. Do not add a hash API or compiler feature through this task.

**Verification plan:**
Literal source setup -> Run the shared command in a clean isolated checkout, both existing Node examples and the published edited-title procedure; identify whether each compiler invocation is source-built or released.
Lock integrity and preservation -> Focused tests for the single permitted tar integrity change, unexpected graph/package.json drift, failed npm commands and exact original-content restoration.
Cross-platform invocation -> Verify native npm execution and path handling on the qualified Windows setup and preserve the existing Linux CI checks.
Existing CI coverage -> Confirm the caller retains every package extraction, source comparison and example test after using the shared command.
Fingerprint documentation -> Check new/changed models, whitespace/key-order equivalence, stale digest rejection and all control-character escaping against the released compiler; preserve exact version scope.

**Plan review:**
Independent developer plan review passed at b038bf42baa4b0d2d873e88df2409e5ba123bbeb. The manager incorporated both required clarifications: exact restoration of both original manifest and lock bytes, and explicit CI path-filter/test discovery. The reviewer also checked invocation/path validation, fresh-tar integrity, preservation of existing CI assertions and released DCM fingerprint evidence. Implementation may proceed within these conditions; result review remains pending.

**Approvals:**
Not required at this risk level. Existing public-readiness and automation authorization covers the bounded repository changes; no global or live environment action is included.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The Core developer completed the isolated reproduction and explicitly adopted the independent plan-review conditions. The manager released the three-file implementation after recording that review. Documentation and Studio work continue independently.

The implementation is frozen as a three-file patch against b038bf42baa4b0d2d873e88df2409e5ba123bbeb: the source setup helper, focused test and existing contract workflow. Native npm refresh replaces the manual CI-only hash edit; both original package files are restored byte for byte on command or postcondition failure. CI uses the same helper, explicitly runs its tests and includes both paths in its event filters. The independent reviewer accepted the bounded result review; integration awaits that verdict and the documentation replay.

## Evidence

Reproduction and native setup results are reported from the isolated b038bf4 checkout; inspect the retained evidence during plan/result review. The separate documentation owner has already qualified released consumer installation outside both source repositories, so do not duplicate that work.

The manager inspected the frozen patch manifest and retained logs. All 18 focused tests pass, including real native Windows npm with spaces, unrelated working directory, changed cached tar bytes and repeated idempotent setup. Exact restoration, malformed identities, unexpected lock/manifest drift and failed command stages are covered. CI equivalence and shell syntax checks pass, retaining extraction/source comparisons and teaching/parity assertions.

A clean source archive passed the literal shared helper. Source compiler packaging and source Java runtime/dependency builds passed, followed by all six runs: Node and Java follow-up, pre-operation and edited-title examples. Their results agree. These source-build checks are distinct from the earlier released-consumer checks. POSIX native execution remains a hosted-CI requirement after integration; no hosted result is claimed yet.

Frozen patch SHA-256: 35f7c91b48508eece9708d4db180e50b1728daaa81139718df41a95074cd4ed2. Helper SHA-256: e98c500a420306764b086f7cdbc3f3264a079dbeda58649d87480eb6fc31da9d. Test SHA-256: fd10f807ea533a8f393f2658efeb161ece8bd04155e9f8f9cfeeb83c48d9c47f. Workflow SHA-256: 785e6c6ed26483375294ed1da2930f7158e2fcf7665ab2d7e75eb6ac9016a041.

## Result review

Independent result review is in progress on the frozen three-file result. Focused and actual source-example evidence is available; final documentation replay, integration and hosted POSIX execution remain open.
