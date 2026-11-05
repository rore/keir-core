# Check a read extension offline

Use the already compiled artifact, its DCM and independent pins. Author expected calls, compact outcome and receipt separately from execution. Predict classification and typed result on success, or classification and error code/path on failure; no internal trace is required or accepted. Do not regenerate expectations from actual reports.

The [task-advice bundle](../../../conformance/read-simulation/task-advice/) copies the demo's exact Maya artifact/model, source and public provenance. The source is evidence only; execution needs no compiler, demo server, database or sibling checkout. `selection.json` pins the bytes and consumer; a raw YAML digest is distinct from the compiler's canonical parsed-source digest.

After building the Java host and installing the reviewed runtime package into the Node example host, run from the repository root:

```sh
node examples/node-host/read-simulation.mjs conformance/read-simulation/task-advice normal conformance/read-simulation/task-advice/open.json
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.ReadSimulationHost conformance/read-simulation/task-advice normal conformance/read-simulation/task-advice/open.json
```

Use `completed.json`, `missing.json` and `business-denied.json` with `normal`; use `denied.json` with the code-owned `denied` fixture. Business `ACCESS_DENIED` consumes one stub/dispatch; host `AUTHORITY_DENIED` consumes none. Both yield `NOT_DISPLAYED`. `mismatch.json` deliberately expects a different valid outcome/receipt; `wrong-error.json` proves original business codes compare privately even when the public diagnostic is `NATIVE_ERROR`. The report's closed native summary identifies the actual classification and safe code. Fixture names are operator arguments, never case fields. Additional diagnostic fixtures are `consumer-throws`, `consumer-unlisted`, and `consumer-wrong`. The example consumer maps successful classification to a closed safe receipt. Domain response checking belongs in this example, not the generic tooling.

Run `node conformance/read-simulation/verify.mjs` for packaged Java/Node report parity and the offline schema checks described in its README. Keep test/setup installation isolated and use the existing pinned toolchain. A PASS proves only this simulated case and its trusted fixture; it does not certify authorization, a real adapter, callback purity, remote cancellation or hard resource containment.
