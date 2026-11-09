# Keir

Keir is an embeddable framework for AI-assisted application extensions. It provides a controlled extension language, a compiler and a runtime, so an agent can turn a business user’s request into behavior the application can safely execute.

The application defines where extensions can run, what data they can use, and which capabilities they can call. The agent builds an extension from those approved pieces; Keir checks it against the application’s model and runs it through the application’s own operations. The application remains in control of its data, permissions and effects.

For example, a user of a task application might ask: “When a task in the launch project is completed, create a follow-up task.” The agent expresses that request as a Keir extension, using the task data and task-creation capability exposed by the application.

**Let AI extend your app. Keep the app in control.**

![An AI assistant drafts a Keir extension from a customer request and the app’s model. Keir compiles it. At execution, the app checks permissions and performs the operation.](docs/images/extension-flow.svg)

## Customizing an application

Different teams using the same application often need different rules. If every rule becomes a feature or setting, each new variation depends on the application team. Keir lets developers expose reusable operations so customers can describe how those operations should be combined.

## From a request to a running extension

For the follow-up request above, the application and Keir work together like this:

1. The developer defines an extension point: a moment after task completion when an extension can run. The application exposes the completed task's ID, an operation to read its project and status, and an operation to create a task. Its model describes those inputs, operations and their types.
2. The assistant writes a Keir program that reads the completed task, checks its project and status, and requests a follow-up only when the condition matches. The chosen project and follow-up title are part of that program.
3. Keir's compiler checks the program against the application's model, including the data types, available operations and call limits. It produces the compiled extension that the application will run.
4. The application provides the review and activation process. For example, it can show simulated matching and nonmatching cases, let a person review the exact compiled version, and separately turn that version on.
5. When a later matching task completion occurs, the runtime evaluates the extension and calls the application's operations. The application checks permissions and performs the write. This execution needs no further AI request.

The runnable follow-up example uses simulated task operations. It includes a matching task, a task that needs no follow-up, and a write whose outcome is uncertain.

## How it fits your application

Keir Core supplies the extension language, compiler, and runtimes for Java and Node.js. Both runtimes execute the same compiled format. An extension can use the data and operations described by the application's model; the runtime calls implementations supplied by the application.

You supply those implementations and decide when extensions run. You also build the assistant integration, customer conversation, review screens, approval and activation process. Your application retains its permissions, data access and storage responsibilities. Passing compilation or simulation does not grant permission to perform an operation.

[Daymark](https://github.com/rore/keir-demo-app) is a task application that brings these parts together. Its assistant drafts extensions, people review and activate them, and task operations run through the application's own services.

<a id="supported-contract"></a>
<a id="current-scope"></a>
## What you can build today

An extension can react to an event after an application operation commits, read related data and conditionally call an operation that changes data. It can also check a proposed operation before it commits and return an allow-or-deny decision. That check uses only reads. An allow decision lets the application continue its own validation; a deny decision or failed check blocks the operation.

Conditions can use optional values, timestamps and membership in small lists supplied by the application. Programs have fixed bounds on steps, calls and data size. An event extension may contain at most one call that can change data; putting a call behind a condition still counts. See the [language reference](docs/specs/language.md) for supported expressions and limits, and the [compatibility matrix](docs/specs/compiler-artifact.md#source-and-artifact-identity) when selecting packages.

## Start here

- [Run an example](docs/quickstart.md): run the follow-up extension, change its title and compile your version.
- [Understand the flow](docs/guided-tour.md): read the example's source and application model together.
- [Integrate Keir](docs/embedding.md): expose application operations and call the compiler and runtime from your code.

## Documentation

The [documentation index](docs/README.md) links the guides and references.

- [Install released packages](docs/installation.md) without building this repository.
- [Build a customer authoring assistant](docs/agents/extension-assistant.md), or [use a coding assistant for integration](docs/agents/host-integration-assistant.md).
- [Keir specification](docs/specs/README.md): language, application model, compiler and runtime contracts.
- [Verification and testing](docs/testing.md): tested behavior and its evidence.
- [Contributing](CONTRIBUTING.md) and [roadmap](roadmap/scope.md).

Keir Core is licensed under the [Apache License 2.0](LICENSE). Packages are available through [GitHub Releases](https://github.com/rore/keir-core/releases).
