package dev.keir.examples;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static dev.keir.examples.EffectSimulation.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectHostTest {
    @TempDir Path temp;
    private static final String ARTIFACT="sha256:d441413373a59a0a40a509143146fef8fe9c1c2426bee430965efddee591f31c";
    private static final String DCM="sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77";
    static byte[] artifact() throws IOException {
        try(var input=EffectHostTest.class.getResourceAsStream("/single-effect/artifact.json")) { assertNotNull(input); return input.readAllBytes(); }
    }
    static List<Object> corpus() throws IOException { return array(EffectJson.parse(EffectHost.scenarios())); }
    static Map<String,Object> scenario() throws IOException { return object(EffectJson.copy(corpus().getFirst())); }
    static List<Object> stubs(Map<String,Object> row) { return array(row.get("stubs")); }
    static Map<String,Object> stub(Map<String,Object> row,int index) { return object(stubs(row).get(index)); }
    static void rejected(Consumer<Map<String,Object>> mutate) throws Exception {
        var row=scenario(); mutate.accept(row);
        assertThrows(RuntimeException.class,()->EffectSimulation.run(artifact(),ARTIFACT,DCM,row));
    }

    @Test void full25ScenarioBatchMatchesHandAuthoredRowsAndKeepsRejectionAndUncertaintyHonest() throws Exception {
        byte[] scenarios=EffectHost.scenarios(); assertTrue(scenarios.length>65536);
        var corpus=corpus(); var rows=EffectHost.execute(artifact(),ARTIFACT,DCM,scenarios); assertEquals(25,rows.size());
        for(int i=0;i<rows.size();i++) {
            var expected=new LinkedHashMap<String,Object>(object(object(corpus.get(i)).get("expect"))); expected.put("id",object(corpus.get(i)).get("id"));
            same(rows.get(i),expected,"Complete row mismatch");
        }
        var byId=new HashMap<String,Map<String,Object>>(); rows.forEach(row->byId.put((String)row.get("id"),row));
        assertEquals("BUSINESS_REJECTION",object(byId.get("write-rejected").get("consumption")).get("classification"));
        assertEquals("UNCERTAIN",object(byId.get("lost-response").get("consumption")).get("classification"));
        assertFalse(object(byId.get("lost-response").get("consumption")).containsKey("result"));
        var failed=object(byId.get("confirmed-return-limit").get("consumption"));
        assertEquals("LIMIT_EXCEEDED",failed.get("classification")); assertEquals("CONFIRMED",object(array(failed.get("effects")).getFirst()).get("state"));
    }

    @Test void closedScenarioValidationRejectsProtectedOverridesUnknownFixturesAndInvalidResponses() throws Exception {
        for(Consumer<Map<String,Object>> mutate:List.<Consumer<Map<String,Object>>>of(
                row->row.put("extra",true),row->row.put("format","other"),
                row->object(row.get("given")).put("requestKey","override"),
                row->object(object(row.get("given")).get("input")).put("event",Map.of()),
                row->object(row.get("given")).put("fixture","../arbitrary-path"),
                row->stub(row,0).put("step","bad/step"),row->stub(row,0).put("version","2"),
                row->object(stub(row,0).get("response")).put("effect","CONFIRMED"),
                row->object(stub(row,1).get("response")).put("effect","NONE"),
                row->object(stub(row,1).get("response")).put("extra",true),
                row->object(object(row.get("expect")).get("consumption")).put("status","DISPLAYED"),
                row->object(object(row.get("given")).get("input")).put("title","x".repeat(257)),
                row->row.put("stubs",Collections.nCopies(5,stub(row,0))))) rejected(mutate);
    }

    @Test void missingExtraWrongOrderStepArgumentsAndProtectedOriginFailOutsideRuntimeHandling() throws Exception {
        for(Consumer<Map<String,Object>> mutate:List.<Consumer<Map<String,Object>>>of(
                row->stubs(row).removeFirst(),row->stubs(row).removeLast(),row->stubs(row).add(EffectJson.copy(stub(row,1))),
                row->Collections.swap(stubs(row),0,1),row->stub(row,0).put("step","wrongLabel"),
                row->object(stub(row,0).get("args")).put("taskId","other"),
                row->object(stub(row,0).get("protectedArgs")).put("actorId","other"),
                row->object(object(stub(row,0).get("response")).get("value")).put("id","wrong-resource"))) rejected(mutate);
    }

    @Test void caughtAdapterMismatchCannotBeReclassifiedIntoAPassingExpectedFailure() throws Exception {
        var row=scenario(); stubs(row).removeLast(); object(stub(row,0).get("args")).put("taskId","other");
        var outcome=Map.<String,Object>of("classification","FAILED","effects",List.of(),"error",Map.of("code","ADAPTER_EXCEPTION","path","/steps/0"),"trace",List.of(
                Map.of("sequence",0,"event","INVOCATION_START"),Map.of("sequence",1,"event","STEP_START","step","source"),
                Map.of("sequence",2,"event","CALL_DISPATCHED","step","source","capability",GET),Map.of("sequence",3,"event","TERMINATED","code","ADAPTER_EXCEPTION")));
        row.put("expect",Map.of("outcome",outcome,"consumption",EffectConsumer.consume(outcome),"calls",List.of(Map.of("step","source","capability",GET,"version","1","args",Map.of("taskId","task1"),"protectedArgs",Map.of("actorId","user1")))));
        validateScenario(row);
        var failure=assertThrows(IllegalArgumentException.class,()->EffectSimulation.run(artifact(),ARTIFACT,DCM,row));
        assertEquals("Simulation violations",failure.getMessage());
    }

    @Test void identicalCallsUseActualDispatchStepsRatherThanSwappedStubLabels() {
        var capture=Map.<String,Object>of("capability",GET,"version","1","args",Map.of("taskId","task1"),"protectedArgs",Map.of("actorId","user1"));
        var trace=List.of(Map.<String,Object>of("event","CALL_DISPATCHED","step","firstRead","capability",GET),
                Map.<String,Object>of("event","CALL_DISPATCHED","step","secondRead","capability",GET));
        var violations=new ArrayList<String>();
        var calls=reconcile(List.of(capture,capture),trace,List.of(Map.of("step","secondRead"),Map.of("step","firstRead")),violations);
        assertEquals(List.of("firstRead","secondRead"),calls.stream().map(call->call.get("step")).toList()); assertEquals(2,violations.size());
        violations.clear(); reconcile(List.of(capture),List.of(),List.of(),violations); assertFalse(violations.isEmpty());
    }

    @Test void batchAndScenarioByteLimitsFailClosed() throws Exception {
        assertThrows(RuntimeException.class,()->EffectHost.execute(artifact(),ARTIFACT,DCM,EffectJson.encode(List.of())));
        assertThrows(RuntimeException.class,()->EffectHost.execute(artifact(),ARTIFACT,DCM,EffectJson.encode(Collections.nCopies(65,scenario()))));
        assertThrows(RuntimeException.class,()->EffectHost.execute(artifact(),ARTIFACT,DCM,EffectJson.encode(List.of(scenario(),scenario()))));
        var big=scenario(); big.put("padding","x".repeat(65536)); assertThrows(RuntimeException.class,()->validateScenario(big));
        assertThrows(RuntimeException.class,()->EffectHost.execute(artifact(),ARTIFACT,DCM,new byte[1_048_577]));
    }

    @Test void packagedEntryPointValidatesArgumentsPinsAndFilesWithoutPartialOutputOrLeakingPaths() throws Exception {
        Path artifact=temp.resolve("artifact.json"); Files.write(artifact,artifact());
        var out=new ByteArrayOutputStream(); var err=new ByteArrayOutputStream();
        assertEquals(0,EffectHost.run(new String[]{artifact.toString(),ARTIFACT,DCM},new PrintStream(out),new PrintStream(err)));
        assertEquals(25,array(EffectJson.parse(out.toByteArray())).size()); assertEquals(0,err.size());
        for(String[] args:List.of(new String[]{},new String[]{artifact.toString(),ARTIFACT},new String[]{artifact.toString(),ARTIFACT,DCM,"extra"})) {
            out.reset(); err.reset(); assertEquals(2,EffectHost.run(args,new PrintStream(out),new PrintStream(err))); assertEquals(0,out.size()); assertTrue(err.toString().startsWith("Usage:"));
        }
        for(String[] args:List.of(new String[]{artifact.toString(),"wrong",DCM},new String[]{artifact.toString(),ARTIFACT,"sha256:"+"0".repeat(64)},new String[]{temp.resolve("private-missing").toString(),ARTIFACT,DCM})) {
            out.reset(); err.reset(); assertEquals(1,EffectHost.run(args,new PrintStream(out),new PrintStream(err))); assertEquals(0,out.size());
            assertEquals("The selected effect simulation could not be executed."+System.lineSeparator(),err.toString()); assertFalse(err.toString().contains(temp.toString()));
        }
    }
}
