# Exact numeric source examples

These complete source examples run through the normal compiler and Java/Node teaching hosts. They are not a public release or an installed-host upgrade.

| Example | Business rule | Normal scenario |
| --- | --- | --- |
| [Stock](stock/source.json) | Read available stock. Below 10, request exactly the shortage through one host-owned WRITE. | Available `"7"` requests `"3"`; the validated report is `REQUESTED` with a CONFIRMED receipt. |
| [Quote](quote/source.json) | Apply an optional discount, round explicitly with HALF_EVEN, and return a decision and amount. | Subtotal `"100"` and rate `"0.075"` produce ALLOW and amount `"92.5"`. |

Each directory contains its DCM, source, actual compiler artifact, raw-byte pins, an independent normal scenario and boundary cases. Java and Node execute the same artifact. Business numbers cross host boundaries as canonical strings, not JSON numbers. Integer is signed int64; Decimal has at most 38 normalized coefficient digits and scale 0–18.

## Run from source

Use the [source build prerequisites](../../CONTRIBUTING.md#build-and-test), including the pinned Python schema dependency. Build the Java modules before running these commands from the repository root. Keep dependency caches in the isolated locations described there.

```sh
node scripts/setup-node-example.mjs
java -Xmx256m -jar examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar stock python
node examples/node-host/verify-scenarios.mjs stock python
```

Replace `stock` with `quote` for the quote example. `python` selects the interpreter containing the schema dependency. The snapshot JAR name is a development coordinate, not a release version. Setup installs a locally packed Node runtime; it does not replace an application's runtime.

The commands verify complete calls, arguments, outcomes, traces, receipts and consumer reports against the independent scenarios. Their decoded Java/Node reports match in [hosted source verification](https://github.com/rore/keir-core/actions/runs/43422161327).

## Host authority stays native

Stock uses teaching stubs, not an inventory service. A real host checks current permission, original request identity, protected actor and event scope before dispatch. It preserves the receipt even if the returned business value is invalid.

The quote consumer validates decision, message and canonical amount before using the decision. ALLOW does not set a native price, authorize payment or commit an operation. The host still owns those actions. New artifacts require separate host selection and approval; old approvals are not transferred.
