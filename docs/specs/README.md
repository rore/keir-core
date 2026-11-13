# Keir specification

The specification defines the language, host model, compiled artifact and execution rules. Start with [Getting started](../quickstart.md) or [Embed Keir](../embedding.md) for a walkthrough.

<a id="current-profile"></a>
<a id="current-behavior"></a>
## Current contract

Published Core 0.4.0 uses source `keir/2`, model `keir-dcm/3`, artifact `keir-artifact/3` and scenario `keir-scenario/3`. Development Core 0.5 retains that source/model authoring pair and adds numeric source `/3` with model `/4`; both new profiles emit artifact/scenario `/4` under producer 0.5.0. Use the [exact compatibility matrix](compiler-artifact.md#source-and-artifact-identity), [released-package installation guide](../installation.md) and [companion catalog](../agents/authoring-companion.md), not matching suffixes alone.

Numeric source/runtime programs, teaching commands and independently installed candidate packages now have distinct passing evidence; see [numeric qualification status](#numeric-qualification-status). The [draft edition 9](../agents/customer-authoring-guide-v9.md) still awaits matching companion qualification and final source/asset freeze. No public Core 0.5 release, Daymark adoption or full-feature acceptance is claimed. Older released packages do not support numeric types.

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

| Evidence | Verified scope | Not established by it |
| --- | --- | --- |
| [Program run 43413093006](https://github.com/rore/keir-core/actions/runs/43413093006) | Both runtimes passed 27 independent stock/quote cases and retained-nominal-name execution, following the shared 97-case compiled batch. | Teaching commands or installed packages from that run alone. |
| [Teaching run 43422161327](https://github.com/rore/keir-core/actions/runs/43422161327) | 224 Java, 280 Node runtime, 19 source-setup and 72 packed-Node-host tests, zero skips/failures. Actual stock/quote Java shaded CLI and installed-Node CLI reports match complete independent scenarios. Both scenario `/4` tuples pass; old/default consumer and receipt checks remain unchanged. | Independent release assets or a real domain service. |
| [Candidate run 43426804326](https://github.com/rore/keir-core/actions/runs/43426804326) | Same source/teaching checks, five builder and seven verifier tests. The packaged compiler reproduces exact stock/quote, retained-name and boundary artifacts. Independently installed Java and Node each pass 130 complete numeric/guarded/historical outcomes plus old host examples; 34 callable Java and 35 Node helper vectors pass. Java's non-String helper case is compile-time-only, not executed. | Public release, matching edition-9 companion, Daymark journey or full-feature acceptance. |

The candidate run used source `4d4b1994aaaa0726cb45a19145f852431e03313f`; job time was 168 seconds and package stage 32 seconds. Its unpublished Actions artifact `numeric-candidate-43426804326-1` contains these three archives plus `LICENSE` and `SHA256SUMS`. All five asset sizes/hashes and all 49 evidence payload hashes were independently checked. These are qualification pins, not final release URLs or companion compatibility pins:

| Candidate archive | Bytes | SHA-256 |
| --- | ---: | --- |
| `keir-compiler-cli-0.5.0.zip` | 8109727 | `ec36271caaa55714257601c2f8bc7aa19f2ced76858f02b15021dacd3a138e76` |
| `keir-java-runtime-maven-0.5.0.zip` | 102558 | `88e9f903b32df239832e7c0a91b2db3a445a8e31bb5066f54578773c8b890e77` |
| `keir-runtime-node-0.5.0.tgz` | 25866 | `d3af9430fde822eb86530da622b0adb17f802c4a0ec1ec2d9e528e38c40e0598` |

Later integrated guidance does not replace that candidate's source identity. Final source/assets, the matching companion payload, exact descriptor evidence and actual-runtime measurements still need their recorded qualification. Raw engine/component timings are historical precursor evidence, not application benchmarks. Published Core 0.4.0 and its immutable guides/assets remain unchanged; candidate qualification does not upgrade a host or transfer approvals.

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
