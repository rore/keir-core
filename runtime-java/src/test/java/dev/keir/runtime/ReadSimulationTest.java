package dev.keir.runtime;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static dev.keir.runtime.RuntimeData.*;
import static org.junit.jupiter.api.Assertions.*;

class ReadSimulationTest {
    static Map<String,Object> obj(Object value) { return map(value,""); }
    static byte[] load(String name) throws Exception { try(var input=ReadSimulationTest.class.getResourceAsStream("/read-simulation/task-advice/"+name)) { return Objects.requireNonNull(input).readAllBytes(); } }
    static ReadSimulation.Selection selection() throws Exception {
        var row=obj(parseJson(load("selection.json"),4096));
        return new ReadSimulation.Selection(load("dcm.json"),load("artifact.json"),(String)row.get("modelDigest"),(String)row.get("artifactDigest"),(String)row.get("compiler"),(String)row.get("entrypoint"),obj(row.get("consumer")));
    }
    static List<Map<String,Object>> receipts() { return List.of(Map.of("status","NOT_DISPLAYED"),Map.of("status","DISPLAYED","code","DONE"),Map.of("status","DISPLAYED","code","PENDING"),Map.of("status","DISPLAYED","code","NEEDS_ATTENTION")); }
    static Object consume(Map<String,Object> outcome) { return "COMPLETED".equals(outcome.get("classification")) ? Map.of("status","DISPLAYED","code",obj(outcome.get("result")).get("classification")) : Map.of("status","NOT_DISPLAYED"); }
    static ReadSimulation.Fixture fixture(boolean allowed,ReadSimulation.Consumer consumer) {
        return new ReadSimulation.Fixture("test.scope",Map.of("actorId","user1"),new KeirRuntime.Deadline(()->0,1000),request->CompletableFuture.completedFuture(allowed),consumer,receipts(),null);
    }
    static Map<String,Object> run(ReadSimulation.Selection selected,ReadSimulation.Fixture fixture,byte[] bytes) { byte[] report=ReadSimulation.run(selected,fixture,bytes); assertTrue(report.length<=16_384); return obj(parseCanonical(report)); }
    static Map<String,Object> row(String name) throws Exception { return obj(parseJson(load(name+".json"),65_536)); }
    static List<Object> stubs(Map<String,Object> row) { return list(row.get("stubs"),""); }
    static void status(String expected,Map<String,Object> report) { assertEquals(expected,report.get("status")); }
    static void code(Map<String,Object> report,String code) { assertTrue(list(report.get("mismatches"),"").stream().anyMatch(item->code.equals(obj(item).get("code"))),()->report.toString()); }

    @Test void exactDemoCasesAndIndependentMismatch() throws Exception {
        for(String name:List.of("open","completed","missing","denied","transient")) status("PASS",run(selection(),fixture(!name.equals("denied"),ReadSimulationTest::consume),load(name+".json")));
        var wrong=run(selection(),fixture(true,ReadSimulationTest::consume),load("mismatch.json")); status("MISMATCH",wrong); code(wrong,"OUTCOME_MISMATCH"); code(wrong,"CONSUMPTION_MISMATCH");
    }
    @Test void strictStubMatchingAndLedgerSurviveCaughtNativeFailure() throws Exception {
        for(String mutation:List.of("missing","extra","args","step")) {
            var row=row("open");
            switch(mutation) {
                case "missing" -> { stubs(row).clear(); obj(row.get("expect")).put("outcome",Map.of("classification","FAILED","error",Map.of("code","ADAPTER_EXCEPTION","path","/steps/0"),"trace",List.of(Map.of("sequence",0,"event","INVOCATION_START"),Map.of("sequence",1,"event","STEP_START","step","source"),Map.of("sequence",2,"event","CALL_DISPATCHED","step","source","capability","tasks.get"),Map.of("sequence",3,"event","TERMINATED","code","ADAPTER_EXCEPTION")))); obj(row.get("expect")).put("consumption",Map.of("status","NOT_DISPLAYED")); }
                case "extra" -> stubs(row).add(stubs(row).getFirst());
                case "args" -> obj(stubs(row).getFirst()).put("args",Map.of("taskId","other"));
                default -> obj(stubs(row).getFirst()).put("step","other");
            }
            var report=run(selection(),fixture(true,ReadSimulationTest::consume),canonical(row)); status("MISMATCH",report);
            code(report,switch(mutation){case "missing"->"MISSING_STUB";case "extra"->"EXTRA_STUB";case "args"->"STUB_MISMATCH";default->"TRACE_MISMATCH";});
            if(mutation.equals("missing")) assertFalse(list(report.get("mismatches"),"").stream().anyMatch(item->"OUTCOME_MISMATCH".equals(obj(item).get("code"))));
        }
    }
    @Test void everyUnusedStubAndClosedAuthorityBoundaryValidateBeforeAuthorization() throws Exception {
        var count=new AtomicInteger(); var base=fixture(true,ReadSimulationTest::consume);
        var f=new ReadSimulation.Fixture(base.scope(),base.bindings(),base.deadline(),request->{count.incrementAndGet();return CompletableFuture.completedFuture(true);},base.consume(),base.allowedReceipts(),null);
        for(String mutation:List.of("authority","version","unused-response","expected-receipt","unknown-error","null")) {
            var row=row("open"); switch(mutation) {
                case "authority" -> row.put("bindings",Map.of("actorId","other"));
                case "version" -> obj(stubs(row).getFirst()).put("version","2");
                case "unused-response" -> { var invalid=obj(parseJson(canonical(stubs(row).getFirst()),65_536)); invalid.put("response",Map.of("kind","SUCCESS","value",Map.of("id","task1","status","UNDECLARED"))); stubs(row).add(invalid); }
                case "expected-receipt" -> obj(row.get("expect")).put("consumption",Map.of("status","DISPLAYED","code","UNLISTED"));
                case "unknown-error" -> obj(stubs(row).getFirst()).put("response",Map.of("kind","BUSINESS_REJECTION","code","UNDECLARED"));
                default -> row.put("input",null);
            }
            byte[] bytes=mutation.equals("null")?"{\"format\":null}".getBytes(StandardCharsets.UTF_8):canonical(row);
            status("INVALID_SCENARIO",run(selection(),f,bytes));
        }
        assertEquals(0,count.get());
    }
    @Test void malformedBytesDuplicatesDepthAndExactByteBoundary() throws Exception {
        var f=fixture(true,ReadSimulationTest::consume); byte[] raw=load("open.json");
        byte[] boundary=Arrays.copyOf(raw,65_536); Arrays.fill(boundary,raw.length,boundary.length,(byte)' ');
        status("PASS",run(selection(),f,boundary)); status("INVALID_SCENARIO",run(selection(),f,Arrays.copyOf(boundary,65_537)));
        for(byte[] bad:List.of(new byte[]{(byte)0xff},"{\"id\":\"a\",\"id\":\"b\"}".getBytes(StandardCharsets.UTF_8),("[".repeat(17)+"0"+"]".repeat(17)).getBytes(StandardCharsets.UTF_8))) status("INVALID_SCENARIO",run(selection(),f,bad));
    }
    @Test void consumersFailClosedAndAllowlistIsSetupValidated() throws Exception {
        for(ReadSimulation.Consumer consumer:List.<ReadSimulation.Consumer>of(outcome->{throw new IllegalStateException("PRIVATE_CANARY");},outcome->Map.of("status","DISPLAYED","code","UNLISTED"),outcome->Map.of("status","DISPLAYED","code","DONE","secret","PRIVATE_CANARY"))) {
            var report=run(selection(),fixture(true,consumer),load("open.json")); status("CONSUMER_ERROR",report); assertFalse(report.containsKey("consumption")); assertFalse(new String(canonical(report),StandardCharsets.UTF_8).contains("PRIVATE_CANARY"));
        }
        status("MISMATCH",run(selection(),fixture(true,outcome->Map.of("status","DISPLAYED","code","DONE")),load("open.json")));
        var f=fixture(true,ReadSimulationTest::consume);
        for(var allowed:List.of(List.<Map<String,Object>>of(),List.of(receipts().getFirst(),receipts().getFirst()),List.of(Map.<String,Object>of("status","DISPLAYED","code","invalid code")))) {
            status("SETUP_ERROR",run(selection(),new ReadSimulation.Fixture(f.scope(),f.bindings(),f.deadline(),f.authorize(),f.consume(),allowed,null),load("open.json")));
        }
    }
    @Test void wrongPinsAndCompilerNeverInvokeHostAndLeakNoSelectionValues() throws Exception {
        var s=selection(); var count=new AtomicInteger(); var f=fixture(true,outcome->{count.incrementAndGet();return consume(outcome);});
        for(var bad:List.of(new ReadSimulation.Selection(s.dcmBytes(),s.artifactBytes(),"PRIVATE_CANARY",s.artifactDigest(),s.compiler(),s.entrypoint(),s.consumer()),new ReadSimulation.Selection(s.dcmBytes(),s.artifactBytes(),s.modelDigest(),s.artifactDigest(),"wrong",s.entrypoint(),s.consumer()))) {
            var report=run(bad,f,load("open.json")); status("SETUP_ERROR",report); assertEquals(Map.of(),report.get("identity")); assertFalse(new String(canonical(report),StandardCharsets.UTF_8).contains("PRIVATE_CANARY"));
        }
        assertEquals(0,count.get());
    }
    @Test void protectedHostBindingsAndResponseRelationshipsStayOutsideCases() throws Exception {
        var f=fixture(true,ReadSimulationTest::consume); var count=new AtomicInteger();
        var checked=new ReadSimulation.Fixture(f.scope(),Map.of("actorId","different-host-user"),f.deadline(),f.authorize(),f.consume(),f.allowedReceipts(),(id,args,response)->{count.incrementAndGet();return !"SUCCESS".equals(response.get("kind"))||Objects.equals(obj(response.get("value")).get("id"),args.get("taskId"));});
        status("PASS",run(selection(),checked,load("open.json"))); assertEquals(1,count.get());
        var row=row("open"); obj(obj(stubs(row).getFirst()).get("response")).put("value",Map.of("id","other","status","OPEN"));
        var report=run(selection(),checked,canonical(row)); status("MISMATCH",report); code(report,"STUB_MISMATCH");
    }
    @Test void repeatedDistinctCallsOrderFreshAuthorizationAndFourStubCeiling() throws Exception {
        byte[] model,artifact,bytes,reordered,selection;
        try(var in=getClass().getResourceAsStream("/read-simulation/repeated/dcm.json")) { model=Objects.requireNonNull(in).readAllBytes(); }
        try(var in=getClass().getResourceAsStream("/read-simulation/repeated/artifact.json")) { artifact=Objects.requireNonNull(in).readAllBytes(); }
        try(var in=getClass().getResourceAsStream("/read-simulation/repeated/selection.json")) { selection=Objects.requireNonNull(in).readAllBytes(); }
        try(var in=getClass().getResourceAsStream("/read-simulation/repeated/repeated.json")) { bytes=Objects.requireNonNull(in).readAllBytes(); }
        try(var in=getClass().getResourceAsStream("/read-simulation/repeated/reordered.json")) { reordered=Objects.requireNonNull(in).readAllBytes(); }
        var meta=obj(parseJson(selection,4096)); var selected=new ReadSimulation.Selection(model,artifact,(String)meta.get("modelDigest"),(String)meta.get("artifactDigest"),(String)meta.get("compiler"),(String)meta.get("entrypoint"),obj(meta.get("consumer")));
        var f=fixture(true,ReadSimulationTest::consume);
        status("PASS",run(selected,f,bytes)); var wrong=run(selected,f,reordered); status("MISMATCH",wrong); code(wrong,"STUB_MISMATCH");
        var count=new AtomicInteger(); var revoked=new ReadSimulation.Fixture(f.scope(),f.bindings(),f.deadline(),request->CompletableFuture.completedFuture(count.incrementAndGet()<3),f.consume(),f.allowedReceipts(),null);
        var rejected=run(selected,revoked,bytes); status("MISMATCH",rejected); code(rejected,"EXTRA_STUB"); assertEquals(3,count.get());
        var row=obj(parseJson(bytes,65_536)); stubs(row).add(stubs(row).getFirst()); stubs(row).add(stubs(row).getFirst());
        status("MISMATCH",run(selected,f,canonical(row))); stubs(row).add(stubs(row).getFirst()); status("INVALID_SCENARIO",run(selected,f,canonical(row)));
    }
    @Test void sixteenUniqueAllowedReceiptsAndModelByteCeilingAreExact() throws Exception {
        var f=fixture(true,ReadSimulationTest::consume); var allowed=new ArrayList<>(receipts());
        for(int i=allowed.size();i<16;i++) allowed.add(Map.of("status","DISPLAYED","code","SAFE_"+i));
        status("PASS",run(selection(),new ReadSimulation.Fixture(f.scope(),f.bindings(),f.deadline(),f.authorize(),f.consume(),allowed,null),load("open.json")));
        allowed.add(Map.of("status","DISPLAYED","code","OVERFLOW")); status("SETUP_ERROR",run(selection(),new ReadSimulation.Fixture(f.scope(),f.bindings(),f.deadline(),f.authorize(),f.consume(),allowed,null),load("open.json")));
        var s=selection(); var model=Arrays.copyOf(s.dcmBytes(),65_536); Arrays.fill(model,s.dcmBytes().length,model.length,(byte)' ');
        status("PASS",run(new ReadSimulation.Selection(model,s.artifactBytes(),s.modelDigest(),s.artifactDigest(),s.compiler(),s.entrypoint(),s.consumer()),f,load("open.json")));
        status("SETUP_ERROR",run(new ReadSimulation.Selection(Arrays.copyOf(model,65_537),s.artifactBytes(),s.modelDigest(),s.artifactDigest(),s.compiler(),s.entrypoint(),s.consumer()),f,load("open.json")));
    }
    @Test void completeCatalogCanContainUnselectedWritesWithoutGrantingWriteAuthority() throws Exception {
        byte[] model,artifact,corpus;
        try(var in=getClass().getResourceAsStream("/generic-hooks/read/dcm.json")) { model=Objects.requireNonNull(in).readAllBytes(); }
        try(var in=getClass().getResourceAsStream("/generic-hooks/read/artifact.json")) { artifact=Objects.requireNonNull(in).readAllBytes(); }
        try(var in=getClass().getResourceAsStream("/generic-hooks/read/scenarios.json")) { corpus=Objects.requireNonNull(in).readAllBytes(); }
        var dcm=obj(parseJson(model,65_536)); var nativeArtifact=obj(parseCanonical(artifact));
        var selected=new ReadSimulation.Selection(model,artifact,digest(canonical(dcm)),digest(artifact),(String)nativeArtifact.get("compiler"),"ClassifyTask",obj(obj(obj(dcm.get("entrypoints")).get("ClassifyTask")).get("consumer")));
        var old=obj(list(parseJson(corpus,1_048_576),"").getFirst()); var stubs=new ArrayList<Object>(); var calls=new ArrayList<Object>();
        for(var item:list(old.get("stubs"),"")) { var stub=new LinkedHashMap<>(obj(item)); stub.remove("protectedArgs"); stubs.add(stub); var call=new LinkedHashMap<>(stub); call.remove("response"); calls.add(call); }
        var nativeOutcome=obj(obj(old.get("expect")).get("outcome"));
        var row=Map.of("format","keir-read-simulation/1","id","complete-catalog","input",obj(old.get("given")).get("input"),"stubs",stubs,"expect",Map.of("calls",calls,"outcome",nativeOutcome,"consumption",Map.of("status","DISPLAYED","code",obj(nativeOutcome.get("result")).get("classification"))));
        status("PASS",run(selected,fixture(true,ReadSimulationTest::consume),canonical(row)));
    }
}
