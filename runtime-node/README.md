<a id="node-runtime"></a>
# Keir runtime for Node.js

Run compiled Keir extensions inside a Node.js application. An event extension runs after commit and may request at most one write. A pre-operation decision permits only reads and returns ALLOW or DENY before the native operation. Your application provides the operations, checks permissions and performs native writes through its own services.

[Run the Node.js example](https://github.com/rore/keir-core/blob/main/docs/quickstart.md) · [Embed Keir](https://github.com/rore/keir-core/blob/main/docs/embedding.md)

Use [released-package installation](https://github.com/rore/keir-core/blob/main/docs/installation.md#install-and-run-in-nodejs) to install the verified runtime tarball in your own application.

## Bind, prepare and run

Start with a domain capability model (DCM), which describes the operations your application exposes, and a rule compiled against that model.

1. **Bind the model.** Register one implementation for every operation in the DCM. Each registration contains `version`, `mode`, `keyType` and `invoke`. Use the same mode, `real` or `simulation`, for every registration.
2. **Prepare the rule.** Your application selects and verifies the compiled bytes. Keir checks that the rule matches the bound model and validates it before execution.
3. **Run the rule.** Pass the hook's business input and fresh host context. You can reuse the prepared rule for later invocations.

```js
import { bindModel, prepareProgram, invokeProgram } from '@keir/runtime-node';

const { boundModel, diagnostic: bindError } = bindModel(dcmBytes, implementations);
if (bindError) throw new Error(bindError.code);

const { prepared, diagnostic: prepareError } = prepareProgram(artifactBytes, boundModel);
if (prepareError) throw new Error(prepareError.code);

const outcome = await invokeProgram(prepared, businessInput, {
  bindings,
  deadline: { clock, expiresAtMs },
  requestKey,
  event: { id, source }
});
```

The host supplies protected values such as the authenticated actor in `bindings`, the original non-null request key, and the event ID and source. For a REQUEST hook, those event fields identify the operation attempt and its host-defined scope, without asserting a commit. The deadline is an absolute time measured by the host's `clock.nowMs()`. Each implementation receives business arguments, protected arguments, the remaining time, and context with the key, event and deadline. The implementation checks current permission and key authenticity.

## Read the outcome

A rule result describes what the rule returned. A separate write receipt records what the implementation reported. A skipped write has no receipt. `CONFIRMED` means the implementation reported a commit, `NONE` means it reported no write, and `UNKNOWN` means the write may have happened. Check with your application’s service before deciding what to do next.

For a pre-operation decision, consume the complete outcome and require valid COMPLETED ALLOW with empty effects before continuing. DENY or failed evaluation blocks the native operation; a handled business rejection returning ALLOW still blocks. Recheck native permissions, selection and mutable facts before commit, within the original deadline. Cancellation closes the attempt. Keep the native result separate from the decision.

Use the [compatibility matrix](https://github.com/rore/keir-core/blob/main/docs/specs/compiler-artifact.md#source-and-artifact-identity) to match the runtime, compiler and model. It distinguishes supported authoring contracts from retained execution of earlier approved artifacts.

[API signatures](https://github.com/rore/keir-core/blob/main/docs/specs/host-model-binding.md#api-shapes) · [Write evidence](https://github.com/rore/keir-core/blob/main/docs/specs/runtime.md#write-evidence) · [Decision walkthrough](https://github.com/rore/keir-core/blob/main/docs/decision-walkthrough.md)

<a id="legacy-effect3"></a>
<a id="single-effect-profile"></a>
## Earlier integrations

See [earlier Node.js APIs](https://github.com/rore/keir-core/blob/main/docs/internal/compatibility-reference.md#earlier-node-runtime-apis) for existing artifacts that use older binding and execution contracts.
