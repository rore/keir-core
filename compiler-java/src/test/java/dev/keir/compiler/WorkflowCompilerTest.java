package dev.keir.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowCompilerTest {
    private static final Path CORPUS = Path.of("..", "conformance", "keir-2");
    private static final KeirCompiler COMPILER = new KeirCompiler();
    private static final KeirCompiler.Environment ENV = KeirCompiler.Environment.supported();

    @Test void inventoryArtifactMatchesTheCompilerAndModelPins() throws Exception {
        byte[] model = file("dcm-inventory.json");
        for (String name : List.of("inventory", "inventory-algebra", "inventory-enum")) {
            byte[] source = file("source-" + name + ".json");
            byte[] selected = name.equals("inventory-enum") ? file("dcm-inventory-enum.json") : model;
            var result = COMPILER.compile(source, selected, ENV);
            assertTrue(result.succeeded(), result.diagnostics().toString());
            assertArrayEquals(file("artifact-" + name + ".json"), result.artifact());
            assertArrayEquals(result.artifact(), COMPILER.compile(source, selected, ENV).artifact());
            assertEquals(Files.readString(CORPUS.resolve("artifact-" + name + ".sha256")).strip(), result.artifactDigest());
        }
        assertEquals(Files.readString(CORPUS.resolve("dcm-inventory.sha256")).strip(),
                StrictData.digest(StrictData.parse(model, "/dcm")));
    }

    @Test void enumMembershipRefinesOnlyDirectDeclaredMemberLiterals() throws Exception {
        for (String needle : List.of("\"UNKNOWN\"", "input.priorityText", "input.rank",
                "(true ? \"LOW\" : \"HIGH\")")) {
            reject("inventory-enum", "inventory-enum", 0, needle + " in input.priorities", "TYPE_MISMATCH");
        }
    }

    @Test void rejectsUnsafeSelectionsAndUnsupportedOperations() throws Exception {
        reject(1, "input.warehouseId in input.labels", "TYPE_MISMATCH");
        reject(1, "\"fragile\" in input.labels", "TYPE_MISMATCH");
        reject(1, "input.enabled", "DATAFLOW_UNAVAILABLE");
        reject(1, "has(input.note) && input.enabled", "DATAFLOW_UNAVAILABLE");
        reject(1, "has(input.enabled) ? true : input.enabled", "DATAFLOW_UNAVAILABLE");
        reject(2, "input.occurredAt + duration(input.itemId)", "TYPE_MISMATCH");
        reject(2, "input.occurredAt + duration(\"31536001s\")", "TYPE_MISMATCH");
        reject(2, "timestamp(\"2026-02-30T00:00:00.000Z\")", "TYPE_MISMATCH");
        reject(1, "input.occurredAt != input.occurredAt", "CEL_POLICY_DENIED");
        reject(1, "input.occurredAt >= input.occurredAt", "CEL_POLICY_DENIED");
        reject(1, "input.labels == input.labels", "CEL_POLICY_DENIED");
    }

    @Test void oldSourceAndModelFormatsFailClosed() throws Exception {
        Path old = CORPUS.resolveSibling("keir-1");
        var source = COMPILER.compile(Files.readAllBytes(old.resolve("source.yaml")), file("dcm-inventory.json"), ENV);
        var model = COMPILER.compile(file("source-inventory.json"), Files.readAllBytes(old.resolve("dcm.json")), ENV);
        assertRejected(source, "PROFILE_UNSUPPORTED", "/source/language");
        assertRejected(model, "PROFILE_UNSUPPORTED", "/dcm/format");
    }

    @SuppressWarnings("unchecked")
    private static void reject(String sourceName, String modelName, int step, String expression, String code) throws Exception {
        var source = (Map<String,Object>)StrictData.parse(file("source-" + sourceName + ".json"), "/source");
        var steps = (List<Map<String,Object>>)source.get("steps");
        steps.get(step).put("let", Map.of("cel", expression));
        var result = COMPILER.compile(StrictData.canonical(source), file("dcm-" + modelName + ".json"), ENV);
        assertRejected(result, code, "/source/steps/" + step + "/let/cel");
    }

    private static void reject(int step, String expression, String code) throws Exception {
        reject("inventory", "inventory", step, expression, code);
    }

    private static void assertRejected(KeirCompiler.Compilation result, String code, String path) {
        assertFalse(result.succeeded());
        assertEquals(code, result.diagnostics().getFirst().code(), result.diagnostics().toString());
        assertEquals(path, result.diagnostics().getFirst().path(), result.diagnostics().toString());
    }

    private static byte[] file(String name) throws Exception {
        return Files.readAllBytes(CORPUS.resolve(name));
    }
}
