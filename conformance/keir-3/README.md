# Core 0.3 conformance fixtures

This is the current shared corpus for `keir/2` source, `keir-dcm/3` models, `keir-artifact/3` artifacts and `keir-scenario/3` simulation. It covers post-commit event extensions and read-only pre-operation decisions. See the [contract matrix](../../docs/specs/compiler-artifact.md#source-and-artifact-identity) for exact version compatibility.

Source and model files are compiler inputs. Artifact files are pinned compiled outputs. `scenarios/` supplies invocation inputs, simulated adapter responses and independently specified outcomes; raw verification vectors exercise parsing and rejection boundaries. Both runtimes consume the same bytes and expectations.

From the repository root, use [Contributing](../../CONTRIBUTING.md#build-and-test) to install local dependencies and run the default suites. Maven verification includes the Java corpus. Run the Node corpus explicitly with a Python environment containing [the pinned checker dependencies](../../scripts/requirements-read2-contracts.txt):

```sh
node runtime-node/test/scenario-conformance.mjs python 3
```

Replace `python` with that environment's Python executable if needed. The explicit `3` selects this corpus; omitting it selects the retained `/2` corpus. The [contract workflow](../../.github/workflows/keir-contract.yml) also compares normalized Java and Node outputs.

See [current verification evidence](../../docs/testing.md#current-single-write-behavior-verification) and the [parent conformance guide](../README.md). Historical corpora require their matching producer and contract versions.
