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
| [Descriptor/runtime/candidate run 43431976777](https://github.com/rore/keir-core/actions/runs/43431976777) | 227 Java, 281 Node runtime, 19 setup and 72 packed-host tests, zero failures/skips; 130 complete installed outcomes per runtime. Seven exact independent descriptor comparisons and bounded real-rule runtime measurements pass. | Public release, matching edition-9 companion, Daymark journey, engine ranking or full-feature acceptance. |

The candidate run used source `4d4b1994aaaa0726cb45a19145f852431e03313f`; job time was 168 seconds and package stage 32 seconds. Its unpublished Actions artifact `numeric-candidate-43426804326-1` contains these three archives plus `LICENSE` and `SHA256SUMS`. All five asset sizes/hashes and all 49 evidence payload hashes were independently checked. These are qualification pins, not final release URLs or companion compatibility pins:

| Candidate archive | Bytes | SHA-256 |
| --- | ---: | --- |
| `keir-compiler-cli-0.5.0.zip` | 8109727 | `ec36271caaa55714257601c2f8bc7aa19f2ced76858f02b15021dacd3a138e76` |
| `keir-java-runtime-maven-0.5.0.zip` | 102558 | `88e9f903b32df239832e7c0a91b2db3a445a8e31bb5066f54578773c8b890e77` |
| `keir-runtime-node-0.5.0.tgz` | 25866 | `d3af9430fde822eb86530da622b0adb17f802c4a0ec1ec2d9e528e38c40e0598` |

The latest qualified unpublished candidate instead uses source `b9782d1cb7dd7106d44878f7199cbd4774e91510` and run `43431976777` (121 seconds; package stage 26 seconds). Its Actions artifact is `numeric-candidate-43431976777-1`. All five assets (8250510 bytes) and all 60 evidence payload hashes (672331 bytes) were independently verified. Earlier qualification above remains tied to `4d4b1994`, not relabeled as this source:

| Latest candidate archive | Bytes | SHA-256 |
| --- | ---: | --- |
| `keir-compiler-cli-0.5.0.zip` | 8109727 | `ec36271caaa55714257601c2f8bc7aa19f2ced76858f02b15021dacd3a138e76` |
| `keir-java-runtime-maven-0.5.0.zip` | 102558 | `88e9f903b32df239832e7c0a91b2db3a445a8e31bb5066f54578773c8b890e77` |
| `keir-runtime-node-0.5.0.tgz` | 26305 | `9c59e4722f4d85bcd0e57580a1cef9526f7ceb52670e345576e39ca29d489736` |

Latest `SHA256SUMS` SHA-256 is `58b025d40c8e43c9a7ce18ad7f000e45a2414fe0a318545dc9542f645d1922df`. Only the Node package README changed from the earlier candidate; its runtime source and metadata bytes stayed identical. These are still qualification pins, not final public-release or companion pins.

### Descriptor and bounded runtime evidence

Run `43431976777` retains the exact reports below in Actions artifact `numeric-fast-43431976777-1`. These measurements use actual source runtimes, not independently installed packages or raw CEL/component probes.

| Evidence/report | Passed scope and limits |
| --- | --- |
| `011-numeric-descriptors.json` | Seven independent compiler/Java/Node byte comparisons: expression-only, numeric optionals, Timestamp/numeric, retained current recipe `/2`, retained nominal names, stock and quote. Payload: 11388 bytes; SHA-256 `d00bd1d654a0e8ae08f05c888844b917cd7bf6b5e4cd00a20ac7ffbdc67fb601`. |
| `017-java-runtime-report.json`, `018-node-runtime-report.json`, `019-runtime-workload.json` | Three genuine four-BIND Boolean/Integer/Decimal rules. Each family has six phases (`bind`, `prepare`, `validateProject`, `invoke`, `validateResult`, `reject`), each with 100 warmups/1000 samples, plus three 1000-request shared-plan batches. Reports include p50/p95/max and throughput. Independent complete-outcome oracles check returned owner and request index, not host event authority. Java uses four threads; Node uses four nonparallel same-isolate lanes. |
| Same runtime reports | Direct boundary parses/projections per chain are Boolean `0/4`, Integer `2/1`, Decimal `2/1` in both runtimes. Retained Decimal operand/result reparsing is **uninstrumented**; no coefficient memoization or parse-once claim. Repeated requested-GC heap samples are recorded. Java allocation covers the measuring thread's six phase loops, not per-invocation or concurrent-worker allocation; Node allocation is unsupported. |
| `016-runtime-manifest.json`, `020-java-runtime-process.json`, `021-node-runtime-process.json` | Linux Azure x64, AMD EPYC 9V74, four visible logical CPUs, 16759439360 host RAM bytes; no exposed cgroup quota data. Java 21.0.13 child: 5724 ms, sampled RSS 265351168 bytes. Node v24.22.2 child: 4725 ms, sampled RSS 276799488 bytes. Both process groups closed and bounds passed; 100 ms RSS sampling can miss spikes. |

Fixed phase order, JIT/GC effects and unequal Java/Node parallelism preclude engine rankings. These are bounded runtime observations, not whole-application benchmarks or hard-memory guarantees. Final source/assets and the matching companion payload remain open gates. Published Core 0.4.0 and its immutable guides/assets remain unchanged; candidate qualification does not upgrade a host or transfer approvals.

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
