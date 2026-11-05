# Keir Core

Keir lets software teams offer AI-assisted customization inside their applications. A business user can ask for behavior the app has no setting for, such as “When someone completes a task in the launch project, create a follow-up.” AI can turn that request into a script. Running arbitrary scripts means the application has to contain untrusted code and control its access to data and resources.

An AI assistant can draft the extension in Keir’s constrained language. Developers can also write extensions by hand. The application defines when extensions can run, what data they receive, and which operations they can call. Keir’s compiler checks the extension against that contract and produces a program the user can try in simulation before approving it.

Once approved, that same compiled program runs inside the application. Keir enforces execution limits and calls only the operations the application exposed. The application checks permissions and carries out each operation through its own services.

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

Keir Core is licensed under the [Apache License 2.0](LICENSE). Public package releases are planned.
