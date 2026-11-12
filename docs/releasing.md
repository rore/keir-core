# Releasing Keir

[GitHub Releases](https://github.com/rore/keir-core/releases) is Keir's package distribution channel. The default branch is where changes land. Create a release when a consumer needs a pinned package, or when a set of fixes or changes is ready to deliver.

## Prepare a release

1. Pick a commit on main that passed Core CI. Use `v<major>.<minor>.<patch>` for Core. The matching [authoring companion](agents/authoring-companion.md) can ship as an asset on the Core release. Use `authoring-v<edition>` when the companion ships independently.
2. Build and smoke-test the package assets using the commands below. Include their licenses, notices and `SHA256SUMS`. Record the supported Keir and DCM formats, prerequisites and install steps in the release notes. Build and verify any companion with its existing command and publish its checksum alongside the archive.
3. Create a GitHub draft against the selected commit. Download its Core assets to a separate directory and run the verifier with the staged build's checksum file. Publish after the comparison passes. If shipped package bytes need to change, cut a new version.
4. Consumers pin the release, asset name and SHA-256 digest. Daymark can keep a verified copy for offline setup. Run the consumer's checks when it adopts the release, and verify a reference host before claiming that it supports the release.
5. When the release is intended for Daymark, update the [running demo](https://github.com/rore/keir-demo-app/blob/main/docs/running.md) after the adopting revision passes its checks. Rehearse any database migration on a copy and keep a verified backup and a rollback path. Confirm that the demo's usual URL serves the checked build with the pinned Keir version before marking the demo rollout complete.

## Build and verify Core assets

Use Python 3.12 or later, JDK 21, Node.js 24, npm and Git. Set `REVISION` to the full source commit, and `VERSION` to the release version declared by the compiler, Node package and teaching host. Run from the repository root.

```sh
python -B scripts/release-core.py --revision "$REVISION" --version "$VERSION" \
  --output ".tools/release-$VERSION" --maven-repo .tools/maven/repository --npm-cache .tools/npm-cache

python -B scripts/verify-core-release.py --version "$VERSION" --assets ".tools/release-$VERSION" \
  --smoke-revision "$REVISION" --maven-repo .tools/maven/repository --npm-cache .tools/npm-cache
```

Use a new output directory. The builder reads source from Git, builds in scratch, and checks the Maven packaging inputs and compiler dependencies against the reviewed notice inventory. Review packaging or dependency changes and update that inventory before building. Add `--offline` to either command when the Maven distribution and dependency caches are populated. To reproduce an existing release byte for byte, use the same build tools and compression versions.

The smoke test compiles the [follow-up extension](../examples/node-host/follow-up/source.yaml) with the packaged compiler. When the selected example model is `keir-dcm/3`, it also compiles the [pre-operation decision](../examples/node-host/pre-operation/source.yaml). It checks Maven coordinates, resolves dependencies from the shipped POMs, and runs each artifact in the packaged Java runtime and the installed Node tarball. Both hosts must produce the exact expected output for each example.

The follow-up cases cover a confirmed write, a skipped write and an uncertain write. The decision cases cover ALLOWED, DENIED and BLOCKED, with the simulated native-attempt count observed separately. These counters do not prove durable native persistence. A `/2` model retains the original follow-up-only smoke path, so historical releases use their matching examples. A release requires the actual integrated package smoke to pass; verifier unit tests alone do not qualify its packages.

For Core 0.4.0, the verifier additionally compiles all four guarded-result examples with the shipped CLI and requires exact fixture byte identity. The shipped Java runtime with dependencies resolved from its packaged POMs and the installed Node tarball must match 15 new complete scenario outcomes plus five retained event/decision cases covering producers 0.2.0, 0.3.0 and 0.3.1. This includes true/skipped results, optional presence, READ decisions, a later handler, rejection/transient/unknown effects and confirmed evidence followed by expiry. Ordered calls/arguments, original context, full traces and receipts are checked, not just values. Smoke runs in an isolated temporary directory outside the source checkout.

The opt-in `python -B scripts/test-verify-core-release.py --source-probes` check exercises that probe code against local compiler/runtime builds and rejects wrong expectations. It does not install packages or replace the committed-source builder and actual installed-package smoke. Companion builds require their recorded compatibility inputs; the [catalog](agents/authoring-companion.md) identifies the matching edition. Preserve earlier archives and records.

For the draft download check, put only the five Core assets in the download directory. Use the original build's checksum file as the trusted input.

```sh
gh release create "v$VERSION" ".tools/release-$VERSION/"* \
  --draft --target "$REVISION" --notes-file release-notes.md

gh release download "v$VERSION" --dir .tools/release-download \
  --pattern 'keir-compiler-cli-*.zip' --pattern 'keir-java-runtime-maven-*.zip' \
  --pattern 'keir-runtime-node-*.tgz' --pattern LICENSE --pattern SHA256SUMS

python -B scripts/verify-core-release.py --version "$VERSION" --assets .tools/release-download \
  --checksums ".tools/release-$VERSION/SHA256SUMS"
```

## Public readiness

Private releases use the same package and verification process. Before making the repository public, review the reachable history, tags, published releases, assets and documentation for private data. Verify licensing for bundled dependencies and test the documented install paths in a clean environment. Confirm that the public package versions, supported contracts and known limits are documented. Changing repository visibility is a separate decision.
