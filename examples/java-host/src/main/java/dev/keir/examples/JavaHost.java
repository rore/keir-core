package dev.keir.examples;

import dev.keir.runtime.KeirRuntime;
import dev.keir.runtime.KeirRuntime.Adapter;
import dev.keir.runtime.KeirRuntime.Invocation;
import dev.keir.runtime.KeirRuntime.Preparation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class JavaHost {
  private static final String DCM_DIGEST = "sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f";
  private static final int MAX_ARTIFACT_BYTES = 1_048_576;

  private JavaHost() {}

  public static void main(String[] args) {
    System.exit(run(args, System.out, System.err));
  }

  public static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length != 2) {
      err.println("Usage: java -jar keir-java-host.jar <artifact.json> <expected-sha256>");
      return 2;
    }
    try {
      var scenarios = execute(readArtifact(Path.of(args[0])), args[1]);
      var output = new StringJoiner(",", "[", "]");
      for (var scenario : scenarios) {
        var outcome = scenario.outcome();
        output.add("{\"scenario\":\"" + scenario.name() + "\",\"outcome\":"
            + new String(outcome.toJson(), StandardCharsets.UTF_8) + ",\"consumption\":"
            + receiptJson(consume(outcome.classification(), outcome.result(), outcome.error())) + "}");
      }
      out.println(output);
      return 0;
    } catch (Exception e) {
      err.println("The selected sample artifact could not be executed.");
      return 1;
    }
  }

  static byte[] readArtifact(Path path) throws IOException {
    try (var input = Files.newInputStream(path)) {
      byte[] bytes = input.readNBytes(MAX_ARTIFACT_BYTES + 1);
      if (bytes.length > MAX_ARTIFACT_BYTES) throw new IOException("Artifact is too large");
      return bytes;
    }
  }

  record Scenario(String name, KeirRuntime.Outcome outcome) {}

  static List<Scenario> execute(byte[] bytes, String expectedArtifactDigest) {
    var admission = Map.<String, Object>of(
        "expectedArtifactDigest", expectedArtifactDigest,
        "expectedDcmDigest", DCM_DIGEST,
        "entrypoint", "ClassifyTask",
        "consumer", Map.of("id", "sample.classification", "version", "1", "kind", "DISPLAY"),
        "compiler", "keir-compiler-java/0.1.0-SNAPSHOT",
        "environment", KeirRuntime.ENVIRONMENT,
        "mode", "simulation",
        "grants", Map.of("tasks.get", 1));
    var adapter = new Adapter("1", "simulation", JavaHost::readTask);
    Preparation preparation = KeirRuntime.prepare(bytes, admission, Map.of("tasks.get", adapter));
    if (!preparation.succeeded()) throw new IllegalArgumentException("Artifact admission failed");

    var results = new ArrayList<Scenario>();
    for (String scenario : List.of("completed", "open", "missing", "wrong-task")) {
      long started = System.nanoTime();
      LongSupplier nowMs = () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
      var invocation = new Invocation(
          Map.of("actorId", "sample-user"),
          new KeirRuntime.Deadline(nowMs, 1000),
          JavaHost::authorize,
          Map.of("tasks.get", 1L));
      var outcome = KeirRuntime.invoke(preparation.prepared(), Map.of("taskId", scenario), invocation)
          .toCompletableFuture().join();
      results.add(new Scenario(scenario, outcome));
    }
    return List.copyOf(results);
  }

  static CompletionStage<?> readTask(Map<String, Object> agentArgs, Map<String, Object> protectedArgs, long remainingMs) {
    if (remainingMs <= 0 || !(agentArgs.get("taskId") instanceof String taskId)
        || !agentArgs.equals(Map.of("taskId", taskId)) || !protectedArgs.equals(Map.of("actorId", "sample-user"))) {
      return CompletableFuture.failedFuture(new IllegalStateException("Sample adapter rejected the request"));
    }
    Map<String, Object> row = switch (taskId) {
      case "completed" -> Map.of("id", taskId, "status", "COMPLETED");
      case "open" -> Map.of("id", taskId, "status", "OPEN");
      case "wrong-task" -> Map.of("id", "different-task", "status", "COMPLETED");
      default -> null;
    };
    if (row == null) return CompletableFuture.completedFuture(Map.of("kind", "BUSINESS_REJECTION", "code", "TASK_NOT_FOUND"));
    // The host adapter owns the requested-resource relationship; the generic runtime cannot infer it.
    if (!taskId.equals(row.get("id"))) return CompletableFuture.failedFuture(new IllegalStateException("Sample adapter returned the wrong resource"));
    return CompletableFuture.completedFuture(Map.of("kind", "SUCCESS", "value", row));
  }

  static CompletionStage<?> authorize(Map<String, Object> request) {
    boolean allowed = request.equals(Map.of("entrypoint", "ClassifyTask"))
        || request.equals(Map.of("entrypoint", "ClassifyTask", "capability", "tasks.get", "version", "1"));
    return CompletableFuture.completedFuture(allowed);
  }

  static Map<String, Object> consume(String classification, Map<String, Object> result, Map<String, Object> error) {
    if (!"COMPLETED".equals(classification)) return Map.of("status", "NOT_DISPLAYED", "code", safeErrorCode(error));
    if (error != null || result == null || result.size() != 1 || !(result.get("classification") instanceof String value)
        || !(value.equals("DONE") || value.equals("PENDING"))) {
      return Map.of("status", "NOT_DISPLAYED", "code", "INVALID_RESULT");
    }
    return Map.of("status", "DISPLAYED", "result", Map.of("classification", value));
  }

  private static String safeErrorCode(Map<String, Object> error) {
    Object code = error == null ? null : error.get("code");
    return code instanceof String value && value.matches("[A-Z][A-Z0-9_]{0,63}") ? value : "INVALID_OUTCOME";
  }

  private static String receiptJson(Map<String, Object> receipt) {
    if ("DISPLAYED".equals(receipt.get("status"))) {
      String classification = (String) ((Map<?, ?>) receipt.get("result")).get("classification");
      return "{\"status\":\"DISPLAYED\",\"result\":{\"classification\":\"" + classification + "\"}}";
    }
    return "{\"status\":\"NOT_DISPLAYED\",\"code\":\"" + receipt.get("code") + "\"}";
  }
}
