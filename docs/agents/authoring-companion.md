<a id="build-and-verify-the-effect4-authoring-companion"></a>
# Build and verify the authoring companion

The authoring companion is a versioned ZIP of data files. It contains the customer authoring guide, the source, model and artifact schemas, and one follow-up example. The example includes matching, skipped and lost-response cases drawn from the shared corpus. These are authoring examples. The full conformance runner still uses its own fixture directory.

## Build the archive

Use Git and Python 3.11 or later from the repository root. Select version 2 to package the [current authoring guide](customer-authoring-guide-v2.md). The builder reads the exact committed inputs and writes a new ZIP. Its manifest identifies the source revision and the compatible compiler and runtimes. See the [package release plan](../../roadmap/features/public-package-release.md) for release identity and publication.

```sh
python scripts/authoring-companion.py build keir-authoring-v2.zip --version 2
```

The command prints the archive’s SHA-256 pin. Repeated builds use the same file order, timestamps and permissions. The schema and guide files keep their exact committed bytes.

Version 2 updates the guide and keeps the same schemas, example, and runtime compatibility. The first archive contains the [archived v1 guide](customer-authoring-guide-v1.md). Omit `--version` or pass `--version 1` to build or verify it with its original digest.

## Verify and load the guide

Select the archive version and set its expected digest in trusted deployment configuration. Get the digest for that version from the reviewed package handoff and pass it to the verifier:

```sh
python scripts/authoring-companion.py verify keir-authoring-v2.zip --version 2 --sha256 "$KEIR_AUTHORING_SHA256"
```

Use a `sha256:` prefix followed by the lowercase hexadecimal digest. The verifier checks the archive against that external pin before reading its contents. The manifest’s member digests describe the package inventory.

After verification, load `customer-authoring-guide-v2.md` from the archive for your authoring assistant. Supply your own scoped domain capability model (DCM), tool definitions and policy separately. Pin your model’s exact identity independently. The host still owns access, approval, activation, and native operations. The bundled model and cases are examples.

For a rendered copy of the guide, open the [guide at the pinned source revision](https://github.com/rore/keir-core/blob/74d128f04a5fd76d744e3b7705f6614e64846b38/docs/agents/customer-authoring-guide-v2.md). Its specification links are repository-relative. From the ZIP alone, open the [pinned specification](https://github.com/rore/keir-core/blob/74d128f04a5fd76d744e3b7705f6614e64846b38/docs/specs/README.md) separately.

When a release adds authorable behavior, update the guide and an example with the code. Update schemas when the accepted document structure changes. Add an offline check that compiles and simulates the exact rule against independent expectations. Review the package’s compatibility and pins with those changes.
