# Java-generated scalar fixtures

These supplemental fixtures are actual `keir-compiler-java/0.1.0-SNAPSHOT` output, not independently compiled Node expressions. They exercise Boolean/empty String values, all four reserved JavaScript field names, Unicode/control values and serialized record boundaries. `changed-condition` changes the authored status predicate while keeping the model and host adapter unchanged.

The scalar variants also exercise named-enum conditional branches. `literal` uses the unchanged shared model to verify a literal Boolean binding, literal enum return and zero call/expression budgets without implicit permissions.

`scalars.dcm.json` is derived from the shared sample model. Each source, artifact and external digest is stored here; the generator requires two Java CLI runs to emit identical canonical bytes. The primary shared read-and-decide fixture remains owned under `conformance/read-and-decide/`.

After building the Java compiler, regenerate from the repository root:

```powershell
node runtime-node/test/fixtures/generate.mjs compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar
```

The generator is development tooling. Installed Node runtime packages do not contain it or depend on Java. Normal tests consume the committed artifacts. Generation proves deterministic compiler output for these inputs; it does not establish Java runtime execution or production containment.
