# Package the second authoring guide

<!-- agent-workflow:start -->
**Outcome:** A host can choose and verify the authoring companion that contains guide v2.
**Target:** Keir Core authoring companion
**Scope:** The Core companion CLI, focused offline tests, host selection documentation, and this Work Record.
**Constraints:** Preserve v1 CLI behavior and exact archive bytes. Reuse the existing schema, example, compiler and runtime contract. Keep Daymark, runtime authority, the README opening edit and publication outside this increment.
**Completion criteria:** Explicit v2 builds are reproducible, verify against a trusted digest and reject mismatched versions or bytes. V1 remains the default and matches its trusted pin. The archived example compiles to the pinned artifact and matches both runtimes.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"A host can choose and verify the authoring companion that contains guide v2.","scope":"The Core companion CLI, focused offline tests, host selection documentation, and this Work Record.","constraints":"Preserve v1 CLI behavior and exact archive bytes. Reuse the existing schema, example, compiler and runtime contract. Keep Daymark, runtime authority, the README opening edit and publication outside this increment.","completion_criteria":"Explicit v2 builds are reproducible, verify against a trusted digest and reject mismatched versions or bytes. V1 remains the default and matches its trusted pin. The archived example compiles to the pinned artifact and matches both runtimes."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Simple
**Reason:** The scripts are Gray. Archive verification consumes untrusted bytes and must preserve the existing integrity checks.
**Discovery:** The guide v2 blob is committed at `74d128f`. Its matching schemas and fixtures are byte-identical to the v1 source pin. The compiler and both runtime implementation sources still match the development compatibility pin.
**Material assumptions:** The reviewed guide and all exact source and runtime pins are available. Keep the original compatibility pin if the compiler and both runtimes verify the same artifact and case expectations.
**Plan:** Add an explicit version selector to build and verify, defaulting to v1. Share all inventory and validation code. For v2 select the committed guide and input revision and emit a distinct manifest format. Keep the verifier's external digest check before parsing and validate the inventory against the caller-selected version. Document selection, trusted-pin verification and the existing authority boundary.
**Verification plan:**
Archive integrity -> compare the default and explicit v1 archive with the independent v1 pin, reproduce v2, and test correct verification, version mismatch, wrong pin and invalid archive encoding.
Compatibility -> compare all shared members, validate the extracted documents, compile the archived source and run the selected scenario expectations on the existing qualified hosts.
Scope -> check the work record, links, scoped diff and unchanged runtime and Daymark inputs.
**Plan review:** Agent technical review: [Plan review](#plan-review).
**Approvals:** Not required at this risk level.
**State:** Ready for review
<!-- agent-workflow:end -->

## Plan review

The independent reviewer passed the Elevated and Simple plan at `5f29066`. The guide and unchanged schema, fixture and runtime inputs were confirmed. The reviewer specified testing both cross-version directions using each archive's correct pin and testing a malformed ZIP with a wrong pin to exercise digest-before-parsing. The agent index and implementation reference will also reflect the new selection.

## Implementation

The builder and verifier share one deterministic ZIP encoding and one set of integrity checks. Both commands accept an explicit version. The v1 default and success response remain compatible. Version 2 selects the guide from `74d128f04a5fd76d744e3b7705f6614e64846b38` and has its own inventory and manifest format. The development compatibility revision stays `98d190104b1978b076437ec38bde6e7502e12012`.

## Evidence

- Four offline CLI tests passed. Default and explicit v1 builds match the trusted SHA-256, and the rebuilt v1 ZIP matches the previously verified archive byte for byte. Repeated v2 builds match and verify. Wrong pins, cross-version archives with correct pins, and noncanonical ZIPs are rejected. An invalid ZIP with a wrong pin fails at the digest check.
- The 43,519-byte v2 ZIP has SHA-256 `f073feaefe99e9dfe3b1dd818347f97cdca46c22a780bfb7e70992913aceac04`. It contains the exact v2 guide and the same schema and example payloads as v1. The archived source, DCM and artifact pass the archived schemas. Compilation produced the archived artifact byte for byte. The Java and Node hosts both matched the three archived outcomes.
- The [package-release feature](../../roadmap/features/public-package-release.md) remains in progress. The manager owns roadmap reconciliation. Host rollout needs the v2 ZIP, its reviewed digest and explicit selection of the v2 guide member through the host's existing configuration.

## Result review

Agent technical review: [Independent review](#independent-review)

Reviewed revision: `5f29066` plus the scoped candidate with content fingerprint `d8b7abc9b855f816851322d737264a768052c586060cea01373f330d3978b487`. The final wording clarifies that the v1 link opens the archived guide page.

Verification adequacy: Pass. The external-pin and archive checks exercise the version selection and archive trust boundary. The schema, compiler and host checks use the exact bundled documents and cases.

### Independent review

The reviewer independently verified both archive digests, the inventory and manifest member hashes, the v2 guide pin and the seven shared payloads. Review checked the v1 CLI behavior, the v2 version-selection and digest boundary, and the existing compiler and Java and Node verification evidence. No blocking findings.
