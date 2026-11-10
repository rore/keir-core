package dev.keir.benchmark;

import com.fasterxml.jackson.core.*;
import com.sun.net.httpserver.*;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.keir.compiler.KeirCompiler;
import dev.keir.runtime.KeirRuntime;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Opt-in synthetic HTTP host. No real authorization, persistence or network adapters. */
public final class HttpProbe {
    private static final JsonFactory JSON = new JsonFactory();
    private static final long ORIGIN = System.nanoTime();
    private static final Semaphore SLOTS = new Semaphore(32);
    private static final ScheduledExecutorService TIMERS = Executors.newScheduledThreadPool(4);
    private static final AtomicLong correct = new AtomicLong(), busy = new AtomicLong(), failures = new AtomicLong();
    private static final AtomicInteger active = new AtomicInteger(), peakActive = new AtomicInteger(), pending = new AtomicInteger();
    private static final AtomicLong peakHeap = new AtomicLong();
    private static final Map<String,KeirRuntime.Prepared> prepared = new HashMap<>();
    private static Map<String,Object> denseInput;
    private static byte[] expectedTypical, expectedDense;

    private static final class Key {
        final long id;
        final int delay;
        final AtomicInteger calls = new AtomicInteger();
        Key(long id, int delay) { this.id = id; this.delay = delay; }
        String task() { return "task" + id; }
        Map<String,Object> bindings() { return Map.of("actorId", "actor" + id); }
        Map<String,Object> event() { return Map.of("id", "event" + id, "source", task()); }
        @Override public String toString() { throw new AssertionError("Native request key must remain opaque"); }
    }
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return (Map<String,Object>) value; }
    private static long now() { return (System.nanoTime() - ORIGIN) / 1_000_000; }

    // Small JSON bridge using the runtime's already pinned Jackson-core dependency.
    private static Object parse(byte[] bytes) throws IOException {
        try (var p = JSON.createParser(bytes)) { p.nextToken(); return readValue(p); }
    }
    private static Object readValue(JsonParser p) throws IOException {
        return switch (p.currentToken()) {
            case START_OBJECT -> {
                var result = new TreeMap<String,Object>();
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String key = p.currentName(); p.nextToken(); result.put(key, readValue(p));
                }
                yield result;
            }
            case START_ARRAY -> {
                var result = new ArrayList<Object>();
                while (p.nextToken() != JsonToken.END_ARRAY) result.add(readValue(p));
                yield result;
            }
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT -> p.getLongValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            default -> throw new IOException("Unexpected JSON token");
        };
    }
    private static byte[] json(Object value) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var g = JSON.createGenerator(bytes)) { writeValue(g, value); }
        return bytes.toByteArray();
    }
    private static void writeValue(JsonGenerator g, Object value) throws IOException {
        if (value instanceof Map<?,?> m) {
            g.writeStartObject();
            for (var entry : new TreeMap<>(map(m)).entrySet()) {
                g.writeFieldName(entry.getKey()); writeValue(g, entry.getValue());
            }
            g.writeEndObject();
        } else if (value instanceof List<?> list) {
            g.writeStartArray(); for (Object item : list) writeValue(g, item); g.writeEndArray();
        } else if (value instanceof String s) g.writeString(s);
        else if (value instanceof Boolean b) g.writeBoolean(b);
        else if (value instanceof Number n) g.writeNumber(n.toString());
        else if (value == null) g.writeNull();
        else throw new IOException("Unsupported JSON value");
    }
    private static Object read(Path dir, String name) throws IOException { return parse(Files.readAllBytes(dir.resolve(name))); }

    // Check exact dense operators/selects: no early true operand or unreachable LET.
    private static int[] shape(Expr e) {
        check(e.hasCallExpr(), "Dense expression must be a call");
        var call = e.getCallExpr();
        check(!call.hasTarget() && call.getArgsCount() == 2, "Unexpected dense call");
        if (call.getFunction().equals("@in")) {
            for (int i = 0; i < 2; i++) {
                var arg = call.getArgs(i); var select = arg.getSelectExpr();
                check(arg.hasSelectExpr() && !select.getTestOnly() && select.getField().equals(i == 0 ? "targetLabel" : "labels")
                        && select.getOperand().hasIdentExpr() && select.getOperand().getIdentExpr().getName().equals("input"), "Dense select changed");
            }
            return new int[]{5, 3, 1};
        }
        check(call.getFunction().equals("_||_"), "Unexpected dense operator");
        int[] a = shape(call.getArgs(0)), b = shape(call.getArgs(1));
        return new int[]{a[0] + b[0] + 1, Math.max(a[1], b[1]) + 1, a[2] + b[2]};
    }
    private static void compile(Path dir) throws Exception {
        var compiler = new KeirCompiler();
        var facts = new TreeMap<String,Object>();
        for (String rule : List.of("typical", "dense")) {
            var result = compiler.compile(Files.readAllBytes(dir.resolve(rule + ".source")),
                    Files.readAllBytes(dir.resolve(rule + ".model")), KeirCompiler.Environment.supported());
            check(result.succeeded(), "Compilation failed: " + result.diagnostics());
            Files.write(dir.resolve(rule + ".artifact"), result.artifact());
            facts.put(rule, Map.of("artifactBytes", result.artifact().length, "artifactDigest", result.artifactDigest()));
            if (rule.equals("dense")) {
                var plan = map(parse(result.artifact()));
                var expressions = map(plan.get("expressions")); int total = 1, depth = 0, scans = 0;
                check(expressions.size() == 9, "Dense expression count changed");
                var steps = (List<?>)plan.get("steps"); check(steps.size() == 8, "Dense step count");
                for (int i = 0; i < 8; i++) {
                    var step = map(steps.get(i));
                    check(step.get("id").equals("scan" + i) && step.get("op").equals("BIND"), "Dense order changed");
                    String ref = (String)map(step.get("value")).get("expr");
                    var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expressions.get(ref)).get("checked")));
                    int[] s = shape(checked.getExpr());
                    check(s[0] == 59 && checked.getTypeMapCount() == 59 && s[2] == 10, "Dense shape changed");
                    total += s[0]; depth = Math.max(depth, s[1]); scans += s[2];
                }
                String ref = (String)map(map(map(plan.get("return")).get("fields")).get("eligible")).get("expr");
                var returned = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expressions.get(ref)).get("checked")));
                check(returned.getTypeMapCount() == 1 && returned.getExpr().hasIdentExpr()
                        && returned.getExpr().getIdentExpr().getName().equals("scan7"), "Dense return changed");
                check(total == 473 && depth == 7 && scans == 80, "Dense totals changed");
                facts.put("denseShape", Map.of("nodes", total, "depth", depth, "scans", scans, "elementVisitsBySemantics", scans * 16));
            }
        }
        System.out.println(new String(json(facts), java.nio.charset.StandardCharsets.UTF_8));
    }

    private static CompletionStage<?> callback(String cap, Map<String,Object> args, Map<String,Object> bindings,
            long remaining, KeirRuntime.BoundEffectContext ctx) {
        check(ctx.requestKey() instanceof Key, "Key type changed"); Key key = (Key)ctx.requestKey();
        check(ctx.requestKey() == key && bindings.equals(key.bindings()) && ctx.event().equals(key.event()), "Invocation context changed");
        check(remaining > 0 && remaining <= 5000, "Remaining deadline changed");
        int ordinal = key.calls.getAndIncrement(); Object result;
        if (cap.equals("tasks.get")) {
            check(ordinal == 0 && args.equals(Map.of("taskId", key.task())), "READ input/order changed");
            result = Map.of("kind", "SUCCESS", "value", Map.of("id", key.task(), "projectId", "project1", "status", "COMPLETED"));
        } else {
            check(cap.equals("tasks.create") && ordinal == 1 && args.equals(Map.of("projectId", "project1", "title", "Follow up")), "WRITE input/order changed");
            result = Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", Map.of("id", "created" + key.id));
        }
        var future = new CompletableFuture<Object>(); pending.incrementAndGet();
        TIMERS.schedule(() -> { pending.decrementAndGet(); future.complete(result); }, key.delay, TimeUnit.MILLISECONDS);
        return future;
    }
    private static Map<String,Object> event(List<Map<String,Object>> trace, String kind, String step, String cap) {
        var e = new TreeMap<String,Object>(); e.put("sequence", trace.size()); e.put("event", kind);
        if (step != null) e.put("step", step); if (cap != null) e.put("capability", cap); trace.add(e); return e;
    }
    // Independent native implementation; expected outcomes come from a separate fixture/manifest.
    private static Map<String,Object> nativeOutcome(boolean dense, boolean eligible) {
        var trace = new ArrayList<Map<String,Object>>(); event(trace, "INVOCATION_START", null, null);
        if (dense) {
            for (int i = 0; i < 8; i++) { event(trace, "STEP_START", "scan" + i, null); event(trace, "VALUE_BOUND", "scan" + i, null); }
        } else {
            event(trace, "STEP_START", "eligibleProject", null); event(trace, "VALUE_BOUND", "eligibleProject", null);
            event(trace, "STEP_START", "source", null); event(trace, "CALL_DISPATCHED", "source", "tasks.get"); event(trace, "CALL_SUCCEEDED", "source", "tasks.get");
            event(trace, "STEP_START", "eligible", null); event(trace, "VALUE_BOUND", "eligible", null);
            event(trace, "STEP_START", "created", null); event(trace, "GUARD_EVALUATED", "created", null).put("taken", eligible);
            check(eligible, "Native fixture must take the write");
            event(trace, "EFFECT_INTENT", "created", "tasks.create"); event(trace, "CALL_DISPATCHED", "created", "tasks.create");
            event(trace, "EFFECT_RECORDED", "created", "tasks.create").put("state", "CONFIRMED"); event(trace, "CALL_SUCCEEDED", "created", "tasks.create");
        }
        event(trace, "RETURNED", null, null); event(trace, "TERMINATED", null, null);
        return Map.of("classification", "COMPLETED", "result", dense ? Map.of("eligible", eligible) : Map.of("status", "CREATED"),
                "effects", dense ? List.of() : List.of(Map.of("step", "created", "capability", "tasks.create", "state", "CONFIRMED")), "trace", trace);
    }
    private static Map<String,Object> nativeRun(boolean dense, Map<String,Object> input, Key key, KeirRuntime.Deadline deadline) {
        if (dense) {
            boolean last = false;
            for (int step = 0; step < 8; step++) {
                boolean found = false;
                for (int scan = 0; scan < 10; scan++) {
                    boolean member = false;
                    for (Object label : (List<?>)input.get("labels")) if (input.get("targetLabel").equals(label)) { member = true; break; }
                    found = found || member;
                }
                last = found;
            }
            return nativeOutcome(true, last);
        }
        var ctx = new KeirRuntime.BoundEffectContext(key, key.event(), deadline);
        var read = map(callback("tasks.get", Map.of("taskId", input.get("taskId")), key.bindings(), 5000, ctx).toCompletableFuture().join());
        var task = map(read.get("value")); boolean eligible = task.get("status").equals("COMPLETED") && task.get("projectId").equals("project1");
        check(eligible, "Unexpected native eligibility");
        var write = map(callback("tasks.create", Map.of("projectId", "project1", "title", "Follow up"), key.bindings(), 5000, ctx).toCompletableFuture().join());
        check(write.get("effect").equals("CONFIRMED") && map(write.get("value")).get("id").equals("created" + key.id), "Native write response changed");
        return nativeOutcome(false, eligible);
    }
    private static Map<String,Object> stats() {
        var mem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return Map.of("correct", correct.get(), "busy", busy.get(), "failures", failures.get(), "active", active.get(),
                "peakActive", peakActive.get(), "pendingCallbacks", pending.get(), "cpuNanos", ProcessHandle.current().info().totalCpuDuration().orElseThrow().toNanos(),
                "heapUsedBytes", mem.getUsed(), "sampledPeakHeapBytes", peakHeap.get());
    }
    private static void respond(HttpExchange x, int status, Object body) throws IOException {
        byte[] bytes = json(body); x.getResponseHeaders().set("Content-Type", "application/json");
        x.sendResponseHeaders(status, bytes.length); try (var stream = x.getResponseBody()) { stream.write(bytes); }
    }
    private static void handle(HttpExchange x) throws IOException {
        if (x.getRequestURI().getPath().equals("/stats")) { respond(x, 200, stats()); return; }
        if (x.getRequestURI().getPath().equals("/reset")) {
            check(active.get() == 0 && pending.get() == 0 && SLOTS.availablePermits() == 32, "Previous stage not drained");
            correct.set(0); busy.set(0); failures.set(0); peakActive.set(0); peakHeap.set(0); respond(x, 200, stats()); return;
        }
        if (!SLOTS.tryAcquire()) { busy.incrementAndGet(); respond(x, 503, Map.of("busy", true)); return; }
        active.incrementAndGet(); peakActive.accumulateAndGet(active.get(), Math::max);
        try {
            var q = new HashMap<String,String>();
            for (String part : x.getRequestURI().getRawQuery().split("&")) { String[] pair = part.split("=", 2); q.put(pair[0], pair[1]); }
            long id = Long.parseLong(q.get("id")); boolean dense = q.get("rule").equals("dense");
            int delay = Integer.parseInt(q.get("delay")); check(delay >= 0 && delay <= 20, "Delay outside workload");
            var key = new Key(id, delay); var input = dense ? new TreeMap<>(denseInput) : new TreeMap<String,Object>();
            input.put(dense ? "itemId" : "taskId", dense ? "item" + id : key.task());
            var deadline = new KeirRuntime.Deadline(HttpProbe::now, now() + 5000);
            Map<String,Object> outcome;
            if (q.get("mode").equals("keir")) outcome = KeirRuntime.invokeBoundEffectProgram(prepared.get(dense ? "dense" : "typical"), input,
                    new KeirRuntime.BoundEffectInvocation(dense ? Map.of() : key.bindings(), deadline, key,
                            dense ? Map.of("id", "event" + id, "source", "item" + id) : key.event())).toCompletableFuture().join().toMap();
            else outcome = nativeRun(dense, input, key, deadline);
            check(key.calls.get() == (dense ? 0 : 2), "Callback count changed");
            byte[] expected = dense ? expectedDense : expectedTypical;
            if (q.containsKey("bad")) expected = json(Map.of("deliberately", "wrong"));
            if (!Arrays.equals(json(outcome), expected)) throw new AssertionError("Complete outcome or trace mismatch: "
                    + new String(json(outcome), java.nio.charset.StandardCharsets.UTF_8));
            correct.incrementAndGet(); respond(x, 200, Map.of("id", id, "correct", true));
        } catch (Throwable failure) {
            if (!x.getRequestURI().getRawQuery().contains("&bad=1")) System.err.println(failure);
            failures.incrementAndGet(); respond(x, 500, Map.of("correct", false, "error", failure instanceof AssertionError ? "ASSERTION" : "INVOCATION"));
        } finally { active.decrementAndGet(); SLOTS.release(); }
    }
    private static void serve(Path dir) throws Exception {
        expectedTypical = json(map(read(dir, "typical.expected"))); expectedDense = json(map(read(dir, "dense.expected")));
        denseInput = map(read(dir, "dense.input"));
        for (String rule : List.of("typical", "dense")) {
            byte[] model = Files.readAllBytes(dir.resolve(rule + ".model")); var m = map(parse(model));
            var adapters = new TreeMap<String,KeirRuntime.BoundEffectAdapter>();
            for (var entry : map(m.get("capabilities")).entrySet()) {
                String cap = entry.getKey();
                adapters.put(cap, new KeirRuntime.BoundEffectAdapter((String)map(entry.getValue()).get("version"), "simulation",
                        (String)map(m.get("hostKey")).get("type"), (args, bindings, remaining, ctx) -> {
                            check(rule.equals("typical"), "Dense adapter dispatched"); return callback(cap, args, bindings, remaining, ctx);
                        }));
            }
            var bound = KeirRuntime.bindModel(model, adapters); check(bound.succeeded(), "Bind failed");
            var program = KeirRuntime.prepareProgram(Files.readAllBytes(dir.resolve(rule + ".artifact")), bound.boundModel());
            check(program.succeeded(), "Prepare failed"); prepared.put(rule, program.prepared());
        }
        TIMERS.scheduleAtFixedRate(() -> peakHeap.accumulateAndGet(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), Math::max), 0, 100, TimeUnit.MILLISECONDS);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 128); server.setExecutor(executor); server.createContext("/", HttpProbe::handle); server.start();
        var ready = Map.of("port", server.getAddress().getPort(), "java", System.getProperty("java.version"), "availableProcessors", Runtime.getRuntime().availableProcessors(), "environment", KeirRuntime.ENVIRONMENT);
        System.out.println(new String(json(ready), java.nio.charset.StandardCharsets.UTF_8));
        // Parent closes stdin only after all HTTP requests and callbacks have drained.
        System.in.read(); server.stop(0); executor.shutdown(); TIMERS.shutdown();
        check(executor.awaitTermination(5, TimeUnit.SECONDS) && TIMERS.awaitTermination(5, TimeUnit.SECONDS), "Executors retained work");
        check(active.get() == 0 && pending.get() == 0 && SLOTS.availablePermits() == 32, "Work retained on shutdown");
        System.out.println("{\"shutdown\":true}");
    }
    public static void main(String[] args) throws Exception {
        check(Runtime.version().feature() == 21, "Use Java 21");
        if (args[0].equals("compile")) compile(Path.of(args[1])); else serve(Path.of(args[1]));
    }
}
