<a id="agents-using-keir"></a>
<a id="integrate-an-authoring-assistant"></a>
# Use Keir with an assistant

Keir’s compiler and runtime work independently of an assistant. Start with the guide for the work you’re doing.

| Audience | What you’re doing | Start here |
| --- | --- | --- |
| Application developer with a coding assistant | Define when extensions run, what operations they can call, and how your app implements those operations. | [Design a host integration](host-integration-assistant.md) |
| Application developer building an in-app assistant | Give customers a way to draft extensions and review simulated behavior before approval and activation. | [Build a customer authoring assistant](extension-assistant.md) |
| Customer authoring assistant | Follow model-facing instructions for drafting, checking and handing back a proposed extension. | [Authoring guide v2](customer-authoring-guide-v2.md) · [Build and verify the companion](authoring-companion.md) |
| Contributor working on Keir itself | Build and test the repository and follow its development workflow. | [Contributing](../../CONTRIBUTING.md) · [Repository agent instructions](../../AGENTS.md) |

The [v2 companion](authoring-companion.md) is a versioned ZIP you can build and verify from a checkout. It contains the model-facing guide, schemas and example scenarios. When loading the guide, supply your application’s exact published domain capability model (DCM), policy and scoped tool definitions separately.

The [Keir specification](../specs/README.md) defines the compiler and runtime interfaces. The assistant guides describe tool responsibilities for your application to implement. The application owns tool access, approval and activation.
