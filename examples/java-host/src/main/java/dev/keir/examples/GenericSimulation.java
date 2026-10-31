package dev.keir.examples;

import dev.keir.runtime.KeirRuntime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static dev.keir.examples.EffectSimulation.*;

/** Ordinary runtime execution with independent native-context and dispatch checks. */
final class GenericSimulation {
    record Program(byte[] bytes,String digest) { Program { bytes=bytes.clone(); } }
    static final Set<String> COMMON=Set.of("normal","invocation-denied","call-denied","wrong-scope","read-throws");
    static final Set<String> EFFECT=Set.of("zero-effect","revoked-after-read","invalid-key","wrong-origin","lost-response","response-expired");
    private GenericSimulation() {}
    private static void scalar(Object value) {
        check(value instanceof String text&&!text.isEmpty()&&text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=256,"Bounded scenario scalar"); EffectJson.encode(value);
    }
    private static void fields(Object value,String... names) { shape(value,names); for(String name:names) scalar(object(value).get(name)); }
    static void callShape(Object value,boolean stub,boolean effect) {
        var call=object(value); shape(call,stub?new String[]{"step","capability","version","args","protectedArgs","response"}:new String[]{"step","capability","version","args","protectedArgs"});
        check(call.get("step") instanceof String id&&id.matches("[A-Za-z][A-Za-z0-9]{0,63}"),"Invalid call step");
        check((effect?Set.of("tasks.get","tasks.create"):Set.of("tasks.get","tasks.lookup")).contains(call.get("capability"))&&"1".equals(call.get("version")),"Unknown simulation capability/version");
        boolean write="tasks.create".equals(call.get("capability"));
        fields(call.get("args"),write?new String[]{"projectId","title"}:new String[]{"taskId"}); fields(call.get("protectedArgs"),"actorId");
        if(!stub)return;
        var response=object(call.get("response"));
        switch((String)response.getOrDefault("kind","")) {
            case "SUCCESS" -> {
                shape(response,write?new String[]{"kind","value","effect"}:new String[]{"kind","value"});
                fields(response.get("value"),write?new String[]{"id"}:effect?new String[]{"id","projectId","status"}:new String[]{"id","status"});
                check(write?"CONFIRMED".equals(response.get("effect")):Set.of("OPEN","COMPLETED").contains(object(response.get("value")).get("status")),"Invalid success response");
            }
            case "BUSINESS_REJECTION" -> {
                shape(response,write?new String[]{"kind","code","effect"}:new String[]{"kind","code"});
                check((write?Set.of("CREATE_REJECTED","ACCESS_DENIED"):Set.of("TASK_NOT_FOUND","ACCESS_DENIED")).contains(response.get("code")),"Undeclared business code");
                check(!write||"NONE".equals(response.get("effect")),"Invalid rejection effect");
            }
            case "TRANSIENT_FAILURE" -> { shape(response,write?new String[]{"kind","effect"}:new String[]{"kind"}); check(!write||"NONE".equals(response.get("effect")),"Invalid transient effect"); }
            default -> { shape(response,"kind","effect"); check(write&&"UNKNOWN_COMMIT".equals(response.get("kind"))&&"UNKNOWN".equals(response.get("effect")),"Invalid response"); }
        }
    }
    static void validateScenario(Object value,boolean effect) {
        GenericCapacity.scenarios(0,0,EffectJson.encode(value).length,0,0); shape(value,"format","id","given","stubs","expect"); var row=object(value);
        check((effect?"keir-scenario/effect-2":"keir-scenario/read-2").equals(row.get("format")),"Unsupported scenario format");
        check(row.get("id") instanceof String id&&id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}"),"Invalid scenario ID");
        shape(row.get("given"),"input","fixture","program"); var given=object(row.get("given")); fields(given.get("input"),"taskId");
        check(COMMON.contains(given.get("fixture"))||effect&&EFFECT.contains(given.get("fixture")),"Unknown trusted fixture");
        check(GenericHost.programNames(effect?"effect":"read").contains(given.get("program")),"Unknown fixed program");
        var stubs=array(row.get("stubs")); GenericCapacity.scenarios(0,0,0,stubs.size(),0); stubs.forEach(stub->callShape(stub,true,effect));
        shape(row.get("expect"),"outcome","calls","consumption"); var expect=object(row.get("expect"));
        var calls=array(expect.get("calls")); GenericCapacity.scenarios(0,0,0,0,calls.size()); calls.forEach(call->callShape(call,false,effect));
        same(consume(expect.get("outcome"),effect),expect.get("consumption"),"Invalid expected consumer receipt");
        check((effect?"REPORTED":"DISPLAYED").equals(object(expect.get("consumption")).get("status"))||!effect&&"NOT_DISPLAYED".equals(object(expect.get("consumption")).get("status")),"Invalid expected outcome");
    }
    static Map<String,Object> consume(Object value,boolean effect) {
        if(effect)return EffectConsumer.consume(value);
        var outcome=object(value); String classification=(String)outcome.get("classification");
        check(Set.of("classification","result","error","trace").containsAll(outcome.keySet()),"Invalid read outcome fields");
        var result=outcome.containsKey("result")?object(outcome.get("result")):null;
        var error=outcome.containsKey("error")?object(outcome.get("error")):null;
        var checked=new LinkedHashMap<String,Object>(outcome); checked.put("effects",List.of());
        if(result!=null) { fields(result,"classification"); check(Set.of("DONE","PENDING").contains(result.get("classification")),"Invalid classification result"); checked.put("result",Map.of("status","CREATED")); }
        check("REPORTED".equals(EffectConsumer.consume(checked).get("status")),"Invalid read outcome");
        return JavaHost.consume(classification,result,error);
    }
    static List<Map<String,Object>> batch(String profile,byte[] dcm,String dcmPin,Map<String,Program> programs,Object corpus) {
        boolean effect="effect".equals(profile); check(effect||"read".equals(profile),"Unknown profile");
        var rows=array(corpus); GenericCapacity.scenarios(EffectJson.encode(corpus).length,rows.size(),0,0,0); check(!rows.isEmpty(),"Empty scenario batch");
        var ids=new HashSet<Object>(); for(Object row:rows) { validateScenario(row,effect); check(ids.add(object(row).get("id")),"Duplicate scenario ID"); }
        check(programs.keySet().equals(Set.copyOf(GenericHost.programNames(profile))),"Incomplete fixed program map");
        try(var harness=new Harness(effect,dcm,dcmPin,programs)) {
            var results=new ArrayList<Map<String,Object>>(); for(Object row:rows)results.add(harness.run(object(row))); return results;
        }
    }
    static final class Active {
        final String fixture; final Map<String,Object> input,event; final UUID key=UUID.fromString("00000000-0000-4000-8000-000000000001");
        final List<Object> stubs; final List<String> violations=new ArrayList<>(); final List<Map<String,Object>> captures=new ArrayList<>();
        final AtomicLong now=new AtomicLong(); int used,authorized;
        Active(Map<String,Object> row) { var given=object(row.get("given")); fixture=(String)given.get("fixture"); input=object(given.get("input")); stubs=array(row.get("stubs")); event=Map.of("id","event1","source",fixture.equals("wrong-origin")?"other-task":"task1"); }
    }
    static final class Harness implements AutoCloseable {
        final boolean effect; final GenericCapacity capacity=new GenericCapacity(); final List<GenericCapacity.Lease> leases=new ArrayList<>();
        final Map<String,KeirRuntime.Prepared> prepared=new LinkedHashMap<>(); KeirRuntime.BoundModel model; Active active;
        Harness(boolean effect,byte[] dcm,String dcmPin,Map<String,Program> programs) {
            this.effect=effect;
            try {
                GenericCapacity.capacity(dcm.length<=65_536); var document=object(EffectJson.parse(dcm));
                // Reserve the full per-model descriptor ceiling; four models cannot exceed the aggregate ceiling.
                leases.add(capacity.model(dcm.length,EffectJson.encode(document).length,65_536));
                var approval=new LinkedHashMap<String,Object>(); approval.put("scope","sample-scope");
                var domain=object(document.get("domain"));
                approval.put("modelRef",Map.of("domainId",domain.get("id"),"domainVersion",domain.get("version"),"format",document.get("format"),"digest",dcmPin,"descriptorRecipe","keir-record-descriptors/1"));
                approval.put("profile",effect?"keir-effect/2":"keir-read/2"); approval.put("environment",KeirRuntime.ENVIRONMENT); approval.put("mode","simulation");
                if(effect)approval.put("keyBinding",new KeirRuntime.KeyBinding("sample.RequestUuid/1",candidate->active!=null&&candidate==active.key));
                var invokers=new LinkedHashMap<String,Object>();
                for(String id:object(document.get("capabilities")).keySet()) {
                    if(effect)invokers.put(id,new KeirRuntime.EffectAdapter("1","simulation","sample.RequestUuid/1",(args,trusted,left,host)->dispatch(id,args,trusted,left,host)));
                    else invokers.put(id,new KeirRuntime.Adapter("1","simulation",(args,trusted,left)->dispatch(id,args,trusted,left,null)));
                }
                try(var work=capacity.work()) { var bound=KeirRuntime.bindModel(dcm,approval,invokers); check(bound.succeeded(),"Model binding failed"); model=bound.boundModel(); }
                for(var entry:programs.entrySet()) {
                    Program program=entry.getValue(); leases.add(capacity.program(program.bytes().length));
                    var admission=new LinkedHashMap<String,Object>(); admission.put("expectedArtifactDigest",program.digest()); admission.put("expectedDcmDigest",dcmPin);
                    admission.put("entrypoint",effect?"TaskCompleted":"ClassifyTask"); admission.put("consumer",effect?Map.of("id","sample.followup.report","version","1","kind","EVENT_REPORT"):Map.of("id","sample.classification","version","1","kind","DISPLAY"));
                    admission.put("compiler","keir-compiler-java/0.1.0-SNAPSHOT"); admission.put("environment",KeirRuntime.ENVIRONMENT); admission.put("mode","simulation");
                    try(var work=capacity.work()) { var result=KeirRuntime.prepareProgram(program.bytes(),model,admission); check(result.succeeded(),"Program preparation failed"); prepared.put(entry.getKey(),result.prepared()); }
                }
            } catch(RuntimeException failure) { close(); throw failure; }
        }
        CompletionStage<?> dispatch(String capability,Map<String,Object> args,Map<String,Object> trusted,long left,KeirRuntime.HostContext host) {
            var current=active; Map<String,Object> selected;
            try {
                check(current!=null,"Dispatch outside invocation"); GenericCapacity.capacity(current.captures.size()<4);
                var captured=object(EffectJson.copy(Map.of("capability",capability,"version","1","args",args,"protectedArgs",trusted))); current.captures.add(captured);
                check(left>0&&left<=1000,"Invalid remaining deadline"); same(trusted,Map.of("actorId","user1"),"Protected actor mismatch");
                if(effect) { check(host!=null&&host.requestKey()==current.key,"Original native key mismatch"); same(host.event(),current.event,"Protected event mismatch"); }
                check(current.used<current.stubs.size(),"Unexpected dispatch"); selected=object(current.stubs.get(current.used));
                same(captured,Map.of("capability",selected.get("capability"),"version",selected.get("version"),"args",selected.get("args"),"protectedArgs",selected.get("protectedArgs")),"Adapter arguments mismatch");
                var response=object(selected.get("response"));
                if(!capability.equals("tasks.create")&&"SUCCESS".equals(response.get("kind")))check(Objects.equals(object(response.get("value")).get("id"),args.get("taskId")),"Read resource mismatch");
                if(capability.equals("tasks.create"))check(Objects.equals(current.event.get("source"),current.input.get("taskId")),"Protected origin mismatch");
                current.used++;
            } catch(RuntimeException failure) { if(current!=null)current.violations.add("Adapter arguments, native identity or fixture mismatch"); throw new IllegalStateException("Simulation adapter mismatch"); }
            if(current.fixture.equals("read-throws")&&!capability.equals("tasks.create")||current.fixture.equals("lost-response")&&capability.equals("tasks.create"))throw new IllegalStateException("Simulated unavailable response");
            if(current.fixture.equals("response-expired")&&capability.equals("tasks.create"))current.now.set(1000);
            return CompletableFuture.completedFuture(EffectJson.copy(selected.get("response")));
        }
        Map<String,Object> run(Map<String,Object> row) {
            check(active==null,"Concurrent scenario reuse"); var current=new Active(row); active=current;
            try(var invocation=capacity.invocation()) {
                String entrypoint=effect?"TaskCompleted":"ClassifyTask";
                KeirRuntime.Authorizer authorize=request->{
                    boolean entry=request.equals(Map.of("entrypoint",entrypoint));
                    boolean call=request.get("capability") instanceof String id&&Set.of("tasks.get","tasks.lookup","tasks.create").contains(id)&&request.equals(Map.of("entrypoint",entrypoint,"capability",id,"version","1"));
                    boolean allowed=(entry||call)&&!(current.fixture.equals("invocation-denied")&&entry)&&!(current.fixture.equals("call-denied")&&!entry&&current.authorized==1)
                        &&!(current.fixture.equals("revoked-after-read")&&!entry&&current.used>0)&&!(current.fixture.equals("wrong-origin")&&"tasks.create".equals(request.get("capability")));
                    current.authorized++; return CompletableFuture.completedFuture(allowed);
                };
                String scope=current.fixture.equals("wrong-scope")?"other-scope":"sample-scope";
                var deadline=new KeirRuntime.Deadline(current.now::get,1000); var handle=prepared.get(object(row.get("given")).get("program"));
                CompletionStage<KeirRuntime.Outcome> pending;
                if(effect)pending=KeirRuntime.invokeEffectProgram(handle,EffectJson.copy(current.input),new KeirRuntime.EffectInvocation2(scope,Map.of("actorId","user1"),deadline,authorize,Optional.empty(),current.fixture.equals("zero-effect")?OptionalLong.of(0):OptionalLong.empty(),current.fixture.equals("invalid-key")?new Object():current.key,current.event));
                else pending=KeirRuntime.invokeProgram(handle,EffectJson.copy(current.input),new KeirRuntime.Invocation2(scope,Map.of("actorId","user1"),deadline,authorize,Optional.empty()));
                KeirRuntime.Outcome outcome;
                try { outcome=pending.toCompletableFuture().get(3,TimeUnit.SECONDS); } catch(Exception failure) { throw new IllegalArgumentException("Simulation invocation failed"); }
                if(current.used!=current.stubs.size())current.violations.add("Unconsumed scenario stub");
                var calls=reconcile(current.captures,outcome.trace(),current.stubs,current.violations); check(current.violations.isEmpty(),"Simulation violations");
                var actual=Map.<String,Object>of("outcome",outcome.toMap(),"calls",calls,"consumption",consume(outcome.toMap(),effect));
                same(actual,row.get("expect"),"Simulation result mismatch"); var result=new LinkedHashMap<String,Object>(actual); result.put("id",row.get("id")); return result;
            } finally { active=null; }
        }
        public void close() { check(active==null,"Cannot release an in-flight model"); prepared.clear(); model=null; for(int i=leases.size()-1;i>=0;i--)leases.get(i).close(); leases.clear(); }
    }
}
