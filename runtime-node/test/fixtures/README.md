# Java-generated scalar fixtures

These supplemental fixtures are actual `keir-compiler-java/0.1.0-SNAPSHOT` output, not independently compiled Node expressions. They exercise Boolean/empty String values, all four reserved JavaScript field names, Unicode/control values and serialized record boundaries. `changed-condition` changes the authored status predicate while keeping the model and host adapter unchanged.

The scalar variants also exercise named-enum conditional branches. `literal` uses the unchanged shared model to verify a literal Boolean binding, literal enum return and zero call/expression budgets without implicit permissions.

`scalars.dcm.json` is derived from the shared sample model. Each source, artifact and external digest is stored here; the generator requires two Java CLI runs to emit identical canonical bytes. The primary shared read-and-decide fixture remains owned under `conformance/read-and-decide/`.

After building the Java compiler, regenerate from the repository root:

```powershell
node runtime-node/test/fixtures/generate.mjs compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar
```

The generator is development tooling. Installed Node runtime packages do not contain it or depend on Java. Normal tests consume the committed artifacts. Generation proves deterministic compiler output for these inputs; it does not establish Java runtime execution or production containment.

## Effect handler record bound

`effect-handler` is actual effect-capable Java compiler output. Its sixteen String output fields exercise a terminal business handler's exact 8,192-byte record limit and one-byte overflow. Fifteen fields project a valid 256-byte title; the last projects caller padding. Overflow must retain NONE and the original rejection in `error.cause`. The primary shared effect fixture is unchanged.

Regenerate this supplemental source/model/artifact and external pins with the [reviewed compiler checkpoint](https://github.com/rore/keir-core/commit/1a8842676b6a57031dbb41448046b11fd3560e48) or an intentionally reviewed compatible revision:

```powershell
node runtime-node/test/fixtures/generate-effect-handler.mjs compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar
```

The generator reads the separately pinned primary effect model, authors only source/model data, and requires two independent `--profile=keir-effect/1` CLI runs to match. It emits a 10,272-byte artifact pinned as `sha256:e9a60681ca2d29b30c651785969d8f42f6441f754aad4b736b3100262f01f05c`; its canonical model pin is `sha256:47ef3f12f2325e10473525d926171d581cca9499deedcb41ab80517ed164ce2c`. No checked AST is authored in Node, and normal tests need neither Java nor generation.
