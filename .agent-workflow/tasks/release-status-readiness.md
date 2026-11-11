# Core 0.3.1 release-status documentation

Task identity: `agent-workflow:release-status-readiness`. Owning roadmap: `roadmap/features/public-package-release.md`; the manager owns its reconciliation. Relay identity discovery is unavailable; no named role is assumed.

<!-- agent-workflow:start -->
**Outcome:** Current-facing Core and Daymark documentation accurately distinguishes the published Core 0.3.1 release from preserved candidate preparation and companion metadata.
**Target:** Core release, companion, specification and conformance documentation; Daymark package and integration documentation.
**Scope:** Correct release-status prose and links in current documentation; retain historical preparation evidence and explain preserved candidate metadata. Deliver reviewed documentation changes through one PR per repository.
**Constraints:** Preserve all packaged, vendored, guide and compatibility-record bytes and hashes, lockfiles, runtime behavior and prior release history. Do not publish another release, change visibility, deploy, alter policy or claim a live rollout. Manager retains canonical roadmap ownership.
**Completion criteria:** Current documentation identifies the published v0.3.1 release and exact source, verified downloads and private repository scope; companion edition 7 is described as an available candidate archive with unchanged candidate compatibility metadata. Documentation checks, immutable-input comparison and independent review pass; both PRs merge without overwriting concurrent changes.
**Requirement baseline:** {"source":"work-record-initial","outcome":"Current-facing Core and Daymark documentation accurately distinguishes the published Core 0.3.1 release from preserved candidate preparation and companion metadata.","scope":"Correct release-status prose and links in current documentation; retain historical preparation evidence and explain preserved candidate metadata. Deliver reviewed documentation changes through one PR per repository.","constraints":"Preserve all packaged, vendored, guide and compatibility-record bytes and hashes, lockfiles, runtime behavior and prior release history. Do not publish another release, change visibility, deploy, alter policy or claim a live rollout. Manager retains canonical roadmap ownership.","completion_criteria":"Current documentation identifies the published v0.3.1 release and exact source, verified downloads and private repository scope; companion edition 7 is described as an available candidate archive with unchanged candidate compatibility metadata. Documentation checks, immutable-input comparison and independent review pass; both PRs merge without overwriting concurrent changes."}
**Relationships:** {"schema_version":1,"sources":null,"pull_requests":["https://github.com/rore/keir-core/pull/22","https://github.com/rore/keir-demo-app/pull/3"]}
**Risk:** Elevated
**Complexity:** Large
**Reason:** The two conformance README paths fall outside the configured documentation exemption and have a gray Redline floor. The text-only contribution spans two independently delivered repositories; no behavior or package changes are planned.
**Discovery:** GitHub release 441427764 is published, non-prerelease and latest; v0.3.1 resolves to 5c772d02b4dc92bb28ea8aedabd34498ff6d9391. All seven asset IDs, sizes and hashes match the verified publication record. Core and demo main begin at f1c6ad674b6815bb1c3e1dfbd8f53739929a1e8b and 02a7f3de81dc5febe3c72ac9d10666c4bca5c954. Current documents still describe an unpublished release or unverified downloads. Immutable companion guide/record and demo JSON describe their original candidate preparation and must remain intact.
**Material assumptions:** The published source, tag and asset pins remain unchanged; recheck before merging and stop on a mismatch. Remote file blobs must match the inspected bases; refresh and review any concurrent edits rather than overwrite them. The companion's existing candidate package map remains empty; availability of its ZIP does not upgrade that metadata.
**Plan:** Follow-up maintenance cleanup: make existing general guide prose version-neutral and link canonical compatibility, installation and provenance pages. Scope is Core docs/specs/{README,compiler-artifact,domain-model,language,workflow-expressions}.md, docs/testing.md, conformance/README.md and conformance/resource-qualification/README.md; demo README.md and docs/{ai-authoring,task-effects,read-extension-lifecycle}.md. Keep exact matrix rows, edition records, release records, versioned examples and vendor/package pins. Change only the release/dependency paragraphs in Studio-facing docs. Check live PRs/branches and remote blobs before delivery; preserve concurrent Studio edits. Validate links, claims, unchanged protected inputs and full diffs, obtain independent review, and merge with head guards. No new documentation page or tracker.
**Verification plan:** General-guide patch/publication references -> remove duplication or retain only exact compatibility statements and canonical links. Published-status claims -> live release/tag/asset metadata and prior download comparisons. Usable documentation -> relative links, anchors, release asset names and whitespace checks. Immutable preparation evidence -> base comparison of protected guides, compatibility JSON, vendor files and package manifests/lockfiles, plus a complete changed-path allowlist. Companion distinction -> existing edition 7 archive verification against the published pin without --core-assets. Safe delivery -> fresh per-repository Redline and shared Workflow evidence, exact-diff independent review, remote base/blob checks, native PR reviews/threads and expected-head merge followed by merged-tree and visibility checks.
**Plan review:** Agent technical review: release_docs_review. Approved the bounded version-neutral follow-up plan at the recorded bases; exact compatibility and environment applicability remain authoritative.
**Approvals:** Existing user authorization covers fixing and merging readiness issues. No additional approval is required at this risk level.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Discovery and recovery

This record is required because the complete scope includes conformance README files outside the configured docs-only exemption. No packaged input, role guidance, policy or canonical roadmap has changed. The manager will receive exact PR/merge evidence for the owning roadmap.

## Plan review

Agent technical review: release_docs_review. Approved at the recorded Core and demo bases after inspecting the task, workflow and risk guidance, live published release/tag/seven asset pins, current prose and protected metadata. The reviewer independently verified the published companion without --core-assets and compared two protected Core files plus 57 demo vendor/package files against the bases. No blocking findings. The exact conformance paths and criterion-to-method verification mappings are recorded above. Historical run totals and the candidate compatibility map remain unchanged.

## Implementation and verification

Updated nine Core documentation files and five demo documentation files. The release record retains the candidate preparation history while adding published status, source/tag identity and verified download evidence. Current specification and integration pages link to v0.3.1. The companion wrapper preserves its edition 7 section anchor and explains the immutable candidate guide wording and empty package map; its reproduction command uses the exact original input commit.

All 14 inspected original file blobs matched current remote main before editing. The complete local change allowlist contains only those documents and this record. No guide, compatibility JSON, vendor file, manifest, lockfile, schema, fixture, code or runtime setting changed. Both full diffs pass git diff --check. All 154 checked relative links and anchors resolve, including the edition 7 anchor. The existing verifier passes for the downloaded published edition 7 archive at SHA-256 8a4101a64fbbf4d09b59f936083a48df39256fcba8386cee66bd5f7bcad2c4a7, without --core-assets.

Fresh Redline reports identify only the two expected conformance documentation paths as gray, all other documentation as blue and this record as excluded. No boundary violation or API/schema/security/runtime-configuration change is detected. Demo's report is blue. Runtime tests were not rerun for prose-only changes; original source/run test totals and measurement limits remain unchanged.

## Result review

Agent technical review: release_docs_review.

Reviewed revision: Core 3c208dbc3ed798d97ba47f8bf96d1640b42c7c5f; demo aaff7fec1e28cf48005d0565a9ba750a338958ed.

Verification adequacy: Approved both exact implementation revisions for the text-only scope after checking full diffs, protected inputs, release pins and focused evidence. The reviewer found one stale current-baseline claim in the testing guide; it was corrected to identify published 0.3.1 and attach the original 0.3.0 source, CI run and 39/51 counts explicitly to historical qualification. Focused follow-up review approved that correction with no remaining findings. The added link resolves, and no runtime, package, vendor, guide or compatibility bytes changed. Only this review/delivery record changes after the reviewed implementation revisions.

## Delivery and recovery

Delivery references: [Core PR #22](https://github.com/rore/keir-core/pull/22) and [Daymark PR #3](https://github.com/rore/keir-demo-app/pull/3). Their merged state identifies the integration commits. Both exact changed-path lists match the approved scope; native review lists and inline threads were empty at pre-merge inspection, with no requested reviewers or teams. Shared Workflow validation passed for both repository path sets after result review. Expected-head guards protect each merge; hosted CI and fresh remote head/pin checks remain the final delivery checks. The manager receives final merge, preserved-byte and release evidence for `roadmap/features/public-package-release.md`; this contribution does not edit that owner-managed roadmap or imply a live rollout.

## Maintenance follow-up

The user requested concise, version-neutral general guides after the publication wording pass introduced repeated release announcements. This refines the same documentation outcome and preserves the original baseline and pins. Bases are Core 9bdff8d5b722aded7a89c712e962c5690b3aa8d6 and demo 631b4c75c562fb141874fc79405283f8218754c8. Direct task messaging is unavailable; no open PR or published ahead-of-main Studio branch was found at pickup. The contribution is limited to package/contract paragraphs, with current remote blob and PR checks before merging. Broader documentation and Studio reviews remain separate. Only the required existing Work Record is updated; no tracker or explanatory page is added. The follow-up technical plan review approved this scope with no blockers.
