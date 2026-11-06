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

## Semantic references for the guide

Supply the [Keir 1 language reference](https://github.com/rore/keir-core/blob/0cae8d4417e19882e4355960b168f49d092f18e6/docs/specs/language.md) for expression and type rules, and the [Keir 1 model reference](https://github.com/rore/keir-core/blob/0cae8d4417e19882e4355960b168f49d092f18e6/docs/specs/domain-model.md) for hooks, capabilities and protected arguments. These online links are pinned to the matching specification revision. The [schemas in the guide](customer-authoring-guide-v3.md#follow-up-example) check document structure and are available inside the archive.

## Record release assets

For a release, record the qualified Core revision and each package’s `version` and asset pin. The `assets` object uses the release filename as its key and a `sha256:<hex>` pin as its value. Use `keir-compiler-cli-<version>.zip`, `keir-java-runtime-maven-<version>.zip`, and `keir-runtime-node-<version>.tgz`. The Java Maven ZIP includes its POMs. Commit the record with `status` set to `release`. Build with `--core-assets <directory>` to check all three files. Consumers can pass the same option to `verify` when they also have the Core downloads.

The digest check establishes byte identity. The [release checklist](../releasing.md) covers asset provenance and installation checks.

## Earlier editions

Use `--version 1` or `--version 2` with the matching historical pin. Their build and verification commands still use the original committed inputs. See the [edition 2 release](https://github.com/rore/keir-core/releases/tag/authoring-v2) and its [pinned guide](https://github.com/rore/keir-core/blob/74d128f04a5fd76d744e3b7705f6614e64846b38/docs/agents/customer-authoring-guide-v2.md).

## Edition 4 candidate

The [Keir 2 authoring guide](customer-authoring-guide-v4.md) and its [workflow expression reference](../specs/workflow-expressions.md) are candidate inputs for edition 4. The archive requires the matching `/2` schemas, a shared verified example, and pins for the exact Core 0.2.0 packages. Keep using edition 3 with the released Keir 1 packages.

Bundle the guide, reference, four linked schemas and the linked inventory example files at their repository-relative paths so the links resolve inside the extracted archive. Use the compiler-owned source, model, artifact and scenarios. Record the archive input revision separately from the Core build revision, and verify the compiler ZIP, Java Maven ZIP and Node tarball against their release digests before pinning the companion archive. [Inventory example](../specs/workflow-expressions.md#inventory-example) · [Shared fixture index](../../conformance/keir-2/README.md)
