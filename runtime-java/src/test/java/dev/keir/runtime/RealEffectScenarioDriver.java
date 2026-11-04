package dev.keir.runtime;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static dev.keir.runtime.RuntimeData.*;

/** Fixed deterministic admission transport; it does not qualify a native deployment. */
public final class RealEffectScenarioDriver {
    private static final Set<String> CASES=Set.of("simulation-main","real-main","simulation-zero","real-zero",
            "simulation-after-write-read","real-after-write-read","missing-qualification","null-qualification",
            "simulation-with-qualification","wrong-build","stale-revision","bad-catalog","oversized-evidence",
            "revoked-preparation","digest-before-qualification","revoked-entry","revoked-pending-authorization",
            "revoked-after-read","revoked-after-confirmed","qualification-expired","definite-mismatch",
            "transient-none","unknown-commit","malformed-confirmed-value","false-guard","zero-effect-ceiling",
            "duplicate-confirmed","confirmed-response-expired");
    private static final Map<String,Object> HOST_APPROVAL=Map.of("id","reviewed-host","revision",1L,
            "hostBuild","sha256:"+"1".repeat(64),"reportDigest","sha256:"+"2".repeat(64));
    private RealEffectScenarioDriver() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }

    static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=0) { err.println("The real-effect scenario driver takes no arguments."); return 2; }
        try {
            Method consume=Class.forName("dev.keir.examples.EffectConsumer").getDeclaredMethod("consume",Object.class);
            consume.setAccessible(true);
            List<Object> rows=list(parseJson(resource("admission-cases.json",1_048_576),1_048_576),"");
            Set<String> seen=new HashSet<>();List<Object> results=new ArrayList<>();
            for(Object value:rows) {
                Map<String,Object> row=map(value,"");shape(row,Set.of("id","expect"),"");
                String id=text(row.get("id"),"",true,128);check(CASES.contains(id) && seen.add(id),"Unknown or duplicate admission case");
                Map<String,Object> result=execute(id,consume);
                same(result.get("facts"),row.get("expect"),"Admission facts differ for "+id);results.add(result);
            }
            check(seen.equals(CASES),"Incomplete admission case corpus");
            out.writeBytes(canonical(results));out.println();return 0;
        } catch(Exception|AssertionError failure) {
            err.println("The real-effect scenario batch could not be executed.");return 1;
        }
    }

    private static byte[] resource(String name,int max) throws IOException {
        try(var stream=RealEffectScenarioDriver.class.getResourceAsStream("/generic-hooks/effect3/"+name)) {
            if(stream==null) throw new IOException("Missing admission fixture");
            byte[] bytes=stream.readNBytes(max+1);check(bytes.length<=max,"Admission fixture exceeds bound");return bytes;
        }
    }
    private static String pin(String name) throws IOException {
        String value=new String(resource(name,74),StandardCharsets.UTF_8).trim();
        check(value.matches("sha256:[0-9a-f]{64}"),"Invalid independent admission pin");return value;
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static void same(Object actual,Object expected,String message) { check(Arrays.equals(canonical(actual),canonical(expected)),message); }
    private static CompletableFuture<Object> done(Object value) { return CompletableFuture.completedFuture(value); }
    @SuppressWarnings("unchecked") private static Map<String,Object> snapshot(Map<String,Object> value) { return (Map<String,Object>)immutable(value); }

    private static final class Fixture {
        final String mode,id,keyType,artifactPin,dcmPin;
        final UUID key=UUID.fromString("00000000-0000-4000-8000-000000000001");
        final byte[] bytes,dcmBytes;
        final Map<String,Object> artifact,dcm,approval=new LinkedHashMap<>(),admission=new LinkedHashMap<>();
        final Map<String,KeirRuntime.EffectAdapter> invokers=new LinkedHashMap<>();
        final List<Map<String,Object>> calls=new ArrayList<>();
        volatile boolean current=true,expire;
        volatile long now;
        Map<String,Object> writeResponse=Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED");

        Fixture(String id) throws IOException {
            this.id=id;mode=id.startsWith("simulation-")?"simulation":"real";
            String program=id.endsWith("-zero")?"zero":id.contains("after-write-read") || id.equals("revoked-after-confirmed")?"after-write-read":"main";
            String name=program.equals("main")?"artifact":"artifact-"+program;
            bytes=resource(name+".json",1_048_576);artifactPin=pin(name+".sha256");
            dcmBytes=resource("dcm.json",65_536);dcmPin=pin("dcm.sha256");
            check(digest(bytes).equals(artifactPin) && digest(dcmBytes).equals(dcmPin),"Independent admission pin differs");
            artifact=map(parseCanonical(bytes),"");dcm=map(parseCanonical(dcmBytes),"");keyType=(String)map(dcm.get("hostKey"),"").get("type");
            approval.put("scope","scope1");approval.put("modelRef",snapshot(map(artifact.get("modelRef"),"")));approval.put("profile","keir-effect/3");
            approval.put("environment",KeirRuntime.ENVIRONMENT);approval.put("mode",mode);
            approval.put("keyBinding",new KeirRuntime.KeyBinding(keyType,value->value==key));
            Map<String,Object> capabilities=map(dcm.get("capabilities"),"");
            for(var entry:capabilities.entrySet()) {
                String capability=entry.getKey();Map<String,Object> cap=map(entry.getValue(),"");String version=(String)cap.get("version");
                invokers.put(capability,new KeirRuntime.EffectAdapter(version,mode,keyType,(args,protectedArgs,remainingMs,context)->{
                    check(context.requestKey()==key,"Original native key identity changed");
                    same(context.event(),Map.of("id","event1","source","task1"),"Protected event changed");
                    same(protectedArgs,Map.of("actorId","user1"),"Protected actor changed");
                    check(remainingMs==1000-now && remainingMs>0,"Adapter received a stale deadline budget");
                    calls.add(Map.of("capability",capability,"version",version,"args",snapshot(args),"protectedArgs",snapshot(protectedArgs)));
                    if("READ".equals(cap.get("effect"))) {
                        if(id.equals("revoked-after-read")) current=false;
                        return done(Map.of("kind","SUCCESS","value",Map.of("id",args.get("taskId"),"projectId",id.equals("false-guard")?"project2":"project1","status","COMPLETED")));
                    }
                    if(id.equals("revoked-after-confirmed")) current=false;
                    if(id.equals("confirmed-response-expired")) now=1000;
                    return done(writeResponse);
                }));
            }
            admission.put("expectedArtifactDigest",artifactPin);admission.put("expectedDcmDigest",dcmPin);admission.put("entrypoint",artifact.get("entrypoint"));
            admission.put("consumer",map(map(dcm.get("entrypoints"),"").get(artifact.get("entrypoint")),"").get("consumer"));
            admission.put("compiler",artifact.get("compiler"));admission.put("environment",KeirRuntime.ENVIRONMENT);admission.put("mode",mode);
        }

        KeirRuntime.Qualification qualification() {
            Map<String,Object> evidence=new LinkedHashMap<>(HOST_APPROVAL);evidence.put("contract","keir-real-effect-evidence/1");
            evidence.put("scope",approval.get("scope"));evidence.put("modelRef",approval.get("modelRef"));evidence.put("environment",KeirRuntime.ENVIRONMENT);
            evidence.put("keyType",keyType);List<Object> catalog=new ArrayList<>();Map<String,Object> caps=map(dcm.get("capabilities"),"");
            for(String capability:new TreeSet<>(caps.keySet())) catalog.add(Map.of("id",capability,"version",map(caps.get(capability),"").get("version")));
            evidence.put("capabilities",catalog);
            if(id.equals("wrong-build")) evidence.put("hostBuild","sha256:"+"3".repeat(64));
            if(id.equals("stale-revision")) evidence.put("revision",2L);
            Map<String,Object> selected=snapshot(evidence);
            if(id.equals("bad-catalog")) catalog.removeLast();
            if(id.equals("oversized-evidence")) evidence.put("id","x".repeat(16384));
            return new KeirRuntime.Qualification(evidence,()->{
                if(expire) now=1000;
                return current && HOST_APPROVAL.entrySet().stream().allMatch(field->Objects.equals(selected.get(field.getKey()),field.getValue()));
            });
        }

        KeirRuntime.EffectInvocation2 invocation() {
            return new KeirRuntime.EffectInvocation2("scope1",Map.of("actorId","user1"),new KeirRuntime.Deadline(()->now,1000),request->{
                if(request.containsKey("capability")) {
                    if(id.equals("qualification-expired")) expire=true;
                    if(id.equals("revoked-pending-authorization")) {
                        var result=new CompletableFuture<Object>();
                        Thread.ofVirtual().start(()->{current=false;result.complete(true);});return result;
                    }
                }
                return done(true);
            },Optional.empty(),id.equals("zero-effect-ceiling")?OptionalLong.of(0):OptionalLong.empty(),key,Map.of("id","event1","source","task1"));
        }
    }

    private static Map<String,Object> rejected(String id,String phase,KeirRuntime.Diagnostic diagnostic,int calls) {
        return Map.of("id",id,"facts",Map.of("phase",phase,"code",diagnostic.code(),"path",diagnostic.path(),"calls",calls));
    }

    private static Map<String,Object> execute(String id,Method consume) throws Exception {
        Fixture f=new Fixture(id);
        KeirRuntime.ModelBinding binding;
        if(id.equals("missing-qualification") || f.mode.equals("simulation") && !id.equals("simulation-with-qualification"))
            binding=KeirRuntime.bindModel(f.dcmBytes,f.approval,f.invokers);
        else binding=KeirRuntime.bindModel(f.dcmBytes,f.approval,f.invokers,id.equals("null-qualification")?null:f.qualification());
        if(!binding.succeeded()) return rejected(id,"binding",binding.diagnostic(),f.calls.size());
        if(id.equals("revoked-preparation") || id.equals("digest-before-qualification")) f.current=false;
        if(id.equals("digest-before-qualification")) f.admission.put("expectedArtifactDigest","sha256:"+"0".repeat(64));
        var preparation=KeirRuntime.prepareProgram(f.bytes,binding.boundModel(),f.admission);
        if(!preparation.succeeded()) return rejected(id,"preparation",preparation.diagnostic(),f.calls.size());
        if(id.equals("revoked-entry")) f.current=false;
        if(id.equals("definite-mismatch")) f.writeResponse=Map.of("kind","BUSINESS_REJECTION","code","KEY_PAYLOAD_MISMATCH","effect","NONE");
        if(id.equals("transient-none")) f.writeResponse=Map.of("kind","TRANSIENT_FAILURE","effect","NONE");
        if(id.equals("unknown-commit")) f.writeResponse=Map.of("kind","UNKNOWN_COMMIT","effect","UNKNOWN");
        if(id.equals("malformed-confirmed-value")) f.writeResponse=Map.of("kind","SUCCESS","value",Map.of(),"effect","CONFIRMED");
        var outcome=KeirRuntime.invokeEffectProgram(preparation.prepared(),Map.of("taskId","task1"),f.invocation()).toCompletableFuture().get(3,TimeUnit.SECONDS);
        Map<String,Object> consumption=map(consume.invoke(null,outcome.toMap()),"");
        var facts=new LinkedHashMap<String,Object>();facts.put("phase","invocation");facts.put("classification",outcome.classification());
        facts.put("calls",f.calls.stream().map(call->call.get("capability")).toList());facts.put("effects",outcome.effects().stream().map(effect->effect.get("state")).toList());
        facts.put("reported",consumption.get("status"));if(outcome.result()!=null) facts.put("result",outcome.result());if(outcome.error()!=null) facts.put("error",outcome.error());
        return Map.of("id",id,"facts",facts,"outcome",outcome.toMap(),"calls",f.calls,"consumption",consumption);
    }
}
