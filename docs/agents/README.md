<a id="agents-using-keir"></a>
<a id="integrate-an-authoring-assistant"></a>
# Use Keir with an assistant

Keir’s compiler and runtime work independently of an assistant. Start with the guide for the work you’re doing.

| Audience | What you’re doing | Start here |
| --- | --- | --- |
| Application developer with a coding assistant | Define when extensions run, what operations they can call, and how your app implements those operations. | [Design a host integration](host-integration-assistant.md) |
| Application developer building an in-app assistant | Give customers a way to draft extensions and review simulated behavior before approval and activation. | [Build a customer authoring assistant](extension-assistant.md) |
| Customer authoring assistant | Follow model-facing instructions for drafting, checking and handing back a proposed extension. | [Select the matching companion](authoring-companion.md#choose-an-edition) · [Build and verify it](authoring-companion.md) |
| Contributor working on Keir itself | Build and test the repository and follow its development workflow. | [Contributing](../../CONTRIBUTING.md) · [Repository agent instructions](../../AGENTS.md) |

Choose the guide matching the host's packages and model using the [companion compatibility table](authoring-companion.md#choose-an-edition). Earlier guides serve their named contracts; do not use a historical guide for current authoring.

For conditional call results, see the [guarded-result examples](../../examples/README.md#guarded-result-scalar-dataflow) and select a [companion matching the installed host](authoring-companion.md#choose-an-edition). Package publication does not upgrade a host's supported capabilities.

For development Core 0.5 numeric authoring, use [draft edition 9](customer-authoring-guide-v9.md) with the [exact profile matrix](../specs/compiler-artifact.md#source-and-artifact-identity). It covers canonical Integer/Decimal strings, exact arithmetic and explicit rounding. Compiler/Java/Node source regressions have passed, but matching packages and the edition-9 companion remain unqualified. This is not a new compatibility row or permission to use numerics on Core 0.4.0. Earlier guide editions and archive bytes remain unchanged; select them by their actual host contracts.

The [authoring companion](authoring-companion.md) packages the guide, schemas and example scenarios as a versioned ZIP. Supply your application's exact published domain capability model (DCM), policy and scoped tool definitions separately.

The [embedding guide](../embedding.md) shows how an application calls the compiler and runtime. The application implements its authoring tools and owns tool access, approval and activation.
