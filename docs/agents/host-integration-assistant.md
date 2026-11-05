# Host integration assistant

Use this guide when helping a developer design an application integration. The application supplies discovery, approval and activation tools. Read the [agent overview](README.md) and [Keir specification](../specs/README.md).

Use the schema for the selected contract when checking decoded values. Schema success establishes structure only. Host approval and execution authority still apply. [Read-only schemas](../../schemas/read-2/README.md) · [Current schemas](../../schemas/effect-4/README.md)

## Task and inputs

The planned host-design delivery is a skill for the developer's existing coding agent, using that agent's model/authentication. In the demo, this pairs with the pre-design-time native-app checkpoint; provider credentials for the separate in-app customer extension assistant do not authenticate the host-design agent.

Help the host developer define domain types, generic hooks and result consumers, an approved capability catalog, protected bindings, adapters and aggregate limits. Use authorized host requirements and interface/source descriptions. An API schema supplies structural information, not proof of authorization, atomicity, idempotency or other business guarantees.

Expose reusable native operations, not the customization being demonstrated: `tasks.create` creates a task; the extension supplies its project/title and decides that creation is a follow-up. Do not hide the program's eligibility or composition in a special adapter. Domain-specific atomic operations remain valid when they are actual host contracts. See the [capability ownership rule](../specs/generic-hook-capability-contract.md#responsibility-boundaries).

Design hooks around inputs, outputs, lifecycle and write limits. Under the [single-write contract](../specs/generic-hook-capability-contract.md), the host publishes the complete DCM catalog and binds every compatible implementation at initialization. Use the exact contract selected by the host.

Assume capability business operations usually live behind internal service-to-service APIs, including HTTP/RPC. A registered adapter bridges the runner to those services; do not equate its local callable interface with a local business implementation. Keep routing, credentials, trusted identity propagation, deadlines and transport/result mapping host-owned. A timed-out remote WRITE may still commit; preserve uncertainty and do not invent retries or verified native guarantees. See the [dispatch boundary](../specs/generic-hook-capability-contract.md#capability-invocation-across-services).

## Workflow

Initialize with `bindModel(dcmBytes, implementations)` and prepare with `prepareProgram(artifactBytes, boundModel)`. Check diagnostics and stop initialization on failure. Bind the complete catalog, including unused operations. Supply fresh bindings, event, original non-null key and absolute deadline per invocation. Host implementations enforce permission and key authenticity, including before native commit. [Binding contract](../specs/host-model-binding.md)

Preserve the host-defined payload, including unused legitimate fields. Customer project and title choices belong in the rule unless the rule explicitly obtains them elsewhere. [Nominal literals](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) give fixed identifiers a type. The host still decides whether the customer can access the resource.

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft the scoped catalog and generic hook separately. Identify protected parameters, effect/lifecycle ceilings, aggregate limits, compatibility and unresolved guarantees. Add named capability restrictions only for an explicit host requirement; do not infer them from one proposed program.
3. Propose host conformance cases and evidence for actual adapters, including rejection and failure paths.
4. Use the [compiler library/CLI](../../compiler-java/README.md) diagnostics and [controlled host verification](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) to revise proposals with the developer. Connect host agent tools separately.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding native guarantees without presenting engineering evidence as a runtime credential.

## Authority and tools

With developer authorization, propose or edit integration artifacts in the assigned workspace. Do not publish the authoritative catalog, widen permissions, certify unverified claims or approve your own proposal merely because its schema validates. Approval and real adapter evidence are separate requirements. The host owns secrets, trusted invocation values and published definitions.

The host integration surface needs authorized interface inspection, definition drafting, model validation and host conformance checks. These are intended operations, not implemented API names. Each tool must expose its schemas, scope, side effects and diagnostics. Report unavailable tools and unresolved host choices; do not invent results.

<a id="legacy-read2-integration-example"></a>
<a id="walkthrough-design-and-verify-a-read2-host-integration"></a>
## Implementation reference: earlier read-only integration

For current behavior, start with [Embed Keir](../embedding.md) and the [Java and Node.js examples](../../examples/README.md).

Ask your coding agent to inspect an authorized native read and propose the smallest integration that lets a customer classify a saved task. Its deliverable is a reviewable host proposal: hook placement and input, catalog and consumer versions, protected context, adapter failure mapping, and the evidence needed before the host owner approves the exact revision. Use the [task-advice bundle](../../conformance/read-simulation/task-advice/) as a worked answer.

1. **Define the hook and catalog.** In [the DCM](../../conformance/read-simulation/task-advice/dcm.json), `ClassifyTask` is an on-demand REQUEST with a task ID as input and a DISPLAY result. The host publishes `tasks.get`, supplies the authenticated actor separately and defines which validated classifications the consumer can display. The adapter checks current resource access and that the returned task matches the requested ID. It may call a local service or HTTP/RPC with the remaining deadline. The [customer program](../../conformance/read-simulation/task-advice/source.yaml) chooses the rule: an open task yields PENDING and a completed task yields DONE. Keep those choices in source.
2. **Review the selected bytes.** Inspect the [selection pins](../../conformance/read-simulation/task-advice/selection.json). Compile against the trusted DCM using the [catalog-bound read command](../internal/implementation-reference.md#compile-the-earlier-read-only-example). Bind that model and its complete versioned invoker set together, then prepare the exact artifact against independently held pins. Supply fresh scope, protected actor, deadline and current authorization on each invocation. The [binding contract](../specs/host-model-binding.md) defines these checks; the [schemas](../../schemas/read-2/README.md) describe decoded structure.
3. **Predict what the host will observe.** Read the cases before running them. Open and completed tasks should produce different display receipts. The `tasks.get` business-denial case expects `ACCESS_DENIED` after one dispatch; its safe report redacts that code to `NATIVE_ERROR`. Host entry denial consumes zero dispatches and reports `AUTHORITY_DENIED`. Both suppress display. The [deliberately wrong expectation](../../conformance/read-simulation/task-advice/mismatch.json) must produce MISMATCH even though the read completes. The [two-read example](../../conformance/read-simulation/repeated/) checks ordered dispatch without requiring a new hook for each program.

### Run the packaged hosts

Follow the [example build and setup](../../examples/README.md) to package the Java compiler and host and install the Node.js runtime. Then run this check from the repository root:

```sh
node conformance/read-simulation/verify.mjs
```

The verifier rebuilds the two fixture artifacts with the [standalone compiler](../../compiler-java/README.md), compares them byte for byte with the pinned artifacts, then compares the Java and Node hosts' canonical reports. It writes reports under `.tools/read-simulation-reports/`; its success line gives the number of reports and exact compiled artifacts. A deliberately mismatching case contributes to a successful verification only when both hosts report the expected mismatch.

Optionally, with the [pinned schema dependency](../../scripts/requirements-read2-contracts.txt) installed in an isolated Python environment, run `python conformance/read-simulation/check_schemas.py` separately after the verifier. This checks report and scenario structure offline. See the [simulation operator guide](read-simulation.md) for the report contract.

### Hand the proposal to the host owner

Record the selected model and artifact identities, the proposed consumer and adapter versions, the observed simulator results, and the native checks still owed. For example, ask for an owner-scoped real-service test using another user's task ID and a revocation test between entry and dispatch. Simulation exercised the fixture invokers. Only the host can demonstrate actual service permissions, resource identity and deadline behavior, then decide whether to publish the proposed catalog. A later model change requires explicit review of the new identity and recompilation for programs adopting it.
