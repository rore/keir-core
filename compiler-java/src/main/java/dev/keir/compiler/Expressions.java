package dev.keir.compiler;

import com.google.protobuf.CodedOutputStream;
import dev.cel.common.CelOptions;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.CelType;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructTypeReference;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.expr.Type;
import dev.cel.parser.CelStandardMacro;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class Expressions {
    private static final Map<String,Integer> NUMERIC_ARITIES = Map.ofEntries(
            Map.entry("decimal",1), Map.entry("toDecimal",1), Map.entry("toInteger",1), Map.entry("size",1),
            Map.entry("-_",1), Map.entry("_+_",2), Map.entry("_-_",2), Map.entry("_*_",2), Map.entry("_/_",2),
            Map.entry("_<_",2), Map.entry("_<=_",2), Map.entry("_>_",2), Map.entry("_>=_",2),
            Map.entry("round",3), Map.entry("multiplyRounded",4), Map.entry("divideRounded",4));
    private static final Map<String,String> NUMERIC_OPERATORS = Map.of("_+_","add", "_-_","subtract", "_*_","multiply",
            "_/_","divide", "-_","negate", "_<_","less", "_<=_","less_equals", "_>_","greater", "_>=_","greater_equals");
    private static final Set<String> ROUNDING = Set.of("UNNECESSARY","DOWN","FLOOR","CEILING","HALF_UP","HALF_EVEN");
    private static final Map<String, String> OVERLOADS = Map.of(
            "_==_", "equals", "_!=_", "not_equals", "!_", "logical_not",
            "_&&_", "logical_and", "_||_", "logical_or", "_?_:_", "conditional",
            "@in", "in_list", "_<_", "less_timestamp",
            "timestamp", "string_to_timestamp", "duration", "string_to_duration");
    private static final Instant EARLIEST = Instant.parse("0001-01-01T00:00:00.000Z");
    private static final Instant LATEST = Instant.parse("9999-12-31T23:59:59.999Z");
    private static final java.time.format.DateTimeFormatter UTC_MILLIS =
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
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
        CelAbstractSyntaxTree parsed;
        Expr expression;
        try {
            parsed = CelCompilerFactory.standardCelCompilerBuilder().setOptions(OPTIONS)
                    .setStandardMacros(Set.of(CelStandardMacro.HAS)).build().parse(text, "<input>").getAst();
            expression = CelProtoAbstractSyntaxTree.fromCelAst(parsed).getExpr();
        } catch (CelValidationException e) { throw new CompileFailure("TYPE_MISMATCH", path); }
        catch (StackOverflowError exhausted) {
            // CEL/ANTLR recursion can exhaust the caller stack before a checked AST exists.
            throw new CompileFailure("BUDGET_EXCEEDED", path);
        }
        available(expression, variables, path);
        return parsed;
    }

    static Compiled compile(CelAbstractSyntaxTree parsed, String expected, Map<String, String> variables,
                            Map<String, String> guardedResults, Dcm dcm, String path) {
        var builder = CelCompilerFactory.standardCelCompilerBuilder().setOptions(OPTIONS)
                .setStandardMacros(Set.of(CelStandardMacro.HAS)).addFileTypes(dcm.fileDescriptor());
        if (dcm.numeric()) {
            builder.addFileTypes(dcm.fileDescriptor().getDependencies());
            CelType decimal = StructTypeReference.create("keir.numeric.Decimal");
            builder.addFunctionDeclarations(
                    declaration("decimal","keir_decimal_from_string",decimal,SimpleType.STRING),
                    declaration("toDecimal","keir_decimal_from_integer",decimal,SimpleType.INT),
                    declaration("toInteger","keir_integer_from_decimal",SimpleType.INT,decimal),
                    declaration("round","keir_decimal_round",decimal,decimal,SimpleType.INT,SimpleType.STRING),
                    declaration("multiplyRounded","keir_decimal_multiply_rounded",decimal,decimal,decimal,SimpleType.INT,SimpleType.STRING),
                    declaration("divideRounded","keir_decimal_divide_rounded",decimal,decimal,decimal,SimpleType.INT,SimpleType.STRING));
            NUMERIC_OPERATORS.forEach((function, operation) -> builder.addFunctionDeclarations(declaration(function,
                    "keir_decimal_" + operation.replace("_equals","_equal"),
                    Set.of("less","less_equals","greater","greater_equals").contains(operation) ? SimpleType.BOOL : decimal,
                    function.equals("-_") ? List.of(decimal) : List.of(decimal,decimal))));
        }
        variables.forEach((name, type) -> builder.addVar(name, celType(type, dcm)));
        var compiler = builder.build();
        CheckedExpr checked;
        try {
            checked = CelProtoAbstractSyntaxTree.fromCelAst(compiler.check(parsed).getAst())
                    .toCheckedExpr();
        } catch (CelValidationException e) {
            throw new CompileFailure("TYPE_MISMATCH", path);
        } catch (StackOverflowError exhausted) {
            throw new CompileFailure("BUDGET_EXCEEDED", path);
        }
        var walker = new Walker(dcm, variables, guardedResults, checked, path);
        Value value = walker.visit(checked.getExpr(), 1, Set.of(), Set.of(), false);
        if (dcm.type(value.type()) == null || dcm.record(value.type()) != null) fail("CEL_POLICY_DENIED", path);
        String type = expected == null ? value.type() : assign(value, expected, dcm, path);
        byte[] bytes = deterministic(checked);
        if (bytes.length > 16_384) fail("BUDGET_EXCEEDED", path);
        return new Compiled(Map.of("checked", Base64.getEncoder().encodeToString(bytes),
                "variables", new TreeMap<>(variables), "resultType", type), type, walker.nodes);
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
        if (annotation instanceof String numeric && dcm.numeric(numeric)) {
            canonicalNumber(value,numeric,path + "/literal");
            return expected == null ? numeric : assign(new Value(numeric),expected,dcm,path + "/literal");
        }
        if (!(annotation instanceof String type) || dcm.type(type) == null
                || !dcm.type(type).kind().equals("nominal")) throw new CompileFailure("NOMINAL_MISMATCH", path + "/type");
        if (!(value instanceof String text)) throw new CompileFailure("PROFILE_UNSUPPORTED", path + "/literal");
        bounded(text, path + "/literal");
        return expected == null ? type : assign(new Value(type), expected, dcm, path + "/literal");
    }

    private static CelType celType(String type, Dcm dcm) {
        if (dcm.numeric(type)) return type.equals("Integer") ? SimpleType.INT : StructTypeReference.create("keir.numeric.Decimal");
        return switch (dcm.type(type).kind()) {
            case "record" -> StructTypeReference.create("keir.read." + type);
            case "list" -> ListType.create(celType(dcm.type(type).element(), dcm));
            default -> switch (type) {
                case "Boolean" -> SimpleType.BOOL;
                case "Timestamp" -> SimpleType.TIMESTAMP;
                default -> SimpleType.STRING;
            };
        };
    }

    private static CelFunctionDecl declaration(String name,String id,CelType result,CelType... args) {
        return declaration(name,id,result,List.of(args));
    }
    private static CelFunctionDecl declaration(String name,String id,CelType result,List<CelType> args) {
        return CelFunctionDecl.newFunctionDeclaration(name,CelOverloadDecl.newGlobalOverload(id,result,args));
    }

    // Independent compiler validation; runtime code is not a compiler dependency.
    private static void canonicalNumber(Object raw,String type,String path) {
        if (!(raw instanceof String)) fail("TYPE_MISMATCH",path);
        String text = (String)raw;
        if (type.equals("Integer")) {
            if (text.length()>20 || !text.matches("0|-?[1-9][0-9]*") || new BigInteger(text).bitLength()>63)
                fail("TYPE_MISMATCH",path);
        } else {
            if (text.length()>40 || !text.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]*[1-9])?") || text.equals("-0"))
                fail("TYPE_MISMATCH",path);
            int point = text.indexOf('.');
            if (point>=0 && text.length()-point-1>18 || new BigInteger(text.replace(".","")).abs().toString().length()>38)
                fail("TYPE_MISMATCH",path);
        }
    }

    static void numericTokens(CelAbstractSyntaxTree parsed,String source,String path) {
        var proto = CelProtoAbstractSyntaxTree.fromCelAst(parsed);
        numericTokens(proto.getExpr(),proto.getSourceInfo().getPositionsMap(),source,path);
    }
    private static void numericTokens(Expr expr,Map<Long,Integer> positions,String source,String path) {
        if (expr.hasConstExpr()) {
            var constant=expr.getConstExpr();
            if (constant.hasBoolValue() || constant.hasStringValue()) return;
            if (!constant.hasInt64Value() || !positions.containsKey(expr.getId())) fail("CEL_POLICY_DENIED",path);
            int offset=positions.get(expr.getId());
            if (offset<0 || offset>=source.codePointCount(0,source.length())) fail("CEL_POLICY_DENIED",path);
            int start=source.offsetByCodePoints(0,offset);
            var token=java.util.regex.Pattern.compile("-?(?:0|[1-9][0-9]*)(?![A-Za-z0-9_.])").matcher(source).region(start,source.length());
            if (!token.lookingAt()) fail("CEL_POLICY_DENIED",path);
        } else if (expr.hasSelectExpr()) numericTokens(expr.getSelectExpr().getOperand(),positions,source,path);
        else if (expr.hasCallExpr()) {
            if (expr.getCallExpr().hasTarget()) numericTokens(expr.getCallExpr().getTarget(),positions,source,path);
            for (Expr arg:expr.getCallExpr().getArgsList()) numericTokens(arg,positions,source,path);
        } else if (!expr.hasIdentExpr()) fail("CEL_POLICY_DENIED",path);
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
        private record Presence(String variable, String field) {}
        private final Dcm dcm;
        private final Map<String, String> variables;
        private final Map<String, String> guardedResults;
        private final CheckedExpr checked;
        private final String path;
        private int nodes;

        Walker(Dcm dcm, Map<String, String> variables, Map<String, String> guardedResults, CheckedExpr checked, String path) {
            this.dcm = dcm; this.variables = variables; this.guardedResults = guardedResults;
            this.checked = checked; this.path = path;
        }

        Value visit(Expr expr, int depth, Set<Presence> proofs, Set<String> guards, boolean selectionOperand) {
            if (++nodes > 64 || depth > 16) fail("BUDGET_EXCEEDED", path);
            Type wireType = checked.getTypeMapMap().get(expr.getId());
            if (wireType == null || !(wireType.getPrimitive() == Type.PrimitiveType.BOOL
                    || dcm.numeric() && (wireType.getPrimitive() == Type.PrimitiveType.INT64
                    || wireType.hasMessageType() && wireType.getMessageType().equals("keir.numeric.Decimal"))
                    || wireType.getPrimitive() == Type.PrimitiveType.STRING
                    || wireType.hasMessageType() && wireType.getMessageType().startsWith("keir.read.")
                    || wireType.hasListType() || wireType.getWellKnown() == Type.WellKnownType.TIMESTAMP
                    || wireType.getWellKnown() == Type.WellKnownType.DURATION))
                fail("CEL_POLICY_DENIED", path);
            if (expr.hasConstExpr()) {
                var constant = expr.getConstExpr();
                if (dcm.numeric() && constant.hasInt64Value()) return new Value("Integer");
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
                if (guardedResults.containsKey(name)) {
                    if (!selectionOperand) fail("CEL_POLICY_DENIED", path);
                    if (!guards.contains(guardedResults.get(name))) fail("DATAFLOW_UNAVAILABLE", path);
                }
                return new Value(type);
            }
            if (expr.hasSelectExpr()) {
                var select = expr.getSelectExpr();
                Value operand = visit(select.getOperand(), depth + 1, proofs, guards, true);
                var record = dcm.record(operand.type());
                String fieldType = record == null ? null : record.fields().get(select.getField());
                if (fieldType == null) fail("CEL_POLICY_DENIED", path);
                if (select.getOperand().hasIdentExpr()
                        && guardedResults.containsKey(select.getOperand().getIdentExpr().getName())
                        && !dcm.type(fieldType).scalar()) fail("CEL_POLICY_DENIED", path);
                boolean optional = record.optionalFields().contains(select.getField());
                Presence presence = presence(expr);
                if (select.getTestOnly()) {
                    if (!optional || presence == null) fail("CEL_POLICY_DENIED", path);
                    return new Value("Boolean");
                }
                if (optional && (presence == null || !proofs.contains(presence)))
                    fail("DATAFLOW_UNAVAILABLE", path);
                return new Value(fieldType);
            }
            if (!expr.hasCallExpr()) throw new CompileFailure("CEL_POLICY_DENIED", path);
            var call = expr.getCallExpr();
            String function = call.getFunction();
            boolean addition = function.equals("_+_");
            boolean constructor = function.equals("timestamp") || function.equals("duration");
            int arity = dcm.numeric() && NUMERIC_ARITIES.containsKey(function) ? NUMERIC_ARITIES.get(function)
                    : function.equals("!_") || constructor ? 1 : function.equals("_?_:_") ? 3 : 2;
            var reference = checked.getReferenceMapMap().get(expr.getId());
            if ((!addition && !OVERLOADS.containsKey(function) && !(dcm.numeric() && NUMERIC_ARITIES.containsKey(function))) || call.hasTarget() || call.getArgsCount() != arity
                    || reference == null)
                fail("CEL_POLICY_DENIED", path);
            var args = new ArrayList<Value>();
            for (int i = 0; i < arity; i++) args.add(visit(call.getArgs(i), depth + 1,
                    branchProofs(call, i, proofs), branchGuards(call, i, guards), false));
            Operation numeric = dcm.numeric() ? numericCall(call,args) : null;
            if (numeric != null) {
                if (!reference.getOverloadIdList().equals(List.of(numeric.overload))) fail("CEL_POLICY_DENIED",path);
                return new Value(numeric.type);
            }
            boolean temporalAdd = addition && pair(args, "Timestamp", "Duration");
            String overload = addition ? temporalAdd ? "add_timestamp_duration" : "add_string" : OVERLOADS.get(function);
            if (overload == null) fail("CEL_POLICY_DENIED",path);
            if (!reference.getOverloadIdList().equals(List.of(overload))) fail("CEL_POLICY_DENIED", path);
            if (constructor) {
                Expr literal = call.getArgs(0);
                if (!literal.hasConstExpr() || !literal.getConstExpr().hasStringValue()) fail("TYPE_MISMATCH", path);
                String text = literal.getConstExpr().getStringValue();
                if (function.equals("timestamp") ? !validTimestamp(text) : !validDuration(text))
                    fail("TYPE_MISMATCH", path);
                return new Value(function.equals("timestamp") ? "Timestamp" : "Duration");
            }
            if (addition) {
                if (temporalAdd) return new Value("Timestamp");
                if (!pair(args, "String", "String")) fail("CEL_POLICY_DENIED", path);
                return new Value("String");
            }
            if (function.equals("@in")) {
                var collection = dcm.type(args.get(1).type());
                if (collection == null || !collection.kind().equals("list")
                        || !membershipNeedle(args.get(0), call.getArgs(0), collection.element()))
                    fail("TYPE_MISMATCH", path);
                return new Value("Boolean");
            }
            if (function.equals("_<_")) {
                if (!pair(args, "Timestamp", "Timestamp")) fail("CEL_POLICY_DENIED", path);
                return new Value("Boolean");
            }
            if (function.equals("_?_:_")) {
                if (!args.get(0).type().equals("Boolean")) fail("TYPE_MISMATCH", path);
                return branches(args.get(1), args.get(2));
            }
            if (function.equals("_==_") || function.equals("_!=_")) {
                Value a = args.get(0), b = args.get(1);
                if (!scalar(a) || !scalar(b) || function.equals("_!=_")
                        && (a.type().equals("Timestamp") || b.type().equals("Timestamp")))
                    fail("CEL_POLICY_DENIED", path);
                if (dcm.type(a.type()).kind().equals("enum")) assign(b, a.type(), dcm, path);
                else if (dcm.type(b.type()).kind().equals("enum")) assign(a, b.type(), dcm, path);
                else assign(a, b.type(), dcm, path);
            } else {
                for (Value arg : args) if (!arg.type().equals("Boolean")) fail("TYPE_MISMATCH", path);
            }
            return new Value("Boolean");
        }

        private record Operation(String type,String overload) {}
        private Operation numericCall(Expr.Call call,List<Value> args) {
            String f=call.getFunction();
            if (f.equals("decimal")) {
                Expr literal=call.getArgs(0);
                if (!literal.hasConstExpr() || !literal.getConstExpr().hasStringValue()) fail("TYPE_MISMATCH",path);
                canonicalNumber(literal.getConstExpr().getStringValue(),"Decimal",path);
                return new Operation("Decimal","keir_decimal_from_string");
            }
            if (f.equals("toDecimal") || f.equals("toInteger")) {
                if (!args.get(0).type.equals(f.equals("toDecimal") ? "Integer" : "Decimal")) fail("TYPE_MISMATCH",path);
                return new Operation(f.equals("toDecimal") ? "Decimal" : "Integer",
                        f.equals("toDecimal") ? "keir_decimal_from_integer" : "keir_integer_from_decimal");
            }
            if (f.equals("size")) {
                if (!dcm.type(args.get(0).type).list()) fail("CEL_POLICY_DENIED",path);
                return new Operation("Integer","size_list");
            }
            if (Set.of("round","multiplyRounded","divideRounded").contains(f)) {
                int index=args.size()-2;
                var scale=call.getArgs(index).getConstExpr(); var mode=call.getArgs(index+1).getConstExpr();
                if (!args.subList(0,index).stream().allMatch(a -> a.type.equals("Decimal"))
                        || !scale.hasInt64Value() || scale.getInt64Value()<0 || scale.getInt64Value()>18
                        || !mode.hasStringValue() || !ROUNDING.contains(mode.getStringValue())) fail("TYPE_MISMATCH",path);
                return new Operation("Decimal",Map.of("round","keir_decimal_round","multiplyRounded","keir_decimal_multiply_rounded",
                        "divideRounded","keir_decimal_divide_rounded").get(f));
            }
            if (!NUMERIC_OPERATORS.containsKey(f) || !args.stream().anyMatch(a -> dcm.numeric(a.type))) return null;
            String type=args.get(0).type;
            if (!dcm.numeric(type) || !args.stream().allMatch(a -> a.type.equals(type))) fail("TYPE_MISMATCH",path);
            String operation=NUMERIC_OPERATORS.get(f);
            return new Operation(Set.of("less","less_equals","greater","greater_equals").contains(operation) ? "Boolean" : type,
                    type.equals("Integer") ? operation+"_int64" : "keir_decimal_"+operation.replace("_equals","_equal"));
        }

        private Value branches(Value a, Value b) {
            if (!scalar(a) || !scalar(b))
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

        private boolean scalar(Value value) {
            var type = dcm.type(value.type());
            return type != null && type.scalar();
        }

        private boolean membershipNeedle(Value needle, Expr source, String element) {
            if (needle.type().equals(element)) return true;
            return dcm.type(element).kind().equals("enum") && source.hasConstExpr()
                    && source.getConstExpr().hasStringValue()
                    && dcm.type(element).values().contains(source.getConstExpr().getStringValue());
        }

        private static boolean pair(List<Value> args, String left, String right) {
            return args.get(0).type().equals(left) && args.get(1).type().equals(right);
        }

        private static Presence presence(Expr expression) {
            if (!expression.hasSelectExpr() || !expression.getSelectExpr().getOperand().hasIdentExpr()) return null;
            var select = expression.getSelectExpr();
            return new Presence(select.getOperand().getIdentExpr().getName(), select.getField());
        }

        private static Set<Presence> branchProofs(Expr.Call call, int index, Set<Presence> incoming) {
            if (index == 0 || !Set.of("_&&_", "_?_:_").contains(call.getFunction())) return incoming;
            Expr condition = call.getArgs(0);
            boolean negated = condition.hasCallExpr() && condition.getCallExpr().getFunction().equals("!_")
                    && condition.getCallExpr().getArgsCount() == 1;
            if (negated && index == 2) condition = condition.getCallExpr().getArgs(0);
            else if (negated || index != 1) return incoming;
            if (!condition.hasSelectExpr() || !condition.getSelectExpr().getTestOnly()) return incoming;
            Presence proof = presence(condition);
            if (proof == null || incoming.contains(proof)) return incoming;
            var extended = new HashSet<>(incoming);
            extended.add(proof);
            return extended;
        }

        private Set<String> branchGuards(Expr.Call call, int index, Set<String> incoming) {
            if (index != 1 || !call.getFunction().equals("_?_:_") || !call.getArgs(0).hasIdentExpr()) return incoming;
            String guard = call.getArgs(0).getIdentExpr().getName();
            // Every map value was validated as an earlier Boolean BIND before CALL lowering.
            if (!guardedResults.containsValue(guard) || incoming.contains(guard)) return incoming;
            var extended = new HashSet<>(incoming);
            extended.add(guard);
            return extended;
        }
    }

    private static boolean validTimestamp(String text) {
        try {
            Instant instant = Instant.parse(text);
            return !instant.isBefore(EARLIEST) && !instant.isAfter(LATEST) && UTC_MILLIS.format(instant).equals(text);
        } catch (DateTimeParseException invalid) { return false; }
    }

    private static boolean validDuration(String text) {
        return text.matches("(?:0|[1-9][0-9]{0,7})s")
                && Integer.parseInt(text.substring(0, text.length() - 1)) <= 31_536_000;
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
