package dev.keir.compiler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CelResourceBoundaryTest {
    private static final String EXPRESSION_PATH = "/source/steps/0/let/cel";
    private static final byte[] MODEL = StrictData.canonical(DcmTest.sample());
    private static final KeirCompiler COMPILER = new KeirCompiler();
    @TempDir Path directory;

    private static byte[] source(String expression) {
        return StrictData.canonical(Map.of("language", "keir/2", "domain",
                Map.of("id", "sample.tasks", "version", "1", "fingerprint", StrictData.digest(DcmTest.sample())),
                "entrypoint", "ClassifyTask", "name", "parser-resource-check",
                "steps", List.of(Map.of("id", "probe", "let", Map.of("cel", expression))),
                "return", Map.of("record", Map.of("classification", Map.of("literal", "DONE")))));
    }

    private static KeirCompiler.Compilation compile(String expression) {
        return COMPILER.compile(source(expression), MODEL, KeirCompiler.Environment.supported());
    }

    private static void rejected(KeirCompiler.Compilation result) {
        assertFalse(result.succeeded());
        assertNull(result.artifact()); assertNull(result.artifactDigest());
        assertEquals(1, result.diagnostics().size());
        var diagnostic = result.diagnostics().getFirst();
        assertTrue(Set.of("TYPE_MISMATCH", "BUDGET_EXCEEDED").contains(diagnostic.code()));
        assertEquals(EXPRESSION_PATH, diagnostic.path());
        assertFalse(diagnostic.message().contains("StackOverflow"));
        assertTrue(diagnostic.message().length() < 256);
    }

    @Test void malformedDeepSyntaxReturnsDiagnosticsAndTheCompilerCanStillBeUsed() {
        for (String expression : List.of("(".repeat(480), "!(".repeat(240), "!".repeat(512), "(".repeat(512))) {
            rejected(assertDoesNotThrow(() -> compile(expression)));
        }
        assertTrue(compile("true").succeeded());
    }

    @Test void callerStackExhaustionAtTheCelBoundaryDoesNotEscapeTheApi() throws Exception {
        assertTrue(compile("true").succeeded());
        byte[] input = source("(".repeat(480));
        var result = new AtomicReference<KeirCompiler.Compilation>();
        var failure = new AtomicReference<Throwable>();
        Thread caller = Thread.ofPlatform().daemon().stackSize(256 * 1024).unstarted(() -> {
            try { result.set(COMPILER.compile(input, MODEL, KeirCompiler.Environment.supported())); }
            catch (Throwable error) { failure.set(error); }
        });
        caller.start(); caller.join(5000);
        assertFalse(caller.isAlive(), "Bounded malformed syntax must settle");
        assertNull(failure.get(), "CEL resource exhaustion must be a diagnostic");
        rejected(result.get());
        assertTrue(compile("true").succeeded());
    }

    @Test void acceptedParenthesesAndAstDepthEdgesKeepTheirMeaning() {
        String parentheses = "(".repeat(254) + "true" + ")".repeat(254);
        assertEquals(512, parentheses.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(compile(parentheses).succeeded());
        String unary = "true";
        for (int i = 0; i < 15; i++) unary = "!(" + unary + ")";
        assertTrue(compile(unary).succeeded());
        var beyondDepth = compile("!(" + unary + ")");
        rejected(beyondDepth);
        assertEquals("BUDGET_EXCEEDED", beyondDepth.diagnostics().getFirst().code());
        var beyondBytes = compile(parentheses + " ");
        rejected(beyondBytes);
        assertEquals("BUDGET_EXCEEDED", beyondBytes.diagnostics().getFirst().code());
    }

    @Test void coldCliReturnsBoundedJsonDiagnosticsForMalformedParenthesesAndUnarySyntax() throws Exception {
        for (String expression : List.of("(".repeat(480), "!(".repeat(240), "!".repeat(512))) {
            var result = cli(expression, "256k");
            assertEquals(1, result.exit()); assertEquals(0, result.output().length);
            var diagnostics = assertInstanceOf(List.class, StrictData.parse(result.errors(), "/diagnostics"));
            assertEquals(1, diagnostics.size());
            var diagnostic = assertInstanceOf(Map.class, diagnostics.getFirst());
            assertEquals(Set.of("code", "path", "message"), diagnostic.keySet());
            assertTrue(Set.of("TYPE_MISMATCH", "BUDGET_EXCEEDED").contains(diagnostic.get("code")));
            assertEquals(EXPRESSION_PATH, diagnostic.get("path"));
            assertTrue(result.errors().length < 1024);
        }
        var valid = cli("(".repeat(254) + "true" + ")".repeat(254), "2m");
        assertEquals(0, valid.exit()); assertEquals(0, valid.errors().length);
        assertInstanceOf(Map.class, StrictData.parse(valid.output(), "/artifact"));
    }

    private record CliResult(int exit, byte[] output, byte[] errors) {}

    private CliResult cli(String expression, String stack) throws Exception {
        Path input = directory.resolve("source.json"), model = directory.resolve("dcm.json");
        Files.write(input, source(expression)); Files.write(model, MODEL);
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process child = new ProcessBuilder(java, "-Xmx256m", "-Xss" + stack,
                "-cp", System.getProperty("java.class.path"), CompilerCli.class.getName(),
                input.toString(), model.toString()).start();
        var output = capture(child, child.getInputStream());
        var errors = capture(child, child.getErrorStream());
        try {
            assertTrue(child.waitFor(5, TimeUnit.SECONDS), "Cold compiler exceeded its test budget");
            return new CliResult(child.exitValue(), output.get(1, TimeUnit.SECONDS), errors.get(1, TimeUnit.SECONDS));
        } finally {
            if (child.isAlive()) child.destroyForcibly();
            assertTrue(child.waitFor(5, TimeUnit.SECONDS), "Cold compiler must be reaped");
        }
    }

    private static CompletableFuture<byte[]> capture(Process child, InputStream input) {
        var result = new CompletableFuture<byte[]>();
        Thread.ofVirtual().start(() -> {
            try (input; var output = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[1024]; int count;
                while ((count = input.read(bytes)) != -1) {
                    if (output.size() + count > 16_384) {
                        child.destroyForcibly(); throw new AssertionError("Cold compiler exceeded its capture budget");
                    }
                    output.write(bytes, 0, count);
                }
                result.complete(output.toByteArray());
            } catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        return result;
    }
}
