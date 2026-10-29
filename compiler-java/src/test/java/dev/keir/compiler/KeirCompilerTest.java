package dev.keir.compiler;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import dev.cel.common.CelOptions;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.expr.CheckedExpr;
import dev.cel.runtime.CelRuntimeFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeirCompilerTest {
    private final KeirCompiler compiler = new KeirCompiler();
    private static final KeirCompiler.Environment ENV = KeirCompiler.Environment.supported();

    @Test void sampleIsDeterministicAndIndependentlyDecodable() throws Exception {
        var a = sample(); var b = sample();
        assertTrue(a.succeeded(), a.diagnostics().toString());
        assertArrayEquals(a.artifact(), b.artifact());
        assertArrayEquals(resource("artifact.json"),a.artifact());
        assertEquals(new String(resource("artifact.sha256"),StandardCharsets.UTF_8).trim(),a.artifactDigest());
        assertEquals("sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(a.artifact())), a.artifactDigest());
        var artifact = map(StrictData.parse(a.artifact(), "/artifact"));
        assertEquals(Set.of("format", "profile", "compiler", "environment", "sourceDigest", "dcmDigest", "dcm", "entrypoint", "descriptors", "expressions", "steps", "return"), artifact.keySet());
        var descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(Base64.getDecoder().decode((String) artifact.get("descriptors")));
        var file = descriptorSet.getFile(0);
        assertEquals("keir_read.proto", file.getName()); assertEquals("proto3", file.getSyntax());
        assertEquals("keir.read", file.getPackage());
        assertEquals(List.of("ClassifyInput", "ClassifyResult", "Task"), file.getMessageTypeList().stream().map(DescriptorProtos.DescriptorProto::getName).toList());
        for (var message : file.getMessageTypeList()) {
            int n = 1;
            for (var field : message.getFieldList()) {
                assertEquals(n++, field.getNumber()); assertEquals(field.getName(), field.getJsonName());
            }
        }
        var expressions = map(artifact.get("expressions"));
        assertEquals(Set.of("e0", "e1", "e2"), expressions.keySet());
        for (var expression : expressions.values()) {
            var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String) map(expression).get("checked")));
            assertTrue(checked.getTypeMapMap().containsKey(checked.getExpr().getId()));
            assertFalse(checked.getReferenceMapMap().isEmpty());
            assertEquals("keir-read/1", checked.getSourceInfo().getLocation());
        }
        assertEquals("Classification", map(expressions.get("e2")).get("resultType"));
        var call = map(((List<?>)artifact.get("steps")).getFirst());
        assertEquals("CALL",call.get("op")); assertEquals("tasks.get",call.get("capability"));
        assertEquals(Map.of("taskId",Map.of("expr","e0","type","TaskId")),call.get("args"));
        assertEquals("DONE", evaluateDecision(artifact, "COMPLETED"));
        assertEquals("PENDING", evaluateDecision(artifact, "OPEN"));
        byte[] copy = a.artifact(); copy[0] = 0; assertNotEquals(0, a.artifact()[0]);
    }

    @Test void changedConditionChangesCheckedBehaviorWithoutHostBranches() throws Exception {
        var source = source();
        map(steps(source).get(1).get("let")).put("cel", "source.status == \"OPEN\"");
        var result = compile(source, model());
        assertTrue(result.succeeded(), result.diagnostics().toString());
        var artifact = map(StrictData.parse(result.artifact(), "/artifact"));
        assertEquals("DONE", evaluateDecision(artifact, "OPEN"));
        assertEquals("PENDING", evaluateDecision(artifact, "COMPLETED"));
        assertNotEquals(sample().artifactDigest(), result.artifactDigest());
    }

    @Test void shapeAuthorityAndDataflowRejectionsNeverEmitAnArtifact() throws Exception {
        var cases = new LinkedHashMap<String, java.util.function.Consumer<Map<String,Object>>>();
        cases.put("FIELD_UNKNOWN", s -> s.put("mystery", true));
        cases.put("PROFILE_UNSUPPORTED", s -> steps(s).getFirst().put("when", Map.of("literal", true)));
        cases.put("CAPABILITY_UNKNOWN", s -> steps(s).getFirst().put("call", "tasks.missing"));
        cases.put("HOST_BINDING_OVERRIDE", s -> map(steps(s).getFirst().get("args")).put("actorId", Map.of("literal", "SECRET")));
        cases.put("DATAFLOW_UNAVAILABLE", s -> map(map(steps(s).getFirst().get("args")).get("taskId")).put("cel", "future.id"));
        cases.put("NOMINAL_MISMATCH", s -> map(steps(s).getFirst().get("args")).put("taskId", Map.of("literal", "task1")));
        cases.put("ENUM_MISMATCH", s -> map(steps(s).get(1).get("let")).put("cel", "source.status == \"UNDECLARED\""));
        cases.put("TYPE_MISMATCH", s -> map(steps(s).get(1).get("let")).put("cel", "source.missing == true"));
        cases.put("CEL_POLICY_DENIED", s -> map(steps(s).get(1).get("let")).put("cel", "source.status.startsWith(\"OPEN\")"));
        for (var c : cases.entrySet()) {
            var s = source(); c.getValue().accept(s); denied(compile(s, model()), c.getKey());
        }
        var s = source(); steps(s).get(1).put("id", "source"); denied(compile(s, model()), "DATAFLOW_UNAVAILABLE");
        s = source(); map(map(s.get("return")).get("record")).put("classification", Map.of("literal", 1L));
        denied(compile(s, model()), "PROFILE_UNSUPPORTED");
        var d = model(); map(map(d.get("entrypoints")).get("ClassifyTask")).put("grants", Map.of());
        denied(compile(source(), d), "ENTRY_GRANT_DENIED");
    }

    @Test void nominalAndEnumOverlaySurvivesLetsAndConditionals() throws Exception {
        var s = source(); var d = model();
        map(map(map(d.get("types")).get("ClassifyInput")).get("fields")).put("userId", "UserId");
        map(map(steps(s).getFirst().get("args")).get("taskId")).put("cel", "input.userId");
        denied(compile(s,d), "NOMINAL_MISMATCH");
        s = source(); steps(s).addFirst(new TreeMap<>(Map.of("id", "raw", "let", Map.of("literal", "DONE"))));
        map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "raw"));
        denied(compile(s,model()), "ENUM_MISMATCH");
        s = source(); map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "completed ? \"DONE\" : \"UNKNOWN\""));
        denied(compile(s,model()), "ENUM_MISMATCH");
        s = source(); map(steps(s).get(1).get("let")).put("cel", "source == source");
        denied(compile(s,model()), "CEL_POLICY_DENIED");
        s = source(); steps(s).addFirst(new TreeMap<>(Map.of("id", "saved", "let", Map.of("cel", "input.taskId"))));
        map(map(steps(s).get(1).get("args")).get("taskId")).put("cel", "true ? saved : input.taskId");
        assertTrue(compile(s,model()).succeeded());
        d = model(); s = source();
        map(map(map(d.get("types")).get("ClassifyInput")).get("fields")).put("classification", "Classification");
        steps(s).add(new TreeMap<>(Map.of("id", "retained", "let", Map.of("cel", "input.classification"))));
        map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "completed ? retained : \"DONE\""));
        assertTrue(compile(s,d).succeeded());
        map(d.get("types")).put("OtherClassification", Map.of("kind", "enum", "values", List.of("DONE", "PENDING")));
        map(map(map(d.get("types")).get("ClassifyInput")).get("fields")).put("other", "OtherClassification");
        map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "input.other"));
        denied(compile(s,d), "ENUM_MISMATCH");
    }

    @Test void unicodeAndEscapedControlValuesSurviveCanonicalArtifactAndCel() throws Exception {
        var d = model(); var s = source();
        map(map(d.get("types")).get("Classification")).put("values", List.of("é😀", "\t"));
        map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "completed ? \"é😀\" : \"\\t\""));
        var a = compile(s,d); var b = compile(s,d);
        assertTrue(a.succeeded(), a.diagnostics().toString()); assertArrayEquals(a.artifact(),b.artifact());
        assertTrue(new String(a.artifact(),StandardCharsets.UTF_8).contains("\\u0009"));
        var artifact = map(StrictData.parse(a.artifact(),"/artifact"));
        assertEquals("é😀",evaluateDecision(artifact,"COMPLETED"));
        assertEquals("\t",evaluateDecision(artifact,"OPEN"));
    }

    @Test void expressionAndProgramCeilingsAreInclusive() throws Exception {
        var s = source();
        map(steps(s).get(1).get("let")).put("cel", "true" + " ".repeat(508));
        assertTrue(compile(s, model()).succeeded());
        map(steps(s).get(1).get("let")).put("cel", "true" + " ".repeat(509));
        denied(compile(s, model()), "BUDGET_EXCEEDED");
        String nodes64 = "!(" + String.join(" && ", Collections.nCopies(32, "flag")) + ")";
        String nodes65 = String.join(" && ", Collections.nCopies(33, "flag"));
        var d = model(); s = programWithOutput(d, 1, nodes64);
        assertTrue(compile(s,d).succeeded());
        map(map(s.get("return")).get("record")).put("field0", Map.of("cel", nodes65));
        denied(compile(s,d), "BUDGET_EXCEEDED");
        String deep = "true";
        for (int i=0;i<15;i++) deep = "true ? true : (" + deep + ")";
        d = model(); s = programWithOutput(d,1,deep); assertTrue(compile(s,d).succeeded());
        map(map(s.get("return")).get("record")).put("field0", Map.of("cel", "true ? true : (" + deep + ")"));
        denied(compile(s,d), "BUDGET_EXCEEDED");
        d = model(); s = programWithOutput(d,8,nodes64); assertTrue(compile(s,d).succeeded());
        map(map(map(d.get("types")).get("ClassifyResult")).get("fields")).put("extra", "Boolean");
        map(map(s.get("return")).get("record")).put("extra", Map.of("cel", "flag"));
        denied(compile(s,d), "BUDGET_EXCEEDED");
        s=source(); for(int i=0;i<6;i++) steps(s).add(new TreeMap<>(Map.of("id","v"+i,"let",Map.of("literal",true))));
        assertTrue(compile(s,model()).succeeded());
        steps(s).add(new TreeMap<>(Map.of("id","v6","let",Map.of("literal",true)))); denied(compile(s,model()),"BUDGET_EXCEEDED");
        d = model(); s = programWithOutput(d,1,"\"" + "é".repeat(128) + "\"");
        map(map(map(d.get("types")).get("ClassifyResult")).get("fields")).put("field0", "String");
        assertTrue(compile(s,d).succeeded());
        map(map(s.get("return")).get("record")).put("field0",Map.of("cel","\"" + "é".repeat(128) + "x\""));
        denied(compile(s,d),"BUDGET_EXCEEDED");
        map(map(s.get("return")).get("record")).put("field0",Map.of("literal","é".repeat(128)));
        assertTrue(compile(s,d).succeeded());
        map(map(s.get("return")).get("record")).put("field0",Map.of("literal","é".repeat(128) + "x"));
        denied(compile(s,d),"BUDGET_EXCEEDED");
        map(map(s.get("return")).get("record")).put("field0",Map.of("literal",""));
        assertTrue(compile(s,d).succeeded());
        map(map(map(d.get("types")).get("ClassifyResult")).get("fields")).put("field0", "Boolean");
        map(map(s.get("return")).get("record")).put("field0",Map.of("literal",false));
        assertTrue(compile(s,d).succeeded());
    }

    @Test void callsAndRawByteCeilingsAreInclusive() throws Exception {
        var d=model(); var s=source();
        map(map(d.get("entrypoints")).get("ClassifyTask")).put("grants",new TreeMap<>(Map.of("tasks.get",4L)));
        for(int i=1;i<4;i++) steps(s).add(new TreeMap<>(Map.of("id","read"+i,"call","tasks.get","args",Map.of("taskId",Map.of("cel","input.taskId")))));
        assertTrue(compile(s,d).succeeded());
        map(d.get("capabilities")).put("tasks.more",map(d.get("capabilities")).get("tasks.get"));
        map(map(map(d.get("entrypoints")).get("ClassifyTask")).get("grants")).put("tasks.more",1L);
        steps(s).add(new TreeMap<>(Map.of("id","read4","call","tasks.more","args",Map.of("taskId",Map.of("cel","input.taskId")))));
        denied(compile(s,d),"BUDGET_EXCEEDED");
        byte[] original=resource("source.yaml");
        byte[] exact=Arrays.copyOf(original,65536); Arrays.fill(exact,original.length,exact.length,(byte)' ');
        assertTrue(compiler.compile(exact,resource("dcm.json"),ENV).succeeded());
        byte[] larger=Arrays.copyOf(exact,65537); larger[65536]=' ';
        denied(compiler.compile(larger,resource("dcm.json"),ENV),"BUDGET_EXCEEDED");
    }

    @Test void canonicalModelByteCeilingIsEnforcedAfterCompactJsonDecoding() throws Exception {
        for (int size : List.of(65_536, 65_537)) {
            var d = modelWithCanonicalBytes(size); var s = source();
            byte[] canonical = StrictData.canonical(d);
            assertEquals(size, canonical.length);
            byte[] compact = new String(canonical, StandardCharsets.UTF_8).replace("\\u000a", "\\n").getBytes(StandardCharsets.UTF_8);
            assertTrue(compact.length < 65_536);
            assertArrayEquals(canonical, StrictData.canonical(StrictData.parse(compact, "/dcm")));
            String fingerprint = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
            assertNotEquals(map(s.get("domain")).get("fingerprint"), fingerprint);
            map(s.get("domain")).put("fingerprint", fingerprint);
            var result = compiler.compile(StrictData.canonical(s), compact, ENV);
            if (size == 65_536) assertTrue(result.succeeded(), result.diagnostics().toString());
            else {
                denied(result, "BUDGET_EXCEEDED");
                assertEquals("/dcm", result.diagnostics().getFirst().path());
                assertEquals("A profile admission limit was exceeded.", result.diagnostics().getFirst().message());
            }
        }
    }
    @Test void diagnosticsAreStableSafeAndPinsFailClosed() throws Exception {
        var s=source(); map(steps(s).get(1).get("let")).put("cel","SECRET_CREDENTIAL == true");
        var a=compile(s,model()); var b=compile(s,model()); assertEquals(a.diagnostics(),b.diagnostics());
        denied(a,"DATAFLOW_UNAVAILABLE"); assertFalse(a.diagnostics().toString().contains("SECRET_CREDENTIAL"));
        assertEquals("/source/steps/1/let/cel",a.diagnostics().getFirst().path());
        s=source(); map(s.get("domain")).put("fingerprint","sha256:forged");
        denied(compiler.compile(StrictData.canonical(s),resource("dcm.json"),ENV),"PIN_MISMATCH");
        denied(compiler.compile(resource("source.yaml"),resource("dcm.json"),new KeirCompiler.Environment("other",ENV.compiler(),ENV.packages())),"PIN_MISMATCH");
        denied(compiler.compile(null,resource("dcm.json"),ENV),"SYNTAX_INVALID");
        denied(compiler.compile(resource("source.yaml"),null,ENV),"SYNTAX_INVALID");
        var d=model(); map(map(d.get("capabilities")).get("tasks.get")).put("effect","WRITE"); denied(compile(source(),d),"PROFILE_UNSUPPORTED");
        d = model(); s = source();
        var selected = new TreeMap<>(map(map(d.get("entrypoints")).get("ClassifyTask")));
        map(d.get("entrypoints")).put("AnotherRequest", selected);
        s.put("entrypoint", "AnotherRequest");
        var other = compile(s,d); assertTrue(other.succeeded());
        assertEquals("AnotherRequest",map(StrictData.parse(other.artifact(),"/artifact")).get("entrypoint"));
        selected.put("grants", Map.of());
        denied(compile(s,d), "ENTRY_GRANT_DENIED");
    }

    @Test void preflightFindsLaterAuthorityAndNestedReferencesBeforePolicyFailures() throws Exception {
        var s = source();
        steps(s).addFirst(new TreeMap<>(Map.of("id", "forbidden", "let", Map.of("cel", "\"x\".startsWith(\"x\")"))));
        map(steps(s).get(1).get("args")).put("actorId", Map.of("literal", "protected"));
        denied(compile(s,model()), "HOST_BINDING_OVERRIDE");
        s = source(); map(steps(s).get(1).get("let")).put("cel", "[missing] == [missing]");
        denied(compile(s,model()), "DATAFLOW_UNAVAILABLE");
        s = source(); map(steps(s).get(1).get("let")).put("cel", "{\"a\": missing} == {\"a\": missing}");
        denied(compile(s,model()), "DATAFLOW_UNAVAILABLE");
        byte[] raw = resource("source.yaml");
        var first = compiler.compile(("z: true\na: true\n" + new String(raw,StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),resource("dcm.json"),ENV);
        var second = compiler.compile(("a: true\nz: true\n" + new String(raw,StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),resource("dcm.json"),ENV);
        assertEquals(first.diagnostics(),second.diagnostics());
        assertEquals("/source/a",first.diagnostics().getFirst().path());
        s = source();
        steps(s).addFirst(new TreeMap<>(Map.of("id", "forbidden", "let", Map.of("cel", "\"x\".startsWith(\"x\")"))));
        map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "\"UNKNOWN\""));
        denied(compile(s,model()), "CEL_POLICY_DENIED");
        map(map(s.get("return")).get("record")).put("classification", Map.of("cel", "["));
        denied(compile(s,model()), "CEL_POLICY_DENIED");
    }

    @Test void consumerIdsMatchRuntimeAdmissionAfterIndependentModelRepinning() throws Exception {
        for (String id : List.of("consumer with spaces", "if", "x".repeat(101))) {
            var d = model(); var s = source();
            map(map(map(d.get("entrypoints")).get("ClassifyTask")).get("consumer")).put("id", id);
            byte[] modelBytes = StrictData.canonical(d);
            String fingerprint = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(modelBytes));
            map(s.get("domain")).put("fingerprint", fingerprint);
            var result = compiler.compile(StrictData.canonical(s), modelBytes, ENV);
            denied(result, "DCM_INVALID");
            assertEquals("/dcm/entrypoints/ClassifyTask/consumer/id", result.diagnostics().getFirst().path());
        }
        for (String id : List.of("sample.classification", "A" + "." + "x".repeat(98))) {
            var d = model(); var s = source();
            map(map(map(d.get("entrypoints")).get("ClassifyTask")).get("consumer")).put("id", id);
            byte[] modelBytes = StrictData.canonical(d);
            String fingerprint = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(modelBytes));
            map(s.get("domain")).put("fingerprint", fingerprint);
            var result = compiler.compile(StrictData.canonical(s), modelBytes, ENV);
            assertTrue(result.succeeded(), result.diagnostics().toString());
        }
    }

    @Test void cliHasNoArtifactOutputOnFailureAndDoesNotLeakPaths() throws Exception {
        Path dir=Files.createTempDirectory(Path.of("target"),"cli-");
        Path source=dir.resolve("source.yaml"),dcm=dir.resolve("dcm.json");
        Files.write(source,resource("source.yaml")); Files.write(dcm,resource("dcm.json"));
        var out=new ByteArrayOutputStream(); var errors=new ByteArrayOutputStream();
        assertEquals(0,CompilerCli.run(new String[]{source.toString(),dcm.toString()},new PrintStream(out),new PrintStream(errors)));
        assertArrayEquals(sample().artifact(),out.toByteArray()); assertEquals(0,errors.size());
        Files.writeString(source,"{"); out.reset(); errors.reset();
        assertEquals(1,CompilerCli.run(new String[]{source.toString(),dcm.toString()},new PrintStream(out),new PrintStream(errors)));
        assertEquals(0,out.size()); assertFalse(errors.toString().contains(dir.toString()));
        out.reset(); errors.reset();
        assertEquals(2,CompilerCli.run(new String[]{"secret-file-name",dcm.toString()},new PrintStream(out),new PrintStream(errors)));
        assertEquals(0,out.size()); assertFalse(errors.toString().contains("secret-file-name"));
    }

    private static String evaluateDecision(Map<String,Object> artifact,String status) throws Exception {
        var set=DescriptorProtos.FileDescriptorSet.parseFrom(Base64.getDecoder().decode((String)artifact.get("descriptors")));
        var descriptor=Descriptors.FileDescriptor.buildFrom(set.getFile(0),new Descriptors.FileDescriptor[0]);
        var taskType=descriptor.findMessageTypeByName("Task");
        var task=DynamicMessage.newBuilder(taskType).setField(taskType.findFieldByName("id"),"task1")
                .setField(taskType.findFieldByName("status"),status).build();
        var runtime=CelRuntimeFactory.plannerRuntimeBuilder().setOptions(CelOptions.current().enableHeterogeneousNumericComparisons(true).comprehensionMaxIterations(0).build()).addFileTypes(descriptor).build();
        var expressions=map(artifact.get("expressions"));
        var bool=runtime.createProgram(CelProtoAbstractSyntaxTree.fromCheckedExpr(checked(expressions,"e1")).getAst()).eval(Map.of("source",task));
        return (String)runtime.createProgram(CelProtoAbstractSyntaxTree.fromCheckedExpr(checked(expressions,"e2")).getAst()).eval(Map.of("completed",bool));
    }
    private static CheckedExpr checked(Map<String,Object> expressions,String id) throws Exception {
        return CheckedExpr.parseFrom(Base64.getDecoder().decode((String)map(expressions.get(id)).get("checked")));
    }
    private static Map<String,Object> programWithOutput(Map<String,Object> d,int fields,String expression) throws Exception {
        var s=source(); var output=new TreeMap<String,Object>(); var bindings=new TreeMap<String,Object>();
        for(int i=0;i<fields;i++){output.put("field"+i,"Boolean");bindings.put("field"+i,Map.of("cel",expression));}
        map(map(d.get("types")).get("ClassifyResult")).put("fields",output);
        s.put("steps",new ArrayList<>(List.of(new TreeMap<>(Map.of("id","flag","let",Map.of("literal",true))))));
        s.put("return",new TreeMap<>(Map.of("record",bindings)));return s;
    }
    private static Map<String,Object> modelWithCanonicalBytes(int size) throws Exception {
        var d = model(); var values = new ArrayList<List<String>>();
        for (int i = 0; i < 7; i++) {
            var cases = new ArrayList<String>();
            for (int j = 0; j < 16; j++) cases.add(Integer.toString(j));
            map(d.get("types")).put("Extra" + i, Map.of("kind", "enum", "values", cases));
            values.add(cases);
        }
        int remaining = size - StrictData.canonical(d).length;
        for (var cases : values) for (int i = 0; i < cases.size(); i++) {
            String prefix = cases.get(i);
            int controls = Math.min(256 - prefix.length(), remaining / 6);
            int ascii = Math.min(256 - prefix.length() - controls, remaining - controls * 6);
            cases.set(i, prefix + "\n".repeat(controls) + "x".repeat(ascii));
            remaining -= controls * 6 + ascii;
        }
        assertEquals(0, remaining);
        return d;
    }
    private KeirCompiler.Compilation compile(Map<String,Object> source,Map<String,Object> dcm) {
        map(source.get("domain")).put("fingerprint",StrictData.digest(dcm));
        return compiler.compile(StrictData.canonical(source),StrictData.canonical(dcm),ENV);
    }
    private KeirCompiler.Compilation sample() throws Exception {return compiler.compile(resource("source.yaml"),resource("dcm.json"),ENV);}
    private static Map<String,Object> source() throws Exception {return map(StrictData.parse(resource("source.yaml"),"/source"));}
    private static Map<String,Object> model() throws Exception {return map(StrictData.parse(resource("dcm.json"),"/dcm"));}
    private static byte[] resource(String name) throws Exception {try(var in=KeirCompilerTest.class.getResourceAsStream("/read-and-decide/"+name)){return Objects.requireNonNull(in).readAllBytes();}}
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object v){return (Map<String,Object>)v;}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> steps(Map<String,Object>s){return (List<Map<String,Object>>)s.get("steps");}
    private static void denied(KeirCompiler.Compilation result,String code){assertFalse(result.succeeded());assertNull(result.artifact());assertNull(result.artifactDigest());assertEquals(code,result.diagnostics().getFirst().code(),result.diagnostics().toString());}
}
