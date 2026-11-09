package dev.keir.examples;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecisionConsumerTest {
    static Map<String,Object> completed(String decision) {
        return new LinkedHashMap<>(Map.of("classification", "COMPLETED", "result", Map.of("decision", decision),
                "effects", List.of(), "trace", List.of(
                        Map.of("sequence", 0, "event", "INVOCATION_START"),
                        Map.of("sequence", 1, "event", "STEP_START", "step", "restricted"),
                        Map.of("sequence", 2, "event", "VALUE_BOUND", "step", "restricted"),
                        Map.of("sequence", 3, "event", "RETURNED"),
                        Map.of("sequence", 4, "event", "TERMINATED"))));
    }

    @Test void allowsAndDeniesWithTheCompleteBoundedPlainTextResult() {
        for (String decision : List.of("ALLOW", "DENY")) {
            var outcome = completed(decision);
            var result = new LinkedHashMap<>(Map.<String,Object>of("decision", decision, "message", "<script>é</script>"));
            outcome.put("result", result);
            var consumption = DecisionConsumer.consumeDecision(outcome);
            assertEquals(Map.of("status", decision.equals("ALLOW") ? "ALLOWED" : "DENIED", "result", result), consumption);
            result.clear();
            assertEquals(decision, ScenarioData.object(consumption.get("result")).get("decision"));
        }
        var outcome = completed("ALLOW");
        outcome.put("result", Map.of("decision", "ALLOW", "message", "é".repeat(128)));
        assertEquals("ALLOWED", DecisionConsumer.consumeDecision(outcome).get("status"));
        outcome.put("trace", trace(List.of(
                Map.of("event", "STEP_START", "step", "value"),
                Map.of("event", "VALUE_BOUND", "step", "value"),
                Map.of("event", "STEP_START", "step", "skipped"),
                Map.of("event", "GUARD_EVALUATED", "step", "skipped", "taken", false),
                Map.of("event", "STEP_SKIPPED", "step", "skipped"))));
        assertEquals("ALLOWED", DecisionConsumer.consumeDecision(outcome).get("status"));
    }

    @Test void blocksEveryFailureAndMalformedClosedResultWithoutLeakingResult() {
        for (String kind : List.of("FAILED", "LIMIT_EXCEEDED", "UNCERTAIN", "BUSINESS_REJECTION", "OTHER")) {
            var outcome = completed("ALLOW"); outcome.put("classification", kind); blocked(outcome);
            outcome.put("error", Map.of("code", "DENIED", "path", "/steps/0")); blocked(outcome);
        }
        for (Object result : List.of(Map.of(), Map.of("decision", "UNKNOWN"), Map.of("decision", true),
                Map.of("decision", "ALLOW", "extra", false), Map.of("decision", "ALLOW", "message", true),
                Map.of("decision", "ALLOW", "message", "é".repeat(129)),
                Map.of("decision", "ALLOW", "message", "\ud800"))) {
            var outcome = completed("ALLOW"); outcome.put("result", result); blocked(outcome);
        }
        var outcome = completed("ALLOW"); outcome.remove("result"); blocked(outcome);
        outcome = completed("ALLOW"); outcome.put("error", Map.of("code", "ERROR", "path", "")); blocked(outcome);
        outcome = completed("ALLOW"); outcome.put("extra", true); blocked(outcome);
        outcome = completed("ALLOW"); outcome.put("effects", List.of(Map.of("state", "CONFIRMED"))); blocked(outcome);
        blocked(null); blocked(List.of());
    }

    @Test void validatesReadTraceAndRejectsForgedOrInconsistentTrace() {
        var outcome = completed("ALLOW");
        var events = new ArrayList<Map<String,Object>>(List.of(
                Map.of("event", "STEP_START", "step", "read"),
                Map.of("event", "CALL_DISPATCHED", "step", "read", "capability", "facts.get"),
                Map.of("event", "CALL_SUCCEEDED", "step", "read", "capability", "facts.get")));
        outcome.put("trace", trace(events));
        assertEquals("ALLOWED", DecisionConsumer.consumeDecision(outcome).get("status"));
        for (var forged : List.of(
                List.<Map<String,Object>>of(),
                List.of(Map.<String,Object>of("event", "CALL_SUCCEEDED", "step", "read", "capability", "facts.get")),
                List.of(events.get(0), events.get(1)),
                List.of(events.get(0), events.get(1), Map.<String,Object>of("event", "CALL_SUCCEEDED", "step", "other", "capability", "facts.get")),
                List.of(events.get(0), Map.<String,Object>of("event", "EFFECT_INTENT", "step", "read", "capability", "facts.get")),
                List.of(Map.<String,Object>of("event", "RETURNED")),
                List.of(events.get(0), Map.<String,Object>of("event", "GUARD_EVALUATED", "step", "read", "taken", true),
                        Map.<String,Object>of("event", "VALUE_BOUND", "step", "read")))) {
            outcome.put("trace", trace(forged)); blocked(outcome);
        }
        outcome.put("trace", List.of(Map.of("sequence", 0, "event", "INVOCATION_START"),
                Map.of("sequence", 1, "event", "TERMINATED"))); blocked(outcome);
        outcome.put("trace", List.of(Map.of("sequence", 0, "event", "INVOCATION_START"),
                Map.of("sequence", 1, "event", "RETURNED"), Map.of("sequence", 2, "event", "TERMINATED", "code", "ERROR"))); blocked(outcome);
        events.clear();
        for (int i = 0; i < 9; i++) {
            events.add(Map.of("event", "STEP_START", "step", "s" + i));
            events.add(Map.of("event", "VALUE_BOUND", "step", "s" + i));
        }
        outcome.put("trace", trace(events)); blocked(outcome);
        events.clear();
        for (int i = 0; i < 5; i++) {
            events.add(Map.of("event", "STEP_START", "step", "s" + i));
            events.add(Map.of("event", "CALL_DISPATCHED", "step", "s" + i, "capability", "facts.get"));
            events.add(Map.of("event", "CALL_SUCCEEDED", "step", "s" + i, "capability", "facts.get"));
        }
        outcome.put("trace", trace(events)); blocked(outcome);
    }

    @Test void validatesTraceIdentifierGrammarAndExactLengthBoundaries() {
        for (String step : List.of("A", "step9", "A".repeat(64), "bad step", "step.name", "_step", "1step", "éstep", "A".repeat(65))) {
            var outcome = completed("ALLOW");
            outcome.put("trace", trace(List.of(
                    Map.of("event", "STEP_START", "step", step),
                    Map.of("event", "VALUE_BOUND", "step", step))));
            assertEquals(Set.of("A", "step9", "A".repeat(64)).contains(step) ? "ALLOWED" : "BLOCKED",
                    DecisionConsumer.consumeDecision(outcome).get("status"), step);
        }
        for (String capability : List.of("A", "facts.get", "A".repeat(100), "bad cap", "_cap", ".cap", "1cap", "bad_cap", "écap", "A".repeat(101))) {
            var outcome = completed("ALLOW");
            outcome.put("trace", trace(List.of(
                    Map.of("event", "STEP_START", "step", "read"),
                    Map.of("event", "CALL_DISPATCHED", "step", "read", "capability", capability),
                    Map.of("event", "CALL_SUCCEEDED", "step", "read", "capability", capability))));
            assertEquals(Set.of("A", "facts.get", "A".repeat(100)).contains(capability) ? "ALLOWED" : "BLOCKED",
                    DecisionConsumer.consumeDecision(outcome).get("status"), capability);
        }
    }

    private static List<Map<String,Object>> trace(List<Map<String,Object>> middle) {
        var events = new ArrayList<Map<String,Object>>();
        events.add(Map.of("sequence", 0, "event", "INVOCATION_START"));
        for (var event : middle) { var copy = new LinkedHashMap<>(event); copy.put("sequence", events.size()); events.add(copy); }
        events.add(Map.of("sequence", events.size(), "event", "RETURNED"));
        events.add(Map.of("sequence", events.size(), "event", "TERMINATED"));
        return events;
    }

    private static void blocked(Object value) { assertEquals(Map.of("status", "BLOCKED"), DecisionConsumer.consumeDecision(value)); }
}
