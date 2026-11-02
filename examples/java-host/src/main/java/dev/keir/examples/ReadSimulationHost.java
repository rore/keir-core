package dev.keir.examples;

import dev.keir.runtime.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static dev.keir.examples.EffectSimulation.*;

/** Fixed code-owned offline fixtures; command arguments belong to the operator. */
public final class ReadSimulationHost {
    private ReadSimulationHost() {}
    public static void main(String[] args) { System.exit(run(args,System.out,System.err)); }
    public static int run(String[] args,PrintStream out,PrintStream err) {
        if(args.length!=3) { err.println("Usage: ReadSimulationHost <bundle-directory> <fixture> <case-file>"); return 2; }
        byte[] report;
        try {
            check(Set.of("normal","denied","consumer-throws","consumer-unlisted","consumer-wrong").contains(args[1]),"Unknown fixed fixture");
            Path bundle=Path.of(args[0]); var selection=object(EffectJson.parse(GenericHost.read(bundle.resolve("selection.json"),4096)));
            shape(selection,"modelDigest","artifactDigest","compiler","entrypoint","consumer");
            var selected=new ReadSimulation.Selection(GenericHost.read(bundle.resolve("dcm.json"),65_536),GenericHost.read(bundle.resolve("artifact.json"),1_048_576),
                (String)selection.get("modelDigest"),(String)selection.get("artifactDigest"),(String)selection.get("compiler"),(String)selection.get("entrypoint"),object(selection.get("consumer")));
            var allowed=List.<Map<String,Object>>of(Map.of("status","NOT_DISPLAYED"),Map.of("status","DISPLAYED","code","DONE"),Map.of("status","DISPLAYED","code","PENDING"),Map.of("status","DISPLAYED","code","NEEDS_ATTENTION"));
            var fixture=new ReadSimulation.Fixture("simulation.example",Map.of("actorId","user1"),new KeirRuntime.Deadline(()->0,1000),
                request->CompletableFuture.completedFuture(!args[1].equals("denied")),outcome->{
                    if(args[1].equals("consumer-throws")) throw new IllegalStateException("Private consumer failure");
                    if(args[1].equals("consumer-unlisted")) return Map.of("status","DISPLAYED","code","UNLISTED");
                    if(args[1].equals("consumer-wrong")) return Map.of("status","DISPLAYED","code","DONE");
                    return "COMPLETED".equals(outcome.get("classification")) ? Map.of("status","DISPLAYED","code",object(outcome.get("result")).get("classification")) : Map.of("status","NOT_DISPLAYED");
                },allowed,(capability,business,response)->!capability.equals("tasks.get") || !"SUCCESS".equals(response.get("kind")) || Objects.equals(object(response.get("value")).get("id"),business.get("taskId")));
            report=ReadSimulation.run(selected,fixture,GenericHost.read(Path.of(args[2]),65_536));
        } catch(Exception failure) { report=ReadSimulation.run(null,null,new byte[0]); }
        out.writeBytes(report); out.println(); return 0;
    }
}
