package dev.keir.runtime;

import com.google.protobuf.Message;
import dev.cel.expr.CheckedExpr;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class NumericIntegrationTest {
    private static List<Object> cases() throws Exception {
        Path path=Path.of("..","conformance","keir-4","engine-matrix.json");
        if (!Files.exists(path)) path=Path.of("..","compiler-java","target","numeric-integration.json");
        return list(map(parseJson(Files.readAllBytes(path),1_048_576),"").get("cases"),"");
    }
    private static Map<String,Object> row(String id) throws Exception {
        return cases().stream().map(value -> map(value,"")).filter(value -> value.get("id").equals(id)).findFirst().orElseThrow();
    }
    private static KeirRuntime.Preparation prepare(Map<String,Object> row) {
        var bound=KeirRuntime.bindModel(canonical(row.get("dcm")),Map.of());
        assertTrue(bound.succeeded(),String.valueOf(bound.diagnostic()));
        return KeirRuntime.prepareProgram(canonical(row.get("artifact")),bound.boundModel());
    }
    private static Map<String,Object> expected(Map<String,Object> row) {
        List<Map<String,Object>> trace=new ArrayList<>();
        trace.add(Map.of("sequence",0L,"event","INVOCATION_START"));
        var ids=new ArrayList<>(List.of("a","b","i","j","flag","text"));
        if (Set.of("bind","expression-only-size").contains(row.get("id"))) ids.add("d");
        for (String id:ids) {
            trace.add(Map.of("sequence",(long)trace.size(),"event","STEP_START","step",id));
            trace.add(Map.of("sequence",(long)trace.size(),"event","VALUE_BOUND","step",id));
        }
        Object value=row.get("expected");
        if (Set.of("ERROR","TIME_ERROR").contains(value)) {
            String code=value.equals("TIME_ERROR") ? "BUDGET_EXCEEDED" : "EXPRESSION_FAILED";
            trace.add(Map.of("sequence",(long)trace.size(),"event","TERMINATED","code",code));
            return Map.of("classification",value.equals("TIME_ERROR") ? "LIMIT_EXCEEDED" : "FAILED",
                    "error",Map.of("code",code,"path","/return"),"effects",List.of(),"trace",trace);
        }
        trace.add(Map.of("sequence",(long)trace.size(),"event","RETURNED"));
        trace.add(Map.of("sequence",(long)trace.size(),"event","TERMINATED"));
        return Map.of("classification","COMPLETED","result",Map.of("value",value),"effects",List.of(),"trace",trace);
    }
    @Test void sameCompilerArtifactsMatchCompleteIndependentOutcomes() throws Exception {
        var cases=cases(); assertEquals(97,cases.size());
        for (Object raw:cases) {
            var row=map(raw,""); var prepared=prepare(row);
            assertTrue(prepared.succeeded(),row.get("id")+": "+prepared.diagnostic());
            var invocation=new KeirRuntime.BoundEffectInvocation(Map.of(),new KeirRuntime.Deadline(() -> 0L,10_000L),
                    "original-key",Map.of("id","event1","source","owner1"));
            var outcome=KeirRuntime.invokeBoundEffectProgram(prepared.prepared(),row.get("input"),invocation).toCompletableFuture().get();
            assertArrayEquals(canonical(expected(row)),outcome.toJson(),(String)row.get("id"));
        }
    }
    @Test void forgedNumericOverloadAndRelabeledOldProducerFailBeforeInvocation() throws Exception {
        var row=row("decimal-add");
        var artifact=map(immutable(row.get("artifact")),"");
        var changed=new LinkedHashMap<>(artifact); changed.put("compiler","keir-compiler-java/0.4.0");
        var candidate=new HashMap<>(row); candidate.put("artifact",changed);
        assertEquals("PIN_MISMATCH",prepare(candidate).diagnostic().code());
        var expressions=new LinkedHashMap<>(map(artifact.get("expressions"),""));
        var expression=new LinkedHashMap<>(map(expressions.get("e0"),""));
        var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked")));
        long id=checked.getExpr().getId();
        var reference=checked.getReferenceMapMap().get(id).toBuilder().clearOverloadId().addOverloadId("add_string");
        expression.put("checked",Base64.getEncoder().encodeToString(checked.toBuilder().putReferenceMap(id,reference.build()).build().toByteArray()));
        expressions.put("e0",expression); changed=new LinkedHashMap<>(artifact); changed.put("expressions",expressions); candidate.put("artifact",changed);
        assertEquals("ARTIFACT_INVALID",prepare(candidate).diagnostic().code());
    }
    @Test void projectionSlotsAreInvocationOwnedAndNumericBindRetainsActualMessage() throws Exception {
        var row=row("decimal-add"); var dcm=map(row.get("dcm"),""); var artifact=map(row.get("artifact"),"");
        Artifact.validateModel(dcm); Artifact.validateEnvelope(artifact,dcm);
        var policy=CapabilityPolicy.catalog(dcm);
        var plan=Artifact.validateProgram(artifact,dcm,policy,1);
        var bundle=CheckedCel.prepare(plan,CheckedCel.bindModel(dcm));
        var activation=bundle.activation();
        var state=new HashMap<String,Object>();
        state.put("input",activation.accept("input","Input",row.get("input"),"/input","INPUT_INVALID"));
        for (Object raw:list(artifact.get("steps"),"")) {
            var step=map(raw,""); String id=(String)step.get("id");
            state.put(id,bundle.evaluate(map(step.get("value"),""),state,activation,id,"/steps"));
        }
        var binding=map(map(artifact.get("return"),"").get("fields"),"");
        assertEquals("3.25",bundle.evaluate(map(binding.get("value"),""),state,activation,"sum","/return"));
        Object engine=activation.get("sum","Decimal","3.25","/return");
        assertInstanceOf(Message.class,engine);
        assertSame(engine,activation.get("sum","Decimal","3.25","/return"));
        int projected=activation.projections(), parsed=activation.boundaryParses();
        assertEquals("3.25",bundle.evaluate(map(binding.get("value"),""),state,activation,null,"/return"));
        assertEquals(projected,activation.projections()); assertEquals(parsed,activation.boundaryParses());
        var fresh=bundle.activation();
        assertEquals("9",fresh.accept("sum","Decimal","9","/return","EXPRESSION_FAILED"));
        assertNotSame(engine,fresh.get("sum","Decimal","9","/return"));
    }
}
