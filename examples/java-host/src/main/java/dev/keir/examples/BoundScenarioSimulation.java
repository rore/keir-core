package dev.keir.examples;

import dev.keir.runtime.KeirRuntime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static dev.keir.examples.ScenarioData.*;

/** Runs the teaching host's ordered stubs with native authority and dispatch checks. */
final class BoundScenarioSimulation implements AutoCloseable {
    final Map<String,KeirRuntime.Prepared> prepared=new LinkedHashMap<>();
    final GenericCapacity capacity=new GenericCapacity();
    final List<GenericCapacity.Lease> leases=new ArrayList<>(); KeirRuntime.BoundModel model; Active current;
    int dispatchCount;
    final Map<String,Boolean> decisions=new HashMap<>();
    final Set<String> taskPrograms=new HashSet<>();
    final Set<String> actorPrograms=new HashSet<>();
    final Set<String> writes=new HashSet<>();
    BoundScenarioSimulation(byte[] dcm,Map<String,byte[]> programs,Map<String,Object> document) {
        try {
            GenericCapacity.capacity(dcm.length<=65_536); leases.add(capacity.model(dcm.length,EffectJson.encode(document).length,65_536));
            var invokers=new LinkedHashMap<String,KeirRuntime.BoundEffectAdapter>();
            String keyType=(String)object(document.get("hostKey")).get("type");
            object(document.get("capabilities")).forEach((id,raw)->{
                String version=(String)object(raw).get("version");
                if("WRITE".equals(object(raw).get("effect"))) writes.add(id);
                invokers.put(id,new KeirRuntime.BoundEffectAdapter(version,"simulation",keyType,
                        (args,protectedArgs,remaining,host)->dispatch(id,version,args,protectedArgs,remaining,host)));
            });
            try(var work=capacity.work()) { var result=KeirRuntime.bindModel(dcm,invokers); check(result.succeeded(),"Bound model binding failed"); model=result.boundModel(); }
            for(var entry:programs.entrySet()) {
                byte[] program=entry.getValue(); leases.add(capacity.program(program.length));
                try(var work=capacity.work()) { var result=KeirRuntime.prepareProgram(program,model); check(result.succeeded(),"Bound program preparation failed"); prepared.put(entry.getKey(),result.prepared()); }
                var artifact=object(EffectJson.parse(program));
                var entrypoint=object(object(document.get("entrypoints")).get(artifact.get("entrypoint")));
                decisions.put(entry.getKey(),"DECISION".equals(object(entrypoint.get("consumer")).get("kind")));
                if("TaskId".equals(object(entrypoint.get("event")).get("sourceType"))) taskPrograms.add(entry.getKey());
                for(Object raw:array(artifact.get("steps"))) {
                    var step=object(raw);
                    if(!"CALL".equals(step.get("op"))) continue;
                    var capability=object(object(document.get("capabilities")).get(step.get("capability")));
                    var actor=object(capability.get("parameters")).get("actorId");
                    if(actor!=null&&"host".equals(object(actor).get("binding"))) actorPrograms.add(entry.getKey());
                }
            }
        } catch(RuntimeException failure) { close(); throw failure; }
    }
    CompletionStage<?> dispatch(String capability,String version,Map<String,Object> args,Map<String,Object> protectedArgs,long remaining,KeirRuntime.BoundEffectContext host) {
        dispatchCount++;
        Active state=current; Map<String,Object> selected; boolean write=writes.contains(capability);
        try {
            check(state!=null&&state.captures.size()<4,"Dispatch outside request");
            check(!state.decision||!write,"Decision dispatched a WRITE");
            var captured=object(EffectJson.copy(Map.of("capability",capability,"version",version,"args",args,"protectedArgs",protectedArgs))); state.captures.add(captured);
            check(state.deadline.equals(host.deadline())&&remaining>0
                    &&remaining==state.deadline.expiresAtMs()-state.now.get(),"Native deadline or remaining budget mismatch");
            check(!protectedArgs.containsKey("actorId")||"user1".equals(protectedArgs.get("actorId")),"Protected actor mismatch");
            check(host.requestKey()==state.key&&host.event().equals(state.event),"Original key or host event mismatch");
            check(state.used<state.stubs.size(),"Unexpected native call"); selected=object(state.stubs.get(state.used));
            same(captured,Map.of("capability",selected.get("capability"),"version",selected.get("version"),"args",selected.get("args"),"protectedArgs",selected.get("protectedArgs")),"Adapter arguments mismatch");
            var envelope=object(selected.get("response"));
            if("tasks.create".equals(capability)) check(state.event.get("source").equals(state.input.get("taskId")),"Host event does not own task");
            else if("tasks.get".equals(capability) && "SUCCESS".equals(envelope.get("kind"))) check(Objects.equals(object(envelope.get("value")).get("id"),args.get("taskId")),"Read resource mismatch");
            state.used++;
        } catch(RuntimeException failure) {
            if(state!=null) state.violations.add("Adapter arguments, native identity or resource mismatch");
            throw new IllegalStateException("Simulation adapter mismatch");
        }
        if(!write&&state.used==1&&state.fixture.equals("read-throws")) throw new IllegalStateException("Unavailable read");
        if(write&&state.fixture.equals("lost-response")) throw new IllegalStateException("Lost response");
        Object response=EffectJson.copy(selected.get("response"));
        if(!write&&state.fixture.equals("read-malformed")) response=Map.of("kind","SUCCESS","value",Map.of("restricted","false"));
        if(write&&state.fixture.equals("response-expired")||!write&&state.fixture.equals("read-expired")) state.now.set(state.deadline.expiresAtMs());
        return CompletableFuture.completedFuture(response);
    }
    Map<String,Object> run(String group,Map<String,Object> row) {
        check(current==null,"Concurrent scenario reuse");
        check(prepared.containsKey(group),"Unknown selected program");
        current=new Active(row,decisions.get(group),taskPrograms.contains(group));
        try(var invocation=capacity.invocation()) {
            Active state=current;
            var handle=prepared.get(group);
            var outcome=KeirRuntime.invokeBoundEffectProgram(handle,EffectJson.copy(state.input),
                    new KeirRuntime.BoundEffectInvocation(actorPrograms.contains(group)?Map.of("actorId","user1"):Map.of(),state.deadline,state.key,state.event))
                    .toCompletableFuture().get(3,TimeUnit.SECONDS);
            if(state.used!=state.stubs.size()) state.violations.add("Unconsumed scenario stub");
            var calls=reconcile(state.captures,outcome.trace(),state.stubs,state.violations); check(state.violations.isEmpty(),"Simulation violations");
            var actual=Map.<String,Object>of("outcome",outcome.toMap(),"calls",calls,"consumption",
                    state.decision?DecisionConsumer.consumeDecision(outcome.toMap()):EffectConsumer.consume(outcome.toMap()));
            same(actual,row.get("expect"),"Simulation result mismatch"); var result=new LinkedHashMap<String,Object>(actual); result.put("id",row.get("id")); return result;
        } catch(Exception failure) { if(failure instanceof RuntimeException runtime) throw runtime; throw new IllegalArgumentException("Bound simulation invocation failed"); }
        finally { current=null; }
    }
    public void close() { check(current==null,"Cannot release in-flight bound model"); prepared.clear(); decisions.clear(); taskPrograms.clear(); actorPrograms.clear(); model=null; for(int i=leases.size()-1;i>=0;i--) leases.get(i).close(); leases.clear(); }

    private static final class Active {
        final String fixture; final Map<String,Object> input,event; final UUID key=UUID.fromString("00000000-0000-4000-8000-000000000001");
        final boolean decision;
        final List<Object> stubs; final List<String> violations=new ArrayList<>(); final List<Map<String,Object>> captures=new ArrayList<>();
        final AtomicLong now=new AtomicLong();
        final KeirRuntime.Deadline deadline=new KeirRuntime.Deadline(now::get,1000);
        int used;
        Active(Map<String,Object> row,boolean decision,boolean taskEvent) { this.decision=decision; var given=object(row.get("given")); fixture=(String)given.get("fixture"); input=object(given.get("input")); stubs=array(row.get("stubs")); event=decision?Map.of("id","attempt1","source","scope1"):Map.of("id","event1","source",taskEvent?"task1":"scope1"); }
    }
}
