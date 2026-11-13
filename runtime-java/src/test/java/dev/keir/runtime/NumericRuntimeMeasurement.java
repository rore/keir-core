package dev.keir.runtime;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import static dev.keir.runtime.RuntimeData.*;

/** Finite hosted evidence only. Uses the production runtime, not the component evaluator. */
public final class NumericRuntimeMeasurement {
    private static void check(boolean value) { if (!value) throw new AssertionError("Numeric runtime measurement mismatch"); }
    private static long heap() { return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static Map<String,Object> measure(Supplier<Object> action,Consumer<Object> verify) {
        for (int i=0;i<100;i++) verify.accept(action.get());
        long[] samples=new long[1000]; long start=System.nanoTime();
        for (int i=0;i<samples.length;i++) {
            long at=System.nanoTime(); Object value=action.get(); samples[i]=System.nanoTime()-at; verify.accept(value);
        }
        long wall=System.nanoTime()-start; Arrays.sort(samples);
        return Map.of("calls",1100,"samples",1000,"warmups",100,"p50Ns",samples[499],"p95Ns",samples[949],
                "maxNs",samples[999],"wallNs",wall,"perSecond",1_000_000_000_000L/Math.max(1,wall));
    }
    private static Map<String,Object> input(Map<String,Object> row,int i,boolean failure) {
        if (failure) { var spec=map(row.get("failure"),""); return Map.of("owner","owner"+i,"value",spec.get("value"),"divisor",spec.get("divisor")); }
        return Map.of("owner","owner"+i,"value",list(row.get("values"),"").get(i%2),"divisor",row.get("divisor"));
    }
    private static Map<String,Object> expected(Map<String,Object> row,int i,boolean failure) {
        var spec=map(row.get("failure"),""); var trace=new ArrayList<Object>();
        event(trace,"INVOCATION_START",Map.of());
        if (!failure || !"INPUT_INVALID".equals(spec.get("code"))) for (String id:List.of("a","b","c","d")) {
            event(trace,"STEP_START",Map.of("step",id)); if (failure && id.equals("c")) break;
            event(trace,"VALUE_BOUND",Map.of("step",id));
        }
        if (!failure) event(trace,"RETURNED",Map.of());
        event(trace,"TERMINATED",failure ? Map.of("code",spec.get("code")) : Map.of());
        var outcome=new LinkedHashMap<String,Object>(Map.of("classification",failure ? "FAILED" : "COMPLETED","effects",List.of(),"trace",trace));
        if (failure) outcome.put("error",Map.of("code",spec.get("code"),"path",spec.get("path")));
        else outcome.put("result",Map.of("owner","owner"+i,"value",list(row.get("expected"),"").get(i%2)));
        return outcome;
    }
    private static void event(List<Object> trace,String name,Map<String,Object> fields) {
        var row=new LinkedHashMap<>(fields); row.put("sequence",(long)trace.size()); row.put("event",name); trace.add(row);
    }
    private static KeirRuntime.Outcome invoke(KeirRuntime.Prepared prepared,Map<String,Object> row,int i,boolean failure) {
        return invokeInput(prepared,input(row,i,failure),i);
    }
    private static KeirRuntime.Outcome invokeInput(KeirRuntime.Prepared prepared,Object input,int i) {
        return KeirRuntime.invokeBoundEffectProgram(prepared,input,new KeirRuntime.BoundEffectInvocation(
                Map.of(),new KeirRuntime.Deadline(() -> 0L,10_000L),new Object(),Map.of("id","event1","source","owner"+i)))
                .toCompletableFuture().join();
    }
    public static void main(String[] args) throws Exception {
        System.out.println("NUMERIC_READY");
        byte[] fixture=Files.readAllBytes(Path.of(args[0])); var document=map(parseJson(fixture,1_048_576),"");
        var reports=new ArrayList<Object>(); var threadBean=ManagementFactory.getThreadMXBean();
        for (Object raw:list(document.get("cases"),"")) {
            var row=map(raw,""); var dcm=map(row.get("dcm"),""); var artifact=map(row.get("artifact"),"");
            byte[] modelBytes=canonical(dcm), artifactBytes=canonical(artifact);
            var bound=KeirRuntime.bindModel(modelBytes,Map.of()); check(bound.succeeded());
            var prepared=KeirRuntime.prepareProgram(artifactBytes,bound.boundModel()); check(prepared.succeeded());
            var plan=Artifact.validateProgram(artifact,dcm,CapabilityPolicy.catalog(dcm),1);
            var bundle=CheckedCel.prepare(plan,CheckedCel.bindModel(dcm));
            byte[] wanted=canonical(expected(row,0,false)); var phases=new TreeMap<String,Object>();
            System.gc(); long before=heap();
            long allocatedBefore=threadBean instanceof com.sun.management.ThreadMXBean bean && bean.isThreadAllocatedMemoryEnabled()
                    ? bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) : -1;
            phases.put("bind",measure(() -> KeirRuntime.bindModel(modelBytes,Map.of()),v -> check(((KeirRuntime.ModelBinding)v).succeeded())));
            phases.put("prepare",measure(() -> KeirRuntime.prepareProgram(artifactBytes,bound.boundModel()),v -> check(((KeirRuntime.Preparation)v).succeeded())));
            phases.put("validateProject",measure(() -> {
                var activation=bundle.activation(); Object value=activation.accept("input","Input",input(row,0,false),"/input","INPUT_INVALID");
                activation.get("input","Input",value,"/input"); return activation;
            },value -> { var activation=(CheckedCel.Activation)value; check(activation.projections()==1);
                int parses=activation.boundaryParses(); activation.get("input","Input",input(row,0,false),"/input");
                check(activation.projections()==1 && activation.boundaryParses()==parses);
            }));
            phases.put("invoke",measure(() -> invoke(prepared.prepared(),row,0,false),v -> check(Arrays.equals(wanted,((KeirRuntime.Outcome)v).toJson()))));
            var result=Map.of("owner","owner0","value",list(row.get("expected"),"").getFirst());
            phases.put("validateResult",measure(() -> Artifact.value(dcm,"Output",result,"/result","EXPRESSION_FAILED"),v -> check(result.equals(v))));
            var invalid=Map.of("owner","owner0","value","1".repeat(257),"divisor",row.get("divisor"));
            byte[] rejected=canonical(Map.of("classification","FAILED","effects",List.of(),"error",Map.of("code","INPUT_INVALID","path","/input"),
                    "trace",List.of(Map.of("sequence",0L,"event","INVOCATION_START"),Map.of("sequence",1L,"event","TERMINATED","code","INPUT_INVALID"))));
            phases.put("reject",measure(() -> invokeInput(prepared.prepared(),invalid,0),v -> check(Arrays.equals(rejected,((KeirRuntime.Outcome)v).toJson()))));
            long allocatedAfter=allocatedBefore>=0 ? ((com.sun.management.ThreadMXBean)threadBean).getThreadAllocatedBytes(Thread.currentThread().threadId()) : -1;
            var activation=bundle.activation(); var state=new HashMap<String,Object>();
            state.put("input",activation.accept("input","Input",input(row,0,false),"/input","INPUT_INVALID"));
            for (Object stepRaw:list(artifact.get("steps"),"")) {
                var step=map(stepRaw,""); String id=(String)step.get("id");
                state.put(id,bundle.evaluate(map(step.get("value"),""),state,activation,id,"/steps"));
            }
            check(Objects.equals(state.get("d"),list(row.get("expected"),"").getFirst()));
            var batches=new ArrayList<Object>();
            try (var pool=Executors.newFixedThreadPool(4)) {
                for (int batch=0;batch<3;batch++) {
                    long at=System.nanoTime(); long[] latencies=new long[1000]; var jobs=new ArrayList<Future<?>>();
                    for (int lane=0;lane<4;lane++) { int first=lane; jobs.add(pool.submit(() -> {
                        for (int i=first;i<1000;i+=4) {
                            boolean failure=i%7==0;
                            byte[] oracle=canonical(expected(row,i,failure)); long began=System.nanoTime();
                            var actual=invoke(prepared.prepared(),row,i,failure); latencies[i]=System.nanoTime()-began;
                            check(Arrays.equals(oracle,actual.toJson()));
                        }
                    })); }
                    for (var job:jobs) job.get(10,TimeUnit.SECONDS);
                    long wall=System.nanoTime()-at; Arrays.sort(latencies); System.gc();
                    batches.add(Map.of("invocations",1000,"lanes",4,"wallNs",wall,"perSecond",1_000_000_000_000L/Math.max(1,wall),
                            "p50Ns",latencies[499],"p95Ns",latencies[949],"maxNs",latencies[999],"heapAfterGcRequest",heap()));
                }
            }
            reports.add(Map.of("id",row.get("id"),"phases",phases,"heapAfterInitialGcRequest",before,"heapAfter",heap(),
                    "mainThreadAllocatedBytes",allocatedBefore>=0 ? allocatedAfter-allocatedBefore : "unsupported",
                    "chainProjections",activation.projections(),"chainBoundaryParses",activation.boundaryParses(),
                    "batches",batches,"expectedSha256",digest(wanted)));
        }
        var report=Map.of("status","passed","fixtureSha256",digest(fixture),"engine","Java",
                "javaVersion",System.getProperty("java.version"),"flags",ManagementFactory.getRuntimeMXBean().getInputArguments(),"cases",reports,
                "limitations",List.of("Production bind/prepare/invoke; assertions and serialization excluded from latency samples, included in wall throughput.",
                        "100 warmups and 1000 samples per phase; warm JIT/GC effects and phase order are not controlled experiments.",
                        "Four Java request threads share one prepared handle; at most four in flight, three fixed 1000-request batches.",
                        "Heap after requested GC is not peak RSS. Main-thread allocation covers all six phase loops, not per-invocation allocation; concurrent-worker allocation is unsupported.",
                        "Boundary parses/projections are directly counted. Decimal operand/result reparsing is retained and not instrumented; no coefficient memo or parse-once claim.",
                        "validateProject measures complete input admission/projection; validateResult measures complete output admission without unnecessary output-engine projection."));
        Files.write(Path.of(args[1]),canonical(report));
    }
}
