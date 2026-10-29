package dev.keir.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import com.fasterxml.jackson.core.JsonFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaHostTest {
  private static final String DIGEST = "sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424";
  @TempDir Path tempDir;

  @Test
  void cliRunsTheFourPinnedScenariosInOneBatch() throws Exception {
    Path artifact = tempDir.resolve("artifact.json");
    copyFixture(artifact);
    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();

    assertEquals(0, JavaHost.run(new String[] {artifact.toString(), DIGEST},
        new PrintStream(stdout), new PrintStream(stderr)));
    String output = stdout.toString();
    assertTrue(stderr.toString().isEmpty());
    var values = new LinkedHashMap<String, String>();
    try (var parser = new JsonFactory().createParser(stdout.toByteArray())) {
      while (parser.nextToken() != null) if (parser.currentToken().isScalarValue())
        values.put(parser.getParsingContext().pathAsPointer().toString(), parser.getValueAsString());
    }
    assertEquals("completed", values.get("/0/scenario"));
    assertEquals("open", values.get("/1/scenario"));
    assertEquals("missing", values.get("/2/scenario"));
    assertEquals("wrong-task", values.get("/3/scenario"));
    assertFalse(values.containsKey("/4/scenario"));
    for (int index = 0; index < 4; index++) {
      String prefix = "/" + index;
      String expected = new String[] {"DONE", "PENDING", "TASK_NOT_FOUND", "ADAPTER_EXCEPTION"}[index];
      assertEquals(new String[] {"COMPLETED", "COMPLETED", "BUSINESS_REJECTION", "FAILED"}[index], values.get(prefix + "/outcome/classification"));
      assertEquals(index < 2 ? "DISPLAYED" : "NOT_DISPLAYED", values.get(prefix + "/consumption/status"));
      assertEquals(expected, values.get(prefix + (index < 2 ? "/outcome/result/classification" : "/outcome/error/code")));
      assertEquals(expected, values.get(prefix + (index < 2 ? "/consumption/result/classification" : "/consumption/code")));
      if (index >= 2) assertFalse(values.containsKey(prefix + "/outcome/result/classification"));
    }
    assertFalse(output.contains("sample-user"));
    assertFalse(output.contains("sample-task"));
  }

  @Test
  void cliRejectsBadPinsArgumentCountsAndMissingFilesWithoutLeakingDetails() throws Exception {
    Path artifact = tempDir.resolve("artifact.json");
    copyFixture(artifact);
    for (String digest : new String[] {"sha256:" + "0".repeat(64), "bad-pin"}) {
      var stdout = new ByteArrayOutputStream();
      var stderr = new ByteArrayOutputStream();
      assertEquals(1, JavaHost.run(new String[] {artifact.toString(), digest},
          new PrintStream(stdout), new PrintStream(stderr)));
      assertTrue(stdout.toString().isEmpty());
      assertEquals("The selected sample artifact could not be executed." + System.lineSeparator(), stderr.toString());
    }
    for (String[] args : new String[][] {{}, {artifact.toString(), DIGEST, "extra"}}) {
      var stderr = new ByteArrayOutputStream();
      assertEquals(2, JavaHost.run(args, new PrintStream(new ByteArrayOutputStream()), new PrintStream(stderr)));
      assertTrue(stderr.toString().contains("Usage:"));
    }
    var missingError = new ByteArrayOutputStream();
    assertEquals(1, JavaHost.run(new String[] {tempDir.resolve("missing.json").toString(), DIGEST},
        new PrintStream(new ByteArrayOutputStream()), new PrintStream(missingError)));
    assertEquals("The selected sample artifact could not be executed." + System.lineSeparator(), missingError.toString());
  }

  @Test
  void artifactReaderEnforcesTheInclusiveSizeLimit() throws Exception {
    Path max = tempDir.resolve("max.json");
    Files.write(max, new byte[1_048_576]);
    assertEquals(1_048_576, JavaHost.readArtifact(max).length);
    Path tooLarge = tempDir.resolve("too-large.json");
    Files.write(tooLarge, new byte[1_048_577]);
    assertThrows(java.io.IOException.class, () -> JavaHost.readArtifact(tooLarge));
    var stdout = new ByteArrayOutputStream();
    var stderr = new ByteArrayOutputStream();
    assertEquals(1, JavaHost.run(new String[] {tooLarge.toString(), DIGEST}, new PrintStream(stdout), new PrintStream(stderr)));
    assertEquals(0, stdout.size());
    assertEquals("The selected sample artifact could not be executed." + System.lineSeparator(), stderr.toString());
  }

  @Test
  void independentConsumerReturnsOnlyClosedReceipts() {
    assertEquals(Map.of("status", "DISPLAYED", "result", Map.of("classification", "DONE")),
        JavaHost.consume("COMPLETED", Map.of("classification", "DONE"), null));
    assertEquals(Map.of("status", "DISPLAYED", "result", Map.of("classification", "PENDING")),
        JavaHost.consume("COMPLETED", Map.of("classification", "PENDING"), null));
    assertNotDisplayed(Map.of("classification", "DONE", "extra", true));
    assertNotDisplayed(Map.of("classification", 1));
    assertNotDisplayed(Map.of("classification", "OTHER"));
    assertNotDisplayed(Map.of("missing", "DONE"));
    assertNotDisplayed(Collections.singletonMap("classification", null));
    assertNotDisplayed(null);
    assertEquals(Map.of("status", "NOT_DISPLAYED", "code", "ACCESS_DENIED"),
        JavaHost.consume("FAILED", null, Map.of("code", "ACCESS_DENIED")));
    assertEquals(Map.of("status", "NOT_DISPLAYED", "code", "INVALID_OUTCOME"),
        JavaHost.consume("FAILED", null, Map.of("code", "ACCESS_DENIED\"\\\n")));
    assertEquals(Map.of("status", "NOT_DISPLAYED", "code", "INVALID_OUTCOME"),
        JavaHost.consume("FAILED", null, null));
    assertEquals(Map.of("status", "NOT_DISPLAYED", "code", "INVALID_OUTCOME"),
        JavaHost.consume("FAILED", null, Map.of("code", 1)));
    assertEquals(Map.of("status", "NOT_DISPLAYED", "code", "INVALID_RESULT"),
        JavaHost.consume("COMPLETED", Map.of("classification", "DONE"), Map.of("code", "IGNORED")));
  }

  @Test
  void authorizationAndAdapterAcceptOnlyTheSampleRequests() {
    assertTrue((Boolean) JavaHost.authorize(Map.of("entrypoint", "ClassifyTask")).toCompletableFuture().join());
    assertTrue((Boolean) JavaHost.authorize(Map.of("entrypoint", "ClassifyTask", "capability", "tasks.get", "version", "1"))
        .toCompletableFuture().join());
    assertFalse((Boolean) JavaHost.authorize(Map.of("entrypoint", "ClassifyTask", "extra", true)).toCompletableFuture().join());
    assertFalse((Boolean) JavaHost.authorize(Map.of("entrypoint", "ClassifyTask", "capability", "tasks.delete", "version", "1"))
        .toCompletableFuture().join());

    var completed = (Map<?, ?>) JavaHost.readTask(Map.of("taskId", "completed"), Map.of("actorId", "sample-user"), 1)
        .toCompletableFuture().join();
    assertEquals(Map.of("kind", "SUCCESS", "value", Map.of("id", "completed", "status", "COMPLETED")), completed);
    var open = (Map<?, ?>) JavaHost.readTask(Map.of("taskId", "open"), Map.of("actorId", "sample-user"), 1)
        .toCompletableFuture().join();
    assertEquals("OPEN", ((Map<?, ?>) open.get("value")).get("status"));
    assertEquals(Map.of("kind", "BUSINESS_REJECTION", "code", "TASK_NOT_FOUND"),
        JavaHost.readTask(Map.of("taskId", "unknown"), Map.of("actorId", "sample-user"), 1).toCompletableFuture().join());
    assertThrows(CompletionException.class, () -> JavaHost.readTask(Map.of("taskId", "completed", "extra", true),
        Map.of("actorId", "sample-user"), 1).toCompletableFuture().join());
    assertThrows(CompletionException.class, () -> JavaHost.readTask(Map.of("taskId", 1),
        Map.of("actorId", "sample-user"), 1).toCompletableFuture().join());
    assertThrows(CompletionException.class, () -> JavaHost.readTask(Map.of("taskId", "completed"),
        Map.of("actorId", "other-user"), 1).toCompletableFuture().join());
    assertThrows(CompletionException.class, () -> JavaHost.readTask(Map.of("taskId", "completed"),
        Map.of("actorId", "sample-user"), 0).toCompletableFuture().join());
    assertThrows(CompletionException.class, () -> JavaHost.readTask(Map.of("taskId", "wrong-task"),
        Map.of("actorId", "sample-user"), 1).toCompletableFuture().join());
  }

  private static void assertNotDisplayed(Map<String, Object> result) {
    assertEquals(Map.of("status", "NOT_DISPLAYED", "code", "INVALID_RESULT"),
        JavaHost.consume("COMPLETED", result, null));
  }

  private void copyFixture(Path destination) throws Exception {
    try (var input = getClass().getResourceAsStream("/read-and-decide/artifact.json")) {
      Files.copy(input, destination);
    }
  }
}
