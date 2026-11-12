package dev.keir.runtime;

import com.google.protobuf.CodedOutputStream;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Constant;
import dev.cel.expr.Expr;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static dev.keir.runtime.RuntimeData.*;
import static org.junit.jupiter.api.Assertions.*;

class GuardedResultRuntimeTest {
    private static final Path CORPUS = Path.of("..", "conformance", "keir-3");
    private static final String AT = "2026-01-01T00:00:00.000Z";
    private static byte[] read(String name) throws Exception { return Files.readAllBytes(CORPUS.resolve(name)); }
    private static Map<String,Object> input(boolean guard) {
        return Map.of("createNew", guard, "existingReference", "existing", "label", "label",
                "enabled", true, "state", "EXISTING", "at", AT);
    }
    private static Map<String,Object> value(String reference) {
        return Map.of("reference", reference, "flag", false, "note", "", "enabled", false,
                "state", "CREATED", "at", AT, "labels", List.of("kept"));
    }

    private static final class Host {
        final Object key = new Object();
        final List<String> calls = new ArrayList<>();
        Object response = Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", value("actual"));
        Object lookupResponse = Map.of("kind", "SUCCESS", "value", value("read-actual"));
        long now;
        boolean throwsResponse, expired;
        final String program;
        final KeirRuntime.BoundModel bound;
        Host() throws Exception { this("guarded-result"); }
        Host(String program) throws Exception {
            this.program = program;
            byte[] modelBytes = read(program.equals("guarded-result-read") ? "dcm-guarded-result-read.json" : "dcm-guarded-result.json");
            var model = map(parseCanonical(modelBytes), "");
            var adapters = new LinkedHashMap<String,KeirRuntime.BoundEffectAdapter>();
            for (String id : map(model.get("capabilities"), "").keySet()) adapters.put(id,
                    new KeirRuntime.BoundEffectAdapter("1", "simulation", "sample.RecordKey/1", (args, protectedArgs, budget, context) -> {
                        assertSame(key, context.requestKey());
                        assertEquals(Map.of("actorId", "actor"), protectedArgs);
                        assertEquals(id.equals("records.create") ? Map.of("label", "label")
                                : Map.of("reference", program.equals("guarded-result-read") ? "existing" : "actual"), args);
                        calls.add(id);
                        if (throwsResponse) throw new IllegalStateException("adapter failed");
                        if (expired) now = 1000;
                        return CompletableFuture.completedFuture(id.equals("records.create") ? response : lookupResponse);
                    }));
            var binding = KeirRuntime.bindModel(modelBytes, adapters);
            assertTrue(binding.succeeded(), () -> String.valueOf(binding.diagnostic()));
            bound = binding.boundModel();
        }
        KeirRuntime.Prepared prepare() throws Exception {
            var preparation = KeirRuntime.prepareProgram(read("artifact-" + program + ".json"), bound);
            assertTrue(preparation.succeeded(), () -> String.valueOf(preparation.diagnostic()));
            return preparation.prepared();
        }
        KeirRuntime.Outcome invoke(KeirRuntime.Prepared program, boolean guard) throws Exception {
            return KeirRuntime.invokeBoundEffectProgram(program, input(guard), new KeirRuntime.BoundEffectInvocation(
                    Map.of("actorId", "actor"), new KeirRuntime.Deadline(() -> now, 1000), key,
                    Map.of("id", "event", "source", "existing"))).toCompletableFuture().get(3, TimeUnit.SECONDS);
        }
    }

    @Test void retainsTheActualGuardedResultWithFreshSparseActivations() throws Exception {
        var host = new Host();
        var prepared = host.prepare();
        for (boolean guard : List.of(false, true, false, true)) {
            var outcome = host.invoke(prepared, guard);
            assertEquals("COMPLETED", outcome.classification(), outcome.toMap().toString());
            assertEquals(Map.of("reference", guard ? "actual" : "existing", "message", guard ? "" : "unchanged",
                    "enabled", !guard, "flag", !guard, "state", guard ? "CREATED" : "EXISTING", "at", AT),
                    outcome.toMap().get("result"));
            assertEquals(guard ? List.of(Map.of("step", "created", "capability", "records.create", "state", "CONFIRMED")) : List.of(), outcome.effects());
        }
        assertEquals(List.of("records.create", "records.create"), host.calls);
    }

    @Test @Timeout(5)
    void overlappingGuardedResultsKeepValuesReceiptsAndContextsInvocationLocal() throws Exception {
        record Call(String capability, Map<String,Object> args, Map<String,Object> protectedArgs,
                    long remainingMs, KeirRuntime.BoundEffectContext context) {}
        record Gate(CompletableFuture<Call> entered, CompletableFuture<Object> response) {}
        var gates = List.of(new Gate(new CompletableFuture<>(), new CompletableFuture<>()),
                new Gate(new CompletableFuture<>(), new CompletableFuture<>()));
        var calls = new AtomicInteger();
        var running = new ArrayList<CompletableFuture<KeirRuntime.Outcome>>();
        var names = List.of("slow", "fast", "skipped");
        var inputs = new LinkedHashMap<String,Map<String,Object>>();
        var invocations = new LinkedHashMap<String,KeirRuntime.BoundEffectInvocation>();
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            var fields = new LinkedHashMap<>(input(!name.equals("skipped")));
            fields.put("label", name); fields.put("existingReference", "existing-" + name);
            fields.put("enabled", index != 0); fields.put("at", "2026-01-0" + (index + 1) + "T00:00:00.000Z");
            inputs.put(name, fields);
            long now = index * 10L;
            invocations.put(name, new KeirRuntime.BoundEffectInvocation(Map.of("actorId", "actor-" + name),
                    new KeirRuntime.Deadline(() -> now, 1000 + now), new Object(),
                    Map.of("id", "event-" + name, "source", "source-" + name)));
        }
        var adapters = new LinkedHashMap<String,KeirRuntime.BoundEffectAdapter>();
        for (String capability : List.of("records.create", "records.lookup")) adapters.put(capability,
                new KeirRuntime.BoundEffectAdapter("1", "simulation", "sample.RecordKey/1", (args, protectedArgs, remainingMs, context) -> {
                    int index = calls.getAndIncrement();
                    if (index >= gates.size()) return CompletableFuture.failedFuture(new AssertionError("Unexpected additional dispatch"));
                    var gate = gates.get(index);
                    gate.entered().complete(new Call(capability, args, protectedArgs, remainingMs, context));
                    return gate.response();
                }));
        var bound = KeirRuntime.bindModel(read("dcm-guarded-result.json"), adapters);
        assertTrue(bound.succeeded(), () -> String.valueOf(bound.diagnostic()));
        var prepared = KeirRuntime.prepareProgram(read("artifact-guarded-result.json"), bound.boundModel());
        assertTrue(prepared.succeeded(), () -> String.valueOf(prepared.diagnostic()));
        BiConsumer<Call,String> assertCall = (call, name) -> {
            var expected = invocations.get(name);
            assertEquals("records.create", call.capability());
            assertEquals(Map.of("label", name), call.args());
            assertEquals(expected.bindings(), call.protectedArgs());
            assertSame(expected.requestKey(), call.context().requestKey());
            assertEquals(expected.event(), call.context().event());
            assertEquals(expected.deadline().expiresAtMs(), call.context().deadline().expiresAtMs());
            assertSame(expected.deadline().clock(), call.context().deadline().clock());
            assertEquals(expected.deadline().clock().getAsLong(), call.context().deadline().clock().getAsLong());
            assertTrue(call.remainingMs() > 0 && call.remainingMs() <= 1000);
        };
        var returnedSlow = new LinkedHashMap<>(value("returned-slow"));
        returnedSlow.put("flag", true); returnedSlow.put("enabled", true); returnedSlow.put("note", "slow-note");
        returnedSlow.put("at", "2026-01-04T00:00:00.000Z");
        var returnedFast = new LinkedHashMap<>(value("returned-fast"));
        returnedFast.remove("enabled"); returnedFast.put("note", "fast-note"); returnedFast.put("at", "2026-01-05T00:00:00.000Z");
        var replies = List.of(Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", returnedSlow),
                Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", returnedFast));
        var receipt = List.of(Map.of("step", "created", "capability", "records.create", "state", "CONFIRMED"));
        var expectedSlow = Map.of("reference", "returned-slow", "message", "slow-note", "enabled", true, "flag", true,
                "state", "CREATED", "at", "2026-01-04T00:00:00.000Z");
        var expectedFast = Map.of("reference", "returned-fast", "message", "fast-note", "enabled", false, "flag", false,
                "state", "CREATED", "at", "2026-01-05T00:00:00.000Z");
        var expectedSkipped = Map.of("reference", "existing-skipped", "message", "unchanged", "enabled", true, "flag", true,
                "state", "EXISTING", "at", "2026-01-03T00:00:00.000Z");
        try {
            var slow = KeirRuntime.invokeBoundEffectProgram(prepared.prepared(), inputs.get("slow"), invocations.get("slow")).toCompletableFuture();
            running.add(slow);
            assertCall.accept(gates.get(0).entered().get(3, TimeUnit.SECONDS), "slow");
            var fast = KeirRuntime.invokeBoundEffectProgram(prepared.prepared(), inputs.get("fast"), invocations.get("fast")).toCompletableFuture();
            running.add(fast);
            assertCall.accept(gates.get(1).entered().get(3, TimeUnit.SECONDS), "fast");
            var skippedFuture = KeirRuntime.invokeBoundEffectProgram(prepared.prepared(), inputs.get("skipped"), invocations.get("skipped")).toCompletableFuture();
            running.add(skippedFuture);
            var skipped = skippedFuture.get(3, TimeUnit.SECONDS);
            assertEquals("COMPLETED", skipped.classification(), skipped.toMap().toString());
            assertEquals(expectedSkipped, skipped.toMap().get("result"));
            assertEquals(List.of(), skipped.effects());
            assertEquals(List.of("created"), skipped.trace().stream().filter(row -> row.get("event").equals("STEP_SKIPPED")).map(row -> row.get("step")).toList());
            assertEquals(2, calls.get()); assertFalse(slow.isDone()); assertFalse(fast.isDone());

            gates.get(1).response().complete(replies.get(1));
            var fastOutcome = fast.get(3, TimeUnit.SECONDS);
            assertEquals("COMPLETED", fastOutcome.classification(), fastOutcome.toMap().toString());
            assertEquals(expectedFast, fastOutcome.toMap().get("result")); assertEquals(receipt, fastOutcome.effects());
            assertFalse(slow.isDone());
            gates.get(0).response().complete(replies.get(0));
            var slowOutcome = slow.get(3, TimeUnit.SECONDS);
            assertEquals("COMPLETED", slowOutcome.classification(), slowOutcome.toMap().toString());
            assertEquals(expectedSlow, slowOutcome.toMap().get("result")); assertEquals(receipt, slowOutcome.effects());
            assertEquals(expectedFast, fastOutcome.toMap().get("result")); assertEquals(receipt, fastOutcome.effects());
            assertEquals(expectedSkipped, skipped.toMap().get("result")); assertEquals(List.of(), skipped.effects());
            assertEquals(2, calls.get());
            assertCall.accept(gates.get(0).entered().get(3, TimeUnit.SECONDS), "slow");
            assertCall.accept(gates.get(1).entered().get(3, TimeUnit.SECONDS), "fast");
        } finally {
            for (int index = 0; index < gates.size(); index++) gates.get(index).response().complete(replies.get(index));
            CompletableFuture.allOf(running.toArray(new CompletableFuture<?>[0])).get(3, TimeUnit.SECONDS);
        }
    }

    private static CheckedCel.Bundle bundle(String program) throws Exception {
        var artifact = map(parseCanonical(read("artifact-" + program + ".json")), "");
        var dcm = map(parseCanonical(read("dcm-guarded-result.json")), "");
        var plan = Artifact.validateProgram(artifact, dcm, CapabilityPolicy.catalog(dcm), 1);
        return CheckedCel.prepare(plan, CheckedCel.bindModel(dcm));
    }

    @Test void unusedConditionalDeclarationsRemainSparseButTrueGuardResultsAreRequired() throws Exception {
        var host = new Host("guarded-result-unused");
        var prepared = host.prepare();
        for (boolean guard : List.of(true, false, true, false)) {
            var outcome = host.invoke(prepared, guard);
            assertEquals("COMPLETED", outcome.classification());
            assertEquals("unused", map(outcome.toMap().get("result"), "").get("message"));
        }
        var bundle = bundle("guarded-result-unused");
        assertEquals(AT, bundle.evaluate(Map.of("expr", "e2"), Map.of("input", input(false), "createNew", false), "/test"));
        for (Map<String,Object> state : List.<Map<String,Object>>of(Map.of("input", input(true), "createNew", true),
                Map.of("input", input(false)), Map.of("createNew", false), Map.of("input", input(false), "createNew", "false"))) {
            var error = assertThrows(RuntimeFault.class, () -> bundle.evaluate(Map.of("expr", "e2"), state, "/test"));
            assertEquals("EXPRESSION_FAILED", error.code); assertEquals("/test", error.path);
        }
        assertEquals(AT, bundle.evaluate(Map.of("expr", "e2"),
                Map.of("input", input(false), "createNew", false, "created", new Object()), "/test"));
    }

    @Test void optionalScalarReadsCannotTurnMissingTrueGuardResultsIntoAbsence() throws Exception {
        var host = new Host(); var prepared = host.prepare();
        for (String note : List.of("absent", "", "present")) {
            var returned = new LinkedHashMap<>(value("actual"));
            if (note.equals("absent")) { returned.remove("note"); returned.remove("enabled"); }
            else returned.put("note", note);
            host.response = Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", returned);
            var outcome = host.invoke(prepared, true);
            assertEquals("COMPLETED", outcome.classification());
            assertEquals(note.equals("absent") ? "" : note, map(outcome.toMap().get("result"), "").get("message"));
            assertEquals(false, map(outcome.toMap().get("result"), "").get("enabled"));
        }
        var bundle = bundle("guarded-result");
        var error = assertThrows(RuntimeFault.class, () -> bundle.evaluate(Map.of("expr", "e6"),
                Map.of("input", input(true), "createNew", true, "reference", "actual"), "/test"));
        assertEquals("EXPRESSION_FAILED", error.code);
    }

    @Test void guardedReadsFeedDecisionsWithNoWriteAuthorityOrReceipts() throws Exception {
        var host = new Host("guarded-result-read"); var prepared = host.prepare();
        for (boolean guard : List.of(false, true)) for (boolean denied : List.of(false, true)) {
            var returned = new LinkedHashMap<>(value("read-actual")); returned.put("flag", denied);
            host.lookupResponse = Map.of("kind", "SUCCESS", "value", returned);
            var outcome = host.invoke(prepared, guard);
            assertEquals("COMPLETED", outcome.classification());
            assertEquals(Map.of("decision", guard && denied ? "DENY" : "ALLOW", "message", guard ? "" : "skipped"), outcome.toMap().get("result"));
            assertEquals(List.of(), outcome.effects());
        }
        assertEquals(List.of("records.lookup", "records.lookup"), host.calls);
    }

    @Test void laterRejectionHandlerUsesPriorResultAndPreservesTheConfirmedReceipt() throws Exception {
        var host = new Host("guarded-result-handler"); var prepared = host.prepare();
        host.lookupResponse = Map.of("kind", "BUSINESS_REJECTION", "code", "DENIED");
        var outcome = host.invoke(prepared, true);
        assertEquals("BUSINESS_REJECTION", outcome.classification());
        assertEquals(Map.of("code", "DENIED", "path", "/steps/3"), outcome.toMap().get("error"));
        assertEquals("actual", map(outcome.toMap().get("result"), "").get("reference"));
        assertEquals(List.of(Map.of("step", "created", "capability", "records.create", "state", "CONFIRMED")), outcome.effects());
        assertEquals(List.of("records.create", "records.lookup"), host.calls);
        host.lookupResponse = Map.of("kind", "TRANSIENT_FAILURE");
        outcome = host.invoke(prepared, true);
        assertEquals("FAILED", outcome.classification());
        assertFalse(outcome.toMap().containsKey("result"));
        assertEquals("CONFIRMED", map(outcome.effects().getFirst(), "").get("state"));
    }

    private record Failure(Object response, String classification, String code, String effect, String path) {}
    @Test void guardedFailuresNeverChooseFallbackAndPreserveEffectEvidence() throws Exception {
        var oversized = new LinkedHashMap<>(value("actual")); oversized.put("note", "x".repeat(257));
        var cases = List.of(
                new Failure(Map.of("kind", "BUSINESS_REJECTION", "code", "DENIED", "effect", "NONE"), "BUSINESS_REJECTION", "DENIED", "NONE", "/steps/1"),
                new Failure(Map.of("kind", "TRANSIENT_FAILURE", "effect", "NONE"), "FAILED", "ADAPTER_TRANSIENT", "NONE", "/steps/1"),
                new Failure(Map.of("kind", "UNKNOWN_COMMIT", "effect", "UNKNOWN"), "UNCERTAIN", "UNKNOWN_COMMIT", "UNKNOWN", "/steps/1"),
                new Failure(Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", Map.of()), "FAILED", "ADAPTER_INVALID", "CONFIRMED", "/steps/1"),
                new Failure(Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", oversized), "LIMIT_EXCEEDED", "BUDGET_EXCEEDED", "CONFIRMED", "/steps/1/note"));
        for (var row : cases) {
            var host = new Host(); host.response = row.response();
            var outcome = host.invoke(host.prepare(), true);
            assertEquals(row.classification(), outcome.classification(), outcome.toMap().toString());
            assertEquals(Map.of("code", row.code(), "path", row.path()), outcome.toMap().get("error"));
            assertFalse(outcome.toMap().containsKey("result"));
            assertEquals(List.of(Map.of("step", "created", "capability", "records.create", "state", row.effect())), outcome.effects());
            assertEquals(List.of("records.create"), host.calls);
        }
        for (boolean lost : List.of(false, true)) {
            var host = new Host(); host.throwsResponse = lost; host.expired = !lost;
            var outcome = host.invoke(host.prepare(), true);
            assertEquals(lost ? "UNCERTAIN" : "LIMIT_EXCEEDED", outcome.classification());
            assertEquals(lost ? "ADAPTER_EXCEPTION" : "BUDGET_EXCEEDED", map(outcome.toMap().get("error"), "").get("code"));
            assertFalse(outcome.toMap().containsKey("result"));
            assertEquals(lost ? "UNKNOWN" : "CONFIRMED", map(outcome.effects().getFirst(), "").get("state"));
        }
    }

    @Test void preparationReconstructsProvenanceAndRejectsAValidUndominatedCheckedTree() throws Exception {
        var host = new Host();
        List<Consumer<Map<String,Object>>> changes = List.of(
                artifact -> map(map(map(artifact.get("expressions"), "").get("e2"), "").get("variables"), "").remove("created"),
                artifact -> map(map(map(artifact.get("expressions"), "").get("e2"), "").get("variables"), "").put("future", "CreatedRecord"),
                artifact -> map(map(map(artifact.get("expressions"), "").get("e2"), "").get("variables"), "").put("created", "RecordInput"),
                artifact -> map(list(artifact.get("steps"), "").get(1), "").put("when", "reference"),
                artifact -> map(map(artifact.get("expressions"), "").get("e2"), "").put("guardedResults", Map.of("created", "createNew")));
        for (var change : changes) {
            var artifact = map(parseCanonical(read("artifact-guarded-result.json")), ""); change.accept(artifact);
            denied(KeirRuntime.prepareProgram(canonical(artifact), host.bound));
        }
        var artifact = map(parseCanonical(read("artifact-guarded-result.json")), "");
        var expression = map(map(artifact.get("expressions"), "").get("e2"), "");
        var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked")));
        var call = checked.getExpr().getCallExpr();
        var condition = call.getArgs(0);
        var forged = checked.toBuilder().removeReferenceMap(condition.getId())
                .setExpr(checked.getExpr().toBuilder().setCallExpr(call.toBuilder().setArgs(0,
                        Expr.newBuilder().setId(condition.getId()).setConstExpr(Constant.newBuilder().setBoolValue(true))))).build();
        byte[] bytes = new byte[forged.getSerializedSize()];
        var output = CodedOutputStream.newInstance(bytes); output.useDeterministicSerialization(); forged.writeTo(output); output.checkNoSpaceLeft();
        expression.put("checked", Base64.getEncoder().encodeToString(bytes));
        denied(KeirRuntime.prepareProgram(canonical(artifact), host.bound));
        for (String producer : List.of("0.3.0", "0.3.1")) {
            artifact = map(parseCanonical(read("artifact-guarded-result.json")), ""); artifact.put("compiler", "keir-compiler-java/" + producer);
            denied(KeirRuntime.prepareProgram(canonical(artifact), host.bound));
        }
        var legacyModel = map(parseCanonical(read("dcm-guarded-result.json")), "");
        legacyModel.put("format", "keir-dcm/2");
        var adapters = new LinkedHashMap<String,KeirRuntime.BoundEffectAdapter>();
        for (String id : map(legacyModel.get("capabilities"), "").keySet()) adapters.put(id,
                new KeirRuntime.BoundEffectAdapter("1", "simulation", "sample.RecordKey/1",
                        (args, bindings, remaining, context) -> { throw new AssertionError("Unexpected dispatch"); }));
        var legacyBinding = KeirRuntime.bindModel(canonical(legacyModel), adapters);
        assertTrue(legacyBinding.succeeded(), () -> String.valueOf(legacyBinding.diagnostic()));
        artifact = map(parseCanonical(read("artifact-guarded-result.json")), "");
        artifact.put("format", "keir-artifact/2"); artifact.put("compiler", "keir-compiler-java/0.2.0");
        artifact.put("modelRef", Artifact.modelRef(legacyModel));
        var legacyDenied = KeirRuntime.prepareProgram(canonical(artifact), legacyBinding.boundModel());
        denied(legacyDenied);
        assertEquals("/artifact/steps/2/value", legacyDenied.diagnostic().path());
        assertTrue(host.calls.isEmpty());
    }

    private static void denied(KeirRuntime.Preparation preparation) {
        assertFalse(preparation.succeeded());
        assertEquals("ARTIFACT_INVALID", preparation.diagnostic().code());
    }
}
