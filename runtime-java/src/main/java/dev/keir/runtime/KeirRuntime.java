package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import static dev.keir.runtime.RuntimeData.*;

/** Execution of admitted read and single-effect artifacts with fresh invocation authority. */
public final class KeirRuntime {
    public static final Map<String,String> ENVIRONMENT = Profile.ENVIRONMENT;
    private KeirRuntime() {}

    @FunctionalInterface public interface AdapterFunction {
        CompletionStage<?> invoke(Map<String,Object> agentArgs, Map<String,Object> protectedArgs, long remainingMs);
    }
    @FunctionalInterface public interface Authorizer { CompletionStage<?> authorize(Map<String,Object> request); }
    @FunctionalInterface public interface EffectAdapterFunction {
        CompletionStage<?> invoke(Map<String,Object> agentArgs, Map<String,Object> protectedArgs, long remainingMs, HostContext context);
    }
    @FunctionalInterface public interface KeyValidator { Object validate(Object key); }
    @FunctionalInterface public interface BoundEffectFunction {
        CompletionStage<?> invoke(Map<String,Object> args, Map<String,Object> protectedArgs, long remainingMs, BoundEffectContext context);
    }
    public record BoundEffectAdapter(String version, String mode, String keyType, BoundEffectFunction invoke) {}
    public record BoundEffectContext(Object requestKey, Map<String,Object> event, Deadline deadline) {
        public BoundEffectContext { event=Map.copyOf(event); }
    }
    public record BoundEffectInvocation(Map<String,Object> bindings, Deadline deadline, Object requestKey, Map<String,Object> event) {}
    public record Adapter(String version, String mode, AdapterFunction invoke) {}
    public record EffectAdapter(String version, String mode, String keyType, EffectAdapterFunction invoke) {}
    public record KeyBinding(String type, KeyValidator validate) {}
    public record Qualification(Map<String,Object> evidence, Supplier<?> isCurrent) {}
    public record HostContext(Object requestKey, Map<String,Object> event) {
        public HostContext { event=Map.copyOf(event); }
    }
    public record Deadline(LongSupplier clock, long expiresAtMs) {}
    public record Invocation(Map<String,Object> bindings, Deadline deadline, Authorizer authorize, Map<String,Long> grants) {}
    public record EffectInvocation(Map<String,Object> bindings, Deadline deadline, Authorizer authorize,
                                   Map<String,Long> grants, Long effectGrant, Object requestKey, Map<String,Object> event) {}
    public record Diagnostic(String code, String path, String message) {}
    public record Preparation(Prepared prepared, Diagnostic diagnostic) {
        public boolean succeeded() { return prepared != null; }
    }

    public record ModelBinding(BoundModel boundModel, Diagnostic diagnostic) {
        public boolean succeeded() { return boundModel != null; }
    }
    public record Invocation2(String scope, Map<String,Object> bindings, Deadline deadline,
                              Authorizer authorize, Optional<Map<String,Object>> capabilityPolicy) {}
    public record EffectInvocation2(String scope, Map<String,Object> bindings, Deadline deadline,
                                    Authorizer authorize, Optional<Map<String,Object>> capabilityPolicy,
                                    OptionalLong effectGrant, Object requestKey, Map<String,Object> event) {}

    /** An exact approved model and complete invoker snapshot, without invocation authority. */
    public static final class BoundModel {
        private final Map<String,Object> dcm, modelRef;
        private final String scope, profile, mode;
        private final boolean effect;
        private final KeyBinding keyBinding;
        private final Map<String,Adapter> adapters;
        private final Map<String,EffectAdapter> effectAdapters;
        private final Map<String,BoundEffectAdapter> boundEffectAdapters;
        private final CheckedCel.Model types;
        private final Qualification qualification;
        private BoundModel(Map<String,Object> dcm, Map<String,Object> modelRef, String scope,
                           String profile, String mode, KeyBinding keyBinding, Map<String,Adapter> adapters,
                           Map<String,EffectAdapter> effectAdapters, CheckedCel.Model types, Qualification qualification) {
            this(dcm,modelRef,scope,profile,mode,keyBinding,adapters,effectAdapters,types,qualification,Map.of());
        }
        private BoundModel(Map<String,Object> dcm, Map<String,Object> modelRef, String scope,
                           String profile, String mode, KeyBinding keyBinding, Map<String,Adapter> adapters,
                           Map<String,EffectAdapter> effectAdapters, CheckedCel.Model types, Qualification qualification,
                           Map<String,BoundEffectAdapter> boundEffectAdapters) {
            this.dcm=snapshot(dcm); this.modelRef=snapshot(modelRef); this.scope=scope;
            this.profile=profile; this.mode=mode; this.effect=Set.of("keir-effect/2","keir-effect/3","keir-effect/4").contains(profile);
            this.keyBinding=keyBinding; this.adapters=Map.copyOf(adapters);
            this.effectAdapters=Map.copyOf(effectAdapters); this.types=types;
            this.qualification=qualification;
            this.boundEffectAdapters=Map.copyOf(boundEffectAdapters);
        }
    }

    public static final class Prepared {
        private final Artifact.Plan plan;
        private final CheckedCel.Bundle cel;
        private final Map<String,Adapter> adapters;
        private final Map<String,Long> grants;
        private final Map<String,Long> limits;
        private final Map<String,List<String>> hostFields;
        private final Map<String,EffectAdapter> effectAdapters;
        private final long effectGrant;
        private final KeyBinding keyBinding;
        private final BoundModel boundModel;
        private final CapabilityPolicy.State policy;
        private Prepared(Artifact.Plan plan, CheckedCel.Bundle cel, Map<String,Adapter> adapters,
                         Map<String,Long> grants, Map<String,Long> limits, Map<String,List<String>> hostFields,
                         Map<String,EffectAdapter> effectAdapters, long effectGrant, KeyBinding keyBinding) {
            this(plan,cel,adapters,grants,limits,hostFields,effectAdapters,effectGrant,keyBinding,null,null);
        }
        private Prepared(Artifact.Plan plan, CheckedCel.Bundle cel, Map<String,Adapter> adapters,
                         Map<String,Long> grants, Map<String,Long> limits, Map<String,List<String>> hostFields,
                         Map<String,EffectAdapter> effectAdapters, long effectGrant, KeyBinding keyBinding,
                         BoundModel boundModel, CapabilityPolicy.State policy) {
            this.plan=plan; this.cel=cel; this.adapters=Map.copyOf(adapters);
            this.grants=Map.copyOf(grants); this.limits=Map.copyOf(limits);
            Map<String,List<String>> fields = new LinkedHashMap<>();
            hostFields.forEach((key,value)->fields.put(key,List.copyOf(value)));
            this.hostFields=Collections.unmodifiableMap(fields);
            this.effectAdapters=Map.copyOf(effectAdapters); this.effectGrant=effectGrant; this.keyBinding=keyBinding;
            this.boundModel=boundModel; this.policy=policy;
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
    private static Map<String,Object> bounded(Object value, String path, int max) {
        Map<String,Object> result=map(value,path);
        require(result.size()<=max,"ARTIFACT_INVALID",path); return result;
    }
    private static boolean same(Object left,Object right) { return Arrays.equals(canonical(left),canonical(right)); }
    private static boolean matchingData(Object left,Object right) {
        try { return same(left,right); } catch (RuntimeException failure) { return false; }
    }

    private static void closed(Object value, Set<String> required, Set<String> optional, String path) {
        Map<String,Object> record=map(value,path);
        Set<String> allowed=new HashSet<>(required); allowed.addAll(optional);
        require(record.keySet().containsAll(required) && allowed.containsAll(record.keySet()),"ADMISSION_INVALID",path);
    }
    private static Diagnostic diagnostic(RuntimeException failure,String code,String path) {
        if (failure instanceof RuntimeFault fault) {
            code=fault.code; path=fault.path;
            if ((path.startsWith("/binding") || path.startsWith("/admission")) &&
                    !Set.of("BUDGET_EXCEEDED","AUTHORITY_DENIED").contains(code)) code="ADMISSION_INVALID";
        }
        return new Diagnostic(code,path,"The selected artifact or host configuration is invalid.");
    }

    public static ModelBinding bindModel(byte[] dcmBytes, Map<String,Object> approval, Map<String,?> invokers) {
        return bindModel(dcmBytes,approval,invokers,null,false);
    }

    public static ModelBinding bindModel(byte[] dcmBytes, Map<String,BoundEffectAdapter> implementations) {
        try {
            require(dcmBytes!=null,"ADMISSION_INVALID","/binding/dcm");
            require(dcmBytes.length<=Profile.DCM_BYTES,"BUDGET_EXCEEDED","/binding/dcm");
            Map<String,Object> dcm;
            CheckedCel.Model types;
            try {
                dcm=map(parseJson(dcmBytes.clone(),Profile.DCM_BYTES),"/binding/dcm");
                Artifact.validateModel2(dcm,"keir-effect/4");
                types=CheckedCel.bindModel2(dcm);
            } catch (RuntimeException failure) {
                throw new RuntimeFault(failure instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)?"BUDGET_EXCEEDED":"ADMISSION_INVALID","/binding/dcm");
            }
            Map<String,Object> caps=map(dcm.get("capabilities"),"");
            require(implementations!=null && implementations.keySet().equals(caps.keySet()),"ADMISSION_INVALID","/binding/implementations");
            Map<String,BoundEffectAdapter> registered=new LinkedHashMap<>(implementations);
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
            return new ModelBinding(new BoundModel(dcm,Artifact.modelRef(dcm),null,"keir-effect/4",mode,null,
                    Map.of(),Map.of(),types,null,registered),null);
        } catch (RuntimeException failure) { return new ModelBinding(null,diagnostic(failure,"ADMISSION_INVALID","/binding")); }
    }

    public static ModelBinding bindModel(byte[] dcmBytes, Map<String,Object> approval, Map<String,?> invokers, Qualification qualification) {
        return bindModel(dcmBytes,approval,invokers,qualification,true);
    }

    private static ModelBinding bindModel(byte[] dcmBytes, Map<String,Object> approval, Map<String,?> invokers,
                                          Qualification qualification, boolean supplied) {
        try {
            Set<String> required=Set.of("scope","modelRef","profile","environment","mode");
            closed(approval,required,Set.of("keyBinding"),"/binding/approval");
            Map<String,Object> selected=new LinkedHashMap<>(approval);
            boolean qualified="keir-effect/3".equals(selected.get("profile"));
            boolean effect=qualified || "keir-effect/2".equals(selected.get("profile"));
            require(effect==selected.containsKey("keyBinding"),"ADMISSION_INVALID","/binding/approval");
            // Validate approval values in lexical order before inspecting untrusted model bytes.
            require(Profile.ENVIRONMENT.equals(selected.get("environment")),"ADMISSION_INVALID","/binding/approval/environment");
            KeyBinding key=null;
            if (effect) {
                require(selected.get("keyBinding") instanceof KeyBinding,"ADMISSION_INVALID","/binding/approval/keyBinding");
                key=(KeyBinding)selected.get("keyBinding");
                text(key.type(),"/binding/approval/keyBinding",true,256);
                require(key.validate()!=null,"ADMISSION_INVALID","/binding/approval/keyBinding");
            }
            require("simulation".equals(selected.get("mode")) || (!effect || qualified) && "real".equals(selected.get("mode")),"ADMISSION_INVALID","/binding/approval/mode");
            String refPath="/binding/approval/modelRef";
            shape(selected.get("modelRef"),Set.of("domainId","domainVersion","format","digest","descriptorRecipe"),refPath);
            Map<String,Object> reference=new LinkedHashMap<>(map(selected.get("modelRef"),refPath));
            require("keir-record-descriptors/1".equals(reference.get("descriptorRecipe")),"ADMISSION_INVALID",refPath+"/descriptorRecipe");
            require(reference.get("digest") instanceof String pin && pin.matches("sha256:[0-9a-f]{64}"),"ADMISSION_INVALID",refPath+"/digest");
            text(reference.get("domainId"),refPath+"/domainId",true,256);
            text(reference.get("domainVersion"),refPath+"/domainVersion",true,256);
            require((qualified?"keir-dcm/effect-3":effect?"keir-dcm/effect-2":"keir-dcm/read-2").equals(reference.get("format")),"ADMISSION_INVALID",refPath+"/format");
            require(effect || "keir-read/2".equals(selected.get("profile")),"ADMISSION_INVALID","/binding/approval/profile");
            String scope=text(selected.get("scope"),"/binding/approval/scope",true,256);
            require(dcmBytes!=null,"ADMISSION_INVALID","/binding/dcm");
            require(dcmBytes.length<=Profile.DCM_BYTES,"BUDGET_EXCEEDED","/binding/dcm");
            Map<String,Object> dcm;
            try {
                dcm=map(parseJson(dcmBytes.clone(),Profile.DCM_BYTES),"/binding/dcm");
                require(canonical(dcm).length<=Profile.DCM_BYTES,"BUDGET_EXCEEDED","/binding/dcm");
                Artifact.validateModel2(dcm,(String)selected.get("profile"));
            } catch (RuntimeException failure) {
                throw new RuntimeFault(failure instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)?"BUDGET_EXCEEDED":"ADMISSION_INVALID","/binding/dcm");
            }
            require(same(reference,Artifact.modelRef(dcm)),"ADMISSION_INVALID",refPath);
            if (effect) require(Objects.equals(key.type(),map(dcm.get("hostKey"),"").get("type")),"ADMISSION_INVALID","/binding/approval/keyBinding");
            boolean needsQualification=qualified && "real".equals(selected.get("mode"));
            require(supplied==needsQualification && (!supplied || qualification!=null),"ADMISSION_INVALID","/binding/qualification");
            Qualification retained=needsQualification ? qualification(qualification,reference,scope,key.type(),dcm) : null;
            CheckedCel.Model types;
            try { types=CheckedCel.bindModel2(dcm); }
            catch (RuntimeException failure) { throw new RuntimeFault(failure instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)?"BUDGET_EXCEEDED":"ADMISSION_INVALID","/binding/dcm/types"); }
            Map<String,Object> caps=map(dcm.get("capabilities"),"");
            require(invokers!=null && invokers.keySet().equals(caps.keySet()),"ADMISSION_INVALID","/binding/invokers");
            Map<String,?> registrations=new LinkedHashMap<>(invokers);
            Map<String,Adapter> reads=new LinkedHashMap<>(); Map<String,EffectAdapter> effects=new LinkedHashMap<>();
            for (String id:new TreeSet<>(caps.keySet())) {
                String path=pointer("/binding/invokers",id);
                Object registration=registrations.get(id); Object version=map(caps.get(id),"").get("version");
                if (effect) {
                    require(registration instanceof EffectAdapter,"ADMISSION_INVALID",path);
                    EffectAdapter adapter=(EffectAdapter)registration;
                    require(Objects.equals(version,adapter.version()) && Objects.equals(selected.get("mode"),adapter.mode()) &&
                            Objects.equals(key.type(),adapter.keyType()) && adapter.invoke()!=null,"ADMISSION_INVALID",path);
                    effects.put(id,adapter);
                } else {
                    require(registration instanceof Adapter,"ADMISSION_INVALID",path);
                    Adapter adapter=(Adapter)registration;
                    require(Objects.equals(version,adapter.version()) && Objects.equals(selected.get("mode"),adapter.mode()) && adapter.invoke()!=null,"ADMISSION_INVALID",path);
                    reads.put(id,adapter);
                }
            }
            BoundModel bound=new BoundModel(dcm,reference,scope,(String)selected.get("profile"),(String)selected.get("mode"),key,reads,effects,types,retained);
            current(bound,"ADMISSION_INVALID","/binding/qualification");
            return new ModelBinding(bound,null);
        } catch (RuntimeException failure) { return new ModelBinding(null,diagnostic(failure,"ADMISSION_INVALID","/binding/approval")); }
    }

    private static Qualification qualification(Qualification qualification,Map<String,Object> reference,String scope,
                                                String keyType,Map<String,Object> dcm) {
        String path="/binding/qualification";
        try {
            Map<String,Object> evidence=map(parseCanonical(canonical(qualification.evidence(),16384)),path);
            shape(evidence,Set.of("contract","id","revision","scope","modelRef","environment","hostBuild","keyType","capabilities","reportDigest"),path);
            require("keir-real-effect-evidence/1".equals(evidence.get("contract")),"ADMISSION_INVALID",path);
            text(evidence.get("id"),path,true,128);
            number(evidence.get("revision"),1,9007199254740991L,path);
            require(scope.equals(text(evidence.get("scope"),path,true,256)) &&
                    keyType.equals(text(evidence.get("keyType"),path,true,256)),"ADMISSION_INVALID",path);
            require(same(reference,evidence.get("modelRef")) && same(Profile.ENVIRONMENT,evidence.get("environment")),"ADMISSION_INVALID",path);
            for (String field:List.of("hostBuild","reportDigest"))
                require(evidence.get(field) instanceof String digest && digest.matches("sha256:[0-9a-f]{64}"),"ADMISSION_INVALID",path);
            Map<String,Object> caps=map(dcm.get("capabilities"),path);
            List<Object> catalog=new ArrayList<>();
            for (String id:new TreeSet<>(caps.keySet())) catalog.add(Map.of("id",id,"version",map(caps.get(id),path).get("version")));
            require(same(catalog,evidence.get("capabilities")) && qualification.isCurrent()!=null,"ADMISSION_INVALID",path);
            return new Qualification(snapshot(evidence),qualification.isCurrent());
        } catch (RuntimeException failure) {
            throw new RuntimeFault(failure instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)?"BUDGET_EXCEEDED":"ADMISSION_INVALID",path);
        }
    }

    private static void current(BoundModel bound,String code,String path) {
        require(isCurrent(bound),code,path);
    }

    private static boolean isCurrent(BoundModel bound) {
        if (bound==null || bound.qualification==null) return true;
        Object accepted;
        try { accepted=bound.qualification.isCurrent().get(); }
        catch (Throwable failure) { accepted=null; }
        return Boolean.TRUE.equals(accepted);
    }

    public static Preparation prepareProgram(byte[] artifactBytes, BoundModel boundModel, Map<String,Object> admission) {
        try {
            require(boundModel!=null && !"keir-effect/4".equals(boundModel.profile),"ADMISSION_INVALID","/binding");
            Set<String> optional=new HashSet<>(Set.of("capabilityPolicy","limits"));
            if (boundModel.effect) optional.add("effectGrant");
            closed(admission,Set.of("expectedArtifactDigest","expectedDcmDigest","entrypoint","consumer","compiler","environment","mode"),optional,"/admission");
            // Preparation is synchronous; validate each field before retaining immutable plan state.
            Map<String,Object> selected=new LinkedHashMap<>(admission);
            require(selected.get("expectedArtifactDigest") instanceof String pin && pin.matches("sha256:[0-9a-f]{64}"),"ADMISSION_INVALID","/admission/expectedArtifactDigest");
            require(artifactBytes!=null,"ARTIFACT_INVALID","/artifact");
            require(artifactBytes.length<=Profile.ARTIFACT_BYTES,"BUDGET_EXCEEDED","/artifact");
            byte[] bytes=artifactBytes.clone();
            require(digest(bytes).equals(selected.get("expectedArtifactDigest")),"PIN_MISMATCH","/artifact");
            Map<String,Object> artifact=map(parseCanonical(bytes),"/artifact");
            Artifact.validateEnvelope2(artifact,boundModel.dcm,boundModel.profile);
            require(boundModel.profile.equals(artifact.get("profile")),"ARTIFACT_INVALID","/artifact/profile");
            Map<String,Object> entries=map(boundModel.dcm.get("entrypoints"),"");
            require(entries.containsKey(artifact.get("entrypoint")),"ARTIFACT_INVALID","/artifact/entrypoint");
            Map<String,Object> entry=map(entries.get(artifact.get("entrypoint")),"");
            CapabilityPolicy.State hook=CapabilityPolicy.narrow(CapabilityPolicy.catalog(boundModel.dcm),entry.getOrDefault("capabilityPolicy",Map.of()),"/admission/capabilityPolicy","ADMISSION_INVALID");
            CapabilityPolicy.State policy=CapabilityPolicy.narrow(hook,selected.getOrDefault("capabilityPolicy",Map.of()),"/admission/capabilityPolicy","ADMISSION_INVALID");
            require(Objects.equals(artifact.get("compiler"),selected.get("compiler")),"ADMISSION_INVALID","/admission/compiler");
            require(matchingData(entry.get("consumer"),selected.get("consumer")),"ADMISSION_INVALID","/admission/consumer");
            long effectGrant=0;
            if (boundModel.effect) effectGrant=number(selected.getOrDefault("effectGrant",entry.getOrDefault("effectGrant",1L)),0,
                    number(entry.getOrDefault("effectGrant",1L),0,1,"/admission/effectGrant"),"/admission/effectGrant");
            require(Objects.equals(artifact.get("entrypoint"),selected.get("entrypoint")),"ADMISSION_INVALID","/admission/entrypoint");
            require(Profile.ENVIRONMENT.equals(selected.get("environment")),"ADMISSION_INVALID","/admission/environment");
            require(Objects.equals(boundModel.modelRef.get("digest"),selected.get("expectedDcmDigest")),"ADMISSION_INVALID","/admission/expectedDcmDigest");
            Map<String,Long> limits=limits2(selected);
            require(boundModel.mode.equals(selected.get("mode")),"ADMISSION_INVALID","/admission/mode");
            Artifact.Plan plan=Artifact.validateProgram2(artifact,boundModel.dcm,boundModel.profile,limits,policy,effectGrant);
            CheckedCel.Bundle cel=CheckedCel.prepare2(plan,boundModel.types);
            require(cel.totalNodes()<=limits.get("totalAstNodes"),"BUDGET_EXCEEDED","/artifact/expressions");
            Map<String,List<String>> hostFields=new LinkedHashMap<>();
            Map<String,Object> caps=map(boundModel.dcm.get("capabilities"),"");
            for (String id:plan.calls().keySet()) for (var parameter:map(map(caps.get(id),"").get("parameters"),"").entrySet()) {
                Map<String,Object> definition=map(parameter.getValue(),"");
                if ("host".equals(definition.get("binding"))) hostFields.computeIfAbsent(parameter.getKey(),ignored->new ArrayList<>()).add((String)definition.get("type"));
            }
            current(boundModel,"ADMISSION_INVALID","/admission/qualification");
            return new Preparation(new Prepared(plan,cel,boundModel.adapters,policy.ceilings(),limits,hostFields,boundModel.effectAdapters,
                    effectGrant,boundModel.keyBinding,boundModel,policy),null);
        } catch (RuntimeException failure) { return new Preparation(null,diagnostic(failure,"ARTIFACT_INVALID","/artifact")); }
    }

    public static Preparation prepareProgram(byte[] artifactBytes, BoundModel boundModel) {
        try {
            require(boundModel!=null && "keir-effect/4".equals(boundModel.profile),"ADMISSION_INVALID","/binding");
            require(artifactBytes!=null,"ARTIFACT_INVALID","/artifact");
            require(artifactBytes.length<=Profile.ARTIFACT_BYTES,"BUDGET_EXCEEDED","/artifact");
            Map<String,Object> artifact=map(parseCanonical(artifactBytes.clone()),"/artifact");
            Artifact.validateEnvelope2(artifact,boundModel.dcm,boundModel.profile);
            Map<String,Object> entry=map(map(boundModel.dcm.get("entrypoints"),"").get(artifact.get("entrypoint")),"");
            CapabilityPolicy.State policy=CapabilityPolicy.narrow(CapabilityPolicy.catalog(boundModel.dcm),
                    entry.getOrDefault("capabilityPolicy",Map.of()),"/artifact/entrypoint/capabilityPolicy","ARTIFACT_INVALID");
            long effectGrant=number(entry.getOrDefault("effectGrant",1L),0,1,"/artifact/entrypoint/effectGrant");
            Map<String,Long> limits=limits2(Map.of());
            Artifact.Plan plan=Artifact.validateProgram2(artifact,boundModel.dcm,boundModel.profile,limits,policy,effectGrant);
            CheckedCel.Bundle cel=CheckedCel.prepare2(plan,boundModel.types);
            require(cel.totalNodes()<=limits.get("totalAstNodes"),"BUDGET_EXCEEDED","/artifact/expressions");
            Map<String,List<String>> hostFields=new LinkedHashMap<>();
            Map<String,Object> caps=map(boundModel.dcm.get("capabilities"),"");
            for (String id:plan.calls().keySet()) for (var parameter:map(map(caps.get(id),"").get("parameters"),"").entrySet()) {
                Map<String,Object> definition=map(parameter.getValue(),"");
                if ("host".equals(definition.get("binding"))) hostFields.computeIfAbsent(parameter.getKey(),ignored->new ArrayList<>()).add((String)definition.get("type"));
            }
            return new Preparation(new Prepared(plan,cel,Map.of(),policy.ceilings(),limits,hostFields,Map.of(),
                    effectGrant,null,boundModel,policy),null);
        } catch (RuntimeException failure) { return new Preparation(null,diagnostic(failure,"ARTIFACT_INVALID","/artifact")); }
    }

    private static Map<String,Long> limits2(Map<String,Object> admission) {
        Map<String,Long> limits=new LinkedHashMap<>(Map.of("steps",(long)Profile.STEPS,"calls",(long)Profile.CALLS,
                "totalAstNodes",(long)Profile.TOTAL_AST_NODES,"traceEvents",(long)Profile.TRACE_EVENTS));
        if (admission.containsKey("limits")) {
            Map<String,Object> supplied=bounded(admission.get("limits"),"/admission/limits",4);
            require(limits.keySet().containsAll(supplied.keySet()),"ADMISSION_INVALID","/admission/limits");
            for (String field:new TreeSet<>(supplied.keySet())) limits.put(field,number(supplied.get(field),field.equals("traceEvents")?2:0,limits.get(field),"/admission/limits"));
        }
        return limits;
    }

    public static Preparation prepare(byte[] artifactBytes, Map<String,Object> admission, Map<String,Adapter> adapters) {
        return prepare(artifactBytes,admission,adapters,Map.of(),false);
    }

    public static Preparation prepareEffect(byte[] artifactBytes, Map<String,Object> admission, Map<String,EffectAdapter> adapters) {
        return prepare(artifactBytes,admission,Map.of(),adapters,true);
    }

    private static Preparation prepare(byte[] artifactBytes, Map<String,Object> admission, Map<String,Adapter> adapters,
                                       Map<String,EffectAdapter> effectAdapters, boolean effect) {
        try {
            Set<String> fields=new HashSet<>(Set.of("expectedArtifactDigest","expectedDcmDigest","entrypoint","consumer","compiler","environment","mode","grants"));
            if (effect) fields.addAll(Set.of("effectGrant","keyBinding"));
            if (admission!=null && admission.containsKey("limits")) fields.add("limits");
            shape(admission,fields,"/admission");
            for (String field:List.of("expectedArtifactDigest","expectedDcmDigest"))
                require(admission.get(field) instanceof String text && text.matches("sha256:[0-9a-f]{64}"),"ADMISSION_INVALID","/admission/"+field);
            require(artifactBytes!=null && artifactBytes.length<=Profile.ARTIFACT_BYTES,"BUDGET_EXCEEDED","/artifact");
            byte[] bytes=artifactBytes.clone();
            require(digest(bytes).equals(admission.get("expectedArtifactDigest")),"PIN_MISMATCH","/artifact");
            Artifact.Plan plan=Artifact.validate(map(parseCanonical(bytes),"/artifact"),effect);
            Map<String,Object> artifact=plan.document();
            require(Objects.equals(artifact.get("dcmDigest"),admission.get("expectedDcmDigest"))
                    && Objects.equals(artifact.get("entrypoint"),admission.get("entrypoint"))
                    && Objects.equals(artifact.get("compiler"),admission.get("compiler"))
                    && same(artifact.get("environment"),admission.get("environment"))
                    && same(plan.entrypoint().get("consumer"),admission.get("consumer")),"PIN_MISMATCH","/admission");
            require("simulation".equals(admission.get("mode")) || !effect && "real".equals(admission.get("mode")),"ADMISSION_INVALID","/admission/mode");
            long effectGrant=0;
            KeyBinding keyBinding=null;
            if (effect) {
                effectGrant=number(admission.get("effectGrant"),0,((Number)plan.entrypoint().get("effectGrant")).longValue(),"/admission/effectGrant");
                require(plan.writeCount()<=effectGrant,"AUTHORITY_DENIED","/admission/effectGrant");
                require(admission.get("keyBinding") instanceof KeyBinding,"ADMISSION_INVALID","/admission/keyBinding");
                keyBinding=(KeyBinding)admission.get("keyBinding");
                require(Objects.equals(keyBinding.type(),map(plan.dcm().get("hostKey"),"").get("type")) && keyBinding.validate()!=null,
                        "ADMISSION_INVALID","/admission/keyBinding");
            }
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
            require(effect ? effectAdapters!=null && effectAdapters.size()<=8 : adapters!=null && adapters.size()<=8,"ADMISSION_INVALID","/admission/adapters");
            Map<String,Object> capabilities=map(plan.dcm().get("capabilities"),"");
            Map<String,Adapter> selected=new LinkedHashMap<>();
            for (var item:adapters.entrySet()) {
                Adapter adapter=item.getValue();
                require(capabilities.containsKey(item.getKey()) && adapter!=null,"ADMISSION_INVALID","/admission/adapters");
                require(Objects.equals(adapter.version(),map(capabilities.get(item.getKey()),"").get("version"))
                        && Objects.equals(adapter.mode(),admission.get("mode")) && adapter.invoke()!=null,"ADMISSION_INVALID","/admission/adapters");
                selected.put(item.getKey(),adapter);
            }
            Map<String,EffectAdapter> selectedEffects=new LinkedHashMap<>();
            if (effect) for (var item:effectAdapters.entrySet()) {
                EffectAdapter adapter=item.getValue();
                require(capabilities.containsKey(item.getKey()) && adapter!=null,"ADMISSION_INVALID","/admission/adapters");
                require(Objects.equals(adapter.version(),map(capabilities.get(item.getKey()),"").get("version"))
                        && Objects.equals(adapter.mode(),admission.get("mode")) && Objects.equals(adapter.keyType(),keyBinding.type())
                        && adapter.invoke()!=null,"ADMISSION_INVALID","/admission/adapters");
                selectedEffects.put(item.getKey(),adapter);
            }
            Map<String,List<String>> hostFields=new LinkedHashMap<>();
            for (String id:plan.calls().keySet()) {
                require(effect ? selectedEffects.containsKey(id) : selected.containsKey(id),"ADMISSION_INVALID","/admission/adapters");
                for (var parameter:map(map(capabilities.get(id),"").get("parameters"),"").entrySet()) {
                    Map<String,Object> definition=map(parameter.getValue(),"");
                    if ("host".equals(definition.get("binding"))) hostFields.computeIfAbsent(parameter.getKey(),ignored->new ArrayList<>()).add((String)definition.get("type"));
                }
            }
            return new Preparation(new Prepared(plan,cel,selected,grants,limits,hostFields,selectedEffects,effectGrant,keyBinding),null);
        } catch (RuntimeException failure) {
            String code=failure instanceof RuntimeFault fault?fault.code:"ARTIFACT_INVALID";
            String path=failure instanceof RuntimeFault fault?fault.path:"/artifact";
            if (code.equals("ARTIFACT_INVALID") && path.startsWith("/admission")) code="ADMISSION_INVALID";
            return new Preparation(null,new Diagnostic(code,path,"The selected artifact or host configuration is invalid."));
        }
    }

    public static CompletionStage<Outcome> invoke(Prepared prepared, Object businessInput, Invocation invocation) {
        Run run=new Run(prepared,false);
        try {
            require(prepared!=null && prepared.boundModel==null && !prepared.plan.effect(),"HOST_CONTEXT_INVALID","");
            return run.start(businessInput,invocation,null).handle((outcome,failure)->failure==null?outcome:run.failed(failure));
        }
        catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    public static CompletionStage<Outcome> invokeEffect(Prepared prepared, Object businessInput, EffectInvocation invocation) {
        Run run=new Run(prepared,true);
        try {
            require(prepared!=null && prepared.boundModel==null && prepared.plan.effect() && invocation!=null,"HOST_CONTEXT_INVALID","");
            var common=new Invocation(invocation.bindings(),invocation.deadline(),invocation.authorize(),invocation.grants());
            return run.start(businessInput,common,invocation).handle((outcome,failure)->failure==null?outcome:run.failed(failure));
        } catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    public static CompletionStage<Outcome> invokeProgram(Prepared prepared, Object businessInput, Invocation2 invocation) {
        Run run=new Run(prepared,false);
        try {
            require(prepared!=null && prepared.boundModel!=null && !prepared.plan.effect() && invocation!=null,"HOST_CONTEXT_INVALID","");
            Invocation common=context2(prepared,invocation.scope(),invocation.bindings(),invocation.deadline(),invocation.authorize(),invocation.capabilityPolicy());
            return run.start(businessInput,common,null).handle((outcome,failure)->failure==null?outcome:run.failed(failure));
        } catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    public static CompletionStage<Outcome> invokeEffectProgram(Prepared prepared, Object businessInput, EffectInvocation2 invocation) {
        Run run=new Run(prepared,true);
        try {
            require(prepared!=null && prepared.boundModel!=null && !"keir-effect/4".equals(prepared.boundModel.profile) && prepared.plan.effect() && invocation!=null,"HOST_CONTEXT_INVALID","");
            Invocation common=context2(prepared,invocation.scope(),invocation.bindings(),invocation.deadline(),invocation.authorize(),invocation.capabilityPolicy());
            require(invocation.effectGrant()!=null,"HOST_CONTEXT_INVALID","");
            long grant=invocation.effectGrant().isPresent()?invocation.effectGrant().getAsLong():prepared.effectGrant;
            require(grant>=0 && grant<=prepared.effectGrant,"HOST_CONTEXT_INVALID","");
            Map<String,Object> event;
            try { event=contextFields2(invocation.event(),Set.of("id","source")); }
            catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
            EffectInvocation effect=new EffectInvocation(common.bindings(),common.deadline(),common.authorize(),common.grants(),grant,invocation.requestKey(),event);
            return run.start(businessInput,common,effect).handle((outcome,failure)->failure==null?outcome:run.failed(failure));
        } catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    public static CompletionStage<Outcome> invokeBoundEffectProgram(Prepared prepared, Object businessInput, BoundEffectInvocation invocation) {
        Run run=new Run(prepared,true);
        try {
            require(prepared!=null && prepared.boundModel!=null && "keir-effect/4".equals(prepared.boundModel.profile) && invocation!=null && invocation.deadline()!=null,
                    "HOST_CONTEXT_INVALID","");
            Deadline deadline=new Deadline(invocation.deadline().clock(),invocation.deadline().expiresAtMs());
            Map<String,Object> bindings;
            try { bindings=contextFields2(invocation.bindings(),prepared.hostFields.keySet()); }
            catch (RuntimeException failure) { throw new RuntimeFault("HOST_BINDING_INVALID","/hostBindings"); }
            Map<String,Object> event;
            try { event=contextFields2(invocation.event(),Set.of("id","source")); }
            catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
            BoundEffectInvocation fresh=new BoundEffectInvocation(bindings,deadline,invocation.requestKey(),event);
            Invocation common=new Invocation(bindings,deadline,null,prepared.grants);
            return run.start(businessInput,common,null,fresh).handle((outcome,failure)->failure==null?outcome:run.failed(failure));
        } catch (RuntimeException failure) { return CompletableFuture.completedFuture(run.failed(failure)); }
    }

    private static Invocation context2(Prepared prepared,String scope,Map<String,Object> bindings,Deadline deadline,
                                       Authorizer authorize,Optional<Map<String,Object>> policy) {
        require(Objects.equals(prepared.boundModel.scope,scope) && policy!=null && deadline!=null && authorize!=null,"HOST_CONTEXT_INVALID","");
        CapabilityPolicy.State narrowed;
        try { narrowed=CapabilityPolicy.narrow(prepared.policy,policy.isPresent()?policy.get():Map.of(),"","HOST_CONTEXT_INVALID"); }
        catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
        Map<String,Object> copied;
        try { copied=contextFields2(bindings,prepared.hostFields.keySet()); }
        catch (RuntimeException failure) { throw new RuntimeFault("HOST_BINDING_INVALID","/hostBindings"); }
        return new Invocation(copied,deadline,authorize,narrowed.ceilings());
    }

    private static Map<String,Object> contextFields2(Map<String,Object> value,Set<String> fields) {
        require(value!=null && value.size()==fields.size(),"HOST_CONTEXT_INVALID","");
        shape(value,fields,"");
        // Validated model fields are scalar; typed preflight rejects containers without traversing them.
        Map<String,Object> copied=new LinkedHashMap<>();
        for(String field:fields) copied.put(field,value.get(field));
        return Collections.unmodifiableMap(copied);
    }

    private static final class Run {
        private final Prepared prepared;
        private final boolean effect;
        private final List<Map<String,Object>> trace=new ArrayList<>();
        private final long traceLimit;
        private final Map<String,Object> state=new LinkedHashMap<>(),bindings=new LinkedHashMap<>();
        private final Map<String,Long> grants=new LinkedHashMap<>();
        private final Map<String,Integer> counts=new HashMap<>();
        private InvocationClock clock;
        private Authorizer authorizer;
        private int totalCalls;
        private long effectGrant;
        private HostContext hostContext;
        private BoundEffectContext boundEffectContext;
        private Map<String,Object> receipt;
        private Run(Prepared prepared,boolean effect) { this.prepared=prepared; this.effect=effect; traceLimit=prepared==null?Profile.TRACE_EVENTS:prepared.limits.get("traceEvents"); emit("INVOCATION_START",Map.of(),""); }
        private void emit(String event,Map<String,Object> attributes,String path) {
            require(trace.size()<traceLimit-1,"BUDGET_EXCEEDED",path);
            Map<String,Object> item=new LinkedHashMap<>(); item.put("sequence",trace.size()); item.put("event",event); item.putAll(attributes); trace.add(item);
        }
        private Outcome terminate(String classification,Map<String,Object> error,Object result) {
            if (receipt!=null && "UNKNOWN".equals(receipt.get("state"))) classification="UNCERTAIN";
            Map<String,Object> end=new LinkedHashMap<>(Map.of("sequence",trace.size(),"event","TERMINATED"));
            if (error!=null) end.put("code",error.get("code")); trace.add(end);
            Map<String,Object> outcome=new LinkedHashMap<>(); outcome.put("classification",classification);
            if (error!=null) outcome.put("error",error);
            if (result!=null || error==null) outcome.put("result",result);
            if (effect) outcome.put("effects",receipt==null?List.of():List.of(receipt));
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
        private CompletionStage<Outcome> start(Object input,Invocation invocation,EffectInvocation effect) {
            return start(input,invocation,effect,null);
        }
        private CompletionStage<Outcome> start(Object input,Invocation invocation,EffectInvocation effect,BoundEffectInvocation bound) {
            require(prepared!=null && invocation!=null && (bound!=null || invocation.authorize()!=null) && invocation.deadline()!=null,"HOST_CONTEXT_INVALID","");
            clock=new InvocationClock(invocation.deadline().clock(),invocation.deadline().expiresAtMs()); clock.remaining("");
            current("");
            state.put("input",Artifact.value(prepared.plan.dcm(),(String)prepared.plan.entrypoint().get("input"),input,"/input","INPUT_INVALID"));
            try {
                shape(invocation.bindings(),prepared.hostFields.keySet(),"/hostBindings");
                for (var field:prepared.hostFields.entrySet()) for (String type:field.getValue())
                    bindings.put(field.getKey(),Artifact.value(prepared.plan.dcm(),type,invocation.bindings().get(field.getKey()),"/hostBindings","HOST_BINDING_INVALID"));
            } catch (RuntimeException failure) { throw new RuntimeFault(failure instanceof RuntimeFault f && f.code.equals("BUDGET_EXCEEDED")?f.code:"HOST_BINDING_INVALID","/hostBindings"); }
            try {
                for (var grant:bounded(invocation.grants(),"",8).entrySet()) {
                    require(prepared.grants.containsKey(grant.getKey()),"HOST_CONTEXT_INVALID","");
                    grants.put(grant.getKey(),number(grant.getValue(),prepared.boundModel==null?1:0,prepared.grants.get(grant.getKey()),""));
                }
            } catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
            if (effect!=null || bound!=null) {
                try { effectGrant=bound!=null?prepared.effectGrant:number(effect.effectGrant(),0,prepared.effectGrant,""); }
                catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
                Object requestKey=bound!=null?bound.requestKey():effect.requestKey();
                Map<String,Object> suppliedEvent=bound!=null?bound.event():effect.event();
                require(requestKey!=null,"HOST_CONTEXT_INVALID","");
                if (bound==null) {
                    clock.remaining("");
                    Object accepted;
                    try { accepted=prepared.keyBinding.validate().validate(requestKey); }
                    catch (Throwable failure) { accepted=null; }
                    clock.remaining("");
                    require(Boolean.TRUE.equals(accepted),"HOST_CONTEXT_INVALID","");
                }
                try { shape(suppliedEvent,Set.of("id","source"),""); }
                catch (RuntimeException failure) { throw new RuntimeFault("HOST_CONTEXT_INVALID",""); }
                Map<String,Object> eventTypes=map(prepared.plan.entrypoint().get("event"),"");
                Map<String,Object> event=new LinkedHashMap<>();
                event.put("id",Artifact.value(prepared.plan.dcm(),(String)eventTypes.get("idType"),suppliedEvent.get("id"),"","HOST_CONTEXT_INVALID"));
                event.put("source",Artifact.value(prepared.plan.dcm(),(String)eventTypes.get("sourceType"),suppliedEvent.get("source"),"","HOST_CONTEXT_INVALID"));
                if (bound!=null) boundEffectContext=new BoundEffectContext(requestKey,event,bound.deadline());
                else hostContext=new HostContext(requestKey,event);
            }
            authorizer=invocation.authorize();
            return bound!=null?step(0):authorized(Map.of("entrypoint",prepared.plan.document().get("entrypoint")),"").thenCompose(ignored->step(0));
        }
        private CompletionStage<Object> authorized(Map<String,Object> request,String path) {
            current(path);
            clock.remaining(path);
            if (boundEffectContext!=null) return CompletableFuture.completedFuture(null);
            return clock.waitFor(budget->authorizer.authorize(snapshot(request)),path,"AUTHORITY_CHECK_FAILED",null).thenApply(allowed->{
                require(allowed instanceof Boolean,"AUTHORITY_CHECK_FAILED",path);
                require(Boolean.TRUE.equals(allowed),"AUTHORITY_DENIED",path); return allowed;
            });
        }
        private void current(String path) {
            if (prepared.boundModel==null || prepared.boundModel.qualification==null) return;
            clock.remaining(path);
            boolean accepted=isCurrent(prepared.boundModel);
            clock.remaining(path);
            require(accepted,"AUTHORITY_DENIED",path);
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
            if (step.containsKey("when")) {
                boolean taken=Boolean.TRUE.equals(state.get(step.get("when")));
                emit("GUARD_EVALUATED",Map.of("step",id,"taken",taken),path);
                if (!taken) { emit("STEP_SKIPPED",Map.of("step",id),path); return step(index+1); }
            }
            String capability=(String)step.get("capability");
            Map<String,Object> cap=map(map(prepared.plan.dcm().get("capabilities"),"").get(capability),"");
            int count=counts.merge(capability,1,Integer::sum); totalCalls++;
            require(totalCalls<=prepared.limits.get("calls"),"BUDGET_EXCEEDED",path);
            if (prepared.boundModel!=null && "WRITE".equals(cap.get("effect"))) require(effectGrant>0,"AUTHORITY_DENIED",path);
            require(count<=grants.getOrDefault(capability,0L),"AUTHORITY_DENIED",path);
            boolean write=prepared.plan.effect() && "WRITE".equals(cap.get("effect"));
            if (write) require(effectGrant>0,"AUTHORITY_DENIED",path);
            Map<String,Object> agent=new LinkedHashMap<>(),protectedArgs=new LinkedHashMap<>();
            for (var parameter:map(cap.get("parameters"),"").entrySet()) {
                String name=parameter.getKey(); Map<String,Object> definition=map(parameter.getValue(),""); String type=(String)definition.get("type");
                if ("host".equals(definition.get("binding"))) protectedArgs.put(name,Artifact.value(prepared.plan.dcm(),type,bindings.get(name),"/hostBindings","HOST_BINDING_INVALID"));
                else agent.put(name,Artifact.value(prepared.plan.dcm(),type,evaluate(map(step.get("args"),"").get(name),path),path,"INPUT_INVALID"));
            }
            return authorized(Map.of("entrypoint",prepared.plan.document().get("entrypoint"),"capability",capability,"version",step.get("version")),path)
                    .thenCompose(ignored->{
                        current(path);
                        clock.remaining(path);
                        var agentSnapshot=snapshot(agent); var protectedSnapshot=snapshot(protectedArgs);
                        java.util.function.LongFunction<CompletionStage<?>> action=budget->boundEffectContext!=null
                                ? prepared.boundModel.boundEffectAdapters.get(capability).invoke().invoke(agentSnapshot,protectedSnapshot,budget,boundEffectContext)
                                : prepared.plan.effect()
                                ? prepared.effectAdapters.get(capability).invoke().invoke(agentSnapshot,protectedSnapshot,budget,hostContext)
                                : prepared.adapters.get(capability).invoke().invoke(agentSnapshot,protectedSnapshot,budget);
                        Runnable dispatch=()->{
                            require(trace.size()+(write?5:3)<=traceLimit,"BUDGET_EXCEEDED",path);
                            if (write) {
                                receipt=Map.of("step",id,"capability",capability,"state","UNKNOWN");
                                emit("EFFECT_INTENT",Map.of("step",id,"capability",capability),path);
                            }
                            emit("CALL_DISPATCHED",Map.of("step",id,"capability",capability),path);
                        };
                        return write ? clock.waitForEffect(action,path,"ADAPTER_EXCEPTION",dispatch,
                                response->settle(response,path),()->emit("EFFECT_RECORDED",receipt,path))
                                : clock.waitFor(action,path,"ADAPTER_EXCEPTION",dispatch);
                    }).thenCompose(response->{
                        try {
                            Map<String,Object> value=map(response,path);
                            if ("SUCCESS".equals(value.get("kind"))) {
                                shape(value,write?Set.of("kind","value","effect"):Set.of("kind","value"),path);
                                if (write) require("CONFIRMED".equals(value.get("effect")),"ADAPTER_INVALID",path);
                                Object result=Artifact.value(prepared.plan.dcm(),(String)cap.get("result"),value.get("value"),path,"ADAPTER_INVALID");
                                if (!step.containsKey("when")) state.put(id,result);
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
