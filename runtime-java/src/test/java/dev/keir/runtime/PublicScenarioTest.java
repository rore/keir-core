package dev.keir.runtime;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static dev.keir.runtime.RuntimeData.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicScenarioTest {
    @Test void matchesFrozenScenariosAndNativeVectorPhases() throws Exception {
        var report = new PublicScenarioDriver().runCorpus();
        assertEquals(20, list(report.get("scenarios"), "").size());
        assertEquals(45, list(report.get("vectors"), "").size());
    }

    @Test void callbackMismatchStillFailsHarnessAfterRuntimeConvertsItToAnOutcome() throws Exception {
        var document = PublicScenarioDriver.scenario("main/read-throws");
        var expect = map(document.get("expect"), "");
        for (Object calls : List.of(document.get("stubs"), expect.get("calls"))) {
            var call = map(list(calls, "").getFirst(), "");
            call.put("args", Map.of("taskId", "other"));
        }
        var driver = new PublicScenarioDriver();
        AssertionError error = assertThrows(AssertionError.class, () -> driver.runScenario("main", document));
        assertTrue(error.getMessage().contains("Callback mismatch"), error.getMessage());
        assertTrue(error.getMessage().contains("ADAPTER_EXCEPTION"), error.getMessage());
    }

    @Test void rejectsDuplicateIdsButPreservesCase() throws Exception {
        var first = PublicScenarioDriver.scenario("no-call/no-call");
        var second = PublicScenarioDriver.scenario("no-call/no-call");
        assertThrows(AssertionError.class, () -> PublicScenarioDriver.checkUniqueIds(List.of(first, second)));
        second.put("id", "NO-CALL");
        assertDoesNotThrow(() -> PublicScenarioDriver.checkUniqueIds(List.of(first, second)));
    }

    @Test void rejectsExpectedDispatchUnderFalseGuardBeforeAnyCallback() throws Exception {
        var document = PublicScenarioDriver.scenario("main/normal");
        var outcome = map(map(document.get("expect"), "").get("outcome"), "");
        var guard = list(outcome.get("trace"), "").stream().map(event -> map(event, ""))
                .filter(event -> "GUARD_EVALUATED".equals(event.get("event"))).findFirst().orElseThrow();
        assertEquals(true, guard.put("taken", false));
        var driver = new PublicScenarioDriver();
        AssertionError error = assertThrows(AssertionError.class, () -> driver.runScenario("main", document));
        assertTrue(error.getMessage().contains("Scenario rejected at semantics"), error.getMessage());
        assertTrue(error.getMessage().contains("Expected dispatch of a step with a false guard"), error.getMessage());
    }
}
