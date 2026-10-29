package dev.keir.runtime;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static dev.keir.runtime.RuntimeData.*;

/** Test-only batch transport for the shared, externally pinned artifact scenarios. */
public final class ScenarioDriver {
    private static final long MAX_INTEGER=9_007_199_254_740_991L;
    private ScenarioDriver() {}

    static byte[] resource(String name) throws IOException {
        try(var input=ScenarioDriver.class.getResourceAsStream(name)) {
            if(input==null) throw new IOException("Missing conformance resource");
            byte[] bytes=input.readNBytes(Profile.ARTIFACT_BYTES+1);
            check(bytes.length<=Profile.ARTIFACT_BYTES,"Conformance resource exceeds bound");
            return bytes;
        }
    }

    private static Map<String,Object> admission() {
        Map<String,Object> admission=new LinkedHashMap<>();
        admission.put("expectedArtifactDigest","sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424");
        admission.put("expectedDcmDigest","sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f");
        admission.put("entrypoint","ClassifyTask"); admission.put("consumer",Map.of("id","sample.classification","version","1","kind","DISPLAY"));
        admission.put("compiler","keir-compiler-java/0.1.0-SNAPSHOT");
        admission.put("environment",Map.of("celJava","0.14.0","celNode","0.6.1","protobufJava","4.35.1","protobufNode","2.6.2"));
        admission.put("mode","simulation"); admission.put("grants",Map.of("tasks.get",1L));
        return admission;
    }

    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static Map<String,Object> object(Object value) {
        check(value instanceof Map<?,?>,"Expected a fixture record");
        return map(value,"/fixture");
    }
    private static List<Object> array(Object value) {
        check(value instanceof List<?>,"Expected a fixture array"); return list(value,"/fixture");
    }
    private static void fields(Object value,Set<String> required,Set<String> optional) {
        Set<String> actual=object(value).keySet();
        check(actual.containsAll(required),"Missing fixture field");
        Set<String> allowed=new HashSet<>(required); allowed.addAll(optional);
        check(allowed.containsAll(actual),"Unknown fixture field");
    }
    private static long time(Object value) {
        long result;
        try {
            if(value instanceof Integer || value instanceof Long) result=((Number)value).longValue();
            else if(value instanceof BigDecimal decimal) result=decimal.longValueExact();
            else throw new ArithmeticException();
        } catch(ArithmeticException error) { throw new AssertionError("Fixture time must be a safe integer"); }
        check(result>=-MAX_INTEGER && result<=MAX_INTEGER,"Fixture time must be a safe integer");
        return result;
    }
    private static void same(Object actual,Object expected,String message) {
        check(Arrays.equals(canonical(actual),canonical(expected)),message);
    }

    static void validateCase(Map<String,Object> row) {
        fields(row,Set.of("id","runs","expected"),Set.of("prepare"));
        check(row.get("id") instanceof String id && id.matches("[a-z0-9][a-z0-9_-]*"),"Invalid scenario id");
        if(row.containsKey("prepare"))fields(row.get("prepare"),Set.of(),Set.of("grants","limits"));
        List<Object> runs=array(row.get("runs"));
        for(Object item:runs) {
            fields(item,Set.of("input","bindings","grants","clock","authorizations","calls"),Set.of());
            Map<String,Object> run=object(item),clock=object(run.get("clock")); fields(clock,Set.of("nowMs","expiresAtMs"),Set.of());
            time(clock.get("nowMs")); time(clock.get("expiresAtMs"));
            for(String kind:List.of("authorizations","calls"))for(Object value:array(run.get(kind))) {
                Map<String,Object> stub=object(value);
                if(kind.equals("authorizations")) {
                    fields(stub,Set.of("request","action"),Set.of("atMs"));
                    fields(stub.get("request"),Set.of("entrypoint"),Set.of("capability","version"));
                    Map<String,Object> request=object(stub.get("request"));
                    check(request.containsKey("capability")==request.containsKey("version"),"Incomplete authorization request");
                    check(Set.of("ALLOW","DENY","THROW","NON_BOOLEAN").contains(stub.get("action")),"Unknown authorization action");
                } else {
                    fields(stub,Set.of("request"),Set.of("response","throw","atMs"));
                    fields(stub.get("request"),Set.of("capability","version","agentArgs","protectedArgs","remainingMs"),Set.of());
                    Map<String,Object> request=object(stub.get("request")); object(request.get("agentArgs")); object(request.get("protectedArgs")); time(request.get("remainingMs"));
                    check(stub.containsKey("response")!=stub.containsKey("throw"),"Adapter needs exactly one response or throw");
                    if(stub.containsKey("throw"))check(Boolean.TRUE.equals(stub.get("throw")),"Invalid adapter throw stub");
                }
                if(stub.containsKey("atMs"))time(stub.get("atMs"));
            }
        }
        fields(row.get("expected"),Set.of("prepare","runs"),Set.of()); Map<String,Object> expected=object(row.get("expected"));
        Map<String,Object> selected=object(expected.get("prepare"));
        if("PREPARED".equals(selected.get("status"))) fields(selected,Set.of("status"),Set.of());
        else {
            fields(selected,Set.of("status","code","path"),Set.of()); check("REJECTED".equals(selected.get("status")),"Invalid preparation status");
            check(runs.isEmpty(),"Rejected preparation cannot request runs");
        }
        List<Object> expectedRuns=array(expected.get("runs")); check(expectedRuns.size()==runs.size(),"Expected and requested runs must correspond");
        for(Object value:expectedRuns) {
            fields(value,Set.of("outcome","calls","authorizations","consumption"),Set.of()); Map<String,Object> run=object(value);
            object(run.get("outcome")); array(run.get("calls")); array(run.get("authorizations")); object(run.get("consumption"));
        }
    }

    static Map<String,Object> loadCorpus() throws IOException {
        Map<String,Object> corpus=object(parseJson(resource("/read-and-decide/scenarios.json"),Profile.ARTIFACT_BYTES));
        fields(corpus,Set.of("format","cases"),Set.of()); check("keir-read-scenarios/1".equals(corpus.get("format")),"Unknown scenario format");
        List<Object> rows=array(corpus.get("cases")); check(!rows.isEmpty(),"Empty scenario corpus"); Set<Object> ids=new HashSet<>();
        for(Object value:rows){Map<String,Object> row=object(value);validateCase(row);check(ids.add(row.get("id")),"Duplicate scenario id");}
        return corpus;
    }

    // DISPLAY validates the selected public output independently of runtime schema validation.
    static Map<String,Object> consume(Map<String,Object> outcome) {
        if(!"COMPLETED".equals(outcome.get("classification"))) {
            Object error=outcome.get("error"); Object code=error instanceof Map<?,?> map?map.get("code"):null;
            return Map.of("status","NOT_DISPLAYED","code",code instanceof String?code:"OUTPUT_INVALID");
        }
        Object result=outcome.get("result");
        if(!(result instanceof Map<?,?> value) || !value.keySet().equals(Set.of("classification")) ||
                !("DONE".equals(value.get("classification")) || "PENDING".equals(value.get("classification"))))
            return Map.of("status","NOT_DISPLAYED","code","OUTPUT_INVALID");
        return Map.of("status","DISPLAYED","result",Map.of("classification",((Map<?,?>)result).get("classification")));
    }

    private static final class Active {
        final Map<String,Object> run;
        final AtomicLong now;
        final Map<String,Integer> used=new HashMap<>(Map.of("calls",0,"authorizations",0));
        final Map<String,List<Object>> captured=new LinkedHashMap<>(Map.of("calls",new ArrayList<>(),"authorizations",new ArrayList<>()));
        Active(Map<String,Object> run) { this.run=run; now=new AtomicLong(time(object(run.get("clock")).get("nowMs"))); }
    }

    private static final class Harness {
        final List<String> violations=new ArrayList<>();
        Active active;
        Map<String,Object> next(String kind,Map<String,Object> request) {
            try {
                check(active!=null,"Callback outside an invocation");
                Object captured=immutable(request); active.captured.get(kind).add(captured);
                int index=active.used.get(kind); active.used.put(kind,index+1);
                List<Object> stubs=array(active.run.get(kind)); check(index<stubs.size(),"Unexpected "+kind+" callback");
                Map<String,Object> stub=object(stubs.get(index)); same(captured,stub.get("request"),"Out-of-order or mismatched "+kind+" callback");
                if(stub.containsKey("atMs"))active.now.set(time(stub.get("atMs")));
                return stub;
            } catch(AssertionError | RuntimeException error) {
                // A safe runtime failure must not hide a broken fixture or an unexpected callback.
                violations.add(error.getMessage()); throw new IllegalStateException("Conformance callback mismatch");
            }
        }
        void finish() {
            for(String kind:List.of("calls","authorizations"))
                if(active.used.get(kind)!=array(active.run.get(kind)).size())violations.add("Unused "+kind+" stub");
            active=null;
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    static Map<String,Object> runCase(Map<String,Object> row) throws Exception {
        validateCase(row); Harness harness=new Harness(); Map<String,Object> admission=admission();
        if(row.containsKey("prepare"))admission.putAll(object(row.get("prepare")));
        Map<String,KeirRuntime.Adapter> adapters=Map.of("tasks.get",new KeirRuntime.Adapter("1","simulation",(agent,protectedArgs,remaining)->{
            Map<String,Object> stub=harness.next("calls",Map.of("capability","tasks.get","version","1","agentArgs",agent,"protectedArgs",protectedArgs,"remainingMs",remaining));
            if(Boolean.TRUE.equals(stub.get("throw")))throw new IllegalStateException("Private adapter fixture failure");
            return CompletableFuture.completedFuture(immutable(stub.get("response")));
        }));
        KeirRuntime.Preparation selected=KeirRuntime.prepare(resource("/read-and-decide/artifact.json"),admission,adapters);
        Map<String,Object> output=new LinkedHashMap<>(); output.put("id",row.get("id")); List<Object> results=new ArrayList<>(); output.put("runs",results);
        if(!selected.succeeded()) {
            KeirRuntime.Diagnostic diagnostic=selected.diagnostic(); check(diagnostic!=null && diagnostic.message()!=null,"Invalid runtime diagnostic");
            output.put("prepare",Map.of("status","REJECTED","code",diagnostic.code(),"path",diagnostic.path()));
        } else {
            output.put("prepare",Map.of("status","PREPARED"));
            for(Object value:array(row.get("runs"))) {
                Map<String,Object> run=object(value); Active active=new Active(run); harness.active=active;
                KeirRuntime.Invocation invocation=new KeirRuntime.Invocation((Map)immutable(run.get("bindings")),
                        new KeirRuntime.Deadline(active.now::get,time(object(run.get("clock")).get("expiresAtMs"))),request->{
                            Map<String,Object> stub=harness.next("authorizations",request);
                            if("THROW".equals(stub.get("action")))throw new IllegalStateException("Private authorization fixture failure");
                            return CompletableFuture.completedFuture("NON_BOOLEAN".equals(stub.get("action"))?"invalid":"ALLOW".equals(stub.get("action")));
                        },(Map)immutable(run.get("grants")));
                KeirRuntime.Outcome result=KeirRuntime.invoke(selected.prepared(),immutable(run.get("input")),invocation).toCompletableFuture().get(5,TimeUnit.SECONDS);
                harness.finish(); Map<String,Object> outcome=result.toMap();
                results.add(Map.of("outcome",outcome,"calls",active.captured.get("calls"),"authorizations",active.captured.get("authorizations"),"consumption",consume(outcome)));
            }
        }
        check(harness.violations.isEmpty(),row.get("id")+": harness violations: "+harness.violations);
        Map<String,Object> expected=new LinkedHashMap<>(object(row.get("expected"))); expected.put("id",row.get("id"));
        same(output,expected,row.get("id")+": conformance mismatch");
        return object(immutable(output));
    }

    static List<Object> runCorpus(Map<String,Object> corpus) throws Exception {
        List<Object> results=new ArrayList<>(); for(Object row:array(corpus.get("cases")))results.add(runCase(object(row))); return Collections.unmodifiableList(results);
    }

    public static void main(String[] args) throws Exception {
        check(args.length==0,"Scenario driver accepts no arguments");
        System.out.write(canonical(runCorpus(loadCorpus())));
    }
}
