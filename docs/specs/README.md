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

Each identifier versions one contract. A change to its accepted structure or meaning changes the identity of that contract.

| Identifier | What it governs |
| --- | --- |
| `keir/1` | Extension source syntax, typing and meaning. |
| `keir-dcm/1` | The model's structure and the meaning of its types, hooks, capabilities and limits. |
| `keir-artifact/1` | The compiler output's encoding and what runtimes do when they execute it. |
| `keir-scenario/1` | The test document's structure and the meaning of its fixtures and expectations. |

These contracts version independently. For example, a new source syntax can keep the same artifact format if it compiles to instructions with unchanged meaning. A change in the meaning of those instructions requires a new artifact identity even if their encoding stays the same.

Package versions identify releases of the compiler and runtimes. Choose a tested combination from the release documentation. The shared `/1` suffix and matching package version numbers alone establish no compatibility. The compiler checks the source's exact model identity. At preparation, the runtime checks the artifact format, producer, dependency environment and model reference against the bound model. The host also supplies the exact capability versions declared in that model. A host-owned domain or capability version describes the host's business contract, independently of the Keir document formats. [Artifact identity](compiler-artifact.md#source-and-artifact-identity) · [Core package record](../agents/authoring-companion-v3-compatibility.json)

Current tooling rejects earlier preview formats. To adopt the current contract, bring the source and model into the supported formats, compile again, and review the new artifact before approval. Keep old artifacts and approvals with their original bytes. [Earlier formats and APIs](../internal/history/README.md)

## Maintaining a contract

Specify feature changes in the affected versioned contracts. Update the applicable schemas, fixtures, and Java and Node conformance evidence together. Keep design rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
