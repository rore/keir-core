package dev.keir.examples;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** A known host's native path; counters observe attempts independently of decision consumption. */
class DecisionContinuationTest {
    private enum Selection { ABSENT, SELECTED, UNAVAILABLE }

    static final class Host {
        Selection selection = Selection.SELECTED;
        long now, expiry = 100, reserve = 10;
        boolean cancelled, authenticated = true, authorized = true, nativeValid = true;
        String input = "proposed", selectedProgram = "approved", approval = "current", readFact = "fresh";
        Object trustedKey = new Object(), suppliedKey = trustedKey;
        int evaluations, nativeAttempts;
        Map<String,Object> consumption;
        String nativeResult;

        void operate(Object outcome, Runnable duringEvaluation, boolean nativeFails) {
            var proposed = input; var program = selectedProgram; var approved = approval; var fact = readFact;
            long startedAt = now;
            long guardExpiry = expiry - reserve;
            if (selection == Selection.UNAVAILABLE || cancelled || now >= expiry) return;
            if (selection == Selection.SELECTED) {
                if (reserve <= 0 || now >= guardExpiry) return;
                evaluations++;
                duringEvaluation.run();
                consumption = DecisionConsumer.consumeDecision(outcome);
                if (!"ALLOWED".equals(consumption.get("status")) || now < startedAt || now >= guardExpiry) return;
                if (selection != Selection.SELECTED || !program.equals(selectedProgram) || !approved.equals(approval) ||
                        !proposed.equals(input) || !fact.equals(readFact)) return;
            }
            if (cancelled || now < startedAt || now >= expiry || !authenticated || !authorized || !nativeValid || suppliedKey != trustedKey) return;
            nativeAttempts++;
            nativeResult = nativeFails ? "NATIVE_FAILURE" : "COMMITTED";
        }
    }

    @Test void onlyAllowedOrAuthoritativeAbsenceReachesNativeCodeOnce() {
        for (String decision : List.of("ALLOW", "DENY", "OTHER")) {
            var host = new Host(); host.operate(DecisionConsumerTest.completed(decision), () -> {}, false);
            assertEquals(decision.equals("ALLOW") ? 1 : 0, host.nativeAttempts);
            assertEquals(1, host.evaluations);
        }
        var absent = new Host(); absent.selection = Selection.ABSENT;
        absent.operate(null, () -> fail("Absent selection must not invoke Keir"), false);
        assertEquals(1, absent.nativeAttempts); assertEquals(0, absent.evaluations); assertNull(absent.consumption);
        var unavailable = new Host(); unavailable.selection = Selection.UNAVAILABLE;
        unavailable.operate(DecisionConsumerTest.completed("ALLOW"), () -> fail("Unavailable binding"), false);
        assertEquals(0, unavailable.nativeAttempts); assertEquals(0, unavailable.evaluations);
    }

    @Test void handledRejectionFailureAndMalformedAllowNeverAttemptNativeMutation() {
        for (String classification : List.of("BUSINESS_REJECTION", "FAILED", "UNCERTAIN", "LIMIT_EXCEEDED")) {
            var outcome = DecisionConsumerTest.completed("ALLOW"); outcome.put("classification", classification);
            var host = new Host(); host.operate(outcome, () -> {}, false);
            assertEquals(0, host.nativeAttempts); assertEquals(Map.of("status", "BLOCKED"), host.consumption);
        }
    }

    @Test void freshHostChecksBlockChangesCancellationAndLateAllow() {
        for (java.util.function.Consumer<Host> race : List.<java.util.function.Consumer<Host>>of(
                h -> h.selectedProgram = "changed", h -> h.approval = "changed", h -> h.input = "changed",
                h -> h.readFact = "changed", h -> h.selection = Selection.ABSENT, h -> h.authorized = false,
                h -> h.authenticated = false, h -> h.nativeValid = false, h -> h.suppliedKey = new Object(),
                h -> h.cancelled = true, h -> h.now = -1, h -> h.now = 90, h -> h.now = 100)) {
            var host = new Host(); host.operate(DecisionConsumerTest.completed("ALLOW"), () -> race.accept(host), false);
            assertEquals(0, host.nativeAttempts);
        }
        var noBudget = new Host(); noBudget.now = 90;
        noBudget.operate(DecisionConsumerTest.completed("ALLOW"), () -> fail("No guard budget"), false);
        assertEquals(0, noBudget.evaluations); assertEquals(0, noBudget.nativeAttempts);
        var absent = new Host(); absent.selection = Selection.ABSENT; absent.authorized = false;
        absent.operate(null, () -> {}, false); assertEquals(0, absent.nativeAttempts);
        var cancelled = new Host();
        cancelled.operate(DecisionConsumerTest.completed("ALLOW"), () -> cancelled.cancelled = true, false);
        cancelled.operate(DecisionConsumerTest.completed("ALLOW"), () -> fail("Late ALLOW reopened cancelled attempt"), false);
        assertEquals(1, cancelled.evaluations); assertEquals(0, cancelled.nativeAttempts);
    }

    @Test void nativeFailureRemainsSeparateFromAllowedDecision() {
        var host = new Host(); host.operate(DecisionConsumerTest.completed("ALLOW"), () -> {}, true);
        assertEquals("ALLOWED", host.consumption.get("status"));
        assertEquals("NATIVE_FAILURE", host.nativeResult); assertEquals(1, host.nativeAttempts);
    }
}
