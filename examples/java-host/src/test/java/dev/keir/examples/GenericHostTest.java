package dev.keir.examples;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static dev.keir.examples.EffectSimulation.*;
import static org.junit.jupiter.api.Assertions.*;

class GenericHostTest {
    @TempDir Path temp;
    static Path fixture(String profile) throws Exception { return Path.of(GenericHostTest.class.getResource("/generic-hooks/"+profile+"/scenarios.json").toURI()).getParent(); }
    static List<Object> corpus(String profile) throws Exception { return array(EffectJson.parse(Files.readAllBytes(fixture(profile).resolve("scenarios.json")))); }
    static Map<String,GenericSimulation.Program> programs(String profile) throws Exception {
        var result=new LinkedHashMap<String,GenericSimulation.Program>();
        for(String name:GenericHost.programNames(profile)) { String file=name.equals("main")?"artifact":"artifact-"+name; result.put(name,new GenericSimulation.Program(Files.readAllBytes(fixture(profile).resolve(file+".json")),Files.readString(fixture(profile).resolve(file+".sha256")).trim())); } return result;
    }
    static List<Map<String,Object>> batch(String profile,Object rows) throws Exception { return GenericSimulation.batch(profile,Files.readAllBytes(fixture(profile).resolve("dcm.json")),Files.readString(fixture(profile).resolve("dcm.sha256")).trim(),programs(profile),rows); }
    static Map<String,Object> normal() throws Exception { return object(EffectJson.copy(corpus("effect").getFirst())); }
    static Map<String,Object> stub(Map<String,Object> row,int index) { return object(array(row.get("stubs")).get(index)); }
    static void capacity(Runnable action) { assertEquals("CAPACITY_EXCEEDED",assertThrows(IllegalArgumentException.class,action::run).getMessage()); }

    @Test void completeHandAuthoredReadAndEffectCorpusUsesOneBindingForThreePrograms() throws Exception {
        for(String profile:List.of("read","effect","effect3")) {
            var scenarios=corpus(profile); byte[] before=EffectJson.encode(scenarios); var actual=batch(profile,scenarios);
            assertEquals(profile.equals("read")?9:19,actual.size());
            for(int i=0;i<actual.size();i++) { var expected=new LinkedHashMap<>(object(object(scenarios.get(i)).get("expect"))); expected.put("id",object(scenarios.get(i)).get("id")); same(actual.get(i),expected,"Complete result mismatch"); }
            assertArrayEquals(before,EffectJson.encode(scenarios));
            assertArrayEquals(EffectJson.encode(actual),GenericHost.execute(fixture(profile)));
            // Release/rebind is an offline cold start with the same independently selected bytes.
            same(batch(profile,scenarios),actual,"Cold start changed behavior");
        }
    }
    @Test void effect3UsesOnlyItsModelDeclaredCreateErrors() throws Exception {
        var effect3=object(EffectJson.copy(corpus("effect3").getFirst()));
        object(stub(effect3,1).get("response")).put("kind","BUSINESS_REJECTION");
        var rejected=object(stub(effect3,1).get("response")); rejected.put("code","KEY_PAYLOAD_MISMATCH"); rejected.remove("value");
        rejected.put("effect","NONE");
        assertEquals("Simulation result mismatch",assertThrows(IllegalArgumentException.class,()->batch("effect3",List.of(effect3))).getMessage());
        var old=normal(); var response=object(stub(old,1).get("response")); response.put("kind","BUSINESS_REJECTION"); response.put("code","KEY_PAYLOAD_MISMATCH"); response.remove("value"); response.put("effect","NONE");
        assertEquals("Undeclared business code",assertThrows(IllegalArgumentException.class,()->batch("effect",List.of(old))).getMessage());
    }
    @Test void closedDataOnlySchemaRejectsPathsProtectedConfigurationAndMalformedResponses() throws Exception {
        for(Consumer<Map<String,Object>> mutate:List.<Consumer<Map<String,Object>>>of(
            row->row.put("module","custom"),row->row.put("id","x".repeat(65)),row->object(row.get("given")).put("requestKey","chosen"),
            row->object(row.get("given")).put("fixture","../private"),row->object(row.get("given")).put("program","../artifact"),
            row->object(object(row.get("given")).get("input")).put("title","customer configuration"),
            row->object(stub(row,1).get("args")).put("sourceTaskId","task1"),row->object(stub(row,0).get("response")).put("effect","NONE"),
            row->object(object(stub(row,0).get("response")).get("value")).put("extra",true),
            row->object(stub(row,1).get("response")).put("effect","NONE"),row->stub(row,0).put("invoke","executable"))) {
            var value=normal(); mutate.accept(value); assertThrows(RuntimeException.class,()->GenericSimulation.validateScenario(value,true));
        }
        var read=object(EffectJson.copy(corpus("read").getFirst())); object(read.get("given")).put("fixture","invalid-key"); assertThrows(RuntimeException.class,()->GenericSimulation.validateScenario(read,false));
        var duplicate=normal(); assertThrows(IllegalArgumentException.class,()->batch("effect",List.of(duplicate,EffectJson.copy(duplicate))));
    }
    @Test void simulationMismatchesCannotHideInsideSafeRuntimeErrors() throws Exception {
        for(Consumer<Map<String,Object>> mutate:List.<Consumer<Map<String,Object>>>of(
            row->array(row.get("stubs")).removeFirst(),row->array(row.get("stubs")).removeLast(),row->Collections.swap(array(row.get("stubs")),0,1),
            row->stub(row,0).put("step","wrongLabel"),row->object(stub(row,0).get("args")).put("taskId","other"),
            row->object(stub(row,0).get("protectedArgs")).put("actorId","other"),row->object(object(stub(row,0).get("response")).get("value")).put("id","wrong-resource"))) {
            var value=normal(); mutate.accept(value); var failure=assertThrows(IllegalArgumentException.class,()->batch("effect",List.of(value))); assertEquals("Simulation violations",failure.getMessage());
        }
        var failure=object(EffectJson.copy(corpus("effect").stream().filter(row->object(row).get("id").equals("read-throws")).findFirst().orElseThrow()));
        object(failure.get("given")).put("fixture","normal"); object(stub(failure,0).get("args")).put("taskId","other");
        assertEquals("Simulation violations",assertThrows(IllegalArgumentException.class,()->batch("effect",List.of(failure))).getMessage());
        var altered=normal(); object(array(object(object(altered.get("expect")).get("outcome")).get("trace")).get(9)).put("taken",false);
        assertThrows(IllegalArgumentException.class,()->batch("effect",List.of(altered)));
    }
    @Test void finiteModelAndProgramReservationsRejectBeforeCallbacksAndReleaseExactlyOnce() {
        var budget=new GenericCapacity(); var models=new ArrayList<GenericCapacity.Lease>(); int[] callbacks={0};
        for(int i=0;i<4;i++) { models.add(budget.model(65_536,65_536,65_536)); callbacks[0]++; }
        capacity(()->{ try(var ignored=budget.model(1,1,1)) { callbacks[0]++; } }); assertEquals(4,callbacks[0]);
        models.getFirst().close(); models.getFirst().close(); try(var reload=budget.model(65_536,65_536,65_536)) { assertNotNull(reload); }
        models.forEach(GenericCapacity.Lease::close);
        for(int field=0;field<3;field++) { final int current=field; capacity(()->budget.model(current==0?65_537:0,current==1?65_537:0,current==2?65_537:0)); }
        var programs=new ArrayList<GenericCapacity.Lease>(); for(int i=0;i<8;i++)programs.add(budget.program(1_048_576));
        capacity(()->budget.program(1)); capacity(()->new GenericCapacity().program(1_048_577)); programs.forEach(GenericCapacity.Lease::close);
        try(var reload=budget.program(1_048_576)) { assertNotNull(reload); }
    }
    @Test void bindPrepareAndInvocationConcurrencyHaveNoQueue() {
        var budget=new GenericCapacity(); int[] callbacks={0};
        try(var binding=budget.work()) { capacity(()->{ try(var preparation=budget.work()) { callbacks[0]++; } }); }
        try(var preparation=budget.work()) { capacity(()->{ try(var binding=budget.work()) { callbacks[0]++; } }); }
        try(var first=budget.invocation();var second=budget.invocation()) { capacity(()->{ try(var third=budget.invocation()) { callbacks[0]++; } }); }
        assertEquals(0,callbacks[0]); try(var fresh=budget.work();var invocation=budget.invocation()) { assertNotNull(fresh); assertNotNull(invocation); }
    }
    @Test void everyScenarioCapacityHasBoundaryAndPlusOne() throws Exception {
        GenericCapacity.scenarios(1_048_576,128,65_536,4,4);
        for(int field=0;field<5;field++) { int current=field; capacity(()->GenericCapacity.scenarios(current==0?1_048_577:0,current==1?129:0,current==2?65_537:0,current==3?5:0,current==4?5:0)); }
        var value=normal(); value.put("padding","x".repeat(65_536)); capacity(()->GenericSimulation.validateScenario(value,true));
        var four=normal(); four.put("stubs",Collections.nCopies(4,stub(four,0))); GenericSimulation.validateScenario(four,true);
        four.put("stubs",Collections.nCopies(5,stub(four,0))); capacity(()->GenericSimulation.validateScenario(four,true));
        var rows=new ArrayList<Object>(); for(int i=0;i<128;i++) { var item=object(EffectJson.copy(corpus("read").getFirst())); item.put("id","row"+i); rows.add(item); }
        assertEquals(128,batch("read",rows).size()); rows.add(normal()); capacity(()->{ try { batch("read",rows); } catch(IOException failure) { throw new RuntimeException(failure); } catch(RuntimeException failure) { throw failure; } catch(Exception failure) { throw new RuntimeException(failure); } });
    }
    @Test void entrypointUsesFixedFilesBoundsAndRedactedFailures() throws Exception {
        var out=new ByteArrayOutputStream(); var err=new ByteArrayOutputStream();
        assertEquals(0,GenericHost.run(new String[]{fixture("effect").toString()},new PrintStream(out),new PrintStream(err))); assertEquals(19,array(EffectJson.parse(out.toByteArray())).size()); assertEquals(0,err.size());
        out.reset(); assertEquals(0,GenericHost.run(new String[]{fixture("effect3").toString()},new PrintStream(out),new PrintStream(err))); assertEquals(19,array(EffectJson.parse(out.toByteArray())).size());
        out.reset(); assertEquals(2,GenericHost.run(new String[]{},new PrintStream(out),new PrintStream(err))); assertEquals(0,out.size());
        err.reset(); assertEquals(1,GenericHost.run(new String[]{temp.resolve("private").toString()},new PrintStream(out),new PrintStream(err))); assertFalse(err.toString().contains(temp.toString()));
        Path file=temp.resolve("bounded.json"); Files.write(file,new byte[1_048_576]); assertEquals(1_048_576,GenericHost.read(file,1_048_576).length); Files.write(file,new byte[1_048_577]); capacity(()->{ try { GenericHost.read(file,1_048_576); } catch(IOException failure) { throw new RuntimeException(failure); } });
    }
    @Test void executeAcceptsOnlyCanonicalPinsWithOptionalLineEnding() throws Exception {
        Path source=fixture("effect"), copy=temp.resolve("effect");
        Files.createDirectory(copy);
        try(var files=Files.list(source)) { for(Path file:files.toList()) Files.copy(file,copy.resolve(file.getFileName())); }
        List<Path> pins;
        try(var files=Files.list(copy)) { pins=files.filter(file->file.getFileName().toString().endsWith(".sha256")).toList(); }
        byte[] expected=GenericHost.execute(source);
        for(String ending:List.of("", "\n", "\r\n")) {
            for(Path pin:pins) Files.writeString(pin,Files.readString(source.resolve(pin.getFileName())).trim()+ending);
            assertArrayEquals(expected,GenericHost.execute(copy));
        }
        Files.writeString(copy.resolve("dcm.sha256"),Files.readString(source.resolve("dcm.sha256")).trim()+" \n");
        assertThrows(IllegalArgumentException.class,()->GenericHost.execute(copy));
        Files.writeString(copy.resolve("dcm.sha256"),Files.readString(source.resolve("dcm.sha256")).trim()+"\n ".repeat(2));
        assertThrows(IllegalArgumentException.class,()->GenericHost.execute(copy));
    }
}
