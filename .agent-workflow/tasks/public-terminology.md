# Separate public terminology from compatibility identifiers

<!-- agent-workflow:start -->
**Outcome:** Readers can learn and use Keir without learning internal compatibility aliases.
**Target:** Keir Core documentation
**Scope:** Public documentation indexes, specifications, assistant guides, compiler and runtime guides, examples, schemas and conformance documentation.
**Constraints:** Preserve the current reader-docs draft, existing anchors, exact wire values, file paths, and package pins. Daymark changes and package requalification are separate.
**Completion criteria:** Public paths explain Keir IR, the DCM and single-write behavior. Exact identifiers and selector commands remain available in implementation reference. Links and anchors resolve, and the existing companion archive remains reproducible with its exact guide.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"Readers can learn and use Keir without learning internal compatibility aliases.","scope":"Public documentation indexes, specifications, assistant guides, compiler and runtime guides, examples, schemas and conformance documentation.","constraints":"Preserve the current reader-docs draft, existing anchors, exact wire values, file paths, and package pins. Daymark changes and package requalification are separate.","completion_criteria":"Public paths explain Keir IR, the DCM and single-write behavior. Exact identifiers and selector commands remain available in implementation reference. Links and anchors resolve, and the existing companion archive remains reproducible with its exact guide."}
**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** Module, schema, examples and conformance READMEs fall outside the Blue documentation paths. Several reader paths and compatibility anchors are affected.
**Discovery:** Public indexes expose wire IDs as terminology. Exact selector invocations and compatibility material are mixed into examples. The Node runtime README is packaged and the first authoring companion pins the original guide bytes.
**Material assumptions:** The immutable package artifacts are independently pinned. If a proposed wording change touches those inputs, retain the exact existing artifact and use a separate new source version or package qualification.
**Plan:** Introduce a central implementation reference and move exact selector commands there. Preserve public paths and add explicit IDs when renaming headings. Rewrite public introductions and indexes in product terms. Keep necessary exact schema, fixture and compatibility material in labeled engineering reference sections. Add a new authoring guide source with no profile aliases and keep the archived guide and Node package inputs pinned.
**Verification plan:**
Reader terminology -> audit public Markdown in scope and document intentional machine-reference exceptions.
Preserve anchors and commands -> check public links including explicit IDs, compare retained commands, and verify the checked examples remain selected by their existing paths.
Immutable artifacts -> compare tracked packaged source bytes with the reviewed versions and rebuild and verify the exact companion digest offline.
**Plan review:** Agent technical review: [Plan review](#plan-review).
**Approvals:** Not required at this risk level.
**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Preserve the existing public documentation draft at the shared checkout. The terminology changes are limited to Markdown.

The public index and onboarding pages use Keir for the language, the DCM and single-write behavior. The implementation appendix holds exact compiler commands and wire identifiers. Specialist documents retain exact examples and historical measurements where the label identifies them as implementation reference. The bundled Node README and the first companion guide retain their existing bytes. A separate guide source carries the new terminology.

## Plan review

The independent reviewer passed the plan. Compare the existing companion against its previously reviewed archive digest. Preserve the exact Node package README and check anchor compatibility against both committed content and the reader-docs draft.

## Evidence

The audit checked 369 local links in 44 public Markdown pages, retained all 158 committed anchors on changed pages and scanned 11 public reader paths for internal profile aliases. The two documented hosts passed the same 11 cases with the pinned fixture. The first authoring companion rebuilt byte for byte against its independent archive pin. The Node package README, example lockfile and first guide source are unchanged. The new guide source is separate from the archive and awaits its own package review. Redline reported Gray with no boundary violations.

## Result review

Agent technical review: [Independent result](#independent-result)

Reviewed revision: `36ab1a1` plus the documentation candidate with content fingerprint `0e964686433080ada6e14719823a44aa401fa70731c1f7ad14f4260b0140b397`.

Verification adequacy: Pass. Independent review checked terminology, committed anchors, unchanged packaged inputs and preservation of the current host contract. The authoring companion was rebuilt against a prior independent archive pin and the example commands passed.

### Independent result

The reviewer confirmed two corrections: an eligible call dispatches at most once, and a trusted digest is one way to check the artifact in host code. Reader routes use product terminology, while labeled implementation references retain exact identifiers. Both packaged source exceptions remain unchanged.

## Follow-up terminology correction

The maintained documentation calls the language Keir. The README introduces Keir as a controlled extension intermediate representation. The revised guide is a separate source draft with SHA-256 `22f3aba081b5653f7f5d4fff8861afe606c840ce56d9b26d7464ffc08ee8ab1d`.

The later compatibility audit moved historical contract details and compiler selectors to the linked implementation references. Current rules remain in the specification. Stable anchors, fixture paths, archive names and the six literal schema IDs retain their exact spelling. The packaged Node README and the first companion guide retain their pinned bytes. The Node tarball used for isolated verification matched the example lockfile, including its README. The first companion archive matched its trusted SHA-256.

The final audit covered 44 public pages, 420 local links and 164 committed anchors. The isolated compiler reproduced four current and historical artifacts byte for byte. The current Java and Node hosts returned identical reports across all 11 cases. The single-document schema check passed. The independent reviewer found two issues and passed the recheck after the current DCM semantic constraints and the earlier artifact-envelope link were corrected. Agent Workflow was clean and Redline reported Gray.

Final documentation candidate at `36ab1a1` with content fingerprint `bc70fcd5159f9a0f994fcda35de517c525dafd25f228475f0e94631946446843`.
