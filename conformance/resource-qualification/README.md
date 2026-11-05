<a id="effect4-resource-qualification"></a>
# Resource qualification for single-write execution

This engineering reference describes the test workload and measured resource behavior. For runtime requirements, see the [Keir specification](../../docs/specs/runtime.md#limits).

From the repository root with Java 21, Node 24, the Maven wrapper, and the locked Node dependencies:

```sh
./mvnw -B -ntp package -DskipTests
npm ci --prefix runtime-node
node conformance/resource-qualification/qualify.mjs > build/resource-qualification.json
```

Use `mvnw.cmd` on Windows. Create the ignored `build/` directory before redirecting output. Use an isolated checkout and existing caches or set a process-local Maven cache. The harness uses local fixtures and test-owned processes only. Neither default suite runs it.

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

The compiler API produces the artifacts used by both runtimes. The CLI independently checks the admitted source byte boundary and its fixture bytes. Runtime preparation exercises intentionally modified checked expressions at 65 nodes, depth 17 and 513 total nodes. Invocation covers the 256/257-byte title boundary, a 32-leaf concatenation with 8,192 intermediate string bytes, and a Boolean comparison with an oversized intermediate. The shared-model case holds four adapter responses pending before releasing them. Deadline cases release late success only after checking the settled UNKNOWN or earlier CONFIRMED receipt.

The qualification prints raw JSON. Wall times include host and engine work. Process CPU includes engine and background threads. Standalone parse timings use the same parsers as the public bind and prepare operations, whose timings also include parsing. Node reports endpoint samples of phase heap and RSS plus process peak RSS. Java reports the sum of phase heap memory-pool peaks, which may occur at different times. Both report post-GC heap before and after the workload and with and after releasing the prepared handles. Forced collection happens outside timed samples. Small phases may be shorter than CPU accounting resolution. Compiler CLI startup and exit are reported separately from in-process compilation.

The source and model byte cases use whitespace padding to isolate their raw input bounds. The exact 1 MiB artifact is canonical JSON with an extra member. It reaches structural rejection, and the 1 MiB + 1 input fails the byte gate. The largest semantically admitted artifact in this run is the aggregate AST case. A future full-size admission claim needs a valid near-ceiling fixture or a derived tighter reachable bound. JVM heap telemetry leaves whole-process RSS open. A platform process sampler is the next step if total resident memory is part of a release claim.

The child watchdog covers test-owned direct children. The compiler CLI creates no descendants. The probe checks timeout, cancellation, capture across both streams, overflow during exit, and process close before temporary cleanup. The adapter deadline measures settlement with a cooperative test host. Hard preemption of synchronous CEL remains unqualified.

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

The fixed allowance covers eight step headers at up to 512 bytes each, up to nine handler wrappers at 256 bytes each, and up to 2,048 bytes for the fixed model reference, compiler pins, return wrapper and surrounding syntax. This gives `19 × (21,848 + 1,208 + 128 + 2,048) + 16,384 = 495,792` bytes. The serial run checks the computed bound against a compiler-produced fixture accepted by both runtimes. The fixture need not approach the bound. The 1 MiB byte cap remains the profile-wide ceiling, with semantic admission at that size still open for other catalogs.

The runtime boundary cases already live in `BoundEffectRuntimeTest.fourCallsAndEightStepsPassButTheirNextValuesFailStatically` and the fixed-ceiling test in `runtime-node/test/bound-effect.test.js`. The serial run adds the current single-write compiler checks.
