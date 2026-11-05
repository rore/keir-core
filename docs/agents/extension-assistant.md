<a id="customer-extension-assistant"></a>
# Build a customer authoring assistant

Use the [model-facing guide](customer-authoring-guide-v3.md) as instructions for an assistant in your application. The [authoring companion](authoring-companion.md) packages the guide with schemas and a worked example.

## Task and inputs

Give the assistant the exact model approved for the customer, including hooks, capabilities, result contracts, types and limits. Give it separate access to the authoring tools you want it to use. For example, the follow-up extension chooses a project and title and calls the app’s existing `tasks.create` operation. The project choice belongs in source. Resource permissions belong to the application.

Use the [schemas bundled with the guide](customer-authoring-guide-v3.md#follow-up-example) to check decoded documents. Use the compiler and runtimes to check what can compile and run.

## Workflow

### Business-user interaction

1. Let the customer describe the desired behavior. Ask only for business choices the assistant cannot resolve from the scoped context.
2. Have the assistant draft against the published model and use structured compiler diagnostics for bounded repair. Preserve the model identity and the customer's intent.
3. Agree on expected outcomes before running the compiled artifact with simulated implementations. Include a case where the extension should do nothing and relevant denial or uncertain-write cases. Show observed results and effect evidence separately.
4. Present the exact checked revision to the authorized reviewer. Record approval and activation as separate decisions. Changes to the revision require renewed checks and approval.

Keep the conversation usable without a dashboard. If the application offers a separate example view, open it on request and preserve the customer’s place and unsent text when a new response arrives.

## Authority and tools

The application selects the DCM and capabilities available for authoring. It supplies protected values at execution and owns permissions, native commits, approval and activation. Keep catalog publication and capability registration outside the assistant’s authority. [Host binding](../specs/host-model-binding.md) defines runtime setup separately.

| Authoring responsibility | What the application supplies |
| --- | --- |
| Scoped discovery | The approved model identity, the available operations and any authorized resource lookup. |
| Draft compilation and diagnostics | A call to the [Keir compiler](../../compiler-java/README.md) with the host-selected model, and safe diagnostic results. |
| Simulation and review | The [same compiled artifact](../embedding.md) with simulated implementations, independent expectations and a result the customer can inspect. |
| Approval and activation | Separate host-owned operations for the authorized user and the exact reviewed revision. |

These are responsibilities for tools your application defines. The compiler and runtime APIs are listed in the [specification](../specs/README.md). Keep tool permissions separate from the capabilities a rule can call, and surface missing tools, failed validation or uncertain outcomes in the review.

<a id="first-worked-customization-classify-a-task"></a>
<a id="implementation-reference-earlier-task-examples"></a>
## Earlier examples

For the current contract, [run the follow-up example](../quickstart.md). For compatibility work, see the [original read-only source](../internal/history/compatibility-reference.md#original-read-source), [original simulated-write scenarios](../../conformance/single-effect/README.md) or [catalog-bound follow-up walkthrough](../internal/history/compatibility-reference.md#earlier-follow-up).
