package dev.keir.examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.keir.examples.ScenarioData.*;
import static org.junit.jupiter.api.Assertions.*;

class BoundScenarioSimulationTest {
    private final Path corpus = Files.isDirectory(Path.of("conformance/keir-3"))
            ? Path.of("conformance/keir-3") : Path.of("../../conformance/keir-3");

    @Test void selectsEachArtifactsConsumerFromOneMixedCatalog() throws Exception {
        byte[] dcm = Files.readAllBytes(corpus.resolve("dcm-mixed.json"));
        var programs = Map.of("event", Files.readAllBytes(corpus.resolve("artifact-mixed-event.json")),
                "guard", Files.readAllBytes(corpus.resolve("artifact-mixed-guard.json")));
        try (var host = new BoundScenarioSimulation(dcm, programs, object(EffectJson.parse(dcm)))) {
            var event = eventScenario();
            for (String decision : List.of("allow", "deny")) {
                var reported = host.run("event", event);
                assertEquals("REPORTED", object(reported.get("consumption")).get("status"));
                assertEquals(List.of(Map.of("step", "written", "capability", "native.write", "state", "CONFIRMED")),
                        object(reported.get("outcome")).get("effects"));
                var guard = object(EffectJson.parseScenario(Files.readAllBytes(corpus.resolve("scenarios/guard/guard-" + decision + ".json"))));
                var consumed = host.run("guard", guard);
                assertEquals(decision.equals("allow") ? "ALLOWED" : "DENIED", object(consumed.get("consumption")).get("status"));
                assertEquals(List.of(), object(consumed.get("outcome")).get("effects"));
            }
            assertEquals(4, host.dispatchCount);
        }
    }

    private static Map<String,Object> eventScenario() {
        var call = Map.<String,Object>of("step", "written", "capability", "native.write", "version", "1", "args", Map.of(), "protectedArgs", Map.of());
        var stub = new LinkedHashMap<>(call);
        stub.put("response", Map.of("kind", "SUCCESS", "value", Map.of("restricted", false), "effect", "CONFIRMED"));
        var trace = new ArrayList<Map<String,Object>>();
        for (var event : List.of(
                Map.<String,Object>of("event", "INVOCATION_START"),
                Map.<String,Object>of("event", "STEP_START", "step", "restricted"),
                Map.<String,Object>of("event", "VALUE_BOUND", "step", "restricted"),
                Map.<String,Object>of("event", "STEP_START", "step", "written"),
                Map.<String,Object>of("event", "EFFECT_INTENT", "step", "written", "capability", "native.write"),
                Map.<String,Object>of("event", "CALL_DISPATCHED", "step", "written", "capability", "native.write"),
                Map.<String,Object>of("event", "EFFECT_RECORDED", "step", "written", "capability", "native.write", "state", "CONFIRMED"),
                Map.<String,Object>of("event", "CALL_SUCCEEDED", "step", "written", "capability", "native.write"),
                Map.<String,Object>of("event", "RETURNED"), Map.<String,Object>of("event", "TERMINATED"))) {
            var row = new LinkedHashMap<>(event); row.put("sequence", trace.size()); trace.add(row);
        }
        var result = Map.of("status", "CREATED");
        var effects = List.of(Map.of("step", "written", "capability", "native.write", "state", "CONFIRMED"));
        var outcome = Map.of("classification", "COMPLETED", "result", result, "effects", effects, "trace", trace);
        var consumption = Map.of("status", "REPORTED", "classification", "COMPLETED", "result", result, "effects", effects);
        return Map.of("id", "mixed-event", "given", Map.of("input", Map.of("restricted", false), "fixture", "normal"),
                "stubs", List.of(stub), "expect", Map.of("outcome", outcome, "calls", List.of(call), "consumption", consumption));
    }
}
