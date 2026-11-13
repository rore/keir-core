package dev.keir.examples;

import dev.keir.runtime.KeirRuntime;
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
    static final Set<String> GUARD_GROUPS = Set.of("guard", "guard-input", "guard-handler", "guard-reversed");
    static final Set<String> NUMERIC_GROUPS = Set.of("stock", "quote");
    private static final Set<String> FIXTURES = Set.of("normal", "read-throws", "lost-response", "response-expired", "read-expired", "read-malformed");

    record Selection(String group, byte[] model, byte[] artifact) {}

    private final Path root;
    private final String python;
    private final Path corpus;
    int dispatchCount;

    ScenarioHost(Path root, String python) {
        this(root, python, root.resolve("conformance/keir-2"));
    }

    ScenarioHost(Path root, String python, Path corpus) {
        this.root = root;
        this.python = python;
        this.corpus = corpus;
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if ((args.length != 2 && args.length != 3) || !(GROUPS.contains(args[0]) || GUARD_GROUPS.contains(args[0]) || NUMERIC_GROUPS.contains(args[0]))) {
            err.println("Usage: ScenarioHost <fixed-scenario-group> <python-executable> [corpus-directory]");
            return 2;
        }
        try {
            Path root = Path.of("").toAbsolutePath();
            var host = args.length == 3 ? new ScenarioHost(root, args[1], root.resolve(args[2])) : new ScenarioHost(root, args[1]);
            out.writeBytes(EffectJson.encode(host.verifyGroup(args[0])));
            out.println();
            return 0;
        } catch (Exception failure) {
            err.println("The selected Java scenario group could not be verified.");
            return 1;
        }
    }

    Path corpus() {
        return corpus;
    }

    Selection selection(String group) throws IOException {
        check(GROUPS.contains(group) || GUARD_GROUPS.contains(group) || NUMERIC_GROUPS.contains(group), "Unknown trusted scenario group");
        if (NUMERIC_GROUPS.contains(group)) {
            Path example=root.resolve("examples/numeric").resolve(group);
            return new Selection(group,pinned(example,"dcm",65_536),pinned(example,"artifact",1_048_576));
        }
        String model = group.equals("guard-reversed") ? "dcm-guard-reversed" : GUARD_GROUPS.contains(group) ? "dcm-guard" : group.startsWith("title") ? "dcm-title" : "dcm";
        String artifact = group.equals("main") ? "artifact" : "artifact-" + group;
        return new Selection(group, pinned(model, 65_536), pinned(artifact, 1_048_576));
    }

    List<Map<String, Object>> verifyGroup(String group) throws Exception {
        check(GROUPS.contains(group) || GUARD_GROUPS.contains(group) || NUMERIC_GROUPS.contains(group), "Unknown trusted scenario group");
        List<Path> files;
        Path directory=NUMERIC_GROUPS.contains(group) ? root.resolve("examples/numeric").resolve(group).resolve("scenarios")
                : corpus().resolve("scenarios").resolve(group);
        try (var entries = Files.list(directory)) {
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
        Selection selected = testSelection;
        if (selected == null && group != null && NUMERIC_GROUPS.contains(group)) selected = selection(group);
        boolean modern = selected != null && "keir-artifact/4".equals(object(EffectJson.parse(selected.artifact())).get("format"));
        var rows = new ArrayList<Map<String, Object>>();
        var ids = new HashSet<Object>();
        for (byte[] bytes : documents) {
            var row = modern ? decode(bytes, "4") : decode(bytes);
            check(ids.add(row.get("id")), "Duplicate scenario ID");
            rows.add(row);
        }

        if (selected == null) selected = selection(group);
        var model = object(EffectJson.parse(selected.model()));
        var artifact = object(EffectJson.parse(selected.artifact()));
        String revision = scenarioRevision(model, artifact);
        check(rows.stream().allMatch(row -> ("keir-scenario/" + revision).equals(row.get("format"))),
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

    private static String scenarioRevision(Map<String,Object> model,Map<String,Object> artifact) {
        boolean old = "keir-dcm/2".equals(model.get("format")) && "keir-artifact/2".equals(artifact.get("format")) &&
                "keir-compiler-java/0.2.0".equals(artifact.get("compiler"));
        boolean current = "keir-dcm/3".equals(model.get("format")) && "keir-artifact/3".equals(artifact.get("format")) &&
                Set.of("keir-compiler-java/0.3.0","keir-compiler-java/0.3.1","keir-compiler-java/0.4.0").contains(artifact.get("compiler"));
        boolean modern = Set.of("keir-dcm/3","keir-dcm/4").contains(model.get("format"))
                && "keir-artifact/4".equals(artifact.get("format")) && "keir-compiler-java/0.5.0".equals(artifact.get("compiler"));
        check(old || current || modern,
                "Incompatible scenario contract");
        return modern ? "4" : old ? "2" : "3";
    }

    Map<String, Object> decode(byte[] bytes) throws Exception {
        String revision = switch ((String)object(EffectJson.parse(pinned("dcm", 65_536))).get("format")) {
            case "keir-dcm/2" -> "2";
            case "keir-dcm/3" -> "3";
            default -> throw new IllegalArgumentException("Incompatible scenario contract");
        };
        return decode(bytes, revision);
    }

    private Map<String,Object> decode(byte[] bytes,String revision) throws Exception {
        Object decoded = EffectJson.parseScenario(bytes);
        checkSchema(bytes, revision);
        var row = object(decoded);
        byteFields(row);
        return row;
    }

    private void checkSchema(byte[] bytes, String revision) throws Exception {
        Path input = Files.createTempFile("keir-scenario-", ".json");
        try {
            Path output = Files.createTempFile("keir-schema-", ".json");
            try {
                Files.write(input, bytes);
                Process process = new ProcessBuilder(python, root.resolve("scripts/check-keir-contracts.py").toString(),
                        "--revision", revision, "scenario", input.toString()).redirectOutput(output.toFile())
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
        var catalog = object(model.get("capabilities"));
        typedValue(model, entry.get("input"), given.get("input"));
        for (var payload : List.of(outcome, consumption)) {
            if (payload.containsKey("result")) typedValue(model, entry.get("output"), payload.get("result"));
        }
        for (var stub : stubs) typedCall(model, catalog, stub);
        for (var call : calls) typedCall(model, catalog, call);
        for (int i = 0; i < stubs.size(); i++) {
            var call = new LinkedHashMap<>(stubs.get(i));
            call.remove("response");
            check(i < calls.size() && call.equals(calls.get(i)), "Stub and expected call mismatch");
        }
        check(stubs.size() == calls.size(), "Stub and expected call count mismatch");
        String consumer = (String)object(entry.get("consumer")).get("kind");
        if (consumer.equals("DECISION")) {
            boolean quote="keir-dcm/4".equals(model.get("format")) && "example.quotes".equals(object(model.get("domain")).get("id"))
                    && "QuoteDecision".equals(entry.get("output"));
            var consumed=quote ? DecisionConsumer.consumeQuote(outcome) : DecisionConsumer.consumeDecision(outcome);
            check(!"COMPLETED".equals(outcome.get("classification")) || Set.of("ALLOWED","DENIED").contains(consumed.get("status")),
                    "Invalid completed decision outcome");
            same(consumed, consumption, "Invalid expected outcome or DECISION");
        } else {
            check("EVENT_REPORT".equals(consumer) && "REPORTED".equals(consumption.get("status")), "Expected EVENT_REPORT");
            same(EffectConsumer.consume(outcome), consumption, "Invalid expected outcome or EVENT_REPORT");
        }
        validateTrace(catalog, artifact, calls, outcome);
    }

    private static void typedCall(Map<String, Object> model, Map<String, Object> catalog, Map<String, Object> call) {
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
            typedValue(model, param.get("type"), (host ? protectedArgs : args).get(name));
        });
        check(agentFields.equals(args.keySet()) && hostFields.equals(protectedArgs.keySet()), "Wrong DCM arguments");
        if (!call.containsKey("response")) return;
        var response = object(call.get("response"));
        boolean write = "WRITE".equals(capability.get("effect"));
        check(write == response.containsKey("effect"), "Wrong DCM effect envelope");
        switch ((String) response.get("kind")) {
            case "SUCCESS" -> typedValue(model, capability.get("result"), response.get("value"));
            case "BUSINESS_REJECTION" -> check(array(capability.get("errors")).contains(response.get("code")),
                    "Undeclared DCM business code");
            default -> { }
        }
    }

    private static void typedValue(Map<String, Object> model, Object name, Object value) {
        var types = object(model.get("types"));
        if ("Boolean".equals(name)) {
            check(value instanceof Boolean, "Wrong DCM value");
        } else if ("String".equals(name)) {
            check(value instanceof String text && text.getBytes(UTF_8).length <= 256, "Wrong DCM string value");
        } else if ("keir-dcm/4".equals(model.get("format")) && Set.of("Integer","Decimal").contains(name)) {
            check(value instanceof String, "Wrong DCM numeric value");
            check(value.equals(name.equals("Integer") ? KeirRuntime.normalizeInteger((String)value)
                    : KeirRuntime.normalizeDecimal((String)value)), "Noncanonical DCM numeric value");
        } else {
            var type = object(types.get(name));
            switch ((String) type.get("kind")) {
                case "nominal" -> typedValue(model, type.get("base"), value);
                case "enum" -> check(array(type.get("values")).contains(value), "Wrong DCM enum value");
                case "record" -> {
                    var record = object(value);
                    var fields = object(type.get("fields"));
                    check(fields.keySet().containsAll(record.keySet()), "Wrong DCM record fields");
                    fields.forEach((field, fieldType) -> {
                        if (fieldType instanceof String) typedValue(model, fieldType, record.get(field));
                        else {
                            var definition = object(fieldType);
                            if (!record.containsKey(field) && Boolean.TRUE.equals(definition.get("optional"))) return;
                            typedValue(model, definition.get("type"), record.get(field));
                        }
                    });
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
        return pinned(corpus(),name,max);
    }

    private static byte[] pinned(Path directory,String name,int max) throws IOException {
        byte[] bytes = read(directory.resolve(name + ".json"), max);
        String pin = new String(read(directory.resolve(name + ".sha256"), 73), UTF_8).replaceFirst("\\r?\\n\\z", "");
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
