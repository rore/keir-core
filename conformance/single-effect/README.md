# Single-effect compiler fixture

This is the Java-compiled sample for the accepted [single-effect contract](../../docs/specs/single-effect.md). Source and model match the [design example](../../docs/internal/single-effect-followup-design.md). It conditionally creates one follow-up after reading task status and project; handlers return terminal typed rejection records. The guarded result is absent from every later expression environment.

After building the [compiler](../../compiler-java/README.md), run from the repository root:

```sh
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar conformance/single-effect/source.yaml conformance/single-effect/dcm.json --profile=keir-effect/1
```

Stdout is the canonical artifact; preserve it as bytes. `artifact.sha256` independently pins the 5,439-byte artifact; `dcm.sha256` pins the 1,721-byte canonical model. Two fresh compiler processes reproduced these bytes, and the original two-argument read CLI still reproduced its golden artifact.

The compiler checkpoint passed 47 tests. Both runtime module checkpoints are verified, including the same handler-boundary fixture; ordered cross-runtime simulation and packaged effect-host behavior remain pending. Fixtures use fictional data; the model's simulation-only assurance cannot authorize real writes.
