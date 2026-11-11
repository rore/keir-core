package dev.keir.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreOperationCompilerTest {
    private static final KeirCompiler COMPILER = new KeirCompiler();
    private static final KeirCompiler.Environment ENV = KeirCompiler.Environment.supported();

    @Test void reproducesSharedGuardAndMixedCatalogArtifactsWithTheirExactPins() throws Exception {
        var corpus = Path.of("..", "conformance", "keir-3");
        for (String name : List.of("guard", "guard-input", "guard-handler", "guard-reversed", "mixed-event", "mixed-guard")) {
            String modelName = switch (name) {
                case "guard-reversed" -> "dcm-guard-reversed";
                case "mixed-event", "mixed-guard" -> "dcm-mixed";
                default -> "dcm-guard";
            };
            byte[] model = Files.readAllBytes(corpus.resolve(modelName + ".json"));
            String modelDigest = Files.readString(corpus.resolve(modelName + ".sha256")).strip();
            assertEquals(modelDigest, StrictData.digest(StrictData.parse(model, "/dcm")), name);
            byte[] source = Files.readAllBytes(corpus.resolve("source-" + name + ".json"));
            var result = COMPILER.compile(source, model, ENV);
            assertTrue(result.succeeded(), result.diagnostics().toString());
            assertArrayEquals(Files.readAllBytes(corpus.resolve("producers/0.4.0/artifact-" + name + ".json")), result.artifact(), name);
            assertEquals(Files.readString(corpus.resolve("producers/0.4.0/artifact-" + name + ".sha256")).strip(), result.artifactDigest(), name);
            var artifact = map(StrictData.parse(result.artifact(), "/artifact"));
            assertEquals(modelDigest, map(artifact.get("modelRef")).get("digest"), name);
        }
    }

    @Test void acceptsOnlyTheTwoCompleteLifecycleTuples() {
        for (String trigger : List.of("EVENT", "REQUEST"))
            for (String placement : List.of("AFTER_COMMIT", "BEFORE_COMMIT"))
                for (String consumer : List.of("EVENT_REPORT", "DECISION")) {
                    var model = model();
                    entry(model).put("trigger", trigger);
                    entry(model).put("placement", placement);
                    map(entry(model).get("consumer")).put("kind", consumer);
                    boolean event = trigger.equals("EVENT") && placement.equals("AFTER_COMMIT")
                            && consumer.equals("EVENT_REPORT");
                    boolean guard = trigger.equals("REQUEST") && placement.equals("BEFORE_COMMIT")
                            && consumer.equals("DECISION");
                    if (event || guard) {
                        var parsed = Dcm.parse(model).entrypoints().get("BeforeOperation");
                        assertEquals(trigger, parsed.trigger());
                        assertEquals(event ? 1 : 0, parsed.effectGrant());
                    } else assertEquals("DCM_INVALID", assertThrows(CompileFailure.class,
                            () -> Dcm.parse(model)).code);
                }
    }

    @Test void decisionEnumRequiresExactlyAllowAndDenyAndRetainsTheirOrderInTheDigest() {
        var model = model();
        String original = StrictData.digest(model);
        map(map(model.get("types")).get("Decision")).put("values", List.of("DENY", "ALLOW"));
        Dcm.parse(model);
        assertNotEquals(original, StrictData.digest(model));
        for (List<String> values : List.of(List.<String>of(), List.of("ALLOW"), List.of("DENY"),
                List.of("ALLOW", "ALLOW"), List.of("DENY", "DENY"), List.of("ALLOW", "DENY", "OTHER"),
                List.of("ALLOW", "OTHER"))) {
            var invalid = model();
            map(map(invalid.get("types")).get("Decision")).put("values", values);
            assertEquals("DCM_INVALID", assertThrows(CompileFailure.class, () -> Dcm.parse(invalid)).code);
        }
    }

    @Test void decisionIsRequiredAndOtherOutputFieldsMustBeScalars() {
        for (Object type : List.of("String", "Boolean", "TaskId", "TaskStatus",
                Map.of("type", "Decision", "optional", true))) {
            var model = model();
            output(model).put("decision", type);
            invalidOutput(model);
        }
        var model = model();
        output(model).remove("decision");
        output(model).put("message", "String");
        invalidOutput(model);
        var nested = model();
        output(nested).put("nested", "Task");
        assertEquals("DCM_INVALID", assertThrows(CompileFailure.class, () -> Dcm.parse(nested)).code);
    }

    @Test void decisionAllowsRequiredAndOptionalBoundedScalarFieldsButNoList() {
        var model = model();
        output(model).put("message", "String");
        output(model).put("optionalMessage", Map.of("type", "String", "optional", true));
        output(model).put("enabled", "Boolean");
        output(model).put("id", "TaskId");
        output(model).put("at", "Timestamp");
        Dcm.parse(model);
        map(model.get("types")).put("Messages", Map.of("kind", "list", "element", "String", "maxItems", 2));
        output(model).put("messages", "Messages");
        invalidOutput(model);
    }

    @Test void decisionEffectGrantDefaultsToZeroAndRejectsEveryOtherValue() {
        assertEquals(0, Dcm.parse(model()).entrypoints().get("BeforeOperation").effectGrant());
        assertEquals(1, Dcm.parse(model()).entrypoints().get("ClassifyTask").effectGrant());
        for (Object value : List.of((byte) 0, (short) 0, 0, 0L, java.math.BigInteger.ZERO)) {
            var model = model();
            entry(model).put("effectGrant", value);
            assertEquals(0, Dcm.parse(model).entrypoints().get("BeforeOperation").effectGrant());
        }
        for (Object value : Arrays.asList(null, 1, -1, 2, 0.0, "0", false)) {
            var model = model();
            entry(model).put("effectGrant", value);
            var error = assertThrows(CompileFailure.class, () -> Dcm.parse(model));
            assertEquals("DCM_INVALID", error.code);
            assertEquals("/dcm/entrypoints/BeforeOperation/effectGrant", error.path);
        }
    }

    @Test void compilesLiteralInputAndReadDecisionsWithTheCurrentExactIdentity() {
        for (List<String> order : List.of(List.of("ALLOW", "DENY"), List.of("DENY", "ALLOW"))) {
            var model = model();
            map(map(model.get("types")).get("Decision")).put("values", order);
            for (String value : List.of("ALLOW", "DENY")) {
                var source = source();
                map(map(source.get("return")).get("record")).put("decision", Map.of("literal", value));
                var artifact = compiled(source, model);
                assertEquals("keir-artifact/3", artifact.get("format"));
                assertEquals("keir-compiler-java/0.4.0", artifact.get("compiler"));
                assertEquals("keir-dcm/3", map(artifact.get("modelRef")).get("format"));
                assertEquals("keir-record-descriptors/2", map(artifact.get("modelRef")).get("descriptorRecipe"));
                assertEquals(ENV.packages(), artifact.get("environment"));
            }
            compiled(source(), model);
            var read = source();
            steps(read).addFirst(new TreeMap<>(Map.of("id", "fact", "call", "host.read", "args", Map.of())));
            steps(read).get(1).put("let", Map.of("cel", "fact.restricted"));
            compiled(read, model);
        }
    }

    @Test void optionalMessagesCompileAndReturnValidationStillFailsClosed() {
        var model = model();
        output(model).put("message", Map.of("type", "String", "optional", true));
        var source = source();
        compiled(source, model);
        var fields = map(map(source.get("return")).get("record"));
        fields.put("message", Map.of("literal", "Customer-authored plain text"));
        compiled(source, model);
        fields.put("message", Map.of("literal", true));
        denied(compile(source, model), "TYPE_MISMATCH", "/source/return/record/message/literal");
        fields.remove("message");
        fields.put("decision", Map.of("literal", "OTHER"));
        denied(compile(source, model), "ENUM_MISMATCH", "/source/return/record/decision/literal");
    }

    @Test void rejectsEveryPotentialWriteIncludingAFalseGuard() {
        for (boolean guarded : List.of(false, true)) for (boolean explicitGrant : List.of(false, true)) {
            var model = model();
            if (explicitGrant) entry(model).put("effectGrant", 0L);
            var source = source();
            steps(source).getFirst().put("let", Map.of("literal", false));
            var write = new TreeMap<String,Object>(Map.of("id", "written", "call", "host.write", "args", Map.of()));
            if (guarded) write.put("when", "restricted");
            steps(source).add(write);
            denied(compile(source, model), "ENTRY_GRANT_DENIED", "/source/steps/1/call");
        }
    }

    @Test void rejectsOldModelsAndProducerSelectionRatherThanStampingAnOldIdentity() {
        var model = model();
        model.put("format", "keir-dcm/2");
        denied(compile(source(), model), "PROFILE_UNSUPPORTED", "/dcm/format");
        var source = source();
        model = model();
        map(source.get("domain")).put("fingerprint", StrictData.digest(model));
        denied(COMPILER.compile(StrictData.canonical(source), StrictData.canonical(model),
                new KeirCompiler.Environment("keir-compiler-java/0.2.0", ENV.packages())), "PIN_MISMATCH", "/source");
    }

    private static void invalidOutput(Map<String,Object> model) {
        var error = assertThrows(CompileFailure.class, () -> Dcm.parse(model));
        assertEquals("DCM_INVALID", error.code);
        assertEquals("/dcm/entrypoints/BeforeOperation/output", error.path);
    }

    private static void denied(KeirCompiler.Compilation result, String code, String path) {
        assertFalse(result.succeeded());
        assertEquals(code, result.diagnostics().getFirst().code(), result.diagnostics().toString());
        assertEquals(path, result.diagnostics().getFirst().path());
    }

    private static Map<String,Object> compiled(Map<String,Object> source, Map<String,Object> model) {
        var result = compile(source, model);
        assertTrue(result.succeeded(), result.diagnostics().toString());
        assertArrayEquals(result.artifact(), compile(source, model).artifact());
        return map(StrictData.parse(result.artifact(), "/artifact"));
    }

    private static KeirCompiler.Compilation compile(Map<String,Object> source, Map<String,Object> model) {
        map(source.get("domain")).put("fingerprint", StrictData.digest(model));
        return COMPILER.compile(StrictData.canonical(source), StrictData.canonical(model), ENV);
    }

    private static Map<String,Object> model() {
        var model = DcmTest.sample();
        var types = map(model.get("types"));
        types.put("Decision", new TreeMap<>(Map.of("kind", "enum", "values", List.of("ALLOW", "DENY"))));
        types.put("DecisionInput", Map.of("kind", "record", "fields", Map.of("restricted", "Boolean")));
        types.put("DecisionResult", new TreeMap<>(Map.of("kind", "record", "fields", new TreeMap<>(Map.of("decision", "Decision")))));
        var entries = map(model.get("entrypoints"));
        entries.put("BeforeOperation", new TreeMap<>(Map.of("input", "DecisionInput", "output", "DecisionResult",
                "trigger", "REQUEST", "placement", "BEFORE_COMMIT", "event", Map.of("idType", "EventId", "sourceType", "EventSource"),
                "consumer", new TreeMap<>(Map.of("id", "sample.decision", "version", "1", "kind", "DECISION")))));
        for (String effect : List.of("READ", "WRITE")) map(model.get("capabilities")).put("host." + effect.toLowerCase(java.util.Locale.ROOT),
                Map.of("version", "1", "parameters", Map.of(), "result", "DecisionInput", "effect", effect, "errors", List.of()));
        return model;
    }

    private static Map<String,Object> source() {
        return new TreeMap<>(Map.of("language", "keir/2", "domain", new TreeMap<>(Map.of("id", "sample.tasks", "version", "1")),
                "entrypoint", "BeforeOperation", "name", "decide-operation",
                "steps", new ArrayList<>(List.of(new TreeMap<>(Map.of("id", "restricted", "let", Map.of("cel", "input.restricted"))))),
                "return", new TreeMap<>(Map.of("record", new TreeMap<>(Map.of("decision", Map.of("cel", "restricted ? \"DENY\" : \"ALLOW\"")))))));
    }

    private static Map<String,Object> entry(Map<String,Object> model) { return map(map(model.get("entrypoints")).get("BeforeOperation")); }
    private static Map<String,Object> output(Map<String,Object> model) { return map(map(map(model.get("types")).get("DecisionResult")).get("fields")); }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return (Map<String,Object>) value; }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> steps(Map<String,Object> source) { return (List<Map<String,Object>>) source.get("steps"); }
}
