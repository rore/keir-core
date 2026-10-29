package dev.keir.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;

/** Stops asynchronous waiting; host callbacks and underlying I/O must enforce their own deadlines. */
final class InvocationClock {
    private static final long MAX_CHUNK_MS = 2_147_483_647L;
    private static final ScheduledThreadPoolExecutor TIMER = timer();
    private final LongSupplier clock;
    private final long expiresAtMs;
    private long last = -1;

    InvocationClock(LongSupplier clock, long expiresAtMs) {
        if (clock == null || expiresAtMs < 0 || expiresAtMs > Profile.MAX_INTEGER) throw invalid();
        this.clock = clock; this.expiresAtMs = expiresAtMs;
    }

    synchronized long remaining(String path) {
        long now;
        try { now = clock.getAsLong(); }
        catch (Throwable failure) { throw invalid(); }
        if (now < 0 || now > Profile.MAX_INTEGER || now < last) throw invalid();
        last = now;
        if (now >= expiresAtMs) throw new RuntimeFault("BUDGET_EXCEEDED", path);
        return expiresAtMs - now;
    }

    CompletionStage<Object> waitFor(LongFunction<CompletionStage<?>> action, String path, String failureCode, Runnable onDispatch) {
        Wait wait = new Wait(path, failureCode);
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
        private final CompletableFuture<Object> result = new CompletableFuture<>();
        private ScheduledFuture<?> timer;
        private boolean active = true;

        Wait(String path, String failureCode) { this.path = path; this.failureCode = failureCode; }

        long commit(Runnable onDispatch) {
            Throwable failure;
            synchronized (this) {
                if (!active) return -1;
                try {
                    long budget = remaining(path);
                    if (onDispatch != null) onDispatch.run();
                    // Dispatch commits here; later cancellation stops waiting, not this in-flight host action.
                    return budget;
                } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
                close();
            }
            deliver(null, failure);
            return -1;
        }

        synchronized void arm(long left) {
            if (!active) return;
            long chunk = Math.min(left, MAX_CHUNK_MS);
            timer = TIMER.schedule(() -> {
                // Host clock reads may block under monitors; keep them off shared scheduling threads.
                Thread.ofPlatform().daemon().name("keir-deadline-check").start(() -> expired(left, chunk));
            }, chunk, TimeUnit.MILLISECONDS);
        }

        void expired(long left, long chunk) {
            Throwable failure;
            synchronized (this) {
                if (!active) return;
                try {
                    remaining(path);
                    if (left > chunk) { arm(left - chunk); return; }
                    failure = new RuntimeFault("BUDGET_EXCEEDED", path);
                } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
                close();
            }
            deliver(null, failure);
        }

        void completed(Object value, Throwable failure) {
            synchronized (this) {
                if (!active) return;
                try {
                    remaining(path);
                    if (failure != null) failure = new RuntimeFault(failureCode, path);
                } catch (Throwable error) { failure = error instanceof RuntimeFault ? error : invalid(); }
                close();
            }
            deliver(value, failure);
        }

        void finish(Object value, Throwable failure) {
            synchronized (this) {
                if (!active) return;
                close();
            }
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
