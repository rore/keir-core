package dev.keir.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiCallConformanceTest {
    private static final Map<String,Object> INVOCATION_REQUEST=Map.of("entrypoint","ClassifyTask");
    private static final Map<String,Object> CALL_REQUEST=Map.of("entrypoint","ClassifyTask","capability","tasks.get","version","1");
    private static final List<Map<String,Object>> FIRST_READ_TRACE=List.of(
        Map.of("sequence",0,"event","INVOCATION_START"),
        Map.of("sequence",1,"event","STEP_START","step","first"),
        Map.of("sequence",2,"event","CALL_DISPATCHED","step","first","capability","tasks.get"),
        Map.of("sequence",3,"event","CALL_SUCCEEDED","step","first","capability","tasks.get"));

    @Test void pinnedTwoCallArtifactEnforcesFreshGrantsAndResetsAcrossPreparedReuse() throws Exception {
        byte[] bytes=resource("artifact.json");
        String artifactPin=new String(resource("artifact.sha256"),StandardCharsets.UTF_8).trim();
        String modelPin=new String(resource("dcm.sha256"),StandardCharsets.UTF_8).trim();
        assertEquals("sha256:aefc5bee01b57762e195d3d24c25be5b418f6d6adcb2796a39dfba5bd68a14ac",artifactPin);
        assertEquals("sha256:02f1586fb84d4cd168948d63f2989224bfb9b0298b700ae72f92a790d146947d",modelPin);
        var artifact=RuntimeData.map(RuntimeData.parseCanonical(bytes),"/artifact");
        var model=RuntimeData.map(RuntimeData.parseJson(resource("dcm.json"),65_536),"/dcm");
        assertEquals(artifactPin,sha256(bytes));
        assertEquals(modelPin,sha256(RuntimeData.canonical(model)));
        assertEquals(modelPin,artifact.get("dcmDigest"));
        assertEquals(model,artifact.get("dcm"));
        var entry=RuntimeData.map(RuntimeData.map(model.get("entrypoints"),"/dcm").get("ClassifyTask"),"/dcm");
        assertEquals(Map.of("tasks.get",2L),entry.get("grants"));
        assertEquals(List.of(
            Map.of("id","first","op","CALL","capability","tasks.get"),
            Map.of("id","second","op","CALL","capability","tasks.get")),
            RuntimeData.list(artifact.get("steps"),"/artifact").stream().map(value->{
                var step=RuntimeData.map(value,"/artifact");
                return Map.of("id",step.get("id"),"op",step.get("op"),"capability",step.get("capability"));
            }).toList());

        Queue<Map<String,Object>> calls=new ConcurrentLinkedQueue<>(),requests=new ConcurrentLinkedQueue<>(),responses=new ConcurrentLinkedQueue<>();
        var admission=Map.<String,Object>of(
            "expectedArtifactDigest",artifactPin,"expectedDcmDigest",modelPin,"entrypoint","ClassifyTask",
            "consumer",Map.of("id","sample.classification","version","1","kind","DISPLAY"),
            "compiler","keir-compiler-java/0.1.0-SNAPSHOT",
            "environment",Map.of("celJava","0.14.0","celNode","0.6.1","protobufJava","4.35.1","protobufNode","2.6.2"),
            "mode","simulation","grants",Map.of("tasks.get",2L));
        var selected=KeirRuntime.prepare(bytes,admission,Map.of("tasks.get",new KeirRuntime.Adapter("1","simulation",(agent,protectedArgs,remaining)->{
            calls.add(Map.of("agentArgs",Map.copyOf(agent),"protectedArgs",Map.copyOf(protectedArgs),"remainingMs",remaining));
            return CompletableFuture.completedFuture(responses.poll());
        })));
        assertTrue(selected.succeeded(),()->String.valueOf(selected.diagnostic()));
        assertTrue(calls.isEmpty()); assertTrue(requests.isEmpty());
        var prepared=selected.prepared();

        responses.add(Map.of("kind","SUCCESS","value",Map.of("id","task1","status","COMPLETED")));
        var deniedTrace=new ArrayList<>(FIRST_READ_TRACE);
        deniedTrace.add(Map.of("sequence",4,"event","STEP_START","step","second"));
        deniedTrace.add(Map.of("sequence",5,"event","TERMINATED","code","AUTHORITY_DENIED"));
        assertEquals(Map.of("classification","FAILED","error",Map.of("code","AUTHORITY_DENIED","path","/steps/1"),"trace",deniedTrace),
            run(prepared,"task1","user1",1,requests).toMap());
        assertEquals(List.of(Map.of("agentArgs",Map.of("taskId","task1"),"protectedArgs",Map.of("actorId","user1"),"remainingMs",10_000L)),new ArrayList<>(calls));
        assertEquals(List.of(INVOCATION_REQUEST,CALL_REQUEST),new ArrayList<>(requests));
        assertTrue(responses.isEmpty());

        calls.clear(); requests.clear();
        responses.add(Map.of("kind","SUCCESS","value",Map.of("id","task2","status","COMPLETED")));
        responses.add(Map.of("kind","SUCCESS","value",Map.of("id","task2","status","OPEN")));
        var completedTrace=new ArrayList<>(FIRST_READ_TRACE);
        completedTrace.add(Map.of("sequence",4,"event","STEP_START","step","second"));
        completedTrace.add(Map.of("sequence",5,"event","CALL_DISPATCHED","step","second","capability","tasks.get"));
        completedTrace.add(Map.of("sequence",6,"event","CALL_SUCCEEDED","step","second","capability","tasks.get"));
        completedTrace.add(Map.of("sequence",7,"event","RETURNED"));
        completedTrace.add(Map.of("sequence",8,"event","TERMINATED"));
        assertEquals(Map.of("classification","COMPLETED","result",Map.of("classification","PENDING"),"trace",completedTrace),
            run(prepared,"task2","user2",2,requests).toMap());
        assertEquals(List.of(
            Map.of("agentArgs",Map.of("taskId","task2"),"protectedArgs",Map.of("actorId","user2"),"remainingMs",10_000L),
            Map.of("agentArgs",Map.of("taskId","task2"),"protectedArgs",Map.of("actorId","user2"),"remainingMs",10_000L)),new ArrayList<>(calls));
        assertEquals(List.of(INVOCATION_REQUEST,CALL_REQUEST,CALL_REQUEST),new ArrayList<>(requests));
        assertTrue(responses.isEmpty());

        calls.clear(); requests.clear();
        responses.add(Map.of("kind","BUSINESS_REJECTION","code","TASK_NOT_FOUND"));
        assertEquals(Map.of("classification","BUSINESS_REJECTION","error",Map.of("code","TASK_NOT_FOUND","path","/steps/0"),"trace",List.of(
            Map.of("sequence",0,"event","INVOCATION_START"),
            Map.of("sequence",1,"event","STEP_START","step","first"),
            Map.of("sequence",2,"event","CALL_DISPATCHED","step","first","capability","tasks.get"),
            Map.of("sequence",3,"event","TERMINATED","code","TASK_NOT_FOUND"))),
            run(prepared,"task3","user3",2,requests).toMap());
        assertEquals(List.of(Map.of("agentArgs",Map.of("taskId","task3"),"protectedArgs",Map.of("actorId","user3"),"remainingMs",10_000L)),new ArrayList<>(calls));
        assertEquals(List.of(INVOCATION_REQUEST,CALL_REQUEST),new ArrayList<>(requests));
        assertTrue(responses.isEmpty());
    }

    private static KeirRuntime.Outcome run(KeirRuntime.Prepared prepared,String task,String actor,long grant,Queue<Map<String,Object>> requests) throws Exception {
        var invocation=new KeirRuntime.Invocation(Map.of("actorId",actor),new KeirRuntime.Deadline(()->0,10_000),request->{
            requests.add(Map.copyOf(request)); return CompletableFuture.completedFuture(true);
        },Map.of("tasks.get",grant));
        return KeirRuntime.invoke(prepared,Map.of("taskId",task),invocation).toCompletableFuture().get(5,TimeUnit.SECONDS);
    }
    private static byte[] resource(String name) throws Exception {
        try(var input=MultiCallConformanceTest.class.getResourceAsStream("/read-and-decide/multi-call/"+name)) {
            assertNotNull(input); return input.readAllBytes();
        }
    }
    private static String sha256(byte[] bytes) throws Exception {
        return "sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
