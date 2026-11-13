package dev.keir.runtime;

import com.google.protobuf.Message;
import dev.cel.expr.CheckedExpr;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class NumericIntegrationTest {
    @Test void descriptorsMatchExactCompilerBytesAndHashes() throws Exception {
        var batch=map(parseJson(Files.readAllBytes(Path.of("..","conformance","keir-4","descriptors.json")),65_536),"");
        var method=CheckedCel.class.getDeclaredMethod("expectedDescriptors",Map.class); method.setAccessible(true);
        for (Object raw:list(batch.get("cases"),"")) {
            var row=map(raw,"");
            var descriptors=(com.google.protobuf.DescriptorProtos.FileDescriptorSet)method.invoke(null,map(row.get("dcm"),""));
            byte[] actual=descriptors.toByteArray();
            assertArrayEquals(Base64.getDecoder().decode((String)row.get("base64")),actual,(String)row.get("id"));
            assertEquals("sha256:"+row.get("sha256"),digest(actual));
        }
    }
    @Test void numericProtectedAgentAndGuardedPathsMatchIndependentCompleteOutcomes() throws Exception {
        var fixture=map(parseJson(Files.readAllBytes(Path.of("..","conformance","keir-4","host-boundaries.json")),65_536),"");
        var dcm=map(fixture.get("dcm"),"");
        record Request(Map<String,Object> spec,Object key,List<String> calls) {}
        Request[] current={null};
        var adapters=new HashMap<String,KeirRuntime.BoundEffectAdapter>();
        for (String capability:map(dcm.get("capabilities"),"").keySet()) {
            adapters.put(capability,new KeirRuntime.BoundEffectAdapter("1","simulation","numeric.Key",
                    (args,protectedArgs,remaining,context) -> {
                        var request=current[0]; var spec=request.spec();
                        assertSame(request.key(),context.requestKey()); assertEquals(fixture.get("bindings"),protectedArgs);
                        assertEquals(10_000L,remaining);
                        boolean read=capability.equals("numbers.read");
                        assertEquals(request.calls().isEmpty() ? "numbers.read" : "numbers.write",capability);
                        var input=map(fixture.get("input"),"");
                        assertEquals(read ? Map.of("integer",input.get("integer"),"decimal",input.get("decimal")) : spec.get("args"),args);
                        request.calls().add(capability);
                        String response=read ? "read" : "write";
                        return CompletableFuture.completedFuture(spec.getOrDefault(response,fixture.get(response)));
                    }));
        }
        var bound=KeirRuntime.bindModel(canonical(dcm),adapters);
        assertTrue(bound.succeeded(),String.valueOf(bound.diagnostic()));
        var prepared=KeirRuntime.prepareProgram(Files.readAllBytes(Path.of("..","conformance","keir-4","host-boundaries-artifact.json")),bound.boundModel());
        assertTrue(prepared.succeeded(),String.valueOf(prepared.diagnostic()));
        for (Object value:list(fixture.get("cases"),"")) {
            var spec=map(value,""); current[0]=new Request(spec,new Object(),new ArrayList<>());
            var context=new KeirRuntime.BoundEffectInvocation(map(spec.getOrDefault("bindings",fixture.get("bindings")),""),
                    new KeirRuntime.Deadline(() -> 0L,10_000L),current[0].key(),Map.of("id","event1","source","owner1"));
            var actual=KeirRuntime.invokeBoundEffectProgram(prepared.prepared(),spec.getOrDefault("input",fixture.get("input")),context)
                    .toCompletableFuture().get(3,TimeUnit.SECONDS);
            var expected=new LinkedHashMap<>(map(spec.get("expect"),""));
            var trace=new ArrayList<Object>();
            for (Object event:list(map(fixture.get("traces"),"").get(spec.get("trace")),"")) {
                var row=new LinkedHashMap<>(map(event,"")); row.put("sequence",(long)trace.size()); trace.add(row);
            }
            expected.put("trace",trace);
            assertArrayEquals(canonical(expected),actual.toJson(),(String)spec.get("id"));
            assertEquals(((Number)spec.get("calls")).intValue(),current[0].calls().size(),(String)spec.get("id"));
        }
    }
    @Test void retainedNominalNamesStillExecuteAsStringsUnderNewProducer() throws Exception {
        var row=map(parseJson(Files.readAllBytes(Path.of("..","conformance","keir-4","retained-names.json")),65_536),"");
        var prepared=prepare(row); assertTrue(prepared.succeeded(),String.valueOf(prepared.diagnostic()));
        var context=new KeirRuntime.BoundEffectInvocation(Map.of(),new KeirRuntime.Deadline(() -> 0L,10_000L),
                "retained-key",Map.of("id","event1","source","owner1"));
        var outcome=KeirRuntime.invokeBoundEffectProgram(prepared.prepared(),row.get("input"),context).toCompletableFuture().get();
        var trace=new ArrayList<Object>(); event(trace,"INVOCATION_START",Map.of());
        event(trace,"STEP_START",Map.of("step","value")); event(trace,"VALUE_BOUND",Map.of("step","value"));
        event(trace,"RETURNED",Map.of()); event(trace,"TERMINATED",Map.of());
        assertArrayEquals(canonical(Map.of("classification","COMPLETED","result",Map.of("value",true),"effects",List.of(),"trace",trace)),outcome.toJson());
    }
    private static void event(List<Object> trace,String event,Map<String,Object> fields) {
        var row=new LinkedHashMap<>(fields); row.put("sequence",(long)trace.size()); row.put("event",event); trace.add(row);
    }
    private static Map<String,Object> programExpected(String program,Map<String,Object> spec) {
        var trace=new ArrayList<Object>(); var effects=new ArrayList<Object>();
        event(trace,"INVOCATION_START",Map.of());
        var result=new LinkedHashMap<String,Object>();
        String classification=(String)spec.getOrDefault("classification","COMPLETED");
        Map<String,Object> error=spec.containsKey("error") && spec.get("error") instanceof String
                ? Map.of("code",spec.get("error"),"path","/input") : spec.containsKey("error") ? map(spec.get("error"),"") : null;
        if (program.equals("quote")) {
            if (error==null) {
                for (String step:List.of("rate","valid","amount","allowed")) {
                    event(trace,"STEP_START",Map.of("step",step)); event(trace,"VALUE_BOUND",Map.of("step",step));
                }
                result.putAll(Map.of("decision",spec.get("decision"),"amount",spec.get("amount"),"message",
                        spec.get("decision").equals("ALLOW") ? "" : "Check the amount, discount and approval limit."));
                event(trace,"RETURNED",Map.of());
            } else classification="FAILED";
        } else {
            event(trace,"STEP_START",Map.of("step","stock"));
            event(trace,"CALL_DISPATCHED",Map.of("step","stock","capability","stock.get"));
            if (!Set.of("read-number","read-noncanonical").contains(spec.get("id"))) {
                event(trace,"CALL_SUCCEEDED",Map.of("step","stock","capability","stock.get"));
                event(trace,"STEP_START",Map.of("step","low")); event(trace,"VALUE_BOUND",Map.of("step","low"));
                event(trace,"STEP_START",Map.of("step","quantity"));
                if (!spec.get("id").equals("overflow")) {
                    event(trace,"VALUE_BOUND",Map.of("step","quantity"));
                    event(trace,"STEP_START",Map.of("step","order"));
                    boolean taken=spec.containsKey("quantity"); event(trace,"GUARD_EVALUATED",Map.of("step","order","taken",taken));
                    if (!taken) event(trace,"STEP_SKIPPED",Map.of("step","order"));
                    else {
                        var target=Map.<String,Object>of("step","order","capability","stock.request");
                        event(trace,"EFFECT_INTENT",target); event(trace,"CALL_DISPATCHED",target);
                        var receipt=new LinkedHashMap<>(target); receipt.put("state",map(spec.get("response"),"").get("effect"));
                        effects.add(receipt); event(trace,"EFFECT_RECORDED",receipt);
                        if (spec.get("classification").equals("COMPLETED")) event(trace,"CALL_SUCCEEDED",target);
                    }
                    if (spec.containsKey("result")) event(trace,"RETURNED",Map.of());
                }
            }
            if (spec.containsKey("result")) result.putAll(map(spec.get("result"),""));
        }
        event(trace,"TERMINATED",error==null ? Map.of() : Map.of("code",error.get("code")));
        var outcome=new LinkedHashMap<String,Object>(Map.of("classification",classification,"effects",effects,"trace",trace));
        if (error!=null) outcome.put("error",error);
        if (!result.isEmpty()) outcome.put("result",result);
        return outcome;
    }

    @Test void completeNumericProgramsPreserveCallsReceiptsAndInvocationIsolation() throws Exception {
        for (String name:List.of("stock","quote")) {
            var root=Path.of("..","examples","numeric",name);
            var program=Map.of("dcm",parseJson(Files.readAllBytes(root.resolve("dcm.json")),65_536),
                    "artifact",parseJson(Files.readAllBytes(root.resolve("artifact.json")),1_048_576));
            record Request(Map<String,Object> spec,List<Object> calls) {}
            var implementations=new HashMap<String,KeirRuntime.BoundEffectAdapter>();
            for (String capability:List.of("stock.get","stock.request")) if (name.equals("stock")) {
                implementations.put(capability,new KeirRuntime.BoundEffectAdapter("1","simulation","example.StockRequestKey",
                        (args,protectedArgs,remaining,context) -> {
                            Request key=(Request)context.requestKey(); var spec=key.spec();
                            assertEquals(Map.of("actorId","user1"),protectedArgs);
                            assertEquals(10_000L,remaining);
                            assertEquals(Map.of("id","event1","source","item1"),context.event());
                            var wanted=new LinkedHashMap<String,Object>(Map.of("itemId","item1"));
                            if (capability.equals("stock.request")) wanted.put("quantity",spec.get("quantity"));
                            assertEquals(wanted,args);
                            key.calls().add(Map.of("capability",capability,"args",immutable(args),"protectedArgs",immutable(protectedArgs)));
                            return CompletableFuture.completedFuture(capability.equals("stock.get")
                                    ? Map.of("kind","SUCCESS","value",Map.of("available",spec.get("available"))) : spec.get("response"));
                        }));
            }
            var bound=KeirRuntime.bindModel(canonical(program.get("dcm")),implementations);
            assertTrue(bound.succeeded(),String.valueOf(bound.diagnostic()));
            var prepared=KeirRuntime.prepareProgram(canonical(program.get("artifact")),bound.boundModel());
            assertTrue(prepared.succeeded(),String.valueOf(prepared.diagnostic()));
            // Four request threads share one immutable prepared plan. No request-owned values are shared.
            try (var pool=Executors.newFixedThreadPool(4)) {
                var jobs=new ArrayList<Future<?>>();
                var scenarios=map(parseJson(Files.readAllBytes(Path.of("..","examples","numeric",name,"cases.json")),65_536),"");
                for (Object value:list(scenarios.get("cases"),"")) jobs.add(pool.submit(() -> {
                    var spec=map(value,""); var request=new Request(spec,new ArrayList<>());
                    var input=name.equals("stock") ? Map.of("itemId","item1") : spec.get("input");
                    var context=new KeirRuntime.BoundEffectInvocation(name.equals("stock") ? Map.of("actorId","user1") : Map.of(),
                            new KeirRuntime.Deadline(() -> 0L,10_000L),request,Map.of("id","event1","source",name.equals("stock") ? "item1" : "quote1"));
                    var outcome=KeirRuntime.invokeBoundEffectProgram(prepared.prepared(),input,context).toCompletableFuture().join();
                    assertArrayEquals(canonical(programExpected(name,spec)),outcome.toJson(),name+"/"+spec.get("id"));
                    assertEquals(name.equals("quote") ? 0 : spec.containsKey("quantity") ? 2 : 1,request.calls().size());
                }));
                for (var job:jobs) job.get(3,TimeUnit.SECONDS);
            }
        }
    }
    private static List<Object> cases() throws Exception {
        Path path=Path.of("..","conformance","keir-4","engine-matrix.json");
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
