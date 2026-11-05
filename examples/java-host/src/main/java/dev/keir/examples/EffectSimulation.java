package dev.keir.examples;

import dev.keir.runtime.KeirRuntime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Fixed simulation fixtures exercise the ordinary prepared runtime, never a second plan evaluator. */
final class EffectSimulation {
    static final String GET="tasks.get", WRITE="tasks.createFollowUp";
    static final Set<String> FIXTURES=Set.of("normal","no-effect-grant","no-write-grant","write-denied","invocation-denied",
            "invalid-key","wrong-origin","read-throws","lost-response","response-expired","response-bad-clock",
            "trace-after-confirmed","simulated-duplicate","simulated-payload-mismatch");
    private EffectSimulation() {}
    static void check(boolean valid,String message) { ScenarioData.check(valid,message); }
    static Map<String,Object> object(Object value) { return ScenarioData.object(value); }
    static List<Object> array(Object value) { return ScenarioData.array(value); }
    static void shape(Object value,String... fields) { check(object(value).keySet().equals(Set.of(fields)),"Closed scenario fields"); }
    static void same(Object actual,Object expected,String message) { ScenarioData.same(actual,expected,message); }
    private static void scalar(Object value) {
        check(value instanceof String text && text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=256,"Bounded scenario scalar");
        EffectJson.encode(value);
    }
    private static void fields(Object value,String... names) {
        shape(value,names); for(String name:names) scalar(object(value).get(name));
    }
    private static void callShape(Object value,boolean stub) {
        var call=object(value);
        if(stub) shape(call,"step","capability","version","args","protectedArgs","response");
        else shape(call,"step","capability","version","args","protectedArgs");
        check(call.get("step") instanceof String id && id.matches("[A-Za-z][A-Za-z0-9]{0,63}"),"Invalid call step");
        check(Set.of(GET,WRITE).contains(call.get("capability")) && "1".equals(call.get("version")),"Unknown simulation capability/version");
        boolean write=WRITE.equals(call.get("capability"));
        fields(call.get("args"),write?new String[]{"sourceTaskId","projectId","title"}:new String[]{"taskId"});
        fields(call.get("protectedArgs"),"actorId");
        if(!stub) return;
        var response=object(call.get("response"));
        if("SUCCESS".equals(response.get("kind"))) {
            shape(response,write?new String[]{"kind","value","effect"}:new String[]{"kind","value"});
            fields(response.get("value"),write?new String[]{"id"}:new String[]{"id","projectId","status"});
            check(write?"CONFIRMED".equals(response.get("effect")):Set.of("OPEN","COMPLETED").contains(object(response.get("value")).get("status")),"Invalid success response");
        } else if("BUSINESS_REJECTION".equals(response.get("kind"))) {
            shape(response,write?new String[]{"kind","code","effect"}:new String[]{"kind","code"});
            check((write?Set.of("CREATE_REJECTED","ACCESS_DENIED"):Set.of("TASK_NOT_FOUND","ACCESS_DENIED")).contains(response.get("code")),"Undeclared business code");
            check(!write||"NONE".equals(response.get("effect")),"Invalid rejection effect");
        } else if("TRANSIENT_FAILURE".equals(response.get("kind"))) {
            shape(response,write?new String[]{"kind","effect"}:new String[]{"kind"});
            check(!write||"NONE".equals(response.get("effect")),"Invalid transient effect");
        } else {
            shape(response,"kind","effect"); check(write&&"UNKNOWN_COMMIT".equals(response.get("kind"))&&"UNKNOWN".equals(response.get("effect")),"Invalid unknown response");
        }
    }
    static void validateScenario(Object value) {
        check(EffectJson.encode(value).length<=65536,"Scenario exceeds byte limit");
        shape(value,"format","id","given","stubs","expect"); var row=object(value);
        check("keir-scenario/effect-1".equals(row.get("format")),"Unsupported scenario format");
        check(row.get("id") instanceof String id&&id.matches("[a-z0-9][a-z0-9_-]*"),"Invalid scenario ID");
        shape(row.get("given"),"input","fixture"); var given=object(row.get("given"));
        fields(given.get("input"),"taskId","projectId","title"); check(FIXTURES.contains(given.get("fixture")),"Unknown trusted fixture");
        var stubs=array(row.get("stubs")); check(stubs.size()<=4,"At most four stubs"); stubs.forEach(stub->callShape(stub,true));
        shape(row.get("expect"),"outcome","calls","consumption"); var expect=object(row.get("expect"));
        array(expect.get("calls")).forEach(call->callShape(call,false));
        var receipt=EffectConsumer.consume(expect.get("outcome"));
        same(receipt,expect.get("consumption"),"Invalid expected consumer receipt"); check("REPORTED".equals(receipt.get("status")),"Invalid expected outcome");
    }
    static List<Map<String,Object>> reconcile(List<Map<String,Object>> captures,List<Map<String,Object>> trace,List<Object> stubs,List<String> violations) {
        return ScenarioData.reconcile(captures,trace,stubs,violations);
    }
    static Map<String,Object> run(byte[] bytes,String artifactPin,String dcmPin,Object value) {
        validateScenario(value); var row=object(value); var given=object(row.get("given")); var input=object(given.get("input"));
        String fixture=(String)given.get("fixture");
        UUID key=UUID.fromString("00000000-0000-4000-8000-000000000001");
        var event=Map.<String,Object>of("id","event1","source",fixture.equals("wrong-origin")?"other-task":"task1");
        var violations=new ArrayList<String>(); var captures=new ArrayList<Map<String,Object>>(); var stubs=array(row.get("stubs"));
        AtomicLong now=new AtomicLong(); int[] used={0};
        var adapters=new LinkedHashMap<String,KeirRuntime.EffectAdapter>();
        for(String capability:List.of(GET,WRITE)) adapters.put(capability,new KeirRuntime.EffectAdapter("1","simulation","sample.RequestUuid/1",(args,protectedArgs,left,host)->{
            Map<String,Object> selected;
            try {
                var captured=object(EffectJson.copy(Map.of("capability",capability,"version","1","args",args,"protectedArgs",protectedArgs))); captures.add(captured);
                check(left>0&&left<=1000,"Invalid remaining budget"); check(host.requestKey()==key,"Native key identity mismatch"); same(host.event(),event,"Protected event mismatch");
                check("user1".equals(protectedArgs.get("actorId")),"Protected actor mismatch");
                check(used[0]<stubs.size(),"Unexpected adapter call"); selected=object(stubs.get(used[0]));
                same(captured,Map.of("capability",selected.get("capability"),"version",selected.get("version"),"args",selected.get("args"),"protectedArgs",selected.get("protectedArgs")),"Adapter call mismatch");
                var response=object(selected.get("response"));
                if(capability.equals(GET)&&"SUCCESS".equals(response.get("kind"))) check(Objects.equals(object(response.get("value")).get("id"),args.get("taskId")),"Read resource mismatch");
                if(capability.equals(WRITE)) check(Objects.equals(event.get("source"),args.get("sourceTaskId")),"Protected origin mismatch");
                if(capability.equals(WRITE)&&fixture.startsWith("simulated-")) {
                    check("event1".equals(event.get("id"))&&"task1".equals(args.get("sourceTaskId"))&&"project1".equals(args.get("projectId")),"Simulated prior payload mismatch");
                    if(fixture.equals("simulated-duplicate")) {
                        check("Follow up".equals(args.get("title")),"Simulated duplicate payload mismatch");
                        same(response,Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED"),"Simulated duplicate response mismatch");
                    } else {
                        check(!"Follow up".equals(args.get("title")),"Simulated changed payload required");
                        same(response,Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"),"Simulated payload rejection mismatch");
                    }
                }
                used[0]++;
            } catch(RuntimeException failure) {
                violations.add("Adapter arguments, native identity or fixture mismatch"); throw new IllegalStateException("Simulation adapter mismatch");
            }
            if(fixture.equals("read-throws")&&capability.equals(GET)||fixture.equals("lost-response")&&capability.equals(WRITE)) throw new IllegalStateException("Simulated unavailable response");
            if(capability.equals(WRITE)&&fixture.equals("response-expired")) now.set(1000);
            if(capability.equals(WRITE)&&fixture.equals("response-bad-clock")) now.set(-1);
            return CompletableFuture.completedFuture(EffectJson.copy(selected.get("response")));
        }));
        var admission=new LinkedHashMap<String,Object>();
        admission.put("expectedArtifactDigest",artifactPin); admission.put("expectedDcmDigest",dcmPin); admission.put("entrypoint","TaskCompleted");
        admission.put("consumer",Map.of("id","sample.followup.report","version","1","kind","EVENT_REPORT")); admission.put("compiler","keir-compiler-java/0.1.0-SNAPSHOT");
        admission.put("environment",KeirRuntime.ENVIRONMENT); admission.put("mode","simulation"); admission.put("grants",Map.of(GET,1L,WRITE,1L)); admission.put("effectGrant",1L);
        admission.put("keyBinding",new KeirRuntime.KeyBinding("sample.RequestUuid/1",candidate->candidate==key));
        if(fixture.equals("trace-after-confirmed")) admission.put("limits",Map.of("traceEvents",13L));
        var prepared=KeirRuntime.prepareEffect(bytes,admission,adapters); check(prepared.succeeded(),"Effect artifact admission failed");
        var invocation=new KeirRuntime.EffectInvocation(Map.of("actorId","user1"),new KeirRuntime.Deadline(now::get,1000),request->{
            boolean entry=request.equals(Map.of("entrypoint","TaskCompleted"));
            boolean call=adapters.containsKey(request.get("capability"))&&request.equals(Map.of("entrypoint","TaskCompleted","capability",request.get("capability"),"version","1"));
            boolean allowed=(entry||call)&&!fixture.equals("invocation-denied")&&(!WRITE.equals(request.get("capability"))||!fixture.equals("write-denied")&&Objects.equals(event.get("source"),input.get("taskId")));
            return CompletableFuture.completedFuture(allowed);
        },fixture.equals("no-write-grant")?Map.of(GET,1L):Map.of(GET,1L,WRITE,1L),fixture.equals("no-effect-grant")?0L:1L,
                fixture.equals("invalid-key")?"not-a-native-key":key,event);
        KeirRuntime.Outcome outcome;
        try { outcome=KeirRuntime.invokeEffect(prepared.prepared(),EffectJson.copy(input),invocation).toCompletableFuture().get(3,TimeUnit.SECONDS); }
        catch(Exception failure) { throw new IllegalArgumentException("Simulation invocation failed"); }
        if(used[0]!=stubs.size()) violations.add("Unconsumed scenario stub");
        var calls=reconcile(captures,outcome.trace(),stubs,violations); check(violations.isEmpty(),"Simulation violations");
        var actual=Map.<String,Object>of("outcome",outcome.toMap(),"calls",calls,"consumption",EffectConsumer.consume(outcome.toMap()));
        same(actual,row.get("expect"),"Simulation result mismatch");
        var result=new LinkedHashMap<String,Object>(actual); result.put("id",row.get("id")); return result;
    }
    static List<Map<String,Object>> batch(byte[] bytes,String artifactPin,String dcmPin,Object corpus) {
        var scenarios=array(corpus); check(!scenarios.isEmpty()&&scenarios.size()<=64,"Bounded scenario batch");
        var ids=new HashSet<Object>(); var results=new ArrayList<Map<String,Object>>();
        for(Object value:scenarios) { check(ids.add(object(value).get("id")),"Duplicate scenario ID"); results.add(run(bytes,artifactPin,dcmPin,value)); }
        return results;
    }
}
