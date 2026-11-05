# Catalog-bound read integration

<a id="legacy-read2-integration-example"></a>
<a id="walkthrough-design-and-verify-a-read2-host-integration"></a>
<a id="implementation-reference-earlier-read-only-integration"></a>
## Compatibility walkthrough for catalog-bound reads

This walkthrough uses the [earlier catalog-bound read contract](compatibility-reference.md#catalog-delta) and its [versioned binding API](compatibility-reference.md#earlier-binding). For a new integration, start with [Embed Keir](../../embedding.md).

Ask your coding agent to inspect an authorized native read and propose the smallest integration that lets a customer classify a saved task. Its deliverable is a reviewable host proposal: hook placement and input, catalog and consumer versions, protected context, adapter failure mapping, and the evidence needed before the host owner approves the exact revision. Use the [task-advice bundle](../../../conformance/read-simulation/task-advice/) as a worked answer.

1. **Define the hook and catalog.** In [the DCM](../../../conformance/read-simulation/task-advice/dcm.json), `ClassifyTask` is an on-demand REQUEST with a task ID as input and a DISPLAY result. The host publishes `tasks.get`, supplies the authenticated actor separately and defines which validated classifications the consumer can display. The adapter checks current resource access and that the returned task matches the requested ID. It may call a local service or HTTP/RPC with the remaining deadline. The [customer program](../../../conformance/read-simulation/task-advice/source.yaml) chooses the rule: an open task yields PENDING and a completed task yields DONE. Keep those choices in source.
2. **Review the selected bytes.** Inspect the [selection pins](../../../conformance/read-simulation/task-advice/selection.json). Compile against the trusted DCM using the [catalog-bound read command](../implementation-reference.md#compile-the-earlier-read-only-example). Bind that model and its complete versioned invoker set together, then prepare the exact artifact against independently held pins. Supply fresh scope, protected actor, deadline and current authorization on each invocation. The [earlier binding contract](compatibility-reference.md#earlier-binding) defines these checks. The [read-only schemas](../../../schemas/read-2/README.md) describe decoded structure.
3. **Predict what the host will observe.** Read the cases before running them. Open and completed tasks should produce different display receipts. The `tasks.get` business-denial case expects `ACCESS_DENIED` after one dispatch; its safe report redacts that code to `NATIVE_ERROR`. Host entry denial consumes zero dispatches and reports `AUTHORITY_DENIED`. Both suppress display. The [deliberately wrong expectation](../../../conformance/read-simulation/task-advice/mismatch.json) must produce MISMATCH even though the read completes. The [two-read example](../../../conformance/read-simulation/repeated/) checks ordered dispatch without requiring a new hook for each program.

### Run the packaged hosts

Follow the [example build and setup](../../../examples/README.md) to package the Java compiler and host and install the Node.js runtime. Then run this check from the repository root:

```sh
node conformance/read-simulation/verify.mjs
```

The verifier rebuilds the two fixture artifacts with the [standalone compiler](../../../compiler-java/README.md), compares them byte for byte with the pinned artifacts, then compares the Java and Node hosts' canonical reports. It writes reports under `.tools/read-simulation-reports/`; its success line gives the number of reports and exact compiled artifacts. A deliberately mismatching case contributes to a successful verification only when both hosts report the expected mismatch.

Optionally, with the [pinned schema dependency](../../../scripts/requirements-read2-contracts.txt) installed in an isolated Python environment, run `python conformance/read-simulation/check_schemas.py` separately after the verifier. This checks report and scenario structure offline. See the [simulation operator guide](read-simulation-guide.md) for the report contract.

### Hand the proposal to the host owner

Record the selected model and artifact identities, the proposed consumer and adapter versions, the observed simulator results, and the native checks still owed. For example, ask for an owner-scoped real-service test using another user's task ID and a revocation test between entry and dispatch. Simulation exercised the fixture invokers. Only the host can demonstrate actual service permissions, resource identity and deadline behavior, then decide whether to publish the proposed catalog. A later model change requires explicit review of the new identity and recompilation for programs adopting it.
