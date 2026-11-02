package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import static dev.keir.runtime.RuntimeData.*;

/** Offline read/2 tooling. Authority and callbacks belong to the embedding host. */
public final class ReadSimulation {
    private ReadSimulation() {}
    public record Selection(byte[] dcmBytes, byte[] artifactBytes, String modelDigest, String artifactDigest,
                            String compiler, String entrypoint, Map<String,Object> consumer) {}
    @FunctionalInterface public interface Consumer { Object consume(Map<String,Object> outcome); }
    @FunctionalInterface public interface ResponseCheck {
        boolean check(String capability, Map<String,Object> args, Map<String,Object> response);
    }
    public record Fixture(String scope, Map<String,Object> bindings, KeirRuntime.Deadline deadline,
                          KeirRuntime.Authorizer authorize, Consumer consume, List<Map<String,Object>> allowedReceipts,
                          ResponseCheck responseCheck) {}

    private static void check(boolean valid) { if (!valid) throw new IllegalArgumentException(); }
    private static Map<String,Object> object(Object value) { return map(value, ""); }
    private static void fields(Object value, String... names) { shape(value, Set.of(names), ""); }
    private static boolean same(Object a,Object b) { return Arrays.equals(canonical(a),canonical(b)); }
    private static Object copy(Object value) { return immutable(parseJson(canonical(value),1_048_576)); }
    private static void token(Object value) { check(value instanceof String s && s.matches("[A-Za-z][A-Za-z0-9_.-]{0,63}")); }
    private static void receipt(Object value) {
        var row=object(value);
        if ("DISPLAYED".equals(row.get("status"))) {
            fields(row,"status","code"); check(row.get("code") instanceof String s && s.matches("[A-Z][A-Z0-9_]{0,63}"));
        } else { fields(row,"status"); check("NOT_DISPLAYED".equals(row.get("status"))); }
    }
    private static void value(Map<String,Object> dcm,Object type,Object data) {
        check(type instanceof String); Artifact.value(dcm,(String)type,data,"","INVALID_SCENARIO");
    }
    private static void args(Map<String,Object> dcm,Map<String,Object> cap,Object value,String binding) {
        var actual=object(value); var wanted=new HashSet<String>();
        for(var entry:object(cap.get("parameters")).entrySet()) {
            var param=object(entry.getValue());
            if(binding.equals(param.get("binding"))) { wanted.add(entry.getKey()); value(dcm,param.get("type"),actual.get(entry.getKey())); }
        }
        check(actual.keySet().equals(wanted));
    }
    private static Map<String,Object> call(Map<String,Object> dcm,Object data,boolean stub) {
        fields(data,stub?new String[]{"step","capability","version","args","response"}:new String[]{"step","capability","version","args"});
        var row=object(data); check(row.get("step") instanceof String s && s.matches("[A-Za-z][A-Za-z0-9]{0,63}"));
        var cap=object(object(dcm.get("capabilities")).get(row.get("capability")));
        check("READ".equals(cap.get("effect")) && Objects.equals(row.get("version"),cap.get("version")));
        args(dcm,cap,row.get("args"),"agent");
        if(stub) {
            var response=object(row.get("response"));
            switch((String)response.getOrDefault("kind","")) {
                case "SUCCESS" -> { fields(response,"kind","value"); value(dcm,cap.get("result"),response.get("value")); }
                case "BUSINESS_REJECTION" -> { fields(response,"kind","code"); check(list(cap.get("errors"),"").contains(response.get("code"))); }
                case "TRANSIENT_FAILURE" -> fields(response,"kind");
                default -> throw new IllegalArgumentException();
            }
        }
        return row;
    }
    private static void outcome(Map<String,Object> dcm,Map<String,Object> entry,Object value) {
        var row=object(value); var classification=row.get("classification");
        check(Set.of("COMPLETED","BUSINESS_REJECTION","FAILED","LIMIT_EXCEEDED").contains(classification));
        if("COMPLETED".equals(classification)) { fields(row,"classification","result","trace"); value(dcm,entry.get("output"),row.get("result")); }
        else { check(!row.containsKey("result") || "BUSINESS_REJECTION".equals(classification)); fields(row,row.containsKey("result")?new String[]{"classification","error","trace","result"}:new String[]{"classification","error","trace"});
            if(row.containsKey("result")) value(dcm,entry.get("output"),row.get("result")); error(row.get("error")); }
        var trace=list(row.get("trace"),""); check(trace.size()>=2 && trace.size()<=64);
        var eventFields=Map.of("INVOCATION_START",List.<String>of(),"STEP_START",List.of("step"),"VALUE_BOUND",List.of("step"),
            "CALL_DISPATCHED",List.of("step","capability"),"CALL_SUCCEEDED",List.of("step","capability"),"RETURNED",List.<String>of(),"TERMINATED",List.<String>of(),
            "STEP_SKIPPED",List.of("step"),"GUARD_EVALUATED",List.of("step","taken"));
        for(int i=0;i<trace.size();i++) {
            var event=object(trace.get(i)); var names=eventFields.get(event.get("event")); check(names!=null);
            var expected=new HashSet<>(names); expected.addAll(Set.of("sequence","event"));
            if("TERMINATED".equals(event.get("event")) && event.containsKey("code")) expected.add("code");
            check(event.keySet().equals(expected)); check(number(event.get("sequence"),0,63,"")==i);
            for(String name:names) {
                if(name.equals("taken")) check(event.get(name) instanceof Boolean);
                else if(name.equals("step")) check(event.get(name) instanceof String s && s.matches("[A-Za-z][A-Za-z0-9]{0,63}"));
                else check(object(dcm.get("capabilities")).containsKey(event.get(name)));
            }
            if(event.containsKey("code")) check(event.get("code") instanceof String s && s.matches("[A-Z][A-Z0-9_]{0,63}"));
            if("INVOCATION_START".equals(event.get("event"))) check(i==0);
            if("TERMINATED".equals(event.get("event"))) check(i==trace.size()-1);
        }
        check("INVOCATION_START".equals(object(trace.getFirst()).get("event")) && "TERMINATED".equals(object(trace.getLast()).get("event")));
        check("COMPLETED".equals(classification) ? !object(trace.getLast()).containsKey("code")
            : Objects.equals(object(trace.getLast()).get("code"),object(row.get("error")).get("code")));
    }
    private static void error(Object value) {
        var row=object(value); fields(row,row.containsKey("cause")?new String[]{"code","path","cause"}:new String[]{"code","path"});
        check(row.get("code") instanceof String s && s.matches("[A-Z][A-Z0-9_]{0,63}")); text(row.get("path"),"",false,1024);
        if(row.containsKey("cause")) { fields(row.get("cause"),"code","path"); error(row.get("cause")); }
    }
    private static Map<String,Object> scenario(byte[] bytes,Map<String,Object> dcm,Map<String,Object> entry,List<Object> receipts) {
        var row=object(parseJson(bytes,65_536)); canonical(row); fields(row,"format","id","input","stubs","expect");
        check("keir-read-simulation/1".equals(row.get("format"))); token(row.get("id")); value(dcm,entry.get("input"),row.get("input"));
        var stubs=list(row.get("stubs"),""); check(stubs.size()<=4); stubs.forEach(stub->call(dcm,stub,true));
        var expected=object(row.get("expect")); fields(expected,"calls","outcome","consumption");
        var calls=list(expected.get("calls"),""); check(calls.size()<=4); calls.forEach(item->call(dcm,item,false));
        outcome(dcm,entry,expected.get("outcome")); receipt(expected.get("consumption")); check(receipts.contains(expected.get("consumption")));
        return object(copy(row));
    }
    private static final class Ledger {
        final List<Map<String,Object>> mismatches=new ArrayList<>();
        void add(String stage,String code,int ordinal) { if(mismatches.size()<8) mismatches.add(Map.of("stage",stage,"code",code,"ordinal",ordinal)); }
    }
    private static byte[] report(String status,Map<String,Object> identity,Ledger ledger,Object consumption) {
        var row=new LinkedHashMap<String,Object>(); row.put("format","keir-read-simulation-report/1"); row.put("status",status);
        row.put("identity",identity); row.put("mismatches",ledger.mismatches); if(consumption!=null) row.put("consumption",consumption);
        byte[] bytes=canonical(row); check(bytes.length<=16_384); return bytes;
    }
    /** Each call owns one fresh binding and run-local mismatch ledger; no live adapter is accepted. */
    public static byte[] run(Selection selection,Fixture fixture,byte[] caseBytes) {
        var ledger=new Ledger(); Map<String,Object> identity=Map.of(); Map<String,Object> dcm,entry,bindings,consumer; List<Object> receipts;
        byte[] artifact; Map<String,Object> row; String scope;
        try {
            check(selection!=null && fixture!=null && selection.dcmBytes()!=null && selection.dcmBytes().length<=65_536);
            check(selection.artifactBytes()!=null && selection.artifactBytes().length<=1_048_576); artifact=selection.artifactBytes().clone();
            check(selection.modelDigest()!=null && selection.modelDigest().matches("sha256:[0-9a-f]{64}"));
            check(selection.artifactDigest()!=null && selection.artifactDigest().matches("sha256:[0-9a-f]{64}"));
            dcm=object(copy(parseJson(selection.dcmBytes().clone(),65_536))); Artifact.validateModel2(dcm,false);
            check("keir-dcm/read-2".equals(dcm.get("format")) && digest(canonical(dcm)).equals(selection.modelDigest()) && digest(artifact).equals(selection.artifactDigest()));
            text(selection.compiler(),"",true,256); text(selection.entrypoint(),"",true,100);
            entry=object(object(dcm.get("entrypoints")).get(selection.entrypoint())); consumer=object(copy(selection.consumer())); check(same(consumer,entry.get("consumer")));
            text(fixture.scope(),"",true,256); scope=fixture.scope(); bindings=object(copy(fixture.bindings()));
            check(fixture.authorize()!=null && fixture.consume()!=null && fixture.deadline()!=null && fixture.deadline().clock()!=null);
            check(fixture.deadline().expiresAtMs()>=0 && fixture.deadline().expiresAtMs()<=9_007_199_254_740_991L);
            receipts=list(copy(fixture.allowedReceipts()),""); check(!receipts.isEmpty() && receipts.size()<=16);
            receipts.forEach(ReadSimulation::receipt); check(new HashSet<>(receipts).size()==receipts.size());
        } catch(RuntimeException failure) { ledger.add("SETUP","SETUP_ERROR",0); return report("SETUP_ERROR",Map.of(),ledger,null); }
        identity=Map.of("modelDigest",selection.modelDigest(),"artifactDigest",selection.artifactDigest());
        var stubs=new ArrayList<Object>(); var captures=new ArrayList<Map<String,Object>>(); int[] used={0}; boolean[] active={true};
        try {
            var invokers=new LinkedHashMap<String,Object>();
            for(var capability:object(dcm.get("capabilities")).entrySet()) {
                String id=capability.getKey(); var cap=object(capability.getValue());
                invokers.put(id,new KeirRuntime.Adapter((String)cap.get("version"),"simulation",(args,protectedArgs,left)->{
                    synchronized(captures) {
                        check(active[0]); int ordinal=captures.size()+1;
                        if(!"READ".equals(cap.get("effect")) || ordinal>4) { ledger.add("STUB","STUB_MISMATCH",Math.min(ordinal,4)); throw new IllegalArgumentException(); }
                        captures.add(object(copy(Map.of("capability",id,"version",cap.get("version"),"args",args))));
                        if(used[0]>=stubs.size()) { ledger.add("STUB","MISSING_STUB",ordinal); throw new IllegalArgumentException(); }
                        var stub=object(stubs.get(used[0])); var wanted=new LinkedHashMap<String,Object>();
                        for(var parameter:object(cap.get("parameters")).entrySet()) if("host".equals(object(parameter.getValue()).get("binding"))) wanted.put(parameter.getKey(),bindings.get(parameter.getKey()));
                        if(!Objects.equals(stub.get("capability"),id) || !Objects.equals(stub.get("version"),cap.get("version")) || !same(args,stub.get("args")) || !same(protectedArgs,wanted) || left<=0) {
                            ledger.add("STUB","STUB_MISMATCH",ordinal); throw new IllegalArgumentException();
                        }
                        var response=object(stub.get("response"));
                        if(fixture.responseCheck()!=null) {
                            boolean accepted; try { accepted=fixture.responseCheck().check(id,object(copy(args)),object(copy(response))); } catch(RuntimeException error) { accepted=false; }
                            if(!accepted) { ledger.add("STUB","STUB_MISMATCH",ordinal); throw new IllegalArgumentException(); }
                        }
                        used[0]++; return CompletableFuture.completedFuture(copy(response));
                    }
                }));
            }
            var approval=Map.<String,Object>of("scope",scope,"modelRef",Artifact.modelRef(dcm),"profile","keir-read/2","environment",KeirRuntime.ENVIRONMENT,"mode","simulation");
            var bound=KeirRuntime.bindModel(canonical(dcm),approval,invokers); check(bound.succeeded());
            var admission=Map.<String,Object>of("expectedArtifactDigest",selection.artifactDigest(),"expectedDcmDigest",selection.modelDigest(),"entrypoint",selection.entrypoint(),"consumer",consumer,"compiler",selection.compiler(),"environment",KeirRuntime.ENVIRONMENT,"mode","simulation");
            var prepared=KeirRuntime.prepareProgram(artifact,bound.boundModel(),admission); check(prepared.succeeded());
            var hostTypes=new LinkedHashMap<String,List<String>>();
            for(Object item:list(object(parseCanonical(artifact)).get("steps"),"")) {
                var step=object(item); if(!"CALL".equals(step.get("op"))) continue;
                var cap=object(object(dcm.get("capabilities")).get(step.get("capability")));
                for(var parameter:object(cap.get("parameters")).entrySet()) {
                    var param=object(parameter.getValue()); if("host".equals(param.get("binding"))) hostTypes.computeIfAbsent(parameter.getKey(),ignored->new ArrayList<>()).add((String)param.get("type"));
                }
            }
            check(bindings.keySet().equals(hostTypes.keySet()));
            for(var host:hostTypes.entrySet()) for(String type:host.getValue()) value(dcm,type,bindings.get(host.getKey()));
            try { row=scenario(caseBytes,dcm,entry,receipts); stubs.addAll(list(row.get("stubs"),"")); }
            catch(RuntimeException failure) { ledger.add("SCENARIO","INVALID_SCENARIO",0); return report("INVALID_SCENARIO",identity,ledger,null); }
            var nativeOutcome=KeirRuntime.invokeProgram(prepared.prepared(),row.get("input"),new KeirRuntime.Invocation2(scope,bindings,fixture.deadline(),fixture.authorize(),Optional.empty())).toCompletableFuture().get(3,TimeUnit.SECONDS);
            var calls=new ArrayList<Map<String,Object>>();
            synchronized(captures) {
                active[0]=false;
                if(used[0]!=stubs.size()) ledger.add("STUB","EXTRA_STUB",used[0]+1);
                var dispatches=nativeOutcome.trace().stream().filter(event->"CALL_DISPATCHED".equals(event.get("event"))).toList();
                if(dispatches.size()!=captures.size()) ledger.add("CALLS","TRACE_MISMATCH",0);
                for(int i=0;i<captures.size();i++) {
                    var event=i<dispatches.size()?dispatches.get(i):Map.<String,Object>of(); var capture=captures.get(i);
                    if(i>=stubs.size() || !Objects.equals(event.get("step"),object(stubs.get(i)).get("step")) || !Objects.equals(event.get("capability"),capture.get("capability"))) ledger.add("CALLS","TRACE_MISMATCH",i+1);
                    if(event.get("step")!=null) { var actual=new LinkedHashMap<>(capture); actual.put("step",event.get("step")); calls.add(actual); }
                }
            }
            var expected=object(row.get("expect"));
            if(!same(calls,expected.get("calls"))) ledger.add("CALLS","CALLS_MISMATCH",0);
            if(!same(nativeOutcome.toMap(),expected.get("outcome"))) ledger.add("OUTCOME","OUTCOME_MISMATCH",0);
            Object consumption;
            try { consumption=copy(fixture.consume().consume(nativeOutcome.toMap())); receipt(consumption); check(receipts.contains(consumption)); }
            catch(RuntimeException failure) { ledger.add("CONSUMER","CONSUMER_ERROR",0); return report("CONSUMER_ERROR",identity,ledger,null); }
            if(!same(consumption,expected.get("consumption"))) ledger.add("CONSUMER","CONSUMPTION_MISMATCH",0);
            return report(ledger.mismatches.isEmpty()?"PASS":"MISMATCH",identity,ledger,consumption);
        } catch(Exception failure) {
            if(ledger.mismatches.isEmpty()) { ledger.add("SETUP","SETUP_ERROR",0); return report("SETUP_ERROR",Map.of(),ledger,null); }
            return report("MISMATCH",identity,ledger,null);
        } finally { synchronized(captures) { active[0]=false; } }
    }
}
