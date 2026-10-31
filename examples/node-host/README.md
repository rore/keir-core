# Node host example

This command-line host consumes the Java-produced shared artifact through a locally packed Node library. It shows completed/open/missing tasks and a deliberately wrong-resource fake, with separate runtime outcomes and DISPLAYED/NOT_DISPLAYED consumption. All users/tasks are fictional fixture data; no real authorization or persistence is certified.

From the repository root, create the ignored package directory if needed and run:

```powershell
npm.cmd pack ./runtime-node --pack-destination .tools/packages --ignore-scripts
npm.cmd ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
node examples/node-host/run.mjs conformance/read-and-decide/artifact.json sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424 sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f
node --test examples/node-host/test.mjs
```

The fixed digests select the reviewed sample artifact/model outside the artifact. A different source or model needs new host selection. Do not read an artifact's self-declared hash as authority. Java is needed only for compilation or fixture regeneration; this host execution uses the packed Node package and its pinned dependencies.

The first two scenarios display DONE and PENDING. Missing task returns declared TASK_NOT_FOUND and NOT_DISPLAYED. The wrong-resource fake fails the host adapter's requested-ID check and is NOT_DISPLAYED; this relationship belongs to the host adapter, not generic runtime task logic. Public runtime traces contain no argument/identity values; trusted sample call captures are printed separately for inspection.

The read host supplies current grants, protected actor binding, authorization and a monotonic deadline. It validates the output before display. Fake READ adapters use explicit simulation registration and cannot be selected through a real-mode admission.

The separate effect command runs the fixed [single-effect corpus](../../conformance/single-effect/README.md) through installed `@keir/runtime-node`, with the same Java-produced artifact and independently held pins:

```sh
node examples/node-host/effect.mjs conformance/single-effect/artifact.json sha256:d441413373a59a0a40a509143146fef8fe9c1c2426bee430965efddee591f31c sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77
node --test examples/node-host/test.mjs
```

It emits deterministic rows containing runtime outcomes, trusted call captures and separate EVENT_REPORT receipts. Handled rejection remains BUSINESS_REJECTION even with a result. Failed execution may retain a CONFIRMED effect; lost response is UNCERTAIN with UNKNOWN, without an ordinary result. The consumer validates closed outcome/result/error/effect/trace envelopes independently; it does not infer external effects from source-authored labels.

The simulator module imports no runtime and receives the selected API from its caller. Default conformance tests inject the source library before host installation; this command injects only the installed package. Its local corpus and trusted fixtures are part of this repository example, with no sibling runtime-source import. Both commands preserve their original native keys and keep them outside public traces. No task application, live activation, real deduplication or persistence guarantee is implemented.

The separate `/2` host binds the complete reviewed catalog once, prepares the fixed Java-produced program variants, and executes each scenario with fresh scope, authorization and native context:

```sh
node examples/node-host/generic.mjs conformance/generic-hooks/read
node examples/node-host/generic.mjs conformance/generic-hooks/effect
node --test examples/node-host/test.mjs
```

The directory is trusted operator input for this qualification command. Model/artifact pins are separate reviewed files; program names map only to fixed basenames. Scenario data cannot select paths, callable code, credentials, mode, keys or authority. All 9 read and 19 effect scenarios use the ordinary installed library; default source tests inject the source library into the same simulator before host installation. Both batches emit `{id,outcome,calls,consumption}` rows and compare them with independently authored expectations.

The `/2` input retains the hook's actual task ID. Project and title are compiled customer choices; changing only those source literals does not change the model, input or adapter. The generic creation bridge receives only project/title business arguments plus separate protected actor and original key/event context. Request-local fixture state uses Node's `AsyncLocalStorage`; no process-global current user is used. The authorizer independently checks the entrypoint, published capability and exact version before fixture decisions. Captures are paired with actual dispatch traces by ordinal, and an external mismatch ledger prevents caught adapter exceptions from hiding a failed simulation.

The local harness permits four live model handles, eight Prepared handles, one synchronous bind/prepare operation and two in-flight invocations, with no queue. It rejects raw model bytes above 65,536 and artifact bytes above 1,048,576 before calling the library. Each model slot reserves the full 65,536-byte canonical-model and descriptor ceilings, bounding each aggregate to 262,144 without another descriptor parser. Prepared slots bound retained artifact selection to 8,388,608 bytes; pinned/in-flight users prevent early release. Capacity rejection has the host test reason `CAPACITY_EXCEEDED`, not a fabricated Keir outcome. Bounded file reads and a 1 MiB output ceiling protect the command's fixed batch. These controlled limits do not claim production sizing, decoded-heap isolation or revocation/retirement services.
