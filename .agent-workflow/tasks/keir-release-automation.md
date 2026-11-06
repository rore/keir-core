# Automate Keir release preparation

<!-- agent-workflow:start -->
**Outcome:** Prepare and verify a Keir Core release with repeatable commands.

**Target:** keir-core

**Scope:** Build package assets and checksums from a pinned commit. Verify the assets in an isolated consumer and compare a second copy downloaded from a draft release. Reuse the existing companion builder.

**Constraints:** Preserve the published archive layout, license notices, exact source and dependency identity, and manual publication decision. Keep builds isolated. This work changes no hosted workflow, release tag or repository visibility.

**Completion criteria:** The command reproduces all Core 0.2.0 release bytes. A package smoke check runs an artifact on the packaged Java and Node runtimes. Verification rejects changed or missing assets.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Prepare and verify a Keir Core release with repeatable commands.","scope":"Build package assets and checksums from a pinned commit. Verify the assets in an isolated consumer and compare a second copy downloaded from a draft release. Reuse the existing companion builder.","constraints":"Preserve the published archive layout, license notices, exact source and dependency identity, and manual publication decision. Keep builds isolated. This work changes no hosted workflow, release tag or repository visibility.","completion_criteria":"The command reproduces all Core 0.2.0 release bytes. A package smoke check runs an artifact on the packaged Java and Node runtimes. Verification rejects changed or missing assets."}

**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Packaging is local, but an incorrect archive could omit notices or change what consumers install.

**Discovery:** A local replay twice reproduced the published assets. Core packaging still depends on scratch code and review-specific license inputs. The companion already has a supported build and verify command.

**Material assumptions:** The published Core 0.2.0 checksums and third-party inputs are authoritative. If the replay differs, compare archive contents before changing any pin or treating the build as qualified.

**Plan:** Promote the Core packager and reviewed third-party inputs into the repository. Reject unreviewed dependency changes. Build in fresh scratch from a full Git commit. Verify a standalone asset set and an optional second copy against trusted local checksums. Exercise the installed packages with the existing follow-up example.

**Verification plan:** Rebuild the shipped package set and compare every digest. Test changed, missing and extra assets. Run a Java and Node package smoke test with an artifact from the packaged compiler.

**Plan review:** Independent review identified the need to pin Maven inputs that can alter the shaded compiler outside the dependency tree and require the reviewed notices in the verifier. The implementation pins the POMs and wrapper files, checks dependency hashes, verifies the required notice bytes, and rejects unsafe or duplicate ZIP entries.

**Approvals:** Local automation and validation are authorized.

**State:** Ready for review
<!-- agent-workflow:end -->

## Verification

The final Windows offline run rebuilt all five published Core 0.2.0 files byte for byte. The verifier checked the release manifest, compiled the follow-up rule, and observed the expected confirmed, skipped and uncertain outcomes through the packaged Java and Node runtimes. The four focused verification tests passed. The builder also rejected a changed dependency, unreviewed Maven inputs, invalid ZIP names and an existing output directory. The checked-in commands prepare and verify assets. Publishing still uses GitHub Releases.
