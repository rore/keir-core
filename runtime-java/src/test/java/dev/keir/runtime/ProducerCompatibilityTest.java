package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class ProducerCompatibilityTest {
    private record Case(String model, String current, String old, String scenario, List<String> calls) {}
    private static final List<Case> CASES = List.of(
            new Case("dcm.json", "artifact.json", "event.json", "main/normal", List.of("tasks.get", "tasks.create")),
            new Case("dcm-guard.json", "artifact-guard-input.json", "decision.json", "guard-input/guard-input-allow", List.of()));
    private static byte[] read(String name) throws Exception {
        try (var stream = ProducerCompatibilityTest.class.getResourceAsStream("/keir-3/" + name)) {
            assertNotNull(stream, name); return stream.readAllBytes();
        }
    }
    private static KeirRuntime.BoundModel bind(Case row, Object key, List<String> calls) throws Exception {
        byte[] bytes = read(row.model()); var model = map(parseCanonical(bytes), "");
        String keyType = (String)map(model.get("hostKey"), "").get("type");
        var implementations = new LinkedHashMap<String,KeirRuntime.BoundEffectAdapter>();
        for (var entry : map(model.get("capabilities"), "").entrySet()) {
            String id = entry.getKey(), version = (String)map(entry.getValue(), "").get("version");
            implementations.put(id, new KeirRuntime.BoundEffectAdapter(version, "simulation", keyType, (args, bindings, remaining, context) -> {
                assertSame(key, context.requestKey()); calls.add(id);
                if (id.equals("tasks.get")) return CompletableFuture.completedFuture(Map.of("kind", "SUCCESS", "value",
                        Map.of("id", "task1", "projectId", "project1", "status", "COMPLETED")));
                assertEquals("tasks.create", id);
                return CompletableFuture.completedFuture(Map.of("kind", "SUCCESS", "value", Map.of("id", "task2"), "effect", "CONFIRMED"));
            }));
        }
        var binding = KeirRuntime.bindModel(bytes, implementations); assertTrue(binding.succeeded(), () -> String.valueOf(binding.diagnostic()));
        return binding.boundModel();
    }
    @Test void originalAndCurrentProducersExecuteEventAndDecisionBytes() throws Exception {
        for (Case row : CASES) for (String producer : List.of("0.3.0", "0.3.1")) {
            byte[] artifact = read(producer.equals("0.3.0") ? "compatibility/0.3.0/" + row.old() : row.current());
            assertEquals("keir-compiler-java/" + producer, map(parseCanonical(artifact), "").get("compiler"));
            Object key = new Object(); var calls = new ArrayList<String>(); var bound = bind(row, key, calls);
            var preparation = KeirRuntime.prepareProgram(artifact, bound);
            assertTrue(preparation.succeeded(), () -> String.valueOf(preparation.diagnostic()));
            var scenario = map(parseScenario(read("scenarios/" + row.scenario() + ".json")), "");
            var invocation = new KeirRuntime.BoundEffectInvocation(row.calls().isEmpty() ? Map.of() : Map.of("actorId", "user1"),
                    new KeirRuntime.Deadline(() -> 0, 1000), key, Map.of("id", "event1", "source", "task1"));
            var outcome = KeirRuntime.invokeBoundEffectProgram(preparation.prepared(), map(scenario.get("given"), "").get("input"), invocation)
                    .toCompletableFuture().get(3, TimeUnit.SECONDS);
            assertArrayEquals(canonical(map(scenario.get("expect"), "").get("outcome")), outcome.toJson()); assertEquals(row.calls(), calls);
        }
    }
    @Test void unknownAndCrossFormatProducersFailBeforeDispatch() throws Exception {
        for (Case row : CASES) {
            var calls = new ArrayList<String>(); var bound = bind(row, new Object(), calls);
            for (String version : List.of("0.3.2", "0.3.1-preview", "0.2.0", "0.3.01", "cross-format")) {
                var artifact = map(parseCanonical(read(row.current())), "");
                artifact.put("compiler", "keir-compiler-java/" + (version.equals("cross-format") ? "0.3.1" : version));
                if (version.equals("cross-format")) artifact.put("format", "keir-artifact/2");
                var result = KeirRuntime.prepareProgram(canonical(artifact), bound);
                assertFalse(result.succeeded()); assertNull(result.prepared());
                assertEquals("PIN_MISMATCH", result.diagnostic().code()); assertEquals("/artifact/compiler", result.diagnostic().path());
            }
            assertTrue(calls.isEmpty());
        }
    }
}
