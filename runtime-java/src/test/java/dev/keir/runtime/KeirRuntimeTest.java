package dev.keir.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeirRuntimeTest {
    private record Call(Map<String,Object> agent, Map<String,Object> protectedArgs, long remaining) {}

    private static byte[] bytes(String resource) throws IOException {
        try (var input = KeirRuntimeTest.class.getResourceAsStream(resource)) {
            assertNotNull(input,resource); return input.readAllBytes();
        }
    }
    private static byte[] bytes() throws IOException { return bytes("/read-and-decide/artifact.json"); }
    private static Map<String,Object> map(Object value) { return RuntimeData.map(value,"/artifact"); }
    private static Map<String,Object> admission(byte[] bytes) {
        Map<String,Object> artifact = map(RuntimeData.parseCanonical(bytes));
        Map<String,Object> model = map(artifact.get("dcm"));
        Map<String,Object> entry = map(map(model.get("entrypoints")).get(artifact.get("entrypoint")));
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("expectedArtifactDigest",RuntimeData.digest(bytes)); result.put("expectedDcmDigest",artifact.get("dcmDigest"));
        result.put("entrypoint",artifact.get("entrypoint")); result.put("compiler",artifact.get("compiler"));
        result.put("consumer",entry.get("consumer")); result.put("environment",artifact.get("environment"));
        result.put("mode","simulation"); result.put("grants",new LinkedHashMap<>(Map.of("tasks.get",1L)));
        return result;
    }
    private static Map<String,KeirRuntime.Adapter> adapters(KeirRuntime.AdapterFunction adapter) {
        return new LinkedHashMap<>(Map.of("tasks.get",new KeirRuntime.Adapter("1","simulation",adapter)));
    }
    private static CompletionStage<?> success(Map<String,Object> agent) {
        return CompletableFuture.completedFuture(Map.of("kind","SUCCESS","value",Map.of("id",agent.get("taskId"),"status","COMPLETED")));
    }
    private static KeirRuntime.Prepared prepared(KeirRuntime.AdapterFunction adapter) throws IOException {
        byte[] bytes = bytes(); return prepared(bytes,admission(bytes),adapters(adapter));
    }
    private static KeirRuntime.Prepared prepared(byte[] bytes,Map<String,Object> admission,Map<String,KeirRuntime.Adapter> adapters) {
        KeirRuntime.Preparation result = KeirRuntime.prepare(bytes,admission,adapters);
        assertTrue(result.succeeded(),()->String.valueOf(result.diagnostic())); assertNotNull(result.prepared()); assertNull(result.diagnostic());
        return result.prepared();
    }
    private static KeirRuntime.Invocation invocation() {
        return invocation(Map.of("actorId","actor-a"),request->CompletableFuture.completedFuture(true),Map.of("tasks.get",1L));
    }
    private static KeirRuntime.Invocation invocation(Map<String,Object> bindings,KeirRuntime.Authorizer authorize,Map<String,Long> grants) {
        return new KeirRuntime.Invocation(bindings,new KeirRuntime.Deadline(()->0L,10_000L),authorize,grants);
    }
    private static KeirRuntime.Outcome await(CompletionStage<KeirRuntime.Outcome> result) throws Exception {
        return result.toCompletableFuture().get(5,TimeUnit.SECONDS);
    }
    private static KeirRuntime.Outcome run(KeirRuntime.Prepared prepared) throws Exception {
        return await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation()));
    }
    private static void error(KeirRuntime.Outcome outcome,String classification,String code,String path) {
        assertEquals(classification,outcome.classification()); assertEquals(Map.of("code",code,"path",path),outcome.error());
        assertNull(outcome.result()); assertFalse(outcome.toMap().containsKey("result"));
        assertEquals("TERMINATED",outcome.trace().getLast().get("event")); assertEquals(code,outcome.trace().getLast().get("code"));
    }
    private static void diagnostic(KeirRuntime.Preparation preparation,String code,String path) {
        assertFalse(preparation.succeeded()); assertNull(preparation.prepared()); assertNotNull(preparation.diagnostic());
        assertEquals(code,preparation.diagnostic().code()); assertEquals(path,preparation.diagnostic().path());
        assertEquals("The selected artifact or host configuration is invalid.",preparation.diagnostic().message());
    }
    private static List<String> events(KeirRuntime.Outcome outcome) { return outcome.trace().stream().map(event->(String)event.get("event")).toList(); }

    @Test void preparationAuthenticatesRawBytesBeforeParsingAndNeverCallsAdapters() throws Exception {
        byte[] bytes=bytes(); AtomicInteger calls=new AtomicInteger();
        Map<String,KeirRuntime.Adapter> adapters=adapters((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        KeirRuntime.Preparation good=KeirRuntime.prepare(bytes,admission(bytes),adapters);
        assertTrue(good.succeeded()); assertEquals(0,calls.get());
        byte[] malformed="{".getBytes(StandardCharsets.UTF_8);
        diagnostic(KeirRuntime.prepare(malformed,admission(bytes),adapters),"PIN_MISMATCH","/artifact");
        Map<String,Object> selected=admission(bytes); selected.put("expectedArtifactDigest",RuntimeData.digest(malformed));
        diagnostic(KeirRuntime.prepare(malformed,selected,adapters),"ARTIFACT_INVALID","/artifact");
        diagnostic(KeirRuntime.prepare(new byte[1_048_577],admission(bytes),adapters),"BUDGET_EXCEEDED","/artifact");
        byte[] noncanonical=Arrays.copyOf(bytes,bytes.length+1); noncanonical[bytes.length]='\n';
        selected=admission(bytes); selected.put("expectedArtifactDigest",RuntimeData.digest(noncanonical));
        diagnostic(KeirRuntime.prepare(noncanonical,selected,adapters),"ARTIFACT_INVALID","/artifact");
        assertEquals(0,calls.get());
    }

    @Test void hostSelectorsAndAdapterRegistrationMustMatchIndependently() throws Exception {
        byte[] bytes=bytes(); AtomicInteger calls=new AtomicInteger();
        Map<String,KeirRuntime.Adapter> adapters=adapters((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        for(String selector:List.of("expectedDcmDigest","entrypoint","compiler")) {
            Map<String,Object> selected=admission(bytes);
            selected.put(selector,selector.equals("expectedDcmDigest")?"sha256:"+"0".repeat(64):"other");
            diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"PIN_MISMATCH","/admission");
        }
        Map<String,Object> selected=admission(bytes); map(selected.get("consumer")).put("id","other.consumer");
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"PIN_MISMATCH","/admission");
        selected=admission(bytes); map(selected.get("environment")).put("celNode","other");
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"PIN_MISMATCH","/admission");
        for(Map<String,KeirRuntime.Adapter> invalid:List.of(Map.<String,KeirRuntime.Adapter>of(),
                Map.of("tasks.get",new KeirRuntime.Adapter("2","simulation",(agent,host,left)->success(agent))),
                Map.of("tasks.get",new KeirRuntime.Adapter("1","real",(agent,host,left)->success(agent))),
                Map.of("unknown",new KeirRuntime.Adapter("1","simulation",(agent,host,left)->success(agent)))))
            diagnostic(KeirRuntime.prepare(bytes,admission(bytes),invalid),"ADMISSION_INVALID","/admission/adapters");
        assertEquals(0,calls.get());
    }

    @Test void admissionGrantsAndLimitsOnlyNarrowDeclaredAuthority() throws Exception {
        byte[] bytes=bytes(); Map<String,KeirRuntime.Adapter> adapters=adapters((agent,host,left)->success(agent));
        Map<String,Object> selected=admission(bytes); selected.remove("grants");
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"ADMISSION_INVALID","/admission");
        selected=admission(bytes); selected.put("grants",Map.of());
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"AUTHORITY_DENIED","/admission/grants");
        for(Map<String,Object> grants:List.of(Map.<String,Object>of("tasks.get",0L),Map.<String,Object>of("tasks.get",2L),Map.<String,Object>of("unknown",1L))) {
            selected=admission(bytes); selected.put("grants",grants);
            diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"ADMISSION_INVALID","/admission/grants");
        }
        selected=admission(bytes); selected.put("limits",Map.of("steps",1L));
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"BUDGET_EXCEEDED","/artifact/steps");
        selected=admission(bytes); selected.put("limits",Map.of("totalAstNodes",0L));
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"BUDGET_EXCEEDED","/artifact/expressions");
        selected=admission(bytes); selected.put("limits",Map.of("traceEvents",65L));
        diagnostic(KeirRuntime.prepare(bytes,selected,adapters),"ADMISSION_INVALID","/admission/limits");
    }

    @Test void onePreparedSnapshotServesIndependentCallersWithFreshProtectedBindings() throws Exception {
        byte[] bytes=bytes(); Map<String,Object> selected=admission(bytes); Queue<Call> calls=new ConcurrentLinkedQueue<>();
        Map<String,KeirRuntime.Adapter> registrations=adapters((agent,host,left)->{
            calls.add(new Call(agent,host,left));
            return CompletableFuture.completedFuture(Map.of("kind","SUCCESS","value",Map.of("id",agent.get("taskId"),"status",agent.get("taskId").equals("task-a")?"COMPLETED":"OPEN")));
        });
        KeirRuntime.Prepared prepared=prepared(bytes,selected,registrations);
        registrations.clear(); selected.clear(); Arrays.fill(bytes,(byte)0);
        CompletionStage<KeirRuntime.Outcome> first=KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation());
        CompletionStage<KeirRuntime.Outcome> second=KeirRuntime.invoke(prepared,Map.of("taskId","task-b"),invocation(Map.of("actorId","actor-b"),request->CompletableFuture.completedFuture(true),Map.of("tasks.get",1L)));
        assertEquals(Map.of("classification","DONE"),await(first).result()); assertEquals(Map.of("classification","PENDING"),await(second).result());
        assertEquals(2,calls.size()); Map<String,Call> byTask=new HashMap<>(); calls.forEach(call->byTask.put((String)call.agent.get("taskId"),call));
        assertEquals(Map.of("actorId","actor-a"),byTask.get("task-a").protectedArgs); assertEquals(Map.of("actorId","actor-b"),byTask.get("task-b").protectedArgs);
        for(Call call:calls) {
            assertEquals(Set.of("taskId"),call.agent.keySet()); assertTrue(call.remaining>0 && call.remaining<=10_000);
            assertThrows(UnsupportedOperationException.class,()->call.agent.put("actorId","override"));
            assertThrows(UnsupportedOperationException.class,()->call.protectedArgs.clear());
        }
    }

    @Test void successfulTraceIsExactOrderedRedactedAndImmutable() throws Exception {
        Queue<Map<String,Object>> requests=new ConcurrentLinkedQueue<>();
        KeirRuntime.Prepared prepared=prepared((agent,host,left)->success(agent));
        KeirRuntime.Outcome result=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of("actorId","actor-a"),request->{requests.add(request);return CompletableFuture.completedFuture(true);},Map.of("tasks.get",1L))));
        assertEquals("COMPLETED",result.classification()); assertEquals(Map.of("classification","DONE"),result.result()); assertNull(result.error());
        assertEquals(List.of(Map.of("entrypoint","ClassifyTask"),Map.of("entrypoint","ClassifyTask","capability","tasks.get","version","1")),new ArrayList<>(requests));
        assertEquals(List.of(
            Map.of("sequence",0,"event","INVOCATION_START"),Map.of("sequence",1,"event","STEP_START","step","source"),
            Map.of("sequence",2,"event","CALL_DISPATCHED","step","source","capability","tasks.get"),Map.of("sequence",3,"event","CALL_SUCCEEDED","step","source","capability","tasks.get"),
            Map.of("sequence",4,"event","STEP_START","step","completed"),Map.of("sequence",5,"event","VALUE_BOUND","step","completed"),
            Map.of("sequence",6,"event","RETURNED"),Map.of("sequence",7,"event","TERMINATED")),result.trace());
        assertArrayEquals(RuntimeData.canonical(result.toMap()),result.toJson()); RuntimeData.parseCanonical(result.toJson());
        String trace=new String(RuntimeData.canonical(result.trace()),StandardCharsets.UTF_8);
        assertFalse(trace.contains("task-a")); assertFalse(trace.contains("actor-a")); assertFalse(trace.contains("COMPLETED"));
        assertThrows(UnsupportedOperationException.class,()->result.result().clear());
        assertThrows(UnsupportedOperationException.class,()->result.trace().clear());
        assertThrows(UnsupportedOperationException.class,()->result.trace().getFirst().clear());
        assertThrows(UnsupportedOperationException.class,()->result.toMap().clear());
        for(Map<String,Object> request:requests) assertThrows(UnsupportedOperationException.class,()->request.clear());
    }

    @Test void inputAndProtectedBindingsAreRejectedBeforeAuthorization() throws Exception {
        AtomicInteger calls=new AtomicInteger(),checks=new AtomicInteger(); KeirRuntime.Prepared prepared=prepared((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        KeirRuntime.Authorizer authorize=request->{checks.incrementAndGet();return CompletableFuture.completedFuture(true);};
        KeirRuntime.Invocation invocation=invocation(Map.of("actorId","actor-a"),authorize,Map.of("tasks.get",1L));
        error(await(KeirRuntime.invoke(prepared,Map.of("taskId",3),invocation)),"FAILED","INPUT_INVALID","/input");
        error(await(KeirRuntime.invoke(prepared,Map.of("taskId","x".repeat(257)),invocation)),"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/input/taskId");
        error(await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a","extra",true),invocation)),"FAILED","INPUT_INVALID","/input");
        error(await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of(),authorize,Map.of("tasks.get",1L)))),"FAILED","HOST_BINDING_INVALID","/hostBindings");
        assertEquals(0,checks.get()); assertEquals(0,calls.get());
    }

    @Test void freshGrantsCannotWidenAndMissingPermissionStopsBeforeCallAuthorization() throws Exception {
        AtomicInteger calls=new AtomicInteger(); Queue<Map<String,Object>> requests=new ConcurrentLinkedQueue<>();
        KeirRuntime.Prepared prepared=prepared((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        KeirRuntime.Authorizer authorize=request->{requests.add(request);return CompletableFuture.completedFuture(true);};
        for(Map<String,Long> grants:List.of(Map.of("tasks.get",0L),Map.of("tasks.get",2L),Map.of("unknown",1L)))
            error(await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of("actorId","actor-a"),authorize,grants))),"FAILED","HOST_CONTEXT_INVALID","");
        assertTrue(requests.isEmpty());
        KeirRuntime.Outcome denied=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of("actorId","actor-a"),authorize,Map.of())));
        error(denied,"FAILED","AUTHORITY_DENIED","/steps/0");
        assertEquals(List.of(Map.of("entrypoint","ClassifyTask")),new ArrayList<>(requests)); assertEquals(0,calls.get());
        assertEquals(List.of("INVOCATION_START","STEP_START","TERMINATED"),events(denied));
    }

    @Test void authorizationFailuresAreSafeAndCapabilityDenialPreventsDispatch() throws Exception {
        AtomicInteger calls=new AtomicInteger(); KeirRuntime.Prepared prepared=prepared((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        List<KeirRuntime.Authorizer> failing=List.of(request->{throw new IllegalStateException("private callback detail");},
                request->CompletableFuture.failedFuture(new IllegalStateException("private callback detail")),request->CompletableFuture.completedFuture("yes"));
        for(KeirRuntime.Authorizer authorize:failing) {
            KeirRuntime.Outcome outcome=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of("actorId","actor-a"),authorize,Map.of("tasks.get",1L))));
            error(outcome,"FAILED","AUTHORITY_CHECK_FAILED",""); assertFalse(new String(outcome.toJson(),StandardCharsets.UTF_8).contains("private"));
        }
        Queue<Map<String,Object>> requests=new ConcurrentLinkedQueue<>();
        KeirRuntime.Outcome denied=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of("actorId","actor-a"),request->{requests.add(request);return CompletableFuture.completedFuture(!request.containsKey("capability"));},Map.of("tasks.get",1L))));
        error(denied,"FAILED","AUTHORITY_DENIED","/steps/0"); assertEquals(2,requests.size()); assertEquals(0,calls.get());
        assertFalse(events(denied).contains("CALL_DISPATCHED"));
    }

    @Test void invocationDataIsSnapshottedBeforeAsynchronousAuthorization() throws Exception {
        Map<String,Object> input=new LinkedHashMap<>(Map.of("taskId","task-a")),bindings=new LinkedHashMap<>(Map.of("actorId","actor-a"));
        Map<String,Long> grants=new LinkedHashMap<>(Map.of("tasks.get",1L)); CompletableFuture<Boolean> release=new CompletableFuture<>();
        CountDownLatch started=new CountDownLatch(1); AtomicInteger checks=new AtomicInteger(); Queue<Call> calls=new ConcurrentLinkedQueue<>();
        KeirRuntime.Prepared prepared=prepared((agent,host,left)->{calls.add(new Call(agent,host,left));return success(agent);});
        KeirRuntime.Authorizer authorize=request->{if(checks.incrementAndGet()==1){started.countDown();return release;}return CompletableFuture.completedFuture(true);};
        CompletionStage<KeirRuntime.Outcome> pending=KeirRuntime.invoke(prepared,input,invocation(bindings,authorize,grants));
        assertTrue(started.await(1,TimeUnit.SECONDS)); input.put("taskId","changed"); bindings.put("actorId","changed"); grants.put("tasks.get",2L); release.complete(true);
        assertEquals("COMPLETED",await(pending).classification()); assertEquals(1,calls.size());
        assertEquals(Map.of("taskId","task-a"),calls.peek().agent); assertEquals(Map.of("actorId","actor-a"),calls.peek().protectedArgs);
    }

    @Test void adapterOutcomesAreClosedAndFailuresStopLaterSteps() throws Exception {
        record Case(Object response,String classification,String code) {}
        List<Case> cases=List.of(new Case(Map.of("kind","BUSINESS_REJECTION","code","TASK_NOT_FOUND"),"BUSINESS_REJECTION","TASK_NOT_FOUND"),
                new Case(Map.of("kind","TRANSIENT_FAILURE"),"FAILED","ADAPTER_TRANSIENT"),
                new Case(Map.of("kind","BUSINESS_REJECTION","code","UNDECLARED"),"FAILED","ADAPTER_INVALID"),
                new Case(Map.of("kind","SUCCESS","value",Map.of("id","task-a","status","UNKNOWN")),"FAILED","ADAPTER_INVALID"),
                new Case(Map.of("kind","SUCCESS","value",Map.of("id","task-a")),"FAILED","ADAPTER_INVALID"),
                new Case(Map.of("kind","SUCCESS","value",Map.of("id","task-a","status","COMPLETED"),"extra","private adapter detail"),"FAILED","ADAPTER_INVALID"),
                new Case(null,"FAILED","ADAPTER_INVALID"));
        for(Case item:cases) {
            AtomicInteger calls=new AtomicInteger(); KeirRuntime.Outcome outcome=run(prepared((agent,host,left)->{calls.incrementAndGet();return CompletableFuture.completedFuture(item.response);}));
            error(outcome,item.classification,item.code,"/steps/0"); assertEquals(1,calls.get());
            assertEquals(List.of("INVOCATION_START","STEP_START","CALL_DISPATCHED","TERMINATED"),events(outcome));
            assertFalse(new String(outcome.toJson(),StandardCharsets.UTF_8).contains("private"));
        }
        for(KeirRuntime.AdapterFunction failing:List.<KeirRuntime.AdapterFunction>of((agent,host,left)->{throw new IllegalStateException("private adapter detail");},
                (agent,host,left)->CompletableFuture.failedFuture(new IllegalStateException("private adapter detail")))) {
            KeirRuntime.Outcome outcome=run(prepared(failing)); error(outcome,"FAILED","ADAPTER_EXCEPTION","/steps/0");
            assertFalse(new String(outcome.toJson(),StandardCharsets.UTF_8).contains("private"));
        }
        KeirRuntime.Outcome oversized=run(prepared((agent,host,left)->CompletableFuture.completedFuture(Map.of("kind","SUCCESS","value",Map.of("id","x".repeat(257),"status","COMPLETED")))));
        error(oversized,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/steps/0/id");
    }

    @Test void deadlineIsCheckedAfterAuthorizationAndLateAdapterResultsAreDiscarded() throws Exception {
        AtomicLong now=new AtomicLong(); AtomicInteger calls=new AtomicInteger(),checks=new AtomicInteger();
        KeirRuntime.Prepared prepared=prepared((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        KeirRuntime.Invocation invocation=new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(now::get,1000),
                request->{if(checks.incrementAndGet()==2)now.set(1000);return CompletableFuture.completedFuture(true);},Map.of("tasks.get",1L));
        KeirRuntime.Outcome expired=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation));
        error(expired,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/steps/0"); assertEquals(0,calls.get()); assertFalse(events(expired).contains("CALL_DISPATCHED"));
        now.set(0); CompletableFuture<Object> release=new CompletableFuture<>(); CountDownLatch dispatched=new CountDownLatch(1);
        prepared=prepared((agent,host,left)->{dispatched.countDown();return release;});
        invocation=new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(now::get,1000),request->CompletableFuture.completedFuture(true),Map.of("tasks.get",1L));
        CompletionStage<KeirRuntime.Outcome> pending=KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation);
        assertTrue(dispatched.await(1,TimeUnit.SECONDS)); now.set(1000); release.complete(Map.of("kind","SUCCESS","value",Map.of("id","task-a","status","COMPLETED")));
        KeirRuntime.Outcome late=await(pending); error(late,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/steps/0"); assertFalse(events(late).contains("CALL_SUCCEEDED"));
    }

    @Test void traceCapacityIsReservedBeforeDispatchAndNativeAsyncWaitExpires() throws Exception {
        byte[] bytes=bytes(); Map<String,Object> selected=admission(bytes); selected.put("limits",Map.of("traceEvents",4L)); AtomicInteger calls=new AtomicInteger();
        KeirRuntime.Prepared prepared=prepared(bytes,selected,adapters((agent,host,left)->{calls.incrementAndGet();return success(agent);}));
        KeirRuntime.Outcome limited=run(prepared); error(limited,"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/steps/0"); assertEquals(0,calls.get()); assertFalse(events(limited).contains("CALL_DISPATCHED"));
        CompletableFuture<Boolean> release=new CompletableFuture<>();
        KeirRuntime.Invocation invocation=new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(()->0,20),request->release,Map.of("tasks.get",1L));
        KeirRuntime.Outcome timedOut=await(KeirRuntime.invoke(prepared((agent,host,left)->{calls.incrementAndGet();return success(agent);}),Map.of("taskId","task-a"),invocation));
        error(timedOut,"LIMIT_EXCEEDED","BUDGET_EXCEEDED",""); release.complete(false); assertEquals(0,calls.get());
        assertEquals(List.of("INVOCATION_START","TERMINATED"),events(timedOut));
    }

    @Test void scalarFixturePreservesFalseEmptyUnicodeAndReservedFieldNames() throws Exception {
        byte[] bytes=bytes("/supplemental/scalars.artifact.json"); Queue<Call> calls=new ConcurrentLinkedQueue<>();
        KeirRuntime.Prepared prepared=prepared(bytes,admission(bytes),adapters((agent,host,left)->{calls.add(new Call(agent,host,left));return success(agent);}));
        Map<String,Object> input=new LinkedHashMap<>(); input.put("constructor","task-a"); input.put("flag",false); input.put("choice","DONE");
        for(String name:List.of("toString","toJSON","valueOf","input","text0","text1","text2","text3"))input.put(name,"");
        input.put("toString","é😀"); input.put("toJSON","e\u0301"); input.put("valueOf","\0\t\n\"\\"); input.put("text0","é".repeat(128));
        KeirRuntime.Outcome outcome=await(KeirRuntime.invoke(prepared,input,invocation())); assertEquals("COMPLETED",outcome.classification());
        Map<String,Object> expected=new LinkedHashMap<>(input); expected.remove("flag"); expected.remove("choice"); expected.put("classification","PENDING"); expected.put("leftEnum","PENDING"); expected.put("rightEnum","DONE");
        assertEquals(expected,outcome.result()); assertEquals(1,calls.size()); assertEquals(Map.of("taskId","task-a"),calls.peek().agent);
        input.put("text0","x".repeat(257)); error(await(KeirRuntime.invoke(prepared,input,invocation())),"LIMIT_EXCEEDED","BUDGET_EXCEEDED","/input/text0"); assertEquals(1,calls.size());
    }

    @Test void literalFixtureRequiresNeitherCallAuthorityNorAnAdapter() throws Exception {
        byte[] bytes=bytes("/supplemental/literal.artifact.json"); Map<String,Object> selected=admission(bytes); selected.put("grants",Map.of());
        selected.put("limits",Map.of("steps",1L,"calls",0L,"totalAstNodes",0L,"traceEvents",5L));
        KeirRuntime.Prepared prepared=prepared(bytes,selected,Map.of());
        KeirRuntime.Outcome outcome=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of(),request->CompletableFuture.completedFuture(true),Map.of())));
        assertEquals("COMPLETED",outcome.classification()); assertEquals(Map.of("classification","PENDING"),outcome.result());
        assertEquals(List.of("INVOCATION_START","STEP_START","VALUE_BOUND","RETURNED","TERMINATED"),events(outcome));
    }

    @Test void invalidTrustedContextFailsClosedBeforeAuthorization() throws Exception {
        AtomicInteger calls=new AtomicInteger(),checks=new AtomicInteger();
        KeirRuntime.Prepared prepared=prepared((agent,host,left)->{calls.incrementAndGet();return success(agent);});
        KeirRuntime.Authorizer authorize=request->{checks.incrementAndGet();return CompletableFuture.completedFuture(true);};
        Map<String,Object> input=Map.of("taskId","task-a");
        error(await(KeirRuntime.invoke(null,input,invocation())),"FAILED","HOST_CONTEXT_INVALID","");
        error(await(KeirRuntime.invoke(prepared,input,null)),"FAILED","HOST_CONTEXT_INVALID","");
        for (KeirRuntime.Invocation invalid:List.of(
                new KeirRuntime.Invocation(Map.of("actorId","actor-a"),null,authorize,Map.of("tasks.get",1L)),
                new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(()->0,1000),null,Map.of("tasks.get",1L)),
                new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(()->-1,1000),authorize,Map.of("tasks.get",1L)),
                new KeirRuntime.Invocation(Map.of("actorId","actor-a"),new KeirRuntime.Deadline(()->0,1000),authorize,null)))
            error(await(KeirRuntime.invoke(prepared,input,invalid)),"FAILED","HOST_CONTEXT_INVALID","");
        assertEquals(0,calls.get()); assertEquals(0,checks.get());
    }

    @Test void freshGrantExhaustionStopsASecondCallBeforeItsAuthorization() throws Exception {
        Map<String,Object> artifact=map(RuntimeData.parseCanonical(bytes()));
        Map<String,Object> model=map(artifact.get("dcm"));
        map(map(model.get("entrypoints")).get("ClassifyTask")).put("grants",Map.of("tasks.get",2L));
        artifact.put("dcmDigest",RuntimeData.digest(RuntimeData.canonical(model)));
        List<Object> steps=RuntimeData.list(artifact.get("steps"),"");
        Map<String,Object> first=map(steps.getFirst()),second=new LinkedHashMap<>(first),bind=map(steps.get(1));
        second.put("id","other"); second.put("args",Map.of("taskId",Map.of("expr","e1","type","TaskId")));
        map(bind.get("value")).put("expr","e2");
        map(map(map(artifact.get("return")).get("fields")).get("classification")).put("expr","e3");
        Map<String,Object> original=map(artifact.get("expressions")),expressions=new LinkedHashMap<>();
        expressions.put("e0",original.get("e0"));
        Map<String,Object> secondInput=new LinkedHashMap<>(map(original.get("e0")));
        secondInput.put("variables",Map.of("input","ClassifyInput","source","Task")); expressions.put("e1",secondInput);
        Map<String,Object> completed=new LinkedHashMap<>(map(original.get("e1")));
        completed.put("variables",Map.of("input","ClassifyInput","source","Task","other","Task")); expressions.put("e2",completed);
        Map<String,Object> returned=new LinkedHashMap<>(map(original.get("e2")));
        returned.put("variables",Map.of("input","ClassifyInput","source","Task","other","Task","completed","Boolean")); expressions.put("e3",returned);
        artifact.put("expressions",expressions); artifact.put("steps",List.of(first,second,bind));
        byte[] bytes=RuntimeData.canonical(artifact); Map<String,Object> selected=admission(bytes); selected.put("grants",Map.of("tasks.get",2L));
        Queue<Call> calls=new ConcurrentLinkedQueue<>(); Queue<Map<String,Object>> requests=new ConcurrentLinkedQueue<>();
        KeirRuntime.Prepared prepared=prepared(bytes,selected,adapters((agent,host,left)->{calls.add(new Call(agent,host,left));return success(agent);}));
        KeirRuntime.Outcome outcome=await(KeirRuntime.invoke(prepared,Map.of("taskId","task-a"),invocation(Map.of("actorId","actor-a"),
                request->{requests.add(request);return CompletableFuture.completedFuture(true);},Map.of("tasks.get",1L))));
        error(outcome,"FAILED","AUTHORITY_DENIED","/steps/1");
        assertEquals(1,calls.size()); assertEquals(List.of(Map.of("entrypoint","ClassifyTask"),
                Map.of("entrypoint","ClassifyTask","capability","tasks.get","version","1")),new ArrayList<>(requests));
        assertEquals(List.of("INVOCATION_START","STEP_START","CALL_DISPATCHED","CALL_SUCCEEDED","STEP_START","TERMINATED"),events(outcome));
        assertEquals("other",outcome.trace().get(4).get("step"));
    }
}
