package dev.keir.runtime;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectArtifactTest {
    private static byte[] resource(String name) throws Exception {
        try (var input=EffectArtifactTest.class.getResourceAsStream("/single-effect/"+name)) {
            assertNotNull(input,name); return input.readAllBytes();
        }
    }
    private static Map<String,Object> artifact() throws Exception { return map(RuntimeData.parseCanonical(resource("artifact.json"))); }
    private static Map<String,Object> map(Object value) { return RuntimeData.map(value,"/artifact"); }
    private static Map<String,Object> dcm(Map<String,Object> artifact) { return map(artifact.get("dcm")); }
    private static Map<String,Object> entry(Map<String,Object> artifact) { return map(map(dcm(artifact).get("entrypoints")).get("TaskCompleted")); }
    private static List<Object> steps(Map<String,Object> artifact) { return RuntimeData.list(artifact.get("steps"),"/artifact/steps"); }
    private static Map<String,Object> step(Map<String,Object> artifact,int index) { return map(steps(artifact).get(index)); }
    private static Map<String,Object> expressions(Map<String,Object> artifact) { return map(artifact.get("expressions")); }
    private static void repin(Map<String,Object> artifact) { artifact.put("dcmDigest",RuntimeData.digest(RuntimeData.canonical(dcm(artifact)))); }
    private static void denied(Map<String,Object> artifact,String code,String path) {
        RuntimeFault failure=assertThrows(RuntimeFault.class,()->Artifact.validate(artifact,true));
        assertEquals(code,failure.code); assertEquals(path,failure.path);
    }

    @Test void actualCompiledEffectArtifactUsesOnlyDefinitelyAvailableContexts() throws Exception {
        byte[] bytes=resource("artifact.json");
        assertEquals(new String(resource("artifact.sha256"),StandardCharsets.UTF_8).trim(),RuntimeData.digest(bytes));
        Map<String,Object> artifact=artifact();
        assertEquals(new String(resource("dcm.sha256"),StandardCharsets.UTF_8).trim(),artifact.get("dcmDigest"));
        Artifact.Plan plan=Artifact.validate(artifact,true);
        assertTrue(plan.effect()); assertEquals(1,plan.writeCount()); assertEquals(2,plan.callCount());
        assertEquals(Map.of("tasks.get",1,"tasks.createFollowUp",1),plan.calls());
        assertEquals(List.of("e0","e1","e2","e3","e4","e5"),new ArrayList<>(plan.contexts().keySet()));
        assertEquals(Map.of("input","FollowupInput"),plan.contexts().get("e0").get("variables"));
        assertEquals(Map.of("input","FollowupInput","source","Task","eligible","Boolean"),plan.contexts().get("e5").get("variables"));
        assertTrue(CheckedCel.prepare(plan).totalNodes()>0);
        assertThrows(UnsupportedOperationException.class,()->plan.contexts().clear());
        assertThrows(UnsupportedOperationException.class,()->map(plan.contexts().get("e5").get("variables")).clear());
        artifact.clear(); assertEquals("keir-effect/1",plan.document().get("profile"));
    }

    @Test void effectAdmissionIsExplicitAndCannotUpgradeReadArtifacts() throws Exception {
        Map<String,Object> artifact=artifact();
        assertThrows(RuntimeFault.class,()->Artifact.validate(artifact()));
        artifact.put("format","keir-artifact/read-1"); denied(artifact,"ARTIFACT_INVALID","/artifact");
        artifact=artifact(); dcm(artifact).put("format","keir-dcm/read-1"); repin(artifact);
        denied(artifact,"ARTIFACT_INVALID","/artifact/dcm");
        try (var input=EffectArtifactTest.class.getResourceAsStream("/read-and-decide/artifact.json")) {
            assertNotNull(input); Map<String,Object> read=map(RuntimeData.parseCanonical(input.readAllBytes()));
            assertFalse(Artifact.validate(read).effect()); assertEquals(0,Artifact.validate(read).writeCount());
            denied(read,"ARTIFACT_INVALID","/artifact");
        }
    }

    @Test void lifecycleKeyAndEventMetadataAreIndependentAdmissionGates() throws Exception {
        Map<String,Object> artifact=artifact(); dcm(artifact).put("assurance","REAL"); repin(artifact);
        denied(artifact,"DCM_INVALID","/artifact/dcm/assurance");
        artifact=artifact(); map(dcm(artifact).get("hostKey")).put("source","input"); repin(artifact);
        denied(artifact,"DCM_INVALID","/artifact/dcm/hostKey/source");
        for (Object value:List.of("",true,"x".repeat(257))) {
            artifact=artifact(); map(dcm(artifact).get("hostKey")).put("type",value); repin(artifact);
            denied(artifact,"DCM_INVALID","/artifact/dcm/hostKey/type");
        }
        artifact=artifact(); map(dcm(artifact).get("hostKey")).put("type","é".repeat(128)); repin(artifact); Artifact.validate(artifact,true);
        artifact=artifact(); entry(artifact).put("placement","BEFORE_COMMIT"); repin(artifact);
        denied(artifact,"DCM_INVALID","/artifact/dcm/entrypoints/TaskCompleted/placement");
        artifact=artifact(); entry(artifact).put("trigger","REQUEST"); repin(artifact);
        denied(artifact,"DCM_INVALID","/artifact/dcm/entrypoints/TaskCompleted/trigger");
        artifact=artifact(); map(entry(artifact).get("consumer")).put("kind","DISPLAY"); repin(artifact);
        denied(artifact,"DCM_INVALID","/artifact/dcm/entrypoints/TaskCompleted/consumer/kind");
        for (String type:List.of("String","TaskStatus","Task","Missing","sample.RequestUuid/1")) {
            artifact=artifact(); map(entry(artifact).get("event")).put("idType",type); repin(artifact);
            denied(artifact,"DCM_INVALID","/artifact/dcm/entrypoints/TaskCompleted/event/idType");
        }
        artifact=artifact(); entry(artifact).put("effectGrant",2L); repin(artifact);
        denied(artifact,"DCM_INVALID","/artifact/dcm/entrypoints/TaskCompleted/effectGrant");
        artifact=artifact(); map(dcm(artifact).get("hostKey")).put("extra",true); repin(artifact);
        denied(artifact,"ARTIFACT_INVALID","/artifact/dcm/hostKey");
    }

    @Test void guardMustNameAnEarlierUnconditionalBooleanBind() throws Exception {
        for (Object guard:Arrays.asList(null,true,"input","source","created","missing")) {
            Map<String,Object> artifact=artifact(); step(artifact,2).put("when",guard);
            denied(artifact,"ARTIFACT_INVALID","/artifact/steps/2/when");
        }
        Map<String,Object> artifact=artifact(); step(artifact,0).put("when","eligible");
        denied(artifact,"ARTIFACT_INVALID","/artifact/steps/0/when");
        artifact=artifact(); step(artifact,1).put("resultType","String"); step(artifact,1).put("value",Map.of("literal","","type","String"));
        denied(artifact,"ARTIFACT_INVALID","/artifact/steps/2/when");
        artifact=artifact(); step(artifact,1).put("when","eligible"); denied(artifact,"ARTIFACT_INVALID","/artifact/steps/1");
        artifact=artifact(); steps(artifact).add(new LinkedHashMap<>(step(artifact,2))); denied(artifact,"ARTIFACT_INVALID","/artifact/steps/3");
    }

    @Test void guardedResultsAreExcludedEvenFromUnusedVariableDeclarations() throws Exception {
        Map<String,Object> artifact=artifact(); map(map(expressions(artifact).get("e5")).get("variables")).put("created","CreatedTask");
        denied(artifact,"ARTIFACT_INVALID","/artifact/return/fields/status");
        artifact=artifact(); map(map(expressions(artifact).get("e4")).get("variables")).put("created","CreatedTask");
        denied(artifact,"ARTIFACT_INVALID","/artifact/steps/2/args/title");
        artifact=artifact(); map(map(expressions(artifact).get("e5")).get("variables")).remove("eligible");
        denied(artifact,"ARTIFACT_INVALID","/artifact/return/fields/status");
    }

    @Test void handlerExpressionsUsePrecallContextAndSortedCodeOrder() throws Exception {
        Map<String,Object> artifact=artifact(); Map<String,Object> root=map(artifact.get("return"));
        Map<String,Object> expression=map(expressions(artifact).get("e5"));
        expressions(artifact).put("e6",new LinkedHashMap<>(expression));
        Map<String,Object> first=Map.of("type","FollowupResult","fields",Map.of("status",Map.of("expr","e5","type","FollowupStatus")));
        Map<String,Object> second=Map.of("type","FollowupResult","fields",Map.of("status",Map.of("expr","e6","type","FollowupStatus")));
        step(artifact,2).put("onError",new LinkedHashMap<>(Map.of("CREATE_REJECTED",second,"ACCESS_DENIED",first)));
        root.put("fields",Map.of("status",Map.of("literal","CREATED","type","FollowupStatus")));
        Artifact.Plan plan=Artifact.validate(artifact,true);
        assertEquals(Map.of("input","FollowupInput","source","Task","eligible","Boolean"),plan.contexts().get("e6").get("variables"));
        assertTrue(CheckedCel.prepare(plan).totalNodes()>0);
        map(expression.get("variables")).put("created","CreatedTask");
        denied(artifact,"ARTIFACT_INVALID","/artifact/steps/2/onError/ACCESS_DENIED/fields/status");
        artifact=artifact();
        Map<String,Object> ownResult=new LinkedHashMap<>(map(expressions(artifact).get("e5")));
        ownResult.put("variables",Map.of("input","FollowupInput","source","Task"));
        expressions(artifact).put("e1",ownResult);
        step(artifact,0).put("onError",Map.of("TASK_NOT_FOUND",Map.of("type","FollowupResult","fields",Map.of("status",Map.of("expr","e1","type","FollowupStatus")))));
        denied(artifact,"ARTIFACT_INVALID","/artifact/steps/0/onError/TASK_NOT_FOUND/fields/status");
    }

    @Test void handlersAreClosedNonemptyDeclaredAndExactlyOutputTyped() throws Exception {
        for (Object handlers:Arrays.asList(null,Map.of(),Map.of("UNKNOWN",Map.of()))) {
            Map<String,Object> artifact=artifact(); step(artifact,0).put("onError",handlers);
            denied(artifact,"ARTIFACT_INVALID",handlers instanceof Map<?,?> map && !map.isEmpty()?"/artifact/steps/0/onError/UNKNOWN":"/artifact/steps/0/onError");
        }
        Map<String,Object> artifact=artifact(); Map<String,Object> handler=map(map(step(artifact,0).get("onError")).get("TASK_NOT_FOUND"));
        handler.put("type","Task"); denied(artifact,"ARTIFACT_INVALID","/artifact/steps/0/onError/TASK_NOT_FOUND");
        artifact=artifact(); handler=map(map(step(artifact,0).get("onError")).get("TASK_NOT_FOUND"));
        map(handler.get("fields")).put("extra",Map.of("literal","","type","String"));
        denied(artifact,"ARTIFACT_INVALID","/artifact/steps/0/onError/TASK_NOT_FOUND/fields");
        artifact=artifact(); Map<String,Object> handlers=new LinkedHashMap<>(); for(int i=0;i<17;i++)handlers.put("ERROR"+i,Map.of());
        step(artifact,0).put("onError",handlers); denied(artifact,"BUDGET_EXCEEDED","/artifact/steps/0/onError");
    }

    @Test void potentialWritesAndCallsRemainConservativeRegardlessOfGuards() throws Exception {
        Map<String,Object> artifact=artifact(); entry(artifact).put("effectGrant",0L); repin(artifact);
        denied(artifact,"ENTRY_GRANT_DENIED","/artifact/steps/2");
        artifact=artifact(); Map<String,Object> caps=map(dcm(artifact).get("capabilities"));
        caps.put("tasks.other",Map.of("version","1","parameters",Map.of(),"result","CreatedTask","effect","WRITE","errors",List.of()));
        map(entry(artifact).get("grants")).put("tasks.other",1L); repin(artifact);
        assertEquals(1,Artifact.validate(artifact,true).writeCount());
        steps(artifact).add(2,Map.of("op","BIND","id","never","value",Map.of("literal",false,"type","Boolean"),"resultType","Boolean"));
        step(artifact,3).put("when","never");
        for (String id:List.of("e2","e3","e4","e5")) map(map(expressions(artifact).get(id)).get("variables")).put("never","Boolean");
        steps(artifact).add(Map.of("op","CALL","id","other","capability","tasks.other","version","1","args",Map.of(),"resultType","CreatedTask","when","never"));
        denied(artifact,"BUDGET_EXCEEDED","/artifact/steps/4");
        artifact=artifact(); caps=map(dcm(artifact).get("capabilities"));
        caps.put("tasks.other",Map.of("version","1","parameters",Map.of(),"result","CreatedTask","effect","READ","errors",List.of()));
        map(entry(artifact).get("grants")).put("tasks.other",3L); repin(artifact);
        for(int i=0;i<3;i++)steps(artifact).add(Map.of("op","CALL","id","extra"+i,"capability","tasks.other","version","1","args",Map.of(),"resultType","CreatedTask","when","eligible"));
        denied(artifact,"BUDGET_EXCEEDED","/artifact/steps/5");
        artifact=artifact(); map(entry(artifact).get("grants")).remove("tasks.createFollowUp"); repin(artifact);
        denied(artifact,"BUDGET_EXCEEDED","/artifact/steps/2");
    }

    @Test void unusedEffectModelDeclarationsAndNativeKeyReferencesAreValidated() throws Exception {
        Map<String,Object> artifact=artifact(); List<Object> errors=new ArrayList<>(); for(int i=0;i<17;i++)errors.add("ERROR"+i);
        map(map(dcm(artifact).get("capabilities")).get("tasks.get")).put("errors",errors); repin(artifact);
        denied(artifact,"BUDGET_EXCEEDED","/artifact/dcm/capabilities/tasks.get/errors");
        artifact=artifact(); map(map(dcm(artifact).get("types")).get("EventId")).put("base","Boolean"); repin(artifact);
        denied(artifact,"ARTIFACT_INVALID","/artifact/dcm");
        artifact=artifact(); map(Artifact.definition(dcm(artifact),"Task").get("fields")).put("id","sample.RequestUuid/1"); repin(artifact);
        denied(artifact,"ARTIFACT_INVALID","/artifact/dcm");
    }
}
