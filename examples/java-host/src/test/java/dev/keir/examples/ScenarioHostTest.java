package dev.keir.examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static dev.keir.examples.ScenarioData.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "KEIR_SCENARIO_PYTHON", matches = ".+")
class ScenarioHostTest {
    private final Path root = Files.isRegularFile(Path.of("scripts/check-keir-contracts.py"))
            ? Path.of("").toAbsolutePath() : Path.of("../..").toAbsolutePath().normalize();
    private final ScenarioHost host = new ScenarioHost(root, System.getenv("KEIR_SCENARIO_PYTHON"));

    @Test void allFrozenGroupsUseThePackagedHost() throws Exception {
        int count = 0;
        for (String group : ScenarioHost.GROUPS) count += host.verifyGroup(group).size();
        assertEquals(8, ScenarioHost.GROUPS.size());
        assertEquals(20, count);
    }

    @Test void currentCorpusUsesBothEventReportsAndDecisionConsumers() throws Exception {
        var current = new ScenarioHost(root, System.getenv("KEIR_SCENARIO_PYTHON"), root.resolve("conformance/keir-3"));
        int events = 0, decisions = 0;
        for (String group : ScenarioHost.GROUPS) events += current.verifyGroup(group).size();
        for (String group : ScenarioHost.GUARD_GROUPS) {
            var rows = current.verifyGroup(group);
            decisions += rows.size();
            for (var row : rows) {
                var nativeHost = new DecisionContinuationTest.Host();
                nativeHost.operate(row.get("outcome"), () -> {}, false);
                same(nativeHost.consumption, row.get("consumption"), "Native gate consumed a different decision");
                assertEquals("ALLOWED".equals(object(row.get("consumption")).get("status")) ? 1 : 0,
                        nativeHost.nativeAttempts, (String)row.get("id"));
            }
        }
        assertEquals(20, events);
        assertEquals(11, decisions);
    }

    @Test void teachingVectorsReachTheirFirstFailureWithNoDispatch() throws Exception {
        var manifest = object(EffectJson.parse(Files.readAllBytes(host.corpus().resolve("scenario-vectors.json"))));
        var vectors = array(manifest.get("vectors")).stream().filter(item -> {
            var scenario = (String) object(item).get("scenario");
            return scenario == null || ScenarioHost.GROUPS.contains(scenario.split("/", 2)[0]);
        }).toList();
        assertEquals(45, vectors.size());
        for (Object item : vectors) {
            var vector = object(item);
            String id = (String) vector.get("id");
            String scenario = (String) vector.get("scenario");
            String group = scenario == null ? null : scenario.split("/", 2)[0];
            byte[] raw = vectorBytes(vector);
            String expected = (String) vector.get("expect");
            switch (expected) {
                case "accept" -> assertEquals(1, host.verify(group, List.of(raw), null).size(), id);
                case "accept-byte-fields" -> assertDoesNotThrow(() -> host.decode(raw), id);
                default -> {
                    var selected = expected.equals("compatibility") ? vectorSelection(group, vector) : null;
                    var failure = assertThrows(IllegalArgumentException.class,
                            () -> host.verify(group, List.of(raw), selected), id);
                    assertEquals(firstFailure(id, expected), failure.getMessage(), id);
                    assertEquals(0, host.dispatchCount, id);
                }
            }
        }
    }

    @Test void currentScenarioHostRetainsOriginalProducerAndRejectsUnknownProducer() throws Exception {
        var current = new ScenarioHost(root, System.getenv("KEIR_SCENARIO_PYTHON"), root.resolve("conformance/keir-3"));
        for (String group : List.of("main", "guard-input")) {
            var selected = current.selection(group);
            String original = group.equals("main") ? "event.json" : "decision.json";
            byte[] oldArtifact = Files.readAllBytes(current.corpus().resolve("compatibility/0.3.0/" + original));
            Path scenario = current.corpus().resolve("scenarios/" + (group.equals("main") ? "main/normal" : "guard-input/guard-input-allow") + ".json");
            var documents = List.of(Files.readAllBytes(scenario));
            same(current.verify(group, documents, selected), current.verify(group, documents,
                    new ScenarioHost.Selection(group, selected.model(), oldArtifact)), "Producer changed scenario behavior");
            var unknown = object(EffectJson.parse(selected.artifact()));
            unknown.put("compiler", "keir-compiler-java/0.3.2");
            var failure = assertThrows(IllegalArgumentException.class, () -> current.verify(group, documents,
                    new ScenarioHost.Selection(group, selected.model(), EffectJson.encode(unknown))));
            assertEquals("Incompatible scenario contract", failure.getMessage());
            assertEquals(0, current.dispatchCount);
        }
    }

    @Test void rejectsScenarioRevisionOutsideTheTrustedCorpusBeforeDispatch() throws Exception {
        var current = new ScenarioHost(root, System.getenv("KEIR_SCENARIO_PYTHON"), root.resolve("conformance/keir-3"));
        for (var selected : List.of(host, current)) {
            var document = object(EffectJson.parseScenario(Files.readAllBytes(selected.corpus().resolve("scenarios/main/normal.json"))));
            document.put("format", selected == host ? "keir-scenario/3" : "keir-scenario/2");
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> selected.verify("main", List.of(EffectJson.encode(document)), null));
            assertEquals("Scenario structure rejected by shared schema", failure.getMessage());
            assertEquals(0, selected.dispatchCount);
        }
    }

    @Test void rejectsInvalidBatchBeforeDispatchAndChecksActualAdapterArguments() throws Exception {
        byte[] normal = scenario("main/normal");
        byte[] invalid = replace(normal, "\"fixture\": \"normal\"", "\"fixture\": \"normal\", \"program\": \"main\"");
        assertEquals("Scenario structure rejected by shared schema", assertThrows(IllegalArgumentException.class,
                () -> host.verify("main", List.of(normal, invalid), null)).getMessage());
        assertEquals(0, host.dispatchCount);
        assertEquals("Duplicate scenario ID", assertThrows(IllegalArgumentException.class,
                () -> host.verify("main", List.of(normal, normal), null)).getMessage());
        assertEquals(0, host.dispatchCount);

        byte[] falseGuard = replace(normal, "\"taken\": true", "\"taken\": false");
        assertEquals("Expected dispatch after a false guard", assertThrows(IllegalArgumentException.class,
                () -> host.verify("main", List.of(falseGuard), null)).getMessage());
        assertEquals(0, host.dispatchCount);

        for (String field : List.of("actorId", "projectId")) {
            var changed = object(EffectJson.parseScenario(normal));
            for (String section : List.of("stubs", "calls")) {
                Object rows = section.equals("stubs") ? changed.get(section) : object(changed.get("expect")).get(section);
                var write = object(array(rows).getLast());
                object(write.get(field.equals("actorId") ? "protectedArgs" : "args")).put(field, "wrong");
            }
            assertEquals("Simulation violations", assertThrows(IllegalArgumentException.class,
                    () -> host.verify("main", List.of(EffectJson.encode(changed)), null)).getMessage());
            assertEquals(2, host.dispatchCount);
        }

        for (String mutation : List.of("swap", "remove", "append")) {
            var changed = object(EffectJson.parseScenario(normal));
            var stubs = array(changed.get("stubs"));
            switch (mutation) {
                case "swap" -> Collections.swap(stubs, 0, 1);
                case "remove" -> stubs.removeLast();
                case "append" -> stubs.add(stubs.getLast());
            }
            assertThrows(IllegalArgumentException.class,
                    () -> host.verify("main", List.of(EffectJson.encode(changed)), null));
            assertEquals(0, host.dispatchCount);
        }
    }

    private static String firstFailure(String id, String phase) {
        return switch (phase) {
            case "lexical" -> "Invalid JSON data.";
            case "structural" -> "Scenario structure rejected by shared schema";
            case "byte-fields" -> "Scenario UTF-8 field bound";
            case "compatibility" -> id.equals("wrong-model-pair")
                    ? "Bound program preparation failed" : "Incompatible scenario contract";
            case "fixture" -> "Unknown trusted fixture";
            case "typing" -> switch (id) {
                case "capability-256-bytes" -> "Unknown DCM capability";
                case "version-256-bytes" -> "Wrong DCM capability version";
                case "wrong-dcm-input-type" -> "Wrong DCM string value";
                default -> "Wrong DCM enum value";
            };
            case "semantics" -> "Invalid expected outcome or EVENT_REPORT";
            default -> throw new AssertionError("Unknown vector phase " + phase);
        };
    }

    private byte[] scenario(String name) throws Exception {
        return Files.readAllBytes(host.corpus().resolve("scenarios").resolve(name + ".json"));
    }

    private byte[] vectorBytes(Map<String, Object> vector) throws Exception {
        if (vector.get("utf8") instanceof String text) return text.getBytes(UTF_8);
        if (vector.get("base64") instanceof String encoded) return Base64.getDecoder().decode(encoded);
        byte[] bytes = scenario((String) vector.get("scenario"));
        if (vector.containsKey("replace")) {
            var values = array(vector.get("replace"));
            String after = (String) values.getLast();
            if (vector.containsKey("expand")) {
                var expansion = array(vector.get("expand"));
                after = after.replace("@", ((String) expansion.getFirst()).repeat(((Number) expansion.getLast()).intValue()));
            }
            bytes = replace(bytes, (String) values.getFirst(), after);
        }
        if (vector.get("padToBytes") instanceof Number length) {
            int size = bytes.length;
            bytes = Arrays.copyOf(bytes, length.intValue());
            Arrays.fill(bytes, size, bytes.length, (byte) ' ');
        }
        return bytes;
    }

    private ScenarioHost.Selection vectorSelection(String group, Map<String, Object> vector) throws Exception {
        var selected = host.selection(group);
        byte[] model = selected.model();
        byte[] artifact = selected.artifact();
        if (vector.get("model") instanceof String name) {
            assertEquals("dcm-title", name);
            model = host.selection("title").model();
        }
        if (vector.get("modelFormat") instanceof String format) model = replace(model, "keir-dcm/2", format);
        if (vector.get("artifactFormat") instanceof String format) artifact = replace(artifact, "keir-artifact/2", format);
        return new ScenarioHost.Selection(group, model, artifact);
    }

    private static byte[] replace(byte[] bytes, String before, String after) {
        String text = new String(bytes, UTF_8);
        int offset = text.indexOf(before);
        assertTrue(offset >= 0, () -> "Missing fixture mutation " + before);
        return (text.substring(0, offset) + after + text.substring(offset + before.length())).getBytes(UTF_8);
    }
}
