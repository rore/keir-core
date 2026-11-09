# Keir

Keir is an embeddable framework for AI-assisted application extensions. It provides a controlled extension language, a compiler and a runtime, so an agent can turn a business user’s request into behavior the application can safely execute.

The application defines where extensions can run, what data they can use, and which capabilities they can call. The agent builds an extension from those approved pieces; Keir checks it against the application’s model and runs it through the application’s own operations. The application remains in control of its data, permissions and effects.

For example, a user of a task application might ask: “When a task in the launch project is completed, create a follow-up task.” The agent expresses that request as a Keir extension, using the task data and task-creation capability exposed by the application.

**Let AI extend your app. Keep the app in control.**

![An AI assistant drafts a Keir extension from a customer request and the app’s model. Keir compiles it. At execution, the app checks permissions and performs the operation.](docs/images/extension-flow.svg)

## Start here

- [Getting started](docs/quickstart.md). Run a follow-up extension, edit it, and compile your change.
- [Embed Keir](docs/embedding.md). Expose application operations and run a compiled rule from your own code.
- [Keir specification](docs/specs/README.md). Look up the language, model, compiler, and runtime contracts.
- [Explore Daymark](https://github.com/rore/keir-demo-app). See how a task application uses Keir for customer authoring, review, and execution.

<a id="supported-contract"></a>
## Current scope

An extension can read data and contain at most one possible write, including guarded calls. The pre-operation decision contract allows only reads and returns `ALLOW` or `DENY` before the application performs its own operation. The application still checks permissions and business constraints before committing.

Java and Node.js run the same compiled format. [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0) includes the current contracts and authoring companion edition 5. See the [specification](docs/specs/README.md) for supported behavior and [verification](docs/testing.md) for tested scope.

## Documentation

- [Documentation index](docs/README.md)
- [Contributing](CONTRIBUTING.md)
- [Roadmap](roadmap/scope.md)

Keir Core is licensed under the [Apache License 2.0](LICENSE). See [GitHub Releases](https://github.com/rore/keir-core/releases) for packages.
