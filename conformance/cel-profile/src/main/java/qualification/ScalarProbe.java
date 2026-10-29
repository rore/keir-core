package qualification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelOptions;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.StructTypeReference;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Isolated CEL-Java scalar qualification probe; this is not a Keir runtime. */
public final class ScalarProbe {
  private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  private static final Descriptors.FileDescriptor TASK_FILE = taskFile();
  private static final Descriptors.Descriptor TASK = TASK_FILE.findMessageTypeByName("Task");
  private static final Set<String> FIELDS = Set.of("enabled", "id", "projectId", "status", "title");
  private static final Set<String> FUNCTIONS = Set.of("_==_", "_!=_", "_&&_", "_||_", "!_", "_?_:_");
  private static final CelOptions OPTIONS = CelOptions.current().enableHeterogeneousNumericComparisons(true).comprehensionMaxIterations(0).build();
  private static final CelRuntime RUNTIME = CelRuntimeFactory.plannerRuntimeBuilder().setOptions(OPTIONS).addFileTypes(TASK_FILE).build();

  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("Usage: ScalarProbe fixtures.json output-directory");
    byte[] inputBytes = Files.readAllBytes(Path.of(args[0]));
    if (inputBytes.length > 1024 * 1024) throw new IllegalArgumentException("Fixture corpus exceeds 1 MiB");
    JsonNode root = JSON.readTree(inputBytes);
    JsonNode inputs = root.path("inputs"), expressions = root.path("expressions"), rejects = root.path("rejects");
    if (!inputs.isArray() || inputs.isEmpty() || !expressions.isArray() || !rejects.isArray()) throw new IllegalArgumentException("Invalid fixture sections");
    List<DynamicMessage> tasks = new ArrayList<>();
    for (JsonNode input : inputs) tasks.add(readTask(input.path("task")));
    int projectionRejects = projectionGuards(inputs.get(0).path("task"));
    var outputExpressions = new ArrayList<Map<String, Object>>();
    var results = new ArrayList<Map<String, Object>>();
    for (JsonNode spec : expressions) {
      String id = spec.path("id").asText(), source = spec.path("source").asText();
      if (source.getBytes(StandardCharsets.UTF_8).length > 512) throw new IllegalArgumentException("Source exceeds 512 UTF-8 bytes");
      CelAbstractSyntaxTree ast = compile(source);
      checkAst(ast);
      CheckedExpr checked = CelProtoAbstractSyntaxTree.fromCelAst(ast).toCheckedExpr();
      byte[] checkedBytes = deterministic(checked);
      if (checkedBytes.length > 16384) throw new IllegalArgumentException("Checked expression exceeds 16384 bytes");
      if (!java.util.Arrays.equals(checkedBytes, deterministic(CelProtoAbstractSyntaxTree.fromCelAst(compile(source)).toCheckedExpr())))
        throw new AssertionError("Non-deterministic checked expression: " + id);
      String outputType = ast.getResultType().name().toLowerCase(Locale.ROOT);
      if (!(outputType.equals("bool") || outputType.equals("string"))) throw new IllegalArgumentException("Root type must be bool or string");
      outputExpressions.add(Map.of("id", id, "source", source, "checkedBase64", Base64.getEncoder().encodeToString(checkedBytes), "javaOutputType", outputType));
      JsonNode expected = spec.path("expected");
      if (!expected.isArray() || expected.size() != tasks.size()) throw new IllegalArgumentException("Expected result count mismatch: " + id);
      for (int i = 0; i < tasks.size(); i++) {
        Object value = RUNTIME.createProgram(ast).eval(Map.of("task", tasks.get(i)));
        if (!value.equals(expected.get(i).isBoolean() ? expected.get(i).booleanValue() : expected.get(i).textValue()))
          throw new AssertionError("Fixture mismatch: " + id + " input " + i + ": " + value);
        results.add(Map.of("id", id, "input", inputs.get(i).path("name").asText(), "value", value));
      }
    }
    var missingVariable = RUNTIME.createProgram(compile("task.status == 'OPEN'"));
    try {
      missingVariable.eval(Map.of());
      throw new AssertionError("Missing task variable unexpectedly evaluated");
    } catch (AssertionError e) { throw e; }
    catch (dev.cel.runtime.CelEvaluationException expectedMissingVariable) { /* Missing host input must remain an evaluation error. */ }
    Object rawDefault = RUNTIME.createProgram(compile("task.status")).eval(Map.of("task", DynamicMessage.newBuilder(TASK).build()));
    if (!"".equals(rawDefault)) throw new AssertionError("Expected proto3 string default");
    Object shortCircuit = RUNTIME.createProgram(compile("false && task.enabled")).eval(Map.of());
    if (!Boolean.FALSE.equals(shortCircuit)) throw new AssertionError("Expected short circuit");
    var rejected = new ArrayList<Map<String, String>>();
    for (JsonNode spec : rejects) {
      String id = spec.path("id").asText(), source = spec.path("source").asText();
      try { checkAst(compile(source)); throw new AssertionError("Unexpected accept: " + id); }
      catch (AssertionError e) { throw e; }
      catch (RuntimeException e) { rejected.add(Map.of("id", id, "message", String.valueOf(e.getMessage()))); }
    }
    Path out = Path.of(args[1]); Files.createDirectories(out);
    byte[] descriptor = DescriptorProtos.FileDescriptorSet.newBuilder().addFile(TASK_FILE.toProto()).build().toByteArray();
    if (descriptor.length > 65536) throw new IllegalArgumentException("Descriptor exceeds 65536 bytes");
    String bundleJson = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
        "descriptorBase64", Base64.getEncoder().encodeToString(descriptor), "expressions", outputExpressions,
        "javaResults", results, "rejects", rejected, "javaProjectionRejects", projectionRejects));
    Files.writeString(out.resolve("bundle.json"), bundleJson.replace("\r\n", "\n"), StandardCharsets.UTF_8);
    var scalar = RUNTIME.createProgram(compile("task.enabled && task.status != 'DONE'"));
    JsonNode maxNodes = null;
    for (JsonNode spec : expressions) if (spec.path("id").asText().equals("max_nodes")) maxNodes = spec;
    var largest = maxNodes == null ? null : RUNTIME.createProgram(compile(maxNodes.path("source").asText()));
    long start = System.nanoTime();
    for (int i = 0; i < 1000; i++) scalar.eval(Map.of("task", tasks.get(0)));
    start = System.nanoTime();
    for (int i = 0; i < 1000; i++) scalar.eval(Map.of("task", tasks.get(i % tasks.size())));
    long scalarNs = System.nanoTime() - start;
    long largestNs = 0;
    if (largest != null) {
      for (int i = 0; i < 1000; i++) largest.eval(Map.of("task", tasks.get(0)));
      start = System.nanoTime();
      for (int i = 0; i < 1000; i++) largest.eval(Map.of("task", tasks.get(0)));
      largestNs = System.nanoTime() - start;
    }
    JSON.writeValue(out.resolve("java-metrics.json").toFile(), Map.of("jdk", System.getProperty("java.version"), "evalIterations", 1000, "scalarEvalNs", scalarNs, "maxNodesEvalNs", largestNs));
  }

  private static CelAbstractSyntaxTree compile(String source) {
    if (source.getBytes(StandardCharsets.UTF_8).length > 512) throw new IllegalArgumentException("Source exceeds 512 UTF-8 bytes");
    try { return compiler().compile(source).getAst(); }
    catch (CelValidationException e) { throw new IllegalArgumentException("CEL compilation failed: " + e.getMessage(), e); }
  }

  private static CelCompiler compiler() {
    return CelCompilerFactory.standardCelCompilerBuilder().setOptions(OPTIONS).setStandardMacros(Set.of())
        .addFileTypes(TASK_FILE).addVar("task", StructTypeReference.create("keir.read.Task")).build();
  }

  private static void checkAst(CelAbstractSyntaxTree ast) {
    Expr expr = CelProtoAbstractSyntaxTree.fromCelAst(ast).toCheckedExpr().getExpr();
    int[] nodes = {0}; visit(expr, 1, nodes);
    String type = ast.getResultType().name();
    if (!type.equals("BOOL") && !type.equals("STRING") && !type.equals("bool") && !type.equals("string")) throw new IllegalArgumentException("Root type must be bool or string");
    var checked = CelProtoAbstractSyntaxTree.fromCelAst(ast).toCheckedExpr();
    for (String id : checked.getReferenceMapMap().values().stream().map(r -> r.getOverloadIdList()).flatMap(List::stream).toList())
        if (!Set.of("equals", "not_equals", "logical_and", "logical_or", "logical_not", "conditional").contains(id))
        throw new IllegalArgumentException("Overload not approved: " + id);
  }

  private static void visit(Expr e, int depth, int[] nodes) {
    if (++nodes[0] > 64 || depth > 16) throw new IllegalArgumentException("AST budget exceeded");
    if (e.hasConstExpr()) {
      var c = e.getConstExpr();
      if (c.hasStringValue() && validUnicode(c.getStringValue()) && c.getStringValue().getBytes(StandardCharsets.UTF_8).length <= 256 || c.hasBoolValue()) return;
      throw new IllegalArgumentException("Only bool and bounded string constants are supported");
    }
    if (e.hasIdentExpr()) { if (!e.getIdentExpr().getName().equals("task")) throw new IllegalArgumentException("Identifier not approved"); return; }
    if (e.hasSelectExpr()) {
      var s = e.getSelectExpr();
      if (!s.getOperand().hasIdentExpr() || !s.getOperand().getIdentExpr().getName().equals("task") || s.getTestOnly() || !FIELDS.contains(s.getField()))
        throw new IllegalArgumentException("Only direct known task fields are supported");
      visit(s.getOperand(), depth + 1, nodes);
      return;
    }
    if (e.hasCallExpr()) {
      var c = e.getCallExpr(); String f = c.getFunction();
      int arity = c.getArgsCount();
      if (c.hasTarget() || !FUNCTIONS.contains(f) || (f.equals("!_") ? arity != 1 : f.equals("_?_:_") ? arity != 3 : arity != 2))
        throw new IllegalArgumentException("Call not approved: " + f);
      if (f.equals("_?_:_")) {
        Expr yes = c.getArgs(1), no = c.getArgs(2);
        if (!literalClass(yes) || !literalClass(no)) throw new IllegalArgumentException("Conditional branches must be ACTIVE or COMPLETED literals");
      }
      for (Expr arg : c.getArgsList()) visit(arg, depth + 1, nodes);
      return;
    }
    throw new IllegalArgumentException("AST construct not approved");
  }

  private static boolean literalClass(Expr e) {
    return e.hasConstExpr() && e.getConstExpr().hasStringValue() && Set.of("ACTIVE", "COMPLETED").contains(e.getConstExpr().getStringValue());
  }

  private static DynamicMessage readTask(JsonNode node) {
    if (!node.isObject() || node.size() != FIELDS.size()) throw new IllegalArgumentException("Task must contain exactly the five fields");
    if (node.toString().getBytes(StandardCharsets.UTF_8).length > 8192) throw new IllegalArgumentException("Task JSON exceeds 8192 bytes");
    var b = DynamicMessage.newBuilder(TASK);
    for (Descriptors.FieldDescriptor field : TASK.getFields()) {
      JsonNode v = node.get(field.getJsonName());
      if (v == null || v.isNull()) throw new IllegalArgumentException("Missing or null: " + field.getJsonName());
      if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.BOOLEAN) {
        if (!v.isBoolean()) throw new IllegalArgumentException("Expected boolean: " + field.getJsonName());
        b.setField(field, v.booleanValue());
      } else {
        if (!v.isTextual()) throw new IllegalArgumentException("Expected string: " + field.getJsonName());
        String s = v.textValue();
        if (!validUnicode(s) || s.getBytes(StandardCharsets.UTF_8).length > 256 || ((field.getName().equals("id") || field.getName().equals("projectId")) && s.isEmpty())) throw new IllegalArgumentException("Invalid string: " + field.getJsonName());
        if (field.getName().equals("status") && !Set.of("OPEN", "DONE").contains(s)) throw new IllegalArgumentException("Invalid status");
        b.setField(field, s);
      }
    }
    return b.build();
  }

  private static boolean validUnicode(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isHighSurrogate(c)) {
        if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
      } else if (Character.isLowSurrogate(c)) return false;
    }
    return true;
  }

  private static int projectionGuards(JsonNode valid) {
    var cases = new ArrayList<JsonNode>();
    for (String field : List.of("id", "projectId", "status", "title", "enabled")) {
      var missing = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy();
      missing.remove(field); cases.add(missing);
      var nul = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy();
      nul.putNull(field); cases.add(nul);
    }
    for (int guard = 0; guard < 7; guard++) {
      var candidate = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy();
      switch (guard) {
        case 0 -> candidate.put("enabled", "true");
        case 1 -> candidate.put("status", "UNKNOWN");
        case 2 -> candidate.put("id", "");
        case 3 -> candidate.put("extra", true);
        case 4 -> candidate.put("title", "x".repeat(257));
        case 5 -> candidate.put("title", "\uD800");
        case 6 -> candidate.put("title", "é".repeat(129));
      }
      cases.add(candidate);
    }
    for (JsonNode candidate : cases) {
      try { readTask(candidate); throw new AssertionError("Projection guard accepted invalid case"); }
      catch (AssertionError e) { throw e; }
      catch (IllegalArgumentException expected) { }
    }
    var boundary = (com.fasterxml.jackson.databind.node.ObjectNode) valid.deepCopy();
    boundary.put("title", "é".repeat(128)); readTask(boundary);
    boundary.put("title", ""); readTask(boundary);
    return cases.size();
  }

  private static Descriptors.FileDescriptor taskFile() {
    var task = DescriptorProtos.DescriptorProto.newBuilder().setName("Task")
        .addField(field("enabled", 1, "enabled", DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL))
        .addField(field("id", 2, "id", DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .addField(field("projectId", 3, "projectId", DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .addField(field("status", 4, "status", DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .addField(field("title", 5, "title", DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING));
    var file = DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_read.proto")
        .setPackage("keir.read").setSyntax("proto3").addMessageType(task).build();
    try { return Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[0]); }
    catch (Descriptors.DescriptorValidationException e) { throw new IllegalStateException(e); }
  }

  private static DescriptorProtos.FieldDescriptorProto field(String name, int number, String jsonName, DescriptorProtos.FieldDescriptorProto.Type type) {
    return DescriptorProtos.FieldDescriptorProto.newBuilder().setName(name).setJsonName(jsonName).setNumber(number).setType(type)
        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL).build();
  }

  private static byte[] deterministic(CheckedExpr checked) throws java.io.IOException {
    var bytes = new ByteArrayOutputStream();
    var output = CodedOutputStream.newInstance(bytes);
    output.useDeterministicSerialization();
    checked.writeTo(output);
    output.flush();
    return bytes.toByteArray();
  }
}
