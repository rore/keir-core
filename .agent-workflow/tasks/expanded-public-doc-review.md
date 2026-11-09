# Expanded public documentation review

<!-- agent-workflow:start -->
**Outcome:**
A new developer can understand Keir, install verified release assets, run and change an example, and follow Daymark's current customization journey without obsolete guidance.

**Target:**
Keir Core and Daymark public documentation and verified consumer examples.

**Scope:**
Public README, contributing, setup, installation, tutorial, integration, reference, testing and historical landing prose in both repositories; minimal consumer-installation examples and their isolated checks. Coordinate implementation fixes with their existing owners.

**Constraints:**
Preserve the current README-to-quickstart-to-embedding-to-specification structure and user-approved opening. Preserve Keir semantics, host authority, published packages and historical archive bytes. No architectural redesign, integrity bypass, fictional commands or unverified passing-test claims. Do not modify runtime/compiler/app implementation, CI or roadmap ownership. Use isolated tools, synthetic data and project-local caches; do not change installed tools, global configuration or live applications/data. Preserve unrelated work and public privacy.

**Completion criteria:**
All 18 expanded-review findings are checked against current revisions and either fixed with appropriate command/UI/link evidence or assigned with a concrete engineering dependency. Released consumer installation and the literal quickstart work; current and historical evidence are separated; Daymark's license, setup, authoring, privacy and implementation links agree with code. Existing source authority and immutable releases remain intact.

**Requirement baseline:**
{"source":"User-supplied expanded documentation review; reviewed Core b038bf4 and Daymark d1eaea9","outcome":"A new developer can understand Keir, install verified release assets, run and change an example, and follow Daymark's current customization journey without obsolete guidance.","scope":"Public README, contributing, setup, installation, tutorial, integration, reference, testing and historical landing prose in both repositories; minimal consumer-installation examples and their isolated checks. Coordinate implementation fixes with their existing owners.","constraints":"Preserve the current README-to-quickstart-to-embedding-to-specification structure and user-approved opening. Preserve Keir semantics, host authority, published packages and historical archive bytes. No architectural redesign, integrity bypass, fictional commands or unverified passing-test claims. Do not modify runtime/compiler/app implementation, CI or roadmap ownership. Use isolated tools, synthetic data and project-local caches; do not change installed tools, global configuration or live applications/data. Preserve unrelated work and public privacy.","completion_criteria":"All 18 expanded-review findings are checked against current revisions and either fixed with appropriate command/UI/link evidence or assigned with a concrete engineering dependency. Released consumer installation and the literal quickstart work; current and historical evidence are separated; Daymark's license, setup, authoring, privacy and implementation links agree with code. Existing source authority and immutable releases remain intact."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-onboarding-readiness"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Large

**Reason:**
Module/example documentation is gray under the current policy; installation examples cross package and tool boundaries. Two repositories and independently owned engineering fixes need coordinated acceptance.

**Discovery:**
The expanded review identifies 18 findings. Existing structure and source/DCM references are sound. The quickstart can mismatch locally repacked tarball integrity; consumer installation is missing. Documentation and UI claims must be checked against their named published revisions, with pending implementation changes qualified separately.

**Material assumptions:**
Published Core 0.3 assets remain immutable and available through the existing release channel. Their qualified local copies can support isolated consumer checks. The quickstart setup and Daymark Java/CI failures need developer-owned investigation; do not document a bypass or describe an unestablished cause. Current Studio boundary may remain a proposal rather than an independently packaged product. Reconcile against actual implementation and owner evidence.

**Plan:**
1. Obtain an independent bounded plan review. Inspect actual public paths and current source before edits; coordinate distinct file ownership and engineering dependencies with the manager.
2. Verify consumer installation using released compiler, Node tarball and included Java Maven repository outside either source repository. Reuse existing package/examples and write only the minimum demonstrated procedure and supporting files.
3. Correct Core navigation, annotated example/model journey, model-specific consumer description, model fingerprint procedure, current test evidence and conformance orientation. Preserve semantic references and historical versions.
4. Reconcile Daymark current guides, settled choices, license, implementation/proposal separation, authoring, setup, UI labels and provider privacy against current source and isolated evidence. Keep compatibility assets and useful history.
5. Review both contributions, run focused literal-command/UI/link/privacy checks, and repeat the first-run acceptance after the owner fixes setup. Deliver exact revisions and unresolved engineering evidence to the manager for integration and independent result review; archive isolated work recoverably afterward.

**Verification plan:**
- Released installation -> compiler plus Node/Java minimal host run outside source repos with verified package bytes.
- Literal quickstart -> clean checkout setup, both hosts and edited-title compile/run after durable setup fix.
- Current Daymark journey -> current source/labels and isolated synthetic-data walkthrough; fresh CI evidence from the implementation owner.
- Accuracy and navigation -> 18-item disposition, changed-page links/anchors and privacy scan, immutable asset/input checks.
- Scoped delivery -> diff review, task-record/Redline checks and independent result review; no broad suite repeats without affected behavior.

**Plan review:**
Agent technical review: expanded_doc_plan approved the bounded approach with concrete-path and engineering-dependency clarifications. See the Plan review notes below.

**Approvals:**
Not required at this risk level. The user requested the expanded public documentation pass.

**Exceptions:** —

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Core baseline b038bf42baa4b0d2d873e88df2409e5ba123bbeb; published Daymark baseline d1eaea993ac705a6bcb9cfc090db03a23299e7b3. Published behavior and pending implementation changes require distinct evidence. The owning roadmap feature is public-onboarding-readiness.

Concrete owned paths: Core README.md, docs/README.md, docs/quickstart.md, docs/embedding.md, docs/testing.md, docs/specs/runtime.md, docs/agents landing pages and historical guide headers, conformance/keir-3/README.md, new docs/installation.md and an annotated walkthrough, plus component README links. Daymark README.md, CONTRIBUTING.md, docs/README.md, scope.md, native-task-app-design.md, studio-embedding.md, authored-extension-drafts.md, ai-authoring.md, read-extension-lifecycle.md, running.md, current walkthrough/privacy prose, src/README.md, keir/README.md and the actual archived canvas link. Resolve actual filenames before editing. Minimal installation examples belong in the installation guide unless separate files prove necessary.

Engineering dependencies are separately owned: Core's developer handles durable local example setup shared with CI and the real DCM fingerprint procedure/tool gap; Daymark's developer investigates Java qualification and obtains current CI evidence under Studio release gates. Documentation does not edit scripts, lockfiles, CI or implementation to hide these failures. Final accepted Studio UI labels/screenshots remain an owner-supplied dependency.

## Plan review

The independent reviewer approved preservation of structure, immutable released bytes, host authority and isolated consumer verification. Added explicit file targets and dependency owners as requested. Released installation must actually work outside source repositories; existing repository-assisted release smoke is supplementary, not consumer-installation acceptance. Preserve package provenance and account for teaching-host relative imports. Verify the literal quickstart after its owner fixes setup, and use only supported fingerprint interfaces/procedures. Daymark claims are pinned to published d1eaea9 until a later revision is accepted. Cached verification records its limitation rather than claiming empty-cache downloads. Signed-out checks await the release owner's visibility decision.

## Evidence

- Consumer installation: released compiler, Node tarball, Java Maven repository and edition-5 companion verified against their published SHA-256 pins. Compiled guard artifact matches sha256:8f0d93951b2026ccd82aaaf3ade53fc77fd755a5ed61f2b69719f8b9b8a5d0a1. Separate Node and Java consumers passed COMPLETED ALLOW/DENY and empty-effect assertions outside both source repositories. npm used normal registry resolution; Maven used cached third-party dependencies. Empty-cache downloads and anonymous access remain unverified.
- Daymark: isolated dependency installation, TypeScript/Vite build, repeat seed and reset with verified backup passed at the published baseline. No service, browser, provider or Java qualification was run for this prose pass.
- Core: release CI run 42576060993 independently confirmed successful at b2329440dd2a25acc547f3ebfea214748d912022. Final changed-page scan resolved 164 Core and 89 Daymark local links and anchors; privacy and whitespace checks passed. The approved README opening and diagram content are unchanged. Workflow validation is clean; Redline classifies the component and corpus README paths as gray with no boundary violation or required checkpoint.
- The source setup helper is independently reviewed at 47306dfa059f1a5066b3cd6efd4756cbfd76e231 and integrated at 41a11a510518e38822a0b2a9af7a325e69c9ceec. A fresh archive of that source plus the final guide passed the literal Windows setup, source compiler/runtime builds and all six Node/Java follow-up, decision and edited-title runs. Installed Node files match source, and only the local archive integrity changes in the lockfile. Third-party caches were seeded. [Hosted Linux CI](https://github.com/rore/keir-core/actions/runs/42619792483) passed at cbc256cb342eb8b00a4774e7aced5353dc5ca334, including shared setup, the independent Node host and retained/current Java/Node scenario comparison; its job is 129168658429. This is separate evidence from the six-flow Windows tutorial replay.
- The practical fingerprint recipe passed independent review and six matching released-compiler-qualified vectors plus ten rejection cases. It remains a standalone copyable recipe, with model validation performed by the compiler.
- Overall acceptance still needs fresh accepted Daymark CI/UI evidence and final UI documentation. Review assertions are not test results.
- Fingerprint investigation evidence: the Core developer's released-0.3 compiler report contains six accepted new/changed/Unicode-control model variants, including all 32 control characters. Pretty and reordered forms produce matching canonical model digests; stale fingerprints are rejected. That investigation now supports the reviewed standalone recipe; it does not establish a new public compiler/runtime helper.

## Result review

Agent technical review: expanded_doc_plan independent result review.

Reviewed revision: Core b038bf42baa4b0d2d873e88df2409e5ba123bbeb plus this documentation diff; Daymark d1eaea993ac705a6bcb9cfc090db03a23299e7b3 plus its documentation diff.

Verification adequacy: sufficient for the bounded documentation increment. Released-install evidence, model-specific consumer wording, implementation links, Daymark source labels and privacy statements were checked. The review required removing private operational narrative from this record; that cleanup happened before commit or publication. Existing implementation and immutable released bytes are unchanged. Overall onboarding acceptance remains open for the separately owned engineering and UI evidence below.

## Expanded-review disposition

Version-neutral followup: general installation and entry guides now select explicit releases through their metadata rather than repeat a current package version. The exact qualified walkthrough is preserved under docs/examples/installation-core-0.3.0.md with unchanged code blocks and corrected relative links. Core checksums and companion checksums remain separate; runtime installation does not require a companion. Independent technical review approves this followup after correcting the extraction-directory wording. The quickstart now uses the reviewed shared setup command; its final Windows replay passed.

Changed parameterized instructions were checked with retained released assets: Core checksum selection passed and invalid version, duplicate, missing and mismatched checksum cases were rejected. Compiler path substitution reproduced the qualified artifact, the Node command installed the verified tarball with isolated cached dependencies, and Java extraction/POM selection matched the shipped repository. The final followup scan resolved 169 local links and anchors; privacy and whitespace checks passed. Downloads used retained copies, with no anonymous-network or empty-cache claim. Unchanged released-host qualification was not repeated. Workflow blocking predicates pass; its original combined documentation/record commit produces only a nonblocking commit-order advisory. Redline remains gray with no boundary violation or required checkpoint.

| Item | Documentation result | Acceptance |
| --- | --- | --- |
| 1. Literal quickstart | Reviewed setup shared with CI; no integrity bypass. | Fresh Windows source-build replay passed all six host flows; Linux setup, host and parity CI passed. |
| 2. Consumer installation | New installation guide, compiler and dual-runtime consumer proof; linked from entry and component pages. | Passed within the stated cache/download boundary. |
| 3. Verification currency | Current release/source/CI and `/3` commands separated from historical counts. | Exact release CI independently confirmed. |
| 4. License and settled choices | Apache 2.0, React/Fastify/SQLite reflected in Daymark contributing and scope. | Source checked; notices preserved. |
| 5. Studio proposal | Proposal status explicit; integration links point to actual source. | Documentation reviewed. |
| 6. Authoring path | Obsolete advice/manual path replaced by current Daymark and Core guides. | Links and semantics reviewed. |
| 7. Decision consumer | Teaching-model field limits and another host's complete-result validation responsibilities explicit. | Checked against both teaching consumers. |
| 8. Daymark story | Short request-to-execution journey and ownership explanation added. | Source checked; current browser evidence remains separate. |
| 9. Annotated Core tour | Existing follow-up source and readable model explained together, with Daymark implementation links. | Source/model checked. |
| 10. Canonical fingerprint | Standalone copyable recipe linked from model identity and embedding; no invented exported helper. | Six qualified-vector matches, ten rejection cases and independent review passed. |
| 11. Assistant entry | README entry and host-builder input/output checklist added. | Links reviewed. |
| 12. Current fit | Positive examples with step/call/list/write boundaries and normative link. | Current language checked. |
| 13. Current corpus | `/3` README added with version pairing, layout and commands. | Runner arguments checked. |
| 14. Daymark currency | Scope, native design and source/assets READMEs reconciled; history and compatibility retained. | Source checked. |
| 15. UI and prerequisites | Published-source activation labels and replacement semantics aligned; native/simulation/live requirements separated. | Open: fresh accepted UI evidence after implementation fixes. |
| 16. Reproducible setup | Clone/cd, port/data, seed/reset and focused troubleshooting added. | Isolated install/build/repeat seed/reset passed. |
| 17. Provider privacy | Project names/IDs, provider retention and scoped no-provider behavior explicit. | Supported pairs and discovery source checked. |
| 18. Historical landings | Editions 1–4 version/index notices and archived canvas link corrected. | Link scan passed; pinned archives unchanged. |

Daymark's reported CI failure remains an implementation-owner investigation. Earlier passing totals do not establish current acceptance. Signed-out landing/release/cross-repository checks await the release owner's visibility decision.
