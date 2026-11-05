# Build and verify the effect/4 authoring companion

The authoring companion is a versioned ZIP of data files. It contains the customer authoring guide, the source, model and artifact schemas, and one follow-up example. The example includes matching, skipped and lost-response cases drawn from the shared corpus. These are authoring examples. The full conformance runner still uses its own fixture directory.

## Build the archive

Use Git and Python 3.11 or later from the repository root. The builder reads the exact committed inputs and writes a new ZIP. Its manifest names the input revision and the verified development revision for the Java compiler, Java runtime and Node.js runtime. See the [package release plan](../../roadmap/features/public-package-release.md) for release identity and publication.

```sh
python scripts/authoring-companion.py build keir-authoring-effect4-v1.zip
```

The command prints the archive’s SHA-256 pin. Repeated builds use the same file order, timestamps and permissions. The schema and guide files keep their exact committed bytes.

## Verify and load the guide

Set the archive’s expected digest in trusted deployment configuration. Get that pin from the reviewed package handoff and pass it to the verifier:

```sh
python scripts/authoring-companion.py verify keir-authoring-effect4-v1.zip --sha256 "$KEIR_AUTHORING_SHA256"
```

Use a `sha256:` prefix followed by the lowercase hexadecimal digest. The verifier checks the archive against that external pin before reading its contents. The manifest’s member digests describe the package inventory.

After verification, load the exact guide bytes for your authoring assistant. Supply your own scoped domain capability model (DCM), tool definitions and policy separately. Pin your model’s exact identity independently. The example model and cases illustrate the profile and carry no authority over your application.

When a release adds authorable behavior, update the guide and an example with the code. Update schemas when the accepted document structure changes. Add an offline check that compiles and simulates the exact rule against independent expectations. Review the package’s compatibility and pins with those changes.
