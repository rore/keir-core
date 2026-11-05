# Keir specification

The specification defines the documents Keir accepts and how a compiled extension runs. For a walkthrough, [get started](../quickstart.md) or [embed Keir](../embedding.md).

<a id="current-profile"></a>
## Current behavior

The current single-write contract runs after a host event commits. An extension can read data and contain at most one possible write, including writes behind guards. Both Java and Node.js can run the same compiled bytes with real or simulated host implementations. The host owns permissions and native commits.

The compiler and runtimes use exact, versioned formats. See [document and compiler identities](compiler-artifact.md#source-and-artifact-identity) for the values to use when compiling or validating documents.

| Topic | Contract |
| --- | --- |
| Source, types, guards, and data flow | [Language](language.md) |
| Hooks, capabilities, and call and write policy | [Domain capability model](domain-model.md) |
| Compilation, diagnostics, and serialized artifact | [Compiler and artifact](compiler-artifact.md) |
| Node and Java APIs, adapter context, and native key | [Host binding and invocation](host-model-binding.md) |
| Execution, limits, outcomes, and traces | [Runtime semantics](runtime.md) |
| Verification scenario format and acceptance | [Verification scenarios](single-effect.md#ordered-simulation-protocol-and-acceptance) |

The [source](../../schemas/source-1/source.schema.json), [DCM](../../schemas/dcm-1/dcm.schema.json), [artifact](../../schemas/artifact-1/artifact.schema.json), and [scenario](../../schemas/scenario-1/scenario.schema.json) schemas validate document structure. This specification defines semantic rules such as matching types, available values and permitted effects.

The [structural checker](../../scripts/check-keir-contracts.py) checks document shape. The [compiler tests](../../compiler-java/src/test/java/dev/keir/compiler/GenericHookCompilerTest.java) check source and model semantics. The [shared scenarios](../../conformance/keir-1/README.md) check invocation outcomes and write evidence in both runtimes. See [verification and resource evidence](../testing.md#current-single-write-behavior-verification) for the tested scope.

## Compatibility

The current compiler accepts `keir/1` source and `keir-dcm/1` models. The runtimes bind `keir-dcm/1` and prepare `keir-artifact/1` against the exact model. See [documentation history](../internal/history/README.md) for earlier formats and APIs.

## Maintaining a contract

Specify feature changes in the affected versioned contracts. Update the applicable schemas, fixtures, and Java and Node conformance evidence together. Keep design rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
