# Keir specification

The specification defines the documents Keir accepts and how a compiled extension runs. For a runnable example, follow the [guided tour](../guided-tour.md) or the [Java and Node quickstart](../quickstart.md).

## Current profile

`keir-effect/4` is the current host-bound profile. It supports bounded reads and at most one potential WRITE capability call in a program, including calls behind guards. The host owns permissions and native commit behavior. Keir validates and executes the program and reports the outcome and write evidence.

| Identifier | Meaning |
| --- | --- |
| `keir/v0` | Source language family |
| `keir-effect/4` | Compiler and runtime profile |
| `keir-dcm/effect-4` | Domain capability model format |
| `keir-artifact/effect-4` | Compiled artifact format |

The compiler takes the profile from trusted configuration, such as the CLI's `--profile=keir-effect/4` option. The artifact records the exact model reference and compiler environment. The runtime checks these against the bound model and supported environment during preparation.

| Topic | Contract |
| --- | --- |
| Hooks, capabilities, and ownership | [Generic hooks and capability composition](generic-hook-capability-contract.md#corrected-host-bound-effect4) |
| Source and compilation | [Compiler and artifact](compiler-artifact.md#effect4-identity), with the [language rules](language.md) and the [effect/4 reference fixture](../../conformance/generic-hooks/effect4/) |
| Binding and invocation | [Host model binding](host-model-binding.md#corrected-host-bound-4-binding) and [runtime](runtime.md#corrected-effect4-invocation-boundary) |
| Native effects and receipts | [Host-bound single-write contract](real-single-effect-admission.md#current-host-bound-contract-effect4) |
| Verified scope | [Conformance and test evidence](../testing.md#effect4-verification) and [resource qualification](../../roadmap/features/runtime-resource-qualification.md) |

## Compatibility

| Earlier profile | Reference |
| --- | --- |
| `keir-read/1` | [Language](language.md), [DCM](domain-model.md), [artifact](compiler-artifact.md), [runtime](runtime.md), and [host integration](host-integration.md) |
| `keir-effect/1` | [Single-effect profile](single-effect.md) |
| `keir-read/2` and `keir-effect/2` | [Generic hook delta](generic-hook-capability-contract.md#versioned-delta), [binding](host-model-binding.md#one-binding-boundary), and [worked example](generic-hook-example.md) |
| `keir-effect/3` | [Qualification and admission protocol](real-single-effect-admission.md#compatibility-boundary) |

Each profile retains its own input, artifact, and invocation rules. Moving to another profile requires selecting its contract and recompiling against the corresponding model. The [read/2 simulation contract](read-simulation.md) describes the separate scenario and report formats.

## Maintaining a contract

Specify feature changes in the affected versioned contracts. Update the applicable schemas, fixtures, and Java and Node conformance evidence together. Keep design rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
