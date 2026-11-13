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

For qualified unpublished Core 0.5 numeric authoring, select [companion edition 9](authoring-companion.md#qualified-candidate-edition-9) with the [exact profile matrix](../specs/compiler-artifact.md#source-and-artifact-identity). It covers canonical Integer/Decimal strings, exact arithmetic and explicit rounding. [Runtime/package checks, the corrected companion and tested Daymark adoption are accepted](../specs/README.md#numeric-qualification-status). Candidate qualification is not public publication or an automatic host upgrade. Do not use numerics on Core 0.4.0. Frozen guides and archives retain their original bytes and preparation-time status; use the live catalog for current selection.

The [authoring companion](authoring-companion.md) packages the guide, schemas and example scenarios as a versioned ZIP. Supply your application's exact published domain capability model (DCM), policy and scoped tool definitions separately.

The [embedding guide](../embedding.md) shows how an application calls the compiler and runtime. The application implements its authoring tools and owns tool access, approval and activation.
