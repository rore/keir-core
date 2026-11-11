package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import static dev.keir.runtime.RuntimeData.*;

/** Execution of admitted read and single-effect artifacts with fresh invocation authority. */
public final class KeirRuntime {
    public static final Map<String,String> ENVIRONMENT = Profile.ENVIRONMENT;
    private KeirRuntime() {}

    @FunctionalInterface public interface BoundEffectFunction {
        CompletionStage<?> invoke(Map<String,Object> args, Map<String,Object> protectedArgs, long remainingMs, BoundEffectContext context);
    }
    public record BoundEffectAdapter(String version, String mode, String keyType, BoundEffectFunction invoke) {}
    public record BoundEffectContext(Object requestKey, Map<String,Object> event, Deadline deadline) {
        public BoundEffectContext { event=Map.copyOf(event); }
    }
    public record BoundEffectInvocation(Map<String,Object> bindings, Deadline deadline, Object requestKey, Map<String,Object> event) {}
    public record Deadline(LongSupplier clock, long expiresAtMs) {}
    public record Diagnostic(String code, String path, String message) {}
    public record Preparation(Prepared prepared, Diagnostic diagnostic) {
        public boolean succeeded() { return prepared != null; }
    }

    public record ModelBinding(BoundModel boundModel, Diagnostic diagnostic) {
        public boolean succeeded() { return boundModel != null; }
    }
    /** An exact approved model and complete invoker snapshot, without invocation authority. */
    public static final class BoundModel {
        private final Map<String,Object> dcm;
        private final CheckedCel.Model types;
        private final Map<String,BoundEffectAdapter> implementations;

        private BoundModel(Map<String,Object> dcm, CheckedCel.Model types, Map<String,BoundEffectAdapter> implementations) {
            this.dcm=snapshot(dcm);
            this.types=types;
            this.implementations=Map.copyOf(implementations);
        }
    }

    public static final class Prepared {
        private final Artifact.Plan plan;
        private final CheckedCel.Bundle cel;
        private final Map<String,Long> grants;
        private final Map<String,List<String>> hostFields;
        private final long effectGrant;
        private final BoundModel boundModel;

        private Prepared(Artifact.Plan plan, CheckedCel.Bundle cel, Map<String,Long> grants,
                         Map<String,List<String>> hostFields, long effectGrant, BoundModel boundModel) {
            this.plan=plan;
            this.cel=cel;
            this.grants=Map.copyOf(grants);
            Map<String,List<String>> fields=new LinkedHashMap<>();
            hostFields.forEach((key,value)->fields.put(key,List.copyOf(value)));
            this.hostFields=Collections.unmodifiableMap(fields);
            this.effectGrant=effectGrant;
            this.boundModel=boundModel;
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
        @SuppressWarnings("unchecked") public List<Map<String,Object>> effects() { return (List<Map<String,Object>>)document.get("effects"); }
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> snapshot(Map<String,Object> value) {
        return (Map<String,Object>)immutable(value);
    }
    private static void require(boolean valid, String code, String path) {
        if (!valid) throw new RuntimeFault(code,path);
    }
    private static Diagnostic diagnostic(RuntimeException failure,String code,String path) {
        if (failure instanceof RuntimeFault fault) {
            code=fault.code; path=fault.path;
            if ((path.startsWith("/binding") || path.startsWith("/admission")) &&
                    !Set.of("BUDGET_EXCEEDED","AUTHORITY_DENIED").contains(code)) code="ADMISSION_INVALID";
        }
        return new Diagnostic(code,path,"The selected artifact or host configuration is invalid.");
    }

    public static ModelBinding bindModel(byte[] dcmBytes, Map<String,BoundEffectAdapter> implementations) {
        try {
            require(dcmBytes!=null,"ADMISSION_INVALID","/binding/dcm");
            require(dcmBytes.length<=Profile.DCM_BYTES,"BUDGET_EXCEEDED","/binding/dcm");
            Map<String,Object> dcm;
            CheckedCel.Model types;
            try {
                dcm=map(parseJson(dcmBytes.clone(),Profile.DCM_BYTES),"/binding/dcm");
                Artifact.validateModel(dcm);
                types=CheckedCel.bindModel(dcm);
            } catch (RuntimeException failure) {
                throw new RuntimeFault(failure instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)?"BUDGET_EXCEEDED":"ADMISSION_INVALID","/binding/dcm");
            }
            Map<String,Object> caps=map(dcm.get("capabilities"),"");
            require(implementations!=null && implementations.size()==caps.size(),"ADMISSION_INVALID","/binding/implementations");
            Map<String,BoundEffectAdapter> registered=new LinkedHashMap<>(implementations);
            require(registered.keySet().equals(caps.keySet()),"ADMISSION_INVALID","/binding/implementations");
            String mode=null, keyType=(String)map(dcm.get("hostKey"),"").get("type");
            for (String id:new TreeSet<>(caps.keySet())) {
                String path=pointer("/binding/implementations",id);
                Object value=registered.get(id);
                require(value instanceof BoundEffectAdapter,"ADMISSION_INVALID",path);
                BoundEffectAdapter adapter=(BoundEffectAdapter)value;
                require(Objects.equals(map(caps.get(id),"").get("version"),adapter.version()) &&
                        Objects.equals(keyType,adapter.keyType()) && adapter.invoke()!=null &&
                        ("simulation".equals(adapter.mode()) || "real".equals(adapter.mode())),"ADMISSION_INVALID",path);
                if (mode==null) mode=adapter.mode();
                require(mode.equals(adapter.mode()),"ADMISSION_INVALID",path);
            }
            return new ModelBinding(new BoundModel(dcm,types,registered),null);
        } catch (RuntimeException failure) { return new ModelBinding(null,diagnostic(failure,"ADMISSION_INVALID","/binding")); }
    }

    public static Preparation prepareProgram(byte[] artifactBytes, BoundModel boundModel) {
        try {
            require(boundModel!=null,"ADMISSION_INVALID","/binding");
            require(artifactBytes!=null,"ARTIFACT_INVALID","/artifact");
            require(artifactBytes.length<=Profile.ARTIFACT_BYTES,"BUDGET_EXCEEDED","/artifact");
            Map<String,Object> artifact=map(parseCanonical(artifactBytes.clone()),"/artifact");
            Artifact.validateEnvelope(artifact,boundModel.dcm);
            Map<String,Object> entry=map(map(boundModel.dcm.get("entrypoints"),"").get(artifact.get("entrypoint")),"");
            CapabilityPolicy.State policy=CapabilityPolicy.narrow(CapabilityPolicy.catalog(boundModel.dcm),
                    entry.getOrDefault("capabilityPolicy",Map.of()),"/artifact/entrypoint/capabilityPolicy","ARTIFACT_INVALID");
            boolean decision="DECISION".equals(map(entry.get("consumer"),"").get("kind"));
            long effectGrant=number(entry.getOrDefault("effectGrant",decision?0L:1L),0,1,"/artifact/entrypoint/effectGrant");
            Artifact.Plan plan=Artifact.validateProgram(artifact,boundModel.dcm,policy,effectGrant);
            CheckedCel.Bundle cel=CheckedCel.prepare(plan,boundModel.types);
            Map<String,List<String>> hostFields=new LinkedHashMap<>();
            Map<String,Object> caps=map(boundModel.dcm.get("capabilities"),"");
            for (String id:plan.calls().keySet()) for (var parameter:map(map(caps.get(id),"").get("parameters"),"").entrySet()) {
                Map<String,Object> definition=map(parameter.getValue(),"");
                if ("host".equals(definition.get("binding"))) hostFields.computeIfAbsent(parameter.getKey(),ignored->new ArrayList<>()).add((String)definition.get("type"));
            }
            return new Preparation(new Prepared(plan,cel,policy.ceilings(),hostFields,effectGrant,boundModel),null);
        } catch (RuntimeException failure) { return new Preparation(null,diagnostic(failure,"ARTIFACT_INVALID","/artifact")); }
    }

    public static CompletionStage<Outcome> invokeBoundEffectProgram(Prepared prepared, Object businessInput, BoundEffectInvocation invocation) {
        Run run=new Run(prepared);
        try {
            require(prepared!=null && invocation!=null && invocation.deadline()!=null,
                    "HOST_CONTEXT_INVALID","");
            Deadline deadline=new Deadline(invocation.deadline().clock(),invocation.deadline().expiresAtMs());
            Map<String,Object> bindings;
            try { bindings=contextFields(invocation.bindings(),prepared.hostFields.keySet()); }
            catch (RuntimeException failure) { throw new RuntimeFault("HOST_BINDING_INVALID","/hostBindings"); }
            Map<String,Object> event;
            try { event=contextFields(invocation.event(),Set.of("id","source")); }
            catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
            BoundEffectInvocation fresh=new BoundEffectInvocation(bindings,deadline,invocation.requestKey(),event);
            return run.start(businessInput,fresh).handle((outcome,failure)->failure==null?outcome:run.failed(failure));
        } catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    private static Map<String,Object> contextFields(Map<String,Object> value,Set<String> fields) {
        require(value!=null && value.size()==fields.size(),"HOST_CONTEXT_INVALID","");
        shape(value,fields,"");
        // Typed preflight validates and snapshots the raw values before dispatch.
        Map<String,Object> copied=new LinkedHashMap<>();
        for(String field:fields) copied.put(field,value.get(field));
        return Collections.unmodifiableMap(copied);
    }

    private static final class Run {
        private final Prepared prepared;
        private final List<Map<String,Object>> trace=new ArrayList<>();
        private final Map<String,Object> state=new LinkedHashMap<>(),bindings=new LinkedHashMap<>();
        private final Map<String,Integer> counts=new HashMap<>();
        private InvocationClock clock;
        private int totalCalls;
        private BoundEffectContext boundEffectContext;
        private Map<String,Object> receipt;
        private Run(Prepared prepared) {
            this.prepared=prepared;
            emit("INVOCATION_START",Map.of(),"");
        }
        private void emit(String event,Map<String,Object> attributes,String path) {
            require(trace.size()<Profile.TRACE_EVENTS-1,"BUDGET_EXCEEDED",path);
            Map<String,Object> item=new LinkedHashMap<>(); item.put("sequence",trace.size()); item.put("event",event); item.putAll(attributes); trace.add(item);
        }
        private Outcome terminate(String classification,Map<String,Object> error,Object result) {
            if (receipt!=null && "UNKNOWN".equals(receipt.get("state"))) classification="UNCERTAIN";
            Map<String,Object> end=new LinkedHashMap<>(Map.of("sequence",trace.size(),"event","TERMINATED"));
            if (error!=null) end.put("code",error.get("code")); trace.add(end);
            Map<String,Object> outcome=new LinkedHashMap<>(); outcome.put("classification",classification);
            if (error!=null) outcome.put("error",error);
            if (result!=null || error==null) outcome.put("result",result);
            outcome.put("effects",receipt==null?List.of():List.of(receipt));
            outcome.put("trace",trace); return new Outcome(outcome);
        }
        private Outcome failed(Throwable failure) {
            return failed(failure,null);
        }
        private Outcome failed(Throwable failure,Map<String,Object> cause) {
            while (failure instanceof CompletionException || failure instanceof ExecutionException) failure=failure.getCause();
            RuntimeFault fault=failure instanceof RuntimeFault f?f:new RuntimeFault("HOST_CONTEXT_INVALID","");
            Map<String,Object> error=new LinkedHashMap<>(Map.of("code",fault.code,"path",fault.path));
            if (cause!=null) error.put("cause",cause);
            return terminate(fault.code.equals("BUDGET_EXCEEDED")?"LIMIT_EXCEEDED":"FAILED",error,null);
        }
        private CompletionStage<Outcome> start(Object input,BoundEffectInvocation invocation) {
            clock=new InvocationClock(invocation.deadline().clock(),invocation.deadline().expiresAtMs());
            clock.remaining("");
            state.put("input",Artifact.value(prepared.plan.dcm(),(String)prepared.plan.entrypoint().get("input"),input,"/input","INPUT_INVALID"));
            try {
                shape(invocation.bindings(),prepared.hostFields.keySet(),"/hostBindings");
                for (var field:prepared.hostFields.entrySet()) {
                    Object value=invocation.bindings().get(field.getKey());
                    for (String type:field.getValue())
                        value=Artifact.value(prepared.plan.dcm(),type,value,"/hostBindings","HOST_BINDING_INVALID");
                    bindings.put(field.getKey(),value);
                }
            } catch (RuntimeException failure) { throw new RuntimeFault(failure instanceof RuntimeFault f && f.code.equals("BUDGET_EXCEEDED")?f.code:"HOST_BINDING_INVALID","/hostBindings"); }
            require(invocation.requestKey()!=null,"HOST_CONTEXT_INVALID","");
            Map<String,Object> eventTypes=map(prepared.plan.entrypoint().get("event"),"");
            Map<String,Object> event=new LinkedHashMap<>();
            event.put("id",Artifact.value(prepared.plan.dcm(),(String)eventTypes.get("idType"),invocation.event().get("id"),"","HOST_CONTEXT_INVALID"));
            event.put("source",Artifact.value(prepared.plan.dcm(),(String)eventTypes.get("sourceType"),invocation.event().get("source"),"","HOST_CONTEXT_INVALID"));
            boundEffectContext=new BoundEffectContext(invocation.requestKey(),event,invocation.deadline());
            return step(0);
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
            clock.remaining(path); require(index<Profile.STEPS,"BUDGET_EXCEEDED",path);
            Map<String,Object> step=map(steps.get(index),path); String id=(String)step.get("id");
            emit("STEP_START",Map.of("step",id),path);
            if ("BIND".equals(step.get("op"))) {
                state.put(id,evaluate(step.get("value"),path)); emit("VALUE_BOUND",Map.of("step",id),path); return step(index+1);
            }
            if (step.containsKey("when")) {
                boolean taken=Boolean.TRUE.equals(state.get(step.get("when")));
                emit("GUARD_EVALUATED",Map.of("step",id,"taken",taken),path);
                if (!taken) { emit("STEP_SKIPPED",Map.of("step",id),path); return step(index+1); }
            }
            String capability=(String)step.get("capability");
            Map<String,Object> cap=map(map(prepared.plan.dcm().get("capabilities"),"").get(capability),"");
            int count=counts.merge(capability,1,Integer::sum); totalCalls++;
            require(totalCalls<=Profile.CALLS,"BUDGET_EXCEEDED",path);
            boolean write="WRITE".equals(cap.get("effect"));
            if (write) require(prepared.effectGrant>0,"AUTHORITY_DENIED",path);
            require(count<=prepared.grants.getOrDefault(capability,0L),"AUTHORITY_DENIED",path);
            Map<String,Object> agent=new LinkedHashMap<>(),protectedArgs=new LinkedHashMap<>();
            Map<String,Object> args=map(step.get("args"),path);
            for (var parameter:map(cap.get("parameters"),"").entrySet()) {
                String name=parameter.getKey(); Map<String,Object> definition=map(parameter.getValue(),""); String type=(String)definition.get("type");
                if ("host".equals(definition.get("binding"))) protectedArgs.put(name,Artifact.value(prepared.plan.dcm(),type,bindings.get(name),"/hostBindings","HOST_BINDING_INVALID"));
                else if (args.containsKey(name)) agent.put(name,Artifact.value(prepared.plan.dcm(),type,evaluate(args.get(name),path),path,"INPUT_INVALID"));
            }
            clock.remaining(path);
            // Preserve the adapter-entry check after the pre-dispatch clock read.
            clock.remaining(path);
            var agentSnapshot=snapshot(agent); var protectedSnapshot=snapshot(protectedArgs);
            java.util.function.LongFunction<CompletionStage<?>> action=budget->
                    prepared.boundModel.implementations.get(capability).invoke().invoke(agentSnapshot,protectedSnapshot,budget,boundEffectContext);
            Runnable dispatch=()->{
                require(trace.size()+(write?5:3)<=Profile.TRACE_EVENTS,"BUDGET_EXCEEDED",path);
                if (write) {
                    receipt=Map.of("step",id,"capability",capability,"state","UNKNOWN");
                    emit("EFFECT_INTENT",Map.of("step",id,"capability",capability),path);
                }
                emit("CALL_DISPATCHED",Map.of("step",id,"capability",capability),path);
            };
            var pending=write ? clock.waitForEffect(action,path,"ADAPTER_EXCEPTION",dispatch,
                    response->settle(response,path),()->emit("EFFECT_RECORDED",receipt,path))
                    : clock.waitFor(action,path,"ADAPTER_EXCEPTION",dispatch);
            return pending.thenCompose(response->{
                try {
                    Map<String,Object> value=map(response,path);
                    if ("SUCCESS".equals(value.get("kind"))) {
                        shape(value,write?Set.of("kind","value","effect"):Set.of("kind","value"),path);
                        if (write) require("CONFIRMED".equals(value.get("effect")),"ADAPTER_INVALID",path);
                        Object result=Artifact.value(prepared.plan.dcm(),(String)cap.get("result"),value.get("value"),path,"ADAPTER_INVALID");
                        if (!step.containsKey("when") || Artifact.guardedResultDataflow(prepared.plan.document())) state.put(id,result);
                    } else if ("BUSINESS_REJECTION".equals(value.get("kind"))) {
                        shape(value,write?Set.of("kind","code","effect"):Set.of("kind","code"),path);
                        if (write) require("NONE".equals(value.get("effect")),"ADAPTER_INVALID",path);
                        require(list(cap.get("errors"),path).contains(value.get("code")),"ADAPTER_INVALID",path); clock.remaining(path);
                        return CompletableFuture.completedFuture(rejected(step,(String)value.get("code"),path));
                    } else if ("TRANSIENT_FAILURE".equals(value.get("kind"))) {
                        shape(value,write?Set.of("kind","effect"):Set.of("kind"),path);
                        if (write) require("NONE".equals(value.get("effect")),"ADAPTER_INVALID",path);
                        throw new RuntimeFault("ADAPTER_TRANSIENT",path);
                    } else if (write && "UNKNOWN_COMMIT".equals(value.get("kind"))) {
                        shape(value,Set.of("kind","effect"),path);
                        require("UNKNOWN".equals(value.get("effect")),"ADAPTER_INVALID",path);
                        throw new RuntimeFault("UNKNOWN_COMMIT",path);
                    } else throw new RuntimeFault("ADAPTER_INVALID",path);
                } catch (RuntimeException failure) {
                    if (failure instanceof RuntimeFault f && Set.of("ADAPTER_TRANSIENT","BUDGET_EXCEEDED","HOST_CONTEXT_INVALID","UNKNOWN_COMMIT").contains(f.code)) throw f;
                    throw new RuntimeFault("ADAPTER_INVALID",path);
                }
                clock.remaining(path); emit("CALL_SUCCEEDED",Map.of("step",id,"capability",capability),path); return step(index+1);
            });
        }

        private Object settle(Object response,String path) {
            if (!(response instanceof Map<?,?>)) return response;
            try {
                Map<String,Object> value=Collections.unmodifiableMap(new LinkedHashMap<>(map(response,path)));
                String evidence=null;
                if ("SUCCESS".equals(value.get("kind")) && value.keySet().equals(Set.of("kind","value","effect")) && "CONFIRMED".equals(value.get("effect"))) evidence="CONFIRMED";
                else if ("BUSINESS_REJECTION".equals(value.get("kind")) && value.keySet().equals(Set.of("kind","code","effect")) && "NONE".equals(value.get("effect"))) evidence="NONE";
                else if ("TRANSIENT_FAILURE".equals(value.get("kind")) && value.keySet().equals(Set.of("kind","effect")) && "NONE".equals(value.get("effect"))) evidence="NONE";
                if (evidence!=null) receipt=Map.of("step",receipt.get("step"),"capability",receipt.get("capability"),"state",evidence);
                return value;
            } catch (RuntimeException failure) { throw new RuntimeFault("ADAPTER_INVALID",path); }
        }

        private Outcome rejected(Map<String,Object> step,String code,String path) {
            Map<String,Object> error=Map.of("code",code,"path",path);
            if (!step.containsKey("onError") || !map(step.get("onError"),path).containsKey(code)) return terminate("BUSINESS_REJECTION",error,null);
            String handlerPath=pointer(path+"/onError",code);
            try {
                Map<String,Object> handler=map(map(step.get("onError"),path).get(code),handlerPath);
                Map<String,Object> result=new LinkedHashMap<>();
                for (var field:map(handler.get("fields"),handlerPath).entrySet())
                    result.put(field.getKey(),evaluate(field.getValue(),pointer(handlerPath,field.getKey())));
                Object value=Artifact.value(prepared.plan.dcm(),(String)handler.get("type"),result,handlerPath,"EXPRESSION_FAILED");
                clock.remaining(handlerPath); emit("RETURNED",Map.of(),handlerPath);
                return terminate("BUSINESS_REJECTION",error,value);
            } catch (RuntimeException failure) { return failed(failure,error); }
        }
    }
}
