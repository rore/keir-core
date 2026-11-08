package dev.keir.runtime;

import dev.keir.compiler.KeirCompiler;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.expr.Reference;
import com.google.protobuf.CodedOutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static dev.keir.runtime.RuntimeData.*;

public final class ResourceProbe {
    private static final Map<String, Object> timings = new LinkedHashMap<>();
    private static final Map<String, Object> facts = new TreeMap<>();
    private static final Map<String, Object> retainedByBlock = new TreeMap<>();
    private static final Map<String, Object> confirmed = Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", Map.of("id", "task2"));
    private static Path directory;
    private static int denseWarmups, denseInvocations;

    public static void main(String[] args) throws Exception {
        check(Runtime.version().feature() == 21, "Use Java 21");
        directory = Path.of(args[1]);
        var manifest = parseJson(read(args[2]), 65_536);
        if (args[0].startsWith("dense-")) {
            try {
                if (args[0].equals("dense-compiler")) denseCompiler(map(manifest, ""));
                else denseRuntime(map(manifest, ""));
            } catch (Throwable failure) {
                System.out.writeBytes(canonical(Map.of("status", "failed", "completedWarmups", denseWarmups,
                        "completedInvocations", denseInvocations, "error", String.valueOf(failure).substring(0, Math.min(1000, String.valueOf(failure).length())))));
                System.exit(1);
            }
        } else if (args[0].equals("compiler")) {
            compile(list(manifest, ""));
        } else {
            runtime(map(manifest, ""));
        }
    }

    private static byte[] read(String name) throws Exception { return Files.readAllBytes(directory.resolve(name)); }

    private static void denseCompiler(Map<String,Object> manifest) throws Exception {
        byte[] source = read((String)manifest.get("source")), model = read((String)manifest.get("model"));
        var compiled = new KeirCompiler().compile(source, model, KeirCompiler.Environment.supported());
        check(compiled.succeeded(), compiled.diagnostics());
        byte[] artifact = compiled.artifact();
        var plan = map(parseCanonical(artifact), "");
        var steps = list(plan.get("steps"), "");
        check(steps.size() == 8, "Expected eight reached BINDs");
        var expressions = map(plan.get("expressions"), "");
        check(expressions.size() == 9, "Expected eight BIND expressions and one return expression");
        var counts = new ArrayList<Integer>();
        int total = 0, scans = 0, depth = 0;
        for (int index = 0; index < 8; index++) {
            var step = map(steps.get(index), "");
            check("BIND".equals(step.get("op")) && ("scan" + index).equals(step.get("id")), "Unexpected dense step");
            String id = (String)map(step.get("value"), "").get("expr");
            var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expressions.get(id), "").get("checked")));
            int[] shape = denseShape(checked.getExpr());
            check(shape[0] == checked.getTypeMapCount() && shape[2] == 10, "Dense AST count mismatch");
            counts.add(shape[0]); total += shape[0]; scans += shape[2]; depth = Math.max(depth, shape[1]);
        }
        String returned = (String)map(map(map(plan.get("return"), "").get("fields"), "").get("eligible"), "").get("expr");
        var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expressions.get(returned), "").get("checked")));
        check(checked.getExpr().hasIdentExpr() && checked.getExpr().getIdentExpr().getName().equals("scan7")
                && checked.getTypeMapCount() == 1, "Unexpected dense return");
        total++;
        Files.write(directory.resolve((String)manifest.get("artifact")), artifact);
        System.out.writeBytes(canonical(Map.of("artifactDigest", digest(artifact), "artifactBytes", artifact.length,
                "nodesPerStep", counts, "totalNodes", total, "depth", depth,
                "membershipScans", scans, "elementVisits", scans * 16, "calls", 0)));
    }

    // Every OR operand is false on the no-match input, so every membership leaf is reached.
    private static int[] denseShape(Expr expression) {
        check(expression.hasCallExpr(), "Expected a dense call expression");
        var call = expression.getCallExpr();
        check(!call.hasTarget() && call.getArgsCount() == 2, "Unexpected dense call form");
        if (call.getFunction().equals("@in")) {
            for (int index = 0; index < 2; index++) {
                var select = call.getArgs(index).getSelectExpr();
                check(call.getArgs(index).hasSelectExpr() && !select.getTestOnly()
                        && select.getField().equals(index == 0 ? "targetLabel" : "labels")
                        && select.getOperand().hasIdentExpr() && select.getOperand().getIdentExpr().getName().equals("input"),
                        "Unexpected membership operand");
            }
            return new int[]{5, 3, 1};
        }
        check(call.getFunction().equals("_||_"), "Expected only OR and membership");
        int[] left = denseShape(call.getArgs(0)), right = denseShape(call.getArgs(1));
        return new int[]{left[0] + right[0] + 1, Math.max(left[1], right[1]) + 1, left[2] + right[2]};
    }

    private static void denseRuntime(Map<String,Object> manifest) throws Exception {
        var limits = map(manifest.get("limits"), "");
        int warmups = ((Number)limits.get("warmups")).intValue(), blocks = ((Number)limits.get("blocks")).intValue();
        int perBlock = ((Number)limits.get("perBlock")).intValue();
        long invocationMs = ((Number)limits.get("invocationMs")).longValue();
        byte[] modelBytes = read((String)manifest.get("model")), artifact = read((String)manifest.get("artifact"));
        Object input = parseCanonical(read((String)manifest.get("input")));
        byte[] expected = canonical(manifest.get("expected"));
        var model = map(parseJson(modelBytes, 65_536), "");
        var adapters = new TreeMap<String,KeirRuntime.BoundEffectAdapter>();
        var dispatches = new java.util.concurrent.atomic.AtomicInteger();
        String keyType = (String)map(model.get("hostKey"), "").get("type");
        for (var entry : map(model.get("capabilities"), "").entrySet()) {
            String version = (String)map(entry.getValue(), "").get("version");
            adapters.put(entry.getKey(), new KeirRuntime.BoundEffectAdapter(version, "simulation", keyType,
                    (args, bindings, remaining, context) -> { dispatches.incrementAndGet(); throw new AssertionError("Unexpected dispatch"); }));
        }
        var binding = KeirRuntime.bindModel(modelBytes, adapters);
        check(binding.succeeded(), binding.diagnostic());
        long prepareStart = System.nanoTime();
        var preparation = KeirRuntime.prepareProgram(artifact, binding.boundModel());
        long prepareUs = (System.nanoTime() - prepareStart) / 1000;
        check(preparation.succeeded(), preparation.diagnostic());
        Supplier<KeirRuntime.Outcome> invoke = () -> {
            var deadline = new KeirRuntime.Deadline(() -> System.nanoTime() / 1_000_000,
                    System.nanoTime() / 1_000_000 + invocationMs);
            var invocation = new KeirRuntime.BoundEffectInvocation(Map.of(), deadline, new Object(),
                    Map.of("id", "event1", "source", "item1"));
            return KeirRuntime.invokeBoundEffectProgram(preparation.prepared(), input, invocation).toCompletableFuture().join();
        };
        for (int index = 0; index < warmups; index++) {
            check(Arrays.equals(expected, invoke.get().toJson()) && dispatches.get() == 0, "Unexpected warmup outcome");
            denseWarmups++;
        }
        long baseline = retained();
        var samples = new ArrayList<Long>();
        var blockResults = new ArrayList<Map<String,Object>>();
        denseProgress(baseline, blockResults);
        for (int block = 1; block <= blocks; block++) {
            long blockCpu = cpuUs();
            for (int index = 0; index < perBlock; index++) {
                long start = System.nanoTime();
                var outcome = invoke.get();
                samples.add((System.nanoTime() - start) / 1000);
                check(Arrays.equals(expected, outcome.toJson()) && dispatches.get() == 0, "Unexpected measured outcome");
                denseInvocations++;
            }
            long usedCpu = cpuUs() - blockCpu;
            blockResults.add(Map.of("block", block, "invocations", perBlock, "cpuUs", usedCpu, "postGcHeapBytes", retained()));
            denseProgress(baseline, blockResults);
        }
        var finalHeaps = blockResults.subList(blockResults.size() - 3, blockResults.size()).stream()
                .mapToLong(row -> ((Number)row.get("postGcHeapBytes")).longValue()).summaryStatistics();
        long span = finalHeaps.getMax() - finalHeaps.getMin(), threshold = Math.max(8 * 1_048_576L, baseline / 10);
        check(span <= threshold, "Final post-GC heap span exceeds screening threshold");
        samples.sort(null);
        System.out.writeBytes(canonical(Map.ofEntries(
                Map.entry("status", "passed"), Map.entry("completedWarmups", denseWarmups),
                Map.entry("completedInvocations", denseInvocations), Map.entry("dispatchCount", dispatches.get()),
                Map.entry("artifactDigest", digest(artifact)), Map.entry("outcomeDigest", digest(expected)),
                Map.entry("environment", Map.of("runtime", "java", "version", System.getProperty("java.version"),
                        "os", System.getProperty("os.name"), "arch", System.getProperty("os.arch"), "pins", KeirRuntime.ENVIRONMENT)),
                Map.entry("prepare", Map.of("wallUs", prepareUs)),
                Map.entry("wall", Map.of("minUs", samples.getFirst(), "medianUs", samples.get(samples.size() / 2),
                        "p95Us", samples.get((samples.size() * 95 + 99) / 100 - 1), "maxUs", samples.getLast())),
                Map.entry("baselineHeapBytes", baseline), Map.entry("finalHeapSpanBytes", span),
                Map.entry("heapSpanLimitBytes", threshold), Map.entry("blocks", blockResults))));
    }

    private static void denseProgress(long baseline, List<Map<String,Object>> blocks) throws Exception {
        Files.write(directory.resolve("dense-java-progress.json"), canonical(Map.of("completedWarmups", denseWarmups,
                "completedInvocations", denseInvocations, "completedBlocks", blocks.size(),
                "baselineHeapBytes", baseline, "blocks", blocks)));
    }

    private static void check(boolean ok, Object message) {
        if (!ok) throw new AssertionError(message);
    }

    private static long heap() { return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static long retained() { System.gc(); return heap(); }
    private static long cpuUs() {
        return ProcessHandle.current().info().totalCpuDuration().orElseThrow().toNanos() / 1000;
    }

    private static <T> T measure(String name, Supplier<T> action) {
        var pools = ManagementFactory.getMemoryPoolMXBeans().stream().filter(pool -> pool.getType() == MemoryType.HEAP).toList();
        pools.forEach(pool -> pool.resetPeakUsage());
        long cpu = cpuUs(), start = System.nanoTime();
        T result = action.get();
        long elapsedUs = (System.nanoTime() - start) / 1000, usedCpu = cpuUs() - cpu;
        long peak = pools.stream().mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
        timings.put(name, Map.of("elapsedUs", elapsedUs, "cpuUs", usedCpu, "sumHeapPoolPeaksBytes", peak));
        return result;
    }

    private static void report(long baseline, Map<String, Object> more) {
        var report = new LinkedHashMap<String, Object>(more);
        report.put("version", System.getProperty("java.version"));
        report.put("timings", timings);
        report.put("retainedHeapBytes", Map.of("baseline", baseline, "after", retained()));
        System.out.writeBytes(canonical(report));
    }

    private static void compile(List<Object> cases) throws Exception {
        var compiler = new KeirCompiler();
        long baseline = retained();
        for (var value : cases) {
            var row = map(value, "");
            String id = (String) row.get("id"), expected = (String) row.get("expected");
            byte[] source = read((String) row.get("source")), model = read((String) row.get("model"));
            var result = measure(id, () -> compiler.compile(source, model, KeirCompiler.Environment.supported()));
            if (expected.isEmpty()) {
                check(result.succeeded(), id + " " + result.diagnostics());
                Files.write(directory.resolve(id + ".json"), result.artifact());
                facts.put(id, Map.of("sourceBytes", source.length, "modelBytes", model.length, "artifactBytes", result.artifact().length));
            } else {
                check(!result.succeeded() && result.diagnostics().size() == 1, id + " " + result.diagnostics());
                var diagnostic = result.diagnostics().getFirst();
                check(diagnostic.code().equals("BUDGET_EXCEEDED") && diagnostic.path().equals(expected), id + " " + diagnostic);
                facts.put(id, Map.of("sourceBytes", source.length, "modelBytes", model.length, "code", diagnostic.code(), "path", diagnostic.path()));
            }
        }
        for (String id : List.of("nodes", "depth")) forgeUnaryRoot(id);
        report(baseline, Map.of("facts", facts));
    }

    // Add one typed unary node without changing the model, step, or expression result type.
    private static void forgeUnaryRoot(String id) throws Exception {
        var artifact = map(parseCanonical(read(id + "-max.json")), "");
        var context = map(map(artifact.get("expressions"), "").get("e0"), "");
        var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String) context.get("checked")));
        long next = Collections.max(checked.getTypeMapMap().keySet()) + 1;
        var original = checked.getExpr();
        var root = Expr.newBuilder().setId(next).setCallExpr(Expr.Call.newBuilder().setFunction("!_").addArgs(original));
        var forged = checked.toBuilder().setExpr(root).putTypeMap(next, checked.getTypeMapOrThrow(original.getId()))
                .putReferenceMap(next, Reference.newBuilder().addOverloadId("logical_not").build()).build();
        var bytes = new ByteArrayOutputStream();
        var wire = CodedOutputStream.newInstance(bytes);
        wire.useDeterministicSerialization();
        forged.writeTo(wire);
        wire.flush();
        context.put("checked", Base64.getEncoder().encodeToString(bytes.toByteArray()));
        Files.write(directory.resolve(id + "-forged.json"), canonical(artifact));
    }

    private static void runtime(Map<String, Object> manifest) throws Exception {
        var limits = map(manifest.get("limits"), "");
        int warmups = ((Number) limits.get("warmups")).intValue(), repeats = ((Number) limits.get("repeats")).intValue();
        int handles = ((Number) limits.get("handles")).intValue();
        var soak = limits.containsKey("soak") ? map(limits.get("soak"), "") : Map.<String, Object>of();
        long deadline = ((Number) limits.get("adapterMs")).longValue();
        byte[] dcm = read((String) manifest.get("dcmName")), maxDcm = read((String) manifest.get("dcmMax"));
        byte[] overDcm = read((String) manifest.get("dcmOver")), titleDcm = read((String) manifest.get("titleModel"));
        byte[] maxArtifact = read("artifact-max.json"), overArtifact = read("artifact-over.json");
        long baseline = retained();
        var base = new Host(dcm);
        measure("parse-dcm-max", () -> parseJson(maxDcm, 65_536));
        measure("parse-artifact-max", () -> parseCanonical(maxArtifact));
        var bound = measure("bind-dcm-max", () -> bind(maxDcm, base));
        var rejected = measure("bind-dcm-over", () -> KeirRuntime.bindModel(overDcm, base.adapters));
        diagnostic("dcm-over", rejected.diagnostic(), "BUDGET_EXCEEDED", "/binding/dcm");
        for (var value : list(manifest.get("admissible"), "")) {
            String id = (String) value;
            byte[] bytes = read(id + ".json");
            var prepared = measure("prepare-" + id, () -> prepare(bytes, bound));
            if (id.equals("aggregate-max")) warm(id, warmups, repeats, () -> base.invoke(prepared, 2000, false), "COMPLETED", List.of());
            else outcome(base.invoke(prepared, 2000, false).join(), "COMPLETED", List.of());
            if (id.equals("embedded-large")) soakCase(id, soak, () -> base.invoke(prepared, 2000, false), "COMPLETED", List.of());
            facts.put(id, "COMPLETED");
        }
        for (String id : List.of("artifact-max", "artifact-over", "aggregate-forged", "nodes-forged", "depth-forged")) {
            byte[] bytes = id.equals("artifact-max") ? maxArtifact : id.equals("artifact-over") ? overArtifact : read(id + ".json");
            var bad = measure("prepare-" + id, () -> KeirRuntime.prepareProgram(bytes, bound));
            diagnostic(id, bad.diagnostic(), id.equals("artifact-max") ? "ARTIFACT_INVALID" : "BUDGET_EXCEEDED",
                    id.endsWith("-forged") ? "/artifact/expressions" : "/artifact");
        }

        byte[] wideDcm = read((String) manifest.get("wideModel")), valueArtifact = read("value.json");
        var wide = measure("bind-wide", () -> bind(wideDcm, base));
        var wideProgram = measure("prepare-value", () -> prepare(valueArtifact, wide));
        for (String id : List.of("value-max", "value-over")) {
            byte[] bytes = read(id + ".json");
            var input = parseCanonical(bytes);
            var result = measure("invoke-" + id, () -> base.invoke(wideProgram, 2000, false, input).join());
            outcome(result, id.equals("value-max") ? "COMPLETED" : "LIMIT_EXCEEDED", List.of());
            facts.put(id, observed(result));
        }
        check(base.reads.get() == 0 && base.writes.get() == 0, "Unexpected dispatch during static or no-call probes");

        var title = new Host(titleDcm);
        var titleBound = measure("bind-title", () -> bind(titleDcm, title));
        for (String id : List.of("title-256", "title-257", "concat", "boolean-valid", "boolean-over")) {
            String program = id.startsWith("title") ? "title" : id.startsWith("boolean") ? "boolean" : id;
            byte[] bytes = read(program + ".json");
            var prepared = measure("prepare-" + id, () -> prepare(bytes, titleBound));
            title.title = "x".repeat(id.equals("title-256") ? 255 : 256);
            title.project = id.equals("boolean-over") ? "project2" : "project1";
            boolean success = id.equals("title-256") || id.equals("boolean-valid");
            String classification = success ? "COMPLETED" : "LIMIT_EXCEEDED";
            var states = success ? List.of("CONFIRMED") : List.<String>of();
            int writesBefore = title.writes.get();
            title.expectedTitle = id.equals("title-256") ? "x".repeat(255) + "!" : "Valid title";
            var result = warm(id, warmups, repeats, () -> title.invoke(prepared, 2000, true), classification, states);
            int soakInvocations = id.equals("concat") || id.equals("boolean-valid")
                    ? soakCase(id, soak, () -> title.invoke(prepared, 2000, true), classification, states) : 0;
            check(title.writes.get() - writesBefore == (success ? repeats + warmups + soakInvocations : 0), id + " unexpected dispatch count");
            check(result.trace().stream().anyMatch(event -> "EFFECT_INTENT".equals(event.get("event"))) == success, id + " unexpected effect intent");
            facts.put(id, observed(result));
        }

        byte[] main = read("main.json"), afterWrite = read("confirmed.json");
        var report = new LinkedHashMap<String, Object>();
        if (soak.isEmpty()) report.putAll(overlap(base, bound, main, handles));

        var normal = prepare(main, bound);
        var confirmedFirst = prepare(afterWrite, bound);
        for (boolean prior : List.of(false, true)) {
            String id = prior ? "prior-confirmed" : "settled-unknown";
            var pending = new CompletableFuture<Object>();
            if (prior) base.pendingRead = pending;
            else base.pendingWrite = pending;
            int priorWrites = base.writes.get(), priorReads = base.reads.get();
            if (prior) base.delayAtRead = priorReads + 2;
            var settled = measure(id, () -> base.invoke(prior ? confirmedFirst : normal, deadline, true).join());
            outcome(settled, prior ? "LIMIT_EXCEEDED" : "UNCERTAIN", List.of(prior ? "CONFIRMED" : "UNKNOWN"));
            check("BUDGET_EXCEEDED".equals(settled.error().get("code")), id + " must exceed the deadline");
            check(base.writes.get() == priorWrites + 1 && base.reads.get() == priorReads + (prior ? 2 : 1)
                    && !pending.isDone(), id + " must time out after dispatch");
            byte[] snapshot = settled.toJson();
            check(pending.complete(prior ? base.read(Map.of("taskId", "task1")) : confirmed), "Late completion already settled");
            check(Arrays.equals(snapshot, settled.toJson()), "Late adapter response changed outcome");
            facts.put(id, observed(settled));
            base.pendingRead = null;
            base.pendingWrite = null;
        }
        report.put("facts", facts);
        report.put("retainedByBlock", retainedByBlock);
        report.put("environment", KeirRuntime.ENVIRONMENT);
        report(baseline, report);
    }

    private static Map<String, Object> overlap(Host base, KeirRuntime.BoundModel bound, byte[] main, int handles) {
        var prepared = measure("prepare-handles", () -> {
            var all = new ArrayList<KeirRuntime.Prepared>();
            for (int i = 0; i < handles; i++) all.add(prepare(main, bound));
            return all;
        });
        long live = retained();
        base.pendingRead = new CompletableFuture<>();
        int before = base.reads.get();
        var active = measure("invoke-overlap", () -> {
            var all = prepared.stream().map(handle -> base.invoke(handle, 2000, true)).toList();
            check(base.reads.get() - before == handles && all.stream().noneMatch(CompletableFuture::isDone), "Expected overlapping calls");
            base.pendingRead.complete(base.read(Map.of("taskId", "task1")));
            for (var invocation : all) outcome(invocation.join(), "COMPLETED", List.of("CONFIRMED"));
            return all.size();
        });
        base.pendingRead = null;
        facts.put("overlap", active);
        java.lang.ref.Reference.reachabilityFence(prepared);
        prepared.clear();
        return Map.of("heapWithLiveHandlesBytes", live, "heapAfterReleasingHandlesBytes", retained());
    }

    private static void diagnostic(String id, KeirRuntime.Diagnostic actual, String code, String path) {
        check(actual != null && actual.code().equals(code) && actual.path().equals(path), id + " " + actual);
        facts.put(id, Map.of("code", actual.code(), "path", actual.path()));
    }
    private static KeirRuntime.BoundModel bind(byte[] bytes, Host host) {
        var result = KeirRuntime.bindModel(bytes, host.adapters);
        check(result.succeeded(), result.diagnostic());
        return result.boundModel();
    }
    private static KeirRuntime.Prepared prepare(byte[] bytes, KeirRuntime.BoundModel model) {
        var result = KeirRuntime.prepareProgram(bytes, model);
        check(result.succeeded(), result.diagnostic());
        return result.prepared();
    }
    private static Map<String, Object> observed(KeirRuntime.Outcome result) {
        var summary = new LinkedHashMap<String, Object>();
        summary.put("classification", result.classification());
        summary.put("effects", result.effects().stream().map(receipt -> receipt.get("state")).toList());
        if (result.error() != null) summary.put("error", result.error());
        return summary;
    }
    private static KeirRuntime.Outcome outcome(KeirRuntime.Outcome result, String classification, List<String> states) {
        check(result.classification().equals(classification) && observed(result).get("effects").equals(states), observed(result));
        if (classification.equals("COMPLETED")) check(Map.of("status", states.isEmpty() ? "NO_OP" : "CREATED").equals(result.result()), result.result());
        return result;
    }
    private static KeirRuntime.Outcome warm(String id, int warmups, int repeats,
            Supplier<CompletableFuture<KeirRuntime.Outcome>> invoke, String classification, List<String> states) {
        for (int i = 0; i < warmups; i++) outcome(invoke.get().join(), classification, states);
        return measure("invoke-" + id, () -> {
            var samples = new ArrayList<Long>();
            KeirRuntime.Outcome last = null;
            for (int i = 0; i < repeats; i++) {
                long start = System.nanoTime();
                last = outcome(invoke.get().join(), classification, states);
                samples.add((System.nanoTime() - start) / 1000);
            }
            samples.sort(null);
            timings.put("distribution-" + id, Map.of("minUs", samples.getFirst(), "medianUs", samples.get(repeats / 2),
                    "p95Us", samples.get((repeats * 95 + 99) / 100 - 1), "maxUs", samples.getLast()));
            return last;
        });
    }

    private static int soakCase(String id, Map<String, Object> soak,
            Supplier<CompletableFuture<KeirRuntime.Outcome>> invoke, String classification, List<String> states) {
        if (soak.isEmpty()) return 0;
        int warmups = ((Number) soak.get("warmups")).intValue(), blocks = ((Number) soak.get("blocks")).intValue();
        int perBlock = ((Number) soak.get("perBlock")).intValue();
        warm(id + "-soak-warmup", 0, warmups, invoke, classification, states);
        var heap = new ArrayList<Long>();
        heap.add(retained());
        for (int block = 1; block <= blocks; block++) {
            warm(id + "-soak-" + block, 0, perBlock, invoke, classification, states);
            heap.add(retained());
        }
        retainedByBlock.put(id, heap);
        facts.put("soak-" + id, Map.of("invocations", blocks * perBlock, "classification", classification, "effects", states));
        return warmups + blocks * perBlock;
    }

    private static final class Host {
        private static final Map<String, Object> EVENT = Map.of("id", "event1", "source", "task1");
        private record RequestKey(Host owner, KeirRuntime.Deadline deadline) {}

        final Map<String, KeirRuntime.BoundEffectAdapter> adapters = new TreeMap<>();
        final java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger();
        String title = "", project = "project1", expectedTitle;
        final boolean hasTitle;
        CompletableFuture<Object> pendingRead, pendingWrite;
        int delayAtRead;

        Host(byte[] bytes) {
            var model = map(parseJson(bytes, 65_536), "");
            hasTitle = map(map(map(model.get("types"), "").get("Task"), "").get("fields"), "").containsKey("title");
            String keyType = (String) map(model.get("hostKey"), "").get("type");
            for (var entry : map(model.get("capabilities"), "").entrySet()) {
                String id = entry.getKey(), version = (String) map(entry.getValue(), "").get("version");
                adapters.put(id, new KeirRuntime.BoundEffectAdapter(version, "real", keyType, (args, bindings, remaining, context) -> {
                    check(context.requestKey() instanceof RequestKey key && key.owner() == this
                            && "user1".equals(bindings.get("actorId")) && EVENT.equals(context.event())
                            && context.deadline().clock() == key.deadline().clock()
                            && context.deadline().expiresAtMs() == key.deadline().expiresAtMs()
                            && remaining > 0 && remaining <= key.deadline().expiresAtMs(), "Unexpected host context");
                    if (id.equals("tasks.create")) {
                        writes.incrementAndGet();
                        if (expectedTitle != null) check(expectedTitle.equals(args.get("title")), "Unexpected composed title");
                        return pendingWrite == null ? CompletableFuture.completedFuture(confirmed) : pendingWrite;
                    }
                    int read = reads.incrementAndGet();
                    if (pendingRead != null && (delayAtRead == 0 || read == delayAtRead)) return pendingRead;
                    return CompletableFuture.completedFuture(read(args));
                }));
            }
        }
        Map<String, Object> read(Map<String, Object> args) {
            var value = new TreeMap<>(Map.of("id", args.get("taskId"), "projectId", project, "status", "COMPLETED"));
            if (hasTitle) value.put("title", title);
            return Map.of("kind", "SUCCESS", "value", value);
        }
        CompletableFuture<KeirRuntime.Outcome> invoke(KeirRuntime.Prepared handle, long budget, boolean hasCalls) {
            return invoke(handle, budget, hasCalls, Map.of("taskId", "task1"));
        }
        CompletableFuture<KeirRuntime.Outcome> invoke(KeirRuntime.Prepared handle, long budget, boolean hasCalls, Object input) {
            long start = System.nanoTime();
            Map<String, Object> bindings = hasCalls ? Map.of("actorId", "user1") : Map.of();
            var deadline = new KeirRuntime.Deadline(() -> (System.nanoTime() - start) / 1_000_000, budget);
            var invocation = new KeirRuntime.BoundEffectInvocation(bindings, deadline, new RequestKey(this, deadline), EVENT);
            return KeirRuntime.invokeBoundEffectProgram(handle, input, invocation).toCompletableFuture().orTimeout(4, TimeUnit.SECONDS);
        }
    }
}
