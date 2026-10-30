package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static dev.keir.runtime.EffectRuntimeTest.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectAdmissionTest {
    @Test void opaqueKeyContractLabelsDoNotAliasBusinessTypes() throws Exception {
        for(String type:List.of("String","Boolean","TaskId","FollowupInput")) {
            var artifact=map(RuntimeData.parseCanonical(bytes()));
            var dcm=map(artifact.get("dcm")); map(dcm.get("hostKey")).put("type",type);
            artifact.put("dcmDigest",RuntimeData.digest(RuntimeData.canonical(dcm)));
            byte[] bytes=RuntimeData.canonical(artifact);
            var admission=admission(bytes);
            admission.put("keyBinding",new KeirRuntime.KeyBinding(type,key->key instanceof UUID));
            List<Object> seen=new CopyOnWriteArrayList<>();
            var adapters=new LinkedHashMap<String,KeirRuntime.EffectAdapter>();
            registrations().forEach((id,adapter)->adapters.put(id,new KeirRuntime.EffectAdapter(adapter.version(),adapter.mode(),type,
                    (agent,host,left,context)->{seen.add(context.requestKey());return adapter.invoke().invoke(agent,host,left,context);} )));
            UUID key=UUID.randomUUID();
            var outcome=await(KeirRuntime.invokeEffect(prepared(bytes,admission,adapters),INPUT,invocation(key)));
            assertEquals("COMPLETED",outcome.classification()); assertEquals(Map.of("status","CREATED"),outcome.result());
            receipt(outcome,"CONFIRMED"); assertEquals(2,seen.size()); seen.forEach(value->assertSame(key,value));
        }
    }

    private static Map<String,KeirRuntime.EffectAdapter> registrations() {
        return adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->done(confirmed()));
    }
    private static void denied(KeirRuntime.Preparation result,String code,String path) {
        assertFalse(result.succeeded()); assertNull(result.prepared());
        assertEquals(code,result.diagnostic().code()); assertEquals(path,result.diagnostic().path());
    }

    @Test void invalidEffectHandlesStillReturnAnEffectOutcomeAndCannotUseTheReadApi() throws Exception {
        var invalid=await(KeirRuntime.invokeEffect(null,INPUT,invocation(UUID.randomUUID())));
        error(invalid,"FAILED","HOST_CONTEXT_INVALID",""); assertEquals(List.of(),invalid.effects());
        var prepared=prepared(registrations());
        error(await(KeirRuntime.invokeEffect(prepared,INPUT,null)),"FAILED","HOST_CONTEXT_INVALID","");
        var common=new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(()->0,10000),r->done(true),GRANTS);
        var readCall=await(KeirRuntime.invoke(prepared,INPUT,common));
        error(readCall,"FAILED","HOST_CONTEXT_INVALID",""); assertFalse(readCall.toMap().containsKey("effects"));
    }

    @Test void explicitEffectAdmissionChecksSelectorsModeKeyRegistrationAndStaticAuthorityWithoutCallbacks() throws Exception {
        byte[] bytes=bytes(); AtomicInteger validations=new AtomicInteger();
        var admission=admission(bytes); admission.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,key->{validations.incrementAndGet();return true;}));
        assertTrue(KeirRuntime.prepareEffect(bytes,admission,registrations()).succeeded()); assertEquals(0,validations.get());
        assertFalse(KeirRuntime.prepare(bytes,admission,Map.of()).succeeded());
        for(String field:List.of("effectGrant","keyBinding")) {
            var bad=admission(bytes); bad.remove(field); denied(KeirRuntime.prepareEffect(bytes,bad,registrations()),"ADMISSION_INVALID","/admission");
        }
        for(Object grant:List.of(-1L,2L,1.0,"1")) {
            var bad=admission(bytes); bad.put("effectGrant",grant); denied(KeirRuntime.prepareEffect(bytes,bad,registrations()),"ADMISSION_INVALID","/admission/effectGrant");
        }
        var zero=admission(bytes); zero.put("effectGrant",0L); denied(KeirRuntime.prepareEffect(bytes,zero,registrations()),"AUTHORITY_DENIED","/admission/effectGrant");
        for(Object binding:List.of(new KeirRuntime.KeyBinding("wrong",key->true),new KeirRuntime.KeyBinding(KEY_TYPE,null),Map.of("type",KEY_TYPE,"validate",true))) {
            var bad=admission(bytes); bad.put("keyBinding",binding); denied(KeirRuntime.prepareEffect(bytes,bad,registrations()),"ADMISSION_INVALID","/admission/keyBinding");
        }
        var real=admission(bytes); real.put("mode","real"); denied(KeirRuntime.prepareEffect(bytes,real,registrations()),"ADMISSION_INVALID","/admission/mode");
        for(String capability:List.of(READ,WRITE)) for(var registration:List.of(
                new KeirRuntime.EffectAdapter("1","simulation","wrong",(a,h,l,c)->done(confirmed())),
                new KeirRuntime.EffectAdapter("2","simulation",KEY_TYPE,(a,h,l,c)->done(confirmed())),
                new KeirRuntime.EffectAdapter("1","real",KEY_TYPE,(a,h,l,c)->done(confirmed())),
                new KeirRuntime.EffectAdapter("1","simulation",KEY_TYPE,null))) {
            var bad=registrations(); bad.put(capability,registration);
            denied(KeirRuntime.prepareEffect(bytes,admission(bytes),bad),"ADMISSION_INVALID","/admission/adapters");
        }
        for(String field:List.of("expectedDcmDigest","entrypoint","compiler")) {
            var bad=admission(bytes); bad.put(field,field.equals("expectedDcmDigest")?"sha256:"+"0".repeat(64):"wrong");
            denied(KeirRuntime.prepareEffect(bytes,bad,registrations()),"PIN_MISMATCH","/admission");
        }
        byte[] bad="{".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        denied(KeirRuntime.prepareEffect(bad,admission(bytes),registrations()),"PIN_MISMATCH","/artifact");
        assertEquals(0,validations.get());
    }

    @Test void invalidKeysAndValidatorsFailBeforeAuthorizationEvenWhenWriteWouldBeSkipped() throws Exception {
        AtomicInteger callbacks=new AtomicInteger();
        var registrations=adapters((a,h,l,c)->{callbacks.incrementAndGet();return done(read("OPEN","project-a"));},(a,h,l,c)->{callbacks.incrementAndGet();return done(confirmed());});
        byte[] bytes=bytes();
        for(Object key:Arrays.asList(null,"uuid-string",Map.of("brand","uuid"))) {
            var result=await(KeirRuntime.invokeEffect(prepared(bytes,admission(bytes),registrations),INPUT,
                    invocation(key,new KeirRuntime.Deadline(()->0,10000),1L,r->{callbacks.incrementAndGet();return done(true);} )));
            error(result,"FAILED","HOST_CONTEXT_INVALID",""); assertEquals(List.of(),result.effects());
        }
        List<KeirRuntime.KeyValidator> validators=List.of(key->false,key->"true",key->null,key->{throw new AssertionError("private validator");});
        for(var validator:validators) {
            var admission=admission(bytes); admission.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,validator));
            var result=await(KeirRuntime.invokeEffect(prepared(bytes,admission,registrations),INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(()->0,10000),1L,r->{callbacks.incrementAndGet();return done(true);} )));
            error(result,"FAILED","HOST_CONTEXT_INVALID",""); assertFalse(new String(result.toJson()).contains("private validator"));
        }
        assertEquals(0,callbacks.get());
    }

    @Test void invocationEventGrantAndAuthorityRemainFreshAndClosed() throws Exception {
        AtomicInteger callbacks=new AtomicInteger();
        var prepared=prepared(adapters((a,h,l,c)->{callbacks.incrementAndGet();return done(read("COMPLETED","project-a"));},(a,h,l,c)->{callbacks.incrementAndGet();return done(confirmed());}));
        var valid=invocation(UUID.randomUUID());
        for(Long grant:Arrays.asList(null,-1L,2L)) {
            var bad=new KeirRuntime.EffectInvocation(valid.bindings(),valid.deadline(),valid.authorize(),valid.grants(),grant,valid.requestKey(),valid.event());
            error(await(KeirRuntime.invokeEffect(prepared,INPUT,bad)),"FAILED","HOST_CONTEXT_INVALID","");
        }
        for(Map<String,Object> event:Arrays.asList(null,Map.<String,Object>of(),Map.<String,Object>of("id",false,"source","task-a"),Map.<String,Object>of("id","event-a","source","task-a","key","override"))) {
            var bad=new KeirRuntime.EffectInvocation(valid.bindings(),valid.deadline(),valid.authorize(),valid.grants(),1L,valid.requestKey(),event);
            error(await(KeirRuntime.invokeEffect(prepared,INPUT,bad)),"FAILED","HOST_CONTEXT_INVALID","");
        }
        assertEquals(0,callbacks.get());
        AtomicInteger auth=new AtomicInteger();
        var denied=new KeirRuntime.EffectInvocation(valid.bindings(),valid.deadline(),r->{auth.incrementAndGet();return done(true);},Map.of(READ,1L),1L,valid.requestKey(),valid.event());
        var result=await(KeirRuntime.invokeEffect(prepared,INPUT,denied)); error(result,"FAILED","AUTHORITY_DENIED","/steps/2");
        assertEquals(2,auth.get()); assertEquals(1,callbacks.get()); assertEquals(List.of(),result.effects());
        for(boolean failure:List.of(false,true)) {
            var rejected=invocation(UUID.randomUUID(),valid.deadline(),1L,r->WRITE.equals(r.get("capability"))?
                    failure?CompletableFuture.failedFuture(new IllegalStateException("detail")):done(false):done(true));
            result=await(KeirRuntime.invokeEffect(prepared,INPUT,rejected));
            error(result,"FAILED",failure?"AUTHORITY_CHECK_FAILED":"AUTHORITY_DENIED","/steps/2"); assertEquals(List.of(),result.effects());
            assertFalse(events(result).contains("EFFECT_INTENT"));
        }
    }

    @Test void opaqueKeyIsNeverConvertedComparedCopiedOrTraversed() throws Exception {
        Object key=new Object() {
            @Override public String toString() { throw new AssertionError("key conversion"); }
            @Override public int hashCode() { throw new AssertionError("key hash"); }
            @Override public boolean equals(Object other) { throw new AssertionError("key comparison"); }
        };
        byte[] bytes=bytes(); var admission=admission(bytes); admission.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,k->k==key));
        AtomicInteger seen=new AtomicInteger();
        var prepared=prepared(bytes,admission,adapters((a,h,l,c)->{assertSame(key,c.requestKey());seen.incrementAndGet();return done(read("COMPLETED","project-a"));},
                (a,h,l,c)->{assertSame(key,c.requestKey());seen.incrementAndGet();return done(confirmed());}));
        var result=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(key))); assertEquals("COMPLETED",result.classification()); assertEquals(2,seen.get());
        assertDoesNotThrow(()->RuntimeData.parseCanonical(result.toJson()));
    }

    @Test void preparedReuseSnapshotsRegistrationButNeverKeepsInvocationKeyEventOrLedger() throws Exception {
        byte[] bytes=bytes(); var admission=admission(bytes); List<KeirRuntime.HostContext> contexts=new CopyOnWriteArrayList<>();
        var registrations=adapters((a,h,l,c)->{contexts.add(c);return done(read("COMPLETED","project-a"));},(a,h,l,c)->{contexts.add(c);return done(confirmed());});
        var prepared=prepared(bytes,admission,registrations); admission.clear(); registrations.clear(); Arrays.fill(bytes,(byte)0);
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        var firstEvent=new LinkedHashMap<String,Object>(Map.of("id","event-one","source","task-one"));
        var valid=invocation(first); var invocation=new KeirRuntime.EffectInvocation(valid.bindings(),valid.deadline(),valid.authorize(),valid.grants(),1L,first,firstEvent);
        var one=KeirRuntime.invokeEffect(prepared,INPUT,invocation); firstEvent.put("id","changed");
        var two=KeirRuntime.invokeEffect(prepared,INPUT,invocation(second));
        receipt(await(one),"CONFIRMED"); receipt(await(two),"CONFIRMED"); assertEquals(4,contexts.size());
        assertEquals(2,contexts.stream().filter(c->c.requestKey()==first).count()); assertEquals(2,contexts.stream().filter(c->c.requestKey()==second).count());
        contexts.stream().filter(c->c.requestKey()==first).forEach(c->assertEquals("event-one",c.event().get("id")));
    }

    @Test void validatorAndPostConfirmationClockChecksPreserveTheRightReceiptBoundary() throws Exception {
        byte[] bytes=bytes(); AtomicLong now=new AtomicLong(); var admission=admission(bytes);
        admission.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,k->{now.set(10000);return true;}));
        AtomicInteger calls=new AtomicInteger(); var registrations=adapters((a,h,l,c)->{calls.incrementAndGet();return done(read("COMPLETED","project-a"));},(a,h,l,c)->done(confirmed()));
        var outcome=await(KeirRuntime.invokeEffect(prepared(bytes,admission,registrations),INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(now::get,10000),1L,r->done(true))));
        error(outcome,"LIMIT_EXCEEDED","BUDGET_EXCEEDED",""); assertEquals(List.of(),outcome.effects()); assertEquals(0,calls.get());
        AtomicBoolean written=new AtomicBoolean(); AtomicInteger checks=new AtomicInteger();
        var prepared=prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{written.set(true);return done(confirmed());}));
        outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(()->written.get()&&checks.incrementAndGet()>=3?10000:0,10000),1L,r->done(true))));
        error(outcome,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/return"); receipt(outcome,"CONFIRMED");
    }

    @Test void throwingValidatorStillChecksTheCompletionDeadline() throws Exception {
        byte[] bytes=bytes(); AtomicLong now=new AtomicLong(); var admission=admission(bytes);
        admission.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,k->{now.set(10000);throw new IllegalStateException("private validator");}));
        var outcome=await(KeirRuntime.invokeEffect(prepared(bytes,admission,registrations()),INPUT,
                invocation(UUID.randomUUID(),new KeirRuntime.Deadline(now::get,10000),1L,r->done(true))));
        error(outcome,"LIMIT_EXCEEDED","BUDGET_EXCEEDED",""); assertEquals(List.of(),outcome.effects());
    }
}
