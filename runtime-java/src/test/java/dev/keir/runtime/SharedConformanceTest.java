package dev.keir.runtime;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedConformanceTest {
    private static Map<String,Object> object(Object value) { return RuntimeData.map(value,"/fixture"); }
    private static List<Object> array(Object value) { return RuntimeData.list(value,"/fixture"); }
    private static Map<String,Object> firstRun(Map<String,Object> row) { return object(array(row.get("runs")).getFirst()); }
    private static Map<String,Object> firstCase() throws Exception {
        return object(array(ScenarioDriver.loadCorpus().get("cases")).getFirst());
    }

    @Test void allSharedScenariosMatchTheirIndependentExpectedResults() throws Exception {
        Map<String,Object> corpus=ScenarioDriver.loadCorpus();
        assertEquals(array(corpus.get("cases")).size(),ScenarioDriver.runCorpus(corpus).size());
    }

    @Test void callbackMismatchStillFailsHarnessAfterRuntimeConvertsItToAnOutcome() throws Exception {
        List<Consumer<Map<String,Object>>> mutations=List.of(
            row->array(firstRun(row).get("authorizations")).clear(),
            row->{List<Object> stubs=array(firstRun(row).get("authorizations"));stubs.add(stubs.getLast());},
            row->Collections.swap(array(firstRun(row).get("authorizations")),0,1),
            row->array(firstRun(row).get("calls")).clear(),
            row->{List<Object> stubs=array(firstRun(row).get("calls"));stubs.add(stubs.getFirst());},
            row->{Map<String,Object> stub=object(array(firstRun(row).get("calls")).getFirst());object(object(stub.get("request")).get("agentArgs")).put("taskId","other");});
        for(Consumer<Map<String,Object>> mutation:mutations) {
            Map<String,Object> row=firstCase(); mutation.accept(row);
            AssertionError failure=assertThrows(AssertionError.class,()->ScenarioDriver.runCase(row));
            assertTrue(failure.getMessage().contains("harness violations"),failure.getMessage());
        }
    }

    @Test void displayConsumerIndependentlyRejectsMalformedOrNoncompletedResults() {
        Map<String,Object> invalid=Map.of("status","NOT_DISPLAYED","code","OUTPUT_INVALID");
        for(Object result:List.of("DONE",Map.of(),Map.of("classification","UNKNOWN"),Map.of("classification","DONE","extra",true)))
            assertEquals(invalid,ScenarioDriver.consume(Map.of("classification","COMPLETED","result",result)));
        assertEquals(invalid,ScenarioDriver.consume(Map.of("classification","COMPLETED")));
        assertEquals(Map.of("status","NOT_DISPLAYED","code","ACCESS_DENIED"),ScenarioDriver.consume(Map.of("classification","FAILED","error",Map.of("code","ACCESS_DENIED","path","/steps/0"))));
        for(String classification:List.of("DONE","PENDING"))
            assertEquals(Map.of("status","DISPLAYED","result",Map.of("classification",classification)),ScenarioDriver.consume(Map.of("classification","COMPLETED","result",Map.of("classification",classification))));
    }

    @Test void transportPreservesSignedClockValuesAndRejectsUnknownFixtureFields() throws Exception {
        Map<String,Object> row=firstCase(); object(firstRun(row).get("clock")).put("nowMs",new BigDecimal("-1"));
        ScenarioDriver.validateCase(row);
        object(firstRun(row).get("clock")).put("nowMs",new BigDecimal("1.5"));
        assertThrows(AssertionError.class,()->ScenarioDriver.validateCase(row));
        object(firstRun(row).get("clock")).put("nowMs",0L); firstRun(row).put("unknown",true);
        assertThrows(AssertionError.class,()->ScenarioDriver.validateCase(row));
    }
}
