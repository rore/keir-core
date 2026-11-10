# Keir

Keir lets developers add AI-assisted customization to their applications. Business users describe the behavior they want in plain language, and an AI assistant turns their requests into extensions using the capabilities the application exposes.

<a id="customizing-an-application"></a>
Keir provides a constrained extension language, a compiler that checks extensions against the application's model, and a runtime that executes them through the application's services. The application stays in control of data access, permissions and effects. Once activated, extensions run without further AI requests.

![An AI assistant drafts a Keir extension from a customer request and the app’s model. Keir compiles it. At execution, the app checks permissions and performs the operation.](docs/images/extension-flow.svg)

<a id="from-a-request-to-a-running-extension"></a>
## From a business request to application behavior

A user of a task application asks: “When a task in the launch project is completed, create a follow-up task.”

The [follow-up example](examples/node-host/follow-up/source.yaml) uses `project1` as the selected launch project. It reads the task, checks its project and completed status, and requests a new task titled `Follow up` when both match.

Before activation, the application can simulate the extension with sample data:

| Simulated task completion | Result |
| --- | --- |
| Completed task in `project1` | Requests a `Follow up` task in `project1`. |
| Completed task in another project | Does not request a new task. |

After review and activation, the application runs the compiled extension after it has saved the task's completion. Its task service checks permissions and business rules before creating the follow-up. The extension cannot prevent the completion that has already been saved.

<a id="how-it-fits-your-application"></a>
## What you embed

Embed the Java or Node.js runtime in your application. Both run the same compiled extension format. Use the compiler when creating or changing an extension.

The application model defines where extensions can run, what input data they receive, and which operations they can call. Your application implements those operations through its own services. It also provides the authoring assistant, simulation, review and activation flow, and storage.

[Daymark](https://github.com/rore/keir-demo-app) demonstrates these pieces in a task application, from a user's request through review and activation to execution.

<a id="supported-contract"></a>
<a id="current-scope"></a>
## What you can build today

- After a change is saved: react to an event, such as a task completion, by reading application data and conditionally requesting a change.
- Before a change is saved: check a proposed operation. For example, require a due date for a task tagged `client` before allowing the application to continue with task creation.

An allow decision lets the application continue its own permission and business-rule checks. It does not grant permission or mean the task has been saved. If the check denies the request or cannot complete, the application blocks the attempt.

An event extension may include at most one call that can change data, even if conditional. Pre-operation checks are read-only. Steps, calls and data size are bounded. See the [language reference](docs/specs/language.md) for supported expressions and limits.

## Start here

- [Install released packages](docs/installation.md): use published compiler and runtime packages in your application without building this repository.
- [Run the repository example](docs/quickstart.md): build local packages, run the follow-up extension and change its title.
- [Understand the flow](docs/guided-tour.md): read the example's source and application model together.
- [Integrate Keir](docs/embedding.md): expose application operations and call the compiler and runtime from your code.
- [Build an authoring assistant](docs/agents/extension-assistant.md): connect a customer conversation to compilation, simulation and review.

## Documentation

The [documentation index](docs/README.md) links the guides and references.

- [Use a coding assistant for integration](docs/agents/host-integration-assistant.md).
- [Keir specification](docs/specs/README.md): language, application model, compiler and runtime contracts. Use the [compatibility matrix](docs/specs/compiler-artifact.md#source-and-artifact-identity) when selecting packages.
- [Verification and testing](docs/testing.md): tested behavior and its evidence.
- [Contributing](CONTRIBUTING.md) and [roadmap](roadmap/scope.md).

Keir Core is licensed under the [Apache License 2.0](LICENSE); bundled components retain their [third-party notices](THIRD-PARTY.md). Packages are available through [GitHub Releases](https://github.com/rore/keir-core/releases).
