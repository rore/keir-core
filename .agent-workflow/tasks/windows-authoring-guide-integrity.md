# Windows assistant-guide checkout integrity

<!-- agent-workflow:start -->
**Outcome:**
The updated demo keeps its pinned assistant guide byte-identical on Windows, so configured AI authoring remains available.

**Target:**
keir-demo-app checkout attributes and focused guide verification; core owns this record.

**Scope:**
Demo .gitattributes and one focused checkout regression test; this core Work Record; data-preserving local delivery.

**Constraints:**
Keep the guide, its expected digest and verification logic unchanged. Preserve provider settings, saved rules and user data. No global Git/tool changes or paid provider calls.

**Completion criteria:**
Forced core.autocrlf=true checkout preserves the pinned guide bytes; its loader succeeds; configured authoring reports available; saved summaries and simulations work; the live demo serves the verified build with database rows and settings preserved.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"The updated demo keeps its pinned assistant guide byte-identical on Windows, so configured AI authoring remains available.","scope":"Demo .gitattributes and one focused checkout regression test; this core Work Record; data-preserving local delivery.","constraints":"Keep the guide, its expected digest and verification logic unchanged. Preserve provider settings, saved rules and user data. No global Git/tool changes or paid provider calls.","completion_criteria":"Forced core.autocrlf=true checkout preserves the pinned guide bytes; its loader succeeds; configured authoring reports available; saved summaries and simulations work; the live demo serves the verified build with database rows and settings preserved."}

**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}

**Risk:** Elevated
**Complexity:** Simple
**Reason:**
Checkout attributes and test paths are gray under the shared policy. This changes byte-preservation configuration, not guide trust or application contracts.

**Discovery:**
Demo aca2068 builds and passes hosted qualification, but its Windows checkout expands the pinned guide from 6481 to 6522 bytes by adding 41 CR characters. loadAuthoringInstructions returns null. Existing attributes protect editions 3–5 but omit edition 7. Core ce31bf6 and the demo consume the independently published 0.3.1 packages.

**Material assumptions:**
The tracked guide blob already matches the pinned digest. Verify its raw hash; if it differs, stop and investigate the package rather than weakening integrity checks. Existing data remains schema 14; rehearsal already preserves all 22 tables and 195 rows without provider calls.

**Plan:**
Replace the three edition-specific guide attributes with one vendor/customer-authoring-guide-v*.md -text rule. Add an isolated Git-index checkout regression using core.autocrlf=true and the existing guide loader. Do not change runtime source, guide bytes, checksums, package versions or global configuration. Review this bounded plan, reproduce failure, implement, run focused verification and obtain result review. Deliver the corrected frozen checkout, verify served assets and preserved data, then delete the obsolete deployment checkout.

**Verification plan:**
Pinned guide bytes -> Reproduce the loader failure and CRLF expansion, then require the fresh forced-autocrlf checkout regression to fail with old attributes and pass with the wildcard.
Assistant readiness -> Invoke the actual copied-checkout loader and check configured authoring availability without provider calls.
Saved user journeys -> Open both saved summaries and simulations on an isolated data copy at desktop and narrow widths.
Data-preserving delivery -> Compare live rows/settings with the stopped backup and hash served assets after startup.
Unchanged product behavior -> Reuse the green full qualification of aca2068 and run the existing authoring-agent suite with the new regression.

**Plan review:**
Agent technical review: [independent plan review](#plan-review). The non-implementer confirmed the attribute omission and approved the bounded fix and isolated checkout verification.

**Approvals:**
Not required at this risk level. The user explicitly requested pulling both repositories and updating the running demo; standing routine demo update authorization applies.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Discovery and copied-data rehearsal completed before this checkout defect was identified. Replaced three guide-specific attributes with one matching all numbered guide editions; added the isolated checkout regression. No application source, guide blob, expected digest, dependencies or global configuration changed. The frozen candidate has not replaced the live deployment yet.

## Plan review

Independent clean-context technical review: PASS. The new wildcard preserves the existing trust checks; the temporary checkout must use a separate Git index with core.autocrlf=true and exercise the real loader at that checkout, without changing shared/global configuration. The regression will copy only the loader and its two local imports into a tiny fixture; dependency resolution uses the existing installed runtime package.

## Evidence

The baseline guide loader returned false with 6522 bytes and 41 CRLF pairs; its required size is 6481 bytes. The copied-data browser checks passed for both saved customizations, with zero page errors, server errors or unexpected writes. The exact demo baseline passed [full hosted qualification](https://github.com/rore/keir-demo-app/actions/runs/43009013779).

The isolated fresh-file checkout regression fails with the original attributes (6522 versus 6481 bytes), then passes with the wildcard; its real loader runs from that temporary checkout. All 50 tests in the checkout and existing authoring-agent suites pass. A first test draft did not remove the existing fixture file, so checkout-index reused it; the final regression unlinks the generated fixture guide before materialization and demonstrates the expected failure. No shared index or global Git configuration is changed.

Configured authoring changed from unavailable/AUTHORING_GUIDE_UNAVAILABLE to available with the exact same settings. The repeated copied-data browser journey passes both saved hooks, Summary and Simulation, desktop and narrow layouts, with zero page/server errors or unexpected writes. The smoke helper was corrected to wait for actual example rows rather than assert during asynchronous loading. Read-only comparison confirms schema 14, all 22 tables and 195 rows, and byte-identical provider settings. No paid provider request was made.

Frozen demo working delta against aca2068: .gitattributes SHA-256 3c3acfba889dbe96bcaa63327bfe7834698593775cc34ce670ce7ee23c8456d0; tests/authoring-guide-checkout.test.mjs SHA-256 40649ffe765610e4345f9f5414e5f0843001f33f74d7cd68d59ac78ad02617cb.

## Result review

Agent technical review: independent non-implementer result review PASS for the exact two-file demo delta. The reviewer inspected the isolated index, fresh-file materialization, byte comparison and real loader invocation and found the focused verification adequate. The review correctly leaves live delivery to subsequent operational verification.

Reviewed revision: demo aca2068 plus the two frozen file hashes above. The reviewer noticed a transient modified-status flag on the guide despite identical HEAD, index and working bytes. Refreshing the staged metadata confirms the guide has no diff; only .gitattributes and the regression test are staged.

Verification adequacy: the new test reproduces the specific Windows conversion even on a non-Windows CI worker, then checks the actual loader. Existing authoring tests cover the unchanged trust policy; copied-data readiness and saved-rule browser checks cover this installation. Broad full CI is reused for the unchanged baseline application code. No live provider response has been tested by this deployment task.

## Delivery

Accepted and delivered as demo [2397667](https://github.com/rore/keir-demo-app/commit/2397667293be19900eed790ca47cdd9208c1f5dd). The commit contains only the reviewed attribute change and checkout regression. Both repositories are published on main; the updated local demo runs the pinned Keir 0.3.1 package.

The actual served HTML, JavaScript and CSS match the built deployment byte-for-byte. Live authoring and native execution report available; both saved customizations remain readable. Startup preserved schema 14, database integrity, foreign keys, every existing table/row and exact provider-setting bytes. The old lock was recovered only after confirming its recorded owner had stopped and its port was free. No unrelated process, global tool, configuration, provider/model choice or user-data location changed.

The obsolete deployment checkout was removed after verifying the new process and data. Only the active deployment and compact stopped-state backup are retained. The same desktop/mobile Summary and Simulation journeys passed on the isolated data copy. No paid provider turn was performed; that remains outside these delivery checks.
