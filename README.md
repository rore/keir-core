# Keir

Keir lets developers add AI-assisted customization to their applications. Business users describe the behavior they want in plain language, and an AI assistant turns their requests into extensions using the capabilities the application exposes.

<a id="customizing-an-application"></a>
<a id="supported-contract"></a>
<a id="current-scope"></a>
<a id="what-you-can-build-today"></a>
Start with [What can you customize with Keir?](docs/customizations.md) for concrete requests, what happens when they run, and the roles of the application, AI and user.

![An AI assistant drafts a Keir extension from a customer request and the app’s model. Keir compiles it. At execution, the app checks permissions and performs the operation.](docs/images/extension-flow.svg)

<a id="from-a-request-to-a-running-extension"></a>
## See it in an application

The [Daymark walkthrough](docs/guided-tour.md) follows a request to create a follow-up task through simulated checks, approval, activation and a saved result.

<a id="how-it-fits-your-application"></a>
## What you embed

Embed the Java or Node.js runtime in your application. Both run the same compiled extension format. Use the compiler when creating or changing an extension.

The application model defines where extensions can run, what input data they receive, and which operations they can call. Your application implements those operations through its own services. It also provides the authoring assistant, simulation, review and activation flow, and storage.

[Daymark](https://github.com/rore/keir-demo-app) demonstrates these pieces in a task application, from a user's request through review and activation to execution.

<a id="start-here"></a>
## Build with Keir

Start with [Getting started](docs/quickstart.md) to run and change one extension.

- [Install released packages](docs/installation.md) when embedding Keir without building this repository.
- [Understand the flow](docs/guided-tour.md): follow a request through review and execution, then explore the code.
- [Integrate Keir](docs/embedding.md): expose application operations and call the compiler and runtime from your code.
- [Build an authoring assistant](docs/agents/extension-assistant.md): connect a customer conversation to compilation, simulation and review.

## Documentation

The [documentation index](docs/README.md) links the guides and references.

- [Use a coding assistant for integration](docs/agents/host-integration-assistant.md).
- [Keir specification](docs/specs/README.md): language, application model, compiler and runtime contracts. Use the [compatibility matrix](docs/specs/compiler-artifact.md#source-and-artifact-identity) when selecting packages.
- [Verification and testing](docs/testing.md): tested behavior and its evidence.
- [Contributing](CONTRIBUTING.md) and [roadmap](roadmap/scope.md).

Keir Core is licensed under the [Apache License 2.0](LICENSE); bundled components retain their [third-party notices](THIRD-PARTY.md). Packages are available through [GitHub Releases](https://github.com/rore/keir-core/releases).
