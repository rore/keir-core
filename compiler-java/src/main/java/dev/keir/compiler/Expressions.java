package dev.keir.compiler;

import com.google.protobuf.CodedOutputStream;
import dev.cel.common.CelOptions;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructTypeReference;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.expr.Type;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class Expressions {
    private static final Map<String, String> OVERLOADS = Map.of(
            "_==_", "equals", "_!=_", "not_equals", "!_", "logical_not",
            "_&&_", "logical_and", "_||_", "logical_or", "_?_:_", "conditional");
    private static final CelOptions OPTIONS = CelOptions.current()
            .enableHeterogeneousNumericComparisons(true).comprehensionMaxIterations(0)
            .maxExpressionCodePointSize(512).maxParseRecursionDepth(512)
            .maxParseExpressionNodeCount(1024).build();

    record Compiled(Map<String, Object> metadata, String type, int nodes) {}
    private record Value(String type, List<String> literals) {
        Value(String type) { this(type, null); }
    }

    static CelAbstractSyntaxTree parse(String text, Set<String> variables, String path) {
        if (text.getBytes(StandardCharsets.UTF_8).length > 512) fail("BUDGET_EXCEEDED", path);
        try {
            var parsed = CelCompilerFactory.standardCelCompilerBuilder().setOptions(OPTIONS)
                    .setStandardMacros(Set.of()).build().parse(text, "keir-read/1").getAst();
            available(CelProtoAbstractSyntaxTree.fromCelAst(parsed).getExpr(), variables, path);
            return parsed;
        } catch (CelValidationException e) { throw new CompileFailure("TYPE_MISMATCH", path); }
    }

    static Compiled compile(CelAbstractSyntaxTree parsed, String expected, Map<String, String> variables,
                            Dcm dcm, String path) {
        var builder = CelCompilerFactory.standardCelCompilerBuilder().setOptions(OPTIONS)
                .setStandardMacros(Set.of()).addFileTypes(dcm.fileDescriptor());
        variables.forEach((name, type) -> builder.addVar(name, celType(type, dcm)));
        var compiler = builder.build();
        try {
            CheckedExpr checked = CelProtoAbstractSyntaxTree.fromCelAst(compiler.check(parsed).getAst())
                    .toCheckedExpr();
            var walker = new Walker(dcm, variables, checked, path);
            Value value = walker.visit(checked.getExpr(), 1);
            if (!dcm.type(value.type()).scalar()) fail("CEL_POLICY_DENIED", path);
            String type = expected == null ? value.type() : assign(value, expected, dcm, path);
            byte[] bytes = deterministic(checked);
            if (bytes.length > 16_384) fail("BUDGET_EXCEEDED", path);
            return new Compiled(Map.of("checked", Base64.getEncoder().encodeToString(bytes),
                    "variables", new TreeMap<>(variables), "resultType", type), type, walker.nodes);
        } catch (CelValidationException e) {
            throw new CompileFailure("TYPE_MISMATCH", path);
        }
    }

    static String literal(Object value, String expected, Dcm dcm, String path) {
        Value type;
        if (value instanceof Boolean) type = new Value("Boolean");
        else if (value instanceof String text) {
            bounded(text, path);
            type = new Value("String", List.of(text));
        } else throw new CompileFailure("PROFILE_UNSUPPORTED", path);
        return expected == null ? type.type() : assign(type, expected, dcm, path);
    }

    static String nominalLiteral(Object value, Object annotation, String expected, Dcm dcm, String path) {
        if (!(annotation instanceof String type) || dcm.type(type) == null
                || !dcm.type(type).kind().equals("nominal")) throw new CompileFailure("NOMINAL_MISMATCH", path + "/type");
        if (!(value instanceof String text)) throw new CompileFailure("PROFILE_UNSUPPORTED", path + "/literal");
        bounded(text, path + "/literal");
        return expected == null ? type : assign(new Value(type), expected, dcm, path + "/literal");
    }

    private static CelType celType(String type, Dcm dcm) {
        if (dcm.type(type).kind().equals("record")) return StructTypeReference.create("keir.read." + type);
        return type.equals("Boolean") ? SimpleType.BOOL : SimpleType.STRING;
    }

    private static String assign(Value value, String expected, Dcm dcm, String path) {
        if (value.type().equals(expected)) return expected;
        if (dcm.type(expected).kind().equals("nominal") || dcm.type(value.type()).kind().equals("nominal"))
            fail("NOMINAL_MISMATCH", path);
        if (dcm.type(expected).kind().equals("enum")) {
            if (value.type().equals("String") && value.literals() != null
                    && dcm.type(expected).values().containsAll(value.literals())) return expected;
            fail("ENUM_MISMATCH", path);
        }
        if (dcm.type(value.type()).kind().equals("enum")) fail("ENUM_MISMATCH", path);
        fail("TYPE_MISMATCH", path);
        return expected;
    }

    private static void available(Expr expr, Set<String> variables, String path) {
        if (expr.hasIdentExpr() && !variables.contains(expr.getIdentExpr().getName()))
            fail("DATAFLOW_UNAVAILABLE", path);
        if (expr.hasSelectExpr()) available(expr.getSelectExpr().getOperand(), variables, path);
        if (expr.hasCallExpr()) {
            if (expr.getCallExpr().hasTarget()) available(expr.getCallExpr().getTarget(), variables, path);
            for (Expr arg : expr.getCallExpr().getArgsList()) available(arg, variables, path);
        }
        if (expr.hasListExpr()) for (Expr item : expr.getListExpr().getElementsList()) available(item, variables, path);
        if (expr.hasStructExpr()) for (var field : expr.getStructExpr().getEntriesList()) {
            if (field.hasMapKey()) available(field.getMapKey(), variables, path);
            available(field.getValue(), variables, path);
        }
        if (expr.hasComprehensionExpr()) {
            var comprehension = expr.getComprehensionExpr();
            available(comprehension.getIterRange(), variables, path);
            available(comprehension.getAccuInit(), variables, path);
            var locals = new java.util.HashSet<>(variables);
            locals.add(comprehension.getIterVar()); locals.add(comprehension.getAccuVar());
            available(comprehension.getLoopCondition(), locals, path);
            available(comprehension.getLoopStep(), locals, path);
            available(comprehension.getResult(), locals, path);
        }
    }

    private static final class Walker {
        private final Dcm dcm;
        private final Map<String, String> variables;
        private final CheckedExpr checked;
        private final String path;
        private int nodes;

        Walker(Dcm dcm, Map<String, String> variables, CheckedExpr checked, String path) {
            this.dcm = dcm; this.variables = variables; this.checked = checked; this.path = path;
        }

        Value visit(Expr expr, int depth) {
            if (++nodes > 64 || depth > 16) fail("BUDGET_EXCEEDED", path);
            Type wireType = checked.getTypeMapMap().get(expr.getId());
            if (wireType == null || !(wireType.getPrimitive() == Type.PrimitiveType.BOOL
                    || wireType.getPrimitive() == Type.PrimitiveType.STRING
                    || wireType.hasMessageType() && wireType.getMessageType().startsWith("keir.read.")))
                fail("CEL_POLICY_DENIED", path);
            if (expr.hasConstExpr()) {
                var constant = expr.getConstExpr();
                if (constant.hasBoolValue()) return new Value("Boolean");
                if (constant.hasStringValue()) {
                    bounded(constant.getStringValue(), path);
                    return new Value("String", List.of(constant.getStringValue()));
                }
                fail("CEL_POLICY_DENIED", path);
            }
            if (expr.hasIdentExpr()) {
                String name = expr.getIdentExpr().getName();
                String type = variables.get(name);
                var reference = checked.getReferenceMapMap().get(expr.getId());
                if (type == null) fail("DATAFLOW_UNAVAILABLE", path);
                if (reference == null || !reference.getName().equals(name)
                        || reference.getOverloadIdCount() != 0) fail("CEL_POLICY_DENIED", path);
                return new Value(type);
            }
            if (expr.hasSelectExpr()) {
                var select = expr.getSelectExpr();
                Value operand = visit(select.getOperand(), depth + 1);
                String fieldType = dcm.type(operand.type()).fields().get(select.getField());
                if (select.getTestOnly() || fieldType == null) fail("CEL_POLICY_DENIED", path);
                return new Value(fieldType);
            }
            if (!expr.hasCallExpr()) throw new CompileFailure("CEL_POLICY_DENIED", path);
            var call = expr.getCallExpr();
            String function = call.getFunction();
            boolean concatenate = function.equals("_+_") && "keir-dcm/effect-4".equals(dcm.root().get("format"));
            String overload = concatenate ? "add_string" : OVERLOADS.get(function);
            int arity = function.equals("!_") ? 1 : function.equals("_?_:_") ? 3 : 2;
            var reference = checked.getReferenceMapMap().get(expr.getId());
            if (overload == null || call.hasTarget() || call.getArgsCount() != arity
                    || reference == null || !reference.getOverloadIdList().equals(List.of(overload)))
                fail("CEL_POLICY_DENIED", path);
            var args = new ArrayList<Value>();
            for (Expr arg : call.getArgsList()) args.add(visit(arg, depth + 1));
            if (concatenate) {
                if (args.stream().anyMatch(arg -> !arg.type().equals("String"))) fail("CEL_POLICY_DENIED", path);
                return new Value("String");
            }
            if (function.equals("_?_:_")) {
                if (!args.get(0).type().equals("Boolean")) fail("TYPE_MISMATCH", path);
                return branches(args.get(1), args.get(2));
            }
            if (function.equals("_==_") || function.equals("_!=_")) {
                Value a = args.get(0), b = args.get(1);
                if (!dcm.type(a.type()).scalar() || !dcm.type(b.type()).scalar())
                    fail("CEL_POLICY_DENIED", path);
                if (dcm.type(a.type()).kind().equals("enum")) assign(b, a.type(), dcm, path);
                else if (dcm.type(b.type()).kind().equals("enum")) assign(a, b.type(), dcm, path);
                else assign(a, b.type(), dcm, path);
            } else {
                for (Value arg : args) if (!arg.type().equals("Boolean")) fail("TYPE_MISMATCH", path);
            }
            return new Value("Boolean");
        }

        private Value branches(Value a, Value b) {
            if (!dcm.type(a.type()).scalar() || !dcm.type(b.type()).scalar())
                fail("CEL_POLICY_DENIED", path);
            if (a.type().equals(b.type())) {
                if (a.literals() != null && b.literals() != null) {
                    var alternatives = new ArrayList<>(a.literals());
                    alternatives.addAll(b.literals());
                    return new Value(a.type(), alternatives);
                }
                return new Value(a.type());
            }
            if (dcm.type(a.type()).kind().equals("enum")) {
                assign(b, a.type(), dcm, path); return new Value(a.type());
            }
            if (dcm.type(b.type()).kind().equals("enum")) {
                assign(a, b.type(), dcm, path); return new Value(b.type());
            }
            assign(a, b.type(), dcm, path);
            return new Value(a.type());
        }
    }

    private static void bounded(String value, String path) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i == value.length() || !Character.isLowSurrogate(value.charAt(i)))
                    fail("TYPE_MISMATCH", path);
            } else if (Character.isLowSurrogate(c)) fail("TYPE_MISMATCH", path);
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > 256) fail("BUDGET_EXCEEDED", path);
    }

    private static byte[] deterministic(CheckedExpr message) {
        try {
            var bytes = new ByteArrayOutputStream();
            var output = CodedOutputStream.newInstance(bytes);
            output.useDeterministicSerialization(); message.writeTo(output); output.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void fail(String code, String path) { throw new CompileFailure(code, path); }
}
