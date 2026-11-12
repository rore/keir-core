# Published capability documentation

Task identity: `agent-workflow:published-capability-docs`. Shared documentation contribution; roadmap classifications and completed feature states stay unchanged. Relay self lookup is unavailable; no role is assumed.

<!-- agent-workflow:start -->
**Outcome:** Current documentation describes published capabilities accurately without repeated release-status maintenance.
**Target:** Keir Core and Daymark documentation.
**Scope:** Correct stale current-facing development/candidate wording in Core guides, specification/index introductions, mutable companion catalog, examples README and current roadmap/design summaries; simplify general release references through existing canonical catalogs. Verify the merged Daymark adoption already supplies accurate publication, pins and historical distribution context, and add no redundant demo edits.
**Constraints:** No runtime/compiler/schema/fixture behavior, dependencies, package pins, immutable customer guides or compatibility metadata, historical qualification records, release assets, tags, visibility or deployment changes. Preserve exact versions for semantic compatibility, examples, reproducibility and provenance. Respect the workflow artifact-storage restriction and existing local execution holds. Use the authorized personal Git identity.
**Completion criteria:** Normal published release, tag and all seven asset downloads are verified; current user-facing false unreleased claims are corrected; general guidance uses durable canonical links; Daymark remains accurately pinned. Independent technical and first-reader reviews approve, affected links/anchors and preservation checks pass, and permitted PRs merge on green with remote verification.
**Requirement baseline:** {"source":"work-record-initial","outcome":"Current documentation describes published capabilities accurately without repeated release-status maintenance.","scope":"Correct stale current-facing development/candidate wording in Core guides, specification/index introductions, mutable companion catalog, examples README and current roadmap/design summaries; simplify general release references through existing canonical catalogs. In Daymark, clarify that newer Core publication does not change its pins and retain historically accurate distribution context.","constraints":"No runtime/compiler/schema/fixture behavior, dependencies, package pins, immutable customer guides or compatibility metadata, historical qualification records, release assets, tags, visibility or deployment changes. Preserve exact versions for semantic compatibility, examples, reproducibility and provenance. Respect the workflow artifact-storage restriction and existing local execution holds. Use the authorized personal Git identity.","completion_criteria":"Normal published release, tag and all seven asset downloads are verified; current user-facing false unreleased claims are corrected; general guidance uses durable canonical links; Daymark remains accurately pinned. Independent technical and first-reader reviews approve, affected links/anchors and preservation checks pass, and permitted PRs merge on green with remote verification."}
**Behavior changes:** [{"target":"task-context.scope","classification":"equivalent","before":"Correct stale current-facing development/candidate wording in Core guides, specification/index introductions, mutable companion catalog, examples README and current roadmap/design summaries; simplify general release references through existing canonical catalogs. In Daymark, clarify that newer Core publication does not change its pins and retain historically accurate distribution context.","after":"Correct stale current-facing development/candidate wording in Core guides, specification/index introductions, mutable companion catalog, examples README and current roadmap/design summaries; simplify general release references through existing canonical catalogs. Verify the merged Daymark adoption already supplies accurate publication, pins and historical distribution context, and add no redundant demo edits.","reason":"Daymark PR8 already fulfills the requested documentation correction with accurate adopted-package provenance and historical context. The required accurate documentation outcome is unchanged; a duplicate edit or PR is unnecessary."}]
**Relationships:** {"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:guarded-result-dataflow"},{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Large
**Reason:** The examples README is an unclassified gray path; two repositories and semantic compatibility introductions require accuracy and preservation review despite prose-only changes.
**Discovery:** Core0.4.0 is a normal published release at source/tag fc0601ae410e06c0be76d6f3b769abbd049b5999. Seven standard published-asset downloads match GitHub digests; Core SHA256SUMS and the independently recorded edition8 pin pass. Package identities are0.4.0. Daymark main77feb100 now adopts compiler/runtime0.4.0 and companion8 through merged PR8; its rewritten provenance supersedes both earlier demo corrections. Immutable companion8 candidate metadata and guide wording remain valid preparation history. Current Core guides/spec indexes falsely call the capability unreleased.
**Material assumptions:** The release API and downloaded package identities agree; a mismatch stops status edits. Current publication does not imply Daymark adoption or released-package metadata inside the immutable companion. Preserve historical Notes sections and read-only/write/failure/guard contracts; change only current labels and navigation.
**Plan:** Reconcile Core main0d174c3c without changing its separate adoption Work Record. Preserve approved current-facing corrections, canonical links, exact producer gates, commands and immutable history. Remove only the three summary clauses that could imply Daymark adoption is still pending. Verify Daymark main77feb100 packages and provenance, discard the superseded demo correction, and add no demo edits or PR. Refresh path/anchor/byte preservation checks against both current mains and obtain focused technical review; have the original clean first-reader assess the updated demo excerpt. Retry the Core ready-for-review PR once under new explicit approval and merge after native checks/reviews pass if policy permits. No runtime/package/qualification execution or storage-host request.
**Verification plan:** Publication -> fresh release/tag API plus every standard release-asset download, GitHub digest, Core SHA256SUMS, independent companion pin and package identity. Accuracy -> exact diff technical review against compatibility and actual Daymark manifests; fresh reader explains published capability versus installed host support and immutable candidate metadata. Links -> complete changed-page relative/cross-repository path and anchor checks with canonical external targets verified. Preservation -> exact allowed paths, old anchors and byte-identical source/spec semantics, immutable guides/metadata, fixtures and vendor pins. Delivery -> fresh main/head, checks, native reviews and permissions; verify merged tree and personal Git identity.
**Plan review:** Agent technical review: published_docs_review; approved original b824111 plan and focused de72429 resumption/reconciliation plan before edits, including dropping superseded Daymark work and synchronizing current record fields.
**Approvals:** The owner replied “i approve” to the explicit question authorizing the two public documentation PRs and merging after checks pass. The current instruction also permits updating or discarding the demo correction after its adoption merge. Honor every action-specific approval decision.
**Exceptions:** —
**State:** Ready for review
<!-- agent-workflow:end -->

## Discovery and delivery boundaries

The published archive is named candidate and preserves its original candidate metadata; those labels do not mean the Core release is unpublished. Verify current package support in the existing matrix and preserve immutable qualification bytes. The roadmap contribution updates availability summaries only, not task ownership, classification, priority or historical approval boundaries. No new documentation subsystem or tracker is introduced.

## Plan review

Agent technical review: published_docs_review. Reviewed plan b824111 and agreed additions before source edits. Approved scope and verification with no remaining blocker after the Large complexity correction. The reviewer independently rehashed all seven downloaded assets against the verification report and confirmed actual Daymark0.3.1/edition7 pins. Current conformance introduction and resource-phase label are included; historical broader-types Notes stay unchanged. No runtime, fixture, package or network execution was used in review.

## Implementation and verification

Corrected current release-status labels and reduced duplicated version references in the existing guides, specification indexes, examples, conformance introductions and roadmap summaries. General reader paths now use installation, compatibility and companion catalogs. Exact semantic producer gates, tested command examples, package/edition rows, source commits and archive pins remain. The companion catalog distinguishes published archive availability from its immutable preparation-time candidate metadata. Daymark's only edit clarifies that newer publication does not upgrade its pins and makes the old private-publication statement historical.

The release is normal and published, with tag/source `fc0601ae410e06c0be76d6f3b769abbd049b5999`. All seven downloaded standard published assets match API digests; Core SHA256SUMS and the independent companion pin pass. Compiler/Node/Java package identities match0.4.0. No workflow-storage request, install or runtime execution was made. Independent plan review separately rehashed all seven downloads.

Complete changed-page checks passed400 Core and4 Daymark links/assets with no missing paths or anchors. All prior anchors and fenced commands/logic sketches are preserved. Exact path inventory, unchanged immutable guides/metadata, historical feature Notes, fixtures, code, schemas, vendor files and actual Daymark0.3.1 archive/JAR digests pass. First-reader and technical result reviews remain pending.

## First-reader review and technical findings

The clean first-reader reviewer read only the exact proposed reader-path snapshot, without project history or source context, and approved with no blockers. The reader correctly separated released Keir capability, illustrative hosts, Daymark's pins and the immutable candidate-label history. Optional minimum-version repetition was not added to general guidance because the owner explicitly prefers durable canonical links; the technical catalog retains exact compatibility identities.

Technical review approved Daymark f3facff and the overall prose/preservation approach, but found two remaining current-status labels in Core b13fa34. Corrected the examples heading's development-preview label, preserving its generated anchor, and changed the feature summary's released-contract exclusion to retained-producer contracts. No semantic rule or historical Notes changed. Final focused result review remains pending.

## Result review

Agent technical review: published_docs_review.

Reviewed revision: `e075197798d239c5e9dc5309af90aeb22d25718c` (Core); `f3facffa3daf1eb30c129412c935e1387bf144a9` (Daymark).

Verification adequacy: approved with no remaining findings after the two focused label corrections. Complete inventory, exact semantic identities, unchanged commands, immutable guide/metadata/history and actual Daymark pins were inspected. Independent read-only rehashing confirmed all seven published assets. Source/doc validation is adequate for the prose-only scope; no package execution or new qualification is claimed.

Clean first-reader review: published_docs_reader approved snapshot `sha256:c82f7b46c310d5c54c59e7e00bf1cb46975dd1e61d1d86d0c99d51e09c518e6a` with no blockers. Only exact reader-path excerpts were provided, without project history, technical source or earlier reviews. The reader correctly understood availability, host duties, illustrative examples, separately pinned Daymark support, archive metadata and next steps. The final technical fixes do not alter that snapshot.

## Delivery blocker and recovery

Both reviewed branches are uploaded: Core `5806f8fe6b860e28364204030592341f3544bd9f`, Daymark `f3facffa3daf1eb30c129412c935e1387bf144a9`, each named docs/published-capabilities. Main was rechecked before upload and no implementation or dependency work was included.

Automatic approval review rejected opening the Core non-draft PR: “Opening a non-draft pull request is an external repository commitment; the user authorized updating documentation but did not explicitly authorize this new PR submission or its stated public review action.” User history was checked after the denial; no materially new authorization for this submission was present. The Core attempt did not create a PR. The equivalent Daymark submission is not attempted under the same unresolved restriction. No draft alternative, alternate API, direct merge or other bypass is used.

Resolve the explicit public PR submission/review approval block before continuing, then recheck current main and actual Daymark adoption pins because separate adoption work is underway. Both reviewed source trees and private verification evidence are preserved. This metadata-only handoff update is committed locally and retained unpushed to keep the remotely reviewed heads stable and avoid unnecessary CI. No release, tag, visibility, billing or deployment operation was performed.

## Approved resumption and reconciliation plan

The owner explicitly approved opening and merging the two documentation PRs after checks pass. The newly merged Daymark PR8 now supplies accurate0.4.0 provenance, archive/guide identities, original adoption history and rollout boundaries. Both earlier Daymark corrections are superseded by that rewrite; discard that redundant documentation delivery rather than open an empty or stale PR. Preserve its branch for traceability.

Core main advanced to `0d174c3c1ca3e486ce0d6f46958086a2b22dfead` only by the separate Daymark adoption Work Record. Reconcile that main without altering its record. Remove the three current Core summary clauses saying Daymark adoption remains separate; the existing links to Daymark's own package record now describe actual adoption. All reviewed language/specification/index changes otherwise remain unchanged. Validate against the new Core and Daymark mains and obtain focused technical review before the single newly authorized PR-submission retry. Do not use the old0.3.1 snapshot as current evidence.

## Reconciled result review

Agent technical review: published_docs_review.

Reviewed revision: `6a5cb5ff8967134fcac9459c92d6b90215f47a82`.

Verification adequacy: approved without findings. The delta contains exactly the three approved summary-clause removals, the merged adoption record is byte-identical, all intended file/hash inventories match, and whitespace passes. Demo is unchanged at main `77feb10032d3b63475057564ca56099b1e498faa`; its0.4.0 package identities and archive/JAR hashes match current evidence. Prior technical reviews stand for unchanged documentation.

The original clean first-reader independently checked refreshed snapshot `sha256:1cdf2201a445394df5e0f49a1748114a090d65b3007906880d4b67d4332d9f44`, with only the updated Daymark excerpt added to the previously approved reader path. Approved without blockers: actual adoption, retained approvals, historical metadata and rollout boundaries are clear and do not turn the illustrative expense host into an implemented integration. Optional wording nits in the already-merged Demo page do not justify a duplicate correction PR.

All400 Core and4 read-only Demo provenance links/assets and old anchors pass against the current bases. Commands, language sketches, immutable guides, metadata, historical feature Notes, source, fixtures and adopted vendor bytes remain unchanged by this documentation contribution. Submit only the needed Core PR under the new explicit approval; the proposed Demo PR is superseded by merged PR8.
