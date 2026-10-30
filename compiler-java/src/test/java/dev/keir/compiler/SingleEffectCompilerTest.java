package dev.keir.compiler;

import dev.cel.expr.CheckedExpr;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SingleEffectCompilerTest {
    private static final KeirCompiler COMPILER = new KeirCompiler();
    private static final KeirCompiler.Environment ENV = KeirCompiler.Environment.effect();
    private static Path fixture(String name) { return Path.of("..","conformance","single-effect",name); }
    private static byte[] bytes(String name) throws IOException { return Files.readAllBytes(fixture(name)); }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    private static Map<String,Object> source() throws IOException { return map(StrictData.parse(bytes("source.yaml"),"/source")); }
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

    @Test void actualFixtureAndOldProfileAreDeterministicAndSeparatelySelected() throws Exception {
        var first=COMPILER.compile(bytes("source.yaml"),bytes("dcm.json"),ENV);
        var second=COMPILER.compile(bytes("source.yaml"),bytes("dcm.json"),ENV);
        var artifact=artifact(first);
        assertArrayEquals(first.artifact(),second.artifact()); assertArrayEquals(bytes("artifact.json"),first.artifact());
        assertEquals(Files.readString(fixture("artifact.sha256")).trim(),first.artifactDigest());
        assertEquals("sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(first.artifact())),first.artifactDigest());
        assertEquals(Files.readString(fixture("dcm.sha256")).trim(),StrictData.digest(model()));
        assertEquals("keir-effect/1",artifact.get("profile")); assertEquals("keir-artifact/effect-1",artifact.get("format"));
        assertEquals("keir-dcm/effect-1",map(artifact.get("dcm")).get("format"));
        var lowered=steps(artifact); assertEquals("eligible",lowered.get(2).get("when"));
        assertEquals(Map.of("type","FollowupResult","fields",Map.of("status",Map.of("literal","REJECTED","type","FollowupStatus"))),
                map(lowered.get(2).get("onError")).get("CREATE_REJECTED"));
        for(Object expression:map(artifact.get("expressions")).values())
            assertFalse(map(map(expression).get("variables")).containsKey("created"));
        assertFalse(COMPILER.compile(bytes("source.yaml"),bytes("dcm.json"),KeirCompiler.Environment.supported()).succeeded());
        var mixed=model(); mixed.put("format","keir-dcm/read-1"); denied(compile(source(),mixed),"PROFILE_UNSUPPORTED");
        var packages=new HashMap<>(ENV.packages()); packages.put("celJava","unselected");
        denied(COMPILER.compile(bytes("source.yaml"),bytes("dcm.json"),new KeirCompiler.Environment(ENV.profile(),ENV.compiler(),packages)),"PIN_MISMATCH");
        try(var input=getClass().getResourceAsStream("/read-and-decide/source.yaml");
            var dcm=getClass().getResourceAsStream("/read-and-decide/dcm.json");
            var expected=getClass().getResourceAsStream("/read-and-decide/artifact.json")) {
            assertArrayEquals(expected.readAllBytes(),COMPILER.compile(input.readAllBytes(),dcm.readAllBytes(),KeirCompiler.Environment.supported()).artifact());
        }
    }

    @Test void handlersUsePreCallEnvironmentAndExpressionsFollowAsciiOrder() throws Exception {
        var source=source();
        handler(source,0,"TASK_NOT_FOUND").put("status",Map.of("cel","input.title == \"\" ? \"SOURCE_MISSING\" : \"SOURCE_MISSING\""));
        handler(source,2,"CREATE_REJECTED").put("status",Map.of("cel","eligible ? \"REJECTED\" : \"REJECTED\""));
        var artifact=artifact(compile(source,model())); var expressions=map(artifact.get("expressions"));
        assertEquals(Set.of("input"),map(map(expressions.get("e1")).get("variables")).keySet());
        assertEquals(Set.of("input","source","eligible"),map(map(expressions.get("e6")).get("variables")).keySet());
        var call=steps(artifact).get(2); var args=map(call.get("args"));
        assertEquals("e3",map(args.get("projectId")).get("expr"));
        assertEquals("e4",map(args.get("sourceTaskId")).get("expr"));
        assertEquals("e5",map(args.get("title")).get("expr"));
        assertEquals("e7",map(map(map(artifact.get("return")).get("fields")).get("status")).get("expr"));
        var reversed=source(); var handlers=new TreeMap<String,Object>();
        handlers.put("TASK_NOT_FOUND",Map.of("record",Map.of("status",Map.of("cel","\"SOURCE_MISSING\""))));
        handlers.put("ACCESS_DENIED",Map.of("record",Map.of("status",Map.of("cel","\"REJECTED\""))));
        steps(reversed).getFirst().put("onError",handlers);
        var ordered=artifact(compile(reversed,model())); var branches=map(steps(ordered).getFirst().get("onError"));
        assertEquals("e1",map(map(map(branches.get("ACCESS_DENIED")).get("fields")).get("status")).get("expr"));
        assertEquals("e2",map(map(map(branches.get("TASK_NOT_FOUND")).get("fields")).get("status")).get("expr"));
    }

    @Test void guardsAndHandlersRejectUnavailableOrIllTypedValues() throws Exception {
        for(String guard:List.of("source","created","later")) sourceDenied(s->steps(s).get(2).put("when",guard),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->steps(s).get(1).put("let",Map.of("literal","true")),"TYPE_MISMATCH");
        sourceDenied(s->steps(s).get(2).put("when",true),"SYNTAX_INVALID");
        sourceDenied(s->steps(s).get(1).put("when","eligible"),"PROFILE_UNSUPPORTED");
        sourceDenied(s->map(map(s.get("return")).get("record")).put("status",Map.of("cel","eligible ? created.id : \"NO_OP\"")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->steps(s).add(Map.of("id","unused","let",Map.of("cel","created.id"))),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->handler(s,0,"TASK_NOT_FOUND").put("status",Map.of("cel","source.id")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->handler(s,0,"TASK_NOT_FOUND").put("status",Map.of("cel","eligible ? \"NO_OP\" : \"REJECTED\"")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->handler(s,2,"CREATE_REJECTED").put("status",Map.of("cel","created.id")),"DATAFLOW_UNAVAILABLE");
        sourceDenied(s->steps(s).getFirst().put("onError",Map.of("UNDECLARED",Map.of("record",Map.of("status",Map.of("literal","NO_OP"))))),"FIELD_UNKNOWN");
        sourceDenied(s->handler(s,0,"TASK_NOT_FOUND").put("status",Map.of("literal","UNDECLARED")),"ENUM_MISMATCH");
        sourceDenied(s->handler(s,0,"TASK_NOT_FOUND").clear(),"TYPE_MISMATCH");
        sourceDenied(s->steps(s).getFirst().put("onError",Map.of()),"SYNTAX_INVALID");
        var source=source();
        String json=new String(StrictData.canonical(source),StandardCharsets.UTF_8);
        String branch=new String(StrictData.canonical(steps(source).getFirst().get("onError")),StandardCharsets.UTF_8);
        String malformed=json.replace("\"onError\":"+branch,"\"onError\":null");
        assertNotEquals(json,malformed);
        denied(COMPILER.compile(malformed.getBytes(StandardCharsets.UTF_8),bytes("dcm.json"),ENV),"SYNTAX_INVALID");
    }

    @Test void authorityPreflightAndNominalBoundariesRemainClosed() throws Exception {
        sourceDenied(s->map(steps(s).get(2).get("args")).put("actorId",Map.of("cel","input.taskId")),"HOST_BINDING_OVERRIDE");
        sourceDenied(s->map(steps(s).get(2).get("args")).put("requestKey",Map.of("literal","invented")),"FIELD_UNKNOWN");
        sourceDenied(s->s.put("event",Map.of("source","invented")),"FIELD_UNKNOWN");
        sourceDenied(s->map(steps(s).get(2).get("args")).put("projectId",Map.of("cel","input.taskId")),"NOMINAL_MISMATCH");
        sourceDenied(s->steps(s).get(1).put("let",Map.of("cel","source.projectId == input.taskId")),"NOMINAL_MISMATCH");
        var model=model(); map(map(model.get("entrypoints")).get("TaskCompleted")).put("effectGrant",0L);
        var source=source(); steps(source).get(1).put("let",Map.of("literal",false));
        denied(compile(source,model),"ENTRY_GRANT_DENIED");
        model=model(); map(map(map(model.get("entrypoints")).get("TaskCompleted")).get("grants")).remove("tasks.createFollowUp");
        denied(compile(source(),model),"ENTRY_GRANT_DENIED");
        source=source(); steps(source).get(1).put("let",Map.of("cel","[1]"));
        steps(source).get(2).put("call","missing.capability");
        denied(compile(source,model()),"CAPABILITY_UNKNOWN");
    }

    @Test void allPotentialWritesCountEvenForConstantOrExclusiveGuards() throws Exception {
        for(boolean exclusive:List.of(false,true)) {
            var source=source(); var model=model();
            map(map(map(model.get("entrypoints")).get("TaskCompleted")).get("grants")).put("tasks.createFollowUp",2L);
            steps(source).get(1).put("let",Map.of("literal",false));
            var second=new TreeMap<>(steps(source).get(2)); second.put("id","secondWrite");
            if(exclusive) { steps(source).add(2,Map.of("id","ineligible","let",Map.of("cel","!eligible"))); second.put("when","ineligible"); }
            steps(source).add(second); denied(compile(source,model),"BUDGET_EXCEEDED");
        }
        for(int extra:List.of(2,3)) {
            var source=source(); var model=model();
            map(map(map(model.get("entrypoints")).get("TaskCompleted")).get("grants")).put("tasks.get",4L);
            for(int i=0;i<extra;i++) steps(source).add(Map.of("id","read"+i,"call","tasks.get","args",Map.of("taskId",Map.of("cel","input.taskId"))));
            var result=compile(source,model); if(extra==2) artifact(result); else denied(result,"BUDGET_EXCEEDED");
        }
    }

    @Test void handlerCountsStringBoundsAndAggregateAstCeilingsAreInclusive() throws Exception {
        var source=source(); var model=model(); var handlers=new TreeMap<String,Object>(); var codes=new ArrayList<String>();
        for(int i=0;i<16;i++) {String code="ERROR"+i;codes.add(code);handlers.put(code,Map.of("record",Map.of("status",Map.of("literal","SOURCE_MISSING"))));}
        map(map(model.get("capabilities")).get("tasks.get")).put("errors",codes); steps(source).getFirst().put("onError",handlers);
        artifact(compile(source,model)); handlers.put("ERROR16",handlers.get("ERROR0")); denied(compile(source,model),"BUDGET_EXCEEDED");
        source=source(); map(steps(source).get(2).get("args")).put("title",Map.of("literal","é".repeat(128))); artifact(compile(source,model()));
        map(steps(source).get(2).get("args")).put("title",Map.of("literal","é".repeat(128)+"x")); denied(compile(source,model()),"BUDGET_EXCEEDED");
        for(boolean over:List.of(false,true)) {
            source=source(); model=model(); handlers=new TreeMap<>(); codes=new ArrayList<>();
            for(int i=0;i<8;i++) {
                String code="ERROR"+i; codes.add(code);
                String condition=booleanTree(i<7?31:20); if(over&&i==7) condition="!"+condition;
                handlers.put(code,Map.of("record",Map.of("status",Map.of("cel",condition+" ? \"SOURCE_MISSING\" : \"NO_OP\""))));
            }
            map(map(model.get("capabilities")).get("tasks.get")).put("errors",codes); steps(source).getFirst().put("onError",handlers);
            var result=compile(source,model);
            if(over) denied(result,"BUDGET_EXCEEDED"); else {
                int nodes=0; for(Object expr:map(artifact(result).get("expressions")).values())
                    nodes+=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expr).get("checked"))).getTypeMapCount();
                assertEquals(512,nodes);
            }
        }
    }
    private static String booleanTree(int leaves) { return leaves==1?"true":"("+booleanTree(leaves/2)+" && "+booleanTree(leaves-leaves/2)+")"; }

    @Test void literalPreflightPrecedesEarlierPolicyFailures() throws Exception {
        for(boolean inHandler:List.of(false,true)) {
            var source=source();
            if(inHandler) handler(source,0,"TASK_NOT_FOUND").put("status",Map.of("literal","UNDECLARED"));
            else map(steps(source).getFirst().get("args")).put("taskId",Map.of("literal","invented"));
            steps(source).addFirst(Map.of("id","policy","let",Map.of("cel","\"x\".startsWith(\"x\")")));
            denied(compile(source,model()),inHandler?"ENUM_MISMATCH":"NOMINAL_MISMATCH");
        }
    }

    @Test void aggregateBudgetIsCheckedAfterRemainingExpressionFailures() throws Exception {
        var source=source(); var model=model(); var handlers=new TreeMap<String,Object>(); var codes=new ArrayList<String>();
        for(int i=0;i<8;i++) {
            String code="ERROR"+i; codes.add(code);
            handlers.put(code,Map.of("record",Map.of("status",Map.of("cel",booleanTree(31)+" ? \"SOURCE_MISSING\" : \"NO_OP\""))));
        }
        map(map(model.get("capabilities")).get("tasks.get")).put("errors",codes);
        steps(source).getFirst().put("onError",handlers);
        map(map(source.get("return")).get("record")).put("status",Map.of("cel","input.title.startsWith(\"x\") ? \"CREATED\" : \"NO_OP\""));
        denied(compile(source,model),"CEL_POLICY_DENIED");
    }

    @Test void cliRequiresExactExternalEffectSelectionAndNeverLeaksFailureArtifacts() throws Exception {
        var stdout=new ByteArrayOutputStream(); var stderr=new ByteArrayOutputStream();
        String[] selected={fixture("source.yaml").toString(),fixture("dcm.json").toString(),"--profile=keir-effect/1"};
        assertEquals(0,CompilerCli.run(selected,new PrintStream(stdout),new PrintStream(stderr)));
        assertArrayEquals(bytes("artifact.json"),stdout.toByteArray()); assertEquals(0,stderr.size());
        for(String[] args:List.of(new String[]{selected[0],selected[1]},new String[]{selected[0],selected[1],"--profile=guessed"})) {
            stdout.reset(); stderr.reset(); assertNotEquals(0,CompilerCli.run(args,new PrintStream(stdout),new PrintStream(stderr)));
            assertEquals(0,stdout.size()); assertFalse(stderr.toString().contains(fixture("source.yaml").toString()));
        }
    }
}
