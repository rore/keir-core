<a id="agents-using-keir"></a>
# Integrate an authoring assistant

An application can use Keir directly or give an assistant tools for drafting, checking and reviewing rules. The application supplies the tools, scoped data and permission checks.

| Integration | Who it helps and what it does | Guide |
| --- | --- | --- |
| Host integration assistant | Helps the application developer define when rules run, what operations they can use and how the app implements those operations | [Host integration](host-integration-assistant.md) |
| Customer authoring assistant | Helps a customer draft a rule and review simulated results before approval and activation | [Customer authoring integration](extension-assistant.md) |

For instructions to supply to a customer-facing model, use the [Keir customer authoring guide](customer-authoring-guide-v2.md). Pair it with the host's exact published DCM and scoped tools. The [pinned authoring companion](authoring-companion.md) includes schemas, examples and the earlier guide selected by its manifest.

Use the [Keir specification](../specs/README.md) and [structural schemas](../../schemas/effect-4/README.md) to define tool inputs and diagnostics. Authoring tools such as compilation and simulation are separate from the operations a rule can call at runtime. Approval and activation stay with the application and its authorized users.

For the earlier read-only contract, see the [offline simulation guide](read-simulation.md).
