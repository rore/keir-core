package dev.keir.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Read2StructuralContractsTest {
    private static final Path FIXTURES = Path.of("..", "conformance", "read2-structural");
    private static byte[] file(Path path) throws Exception { return Files.readAllBytes(path); }
    private static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    private static Object json(Path path) throws Exception { return StrictData.parse(file(path), "/fixture"); }

    @Test void jsonSourceExamplesMatchStrictYamlAndUnchangedCompilerBytes() throws Exception {
        var compiler = new KeirCompiler();
        var model = file(Path.of("..", "conformance", "generic-hooks", "read", "dcm.json"));
        for (String suffix : List.of("", "-repeated", "-alternate", "-literals")) {
            byte[] source = file(FIXTURES.resolve("source" + suffix + ".json"));
            Path artifact = suffix.equals("-literals") ? FIXTURES.resolve("artifact-literals.json")
                    : Path.of("..", "conformance", "generic-hooks", "read", "artifact" + suffix + ".json");
            if (!suffix.equals("-literals")) {
                byte[] yaml = file(Path.of("..", "conformance", "generic-hooks", "read", "source" + suffix + ".yaml"));
                assertArrayEquals(StrictData.canonical(StrictData.parse(yaml,"/source")), StrictData.canonical(StrictData.parse(source,"/source")));
            }
            var result = compiler.compile(source, model, KeirCompiler.Environment.read2());
            assertTrue(result.succeeded(), result.diagnostics().toString());
            assertArrayEquals(file(artifact), result.artifact());
            assertEquals(Files.readString(artifact.resolveSibling(artifact.getFileName().toString().replace(".json", ".sha256"))).trim(), result.artifactDigest());
            assertEquals(StrictData.digest(StrictData.parse(source,"/source")), map(StrictData.parse(result.artifact(),"/artifact")).get("sourceDigest"));
        }
    }

    @Test void structurallyValidSourceStillRequiresNativeSemantics() throws Exception {
        var compiler = new KeirCompiler();
        byte[] model = file(Path.of("..", "conformance", "generic-hooks", "read", "dcm.json"));
        for (Object value : (List<?>)json(FIXTURES.resolve("residuals.json"))) {
            var row = map(value);
            if ("bind".equals(row.get("stage"))) {
                byte[] dcm = row.containsKey("raw") ? ((String)row.get("raw")).getBytes(StandardCharsets.UTF_8) : StrictData.canonical(row.get("document"));
                var result = compiler.compile(file(FIXTURES.resolve("source.json")),dcm,KeirCompiler.Environment.read2());
                var expected = map(row.get("compilerExpected"));
                assertFalse(result.succeeded(), (String)row.get("id")); assertNull(result.artifact());
                assertEquals(expected.get("code"),result.diagnostics().getFirst().code(),(String)row.get("id"));
                assertEquals(expected.get("path"),result.diagnostics().getFirst().path(),(String)row.get("id"));
                continue;
            }
            if (!"compile".equals(row.get("stage"))) continue;
            var result = compiler.compile(StrictData.canonical(row.get("document")), model, KeirCompiler.Environment.read2());
            var expected = map(row.get("expected"));
            assertFalse(result.succeeded(), (String)row.get("id"));
            assertNull(result.artifact());
            assertEquals(expected.get("code"), result.diagnostics().getFirst().code(), (String)row.get("id"));
            assertEquals(expected.get("path"), result.diagnostics().getFirst().path(), (String)row.get("id"));
        }
    }

    @Test void schemaEnvironmentSnapshotTracksActualCompilerConfiguration() throws Exception {
        var schema = map(json(Path.of("..", "schemas", "read-2", "artifact.schema.json")));
        var properties = map(map(map(schema.get("properties")).get("environment")).get("properties"));
        var expected = new TreeMap<String,String>();
        properties.forEach((key,value) -> expected.put(key,(String)map(value).get("const")));
        assertEquals(KeirCompiler.Environment.read2().packages(), expected);
        assertEquals("urn:keir:schema:read-2:artifact:1",schema.get("$id"));
    }
}
