# Build the effect/4 authoring companion

<!-- agent-workflow:start -->
**Outcome:** A host developer can obtain and verify the exact effect/4 guide, schemas and authoring examples offline.
**Target:** Keir Core
**Scope:** One deterministic data-only ZIP builder and verifier, a focused test, and host consumption documentation.
**Constraints:** Reuse current fixtures and exact verified development identities. The archive requires an independent trusted digest. Runner behavior, runtime packages, Daymark and publication are outside this increment.
**Completion criteria:** Repeated builds match byte for byte. Verification rejects wrong pins and malformed archives. Packaged schema checks and compilation preserve the independently verified example. Hosts have a documented way to verify and load the guide separately from their exact model.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"A host developer can obtain and verify the exact effect/4 guide, schemas and authoring examples offline.","scope":"One deterministic data-only ZIP builder and verifier, a focused test, and host consumption documentation.","constraints":"Reuse current fixtures and exact verified development identities. The archive requires an independent trusted digest. Runner behavior, runtime packages, Daymark and publication are outside this increment.","completion_criteria":"Repeated builds match byte for byte. Verification rejects wrong pins and malformed archives. Packaged schema checks and compilation preserve the independently verified example. Hosts have a documented way to verify and load the guide separately from their exact model."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Simple
**Reason:** The packaging script and tests are outside Blue zones. The verifier handles an untrusted archive.
**Discovery:** The effect/4 structural compiler test already recompiles the example JSON source to the pinned artifact. The full conformance harness checks that artifact on both runtimes. The offline consumer evidence covers the staged compiler, Java runtime and Node runtime from `98d1901`.
**Material assumptions:** The guide, schemas and example files at `57a74a5` exist as committed blobs. If the example artifact differs from the verified compiler output, stop and reconcile compatibility before delivering any archive.
**Plan:** Add `scripts/authoring-companion.py`, `scripts/test-authoring-companion.py` and `docs/agents/authoring-companion.md`, with an entry link in the agent index. Read a fixed allowlist from the reviewed commit. Include the committed guide, three schemas, source/model/artifact triplet and the matching, skipped and lost-response rows from the checked-in corpus. Generate a closed manifest with file sizes and digests and an exact development compatibility revision. Write a deterministic stored ZIP. Require a trusted external digest before opening a bounded archive, verify its exact inventory and canonical manifest, and avoid extraction.
**Verification plan:**
Repeatable archive -> build twice offline and compare archive bytes, guide digest and selected fixture bytes.
Verifier rejects wrong pins and malformed archives -> test good, wrong pin, modified payload, duplicate/extra entries and malformed ZIP.
Current example preserved -> run the existing schema checks on extracted copies and compile the bundled source/model with the vetted compiler. Compare emitted bytes and reuse exact Java/Node evidence for the same artifact and cases.
Host workflow documented -> verify links and CLI examples. Inspect scoped changes for private content.
**Plan review:** Agent technical review: [Plan review](#plan-review).
**Approvals:** Not required at this risk level.
**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Isolated candidate starts at `57a74a5`. Planned paths are the builder, focused test, companion guide, agent index and this record.

The implementation verifies the external digest before ZIP parsing. It regenerates the expected manifest and canonical archive from checked members. The development compatibility revision is shared by the compiler and both runtimes.

## Plan review

The independent reviewer passed the plan at `57a74a5`. The plan uses the current effect/4 corpus, reuses staged consumer evidence for creation and no-op, and uses shared conformance for the lost-response case. Verify malformed ZIPs with matching external digests to reach structural rejection. Regenerate the canonical archive from checked payloads to reject alternate archive encodings and trailing data.

## Evidence

The three offline CLI tests passed against the candidate based on `57a74a5`. Repeated builds produced identical archives. The tests checked the exact guide and case identities, the external pin, and rejection of a changed payload, duplicate and extra entries, trailing data, truncation and oversized input. The archived source, model and artifact passed validation against the packaged schemas using the existing offline checker. The verified development compiler reproduced the artifact byte for byte. All three bundled scenarios matched the retained exact-artifact Java and Node conformance outcomes. Redline classified the scripts as Gray with no boundary violations.

The archive is a development candidate for the package-release feature. Reference-host adoption and public release remain separate.

## Result review

Agent technical review: [Independent verification](#independent-verification)

Reviewed revision: `57a74a5` plus the four-file candidate with content fingerprint `1fdba7ae8dcd3bf3f9e1f59d141e38568f44bf953bf47a7dda4575af2286e663`.

Verification adequacy: Pass. The offline tests exercise reproducibility, the external pin and canonical-archive rejection. The schema and compiler checks exercise the exact archived inputs. Existing conformance covers the same artifact and scenario expectations.

### Independent verification

The non-implementer inspected the four deliverable files and independently checked the archive inventory, manifest digests, guide digest, scenario IDs and archived artifact against the retained compiler output. The reviewer found no blocking issue in scope, external-pin enforcement, bounded parsing, canonical verification or host authority separation.
