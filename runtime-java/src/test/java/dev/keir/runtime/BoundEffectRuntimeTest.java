package dev.keir.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
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
import static dev.keir.runtime.GenericRuntimeTest.*;

class BoundEffectRuntimeTest {
    @Test void pinnedCelEvaluatesTheLargestAdmittedConcatenationTreeTo8192Bytes() throws Exception {
        var artifact=document(file("effect4","artifact-title-max.json"));
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
        return map(parseJson(file("effect4","dcm.json"),Profile.DCM_BYTES),"");
    }
    private static Map<String,KeirRuntime.BoundEffectAdapter> implementations(String mode,KeirRuntime.BoundEffectFunction read,KeirRuntime.BoundEffectFunction write) {
        return new LinkedHashMap<>(Map.of("tasks.get",new KeirRuntime.BoundEffectAdapter("1",mode,KEY_TYPE,read),
                "tasks.create",new KeirRuntime.BoundEffectAdapter("1",mode,KEY_TYPE,write)));
    }
    private static Map<String,KeirRuntime.BoundEffectAdapter> implementations(String mode) {
        return implementations(mode,(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(confirmed()));
    }
    private static KeirRuntime.BoundModel bind(Map<String,KeirRuntime.BoundEffectAdapter> implementations) throws Exception {
        var result=KeirRuntime.bindModel(file("effect4","dcm.json"),implementations);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); assertNull(result.diagnostic()); return result.boundModel();
    }
    private static KeirRuntime.Prepared prepare(String program,KeirRuntime.BoundModel bound) throws Exception {
        var result=KeirRuntime.prepareProgram(file("effect4",program.equals("main")?"artifact.json":"artifact-"+program+".json"),bound);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); return result.prepared();
    }
    private static KeirRuntime.BoundEffectInvocation invocation(Object key,String actor,String event,KeirRuntime.Deadline deadline,boolean calls) {
        return new KeirRuntime.BoundEffectInvocation(calls?Map.of("actorId",actor):Map.of(),deadline,key,Map.of("id",event,"source","task1"));
    }
    private static KeirRuntime.BoundEffectInvocation invocation(boolean calls) {
        return invocation(KEY,"user1","event1",new KeirRuntime.Deadline(()->0,1000),calls);
    }
    private static KeirRuntime.Outcome invoke(KeirRuntime.Prepared prepared,boolean calls) throws Exception {
        return await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),invocation(calls)));
    }
    private static void receipt(KeirRuntime.Outcome outcome,String state) {
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

    @Test void effect4StructuralArtifactRequiresNativePreparation() throws Exception {
        Path fixtures=Path.of("..","conformance","effect4-structural");
        byte[] bytes=Files.readAllBytes(fixtures.resolve("artifact-handler.json"));
        assertEquals(Files.readString(fixtures.resolve("artifact-handler.sha256")).trim(),digest(bytes));
        var calls=new ArrayList<String>();
        var bound=bind(implementations("simulation",(a,h,ms,c)->{
            calls.add("tasks.get"); return done(effectRead("COMPLETED"));
        },(a,h,ms,c)->{
            calls.add("tasks.create"); return done(Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"));
        }));
        for (Object value:list(parseJson(Files.readAllBytes(fixtures.resolve("residuals.json")),Profile.ARTIFACT_BYTES),"")) {
            var row=map(value,"");
            var artifact=document(bytes);
            var path=list(row.get("path"),"");
            Object target=artifact;
            for (Object part:path.subList(0,path.size()-1)) {
                target=part instanceof Number index ? list(target,"").get(index.intValue()) : map(target,"").get(part);
            }
            map(target,"").put((String)path.getLast(),row.get("value"));
            denied(KeirRuntime.prepareProgram(canonical(artifact),bound),"ARTIFACT_INVALID",(String)row.get("javaPath"));
            assertTrue(calls.isEmpty(),(String)row.get("id"));
        }
        var prepared=KeirRuntime.prepareProgram(bytes,bound);
        assertTrue(prepared.succeeded(),()->String.valueOf(prepared.diagnostic()));
        assertTrue(calls.isEmpty());
        var outcome=invoke(prepared.prepared(),true);
        assertEquals("BUSINESS_REJECTION",outcome.classification());
        error(outcome,"CREATE_REJECTED","/steps/3");
        assertEquals(Map.of("status","NO_OP"),outcome.result());
        assertEquals(List.of("tasks.get","tasks.create"),calls);
        receipt(outcome,"NONE");
    }

    @Test void completeCatalogRequiresExactCompatibleRegistrationsIncludingUnusedCapabilities() throws Exception {
        var bytes=file("effect4","dcm.json");
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
        var wrong=new LinkedHashMap<String,Object>();wrong.putAll(effects((a,h,ms,c)->done(null),(a,h,ms,c)->done(null)));
        @SuppressWarnings({"rawtypes","unchecked"}) Map<String,KeirRuntime.BoundEffectAdapter> unsafe=(Map)wrong;
        bindingDenied(KeirRuntime.bindModel(file("effect4","dcm.json"),unsafe),"ADMISSION_INVALID","/binding/implementations/tasks.create");
        for(String old:List.of("read","effect","effect3"))
            bindingDenied(KeirRuntime.bindModel(file(old,"dcm.json"),implementations("simulation")),"ADMISSION_INVALID","/binding/dcm");
        var bytes=file("effect4","dcm.json");var legacy=approval("effect3","scope");
        legacy.put("profile","keir-effect/4");legacy.put("modelRef",Artifact.modelRef(model()));
        assertFalse(KeirRuntime.bindModel(bytes,legacy,effects((a,h,ms,c)->done(null),(a,h,ms,c)->done(null))).succeeded());
        assertFalse(KeirRuntime.bindModel(bytes,legacy,Map.of(),null).succeeded());
    }

    @Test void staticPreparationChecksCompilerEnvironmentModelAndApiFamily() throws Exception {
        var bound=bind(implementations("real"));var bytes=file("effect4","artifact.json");
        assertFalse(KeirRuntime.prepareProgram(bytes,bound,null).succeeded());
        assertFalse(KeirRuntime.prepareProgram(bytes,null).succeeded());
        assertFalse(KeirRuntime.prepareProgram(null,bound).succeeded());
        for(String field:List.of("compiler","environment","modelRef","format","profile")) {
            var artifact=document(bytes);artifact.put(field,field.equals("environment")||field.equals("modelRef")?Map.of():"unsupported");
            var result=KeirRuntime.prepareProgram(canonical(artifact),bound);
            denied(result,List.of("compiler","environment").contains(field)?"PIN_MISMATCH":"ARTIFACT_INVALID","/artifact/"+field);
        }
        var old=bound("effect3","scope",effects((a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(confirmed())));
        assertFalse(KeirRuntime.prepareProgram(bytes,old).succeeded());
        assertFalse(KeirRuntime.prepareProgram(file("effect3","artifact.json"),bound).succeeded());
        var prepared=prepare("main",bound);
        error(await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),null)),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeBoundEffectProgram(prepared,Map.of("taskId","task1"),null)),"HOST_CONTEXT_INVALID","");
        var oldPrepared=prepared("effect3","main",old);
        error(await(KeirRuntime.invokeBoundEffectProgram(oldPrepared,Map.of("taskId","task1"),invocation(true))),"HOST_CONTEXT_INVALID","");
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
        var artifact=document(file("effect4","artifact-no-call.json"));artifact.put("modelRef",Artifact.modelRef(dcm));
        var preparation=KeirRuntime.prepareProgram(canonical(artifact),binding.boundModel());assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic()));
        var result=invoke(preparation.prepared(),false);assertEquals("COMPLETED",result.classification());assertEquals(List.of(),result.effects());
        var call=document(file("effect4","artifact.json"));call.put("modelRef",Artifact.modelRef(dcm));
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
        var tooManySteps=document(file("effect4","artifact.json"));var steps=list(tooManySteps.get("steps"),"");
        while(steps.size()<9)steps.add(Map.of("op","BIND","id","extra"+steps.size(),"resultType","Boolean","value",Map.of("type","Boolean","literal",true)));
        denied(KeirRuntime.prepareProgram(canonical(tooManySteps),bound),"BUDGET_EXCEEDED","/artifact/steps");
        var secondWrite=document(file("effect4","artifact.json"));steps=list(secondWrite.get("steps"),"");var call=new LinkedHashMap<>(map(steps.get(3),""));call.put("id","secondWrite");steps.add(call);
        denied(KeirRuntime.prepareProgram(canonical(secondWrite),bound),"BUDGET_EXCEEDED","/artifact/steps/4");
        var oversized=document(file("effect4","artifact.json"));oversized.put("expressions",new LinkedHashMap<String,Object>());
        for(int i=0;i<513;i++)map(oversized.get("expressions"),"").put("e"+i,Map.of());
        assertFalse(KeirRuntime.prepareProgram(canonical(oversized),bound).succeeded());
        var hugeDcm=file("effect4","dcm.json");var padded=Arrays.copyOf(hugeDcm,65_537);Arrays.fill(padded,hugeDcm.length,padded.length,(byte)' ');
        bindingDenied(KeirRuntime.bindModel(padded,implementations("real")),"BUDGET_EXCEEDED","/binding/dcm");
        for(String field:List.of("capabilityPolicy","effectGrant")) {
            var dcm=model();var entry=map(map(dcm.get("entrypoints"),"").get("TaskCompleted"),"");
            entry.put(field,field.equals("effectGrant")?0L:Map.of("maxCalls",Map.of("tasks.create",0L)));
            var binding=KeirRuntime.bindModel(canonical(dcm),implementations("real"));assertTrue(binding.succeeded());
            var artifact=document(file("effect4","artifact.json"));artifact.put("modelRef",Artifact.modelRef(dcm));
            assertFalse(KeirRuntime.prepareProgram(canonical(artifact),binding.boundModel()).succeeded());
        }
    }

    @Test void fourCallsAndEightStepsPassButTheirNextValuesFailStatically() throws Exception {
        var bound=bind(implementations("real"));
        for(int count:List.of(4,5)) {
            var artifact=document(file("effect4","artifact-no-call.json"));artifact.put("expressions",Map.of());
            var steps=new ArrayList<Object>();
            for(int i=0;i<count;i++)steps.add(Map.of("op","CALL","id","read"+i,"capability","tasks.get","version","1","resultType","Task",
                    "args",Map.of("taskId",Map.of("type","TaskId","literal","task1"))));
            artifact.put("steps",steps);artifact.put("return",Map.of("type","FollowupResult","fields",Map.of("status",Map.of("type","FollowupStatus","literal","NO_OP"))));
            var preparation=KeirRuntime.prepareProgram(canonical(artifact),bound);
            if(count==4) {assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic()));assertEquals("COMPLETED",invoke(preparation.prepared(),true).classification());}
            else denied(preparation,"BUDGET_EXCEEDED","/artifact/steps/4");
        }
        for(int count:List.of(8,9)) {
            var artifact=document(file("effect4","artifact-no-call.json"));artifact.put("expressions",Map.of());
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

    @Test void checkedAstBudgetStillAppliesThroughPublicPreparation() throws Exception {
        var bound=bind(implementations("real"));
        for(int leaves:List.of(32,33)) {
            var artifact=document(file("effect4","artifact-no-call.json"));var checked=CheckedExpr.newBuilder().setSourceInfo(SourceInfo.newBuilder().setLocation("keir-read/1"));
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
        BoundEffectScenarioDriver.verifyNativeTransports();
    }
}
