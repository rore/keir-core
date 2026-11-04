package dev.keir.runtime;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import com.sun.net.httpserver.HttpServer;
import static dev.keir.runtime.RuntimeData.*;

/** Fixed host-bound corpus transport; native permission checks belong to the fixture host. */
public final class BoundEffectScenarioDriver {
    private static final Set<String> CASES=Set.of("simulation-main","simulation-zero","simulation-after-write-read","simulation-no-call",
            "real-main","real-zero","real-after-write-read","real-no-call","missing-implementation","mixed-mode","unsupported-compiler",
            "empty-catalog","denied-entry","invalid-key","denied-write","denied-pending-native","denied-after-confirmed","expired-entry",
            "false-guard","definite-mismatch","transient-none","unknown-commit","lost-response","malformed-confirmed-value","confirmed-response-expired");
    private BoundEffectScenarioDriver() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }

    static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=0) {err.println("The host-bound scenario driver takes no arguments.");return 2;}
        try {
            Method consume=Class.forName("dev.keir.examples.EffectConsumer").getDeclaredMethod("consume",Object.class);consume.setAccessible(true);
            List<Object> rows=list(parseJson(resource("admission-cases.json",1_048_576),1_048_576),"");
            Set<String> seen=new HashSet<>();List<Object> results=new ArrayList<>();
            for(Object value:rows) {
                Map<String,Object> row=map(value,"");shape(row,Set.of("id","expect"),"");
                String id=text(row.get("id"),"",true,128);check(CASES.contains(id) && seen.add(id),"Unknown or duplicate host-bound case");
                Map<String,Object> result=execute(id,consume);
                check(Arrays.equals(canonical(result.get("facts")),canonical(row.get("expect"))),"Host-bound facts differ for "+id);
                results.add(result);
            }
            check(seen.equals(CASES),"Incomplete host-bound case corpus");verifyNativeTransports();
            out.writeBytes(canonical(results));out.println();return 0;
        } catch(Exception|AssertionError failure) {err.println("The host-bound scenario batch could not be executed: "+failure.getMessage());return 1;}
    }

    private static byte[] resource(String name,int max) throws IOException {
        try(var stream=BoundEffectScenarioDriver.class.getResourceAsStream("/generic-hooks/effect4/"+name)) {
            if(stream==null)throw new IOException("Missing host-bound fixture");
            byte[] bytes=stream.readNBytes(max+1);check(bytes.length<=max,"Host-bound fixture exceeds bound");return bytes;
        }
    }
    private static String pin(String name) throws IOException {
        String value=new String(resource(name,74),StandardCharsets.UTF_8).trim();
        check(value.matches("sha256:[0-9a-f]{64}"),"Invalid independent host-bound pin");return value;
    }
    private static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") private static Map<String,Object> snapshot(Map<String,Object> value) {return (Map<String,Object>)immutable(value);}

    private static final class Fixture {
        final String id,program;
        final byte[] dcmBytes,bytes;
        final Map<String,Object> dcm;
        final Object key=new Object() {@Override public String toString() {throw new AssertionError("Native key cannot be converted");}};
        final Map<String,KeirRuntime.BoundEffectAdapter> implementations=new LinkedHashMap<>();
        final List<Map<String,Object>> calls=new ArrayList<>();
        final List<String> nativeActions=new ArrayList<>();
        volatile boolean allowed=true;
        volatile long now;

        Fixture(String id) throws IOException {
            this.id=id;program=id.endsWith("-no-call") || id.equals("empty-catalog")?"no-call":id.endsWith("-zero")?"zero":
                    id.contains("after-write-read") || id.equals("denied-after-confirmed")?"after-write-read":"main";
            String name=program.equals("main")?"artifact":"artifact-"+program;
            bytes=resource(name+".json",Profile.ARTIFACT_BYTES);dcmBytes=resource("dcm.json",Profile.DCM_BYTES);
            check(digest(bytes).equals(pin(name+".sha256")) && digest(dcmBytes).equals(pin("dcm.sha256")),"Independent host-bound pin differs");
            dcm=map(parseJson(dcmBytes,Profile.DCM_BYTES),"");String mode=id.startsWith("simulation-")?"simulation":"real";
            for(var registration:map(dcm.get("capabilities"),"").entrySet()) {
                String capability=registration.getKey();Map<String,Object> cap=map(registration.getValue(),"");
                String version=(String)cap.get("version"),keyType=(String)map(dcm.get("hostKey"),"").get("type");
                implementations.put(capability,new KeirRuntime.BoundEffectAdapter(version,mode,keyType,(args,protectedArgs,remainingMs,context)->{
                    calls.add(Map.of("capability",capability,"version",version,"args",snapshot(args),"protectedArgs",snapshot(protectedArgs)));
                    check(remainingMs==1000-now && remainingMs>0,"Adapter received a stale deadline budget");
                    CompletionStage<Void> beforeNative=id.equals("denied-pending-native") && capability.equals("tasks.create")
                            ? CompletableFuture.runAsync(()->allowed=false):CompletableFuture.completedFuture(null);
                    return beforeNative.thenApply(ignored->{
                        boolean write="WRITE".equals(cap.get("effect"));
                        boolean permitted=allowed && context.requestKey()==key && "user1".equals(protectedArgs.get("actorId")) &&
                                Map.of("id","event1","source","task1").equals(context.event()) && context.deadline().expiresAtMs()==1000 &&
                                context.deadline().clock().getAsLong()<context.deadline().expiresAtMs();
                        if(!permitted)return write?Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED","effect","NONE"):
                                Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED");
                        nativeActions.add(capability);
                        if(!write) {
                            if(id.equals("denied-write"))allowed=false;
                            return Map.of("kind","SUCCESS","value",Map.of("id",args.get("taskId"),"projectId",id.equals("false-guard")?"project2":"project1","status","COMPLETED"));
                        }
                        if(id.equals("denied-after-confirmed"))allowed=false;
                        if(id.equals("confirmed-response-expired"))now=1000;
                        return switch(id) {
                            case "lost-response" -> throw new IllegalStateException("Native response lost");
                            case "definite-mismatch" -> Map.of("kind","BUSINESS_REJECTION","code","KEY_PAYLOAD_MISMATCH","effect","NONE");
                            case "transient-none" -> Map.of("kind","TRANSIENT_FAILURE","effect","NONE");
                            case "unknown-commit" -> Map.of("kind","UNKNOWN_COMMIT","effect","UNKNOWN");
                            case "malformed-confirmed-value" -> Map.of("kind","SUCCESS","value",Map.of(),"effect","CONFIRMED");
                            default -> Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED");
                        };
                    });
                }));
            }
        }
        KeirRuntime.BoundEffectInvocation invocation() {
            Object original=id.equals("invalid-key") || id.equals("real-no-call")?new Object():key;
            return new KeirRuntime.BoundEffectInvocation(program.equals("no-call")?Map.of():Map.of("actorId","user1"),
                    new KeirRuntime.Deadline(()->now,1000),original,Map.of("id","event1","source","task1"));
        }
    }

    private static Map<String,Object> rejected(String id,String phase,KeirRuntime.Diagnostic diagnostic,int calls) {
        return Map.of("id",id,"facts",Map.of("phase",phase,"code",diagnostic.code(),"path",diagnostic.path(),"calls",calls));
    }
    private static Map<String,Object> execute(String id,Method consume) throws Exception {
        Fixture fixture=new Fixture(id);byte[] dcmBytes=fixture.dcmBytes,bytes=fixture.bytes;
        Map<String,KeirRuntime.BoundEffectAdapter> implementations=fixture.implementations;
        if(id.equals("missing-implementation"))implementations.remove("tasks.create");
        if(id.equals("mixed-mode")) {
            var adapter=implementations.get("tasks.get");implementations.put("tasks.get",new KeirRuntime.BoundEffectAdapter(adapter.version(),"simulation",adapter.keyType(),adapter.invoke()));
        }
        if(id.equals("empty-catalog")) {
            fixture.dcm.put("capabilities",Map.of());dcmBytes=canonical(fixture.dcm);implementations=Map.of();
            var artifact=map(parseCanonical(bytes),"");artifact.put("modelRef",Artifact.modelRef(fixture.dcm));bytes=canonical(artifact);
        }
        var binding=KeirRuntime.bindModel(dcmBytes,implementations);
        if(!binding.succeeded())return rejected(id,"binding",binding.diagnostic(),fixture.calls.size());
        if(id.equals("unsupported-compiler")) {var artifact=map(parseCanonical(bytes),"");artifact.put("compiler","unsupported");bytes=canonical(artifact);}
        var preparation=KeirRuntime.prepareProgram(bytes,binding.boundModel());
        if(!preparation.succeeded())return rejected(id,"preparation",preparation.diagnostic(),fixture.calls.size());
        if(id.equals("denied-entry"))fixture.allowed=false;
        if(id.equals("expired-entry"))fixture.now=1000;
        var outcome=KeirRuntime.invokeBoundEffectProgram(preparation.prepared(),Map.of("taskId","task1"),fixture.invocation()).toCompletableFuture().get(3,TimeUnit.SECONDS);
        Map<String,Object> consumption=map(consume.invoke(null,outcome.toMap()),"");
        var facts=new LinkedHashMap<String,Object>();facts.put("phase","invocation");facts.put("classification",outcome.classification());
        facts.put("calls",fixture.calls.stream().map(call->call.get("capability")).toList());facts.put("effects",outcome.effects().stream().map(effect->effect.get("state")).toList());
        facts.put("nativeActions",fixture.nativeActions);facts.put("reported",consumption.get("status"));
        if(outcome.result()!=null)facts.put("result",outcome.result());if(outcome.error()!=null)facts.put("error",outcome.error());
        return Map.of("id",id,"facts",facts,"outcome",outcome.toMap(),"calls",fixture.calls,"consumption",consumption);
    }

    static void verifyNativeTransports() throws Exception {
        for(String transport:List.of("local","loopback")) for(String scenario:List.of("confirmed","denied","wrong-key","lost-response")) {
            Fixture fixture=new Fixture("real-main");var attempts=new AtomicInteger();var commits=new AtomicInteger();var current=new AtomicBoolean(true);
            Function<Map<String,Object>,Map<String,Object>> nativeCreate=body->{
                attempts.incrementAndGet();shape(body,Set.of("actor","event","source","token","expiresAtMs","projectId","title"),"");
                boolean permitted=current.get() && "native-token".equals(body.get("token")) && "user1".equals(body.get("actor")) &&
                        "event1".equals(body.get("event")) && "task1".equals(body.get("source")) && "project1".equals(body.get("projectId")) &&
                        "Follow up".equals(body.get("title")) && number(body.get("expiresAtMs"),0,Profile.MAX_INTEGER,"")>fixture.now;
                if(!permitted)return Map.of("kind","BUSINESS_REJECTION","code","ACCESS_DENIED","effect","NONE");
                commits.incrementAndGet();
                if(scenario.equals("lost-response"))throw new IllegalStateException("Response lost after native commit");
                return Map.of("kind","SUCCESS","value",Map.of("id","task2"),"effect","CONFIRMED");
            };
            HttpServer server=null;HttpClient client=transport.equals("loopback")?HttpClient.newHttpClient():null;
            try {
                if(client!=null) {
                    server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
                    server.createContext("/tasks",exchange->{
                        try {
                            check(exchange.getRequestMethod().equals("POST"),"Native service requires POST");
                            byte[] bytes=exchange.getRequestBody().readNBytes(1025);check(bytes.length<=1024,"Native request exceeds bound");
                            byte[] response=canonical(nativeCreate.apply(map(parseJson(bytes,1024),"")));
                            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
                            exchange.getResponseBody().write(response);
                        } catch(RuntimeException|AssertionError failure) {
                            // A lost response must not become definite no-effect evidence.
                        } finally {exchange.close();}
                    });
                    server.start();
                }
                URI endpoint=server==null?null:URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/tasks");
                var original=fixture.implementations.get("tasks.create");
                fixture.implementations.put("tasks.create",new KeirRuntime.BoundEffectAdapter(original.version(),original.mode(),original.keyType(),(args,protectedArgs,remainingMs,context)->{
                    check(context.requestKey()==fixture.key,"Original native key identity changed");check(remainingMs>0,"Native dispatch budget expired");
                    Map<String,Object> body=Map.of("actor",protectedArgs.get("actorId"),"event",context.event().get("id"),"source",context.event().get("source"),
                            "token",scenario.equals("wrong-key")?"untrusted":"native-token","expiresAtMs",context.deadline().expiresAtMs(),
                            "projectId",args.get("projectId"),"title",args.get("title"));
                    if(client==null)return CompletableFuture.supplyAsync(()->nativeCreate.apply(body));
                    var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(remainingMs)).header("Content-Type","application/json")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(canonical(body))).build();
                    return client.sendAsync(request,HttpResponse.BodyHandlers.ofByteArray()).thenApply(response->{
                        check(response.statusCode()==200 && response.body().length<=1024,"Invalid native response");
                        return map(parseJson(response.body(),1024),"");
                    });
                }));
                var binding=KeirRuntime.bindModel(fixture.dcmBytes,fixture.implementations);check(binding.succeeded(),"Native binding failed");
                var preparation=KeirRuntime.prepareProgram(fixture.bytes,binding.boundModel());check(preparation.succeeded(),"Native preparation failed");
                check(attempts.get()==0,"Initialization performed a native operation");current.set(!scenario.equals("denied"));
                var outcome=KeirRuntime.invokeBoundEffectProgram(preparation.prepared(),Map.of("taskId","task1"),fixture.invocation()).toCompletableFuture().get(3,TimeUnit.SECONDS);
                String label=transport+"/"+scenario;
                check(attempts.get()==1,"Native attempts differ for "+label+": "+attempts.get());
                check(commits.get()==(scenario.equals("confirmed") || scenario.equals("lost-response")?1:0),"Native commits differ for "+label);
                check(outcome.effects().size()==1 && outcome.effects().getFirst().get("state").equals(scenario.equals("confirmed")?"CONFIRMED":scenario.equals("lost-response")?"UNKNOWN":"NONE"),"Native receipt differs for "+label);
                check(outcome.classification().equals(scenario.equals("confirmed")?"COMPLETED":scenario.equals("lost-response")?"UNCERTAIN":"BUSINESS_REJECTION"),"Native outcome differs for "+label);
            } finally {
                if(server!=null)server.stop(0);
                if(client!=null)client.close();
            }
        }
    }
}
