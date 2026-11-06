package dev.keir.runtime;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Constant;
import dev.cel.expr.Expr;
import dev.cel.expr.Reference;
import dev.cel.expr.SourceInfo;
import dev.cel.expr.Type;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CheckedCelTest {
    private static final Type BOOL = Type.newBuilder().setPrimitive(Type.PrimitiveType.BOOL).build();
    private static final Type STRING = Type.newBuilder().setPrimitive(Type.PrimitiveType.STRING).build();
    private static final Map<String,String> OVERLOADS = Map.of("_==_", "equals", "_!=_", "not_equals",
            "!_", "logical_not", "_&&_", "logical_and", "_||_", "logical_or", "_?_:_", "conditional");

    private static Map<String,Object> map(Object value) { return RuntimeData.map(value, "/test"); }
    private static Map<String,Object> fixture(String name) throws IOException {
        try (var input = CheckedCelTest.class.getResourceAsStream(name)) {
            assertNotNull(input, name);
            return map(RuntimeData.parseCanonical(input.readAllBytes()));
        }
    }
    private static Artifact.Plan sample() throws IOException {
        var model=fixture("/keir-2/dcm.json");
        return Artifact.validateProgram(fixture("/keir-2/artifact.json"),model,CapabilityPolicy.catalog(model),1);
    }
    private static CheckedCel.Bundle prepare(Artifact.Plan plan) {
        return CheckedCel.prepare(plan,CheckedCel.bindModel(plan.dcm()));
    }
    private static byte[] bytes(Message message) throws IOException {
        byte[] bytes = new byte[message.getSerializedSize()];
        var output = CodedOutputStream.newInstance(bytes); output.useDeterministicSerialization();
        message.writeTo(output); output.checkNoSpaceLeft(); return bytes;
    }
    private static String encoded(Message message) throws IOException { return Base64.getEncoder().encodeToString(bytes(message)); }
    private static CheckedExpr checked(Map<String,Object> context) throws IOException {
        return CheckedExpr.parseFrom(Base64.getDecoder().decode((String)context.get("checked")));
    }
    private static Artifact.Plan contexts(Artifact.Plan base, Map<String,Map<String,Object>> contexts) {
        return new Artifact.Plan(base.document(), base.dcm(), base.entrypoint(), contexts, base.calls());
    }
    private static Artifact.Plan one(Artifact.Plan base, CheckedExpr checked, Map<String,Object> variables, String type) throws IOException {
        return contexts(base, new LinkedHashMap<>(Map.of("e0", Map.of("checked", encoded(checked), "variables", variables, "resultType", type))));
    }
    private static RuntimeFault denied(Artifact.Plan plan, String code) {
        RuntimeFault fault = assertThrows(RuntimeFault.class, () -> prepare(plan));
        assertEquals(code, fault.code); assertNull(fault.getCause()); return fault;
    }
    private static Map<String,Object> state(boolean completed) {
        return Map.of("input",Map.of("taskId","task1"),"eligibleProject","project1",
                "source",Map.of("id","task1","projectId","project1","status",completed?"COMPLETED":"OPEN"),"eligible",completed);
    }

    @Test void evaluatesTheSameRealCompilerArtifactWithFreshValues() throws Exception {
        var bundle=prepare(sample());
        assertTrue(bundle.totalNodes()>0);
        assertEquals("task1",bundle.evaluate(Map.of("expr","e0"),state(true),"/steps/1"));
        assertEquals(true,bundle.evaluate(Map.of("expr","e1"),state(true),"/steps/2"));
        assertEquals(false,bundle.evaluate(Map.of("expr","e1"),state(false),"/steps/2"));
        assertEquals("CREATED",bundle.evaluate(Map.of("expr","e3"),state(true),"/return"));
        assertEquals("NO_OP",bundle.evaluate(Map.of("expr","e3"),state(false),"/return"));
        assertEquals(false,bundle.evaluate(Map.of("literal",false,"type","Boolean"),Map.of(),"/steps/0"));
        assertEquals("",bundle.evaluate(Map.of("literal","","type","String"),Map.of(),"/return"));
    }

    @Test void projectionPreservesFalseEmptyUnicodeControlsAndReservedFieldNames() throws Exception {
        var base=sample();
        var model=fixture("/keir-2/dcm.json");
        var message=Type.newBuilder().setMessageType("keir.read.TaskCompletedInput").build();
        Map<String,Object> values=Map.of("constructor",false,"input","","toJSON","é😀","toString","\0\t\n\u001f",
                "valueOf","é".repeat(128),"text","e\u0301");
        var fields=new LinkedHashMap<String,Object>();
        values.forEach((key,value)->fields.put(key,value instanceof Boolean?"Boolean":"String"));
        Artifact.definition(model,"TaskCompletedInput").put("fields",fields);
        var plan=new Artifact.Plan(base.document(),model,base.entrypoint(),Map.of(),base.calls());
        var expressions=new LinkedHashMap<String,Map<String,Object>>();
        for(String name:fields.keySet()) {
            var select=Expr.newBuilder().setId(1).setSelectExpr(Expr.Select.newBuilder().setOperand(ident(2,"input")).setField(name)).build();
            String type=(String)fields.get(name);
            var checked=synthetic(select,Map.of(1L,type.equals("Boolean")?BOOL:STRING,2L,message));
            expressions.put(name,one(plan,checked,Map.of("input","TaskCompletedInput"),type).contexts().get("e0"));
        }
        var bundle=prepare(contexts(plan,expressions));
        for(String name:fields.keySet())
            assertEquals(values.get(name),bundle.evaluate(Map.of("expr",name),Map.of("input",values),"/return"),name);
        var fresh=new LinkedHashMap<>(values); fresh.put("toString","next");
        assertEquals("next",bundle.evaluate(Map.of("expr","toString"),Map.of("input",fresh),"/return"));
    }

    @Test void projectionRejectsMissingOrInvalidValuesBeforeProtobufDefaults() throws Exception {
        var bundle = prepare(sample());
        for (Map<String,Object> values : List.<Map<String,Object>>of(Map.of("input", Map.of()), Map.of("input", Map.of("taskId", true)), Map.of("input", Map.of("taskId", "x", "extra", "y")))) {
            RuntimeFault fault = assertThrows(RuntimeFault.class, () -> bundle.evaluate(Map.of("expr", "e0", "type", "TaskId"), Map.of("input",values.get("input"),"eligibleProject","project1"), "/steps/0"));
            assertEquals("EXPRESSION_FAILED", fault.code); assertEquals("/steps/0", fault.path);
        }
        RuntimeFault missing = assertThrows(RuntimeFault.class, () -> bundle.evaluate(Map.of("expr", "absent", "type", "Boolean"), Map.of(), "/return"));
        assertEquals("EXPRESSION_FAILED", missing.code);
    }

    @Test void rejectsMissingExtraAndForgedCheckedMetadata() throws Exception {
        Artifact.Plan base = sample(); var context = base.contexts().get("e1");
        List<Consumer<CheckedExpr.Builder>> changes = List.of(
                b -> b.removeTypeMap(b.getTypeMapMap().keySet().iterator().next()),
                b -> b.putTypeMap(999, BOOL),
                b -> b.removeReferenceMap(b.getReferenceMapMap().keySet().iterator().next()),
                b -> b.putReferenceMap(999, Reference.getDefaultInstance()),
                b -> b.putTypeMap(b.getExpr().getId(), STRING),
                b -> b.putReferenceMap(b.getExpr().getId(), Reference.newBuilder().addOverloadId("logical_or").build()),
                b -> b.putReferenceMap(b.getExpr().getId(), Reference.newBuilder().setName("forged").addOverloadId("equals").build()),
                b -> b.putReferenceMap(b.getExpr().getId(), Reference.newBuilder().addOverloadId("equals").setValue(Constant.newBuilder().setBoolValue(true)).build()));
        for (var change : changes) {
            var modified = checked(context).toBuilder(); change.accept(modified);
            denied(one(base, modified.build(), map(context.get("variables")), "Boolean"), "ARTIFACT_INVALID");
        }
        var identity = checked(base.contexts().get("e0")).toBuilder();
        long identifier = identity.getReferenceMapMap().keySet().iterator().next();
        identity.putReferenceMap(identifier, Reference.newBuilder().setName("other").build());
        denied(one(base, identity.build(), Map.of("input", "TaskCompletedInput"), "TaskId"), "ARTIFACT_INVALID");
    }

    @Test void rejectsInvalidNodeIdentityAndPresenceSelections() throws Exception {
        Artifact.Plan base = sample(); var context = base.contexts().get("e0");
        for (long id : List.of(0L, -1L, 9_007_199_254_740_992L)) {
            var modified = checked(context).toBuilder(); modified.setExpr(modified.getExpr().toBuilder().setId(id));
            denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
        }
        var modified = checked(context).toBuilder();
        modified.setExpr(modified.getExpr().toBuilder().setSelectExpr(modified.getExpr().getSelectExpr().toBuilder().setTestOnly(true)));
        denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
        modified = checked(context).toBuilder();
        modified.setExpr(modified.getExpr().toBuilder().setSelectExpr(modified.getExpr().getSelectExpr().toBuilder()
                .setOperand(modified.getExpr().getSelectExpr().getOperand().toBuilder().setId(modified.getExpr().getId()))));
        denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
    }

    @Test void rejectsSourceMetadataOutsideTheQualifiedSubset() throws Exception {
        Artifact.Plan base = sample(); var context = base.contexts().get("e0");
        List<Consumer<CheckedExpr.Builder>> changes = List.of(
                b -> b.setExprVersion("future"), b -> b.clearSourceInfo(),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().setSyntaxVersion("future")),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().setLocation("untrusted-location")),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().setLocation("keir-read/1")),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().addExtensions(SourceInfo.Extension.getDefaultInstance())),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().putMacroCalls(1, Expr.getDefaultInstance())),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().addLineOffsets(-1)),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().addLineOffsets(514)),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().putPositions(1, -1)),
                b -> b.setSourceInfo(b.getSourceInfo().toBuilder().putPositions(1, 513)));
        for (var change : changes) {
            var modified = checked(context).toBuilder(); change.accept(modified);
            denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
        }
        var boundary = checked(context).toBuilder();
        boundary.setSourceInfo(boundary.getSourceInfo().toBuilder().setLocation("<input>").addLineOffsets(513).putPositions(1, 512));
        prepare(one(base, boundary.build(), map(context.get("variables")), "TaskId"));
    }

    @Test void rejectsUnknownProtobufFieldsAtNestedLevelsAndAlternateEncodings() throws Exception {
        Artifact.Plan base = sample(); var context = base.contexts().get("e0");
        var unknown = UnknownFieldSet.newBuilder().addField(999, UnknownFieldSet.Field.newBuilder().addVarint(1).build()).build();
        var modified = checked(context).toBuilder().setUnknownFields(unknown);
        denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
        modified = checked(context).toBuilder();
        modified.setExpr(modified.getExpr().toBuilder().setUnknownFields(unknown));
        denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
        modified = checked(context).toBuilder(); long typeId = modified.getTypeMapMap().keySet().iterator().next();
        modified.putTypeMap(typeId, modified.getTypeMapOrThrow(typeId).toBuilder().setUnknownFields(unknown).build());
        denied(one(base, modified.build(), map(context.get("variables")), "TaskId"), "ARTIFACT_INVALID");
        byte[] original = bytes(checked(context)); byte[] duplicate = new byte[original.length * 2];
        System.arraycopy(original, 0, duplicate, 0, original.length); System.arraycopy(original, 0, duplicate, original.length, original.length);
        var contexts = new LinkedHashMap<>(base.contexts()); var changed = new HashMap<>(context); changed.put("checked", Base64.getEncoder().encodeToString(duplicate)); contexts.put("e0", changed);
        denied(contexts(base, contexts), "ARTIFACT_INVALID");
    }

    @Test void rejectsNoncanonicalBase64MalformedAndOversizedMessages() throws Exception {
        Artifact.Plan base = sample();
        for (String value : List.of("", "!!!!", "AA", "AB==", "AA==\n", Base64.getEncoder().encodeToString(new byte[]{(byte)0xff}), Base64.getEncoder().encodeToString(new byte[16_385]))) {
            var selected = new LinkedHashMap<>(base.contexts()); var changed = new HashMap<>(selected.get("e0")); changed.put("checked", value); selected.put("e0", changed);
            denied(contexts(base, selected), "ARTIFACT_INVALID");
        }
    }

    @Test void derivedModelMustMatchThePlanModel() throws Exception {
        var plan=sample();
        var other=fixture("/keir-2/dcm-title.json");
        var fault=assertThrows(RuntimeFault.class,()->CheckedCel.prepare(plan,CheckedCel.bindModel(other)));
        assertEquals("/artifact/modelRef",fault.path);
    }

    private static Expr bool(long id) { return Expr.newBuilder().setId(id).setConstExpr(Constant.newBuilder().setBoolValue(true)).build(); }
    private static Expr string(long id, String value) { return Expr.newBuilder().setId(id).setConstExpr(Constant.newBuilder().setStringValue(value)).build(); }
    private static Expr ident(long id, String name) { return Expr.newBuilder().setId(id).setIdentExpr(Expr.Ident.newBuilder().setName(name)).build(); }
    private static Expr call(long id, String function, Expr... args) {
        return Expr.newBuilder().setId(id).setCallExpr(Expr.Call.newBuilder().setFunction(function).addAllArgs(Arrays.asList(args))).build();
    }
    private static CheckedExpr synthetic(Expr expression, Map<Long,Type> types) {
        var checked = CheckedExpr.newBuilder().setExpr(expression).setSourceInfo(SourceInfo.newBuilder().setLocation("<input>"));
        annotate(expression, types, checked); return checked.build();
    }
    private static void annotate(Expr expression, Map<Long,Type> types, CheckedExpr.Builder checked) {
        checked.putTypeMap(expression.getId(), types.getOrDefault(expression.getId(), BOOL));
        if (expression.hasIdentExpr()) checked.putReferenceMap(expression.getId(), Reference.newBuilder().setName(expression.getIdentExpr().getName()).build());
        if (expression.hasSelectExpr()) annotate(expression.getSelectExpr().getOperand(), types, checked);
        if (expression.hasCallExpr()) {
            checked.putReferenceMap(expression.getId(), Reference.newBuilder().addOverloadId(OVERLOADS.getOrDefault(expression.getCallExpr().getFunction(), "forged")).build());
            for (Expr child : expression.getCallExpr().getArgsList()) annotate(child, types, checked);
        }
    }

    @Test void semanticOverlayPreservesNominalsEnumsAndLiteralRefinement() throws Exception {
        Artifact.Plan base = sample();
        var nominal = synthetic(call(1, "_==_", ident(2, "taskId"), ident(3, "userId")), Map.of(2L, STRING, 3L, STRING));
        denied(one(base, nominal, Map.of("taskId", "TaskId", "userId", "UserId"), "Boolean"), "ARTIFACT_INVALID");
        var literalNominal = synthetic(string(1, "task1"), Map.of(1L, STRING));
        denied(one(base, literalNominal, Map.of(), "TaskId"), "ARTIFACT_INVALID");
        for (String literal : List.of("OPEN", "MISSING")) {
            var equality = synthetic(call(1, "_==_", ident(2, "status"), string(3, literal)), Map.of(2L, STRING, 3L, STRING));
            var selected = one(base, equality, Map.of("status", "TaskStatus"), "Boolean");
            if (literal.equals("OPEN")) assertEquals(true, prepare(selected).evaluate(Map.of("expr", "e0"), Map.of("status", "OPEN"), "/return"));
            else denied(selected, "ARTIFACT_INVALID");
        }
        var enumValue = synthetic(ident(1, "status"), Map.of(1L, STRING));
        denied(one(base, enumValue, Map.of("status", "TaskStatus"), "String"), "ARTIFACT_INVALID");
        denied(one(base, synthetic(ident(1, "raw"), Map.of(1L, STRING)), Map.of("raw", "String"), "FollowupStatus"), "ARTIFACT_INVALID");
        var conditional = synthetic(call(1, "_?_:_", bool(2), ident(3, "taskId"), ident(4, "taskId")), Map.of(1L, STRING, 3L, STRING, 4L, STRING));
        assertEquals("same", prepare(one(base, conditional, Map.of("taskId", "TaskId"), "TaskId"))
                .evaluate(Map.of("expr", "e0"), Map.of("taskId", "same"), "/return"));
    }

    @Test void rejectsRecordConditionalBranchesAndForbiddenAstShapes() throws Exception {
        Artifact.Plan base = sample(); Type input = Type.newBuilder().setMessageType("keir.read.TaskCompletedInput").build();
        Expr recordConditional = call(2, "_?_:_", bool(3), ident(4, "input"), ident(5, "input"));
        Expr selection = Expr.newBuilder().setId(1).setSelectExpr(Expr.Select.newBuilder().setOperand(recordConditional).setField("taskId")).build();
        denied(one(base, synthetic(selection, Map.of(1L, STRING, 2L, input, 4L, input, 5L, input)), Map.of("input", "TaskCompletedInput"), "TaskId"), "ARTIFACT_INVALID");
        for (Expr invalid : List.of(Expr.newBuilder().setId(1).setListExpr(Expr.CreateList.getDefaultInstance()).build(),
                Expr.newBuilder().setId(1).setStructExpr(Expr.CreateStruct.getDefaultInstance()).build(),
                Expr.newBuilder().setId(1).setConstExpr(Constant.newBuilder().setInt64Value(1)).build(),
                call(1, "unqualified", bool(2))))
            denied(one(base, synthetic(invalid, Map.of()), Map.of(), "Boolean"), "ARTIFACT_INVALID");
        var receiver = call(1, "!_", bool(2)).toBuilder(); receiver.setCallExpr(receiver.getCallExpr().toBuilder().setTarget(bool(3)));
        denied(one(base, synthetic(receiver.build(), Map.of()), Map.of(), "Boolean"), "ARTIFACT_INVALID");
        denied(one(base, synthetic(call(1, "!_", bool(2), bool(3)), Map.of()), Map.of(), "Boolean"), "ARTIFACT_INVALID");
    }

    private static Expr tree(int leaves, long[] id) {
        long current = ++id[0]; if (leaves == 1) return bool(current);
        return call(current, "_&&_", tree(leaves / 2, id), tree(leaves - leaves / 2, id));
    }
    private static Expr deep(int depth) { Expr result = bool(depth); for (int id = depth - 1; id > 0; id--) result = call(id, "!_", result); return result; }

    @Test void enforcesAstNodeDepthAggregateAndWireRecursionBounds() throws Exception {
        Artifact.Plan base = sample();
        CheckedExpr nodes64 = synthetic(call(64, "!_", tree(32, new long[]{0})), Map.of());
        var selected = one(base, nodes64, Map.of(), "Boolean"); assertEquals(64, prepare(selected).totalNodes());
        assertEquals(16, prepare(one(base, synthetic(deep(16), Map.of()), Map.of(), "Boolean")).totalNodes());
        denied(one(base, synthetic(tree(33, new long[]{0}), Map.of()), Map.of(), "Boolean"), "BUDGET_EXCEEDED");
        denied(one(base, synthetic(deep(17), Map.of()), Map.of(), "Boolean"), "BUDGET_EXCEEDED");
        denied(one(base, synthetic(deep(40), Map.of()), Map.of(), "Boolean"), "BUDGET_EXCEEDED");
        var contexts = new LinkedHashMap<String,Map<String,Object>>();
        for (int i = 0; i < 8; i++) contexts.put("e"+i, selected.contexts().get("e0"));
        assertEquals(512, prepare(contexts(base, contexts)).totalNodes());
        contexts.put("e8", selected.contexts().get("e0")); denied(contexts(base, contexts), "BUDGET_EXCEEDED");
    }

}
