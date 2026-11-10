package dev.keir.runtime;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static dev.keir.runtime.RuntimeData.*;

/** A bounded synthetic host workload; its measurements are not a service capacity promise. */
public final class ConcurrentProbe {
    private static final long ORIGIN = System.nanoTime();
    private static final AtomicInteger ids = new AtomicInteger(), dispatches = new AtomicInteger();
    private static final AtomicInteger warmed = new AtomicInteger(), checked = new AtomicInteger();
    private static final ConcurrentMap<Key, Pending> pending = new ConcurrentHashMap<>();
    private static final List<Map<String,Object>> checkpoints = new ArrayList<>();
    private static Path directory;
    private static KeirRuntime.Prepared prepared;
    private static Object input;
    private static byte[] expected;
    private static long invocationMs, memoryBudget;

    private static final class Key {
        final String actor, event;
        Key(int id) { actor = "actor" + id; event = "event" + id; }
        @Override public String toString() { throw new AssertionError("The native key must remain opaque"); }
    }
    private static final class Pending {
        final Key key;
        final AtomicInteger calls = new AtomicInteger();
        final CompletableFuture<Object> read = new CompletableFuture<>();
        Pending(Key key) { this.key = key; }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static long now() { return (System.nanoTime() - ORIGIN) / 1_000_000; }
    private static long cpuUs() { return ProcessHandle.current().info().totalCpuDuration().orElseThrow().toNanos() / 1000; }
    private static long retained() { System.gc(); return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static byte[] read(String name) throws Exception { return Files.readAllBytes(directory.resolve(name)); }

    private static CompletionStage<?> call(String capability, Map<String,Object> args,
            Map<String,Object> bindings, long remaining, KeirRuntime.BoundEffectContext context) {
        Pending state = pending.get(context.requestKey());
        check(state != null && context.requestKey() == state.key, "Original key identity changed");
        check(bindings.equals(Map.of("actorId", state.key.actor)), "Protected actor changed");
        check(context.event().equals(Map.of("id", state.key.event, "source", "task1")), "Event context changed");
        check(remaining > 0 && remaining <= invocationMs, "Invalid remaining budget");
        dispatches.incrementAndGet();
        int ordinal = state.calls.getAndIncrement();
        if (capability.equals("tasks.get")) {
            check(ordinal == 0 && args.equals(Map.of("taskId", "task1")), "Unexpected READ or arguments");
            return state.read;
        }
        check(capability.equals("tasks.create") && ordinal == 1
                && args.equals(Map.of("projectId", "project1", "title", "Follow up")), "Unexpected WRITE or arguments");
        return CompletableFuture.completedFuture(Map.of("kind", "SUCCESS", "value", Map.of("id", "task2"), "effect", "CONFIRMED"));
    }

    private static long[] block(int count, int concurrency, boolean warmup) throws Exception {
        long[] samples = new long[count];
        for (int offset = 0; offset < count; offset += concurrency) {
            List<Pending> batch = new ArrayList<>();
            List<CompletableFuture<?>> futures = new ArrayList<>();
            try {
                for (int index = 0; index < Math.min(concurrency, count - offset); index++) {
                    Key key = new Key(ids.incrementAndGet()); Pending state = new Pending(key);
                    pending.put(key, state); batch.add(state);
                    int slot = offset + index;
                    var invocation = new KeirRuntime.BoundEffectInvocation(Map.of("actorId", key.actor),
                            new KeirRuntime.Deadline(ConcurrentProbe::now, now() + invocationMs), key,
                            Map.of("id", key.event, "source", "task1"));
                    long start = System.nanoTime();
                    futures.add(KeirRuntime.invokeBoundEffectProgram(prepared, input, invocation).thenAccept(outcome -> {
                        samples[slot] = (System.nanoTime() - start) / 1000;
                        check(state.calls.get() == 2, "Unexpected call count");
                        check(Arrays.equals(expected, outcome.toJson()), "Unexpected complete outcome");
                        (warmup ? warmed : checked).incrementAndGet();
                    }).toCompletableFuture());
                }
                check(batch.stream().allMatch(state -> state.calls.get() == 1), "Batch did not overlap at READ");
                for (Pending state : batch) state.read.complete(Map.of("kind", "SUCCESS", "value",
                        Map.of("id", "task1", "projectId", "project1", "status", "COMPLETED")));
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
            } finally { for (Pending state : batch) pending.remove(state.key); }
        }
        return samples;
    }

    private static Map<String,Object> processObservation() throws Exception {
        Path status = Path.of("/proc/self/status");
        if (!Files.isRegularFile(status)) return Map.of("supported", false);
        var observation = new LinkedHashMap<String,Object>();
        observation.put("supported", true);
        observation.put("counter", "Linux /proc/self/status VmHWM and Threads");
        for (String line : Files.readAllLines(status)) {
            if (line.startsWith("VmHWM:")) observation.put("peakRssBytes", Long.parseLong(line.trim().split("\\s+")[1]) * 1024);
            if (line.startsWith("Threads:")) observation.put("osThreads", Long.parseLong(line.trim().split("\\s+")[1]));
        }
        check(observation.get("peakRssBytes") instanceof Long, "Missing process memory observation");
        check((Long)observation.get("peakRssBytes") <= memoryBudget, "Observed process memory exceeded workload budget");
        return observation;
    }

    private static Map<String,Object> blockedClocks(int count) throws Exception {
        Thread caller = Thread.currentThread();
        CountDownLatch entered = new CountDownLatch(count), release = new CountDownLatch(1);
        AtomicInteger virtual = new AtomicInteger(), platform = new AtomicInteger();
        List<CompletionStage<Object>> outcomes = new ArrayList<>();
        int before = ManagementFactory.getThreadMXBean().getThreadCount();
        var processBefore = processObservation();
        int whileBlocked;
        Map<String,Object> processBlocked;
        try {
            for (int index = 0; index < count; index++) {
                AtomicBoolean dispatched = new AtomicBoolean();
                var clock = new InvocationClock(() -> {
                    if (dispatched.get() && Thread.currentThread() != caller) {
                        if (Thread.currentThread().isVirtual()) virtual.incrementAndGet(); else platform.incrementAndGet();
                        entered.countDown();
                        try { release.await(); }
                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                    }
                    return 0;
                }, 25);
                outcomes.add(clock.waitFor(left -> { dispatched.set(true); return new CompletableFuture<>(); },
                        "/steps/0", "ADAPTER_EXCEPTION", null));
            }
            check(entered.await(5, TimeUnit.SECONDS), "Not all expiry checks could park");
            check(virtual.get() == count && platform.get() == 0, "Expiry checks used platform threads");
            whileBlocked = ManagementFactory.getThreadMXBean().getThreadCount();
            processBlocked = processObservation();
        } finally { release.countDown(); }
        for (var outcome : outcomes) {
            try { outcome.toCompletableFuture().get(5, TimeUnit.SECONDS); throw new AssertionError("Expected budget failure"); }
            catch (ExecutionException failure) {
                check(failure.getCause() instanceof RuntimeFault fault && fault.code.equals("BUDGET_EXCEEDED"), "Wrong expiry failure");
            }
        }
        return Map.of("checkedBudgetFailures", outcomes.size(), "virtualChecks", virtual.get(), "platformChecks", platform.get(),
                "platformJavaThreadsBefore", before, "platformJavaThreadsBlocked", whileBlocked,
                "processBefore", processBefore, "processBlocked", processBlocked);
    }

    private static void checkpoint() throws Exception {
        Files.write(directory.resolve("concurrent-java-progress.json"), canonical(Map.of("completedWarmups", warmed.get(),
                "completedInvocations", checked.get(), "completedBlocks", checkpoints.size(), "blocks", checkpoints)));
    }

    private static Map<String,Object> run(Map<String,Object> manifest) throws Exception {
        var limits = map(manifest.get("limits"), "");
        invocationMs = ((Number)limits.get("invocationMs")).longValue();
        memoryBudget = ((Number)limits.get("observedProcessBytes")).longValue();
        byte[] modelBytes = read((String)manifest.get("model")), artifact = read((String)manifest.get("artifact"));
        var model = map(parseJson(modelBytes, 65536), "");
        input = parseCanonical(read((String)manifest.get("input"))); expected = canonical(manifest.get("expected"));
        var adapters = new TreeMap<String,KeirRuntime.BoundEffectAdapter>();
        String keyType = (String)map(model.get("hostKey"), "").get("type");
        for (var capability : map(model.get("capabilities"), "").entrySet()) {
            String id = capability.getKey(), version = (String)map(capability.getValue(), "").get("version");
            adapters.put(id, new KeirRuntime.BoundEffectAdapter(version, "simulation", keyType,
                    (args, bindings, remaining, context) -> call(id, args, bindings, remaining, context)));
        }
        var binding = KeirRuntime.bindModel(modelBytes, adapters); check(binding.succeeded(), "Model binding failed");
        var preparation = KeirRuntime.prepareProgram(artifact, binding.boundModel());
        check(preparation.succeeded(), "Preparation failed"); prepared = preparation.prepared();
        int warmups = ((Number)limits.get("warmups")).intValue();
        processObservation();
        block(warmups, 32, true); long baseline = retained(); checkpoint(); processObservation();
        List<Map<String,Object>> distributions = new ArrayList<>();
        for (Object raw : list(limits.get("concurrency"), "")) {
            int concurrency = ((Number)raw).intValue(); List<Long> samples = new ArrayList<>();
            for (int block = 0; block < ((Number)limits.get("blocks")).intValue(); block++) {
                long start = System.nanoTime(), cpu = cpuUs();
                long[] part = block(((Number)limits.get("perBlock")).intValue(), concurrency, false);
                long elapsed = (System.nanoTime() - start) / 1000, usedCpu = cpuUs() - cpu;
                for (long sample : part) samples.add(sample);
                checkpoints.add(Map.of("concurrency", concurrency, "block", block, "checked", part.length,
                        "elapsedUs", elapsed, "cpuUs", usedCpu, "retainedHeapBytes", retained(), "process", processObservation()));
                checkpoint();
            }
            Collections.sort(samples);
            distributions.add(Map.of("concurrency", concurrency, "checked", samples.size(), "medianUs", samples.get(samples.size() / 2),
                    "p95Us", samples.get((int)Math.ceil(samples.size() * .95) - 1), "maxUs", samples.getLast()));
        }
        var expiry = blockedClocks(((Number)limits.get("blockedClocks")).intValue());
        check(pending.isEmpty(), "Invocation fixture state was retained");
        var report = new LinkedHashMap<String,Object>();
        report.put("status", "passed"); report.put("java", System.getProperty("java.version"));
        report.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        report.put("completedWarmups", warmed.get()); report.put("completedInvocations", checked.get());
        report.put("dispatchCount", dispatches.get()); report.put("artifactDigest", digest(artifact));
        report.put("outcomeDigest", digest(expected)); report.put("baselineHeapBytes", baseline);
        report.put("concurrency", distributions); report.put("blocks", checkpoints); report.put("blockedClocks", expiry);
        report.put("process", processObservation()); return report;
    }

    public static void main(String[] args) throws Exception {
        directory = Path.of(args[0]);
        try {
            check(Runtime.version().feature() == 21, "Use Java21");
            System.out.writeBytes(canonical(run(map(parseJson(read(args[1]), 65536), ""))));
        } catch (Throwable failure) {
            String message = String.valueOf(failure);
            checkpoint();
            System.out.writeBytes(canonical(Map.of("status", "failed", "completedWarmups", warmed.get(),
                    "completedInvocations", checked.get(), "dispatchCount", dispatches.get(),
                    "error", message.substring(0, Math.min(1000, message.length())))));
            System.exit(1);
        }
    }
}
