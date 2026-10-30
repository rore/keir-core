package dev.keir.compiler;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Standalone process boundary: bounded input/output, no child processes or host authority. */
public final class CompilerCli {
    private CompilerCli() {}

    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }

    static int run(String[] args, PrintStream output, PrintStream errors) {
        boolean effect = args.length == 3 && "--profile=keir-effect/1".equals(args[2]);
        if (args.length != 2 && !effect) return error(errors, "SYNTAX_INVALID", "/source");
        byte[] source, model;
        try { source = read(args[0]); }
        catch (IOException | RuntimeException e) { return error(errors, "SYNTAX_INVALID", "/source"); }
        try { model = read(args[1]); }
        catch (IOException | RuntimeException e) { return error(errors, "SYNTAX_INVALID", "/dcm"); }
        var result = new KeirCompiler().compile(source, model,
                effect ? KeirCompiler.Environment.effect() : KeirCompiler.Environment.supported());
        if (result.succeeded()) {
            output.writeBytes(result.artifact());
            return 0;
        }
        errors.writeBytes(StrictData.canonical(result.diagnostics().stream()
                .map(d -> Map.of("code", d.code(), "path", d.path(), "message", d.message())).toList()));
        errors.println();
        return 1;
    }

    private static byte[] read(String path) throws IOException {
        try (var input = Files.newInputStream(Path.of(path))) { return input.readNBytes(65_537); }
    }

    private static int error(PrintStream errors, String code, String path) {
        errors.writeBytes(StrictData.canonical(List.of(Map.of("code", code, "path", path,
                "message", "The compiler requires two readable bounded document files."))));
        errors.println();
        return 2;
    }
}
