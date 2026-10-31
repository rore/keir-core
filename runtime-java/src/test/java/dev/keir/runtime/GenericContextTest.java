package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.GenericRuntimeTest.*;

class GenericContextTest {
    private static Map<String,Object> cycle() {
        Map<String,Object> value=new LinkedHashMap<>();value.put("self",value);return value;
    }
    private static Map<String,Object> deep() {
        Map<String,Object> value=Map.of("leaf",true);
        for(int i=0;i<10_000;i++)value=Map.of("nested",value);
        return value;
    }
    private static void failed(CompletionStage<KeirRuntime.Outcome> pending,String code,String path) throws Exception {
        var outcome=await(pending);error(outcome,code,path);
        assertEquals("FAILED",outcome.classification());assertEquals(2,outcome.trace().size());
    }

    @Test void cyclicAndDeepPoliciesReturnSafeOutcomesFromBothPublicApis() throws Exception {
        AtomicInteger callbacks=new AtomicInteger();
        var read=prepared("read","main",bound("read","scope",reads((a,h,ms)->{callbacks.incrementAndGet();return done(read("COMPLETED"));})));
        var effect=prepared("effect","main",bound("effect","scope",effects((a,h,ms,c)->{callbacks.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->{callbacks.incrementAndGet();return done(confirmed());})));
        for(var invalid:List.of(cycle(),deep())) {
            var policy=Optional.<Map<String,Object>>of(Map.of("allow",invalid));
            var authorize=(KeirRuntime.Authorizer)r->{callbacks.incrementAndGet();return done(true);};
            failed(assertDoesNotThrow(()->KeirRuntime.invokeProgram(read,Map.of("taskId","task1"),readInvocation("scope",policy,authorize))),"HOST_CONTEXT_INVALID","");
            failed(assertDoesNotThrow(()->KeirRuntime.invokeEffectProgram(effect,Map.of("taskId","task1"),effectInvocation("scope",policy,OptionalLong.empty(),authorize))),"HOST_CONTEXT_INVALID","");
        }
        assertEquals(0,callbacks.get());
    }

    @Test void cyclicAndDeepBindingsReturnSafeOutcomesBeforeDispatch() throws Exception {
        AtomicInteger callbacks=new AtomicInteger();
        var read=prepared("read","main",bound("read","scope",reads((a,h,ms)->{callbacks.incrementAndGet();return done(read("COMPLETED"));})));
        var effect=prepared("effect","main",bound("effect","scope",effects((a,h,ms,c)->{callbacks.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->done(confirmed()))));
        for(var invalid:List.of(cycle(),deep())) {
            var valid=readInvocation("scope",Optional.empty(),r->{callbacks.incrementAndGet();return done(true);});
            var invocation=new KeirRuntime.Invocation2("scope",Map.of("actorId",invalid),valid.deadline(),valid.authorize(),Optional.empty());
            failed(assertDoesNotThrow(()->KeirRuntime.invokeProgram(read,Map.of("taskId","task1"),invocation)),"HOST_BINDING_INVALID","/hostBindings");
            var nativeContext=effectInvocation("scope",Optional.empty(),OptionalLong.empty(),valid.authorize());
            var effectInvocation=new KeirRuntime.EffectInvocation2("scope",Map.of("actorId",invalid),valid.deadline(),valid.authorize(),Optional.empty(),OptionalLong.empty(),KEY,nativeContext.event());
            failed(assertDoesNotThrow(()->KeirRuntime.invokeEffectProgram(effect,Map.of("taskId","task1"),effectInvocation)),"HOST_BINDING_INVALID","/hostBindings");
        }
        assertEquals(0,callbacks.get());
    }

    @Test void cyclicAndDeepEventsReturnSafeOutcomesWithoutCallingAuthorizationOrAdapters() throws Exception {
        AtomicInteger callbacks=new AtomicInteger();
        var prepared=prepared("effect","main",bound("effect","scope",effects((a,h,ms,c)->{callbacks.incrementAndGet();return done(effectRead("COMPLETED"));},(a,h,ms,c)->done(confirmed()))));
        var valid=effectInvocation("scope",Optional.empty(),OptionalLong.empty(),r->{callbacks.incrementAndGet();return done(true);});
        for(var invalid:List.of(cycle(),deep())) {
            var invocation=new KeirRuntime.EffectInvocation2("scope",valid.bindings(),valid.deadline(),valid.authorize(),Optional.empty(),OptionalLong.empty(),KEY,Map.of("id",invalid,"source","task1"));
            failed(assertDoesNotThrow(()->KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation)),"HOST_CONTEXT_INVALID","");
        }
        assertEquals(0,callbacks.get());
    }

    @Test void validPolicyBindingsAndEventRemainSnapshotsAcrossPendingAuthorization() throws Exception {
        var authorization=new CompletableFuture<Object>();var seen=new ArrayList<KeirRuntime.HostContext>();
        var prepared=prepared("effect","main",bound("effect","scope",effects((a,h,ms,c)->{assertEquals("user1",h.get("actorId"));seen.add(c);return done(effectRead("COMPLETED"));},
                (a,h,ms,c)->{assertEquals("user1",h.get("actorId"));seen.add(c);return done(confirmed());})));
        var bindings=new LinkedHashMap<String,Object>(Map.of("actorId","user1"));
        var event=new LinkedHashMap<String,Object>(Map.of("id","event1","source","task1"));
        var allow=new ArrayList<Object>(List.of("tasks.get","tasks.create"));
        var policy=new LinkedHashMap<String,Object>(Map.of("allow",allow));
        var invocation=new KeirRuntime.EffectInvocation2("scope",bindings,new KeirRuntime.Deadline(()->0,1000),r->r.size()==1?authorization:done(true),Optional.of(policy),OptionalLong.empty(),KEY,event);
        var pending=KeirRuntime.invokeEffectProgram(prepared,Map.of("taskId","task1"),invocation);
        bindings.put("actorId","changed");event.put("source","changed");allow.clear();policy.put("maxCalls",Map.of("tasks.create",0L));
        authorization.complete(true);var outcome=await(pending);
        assertEquals("COMPLETED",outcome.classification());assertEquals(Map.of("status","CREATED"),outcome.result());
        assertEquals(2,seen.size());for(var context:seen){assertSame(KEY,context.requestKey());assertEquals(Map.of("id","event1","source","task1"),context.event());}
    }
}
