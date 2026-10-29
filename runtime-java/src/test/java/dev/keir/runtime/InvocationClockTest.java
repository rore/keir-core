package dev.keir.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvocationClockTest {
    private static Object await(CompletionStage<Object> stage) throws Exception {
        return stage.toCompletableFuture().get(2, TimeUnit.SECONDS);
    }
    private static RuntimeFault failure(CompletionStage<Object> stage, String code, String path) {
        ExecutionException error = assertThrows(ExecutionException.class, () -> await(stage));
        RuntimeFault fault = assertInstanceOf(RuntimeFault.class, error.getCause());
        assertEquals(code, fault.code); assertEquals(path, fault.path);
        assertEquals(code, fault.getMessage()); assertNull(fault.getCause());
        return fault;
    }
    private static RuntimeFault remainingFailure(InvocationClock clock, String code, String path) {
        RuntimeFault fault = assertThrows(RuntimeFault.class, () -> clock.remaining("/steps/0"));
        assertEquals(code, fault.code); assertEquals(path, fault.path); return fault;
    }

    @Test void immediateCompletionReceivesBudgetAfterDispatchAndRechecksTheClock() throws Exception {
        List<String> order = new ArrayList<>();
        InvocationClock clock = new InvocationClock(() -> { order.add("clock"); return 5; }, 1005);
        var result = clock.waitFor(left -> {
            order.add("action"); assertEquals(1000, left);
            return CompletableFuture.completedFuture("value");
        }, "/steps/0", "ADAPTER_EXCEPTION", () -> order.add("dispatch"));
        assertEquals("value", await(result));
        assertEquals(List.of("clock", "clock", "dispatch", "action", "clock"), order);
        assertNull(await(clock.waitFor(left -> null, "/steps/0", "ADAPTER_EXCEPTION", null)));
    }

    @Test void stationaryLogicalClockStillHasAFiniteNativeTimeoutAndIgnoresLateResults() {
        AtomicInteger reads = new AtomicInteger();
        InvocationClock clock = new InvocationClock(() -> { reads.incrementAndGet(); return 0; }, 25);
        var pending = new CompletableFuture<Object>();
        var result = clock.waitFor(left -> pending, "/steps/0", "ADAPTER_EXCEPTION", null);
        RuntimeFault timedOut = failure(result, "BUDGET_EXCEEDED", "/steps/0");
        int readsAtTimeout = reads.get();
        assertTrue(pending.complete("late"));
        assertSame(timedOut, failure(result, "BUDGET_EXCEEDED", "/steps/0"));
        assertEquals(readsAtTimeout, reads.get());
    }

    @Test void actionExceptionsAndRejectedStagesAreRedacted() {
        InvocationClock clock = new InvocationClock(() -> 0, 1000);
        failure(clock.waitFor(left -> { throw new IllegalStateException("sensitive detail"); }, "/steps/0", "ADAPTER_EXCEPTION", null),
                "ADAPTER_EXCEPTION", "/steps/0");
        failure(clock.waitFor(left -> CompletableFuture.failedFuture(new RuntimeFault("UNTRUSTED_CODE", "/untrusted")),
                "/steps/1", "AUTHORITY_CHECK_FAILED", null), "AUTHORITY_CHECK_FAILED", "/steps/1");
        failure(clock.waitFor(left -> { throw new AssertionError("sensitive detail"); }, "", "AUTHORITY_CHECK_FAILED", null),
                "AUTHORITY_CHECK_FAILED", "");
    }

    @Test void actionFailureRechecksClockBeforeChoosingItsSafeFailureCode() {
        AtomicLong now = new AtomicLong(); InvocationClock clock = new InvocationClock(now::get, 1000);
        failure(clock.waitFor(left -> { now.set(1000); throw new IllegalStateException("detail"); }, "/steps/0", "ADAPTER_EXCEPTION", null),
                "BUDGET_EXCEEDED", "/steps/0");
        now.set(0); clock = new InvocationClock(now::get, 1000); var rejected = new CompletableFuture<Object>();
        var result = clock.waitFor(left -> rejected, "/steps/1", "AUTHORITY_CHECK_FAILED", null);
        now.set(1000); rejected.completeExceptionally(new IllegalStateException("detail"));
        failure(result, "BUDGET_EXCEEDED", "/steps/1");
    }

    @Test void successfulCompletionAtExpiryOrAfterRegressionNeverReturnsAValue() {
        AtomicLong now = new AtomicLong(5); InvocationClock clock = new InvocationClock(now::get, 1000);
        var pending = new CompletableFuture<Object>();
        var result = clock.waitFor(left -> pending, "/steps/0", "ADAPTER_EXCEPTION", null);
        now.set(1000); pending.complete("too late"); failure(result, "BUDGET_EXCEEDED", "/steps/0");
        now.set(5); clock = new InvocationClock(now::get, 1000); var other = new CompletableFuture<Object>();
        result = clock.waitFor(left -> other, "/return", "ADAPTER_EXCEPTION", null);
        now.set(4); other.complete("invalid clock"); failure(result, "HOST_CONTEXT_INVALID", "");
    }

    @Test void validatesClockAndExpiryBoundsWithoutReadingDuringConstruction() {
        assertThrows(RuntimeFault.class, () -> new InvocationClock(null, 1));
        for (long expires : List.of(-1L, Profile.MAX_INTEGER + 1)) {
            RuntimeFault fault = assertThrows(RuntimeFault.class, () -> new InvocationClock(() -> 0, expires));
            assertEquals("HOST_CONTEXT_INVALID", fault.code); assertEquals("", fault.path);
        }
        AtomicInteger reads = new AtomicInteger();
        InvocationClock clock = new InvocationClock(() -> { reads.incrementAndGet(); return Profile.MAX_INTEGER - 1; }, Profile.MAX_INTEGER);
        assertEquals(0, reads.get()); assertEquals(1, clock.remaining(""));
        assertEquals(Profile.MAX_INTEGER, new InvocationClock(() -> 0, Profile.MAX_INTEGER).remaining(""));
        for (long now : List.of(-1L, Profile.MAX_INTEGER + 1))
            remainingFailure(new InvocationClock(() -> now, Profile.MAX_INTEGER), "HOST_CONTEXT_INVALID", "");
        remainingFailure(new InvocationClock(() -> 0, 0), "BUDGET_EXCEEDED", "/steps/0");
        remainingFailure(new InvocationClock(() -> Profile.MAX_INTEGER, Profile.MAX_INTEGER), "BUDGET_EXCEEDED", "/steps/0");
    }

    @Test void clockThrowsAndRegressionsUseEmptyHostContextPath() {
        remainingFailure(new InvocationClock(() -> { throw new IllegalStateException("sensitive clock detail"); }, 10), "HOST_CONTEXT_INVALID", "");
        AtomicLong now = new AtomicLong(2); InvocationClock clock = new InvocationClock(now::get, 10);
        assertEquals(8, clock.remaining("/steps/0")); assertEquals(8, clock.remaining("/steps/1"));
        now.set(1); remainingFailure(clock, "HOST_CONTEXT_INVALID", "");
        AtomicInteger reads = new AtomicInteger();
        InvocationClock throwing = new InvocationClock(() -> {
            if (reads.incrementAndGet() > 1) throw new IllegalArgumentException("sensitive clock detail"); return 0;
        }, 1000);
        failure(throwing.waitFor(left -> CompletableFuture.failedFuture(new IllegalStateException("detail")), "/steps/0", "ADAPTER_EXCEPTION", null),
                "HOST_CONTEXT_INVALID", "");
    }

    @Test void expiredOrFailedDispatchNeverCallsTheAction() {
        AtomicInteger actions = new AtomicInteger();
        var expired = new InvocationClock(() -> 1, 1).waitFor(left -> { actions.incrementAndGet(); return null; }, "/steps/0", "ADAPTER_EXCEPTION", actions::incrementAndGet);
        failure(expired, "BUDGET_EXCEEDED", "/steps/0"); assertEquals(0, actions.get());
        InvocationClock clock = new InvocationClock(() -> 0, 1000);
        failure(clock.waitFor(left -> { actions.incrementAndGet(); return null; }, "/steps/1", "ADAPTER_EXCEPTION",
                () -> { throw new RuntimeFault("BUDGET_EXCEEDED", "/steps/1"); }), "BUDGET_EXCEEDED", "/steps/1");
        failure(clock.waitFor(left -> { actions.incrementAndGet(); return null; }, "/steps/1", "ADAPTER_EXCEPTION",
                () -> { throw new IllegalStateException("sensitive dispatch detail"); }), "HOST_CONTEXT_INVALID", "");
        assertEquals(0, actions.get());
    }

    @Test void aBlockedTimeoutContinuationCannotPreventAnotherWaitFromExpiring() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        var first = new InvocationClock(() -> 0, 25).waitFor(left -> new CompletableFuture<>(), "/steps/0", "ADAPTER_EXCEPTION", null);
        var blocked = first.whenComplete((value, error) -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        });
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var second = new InvocationClock(() -> 0, 25).waitFor(left -> new CompletableFuture<>(), "/steps/1", "ADAPTER_EXCEPTION", null);
            failure(second, "BUDGET_EXCEEDED", "/steps/1");
            assertFalse(blocked.toCompletableFuture().isDone());
        } finally { release.countDown(); }
        failure(blocked, "BUDGET_EXCEEDED", "/steps/0");
    }

    @Test void aBlockedExpiryClockCannotPreventAnotherWaitFromExpiring() throws Exception {
        Thread caller = Thread.currentThread();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean dispatched = new AtomicBoolean();
        var first = new InvocationClock(() -> {
            if (dispatched.get() && Thread.currentThread() != caller) {
                entered.countDown();
                try { release.await(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
            return 0;
        }, 100).waitFor(left -> { dispatched.set(true); return new CompletableFuture<>(); }, "/steps/0", "ADAPTER_EXCEPTION", null);
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS), "First wait must reach its expiry clock read");
            var second = new InvocationClock(() -> 0, 25).waitFor(left -> new CompletableFuture<>(), "/steps/1", "ADAPTER_EXCEPTION", null);
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> second.toCompletableFuture().get(5, TimeUnit.SECONDS));
            RuntimeFault fault = assertInstanceOf(RuntimeFault.class, error.getCause());
            assertEquals("BUDGET_EXCEEDED", fault.code); assertEquals("/steps/1", fault.path);
            assertFalse(first.toCompletableFuture().isDone(), "The blocking host clock itself is not preempted");
        } finally { release.countDown(); }
        failure(first, "BUDGET_EXCEEDED", "/steps/0");
    }

    @Test void expiryBeforeDispatchCommitPreventsBothTraceHookAndHostAction() {
        AtomicInteger reads = new AtomicInteger(), dispatches = new AtomicInteger(), actions = new AtomicInteger();
        InvocationClock clock = new InvocationClock(() -> reads.incrementAndGet() == 1 ? 0 : 1000, 1000);
        var result = clock.waitFor(left -> { actions.incrementAndGet(); return CompletableFuture.completedFuture("value"); },
                "/steps/0", "ADAPTER_EXCEPTION", dispatches::incrementAndGet);
        failure(result, "BUDGET_EXCEEDED", "/steps/0");
        assertEquals(0, dispatches.get()); assertEquals(0, actions.get());
    }

    @Test void hostActionReceivesTheRemainingBudgetAtDispatchCommit() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        InvocationClock clock = new InvocationClock(() -> reads.incrementAndGet() == 1 ? 0 : 5, 10);
        var result = clock.waitFor(left -> {
            assertEquals(5, left);
            return CompletableFuture.completedFuture("value");
        }, "/steps/0", "ADAPTER_EXCEPTION", null);
        assertEquals("value", await(result));
    }
}
