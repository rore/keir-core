package dev.keir.compiler;

import dev.cel.expr.CheckedExpr;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenericHookCompilerTest {
    private static final KeirCompiler COMPILER = new KeirCompiler();
    private static final KeirCompiler.Environment ENV = KeirCompiler.Environment.supported();
    private static Path fixture(String name) {
        Path root = Path.of("..","conformance","keir-3");
        return (name.startsWith("artifact") ? root.resolve("producers/0.4.0") : root).resolve(name);
    }
    private static byte[] bytes(String name) throws IOException { return Files.readAllBytes(fixture(name)); }
    private static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    private static List<Map<String,Object>> steps(Map<String,Object> value) { return (List<Map<String,Object>>)value.get("steps"); }
    private static Map<String,Object> source() throws IOException { return map(StrictData.parse(bytes("source.yaml"),"/source")); }
    private static Map<String,Object> model() throws IOException { return map(StrictData.parse(bytes("dcm.json"),"/dcm")); }
    private static Map<String,Object> entry(Map<String,Object> model) { return map(map(model.get("entrypoints")).get("TaskCompleted")); }
    private static KeirCompiler.Compilation compile(Map<String,Object> source,Map<String,Object> model) {
        map(source.get("domain")).put("fingerprint",StrictData.digest(model));
        return COMPILER.compile(StrictData.canonical(source),StrictData.canonical(model),ENV);
    }
    private static KeirCompiler.Compilation malformed(Map<String,Object> source,Map<String,Object> model) {
        var yaml=new org.yaml.snakeyaml.Yaml();
        return COMPILER.compile(yaml.dump(source).getBytes(StandardCharsets.UTF_8),yaml.dump(model).getBytes(StandardCharsets.UTF_8),ENV);
    }
    private static Map<String,Object> artifact(KeirCompiler.Compilation result) {
        assertTrue(result.succeeded(),result.diagnostics().toString());
        return map(StrictData.parse(result.artifact(),"/artifact"));
    }
    private static void denied(KeirCompiler.Compilation result,String code,String path) {
        assertFalse(result.succeeded()); assertNull(result.artifact()); assertNull(result.artifactDigest());
        assertEquals(code,result.diagnostics().getFirst().code(),result.diagnostics().toString());
        assertEquals(path,result.diagnostics().getFirst().path());
    }

    @Test void titleConcatenationUsesOnlyTheStringOverload() throws Exception {
        var dcmBytes=bytes("dcm-title.json");
        var model=map(StrictData.parse(dcmBytes,"/dcm"));
        for(String variant:List.of("title","title-max")) {
            var compiled=COMPILER.compile(bytes("source-"+variant+".yaml"),dcmBytes,ENV);
            var document=artifact(compiled);
            assertArrayEquals(bytes("artifact-"+variant+".json"),compiled.artifact());
            var call=steps(document).getLast();
            var title=map(map(call.get("args")).get("title"));
            var expression=map(map(document.get("expressions")).get(title.get("expr")));
            var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked")));
            assertEquals("_+_",checked.getExpr().getCallExpr().getFunction());
            assertEquals(List.of("add_string"),checked.getReferenceMapMap().get(checked.getExpr().getId()).getOverloadIdList());
            assertEquals("String",expression.get("resultType"));
        }
        for(String invalid:List.of("1 + 2","[1] + [2]","source.id + \"!\"","source.status + \"!\"")) {
            var rule=map(StrictData.parse(bytes("source-title.yaml"),"/source"));
            map(steps(rule).getLast().get("args")).put("title",Map.of("cel",invalid));
            var rejected=compile(rule,model);
            assertFalse(rejected.succeeded(),invalid);
            assertEquals("CEL_POLICY_DENIED",rejected.diagnostics().getFirst().code(),rejected.diagnostics().toString());
        }
    }

    @Test void declaredErrorsAreBoundedAcrossTheCompleteCatalog() throws Exception {
        for (String id:List.of("tasks.get","tasks.create")) {
            var model=model();
            var capability=map(map(model.get("capabilities")).get(id));
            var errors=new ArrayList<>((List<String>)capability.get("errors"));
            while(errors.size()<16) errors.add("ERROR"+errors.size());
            capability.put("errors",errors);
            var accepted=compile(source(),model);
            assertTrue(accepted.succeeded(),accepted.diagnostics().toString());
            assertNotNull(accepted.artifactDigest()); assertTrue(accepted.diagnostics().isEmpty());
            errors.add("EXTRA_ERROR");
            denied(compile(source(),model),"BUDGET_EXCEEDED","/dcm/capabilities/"+id+"/errors");
        }
    }

    @Test void compilerReproducesAllSharedFixtures() throws Exception {
        for (String suffix : List.of("", "-zero", "-after-write-read", "-no-call", "-handler",
                "-title", "-title-max", "-title-composed")) {
            String modelStem=suffix.startsWith("-title") ? "dcm-title" : "dcm";
            byte[] dcm=bytes(modelStem+".json");
            byte[] source=bytes("source"+suffix+(suffix.equals("-handler") ? ".json" : ".yaml"));
            var first=COMPILER.compile(source,dcm,ENV);
            var artifact=artifact(first);
            assertArrayEquals(first.artifact(),COMPILER.compile(source,dcm,ENV).artifact(),suffix);
            assertArrayEquals(bytes("artifact"+suffix+".json"),first.artifact(),suffix);
            assertEquals(Files.readString(fixture("artifact"+suffix+".sha256")).trim(),first.artifactDigest(),suffix);
            assertEquals(Set.of("format","compiler","environment","sourceDigest","modelRef","entrypoint",
                    "expressions","steps","return"),artifact.keySet());
            assertEquals("keir-artifact/3",artifact.get("format"));
            assertEquals("keir-compiler-java/0.4.0",artifact.get("compiler"));
            assertEquals(StrictData.digest(StrictData.parse(source,"/source")),artifact.get("sourceDigest"));
            assertEquals(Map.of("celJava","0.14.0","celNode","0.6.1","protobufJava","4.35.1",
                    "protobufNode","2.6.2"),artifact.get("environment"));
            var model=map(StrictData.parse(dcm,"/dcm"));
            var domain=map(model.get("domain"));
            var digest=Files.readString(fixture(modelStem+".sha256")).trim();
            assertEquals(digest,StrictData.digest(model));
            assertEquals(Map.of("domainId",domain.get("id"),"domainVersion",domain.get("version"),
                    "format","keir-dcm/3","digest",digest,"descriptorRecipe","keir-record-descriptors/2"),
                    artifact.get("modelRef"));
            for (Object expression : map(artifact.get("expressions")).values()) {
                var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expression).get("checked")));
                assertEquals("<input>",checked.getSourceInfo().getLocation());
            }
            if (suffix.equals("-no-call")) assertTrue(steps(artifact).stream().allMatch(step -> "BIND".equals(step.get("op"))));
        }
    }

    @Test void jsonAndYamlSourceRepresentationsCompileToTheSameBytes() throws Exception {
        var result=COMPILER.compile(StrictData.canonical(source()),bytes("dcm.json"),ENV);
        artifact(result);
        assertArrayEquals(bytes("artifact.json"),result.artifact());
    }

    @Test void artifactSchemaPinsMatchTheCompiler() throws Exception {
        var schema=map(StrictData.parse(Files.readAllBytes(Path.of("..","schemas","artifact-3",
                "artifact.schema.json")),"/schema"));
        assertEquals("urn:keir:schema:artifact:3",schema.get("$id"));
        var properties=map(schema.get("properties"));
        assertEquals(List.of("keir-compiler-java/0.3.0", "keir-compiler-java/0.3.1", ENV.compiler()),map(properties.get("compiler")).get("enum"));
        var environment=map(map(properties.get("environment")).get("properties"));
        var packages=new TreeMap<String,Object>();
        environment.forEach((key,value) -> packages.put(key,map(value).get("const")));
        assertEquals(ENV.packages(),packages);
    }

    @Test void defaultCliMatchesTheApiAndRejectsExtraSelectors() throws Exception {
        var stdout=new ByteArrayOutputStream(); var stderr=new ByteArrayOutputStream();
        var out=new PrintStream(stdout); var err=new PrintStream(stderr);
        assertEquals(0,CompilerCli.run(new String[]{fixture("source.yaml").toString(),fixture("dcm.json").toString()},out,err));
        var result=COMPILER.compile(bytes("source.yaml"),bytes("dcm.json"),ENV);
        artifact(result);
        assertArrayEquals(result.artifact(),stdout.toByteArray());
        assertEquals(0,stderr.size());
        stdout.reset();
        for (String selector : List.of("--profile=keir-effect/4","--profile=keir-read/1","--contract=keir/2")) {
            stderr.reset();
            assertEquals(2,CompilerCli.run(new String[]{fixture("source.yaml").toString(),fixture("dcm.json").toString(),selector},out,err));
            assertEquals(0,stdout.size());
            assertFalse(stderr.toString().contains(fixture("source.yaml").toString()));
        }
    }

    @Test void onlyCurrentSourceAndModelIdentitiesAreAdmitted() throws Exception {
        var source=source();
        source.put("language","keir/v0");
        denied(compile(source,model()),"PROFILE_UNSUPPORTED","/source/language");
        source.put("language","keir/1");
        denied(compile(source,model()),"PROFILE_UNSUPPORTED","/source/language");
        source=source(); source.put("profile","keir-effect/4");
        denied(compile(source,model()),"FIELD_UNKNOWN","/source/profile");
        for (String format : List.of("keir-dcm/read-1","keir-dcm/read-2","keir-dcm/effect-1",
                "keir-dcm/effect-2","keir-dcm/effect-3","keir-dcm/effect-4","keir-dcm/1","keir-dcm/2")) {
            var model=model(); model.put("format",format);
            denied(compile(source(),model),"PROFILE_UNSUPPORTED","/dcm/format");
        }
        var model=model(); model.put("assurance","HOST_BOUND_SINGLE_WRITE");
        denied(compile(source(),model),"FIELD_UNKNOWN","/dcm/assurance");
        model=model(); entry(model).put("grants",Map.of());
        denied(compile(source(),model),"FIELD_UNKNOWN","/dcm/entrypoints/TaskCompleted/grants");
        model=model();
        var mismatched=source(); map(mismatched.get("domain")).put("version","wrong");
        denied(compile(mismatched,model),"PIN_MISMATCH","/source/domain/version");
    }

    @Test void theCompleteModelIsValidatedAndPinnedEvenWhenCapabilitiesAreUnused() throws Exception {
        var noCall=map(StrictData.parse(bytes("source-no-call.yaml"),"/source"));
        var model=model(); map(model.get("capabilities")).clear();
        assertTrue(steps(artifact(compile(noCall,model))).stream().allMatch(step -> "BIND".equals(step.get("op"))));
        model=model(); map(map(model.get("capabilities")).get("tasks.create")).put("version","2");
        denied(COMPILER.compile(bytes("source-no-call.yaml"),StrictData.canonical(model),ENV),
                "PIN_MISMATCH","/source/domain/fingerprint");
        map(map(model.get("capabilities")).get("tasks.create")).put("result","Missing");
        denied(compile(noCall,model),"DCM_INVALID","/dcm/capabilities/tasks.create/result");
    }

    @Test void allPotentialWritesCountEvenWithFalseOrExclusiveGuards() throws Exception {
        for (boolean exclusive : List.of(false,true)) {
            var source=source(); steps(source).get(2).put("let",Map.of("literal",false));
            var second=new TreeMap<>(steps(source).get(3)); second.put("id","secondWrite");
            if (exclusive) {
                steps(source).add(3,Map.of("id","ineligible","let",Map.of("cel","!eligible")));
                second.put("when","ineligible");
            }
            steps(source).add(second);
            denied(compile(source,model()),"BUDGET_EXCEEDED","/source/steps/"+(exclusive ? 5 : 4)+"/call");
        }
        var model=model(); entry(model).put("effectGrant",0L);
        var source=source(); steps(source).get(2).put("let",Map.of("literal",false));
        denied(compile(source,model),"ENTRY_GRANT_DENIED","/source/steps/3/call");
    }

    @Test void authoredConstantsAndGenericCreateArgumentsRemainInTheProgram() throws Exception {
        var main=artifact(compile(source(),model()));
        assertEquals(Map.of("op","BIND","id","eligibleProject","resultType","ProjectId","value",Map.of("literal","project1","type","ProjectId")),steps(main).getFirst());
        assertEquals(Set.of("projectId","title"),map(steps(main).get(3).get("args")).keySet());
        assertEquals(Map.of("literal","Follow up","type","String"),map(steps(main).get(3).get("args")).get("title"));
        assertEquals("ProjectId",map(map(map(main.get("expressions")).get("e1")).get("variables")).get("eligibleProject"));
        var direct=source();
        map(steps(direct).get(3).get("args")).put("projectId",Map.of("literal","project1","type","ProjectId"));
        assertEquals(Map.of("literal","project1","type","ProjectId"),map(steps(artifact(compile(direct,model()))).get(3).get("args")).get("projectId"));
    }

    @Test void optionalPolicyInheritsCatalogButEmptyAllowAndZeroQuotasDeny() throws Exception {
        for (Object policy : List.of(Map.of(),Map.of("maxCalls",Map.of()),Map.of("deny",List.of()),
                Map.of("allow",List.of("tasks.get","tasks.create")),Map.of("maxCalls",Map.of("tasks.get",1L)))) {
            var model=model(); entry(model).put("capabilityPolicy",policy);
            artifact(compile(source(),model));
        }
        for (Object policy : List.of(Map.of("allow",List.of()),Map.of("deny",List.of("tasks.get")),Map.of("maxCalls",Map.of("tasks.get",0L)),
                Map.of("allow",List.of("tasks.get"),"deny",List.of("tasks.get")))) {
            var model=model(); entry(model).put("capabilityPolicy",policy);
            denied(compile(source(),model),"ENTRY_GRANT_DENIED","/source/steps/1/call");
        }
        var repeated=source();
        var read=new TreeMap<>(steps(repeated).get(1)); read.put("id","secondRead");
        steps(repeated).add(read);
        var model=model(); entry(model).put("capabilityPolicy",Map.of("maxCalls",Map.of("tasks.get",1L)));
        denied(compile(repeated,model),"ENTRY_GRANT_DENIED","/source/steps/4/call");
    }

    @Test void malformedPolicyNeverMeansOmissionOrSilentClipping() throws Exception {
        var cases=new LinkedHashMap<Object,String>();
        cases.put(null,""); cases.put(List.of(),""); cases.put(Map.of("allow",List.of("tasks.missing")),"/allow/0");
        cases.put(Map.of("allow",List.of("tasks.get","tasks.get")),"/allow/1");
        cases.put(Map.of("deny",List.of("tasks.missing")),"/deny/0");
        cases.put(Map.of("maxCalls",Map.of("tasks.missing",0L)),"/maxCalls/tasks.missing");
        cases.put(Map.of("maxCalls",Map.of("tasks.get",5L)),"/maxCalls/tasks.get");
        cases.put(Map.of("maxCalls",Map.of("tasks.get",-1L)),"/maxCalls/tasks.get");
        cases.put(Map.of("maxCalls",Map.of("tasks.get",1.5)),"/maxCalls/tasks.get");
        cases.put(Map.of("maxCalls",Map.of("tasks.get",true)),"/maxCalls/tasks.get");
        cases.put(Map.of("allow",List.of(),"maxCalls",Map.of("tasks.get",1L)),"/maxCalls/tasks.get");
        cases.put(Map.of("deny",List.of("tasks.get"),"maxCalls",Map.of("tasks.get",1L)),"/maxCalls/tasks.get");
        for (var test : cases.entrySet()) {
            var model=model(); entry(model).put("capabilityPolicy",test.getKey());
            denied(malformed(source(),model),"DCM_INVALID","/dcm/entrypoints/TaskCompleted/capabilityPolicy"+test.getValue());
        }
        for (String field : List.of("allow","deny","maxCalls")) {
            var model=model(); var policy=new TreeMap<String,Object>(); policy.put(field,null);
            entry(model).put("capabilityPolicy",policy);
            denied(malformed(source(),model),"DCM_INVALID","/dcm/entrypoints/TaskCompleted/capabilityPolicy/"+field);
        }
        var model=model(); entry(model).put("capabilityPolicy",Map.of("extra",true));
        denied(compile(source(),model),"DCM_INVALID","/dcm/entrypoints/TaskCompleted/capabilityPolicy/extra");
    }

    @Test void nominalAnnotationsResolveOnlyPublishedBusinessNominals() throws Exception {
        for (Object type : Arrays.asList(null,true,"Missing","String","Boolean","Task","TaskStatus","sample.RequestUuid/1")) {
            var source=source(); var literal=new TreeMap<String,Object>(); literal.put("literal","project1"); literal.put("type",type);
            steps(source).getFirst().put("let",literal);
            denied(malformed(source,model()),"NOMINAL_MISMATCH","/source/steps/0/let/type");
        }
        var source=source(); steps(source).getFirst().put("let",Map.of("literal","project1"));
        denied(compile(source,model()),"NOMINAL_MISMATCH","/source/steps/2/let/cel");
        source=source(); steps(source).getFirst().put("let",Map.of("literal","project1","type","TaskId"));
        denied(compile(source,model()),"NOMINAL_MISMATCH","/source/steps/2/let/cel");
        source=source(); map(steps(source).get(3).get("args")).put("projectId",Map.of("literal","task1","type","TaskId"));
        denied(compile(source,model()),"NOMINAL_MISMATCH","/source/steps/3/args/projectId/literal");
        var model=model(); map(model.get("hostKey")).put("type","ProjectId");
        artifact(compile(source(),model));
    }

    @Test void nominalSyntaxCannotCastExpressionsOrOverrideProtectedBindings() throws Exception {
        var source=source(); steps(source).getFirst().put("let",Map.of("cel","\"project1\"","type","ProjectId"));
        denied(compile(source,model()),"FIELD_UNKNOWN","/source/steps/0/let/type");
        source=source(); steps(source).getFirst().put("let",Map.of("literal",true,"type","ProjectId"));
        denied(compile(source,model()),"PROFILE_UNSUPPORTED","/source/steps/0/let/literal");
        source=source(); map(steps(source).get(1).get("args")).put("actorId",Map.of("literal","user1","type","UserId"));
        denied(compile(source,model()),"HOST_BINDING_OVERRIDE","/source/steps/1/args/actorId");
        source=source(); map(steps(source).get(2).get("let")).put("cel","string(eligibleProject) == \"project1\"");
        denied(compile(source,model()),"CEL_POLICY_DENIED","/source/steps/2/let/cel");
    }

    @Test void nominalLiteralUsesExistingUnicodeAndByteBounds() throws Exception {
        for (String value : List.of("","é".repeat(128),"😀".repeat(64))) {
            var source=source(); steps(source).getFirst().put("let",Map.of("literal",value,"type","ProjectId"));
            artifact(compile(source,model()));
        }
        var source=source(); steps(source).getFirst().put("let",Map.of("literal","é".repeat(129),"type","ProjectId"));
        denied(compile(source,model()),"BUDGET_EXCEEDED","/source/steps/0/let/literal");
        String raw=new String(bytes("source.yaml"),StandardCharsets.UTF_8).replace("literal: 'project1'","literal: \"\\ud800\"");
        assertTrue(raw.contains("\\ud800"));
        assertTrue(raw.contains("\\ud800"));
        denied(COMPILER.compile(raw.getBytes(StandardCharsets.UTF_8),bytes("dcm.json"),ENV),"SYNTAX_INVALID","/source");
    }

}
