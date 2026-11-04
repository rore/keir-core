package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;
import static dev.keir.runtime.GenericRuntimeTest.*;

class RealEffectAdmissionTest {
    private static final String QUALIFICATION="/binding/qualification";
    private static Map<String,Object> approved(String mode) throws Exception {
        var result=approval("effect3","scope"); result.put("mode",mode); return result;
    }
    private static Map<String,Object> evidence() throws Exception {
        var model=document(file("effect3","dcm.json"));
        var result=new LinkedHashMap<String,Object>();
        result.put("contract","keir-real-effect-evidence/1"); result.put("id","reviewed-host"); result.put("revision",1L);
        result.put("scope","scope"); result.put("modelRef",new LinkedHashMap<>(Artifact.modelRef(model)));
        result.put("environment",new LinkedHashMap<>(KeirRuntime.ENVIRONMENT)); result.put("hostBuild","sha256:"+"1".repeat(64));
        result.put("keyType",KEY_TYPE); result.put("reportDigest","sha256:"+"2".repeat(64));
        var caps=map(model.get("capabilities"),""); var catalog=new ArrayList<Object>();
        for(String id:new TreeSet<>(caps.keySet())) catalog.add(new LinkedHashMap<>(Map.of("id",id,"version",map(caps.get(id),"").get("version"))));
        result.put("capabilities",catalog); return result;
    }
    private static Map<String,KeirRuntime.EffectAdapter> invokers(String mode,KeirRuntime.EffectAdapterFunction get,KeirRuntime.EffectAdapterFunction create) {
        return new LinkedHashMap<>(Map.of("tasks.get",new KeirRuntime.EffectAdapter("1",mode,KEY_TYPE,get),
                "tasks.create",new KeirRuntime.EffectAdapter("1",mode,KEY_TYPE,create)));
    }
    private static Map<String,KeirRuntime.EffectAdapter> invokers(String mode) {
        return invokers(mode,(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(confirmed()));
    }
    private static KeirRuntime.ModelBinding bind(Map<String,Object> evidence,Supplier<?> current) throws Exception {
        return KeirRuntime.bindModel(file("effect3","dcm.json"),approved("real"),invokers("real"),new KeirRuntime.Qualification(evidence,current));
    }
    private static KeirRuntime.BoundModel bound(Map<String,KeirRuntime.EffectAdapter> invokers,Supplier<?> current) throws Exception {
        var binding=KeirRuntime.bindModel(file("effect3","dcm.json"),approved("real"),invokers,new KeirRuntime.Qualification(evidence(),current));
        assertTrue(binding.succeeded(),()->String.valueOf(binding.diagnostic())); return binding.boundModel();
    }
    private static Map<String,Object> admission3(byte[] bytes,String mode) {
        var result=admission("effect3",bytes); result.put("mode",mode); return result;
    }
    private static KeirRuntime.Prepared prepare(String program,KeirRuntime.BoundModel bound,String mode) throws Exception {
        byte[] bytes=file("effect3",program.equals("main")?"artifact.json":"artifact-"+program+".json");
        var preparation=KeirRuntime.prepareProgram(bytes,bound,admission3(bytes,mode));
        assertTrue(preparation.succeeded(),()->String.valueOf(preparation.diagnostic())); return preparation.prepared();
    }
    private static KeirRuntime.EffectInvocation2 invocation(KeirRuntime.Authorizer authorizer) {
        return effectInvocation("scope",Optional.empty(),OptionalLong.empty(),authorizer);
    }
    private static KeirRuntime.EffectInvocation2 timed(AtomicLong clock,KeirRuntime.Authorizer authorizer) {
        var normal=invocation(authorizer);
        return new KeirRuntime.EffectInvocation2(normal.scope(),normal.bindings(),new KeirRuntime.Deadline(clock::get,1000),normal.authorize(),
                normal.capabilityPolicy(),normal.effectGrant(),normal.requestKey(),normal.event());
    }
    private static KeirRuntime.Outcome invoke(KeirRuntime.Prepared prepared) throws Exception {
        return await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation(r->done(true))));
    }
    private static void noIntent(KeirRuntime.Outcome result) {
        assertEquals(List.of(),result.effects()); assertTrue(result.trace().stream().noneMatch(e->"EFFECT_INTENT".equals(e.get("event"))));
    }

    @Test void sameArtifactExecutesInSeparatelyAdmittedModesWithOriginalProtectedIdentity() throws Exception {
        for(String mode:List.of("simulation","real")) {
            var calls=new ArrayList<String>();
            var invokers=invokers(mode,(a,h,ms,c)->{calls.add("READ");assertSame(KEY,c.requestKey());assertEquals(Map.of("actorId","user1"),h);return done(effectRead("COMPLETED"));},
                    (a,h,ms,c)->{calls.add("WRITE");assertSame(KEY,c.requestKey());assertEquals(Map.of("id","event1","source","task1"),c.event());assertEquals(Map.of("projectId","project1","title","Follow up"),a);return done(confirmed());});
            var binding=mode.equals("real")?bound(invokers,()->true):KeirRuntime.bindModel(file("effect3","dcm.json"),approved(mode),invokers).boundModel();
            assertNotNull(binding);
            for(String program:List.of("main","zero","after-write-read")) {
                calls.clear();var outcome=invoke(prepare(program,binding,mode)); assertEquals("COMPLETED",outcome.classification());
                assertEquals(program.equals("zero")?List.of():List.of("CONFIRMED"),outcome.effects().stream().map(e->e.get("state")).toList());
                assertEquals(program.equals("main")?List.of("READ","WRITE"):program.equals("zero")?List.of("READ"):List.of("READ","WRITE","READ"),calls);
            }
        }
    }

    @Test void optionalQualificationDistinguishesOmissionNullAndForbiddenProfiles() throws Exception {
        var bytes=file("effect3","dcm.json");var qualification=new KeirRuntime.Qualification(evidence(),()->true);
        bindingDenied(KeirRuntime.bindModel(bytes,approved("real"),invokers("real")),"ADMISSION_INVALID",QUALIFICATION);
        bindingDenied(KeirRuntime.bindModel(bytes,approved("real"),invokers("real"),null),"ADMISSION_INVALID",QUALIFICATION);
        assertTrue(KeirRuntime.bindModel(bytes,approved("simulation"),invokers("simulation")).succeeded());
        for(var supplied:Arrays.asList(null,qualification)) {
            bindingDenied(KeirRuntime.bindModel(bytes,approved("simulation"),invokers("simulation"),supplied),"ADMISSION_INVALID",QUALIFICATION);
            bindingDenied(KeirRuntime.bindModel(file("effect","dcm.json"),approval("effect","scope"),effects((a,h,ms,c)->done(true),(a,h,ms,c)->done(true)),supplied),"ADMISSION_INVALID",QUALIFICATION);
            bindingDenied(KeirRuntime.bindModel(file("read","dcm.json"),approval("read","scope"),reads((a,h,ms)->done(true)),supplied),"ADMISSION_INVALID",QUALIFICATION);
        }
        var old=approval("effect","scope"); old.put("mode","real");
        bindingDenied(KeirRuntime.bindModel(file("effect","dcm.json"),old,invokers("real")),"ADMISSION_INVALID","/binding/approval/mode");
        for(String mode:List.of("real","simulation"))
            bindingDenied(KeirRuntime.bindModel(bytes,approved(mode),invokers(mode.equals("real")?"simulation":"real"),mode.equals("real")?qualification:null),
                    "ADMISSION_INVALID",mode.equals("real")?"/binding/invokers/tasks.create":QUALIFICATION);
        bindingDenied(KeirRuntime.bindModel(bytes,approved("simulation"),invokers("real")),"ADMISSION_INVALID","/binding/invokers/tasks.create");
    }

    @Test void evidenceIsClosedExactBoundedAndCompleteIncludingUnusedCapabilities() throws Exception {
        AtomicInteger callbacks=new AtomicInteger(); Supplier<?> current=()->{callbacks.incrementAndGet();return true;};
        for(String field:evidence().keySet()) {
            var missing=evidence(); missing.remove(field);bindingDenied(bind(missing,current),"ADMISSION_INVALID",QUALIFICATION);
        }
        var extra=evidence();extra.put("extra",true);bindingDenied(bind(extra,current),"ADMISSION_INVALID",QUALIFICATION);
        var invalidValues=new LinkedHashMap<String,List<Object>>();
        invalidValues.put("contract",List.of("wrong")); invalidValues.put("id",List.of("","x".repeat(129),"é".repeat(65),"\ud800"));
        invalidValues.put("revision",List.of(0L,-1L,9007199254740992L,1.5,true)); invalidValues.put("scope",List.of("wrong",""));
        invalidValues.put("modelRef",List.of(Map.of(),Map.of("digest","sha256:"+"0".repeat(64))));
        invalidValues.put("environment",List.of(Map.of())); invalidValues.put("hostBuild",List.of("wrong","sha256:"+"A".repeat(64)));
        invalidValues.put("reportDigest",List.of("sha256:"+"0".repeat(63))); invalidValues.put("keyType",List.of("wrong"));
        invalidValues.put("capabilities",List.of(List.of(),List.of(Map.of("id","tasks.create","version","1")),
                List.of(Map.of("id","tasks.get","version","1")),
                List.of(Map.of("id","tasks.get","version","1"),Map.of("id","tasks.create","version","1")),
                List.of(Map.of("id","tasks.create","version","2"),Map.of("id","tasks.get","version","1")),
                List.of(Map.of("id","tasks.create","version","1","extra",true),Map.of("id","tasks.get","version","1"))));
        for(var field:invalidValues.entrySet()) for(Object value:field.getValue()) {
            var invalid=evidence();invalid.put(field.getKey(),value);bindingDenied(bind(invalid,current),"ADMISSION_INVALID",QUALIFICATION);
        }
        var tooLarge=evidence();tooLarge.put("id","x".repeat(16384));bindingDenied(bind(tooLarge,current),"BUDGET_EXCEEDED",QUALIFICATION);
        tooLarge.put("id","\u0001".repeat(3000));bindingDenied(bind(tooLarge,current),"BUDGET_EXCEEDED",QUALIFICATION);
        bindingDenied(bind(null,current),"ADMISSION_INVALID",QUALIFICATION);bindingDenied(bind(evidence(),null),"ADMISSION_INVALID",QUALIFICATION);
        assertEquals(0,callbacks.get());
        assertTrue(bind(evidence(),current).succeeded());assertEquals(1,callbacks.get());
    }

    @Test void callbackRequiresBooleanTrueByValueAndContainsThrowableAndAsyncResults() throws Exception {
        for(Supplier<?> current:List.<Supplier<?>>of(()->false,()->null,()->"true",()->done(true),()->{throw new IllegalStateException("private");},()->{throw new AssertionError("private");})) {
            bindingDenied(bind(evidence(),current),"ADMISSION_INVALID",QUALIFICATION);
            AtomicBoolean stale=new AtomicBoolean();
            var bound=bound(invokers("real"),()->stale.get()?current.get():true);var prepared=prepare("main",bound,"real");stale.set(true);
            var bytes=file("effect3","artifact.json");denied(KeirRuntime.prepareProgram(bytes,bound,admission3(bytes,"real")),"ADMISSION_INVALID","/admission/qualification");
            AtomicInteger auth=new AtomicInteger();
            var result=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation(r->{auth.incrementAndGet();return done(true);})));
            error(result,"AUTHORITY_DENIED","");assertEquals(0,auth.get());noIntent(result);
        }
        @SuppressWarnings("removal") Boolean boxed=new Boolean(true);
        assertTrue(bind(evidence(),()->boxed).succeeded());
    }

    @Test void bindingSnapshotsEvidenceAndRegistrationsBeforeCurrentCallback() throws Exception {
        var evidence=evidence();var registrations=invokers("real");var approval=approved("real");AtomicInteger checks=new AtomicInteger();
        var binding=KeirRuntime.bindModel(file("effect3","dcm.json"),approval,registrations,new KeirRuntime.Qualification(evidence,()->{
            checks.incrementAndGet();evidence.clear();registrations.clear();approval.clear();return true;
        }));
        assertTrue(binding.succeeded(),()->String.valueOf(binding.diagnostic()));
        assertEquals("COMPLETED",invoke(prepare("main",binding.boundModel(),"real")).classification());assertTrue(checks.get()>2);
        AtomicBoolean current=new AtomicBoolean(true);var first=bound(invokers("real"),current::get);var prepared=prepare("main",first,"real");
        var other=bound(invokers("real"),()->true);assertEquals("COMPLETED",invoke(prepare("main",other,"real")).classification());
        current.set(false);error(invoke(prepared),"AUTHORITY_DENIED","");
    }

    @Test void invokerValidationAndExactPreparationPrecedeCurrentCallback() throws Exception {
        AtomicInteger checks=new AtomicInteger();Supplier<?> current=()->{checks.incrementAndGet();return true;};
        var wrong=invokers("real");wrong.remove("tasks.get");
        bindingDenied(KeirRuntime.bindModel(file("effect3","dcm.json"),approved("real"),wrong,new KeirRuntime.Qualification(evidence(),current)),"ADMISSION_INVALID","/binding/invokers");
        assertEquals(0,checks.get());var bound=bound(invokers("real"),current);assertEquals(1,checks.get());
        var bytes=file("effect3","artifact.json");var admission=admission3(bytes,"real");
        denied(KeirRuntime.prepareProgram(new byte[]{123},bound,admission),"PIN_MISMATCH","/artifact");
        for(String field:List.of("compiler","consumer","entrypoint","environment","expectedDcmDigest","mode")) {
            var bad=admission3(bytes,"real");bad.put(field,"wrong");denied(KeirRuntime.prepareProgram(bytes,bound,bad),"ADMISSION_INVALID","/admission/"+field);
        }
        for(String field:List.of("profile","format","modelRef")) {
            var artifact=document(bytes);artifact.put(field,field.equals("modelRef")?Map.of():"wrong");var forged=canonical(artifact);
            denied(KeirRuntime.prepareProgram(forged,bound,admission3(forged,"real")),"ARTIFACT_INVALID","/artifact/"+field);
        }
        assertEquals(1,checks.get());assertEquals("COMPLETED",invoke(prepare("main",bound,"real")).classification());
    }

    @Test void revocationAtEntryAndDuringKeyValidationPreventsAuthorization() throws Exception {
        AtomicBoolean current=new AtomicBoolean(true);AtomicInteger auth=new AtomicInteger(),keys=new AtomicInteger(),calls=new AtomicInteger();
        var approval=approved("real");approval.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,key->{keys.incrementAndGet();current.set(false);return true;}));
        var registration=invokers("real",(a,h,ms,c)->{calls.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->{calls.incrementAndGet();return done(confirmed());});
        var binding=KeirRuntime.bindModel(file("effect3","dcm.json"),approval,registration,new KeirRuntime.Qualification(evidence(),current::get));
        var prepared=prepare("main",binding.boundModel(),"real");current.set(false);
        var result=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation(r->{auth.incrementAndGet();return done(true);})));
        error(result,"AUTHORITY_DENIED","");assertEquals(0,keys.get());noIntent(result);
        current.set(true);result=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation(r->{auth.incrementAndGet();return done(true);})));
        error(result,"AUTHORITY_DENIED","");assertEquals(1,keys.get());assertEquals(0,auth.get());assertEquals(0,calls.get());noIntent(result);
    }

    @Test void zeroCeilingAndFalseGuardNeverReserveAWriteAndForgedSecondWriteFailsPreparation() throws Exception {
        AtomicBoolean completed=new AtomicBoolean();AtomicInteger reads=new AtomicInteger(),writes=new AtomicInteger();
        var bound=bound(invokers("real",(a,h,ms,c)->{reads.incrementAndGet();return done(effectRead(completed.get()?"COMPLETED":"OPEN"));},
                (a,h,ms,c)->{writes.incrementAndGet();return done(confirmed());}),()->true);
        var prepared=prepare("main",bound,"real");var zero=effectInvocation("scope",Optional.empty(),OptionalLong.of(0),r->done(true));
        var skipped=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),zero));
        assertEquals(Map.of("status","NO_OP"),skipped.result());noIntent(skipped);
        completed.set(true);var denied=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),zero));
        error(denied,"AUTHORITY_DENIED","/steps/3");noIntent(denied);assertEquals(2,reads.get());assertEquals(0,writes.get());
        var bytes=file("effect3","artifact-zero.json");var admission=admission3(bytes,"real");admission.put("effectGrant",0L);
        var preparation=KeirRuntime.prepareProgram(bytes,bound,admission);assertTrue(preparation.succeeded());assertEquals("COMPLETED",invoke(preparation.prepared()).classification());
        var artifact=document(file("effect3","artifact.json"));var steps=list(artifact.get("steps"),"");
        var second=new LinkedHashMap<>(map(steps.get(3),""));second.put("id","secondWrite");steps.add(second);bytes=canonical(artifact);
        denied(KeirRuntime.prepareProgram(bytes,bound,admission3(bytes,"real")),"BUDGET_EXCEEDED","/artifact/steps/4");
    }

    @Test void revocationDuringPendingReadOrWriteAuthorizationDeniesBeforeDispatch() throws Exception {
        for(String pendingCapability:List.of("tasks.get","tasks.create")) {
            AtomicBoolean current=new AtomicBoolean(true);AtomicInteger reads=new AtomicInteger(),writes=new AtomicInteger();var pending=new CompletableFuture<Object>();
            var entered=new CountDownLatch(1);
            var bound=bound(invokers("real",(a,h,ms,c)->{reads.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->{writes.incrementAndGet();return done(confirmed());}),current::get);
            var outstanding=KeirRuntime.invokeEffectProgram(prepare("main",bound,"real"),Map.of("taskId","task1"),invocation(r->{
                if(pendingCapability.equals(r.get("capability"))) {entered.countDown();return pending;}return done(true);
            }));
            assertTrue(entered.await(2,TimeUnit.SECONDS));current.set(false);pending.complete(true);var result=await(outstanding);
            error(result,"AUTHORITY_DENIED",pendingCapability.equals("tasks.get")?"/steps/1":"/steps/3");
            assertEquals(pendingCapability.equals("tasks.get")?0:1,reads.get());assertEquals(0,writes.get());noIntent(result);
        }
    }

    @Test void qualificationCallbackConsumesFreshDispatchBudgetAndCannotReserveAnExpiredWrite() throws Exception {
        for(boolean expires:List.of(false,true)) {
            AtomicLong clock=new AtomicLong();AtomicBoolean afterAuthorization=new AtomicBoolean();AtomicInteger writes=new AtomicInteger();
            var bound=bound(invokers("real",(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->{writes.incrementAndGet();assertEquals(750,ms);return done(confirmed());}),()->{
                if(afterAuthorization.compareAndSet(true,false)) clock.set(expires?1000:250);return true;
            });
            var result=await(KeirRuntime.invokeEffectProgram(prepare("main",bound,"real"),Map.of("taskId","task1"),timed(clock,r->{
                if("tasks.create".equals(r.get("capability"))) afterAuthorization.set(true);return done(true);
            })));
            if(expires) {error(result,"BUDGET_EXCEEDED","/steps/3");assertEquals(0,writes.get());noIntent(result);}
            else {assertEquals("COMPLETED",result.classification());assertEquals(1,writes.get());}
        }
    }

    @Test void deadlineIsRecheckedAfterQualificationFailureBeforeEntryAuthorization() throws Exception {
        for(boolean throwsFailure:List.of(false,true)) {
            AtomicLong clock=new AtomicLong();AtomicBoolean expire=new AtomicBoolean();AtomicInteger auth=new AtomicInteger();
            var bound=bound(invokers("real"),()->{if(expire.get()) {clock.set(1000);if(throwsFailure) throw new AssertionError("private");return false;}return true;});
            var prepared=prepare("main",bound,"real");expire.set(true);
            var result=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),timed(clock,r->{auth.incrementAndGet();return done(true);})));
            error(result,"BUDGET_EXCEEDED","");assertEquals(0,auth.get());noIntent(result);
        }
    }

    @Test void callbackFailureAfterConfirmedWriteKeepsEvidenceAndBlocksTheNextRead() throws Exception {
        for(boolean throwsFailure:List.of(false,true)) {
            AtomicBoolean revoked=new AtomicBoolean();AtomicInteger reads=new AtomicInteger();
            var bound=bound(invokers("real",(a,h,ms,c)->{reads.incrementAndGet();return done(effectRead("COMPLETED"));},
                    (a,h,ms,c)->{revoked.set(true);return done(confirmed());}),()->{if(revoked.get() && throwsFailure) throw new AssertionError("private");return !revoked.get();});
            var result=invoke(prepare("after-write-read",bound,"real"));
            assertEquals("AUTHORITY_DENIED",result.error().get("code"));assertEquals("FAILED",result.classification());assertEquals(1,reads.get());
            assertEquals("CONFIRMED",result.effects().get(0).get("state"));assertEquals(1,result.trace().stream().filter(e->"EFFECT_RECORDED".equals(e.get("event"))).count());
        }
    }

    @Test void revocationDoesNotInterfereWithDispatchedResponseSettlement() throws Exception {
        for(var response:List.of(confirmed(),Map.of("kind","SUCCESS","value",Map.of("bad",true),"effect","CONFIRMED"),
                Map.of("kind","BUSINESS_REJECTION","code","KEY_PAYLOAD_MISMATCH","effect","NONE"),Map.of("kind","TRANSIENT_FAILURE","effect","NONE"),
                Map.of("kind","UNKNOWN_COMMIT","effect","UNKNOWN"),Map.of("kind","SUCCESS","value",Map.of("id","task2")))) {
            AtomicBoolean current=new AtomicBoolean(true);
            var bound=bound(invokers("real",(a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->{current.set(false);return done(response);}),current::get);
            var result=invoke(prepare("main",bound,"real"));String kind=(String)response.get("kind");
            String state=response.containsKey("effect")?(String)response.get("effect"):"UNKNOWN";
            assertEquals(state,result.effects().get(0).get("state"));
            if(response.equals(confirmed())) assertEquals("COMPLETED",result.classification());
            else if(kind.equals("BUSINESS_REJECTION")) assertEquals("BUSINESS_REJECTION",result.classification());
            else if(state.equals("UNKNOWN")) assertEquals("UNCERTAIN",result.classification());
            else assertEquals("FAILED",result.classification());
        }
    }
}
