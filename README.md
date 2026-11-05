# Keir Core

Keir is an embeddable framework for customer-defined extensions. It provides a constrained language, a compiler, and runtimes for Java and Node.js. Extensions combine operations your application exposes.

In a task app, a customer might ask, “When someone completes a task in the launch project, create a follow-up.” An AI assistant can turn that request into a Keir extension using the app’s existing task operations. A developer can write the same extension by hand. Either way, the app can run the compiled extension without an AI service.

Your application defines when an extension can run, what data it receives, and which operations it can call. Keir compiles the extension against that contract, so you can simulate it before approval and run the same compiled version with execution limits. Your application checks permissions and performs each operation through its own services.

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
