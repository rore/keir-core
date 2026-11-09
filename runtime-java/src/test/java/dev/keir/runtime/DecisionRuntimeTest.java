package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class DecisionRuntimeTest {
    private static byte[] file(String name) throws Exception {
        try(var stream=DecisionRuntimeTest.class.getResourceAsStream("/keir-3/"+name+".json")) {
            assertNotNull(stream,name); return stream.readAllBytes();
        }
    }
    private static Map<String,Object> document(String name) throws Exception { return map(parseCanonical(file(name)),""); }
    private static Map<String,Object> entry(Map<String,Object> model) { return map(map(model.get("entrypoints"),"").get("BeforeOperation"),""); }
    private static Map<String,KeirRuntime.BoundEffectAdapter> adapters(KeirRuntime.BoundEffectFunction read,AtomicInteger writes) {
        return Map.of("policy.get",new KeirRuntime.BoundEffectAdapter("1","simulation","sample.OperationKey/1",read),
            "native.write",new KeirRuntime.BoundEffectAdapter("1","simulation","sample.OperationKey/1",(a,h,m,c)->{
                writes.incrementAndGet(); return CompletableFuture.completedFuture(Map.of("kind","SUCCESS","value",Map.of("restricted",false),"effect","CONFIRMED"));
            }));
    }
    private static KeirRuntime.Prepared prepare(String artifact,String model,KeirRuntime.BoundEffectFunction read,AtomicInteger writes) throws Exception {
        var binding=KeirRuntime.bindModel(file(model),adapters(read,writes));
        assertTrue(binding.succeeded(),()->String.valueOf(binding.diagnostic()));
        var preparation=KeirRuntime.prepareProgram(file(artifact),binding.boundModel());
        assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic())); return preparation.prepared();
    }
    private static KeirRuntime.BoundEffectInvocation invocation(Map<String,Object> bindings,AtomicLong now,long expiry,Object key) {
        return new KeirRuntime.BoundEffectInvocation(bindings,new KeirRuntime.Deadline(now::get,expiry),key,Map.of("id","attempt1","source","scope1"));
    }
    private static KeirRuntime.Outcome await(CompletionStage<KeirRuntime.Outcome> result) throws Exception { return result.toCompletableFuture().get(2,TimeUnit.SECONDS); }

    @Test void bothEnumOrdersRunTheSameReadOnlyDecisionAndKeepProtectedContext() throws Exception {
        for(String suffix:List.of("","-reversed")) for(boolean restricted:List.of(false,true)) {
            var writes=new AtomicInteger(); var calls=new AtomicInteger(); Object key=new Object();
            var prepared=prepare("artifact-guard"+suffix,"dcm-guard"+suffix,(a,h,m,c)->{
                calls.incrementAndGet(); assertEquals(Map.of(),a); assertEquals(Map.of("actorId","actor1"),h);
                assertSame(key,c.requestKey()); assertEquals(Map.of("id","attempt1","source","scope1"),c.event());
                return CompletableFuture.completedFuture(Map.of("kind","SUCCESS","value",Map.of("restricted",restricted)));
            },writes);
            var outcome=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",false),invocation(Map.of("actorId","actor1"),new AtomicLong(),10000,key)));
            assertEquals("COMPLETED",outcome.classification()); assertNull(outcome.error());
            assertEquals(restricted?"DENY":"ALLOW",outcome.result().get("decision"));
            assertEquals(restricted?"Operation restricted.":"",outcome.result().get("message"));
            assertEquals(List.of(),outcome.effects()); assertEquals(1,calls.get()); assertEquals(0,writes.get());
        }
    }

    @Test void pureAllowNeedsNoActorBindingButStillValidatesInputAndContext() throws Exception {
        var calls=new AtomicInteger(); var writes=new AtomicInteger();
        var prepared=prepare("artifact-guard-input","dcm-guard",(a,h,m,c)->{calls.incrementAndGet();throw new AssertionError("Unused READ dispatched");},writes);
        for(boolean restricted:List.of(false,true)) {
            var outcome=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",restricted),invocation(Map.of(),new AtomicLong(),10000,"opaque")));
            assertEquals("COMPLETED",outcome.classification()); assertEquals(restricted?"DENY":"ALLOW",outcome.result().get("decision"));
        }
        var badInput=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted","false"),invocation(Map.of(),new AtomicLong(),10000,"opaque")));
        assertEquals("INPUT_INVALID",badInput.error().get("code"));
        var badKey=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",false),invocation(Map.of(),new AtomicLong(),10000,null)));
        assertEquals("HOST_CONTEXT_INVALID",badKey.error().get("code"));
        assertEquals(0,calls.get()); assertEquals(0,writes.get());
    }

    @Test void readFailuresAndHandlerAllowNeverBecomeCompletion() throws Exception {
        for(Object response:List.of(Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED"),Map.of("kind","TRANSIENT_FAILURE"),
                Map.of("kind","SUCCESS","value",Map.of("restricted","false")),Map.of("kind","SUCCESS","value",Map.of("restricted",false),"extra",true))) {
            var writes=new AtomicInteger();
            var prepared=prepare("artifact-guard","dcm-guard",(a,h,m,c)->CompletableFuture.completedFuture(response),writes);
            var outcome=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",false),invocation(Map.of("actorId","actor1"),new AtomicLong(),10000,"key")));
            assertNotEquals("COMPLETED",outcome.classification()); assertNull(outcome.result()); assertEquals(List.of(),outcome.effects()); assertEquals(0,writes.get());
        }
        var prepared=prepare("artifact-guard-handler","dcm-guard",(a,h,m,c)->CompletableFuture.completedFuture(Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED")),new AtomicInteger());
        var handled=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",false),invocation(Map.of("actorId","actor1"),new AtomicLong(),10000,"key")));
        assertEquals("BUSINESS_REJECTION",handled.classification()); assertEquals("ALLOW",handled.result().get("decision")); assertNotNull(handled.error());
        assertEquals(List.of(),handled.effects());
    }

    @Test void bindingRejectsInvalidLifecycleEffectAndDecisionOutputBeforeCallingAnything() throws Exception {
        for(String mutation:List.of("old-format","trigger","placement","consumer","write-grant","missing-decision","optional-decision","wrong-enum","extra-enum","list-output")) {
            var model=document("dcm-guard"); var entry=entry(model); var types=map(model.get("types"),"");
            var fields=map(map(types.get("DecisionResult"),"").get("fields"),"");
            switch(mutation) {
                case "old-format" -> model.put("format","keir-dcm/2");
                case "trigger" -> entry.put("trigger","EVENT");
                case "placement" -> entry.put("placement","AFTER_COMMIT");
                case "consumer" -> map(entry.get("consumer"),"").put("kind","EVENT_REPORT");
                case "write-grant" -> entry.put("effectGrant",1L);
                case "missing-decision" -> fields.remove("decision");
                case "optional-decision" -> fields.put("decision",Map.of("type","Decision","optional",true));
                case "wrong-enum" -> fields.put("decision","String");
                case "extra-enum" -> map(types.get("Decision"),"").put("values",List.of("ALLOW","DENY","UNKNOWN"));
                case "list-output" -> { types.put("Messages",Map.of("kind","list","element","String","maxItems",2L));fields.put("message","Messages"); }
            }
            var calls=new AtomicInteger(); var binding=KeirRuntime.bindModel(canonical(model),adapters((a,h,m,c)->{calls.incrementAndGet();return null;},calls));
            assertFalse(binding.succeeded(),mutation);
            assertEquals("ADMISSION_INVALID",binding.diagnostic().code(),mutation);
            assertEquals("/binding/dcm",binding.diagnostic().path(),mutation);
            assertEquals(0,calls.get());
        }
        var model=document("dcm-guard"); entry(model).put("effectGrant",0L);
        var fields=map(map(map(model.get("types"),"").get("DecisionResult"),"").get("fields"),"");
        fields.put("when",Map.of("type","Timestamp","optional",true)); fields.put("flag","Boolean");
        assertTrue(KeirRuntime.bindModel(canonical(model),adapters((a,h,m,c)->null,new AtomicInteger())).succeeded());
    }

    @Test void forgedFalseGuardWriteAndMixedIdentityPairsFailPreparation() throws Exception {
        var binding=KeirRuntime.bindModel(file("dcm-guard"),adapters((a,h,m,c)->null,new AtomicInteger()));
        assertTrue(binding.succeeded());
        var artifact=document("artifact-guard-input");
        list(artifact.get("steps"),"").add(Map.of("op","CALL","id","written","capability","native.write","version","1","args",Map.of(),"resultType","Policy","when","restricted"));
        var rejected=KeirRuntime.prepareProgram(canonical(artifact),binding.boundModel());
        assertFalse(rejected.succeeded()); assertEquals("ENTRY_GRANT_DENIED",rejected.diagnostic().code());
        for(String field:List.of("format","compiler","modelRef")) {
            artifact=document("artifact-guard");
            if(field.equals("modelRef")) map(artifact.get(field),"").put("format","keir-dcm/2");
            else artifact.put(field,field.equals("format")?"keir-artifact/2":"keir-compiler-java/0.2.0");
            var mixed=KeirRuntime.prepareProgram(canonical(artifact),binding.boundModel());
            assertFalse(mixed.succeeded(),field);
            assertEquals(field.equals("modelRef")?"ARTIFACT_INVALID":"PIN_MISMATCH",mixed.diagnostic().code(),field);
            assertEquals(field.equals("modelRef")?"/artifact/modelRef":"/artifact/compiler",mixed.diagnostic().path(),field);
        }
    }

    @Test void expiredReadAndLateResultCannotReviseTerminalOutcome() throws Exception {
        var now=new AtomicLong(); var calls=new AtomicInteger(); var pending=new CompletableFuture<Object>();
        var prepared=prepare("artifact-guard","dcm-guard",(a,h,m,c)->{calls.incrementAndGet();return pending;},new AtomicInteger());
        var expired=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",false),invocation(Map.of("actorId","actor1"),now,0,"key")));
        assertEquals("LIMIT_EXCEEDED",expired.classification()); assertEquals(0,calls.get());
        var running=KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("restricted",false),invocation(Map.of("actorId","actor1"),now,20,"key"));
        var outcome=await(running); byte[] terminal=outcome.toJson(); assertEquals("LIMIT_EXCEEDED",outcome.classification()); assertNull(outcome.result());
        pending.complete(Map.of("kind","SUCCESS","value",Map.of("restricted",false)));
        assertArrayEquals(terminal,await(running).toJson()); assertEquals(1,calls.get()); assertEquals(List.of(),outcome.effects());
    }
}
