# Decision authoring documentation and companion edition 5

<!-- agent-workflow:start -->
**Outcome:**
Current public references explain the supported authoring and decision contracts; companion edition 5 reproducibly packages their actual producer inputs without changing historical archives.

**Target:**
Keir Core public documentation and authoring companion tooling.

**Scope:**
Public README prose, including module, package, example and conformance entry points; current public prose under docs/, including release guidance; scripts/authoring-companion.py, scripts/test-authoring-companion.py, a versioned edition 5 guide and compatibility record.

**Constraints:**
Follow frozen contract 9d2e593. Preserve the user-approved opening and generic diagram, exact edition 4 bytes, old approval identities, runtime host authority and failure boundaries. Do not edit schemas, fixtures, compiler/runtime code, architect-owned decision/host proposals, release tools, roadmap, governance or environment. No push or release. Edition 5 stays candidate until verified asset evidence arrives.

**Completion criteria:**
Primary references and agent paths coherently describe current authoring; one domain-neutral decision walkthrough distinguishes permission from native commit. Edition 5 builds and verifies deterministic actual inputs with exact compatibility metadata; historical archive regression, focused negative tests, link and privacy checks pass. Missing producer or release evidence is explicit.

**Requirement baseline:**
{"source":"manager assignment; frozen contract 9d2e593 and accepted public-path inventory at a8ef8a95","outcome":"Current public references explain the supported authoring and decision contracts; companion edition 5 reproducibly packages their actual producer inputs without changing historical archives.","scope":"README.md, current public prose under docs/, scripts/authoring-companion.py, scripts/test-authoring-companion.py, a versioned edition 5 guide and compatibility record.","constraints":"Follow frozen contract 9d2e593. Preserve the user-approved opening and generic diagram, exact edition 4 bytes, old approval identities, runtime host authority and failure boundaries. Do not edit schemas, fixtures, compiler/runtimes, architect-owned decision/host proposals, release tools, roadmap, governance or environment. No push or release. Edition 5 stays candidate until verified asset evidence arrives.","completion_criteria":"Primary references and agent paths coherently describe current authoring; one domain-neutral decision walkthrough distinguishes permission from native commit. Edition 5 builds and verifies deterministic actual inputs with exact compatibility metadata; historical archive regression, focused negative tests, link and privacy checks pass. Missing producer or release evidence is explicit."}

**Behavior changes:**
[{"target": "task-context.scope", "classification": "equivalent", "before": "README.md, current public prose under docs/, scripts/authoring-companion.py, scripts/test-authoring-companion.py, a versioned edition 5 guide and compatibility record.", "after": "Public README prose in the root, compiler-java, runtime-java and runtime-node; current public prose under docs/, including release guidance; scripts/authoring-companion.py, scripts/test-authoring-companion.py, a versioned edition 5 guide and compatibility record.", "reason": "Clarify the public-documentation locations confirmed by the task owner in the release-doc follow-up; the same documentation outcome and code ownership boundaries remain."}, {"target": "task-context.constraints", "classification": "equivalent", "before": "Follow frozen contract 9d2e593. Preserve the user-approved opening and generic diagram, exact edition 4 bytes, old approval identities, runtime host authority and failure boundaries. Do not edit schemas, fixtures, compiler/runtimes, architect-owned decision/host proposals, release tools, roadmap, governance or environment. No push or release. Edition 5 stays candidate until verified asset evidence arrives.", "after": "Follow frozen contract 9d2e593. Preserve the user-approved opening and generic diagram, exact edition 4 bytes, old approval identities, runtime host authority and failure boundaries. Do not edit schemas, fixtures, compiler/runtime code, architect-owned decision/host proposals, release tools, roadmap, governance or environment. No push or release. Edition 5 stays candidate until verified asset evidence arrives.", "reason": "Clarify that compiler/runtime code remains excluded while their public README prose is included; no runtime behavior or authority changes."}, {"target": "task-context.scope", "classification": "equivalent", "before": "Public README prose in the root, compiler-java, runtime-java and runtime-node; current public prose under docs/, including release guidance; scripts/authoring-companion.py, scripts/test-authoring-companion.py, a versioned edition 5 guide and compatibility record.", "after": "Public README prose, including module, package, example and conformance entry points; current public prose under docs/, including release guidance; scripts/authoring-companion.py, scripts/test-authoring-companion.py, a versioned edition 5 guide and compatibility record.", "reason": "Name the public example and conformance README entry points needed to reconcile the newly stable standalone examples; the same documentation objective and code exclusions remain."}]

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:pre-operation-guard"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:**
Documentation under docs/ is blue; the companion scripts and module/example/conformance READMEs are gray under the current policy. Packaging validates trusted compatibility and archive bytes, with cross-version regression risk.

**Discovery:**
Read the frozen decision contract at 9d2e593, primary-path inventory accepted at a8ef8a95, companion build/verify implementation and tests from 9fbf0a1. Existing pinned-edition machinery already validates commit provenance, exact inventory, byte pins and assets. Primary language/DCM/artifact references still describe /1 despite current /2 authoring. Bundled spec changes must precede the Core compatibility source pin because build_pinned compares all nonguide inputs.

**Material assumptions:**
Producer owners supply actual /3 schemas and compiler-produced examples. If unavailable, continue prose but do not invent fixture paths, artifacts or passing runtime results. Manager supplies exact release revision/assets/hashes; absent evidence keeps candidate status and empty packages. Frozen semantics are authoritative; discrepancies return affected work to planning and are reported.

**Plan:**
1. Independently review this plan before tool edits. Reconcile existing primary references with source /2, DCM/artifact/scenario /3 and descriptor /2, explicitly retaining exact /2 event execution only on runtime 0.3. Preserve historical docs and user-approved introduction.
2. Update human and agent navigation, and add one neutral decision authoring walkthrough using existing binding/preparation/invocation interfaces, discovered READs, denial/failure simulation and independent host commit checks. Keep proposed/unreleased status truthful until implementation acceptance.
3. Extend PINNED_EDITIONS for edition 5 using actual producer schemas/examples and matching semantic references. Pin the human references to immutable online doc commits to keep the archive self-consistent without recursive repository links; retain actual schemas/examples in the bundle. Reuse archive encoding, compatibility validation and offline verification. Validate exact 0.3 producer, model/artifact digest pins, descriptor/environment tuple and package versions; never relax edition 4 behavior. Keep CLI default unchanged.
4. Add focused edition 5 candidate/release, provenance/mixed-version/tamper/asset failure tests and an exact published edition 4 archive regression. Run affected checks; hand commits or preserved diff to manager for independent result review and integration. No release or push.

**Verification plan:**
- Deterministic edition 5 package and rejected mixed/tampered inputs -> scripts/test-authoring-companion.py, including actual committed-input build and offline verify.
- Preserved historical bytes -> reproduce published edition 4 and compare its independently recorded archive digest.
- Navigable, public-safe current prose and package references -> relative link/anchor checks and changed-content privacy/release-claim scan.
- Correct authoring/decision semantics -> compare the exact format/producer/descriptor/environment tuple and host boundary with frozen design plus actual producer files; retain runtime-owner evidence separately.
- Traceable scoped delivery -> task-record resolver/checker and fresh Redline report.

**Plan review:**
Agent technical review: decision_companion_plan, clean-context plan review against 9fbf0a1 and frozen 9d2e593; approved with verification clarifications recorded below.

**Approvals:**
Not required at this risk level. Manager assigned implementation within the user's documentation task.

**Exceptions:** —

**State:** Blocked
<!-- agent-workflow:end -->

## Implementation

Planning from Core 9fbf0a1. Isolated branch feat/decision-authoring-guide. Public prose updates underway. Actual producer inventory and release hashes remain external dependencies. Manager owns roadmap and final result review.

First prose pass updates the current source/model/artifact references, indexes, runtime/host obligations and agent paths. Added one domain-neutral decision walkthrough and a candidate edition 5 guide. Preserved the user-approved introduction and generic diagram. Added an exact published edition 4 archive regression; no companion production-code edits yet.

Manager reviewed and accepted the language/model/walkthrough direction, retaining candidate status and requiring real current producer links before release. A Node scalar-output admission mismatch is assigned to its runtime owner; docs retain the frozen bounded-scalar-only rule pending corrected test evidence.

Read-only producer inspection found actual staged /3 schemas and guard-input, guard and guard-handler source/artifact/scenario files. Edition 5 tooling now uses those observed paths and validates the exact model/artifact pins, compiler, source/scenario formats, descriptor recipe, environment and package versions. Existing encoding, size bounds, immutable source checks, CLI default and historical branches are retained. Local producer copies are test inputs only, byte-identical to the inspected files and excluded from this delivery. Stable source revision, real committed-build provenance and release pins remain pending evidence; no compatibility record invents that source identity.

Focused manager review found that matching source headers alone allowed stale example source beside an unchanged artifact. Added an edition 5-only raw source-byte digest check against artifact.sourceDigest, using the three canonical JSON producer files directly. The regression changes only each source name and leaves domain, entrypoint and compiled artifact untouched. All three cases reproduced the gap before the fix. No canonicalizer, compiler subprocess or historical-edition change was added.

Next action: receive a genuine producer commit and final independent runtime acceptance, then obtain immutable semantic-reference links from integrated prose. Compiler/Java owner migrates model/source/Java examples; Node owner migrates Node runners/package pins. Add the candidate edition 5 compatibility record with the real source revision, then prove a genuine committed-input CLI build, extracted-guide links and later the verified release asset row. No invented revision or release hashes. Preserve uncommitted changes for integration.

Release follow-up reconciles the release guide with the verifier's actual conditional two-example smoke and updates the three module/package READMEs for both lifecycles. No verifier, command flag or release-process changes. The inspected pre-operation Java/source files existed at that phase; the Node launcher was then pending. The stable inventory below supersedes that temporary limitation.

Stable producer handoff now confirms both standalone hosts and migrated follow-up inputs. Twenty companion inputs match the supplied staged tree byte for byte after a CRLF-only source-schema normalization; that object is a tree, not a commit. Producer example hashes and current fixture hashes were checked before local link-target copies. The quickstart and example READMEs now run both standalone examples, and primary/workflow/conformance references use current /3 examples with explicitly historical /2 compatibility. Runtime owner reports corrected scalar-output and mixed-pair cases with focused source/installed checks; source inspection matches the documented scalar-only output rule. Independent final Node review and integrated 0.3 package qualification remain pending.

## Plan review

Clean-context non-implementer approved the bounded approach. Reuse existing PINNED_EDITIONS/build/verify machinery without a framework or dependency. Edition 5 must check exact descriptor recipe /2 and all four dependency environment pins as well as compiler/model/artifact/package identities. Add negatives for each changed tuple member and each mismatched package role. Exercise actual committed-input build provenance, not only in-memory archive creation. Keep the guide first in the inventory, check extracted-bundle links/anchors and the existing 256 KiB ceiling. Reproduce edition 4 only against independently supplied published revision/pin; otherwise mark pending. Walkthrough must cover explicit absence versus unavailable selection, handled rejection with ALLOW still blocked, input/selection/read consistency, deadline reserve/cancellation and separate native results. These are host obligations, not new runtime guarantees. No scope or architecture blocker.

## Evidence

Canonical Minimap CLI returned the exact relationship tuple above. Relay participation attached successfully. No package or runtime claims yet.

- Focused companion suite passed all 13 tests using existing verified historical Core assets and the byte-identical staged producer snapshot. New edition 5 checks cover deterministic inventory/size, offline verification, tampering, every example tuple/environment mismatch, every wrong package role, noncommit revision, source-input mismatch, stale source bytes beside unchanged artifacts, and missing/mismatched assets. The stale-source regression failed for all three examples before the fix and passed afterward. Build-path unit cases substitute test blobs and asset bytes; they do not establish genuine committed-source or release compatibility. The initial historical suite run without required Core 0.1 assets failed its release-row setup; supplying the existing assets resolved that setup dependency without an installation change.
- Rebuilt published edition 4 through the real CLI from c6a6d171982514569c29c06de7a429d301d25b2a with the recorded Core 0.2 packages. Archive digest exactly matched sha256:4a9f4f30212ff1d722cf6c993902d0802bee7e7ffebc00304102a5964b5bec63; offline inventory and package verification passed. Published checksum and source manifest supply the independent baseline.
- Checked 27 changed public documents and 368 relative links/anchors after standalone-example reconciliation. Link, privacy and git diff --check checks passed. Twelve actual producer source/model/artifact/scenario documents validate against their matching structural schemas. All 20 nonguide package inputs match the supplied staged tree; the only initial difference was the checkout's CRLF spelling of the unchanged source schema, normalized to the Git blob bytes for the private package snapshot. Nine producer example files match their supplied manifest. New primary pointers use actual /3 paths; no qualification is inferred from their presence.
- Work Record exact-ref resolver found this record and full record checks passed, including equivalent clarification of the prose locations. Redline classifies both companion scripts and module/example/conformance READMEs GRAY, docs/ and root prose blue, with no boundary or API changes. No policy edits.
- The six affected edition 4/5 tests passed after snapshot reconciliation, retaining the exact published edition 4 digest and new source-byte mismatch regression. The other historical tests retain the prior full-suite evidence. No broad runtime or package suite was repeated for prose-only changes.
- The runtime owner supplied current standalone example evidence and corrected scalar-output/mixed-pair checks; those reports are owner evidence, not a new local test or final independent runtime acceptance. A genuine source commit, integrated 0.3 package qualification, immutable online semantic links and real package hashes remain pending. The edition 5 guide's semantic links currently resolve in the repository and must be pinned before archive publication.

## Result review

The integration owner reviewed and accepted the remaining documentation/tool delta against its explicit 30-file ownership manifest. Twenty remaining files were updated only after checking each expected integration hash and proposed hash; all 30 final hashes match the handoff. The narrow tool correction binds each bundled canonical source to the artifact's sourceDigest. All four edition-5 tests pass in the combined integration checkout, including the stale-source negative. Current standalone example paths and conditional two-example release guidance agree with the integrated implementations. A fresh changed-content privacy scan and diff check pass.

This accepts the implemented prose and tool correction. Completion remains pending a genuine integrated source commit, immutable semantic-reference links, verified Core 0.3 asset hashes and the actual committed-input guide build/download verification. No candidate test archive is treated as a published release.
