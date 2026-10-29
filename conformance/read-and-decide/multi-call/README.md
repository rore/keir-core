# Two-read fresh-grant fixture

This supplemental fixture closes the one-read sample's multi-call grant test gap. The trusted model permits two `tasks.get` calls. Source has exactly two READ steps; its DISPLAY classification depends on the second response. It adds no scenario-format controls and changes none of the primary golden files or 61-case corpus.

The actual Java compiler emits the committed 2,966-byte `artifact.json`. Host/test selection holds its digest in `artifact.sha256` and the canonical trusted-model digest in `dcm.sha256`, outside the artifact. Tests consume these bytes without a compiler dependency.

From the repository root, with the existing compiler CLI built, reproduce in an ignored output directory:

```sh
mkdir -p .tools/multi-call
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar conformance/read-and-decide/multi-call/source.yaml conformance/read-and-decide/multi-call/dcm.json > .tools/multi-call/one.json
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar conformance/read-and-decide/multi-call/source.yaml conformance/read-and-decide/multi-call/dcm.json > .tools/multi-call/two.json
cmp .tools/multi-call/one.json .tools/multi-call/two.json
cmp .tools/multi-call/one.json conformance/read-and-decide/multi-call/artifact.json
sha256sum conformance/read-and-decide/multi-call/artifact.json
node --test runtime-node/test/multi-call.test.js
```

Check both compiler exits before comparing output. Preserve stdout bytes; on Windows use a byte-preserving process API or PowerShell 7.4+ native redirection. The canonical model digest sorts keys and uses the profile JSON encoding, rather than hashing this formatted `dcm.json` text.

Generated twice with compiler `keir-compiler-java/0.1.0-SNAPSHOT`, CEL Java `0.14.0`, protobuf Java `4.35.1`, CEL Node `0.6.1` and protobuf Node `2.6.2`. Both fresh Java processes emitted identical bytes. Independently hashed pins are:

- Artifact: `sha256:aefc5bee01b57762e195d3d24c25be5b418f6d6adcb2796a39dfba5bd68a14ac`.
- Canonical trusted model: `sha256:02f1586fb84d4cd168948d63f2989224bfb9b0298b700ae72f92a790d146947d`.

Node `multi-call.test.js` and Java `MultiCallConformanceTest` hand-author exact outcomes, traces, calls and authorization requests against these same bytes and separately held pins. Admission two/current one permits the first read, then denies at `/steps/1` before second-call authorization or dispatch. Reusing that prepared handle with current two and a different task/actor permits both reads and returns PENDING for the second OPEN response. A first-read TASK_NOT_FOUND stops before the second step. Both counterparts passed; Java's earlier in-memory regression remains additional coverage.
