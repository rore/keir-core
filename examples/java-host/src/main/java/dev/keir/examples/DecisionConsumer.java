package dev.keir.examples;

import dev.keir.runtime.KeirRuntime;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pure host consumer for this example's DecisionResult, including optional plain-text message. */
final class DecisionConsumer {
    private DecisionConsumer() {}

    static Map<String,Object> consumeDecision(Object value) {
        try {
            if (!(value instanceof Map<?,?> outcome) ||
                    !outcome.keySet().equals(Set.of("classification", "result", "effects", "trace")) ||
                    !"COMPLETED".equals(outcome.get("classification")) ||
                    !(outcome.get("effects") instanceof List<?> effects) || !effects.isEmpty() ||
                    !(outcome.get("result") instanceof Map<?,?> result) ||
                    !result.keySet().contains("decision") || !Set.of("decision", "message").containsAll(result.keySet()) ||
                    !(result.get("decision") instanceof String decision) || !Set.of("ALLOW", "DENY").contains(decision)) return blocked();
            if (result.containsKey("message")) {
                if (!(result.get("message") instanceof String message)) return blocked();
                EffectJson.unicode(message);
                if (message.getBytes(StandardCharsets.UTF_8).length > 256) return blocked();
            }
            if (!EffectConsumer.trace(outcome.get("trace")) || !completedTrace((List<?>)outcome.get("trace"))) return blocked();
            return Map.of("status", decision.equals("ALLOW") ? "ALLOWED" : "DENIED", "result", EffectJson.copy(result));
        } catch (RuntimeException invalid) { return blocked(); }
    }

    private static Map<String,Object> blocked() { return Map.of("status", "BLOCKED"); }

    static Map<String,Object> consumeQuote(Object value) {
        try {
            if (!(value instanceof Map<?,?> raw) || !(raw.get("result") instanceof Map<?,?> result)
                    || !result.keySet().equals(Set.of("decision","message","amount"))
                    || !(result.get("amount") instanceof String amount)
                    || !KeirRuntime.normalizeDecimal(amount).equals(amount)) return blocked();
            var snapshot=new LinkedHashMap<>(ScenarioData.object(value));
            snapshot.put("result",Map.of("decision",result.get("decision"),"message",result.get("message")));
            var consumed=consumeDecision(snapshot);
            return consumed.get("status").equals("BLOCKED") ? consumed
                    : Map.of("status",consumed.get("status"),"result",EffectJson.copy(result));
        } catch (RuntimeException invalid) { return blocked(); }
    }

    private static boolean completedTrace(List<?> trace) {
        String step = null, capability = null;
        boolean guarded = false, skipped = false, returned = false;
        int calls = 0;
        Set<String> steps = new HashSet<>();
        for (int i = 1; i < trace.size() - 1; i++) {
            Map<?,?> event = (Map<?,?>)trace.get(i);
            String name = (String)event.get("event");
            if (event.containsKey("step") && !((String)event.get("step")).matches("[A-Za-z][A-Za-z0-9]{0,63}")) return false;
            if (event.containsKey("capability") && !((String)event.get("capability")).matches("[A-Za-z][A-Za-z0-9.]{0,99}")) return false;
            if (returned) return false;
            if (name.equals("STEP_START")) {
                if (step != null || !steps.add((String)event.get("step")) || steps.size() > 8) return false;
                step = (String)event.get("step"); guarded = false; skipped = false;
            } else if (name.equals("RETURNED")) {
                if (step != null || i != trace.size() - 2) return false;
                returned = true;
            } else {
                if (step == null || !step.equals(event.get("step"))) return false;
                switch (name) {
                    case "GUARD_EVALUATED" -> {
                        if (guarded || capability != null) return false;
                        guarded = true; skipped = Boolean.FALSE.equals(event.get("taken"));
                    }
                    case "STEP_SKIPPED" -> {
                        if (!skipped || capability != null) return false;
                        step = null;
                    }
                    case "VALUE_BOUND" -> {
                        if (guarded || capability != null) return false;
                        step = null;
                    }
                    case "CALL_DISPATCHED" -> {
                        if (skipped || capability != null || ++calls > 4) return false;
                        capability = (String)event.get("capability");
                    }
                    case "CALL_SUCCEEDED" -> {
                        if (capability == null || !capability.equals(event.get("capability"))) return false;
                        capability = null; step = null;
                    }
                    default -> { return false; }
                }
            }
        }
        return returned && !steps.isEmpty() && !((Map<?,?>)trace.getLast()).containsKey("code");
    }
}
