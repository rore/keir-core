import dev.keir.runtime.KeirRuntime;
import dev.keir.runtime.KeirRuntime.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import static java.util.concurrent.CompletableFuture.completedFuture;

class FollowUp {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Expected at most one artifact path");

        var model = Files.readAllBytes(Path.of("examples/node-host/follow-up/dcm.json"));
        var fixture = Path.of("conformance/keir-1");
        var artifactPath = args.length == 0 ? fixture.resolve("artifact.json") : Path.of(args[0]);
        var artifact = Files.readAllBytes(artifactPath);
        if (args.length == 0) {
            var expected = Files.readString(fixture.resolve("artifact.sha256")).strip();
            var actual = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(artifact));
            if (!actual.equals(expected)) throw new IllegalStateException("Pinned example artifact digest mismatch");
        }
        var projects = Map.of("task1", "project1", "task2", "project2", "task3", "project1");
        var read = new BoundEffectAdapter("1", "simulation", "sample.RequestUuid/1",
            (business, protectedArgs, remainingMs, context) -> {
                var task = (String) business.get("taskId");
                var project = projects.get(task);
                if (project == null) {
                    return completedFuture(Map.of("kind", "BUSINESS_REJECTION", "code", "TASK_NOT_FOUND"));
                }
                return completedFuture(Map.of("kind", "SUCCESS", "value",
                    Map.of("id", task, "projectId", project, "status", "COMPLETED")));
            });
        var createdTitle = new AtomicReference<String>();
        var create = new BoundEffectAdapter("1", "simulation", "sample.RequestUuid/1",
            (business, protectedArgs, remainingMs, context) -> {
                if (context.event().get("source").equals("task3")) {
                    return completedFuture(Map.of("kind", "UNKNOWN_COMMIT", "effect", "UNKNOWN"));
                }
                createdTitle.set((String) business.get("title"));
                return completedFuture(Map.of("kind", "SUCCESS", "effect", "CONFIRMED",
                    "value", Map.of("id", "follow-up")));
            });
        var binding = KeirRuntime.bindModel(model, Map.of("tasks.get", read, "tasks.create", create));
        if (!binding.succeeded()) throw new IllegalStateException(binding.diagnostic().code());
        var program = KeirRuntime.prepareProgram(artifact, binding.boundModel());
        if (!program.succeeded()) throw new IllegalStateException(program.diagnostic().code());

        long start = System.nanoTime();
        LongSupplier clock = () -> (System.nanoTime() - start) / 1_000_000;
        for (var id : List.of("task1", "task2", "task3")) {
            createdTitle.set(null);
            var invocation = new BoundEffectInvocation(
                Map.of("actorId", "demo-user"), new Deadline(clock, clock.getAsLong() + 1000),
                UUID.randomUUID(), Map.of("id", "completion-" + id, "source", id));
            var outcome = KeirRuntime.invokeBoundEffectProgram(
                program.prepared(), Map.of("taskId", id), invocation).toCompletableFuture().join();
            var receipt = outcome.effects().isEmpty() ? "-" : outcome.effects().getFirst().get("state");
            if (!outcome.classification().equals("COMPLETED") && !outcome.classification().equals("UNCERTAIN")) {
                throw new IllegalStateException(outcome.error().get("code").toString());
            }
            var status = outcome.classification().equals("COMPLETED") ? outcome.result().get("status") : "UNCERTAIN";
            var title = createdTitle.get();
            System.out.printf("%s %s %s%s%n", id, status, receipt, title == null ? "" : " | " + title);
        }
    }
}
