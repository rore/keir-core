package dev.keir.runtime;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtifactTest {
    private static Map<String,Object> fixture(String name) throws IOException {
        try (var input = ArtifactTest.class.getResourceAsStream(name)) {
            assertNotNull(input,name);
            return RuntimeData.map(RuntimeData.parseCanonical(input.readAllBytes()),"/artifact");
        }
    }
    private static Map<String,Object> artifact() throws IOException { return fixture("/read-and-decide/artifact.json"); }
    private static Map<String,Object> map(Object value) { return RuntimeData.map(value,"/artifact"); }
    private static Map<String,Object> dcm(Map<String,Object> artifact) { return map(artifact.get("dcm")); }
    private static Map<String,Object> step(Map<String,Object> artifact,int index) { return map(RuntimeData.list(artifact.get("steps"),"/artifact").get(index)); }
    private static void repin(Map<String,Object> artifact) { artifact.put("dcmDigest",RuntimeData.digest(RuntimeData.canonical(artifact.get("dcm")))); }
    private static void fails(Map<String,Object> artifact,String code,String path) {
        RuntimeFault fault = assertThrows(RuntimeFault.class,()->Artifact.validate(artifact));
        assertEquals(code,fault.code); assertEquals(path,fault.path);
    }
    private static void invalidValue(Map<String,Object> model,String type,Object value,String code,String path) {
        RuntimeFault fault = assertThrows(RuntimeFault.class,()->Artifact.value(model,type,value,"/input","INPUT_INVALID"));
        assertEquals(code,fault.code); assertEquals(path,fault.path);
    }

    @Test void realArtifactsKeepBindingOrderAndSnapshots() throws Exception {
        for (String name : List.of("/read-and-decide/artifact.json","/supplemental/changed-condition.artifact.json","/supplemental/literal.artifact.json","/supplemental/scalars.artifact.json"))
            Artifact.validate(fixture(name));
        Map<String,Object> artifact = artifact(); Artifact.Plan plan = Artifact.validate(artifact);
        assertEquals(List.of("e0","e1","e2"),new ArrayList<>(plan.contexts().keySet()));
        assertEquals(Map.of("input","ClassifyInput"),plan.contexts().get("e0").get("variables"));
        assertEquals(Map.of("input","ClassifyInput","source","Task"),plan.contexts().get("e1").get("variables"));
        assertEquals(Map.of("input","ClassifyInput","source","Task","completed","Boolean"),plan.contexts().get("e2").get("variables"));
        assertEquals(Map.of("tasks.get",1),plan.calls()); assertEquals(1,plan.callCount());
        dcm(artifact).clear(); assertTrue(plan.dcm().containsKey("types"));
        assertThrows(UnsupportedOperationException.class,()->plan.dcm().clear());
        assertThrows(UnsupportedOperationException.class,()->map(plan.contexts().get("e0").get("variables")).clear());
        assertThrows(UnsupportedOperationException.class,()->plan.calls().clear());
    }

    @Test void envelopeTamperingAndExpressionContextsFailClosed() throws Exception {
        Map<String,Object> a=artifact(); a.put("extra",true); fails(a,"ARTIFACT_INVALID","/artifact");
        a=artifact(); map(a.get("environment")).put("celJava","other"); fails(a,"PIN_MISMATCH","/artifact/environment");
        a=artifact(); dcm(a).put("format","other"); fails(a,"ARTIFACT_INVALID","/artifact/dcmDigest");
        a=artifact(); map(map(a.get("expressions")).get("e1")).put("variables",Map.of("input","ClassifyInput")); fails(a,"ARTIFACT_INVALID","/artifact/steps/1/value");
        a=artifact(); map(map(a.get("expressions")).get("e1")).put("resultType","String"); fails(a,"ARTIFACT_INVALID","/artifact/steps/1/value");
        a=artifact(); map(step(a,0).get("args")).put("actorId",Map.of("literal","x","type","UserId")); fails(a,"ARTIFACT_INVALID","/artifact/steps/0/args");
        a=artifact(); map(map(step(a,0).get("args")).get("taskId")).put("expr","e1"); fails(a,"ARTIFACT_INVALID","/artifact/steps/0/args/taskId");
        a=artifact(); map(a.get("expressions")).put("e3",map(a.get("expressions")).get("e0")); fails(a,"ARTIFACT_INVALID","/artifact/expressions");
        a=artifact(); step(a,1).put("id","source"); fails(a,"ARTIFACT_INVALID","/artifact/steps/1");
    }

    @Test void modelValidatesUnusedTypesCapabilitiesAndConsumerIds() throws Exception {
        List<Consumer<Map<String,Object>>> edits = List.of(
            model->Artifact.types(model).put("Unused",Map.of("kind","number")),
            model->map(Artifact.definition(model,"Task").get("fields")).put("nested","Task"),
            model->Artifact.types(model).put("input",Map.of("kind","nominal","base","String")),
            model->map(map(model.get("capabilities")).get("tasks.get")).put("effect","WRITE"),
            model->map(Artifact.definition(model,"Classification")).put("values",List.of("DONE","DONE")));
        for (int i=0;i<edits.size();i++) {
            Map<String,Object> a=artifact(); edits.get(i).accept(dcm(a)); repin(a);
            fails(a,i==0?"DCM_INVALID":"ARTIFACT_INVALID","/artifact/dcm");
        }
        for (String id : List.of("true","false","while","bad_id","x".repeat(101),"")) {
            Map<String,Object> a=artifact(); map(map(map(dcm(a).get("entrypoints")).get("ClassifyTask")).get("consumer")).put("id",id); repin(a);
            fails(a,"ARTIFACT_INVALID","/artifact/dcm");
        }
        for (String id : List.of("valid.name","x".repeat(100),"constructor","toJSON","toString","valueOf","input")) {
            Map<String,Object> a=artifact(); map(map(map(dcm(a).get("entrypoints")).get("ClassifyTask")).get("consumer")).put("id",id); repin(a); Artifact.validate(a);
        }
    }

    @Test void quotasAndLiteralTypesAreCheckedBeforeExecution() throws Exception {
        Map<String,Object> a=artifact(); map(map(dcm(a).get("entrypoints")).get("ClassifyTask")).put("grants",Map.of()); repin(a);
        fails(a,"BUDGET_EXCEEDED","/artifact/steps/0");
        a=artifact(); a.put("steps",List.of()); fails(a,"BUDGET_EXCEEDED","/artifact/steps");
        a=artifact(); a.put("steps",Collections.nCopies(9,step(a,0))); fails(a,"BUDGET_EXCEEDED","/artifact/steps");
        a=artifact(); map(step(a,0).get("args")).put("taskId",Map.of("literal","id","type","TaskId")); fails(a,"ARTIFACT_INVALID","/artifact/steps/0/args/taskId");
        a=fixture("/supplemental/literal.artifact.json"); map(step(a,0).get("value")).put("literal",""); fails(a,"ARTIFACT_INVALID","/artifact/steps/0/value");
    }

    @Test void typedValuesPreserveFalseEmptyUnicodeAndReservedFieldNames() throws Exception {
        Map<String,Object> model = dcm(fixture("/supplemental/scalars.artifact.json"));
        Map<String,Object> value = new LinkedHashMap<>();
        map(Artifact.definition(model,"ClassifyInput").get("fields")).forEach((field,type)->value.put(field,switch((String)type){case "Boolean"->false;case "Classification"->"DONE";default->"";}));
        Map<String,Object> validated = map(Artifact.value(model,"ClassifyInput",value,"/input","INPUT_INVALID"));
        assertEquals(false,validated.get("flag")); assertEquals("",validated.get("constructor")); assertEquals("",validated.get("input"));
        value.put("toJSON","changed"); assertEquals("",validated.get("toJSON"));
        assertThrows(UnsupportedOperationException.class,()->validated.clear());
        assertEquals("😀".repeat(64),Artifact.value(model,"String","😀".repeat(64),"/input","INPUT_INVALID"));
        invalidValue(model,"String","😀".repeat(65),"BUDGET_EXCEEDED","/input");
        invalidValue(model,"String","\ud800","INPUT_INVALID","/input");
        invalidValue(model,"Boolean",null,"INPUT_INVALID","/input");
        invalidValue(model,"Classification","UNKNOWN","INPUT_INVALID","/input");
        invalidValue(model,"Classification","x".repeat(257),"BUDGET_EXCEEDED","/input");
        invalidValue(model,"Missing","","INPUT_INVALID","/input");
        value.put("flag",null); invalidValue(model,"ClassifyInput",value,"INPUT_INVALID","/input");
        value.put("flag",false); value.put("text0","x".repeat(257)); invalidValue(model,"ClassifyInput",value,"BUDGET_EXCEEDED","/input/text0");
        value.put("text0",""); value.remove("flag"); invalidValue(model,"ClassifyInput",value,"INPUT_INVALID","/input");
        value.put("flag",false); value.put("extra",true); invalidValue(model,"ClassifyInput",value,"INPUT_INVALID","/input");
    }

    @Test void recordSerializedBoundRunsAfterShapeAndFieldChecks() throws Exception {
        Map<String,Object> model=dcm(artifact()); Map<String,Object> fields=new LinkedHashMap<>(), input=new LinkedHashMap<>();
        for(int i=0;i<16;i++){String name="field"+i+"x".repeat(56);fields.put(name,"String");input.put(name,"\0".repeat(256));}
        Artifact.types(model).put("Large",Map.of("kind","record","fields",fields));
        invalidValue(model,"Large",input,"BUDGET_EXCEEDED","/input");
        input.put("extra",true); invalidValue(model,"Large",input,"INPUT_INVALID","/input");
        fields.clear(); input.clear();
        for(int i=0;i<6;i++){fields.put("field"+i,"String");input.put("field"+i,i<5?"\0".repeat(256):"");}
        int padding=8192-RuntimeData.canonical(input).length;
        String tail="\0".repeat(padding/6)+"x".repeat(padding%6);
        input.put("field5",tail);
        assertEquals(8192,RuntimeData.canonical(input).length);
        Artifact.value(model,"Large",input,"/input","INPUT_INVALID");
        input.put("field5",tail+"x");
        assertEquals(8193,RuntimeData.canonical(input).length);
        invalidValue(model,"Large",input,"BUDGET_EXCEEDED","/input");
    }
}
