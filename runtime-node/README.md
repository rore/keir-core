<a id="node-runtime"></a>
# Keir runtime for Node.js

Run compiled Keir rules inside a Node.js application. A rule can read data and request at most one write. Your application provides the operations a rule can call, checks permissions and performs any write through its own services.

[Run the Node.js example](https://github.com/rore/keir-core/blob/main/docs/quickstart.md) · [Embed Keir](https://github.com/rore/keir-core/blob/main/docs/embedding.md)

## Bind, prepare and run

Start with a domain capability model (DCM), which describes the operations your application exposes, and a rule compiled against that model.

1. **Bind the model.** Register one implementation for every operation in the DCM. Each registration contains `version`, `mode`, `keyType` and `invoke`. Use the same mode, `real` or `simulation`, for every registration.
2. **Prepare the rule.** Your application selects and verifies the compiled bytes. Keir checks that the rule matches the bound model and validates it before execution.
3. **Run the rule.** Pass the event’s business input and fresh host context. You can reuse the prepared rule for later events.

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

The host supplies protected values such as the authenticated actor in `bindings`, the original non-null request key, and the event ID and source. The deadline is an absolute time measured by the host’s `clock.nowMs()`. Each implementation receives business arguments, protected arguments, the remaining time, and context with the key, event and deadline. The implementation checks current permission and key authenticity.

## Read the outcome

A rule result describes what the rule returned. A separate write receipt records what the implementation reported. A skipped write has no receipt. `CONFIRMED` means the implementation reported a commit, `NONE` means it reported no write, and `UNKNOWN` means the write may have happened. Check with your application’s service before deciding what to do next.

[API signatures](https://github.com/rore/keir-core/blob/main/docs/specs/host-model-binding.md#api-shapes) · [Write outcomes](https://github.com/rore/keir-core/blob/main/docs/specs/real-single-effect-admission.md#effect-outcomes-and-key-identity)

<a id="legacy-effect3"></a>
<a id="single-effect-profile"></a>
## Earlier integrations

See [earlier Node.js APIs](https://github.com/rore/keir-core/blob/main/docs/internal/compatibility-reference.md#earlier-node-runtime-apis) for existing artifacts that use older binding and execution contracts.
