# Keir specification

The specification defines the language, host model, compiled artifact and execution rules. Start with [Getting started](../quickstart.md) or [Embed Keir](../embedding.md) for a walkthrough.

<a id="current-profile"></a>
<a id="current-behavior"></a>
## Current contract

These references define source `keir/2`, model `keir-dcm/3`, artifact `keir-artifact/3` and scenario `keir-scenario/3`. Use the [compatibility matrix](compiler-artifact.md#source-and-artifact-identity) for supported compiler/runtime tuples, the [installation guide](../installation.md) to select packages and the [companion catalog](../agents/authoring-companion.md) to select an authoring guide.

The host chooses one of two lifecycles. An event extension runs after the original operation commits and may contain at most one possible write. A pre-operation decision runs before the native operation, may only read, and returns a typed `ALLOW` or `DENY`. The host consumes that decision and retains its native permission, consistency and commit checks. Both runtimes execute the same artifact bytes.

| Topic | Reference |
| --- | --- |
| Source, types, expressions, guards and data flow | [Language](language.md) |
| Hooks, capabilities, types and call limits | [Domain capability model](domain-model.md) |
| Compilation, diagnostics, formats and compatibility | [Compiler and artifact](compiler-artifact.md) |
| Java and Node APIs, protected context and native key | [Host binding and invocation](host-model-binding.md) |
| Execution, limits, outcomes and write evidence | [Runtime semantics](runtime.md) |
| Lists, optional values, elapsed time and descriptor recipe | [Workflow expressions](workflow-expressions.md) |
| Pre-operation decision and host commit obligations | [Decision contract](pre-operation-decisions.md) · [Walkthrough](../decision-walkthrough.md) |
| Ordered simulation and outcome consumption | [Verification scenarios](single-effect.md#ordered-simulation-protocol-and-acceptance) |

The [format matrix](compiler-artifact.md#source-and-artifact-identity) identifies the structural schemas. Schemas check document shape; these references also define typing, availability, lifecycle and permitted effects. [Conformance fixtures](../../conformance/README.md) and [verification records](../testing.md) identify actual tested scope.

## Compatibility

Each identifier versions one contract independently.

| Identifier | What it governs |
| --- | --- |
| `keir/2` | Source syntax, typing and expression meaning. |
| `keir-dcm/3` | Host model structure, types, hooks, capabilities and lifecycle rules. |
| `keir-artifact/3` | Compiler output encoding and execution meaning. |
| `keir-scenario/3` | Simulation inputs and expected outcomes, calls and consumption. |
| `keir-record-descriptors/2` | Derivation of typed protobuf records used by CEL. |

Package versions identify compiler and runtime releases. Equal suffixes or package version numbers alone establish no compatibility. Preparation checks the complete artifact/model/producer/descriptor/environment tuple. Binding also checks the capability versions and native key type supplied by the host.

The [exact matrix](compiler-artifact.md#source-and-artifact-identity) defines supported compiler/runtime tuples and retained execution compatibility. [Guarded-result scalar availability](language.md#steps-and-data-flow) is producer-specific; retained tuples keep their original semantics. The [examples](../../examples/README.md#guarded-result-scalar-dataflow) identify the producer they exercise. Retaining historical artifacts is an execution path, not another authoring compiler mode.

Keep existing approved artifacts with their original model snapshots and bytes. Publish the current authoring catalog as `/3`; compile and review new or edited extensions against it. Recompilation creates a new artifact requiring its own approval. Package installation must not silently recompile or deactivate existing selected extensions.

Earlier `/1` and preview contracts require their matching historical tooling. Current tooling rejects them. See [documentation history](../internal/history/README.md) and the [Core 0.1.0 specification snapshot](https://github.com/rore/keir-core/tree/0cae8d4417e19882e4355960b168f49d092f18e6/docs/specs).

## Maintaining a contract

Update affected specifications, schemas, fixtures and Java/Node conformance together. Keep rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
