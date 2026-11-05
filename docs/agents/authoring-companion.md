<a id="build-and-verify-the-effect4-authoring-companion"></a>
# Build and verify the authoring companion

Edition 3 packages the [customer authoring guide](customer-authoring-guide-v3.md), the Keir 1 schemas, and a follow-up example with three simulated outcomes. The guide’s links work inside the extracted archive.

## Build the archive

Use Git and Python 3.11 or later. From the repository root, select a commit that contains the edition 3 inputs. The builder reads every input from that commit and prints the archive’s SHA-256 pin.

```sh
python scripts/authoring-companion.py build keir-authoring-v3.zip --version 3 --revision "$(git rev-parse HEAD)" --core-assets path/to/core-assets
```

The manifest distinguishes the commit that supplied the archive inputs from the Core revision recorded in the [compatibility record](authoring-companion-v3-compatibility.json). For a source candidate with an empty package list, omit `--core-assets`.

## Verify and load the guide

Get the archive pin from the trusted handoff or the release checksums. Substitute that value for `sha256:<hex>`.

```sh
python scripts/authoring-companion.py verify keir-authoring-v3.zip --version 3 --sha256 "sha256:<hex>"
```

The verifier checks the pin before reading the archive. Load the guide named in `manifest.json` and supply your application's exact model, scoped tools and policy separately. The host owns permissions, approval and activation.

## Record release assets

For a release, record the qualified Core revision and each package’s `version` and asset pin. The `assets` object uses the release filename as its key and a `sha256:<hex>` pin as its value. Use `keir-compiler-cli-<version>.zip`, `keir-java-runtime-maven-<version>.zip`, and `keir-runtime-node-<version>.tgz`. The Java Maven ZIP includes its POMs. Commit the record with `status` set to `release`. Build with `--core-assets <directory>` to check all three files. Consumers can pass the same option to `verify` when they also have the Core downloads.

The digest check establishes byte identity. The [release checklist](../releasing.md) covers asset provenance and installation checks.

## Earlier editions

Use `--version 1` or `--version 2` with the matching historical pin. Their build and verification commands still use the original committed inputs. See the [edition 2 release](https://github.com/rore/keir-core/releases/tag/authoring-v2) and its [pinned guide](https://github.com/rore/keir-core/blob/74d128f04a5fd76d744e3b7705f6614e64846b38/docs/agents/customer-authoring-guide-v2.md).
