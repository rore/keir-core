package dev.keir.runtime;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;

/** Test-only transport; the independently packaged host supplies the executor. */
public final class GenericScenarioDriver {
    private GenericScenarioDriver() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }
    static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=0) { err.println("The generic scenario driver takes no arguments."); return 2; }
        try {
            var execute=Class.forName("dev.keir.examples.GenericHost").getMethod("execute",Path.class);
            var rows=new LinkedHashMap<String,Object>();
            Path resources=Path.of(GenericScenarioDriver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for(String profile:List.of("read","effect")) {
                rows.put(profile,RuntimeData.parseJson((byte[])execute.invoke(null,resources.resolve("generic-hooks").resolve(profile)),1_048_576));
            }
            out.writeBytes(RuntimeData.canonical(rows)); out.println(); return 0;
        } catch(Exception failure) { err.println("The generic scenario batch could not be executed."); return 1; }
    }
}
