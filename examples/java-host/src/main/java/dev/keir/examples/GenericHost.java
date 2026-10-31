package dev.keir.examples;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Fixed, offline, data-only qualification host for both generic profiles. */
public final class GenericHost {
    private GenericHost() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }
    public static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=1) { err.println("Usage: GenericHost <read-or-effect-fixture-directory>"); return 2; }
        try { out.writeBytes(execute(Path.of(args[0]))); out.println(); return 0; }
        catch(Exception failure) { err.println("The selected generic simulation could not be executed."); return 1; }
    }
    public static byte[] execute(Path directory) throws IOException {
        String profile=directory.getFileName().toString();
        EffectSimulation.check(Set.of("read","effect").contains(profile),"Unknown fixture directory");
        var programs=new LinkedHashMap<String,GenericSimulation.Program>();
        for(String name:programNames(profile)) {
            String file=name.equals("main")?"artifact":"artifact-"+name;
            programs.put(name,new GenericSimulation.Program(read(directory.resolve(file+".json"),1_048_576),pin(directory.resolve(file+".sha256"))));
        }
        byte[] dcm=read(directory.resolve("dcm.json"),65_536);
        byte[] corpus=read(directory.resolve("scenarios.json"),1_048_576);
        return execute(profile,dcm,pin(directory.resolve("dcm.sha256")),programs,corpus);
    }
    static List<String> programNames(String profile) { return profile.equals("read")?List.of("main","repeated","alternate"):List.of("main","project2","title2"); }
    static byte[] execute(String profile,byte[] dcm,String pin,Map<String,GenericSimulation.Program> programs,byte[] corpus) {
        return EffectJson.encode(GenericSimulation.batch(profile,dcm,pin,programs,EffectJson.parse(corpus)));
    }
    static byte[] read(Path path,int max) throws IOException {
        try(var stream=Files.newInputStream(path)) { byte[] bytes=stream.readNBytes(max+1); GenericCapacity.capacity(bytes.length<=max); return bytes; }
    }
    private static String pin(Path path) throws IOException {
        String value=new String(read(path,73),StandardCharsets.UTF_8);
        if(value.endsWith("\r\n")) value=value.substring(0,value.length()-2);
        else if(value.endsWith("\n")) value=value.substring(0,value.length()-1);
        EffectSimulation.check(value.matches("sha256:[0-9a-f]{64}"),"Invalid independent fixture pin"); return value;
    }
}
