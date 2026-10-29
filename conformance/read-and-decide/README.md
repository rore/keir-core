# Read-and-decide golden fixture

`source.yaml` and its trusted `dcm.json` describe the controlled read/classify sample. The Java compiler emits `artifact.json`; both runtimes must consume these same bytes. `artifact.sha256` contains the separately held `sha256:` digest for this curated test fixture. It does not authenticate arbitrary customer output or certify a real adapter.

The artifact uses candidate profile `keir-read/1` and compiler identity `keir-compiler-java/0.1.0-SNAPSHOT`. This fixture is not a completed runtime-conformance claim. See the [compiler commands](../../compiler-java/README.md) and [acceptance contract](../../docs/specs/host-integration.md#acceptance-fixtures).

Compiler tests read this directory and require byte-for-byte agreement with the golden artifact. Regenerate through the Java compiler after an intentional reviewed source/model/environment change, independently verify the resulting digest, and update all four files together. Do not recompile separately for Java and Node.
