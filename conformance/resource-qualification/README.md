<a id="effect4-resource-qualification"></a>
# Resource qualification for public Keir single-write execution

This engineering reference describes the test workload and measured resource behavior. For runtime requirements, see the [Keir specification](../../docs/specs/runtime.md#limits).

The commands below follow the Core 0.3.0 candidate and its model/artifact generation 3 fixtures. A focused check compiled the normal, title and Inventory examples and matched their complete outcomes in Java and Node. The recorded resource measurements below remain historical Keir 2 evidence; this alignment does not rerun or qualify the full resource workload on the new generation.

Run from the root of an isolated checkout with Java 21, Node 24, the Maven wrapper and seeded task-local caches.

```sh
mkdir -p build
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -o -B -ntp -Dmaven.repo.local=.tools/maven/repository -DskipTests package
npm ci --offline --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
node conformance/resource-qualification/qualify.mjs > build/resource-qualification.json
```

On Windows, create the ignored `build/` directory with `New-Item -ItemType Directory -Force build`. Set `$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven'` for the current shell and use `mvnw.cmd` with the same flags. The harness uses local fixtures and test-owned processes and runs separately from the default suites.

## Workload limits

| Surface | Limit |
| --- | --- |
| Source and model documents | 65,536 bytes and 65,537 bytes |
| Artifact document | 1,048,576 bytes and 1,048,577 bytes |
| CEL expression | 512 source bytes, 64 AST nodes, 16 levels, 256 UTF-8 bytes per admitted string |
| Whole program | 8 steps, 4 calls and 512 aggregate AST nodes |
| Typed input record | 8,192 canonical bytes and 8,193 bytes with every field within the scalar ceiling |
| Warm loop | 30 measured invocations per case after 5 warmups |
| Shared model | 4 live prepared handles, 4 overlapping invocations |
| Pending adapter | 500 ms deadline and a late success released after observing settlement |
| Child supervision | 1 direct child at a time, 45 s per real child, 1 MiB combined capture |
| Containment probes | 1 direct child at a time, 1 s timeout, 16 KiB combined capture |

The public compiler API produces the artifacts used by both runtimes. The two-file CLI independently checks the admitted source byte boundary. The harness verifies the current [model](../keir-3/dcm.json) and adjacent fixture pins and compares the exact compiler output. Runtime preparation exercises intentionally modified checked expressions at 65 nodes, depth 17 and 513 total nodes. Invocation covers the 256/257-byte title boundary, a 32-leaf concatenation with 8,192 intermediate string bytes, and a Boolean comparison with an oversized intermediate. The shared-model case holds four adapter responses pending before releasing them. Deadline cases release late success only after checking the settled UNKNOWN or earlier CONFIRMED receipt.

The qualification prints raw JSON. Wall times include host and engine work. Process CPU includes engine and background threads. Standalone parse timings use the same parsers as the public bind and prepare operations, whose timings also include parsing. Node reports endpoint samples of phase heap and RSS plus process peak RSS. Java reports the sum of phase heap memory-pool peaks, which may occur at different times. Both report post-GC heap before and after the workload and with and after releasing the prepared handles. Forced collection happens outside timed samples. Small phases may be shorter than CPU accounting resolution. Compiler CLI startup and exit are reported separately from in-process compilation.

The source and model byte cases use whitespace padding to isolate their raw input bounds. The exact 1 MiB artifact is canonical JSON with an extra member. It reaches structural rejection, and the 1 MiB + 1 input fails the byte gate. The largest semantically admitted artifact in this run is the aggregate AST case. A future full-size admission claim needs a valid near-ceiling fixture or a derived tighter reachable bound. JVM heap telemetry leaves whole-process RSS open. A platform process sampler is the next step if total resident memory is part of a release claim.

The child watchdog covers test-owned direct children. The compiler CLI creates no descendants. The probe checks timeout, cancellation, capture across both streams, overflow during exit, and process close before temporary cleanup. In serial mode, cleanup waits for any started sampler as well as the workload JVM. The adapter deadline measures settlement with a cooperative test host. Hard preemption of synchronous CEL remains unqualified.

## Serial embedded workload on Windows

```powershell
node conformance/resource-qualification/qualify.mjs --soak > build/resource-embedded.json
```

The same preparation commands apply. Use the existing Windows PowerShell and run in an isolated checkout. The serial run skips the four-way overlap probe. It holds one prepared program per measured case and awaits each invocation before starting the next. Compiler and preparation work occur outside the invocation samples. Cases cover a large 512-node program, a 32-leaf concatenation that stops at the title boundary, and a Boolean intermediate with one confirmed write. The large program short-circuits at runtime. It measures admission and serial execution of this fixture rather than evaluation of all 512 nodes.

| Serial limit | Value |
| --- | --- |
| Active invocations per host | 1 |
| Soak per case per runtime | 100 warmups, then 5 blocks of 200 |
| Timed invocations per runtime | 3,000 across the three soak cases |
| Engine heap setting | Java `-Xmx256m`, Node `--max-old-space-size=256` |
| Observed process-memory budget | 512 MiB per measured process |
| Workload and sampler | At most one workload child and one Windows sampler, 45 s watchdog and 1 MiB capture each |
| JVM observation | `Get-Process` working set and OS peak working set, with 100 ms between refreshes |

The report includes block CPU and elapsed times, invocation min/median/p95/max, and retained heap after the warmup and each block. Retained heap uses forced collection between measurements. The sampler records the highest OS working-set peak observed before exit for the compiler API process and Java runtime. The short standalone CLI is measured for wall time and byte parity. A peak occurring after the final refresh can escape observation. Sample offsets use the sampler's own clock and describe gaps between its observations. The OS peak counter includes earlier peaks as of each refresh. Node reports its process peak RSS separately. The 512 MiB budget is an assertion on the recorded observations. Neither it nor the heap settings constrain the full address space or prove a production memory limit.

### Artifact bound for the measured catalogs

For the exact base and title DCMs, a single-write artifact is bounded by **495,792 bytes**. The report lists the DCM digests. These models have one output field, a read with one agent argument and two possible handlers, and a write with two agent arguments and three possible handlers. At most four calls, one write and eight steps allow up to 19 binding slots. At the maximum those slots comprise four LETs, nine read bindings across three reads, five write bindings and one return binding. Each handler contributes one slot for the single output field.

| Component | Conservative allowance |
| --- | ---: |
| Checked-expression base64 per slot, from the 16,384-byte checked limit | 21,848 B |
| Variables per expression, at most nine ASCII name/type pairs of up to 64 bytes each, including JSON framing | 1,208 B |
| Expression member key, result type and remaining expression JSON syntax | 128 B |
| Lowered binding and field name per slot, including a possible 256-byte literal escaped at up to six bytes per byte | 2,048 B |
| All fixed envelope and plan framing | 16,384 B |

The fixed allowance covers eight step headers at up to 512 bytes each, up to nine handler wrappers at 256 bytes each, and up to 2,048 bytes for the fixed model reference, compiler pins, return wrapper and surrounding syntax. This gives `19 × (21,848 + 1,208 + 128 + 2,048) + 16,384 = 495,792` bytes. The serial run checks the computed bound against a compiler-produced fixture accepted by both runtimes. The fixture need not approach the bound. The public artifact byte ceiling is 1 MiB. Semantic admission at that size remains open for other catalogs.

The runtime boundary cases already live in `BoundEffectRuntimeTest.fourCallsAndEightStepsPassButTheirNextValuesFailStatically` and the fixed-ceiling test in `runtime-node/test/bound-effect.test.js`. The serial run adds the current single-write compiler checks.

<a id="fully-evaluated-keir-2-membership-workload"></a>
## Fully evaluated membership workload

After the same offline package preparation, run the opt-in dense phase on Windows.

```powershell
node conformance/resource-qualification/qualify.mjs --dense > build/resource-dense.json
```

This phase generates one test source against the unchanged, pinned Inventory DCM. Eight BIND steps each evaluate a balanced OR of ten membership tests. The input is the shared maximum no-match Inventory input with sixteen 256-byte labels and a 256-byte needle. Every operand is false, so every OR branch and membership test is reached. The compiler probe verifies the exact select and call shapes, 59 nodes per BIND, 473 nodes including the return, depth 7 and 404 expression-source bytes. The pinned engines scan these lists linearly. The resulting 80 scans imply 1,280 element comparisons per invocation. These counts follow from the checked AST, the no-match input and the pinned list implementations. No instrumented operator counter is installed.

The public runtime APIs prepare the same compiler-produced artifact bytes once in each worker. Each worker registers the complete host catalog with callbacks that fail on any dispatch, then checks every invocation against an independently constructed full outcome and 19-event trace. The expected result is `{eligible:false}`, with no effects and zero adapter calls.

| Dense workload guard | Value |
| --- | --- |
| Serial invocations per runtime | 100 warmups, then 5 blocks of 1,000 |
| Engine heap | 256 MiB |
| Observed process-memory budget | 512 MiB |
| Direct child watchdog and capture | 45 seconds and 1 MiB combined output |
| Total execution budget | 135 seconds including probe compilation |
| Concurrency | One workload process, plus one Windows JVM sampler |
| Retention screen | Final three post-GC samples span at most the larger of 8 MiB and 10% of warmed heap |

Probe compilation and artifact production share the first 45-second phase. Each runtime has at most 45 seconds, further restricted by the remaining total budget. The existing supervisor closes each direct child before proceeding. The JVM sampler stops its worker on an observed process-memory breach. Node checks peak RSS after each block. A failed guard ends qualification. The report and generated evidence remain under ignored `build/`; the report names its evidence directory. Per-block progress files preserve the last fully checked counts if a watchdog terminates a worker. Invocations within an unfinished block remain uncounted.

### Recorded bounded sample

This historical Keir 2 sample used Core source baseline `a79ceba`, Java 21.0.12 and Node 24.14.1 on Windows x64, with CEL-Java 0.14.0, CEL-ES 0.6.1, protobuf-Java 4.35.1 and protobuf-ES 2.6.2. Both runtimes completed all 5,100 invocations and matched the expected artifact and outcome digests. The complete phase finished in 14.064 seconds.

| Measurement | Java | Node |
| --- | ---: | ---: |
| Measured invocations | 5,000 | 5,000 |
| Invocation median | 1,002 microseconds | 543.5 microseconds |
| Invocation p95 | 1,564 microseconds | 1,251.2 microseconds |
| Invocation maximum | 3,852 microseconds | 4,405 microseconds |
| Observed process peak | 312,188,928 bytes | 94,392,320 bytes |
| Final three retained-heap sample span | 56,912 bytes | 3,968 bytes |

The DCM SHA-256 is `77c67ad6a82149d461181d5c4ea661763a84b7055c6589bcce8049710362d959`. The generated 32,675-byte artifact SHA-256 is `79f5f443b1ffead00e1a89b3ac250d561ca0994f467b6afad88a142329aac3fe`. The complete expected outcome SHA-256 is `ac36df3179b7953cd347710d5b13734104d7ff9322dfb9e54f5b302eafefa4dd`. The raw qualification report SHA-256 is `1fb7f80a8bd6a3c15cc82bd6ece5d323e1f3696d4f3263cb310dcf05ab74c425`.

Per-invocation wall samples include public invocation and outcome construction. Assertions, canonical outcome comparison and explicit collection occur outside those wall samples. Block CPU includes assertions and observation overhead before collection. Retained heap includes the probe's sample arrays. Java process peaks come from the existing 100 ms Windows sampler and may miss a later peak before exit; Node reports its process peak RSS. Allocation rate and compiler process RSS are unmeasured.

This is a bounded serial screening result. It establishes neither leak freedom nor a latency SLA. It does not cover concurrent invocations, every admitted catalog, maximum possible allocation intermediates or synchronous CPU preemption. The existing runtime deadline checks still surround synchronous CEL execution.

Focused failure checks changed the expected result in private test manifests. Both workers rejected the first warmup and reported zero successfully checked invocations. A Node manifest with a one-byte observed-memory ceiling stopped after one measured invocation and saved that count before reporting failure.
