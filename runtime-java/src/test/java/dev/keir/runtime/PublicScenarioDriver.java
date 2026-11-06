package dev.keir.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static dev.keir.runtime.RuntimeData.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the compiler-owned frozen corpus through the public host-bound API. */
public final class PublicScenarioDriver {
    private static final Map<String,List<String>> GROUPS = Map.ofEntries(
            Map.entry("main", List.of("lost-response", "normal", "open", "other-project", "read-throws",
                    "response-expired", "unknown-commit", "write-rejected", "write-transient")),
            Map.entry("zero", List.of("zero")),
            Map.entry("after-write-read", List.of("after-write-read", "confirmed-then-read-failure")),
            Map.entry("no-call", List.of("no-call")),
            Map.entry("handler", List.of("handled-rejection")),
            Map.entry("inventory", List.of("inventory-adjusted", "inventory-optional-false", "inventory-lazy-overflow",
                    "inventory-selected-overflow", "inventory-handler-omits-due", "inventory-max-no-match")),
            Map.entry("inventory-algebra", List.of("inventory-confirmed-then-overflow")),
            Map.entry("inventory-enum", List.of("inventory-enum-literal")),
            Map.entry("title", List.of("title-256", "title-257", "title-unicode")),
            Map.entry("title-max", List.of("title-max")),
            Map.entry("title-composed", List.of("title-composed-overflow", "title-composed-valid")));
    private static final Set<String> FIXTURES = Set.of("normal", "read-throws", "lost-response", "response-expired");
    private static final String ID = "[A-Za-z0-9][A-Za-z0-9_.-]{0,63}";
    private static final String STEP = "[A-Za-z][A-Za-z0-9]{0,63}";
    private static final String CODE = "[A-Z][A-Z0-9_]{0,63}";
    private static final Set<String> STATES = Set.of("CONFIRMED", "NONE", "UNKNOWN");
    private static final long EXPIRES_AT = 10_000;
    private static final Map<String,Object> EVENT = Map.of("id", "event1", "source", "task1");
    private final Map<String,byte[]> pins = new HashMap<>();

    PublicScenarioDriver() throws IOException {
        for (String group : GROUPS.keySet()) {
            verifyPin(artifactName(group));
            verifyPin(modelName(group));
        }
    }

    public static void main(String[] args) throws Exception {
        assertEquals(0, args.length, "Public scenario driver takes no arguments");
        System.out.writeBytes(canonical(new PublicScenarioDriver().runCorpus()));
        System.out.println();
    }

    Map<String,Object> runCorpus() throws Exception {
        Map<String,Map<String,Object>> documents = new LinkedHashMap<>();
        for (String group : new TreeSet<>(GROUPS.keySet())) {
            for (String name : GROUPS.get(group)) {
                String label = group + "/" + name;
                documents.put(label, assertDoesNotThrow(() -> scenario(label), label));
            }
        }
        assertEquals(28, documents.size(), "Shared scenario count");
        checkUniqueIds(documents.values());

        List<Object> scenarios = new ArrayList<>();
        for (var entry : documents.entrySet()) {
            String label = entry.getKey();
            scenarios.add(assertDoesNotThrow(() -> runScenario(group(label), entry.getValue()), label));
        }

        List<Map<String,Object>> manifest = rows(object(parseJson(resource("scenario-vectors.json"), 65_536)).get("vectors"));
        assertEquals(51, manifest.size(), "Shared vector count");
        checkUniqueIds(manifest);
        List<Object> vectors = new ArrayList<>();
        for (var vector : manifest) {
            String id = (String)vector.get("id");
            String expected = (String)vector.get("expect");
            byte[] bytes = vectorBytes(vector);
            String selectedGroup = vector.containsKey("scenario") ? group((String)vector.get("scenario")) : null;
            Run run = assertDoesNotThrow(() -> run(selectedGroup, bytes, null, vector, expected), id);
            assertEquals(expected, run.phase(), id);
            if (!expected.equals("accept")) assertEquals(0, run.dispatchCount(), id);
            vectors.add(Map.of("id", id, "phase", run.phase(), "dispatchCount", run.dispatchCount()));
        }
        return Map.of("scenarios", scenarios, "vectors", vectors);
    }

    static Map<String,Object> scenario(String label) throws IOException {
        return object(parseScenario(resource("scenarios/" + label + ".json")));
    }

    static void checkUniqueIds(Collection<Map<String,Object>> documents) {
        Set<Object> seen = new HashSet<>();
        for (var document : documents) {
            Object id = document.get("id");
            assertTrue(seen.add(id), "Duplicate scenario id " + id);
        }
    }

    Map<String,Object> runScenario(String group, Map<String,Object> document) throws Exception {
        Run result = run(group, null, document, Map.of(), "accept");
        if (!result.phase().equals("accept")) assertEquals(0, result.dispatchCount(), "Rejected scenario dispatched a stub");
        assertEquals("accept", result.phase(), () -> "Scenario rejected at " + result.phase() + ": " + result.failure());
        return result.report();
    }

    private record Run(String phase, int dispatchCount, Map<String,Object> report, Throwable failure) {}

    private Run run(String group, byte[] bytes, Map<String,Object> decoded, Map<String,Object> vector, String expected)
            throws Exception {
        String phase = "lexical";
        var callbacks = new Callbacks(group != null && group.startsWith("inventory")
                ? Map.of("id", "event1", "source", "item1") : EVENT);
        try {
            Object document = bytes == null ? decoded : parseScenario(bytes);
            phase = "structural";
            var scenario = object(document);
            structure(scenario);
            phase = "byte-fields";
            byteFields(scenario);
            if (expected.equals("accept-byte-fields")) return new Run(expected, callbacks.calls.size(), null, null);

            // Pin verification and harness setup failures cannot count as compatibility evidence.
            phase = "setup";
            assertTrue(GROUPS.containsKey(group), "Missing trusted artifact selection");
            byte[] dcmBytes = withFormat(pins.get(vector.getOrDefault("model", modelName(group))), vector.get("modelFormat"));
            byte[] artifactBytes = withFormat(pins.get(artifactName(group)), vector.get("artifactFormat"));
            var dcm = object(parseCanonical(dcmBytes));
            var artifact = object(parseCanonical(artifactBytes));
            var binding = KeirRuntime.bindModel(dcmBytes, callbacks.implementations(dcm));
            if (vector.containsKey("modelFormat")) {
                assertRejected(binding.succeeded(), binding.diagnostic(), "ADMISSION_INVALID", "/binding/dcm");
                return new Run("compatibility", callbacks.calls.size(), null, null);
            }
            assertTrue(binding.succeeded(), () -> "Scenario DCM compatibility: " + binding.diagnostic());
            var preparation = KeirRuntime.prepareProgram(artifactBytes, binding.boundModel());
            if (vector.containsKey("artifactFormat") || vector.containsKey("model")) {
                assertRejected(preparation.succeeded(), preparation.diagnostic(), "ARTIFACT_INVALID",
                        vector.containsKey("artifactFormat") ? "/artifact/format" : "/artifact/modelRef");
                return new Run("compatibility", callbacks.calls.size(), null, null);
            }
            assertTrue(preparation.succeeded(), () -> "Scenario artifact compatibility: " + preparation.diagnostic());

            phase = "fixture";
            var given = object(scenario.get("given"));
            callbacks.fixture = (String)given.get("fixture");
            assertTrue(FIXTURES.contains(callbacks.fixture), "Unknown trusted fixture " + callbacks.fixture);
            phase = "typing";
            var entry = object(object(dcm.get("entrypoints")).get(artifact.get("entrypoint")));
            var stubs = rows(scenario.get("stubs"));
            var expect = object(scenario.get("expect"));
            var calls = rows(expect.get("calls"));
            var outcome = object(expect.get("outcome"));
            var consumption = object(expect.get("consumption"));
            type(dcm, entry.get("input"), given.get("input"), "/given/input");
            for (var payload : List.of(outcome, consumption)) {
                if (payload.containsKey("result")) type(dcm, entry.get("output"), payload.get("result"), "/expect/result");
            }
            for (var call : stubs) typedCall(dcm, call);
            for (var call : calls) typedCall(dcm, call);

            phase = "semantics";
            var stubCalls = stubs.stream().map(stub -> without(stub, "response")).toList();
            assertEquals(calls, stubCalls, "Stub calls and independent expected calls");
            semantics(artifact, dcm, calls, outcome);
            assertEquals(consumption, report(entry, outcome), "Independent expected EVENT_REPORT");

            phase = "execution";
            assertEquals("accept", expected, "Rejection vector passed every preflight phase");
            assertTrue(callbacks.calls.isEmpty(), "Preflight dispatched a stub");
            callbacks.stubs = stubs;
            var context = new KeirRuntime.BoundEffectInvocation(
                    Set.of("no-call", "inventory-enum").contains(group) ? Map.of() : Map.of("actorId", "user1"),
                    callbacks.deadline, callbacks.key, callbacks.event);
            var actual = KeirRuntime.invokeBoundEffectProgram(preparation.prepared(), given.get("input"), context)
                    .toCompletableFuture().get(3, TimeUnit.SECONDS);
            assertNull(callbacks.mismatch, () -> "Callback mismatch despite runtime outcome " + actual.error()
                    + ": " + callbacks.mismatch);
            assertEquals(stubs.size(), callbacks.calls.size(), "Each stub must be consumed exactly once");
            var dispatches = events(actual.trace(), "CALL_DISPATCHED");
            assertEquals(callbacks.calls.size(), dispatches.size(), "Callback and dispatch trace counts");
            List<Map<String,Object>> observedCalls = new ArrayList<>();
            for (int i = 0; i < callbacks.calls.size(); i++) {
                var call = new LinkedHashMap<>(callbacks.calls.get(i));
                var dispatch = dispatches.get(i);
                assertEquals(call.get("capability"), dispatch.get("capability"), "Callback and trace capability");
                call.put("step", dispatch.get("step"));
                observedCalls.add(call);
            }
            assertEquals(stubCalls, observedCalls, "Observed calls against stubs");
            assertEquals(calls, observedCalls, "Observed calls against independent expectation");
            assertArrayEquals(canonical(outcome), actual.toJson(), "Complete runtime outcome");
            var observedConsumption = report(entry, actual.toMap());
            assertEquals(consumption, observedConsumption, "Observed EVENT_REPORT");
            return new Run("accept", callbacks.calls.size(), Map.of("group", group, "id", scenario.get("id"),
                    "calls", observedCalls, "outcome", actual.toMap(), "consumption", observedConsumption), null);
        } catch (RuntimeFault | AssertionError failure) {
            if (phase.equals("setup") || phase.equals("execution")) throw failure;
            return new Run(phase, callbacks.calls.size(), null, failure);
        }
    }

    private static final class Callbacks {
        final Object key = new Object();
        final AtomicLong now = new AtomicLong();
        final KeirRuntime.Deadline deadline = new KeirRuntime.Deadline(now::get, EXPIRES_AT);
        final List<Map<String,Object>> calls = new ArrayList<>();
        final Map<String,Object> event;
        List<Map<String,Object>> stubs = List.of();
        String fixture;
        AssertionError mismatch;

        Callbacks(Map<String,Object> event) {
            this.event = event;
        }

        Map<String,KeirRuntime.BoundEffectAdapter> implementations(Map<String,Object> dcm) {
            String keyType = (String)object(dcm.get("hostKey")).get("type");
            Map<String,KeirRuntime.BoundEffectAdapter> implementations = new HashMap<>();
            object(dcm.get("capabilities")).forEach((capability, definition) -> {
                var registration = object(definition);
                String version = (String)registration.get("version");
                boolean write = registration.get("effect").equals("WRITE");
                implementations.put(capability, new KeirRuntime.BoundEffectAdapter(version, "simulation", keyType,
                        (args, protectedArgs, remaining, context) -> {
                            int index = calls.size();
                            var capture = Map.of("capability", capability, "version", version,
                                    "args", immutable(args), "protectedArgs", immutable(protectedArgs));
                            calls.add(capture);
                            try {
                                assertTrue(index < stubs.size(), "Unexpected callback");
                                assertEquals(without(stubs.get(index), "step", "response"), capture, "Stub callback request");
                                assertSame(key, context.requestKey(), "Trusted native key");
                                assertEquals(event, context.event(), "Trusted event");
                                assertEquals(deadline, context.deadline(), "Trusted deadline");
                                assertEquals(EXPIRES_AT - now.get(), remaining, "Callback budget");
                            } catch (AssertionError failure) {
                                if (mismatch == null) mismatch = failure;
                                throw new IllegalStateException("Stub callback mismatch", failure);
                            }
                            if ("read-throws".equals(fixture) && index == 0 && !write)
                                throw new IllegalStateException("Fixture READ failure");
                            if ("lost-response".equals(fixture) && write)
                                throw new IllegalStateException("Fixture lost WRITE response");
                            if ("response-expired".equals(fixture) && write) now.set(EXPIRES_AT);
                            return CompletableFuture.completedFuture(stubs.get(index).get("response"));
                        }));
            });
            return implementations;
        }
    }

    private static void typedCall(Map<String,Object> dcm, Map<String,Object> call) {
        var capability = object(object(dcm.get("capabilities")).get(call.get("capability")));
        assertEquals(capability.get("version"), call.get("version"), "Selected capability version");
        var parameters = object(capability.get("parameters"));
        typedArgs(dcm, parameters, call.get("args"), "agent");
        typedArgs(dcm, parameters, call.get("protectedArgs"), "host");
        if (!call.containsKey("response")) return;
        var response = object(call.get("response"));
        assertEquals(capability.get("effect").equals("WRITE"), response.containsKey("effect"), "WRITE evidence");
        if (response.get("kind").equals("SUCCESS")) type(dcm, capability.get("result"), response.get("value"), "/stub/result");
        if (response.get("kind").equals("BUSINESS_REJECTION"))
            assertTrue(list(capability.get("errors"), "/stub").contains(response.get("code")), "Declared stub error");
    }

    private static void typedArgs(Map<String,Object> dcm, Map<String,Object> parameters, Object arguments, String binding) {
        var args = object(arguments);
        Set<String> names = new HashSet<>();
        parameters.forEach((name, definition) -> {
            var parameter = object(definition);
            if (binding.equals(parameter.get("binding"))) {
                if (Boolean.TRUE.equals(parameter.get("optional")) && !args.containsKey(name)) return;
                names.add(name);
                type(dcm, parameter.get("type"), args.get(name), "/call/" + binding + "/" + name);
            }
        });
        assertEquals(names, args.keySet(), "Exact " + binding + " argument names");
    }

    private static void type(Map<String,Object> dcm, Object name, Object value, String path) {
        Artifact.value(dcm, (String)name, value, path, "SCENARIO_INVALID");
    }

    private static void semantics(Map<String,Object> artifact, Map<String,Object> dcm,
                                  List<Map<String,Object>> calls, Map<String,Object> outcome) {
        var trace = rows(outcome.get("trace"));
        for (int i = 0; i < trace.size(); i++) {
            var event = trace.get(i);
            assertEquals(i, ((Number)event.get("sequence")).intValue(), "Contiguous trace");
            assertEquals(i == 0, event.get("event").equals("INVOCATION_START"), "Trace start");
            assertEquals(i == trace.size() - 1, event.get("event").equals("TERMINATED"), "Trace termination");
        }
        Object errorCode = outcome.containsKey("error") ? object(outcome.get("error")).get("code") : null;
        assertEquals(errorCode, trace.getLast().get("code"), "Termination code");
        var dispatches = events(trace, "CALL_DISPATCHED");
        assertEquals(targets(calls), targets(dispatches), "Expected dispatch order");
        for (var guard : events(trace, "GUARD_EVALUATED")) {
            if (Boolean.FALSE.equals(guard.get("taken"))) {
                assertTrue(dispatches.stream().noneMatch(call -> call.get("step").equals(guard.get("step"))),
                        "Expected dispatch of a step with a false guard");
            }
        }
        var steps = rows(artifact.get("steps"));
        for (var call : calls) {
            assertTrue(steps.stream().anyMatch(step -> step.get("op").equals("CALL")
                    && step.get("id").equals(call.get("step")) && step.get("capability").equals(call.get("capability"))),
                    "Expected call names an artifact step");
        }
        var intents = events(trace, "EFFECT_INTENT");
        var receipts = events(trace, "EFFECT_RECORDED");
        var catalog = object(dcm.get("capabilities"));
        var writes = calls.stream().filter(call -> object(catalog.get(call.get("capability"))).get("effect").equals("WRITE")).toList();
        assertEquals(targets(writes), targets(intents), "Expected write intents");
        assertEquals(targets(writes), targets(receipts), "Expected receipt targets");
        assertEquals(outcome.get("effects"), receipts.stream().map(receipt -> without(receipt, "event", "sequence")).toList(),
                "Expected trace receipts and outcome effects");
        for (var intent : intents) {
            var write = dispatches.stream().filter(call -> call.get("step").equals(intent.get("step"))).findFirst().orElseThrow();
            assertTrue(sequence(intent) < sequence(write) && sequence(write) < sequence(receipts.getFirst()),
                    "Intent, WRITE dispatch and receipt order");
        }
        String classification = (String)outcome.get("classification");
        List<Object> states = receipts.stream().map(receipt -> receipt.get("state")).toList();
        assertEquals(classification.equals("UNCERTAIN"), states.contains("UNKNOWN"), "Uncertainty and effect evidence");
        if (classification.equals("COMPLETED"))
            assertFalse(states.contains("NONE"), "Completed WRITE requires confirmation");
    }

    private static Map<String,Object> report(Map<String,Object> entry, Map<String,Object> outcome) {
        assertEquals("EVENT_REPORT", object(entry.get("consumer")).get("kind"), "Selected consumer");
        var result = without(outcome, "trace");
        result.put("status", "REPORTED");
        return result;
    }

    private static void structure(Map<String,Object> scenario) {
        fields(scenario, "format id given stubs expect");
        assertEquals("keir-scenario/2", scenario.get("format"), "Scenario format");
        matches(scenario.get("id"), ID);
        var given = object(scenario.get("given"));
        fields(given, "input fixture");
        object(given.get("input"));
        matches(given.get("fixture"), ID);
        var expect = object(scenario.get("expect"));
        fields(expect, "outcome calls consumption");
        for (var call : boundedRows(expect.get("calls"), 0, 4)) callShape(call, false);
        for (var stub : boundedRows(scenario.get("stubs"), 0, 4)) callShape(stub, true);
        var outcome = object(expect.get("outcome"));
        payloadShape(outcome, "classification effects trace");
        for (var event : boundedRows(outcome.get("trace"), 2, 64)) traceShape(event);
        var consumption = object(expect.get("consumption"));
        switch (string(consumption.get("status"))) {
            case "REPORTED" -> payloadShape(consumption, "status classification effects");
            case "NOT_REPORTED" -> {
                fields(consumption, "status code");
                matches(consumption.get("code"), CODE);
            }
            default -> fail("Unknown consumption status");
        }
    }

    private static void callShape(Map<String,Object> call, boolean stub) {
        fields(call, "step capability version args protectedArgs" + (stub ? " response" : ""));
        targetShape(call);
        boundedText(call.get("version"), 1, 256);
        object(call.get("args"));
        object(call.get("protectedArgs"));
        if (!stub) return;
        var response = object(call.get("response"));
        String kind = string(response.get("kind"));
        String required = switch (kind) {
            case "SUCCESS" -> {
                object(response.get("value"));
                yield "kind value";
            }
            case "BUSINESS_REJECTION" -> {
                matches(response.get("code"), CODE);
                yield "kind code";
            }
            case "TRANSIENT_FAILURE", "UNKNOWN_COMMIT" -> "kind";
            default -> throw new AssertionError("Unknown stub response kind " + kind);
        };
        fields(response, required + (response.containsKey("effect") || kind.equals("UNKNOWN_COMMIT") ? " effect" : ""));
        if (response.containsKey("effect")) {
            assertEquals(switch (kind) {
                case "SUCCESS" -> "CONFIRMED";
                case "UNKNOWN_COMMIT" -> "UNKNOWN";
                default -> "NONE";
            }, response.get("effect"), "Stub response effect");
        }
    }

    private static void payloadShape(Map<String,Object> payload, String required) {
        String classification = string(payload.get("classification"));
        boolean result = payload.containsKey("result");
        boolean causeAllowed = false;
        switch (classification) {
            case "COMPLETED" -> fields(payload, required + " result");
            case "BUSINESS_REJECTION" -> fields(payload, required + (result ? " result error" : " error"));
            case "UNCERTAIN", "FAILED", "LIMIT_EXCEEDED" -> {
                fields(payload, required + " error");
                causeAllowed = !classification.equals("UNCERTAIN");
            }
            default -> fail("Unknown outcome classification " + classification);
        }
        if (result) object(payload.get("result"));
        if (payload.containsKey("error")) errorShape(object(payload.get("error")), causeAllowed);
        for (var effect : boundedRows(payload.get("effects"), 0, 1)) {
            fields(effect, "step capability state");
            targetShape(effect);
            assertTrue(STATES.contains(string(effect.get("state"))), "Unknown effect state");
        }
    }

    private static void errorShape(Map<String,Object> error, boolean causeAllowed) {
        boolean cause = causeAllowed && error.containsKey("cause");
        fields(error, cause ? "code path cause" : "code path");
        matches(error.get("code"), CODE);
        boundedText(error.get("path"), 0, 1024);
        if (cause) errorShape(object(error.get("cause")), false);
    }

    private static void traceShape(Map<String,Object> event) {
        String type = string(event.get("event"));
        String attributes = switch (type) {
            case "INVOCATION_START", "RETURNED" -> "";
            case "STEP_START", "VALUE_BOUND", "STEP_SKIPPED" -> " step";
            case "GUARD_EVALUATED" -> " step taken";
            case "EFFECT_INTENT", "CALL_DISPATCHED", "CALL_SUCCEEDED" -> " step capability";
            case "EFFECT_RECORDED" -> " step capability state";
            case "TERMINATED" -> event.containsKey("code") ? " code" : "";
            default -> throw new AssertionError("Unknown trace event " + type);
        };
        fields(event, "sequence event" + attributes);
        number(event.get("sequence"), 0, 63, "/trace/sequence");
        if (event.containsKey("step")) matches(event.get("step"), STEP);
        if (event.containsKey("capability")) boundedText(event.get("capability"), 1, 256);
        if (event.containsKey("taken")) assertInstanceOf(Boolean.class, event.get("taken"));
        if (event.containsKey("state")) assertTrue(STATES.contains(string(event.get("state"))), "Unknown trace state");
        if (event.containsKey("code")) matches(event.get("code"), CODE);
    }

    private static void targetShape(Map<String,Object> target) {
        matches(target.get("step"), STEP);
        boundedText(target.get("capability"), 1, 256);
    }

    private static void byteFields(Map<String,Object> scenario) {
        var expect = object(scenario.get("expect"));
        var outcome = object(expect.get("outcome"));
        var consumption = object(expect.get("consumption"));
        for (var values : List.of(scenario.get("stubs"), expect.get("calls"), outcome.get("trace"),
                outcome.get("effects"), consumption.getOrDefault("effects", List.of()))) {
            for (var row : rows(values)) {
                if (row.containsKey("capability")) text(row.get("capability"), "/capability", true, 256);
                if (row.containsKey("version")) text(row.get("version"), "/version", true, 256);
            }
        }
        for (var payload : List.of(outcome, consumption)) {
            if (!payload.containsKey("error")) continue;
            var error = object(payload.get("error"));
            text(error.get("path"), "/error/path", false, 1024);
            if (error.containsKey("cause")) text(object(error.get("cause")).get("path"), "/error/cause/path", false, 1024);
        }
    }

    private void verifyPin(String name) throws IOException {
        if (pins.containsKey(name)) return;
        byte[] bytes = resource(name + ".json");
        assertEquals(new String(resource(name + ".sha256"), UTF_8).strip(), digest(bytes), name + " pin");
        pins.put(name, bytes);
    }

    private static byte[] resource(String name) throws IOException {
        try (var stream = PublicScenarioDriver.class.getResourceAsStream("/keir-2/" + name)) {
            if (stream == null) throw new IOException("Missing public conformance resource " + name);
            return stream.readAllBytes();
        }
    }

    private static byte[] vectorBytes(Map<String,Object> vector) throws IOException {
        if (vector.containsKey("utf8")) return string(vector.get("utf8")).getBytes(UTF_8);
        if (vector.containsKey("base64")) return Base64.getDecoder().decode(string(vector.get("base64")));
        byte[] bytes = resource("scenarios/" + vector.get("scenario") + ".json");
        if (vector.containsKey("replace")) {
            var replacement = list(vector.get("replace"), "/vector/replace");
            String search = string(replacement.getFirst());
            String template = string(replacement.getLast());
            if (vector.containsKey("expand")) {
                var expansion = list(vector.get("expand"), "/vector/expand");
                template = replaceFirst(template, "@", string(expansion.getFirst()).repeat(((Number)expansion.getLast()).intValue()));
            }
            bytes = replaceFirst(new String(bytes, UTF_8), search, template).getBytes(UTF_8);
        }
        if (vector.containsKey("padToBytes")) {
            int target = ((Number)vector.get("padToBytes")).intValue();
            assertTrue(target >= bytes.length, "Vector padding target");
            int original = bytes.length;
            bytes = Arrays.copyOf(bytes, target);
            Arrays.fill(bytes, original, target, (byte)' ');
        }
        return bytes;
    }

    private static String replaceFirst(String input, String search, String replacement) {
        int index = input.indexOf(search);
        assertTrue(index >= 0, () -> "Missing vector replacement target " + search);
        return input.substring(0, index) + replacement + input.substring(index + search.length());
    }

    private static byte[] withFormat(byte[] pinned, Object format) {
        assertNotNull(pinned, "Missing verified pin");
        if (format == null) return pinned;
        var envelope = new LinkedHashMap<>(object(parseCanonical(pinned)));
        envelope.put("format", format);
        return canonical(envelope);
    }

    private static void assertRejected(boolean succeeded, KeirRuntime.Diagnostic diagnostic, String code, String path) {
        assertFalse(succeeded, "Incompatible bytes unexpectedly admitted");
        assertNotNull(diagnostic, "Runtime compatibility diagnostic");
        assertEquals(code, diagnostic.code(), diagnostic.toString());
        assertEquals(path, diagnostic.path(), diagnostic.toString());
    }

    private static String group(String label) { return label.substring(0, label.indexOf('/')); }
    private static String artifactName(String group) { return group.equals("main") ? "artifact" : "artifact-" + group; }
    private static String modelName(String group) {
        if (group.equals("inventory-enum")) return "dcm-inventory-enum";
        if (group.startsWith("inventory")) return "dcm-inventory";
        return group.startsWith("title") ? "dcm-title" : "dcm";
    }
    private static Map<String,Object> object(Object value) { return map(value, "/scenario"); }
    private static List<Map<String,Object>> rows(Object value) {
        return list(value, "/scenario").stream().map(PublicScenarioDriver::object).toList();
    }

    private static List<Map<String,Object>> boundedRows(Object value, int min, int max) {
        var items = rows(value);
        assertTrue(items.size() >= min && items.size() <= max, "Scenario array bounds");
        return items;
    }

    private static Map<String,Object> without(Map<String,Object> value, String... keys) {
        var copy = new LinkedHashMap<>(value);
        for (String key : keys) copy.remove(key);
        return copy;
    }

    private static List<Map<String,Object>> events(List<Map<String,Object>> trace, String event) {
        return trace.stream().filter(row -> row.get("event").equals(event)).toList();
    }

    private static List<Map<String,Object>> targets(List<Map<String,Object>> calls) {
        return calls.stream().map(call -> Map.of("step", call.get("step"), "capability", call.get("capability"))).toList();
    }

    private static int sequence(Map<String,Object> row) { return ((Number)row.get("sequence")).intValue(); }
    private static String string(Object value) { return assertInstanceOf(String.class, value); }
    private static void fields(Map<String,Object> value, String keys) {
        assertEquals(Set.of(keys.split(" ")), value.keySet(), "Scenario object fields");
    }

    private static void matches(Object value, String pattern) {
        assertTrue(string(value).matches(pattern), () -> "Invalid scenario token " + value);
    }

    private static void boundedText(Object value, int min, int max) {
        String text = string(value);
        int length = text.codePointCount(0, text.length());
        assertTrue(length >= min && length <= max, "Scenario string length");
    }
}
