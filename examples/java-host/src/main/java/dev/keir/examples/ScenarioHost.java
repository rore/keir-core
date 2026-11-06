package dev.keir.examples;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static java.nio.charset.StandardCharsets.UTF_8;
import static dev.keir.examples.ScenarioData.*;

/** Offline verification of the packaged Java host against the frozen compiler scenarios. */
public final class ScenarioHost {
    static final Set<String> GROUPS = Set.of("main", "zero", "after-write-read", "no-call",
            "handler", "title", "title-max", "title-composed");
    private static final Set<String> FIXTURES = Set.of("normal", "read-throws", "lost-response", "response-expired");

    record Selection(String group, byte[] model, byte[] artifact) {}

    private final Path root;
    private final String python;
    int dispatchCount;

    ScenarioHost(Path root, String python) {
        this.root = root;
        this.python = python;
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 2 || !GROUPS.contains(args[0])) {
            err.println("Usage: ScenarioHost <fixed-scenario-group> <python-executable>");
            return 2;
        }
        try {
            var host = new ScenarioHost(Path.of("").toAbsolutePath(), args[1]);
            out.writeBytes(EffectJson.encode(host.verifyGroup(args[0])));
            out.println();
            return 0;
        } catch (Exception failure) {
            err.println("The selected Java scenario group could not be verified.");
            return 1;
        }
    }

    Path corpus() {
        return root.resolve("conformance/keir-2");
    }

    Selection selection(String group) throws IOException {
        check(GROUPS.contains(group), "Unknown trusted scenario group");
        String model = group.startsWith("title") ? "dcm-title" : "dcm";
        String artifact = group.equals("main") ? "artifact" : "artifact-" + group;
        return new Selection(group, pinned(model, 65_536), pinned(artifact, 1_048_576));
    }

    List<Map<String, Object>> verifyGroup(String group) throws Exception {
        check(GROUPS.contains(group), "Unknown trusted scenario group");
        List<Path> files;
        try (var entries = Files.list(corpus().resolve("scenarios").resolve(group))) {
            files = entries.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .limit(129).sorted().toList();
        }
        var bytes = new ArrayList<byte[]>();
        int total = 0;
        for (Path file : files) {
            byte[] raw = read(file, 65_536);
            total += raw.length;
            GenericCapacity.scenarios(total, files.size(), raw.length, 0, 0);
            bytes.add(raw);
        }
        return verify(group, bytes, null);
    }

    List<Map<String, Object>> verify(String group, List<byte[]> documents, Selection testSelection) throws Exception {
        dispatchCount = 0;
        check(!documents.isEmpty(), "Empty scenario batch");
        var rows = new ArrayList<Map<String, Object>>();
        var ids = new HashSet<Object>();
        for (byte[] bytes : documents) {
            var row = decode(bytes);
            check(ids.add(row.get("id")), "Duplicate scenario ID");
            rows.add(row);
        }

        Selection selected = testSelection == null ? selection(group) : testSelection;
        var model = object(EffectJson.parse(selected.model()));
        var artifact = object(EffectJson.parse(selected.artifact()));
        check("keir-dcm/2".equals(model.get("format")) && "keir-artifact/2".equals(artifact.get("format")),
                "Incompatible scenario contract");
        try (var simulation = new BoundScenarioSimulation(selected.model(),
                Map.of(selected.group(), selected.artifact()), model)) {
            try {
                for (var row : rows) validateScenario(model, artifact, row);
                var results = new ArrayList<Map<String, Object>>();
                for (var row : rows) results.add(simulation.run(selected.group(), row));
                return results;
            } finally {
                dispatchCount = simulation.dispatchCount;
            }
        }
    }

    Map<String, Object> decode(byte[] bytes) throws Exception {
        Object decoded = EffectJson.parseScenario(bytes);
        checkSchema(bytes);
        var row = object(decoded);
        byteFields(row);
        return row;
    }

    private void checkSchema(byte[] bytes) throws Exception {
        Path input = Files.createTempFile("keir-scenario-", ".json");
        try {
            Path output = Files.createTempFile("keir-schema-", ".json");
            try {
                Files.write(input, bytes);
                Process process = new ProcessBuilder(python, root.resolve("scripts/check-keir-contracts.py").toString(),
                        "--revision", "2", "scenario", input.toString()).redirectOutput(output.toFile())
                        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                try {
                    check(process.waitFor(30, TimeUnit.SECONDS), "Scenario schema checker timed out");
                    var report = object(EffectJson.parse(read(output, 1_048_576)));
                    if (process.exitValue() == 1 && Boolean.FALSE.equals(report.get("valid"))) {
                        throw new IllegalArgumentException("Scenario structure rejected by shared schema");
                    }
                    check(process.exitValue() == 0 && Boolean.TRUE.equals(report.get("valid")),
                            "Scenario schema checker failed");
                } finally {
                    if (process.isAlive()) process.destroyForcibly();
                }
            } finally {
                Files.delete(output);
            }
        } finally {
            Files.delete(input);
        }
    }

    private static void byteFields(Map<String, Object> row) {
        var expect = object(row.get("expect"));
        var outcome = object(expect.get("outcome"));
        var consumption = object(expect.get("consumption"));
        for (Object collection : List.of(row.get("stubs"), expect.get("calls"), outcome.get("trace"),
                outcome.get("effects"), consumption.getOrDefault("effects", List.of()))) {
            for (Object item : array(collection)) {
                var fields = object(item);
                if (fields.containsKey("capability")) byteBound(fields.get("capability"), 256);
                if (fields.containsKey("version")) byteBound(fields.get("version"), 256);
            }
        }
        for (var payload : List.of(outcome, consumption)) {
            if (!payload.containsKey("error")) continue;
            var error = object(payload.get("error"));
            byteBound(error.get("path"), 1024);
            if (error.containsKey("cause")) byteBound(object(error.get("cause")).get("path"), 1024);
        }
    }

    private static void byteBound(Object value, int max) {
        check(((String) value).getBytes(UTF_8).length <= max, "Scenario UTF-8 field bound");
    }

    private static void validateScenario(Map<String, Object> model, Map<String, Object> artifact,
                                         Map<String, Object> row) {
        var given = object(row.get("given"));
        check(FIXTURES.contains(given.get("fixture")), "Unknown trusted fixture");
        var entry = object(object(model.get("entrypoints")).get(artifact.get("entrypoint")));
        var expect = object(row.get("expect"));
        var outcome = object(expect.get("outcome"));
        var consumption = object(expect.get("consumption"));
        var stubs = array(row.get("stubs")).stream().map(ScenarioData::object).toList();
        var calls = array(expect.get("calls")).stream().map(ScenarioData::object).toList();
        var types = object(model.get("types"));
        var catalog = object(model.get("capabilities"));
        typedValue(types, entry.get("input"), given.get("input"));
        for (var payload : List.of(outcome, consumption)) {
            if (payload.containsKey("result")) typedValue(types, entry.get("output"), payload.get("result"));
        }
        for (var stub : stubs) typedCall(types, catalog, stub);
        for (var call : calls) typedCall(types, catalog, call);
        for (int i = 0; i < stubs.size(); i++) {
            var call = new LinkedHashMap<>(stubs.get(i));
            call.remove("response");
            check(i < calls.size() && call.equals(calls.get(i)), "Stub and expected call mismatch");
        }
        check(stubs.size() == calls.size(), "Stub and expected call count mismatch");
        check("EVENT_REPORT".equals(object(entry.get("consumer")).get("kind"))
                && "REPORTED".equals(consumption.get("status")), "Expected EVENT_REPORT");
        same(EffectConsumer.consume(outcome), consumption, "Invalid expected outcome or EVENT_REPORT");
        validateTrace(catalog, artifact, calls, outcome);
    }

    private static void typedCall(Map<String, Object> types, Map<String, Object> catalog, Map<String, Object> call) {
        check(catalog.containsKey(call.get("capability")), "Unknown DCM capability");
        var capability = object(catalog.get(call.get("capability")));
        check(Objects.equals(capability.get("version"), call.get("version")), "Wrong DCM capability version");
        var args = object(call.get("args"));
        var protectedArgs = object(call.get("protectedArgs"));
        var agentFields = new HashSet<String>();
        var hostFields = new HashSet<String>();
        object(capability.get("parameters")).forEach((name, raw) -> {
            var param = object(raw);
            boolean host = "host".equals(param.get("binding"));
            (host ? hostFields : agentFields).add(name);
            typedValue(types, param.get("type"), (host ? protectedArgs : args).get(name));
        });
        check(agentFields.equals(args.keySet()) && hostFields.equals(protectedArgs.keySet()), "Wrong DCM arguments");
        if (!call.containsKey("response")) return;
        var response = object(call.get("response"));
        boolean write = "WRITE".equals(capability.get("effect"));
        check(write == response.containsKey("effect"), "Wrong DCM effect envelope");
        switch ((String) response.get("kind")) {
            case "SUCCESS" -> typedValue(types, capability.get("result"), response.get("value"));
            case "BUSINESS_REJECTION" -> check(array(capability.get("errors")).contains(response.get("code")),
                    "Undeclared DCM business code");
            default -> { }
        }
    }

    private static void typedValue(Map<String, Object> types, Object name, Object value) {
        if ("Boolean".equals(name)) {
            check(value instanceof Boolean, "Wrong DCM value");
        } else if ("String".equals(name)) {
            check(value instanceof String text && text.getBytes(UTF_8).length <= 256, "Wrong DCM string value");
        } else {
            var type = object(types.get(name));
            switch ((String) type.get("kind")) {
                case "nominal" -> typedValue(types, type.get("base"), value);
                case "enum" -> check(array(type.get("values")).contains(value), "Wrong DCM enum value");
                case "record" -> {
                    var record = object(value);
                    var fields = object(type.get("fields"));
                    check(fields.keySet().equals(record.keySet()), "Wrong DCM record fields");
                    fields.forEach((field, fieldType) -> typedValue(types, fieldType, record.get(field)));
                }
                default -> throw new IllegalArgumentException("Unsupported scenario DCM type");
            }
        }
        check(EffectJson.encode(value).length <= 8192, "DCM value byte bound");
    }

    private static void validateTrace(Map<String, Object> catalog, Map<String, Object> artifact,
                                      List<Map<String, Object>> calls, Map<String, Object> outcome) {
        var steps = array(artifact.get("steps")).stream().map(ScenarioData::object).toList();
        for (var call : calls) {
            check(steps.stream().anyMatch(step -> "CALL".equals(step.get("op"))
                    && call.get("step").equals(step.get("id")) && call.get("capability").equals(step.get("capability"))),
                    "Expected call has no matching artifact step");
        }
        var trace = array(outcome.get("trace")).stream().map(ScenarioData::object).toList();
        var dispatches = events(trace, "CALL_DISPATCHED");
        for (var guard : events(trace, "GUARD_EVALUATED")) {
            if (Boolean.FALSE.equals(guard.get("taken"))) {
                check(dispatches.stream().noneMatch(call -> guard.get("step").equals(call.get("step"))),
                        "Expected dispatch after a false guard");
            }
        }
        var writes = calls.stream().filter(call -> "WRITE".equals(object(catalog.get(call.get("capability"))).get("effect"))).toList();
        var intents = events(trace, "EFFECT_INTENT");
        var receipts = events(trace, "EFFECT_RECORDED");
        check(targets(calls).equals(targets(dispatches)), "Expected dispatch order mismatch");
        check(targets(writes).equals(targets(intents)) && targets(writes).equals(targets(receipts)),
                "Expected WRITE trace mismatch");
        for (var intent : intents) {
            var dispatch = dispatches.stream().filter(event -> event.get("step").equals(intent.get("step"))).findFirst().orElseThrow();
            check(sequence(intent) < sequence(dispatch) && sequence(dispatch) < sequence(receipts.getFirst()),
                    "Expected WRITE intent, dispatch and receipt order mismatch");
        }
        var effects = receipts.stream().map(receipt ->
                Map.of("step", receipt.get("step"), "capability", receipt.get("capability"), "state", receipt.get("state"))).toList();
        check(effects.equals(outcome.get("effects")), "Expected WRITE effects mismatch");
    }

    private static List<Map<String, Object>> events(List<Map<String, Object>> trace, String event) {
        return trace.stream().filter(item -> event.equals(item.get("event"))).toList();
    }

    private static List<List<Object>> targets(List<Map<String, Object>> calls) {
        return calls.stream().map(call -> List.of(call.get("step"), call.get("capability"))).toList();
    }

    private static long sequence(Map<String, Object> event) {
        return ((Number) event.get("sequence")).longValue();
    }

    private byte[] pinned(String name, int max) throws IOException {
        byte[] bytes = read(corpus().resolve(name + ".json"), max);
        String pin = new String(read(corpus().resolve(name + ".sha256"), 73), UTF_8).replaceFirst("\\r?\\n\\z", "");
        check(sha256(bytes).equals(pin), "Pinned scenario selection mismatch");
        return bytes;
    }

    private static byte[] read(Path path, int max) throws IOException {
        try (var stream = Files.newInputStream(path)) {
            byte[] bytes = stream.readNBytes(max + 1);
            GenericCapacity.capacity(bytes.length <= max);
            return bytes;
        }
    }
}
