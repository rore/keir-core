# Public release readiness

Task identity: `agent-workflow:public-release-readiness`. Covers Core and Daymark. Role discovery is unavailable; no Relay role assumed. The user explicitly requested reviewable branches/draft PRs without merge, overriding the usual main delivery policy. The manager retains canonical roadmap ownership; report contribution and remaining gaps against `roadmap/features/public-package-release.md` and `roadmap/features/public-onboarding-readiness.md` for reconciliation.

<!-- agent-workflow:start -->
**Outcome:** Prepare reviewable licensing, provenance and fresh-clone tooling fixes for a public open-source release.
**Target:** rore/keir-core and rore/keir-demo-app.
**Scope:** New licensed companion edition using existing qualified contracts, historical companion source-revision resolution without changing published archive bytes, Minimap copyright/license notices in both bundled copies, source-provenance documentation and focused regression checks. Daymark documentation explains original package pins and reachable equivalent sources while preserving vendor metadata and packages.
**Constraints:** Preserve original package provenance, published assets, history, existing branches and active work. No merge, release publication, visibility/settings change, force push or Support contact. No credential or private identifier output. Respect prior download denials. Do not claim inaccessible Java package or artifact checks passed. Keep implementation small and isolated.
**Completion criteria:** Historical companion tests and fixed published hashes pass in a fresh clone without original removed commits; new edition bundles LICENSE and verifies offline; Minimap copies contain authentic upstream MIT notice; all original vendor/package pins unchanged; reviewable commits and draft PRs offered with exact verification and remaining gaps.
**Requirement baseline:**
{"source":"delegated-user-request","outcome":"Complete necessary release-readiness checks and prepare fixes","scope":"Companion license packaging, Minimap attribution, truthful history-rewrite provenance and fresh-clone rebuild fixes; feasible release checks","constraints":"Reviewable branch or draft PR, no merge/publish/visibility/history/asset replacement/Support contact; preserve work and original provenance; respect access denials","completion_criteria":"Verified narrow fixes, truthful unchanged historical pins, exact tests and remaining access/decision gaps"}
**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** Fresh Redline report classifies packaging/tool/vendor paths gray. This touches archive verification and reproducibility across two repositories but does not alter runtime contracts or published artifacts.
**Discovery:** Core baseline 31c35f70a16ef46681cc6f1c1f957bcb49a52342; Daymark baseline 74c24cd2cba90aaf804f06cddaeffa873f3c3460. Isolated review branches; original checkouts untouched. Fresh clone companion suite fails with four errors due to missing pre-rewrite Git IDs. Historical payloads are unchanged by the history cleanup. Companion editions 1–5 omit LICENSE. Both bundled Minimap 0.3.6 skill copies omit Minimap's own MIT copyright/license; js-yaml already has its correct license. Upstream Minimap v0.3.6 resolves to c1b311ad9c5df7f5d70ab63b50ce5f8c8603e7b1; authentic LICENSE obtained and hashed. Prior binary scan found no exact work tokens or recognizable secrets in six JAR versions and vendor payloads. Original release downloads and Actions materialization remain unavailable; do not retry without materially new evidence.
**Material assumptions:** Selected historical companion source files are byte-identical in mapped reachable commits; compare against the private verified original backup before using any mapping, and fixed published archive digests must still pass. Existing edition 5 contracts/fixtures remain qualified Core 0.3.0 inputs; new edition is explicitly a candidate, not a published release. If scope expands to runtime semantics or real release changes, stop that work and return to planning.
**Plan:** Add a small explicit original-to-reachable revision receipt for nine historical companion/Core inputs; resolve Git reads/comparisons to reachable commits while keeping original manifest and compatibility revision values unchanged. Reject unknown missing revisions with actionable errors. Preserve all original edition bytes and trusted pins. Add edition 6 as an unpublished candidate using edition 5 contracts and examples plus the full Apache LICENSE and a copied versioned guide; require LICENSE in that edition's deterministic inventory and manifest. Add tests for original archive pins, offline verification, missing license, and a fresh-clone mapping with removed objects. Do not change historical compatibility records, vendor manifests or assets. Add authentic Minimap LICENSE and short origin NOTICE to both copies, plus root third-party index. Document original provenance versus reachable source IDs, legacy companion license availability, new-release requirement, and remaining checks. Daymark receives documentation links only. Commit on task branches, independent result review, then normal push and draft PRs; no merge. Roadmap reconciliation remains with manager.
**Verification plan:** Run companion regression suite from the fresh branch, including exact historical pins and licensed edition round-trip/negative checks. Independently compare nine source mappings against original backup for every used payload, preserving original manifest IDs. Run Core release-verifier unit tests and whitespace/link/privacy checks appropriate to changed files. Build and verify the new candidate from committed HEAD in clean checkout with no Git needed for verify. Check both Minimap license bytes against the authentic upstream release. Confirm Daymark vendor package/provenance hashes unchanged and both original checkouts clean. Reuse prior clean npm/Maven dependency and packaged-compiler evidence without claiming a released Java runtime consumer pass. Independent technical plan/result reviews required before implementation/delivery.
**Plan review:** Pending independent technical review.
**Approvals:** Not required at this risk level. The user's instruction authorizes preparing changes and checks; it does not authorize merge or publication.
**Exceptions:** —
**State:** Blocked
<!-- agent-workflow:end -->

## Implementation
Discovery and pre-edit Redline complete. Await independent plan review before source edits. No current source, package or remote changes.

## Evidence
Fresh clone baseline companion test run: four errors, all caused by removed historical Git objects. Prior read-only assessment retains full original binary scan and clean-cache checks; release/artifact access gaps remain explicit.

## Result review
Pending.
