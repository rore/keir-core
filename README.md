# Keir Core

Keir is an embeddable framework for AI-assisted application extensions. It provides a constrained language, a compiler, and runtimes for Java and Node.js.

In a task app, a business user might ask, “When a task in the launch project is completed, create a follow-up task.” An AI assistant can draft a Keir extension that reads the completed task, checks its project, and requests a follow-up.

Your application defines when extensions run, what data they receive, and which operations they can call. Keir checks the extension against that model and compiles it. You can simulate the compiled extension before approval and run the same version through the app’s own services. The app supplies the authenticated actor and checks permissions.

Developers can also write extensions by hand. The runtime executes compiled extensions without an AI service.

![An AI assistant drafts a Keir extension from a customer request and the app’s model. Keir compiles it. At execution, the app checks permissions and performs the operation.](docs/images/extension-flow.svg)

## Start here

- [Getting started](docs/quickstart.md). Run a follow-up extension, edit it, and compile your change.
- [Embed Keir](docs/embedding.md). Expose application operations and run a compiled rule from your own code.
- [Keir specification](docs/specs/README.md). Look up the language, model, compiler, and runtime contracts.
- [Explore Daymark](https://github.com/rore/keir-demo-app). See how a task application uses Keir for customer authoring, review, and execution.

<a id="supported-contract"></a>
## Current scope

An extension can read data and contain at most one possible write, including guarded calls. Java and Node.js run the same compiled format. See the [Keir specification](docs/specs/README.md) for supported behavior, [verification](docs/testing.md) for measured results and their limits, and the [release plan](roadmap/features/public-package-release.md) for package availability.

## Documentation

- [Documentation index](docs/README.md)
- [Contributing](CONTRIBUTING.md)
- [Roadmap](roadmap/scope.md)

Keir Core is licensed under the [Apache License 2.0](LICENSE). See [GitHub Releases](https://github.com/rore/keir-core/releases) for packages.
