<a id="run-your-first-keir-extension"></a>
<a id="try-a-follow-up-rule"></a>
# Run your first Keir rule

Run a rule that requests a follow-up when a task is completed in a particular project. You’ll see when it requests a task and when it skips the request.

You need **Node.js 24 with npm** and a local copy of Keir Core. The example uses a rule that’s already compiled and simulates task reads and creation.

<a id="compile-and-run"></a>
## Run the example

From the repository root, package the Node runtime and install it in the example app:

```sh
mkdir -p .tools/packages
npm pack ./runtime-node --pack-destination .tools/packages --cache .tools/npm-cache --ignore-scripts
npm ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts
```

In PowerShell, use `New-Item -ItemType Directory -Force .tools/packages` for the first command and `npm.cmd` for the npm commands.

Run the example:

```sh
node examples/node-host/generic.mjs conformance/generic-hooks/effect4
```

The command prints a JSON report. Find these two cases by their `id`:

| Case | Input | Result |
| --- | --- | --- |
| `normal` | A completed task in the chosen project | `CREATED`. The simulated task service confirmed creation. |
| `other-project` | A completed task in a different project | `NO_OP`. The rule skipped the request. |

Both cases use the [same rule](../conformance/generic-hooks/effect4/source.yaml). The project on the completed task determines whether the rule calls the app’s create operation.

<a id="embed-the-same-flow"></a>
[See how Keir works](guided-tour.md) · [Compile and run the rule in both runtimes](../examples/README.md) · [Embed Keir in an application](embedding.md)
