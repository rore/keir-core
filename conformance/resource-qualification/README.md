# Effect/4 resource qualification

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
