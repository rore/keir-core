# Reusable read simulation

`task-advice/` preserves the demo's exact Maya source/model/artifact with public provenance and independent selection pins. Open, completed, declared missing and host-denied expectations are authored independently. `mismatch.json` deliberately expects a different valid classification. `repeated/` adds a compiled two-read program with distinct business arguments and asymmetric status checks; reordering stubs must fail.

Run `node conformance/read-simulation/verify.mjs` after building Java and installing the reviewed Node package into the example host. It recompiles both sources for byte identity, then compares canonical reports from actual packaged Java and installed Node entrypoints. It also exercises consumer failures, malformed authority-bearing data and bounded parsing. Reports are saved in ignored `.tools/read-simulation-reports/` for schema checks.

With the pinned development schema dependency already installed in an isolated environment, run `python conformance/read-simulation/check_schemas.py`. It validates all decoded case/report structures offline and tests rejection of authority fields, invalid bounds and unknown external schema references. Byte/depth/DCM/allowlist checks remain native obligations. Both commands run in product CI.

See [the contract](../../docs/specs/read-simulation.md) and [operator guide](../../docs/agents/read-simulation.md). Existing legacy/generic corpora and runtime semantics remain unchanged.
