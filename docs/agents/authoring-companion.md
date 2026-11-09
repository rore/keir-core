<a id="build-and-verify-the-effect4-authoring-companion"></a>
# Build and verify the authoring companion

The companion gives an authoring assistant a versioned guide, structural schemas and compiler-produced examples. The host supplies its own exact model, scoped tools and policy separately. Keir does not define your application's discovery, review or activation tools.

## Choose an edition

| Host contract | Companion | Status |
| --- | --- | --- |
| Core 0.3.0; source `/2`, model/artifact/scenario `/3` | [Edition 5 guide](customer-authoring-guide-v5.md) | Candidate. Publish only after package and fixture qualification and verified asset pins. |
| Core 0.2.0; source/model/artifact/scenario `/2` | [Edition 4 guide](customer-authoring-guide-v4.md) | Released. [Exact package record](authoring-companion-v4-compatibility.json). |
| Core 0.1.0; `/1` documents | [Edition 3 guide](customer-authoring-guide-v3.md) | Historical. [Exact package record](authoring-companion-v3-compatibility.json). |

Editions 1 and 2 use earlier preview contracts. Their original archives remain reproducible with the matching version and trusted pin. Do not use a historical guide for current authoring.

## Build from committed inputs

Use Git and Python 3.11 or later. Pass the full companion source commit from the trusted release record, not an arbitrary checkout revision. The builder reads the guide, schemas, examples and compatibility record from that commit and prints the archive's SHA-256 pin.

For released edition 4, supply the three Core 0.2.0 package downloads from its compatibility record.

```sh
python scripts/authoring-companion.py build keir-authoring-v4.zip --version 4 --revision c6a6d171982514569c29c06de7a429d301d25b2a --core-assets path/to/core-assets
```

The manifest distinguishes the companion input commit from the compatible Core source commit. Nonguide inputs must match that Core revision exactly. A source candidate has `status: candidate` and an empty package map; omit `--core-assets` for that row. Candidate bytes do not establish released-package compatibility.

The builder creates a new file and refuses to overwrite an existing archive. Exact historical revisions reproduce the original archive bytes even when current documentation changes.

Edition 5's bundled example sources are canonical JSON. The builder checks each source file's exact byte digest against its artifact's `sourceDigest`, as well as the model and artifact pins. An edited source must be recompiled before packaging.

## Verify before loading

Obtain the archive pin independently from trusted release checksums or the authorized handoff. Verify it before extracting or loading the guide. Edition 4's published pin is shown here.

```sh
python scripts/authoring-companion.py verify keir-authoring-v4.zip --version 4 --sha256 sha256:4a9f4f30212ff1d722cf6c993902d0802bee7e7ffebc00304102a5964b5bec63
```

Verification checks the trusted digest before reading ZIP members, then checks the exact inventory, deterministic encoding, manifest and payload hashes. It works offline without Git. Add `--core-assets path/to/core-assets` to check the three package files too.

Load only the guide named by `manifest.json`. Match its contracts and producer to the host's supported tuple. Supply the published model and authorized tools separately; the companion does not grant permissions, approval or activation authority.

## Semantic references

Schemas validate structure. The language and model references explain types, expressions, data flow, lifecycle and effect limits. Edition 4 bundles the matching workflow expression specification. Edition 5 must use matching bundled references or immutable online specification links and actual producer schemas/examples. The [current specification index](../specs/README.md) distinguishes the candidate contract from released snapshots.

## Record release assets

A release compatibility record contains the qualified Core revision and all three package versions and asset pins. Use `keir-compiler-cli-<version>.zip`, `keir-java-runtime-maven-<version>.zip` and `keir-runtime-node-<version>.tgz`, each with its independently verified `sha256:<hex>` digest. The Java Maven ZIP includes its POMs.

Set `status: release` only after that evidence exists. Build with `--core-assets` to verify every named file. Byte identity alone does not establish provenance or successful installation; the [release checklist](../releasing.md) covers those checks. Preserve earlier editions and their exact bytes.
