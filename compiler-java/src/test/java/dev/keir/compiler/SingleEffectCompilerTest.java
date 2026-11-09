package dev.keir.compiler;

import dev.cel.expr.CheckedExpr;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SingleEffectCompilerTest {
    private static final KeirCompiler COMPILER = new KeirCompiler();
    private static final KeirCompiler.Environment ENV = KeirCompiler.Environment.supported();
    private static Path fixture(String name) { return Path.of("..","conformance","keir-3",name); }
    private static byte[] bytes(String name) throws IOException { return Files.readAllBytes(fixture(name)); }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    private static Map<String,Object> source() throws IOException {
        var source=map(StrictData.parse(bytes("source-handler.json"),"/source"));
        steps(source).get(1).put("onError",Map.of("TASK_NOT_FOUND",
                Map.of("record",new TreeMap<>(Map.of("status",Map.of("literal","NO_OP"))))));
        return source;
    }
    private static Map<String,Object> model() throws IOException { return map(StrictData.parse(bytes("dcm.json"),"/dcm")); }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> steps(Map<String,Object> source) { return (List<Map<String,Object>>)source.get("steps"); }
    private static Map<String,Object> handler(Map<String,Object> source,int step,String code) {
        return map(map(map(steps(source).get(step).get("onError")).get(code)).get("record"));
    }
    private static KeirCompiler.Compilation compile(Map<String,Object> source,Map<String,Object> model) {
        map(source.get("domain")).put("fingerprint",StrictData.digest(model));
        return COMPILER.compile(StrictData.canonical(source),StrictData.canonical(model),ENV);
    }
    private static Map<String,Object> artifact(KeirCompiler.Compilation result) {
        assertTrue(result.succeeded(),result.diagnostics().toString());
        return map(StrictData.parse(result.artifact(),"/artifact"));
    }
    private static void denied(KeirCompiler.Compilation result,String code) {
        assertFalse(result.succeeded()); assertNull(result.artifact()); assertNull(result.artifactDigest());
        assertEquals(code,result.diagnostics().getFirst().code(),result.diagnostics().toString());
    }
    private static void sourceDenied(Consumer<Map<String,Object>> change,String code) throws Exception {
        var source=source(); change.accept(source); denied(compile(source,model()),code);
    }

    @Test void handlersUseThePreCallEnvironmentAndLowerInAsciiOrder() throws Exception {
        var source=source();
        var handlers=new LinkedHashMap<String,Object>();
        handlers.put("TASK_NOT_FOUND",Map.of("record",Map.of("status",Map.of("cel","\"NO_OP\""))));
        handlers.put("ACCESS_DENIED",Map.of("record",Map.of("status",Map.of("cel","\"CREATED\""))));
        steps(source).get(1).put("onError",handlers);
        handler(source,3,"CREATE_REJECTED").put("status",Map.of("cel","eligible ? \"NO_OP\" : \"CREATED\""));
        var artifact=artifact(compile(source,model()));
        var expressions=map(artifact.get("expressions"));
        var branches=map(steps(artifact).get(1).get("onError"));
        assertEquals("e1",map(map(map(branches.get("ACCESS_DENIED")).get("fields")).get("status")).get("expr"));
        assertEquals("e2",map(map(map(branches.get("TASK_NOT_FOUND")).get("fields")).get("status")).get("expr"));
        assertEquals(Set.of("input","eligibleProject"),map(map(expressions.get("e1")).get("variables")).keySet());
        assertEquals(Set.of("input","eligibleProject","source","eligible"),map(map(expressions.get("e5")).get("variables")).keySet());
        assertEquals("e6",map(map(map(artifact.get("return")).get("fields")).get("status")).get("expr"));
    }

    @Test void guardsAndHandlersRejectUnavailableOrIllTypedValues() throws Exception {
        for(String guard:List.of("source","created","later")) sourceDenied(s->steps(s).get(3).put("when",guard),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->steps(s).get(2).put("let",Map.of("literal","true")),"TYPE_MISMATCH");
        sourceDenied(s->steps(s).get(3).put("when",true),"SYNTAX_INVALID");
        sourceDenied(s->steps(s).get(2).put("when","eligible"),"PROFILE_UNSUPPORTED");
        sourceDenied(s->map(map(s.get("return")).get("record")).put("status",Map.of("cel","eligible ? created.id : \"NO_OP\"")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->steps(s).add(Map.of("id","unused","let",Map.of("cel","created.id"))),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->handler(s,1,"TASK_NOT_FOUND").put("status",Map.of("cel","source.id")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->handler(s,1,"TASK_NOT_FOUND").put("status",Map.of("cel","eligible ? \"NO_OP\" : \"CREATED\"")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->handler(s,3,"CREATE_REJECTED").put("status",Map.of("cel","created.id")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->steps(s).get(1).put("onError",Map.of("UNDECLARED",Map.of("record",Map.of("status",Map.of("literal","NO_OP"))))),"FIELD_UNKNOWN");
        sourceDenied(s->handler(s,1,"TASK_NOT_FOUND").put("status",Map.of("literal","UNDECLARED")),"ENUM_MISMATCH");
        sourceDenied(s->handler(s,1,"TASK_NOT_FOUND").clear(),"TYPE_MISMATCH");
        sourceDenied(s->steps(s).get(1).put("onError",Map.of()),"SYNTAX_INVALID");
        var source=source();
        String json=new String(StrictData.canonical(source),StandardCharsets.UTF_8);
        String branch=new String(StrictData.canonical(steps(source).get(1).get("onError")),StandardCharsets.UTF_8);
        String malformed=json.replace("\"onError\":"+branch,"\"onError\":null");
        assertNotEquals(json,malformed);
        denied(COMPILER.compile(malformed.getBytes(StandardCharsets.UTF_8),bytes("dcm.json"),ENV),"SYNTAX_INVALID");
    }

    @Test void authorityPreflightAndNominalBoundariesRemainClosed() throws Exception {
        sourceDenied(s->map(steps(s).get(3).get("args")).put("actorId",Map.of("cel","input.taskId")),"HOST_BINDING_OVERRIDE");
        sourceDenied(s->map(steps(s).get(3).get("args")).put("requestKey",Map.of("literal","invented")),"FIELD_UNKNOWN");
        sourceDenied(s->s.put("event",Map.of("source","invented")),"FIELD_UNKNOWN");
        sourceDenied(s->map(steps(s).get(3).get("args")).put("projectId",Map.of("cel","input.taskId")),"NOMINAL_MISMATCH");
        sourceDenied(s->steps(s).get(2).put("let",Map.of("cel","source.projectId == input.taskId")),"NOMINAL_MISMATCH");
        var source=source(); steps(source).get(2).put("let",Map.of("cel","[1]"));
        steps(source).get(3).put("call","missing.capability");
        denied(compile(source,model()),"CAPABILITY_UNKNOWN");
    }

    @Test void handlerCountsAndStringBoundsAreInclusive() throws Exception {
        var source=source(); var model=model(); var handlers=new TreeMap<String,Object>(); var codes=new ArrayList<String>();
        for(int i=0;i<16;i++) {String code="ERROR"+i;codes.add(code);handlers.put(code,Map.of("record",Map.of("status",Map.of("literal","NO_OP"))));}
        map(map(model.get("capabilities")).get("tasks.get")).put("errors",codes); steps(source).get(1).put("onError",handlers);
        artifact(compile(source,model)); handlers.put("ERROR16",handlers.get("ERROR0")); denied(compile(source,model),"BUDGET_EXCEEDED");
        source=source(); map(steps(source).get(3).get("args")).put("title",Map.of("literal","é".repeat(128))); artifact(compile(source,model()));
        map(steps(source).get(3).get("args")).put("title",Map.of("literal","é".repeat(128)+"x")); denied(compile(source,model()),"BUDGET_EXCEEDED");
    }
    private static String booleanTree(int leaves) { return leaves==1?"true":"("+booleanTree(leaves/2)+" && "+booleanTree(leaves-leaves/2)+")"; }

    @Test void handlerExpressionsCountTowardTheAggregateAstCeiling() throws Exception {
        var source=source();
        var model=model();
        var handlers=new TreeMap<String,Object>();
        for (int i=0;i<8;i++) {
            String condition=booleanTree(i<7 ? 31 : 23);
            handlers.put("ERROR"+i,Map.of("record",Map.of("status",
                    Map.of("cel",condition+" ? \"CREATED\" : \"NO_OP\""))));
        }
        map(map(model.get("capabilities")).get("tasks.get")).put("errors",new ArrayList<>(handlers.keySet()));
        steps(source).get(1).put("onError",handlers);
        var expressions=map(artifact(compile(source,model)).get("expressions"));
        int nodes=0;
        for (Object expression : expressions.values())
            nodes+=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expression).get("checked"))).getTypeMapCount();
        assertEquals(512,nodes);
        handlers.put("ERROR7",Map.of("record",Map.of("status",
                Map.of("cel","!"+booleanTree(23)+" ? \"CREATED\" : \"NO_OP\""))));
        denied(compile(source,model),"BUDGET_EXCEEDED");
    }

    @Test void literalPreflightPrecedesEarlierPolicyFailures() throws Exception {
        for(boolean inHandler:List.of(false,true)) {
            var source=source();
            if(inHandler) handler(source,1,"TASK_NOT_FOUND").put("status",Map.of("literal","UNDECLARED"));
            else map(steps(source).get(1).get("args")).put("taskId",Map.of("literal","invented"));
            steps(source).addFirst(Map.of("id","policy","let",Map.of("cel","\"x\".startsWith(\"x\")")));
            denied(compile(source,model()),inHandler?"ENUM_MISMATCH":"NOMINAL_MISMATCH");
        }
    }

    @Test void aggregateBudgetIsCheckedAfterRemainingExpressionFailures() throws Exception {
        var source=source(); var model=model(); var handlers=new TreeMap<String,Object>(); var codes=new ArrayList<String>();
        for(int i=0;i<8;i++) {
            String code="ERROR"+i; codes.add(code);
            handlers.put(code,Map.of("record",Map.of("status",Map.of("cel",booleanTree(31)+" ? \"CREATED\" : \"NO_OP\""))));
        }
        map(map(model.get("capabilities")).get("tasks.get")).put("errors",codes);
        steps(source).get(1).put("onError",handlers);
        map(map(source.get("return")).get("record")).put("status",Map.of("cel","source.status.startsWith(\"x\") ? \"CREATED\" : \"NO_OP\""));
        denied(compile(source,model),"CEL_POLICY_DENIED");
    }

}
