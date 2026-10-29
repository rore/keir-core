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

The host supplies current grants, protected actor binding, authorization and a monotonic deadline. It validates the output before display. Fake READ adapters use explicit simulation registration and cannot be selected through a real-mode admission. No effect, transaction guard, task application or live activation is implemented.
