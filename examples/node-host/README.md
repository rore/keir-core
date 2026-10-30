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
