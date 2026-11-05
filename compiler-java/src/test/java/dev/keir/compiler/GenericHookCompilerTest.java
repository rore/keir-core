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
    private static Path fixture(String profile,String name) { return Path.of("..","conformance","generic-hooks",profile,name); }
    private static byte[] bytes(String profile,String name) throws IOException { return Files.readAllBytes(fixture(profile,name)); }
    private static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    private static List<Map<String,Object>> steps(Map<String,Object> value) { return (List<Map<String,Object>>)value.get("steps"); }
    private static Map<String,Object> source(String profile) throws IOException { return map(StrictData.parse(bytes(profile,"source.yaml"),"/source")); }
    private static Map<String,Object> model(String profile) throws IOException { return map(StrictData.parse(bytes(profile,"dcm.json"),"/dcm")); }
    private static Map<String,Object> entry(Map<String,Object> model) { return map(map(model.get("entrypoints")).values().iterator().next()); }
    private static KeirCompiler.Environment environment(String profile) { return switch (profile) {
        case "read" -> KeirCompiler.Environment.read2();
        case "effect3" -> KeirCompiler.Environment.effect3();
        case "effect4" -> KeirCompiler.Environment.effect4();
        default -> KeirCompiler.Environment.effect2();
    }; }
    private static KeirCompiler.Compilation compile(String profile,Map<String,Object> source,Map<String,Object> model) {
        map(source.get("domain")).put("fingerprint",StrictData.digest(model));
        return COMPILER.compile(StrictData.canonical(source),StrictData.canonical(model),environment(profile));
    }
    private static KeirCompiler.Compilation malformed(String profile,Map<String,Object> source,Map<String,Object> model) {
        var yaml=new org.yaml.snakeyaml.Yaml();
        return COMPILER.compile(yaml.dump(source).getBytes(StandardCharsets.UTF_8),yaml.dump(model).getBytes(StandardCharsets.UTF_8),environment(profile));
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

    @Test void titleConcatenationUsesOnlyTheEffect4StringOverload() throws Exception {
        var dcmBytes=bytes("effect4","dcm-title.json");
        var model=map(StrictData.parse(dcmBytes,"/dcm"));
        for(String variant:List.of("title","title-max")) {
            var compiled=COMPILER.compile(bytes("effect4","source-"+variant+".yaml"),dcmBytes,environment("effect4"));
            var document=artifact(compiled);
            assertArrayEquals(bytes("effect4","artifact-"+variant+".json"),compiled.artifact());
            var call=steps(document).getLast();
            var title=map(map(call.get("args")).get("title"));
            var expression=map(map(document.get("expressions")).get(title.get("expr")));
            var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked")));
            assertEquals("_+_",checked.getExpr().getCallExpr().getFunction());
            assertEquals(List.of("add_string"),checked.getReferenceMapMap().get(checked.getExpr().getId()).getOverloadIdList());
            assertEquals("String",expression.get("resultType"));
        }
        for(String invalid:List.of("1 + 2","[1] + [2]","source.id + \"!\"","source.status + \"!\"")) {
            var rule=map(StrictData.parse(bytes("effect4","source-title.yaml"),"/source"));
            map(steps(rule).getLast().get("args")).put("title",Map.of("cel",invalid));
            var rejected=compile("effect4",rule,model);
            assertFalse(rejected.succeeded(),invalid);
            assertEquals("CEL_POLICY_DENIED",rejected.diagnostics().getFirst().code(),rejected.diagnostics().toString());
        }
        for(String profile:List.of("effect","effect3")) {
            var rule=source(profile);
            map(steps(rule).getLast().get("args")).put("title",Map.of("cel","\"a\" + \"b\""));
            assertEquals("CEL_POLICY_DENIED",compile(profile,rule,model(profile)).diagnostics().getFirst().code());
        }
    }

    @Test void declaredErrorsAreBoundedAcrossGenericProfilesAndTheCompleteCatalog() throws Exception {
        for (String profile:List.of("read","effect")) for (String id:List.of("tasks.get","tasks.create")) {
            var model=model(profile);
            var capability=map(map(model.get("capabilities")).get(id));
            var errors=new ArrayList<>((List<String>)capability.get("errors"));
            while(errors.size()<16) errors.add("ERROR"+errors.size());
            capability.put("errors",errors);
            var accepted=compile(profile,source(profile),model);
            assertTrue(accepted.succeeded(),accepted.diagnostics().toString());
            assertNotNull(accepted.artifactDigest()); assertTrue(accepted.diagnostics().isEmpty());
            errors.add("EXTRA_ERROR");
            denied(compile(profile,source(profile),model),"BUDGET_EXCEEDED","/dcm/capabilities/"+id+"/errors");
        }
    }

    @Test void compilerReproducesEveryReferenceFixtureWithoutEmbeddedModels() throws Exception {
        for (String profile : List.of("read","effect")) for (String suffix : profile.equals("read")
                ? List.of("","-repeated","-alternate") : List.of("","-project2","-title2")) {
            var first=COMPILER.compile(bytes(profile,"source"+suffix+".yaml"),bytes(profile,"dcm.json"),environment(profile));
            var second=COMPILER.compile(bytes(profile,"source"+suffix+".yaml"),bytes(profile,"dcm.json"),environment(profile));
            var artifact=artifact(first);
            assertArrayEquals(first.artifact(),second.artifact());
            assertArrayEquals(bytes(profile,"artifact"+suffix+".json"),first.artifact());
            assertEquals(Files.readString(fixture(profile,"artifact"+suffix+".sha256")).trim(),first.artifactDigest());
            assertEquals(Set.of("format","profile","compiler","environment","sourceDigest","modelRef","entrypoint","expressions","steps","return"),artifact.keySet());
            assertEquals("keir-artifact/"+profile+"-2",artifact.get("format"));
            assertEquals(environment(profile).profile(),artifact.get("profile"));
            var model=model(profile); var domain=map(model.get("domain"));
            assertEquals(Map.of("domainId",domain.get("id"),"domainVersion",domain.get("version"),"format",model.get("format"),
                    "digest",StrictData.digest(model),"descriptorRecipe","keir-record-descriptors/1"),artifact.get("modelRef"));
            assertEquals(Files.readString(fixture(profile,"dcm.sha256")).trim(),StrictData.digest(model));
            for (Object expression : map(artifact.get("expressions")).values()) {
                var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expression).get("checked")));
                assertEquals("keir-read/1",checked.getSourceInfo().getLocation());
            }
        }
        assertEquals(1534,bytes("effect","dcm.json").length);
        assertEquals("sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04",StrictData.digest(model("effect")));
    }

    @Test void effect3CompilesPinnedSourcesAndKeepsOnePotentialWriteLimit() throws Exception {
        var model=model("effect3");
        assertEquals("HOST_QUALIFIED_SINGLE_WRITE",model.get("assurance"));
        assertEquals("KEY_PAYLOAD_MISMATCH",((List<?>)map(map(model.get("capabilities")).get("tasks.create")).get("errors")).getLast());
        assertEquals(Files.readString(fixture("effect3","dcm.sha256")).trim(),StrictData.digest(model));
        for (String suffix : List.of("", "-zero", "-after-write-read")) {
            String sourceName="source"+suffix+".yaml", artifactName="artifact"+suffix+".json";
            var first=COMPILER.compile(bytes("effect3",sourceName),bytes("effect3","dcm.json"),environment("effect3"));
            var second=COMPILER.compile(bytes("effect3",sourceName),bytes("effect3","dcm.json"),environment("effect3"));
            assertArrayEquals(first.artifact(),second.artifact());
            assertArrayEquals(bytes("effect3",artifactName),first.artifact());
            assertEquals(Files.readString(fixture("effect3","artifact"+suffix+".sha256")).trim(),first.artifactDigest());
            var artifact=artifact(first);
            assertEquals("keir-artifact/effect-3",artifact.get("format"));
            assertEquals("keir-effect/3",artifact.get("profile"));
            assertEquals("keir-dcm/effect-3",map(artifact.get("modelRef")).get("format"));
        }
        var zero=artifact(COMPILER.compile(bytes("effect3","source-zero.yaml"),bytes("effect3","dcm.json"),environment("effect3")));
        assertTrue(((List<?>)zero.get("steps")).stream().noneMatch(step -> "CALL".equals(map(step).get("op")) && "tasks.create".equals(map(step).get("capability"))));
        var source=source("effect3"); var duplicate=new TreeMap<>(steps(source).get(3)); duplicate.put("id","secondWrite"); duplicate.put("when","eligible"); steps(source).add(duplicate);
        denied(compile("effect3",source,model("effect3")),"BUDGET_EXCEEDED","/source/steps/4/call");
        source=source("effect3"); steps(source).get(2).put("let",Map.of("literal",false));
        duplicate=new TreeMap<>(steps(source).get(3)); duplicate.put("id","secondWrite"); duplicate.put("when","eligible"); steps(source).add(duplicate);
        denied(compile("effect3",source,model("effect3")),"BUDGET_EXCEEDED","/source/steps/4/call");
    }

    @Test void effect3RequiresItsExactAssuranceAndFormats() throws Exception {
        var model=model("effect3"); model.put("assurance","SIMULATION_ONLY");
        denied(compile("effect3",source("effect3"),model),"DCM_INVALID","/dcm/assurance");
        model=model("effect3"); model.put("format","keir-dcm/effect-2");
        denied(compile("effect3",source("effect3"),model),"PROFILE_UNSUPPORTED","/dcm/format");
        var source=source("effect3"); map(source.get("domain")).put("version","2");
        denied(compile("effect3",source,model("effect3")),"PIN_MISMATCH","/source/domain/version");
        var stdout=new ByteArrayOutputStream(); var stderr=new ByteArrayOutputStream();
        String[] args={fixture("effect3","source.yaml").toString(),fixture("effect3","dcm.json").toString(),"--profile=keir-effect/3"};
        assertEquals(0,CompilerCli.run(args,new PrintStream(stdout),new PrintStream(stderr)));
        assertArrayEquals(bytes("effect3","artifact.json"),stdout.toByteArray()); assertEquals(0,stderr.size());
    }

    @Test void effect4ReproducesPinnedFixturesAndKeepsItsBoundSingleWriteContract() throws Exception {
        var model=model("effect4");
        assertEquals("HOST_BOUND_SINGLE_WRITE",model.get("assurance"));
        assertEquals(Files.readString(fixture("effect4","dcm.sha256")).trim(),StrictData.digest(model));
        for (String suffix : List.of("", "-zero", "-after-write-read", "-no-call")) {
            var first=COMPILER.compile(bytes("effect4","source"+suffix+".yaml"),bytes("effect4","dcm.json"),environment("effect4"));
            var second=COMPILER.compile(bytes("effect4","source"+suffix+".yaml"),bytes("effect4","dcm.json"),environment("effect4"));
            assertArrayEquals(first.artifact(),second.artifact());
            assertArrayEquals(bytes("effect4","artifact"+suffix+".json"),first.artifact());
            assertEquals(Files.readString(fixture("effect4","artifact"+suffix+".sha256")).trim(),first.artifactDigest());
            var artifact=artifact(first);
            assertEquals("keir-artifact/effect-4",artifact.get("format"));
            assertEquals("keir-effect/4",artifact.get("profile"));
            assertEquals("keir-dcm/effect-4",map(artifact.get("modelRef")).get("format"));
            if (suffix.equals("-no-call")) assertTrue(steps(artifact).stream().allMatch(step -> "BIND".equals(map(step).get("op"))));
        }
        var source=source("effect4"); var duplicate=new TreeMap<>(steps(source).get(3)); duplicate.put("id","secondWrite"); duplicate.put("when","eligible"); steps(source).add(duplicate);
        denied(compile("effect4",source,model("effect4")),"BUDGET_EXCEEDED","/source/steps/4/call");
        var noCall=map(StrictData.parse(bytes("effect4","source-no-call.yaml"),"/source"));
        model= model("effect4"); map(model.get("capabilities")).clear();
        var empty=artifact(compile("effect4",noCall,model));
        assertTrue(steps(empty).stream().allMatch(step -> "BIND".equals(map(step).get("op"))));
        var missingCall=source("effect4"); steps(missingCall).get(1).put("call","tasks.missing");
        denied(compile("effect4",missingCall,model("effect4")),"CAPABILITY_UNKNOWN","/source/steps/1/call");
    }

    @Test void effect4RequiresItsOwnAssuranceAndCliProfile() throws Exception {
        var model=model("effect4"); model.put("assurance","HOST_QUALIFIED_SINGLE_WRITE");
        denied(compile("effect4",source("effect4"),model),"DCM_INVALID","/dcm/assurance");
        model=model("effect4"); model.put("format","keir-dcm/effect-3");
        denied(compile("effect4",source("effect4"),model),"PROFILE_UNSUPPORTED","/dcm/format");
        var stdout=new ByteArrayOutputStream(); var stderr=new ByteArrayOutputStream();
        String[] args={fixture("effect4","source.yaml").toString(),fixture("effect4","dcm.json").toString(),"--profile=keir-effect/4"};
        assertEquals(0,CompilerCli.run(args,new PrintStream(stdout),new PrintStream(stderr)));
        assertArrayEquals(bytes("effect4","artifact.json"),stdout.toByteArray()); assertEquals(0,stderr.size());
    }

    @Test void effect4StructuralSourcesCompileToThePinnedArtifacts() throws Exception {
        Path fixtures=Path.of("..","conformance","effect4-structural");
        byte[] dcm=bytes("effect4","dcm.json"), source=Files.readAllBytes(fixtures.resolve("source.json"));
        assertArrayEquals(StrictData.canonical(StrictData.parse(bytes("effect4","source.yaml"),"/source")),
                StrictData.canonical(StrictData.parse(source,"/source")));
        var baseline=COMPILER.compile(source,dcm,environment("effect4"));
        assertTrue(baseline.succeeded(),baseline.diagnostics().toString());
        assertArrayEquals(bytes("effect4","artifact.json"),baseline.artifact());
        assertEquals(Files.readString(fixture("effect4","artifact.sha256")).trim(),baseline.artifactDigest());
        var guarded=COMPILER.compile(Files.readAllBytes(fixtures.resolve("source-handler.json")),dcm,environment("effect4"));
        assertTrue(guarded.succeeded(),guarded.diagnostics().toString());
        assertArrayEquals(Files.readAllBytes(fixtures.resolve("artifact-handler.json")),guarded.artifact());
        assertEquals(Files.readString(fixtures.resolve("artifact-handler.sha256")).trim(),guarded.artifactDigest());
    }

    @Test void authoredConstantsAndGenericCreateArgumentsRemainInTheProgram() throws Exception {
        var main=artifact(compile("effect",source("effect"),model("effect")));
        assertEquals(Map.of("op","BIND","id","eligibleProject","resultType","ProjectId","value",Map.of("literal","project1","type","ProjectId")),steps(main).getFirst());
        assertEquals(Set.of("projectId","title"),map(steps(main).get(3).get("args")).keySet());
        assertEquals(Map.of("literal","Follow up","type","String"),map(steps(main).get(3).get("args")).get("title"));
        assertEquals("ProjectId",map(map(map(main.get("expressions")).get("e1")).get("variables")).get("eligibleProject"));
        var direct=source("effect");
        map(steps(direct).get(3).get("args")).put("projectId",Map.of("literal","project1","type","ProjectId"));
        assertEquals(Map.of("literal","project1","type","ProjectId"),map(steps(artifact(compile("effect",direct,model("effect")))).get(3).get("args")).get("projectId"));
        for (String suffix : List.of("-project2","-title2")) {
            var variant=map(StrictData.parse(bytes("effect","artifact"+suffix+".json"),"/artifact"));
            assertEquals(main.get("modelRef"),variant.get("modelRef"));
            assertNotEquals(main.get("sourceDigest"),variant.get("sourceDigest"));
            assertNotEquals(StrictData.digest(main),StrictData.digest(variant));
        }
    }

    @Test void optionalPolicyInheritsCatalogButEmptyAllowAndZeroQuotasDeny() throws Exception {
        for (Object policy : List.of(Map.of(),Map.of("maxCalls",Map.of()),Map.of("deny",List.of()),
                Map.of("allow",List.of("tasks.get")),Map.of("maxCalls",Map.of("tasks.get",1L)))) {
            var model=model("read"); entry(model).put("capabilityPolicy",policy);
            artifact(compile("read",source("read"),model));
        }
        for (Object policy : List.of(Map.of("allow",List.of()),Map.of("deny",List.of("tasks.get")),Map.of("maxCalls",Map.of("tasks.get",0L)),
                Map.of("allow",List.of("tasks.get"),"deny",List.of("tasks.get")))) {
            var model=model("read"); entry(model).put("capabilityPolicy",policy);
            denied(compile("read",source("read"),model),"ENTRY_GRANT_DENIED","/source/steps/0/call");
        }
        var repeated=map(StrictData.parse(bytes("read","source-repeated.yaml"),"/source"));
        var model=model("read"); entry(model).put("capabilityPolicy",Map.of("maxCalls",Map.of("tasks.get",1L)));
        denied(compile("read",repeated,model),"ENTRY_GRANT_DENIED","/source/steps/1/call");
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
            var model=model("read"); entry(model).put("capabilityPolicy",test.getKey());
            denied(malformed("read",source("read"),model),"DCM_INVALID","/dcm/entrypoints/ClassifyTask/capabilityPolicy"+test.getValue());
        }
        for (String field : List.of("allow","deny","maxCalls")) {
            var model=model("read"); var policy=new TreeMap<String,Object>(); policy.put(field,null);
            entry(model).put("capabilityPolicy",policy);
            denied(malformed("read",source("read"),model),"DCM_INVALID","/dcm/entrypoints/ClassifyTask/capabilityPolicy/"+field);
        }
        var model=model("read"); entry(model).put("capabilityPolicy",Map.of("extra",true));
        denied(compile("read",source("read"),model),"DCM_INVALID","/dcm/entrypoints/ClassifyTask/capabilityPolicy/extra");
    }

    @Test void newProfilesRejectLegacyGrantsAndRetainLifecycleRestrictions() throws Exception {
        for (String profile : List.of("read","effect")) {
            var model=model(profile); entry(model).put("grants",Map.of());
            denied(compile(profile,source(profile),model),"FIELD_UNKNOWN","/dcm/entrypoints/"+(profile.equals("read")?"ClassifyTask":"TaskCompleted")+"/grants");
        }
        var model=model("read"); entry(model).put("effectGrant",0L);
        denied(compile("read",source("read"),model),"FIELD_UNKNOWN","/dcm/entrypoints/ClassifyTask/effectGrant");
        model=model("read"); entry(model).put("trigger","EVENT");
        denied(compile("read",source("read"),model),"PROFILE_UNSUPPORTED","/dcm/entrypoints/ClassifyTask/trigger");
        model=model("effect"); entry(model).put("placement","BEFORE_COMMIT");
        denied(compile("effect",source("effect"),model),"DCM_INVALID","/dcm/entrypoints/TaskCompleted/placement");
        model=model("effect"); model.put("assurance","REAL");
        denied(compile("effect",source("effect"),model),"DCM_INVALID","/dcm/assurance");
    }

    @Test void completeReadCatalogValidatesUnusedWritesButNeverPermitsTheirCalls() throws Exception {
        var model=model("read"); var dcm=Dcm.parse(model,false,true);
        assertEquals(Set.of("tasks.get","tasks.lookup","tasks.create"),dcm.capabilities().keySet());
        assertEquals("WRITE",dcm.capabilities().get("tasks.create").effect());
        var source=source("read"); steps(source).getFirst().put("call","tasks.create");
        denied(compile("read",source,model),"ENTRY_GRANT_DENIED","/source/steps/0/call");
        map(map(model.get("capabilities")).get("tasks.create")).put("result","Missing");
        denied(compile("read",source("read"),model),"DCM_INVALID","/dcm/capabilities/tasks.create/result");
    }

    @Test void aggregateAndHardWriteCeilingsCountAllPotentialCalls() throws Exception {
        for (int count : List.of(4,5)) {
            var source=source("read"); var calls=new ArrayList<Map<String,Object>>();
            calls.add(steps(source).getFirst());
            for (int i=1;i<count;i++) calls.add(Map.of("id","call"+i,"call","tasks.get","args",Map.of("taskId",Map.of("cel","input.taskId"))));
            calls.add(steps(source).get(1)); source.put("steps",calls);
            if (count==4) artifact(compile("read",source,model("read")));
            else denied(compile("read",source,model("read")),"BUDGET_EXCEEDED","/source/steps");
        }
        var source=source("effect"); steps(source).get(2).put("let",Map.of("literal",false));
        var duplicate=new TreeMap<>(steps(source).get(3)); duplicate.put("id","secondWrite"); steps(source).add(duplicate);
        denied(compile("effect",source,model("effect")),"BUDGET_EXCEEDED","/source/steps/4/call");
        var model=model("effect"); entry(model).put("effectGrant",0L);
        denied(compile("effect",source("effect"),model),"ENTRY_GRANT_DENIED","/source/steps/3/call");
        for (Object invalid : Arrays.asList(null,true,2L)) {
            model=model("effect"); entry(model).put("effectGrant",invalid);
            denied(malformed("effect",source("effect"),model),"DCM_INVALID","/dcm/entrypoints/TaskCompleted/effectGrant");
        }
    }

    @Test void nominalAnnotationsResolveOnlyPublishedBusinessNominals() throws Exception {
        for (Object type : Arrays.asList(null,true,"Missing","String","Boolean","Task","TaskStatus","sample.RequestUuid/1")) {
            var source=source("effect"); var literal=new TreeMap<String,Object>(); literal.put("literal","project1"); literal.put("type",type);
            steps(source).getFirst().put("let",literal);
            denied(malformed("effect",source,model("effect")),"NOMINAL_MISMATCH","/source/steps/0/let/type");
        }
        var source=source("effect"); steps(source).getFirst().put("let",Map.of("literal","project1"));
        denied(compile("effect",source,model("effect")),"NOMINAL_MISMATCH","/source/steps/2/let/cel");
        source=source("effect"); steps(source).getFirst().put("let",Map.of("literal","project1","type","TaskId"));
        denied(compile("effect",source,model("effect")),"NOMINAL_MISMATCH","/source/steps/2/let/cel");
        source=source("effect"); map(steps(source).get(3).get("args")).put("projectId",Map.of("literal","task1","type","TaskId"));
        denied(compile("effect",source,model("effect")),"NOMINAL_MISMATCH","/source/steps/3/args/projectId/literal");
        var model=model("effect"); map(model.get("hostKey")).put("type","ProjectId");
        artifact(compile("effect",source("effect"),model));
    }

    @Test void nominalSyntaxCannotCastExpressionsOrOverrideProtectedBindings() throws Exception {
        var source=source("effect"); steps(source).getFirst().put("let",Map.of("cel","\"project1\"","type","ProjectId"));
        denied(compile("effect",source,model("effect")),"FIELD_UNKNOWN","/source/steps/0/let/type");
        source=source("effect"); steps(source).getFirst().put("let",Map.of("literal",true,"type","ProjectId"));
        denied(compile("effect",source,model("effect")),"PROFILE_UNSUPPORTED","/source/steps/0/let/literal");
        source=source("effect"); map(steps(source).get(1).get("args")).put("actorId",Map.of("literal","user1","type","UserId"));
        denied(compile("effect",source,model("effect")),"HOST_BINDING_OVERRIDE","/source/steps/1/args/actorId");
        source=source("effect"); map(steps(source).get(2).get("let")).put("cel","string(eligibleProject) == \"project1\"");
        denied(compile("effect",source,model("effect")),"CEL_POLICY_DENIED","/source/steps/2/let/cel");
    }

    @Test void nominalLiteralUsesExistingUnicodeAndByteBounds() throws Exception {
        for (String value : List.of("","é".repeat(128),"😀".repeat(64))) {
            var source=source("effect"); steps(source).getFirst().put("let",Map.of("literal",value,"type","ProjectId"));
            artifact(compile("effect",source,model("effect")));
        }
        var source=source("effect"); steps(source).getFirst().put("let",Map.of("literal","é".repeat(129),"type","ProjectId"));
        denied(compile("effect",source,model("effect")),"BUDGET_EXCEEDED","/source/steps/0/let/literal");
        String raw=new String(bytes("effect","source.yaml"),StandardCharsets.UTF_8).replace("literal: 'project1'","literal: \"\\ud800\"");
        denied(COMPILER.compile(raw.getBytes(StandardCharsets.UTF_8),bytes("effect","dcm.json"),environment("effect")),"SYNTAX_INVALID","/source");
    }

    @Test void exactCliSelectionAndMixedProfilesFailWithoutOutput() throws Exception {
        for (String profile : List.of("read","effect")) {
            var stdout=new ByteArrayOutputStream(); var stderr=new ByteArrayOutputStream();
            String[] args={fixture(profile,"source.yaml").toString(),fixture(profile,"dcm.json").toString(),"--profile=keir-"+profile+"/2"};
            assertEquals(0,CompilerCli.run(args,new PrintStream(stdout),new PrintStream(stderr)));
            assertArrayEquals(bytes(profile,"artifact.json"),stdout.toByteArray()); assertEquals(0,stderr.size());
            for (String selection : List.of("--profile=keir-"+profile+"/1","--profile=keir-"+profile+"/3")) {
                args[2]=selection; stdout.reset(); stderr.reset();
                assertNotEquals(0,CompilerCli.run(args,new PrintStream(stdout),new PrintStream(stderr))); assertEquals(0,stdout.size());
            }
            args[2]=null; stdout.reset(); stderr.reset();
            assertEquals(2,CompilerCli.run(args,new PrintStream(stdout),new PrintStream(stderr))); assertEquals(0,stdout.size());
            var model=model(profile); model.put("format","keir-dcm/"+profile+"-1");
            denied(compile(profile,source(profile),model),"PROFILE_UNSUPPORTED","/dcm/format");
        }
        var oldSource=map(StrictData.parse(Files.readAllBytes(Path.of("..","conformance","single-effect","source.yaml")),"/source"));
        steps(oldSource).addFirst(Map.of("id","constant","let",Map.of("literal","p","type","ProjectId")));
        denied(COMPILER.compile(StrictData.canonical(oldSource),Files.readAllBytes(Path.of("..","conformance","single-effect","dcm.json")),KeirCompiler.Environment.effect()),
                "FIELD_UNKNOWN","/source/steps/0/let/type");
    }
}
