# Current `/3` conformance fixtures

Root historical outputs retain compiler 0.3.1. Unchanged original 0.3.0 event and decision bytes remain under `compatibility/0.3.0/`. Both runtimes execute those bytes as compatibility regressions. Separately pinned current-compiler outputs of the existing sources live under `producers/0.4.0/`; recompilation never replaces a historical artifact or approval. Formats, model bytes and engine pins remain unchanged.

The new `source-guarded-result*.json` sources, their `artifact-guarded-result*.json` outputs and two `dcm-guarded-result*.json` models exercise producer `keir-compiler-java/0.4.0`. The [compatibility matrix](../../docs/specs/compiler-artifact.md#source-and-artifact-identity) defines support; source conformance alone does not qualify installed packages. Their 15 scenarios cover actual scalar results and false-guard omission, optional absent/empty/false values, guarded READ decisions, unused declarations, a later rejection handler, transient/rejected/uncertain calls and confirmed evidence followed by expiry. Each scenario specifies the complete calls, outcome, trace, effects and consumption independently. Raw malformed/forged and direct sparse-activation cases also run in the focused public runtime regressions.

This is the current shared corpus for `keir/2` source, `keir-dcm/3` models, `keir-artifact/3` artifacts and `keir-scenario/3` simulation. It covers post-commit event extensions and read-only pre-operation decisions. See the [contract matrix](../../docs/specs/compiler-artifact.md#source-and-artifact-identity) for exact version compatibility.

Source and model files are compiler inputs. Artifact files are pinned compiled outputs. `scenarios/` supplies invocation inputs, simulated adapter responses and independently specified outcomes; raw verification vectors exercise parsing and rejection boundaries. Both runtimes consume the same bytes and expectations.

From the repository root, use [Contributing](../../CONTRIBUTING.md#build-and-test) to install local dependencies and run the default suites. Maven verification includes the Java corpus. Run the Node corpus explicitly with a Python environment containing [the pinned checker dependencies](../../scripts/requirements-read2-contracts.txt):

```sh
node runtime-node/test/scenario-conformance.mjs python 3
```

Replace `python` with that environment's Python executable if needed. The explicit `3` selects this corpus; omitting it selects the retained `/2` corpus. The [contract workflow](../../.github/workflows/keir-contract.yml) also compares normalized Java and Node outputs.

The extended `/3` corpus has 54 scenarios and the same 51 raw vectors; the retained `/2` corpus keeps 28 scenarios and 51 vectors. Compare full normalized reports, not just returned values or totals. The compiler tests separately verify deterministic bytes and the new producer pins. Schema checking includes those pins but does not establish independent runtime preparation or approval.

See [current verification evidence](../../docs/testing.md#current-single-write-behavior-verification) and the [parent conformance guide](../README.md). Historical corpora require their matching producer and contract versions.
