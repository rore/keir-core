package dev.keir.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardedResultCompilerTest {
    private static final Path CORPUS = Path.of("..", "conformance", "keir-3");

    @Test void compilesExactGuardScalarSelectionsAndOptionalPresenceWithCompleteDeclarations() throws Exception {
        var compiler = new KeirCompiler();
        byte[] source = Files.readAllBytes(CORPUS.resolve("source-guarded-result.json"));
        byte[] model = Files.readAllBytes(CORPUS.resolve("dcm-guarded-result.json"));
        var result = compiler.compile(source, model, KeirCompiler.Environment.supported());
        assertTrue(result.succeeded(), result.diagnostics().toString());
        var artifact = map(StrictData.parse(result.artifact(), "/artifact"));
        assertEquals("keir-compiler-java/0.4.0", artifact.get("compiler"));
        var expressions = map(artifact.get("expressions"));
        assertEquals(Map.of("input", "RecordInput", "createNew", "Boolean", "created", "CreatedRecord"),
                map(map(expressions.get("e2")).get("variables")));
        assertEquals("RecordRef", map(expressions.get("e2")).get("resultType"));
        assertArrayEquals(result.artifact(), compiler.compile(source, model, KeirCompiler.Environment.supported()).artifact());
    }

    @Test void rejectsUnprovedAndMisleadingGuardReadsAtTheirOwningBinding() throws Exception {
        for (String expression : List.of("created.reference", "input.createNew ? created.reference : input.existingReference",
                "createNew == true ? created.reference : input.existingReference",
                "createNew && true ? created.reference : input.existingReference",
                "!createNew ? input.existingReference : created.reference",
                "false ? created.reference : input.existingReference",
                "true ? created.reference : input.existingReference", "future.reference")) {
            var source = source();
            steps(source).get(2).put("let", Map.of("cel", expression));
            rejected(compile(source), "DATAFLOW_UNAVAILABLE", "/source/steps/2/let/cel");
        }
    }

    @Test void rejectsGuardedRecordsListsAndUnprovedOptionalPresence() throws Exception {
        for (String expression : List.of("created", "createNew ? (\"kept\" in created.labels) : false")) {
            var source = source();
            steps(source).get(2).put("let", Map.of("cel", expression));
            rejected(compile(source), "CEL_POLICY_DENIED", "/source/steps/2/let/cel");
        }
        for (String expression : List.of("has(created.note)", "createNew ? created.note : \"\"",
                "createNew && has(created.note) ? created.note : \"\"")) {
            var source = source();
            steps(source).get(2).put("let", Map.of("cel", expression));
            rejected(compile(source), "DATAFLOW_UNAVAILABLE", "/source/steps/2/let/cel");
        }
    }

    @Test void acceptsNestedExactIdentifierProofsWithoutPropagatingAliases() throws Exception {
        for (String expression : List.of("(createNew) ? created.reference : input.existingReference",
                "createNew ? (input.enabled ? created.reference : created.reference) : input.existingReference")) {
            var source = source();
            steps(source).get(2).put("let", Map.of("cel", expression));
            accepted(compile(source));
        }
        var source = source();
        steps(source).add(1, new TreeMap<>(Map.of("id", "alias", "let", Map.of("cel", "createNew"))));
        steps(source).get(3).put("let", Map.of("cel", "alias ? created.reference : input.existingReference"));
        rejected(compile(source), "DATAFLOW_UNAVAILABLE", "/source/steps/3/let/cel");
        steps(source).get(2).put("when", "alias");
        // Every guarded access must now use the CALL's new exact guard, not its origin.
        var fields = map(map(source.get("return")).get("record"));
        for (Object binding : fields.values()) {
            var value = map(binding);
            value.put("cel", ((String)value.get("cel")).replace("createNew", "alias"));
        }
        accepted(compile(source));
    }

    @Test void preservesNominalEnumAndScalarBranchDiagnostics() throws Exception {
        for (String fallback : List.of("input.label", "\"untyped\"")) {
            var source = source();
            steps(source).get(2).put("let", Map.of("cel", "createNew ? created.reference : " + fallback));
            rejected(compile(source), "NOMINAL_MISMATCH", "/source/steps/2/let/cel");
        }
        var source = source();
        map(map(source.get("return")).get("record")).put("state", Map.of("cel", "createNew ? created.state : \"OTHER\""));
        rejected(compile(source), "ENUM_MISMATCH", "/source/return/record/state/cel");
        source = source();
        steps(source).get(2).put("let", Map.of("cel", "createNew ? created.flag : \"wrong\""));
        rejected(compile(source), "TYPE_MISMATCH", "/source/steps/2/let/cel");
    }

    @Test void checksArgumentsAndHandlersBeforeAddingTheirOwnResult() throws Exception {
        var source = source();
        steps(source).add(new TreeMap<>(Map.of("id", "later", "call", "records.lookup", "when", "createNew",
                "args", Map.of("reference", Map.of("cel", "created.reference")))));
        rejected(compile(source), "DATAFLOW_UNAVAILABLE", "/source/steps/3/args/reference/cel");
        steps(source).getLast().put("args", Map.of("reference", Map.of("cel", "createNew ? created.reference : input.existingReference")));
        steps(source).getLast().put("onError", Map.of("DENIED", source.get("return")));
        accepted(compile(source));
        source = source();
        steps(source).get(1).put("onError", Map.of("DENIED", source.get("return")));
        rejected(compile(source), "DATAFLOW_UNAVAILABLE", "/source/steps/1/onError/DENIED/record/at/cel");
        source = source();
        steps(source).get(1).put("args", Map.of("label", Map.of("cel", "createNew ? created.note : \"\"")));
        rejected(compile(source), "DATAFLOW_UNAVAILABLE", "/source/steps/1/args/label/cel");
    }

    @Test void falseGuardsDoNotDiscountPossibleWritesOrCalls() throws Exception {
        var source = source();
        steps(source).getFirst().put("let", Map.of("literal", false));
        steps(source).add(new TreeMap<>(Map.of("id", "second", "call", "records.create", "when", "createNew",
                "args", Map.of("label", Map.of("cel", "unavailable")))));
        rejected(compile(source), "BUDGET_EXCEEDED", "/source/steps/3/call");
        source = source();
        for (int i = 0; i < 4; i++) steps(source).add(new TreeMap<>(Map.of("id", "read" + i,
                "call", "records.lookup", "when", "createNew", "args", Map.of("reference", Map.of("cel", "input.existingReference")))));
        rejected(compile(source), "BUDGET_EXCEEDED", "/source/steps");
    }

    private static Map<String,Object> source() throws Exception {
        return map(StrictData.parse(Files.readAllBytes(CORPUS.resolve("source-guarded-result.json")), "/source"));
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> steps(Map<String,Object> source) { return (List<Map<String,Object>>)source.get("steps"); }
    private static KeirCompiler.Compilation compile(Map<String,Object> source) throws Exception {
        return new KeirCompiler().compile(StrictData.canonical(source), Files.readAllBytes(CORPUS.resolve("dcm-guarded-result.json")),
                KeirCompiler.Environment.supported());
    }
    private static void accepted(KeirCompiler.Compilation result) { assertTrue(result.succeeded(), result.diagnostics().toString()); }
    private static void rejected(KeirCompiler.Compilation result, String code, String path) {
        assertFalse(result.succeeded());
        assertEquals(code, result.diagnostics().getFirst().code(), result.diagnostics().toString());
        assertEquals(path, result.diagnostics().getFirst().path());
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> map(Object value) { return (Map<String,Object>) value; }
}
