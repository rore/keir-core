package dev.keir.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;

/** Stops asynchronous waiting; host callbacks and underlying I/O must enforce their own deadlines. */
final class InvocationClock {
    private static final long MAX_CHUNK_MS = 2_147_483_647L;
    private static final ScheduledThreadPoolExecutor TIMER = timer();
    private final LongSupplier clock;
    private final long expiresAtMs;
    private final ReentrantLock clockLock = new ReentrantLock();
    private long last = -1;

    InvocationClock(LongSupplier clock, long expiresAtMs) {
        if (clock == null || expiresAtMs < 0 || expiresAtMs > Profile.MAX_INTEGER) throw invalid();
        this.clock = clock; this.expiresAtMs = expiresAtMs;
    }

    long remaining(String path) {
        clockLock.lock();
        try {
            long now;
            try { now = clock.getAsLong(); }
            catch (Throwable failure) { throw invalid(); }
            if (now < 0 || now > Profile.MAX_INTEGER || now < last) throw invalid();
            last = now;
            if (now >= expiresAtMs) throw new RuntimeFault("BUDGET_EXCEEDED", path);
            return expiresAtMs - now;
        } finally { clockLock.unlock(); }
    }

    CompletionStage<Object> waitFor(LongFunction<CompletionStage<?>> action, String path, String failureCode, Runnable onDispatch) {
        return waitForEffect(action, path, failureCode, onDispatch, null, null);
    }

    CompletionStage<Object> waitForEffect(LongFunction<CompletionStage<?>> action, String path, String failureCode,
                                        Runnable onDispatch, UnaryOperator<Object> onResponse, Runnable onSettled) {
        Wait wait = new Wait(path, failureCode, onResponse, onSettled);
        long budget;
        try {
            budget = remaining(path);
            wait.arm(budget);
            budget = wait.commit(onDispatch);
            if (budget < 0) return wait.result.minimalCompletionStage();
        } catch (Throwable failure) {
            wait.finish(null, failure instanceof RuntimeFault ? failure : invalid());
            return wait.result.minimalCompletionStage();
        }
        try {
            CompletionStage<?> pending = action.apply(budget);
            if (pending == null) wait.completed(null, null);
            else pending.whenComplete(wait::completed);
        } catch (Throwable failure) { wait.completed(null, failure); }
        return wait.result.minimalCompletionStage();
    }

    private final class Wait {
        private final String path, failureCode;
        private final UnaryOperator<Object> onResponse;
        private final Runnable onSettled;
        private final boolean effect;
        private final CompletableFuture<Object> result = new CompletableFuture<>();
        private final ReentrantLock stateLock = new ReentrantLock();
        private ScheduledFuture<?> timer;
        private boolean active = true;
        private boolean committed;

        Wait(String path, String failureCode, UnaryOperator<Object> onResponse, Runnable onSettled) {
            this.path = path; this.failureCode = failureCode; this.onResponse = onResponse; this.onSettled = onSettled;
            effect = onResponse != null || onSettled != null;
        }

        long commit(Runnable onDispatch) {
            Throwable failure;
            stateLock.lock();
            try {
                if (!active) return -1;
                try {
                    long budget = remaining(path);
                    if (onDispatch != null) onDispatch.run();
                    // Dispatch commits here; later cancellation stops waiting, not this in-flight host action.
                    committed = true;
                    return budget;
                } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
                close();
            } finally { stateLock.unlock(); }
            deliver(null, failure);
            return -1;
        }

        void arm(long left) {
            stateLock.lock();
            try {
                if (!active) return;
                long chunk = Math.min(left, MAX_CHUNK_MS);
                timer = TIMER.schedule(() -> {
                    // Core locks permit a cooperative host clock to park without pinning a carrier.
                    Thread.ofVirtual().name("keir-deadline-check").start(() -> expired(left, chunk));
                }, chunk, TimeUnit.MILLISECONDS);
            } finally { stateLock.unlock(); }
        }

        void expired(long left, long chunk) {
            if (effect && left <= chunk) {
                if (claim()) settled(null, new RuntimeFault("BUDGET_EXCEEDED", path), false);
                return;
            }
            Throwable failure;
            stateLock.lock();
            try {
                if (!active) return;
                try {
                    remaining(path);
                    if (left > chunk) { arm(left - chunk); return; }
                    failure = new RuntimeFault("BUDGET_EXCEEDED", path);
                } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
                close();
            } finally { stateLock.unlock(); }
            if (effect) settled(null, failure, false);
            else deliver(null, failure);
        }

        void completed(Object value, Throwable failure) {
            if (effect) {
                if (claim()) settled(value, failure == null ? null : new RuntimeFault(failureCode, path), failure == null);
                return;
            }
            stateLock.lock();
            try {
                if (!active) return;
                try {
                    remaining(path);
                    if (failure != null) failure = new RuntimeFault(failureCode, path);
                } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
                close();
            } finally { stateLock.unlock(); }
            deliver(value, failure);
        }

        private boolean claim() {
            stateLock.lock();
            try {
                if (!active) return false;
                close();
                return true;
            } finally { stateLock.unlock(); }
        }

        private void settled(Object value, Throwable failure, boolean response) {
            try {
                if (response && onResponse != null) value = onResponse.apply(value);
            } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
            finally {
                try { if (committed && onSettled != null) onSettled.run(); }
                catch (Throwable error) { if (failure == null) failure = error instanceof RuntimeFault ? error : invalid(); }
            }
            try { remaining(path); }
            catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
            deliver(value, failure);
        }

        void finish(Object value, Throwable failure) {
            stateLock.lock();
            try {
                if (!active) return;
                close();
            } finally { stateLock.unlock(); }
            deliver(value, failure);
        }

        private void close() {
            active = false;
            if (timer != null) timer.cancel(false);
        }

        private void deliver(Object value, Throwable failure) {
            // CompletionStage handlers may block; keep them off the shared timer and outside state locks.
            Thread.ofVirtual().name("keir-deadline-completion").start(() -> {
                if (failure == null) result.complete(value);
                else result.completeExceptionally(failure);
            });
        }
    }

    private static RuntimeFault invalid() { return new RuntimeFault("HOST_CONTEXT_INVALID", ""); }
    private static ScheduledThreadPoolExecutor timer() {
        var timer = new ScheduledThreadPoolExecutor(1, action -> {
            Thread thread = new Thread(action, "keir-deadline"); thread.setDaemon(true); return thread;
        });
        timer.setRemoveOnCancelPolicy(true);
        return timer;
    }
}
