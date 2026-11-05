<a id="customer-extension-assistant"></a>
# Build a customer authoring assistant

Use the [model-facing guide](customer-authoring-guide-v3.md) as instructions for an assistant in your application. The [authoring companion](authoring-companion.md) packages the guide with schemas and a worked example.

## Task and inputs

Give the assistant the exact model approved for the customer, including hooks, capabilities, result contracts, types and limits. Give it separate access to the authoring tools you want it to use. For example, the follow-up extension chooses a project and title and calls the app’s existing `tasks.create` operation. The project choice belongs in source. Resource permissions belong to the application.

Use the [language reference](../specs/language.md) and [domain capability model reference](../specs/domain-model.md) to understand expression, type and call rules. Use the [schemas bundled with the guide](customer-authoring-guide-v3.md#follow-up-example) to check document structure.

## Workflow

### Business-user interaction

1. Let the customer describe the desired behavior. Ask only for business choices the assistant cannot resolve from the scoped context.
2. Have the assistant draft against the published model and use structured compiler diagnostics for bounded repair. Preserve the model identity and the customer's intent.
3. Agree on expected outcomes before running the compiled artifact with simulated implementations. Include a case where the extension should do nothing and relevant denial or uncertain-write cases. Show observed results and effect evidence separately.
4. Present the exact checked revision to the authorized reviewer. Record approval and activation as separate decisions. Changes to the revision require renewed checks and approval.

Keep the conversation usable without a dashboard. If the application offers a separate example view, open it on request and preserve the customer’s place and unsent text when a new response arrives.

### Example authoring session

A customer asks for a follow-up when someone completes a task in the launch project. These tool names illustrate an API the application defines.

1. **`discoverExtensionContext`** returns the approved task model, `TaskCompleted` entrypoint and permitted operations. **`findProject`** resolves “launch” within the customer’s scope and returns its `ProjectId`.
2. **`compileDraft`** compiles against that exact model. If the draft declares the selected project as a plain string, the compiler reports `NOMINAL_MISMATCH` at the project comparison. The assistant changes the declaration to a typed `ProjectId` literal and compiles again.
3. **`simulateDraft`** takes the compiled artifact and runs it with the app’s simulated operations. The assistant compares against agreed expectations for a matching task, a task in another project, and a lost write response.
4. **`submitForReview`** presents the exact artifact, the expected and observed outcomes, and the write receipts. The customer can review the result. The host handles approval and activation separately for that revision.

## Authority and tools

The application selects the DCM and capabilities available for authoring. It supplies protected values at execution and owns permissions, native commits, approval and activation. Keep catalog publication and capability registration outside the assistant’s authority. See the [embedding guide](../embedding.md) for runtime setup.

| Authoring responsibility | What the application supplies |
| --- | --- |
| Scoped discovery | The approved model identity, the available operations and any authorized resource lookup. |
| Draft compilation and diagnostics | A call to the [Keir compiler](../../compiler-java/README.md) with the host-selected model, and safe diagnostic results. |
| Simulation and review | The [same compiled artifact](../embedding.md) with simulated implementations, independent expectations and a result the customer can inspect. |
| Approval and activation | Separate host-owned operations for the authorized user and the exact reviewed revision. |

These are responsibilities for tools your application defines. Keep tool permissions separate from the capabilities a rule can call, and surface missing tools, failed validation or uncertain outcomes in the review.

<a id="first-worked-customization-classify-a-task"></a>
<a id="implementation-reference-earlier-task-examples"></a>
## Earlier examples

For the current contract, [run the follow-up example](../quickstart.md). For compatibility work, see the [original read-only source](../internal/history/compatibility-reference.md#original-read-source), [original simulated-write scenarios](../../conformance/single-effect/README.md) or [catalog-bound follow-up walkthrough](../internal/history/compatibility-reference.md#earlier-follow-up).
