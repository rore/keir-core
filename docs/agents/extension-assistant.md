<a id="customer-extension-assistant"></a>
# Build a customer authoring assistant

Select the [authoring companion](authoring-companion.md#choose-an-edition) matching the host's packages and model. Its compatibility record identifies the exact package and document combination.

## Task and inputs

Give the assistant the exact model approved for the customer, including hooks, capabilities, result contracts, types and limits. Give it separate access to the authoring tools you want it to use. For example, the follow-up extension chooses a project and title and calls the app’s existing `tasks.create` operation. The project choice belongs in source. Resource permissions belong to the application.

Use the [language reference](../specs/language.md) and [domain capability model reference](../specs/domain-model.md) to understand expression, type and call rules. Use the matching schemas identified by the [format matrix](../specs/compiler-artifact.md#source-and-artifact-identity) to check document structure.

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

### Author a pre-operation decision

Discover a REQUEST/BEFORE_COMMIT/DECISION hook in the host's current model. Read its input and result, then compose approved READs if the request needs more facts. Compile against that exact model. A WRITE is rejected even when guarded by false; repair the draft without replacing the requested decision with a mutation.

Agree on ALLOW, DENY, failed read, timeout and changed-fact cases before simulation. Report the runtime result, decision consumption and native result separately. A handled BUSINESS_REJECTION returning ALLOW still blocks. Present the exact artifact and evidence for approval and activation. The [neutral walkthrough](../decision-walkthrough.md) follows this flow; discovery, compilation, simulation and review tool names and permissions are defined by your application.

## Guarded results require matching host support

Use guarded-result scalar access only when the host explicitly supports producer `keir-compiler-java/0.4.0` and matching runtimes. The development allocation is not a published release. Older installed consumers and companion editions retain their original limits; do not suggest an automatic upgrade or recompile an approved artifact in place.

For an earlier CALL `r` with `when: g`, use `g ? r.reference : input.existingReference`. For an optional scalar, keep both proofs: `g ? (has(r.note) ? r.note : "") : ""`. Use the exact Boolean BIND identifier named by the CALL's `when`, not an equivalent expression or an alias of a different guard. If the CALL itself names an alias as its guard, use that exact identifier. Do not read the record or its lists, infer proof from a later CALL's guard, or refer to a CALL's own result in its arguments/handler. Preserve exact nominal/enum/timestamp types and the one-possible-WRITE ceiling (zero for decisions).

Agree on true, skipped, optional absent/empty/false, rejected and uncertain cases before simulation. Return the adapter's actual validated reference on success. Failure is not absence: never replace rejection, expiry, invalid data or UNKNOWN effect with the skipped fallback. Show receipts separately, including CONFIRMED beside a later failure. The [complete neutral examples](../../conformance/keir-3/README.md) and [language rules](../specs/language.md#steps-and-data-flow) define the bounded capability.

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
