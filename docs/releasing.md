# Releasing Keir

[GitHub Releases](https://github.com/rore/keir-core/releases) is Keir's package distribution channel. The default branch is where changes land. Create a release when a consumer needs a pinned package, or when a set of fixes or changes is ready to deliver.

## Prepare a release

1. Pick an exact commit on main and a new version. Use `v<major>.<minor>.<patch>` for a Core release and `authoring-v<edition>` for an independently shipped [authoring companion](agents/authoring-companion.md). An authoring-only release describes the Core versions it works with.
2. Build from a clean checkout. Attach the packages in scope, their licenses and required notices, and `SHA256SUMS`. A Core release contains the compiler CLI, Node runtime, Java runtime and Maven metadata, and a link to the matching authoring companion. Record the supported Keir and DCM formats, prerequisites, and installation steps in the release notes.
3. Install the exact downloads in a separate checkout or consumer project. For Core, compile a rule once and check that both packaged runtimes agree. For the companion, verify the archive against the expected pin and check the example. Verify a reference host before claiming that it supports the release. Record the tested commit and any limits.
4. Create a draft against that commit. Compare GitHub's asset digests with the verified files before publishing. If shipped package bytes need to change, cut a new version.
5. Consumers pin the release, asset name and SHA-256 digest. Daymark can keep a verified copy of the exact dependency so its local setup doesn't require a GitHub download. Record where each copy came from.

## Public readiness

Private releases use the same package and verification process. Before making the repository public, review the reachable history, tags, published releases, assets and documentation for private data. Verify licensing for bundled dependencies and test the documented install paths in a clean environment. Confirm that the public package versions, supported contracts and known limits are documented. Changing repository visibility is a separate decision.
