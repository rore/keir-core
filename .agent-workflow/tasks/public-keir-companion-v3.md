# Public Keir authoring companion edition 3

<!-- agent-workflow:start -->
**Outcome:** A reproducible companion for authoring against the public Keir 1 contracts.

**Target:** The Core authoring companion command, guide and archive.

**Scope:** Add an explicit edition 3 build and verifier, the current guide, a committed compatibility row, and focused tests.

**Constraints:** Start from `94bdbb7`. Preserve published editions and their reproducible bytes. Use committed inputs. Keep Core package publication, hosted CI, Daymark, and broader specification changes outside this delivery.

**Completion criteria:** Two builds from the same committed candidate produce identical archives. The trusted-pin verifier validates the inventory and member bytes. All bundled links resolve inside the archive. The bundled example recompiles byte for byte and its three scenario expectations match both runtime reports. Release metadata clearly distinguishes an evaluated source candidate from actual versioned assets.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A reproducible companion for authoring against the public Keir 1 contracts.","scope":"Add an explicit edition 3 build and verifier, the current guide, a committed compatibility row, and focused tests.","constraints":"Start from 94bdbb7. Preserve published editions and their reproducible bytes. Use committed inputs. Keep Core package publication, hosted CI, Daymark, and broader specification changes outside this delivery.","completion_criteria":"Two builds from the same committed candidate produce identical archives. The trusted-pin verifier validates the inventory and member bytes. All bundled links resolve inside the archive. The bundled example recompiles byte for byte and its three scenario expectations match both runtime reports. Release metadata clearly distinguishes an evaluated source candidate from actual versioned assets."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The archive crosses a release trust boundary. The additive implementation retains the existing deterministic format and independent pin, with a new versioned inventory and explicit compatibility record.

**Discovery:** The edition 1 and 2 builder reads fixed historical commits and writes bounded, stored ZIP entries. The qualified integration tree contains public compiler and runtime code, the readable teaching model, frozen source and artifact, and scenario documents. Several general spec links still describe the earlier contract.

**Material assumptions:** The selected Core source candidate supports the checked-in example. If compilation, schema checks or qualified runtime reports disagree, stop before handing off the archive and resolve the mismatch with its owner. Exact package asset metadata is pending until the release owner supplies the built assets and checks.

**Plan:** Extend `scripts/authoring-companion.py` and its tests without changing the historical paths. Require a full Git commit for v3 builds and read every input, including compatibility metadata, from that commit. Keep repository-relative archive names so the self-contained guide links to the four bundled schemas, example and three frozen scenarios. Identify the guide in the manifest and derive producer and environment from the bundled artifact. Update the companion entry point and guide links. Candidate metadata records the tested Core source revision and no released packages. The release owner alone sets a release row after qualifying the exact compiler, Java runtime and Node runtime assets from its named revision. Each package records its version and asset filenames and digests, including required Java metadata. The builder requires matching local bytes for every named asset in a release row. A consumer verifies the archive offline against an independent digest and may additionally check the exact Core assets. Byte checks establish equality with the row. Release qualification separately establishes provenance and behavior.

**Verification plan:** Run historical byte-reproducibility and tamper tests. Test exact v3 members, metadata, link targets, repeated builds, wrong pin, changed manifest and inventory, verification without Git or assets, invalid release records and all-or-none asset matches. Validate the bundled schemas and examples with the existing structural checker and frozen fixture checks. Recompile the archived source with the archived model using the qualified compiler and compare the exact artifact. Check the selected cases against both qualified runtime reports. Scan changed tracked content for private details and report the local archive and Git pins with remaining release checks.

**Plan review:** Agent technical review: [Edition 3 plan review](#edition-3-plan-review).

**Approvals:** Not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

[Owning feature](../../roadmap/features/public-package-release.md) · [Contract cutover design](../../docs/internal/public-contract-normalization.md)

## Implementation

Discovery and scope are recorded above. The plan review narrowed links to bundled current inputs and separated asset checks from release qualification.

The command now reads an explicit commit for edition 3 and checks the bundled contract inputs against the compatibility revision. The candidate row, guide, and archive tests are in place. Historical reproduction passed before candidate verification.

## Evidence

- Archive inputs at `117494fe3c5b18bde9ed72b4da1a79c663f67155` produced `sha256:5e2ca73cc56e1d36005928f8c00a83b47863ede4001a8283e725a211be9cb134`. Repeated builds matched. The candidate row names Core `94bdbb7` and has no release asset pins.
- All seven companion tests passed. They cover v1 and v2 reproduction, v3 byte identity, the exact inventory, all ten guide links, offline verification, malformed metadata and complete release asset pins. Edition 2 also matched the previously verified release download byte for byte at `sha256:f073feaefe99e9dfe3b1dd818347f97cdca46c22a780bfb7e70992913aceac04`.
- The bundled schemas validated all six example documents. The compiler rebuilt the archived artifact byte for byte at `sha256:fa5d170dc989e4cc59695ba84e1a8b68648599f07914a2843ad2fe671ef6605b`. The Java and Node packaged verification paths matched all three archived outcomes, traces, ordered calls and consumer reports. All ten schema and example members were identical in the final candidate and the qualified inputs.
- The scope and privacy scan passed. Redline reported gray in shadow mode with zero boundary violations. The actual Java Maven ZIP inventory contains the runtime JAR and both the runtime and parent POMs.

## Result review

Agent technical review: Independent edition 3 result review.

Reviewed revision: `117494fe3c5b18bde9ed72b4da1a79c663f67155`.

Verification adequacy: Passed after disabling Git replacement objects, binding each release role to its versioned asset filename, explaining the lost-response fixture and correcting the companion navigation. Candidate checks and unchanged historical builds passed. The release owner still selects and qualifies the exact versioned assets, commits their compatibility row, and verifies the resulting archive pin before publication.

## Edition 3 plan review

Independent review against `94bdbb7` confirmed the committed-input and independent-pin approach. It flagged obsolete spec links and the risk of treating file digests as qualification. The plan now uses archive-local references and assigns qualification to the release owner. Edition 3 is a source candidate in this delivery.
