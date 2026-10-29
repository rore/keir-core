package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import static dev.keir.runtime.RuntimeData.*;

/** Execution of admitted keir-read/1 artifacts, with authority supplied anew for each invocation. */
public final class KeirRuntime {
    public static final Map<String,String> ENVIRONMENT = Profile.ENVIRONMENT;
    private KeirRuntime() {}

    @FunctionalInterface public interface AdapterFunction {
        CompletionStage<?> invoke(Map<String,Object> agentArgs, Map<String,Object> protectedArgs, long remainingMs);
    }
    @FunctionalInterface public interface Authorizer { CompletionStage<?> authorize(Map<String,Object> request); }
    public record Adapter(String version, String mode, AdapterFunction invoke) {}
    public record Deadline(LongSupplier clock, long expiresAtMs) {}
    public record Invocation(Map<String,Object> bindings, Deadline deadline, Authorizer authorize, Map<String,Long> grants) {}
    public record Diagnostic(String code, String path, String message) {}
    public record Preparation(Prepared prepared, Diagnostic diagnostic) {
        public boolean succeeded() { return prepared != null; }
    }

    public static final class Prepared {
        private final Artifact.Plan plan;
        private final CheckedCel.Bundle cel;
        private final Map<String,Adapter> adapters;
        private final Map<String,Long> grants;
        private final Map<String,Long> limits;
        private final Map<String,List<String>> hostFields;
        private Prepared(Artifact.Plan plan, CheckedCel.Bundle cel, Map<String,Adapter> adapters,
                         Map<String,Long> grants, Map<String,Long> limits, Map<String,List<String>> hostFields) {
            this.plan=plan; this.cel=cel; this.adapters=Map.copyOf(adapters);
            this.grants=Map.copyOf(grants); this.limits=Map.copyOf(limits);
            Map<String,List<String>> fields = new LinkedHashMap<>();
            hostFields.forEach((key,value)->fields.put(key,List.copyOf(value)));
            this.hostFields=Collections.unmodifiableMap(fields);
        }
    }

    public static final class Outcome {
        private final Map<String,Object> document;
        private Outcome(Map<String,Object> document) { this.document=snapshot(document); }
        public Map<String,Object> toMap() { return document; }
        public byte[] toJson() { return canonical(document); }
        public String classification() { return (String)document.get("classification"); }
        public Map<String,Object> result() { return document.containsKey("result") ? map(document.get("result"),"") : null; }
        public Map<String,Object> error() { return document.containsKey("error") ? map(document.get("error"),"") : null; }
        @SuppressWarnings("unchecked") public List<Map<String,Object>> trace() { return (List<Map<String,Object>>)document.get("trace"); }
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> snapshot(Map<String,Object> value) {
        return (Map<String,Object>)immutable(value);
    }
    private static void require(boolean valid, String code, String path) {
        if (!valid) throw new RuntimeFault(code,path);
    }
    private static Map<String,Object> bounded(Object value, String path, int max) {
        Map<String,Object> result=map(value,path);
        require(result.size()<=max,"ARTIFACT_INVALID",path); return result;
    }
    private static boolean same(Object left,Object right) { return Arrays.equals(canonical(left),canonical(right)); }

    public static Preparation prepare(byte[] artifactBytes, Map<String,Object> admission, Map<String,Adapter> adapters) {
        try {
            Set<String> fields=new HashSet<>(Set.of("expectedArtifactDigest","expectedDcmDigest","entrypoint","consumer","compiler","environment","mode","grants"));
            if (admission!=null && admission.containsKey("limits")) fields.add("limits");
            shape(admission,fields,"/admission");
            for (String field:List.of("expectedArtifactDigest","expectedDcmDigest"))
                require(admission.get(field) instanceof String text && text.matches("sha256:[0-9a-f]{64}"),"ADMISSION_INVALID","/admission/"+field);
            require(artifactBytes!=null && artifactBytes.length<=Profile.ARTIFACT_BYTES,"BUDGET_EXCEEDED","/artifact");
            byte[] bytes=artifactBytes.clone();
            require(digest(bytes).equals(admission.get("expectedArtifactDigest")),"PIN_MISMATCH","/artifact");
            Artifact.Plan plan=Artifact.validate(map(parseCanonical(bytes),"/artifact"));
            Map<String,Object> artifact=plan.document();
            require(Objects.equals(artifact.get("dcmDigest"),admission.get("expectedDcmDigest"))
                    && Objects.equals(artifact.get("entrypoint"),admission.get("entrypoint"))
                    && Objects.equals(artifact.get("compiler"),admission.get("compiler"))
                    && same(artifact.get("environment"),admission.get("environment"))
                    && same(plan.entrypoint().get("consumer"),admission.get("consumer")),"PIN_MISMATCH","/admission");
            require("simulation".equals(admission.get("mode")) || "real".equals(admission.get("mode")),"ADMISSION_INVALID","/admission/mode");
            Map<String,Long> grants=new LinkedHashMap<>();
            Map<String,Object> entryGrants=map(plan.entrypoint().get("grants"),"/admission/grants");
            for (var grant:bounded(admission.get("grants"),"/admission/grants",8).entrySet()) {
                require(entryGrants.containsKey(grant.getKey()),"ADMISSION_INVALID","/admission/grants");
                grants.put(grant.getKey(),number(grant.getValue(),1,((Number)entryGrants.get(grant.getKey())).longValue(),"/admission/grants"));
            }
            Map<String,Long> limits=new LinkedHashMap<>(Map.of("steps",(long)Profile.STEPS,"calls",(long)Profile.CALLS,
                    "totalAstNodes",(long)Profile.TOTAL_AST_NODES,"traceEvents",(long)Profile.TRACE_EVENTS));
            if (admission.containsKey("limits")) for (var limit:bounded(admission.get("limits"),"/admission/limits",4).entrySet()) {
                require(limits.containsKey(limit.getKey()),"ADMISSION_INVALID","/admission/limits");
                limits.put(limit.getKey(),number(limit.getValue(),"traceEvents".equals(limit.getKey())?2:0,limits.get(limit.getKey()),"/admission/limits"));
            }
            require(list(artifact.get("steps"),"").size()<=limits.get("steps") && plan.callCount()<=limits.get("calls"),"BUDGET_EXCEEDED","/artifact/steps");
            for (var call:plan.calls().entrySet()) require(call.getValue()<=grants.getOrDefault(call.getKey(),0L),"AUTHORITY_DENIED","/admission/grants");
            CheckedCel.Bundle cel=CheckedCel.prepare(plan);
            require(cel.totalNodes()<=limits.get("totalAstNodes"),"BUDGET_EXCEEDED","/artifact/expressions");
            require(adapters!=null && adapters.size()<=8,"ADMISSION_INVALID","/admission/adapters");
            Map<String,Object> capabilities=map(plan.dcm().get("capabilities"),"");
            Map<String,Adapter> selected=new LinkedHashMap<>();
            for (var item:adapters.entrySet()) {
                Adapter adapter=item.getValue();
                require(capabilities.containsKey(item.getKey()) && adapter!=null,"ADMISSION_INVALID","/admission/adapters");
                require(Objects.equals(adapter.version(),map(capabilities.get(item.getKey()),"").get("version"))
                        && Objects.equals(adapter.mode(),admission.get("mode")) && adapter.invoke()!=null,"ADMISSION_INVALID","/admission/adapters");
                selected.put(item.getKey(),adapter);
            }
            Map<String,List<String>> hostFields=new LinkedHashMap<>();
            for (String id:plan.calls().keySet()) {
                require(selected.containsKey(id),"ADMISSION_INVALID","/admission/adapters");
                for (var parameter:map(map(capabilities.get(id),"").get("parameters"),"").entrySet()) {
                    Map<String,Object> definition=map(parameter.getValue(),"");
                    if ("host".equals(definition.get("binding"))) hostFields.computeIfAbsent(parameter.getKey(),ignored->new ArrayList<>()).add((String)definition.get("type"));
                }
            }
            return new Preparation(new Prepared(plan,cel,selected,grants,limits,hostFields),null);
        } catch (RuntimeException failure) {
            String code=failure instanceof RuntimeFault fault?fault.code:"ARTIFACT_INVALID";
            String path=failure instanceof RuntimeFault fault?fault.path:"/artifact";
            if (code.equals("ARTIFACT_INVALID") && path.startsWith("/admission")) code="ADMISSION_INVALID";
            return new Preparation(null,new Diagnostic(code,path,"The selected artifact or host configuration is invalid."));
        }
    }

    public static CompletionStage<Outcome> invoke(Prepared prepared, Object businessInput, Invocation invocation) {
        Run run=new Run(prepared);
        try { return run.start(businessInput,invocation).handle((outcome,failure)->failure==null?outcome:run.failed(failure)); }
        catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    private static final class Run {
        private final Prepared prepared;
        private final List<Map<String,Object>> trace=new ArrayList<>();
        private final long traceLimit;
        private final Map<String,Object> state=new LinkedHashMap<>(),bindings=new LinkedHashMap<>();
        private final Map<String,Long> grants=new LinkedHashMap<>();
        private final Map<String,Integer> counts=new HashMap<>();
        private InvocationClock clock;
        private Authorizer authorizer;
        private int totalCalls;
        private Run(Prepared prepared) { this.prepared=prepared; traceLimit=prepared==null?Profile.TRACE_EVENTS:prepared.limits.get("traceEvents"); emit("INVOCATION_START",Map.of(),""); }
        private void emit(String event,Map<String,Object> attributes,String path) {
            require(trace.size()<traceLimit-1,"BUDGET_EXCEEDED",path);
            Map<String,Object> item=new LinkedHashMap<>(); item.put("sequence",trace.size()); item.put("event",event); item.putAll(attributes); trace.add(item);
        }
        private Outcome terminate(String classification,Map<String,Object> error,Object result) {
            Map<String,Object> end=new LinkedHashMap<>(Map.of("sequence",trace.size(),"event","TERMINATED"));
            if (error!=null) end.put("code",error.get("code")); trace.add(end);
            Map<String,Object> outcome=new LinkedHashMap<>(); outcome.put("classification",classification);
            if (error!=null) outcome.put("error",error); else outcome.put("result",result);
            outcome.put("trace",trace); return new Outcome(outcome);
        }
        private Outcome failed(Throwable failure) {
            while (failure instanceof CompletionException || failure instanceof ExecutionException) failure=failure.getCause();
            RuntimeFault fault=failure instanceof RuntimeFault f?f:new RuntimeFault("HOST_CONTEXT_INVALID","");
            return terminate(fault.code.equals("BUDGET_EXCEEDED")?"LIMIT_EXCEEDED":"FAILED",Map.of("code",fault.code,"path",fault.path),null);
        }
        private CompletionStage<Outcome> start(Object input,Invocation invocation) {
            require(prepared!=null && invocation!=null && invocation.authorize()!=null && invocation.deadline()!=null,"HOST_CONTEXT_INVALID","");
            clock=new InvocationClock(invocation.deadline().clock(),invocation.deadline().expiresAtMs()); clock.remaining("");
            state.put("input",Artifact.value(prepared.plan.dcm(),(String)prepared.plan.entrypoint().get("input"),input,"/input","INPUT_INVALID"));
            try {
                shape(invocation.bindings(),prepared.hostFields.keySet(),"/hostBindings");
                for (var field:prepared.hostFields.entrySet()) for (String type:field.getValue())
                    bindings.put(field.getKey(),Artifact.value(prepared.plan.dcm(),type,invocation.bindings().get(field.getKey()),"/hostBindings","HOST_BINDING_INVALID"));
            } catch (RuntimeException failure) { throw new RuntimeFault(failure instanceof RuntimeFault f && f.code.equals("BUDGET_EXCEEDED")?f.code:"HOST_BINDING_INVALID","/hostBindings"); }
            try {
                for (var grant:bounded(invocation.grants(),"",8).entrySet()) {
                    require(prepared.grants.containsKey(grant.getKey()),"HOST_CONTEXT_INVALID","");
                    grants.put(grant.getKey(),number(grant.getValue(),1,prepared.grants.get(grant.getKey()),""));
                }
            } catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
            authorizer=invocation.authorize();
            return authorized(Map.of("entrypoint",prepared.plan.document().get("entrypoint")),"").thenCompose(ignored->step(0));
        }
        private CompletionStage<Object> authorized(Map<String,Object> request,String path) {
            clock.remaining(path);
            return clock.waitFor(budget->authorizer.authorize(snapshot(request)),path,"AUTHORITY_CHECK_FAILED",null).thenApply(allowed->{
                require(allowed instanceof Boolean,"AUTHORITY_CHECK_FAILED",path);
                require(Boolean.TRUE.equals(allowed),"AUTHORITY_DENIED",path); return allowed;
            });
        }
        private Object evaluate(Object binding,String path) {
            clock.remaining(path);
            // shortcut: synchronous CEL is checked before and after; interruption needs separate qualification.
            Object value=prepared.cel.evaluate(map(binding,path),state,path);
            clock.remaining(path); return value;
        }
        private CompletionStage<Outcome> step(int index) {
            List<Object> steps=list(prepared.plan.document().get("steps"),"");
            if (index==steps.size()) {
                Map<String,Object> result=new LinkedHashMap<>();
                for (var field:map(map(prepared.plan.document().get("return"),"").get("fields"),"").entrySet()) result.put(field.getKey(),evaluate(field.getValue(),"/return"));
                Object value=Artifact.value(prepared.plan.dcm(),(String)prepared.plan.entrypoint().get("output"),result,"/return","EXPRESSION_FAILED");
                clock.remaining("/return"); emit("RETURNED",Map.of(),"/return");
                return CompletableFuture.completedFuture(terminate("COMPLETED",null,value));
            }
            String path="/steps/"+index;
            clock.remaining(path); require(index<prepared.limits.get("steps"),"BUDGET_EXCEEDED",path);
            Map<String,Object> step=map(steps.get(index),path); String id=(String)step.get("id");
            emit("STEP_START",Map.of("step",id),path);
            if ("BIND".equals(step.get("op"))) {
                state.put(id,evaluate(step.get("value"),path)); emit("VALUE_BOUND",Map.of("step",id),path); return step(index+1);
            }
            String capability=(String)step.get("capability");
            Map<String,Object> cap=map(map(prepared.plan.dcm().get("capabilities"),"").get(capability),"");
            int count=counts.merge(capability,1,Integer::sum); totalCalls++;
            require(totalCalls<=prepared.limits.get("calls"),"BUDGET_EXCEEDED",path);
            require(count<=grants.getOrDefault(capability,0L),"AUTHORITY_DENIED",path);
            Map<String,Object> agent=new LinkedHashMap<>(),protectedArgs=new LinkedHashMap<>();
            for (var parameter:map(cap.get("parameters"),"").entrySet()) {
                String name=parameter.getKey(); Map<String,Object> definition=map(parameter.getValue(),""); String type=(String)definition.get("type");
                if ("host".equals(definition.get("binding"))) protectedArgs.put(name,Artifact.value(prepared.plan.dcm(),type,bindings.get(name),"/hostBindings","HOST_BINDING_INVALID"));
                else agent.put(name,Artifact.value(prepared.plan.dcm(),type,evaluate(map(step.get("args"),"").get(name),path),path,"INPUT_INVALID"));
            }
            return authorized(Map.of("entrypoint",prepared.plan.document().get("entrypoint"),"capability",capability,"version",step.get("version")),path)
                    .thenCompose(ignored->{
                        clock.remaining(path);
                        return clock.waitFor(budget->prepared.adapters.get(capability).invoke().invoke(snapshot(agent),snapshot(protectedArgs),budget),path,"ADAPTER_EXCEPTION",()->{
                            require(trace.size()+3<=traceLimit,"BUDGET_EXCEEDED",path);
                            emit("CALL_DISPATCHED",Map.of("step",id,"capability",capability),path);
                        });
                    }).thenCompose(response->{
                        try {
                            Map<String,Object> value=map(response,path);
                            if ("SUCCESS".equals(value.get("kind"))) {
                                shape(value,Set.of("kind","value"),path);
                                state.put(id,Artifact.value(prepared.plan.dcm(),(String)cap.get("result"),value.get("value"),path,"ADAPTER_INVALID"));
                            } else if ("BUSINESS_REJECTION".equals(value.get("kind"))) {
                                shape(value,Set.of("kind","code"),path);
                                require(list(cap.get("errors"),path).contains(value.get("code")),"ADAPTER_INVALID",path); clock.remaining(path);
                                return CompletableFuture.completedFuture(terminate("BUSINESS_REJECTION",Map.of("code",value.get("code"),"path",path),null));
                            } else if ("TRANSIENT_FAILURE".equals(value.get("kind"))) {
                                shape(value,Set.of("kind"),path); throw new RuntimeFault("ADAPTER_TRANSIENT",path);
                            } else throw new RuntimeFault("ADAPTER_INVALID",path);
                        } catch (RuntimeException failure) {
                            if (failure instanceof RuntimeFault f && Set.of("ADAPTER_TRANSIENT","BUDGET_EXCEEDED","HOST_CONTEXT_INVALID").contains(f.code)) throw f;
                            throw new RuntimeFault("ADAPTER_INVALID",path);
                        }
                        clock.remaining(path); emit("CALL_SUCCEEDED",Map.of("step",id,"capability",capability),path); return step(index+1);
                    });
        }
    }
}
