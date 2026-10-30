package dev.keir.runtime;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Test-only transport; the combined verifier supplies the independently packaged host. */
public final class EffectScenarioDriver {
    private EffectScenarioDriver() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }
    static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=0) { err.println("The effect scenario driver takes no arguments."); return 2; }
        try {
            // Avoid a runtime-to-host build dependency; the host JAR is already on the verifier's classpath.
            var execute=Class.forName("dev.keir.examples.EffectHost").getMethod("execute",byte[].class,String.class,String.class,byte[].class);
            Object rows=execute.invoke(null,ScenarioDriver.resource("/single-effect/artifact.json"),
                    new String(ScenarioDriver.resource("/single-effect/artifact.sha256"),StandardCharsets.UTF_8).trim(),
                    new String(ScenarioDriver.resource("/single-effect/dcm.sha256"),StandardCharsets.UTF_8).trim(),
                    ScenarioDriver.resource("/single-effect/scenarios.json"));
            byte[] output=RuntimeData.canonical(rows); out.writeBytes(output); out.println(); return 0;
        } catch(ReflectiveOperationException|java.io.IOException|RuntimeException failure) {
            err.println("The effect scenario batch could not be executed."); return 1;
        }
    }
}
