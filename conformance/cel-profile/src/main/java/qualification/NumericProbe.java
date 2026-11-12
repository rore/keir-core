package qualification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOptions;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructTypeReference;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.expr.Type;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import dev.cel.runtime.CelStandardFunctions;
import dev.cel.runtime.standard.AddOperator;
import dev.cel.runtime.standard.DivideOperator;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Qualification-only numeric experiment. Not a Keir compiler, runtime or public codec. */
public final class NumericProbe {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private static final String DECIMAL = "keir.numeric.Decimal", INPUT = "keir.read.Input";
    private static final CelType D = StructTypeReference.create(DECIMAL), I = SimpleType.INT, S = SimpleType.STRING, B = SimpleType.BOOL;
    private static final CelOptions OPTIONS = CelOptions.current().enableHeterogeneousNumericComparisons(true)
            .comprehensionMaxIterations(0).maxExpressionCodePointSize(512).build();
    private static final Set<String> MODES = Set.of("UNNECESSARY", "DOWN", "FLOOR", "CEILING", "HALF_UP", "HALF_EVEN");
    private static final Map<String,String> VARIABLES = Map.of("a", "Decimal", "b", "Decimal", "d", "Decimal",
            "i", "Integer", "j", "Integer", "flag", "Boolean", "text", "String", "input", "Input");
    private static final BigInteger TEN = BigInteger.TEN;
    private record Dec(BigInteger c, int s) {}
    private record Model(Descriptors.FileDescriptor numeric, Descriptors.FileDescriptor input,
                         DescriptorProtos.FileDescriptorSet descriptors) {}
    private record Signature(String function, String id, CelType result, List<CelType> args) {}
    private record Row(String id, String model, String type, String source, CheckedExpr checked) {}
    private record Entry(Row row, Kernel kernel, CelRuntime.Program program) {}
    private record Prepared(Map<String,Kernel> kernels, Map<String,Entry> expressions) {}
    private static final class NumericFailure extends RuntimeException {}
    private static final class TimeFailure extends RuntimeException {}
    private static final List<Signature> SIGNATURES = signatures();
    private static Path directory;
    private static final Map<String,Object> progress = new LinkedHashMap<>();
    private static int checkedCases;

    private static void require(boolean value) { if (!value) throw new IllegalArgumentException("Probe policy/domain rejection"); }
    private static void equal(Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) throw new AssertionError("Independent expectation mismatch");
    }
    private static BigInteger power(int exponent) { require(exponent >= 0 && exponent <= 36); return TEN.pow(exponent); }
    private static Dec reduced(BigInteger c, int s) {
        if (c.signum() == 0) return new Dec(BigInteger.ZERO, 0);
        while (s > 0 && c.remainder(TEN).signum() == 0) { c = c.divide(TEN); s--; }
        return new Dec(c, s);
    }
    private static Dec bounded(BigInteger c, int s, String candidate) {
        Dec n = reduced(c, s); int scale = candidate.equals("fixed9") ? 9 : 18;
        require(n.s <= scale && n.c.abs().toString().length() <= 38);
        if (!candidate.equals("normalized18")) require(n.c.multiply(power(scale - n.s)).abs().toString().length() <= 38);
        return n;
    }
    private static String text(Dec n) {
        String digits = n.c.abs().toString();
        digits = "0".repeat(Math.max(0, n.s + 1 - digits.length())) + digits;
        return (n.c.signum() < 0 ? "-" : "") + (n.s == 0 ? digits : digits.substring(0, digits.length() - n.s) + "." + digits.substring(digits.length() - n.s));
    }
    private static Dec parse(Object raw, String candidate, boolean normalize) {
        require(raw instanceof String); String value = (String)raw;
        require(value.length() <= (normalize ? 256 : 40));
        require(value.matches(normalize ? "[+-]?[0-9]+(?:\\.[0-9]+)?" : "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]*[1-9])?"));
        int point = value.indexOf('.'), scale = point < 0 ? 0 : value.length() - point - 1;
        Dec result = reduced(new BigInteger(value.replace(".", "")), scale);
        require(result.s <= 18 && result.c.abs().toString().length() <= 38);
        result = bounded(result.c, result.s, candidate);
        if (!normalize) require(text(result).equals(value));
        return result;
    }
    private static String normalize(Object raw, String kind) {
        Dec n = parse(raw, "normalized18", true);
        if (kind.equals("integer")) { require(n.s == 0); n.c.longValueExact(); }
        else require(kind.equals("decimal"));
        return text(n);
    }
    private static long integer(Object raw) {
        require(raw instanceof String); String value = (String)raw;
        require(value.length() <= 20 && value.matches("0|-?[1-9][0-9]*"));
        return new BigInteger(value).longValueExact();
    }
    private static BigInteger quotient(BigInteger n, BigInteger d, String mode) {
        require(d.signum() != 0 && MODES.contains(mode));
        if (d.signum() < 0) { n = n.negate(); d = d.negate(); }
        var parts = n.abs().divideAndRemainder(d); BigInteger q = parts[0], r = parts[1];
        if (mode.equals("UNNECESSARY")) require(r.signum() == 0);
        boolean up = r.signum() != 0 && (mode.equals("FLOOR") && n.signum() < 0 || mode.equals("CEILING") && n.signum() > 0
                || (mode.equals("HALF_UP") || mode.equals("HALF_EVEN")) && (r.shiftLeft(1).compareTo(d) > 0
                || r.shiftLeft(1).equals(d) && (mode.equals("HALF_UP") || q.testBit(0))));
        if (up) q = q.add(BigInteger.ONE);
        return n.signum() < 0 ? q.negate() : q;
    }
    private static List<Signature> signatures() {
        var list = new ArrayList<Signature>();
        list.add(new Signature("decimal", "keir_decimal_from_string", D, List.of(S)));
        list.add(new Signature("toDecimal", "keir_decimal_from_integer", D, List.of(I)));
        list.add(new Signature("toInteger", "keir_integer_from_decimal", I, List.of(D)));
        String[] functions = {"_+_", "_-_", "_*_", "_/_", "-_", "_<_", "_<=_", "_>_", "_>=_"};
        String[] ids = {"add", "subtract", "multiply", "divide", "negate", "less", "less_equal", "greater", "greater_equal"};
        for (int i = 0; i < functions.length; i++) list.add(new Signature(functions[i], "keir_decimal_" + ids[i], i >= 5 ? B : D, i == 4 ? List.of(D) : List.of(D, D)));
        list.add(new Signature("round", "keir_decimal_round", D, List.of(D, I, S)));
        list.add(new Signature("multiplyRounded", "keir_decimal_multiply_rounded", D, List.of(D, D, I, S)));
        list.add(new Signature("divideRounded", "keir_decimal_divide_rounded", D, List.of(D, D, I, S)));
        return List.copyOf(list);
    }
    private static final class Kernel {
        final String candidate;
        final Model model;
        final CelRuntime runtime;
        final ConcurrentMap<String,AtomicLong> counters = new ConcurrentHashMap<>();
        Kernel(String candidate, Model model) {
            this.candidate = candidate; this.model = model;
            var standard = CelStandardFunctions.newBuilder().filterFunctions((f, overload) ->
                    overload != DivideOperator.DivideOverload.DIVIDE_INT64 && overload != AddOperator.AddOverload.ADD_TIMESTAMP_DURATION).build();
            List<CelFunctionBinding> bindings = new ArrayList<>();
            for (Signature s : SIGNATURES) bindings.add(CelFunctionBinding.from(s.id,
                    s.args.stream().<Class<?>>map(t -> t.equals(D) ? Message.class : t.equals(I) ? Long.class : String.class).toList(),
                    args -> { hit(s.id); try { return operation(s.id, args); } catch (RuntimeException failure) { throw new NumericFailure(); } }));
            bindings.add(CelFunctionBinding.from("divide_int64", Long.class, Long.class, (a, b) -> {
                hit("divide_int64");
                try { return quotient(BigInteger.valueOf(a), BigInteger.valueOf(b), "UNNECESSARY").longValueExact(); }
                catch (RuntimeException invalid) { throw new NumericFailure(); }
            }));
            bindings.add(CelFunctionBinding.from("add_timestamp_duration", Instant.class, java.time.Duration.class, (a, b) -> {
                hit("add_timestamp_duration"); Instant result = a.plus(b);
                if (result.isAfter(Instant.parse("9999-12-31T23:59:59.999Z")) || result.isBefore(Instant.parse("0001-01-01T00:00:00.000Z"))) throw new TimeFailure();
                return result;
            }));
            runtime = CelRuntimeFactory.plannerRuntimeBuilder().setOptions(OPTIONS).addFileTypes(model.numeric, model.input,
                    Timestamp.getDescriptor().getFile(), com.google.protobuf.Duration.getDescriptor().getFile())
                    .setStandardFunctions(standard).addFunctionBindings(bindings).build();
        }
        void hit(String name) { counters.computeIfAbsent(name, ignored -> new AtomicLong()).incrementAndGet(); }
        long count(String name) { var counter = counters.get(name); return counter == null ? 0 : counter.get(); }
        Map<String,Long> counts() { var result = new TreeMap<String,Long>(); counters.forEach((name, n) -> result.put(name, n.get())); return result; }
        Message message(Dec n) {
            var descriptor = model.numeric.findMessageTypeByName("Decimal");
            return DynamicMessage.newBuilder(descriptor).setField(descriptor.findFieldByName("value"), text(n)).build();
        }
        Dec read(Object raw) {
            hit("decimalOperandOrResultParses"); require(raw instanceof Message); Message message = (Message)raw;
            require(message.getDescriptorForType().getFullName().equals(DECIMAL) && message.getUnknownFields().asMap().isEmpty());
            return parse(message.getField(message.getDescriptorForType().findFieldByName("value")), candidate, false);
        }
        Message result(BigInteger c, int s) { return message(bounded(c, s, candidate)); }
        Object operation(String id, Object[] args) {
            if (id.equals("keir_decimal_from_string")) return message(parse(args[0], candidate, false));
            if (id.equals("keir_decimal_from_integer")) return result(BigInteger.valueOf((Long)args[0]), 0);
            Dec a = read(args[0]);
            if (id.equals("keir_integer_from_decimal")) { require(a.s == 0); return a.c.longValueExact(); }
            if (id.equals("keir_decimal_negate")) return result(a.c.negate(), a.s);
            if (id.equals("keir_decimal_round")) {
                int s = Math.toIntExact((Long)args[1]); require(s >= 0 && s <= 18);
                return a.s <= s ? result(a.c, a.s) : result(quotient(a.c, power(a.s - s), (String)args[2]), s);
            }
            Dec b = read(args[1]); int s = Math.max(a.s, b.s);
            BigInteger left = a.c.multiply(power(s - a.s)), right = b.c.multiply(power(s - b.s));
            return switch (id) {
                case "keir_decimal_add" -> result(left.add(right), s);
                case "keir_decimal_subtract" -> result(left.subtract(right), s);
                case "keir_decimal_multiply" -> result(a.c.multiply(b.c), a.s + b.s);
                case "keir_decimal_divide" -> result(quotient(a.c.multiply(power(18 + b.s - a.s)), b.c, "UNNECESSARY"), 18);
                case "keir_decimal_less" -> left.compareTo(right) < 0;
                case "keir_decimal_less_equal" -> left.compareTo(right) <= 0;
                case "keir_decimal_greater" -> left.compareTo(right) > 0;
                case "keir_decimal_greater_equal" -> left.compareTo(right) >= 0;
                case "keir_decimal_multiply_rounded", "keir_decimal_divide_rounded" -> {
                    int digits = Math.toIntExact((Long)args[2]); require(digits >= 0 && digits <= 18); String mode = (String)args[3];
                    if (id.equals("keir_decimal_multiply_rounded")) {
                        BigInteger product = a.c.multiply(b.c); int productScale = a.s + b.s;
                        yield productScale <= digits ? result(product, productScale) : result(quotient(product, power(productScale - digits), mode), digits);
                    }
                    int exponent = digits + b.s - a.s;
                    yield result(quotient(a.c.multiply(power(Math.max(0, exponent))), b.c.multiply(power(Math.max(0, -exponent))), mode), digits);
                }
                default -> throw new IllegalArgumentException("Unknown probe operation");
            };
        }
        Map<String,Object> project(Map<String,Object> supplied, String kind) {
            hit("projections");
            var raw = new HashMap<String,Object>(Map.of("a", "1.25", "b", "2", "d", "1", "i", "6", "j", "2", "flag", true,
                    "text", "tenant", "input", kind.equals("numeric") ? Map.of() : Map.of("flag", false, "text", "", "tags", List.of())));
            raw.putAll(supplied); require(raw.get("flag") instanceof Boolean && raw.get("text") instanceof String);
            var vars = new HashMap<String,Object>(); vars.put("flag", raw.get("flag")); vars.put("text", raw.get("text"));
            vars.put("i", integer(raw.get("i"))); vars.put("j", integer(raw.get("j")));
            for (String name : List.of("a", "b", "d")) vars.put(name, message(parse(raw.get(name), candidate, false)));
            Map<String,Object> input = map(raw.get("input"));
            var descriptor = model.input.findMessageTypeByName("Input"); var builder = DynamicMessage.newBuilder(descriptor);
            if (kind.equals("numeric")) {
                require(Set.of("maybe", "zero").containsAll(input.keySet()));
                if (input.containsKey("maybe")) builder.setField(descriptor.findFieldByName("maybe"), message(parse(input.get("maybe"), candidate, false)));
                if (input.containsKey("zero")) builder.setField(descriptor.findFieldByName("zero"), integer(input.get("zero")));
            } else {
                equal(input.keySet(), Set.of("flag", "text", "tags")); require(input.get("flag") instanceof Boolean && input.get("text") instanceof String);
                List<Object> tags = list(input.get("tags")); require(tags.size() <= 16 && tags.stream().allMatch(x -> x instanceof String t && t.length() <= 256));
                builder.setField(descriptor.findFieldByName("flag"), input.get("flag")); builder.setField(descriptor.findFieldByName("text"), input.get("text"));
                builder.setField(descriptor.findFieldByName("tags"), tags);
            }
            vars.put("input", builder.build()); return vars;
        }
        Object output(Object value, String type) {
            return switch (type) {
                case "Decimal" -> text(read(value));
                case "Integer" -> { require(value instanceof Long); yield Long.toString((Long)value); }
                case "Boolean" -> { require(value instanceof Boolean); yield value; }
                case "String" -> { require(value instanceof String); yield value; }
                default -> throw new IllegalArgumentException("Result type");
            };
        }
    }

    private static Model model(boolean numeric) throws Exception {
        var string = DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
        var decimal = DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_numeric.proto").setPackage("keir.numeric").setSyntax("proto3")
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Decimal").addField(field("value", 1, string))).build();
        var input = DescriptorProtos.DescriptorProto.newBuilder().setName("Input");
        if (numeric) {
            input.addField(field("maybe", 1, DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".keir.numeric.Decimal"));
            input.addOneofDecl(DescriptorProtos.OneofDescriptorProto.newBuilder().setName("_zero"));
            input.addField(field("zero", 2, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64).setProto3Optional(true).setOneofIndex(0));
        } else {
            input.addField(field("flag", 1, DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL));
            input.addField(field("tags", 2, string).setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED));
            input.addField(field("text", 3, string));
        }
        var file = DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_read.proto").setPackage("keir.read").setSyntax("proto3")
                .addDependency("keir_numeric.proto").addMessageType(input).build();
        var set = DescriptorProtos.FileDescriptorSet.newBuilder().addFile(Timestamp.getDescriptor().getFile().toProto())
                .addFile(com.google.protobuf.Duration.getDescriptor().getFile().toProto()).addFile(decimal).addFile(file).build();
        require(set.getSerializedSize() <= 65536);
        // Both engines derive their registry from these same emitted descriptor bytes.
        var decoded = DescriptorProtos.FileDescriptorSet.parseFrom(deterministic(set));
        var decodedDecimal = Descriptors.FileDescriptor.buildFrom(decoded.getFile(2), new Descriptors.FileDescriptor[0]);
        var decodedInput = Descriptors.FileDescriptor.buildFrom(decoded.getFile(3), new Descriptors.FileDescriptor[]{decodedDecimal});
        return new Model(decodedDecimal, decodedInput, decoded);
    }
    private static DescriptorProtos.FieldDescriptorProto.Builder field(String name, int number, DescriptorProtos.FieldDescriptorProto.Type type) {
        return DescriptorProtos.FieldDescriptorProto.newBuilder().setName(name).setJsonName(name).setNumber(number).setType(type)
                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
    }
    private static CheckedExpr compile(String source, Model model) throws Exception {
        require(source.getBytes(StandardCharsets.UTF_8).length <= 512);
        var builder = CelCompilerFactory.standardCelCompilerBuilder().setOptions(OPTIONS).setStandardMacros(Set.of(CelStandardMacro.HAS)).addFileTypes(model.descriptors);
        VARIABLES.forEach((name, type) -> builder.addVar(name, celType(type)));
        for (Signature signature : SIGNATURES) builder.addFunctionDeclarations(CelFunctionDecl.newFunctionDeclaration(signature.function,
                CelOverloadDecl.newGlobalOverload(signature.id, signature.result, signature.args)));
        return CelProtoAbstractSyntaxTree.fromCelAst(builder.build().compile(source).getAst()).toCheckedExpr();
    }
    private static CelType celType(String name) {
        return switch (name) { case "Integer" -> I; case "String" -> S; case "Boolean" -> B; case "Decimal" -> D;
            default -> StructTypeReference.create(INPUT); };
    }
    private static Type wire(String name) {
        var type = Type.newBuilder();
        return switch (name) {
            case "Integer" -> type.setPrimitive(Type.PrimitiveType.INT64).build();
            case "String" -> type.setPrimitive(Type.PrimitiveType.STRING).build();
            case "Boolean" -> type.setPrimitive(Type.PrimitiveType.BOOL).build();
            case "Decimal" -> type.setMessageType(DECIMAL).build();
            case "Input" -> type.setMessageType(INPUT).build();
            case "Tags" -> type.setListType(Type.ListType.newBuilder().setElemType(wire("String"))).build();
            case "Timestamp" -> type.setWellKnown(Type.WellKnownType.TIMESTAMP).build();
            case "Duration" -> type.setWellKnown(Type.WellKnownType.DURATION).build();
            default -> throw new IllegalArgumentException("Checked type");
        };
    }
    private static final class Gate {
        final CheckedExpr checked; final String model;
        final Set<Long> ids = new HashSet<>(), refs = new HashSet<>();
        Gate(CheckedExpr checked, String model) { this.checked = checked; this.model = model; }
        String verify() {
            String type = visit(checked.getExpr(), 1, Set.of());
            equal(ids, checked.getTypeMapMap().keySet()); equal(refs, checked.getReferenceMapMap().keySet()); return type;
        }
        String visit(Expr expr, int depth, Set<String> present) {
            require(expr.getId() > 0 && ids.add(expr.getId()) && ids.size() <= 64 && depth <= 16); String type;
            if (expr.hasConstExpr()) {
                var c = expr.getConstExpr();
                if (c.hasBoolValue()) type = "Boolean";
                else if (c.hasInt64Value()) type = "Integer";
                else { require(c.hasStringValue() && c.getStringValue().getBytes(StandardCharsets.UTF_8).length <= 256); type = "String"; }
            } else if (expr.hasIdentExpr()) {
                String name = expr.getIdentExpr().getName(); type = VARIABLES.get(name); require(type != null);
                var ref = checked.getReferenceMapMap().get(expr.getId()); require(ref != null && ref.getName().equals(name) && !ref.hasValue() && ref.getOverloadIdCount() == 0); refs.add(expr.getId());
            } else if (expr.hasSelectExpr()) {
                var select = expr.getSelectExpr(); equal(visit(select.getOperand(), depth + 1, present), "Input");
                Map<String,String> fields = model.equals("numeric") ? Map.of("maybe", "Decimal", "zero", "Integer") : Map.of("flag", "Boolean", "text", "String", "tags", "Tags");
                type = fields.get(select.getField()); require(type != null);
                if (select.getTestOnly()) { require(model.equals("numeric")); type = "Boolean"; }
                else if (model.equals("numeric")) require(present.contains(select.getField()));
            } else {
                require(expr.hasCallExpr()); var call = expr.getCallExpr(); String f = call.getFunction(); require(!call.hasTarget());
                int arity = Set.of("_?_:_", "round").contains(f) ? 3 : Set.of("multiplyRounded", "divideRounded").contains(f) ? 4 :
                        Set.of("!_", "-_", "decimal", "toDecimal", "toInteger", "timestamp", "duration", "size").contains(f) ? 1 : 2;
                require(call.getArgsCount() == arity); Expr proof = call.getArgs(0);
                boolean negated = proof.hasCallExpr() && proof.getCallExpr().getFunction().equals("!_");
                if (negated) proof = proof.getCallExpr().getArgs(0);
                String field = proof.hasSelectExpr() && proof.getSelectExpr().getTestOnly() ? proof.getSelectExpr().getField() : null;
                var args = new ArrayList<String>();
                for (int i = 0; i < arity; i++) {
                    var proofs = new HashSet<>(present);
                    if (field != null && (f.equals("_&&_") && i == 1 && !negated || f.equals("_?_:_") && i == (negated ? 2 : 1))) proofs.add(field);
                    args.add(visit(call.getArgs(i), depth + 1, proofs));
                }
                String overload;
                if (Set.of("_==_", "_!=_").contains(f)) { require(args.get(0).equals(args.get(1)) && !Set.of("Input", "Tags", "Duration").contains(args.get(0))); type = "Boolean"; overload = f.equals("_==_") ? "equals" : "not_equals"; }
                else if (f.equals("_?_:_")) { equal(args.get(0), "Boolean"); equal(args.get(1), args.get(2)); require(!Set.of("Input", "Tags").contains(args.get(1))); type = args.get(1); overload = "conditional"; }
                else if (Set.of("!_", "_&&_", "_||_").contains(f)) { require(args.stream().allMatch(t -> t.equals("Boolean"))); type = "Boolean"; overload = Map.of("!_", "logical_not", "_&&_", "logical_and", "_||_", "logical_or").get(f); }
                else if (f.equals("decimal")) { require(call.getArgs(0).hasConstExpr() && call.getArgs(0).getConstExpr().hasStringValue()); parse(call.getArgs(0).getConstExpr().getStringValue(), "normalized18", false); type = "Decimal"; overload = "keir_decimal_from_string"; }
                else if (f.equals("toDecimal") || f.equals("toInteger")) { equal(args.get(0), f.equals("toDecimal") ? "Integer" : "Decimal"); type = f.equals("toDecimal") ? "Decimal" : "Integer"; overload = f.equals("toDecimal") ? "keir_decimal_from_integer" : "keir_integer_from_decimal"; }
                else if (f.equals("size")) { equal(args.get(0), "Tags"); type = "Integer"; overload = "size_list"; }
                else if (f.equals("timestamp") || f.equals("duration")) { require(call.getArgs(0).hasConstExpr() && call.getArgs(0).getConstExpr().hasStringValue()); type = f.equals("timestamp") ? "Timestamp" : "Duration"; overload = "string_to_" + f; }
                else if (Set.of("round", "multiplyRounded", "divideRounded").contains(f)) {
                    int offset = arity - 2; var scale = call.getArgs(offset).getConstExpr(); var mode = call.getArgs(offset + 1).getConstExpr();
                    require(args.subList(0, offset).stream().allMatch(t -> t.equals("Decimal")) && scale.hasInt64Value() && scale.getInt64Value() >= 0 && scale.getInt64Value() <= 18 && mode.hasStringValue() && MODES.contains(mode.getStringValue()));
                    type = "Decimal"; overload = Map.of("round", "keir_decimal_round", "multiplyRounded", "keir_decimal_multiply_rounded", "divideRounded", "keir_decimal_divide_rounded").get(f);
                } else {
                    String operation = Map.of("_+_", "add", "_-_", "subtract", "_*_", "multiply", "_/_", "divide", "-_", "negate", "_<_", "less", "_<=_", "less_equals", "_>_", "greater", "_>=_", "greater_equals").get(f); require(operation != null);
                    if (f.equals("_+_") && args.equals(List.of("String", "String"))) { type = "String"; overload = "add_string"; }
                    else if (f.equals("_+_") && args.equals(List.of("Timestamp", "Duration"))) { type = "Timestamp"; overload = "add_timestamp_duration"; }
                    else { require(args.stream().allMatch(t -> t.equals(args.get(0))) && Set.of("Integer", "Decimal").contains(args.get(0)));
                        type = Set.of("less", "less_equals", "greater", "greater_equals").contains(operation) ? "Boolean" : args.get(0);
                        overload = args.get(0).equals("Integer") ? operation + "_int64" : "keir_decimal_" + operation.replace("_equals", "_equal"); }
                }
                var ref = checked.getReferenceMapMap().get(expr.getId()); require(ref != null && ref.getName().isEmpty() && !ref.hasValue()); equal(ref.getOverloadIdList(), List.of(overload)); refs.add(expr.getId());
            }
            equal(checked.getTypeMapMap().get(expr.getId()), wire(type)); return type;
        }
    }

    private static Prepared prepare(List<Row> rows, Map<String,Model> models, String candidate) throws Exception {
        var kernels = new HashMap<String,Kernel>(); models.forEach((name, model) -> kernels.put(name, new Kernel(candidate, model)));
        var expressions = new HashMap<String,Entry>();
        for (Row row : rows) {
            var checked = CheckedExpr.parseFrom(deterministic(row.checked));
            equal(new Gate(checked, row.model).verify(), row.type);
            Kernel k = kernels.get(row.model);
            expressions.put(row.id, new Entry(row, k, k.runtime.createProgram(CelProtoAbstractSyntaxTree.fromCheckedExpr(checked).getAst())));
        }
        return new Prepared(Map.copyOf(kernels), Map.copyOf(expressions));
    }
    private static Object eval(Entry entry, Map<String,Object> vars) {
        try { return entry.kernel.output(entry.program.eval(vars), entry.row.type); }
        catch (Exception failure) { return expressionError(failure); }
    }
    private static String expressionError(Exception failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) if (t instanceof TimeFailure) return "TIME_ERROR";
        return "ERROR";
    }
    private static Object evaluate(Prepared p, Map<String,Object> spec) {
        Entry entry = p.expressions.get(spec.get("id"));
        var vars = entry.kernel.project(spec.containsKey("vars") ? map(spec.get("vars")) : Map.of(), (String)spec.getOrDefault("model", "plain"));
        for (Object name : list(spec.getOrDefault("omit", List.of()))) { require(name.equals("a")); vars.remove(name); }
        if (spec.containsKey("bind")) {
            Entry binding = p.expressions.get(spec.get("id") + "-bind");
            try {
                Object first = binding.program.eval(vars);
                binding.kernel.read(first);
                vars.put("d", first);
            } catch (Exception failure) { return expressionError(failure); }
        }
        return eval(entry, vars);
    }
    @FunctionalInterface private interface Action { Object run() throws Exception; }
    private static Map<String,Object> distribution(long[] values, long elapsed) {
        long[] sorted = values.clone(); Arrays.sort(sorted);
        return Map.of("checked", values.length, "elapsedNs", elapsed, "throughputPerSecond", values.length * 1e9 / elapsed,
                "p50Ns", sorted[(int)Math.ceil(sorted.length * .5) - 1], "p95Ns", sorted[(int)Math.ceil(sorted.length * .95) - 1],
                "p99Ns", sorted[(int)Math.ceil(sorted.length * .99) - 1], "maxNs", sorted[sorted.length - 1]);
    }
    private static Map<String,Object> measure(Action action, Object expected) throws Exception {
        for (int i = 0; i < 100; i++) equal(action.run(), expected);
        long[] times = new long[1000]; long start = System.nanoTime();
        for (int i = 0; i < times.length; i++) { long t = System.nanoTime(); Object value = action.run(); times[i] = System.nanoTime() - t; equal(value, expected); }
        return distribution(times, System.nanoTime() - start);
    }
    private static Map<String,Object> measureEngine(Entry entry, Map<String,Object> vars, Object expected) throws Exception {
        for (int i = 0; i < 100; i++) equal(entry.kernel.output(entry.program.eval(vars), entry.row.type), expected);
        long[] times = new long[1000]; long start = System.nanoTime();
        for (int i = 0; i < times.length; i++) {
            long t = System.nanoTime(); Object value = entry.program.eval(vars); times[i] = System.nanoTime() - t;
            equal(entry.kernel.output(value, entry.row.type), expected);
        }
        return distribution(times, System.nanoTime() - start);
    }
    private static Map<String,Object> memory() throws Exception {
        var result = new LinkedHashMap<String,Object>(); result.put("heapBytes", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        result.put("cpuNs", ProcessHandle.current().info().totalCpuDuration().orElseThrow().toNanos());
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean threads
                && threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled()) {
            result.put("currentThreadAllocatedBytes", threads.getThreadAllocatedBytes(Thread.currentThread().threadId()));
            result.put("allocationScope", "current measuring thread only; not whole process or concurrent workers");
        }
        Path proc = Path.of("/proc/self/status"); result.put("rssSupported", Files.isRegularFile(proc));
        if (Files.isRegularFile(proc)) for (String line : Files.readAllLines(proc)) if (line.startsWith("VmHWM:")) {
            long peak = Long.parseLong(line.trim().split("\\s+")[1]) * 1024;
            require(peak <= 512L * 1024 * 1024); result.put("processPeakRssBytes", peak);
        }
        return result;
    }
    private static long retained() { System.gc(); return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(); }
    private static Map<String,Object> run(Map<String,Object> fixtures, List<Row> rows, Map<String,Model> models) throws Exception {
        var report = new LinkedHashMap<String,Object>(); report.put("status", "running");
        report.put("java", System.getProperty("java.version")); report.put("os", System.getProperty("os.name")); report.put("arch", System.getProperty("os.arch"));
        report.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        report.put("adaptation", "value:string is reparsed per Decimal operand; no parse-once or tenant cache claim");
        report.put("projectionWorkload", "common eight-declaration activation for all families, not a legacy nonnumeric Core invocation");
        var threadBean = ManagementFactory.getThreadMXBean();
        boolean allocationSupported = threadBean instanceof com.sun.management.ThreadMXBean t && t.isThreadAllocatedMemorySupported();
        boolean allocationEnabled = allocationSupported && ((com.sun.management.ThreadMXBean)threadBean).isThreadAllocatedMemoryEnabled();
        report.put("allocation", Map.of("supported", allocationSupported, "enabled", allocationEnabled,
                "scope", "current thread cumulative bytes when already enabled; no counter enablement or whole-process claim"));
        var helperResults = new ArrayList<>();
        for (Object raw : list(fixtures.get("helpers"))) {
            var spec = map(raw); Object value;
            Object input = spec.containsKey("repeat") ? ((String)spec.get("repeat")).repeat(((Number)spec.get("count")).intValue()) : spec.get("input");
            try { value = normalize(input, (String)spec.get("kind")); } catch (RuntimeException invalid) { value = "ERROR"; }
            equal(value, spec.get("expected")); helperResults.add(Map.of("kind", spec.get("kind"), "actual", value));
        }
        report.put("helpers", helperResults);
        var cases = new ArrayList<>(); var counts = new TreeMap<String,Object>(); Prepared p = null;
        for (String candidate : List.of("fixed9", "fixed18", "normalized18")) {
            long start = System.nanoTime(); p = prepare(rows, models, candidate); long ns = System.nanoTime() - start;
            for (Object raw : list(fixtures.get("cases"))) {
                var spec = map(raw); Entry entry = p.expressions.get(spec.get("id")); String counter = (String)spec.getOrDefault("counter", "");
                progress.put("phase", candidate + "/" + spec.get("id")); progress.put("checkedCases", checkedCases); save("java-progress.json", progress);
                long before = entry.kernel.count(counter); Object actual = evaluate(p, spec), expected = spec.get("expected");
                if (expected instanceof Map<?,?>) expected = map(expected).get(candidate);
                equal(actual, expected); if (!counter.isEmpty()) require(entry.kernel.count(counter) > before);
                cases.add(Map.of("id", spec.get("id"), "candidate", candidate, "actual", actual));
                checkedCases++; progress.put("checkedCases", checkedCases);
            }
            var byModel = new TreeMap<String,Object>(); p.kernels.forEach((name, k) -> byModel.put(name, k.counts()));
            counts.put(candidate, Map.of("preparationNs", ns, "byModel", byModel));
        }
        report.put("cases", cases); report.put("counters", counts);
        int negatives = 0;
        for (var item : p.kernels.entrySet()) {
            for (Object invalid : List.of(0L, 1.0, BigInteger.ONE, new java.math.BigDecimal("1"), "100.00", "-0")) {
                try { item.getValue().project(Map.of("a", invalid), item.getKey()); throw new AssertionError("Native/noncanonical value accepted"); }
                catch (RuntimeException expected) { negatives++; }
            }
            try { item.getValue().read(DynamicMessage.newBuilder(item.getValue().model.numeric.findMessageTypeByName("Decimal")).build()); throw new AssertionError("Default Decimal accepted"); }
            catch (RuntimeException expected) { negatives++; }
        }
        Row donor = rows.stream().filter(r -> r.id.equals("decimal-add")).findFirst().orElseThrow();
        var changes = List.of(donor.checked.toBuilder().clearTypeMap().build(),
                donor.checked.toBuilder().putTypeMap(99999, wire("Decimal")).build(),
                donor.checked.toBuilder().putReferenceMap(donor.checked.getExpr().getId(), donor.checked.getReferenceMapMap().get(donor.checked.getExpr().getId()).toBuilder().clearOverloadId().addOverloadId("forged").build()).build());
        for (var changed : changes) { try { new Gate(changed, "plain").verify(); throw new AssertionError("Forged metadata accepted"); } catch (IllegalArgumentException | AssertionError rejected) { if (rejected.getMessage().equals("Forged metadata accepted")) throw rejected; negatives++; } }
        Row intConstant = rows.stream().filter(r -> r.id.equals("integer-max")).findFirst().orElseThrow();
        var floating = intConstant.checked.toBuilder().setExpr(intConstant.checked.getExpr().toBuilder().setConstExpr(dev.cel.expr.Constant.newBuilder().setDoubleValue(1.5))).build();
        try { new Gate(floating, "plain").verify(); throw new AssertionError("Double accepted"); } catch (IllegalArgumentException rejected) { negatives++; }
        var oracle = list(fixtures.get("cases")).stream().map(NumericProbe::map).filter(s -> s.get("id").equals("decimal-add")).findFirst().orElseThrow();
        var wrongOracle = new HashMap<>(oracle); wrongOracle.put("expected", "deliberately-wrong");
        Object oracleActual = evaluate(p, wrongOracle);
        equal(oracleActual, oracle.get("expected")); // Evaluation/setup failures cannot satisfy this negative check.
        try { equal(oracleActual, wrongOracle.get("expected")); throw new IllegalStateException("Comparator did not reject"); } catch (AssertionError expected) { negatives++; }
        report.put("negativeChecks", negatives);
        Prepared measured = p;
        var recover = new HashMap<>(map(list(fixtures.get("cases")).stream().map(NumericProbe::map).filter(s -> s.get("id").equals("integer-exact-divide")).findFirst().orElseThrow()));
        recover.put("vars", Map.of("j", "0")); equal(evaluate(p, recover), "ERROR"); recover.remove("vars"); equal(evaluate(p, recover), "3");
        var decimalRecover = new HashMap<>(map(list(fixtures.get("cases")).stream().map(NumericProbe::map).filter(s -> s.get("id").equals("decimal-divide")).findFirst().orElseThrow()));
        decimalRecover.put("vars", Map.of("b", "0")); equal(evaluate(p, decimalRecover), "ERROR"); decimalRecover.remove("vars"); equal(evaluate(p, decimalRecover), "0.625");
        var timings = new ArrayList<>(); long baseline = retained();
        for (Object raw : list(fixtures.get("cases"))) {
            var spec = map(raw); if (!Boolean.TRUE.equals(spec.get("timed"))) continue;
            progress.put("phase", "timing/" + spec.get("id")); save("java-progress.json", progress);
            Entry entry = p.expressions.get(spec.get("id")); var vars = entry.kernel.project(spec.containsKey("vars") ? map(spec.get("vars")) : Map.of(), (String)spec.getOrDefault("model", "plain"));
            var before = memory(); var engine = measureEngine(entry, vars, spec.get("expected")); var warm = measure(() -> eval(entry, vars), spec.get("expected"));
            var complete = measure(() -> evaluate(measured, spec), spec.get("expected"));
            timings.add(Map.of("id", spec.get("id"), "preparedEngineEvaluation", engine, "preparedEvaluationAndResultValidation", warm,
                    "projectionEvaluationAndResultValidation", complete, "before", before, "after", memory(), "retainedHeapBytes", retained()));
        }
        report.put("timings", timings); report.put("baselineRetainedHeapBytes", baseline);
        report.put("normalization", measure(() -> normalize("100.00", "decimal"), "100"));
        Kernel plain = p.kernels.get("plain"); report.put("projection", measure(() -> plain.project(Map.of(), "plain").get("i").getClass().getName(), "java.lang.Long"));
        var warmPreparations = new ArrayList<>();
        for (String id : List.of("scalar", "integer-add", "decimal-add")) {
            Row row = p.expressions.get(id).row; long[] prepTimes = new long[30];
            for (int i = 0; i < prepTimes.length; i++) { long t = System.nanoTime(); prepare(List.of(row), Map.of(row.model, models.get(row.model)), "normalized18"); prepTimes[i] = System.nanoTime() - t; }
            var measuredPrep = new LinkedHashMap<>(distribution(prepTimes, Arrays.stream(prepTimes).sum())); measuredPrep.put("id", id); warmPreparations.add(measuredPrep);
        }
        report.put("warmPreparations", warmPreparations);
        var concurrent = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            for (Object raw : list(fixtures.get("concurrent"))) {
                var spec = map(raw); Entry entry = p.expressions.get(spec.get("id"));
                progress.put("phase", "concurrency/" + spec.get("id")); save("java-progress.json", progress);
                for (int i = 0; i < 100; i++) lane(entry, i);
                CountDownLatch ready = new CountDownLatch(4), release = new CountDownLatch(1);
                var futures = new ArrayList<Future<long[]>>(); long start = System.nanoTime(); var before = memory();
                for (int lane = 0; lane < 4; lane++) { int offset = lane * 250;
                    futures.add(pool.submit(() -> { ready.countDown(); require(release.await(5, TimeUnit.SECONDS)); long[] values = new long[250];
                        for (int i = 0; i < 250; i++) values[i] = lane(entry, offset + i); return values; })); }
                require(ready.await(5, TimeUnit.SECONDS)); release.countDown(); long[] values = new long[1000]; int offset = 0;
                for (var future : futures) { long[] part = future.get(30, TimeUnit.SECONDS); System.arraycopy(part, 0, values, offset, part.length); offset += part.length; }
                var result = new LinkedHashMap<>(distribution(values, System.nanoTime() - start)); result.put("id", spec.get("id"));
                result.put("scheduling", "four actual executor threads sharing one immutable Program; fresh per-invocation activations"); result.put("before", before); result.put("after", memory()); concurrent.add(result);
            }
        } finally { pool.shutdownNow(); require(pool.awaitTermination(5, TimeUnit.SECONDS)); }
        report.put("concurrency", concurrent); var finalCounters = new TreeMap<String,Object>(); p.kernels.forEach((name, k) -> finalCounters.put(name, k.counts()));
        report.put("finalCounters", finalCounters); report.put("memory", memory()); report.put("finalRetainedHeapBytes", retained());
        report.put("status", "passed"); return report;
    }
    private static long lane(Entry entry, int index) {
        String tenant = "tenant-" + index; long start = System.nanoTime();
        Object actual = eval(entry, entry.kernel.project(Map.of("i", Integer.toString(index), "text", tenant), "plain"));
        long elapsed = System.nanoTime() - start;
        Object expected = switch (entry.row.type) { case "String" -> tenant + "-ok"; case "Integer" -> Integer.toString(index + 1); default -> index + ".25"; };
        equal(actual, expected);
        return elapsed;
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { require(value instanceof Map<?,?>); return (Map<String,Object>)value; }
    @SuppressWarnings("unchecked") private static List<Object> list(Object value) { require(value instanceof List<?>); return (List<Object>)value; }
    private static byte[] deterministic(Message value) throws Exception {
        var bytes = new ByteArrayOutputStream(); var stream = CodedOutputStream.newInstance(bytes); stream.useDeterministicSerialization(); value.writeTo(stream); stream.flush(); return bytes.toByteArray();
    }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void save(String name, Object value) throws Exception { Files.write(directory.resolve(name), JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(value)); }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        require(args.length == 2 && Runtime.version().feature() == 21); directory = Path.of(args[1]);
        System.out.println("NUMERIC_READY");
        try {
            long compilationStart = System.nanoTime();
            byte[] bytes = Files.readAllBytes(Path.of(args[0])); require(bytes.length <= 1048576);
            Map<String,Object> fixtures = JSON.readValue(bytes, Map.class);
            require(list(fixtures.get("helpers")).size() <= 64 && list(fixtures.get("cases")).size() <= 128 && list(fixtures.get("rejectSources")).size() <= 32 && list(fixtures.get("concurrent")).size() == 3);
            require(list(fixtures.get("cases")).stream().map(NumericProbe::map).filter(s -> Boolean.TRUE.equals(s.get("timed"))).count() <= 8);
            for (Object raw : list(fixtures.get("helpers"))) { var spec = map(raw); if (spec.containsKey("repeat")) require(spec.get("repeat").equals("0") && ((Number)spec.get("count")).intValue() >= 0 && ((Number)spec.get("count")).intValue() <= 257); }
            var models = Map.of("plain", model(false), "numeric", model(true)); var rows = new ArrayList<Row>(); var emitted = new ArrayList<>(); var names = new HashSet<String>();
            var specs = new ArrayList<>(list(fixtures.get("cases"))); specs.addAll(list(fixtures.get("concurrent")));
            for (Object raw : specs) {
                var spec = map(raw); String id = (String)spec.get("id"), kind = (String)spec.getOrDefault("model", "plain");
                require(id.matches("[a-z][a-z0-9-]{0,63}") && names.add(id));
                if (spec.containsKey("bind")) rows.add(new Row(id + "-bind", kind, "Decimal", (String)spec.get("bind"), compile((String)spec.get("bind"), models.get(kind))));
                rows.add(new Row(id, kind, (String)spec.get("type"), (String)spec.get("source"), compile((String)spec.get("source"), models.get(kind))));
            }
            for (Row row : rows) {
                equal(new Gate(row.checked, row.model).verify(), row.type); byte[] checked = deterministic(row.checked); require(checked.length <= 16384);
                equal(hash(checked), hash(deterministic(compile(row.source, models.get(row.model)))));
                emitted.add(Map.of("id", row.id, "model", row.model, "type", row.type, "checked", Base64.getEncoder().encodeToString(checked), "sha256", hash(checked),
                        "typeMap", row.checked.getTypeMapMap().toString(), "referenceMap", row.checked.getReferenceMapMap().toString()));
            }
            int rejects = 0;
            for (Object source : list(fixtures.get("rejectSources"))) {
                try { new Gate(compile((String)source, models.get("numeric")), "numeric").verify(); throw new AssertionError("Forbidden source accepted"); }
                catch (Exception rejected) { rejects++; }
            }
            var descriptors = new TreeMap<String,String>(); var descriptorHashes = new TreeMap<String,String>();
            for (var model : models.entrySet()) { byte[] descriptor = deterministic(model.getValue().descriptors); descriptors.put(model.getKey(), Base64.getEncoder().encodeToString(descriptor)); descriptorHashes.put(model.getKey(), hash(descriptor)); }
            save("bundle.json", Map.of("descriptors", descriptors, "descriptorHashes", descriptorHashes, "expressions", emitted, "fixtureSha256", hash(bytes), "rejectedSources", rejects));
            long compilationNs = System.nanoTime() - compilationStart;
            var report = run(fixtures, rows, models); report.put("checkedBundleConstructionAndValidationNs", compilationNs);
            report.put("fixtureSha256", hash(bytes)); report.put("rejectedSources", rejects); save("java-report.json", report);
            System.out.println("Numeric Java probe completed; independent expectations passed.");
        } catch (Throwable failure) {
            save("java-report.json", Map.of("status", "failed", "stage", "Java numeric probe", "progress", progress, "errorClass", failure.getClass().getName()));
            System.err.println("Numeric Java probe failed: " + failure.getClass().getName()); failure.printStackTrace(System.err); System.exit(1);
        }
    }
}
