package dev.keir.runtime;

import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.keir.compiler.KeirCompiler;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static dev.keir.runtime.RuntimeData.*;

/** Opt-in bounded measurement using the actual compiler and runtime. */
public final class GuardedResultProbe {
    private static Path directory;
    private static Map<String,Object> manifest;
    private static final List<Object> blocks = new ArrayList<>();
    private static volatile State current;
    private static int completedWarmups, completedInvocations, dispatchCount, maxRetainedResults;
    private static final class State {
        final Object key = new Object();
        final int index;
        final Map<String,Object> example;
        final List<String> steps;
        int ordinal;
        State(int index, Map<String,Object> example) {
            this.index=index; this.example=example;
            var input=map(example.get("input"), "");
            steps=new ArrayList<>();
            for (int i=0; i<4; i++) if (Boolean.TRUE.equals(input.get(i==0 || i==3 ? "createNew" : "enabled"))) steps.add("a"+i);
        }
    }
    public static void main(String[] args) throws Exception {
        directory=Path.of(args[1]); manifest=map(parseJson(Files.readAllBytes(directory.resolve(args[2])),65_536),"");
        try {
            check(Runtime.version().feature()==21,"Use Java 21");
            System.out.writeBytes(canonical(args[0].equals("compile") ? compile() : run()));
        } catch (Throwable error) {
            checkpoint();
            System.out.writeBytes(canonical(Map.of("status","failed","completedWarmups",completedWarmups,
                    "completedInvocations",completedInvocations,"dispatchCount",dispatchCount,"error",String.valueOf(error.getMessage()))));
            System.exit(1);
        }
    }
    private static byte[] read(String field) throws Exception { return Files.readAllBytes(directory.resolve((String)manifest.get(field))); }
    private static void check(boolean valid,Object message) { if (!valid) throw new AssertionError(String.valueOf(message)); }
    private static int number(Map<String,Object> values,String key) { return ((Number)values.get(key)).intValue(); }
    private static Map<String,Object> compile() throws Exception {
        var result=new KeirCompiler().compile(read("source"),read("model"),KeirCompiler.Environment.supported());
        check(result.succeeded(),result.diagnostics());
        Files.write(directory.resolve((String)manifest.get("artifact")),result.artifact());
        var artifact=map(parseCanonical(result.artifact()),"");
        int aggregate=0, depth=0, maxVariables=0, maxNodes=0;
        for (Object raw : map(artifact.get("expressions"),"").values()) {
            var expression=map(raw,"");
            var checked=CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked")));
            int[] shape=shape(checked.getExpr());
            check(shape[0]==checked.getTypeMapCount(),"AST/type-map count mismatch");
            aggregate+=shape[0]; depth=Math.max(depth,shape[1]); maxNodes=Math.max(maxNodes,shape[0]);
            maxVariables=Math.max(maxVariables,map(expression.get("variables"),"").size());
        }
        var steps=list(artifact.get("steps"),"");
        long calls=steps.stream().map(row -> map(row,"")).filter(row -> "CALL".equals(row.get("op"))).count();
        return Map.of("aggregateNodes",aggregate,"depth",depth,"maxVariables",maxVariables,"maxExpressionNodes",maxNodes,
                "steps",steps.size(),"calls",calls,"artifactDigest",digest(result.artifact()));
    }
    private static int[] shape(Expr expression) {
        int count=1, depth=1;
        var children=new ArrayList<Expr>();
        if (expression.hasSelectExpr()) children.add(expression.getSelectExpr().getOperand());
        if (expression.hasCallExpr()) {
            if (expression.getCallExpr().hasTarget()) children.add(expression.getCallExpr().getTarget());
            children.addAll(expression.getCallExpr().getArgsList());
        }
        for (var child : children) { int[] nested=shape(child); count+=nested[0]; depth=Math.max(depth,1+nested[1]); }
        return new int[]{count,depth};
    }
    private static String reference(int index,String step) {
        String prefix="r"+index+"-"+step; return prefix+"r".repeat(256-prefix.length());
    }
    private static long now() { return System.nanoTime()/1_000_000; }
    private static long retained() { System.gc(); return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(); }
    private static long cpu() {
        var bean=ManagementFactory.getOperatingSystemMXBean();
        return bean instanceof com.sun.management.OperatingSystemMXBean os ? os.getProcessCpuTime() : -1;
    }
    private static void checkpoint() throws Exception {
        Files.write(directory.resolve("guarded-java-progress.json"),canonical(Map.of("completedWarmups",completedWarmups,
                "completedInvocations",completedInvocations,"dispatchCount",dispatchCount,"blocks",blocks)));
    }
    private static Map<String,Object> run() throws Exception {
        var limits=map(manifest.get("limits"),"");
        var modelBytes=read("model"); var model=map(parseCanonical(modelBytes),""); var artifact=read("artifact");
        var base=map(manifest.get("value"),"");
        int valueBytes=canonical(base).length; check(valueBytes==8192 || valueBytes==8193,"Expected boundary or boundary+1 result");
        var adapters=new LinkedHashMap<String,KeirRuntime.BoundEffectAdapter>();
        for (String id : map(model.get("capabilities"),"").keySet()) adapters.put(id,
                new KeirRuntime.BoundEffectAdapter("1","simulation",(String)map(model.get("hostKey"),"").get("type"),
                        (args,protectedArgs,remaining,context) -> {
                            var active=current;
                            check(context.requestKey()==active.key,"Original key identity changed");
                            check(context.event().equals(Map.of("id","event","source","existing")),"Unexpected event");
                            check(protectedArgs.equals(Map.of("actorId","actor")),"Unexpected protected args");
                            check(remaining>0 && remaining<=number(limits,"invocationMs"),"Unexpected invocation budget");
                            check(id.equals(list(active.example.get("calls"),"").get(active.ordinal)),"Unexpected call order");
                            check(args.equals(id.equals("records.create") ? Map.of("label","label") : Map.of("reference","existing")),"Unexpected args");
                            String step=active.steps.get(active.ordinal++); dispatchCount++;
                            var value=new LinkedHashMap<>(base); value.put("reference",reference(active.index,step));
                            return CompletableFuture.completedFuture(id.equals("records.create")
                                    ? Map.of("kind","SUCCESS","effect","CONFIRMED","value",value) : Map.of("kind","SUCCESS","value",value));
                        }));
        var binding=KeirRuntime.bindModel(modelBytes,adapters); check(binding.succeeded(),binding.diagnostic());
        var preparation=KeirRuntime.prepareProgram(artifact,binding.boundModel()); check(preparation.succeeded(),preparation.diagnostic());
        var outcomeDigests=new ArrayList<Object>();
        int warmups=number(limits,"warmups"), perBlock=number(limits,"perBlock");
        for (int index=0; index<warmups; index++) { invoke(preparation.prepared(),index,limits,outcomeDigests); completedWarmups++; }
        long baseline=retained(); checkpoint();
        for (int block=0; block<number(limits,"blocks"); block++) {
            long start=System.nanoTime(), beforeCpu=cpu();
            for (int index=0; index<perBlock; index++) { invoke(preparation.prepared(),warmups+completedInvocations,limits,outcomeDigests); completedInvocations++; }
            blocks.add(Map.of("block",block,"checked",perBlock,"elapsedUs",(System.nanoTime()-start)/1000,
                    "cpuNs",cpu()-beforeCpu,"retainedHeapBytes",retained())); checkpoint();
        }
        var result=new LinkedHashMap<String,Object>(); result.put("status","passed");
        result.put("completedWarmups",completedWarmups); result.put("completedInvocations",completedInvocations);
        result.put("dispatchCount",dispatchCount); result.put("maxRetainedResults",maxRetainedResults); result.put("resultBytes",canonical(base).length);
        result.put("artifactDigest",digest(artifact)); result.put("outcomesDigest",digest(canonical(outcomeDigests)));
        result.put("baselineHeapBytes",baseline); result.put("blocks",blocks);
        result.put("process",Map.of("heapCeilingBytes",Runtime.getRuntime().maxMemory(),"processWorkingSetSupported",false));
        return result;
    }
    private static void invoke(KeirRuntime.Prepared prepared,int index,Map<String,Object> limits,List<Object> digests) throws Exception {
        var examples=list(manifest.get("expected"),""); var example=map(examples.get(index%examples.size()),""); current=new State(index,example);
        var expected=new LinkedHashMap<>(map(example.get("outcome"),""));
        if (expected.containsKey("result")) {
            var result=new LinkedHashMap<>(map(expected.get("result"),"")); expected.put("result",result);
            if (Boolean.TRUE.equals(map(example.get("input"),"").get("createNew"))) result.put("reference",reference(index,"a3"));
        }
        var outcome=KeirRuntime.invokeBoundEffectProgram(prepared,example.get("input"),new KeirRuntime.BoundEffectInvocation(
                Map.of("actorId","actor"),new KeirRuntime.Deadline(GuardedResultProbe::now,now()+number(limits,"invocationMs")),
                current.key,Map.of("id","event","source","existing"))).toCompletableFuture().get(3,TimeUnit.SECONDS);
        check(current.ordinal==list(example.get("calls"),"").size(),"Unexpected dispatch count");
        check(Arrays.equals(canonical(expected),outcome.toJson()),"Unexpected complete outcome");
        if (outcome.classification().equals("COMPLETED")) maxRetainedResults=Math.max(maxRetainedResults,current.steps.size());
        digests.add(digest(outcome.toJson())); current=null;
    }
}
