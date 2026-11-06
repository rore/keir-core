package dev.keir.runtime;

import com.google.protobuf.CodedOutputStream;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

import static dev.keir.runtime.BoundEffectRuntimeTest.*;
import static dev.keir.runtime.RuntimeData.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowRuntimeTest {
    @Test void rawInputValuesFailBeforeAnyDispatch() throws Exception {
        var calls = new AtomicInteger();
        var prepared = KeirRuntime.prepareProgram(file("artifact-inventory.json"), inventoryModel(calls)).prepared();
        assertNotNull(prepared);
        var input = inventoryInput();
        for (String invalid : List.of("2026-02-30T00:00:00.000Z", "2026-03-07T24:00:00.000Z",
                "2016-12-31T23:59:60.000Z", "2026-03-07T00:00:00Z", "2026-03-07T00:00:00.0000Z",
                "2026-03-07T00:00:00.000+00:00", "0000-01-01T00:00:00.000Z")) {
            input.put("occurredAt", invalid);
            error(await(KeirRuntime.invokeBoundEffectProgram(prepared, input, context())), "INPUT_INVALID", "/input");
        }
        input.put("occurredAt", "2026-03-07T12:34:56.789Z");
        input.put("enabled", null);
        error(await(KeirRuntime.invokeBoundEffectProgram(prepared, input, context())), "INPUT_INVALID", "/input");
        input.remove("enabled");
        input.put("labels", Collections.nCopies(17, "fragile"));
        error(await(KeirRuntime.invokeBoundEffectProgram(prepared, input, context())), "BUDGET_EXCEEDED", "/input/labels");
        input.put("labels", Collections.nCopies(16, "\0".repeat(256)));
        var oversized = await(KeirRuntime.invokeBoundEffectProgram(prepared, input, context()));
        assertEquals("LIMIT_EXCEEDED", oversized.classification());
        error(oversized, "BUDGET_EXCEEDED", "/input");
        assertTrue(oversized.effects().isEmpty());
        assertEquals(0, calls.get());
    }

    @Test void checkedMembershipMetadataIsVerifiedBeforePlanning() throws Exception {
        var calls = new AtomicInteger();
        var bound = inventoryModel(calls);
        var artifact = document(file("artifact-inventory.json"));
        var context = map(map(artifact.get("expressions"), "").get("e2"), "");
        var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)context.get("checked")));
        var membership = checked.getReferenceMapMap().entrySet().stream()
                .filter(ref -> ref.getValue().getOverloadIdList().equals(List.of("in_list")))
                .findFirst().orElseThrow();
        var forged = checked.toBuilder().putReferenceMap(membership.getKey(),
                membership.getValue().toBuilder().clearOverloadId().addOverloadId("in_map").build()).build();
        context.put("checked", encode(forged));
        denied(KeirRuntime.prepareProgram(canonical(artifact), bound), "ARTIFACT_INVALID", "/artifact/expressions");
        assertEquals(0, calls.get());
    }

    @Test void preparationRejectsProofForAnotherOptionalFieldAndTheOldDescriptorRecipe() throws Exception {
        var calls = new AtomicInteger();
        var bound = inventoryModel(calls);
        var artifact = document(file("artifact-inventory.json"));
        var context = map(map(artifact.get("expressions"), "").get("e2"), "");
        var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)context.get("checked")));
        Expr forged = replace(checked.getExpr(), expr -> {
            if (!expr.hasSelectExpr() || !expr.getSelectExpr().getTestOnly()
                    || !expr.getSelectExpr().getField().equals("enabled")) return expr;
            return expr.toBuilder().setSelectExpr(expr.getSelectExpr().toBuilder().setField("note")).build();
        });
        assertNotEquals(checked.getExpr(), forged);
        context.put("checked", encode(checked.toBuilder().setExpr(forged).build()));
        denied(KeirRuntime.prepareProgram(canonical(artifact), bound), "ARTIFACT_INVALID", "/artifact/expressions");

        var wrongRecipe = document(file("artifact-inventory.json"));
        map(wrongRecipe.get("modelRef"), "").put("descriptorRecipe", "keir-record-descriptors/1");
        denied(KeirRuntime.prepareProgram(canonical(wrongRecipe), bound), "ARTIFACT_INVALID", "/artifact/modelRef");
        assertEquals(0, calls.get());
    }

    @Test void rawTimestampLiteralCannotBypassCheckedConstructor() throws Exception {
        var calls = new AtomicInteger();
        var bound = inventoryModel(calls);
        var artifact = document(file("artifact-inventory.json"));
        var step = map(list(artifact.get("steps"), "").get(2), "");
        step.put("value", Map.of("type", "Timestamp", "literal", "2026-03-07T12:34:56.789Z"));
        map(artifact.get("expressions"), "").remove("e3");
        var result = KeirRuntime.prepareProgram(canonical(artifact), bound);
        assertFalse(result.succeeded());
        assertEquals("ARTIFACT_INVALID", result.diagnostic().code());
        assertEquals(0, calls.get());
    }

    private static KeirRuntime.BoundModel inventoryModel(AtomicInteger calls) throws Exception {
        var stub = new KeirRuntime.BoundEffectAdapter("1", "simulation", "sample.InventoryRequest/1",
                (args, protectedArgs, remaining, context) -> {
                    calls.incrementAndGet();
                    throw new AssertionError("Invalid program or input dispatched");
                });
        var binding = KeirRuntime.bindModel(file("dcm-inventory.json"),
                Map.of("inventory.findOpen", stub, "inventory.adjust", stub));
        assertTrue(binding.succeeded(), () -> String.valueOf(binding.diagnostic()));
        return binding.boundModel();
    }

    private static String encode(CheckedExpr expression) throws Exception {
        byte[] bytes = new byte[expression.getSerializedSize()];
        var output = CodedOutputStream.newInstance(bytes);
        output.useDeterministicSerialization();
        expression.writeTo(output);
        output.checkNoSpaceLeft();
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static Expr replace(Expr expr, UnaryOperator<Expr> operation) {
        if (!expr.hasCallExpr()) return operation.apply(expr);
        var call = expr.getCallExpr().toBuilder().clearArgs();
        for (Expr arg : expr.getCallExpr().getArgsList()) call.addArgs(replace(arg, operation));
        return operation.apply(expr.toBuilder().setCallExpr(call).build());
    }

    private static Map<String,Object> inventoryInput() throws Exception {
        var scenario = PublicScenarioDriver.scenario("inventory/inventory-adjusted");
        return new LinkedHashMap<>(map(map(scenario.get("given"), "").get("input"), ""));
    }

    private static KeirRuntime.BoundEffectInvocation context() {
        return new KeirRuntime.BoundEffectInvocation(Map.of("actorId", "user1"),
                new KeirRuntime.Deadline(() -> 0, 10000), new Object(), Map.of("id", "event1", "source", "item1"));
    }
}
