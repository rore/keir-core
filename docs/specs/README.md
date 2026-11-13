# Keir specification

The specification defines the language, host model, compiled artifact and execution rules. Start with [Getting started](../quickstart.md) or [Embed Keir](../embedding.md) for a walkthrough.

<a id="current-profile"></a>
<a id="current-behavior"></a>
## Current contract

Published Core 0.4.0 uses source `keir/2`, model `keir-dcm/3`, artifact `keir-artifact/3` and scenario `keir-scenario/3`. Development Core 0.5 retains that source/model authoring pair and adds numeric source `/3` with model `/4`; both new profiles emit artifact/scenario `/4` under producer 0.5.0. Use the [exact compatibility matrix](compiler-artifact.md#source-and-artifact-identity), [released-package installation guide](../installation.md) and [companion catalog](../agents/authoring-companion.md), not matching suffixes alone.

Core 0.5 is a **qualified unpublished candidate**. Compiler/Java/Node checks, installed packages, the corrected edition-9 companion and Daymark's tested adoption journey are complete and manager-accepted; see [numeric qualification status](#numeric-qualification-status). This is not a public release, live-provider qualification or an automatic host upgrade. Published Core 0.4.0 remains nonnumeric.

The host chooses one of two lifecycles. An event extension runs after the original operation commits and may contain at most one possible write. A pre-operation decision runs before the native operation, may only read, and returns a typed `ALLOW` or `DENY`. The host consumes that decision and retains its native permission, consistency and commit checks. Both runtimes execute the same artifact bytes.

| Topic | Reference |
| --- | --- |
| Source, types, expressions, guards and data flow | [Language](language.md) |
| Integer/Decimal strings and exact normalization helpers | [Numeric values](domain-model.md#numeric-values) · [Host helpers](host-model-binding.md#exact-numeric-text-helpers) |
| Hooks, capabilities, types and call limits | [Domain capability model](domain-model.md) |
| Compilation, diagnostics, formats and compatibility | [Compiler and artifact](compiler-artifact.md) |
| Java and Node APIs, protected context and native key | [Host binding and invocation](host-model-binding.md) |
| Execution, limits, outcomes and write evidence | [Runtime semantics](runtime.md) |
| Numeric arithmetic/rounding, lists, optional values and elapsed time | [Workflow expressions](workflow-expressions.md) |
| Pre-operation decision and host commit obligations | [Decision contract](pre-operation-decisions.md) · [Walkthrough](../decision-walkthrough.md) |
| Ordered simulation and outcome consumption | [Verification scenarios](single-effect.md#ordered-simulation-protocol-and-acceptance) |

The [format matrix](compiler-artifact.md#source-and-artifact-identity) identifies the structural schemas. Schemas check document shape; these references also define typing, availability, lifecycle and permitted effects. [Conformance fixtures](../../conformance/README.md) and [verification records](../testing.md) identify actual tested scope.

## Numeric qualification status

The [accepted numeric feature record](../../roadmap/features/numeric-values-and-rules.md#manager-accepted-core-contribution) owns final Core results, exact package/source pins and the separately qualified corrected companion. Its [Daymark acceptance](../../roadmap/features/numeric-values-and-rules.md#daymark-green-journey-and-combined-acceptance) records the complete authoring, simulation, approval, separate activation and native-enforcement journey. Earlier checkpoints retain their original scope and hashes there; they are not current blockers. Select [companion edition 9](../agents/authoring-companion.md#qualified-candidate-edition-9) only with its exact supported packages and host definition.

Full numeric qualification is a [manually dispatched workflow](../../.github/workflows/numeric-fast-regression.yml), with modes that can select frozen source revisions. A previous passing run does not prove that every future change receives full qualification. Use [ordinary regression and release checks](../testing.md) for the change being delivered.

### Descriptor and bounded runtime evidence

Exact cross-runtime descriptor comparisons and [bounded actual-runtime measurements](../../roadmap/features/numeric-values-and-rules.md#exact-descriptors-and-actual-runtime-measurements) passed. Measurements use 1,000 samples per phase, four Java threads and four nonparallel same-isolate Node lanes. Decimal operand/result reparsing remains uninstrumented. Fixed order, JIT/GC and sampled memory preclude engine rankings, parse-once claims, production-capacity certification or hard-memory guarantees. Host calls and the surrounding application need representative workload measurements of their own.

## Compatibility

Each identifier versions one contract independently.

| Identifier | What it governs |
| --- | --- |
| `keir/2`, development `keir/3` | Source syntax, typing and expression meaning. |
| `keir-dcm/3`, development `keir-dcm/4` | Host model structure, types, hooks, capabilities and lifecycle rules. |
| `keir-artifact/3`, development `keir-artifact/4` | Compiler output encoding and execution meaning. |
| `keir-scenario/3`, development `keir-scenario/4` | Simulation inputs and expected outcomes, calls and consumption. |
| `keir-record-descriptors/2`, development `/3` | Derivation of typed protobuf records used by CEL. |

Package versions identify compiler and runtime releases. Equal suffixes or package version numbers alone establish no compatibility. Preparation checks the complete artifact/model/producer/descriptor/environment tuple. Binding also checks the capability versions and native key type supplied by the host.

The [exact matrix](compiler-artifact.md#source-and-artifact-identity) defines supported tuples and retained execution. [Guarded-result availability](language.md#steps-and-data-flow) is producer-specific; old tuples retain their semantics. Development 0.5 deliberately retains current `/2` source + `/3` model authoring, but always emits its own producer and artifact identity. This does not emulate every historical compiler. The [existing examples](../../examples/README.md#guarded-result-scalar-dataflow) identify the producer they exercise.

Keep approved artifacts with their original model snapshots and bytes. Choose the host's supported authoring pair explicitly; numeric adoption needs a new `/4` model. Recompilation creates a new artifact requiring fresh pins and approval, even when the model remains `/3`. Package installation must not silently recompile, transfer approval or deactivate existing selections. Core numeric types enforce no units or currencies; host authority and native checks remain unchanged.

Earlier `/1` and preview contracts require their matching historical tooling. Current tooling rejects them. See [documentation history](../internal/history/README.md) and the [Core 0.1.0 specification snapshot](https://github.com/rore/keir-core/tree/0cae8d4417e19882e4355960b168f49d092f18e6/docs/specs).

## Maintaining a contract

Update affected specifications, schemas, fixtures and Java/Node conformance together. Keep rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
