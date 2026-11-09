import dev.keir.runtime.KeirRuntime;
import dev.keir.runtime.KeirRuntime.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import static java.util.concurrent.CompletableFuture.completedFuture;

/** Simulation: the counter observes native continuation, not a durable native commit. */
class BeforeOperation {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Expected at most one artifact path");
        var model = Files.readAllBytes(Path.of("examples/node-host/pre-operation/dcm.json"));
        var fixture = Path.of("conformance/keir-3");
        var artifact = Files.readAllBytes(args.length == 0 ? fixture.resolve("artifact-guard.json") : Path.of(args[0]));
        if (args.length == 0) {
            var expected = Files.readString(fixture.resolve("artifact-guard.sha256")).strip();
            var actual = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(artifact));
            if (!expected.equals(actual)) throw new IllegalStateException("Pinned example artifact digest mismatch");
        }
        var selectedCase = new AtomicReference<String>();
        var trustedKey = new AtomicReference<UUID>();
        var extensionWrites = new AtomicInteger();
        var read = new BoundEffectAdapter("1", "simulation", "sample.OperationKey/1",
            (business, protectedArgs, remainingMs, context) -> {
                if (!business.isEmpty() || !Map.of("actorId", "user1").equals(protectedArgs) ||
                        remainingMs <= 0 || context.requestKey() != trustedKey.get() ||
                        !Map.of("id", "attempt-" + selectedCase.get(), "source", "scope1").equals(context.event()))
                    throw new IllegalStateException("Invalid trusted invocation");
                if (selectedCase.get().equals("unavailable"))
                    return completedFuture(Map.of("kind", "BUSINESS_REJECTION", "code", "ACCESS_DENIED"));
                return completedFuture(Map.of("kind", "SUCCESS", "value", Map.of("restricted", selectedCase.get().equals("deny"))));
            });
        var unusedWrite = new BoundEffectAdapter("1", "simulation", "sample.OperationKey/1",
            (business, protectedArgs, remainingMs, context) -> {
                extensionWrites.incrementAndGet();
                throw new IllegalStateException("A guard cannot dispatch a WRITE");
            });
        var binding = KeirRuntime.bindModel(model, Map.of("policy.get", read, "native.write", unusedWrite));
        if (!binding.succeeded()) throw new IllegalStateException(binding.diagnostic().code());
        var program = KeirRuntime.prepareProgram(artifact, binding.boundModel());
        if (!program.succeeded()) throw new IllegalStateException(program.diagnostic().code());
        long start = System.nanoTime();
        LongSupplier clock = () -> (System.nanoTime() - start) / 1_000_000;
        int totalNativeAttempts = 0;
        for (var name : List.of("allow", "deny", "unavailable")) {
            selectedCase.set(name); trustedKey.set(UUID.randomUUID());
            long nativeExpiry = clock.getAsLong() + 1000, reserveMs = 100;
            var invocation = new BoundEffectInvocation(Map.of("actorId", "user1"), new Deadline(clock, nativeExpiry - reserveMs),
                trustedKey.get(), Map.of("id", "attempt-" + name, "source", "scope1"));
            var outcome = KeirRuntime.invokeBoundEffectProgram(program.prepared(), Map.of("restricted", false), invocation)
                .toCompletableFuture().join();
            var consumption = consumeDecision(outcome);
            var status = (String) consumption.get("status");
            int nativeAttempts = 0;
            if (status.equals("ALLOWED")) {
                // A real host rechecks authorization, proposal/selection/read consistency and deadline atomically before its write.
                if (invocation.requestKey() != trustedKey.get() || clock.getAsLong() >= nativeExpiry)
                    throw new IllegalStateException("Native continuation is no longer authorized");
                nativeAttempts++; totalNativeAttempts++;
            }
            var expected = name.equals("allow") ? "ALLOWED" : name.equals("deny") ? "DENIED" : "BLOCKED";
            if (!status.equals(expected) || nativeAttempts != (name.equals("allow") ? 1 : 0) || extensionWrites.get() != 0)
                throw new IllegalStateException("Unexpected decision or native continuation");
            var message = status.equals("DENIED") ? (String) outcome.result().getOrDefault("message", "") : "";
            System.out.printf("%s %s | native attempts=%d%s%n", name, status, nativeAttempts, message.isEmpty() ? "" : " | " + message);
        }
        if (totalNativeAttempts != 1) throw new IllegalStateException("Unexpected native attempt count");
    }

    private static Map<String,Object> consumeDecision(Outcome outcome) {
        if (outcome == null || !outcome.classification().equals("COMPLETED") || outcome.error() != null || !outcome.effects().isEmpty())
            return Map.of("status", "BLOCKED");
        // Outcome is the immutable validated result of this host's own invocation, never customer-supplied JSON.
        var result = outcome.result();
        if (result == null || !result.containsKey("decision") || !Set.of("decision", "message").containsAll(result.keySet()) ||
                !(result.get("decision") instanceof String decision) || !Set.of("ALLOW", "DENY").contains(decision))
            return Map.of("status", "BLOCKED");
        if (result.containsKey("message") && (!(result.get("message") instanceof String message) ||
                message.getBytes(StandardCharsets.UTF_8).length > 256)) return Map.of("status", "BLOCKED");
        return Map.of("status", decision.equals("ALLOW") ? "ALLOWED" : "DENIED", "result", result);
    }
}
