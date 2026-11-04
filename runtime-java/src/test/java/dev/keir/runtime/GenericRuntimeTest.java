package dev.keir.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class GenericRuntimeTest {
    static final String KEY_TYPE="sample.RequestUuid/1";
    static final UUID KEY=UUID.fromString("00000000-0000-4000-8000-000000000001");
    static byte[] file(String profile,String file) throws IOException {
        try(var stream=GenericRuntimeTest.class.getResourceAsStream("/generic-hooks/"+profile+"/"+file)) {
            assertNotNull(stream,file); return stream.readAllBytes();
        }
    }
    static Map<String,Object> document(byte[] bytes) { return map(parseCanonical(bytes),""); }
    static Map<String,Object> approval(String profile,String scope) throws IOException {
        var result=new LinkedHashMap<String,Object>();
        result.put("scope",scope); result.put("profile","keir-"+profile+"/2");
        result.put("modelRef",Artifact.modelRef(document(file(profile,"dcm.json"))));
        result.put("environment",KeirRuntime.ENVIRONMENT); result.put("mode","simulation");
        if(profile.equals("effect")) result.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,key->key instanceof UUID));
        return result;
    }
    static Map<String,Object> admission(String profile,byte[] bytes) {
        var artifact=document(bytes); var result=new LinkedHashMap<String,Object>();
        result.put("expectedArtifactDigest",digest(bytes)); result.put("expectedDcmDigest",map(artifact.get("modelRef"),"").get("digest"));
        result.put("compiler",artifact.get("compiler")); result.put("environment",KeirRuntime.ENVIRONMENT);
        result.put("entrypoint",artifact.get("entrypoint")); result.put("mode","simulation");
        result.put("consumer",profile.equals("effect")?Map.of("id","sample.followup.report","version","1","kind","EVENT_REPORT"):
                Map.of("id","sample.classification","version","1","kind","DISPLAY")); return result;
    }
    static CompletableFuture<Object> done(Object value) { return CompletableFuture.completedFuture(value); }
    static KeirRuntime.Outcome await(CompletionStage<KeirRuntime.Outcome> value) throws Exception { return value.toCompletableFuture().get(3,TimeUnit.SECONDS); }
    static Map<String,Object> read(String status) { return Map.of("kind","SUCCESS","value",Map.of("id","task1","status",status)); }
    static Map<String,Object> effectRead(String status) { return Map.of("kind","SUCCESS","value",Map.of("id","task1","projectId","project1","status",status)); }
    static Map<String,Object> confirmed() { return Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED"); }
    static Map<String,KeirRuntime.Adapter> reads(KeirRuntime.AdapterFunction callback) {
        var result=new LinkedHashMap<String,KeirRuntime.Adapter>();
        for(String id:List.of("tasks.get","tasks.lookup","tasks.create")) result.put(id,new KeirRuntime.Adapter("1","simulation",callback));
        return result;
    }
    static Map<String,KeirRuntime.EffectAdapter> effects(KeirRuntime.EffectAdapterFunction get,KeirRuntime.EffectAdapterFunction create) {
        return new LinkedHashMap<>(Map.of("tasks.get",new KeirRuntime.EffectAdapter("1","simulation",KEY_TYPE,get),
                "tasks.create",new KeirRuntime.EffectAdapter("1","simulation",KEY_TYPE,create)));
    }
    static KeirRuntime.BoundModel bound(String profile,String scope,Map<String,?> adapters) throws IOException {
        var result=KeirRuntime.bindModel(file(profile,"dcm.json"),approval(profile,scope),adapters);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); assertNull(result.diagnostic()); return result.boundModel();
    }
    static KeirRuntime.Prepared prepared(String profile,String program,KeirRuntime.BoundModel bound) throws IOException {
        byte[] bytes=file(profile,program.equals("main")?"artifact.json":"artifact-"+program+".json");
        var result=KeirRuntime.prepareProgram(bytes,bound,admission(profile,bytes));
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); return result.prepared();
    }
    static KeirRuntime.Invocation2 readInvocation(String scope,Optional<Map<String,Object>> policy,KeirRuntime.Authorizer authorize) {
        return new KeirRuntime.Invocation2(scope,Map.of("actorId","user1"),new KeirRuntime.Deadline(()->0,1000),authorize,policy);
    }
    static KeirRuntime.EffectInvocation2 effectInvocation(String scope,Optional<Map<String,Object>> policy,OptionalLong grant,KeirRuntime.Authorizer authorize) {
        return new KeirRuntime.EffectInvocation2(scope,Map.of("actorId","user1"),new KeirRuntime.Deadline(()->0,1000),authorize,policy,grant,KEY,Map.of("id","event1","source","task1"));
    }
    static void error(KeirRuntime.Outcome outcome,String code,String path) {
        assertEquals(code,outcome.error().get("code")); assertEquals(path,outcome.error().get("path"));
    }
    static void denied(KeirRuntime.Preparation value,String code,String path) {
        assertFalse(value.succeeded()); assertNull(value.prepared()); assertEquals(code,value.diagnostic().code()); assertEquals(path,value.diagnostic().path());
    }
    static void bindingDenied(KeirRuntime.ModelBinding value,String code,String path) {
        assertFalse(value.succeeded()); assertNull(value.boundModel()); assertEquals(code,value.diagnostic().code()); assertEquals(path,value.diagnostic().path());
    }

    @Test void bindingChecksDeclaredErrorBoundaryAcrossTheCompleteCatalog() throws Exception {
        for(String profile:List.of("read","effect")) for(String id:List.of("tasks.get","tasks.create")) {
            var model=document(file(profile,"dcm.json"));
            var capability=map(map(model.get("capabilities"),"").get(id),"");
            var errors=new ArrayList<>((List<String>)capability.get("errors"));
            while(errors.size()<16) errors.add("ERROR"+errors.size());
            capability.put("errors",errors);
            AtomicInteger calls=new AtomicInteger();
            Map<String,?> invokers=profile.equals("read")
                ? reads((a,h,ms)->{calls.incrementAndGet();return done(null);})
                : effects((a,h,ms,c)->{calls.incrementAndGet();return done(null);},(a,h,ms,c)->{calls.incrementAndGet();return done(null);});
            for(int count:List.of(16,17)) {
                if(count==17) errors.add("EXTRA_ERROR");
                var approved=approval(profile,"scope"); approved.put("modelRef",Artifact.modelRef(model));
                var result=KeirRuntime.bindModel(canonical(model),approved,invokers);
                if(count==16) { assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); assertNull(result.diagnostic()); }
                else bindingDenied(result,"BUDGET_EXCEEDED","/binding/dcm");
                assertEquals(0,calls.get());
            }
        }
    }

    @Test void completeReadCatalogSupportsOtherAndRepeatedReadsWithNoNamedPolicy() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        var bound=bound("read","scope",reads((a,h,ms)->{calls.incrementAndGet();assertEquals(Map.of("actorId","user1"),h);return done(read("COMPLETED"));}));
        for(String program:List.of("main","repeated","alternate")) {
            var result=await(KeirRuntime.invokeProgram(prepared("read",program,bound),Map.of("taskId","task1"),readInvocation("scope",Optional.empty(),r->done(true))));
            assertEquals("COMPLETED",result.classification()); assertEquals(Map.of("classification","DONE"),result.result());
        }
        assertEquals(4,calls.get());
    }

    @Test void authoredProjectAndTitleVariantsReuseOneBindingWithProtectedOriginalKey() throws Exception {
        var captured=new ArrayList<Map<String,Object>>();
        var bound=bound("effect","scope",effects((a,h,ms,c)->{assertSame(KEY,c.requestKey());return done(effectRead("COMPLETED"));},
                (a,h,ms,c)->{assertSame(KEY,c.requestKey());assertEquals(Map.of("id","event1","source","task1"),c.event());captured.add(a);return done(confirmed());}));
        for(String program:List.of("main","project2","title2")) {
            var result=await(KeirRuntime.invokeEffectProgram(prepared("effect",program,bound),Map.of("taskId","task1"),effectInvocation("scope",Optional.empty(),OptionalLong.empty(),r->done(true))));
            assertEquals("COMPLETED",result.classification());
            assertEquals(Map.of("status",program.equals("project2")?"NO_OP":"CREATED"),result.result());
        }
        assertEquals(List.of(Map.of("projectId","project1","title","Follow up"),Map.of("projectId","project1","title","Follow up again")),captured);
    }

    @Test void bindingRequiresEveryInvokerAndSnapshotsRegistrationWithoutCallbacks() throws Exception {
        AtomicInteger calls=new AtomicInteger(); var registrations=reads((a,h,ms)->{calls.incrementAndGet();return done(read("COMPLETED"));});
        var bound=bound("read","scope",registrations); assertEquals(0,calls.get());
        registrations.clear();
        var result=await(KeirRuntime.invokeProgram(prepared("read","main",bound),Map.of("taskId","task1"),readInvocation("scope",Optional.empty(),r->done(true))));
        assertEquals("COMPLETED",result.classification()); assertEquals(1,calls.get());
        var missing=reads((a,h,ms)->done(read("COMPLETED"))); missing.remove("tasks.create");
        bindingDenied(KeirRuntime.bindModel(file("read","dcm.json"),approval("read","scope"),missing),"ADMISSION_INVALID","/binding/invokers");
        var extra=reads((a,h,ms)->done(read("COMPLETED"))); extra.put("tasks.other",extra.get("tasks.get"));
        bindingDenied(KeirRuntime.bindModel(file("read","dcm.json"),approval("read","scope"),extra),"ADMISSION_INVALID","/binding/invokers");
        for(var bad:List.of(new KeirRuntime.Adapter("2","simulation",(a,h,ms)->done(true)),new KeirRuntime.Adapter("1","real",(a,h,ms)->done(true)),new KeirRuntime.Adapter("1","simulation",null))) {
            var invokers=reads((a,h,ms)->done(true)); invokers.put("tasks.create",bad);
            bindingDenied(KeirRuntime.bindModel(file("read","dcm.json"),approval("read","scope"),invokers),"ADMISSION_INVALID","/binding/invokers/tasks.create");
        }
    }

    @Test void bindingDiagnosticsFollowApprovalModelReferenceAndRegistrationOrder() throws Exception {
        var approval=approval("read","scope"); approval.put("unknown",true);
        bindingDenied(KeirRuntime.bindModel(null,approval,Map.of()),"ADMISSION_INVALID","/binding/approval");
        for(String field:List.of("environment","mode","scope")) {
            var invalid=approval("read","scope"); invalid.put(field,"wrong"); if(field.equals("scope")) invalid.put(field,"");
            bindingDenied(KeirRuntime.bindModel(null,invalid,Map.of()),"ADMISSION_INVALID","/binding/approval/"+field);
        }
        var recipe=approval("read","scope"); var ref=new LinkedHashMap<>(map(recipe.get("modelRef"),"")); ref.put("descriptorRecipe","unknown");recipe.put("modelRef",ref);
        bindingDenied(KeirRuntime.bindModel(null,recipe,Map.of()),"ADMISSION_INVALID","/binding/approval/modelRef/descriptorRecipe");
        bindingDenied(KeirRuntime.bindModel(null,approval("read","scope"),Map.of()),"ADMISSION_INVALID","/binding/dcm");
        bindingDenied(KeirRuntime.bindModel(new byte[65537],approval("read","scope"),Map.of()),"BUDGET_EXCEEDED","/binding/dcm");
        var mismatch=approval("read","scope"); var wrongRef=new LinkedHashMap<>(map(mismatch.get("modelRef"),"")); wrongRef.put("digest","sha256:"+"0".repeat(64));mismatch.put("modelRef",wrongRef);
        bindingDenied(KeirRuntime.bindModel(file("read","dcm.json"),mismatch,Map.of()),"ADMISSION_INVALID","/binding/approval/modelRef");
    }

    @Test void effectBindingValidatesKeyMetadataWithoutExecutingValidator() throws Exception {
        AtomicInteger validations=new AtomicInteger();
        var approval=approval("effect","scope");approval.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,key->{validations.incrementAndGet();return true;}));
        var valid=effects((a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(confirmed()));
        assertTrue(KeirRuntime.bindModel(file("effect","dcm.json"),approval,valid).succeeded());assertEquals(0,validations.get());
        for(var binding:List.of(new KeirRuntime.KeyBinding("wrong",key->true),new KeirRuntime.KeyBinding(KEY_TYPE,null))) {
            var invalid=approval("effect","scope");invalid.put("keyBinding",binding);
            bindingDenied(KeirRuntime.bindModel(file("effect","dcm.json"),invalid,valid),"ADMISSION_INVALID","/binding/approval/keyBinding");
        }
        for(var registration:List.of(new KeirRuntime.EffectAdapter("1","simulation","wrong",(a,h,ms,c)->done(true)),
                new KeirRuntime.EffectAdapter("1","simulation",KEY_TYPE,null),new KeirRuntime.EffectAdapter("2","simulation",KEY_TYPE,(a,h,ms,c)->done(true)))) {
            var invalid=new LinkedHashMap<>(valid);invalid.put("tasks.get",registration);
            bindingDenied(KeirRuntime.bindModel(file("effect","dcm.json"),approval,invalid),"ADMISSION_INVALID","/binding/invokers/tasks.get");
        }
        assertEquals(0,validations.get());
    }

    @Test void preparationAuthenticatesRawBytesBeforeParsingAndEnforcesClosedAdmission() throws Exception {
        var bound=bound("read","scope",reads((a,h,ms)->done(read("COMPLETED")))); byte[] bytes=file("read","artifact.json");
        denied(KeirRuntime.prepareProgram(bytes,null,Map.of()),"ADMISSION_INVALID","/binding");
        for(String field:List.of("grants","keyBinding","scope","profile","invokers")) {
            var invalid=admission("read",bytes);invalid.put(field,Map.of());denied(KeirRuntime.prepareProgram(bytes,bound,invalid),"ADMISSION_INVALID","/admission");
        }
        denied(KeirRuntime.prepareProgram(new byte[]{123},bound,admission("read",bytes)),"PIN_MISMATCH","/artifact");
        denied(KeirRuntime.prepareProgram(null,bound,admission("read",bytes)),"ARTIFACT_INVALID","/artifact");
        for(String field:List.of("compiler","consumer","entrypoint","environment","expectedDcmDigest","mode")) {
            var invalid=admission("read",bytes);invalid.put(field,"wrong");
            denied(KeirRuntime.prepareProgram(bytes,bound,invalid),"ADMISSION_INVALID","/admission/"+field);
        }
    }

    @Test void unknownStringCapabilityAndMalformedCapabilityHaveDistinctGenericDiagnostics() throws Exception {
        for(String profile:List.of("read","effect")) {
            var model=profile.equals("read")?bound("read","scope",reads((a,h,ms)->done(true))):
                    bound("effect","scope",effects((a,h,ms,c)->done(true),(a,h,ms,c)->done(true)));
            int index=profile.equals("read")?0:1;
            for(Object capability:List.of("tasks.missing",false,Map.of("invalid",true))) {
                var artifact=document(file(profile,"artifact.json"));
                map(list(artifact.get("steps"),"").get(index),"").put("capability",capability);
                byte[] bytes=canonical(artifact);
                denied(KeirRuntime.prepareProgram(bytes,model,admission(profile,bytes)),
                        capability instanceof String?"CAPABILITY_UNKNOWN":"ARTIFACT_INVALID","/artifact/steps/"+index);
            }
        }
    }

    @Test void admissionAndInvocationPolicyOnlyNarrowAndOmissionInherits() throws Exception {
        AtomicInteger calls=new AtomicInteger(); var bound=bound("read","scope",reads((a,h,ms)->{calls.incrementAndGet();return done(read("COMPLETED"));}));
        byte[] bytes=file("read","artifact-repeated.json");
        for(Object policy:Arrays.asList(null,Map.of("allow",List.of()),Map.of("maxCalls",Map.of("tasks.get",0L)),Map.of("allow",List.of("unknown")),Map.of("maxCalls",Map.of("tasks.get",5L)))) {
            var selected=admission("read",bytes); selected.put("capabilityPolicy",policy);
            String code=policy instanceof Map<?,?> p && (p.equals(Map.of("allow",List.of())) || p.equals(Map.of("maxCalls",Map.of("tasks.get",0L))))?"AUTHORITY_DENIED":"ADMISSION_INVALID";
            denied(KeirRuntime.prepareProgram(bytes,bound,selected),code,"/admission/capabilityPolicy");
        }
        var selected=admission("read",bytes);selected.put("capabilityPolicy",Map.of("allow",List.of("tasks.get"),"maxCalls",Map.of("tasks.get",2L)));
        var preparation=KeirRuntime.prepareProgram(bytes,bound,selected);assertTrue(preparation.succeeded());
        var prepared=preparation.prepared();
        var denied=await(KeirRuntime.invokeProgram(prepared,Map.of("taskId","task1"),readInvocation("scope",Optional.of(Map.of("maxCalls",Map.of("tasks.get",1L))),r->done(true))));
        error(denied,"AUTHORITY_DENIED","/steps/1");assertEquals(1,calls.get());
        var widened=await(KeirRuntime.invokeProgram(prepared,Map.of("taskId","task1"),readInvocation("scope",Optional.of(Map.of("allow",List.of("tasks.lookup"))),r->done(true))));
        error(widened,"HOST_CONTEXT_INVALID","");assertEquals(1,calls.get());
        assertEquals("COMPLETED",await(KeirRuntime.invokeProgram(prepared,Map.of("taskId","task1"),readInvocation("scope",Optional.of(Map.of()),r->done(true)))).classification());
    }

    @Test void freshEffectZeroAllowsSkippedWriteButDeniesReachedWriteBeforeAuthorization() throws Exception {
        AtomicInteger calls=new AtomicInteger(),auth=new AtomicInteger(); AtomicBoolean completed=new AtomicBoolean(false);
        var bound=bound("effect","scope",effects((a,h,ms,c)->{calls.incrementAndGet();return done(effectRead(completed.get()?"COMPLETED":"OPEN"));},(a,h,ms,c)->{calls.incrementAndGet();return done(confirmed());}));
        var prepared=prepared("effect","main",bound);
        var invocation=effectInvocation("scope",Optional.empty(),OptionalLong.of(0),r->{auth.incrementAndGet();return done(true);});
        assertEquals(Map.of("status","NO_OP"),await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation)).result());
        completed.set(true);error(await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation)),"AUTHORITY_DENIED","/steps/3");
        assertEquals(2,calls.get());assertEquals(4,auth.get());
    }

    @Test void publicAdmissionLimitsAndEffectCeilingsRejectMalformedWideningAndStaticOverflow() throws Exception {
        var bound=bound("read","scope",reads((a,h,ms)->done(read("COMPLETED"))));byte[] bytes=file("read","artifact.json");
        for(Object limits:Arrays.asList(null,List.of(),Map.of("unknown",0L),Map.of("calls",5L),Map.of("traceEvents",1L),Map.of("steps",1.5))) {
            var selected=admission("read",bytes);selected.put("limits",limits);
            denied(KeirRuntime.prepareProgram(bytes,bound,selected),"ADMISSION_INVALID","/admission/limits");
        }
        for(String field:List.of("steps","calls","totalAstNodes")) {
            var selected=admission("read",bytes);selected.put("limits",Map.of(field,0L));
            denied(KeirRuntime.prepareProgram(bytes,bound,selected),"BUDGET_EXCEEDED",field.equals("totalAstNodes")?"/artifact/expressions":"/artifact/steps");
        }
        var selected=admission("read",bytes);selected.put("limits",Map.of("calls",1L));assertTrue(KeirRuntime.prepareProgram(bytes,bound,selected).succeeded());
        var effect=bound("effect","scope",effects((a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->done(confirmed())));
        bytes=file("effect","artifact.json");
        for(Object grant:Arrays.asList(null,-1L,2L,true)) {
            selected=admission("effect",bytes);selected.put("effectGrant",grant);
            denied(KeirRuntime.prepareProgram(bytes,effect,selected),"ADMISSION_INVALID","/admission/effectGrant");
        }
        selected=admission("effect",bytes);selected.put("effectGrant",0L);
        denied(KeirRuntime.prepareProgram(bytes,effect,selected),"AUTHORITY_DENIED","/admission/effectGrant");
    }

    @Test void wrongScopeWrongProfileAndNullOptionalsFailBeforeCallbacks() throws Exception {
        AtomicInteger callbacks=new AtomicInteger(); var bound=bound("effect","scope",effects((a,h,ms,c)->{callbacks.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->done(confirmed())));
        var prepared=prepared("effect","main",bound);
        error(await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),effectInvocation("other",Optional.empty(),OptionalLong.empty(),r->{callbacks.incrementAndGet();return done(true);}))),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),effectInvocation("scope",null,OptionalLong.empty(),r->done(true)))),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),effectInvocation("scope",Optional.empty(),null,r->done(true)))),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeEffect(prepared,Map.of(),null)),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeProgram(prepared,Map.of(),readInvocation("scope",Optional.empty(),r->done(true)))),"HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invokeEffectProgram(null,Map.of(),null)),"HOST_CONTEXT_INVALID","");assertEquals(0,callbacks.get());
    }

    @Test void malformedNestedHostMapsKeepTheirBoundaryDiagnosticsAndDigestPrecedence() throws Exception {
        var prepared=prepared("read","main",bound("read","scope",reads((a,h,ms)->done(read("COMPLETED")))));
        Map<String,Object> policy=Map.of("allow",Map.of(1L,true));
        error(await(KeirRuntime.invokeProgram(prepared,Map.of("taskId","task1"),readInvocation("scope",Optional.of(policy),r->done(true)))),"HOST_CONTEXT_INVALID","");
        byte[] bytes=file("read","artifact.json");var bound=bound("read","scope",reads((a,h,ms)->done(true)));
        var selected=admission("read",bytes);selected.put("capabilityPolicy",policy);
        denied(KeirRuntime.prepareProgram(new byte[]{123},bound,selected),"PIN_MISMATCH","/artifact");
        denied(KeirRuntime.prepareProgram(bytes,bound,selected),"ADMISSION_INVALID","/admission/capabilityPolicy");
        selected=admission("read",bytes);selected.put("consumer",policy);
        denied(KeirRuntime.prepareProgram(bytes,bound,selected),"ADMISSION_INVALID","/admission/consumer");
    }

    @Test void reusedBindingStillRequiresCurrentAuthorizationAtEachDispatch() throws Exception {
        AtomicInteger calls=new AtomicInteger(),checks=new AtomicInteger();
        var prepared=prepared("effect","main",bound("effect","scope",effects((a,h,ms,c)->{calls.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->{calls.incrementAndGet();return done(confirmed());})));
        var invocation=effectInvocation("scope",Optional.empty(),OptionalLong.empty(),r->done(checks.incrementAndGet()<3));
        var result=await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation));
        error(result,"AUTHORITY_DENIED","/steps/3");assertEquals(1,calls.get());assertEquals(List.of(),result.effects());
        error(await(KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),effectInvocation("scope",Optional.empty(),OptionalLong.empty(),r->done(false)))),"AUTHORITY_DENIED","");
        assertEquals(1,calls.get());
    }

    @Test void pendingAuthorizationSnapshotsFreshIdentityAndOldBindingKeepsOldInvoker() throws Exception {
        var pending=new CompletableFuture<Object>();var seen=new ArrayList<Map<String,Object>>();
        var registration=reads((a,h,ms)->{seen.add(h);return done(read("COMPLETED"));});
        var first=bound("read","scope",registration);var prepared=prepared("read","main",first);
        registration.put("tasks.get",new KeirRuntime.Adapter("1","simulation",(a,h,ms)->done(read("OPEN"))));
        var second=bound("read","scope2",registration);var secondPrepared=prepared("read","main",second);
        var identity=new LinkedHashMap<String,Object>(Map.of("actorId","first-user"));
        var invocation=new KeirRuntime.Invocation2("scope",identity,new KeirRuntime.Deadline(()->0,1000),r->r.size()==1?pending:done(true),Optional.empty());
        var outstanding=KeirRuntime.invokeProgram(prepared,Map.of("taskId","task1"),invocation);identity.put("actorId","changed");
        assertEquals(Map.of("classification","PENDING"),await(KeirRuntime.invokeProgram(secondPrepared,Map.of("taskId","task1"),readInvocation("scope2",Optional.empty(),r->done(true)))).result());
        pending.complete(true);assertEquals(Map.of("classification","DONE"),await(outstanding).result());
        assertEquals(List.of(Map.of("actorId","first-user")),seen);
    }

    @Test void modelDigestsWithSameNamedTypesCoexistAndNeverSwitchExistingPlans() throws Exception {
        var first=prepared("read","main",bound("read","scope",reads((a,h,ms)->done(read("COMPLETED")))));
        var dcm=document(file("read","dcm.json"));
        map(dcm.get("domain"),"").put("version","3");
        map(map(dcm.get("types"),"").get("TaskStatus"),"").put("values",List.of("OPEN","COMPLETED","ARCHIVED"));
        byte[] modelBytes=canonical(dcm);var reference=Artifact.modelRef(dcm);
        var approval=approval("read","other");approval.put("modelRef",reference);
        var binding=KeirRuntime.bindModel(modelBytes,approval,reads((a,h,ms)->done(read("ARCHIVED"))));assertTrue(binding.succeeded());
        var artifact=document(file("read","artifact.json"));artifact.put("modelRef",reference);byte[] bytes=canonical(artifact);
        var selection=KeirRuntime.prepareProgram(bytes,binding.boundModel(),admission("read",bytes));assertTrue(selection.succeeded());
        var pending=KeirRuntime.invokeProgram(first,Map.of("taskId","task1"),readInvocation("scope",Optional.empty(),r->done(true)));
        assertEquals(Map.of("classification","PENDING"),await(KeirRuntime.invokeProgram(selection.prepared(),Map.of("taskId","task1"),readInvocation("other",Optional.empty(),r->done(true)))).result());
        assertEquals(Map.of("classification","DONE"),await(pending).result());
        assertEquals(Map.of("classification","DONE"),await(KeirRuntime.invokeProgram(first,Map.of("taskId","task1"),readInvocation("scope",Optional.empty(),r->done(true)))).result());
    }

    @Test void confirmedEvidenceSurvivesInvalidPayloadAndLateEvidenceCannotRewriteTimeout() throws Exception {
        var clock=new AtomicLong(); var response=new CompletableFuture<Object>();
        var bound=bound("effect","scope",effects((a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->{clock.set(1000);return done(Map.of("kind","SUCCESS","value",Map.of("wrong","result"),"effect","CONFIRMED"));}));
        var normal=effectInvocation("scope",Optional.empty(),OptionalLong.empty(),r->done(true));
        var invocation=new KeirRuntime.EffectInvocation2(normal.scope(),normal.bindings(),new KeirRuntime.Deadline(clock::get,1000),normal.authorize(),normal.capabilityPolicy(),normal.effectGrant(),KEY,normal.event());
        var result=await(KeirRuntime.invokeEffectProgram(prepared("effect","main",bound),Map.of("taskId","task1"),invocation));
        assertEquals("CONFIRMED",result.effects().get(0).get("state"));error(result,"BUDGET_EXCEEDED","/steps/3");
        var lateBound=bound("effect","scope",effects((a,h,ms,c)->done(effectRead("COMPLETED")),(a,h,ms,c)->response));
        var latePrepared=prepared("effect","main",lateBound);
        var late=new KeirRuntime.EffectInvocation2("scope",normal.bindings(),new KeirRuntime.Deadline(System::currentTimeMillis,System.currentTimeMillis()+150),normal.authorize(),Optional.empty(),OptionalLong.empty(),KEY,normal.event());
        result=await(KeirRuntime.invokeEffectProgram(latePrepared,Map.of("taskId","task1"),late));
        assertEquals("UNCERTAIN",result.classification());assertEquals("UNKNOWN",result.effects().get(0).get("state"));
        byte[] snapshot=result.toJson();response.complete(confirmed());assertArrayEquals(snapshot,result.toJson());
    }
}
