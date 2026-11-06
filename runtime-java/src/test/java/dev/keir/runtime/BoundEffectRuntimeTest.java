package dev.keir.runtime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;
import com.sun.net.httpserver.HttpServer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Constant;
import dev.cel.expr.Expr;
import dev.cel.expr.Reference;
import dev.cel.expr.SourceInfo;
import dev.cel.expr.Type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class BoundEffectRuntimeTest {
    static final String KEY_TYPE="sample.RequestUuid/1";
    static final UUID KEY=UUID.fromString("00000000-0000-4000-8000-000000000001");
    static byte[] file(String file) throws IOException {
        try(var stream=BoundEffectRuntimeTest.class.getResourceAsStream("/keir-2/"+file)) {
            assertNotNull(stream,file); return stream.readAllBytes();
        }
    }
    static Map<String,Object> document(byte[] bytes) { return map(parseCanonical(bytes),""); }
    static CompletableFuture<Object> done(Object value) { return CompletableFuture.completedFuture(value); }
    static KeirRuntime.Outcome await(CompletionStage<KeirRuntime.Outcome> value) throws Exception { return value.toCompletableFuture().get(3,TimeUnit.SECONDS); }
    static Map<String,Object> effectRead(String status) { return Map.of("kind","SUCCESS","value",Map.of("id","task1","projectId","project1","status",status)); }
    static Map<String,Object> confirmed() { return Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED"); }
    static void error(KeirRuntime.Outcome outcome,String code,String path) {
        assertEquals(code,outcome.error().get("code")); assertEquals(path,outcome.error().get("path"));
    }
    static void denied(KeirRuntime.Preparation value,String code,String path) {
        assertFalse(value.succeeded()); assertNull(value.prepared()); assertEquals(code,value.diagnostic().code()); assertEquals(path,value.diagnostic().path());
    }
    static void bindingDenied(KeirRuntime.ModelBinding value,String code,String path) {
        assertFalse(value.succeeded()); assertNull(value.boundModel()); assertEquals(code,value.diagnostic().code()); assertEquals(path,value.diagnostic().path());
    }

    @Test void pinnedCelEvaluatesTheLargestAdmittedConcatenationTreeTo8192Bytes() throws Exception {
        var artifact=document(file("artifact-title-max.json"));
        var step=map(list(artifact.get("steps"),"").getLast(),"");
        var title=map(map(step.get("args"),"").get("title"),"");
        var expression=map(map(artifact.get("expressions"),"").get(title.get("expr")),"");
        var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked")));
        assertEquals(63,checked.getTypeMapCount());
        assertEquals(31,checked.getReferenceMapMap().values().stream().filter(ref->ref.getOverloadIdList().equals(List.of("add_string"))).count());
        var runtime=dev.cel.runtime.CelRuntimeFactory.plannerRuntimeBuilder()
                .setOptions(dev.cel.common.CelOptions.current().enableHeterogeneousNumericComparisons(true)
                        .comprehensionMaxIterations(0).build()).build();
        var program=runtime.createProgram(dev.cel.common.CelProtoAbstractSyntaxTree.fromCheckedExpr(checked).getAst());
        String value=(String)program.eval(Map.of("base","é".repeat(128)));
        assertEquals("é".repeat(4096),value);
        assertEquals(8192,value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }

    private static Map<String,Object> model() throws Exception {
        return map(parseJson(file("dcm.json"),Profile.DCM_BYTES),"");
    }
    static Map<String,KeirRuntime.BoundEffectAdapter> implementations(String mode,KeirRuntime.BoundEffectFunction read,KeirRuntime.BoundEffectFunction write) {
        return new LinkedHashMap<>(Map.of("tasks.get",new KeirRuntime.BoundEffectAdapter("1",mode,KEY_TYPE,read),
                "tasks.create",new KeirRuntime.BoundEffectAdapter("1",mode,KEY_TYPE,write)));
    }
    private static Map<String,KeirRuntime.BoundEffectAdapter> implementations(String mode) {
        return implementations(mode,(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(confirmed()));
    }
    private static KeirRuntime.BoundModel bind(Map<String,KeirRuntime.BoundEffectAdapter> implementations) throws Exception {
        var result=KeirRuntime.bindModel(file("dcm.json"),implementations);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); assertNull(result.diagnostic()); return result.boundModel();
    }
    private static KeirRuntime.Prepared prepare(String program,KeirRuntime.BoundModel bound) throws Exception {
        var result=KeirRuntime.prepareProgram(file(program.equals("main")?"artifact.json":"artifact-"+program+".json"),bound);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); return result.prepared();
    }
    private static KeirRuntime.BoundEffectInvocation invocation(Object key,String actor,String event,KeirRuntime.Deadline deadline,boolean calls) {
        return new KeirRuntime.BoundEffectInvocation(calls?Map.of("actorId",actor):Map.of(),deadline,key,Map.of("id",event,"source","task1"));
    }
    static KeirRuntime.BoundEffectInvocation invocation(boolean calls) {
        return invocation(KEY,"user1","event1",new KeirRuntime.Deadline(()->0,1000),calls);
    }
    private static KeirRuntime.Outcome invoke(KeirRuntime.Prepared prepared,boolean calls) throws Exception {
        return await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invocation(calls)));
    }
    static void receipt(KeirRuntime.Outcome outcome,String state) {
        assertEquals(List.of(state),outcome.effects().stream().map(e->e.get("state")).toList());
    }

    @Test void sameCompilerArtifactRunsInRealAndSimulationWithNoInitializationCalls() throws Exception {
        for(String mode:List.of("simulation","real")) {
            var calls=new ArrayList<String>();
            var registrations=implementations(mode,(a,h,ms,c)->{calls.add("READ");assertSame(KEY,c.requestKey());return done(effectRead("COMPLETED"));},
                    (a,h,ms,c)->{calls.add("WRITE");assertSame(KEY,c.requestKey());assertEquals(Map.of("actorId","user1"),h);return done(confirmed());});
            var bound=bind(registrations); var prepared=prepare("main",bound);
            assertTrue(calls.isEmpty()); registrations.clear();
            var result=invoke(prepared,true); assertEquals("COMPLETED",result.classification()); receipt(result,"CONFIRMED");
            assertEquals(List.of("READ","WRITE"),calls);
            assertTrue(result.trace().size()<=64);
        }
    }

    @Test void guardedHandlerRequiresNativePreparationAndKeepsRejectionEvidence() throws Exception {
        byte[] bytes=file("artifact-handler.json");
        assertEquals(new String(file("artifact-handler.sha256"),StandardCharsets.UTF_8).trim(),digest(bytes));
        var calls=new ArrayList<String>();
        var bound=bind(implementations("simulation",(a,h,ms,c)->{
            calls.add("tasks.get"); return done(effectRead("COMPLETED"));
        },(a,h,ms,c)->{
            calls.add("tasks.create"); return done(Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"));
        }));
        var artifact=document(bytes);
        map(list(artifact.get("steps"),"").getLast(),"").put("when","missing");
        denied(KeirRuntime.prepareProgram(canonical(artifact),bound),"ARTIFACT_INVALID","/artifact/steps/3/when");
        artifact=document(bytes);
        var handler=map(map(list(artifact.get("steps"),"").getLast(),"").get("onError"),"");
        handler.put("NOT_DECLARED",handler.remove("CREATE_REJECTED"));
        denied(KeirRuntime.prepareProgram(canonical(artifact),bound),"ARTIFACT_INVALID","/artifact/steps/3/onError/NOT_DECLARED");
        artifact=document(bytes);
        map(map(artifact.get("expressions"),"").get("e0"),"").put("checked","/w==");
        denied(KeirRuntime.prepareProgram(canonical(artifact),bound),"ARTIFACT_INVALID","/artifact/expressions");
        artifact=document(bytes);
        map(map(artifact.get("expressions"),"").get("e3"),"").put("variables",
                Map.of("eligible","Boolean","eligibleProject","ProjectId","input","TaskCompletedInput","source","Task","created","CreatedTask"));
        denied(KeirRuntime.prepareProgram(canonical(artifact),bound),"ARTIFACT_INVALID","/artifact/return/fields/status");
        var prepared=prepare("handler",bound);
        assertTrue(calls.isEmpty());
        var outcome=invoke(prepared,true);
        assertEquals("BUSINESS_REJECTION",outcome.classification());
        error(outcome,"CREATE_REJECTED","/steps/3");
        assertEquals(Map.of("status","NO_OP"),outcome.result());
        assertEquals(List.of("tasks.get","tasks.create"),calls);
        receipt(outcome,"NONE");
        assertThrows(UnsupportedOperationException.class,()->outcome.result().clear());
        assertThrows(UnsupportedOperationException.class,()->outcome.trace().getFirst().clear());
        assertThrows(UnsupportedOperationException.class,()->outcome.effects().getFirst().clear());
    }

    @Test void completeCatalogRequiresExactCompatibleRegistrationsIncludingUnusedCapabilities() throws Exception {
        var bytes=file("dcm.json");
        assertFalse(KeirRuntime.bindModel(bytes,null).succeeded());
        for(String id:List.of("tasks.create","tasks.get")) {
            var missing=implementations("real");missing.remove(id);
            bindingDenied(KeirRuntime.bindModel(bytes,missing),"ADMISSION_INVALID","/binding/implementations");
            for(var invalid:Arrays.asList(null,new KeirRuntime.BoundEffectAdapter("2","real",KEY_TYPE,(a,h,ms,c)->done(null)),
                    new KeirRuntime.BoundEffectAdapter("1","real","wrong",(a,h,ms,c)->done(null)),
                    new KeirRuntime.BoundEffectAdapter("1","unknown",KEY_TYPE,(a,h,ms,c)->done(null)),
                    new KeirRuntime.BoundEffectAdapter("1","real",KEY_TYPE,null))) {
                var bad=implementations("real");bad.put(id,invalid);
                bindingDenied(KeirRuntime.bindModel(bytes,bad),"ADMISSION_INVALID","/binding/implementations/"+id);
            }
        }
        var extra=implementations("real");extra.put("tasks.extra",extra.get("tasks.get"));
        bindingDenied(KeirRuntime.bindModel(bytes,extra),"ADMISSION_INVALID","/binding/implementations");
        var mixed=implementations("real");mixed.put("tasks.get",implementations("simulation").get("tasks.get"));
        bindingDenied(KeirRuntime.bindModel(bytes,mixed),"ADMISSION_INVALID","/binding/implementations/tasks.get");
        var bound=bind(implementations("real"));assertEquals("COMPLETED",invoke(prepare("no-call",bound),false).classification());
    }

    @Test void bindingRejectsWrongRecordFamiliesAndLegacyFormatsWithoutRunningThem() throws Exception {
        @SuppressWarnings({"rawtypes","unchecked"}) Map<String,KeirRuntime.BoundEffectAdapter> unsafe=
                (Map)Map.of("tasks.create",new Object(),"tasks.get",new Object());
        bindingDenied(KeirRuntime.bindModel(file("dcm.json"),unsafe),"ADMISSION_INVALID","/binding/implementations/tasks.create");
        for(String format:List.of("keir-dcm/effect-4","keir-dcm/effect-3","keir-dcm/read-2","keir-dcm/1")) {
            var old=model(); old.put("format",format);
            assertFalse(KeirRuntime.bindModel(canonical(old),implementations("simulation")).succeeded(),format);
        }
        var extra=model(); extra.put("assurance","unsupported");
        assertFalse(KeirRuntime.bindModel(canonical(extra),implementations("simulation")).succeeded());
        var invalid=new String(file("dcm.json"),StandardCharsets.UTF_8)
                .replace("\"format\":","\"format\":\"keir-dcm/2\",\"format\":").getBytes(StandardCharsets.UTF_8);
        assertFalse(KeirRuntime.bindModel(invalid,implementations("simulation")).succeeded());
    }

    @Test void unusedModelDeclarationsAndLifecycleMetadataFailClosed() throws Exception {
        for(String field:List.of("types","capabilities","consumer","event","placement")) {
            var model=model();
            var entry=map(map(model.get("entrypoints"),"").get("TaskCompleted"),"");
            switch(field) {
                case "types" -> map(model.get(field),"").put("Unused",Map.of("kind","record","fields",Map.of("x","Missing")));
                case "capabilities" -> map(map(model.get(field),"").get("tasks.create"),"").put("errors",List.of("INVALID","INVALID"));
                case "consumer" -> map(entry.get(field),"").put("kind","DISPLAY");
                case "event" -> map(entry.get(field),"").put("sourceType","String");
                case "placement" -> entry.put(field,"BEFORE_COMMIT");
                default -> throw new AssertionError(field);
            }
            bindingDenied(KeirRuntime.bindModel(canonical(model),implementations("simulation")),"ADMISSION_INVALID","/binding/dcm");
        }
    }

    @Test void staticPreparationChecksCompilerEnvironmentModelAndClosedEnvelope() throws Exception {
        var bound=bind(implementations("real"));var bytes=file("artifact.json");
        assertFalse(KeirRuntime.prepareProgram(bytes,null).succeeded());
        assertFalse(KeirRuntime.prepareProgram(null,bound).succeeded());
        var mutations=Map.<String,Object>of("compiler","keir-compiler-java/0.2.0-SNAPSHOT","environment",Map.of(),
                "modelRef",Map.of(),"format","keir-artifact/effect-4");
        for(var mutation:mutations.entrySet()) {
            var artifact=document(bytes);artifact.put(mutation.getKey(),mutation.getValue());
            denied(KeirRuntime.prepareProgram(canonical(artifact),bound),
                    Set.of("compiler","environment").contains(mutation.getKey())?"PIN_MISMATCH":"ARTIFACT_INVALID",
                    "/artifact/"+mutation.getKey());
        }
        for(String field:List.of("profile","dcm","descriptors")) {
            var artifact=document(bytes);artifact.put(field,"legacy");
            assertFalse(KeirRuntime.prepareProgram(canonical(artifact),bound).succeeded(),field);
        }
        for(String field:List.of("digest","descriptorRecipe","domainVersion")) {
            var artifact=document(bytes);map(artifact.get("modelRef"),"").put(field,"forged");
            denied(KeirRuntime.prepareProgram(canonical(artifact),bound),"ARTIFACT_INVALID","/artifact/modelRef");
        }
        var forged=document(bytes);forged.put("compiler","keir-compiler-java/1.0.0");
        denied(KeirRuntime.prepareProgram(canonical(forged),bound),"PIN_MISMATCH","/artifact/compiler");
        var old=document(bytes);old.put("format","keir-artifact/1");
        denied(KeirRuntime.prepareProgram(canonical(old),bound),"ARTIFACT_INVALID","/artifact/format");
        byte[] padded=Arrays.copyOf(bytes,bytes.length+1);padded[bytes.length]=' ';
        assertFalse(KeirRuntime.prepareProgram(padded,bound).succeeded());
        var prepared=prepare("main",bound);
        error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),null)),"HOST_CONTEXT_INVALID","");
    }

    @Test void onePreparedHandleReceivesFreshActorsEventsOriginalOpaqueKeysAndDeadlines() throws Exception {
        var seen=new ArrayList<KeirRuntime.BoundEffectContext>();var actors=new ArrayList<Object>();
        var prepared=prepare("main",bind(implementations("real",(a,h,ms,c)->{actors.add(h.get("actorId"));seen.add(c);return done(effectRead("COMPLETED"));},
                (a,h,ms,c)->{actors.add(h.get("actorId"));seen.add(c);return done(confirmed());})));
        var keys=List.of(new Object(),new Object());
        for(int i=0;i<2;i++) {
            var clock=new AtomicLong(10+i);var deadline=new KeirRuntime.Deadline(clock::get,1000+i);
            var result=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invocation(keys.get(i),"user"+i,"event"+i,deadline,true)));
            assertEquals("COMPLETED",result.classification());
            for(var context:seen.subList(i*2,i*2+2)) {
                assertSame(keys.get(i),context.requestKey());assertEquals(Map.of("id","event"+i,"source","task1"),context.event());
                assertSame(deadline.clock(),context.deadline().clock());assertEquals(deadline.expiresAtMs(),context.deadline().expiresAtMs());
                assertThrows(UnsupportedOperationException.class,()->context.event().put("id","changed"));
            }
        }
        assertEquals(List.of("user0","user0","user1","user1"),actors);
    }

    @Test void contextIsSnapshottedBeforeAwaitAndOpaqueKeyIsNeverInspected() throws Exception {
        Object key=new Object() { @Override public String toString() {throw new AssertionError("opaque");} };
        var pending=new CompletableFuture<Object>();var entered=new CountDownLatch(1);
        var prepared=prepare("main",bind(implementations("real",(a,h,ms,c)->{entered.countDown();return pending;},
                (a,h,ms,c)->{assertSame(key,c.requestKey());assertEquals("event1",c.event().get("id"));assertEquals("user1",h.get("actorId"));return done(confirmed());})));
        var bindings=new LinkedHashMap<String,Object>(Map.of("actorId","user1"));
        var event=new LinkedHashMap<String,Object>(Map.of("id","event1","source","task1"));
        var deadline=new KeirRuntime.Deadline(()->0,1000);
        var result=KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),new KeirRuntime.BoundEffectInvocation(bindings,deadline,key,event));
        assertTrue(entered.await(1,TimeUnit.SECONDS));bindings.clear();event.clear();pending.complete(effectRead("COMPLETED"));
        assertEquals("COMPLETED",await(result).classification());
    }

    @Test void structuralPreflightRejectsMissingExtraInvalidContextBeforeDispatch() throws Exception {
        AtomicInteger calls=new AtomicInteger();var prepared=prepare("main",bind(implementations("real",(a,h,ms,c)->{calls.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->done(confirmed()))));
        var valid=invocation(true);var cycle=new LinkedHashMap<String,Object>();cycle.put("self",cycle);
        for(var bindings:Arrays.asList(null,Map.<String,Object>of(),Map.<String,Object>of("actorId","user1","extra",true),Map.<String,Object>of("actorId",cycle))) {
            var invalid=new KeirRuntime.BoundEffectInvocation(bindings,valid.deadline(),KEY,valid.event());
            error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invalid)),"HOST_BINDING_INVALID","/hostBindings");
        }
        for(var event:Arrays.asList(null,Map.<String,Object>of(),Map.<String,Object>of("id","event1","source","task1","extra",true),Map.<String,Object>of("id",cycle,"source","task1"))) {
            var invalid=new KeirRuntime.BoundEffectInvocation(valid.bindings(),valid.deadline(),KEY,event);
            error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invalid)),"HOST_CONTEXT_INVALID","");
        }
        for(var invalid:List.of(new KeirRuntime.BoundEffectInvocation(valid.bindings(),valid.deadline(),null,valid.event()),
                new KeirRuntime.BoundEffectInvocation(valid.bindings(),null,KEY,valid.event()),
                new KeirRuntime.BoundEffectInvocation(valid.bindings(),new KeirRuntime.Deadline(null,1000),KEY,valid.event())))
            error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invalid)),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId",true),valid)),"INPUT_INVALID","/input");
        var expired=new KeirRuntime.BoundEffectInvocation(valid.bindings(),new KeirRuntime.Deadline(()->1000,1000),KEY,valid.event());
        error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),expired)),"BUDGET_EXCEEDED","");
        assertEquals(0,calls.get());
    }

    @Test void operationFreeEmptyCatalogHasNoModeDefaultAndCannotDispatchAbsentCapability() throws Exception {
        var dcm=model();dcm.put("capabilities",Map.of());
        var binding=KeirRuntime.bindModel(canonical(dcm),Map.<String,KeirRuntime.BoundEffectAdapter>of());assertTrue(binding.succeeded());
        var artifact=document(file("artifact-no-call.json"));artifact.put("modelRef",Artifact.modelRef(dcm));
        var preparation=KeirRuntime.prepareProgram(canonical(artifact),binding.boundModel());assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic()));
        var result=invoke(preparation.prepared(),false);assertEquals("COMPLETED",result.classification());assertEquals(List.of(),result.effects());
        var call=document(file("artifact.json"));call.put("modelRef",Artifact.modelRef(dcm));
        denied(KeirRuntime.prepareProgram(canonical(call),binding.boundModel()),"CAPABILITY_UNKNOWN","/artifact/steps/1");
    }

    @Test void zeroCallProgramDoesNotQueryHostPermissionOrKeyAuthenticity() throws Exception {
        AtomicInteger calls=new AtomicInteger();var permitted=new AtomicBoolean(false);
        var bound=bind(implementations("real",(a,h,ms,c)->{calls.incrementAndGet();return done(Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED"));},
                (a,h,ms,c)->{calls.incrementAndGet();assertTrue(permitted.get());return done(confirmed());}));
        var prepared=prepare("no-call",bound);var result=invoke(prepared,false);
        assertEquals("COMPLETED",result.classification());assertEquals(Map.of("status","NO_OP"),result.result());assertEquals(0,calls.get());
    }

    @Test void hostPermissionDenialIsAnAttemptedWriteWithNoneEvidence() throws Exception {
        var entered=new AtomicInteger();var nativeWrites=new AtomicInteger();var permitted=new AtomicBoolean(false);
        var prepared=prepare("main",bind(implementations("real",(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->{
            entered.incrementAndGet();
            if(!permitted.get())return done(Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED","effect","NONE"));
            nativeWrites.incrementAndGet();return done(confirmed());
        })));
        var result=invoke(prepared,true);assertEquals("BUSINESS_REJECTION",result.classification());receipt(result,"NONE");
        assertEquals(1,entered.get());assertEquals(0,nativeWrites.get());
        assertTrue(result.trace().stream().anyMatch(e->"CALL_DISPATCHED".equals(e.get("event")) && "tasks.create".equals(e.get("capability"))));
    }

    @Test void pendingHostPermissionUsesCurrentAuthorityAndAbsoluteDeadlineBeforeNativeWrite() throws Exception {
        for(boolean expires:List.of(false,true)) {
            var permission=new CompletableFuture<Boolean>();var entered=new CountDownLatch(1);var permitted=new AtomicBoolean(true);var nativeWrites=new AtomicInteger();var clock=new AtomicLong();
            var deadline=new KeirRuntime.Deadline(clock::get,1000);
            var prepared=prepare("main",bind(implementations("real",(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->{
                assertSame(deadline.clock(),c.deadline().clock());assertEquals(1000,c.deadline().expiresAtMs());assertEquals(1000,ms);entered.countDown();
                return permission.thenApply(ignored->{
                    if(!permitted.get() || c.deadline().clock().getAsLong()>=c.deadline().expiresAtMs())
                        return Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED","effect","NONE");
                    nativeWrites.incrementAndGet();return confirmed();
                });
            })));
            var outstanding=KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invocation(KEY,"user1","event1",deadline,true));
            assertTrue(entered.await(1,TimeUnit.SECONDS));if(expires)clock.set(1000);else permitted.set(false);permission.complete(true);
            var result=await(outstanding);receipt(result,"NONE");assertEquals(0,nativeWrites.get());
            if(expires)error(result,"BUDGET_EXCEEDED","/steps/3");else assertEquals("BUSINESS_REJECTION",result.classification());
        }
    }

    @Test void confirmedReceiptSurvivesLaterHostDenial() throws Exception {
        var reads=new AtomicInteger();var prepared=prepare("after-write-read",bind(implementations("real",(a,h,ms,c)->
                done(reads.incrementAndGet()==1?effectRead("COMPLETED"):Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED")),(a,h,ms,c)->done(confirmed()))));
        var result=invoke(prepared,true);assertEquals("BUSINESS_REJECTION",result.classification());receipt(result,"CONFIRMED");assertEquals(2,reads.get());
    }

    @Test void thrownLostMalformedAndTimedOutWriteResponsesNeverBecomeNoneOrRetry() throws Exception {
        for(KeirRuntime.BoundEffectFunction write:List.<KeirRuntime.BoundEffectFunction>of(
                (a,h,ms,c)->{throw new IllegalStateException("lost response");},
                (a,h,ms,c)->CompletableFuture.failedFuture(new IllegalStateException("lost response")),
                (a,h,ms,c)->done(Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED")),
                (a,h,ms,c)->new CompletableFuture<>())) {
            var writes=new AtomicInteger();var entered=new CountDownLatch(1);
            var prepared=prepare("main",bind(implementations("real",(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->{writes.incrementAndGet();entered.countDown();return write.invoke(a,h,ms,c);})));
            var result=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invocation(KEY,"user1","event1",new KeirRuntime.Deadline(()->0,100),true)));
            assertTrue(entered.await(1,TimeUnit.SECONDS));assertEquals("UNCERTAIN",result.classification());receipt(result,"UNKNOWN");assertEquals(1,writes.get());
        }
    }

    @Test void dcmPolicySingleWriteAndResourceCeilingsRemainEnforced() throws Exception {
        var bound=bind(implementations("real"));
        var tooManySteps=document(file("artifact.json"));var steps=list(tooManySteps.get("steps"),"");
        while(steps.size()<9)steps.add(Map.of("op","BIND","id","extra"+steps.size(),"resultType","Boolean","value",Map.of("type","Boolean","literal",true)));
        denied(KeirRuntime.prepareProgram(canonical(tooManySteps),bound),"BUDGET_EXCEEDED","/artifact/steps");
        var secondWrite=document(file("artifact.json"));steps=list(secondWrite.get("steps"),"");var call=new LinkedHashMap<>(map(steps.get(3),""));call.put("id","secondWrite");steps.add(call);
        denied(KeirRuntime.prepareProgram(canonical(secondWrite),bound),"BUDGET_EXCEEDED","/artifact/steps/4");
        var oversized=document(file("artifact.json"));oversized.put("expressions",new LinkedHashMap<String,Object>());
        for(int i=0;i<513;i++)map(oversized.get("expressions"),"").put("e"+i,Map.of());
        assertFalse(KeirRuntime.prepareProgram(canonical(oversized),bound).succeeded());
        var hugeDcm=file("dcm.json");var padded=Arrays.copyOf(hugeDcm,65_537);Arrays.fill(padded,hugeDcm.length,padded.length,(byte)' ');
        bindingDenied(KeirRuntime.bindModel(padded,implementations("real")),"BUDGET_EXCEEDED","/binding/dcm");
        for(String field:List.of("capabilityPolicy","effectGrant")) {
            var dcm=model();var entry=map(map(dcm.get("entrypoints"),"").get("TaskCompleted"),"");
            entry.put(field,field.equals("effectGrant")?0L:Map.of("maxCalls",Map.of("tasks.create",0L)));
            var binding=KeirRuntime.bindModel(canonical(dcm),implementations("real"));assertTrue(binding.succeeded());
            var artifact=document(file("artifact.json"));artifact.put("modelRef",Artifact.modelRef(dcm));
            assertFalse(KeirRuntime.prepareProgram(canonical(artifact),binding.boundModel()).succeeded());
        }
    }

    @Test void fourCallsAndEightStepsPassButTheirNextValuesFailStatically() throws Exception {
        var bound=bind(implementations("real"));
        for(int count:List.of(4,5)) {
            var artifact=document(file("artifact-no-call.json"));artifact.put("expressions",Map.of());
            var steps=new ArrayList<Object>();
            for(int i=0;i<count;i++)steps.add(Map.of("op","CALL","id","read"+i,"capability","tasks.get","version","1","resultType","Task",
                    "args",Map.of("taskId",Map.of("type","TaskId","literal","task1"))));
            artifact.put("steps",steps);artifact.put("return",Map.of("type","FollowupResult","fields",Map.of("status",Map.of("type","FollowupStatus","literal","NO_OP"))));
            var preparation=KeirRuntime.prepareProgram(canonical(artifact),bound);
            if(count==4) {assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic()));assertEquals("COMPLETED",invoke(preparation.prepared(),true).classification());}
            else denied(preparation,"BUDGET_EXCEEDED","/artifact/steps/4");
        }
        for(int count:List.of(8,9)) {
            var artifact=document(file("artifact-no-call.json"));artifact.put("expressions",Map.of());
            var steps=new ArrayList<Object>();
            for(int i=0;i<count;i++)steps.add(Map.of("op","BIND","id","bound"+i,"resultType","Boolean","value",Map.of("type","Boolean","literal",true)));
            artifact.put("steps",steps);artifact.put("return",Map.of("type","FollowupResult","fields",Map.of("status",Map.of("type","FollowupStatus","literal","NO_OP"))));
            var preparation=KeirRuntime.prepareProgram(canonical(artifact),bound);
            if(count==8) {assertTrue(preparation.succeeded());assertEquals("COMPLETED",invoke(preparation.prepared(),false).classification());}
            else denied(preparation,"BUDGET_EXCEEDED","/artifact/steps");
        }
    }

    private static Expr booleanTree(int leaves,AtomicLong next,CheckedExpr.Builder checked) {
        long id=next.incrementAndGet();checked.putTypeMap(id,Type.newBuilder().setPrimitive(Type.PrimitiveType.BOOL).build());
        if(leaves==1)return Expr.newBuilder().setId(id).setConstExpr(Constant.newBuilder().setBoolValue(true)).build();
        checked.putReferenceMap(id,Reference.newBuilder().addOverloadId("logical_and").build());
        return Expr.newBuilder().setId(id).setCallExpr(Expr.Call.newBuilder().setFunction("_&&_")
                .addArgs(booleanTree(leaves/2,next,checked)).addArgs(booleanTree(leaves-leaves/2,next,checked))).build();
    }

    @Test void lateWriteResponsesCannotReviseTheTerminalOutcome() throws Exception {
        for(var response:List.of(confirmed(),Map.of("kind","TRANSIENT_FAILURE","effect","NONE"))) {
            var pending=new CompletableFuture<Object>();
            var writes=new AtomicInteger();
            var prepared=prepare("main",bind(implementations("simulation",
                    (a,h,ms,c)->done(effectRead("COMPLETED")),
                    (a,h,ms,c)->{writes.incrementAndGet();return pending;})));
            var context=invocation(KEY,"user1","event1",new KeirRuntime.Deadline(()->0,30),true);
            var result=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),context));
            assertEquals("UNCERTAIN",result.classification());
            receipt(result,"UNKNOWN");
            byte[] terminal=result.toJson();
            assertTrue(pending.complete(response));
            assertArrayEquals(terminal,result.toJson());
            assertEquals(1,writes.get());
        }
    }

    @Test void winningWriteEnvelopeIsSnapshottedBeforeCompletionClock() throws Exception {
        var response=new LinkedHashMap<>(confirmed());
        var wrote=new AtomicBoolean();
        var deadline=new KeirRuntime.Deadline(()->{
            if(wrote.compareAndSet(true,false)) {
                response.clear();
                response.putAll(Map.of("kind","TRANSIENT_FAILURE","effect","NONE"));
            }
            return 0;
        },10000);
        var prepared=prepare("main",bind(implementations("simulation",
                (a,h,ms,c)->done(effectRead("COMPLETED")),
                (a,h,ms,c)->{wrote.set(true);return done(response);})));
        var context=invocation(KEY,"user1","event1",deadline,true);
        var outcome=await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),context));
        assertEquals("TRANSIENT_FAILURE",response.get("kind"));
        assertEquals("COMPLETED",outcome.classification());
        assertEquals(Map.of("status","CREATED"),outcome.result());
        receipt(outcome,"CONFIRMED");
    }

    @Test void malformedWriteEnvelopesCannotSettleCommitEvidence() throws Exception {
        var extraField=new LinkedHashMap<>(confirmed());
        extraField.put("extra",true);
        for(var response:List.of(extraField,Map.of("kind","BUSINESS_REJECTION","effect","NONE"))) {
            var prepared=prepare("main",bind(implementations("simulation",
                    (a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(response))));
            var outcome=invoke(prepared,true);
            error(outcome,"ADAPTER_INVALID","/steps/3");
            assertEquals("UNCERTAIN",outcome.classification());
            receipt(outcome,"UNKNOWN");
        }
    }

    @Test void malformedReadAndConfirmedWriteValuesCannotEraseEvidenceOrDispatchFurther() throws Exception {
        var writes=new AtomicInteger();
        var readInvalid=prepare("main",bind(implementations("simulation",
                (a,h,ms,c)->done(Map.of("kind","SUCCESS","value",Map.of(),"effect","CONFIRMED")),
                (a,h,ms,c)->{writes.incrementAndGet();return done(confirmed());})));
        var readOutcome=invoke(readInvalid,true);
        error(readOutcome,"ADAPTER_INVALID","/steps/1");
        assertTrue(readOutcome.effects().isEmpty());
        assertEquals(0,writes.get());
        var reads=new AtomicInteger();
        var confirmedInvalid=prepare("after-write-read",bind(implementations("simulation",
                (a,h,ms,c)->{reads.incrementAndGet();return done(effectRead("COMPLETED"));},
                (a,h,ms,c)->done(Map.of("kind","SUCCESS","value",Map.of(),"effect","CONFIRMED")))));
        var writeOutcome=invoke(confirmedInvalid,true);
        error(writeOutcome,"ADAPTER_INVALID","/steps/3");
        receipt(writeOutcome,"CONFIRMED");
        assertEquals(1,reads.get());
    }

    @Test void checkedAstBudgetStillAppliesThroughPublicPreparation() throws Exception {
        var bound=bind(implementations("real"));
        for(int leaves:List.of(32,33)) {
            var artifact=document(file("artifact-no-call.json"));var checked=CheckedExpr.newBuilder().setSourceInfo(SourceInfo.newBuilder().setLocation("<input>"));
            checked.setExpr(booleanTree(leaves,new AtomicLong(),checked));
            var expressions=new LinkedHashMap<String,Object>();expressions.put("e0",Map.of("checked",Base64.getEncoder().encodeToString(checked.build().toByteArray()),
                    "variables",Map.of("input","TaskCompletedInput"),"resultType","Boolean"));
            expressions.put("e1",map(artifact.get("expressions"),"").get("e0"));artifact.put("expressions",expressions);
            map(list(artifact.get("steps"),"").getFirst(),"").put("value",Map.of("expr","e0","type","Boolean"));
            map(map(artifact.get("return"),"").get("fields"),"").put("status",Map.of("expr","e1","type","FollowupStatus"));
            var preparation=KeirRuntime.prepareProgram(canonical(artifact),bound);
            if(leaves==32) {assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic()));assertEquals("COMPLETED",invoke(preparation.prepared(),false).classification());}
            else denied(preparation,"BUDGET_EXCEEDED","/artifact/expressions");
        }
    }

    @Test void localAndLoopbackNativeHostsPreserveAuthorityReceiptsAndNoRetry() throws Exception {
        for(String transport:List.of("local","loopback")) for(String scenario:List.of("confirmed","denied","wrong-key","lost-response")) {
            var now=new AtomicLong(); var registrations=implementations("real");var attempts=new AtomicInteger();var commits=new AtomicInteger();var current=new AtomicBoolean(true);
            Function<Map<String,Object>,Map<String,Object>> nativeCreate=body->{
                attempts.incrementAndGet();shape(body,Set.of("actor","event","source","token","expiresAtMs","projectId","title"),"");
                boolean permitted=current.get() && "native-token".equals(body.get("token")) && "user1".equals(body.get("actor")) &&
                        "event1".equals(body.get("event")) && "task1".equals(body.get("source")) && "project1".equals(body.get("projectId")) &&
                        "Follow up".equals(body.get("title")) && number(body.get("expiresAtMs"),0,Profile.MAX_INTEGER,"")>now.get();
                if(!permitted)return Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED","effect","NONE");
                commits.incrementAndGet();
                if(scenario.equals("lost-response"))throw new IllegalStateException("Response lost after native commit");
                return Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED");
            };
            HttpServer server=null;HttpClient client=transport.equals("loopback")?HttpClient.newHttpClient():null;
            try {
                if(client!=null) {
                    server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
                    server.createContext("/tasks",exchange->{
                        try {
                            assertTrue(exchange.getRequestMethod().equals("POST"),"Native service requires POST");
                            byte[] bytes=exchange.getRequestBody().readNBytes(1025);assertTrue(bytes.length<=1024,"Native request exceeds bound");
                            byte[] response=canonical(nativeCreate.apply(map(parseJson(bytes,1024),"")));
                            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
                            exchange.getResponseBody().write(response);
                        } catch(RuntimeException|AssertionError failure) {
                            // A lost response must not become definite no-effect evidence.
                        } finally {exchange.close();}
                    });
                    server.start();
                }
                URI endpoint=server==null?null:URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/tasks");
                var original=registrations.get("tasks.create");
                registrations.put("tasks.create",new KeirRuntime.BoundEffectAdapter(original.version(),original.mode(),original.keyType(),(args,protectedArgs,remainingMs,context)->{
                    assertTrue(context.requestKey()==KEY,"Original native key identity changed");assertTrue(remainingMs>0,"Native dispatch budget expired");
                    Map<String,Object> body=Map.of("actor",protectedArgs.get("actorId"),"event",context.event().get("id"),"source",context.event().get("source"),
                            "token",scenario.equals("wrong-key")?"untrusted":"native-token","expiresAtMs",context.deadline().expiresAtMs(),
                            "projectId",args.get("projectId"),"title",args.get("title"));
                    if(client==null)return CompletableFuture.supplyAsync(()->nativeCreate.apply(body));
                    var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(remainingMs)).header("Content-Type","application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(canonical(body))).build();
                    return client.sendAsync(request,HttpResponse.BodyHandlers.ofByteArray()).thenApply(response->{
                        assertTrue(response.statusCode()==200 && response.body().length<=1024,"Invalid native response");
                        return map(parseJson(response.body(),1024),"");
                    });
                }));
                var binding=bind(registrations);
                var preparation=prepare("main",binding);
                assertTrue(attempts.get()==0,"Initialization performed a native operation");current.set(!scenario.equals("denied"));
                var outcome=KeirRuntime.invokeBoundEffectProgram(preparation,Map.of("taskId","task1"),invocation(KEY,"user1","event1",new KeirRuntime.Deadline(now::get,1000),true)).toCompletableFuture().get(3,TimeUnit.SECONDS);
                String label=transport+"/"+scenario;
                assertTrue(attempts.get()==1,"Native attempts differ for "+label+": "+attempts.get());
                assertTrue(commits.get()==(scenario.equals("confirmed") || scenario.equals("lost-response")?1:0),"Native commits differ for "+label);
                assertTrue(outcome.effects().size()==1 && outcome.effects().getFirst().get("state").equals(scenario.equals("confirmed")?"CONFIRMED":scenario.equals("lost-response")?"UNKNOWN":"NONE"),"Native receipt differs for "+label);
                assertTrue(outcome.classification().equals(scenario.equals("confirmed")?"COMPLETED":scenario.equals("lost-response")?"UNCERTAIN":"BUSINESS_REJECTION"),"Native outcome differs for "+label);
            } finally {
                if(server!=null)server.stop(0);
                if(client!=null)client.close();
            }
        }
    }
}
