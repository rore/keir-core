package dev.keir.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectRuntimeTest {
    static final String READ="tasks.get", WRITE="tasks.createFollowUp", KEY_TYPE="sample.RequestUuid/1";
    static final Map<String,Long> GRANTS=Map.of(READ,1L,WRITE,1L);
    static final Map<String,Object> INPUT=Map.of("taskId","task-a","projectId","project-a","title","Follow up");
    static Map<String,Object> map(Object value) { return RuntimeData.map(value,""); }
    static byte[] bytes() throws IOException {
        try (var input=EffectRuntimeTest.class.getResourceAsStream("/single-effect/artifact.json")) {
            assertNotNull(input); return input.readAllBytes();
        }
    }
    static Map<String,Object> admission(byte[] bytes) {
        var artifact=map(RuntimeData.parseCanonical(bytes)); var dcm=map(artifact.get("dcm"));
        var entry=map(map(dcm.get("entrypoints")).get(artifact.get("entrypoint")));
        var result=new LinkedHashMap<String,Object>();
        result.put("expectedArtifactDigest",RuntimeData.digest(bytes)); result.put("expectedDcmDigest",artifact.get("dcmDigest"));
        result.put("entrypoint",artifact.get("entrypoint")); result.put("consumer",entry.get("consumer"));
        result.put("compiler",artifact.get("compiler")); result.put("environment",artifact.get("environment"));
        result.put("mode","simulation"); result.put("grants",GRANTS); result.put("effectGrant",1L);
        result.put("keyBinding",new KeirRuntime.KeyBinding(KEY_TYPE,key->key instanceof UUID));
        return result;
    }
    static CompletionStage<?> done(Object value) { return CompletableFuture.completedFuture(value); }
    static Map<String,Object> read(String status,String project) {
        return Map.of("kind","SUCCESS","value",Map.of("id","task-a","status",status,"projectId",project));
    }
    static Map<String,Object> confirmed() { return Map.of("kind","SUCCESS","effect","CONFIRMED","value",Map.of("id","created-a")); }
    static Map<String,KeirRuntime.EffectAdapter> adapters(KeirRuntime.EffectAdapterFunction read,KeirRuntime.EffectAdapterFunction write) {
        return new LinkedHashMap<>(Map.of(READ,new KeirRuntime.EffectAdapter("1","simulation",KEY_TYPE,read),
                WRITE,new KeirRuntime.EffectAdapter("1","simulation",KEY_TYPE,write)));
    }
    static KeirRuntime.Prepared prepared(Map<String,KeirRuntime.EffectAdapter> adapters) throws Exception {
        byte[] bytes=bytes(); return prepared(bytes,admission(bytes),adapters);
    }
    static KeirRuntime.Prepared prepared(byte[] bytes,Map<String,Object> admission,Map<String,KeirRuntime.EffectAdapter> adapters) {
        var result=KeirRuntime.prepareEffect(bytes,admission,adapters);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); return result.prepared();
    }
    static KeirRuntime.EffectInvocation invocation(Object key) {
        return invocation(key,new KeirRuntime.Deadline(()->0,10_000),1L,request->done(true));
    }
    static KeirRuntime.EffectInvocation invocation(Object key,KeirRuntime.Deadline deadline,Long grant,KeirRuntime.Authorizer authorize) {
        return new KeirRuntime.EffectInvocation(Map.of("actorId","actor-a"),deadline,authorize,GRANTS,grant,key,Map.of("id","event-a","source","task-a"));
    }
    static KeirRuntime.Outcome await(CompletionStage<KeirRuntime.Outcome> result) throws Exception { return result.toCompletableFuture().get(3,TimeUnit.SECONDS); }
    static KeirRuntime.Outcome run(KeirRuntime.EffectAdapterFunction write) throws Exception {
        return await(KeirRuntime.invokeEffect(prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),write)),INPUT,invocation(UUID.randomUUID())));
    }
    static List<String> events(KeirRuntime.Outcome outcome) { return outcome.trace().stream().map(e->(String)e.get("event")).toList(); }
    static void receipt(KeirRuntime.Outcome outcome,String state) {
        assertEquals(List.of(Map.of("step","created","capability",WRITE,"state",state)),outcome.effects());
        assertThrows(UnsupportedOperationException.class,()->outcome.effects().clear());
        assertThrows(UnsupportedOperationException.class,()->outcome.effects().getFirst().clear());
    }
    static void error(KeirRuntime.Outcome outcome,String classification,String code,String path) {
        assertEquals(classification,outcome.classification()); assertNull(outcome.result());
        assertEquals(Map.of("code",code,"path",path),outcome.error());
        assertEquals("TERMINATED",outcome.trace().getLast().get("event"));
        assertEquals(code,outcome.trace().getLast().get("code"));
    }

    @Test void originalKeyAndImmutableProtectedContextReachBothAdaptersWithExactTrace() throws Exception {
        UUID key=UUID.randomUUID(); List<KeirRuntime.HostContext> contexts=new CopyOnWriteArrayList<>();
        List<Map<String,Object>> requests=new CopyOnWriteArrayList<>();
        var adapters=adapters((agent,host,left,context)->{
            assertEquals(Map.of("taskId","task-a"),agent); assertEquals(Map.of("actorId","actor-a"),host);
            contexts.add(context); return done(read("COMPLETED","project-a"));
        },(agent,host,left,context)->{
            assertEquals(Map.of("sourceTaskId","task-a","projectId","project-a","title","Follow up"),agent);
            assertEquals(Map.of("actorId","actor-a"),host); assertTrue(left>0); contexts.add(context);
            assertThrows(UnsupportedOperationException.class,()->agent.clear()); return done(confirmed());
        });
        var outcome=await(KeirRuntime.invokeEffect(prepared(adapters),INPUT,invocation(key,new KeirRuntime.Deadline(()->0,10000),1L,r->{requests.add(r);return done(true);})));
        assertEquals("COMPLETED",outcome.classification()); assertEquals(Map.of("status","CREATED"),outcome.result()); receipt(outcome,"CONFIRMED");
        assertEquals(2,contexts.size());
        for(var context:contexts) {
            assertSame(key,context.requestKey()); assertEquals(Map.of("id","event-a","source","task-a"),context.event());
            assertThrows(UnsupportedOperationException.class,()->context.event().clear());
        }
        assertEquals(List.of(Map.of("entrypoint","TaskCompleted"),Map.of("entrypoint","TaskCompleted","capability",READ,"version","1"),
                Map.of("entrypoint","TaskCompleted","capability",WRITE,"version","1")),requests);
        assertEquals(List.of("INVOCATION_START","STEP_START","CALL_DISPATCHED","CALL_SUCCEEDED","STEP_START","VALUE_BOUND",
                "STEP_START","GUARD_EVALUATED","EFFECT_INTENT","CALL_DISPATCHED","EFFECT_RECORDED","CALL_SUCCEEDED","RETURNED","TERMINATED"),events(outcome));
        assertEquals(Map.of("sequence",7,"event","GUARD_EVALUATED","step","created","taken",true),outcome.trace().get(7));
        assertEquals(Map.of("sequence",10,"event","EFFECT_RECORDED","step","created","capability",WRITE,"state","CONFIRMED"),outcome.trace().get(10));
        for(String secret:List.of(key.toString(),"event-a","task-a","actor-a","Follow up"))
            assertFalse(new String(outcome.toJson(),java.nio.charset.StandardCharsets.UTF_8).contains(secret));
    }

    @Test void falseGuardsSkipWriteAuthorityAndGrantAndProduceNoReceipt() throws Exception {
        for(var read:List.of(read("OPEN","project-a"),read("COMPLETED","project-other"))) {
            AtomicInteger calls=new AtomicInteger(),auth=new AtomicInteger();
            var prepared=prepared(adapters((a,h,l,c)->done(read),(a,h,l,c)->{calls.incrementAndGet();return done(confirmed());}));
            var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(()->0,10000),0L,r->{auth.incrementAndGet();return done(true);})));
            assertEquals("COMPLETED",outcome.classification()); assertEquals(Map.of("status","NO_OP"),outcome.result());
            assertEquals(List.of(),outcome.effects()); assertEquals(0,calls.get()); assertEquals(2,auth.get());
            assertEquals(Map.of("sequence",7,"event","GUARD_EVALUATED","step","created","taken",false),outcome.trace().get(7));
            assertEquals("STEP_SKIPPED",outcome.trace().get(8).get("event"));
        }
    }

    @Test void terminalReadAndWriteBusinessHandlersKeepOriginalErrorAndNeverReachReturn() throws Exception {
        for(boolean write:List.of(false,true)) {
            String code=write?"CREATE_REJECTED":"TASK_NOT_FOUND";
            AtomicInteger calls=new AtomicInteger();
            var prepared=prepared(adapters((a,h,l,c)->done(write?read("COMPLETED","project-a"):Map.of("kind","BUSINESS_REJECTION","code",code)),
                    (a,h,l,c)->{calls.incrementAndGet();return done(Map.of("kind","BUSINESS_REJECTION","code",code,"effect","NONE"));}));
            var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID())));
            assertEquals("BUSINESS_REJECTION",outcome.classification());
            assertEquals(Map.of("status",write?"REJECTED":"SOURCE_MISSING"),outcome.result());
            assertEquals(Map.of("code",code,"path",write?"/steps/2":"/steps/0"),outcome.error());
            assertEquals(write?1:0,calls.get()); assertEquals(write?13:5,outcome.trace().size());
            if(write) receipt(outcome,"NONE"); else assertEquals(List.of(),outcome.effects());
        }
        var rejected=run((a,h,l,c)->done(Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED","effect","NONE")));
        error(rejected,"BUSINESS_REJECTION","ACCESS_DENIED","/steps/2"); receipt(rejected,"NONE");
    }

    @Test void evidenceTagsAndOuterShapeDetermineReceiptBeforeResultValidation() throws Exception {
        List<Object> responses=new ArrayList<>(List.of(
                Map.of("kind","SUCCESS","effect","CONFIRMED","value",Map.of("id",false)),
                Map.of("kind","SUCCESS","effect","CONFIRMED","value",Map.of("id","x".repeat(257))),
                Map.of("kind","BUSINESS_REJECTION","effect","NONE","code","UNDECLARED"),
                Map.of("kind","TRANSIENT_FAILURE","effect","NONE"),
                Map.of("kind","UNKNOWN_COMMIT","effect","UNKNOWN"),
                Map.of("kind","SUCCESS","effect","NONE","value",Map.of("id","x")),
                Map.of("kind","SUCCESS","effect","CONFIRMED","value",Map.of("id","x"),"extra",true),
                Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED"),"invalid"));
        var nullValue=new LinkedHashMap<String,Object>(); nullValue.put("kind","SUCCESS"); nullValue.put("effect","CONFIRMED"); nullValue.put("value",null); responses.add(nullValue);
        responses.add(Map.of(1,"invalid key"));
        for(int i=0;i<responses.size();i++) {
            Object response=responses.get(i); var outcome=run((a,h,l,c)->done(response));
            String code=i==1?"BUDGET_EXCEEDED":i==3?"ADAPTER_TRANSIENT":i==4?"UNKNOWN_COMMIT":"ADAPTER_INVALID";
            String state=i<2||i==9?"CONFIRMED":i<4?"NONE":"UNKNOWN";
            error(outcome,state.equals("UNKNOWN")?"UNCERTAIN":i==1?"LIMIT_EXCEEDED":"FAILED",code,i==1?"/steps/2/id":"/steps/2"); receipt(outcome,state);
            assertFalse(events(outcome).contains("RETURNED")); assertEquals("EFFECT_RECORDED",outcome.trace().get(outcome.trace().size()-2).get("event"));
        }
    }

    @Test void readOutcomesRejectEffectTagsAndCannotProduceWriteReceipts() throws Exception {
        for(Object response:List.of(Map.of("kind","SUCCESS","effect","CONFIRMED","value",map(read("COMPLETED","project-a").get("value"))),
                Map.of("kind","TRANSIENT_FAILURE"),Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED"))) {
            AtomicInteger writes=new AtomicInteger();
            var prepared=prepared(adapters((a,h,l,c)->done(response),(a,h,l,c)->{writes.incrementAndGet();return done(confirmed());}));
            var result=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID())));
            String kind=(String)map(response).get("kind");
            error(result,kind.equals("BUSINESS_REJECTION")?"BUSINESS_REJECTION":"FAILED",kind.equals("TRANSIENT_FAILURE")?"ADAPTER_TRANSIENT":kind.equals("BUSINESS_REJECTION")?"ACCESS_DENIED":"ADAPTER_INVALID","/steps/0");
            assertEquals(List.of(),result.effects()); assertEquals(0,writes.get());
        }
    }

    @Test void writeThrowAndRejectedFutureAreUnknownAndRedacted() throws Exception {
        for(boolean sync:List.of(false,true)) {
            var outcome=run((a,h,l,c)->{
                if(sync) throw new IllegalStateException("private detail");
                return CompletableFuture.failedFuture(new IllegalStateException("private detail"));
            });
            error(outcome,"UNCERTAIN","ADAPTER_EXCEPTION","/steps/2"); receipt(outcome,"UNKNOWN");
            assertFalse(new String(outcome.toJson()).contains("private detail"));
        }
    }

    @Test void winningResponseEvidencePrecedesExpiryAndInvalidCompletionClocks() throws Exception {
        for(Object response:List.of(confirmed(),Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"),Map.of("kind","TRANSIENT_FAILURE","effect","NONE"))) {
            for(long end:List.of(10000L,-1L,4L)) {
                AtomicLong now=new AtomicLong(5);
                var prepared=prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{now.set(end);return done(response);}));
                var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(now::get,10000),1L,r->done(true))));
                error(outcome,end==10000?"LIMIT_EXCEEDED":"FAILED",end==10000?"BUDGET_EXCEEDED":"HOST_CONTEXT_INVALID",end==10000?"/steps/2":"");
                receipt(outcome,map(response).get("effect").toString()); assertFalse(events(outcome).contains("RETURNED"));
            }
        }
    }

    @Test void timeoutWinnerAndLateConfirmedOrNoneResponsesCannotReviseOutcome() throws Exception {
        for(Object late:List.of(confirmed(),Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"),Map.of("kind","TRANSIENT_FAILURE","effect","NONE"))) {
            var response=new CompletableFuture<Object>(); CountDownLatch dispatched=new CountDownLatch(1);
            // Only the write gets a short native wait; earlier phases keep ample headroom.
            AtomicLong now=new AtomicLong();
            var prepared=prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{dispatched.countDown();return response;}));
            var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(now::get,10100),1L,r->{
                if(WRITE.equals(r.get("capability"))) now.set(10000); return done(true);
            })));
            assertEquals(0,dispatched.getCount()); error(outcome,"UNCERTAIN","BUDGET_EXCEEDED","/steps/2"); receipt(outcome,"UNKNOWN");
            byte[] before=outcome.toJson(); assertTrue(response.complete(late)); assertArrayEquals(before,outcome.toJson());
        }
    }

    @Test void zeroFreshEffectGrantDeniesBeforeWriteAuthorizationAndDispatch() throws Exception {
        AtomicInteger auth=new AtomicInteger(),writes=new AtomicInteger();
        var prepared=prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{writes.incrementAndGet();return done(confirmed());}));
        var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(()->0,10000),0L,r->{auth.incrementAndGet();return done(true);})));
        error(outcome,"FAILED","AUTHORITY_DENIED","/steps/2"); assertEquals(List.of(),outcome.effects()); assertEquals(2,auth.get()); assertEquals(0,writes.get());
    }

    @Test void exactWriteTraceReserveBoundaryNeverCreatesPhantomIntent() throws Exception {
        for(long limit:List.of(12L,13L,14L)) {
            AtomicInteger writes=new AtomicInteger(); byte[] bytes=bytes(); var admission=admission(bytes); admission.put("limits",Map.of("traceEvents",limit));
            var prepared=prepared(bytes,admission,adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{writes.incrementAndGet();return done(confirmed());}));
            var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID())));
            assertTrue(outcome.trace().size()<=limit);
            if(limit==12) { error(outcome,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/steps/2"); assertEquals(0,writes.get()); assertEquals(List.of(),outcome.effects()); assertFalse(events(outcome).contains("EFFECT_INTENT")); }
            else { assertEquals(1,writes.get()); receipt(outcome,"CONFIRMED");
                if(limit==13) error(outcome,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/return"); else assertEquals("COMPLETED",outcome.classification()); }
        }
    }

    @Test void handlerFailureRetainsOriginalRejectionAsNonrecursiveCause() throws Exception {
        AtomicBoolean rejected=new AtomicBoolean(); AtomicInteger checks=new AtomicInteger();
        var prepared=prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{rejected.set(true);return done(Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"));}));
        var outcome=await(KeirRuntime.invokeEffect(prepared,INPUT,invocation(UUID.randomUUID(),new KeirRuntime.Deadline(()->rejected.get()&&checks.incrementAndGet()>=3?10000:0,10000),1L,r->done(true))));
        assertEquals("LIMIT_EXCEEDED",outcome.classification()); assertNull(outcome.result()); receipt(outcome,"NONE");
        assertEquals(Map.of("code","BUDGET_EXCEEDED","path","/steps/2/onError/CREATE_REJECTED/status","cause",Map.of("code","CREATE_REJECTED","path","/steps/2")),outcome.error());
    }

    @Test void winningOuterEnvelopeIsSnapshottedBeforeLaterAdapterMutation() throws Exception {
        var response=new LinkedHashMap<String,Object>(confirmed()); AtomicBoolean writing=new AtomicBoolean();
        CountDownLatch settled=new CountDownLatch(1),release=new CountDownLatch(1);
        var prepared=prepared(adapters((a,h,l,c)->done(read("COMPLETED","project-a")),(a,h,l,c)->{writing.set(true);return done(response);}));
        var invocation=invocation(UUID.randomUUID(),new KeirRuntime.Deadline(()->{
            if(writing.compareAndSet(true,false)) {
                settled.countDown();
                try { assertTrue(release.await(2,TimeUnit.SECONDS),"Snapshot test was not released"); }
                catch(InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
            }
            return 0;
        },10000),1L,r->done(true));
        // CompletionStage callbacks may run inline before invokeEffect returns.
        var pending=CompletableFuture.supplyAsync(()->KeirRuntime.invokeEffect(prepared,INPUT,invocation)).thenCompose(stage->stage);
        try {
            assertTrue(settled.await(2,TimeUnit.SECONDS)); response.clear(); response.putAll(Map.of("kind","TRANSIENT_FAILURE","effect","NONE"));
        } finally { release.countDown(); }
        var outcome=await(pending); assertEquals("COMPLETED",outcome.classification()); assertEquals(Map.of("status","CREATED"),outcome.result()); receipt(outcome,"CONFIRMED");
    }
}
