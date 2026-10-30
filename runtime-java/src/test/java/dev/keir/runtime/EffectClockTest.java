package dev.keir.runtime;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectClockTest {
    private static Object await(CompletionStage<Object> result) throws Exception {
        return result.toCompletableFuture().get(2, TimeUnit.SECONDS);
    }
    private static RuntimeFault failure(CompletionStage<Object> result, String code, String path) {
        ExecutionException error = assertThrows(ExecutionException.class, () -> await(result));
        RuntimeFault fault = assertInstanceOf(RuntimeFault.class, error.getCause());
        assertEquals(code, fault.code); assertEquals(path, fault.path);
        assertEquals(code, fault.getMessage()); assertNull(fault.getCause());
        return fault;
    }

    @Test void winningConfirmationAndReservedRecordPrecedeTheExpiryClockCheck() {
        List<String> order = new ArrayList<>(); AtomicLong now = new AtomicLong();
        AtomicReference<String> receipt = new AtomicReference<>("UNKNOWN");
        InvocationClock clock = new InvocationClock(() -> { order.add("clock:" + receipt.get()); return now.get(); }, 1000);
        var result = clock.waitForEffect(left -> {
            order.add("action"); return CompletableFuture.completedFuture("CONFIRMED");
        }, "/steps/0", "ADAPTER_EXCEPTION", () -> order.add("dispatch"), value -> {
            order.add("response"); receipt.set((String)value); now.set(1000); return value;
        }, () -> order.add("settled:" + receipt.get()));
        failure(result, "BUDGET_EXCEEDED", "/steps/0");
        assertEquals("CONFIRMED", receipt.get());
        assertEquals(List.of("clock:UNKNOWN", "clock:UNKNOWN", "dispatch", "action", "response", "settled:CONFIRMED", "clock:CONFIRMED"), order);
    }

    @Test void winningNoneEvidenceSurvivesARegressingOrThrowingCompletionClock() {
        for (String response : List.of("BUSINESS_REJECTION", "TRANSIENT_FAILURE")) for (boolean throwing : List.of(false, true)) {
            AtomicLong now = new AtomicLong(5); AtomicBoolean failClock = new AtomicBoolean();
            AtomicReference<String> receipt = new AtomicReference<>("UNKNOWN"); AtomicInteger records = new AtomicInteger();
            InvocationClock clock = new InvocationClock(() -> {
                if (failClock.get()) throw new IllegalStateException("sensitive clock detail"); return now.get();
            }, 1000);
            var result = clock.waitForEffect(left -> CompletableFuture.completedFuture(Map.of("kind", response, "effect", "NONE")),
                    "/steps/1", "ADAPTER_EXCEPTION", null, value -> {
                        receipt.set((String)((Map<?,?>)value).get("effect"));
                        if (throwing) failClock.set(true); else now.set(4);
                        return value;
                    }, () -> { assertEquals("NONE", receipt.get()); records.incrementAndGet(); });
            failure(result, "HOST_CONTEXT_INVALID", "");
            assertEquals("NONE", receipt.get()); assertEquals(1, records.get());
        }
    }

    @Test void responseTransformationIsReturnedWithoutTraversingItsValue() throws Exception {
        Object opaque = new Object() { @Override public String toString() { throw new AssertionError("No value conversion"); } };
        Map<String,Object> original = new LinkedHashMap<>(Map.of("kind", "SUCCESS", "effect", "CONFIRMED", "value", opaque));
        AtomicReference<Map<String,Object>> snapshot = new AtomicReference<>(); AtomicInteger records = new AtomicInteger();
        var result = new InvocationClock(() -> 0, 1000).waitForEffect(left -> CompletableFuture.completedFuture(original),
                "/steps/0", "ADAPTER_EXCEPTION", null, value -> {
                    var copy = new LinkedHashMap<String,Object>(); ((Map<?,?>)value).forEach((key,item) -> copy.put((String)key,item));
                    snapshot.set(Collections.unmodifiableMap(copy)); return snapshot.get();
                }, records::incrementAndGet);
        Object returned = await(result); original.clear();
        assertSame(snapshot.get(), returned); assertSame(opaque, snapshot.get().get("value"));
        assertEquals("CONFIRMED", snapshot.get().get("effect")); assertEquals(1, records.get());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.get().clear());
    }

    @Test void throwsAndRejectedStagesSettleOnceBeforeTheirClockCheckAndRedactTheCause() {
        for (boolean thrown : List.of(false, true)) {
            List<String> order = new ArrayList<>(); AtomicInteger records = new AtomicInteger(), responses = new AtomicInteger();
            InvocationClock clock = new InvocationClock(() -> { order.add("clock"); return 0; }, 1000);
            var result = clock.waitForEffect(left -> {
                order.add("action");
                if (thrown) throw new RuntimeFault("UNTRUSTED_CODE", "/untrusted");
                return CompletableFuture.failedFuture(new IllegalStateException("sensitive adapter detail"));
            }, "/steps/0", "ADAPTER_EXCEPTION", null, value -> { responses.incrementAndGet(); return value; },
                    () -> { order.add("settled:UNKNOWN"); records.incrementAndGet(); });
            failure(result, "ADAPTER_EXCEPTION", "/steps/0");
            assertEquals(0, responses.get()); assertEquals(1, records.get());
            assertEquals(List.of("clock", "clock", "action", "settled:UNKNOWN", "clock"), order);
        }
    }

    @Test void responseHookFailureStillSettlesAndTheCompletionClockHasFinalPriority() {
        for (boolean expired : List.of(false, true)) {
            AtomicLong now = new AtomicLong(); AtomicInteger records = new AtomicInteger();
            var result = new InvocationClock(now::get, 1000).waitForEffect(left -> CompletableFuture.completedFuture("malformed"),
                    "/steps/0", "ADAPTER_EXCEPTION", null, value -> {
                        if (expired) now.set(1000);
                        throw new RuntimeFault("ADAPTER_INVALID", "/steps/0");
                    }, records::incrementAndGet);
            failure(result, expired ? "BUDGET_EXCEEDED" : "ADAPTER_INVALID", "/steps/0");
            assertEquals(1, records.get());
        }
        AtomicInteger records = new AtomicInteger();
        var result = new InvocationClock(() -> 0, 1000).waitForEffect(left -> CompletableFuture.completedFuture("response"),
                "/steps/0", "ADAPTER_EXCEPTION", null, value -> { throw new IllegalStateException("sensitive hook detail"); }, records::incrementAndGet);
        failure(result, "HOST_CONTEXT_INVALID", ""); assertEquals(1, records.get());
    }

    @Test void timeoutWinsOnceAndLateResponseNeverRunsHooksOrReadsTheClock() {
        AtomicInteger reads = new AtomicInteger(), responses = new AtomicInteger(), records = new AtomicInteger();
        AtomicReference<String> receipt = new AtomicReference<>("UNKNOWN"); var pending = new CompletableFuture<Object>();
        InvocationClock clock = new InvocationClock(() -> { reads.incrementAndGet(); return 0; }, 25);
        var result = clock.waitForEffect(left -> pending, "/steps/0", "ADAPTER_EXCEPTION", null,
                value -> { responses.incrementAndGet(); receipt.set((String)value); return value; }, records::incrementAndGet);
        RuntimeFault timedOut = failure(result, "BUDGET_EXCEEDED", "/steps/0");
        int settledReads = reads.get();
        pending.complete("CONFIRMED");
        assertSame(timedOut, failure(result, "BUDGET_EXCEEDED", "/steps/0"));
        assertEquals("UNKNOWN", receipt.get()); assertEquals(0, responses.get()); assertEquals(1, records.get());
        assertEquals(settledReads, reads.get());
    }

    @Test void failedPrecommitClockOrTraceReserveCreatesNoIntentAndNoSettlement() {
        for (boolean clockFailure : List.of(false, true)) {
            AtomicInteger reads = new AtomicInteger(), actions = new AtomicInteger(), intents = new AtomicInteger(), records = new AtomicInteger();
            InvocationClock clock = new InvocationClock(() -> clockFailure && reads.incrementAndGet() > 1 ? 1000 : 0, 1000);
            var result = clock.waitForEffect(left -> { actions.incrementAndGet(); return CompletableFuture.completedFuture("CONFIRMED"); },
                    "/steps/0", "ADAPTER_EXCEPTION", () -> {
                        if (!clockFailure) throw new RuntimeFault("BUDGET_EXCEEDED", "/steps/0");
                        intents.incrementAndGet();
                    }, value -> { throw new AssertionError("No response before dispatch"); }, records::incrementAndGet);
            failure(result, "BUDGET_EXCEEDED", "/steps/0");
            assertEquals(0, actions.get()); assertEquals(0, intents.get()); assertEquals(0, records.get());
        }
    }

    @Test void aClaimedResponseFinishesItsHooksBeforeAnyClockReadAndIgnoresLateFailure() throws Exception {
        AtomicInteger reads = new AtomicInteger(), records = new AtomicInteger(); AtomicLong now = new AtomicLong();
        var pending = new CompletableFuture<Object>(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicReference<String> receipt = new AtomicReference<>("UNKNOWN");
        InvocationClock clock = new InvocationClock(() -> { reads.incrementAndGet(); return now.get(); }, 1000);
        var result = clock.waitForEffect(left -> pending, "/steps/0", "ADAPTER_EXCEPTION", null, value -> {
            receipt.set("CONFIRMED"); entered.countDown();
            try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            return value;
        }, records::incrementAndGet);
        Thread response = Thread.ofVirtual().start(() -> pending.complete("response"));
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertEquals(2, reads.get()); assertEquals(0, records.get());
            assertFalse(result.toCompletableFuture().isDone());
            assertFalse(pending.completeExceptionally(new IllegalStateException("late failure")));
            now.set(1000);
        } finally { release.countDown(); }
        response.join(); failure(result, "BUDGET_EXCEEDED", "/steps/0");
        assertEquals("CONFIRMED", receipt.get()); assertEquals(1, records.get()); assertEquals(3, reads.get());
    }
}
