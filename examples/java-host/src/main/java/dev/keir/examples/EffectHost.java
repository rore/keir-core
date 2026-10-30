package dev.keir.examples;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/** Packaged simulation of the admitted post-commit effect; it does not activate real adapters. */
public final class EffectHost {
    private EffectHost() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }
    public static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=3) { err.println("Usage: java -cp keir-java-host.jar dev.keir.examples.EffectHost <artifact> <artifact-sha256> <dcm-sha256>"); return 2; }
        try {
            byte[] artifact=JavaHost.readArtifact(Path.of(args[0]));
            byte[] output=EffectJson.encode(execute(artifact,args[1],args[2],scenarios()));
            out.writeBytes(output); out.println(); return 0;
        } catch(Exception failure) { err.println("The selected effect simulation could not be executed."); return 1; }
    }
    public static List<Map<String,Object>> execute(byte[] artifact,String artifactPin,String dcmPin,byte[] corpus) {
        return EffectSimulation.batch(artifact,artifactPin,dcmPin,EffectJson.parse(corpus));
    }
    static byte[] scenarios() throws IOException {
        try(var input=EffectHost.class.getResourceAsStream("/single-effect/scenarios.json")) {
            if(input==null) throw new IOException("Missing scenario resource");
            byte[] bytes=input.readNBytes(1_048_577);
            if(bytes.length>1_048_576) throw new IOException("Scenario batch exceeds bound"); return bytes;
        }
    }
}
