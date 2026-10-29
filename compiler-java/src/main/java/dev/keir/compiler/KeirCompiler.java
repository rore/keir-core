package dev.keir.compiler;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Compiler for the candidate keir-read/1 profile; grants no activation authority. */
public final class KeirCompiler {
    public static final String PROFILE = "keir-read/1";
    public static final String IDENTITY = "keir-compiler-java/0.1.0-SNAPSHOT";
    private static final Map<String, String> PACKAGES = Map.of("celJava", "0.14.0", "celNode", "0.6.1",
            "protobufJava", "4.35.1", "protobufNode", "2.6.2");
    private static final Set<String> RESERVED = Set.of("input", "as", "break", "const", "continue", "else",
            "for", "function", "if", "import", "in", "let", "loop", "namespace", "package", "return",
            "var", "void", "while", "true", "false", "null");
    private static final Set<String> UNSUPPORTED = Set.of("when", "onError", "write", "retry", "imports");

    public record Environment(String profile, String compiler, Map<String, String> packages) {
        public Environment { packages = Map.copyOf(packages); }
        public static Environment supported() { return new Environment(PROFILE, IDENTITY, PACKAGES); }
    }
    public record Diagnostic(String code, String path, String message) {}
    public record Compilation(byte[] artifact, String artifactDigest, List<Diagnostic> diagnostics) {
        public Compilation {
            artifact = artifact == null ? null : artifact.clone();
            diagnostics = List.copyOf(diagnostics);
        }
        @Override public byte[] artifact() { return artifact == null ? null : artifact.clone(); }
        public boolean succeeded() { return artifact != null; }
    }

    public Compilation compile(byte[] sourceBytes, byte[] trustedDcmBytes, Environment environment) {
        try {
            if (sourceBytes == null) fail("SYNTAX_INVALID", "/source");
            if (trustedDcmBytes == null) fail("SYNTAX_INVALID", "/dcm");
            Object parsedSource = StrictData.parse(sourceBytes, "/source");
            Object parsedDcm = StrictData.parse(trustedDcmBytes, "/dcm");
            Map<String, Object> source = sourceShape(parsedSource);
            Dcm dcm = Dcm.parse(parsedDcm);
            if (environment == null || !PROFILE.equals(environment.profile())
                    || !IDENTITY.equals(environment.compiler()) || !PACKAGES.equals(environment.packages()))
                fail("PIN_MISMATCH", "/source");
            var domain = object(source.get("domain"), "/source/domain");
            var modelDomain = object(dcm.root().get("domain"), "/dcm/domain");
            if (!domain.get("id").equals(modelDomain.get("id"))) fail("PIN_MISMATCH", "/source/domain/id");
            if (!domain.get("version").equals(modelDomain.get("version"))) fail("PIN_MISMATCH", "/source/domain/version");
            String dcmDigest = StrictData.digest(dcm.root());
            if (!domain.get("fingerprint").equals(dcmDigest)) fail("PIN_MISMATCH", "/source/domain/fingerprint");
            String entryId = (String) source.get("entrypoint");
            var entry = dcm.entrypoints().get(entryId);
            if (entry == null) fail("ENTRY_GRANT_DENIED", "/source/entrypoint");
            preflight(source, dcm, entry);
            var variables = new TreeMap<String, String>();
            variables.put("input", entry.input());
            var expressions = new TreeMap<String, Object>();
            var steps = new ArrayList<Map<String, Object>>();
            int[] nodes = {0};
            int totalCalls = 0;
            var sourceSteps = (List<?>) source.get("steps");
            for (int i = 0; i < sourceSteps.size(); i++) {
                String path = "/source/steps/" + i;
                var step = object(sourceSteps.get(i), path);
                String id = (String) step.get("id");
                if (step.containsKey("call")) {
                    String capability = (String) step.get("call");
                    var definition = dcm.capabilities().get(capability);
                    var supplied = object(step.get("args"), path + "/args");
                    var args = new TreeMap<String, Object>();
                    for (var parameter : definition.parameters().entrySet()) {
                        if (!parameter.getValue().binding().equals("agent")) continue;
                        String name = parameter.getKey(), argPath = pointer(path + "/args", name);
                        args.put(name, binding(supplied.get(name), parameter.getValue().type(), variables,
                                dcm, expressions, nodes, argPath));
                    }
                    steps.add(Map.of("op", "CALL", "id", id, "capability", capability,
                            "version", definition.version(), "args", args, "resultType", definition.result()));
                    variables.put(id, definition.result());
                    totalCalls++;
                } else {
                    var value = binding(step.get("let"), null, variables, dcm, expressions, nodes, path + "/let");
                    String type = (String) value.get("type");
                    steps.add(Map.of("op", "BIND", "id", id, "value", value, "resultType", type));
                    variables.put(id, type);
                }
            }
            var output = dcm.record(entry.output()).fields();
            var returned = object(object(source.get("return"), "/source/return").get("record"), "/source/return/record");
            var fields = new TreeMap<String, Object>();
            for (var field : output.entrySet()) {
                String path = pointer("/source/return/record", field.getKey());
                fields.put(field.getKey(), binding(returned.get(field.getKey()), field.getValue(), variables,
                        dcm, expressions, nodes, path));
            }
            if (totalCalls > 4 || nodes[0] > 512) fail("BUDGET_EXCEEDED", "/source/steps");
            byte[] descriptors = dcm.descriptorBytes();
            if (descriptors.length > 65_536) fail("BUDGET_EXCEEDED", "/dcm/types");
            var artifact = new TreeMap<String, Object>();
            artifact.put("format", "keir-artifact/read-1"); artifact.put("profile", PROFILE);
            artifact.put("compiler", environment.compiler()); artifact.put("environment", environment.packages());
            artifact.put("sourceDigest", StrictData.digest(source)); artifact.put("dcmDigest", dcmDigest);
            artifact.put("dcm", dcm.root()); artifact.put("entrypoint", entryId);
            artifact.put("descriptors", Base64.getEncoder().encodeToString(descriptors));
            artifact.put("expressions", expressions); artifact.put("steps", steps);
            artifact.put("return", Map.of("type", entry.output(), "fields", fields));
            byte[] bytes = StrictData.canonical(artifact);
            if (bytes.length > 1_048_576) fail("BUDGET_EXCEEDED", "/source");
            return new Compilation(bytes, StrictData.digest(artifact), List.of());
        } catch (CompileFailure failure) {
            return new Compilation(null, null, List.of(new Diagnostic(failure.code, failure.path, message(failure.code))));
        }
    }

    private static Map<String, Object> binding(Object raw, String expected, Map<String, String> variables,
            Dcm dcm, Map<String, Object> expressions, int[] nodes, String path) {
        var binding = object(raw, path);
        if (binding.containsKey("literal")) {
            Object value = binding.get("literal");
            String type = Expressions.literal(value, expected, dcm, path + "/literal");
            return Map.of("literal", value, "type", type);
        }
        var parsed = Expressions.parse((String) binding.get("cel"), variables.keySet(), path + "/cel");
        var compiled = Expressions.compile(parsed, expected, variables, dcm, path + "/cel");
        String id = "e" + expressions.size();
        expressions.put(id, compiled.metadata()); nodes[0] += compiled.nodes();
        return Map.of("expr", id, "type", compiled.type());
    }

    private static void preflight(
            Map<String, Object> source, Dcm dcm, Dcm.Entrypoint entry) {
        var names = new HashSet<String>(); names.add("input");
        var calls = new HashMap<String, Integer>();
        var steps = (List<?>) source.get("steps");
        for (int i = 0; i < steps.size(); i++) {
            String path = "/source/steps/" + i;
            var step = object(steps.get(i), path);
            String id = (String) step.get("id");
            if (names.contains(id) || RESERVED.contains(id) || dcm.types().containsKey(id)
                    || id.equals("Boolean") || id.equals("String")) fail("DATAFLOW_UNAVAILABLE", path + "/id");
            if (step.containsKey("call")) {
                String capability = (String) step.get("call");
                var definition = dcm.capabilities().get(capability);
                if (definition == null) fail("CAPABILITY_UNKNOWN", path + "/call");
                if (calls.merge(capability, 1, Integer::sum) > entry.grants().getOrDefault(capability, 0))
                    fail("ENTRY_GRANT_DENIED", path + "/call");
                var args = object(step.get("args"), path + "/args");
                for (String key : args.keySet()) {
                    var parameter = definition.parameters().get(key);
                    if (parameter == null) fail("FIELD_UNKNOWN", pointer(path + "/args", key));
                    if (parameter.binding().equals("host")) fail("HOST_BINDING_OVERRIDE", pointer(path + "/args", key));
                }
                for (var parameter : definition.parameters().entrySet()) if (parameter.getValue().binding().equals("agent")) {
                    String argPath = pointer(path + "/args", parameter.getKey());
                    if (!args.containsKey(parameter.getKey())) fail("TYPE_MISMATCH", argPath);
                    checkLiteral(args.get(parameter.getKey()), parameter.getValue().type(), dcm, argPath);
                }
            } else checkLiteral(step.get("let"), null, dcm, path + "/let");
            names.add(id);
        }
        var fields = object(object(source.get("return"), "/source/return").get("record"), "/source/return/record");
        var output = dcm.record(entry.output()).fields();
        for (String key : fields.keySet()) if (!output.containsKey(key)) fail("FIELD_UNKNOWN", pointer("/source/return/record", key));
        for (var field : output.entrySet()) {
            String path = pointer("/source/return/record", field.getKey());
            if (!fields.containsKey(field.getKey())) fail("TYPE_MISMATCH", path);
            checkLiteral(fields.get(field.getKey()), field.getValue(), dcm, path);
        }
    }

    private static void checkLiteral(Object raw, String expected, Dcm dcm, String path) {
        var value = object(raw, path);
        if (value.containsKey("literal")) Expressions.literal(value.get("literal"), expected, dcm, path + "/literal");
    }

    private static Map<String, Object> sourceShape(Object parsed) {
        var source = object(parsed, "/source");
        shape(source, Set.of("language", "domain", "entrypoint", "name", "steps", "return"), "/source");
        if (!"keir/v0".equals(source.get("language"))) fail("PROFILE_UNSUPPORTED", "/source/language");
        var domain = object(source.get("domain"), "/source/domain");
        shape(domain, Set.of("id", "version", "fingerprint"), "/source/domain");
        domain.forEach((key, value) -> text(value, pointer("/source/domain", key), 256));
        identifier(source.get("entrypoint"), "[A-Za-z][A-Za-z0-9.]{0,99}", "/source/entrypoint");
        identifier(source.get("name"), "[a-z][a-z0-9-]{0,63}", "/source/name");
        if (!(source.get("steps") instanceof List<?>)) fail("SYNTAX_INVALID", "/source/steps");
        var steps = (List<?>) source.get("steps");
        if (steps.size() > 8) fail("BUDGET_EXCEEDED", "/source/steps");
        if (steps.isEmpty()) fail("SYNTAX_INVALID", "/source/steps");
        for (int i = 0; i < steps.size(); i++) {
            String path = "/source/steps/" + i;
            var step = object(steps.get(i), path);
            shape(step, step.containsKey("call") ? Set.of("id", "call", "args") : Set.of("id", "let"), path);
            identifier(step.get("id"), "[A-Za-z][A-Za-z0-9]{0,63}", path + "/id");
            if (step.containsKey("call")) {
                identifier(step.get("call"), "[A-Za-z][A-Za-z0-9.]{0,99}", path + "/call");
                var args = object(step.get("args"), path + "/args");
                if (args.size() > 16) fail("BUDGET_EXCEEDED", path + "/args");
                for (var arg : args.entrySet()) {
                    String argPath = pointer(path + "/args", arg.getKey());
                    identifier(arg.getKey(), "[A-Za-z][A-Za-z0-9]{0,63}", argPath);
                    bindingShape(arg.getValue(), argPath);
                }
            } else bindingShape(step.get("let"), path + "/let");
        }
        var returned = object(source.get("return"), "/source/return");
        shape(returned, Set.of("record"), "/source/return");
        var fields = object(returned.get("record"), "/source/return/record");
        if (fields.size() > 16) fail("BUDGET_EXCEEDED", "/source/return/record");
        for (var field : fields.entrySet()) {
            String path = pointer("/source/return/record", field.getKey());
            identifier(field.getKey(), "[A-Za-z][A-Za-z0-9]{0,63}", path);
            bindingShape(field.getValue(), path);
        }
        return source;
    }

    private static void bindingShape(Object raw, String path) {
        var binding = object(raw, path);
        if (binding.containsKey("literal")) {
            shape(binding, Set.of("literal"), path);
            Object value = binding.get("literal");
            if (!(value instanceof String) && !(value instanceof Boolean)) fail("PROFILE_UNSUPPORTED", path + "/literal");
            if (value instanceof String) text(value, path + "/literal", 256);
        } else {
            shape(binding, Set.of("cel"), path);
            text(binding.get("cel"), path + "/cel", 512);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?>)) fail("SYNTAX_INVALID", path);
        return new TreeMap<>((Map<String, Object>) value);
    }
    private static void shape(Map<String, Object> value, Set<String> required, String path) {
        for (String key : value.keySet()) if (!required.contains(key))
            fail(UNSUPPORTED.contains(key) ? "PROFILE_UNSUPPORTED" : "FIELD_UNKNOWN", pointer(path, key));
        for (String key : new java.util.TreeSet<>(required)) if (!value.containsKey(key))
            fail("SYNTAX_INVALID", pointer(path, key));
    }
    private static void identifier(Object value, String pattern, String path) {
        if (!(value instanceof String s) || !s.matches(pattern)) fail("SYNTAX_INVALID", path);
    }
    private static void text(Object value, String path, int maxBytes) {
        if (!(value instanceof String)) fail("SYNTAX_INVALID", path);
        if (((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > maxBytes)
            fail("BUDGET_EXCEEDED", path);
    }
    private static String pointer(String path, String part) { return path + "/" + part.replace("~", "~0").replace("/", "~1"); }
    private static void fail(String code, String path) { throw new CompileFailure(code, path); }
    private static String message(String code) {
        return switch (code) {
            case "SYNTAX_INVALID" -> "The document does not match the required syntax.";
            case "FIELD_UNKNOWN" -> "The document contains an unknown field.";
            case "PROFILE_UNSUPPORTED" -> "The construct is outside the selected profile.";
            case "PIN_MISMATCH" -> "The selected model or environment does not match.";
            case "DCM_INVALID" -> "The domain model is invalid.";
            case "CAPABILITY_UNKNOWN" -> "The capability is not declared.";
            case "ENTRY_GRANT_DENIED" -> "The entrypoint does not grant this call.";
            case "HOST_BINDING_OVERRIDE" -> "A protected parameter cannot be supplied by source.";
            case "NOMINAL_MISMATCH" -> "The nominal types do not match.";
            case "ENUM_MISMATCH" -> "The value does not match the required enum.";
            case "DATAFLOW_UNAVAILABLE" -> "The binding name is unavailable.";
            case "CEL_POLICY_DENIED" -> "The checked expression is outside the allowed profile.";
            case "BUDGET_EXCEEDED" -> "A profile admission limit was exceeded.";
            default -> "The value does not match the required type.";
        };
    }
}
