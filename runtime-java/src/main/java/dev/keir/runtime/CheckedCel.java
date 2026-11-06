package dev.keir.runtime;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.Timestamp;
import com.google.protobuf.WireFormat;
import dev.cel.common.CelOptions;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.expr.Type;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import dev.cel.runtime.CelStandardFunctions;
import dev.cel.runtime.standard.AddOperator;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import static dev.keir.runtime.RuntimeData.*;

/** Admission checks remain independent of the CEL planner's willingness to execute bytes. */
final class CheckedCel {
    private static final String EXPRESSIONS = "/artifact/expressions";
    private static final long MAX_ID = 9_007_199_254_740_991L;
    private static final Map<String,String> OVERLOADS = Map.of("_==_", "equals", "_!=_", "not_equals",
            "!_", "logical_not", "_&&_", "logical_and", "_||_", "logical_or", "_?_:_", "conditional",
            "@in", "in_list", "_<_", "less_timestamp", "timestamp", "string_to_timestamp", "duration", "string_to_duration");
    private static final Instant MIN_TIMESTAMP = Instant.parse("0001-01-01T00:00:00.000Z");
    private static final Instant MAX_TIMESTAMP = Instant.parse("9999-12-31T23:59:59.999Z");
    private static final DateTimeFormatter UTC_MILLIS = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    private CheckedCel() {}
    private record Value(String type, List<String> literals) { Value(String type) { this(type, null); } }
    private record Prepared(CelRuntime.Program program, Map<String,Object> variables, String resultType) {}
    private static final class TimestampOverflow extends RuntimeException {}

    private static boolean timestampInRange(Instant instant) {
        return !instant.isBefore(MIN_TIMESTAMP) && !instant.isAfter(MAX_TIMESTAMP);
    }

    static final class Model {
        private final Map<String,Object> dcm;
        private final Descriptors.FileDescriptor descriptor;
        private final CelRuntime runtime;
        private final int descriptorBytes;

        private Model(Map<String,Object> dcm, Descriptors.FileDescriptor descriptor, int descriptorBytes) {
            this.dcm = map(immutable(dcm),"/binding/dcm");
            this.descriptor = descriptor; this.descriptorBytes = descriptorBytes;
            var standard = CelStandardFunctions.newBuilder().filterFunctions((function, overload) ->
                    overload != AddOperator.AddOverload.ADD_TIMESTAMP_DURATION).build();
            var checkedAdd = CelFunctionBinding.from("add_timestamp_duration", Instant.class, Duration.class,
                    (instant, duration) -> {
                        Instant result = instant.plus(duration);
                        if (!timestampInRange(result)) throw new TimestampOverflow();
                        return result;
                    });
            this.runtime = CelRuntimeFactory.plannerRuntimeBuilder()
                    .setOptions(CelOptions.current().enableHeterogeneousNumericComparisons(true)
                            .comprehensionMaxIterations(0).build()).addFileTypes(descriptor)
                    .setStandardFunctions(standard).addFunctionBindings(checkedAdd).build();
        }

        int descriptorBytes() { return descriptorBytes; }
    }

    static Model bindModel(Map<String,Object> dcm) {
        var descriptors = expectedDescriptors(dcm);
        int size = descriptors.getSerializedSize();
        if (size > 65_536) throw new RuntimeFault("BUDGET_EXCEEDED","/binding/dcm/types");
        try {
            int recordFile = descriptors.getFileCount() - 1;
            var dependencies = recordFile == 0 ? new Descriptors.FileDescriptor[0]
                    : new Descriptors.FileDescriptor[]{Timestamp.getDescriptor().getFile()};
            return new Model(dcm,Descriptors.FileDescriptor.buildFrom(descriptors.getFile(recordFile),dependencies),size);
        } catch (Exception failure) { throw new RuntimeFault("ADMISSION_INVALID","/binding/dcm"); }
    }

    static final class Bundle {
        private final Map<String,Object> dcm;
        private final Descriptors.FileDescriptor descriptor;
        private final Map<String,Prepared> expressions;
        private final int totalNodes;

        private Bundle(Map<String,Object> dcm, Descriptors.FileDescriptor descriptor,
                       Map<String,Prepared> expressions, int totalNodes) {
            this.dcm = dcm; this.descriptor = descriptor;
            this.expressions = Map.copyOf(expressions); this.totalNodes = totalNodes;
        }

        int totalNodes() { return totalNodes; }

        Object evaluate(Map<String,Object> binding, Map<String,Object> state, String path) {
            if (binding.containsKey("literal"))
                return Artifact.value(dcm, (String)binding.get("type"), binding.get("literal"), path, "EXPRESSION_FAILED");
            Prepared expression = expressions.get(binding.get("expr"));
            if (expression == null) throw new RuntimeFault("EXPRESSION_FAILED", path);
            Map<String,Object> variables = new HashMap<>();
            for (var variable : expression.variables().entrySet()) {
                String type = (String)variable.getValue();
                Object value = Artifact.value(dcm, type, state.get(variable.getKey()), path, "EXPRESSION_FAILED");
                if (Artifact.record(dcm, type)) {
                    var record = DynamicMessage.newBuilder(descriptor.findMessageTypeByName(type));
                    for (var field : map(value, path).entrySet()) {
                        var definition = record.getDescriptorForType().findFieldByName(field.getKey());
                        Object fieldValue = field.getValue();
                        if (definition.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                            Instant instant = Artifact.timestamp((String)fieldValue);
                            fieldValue = Timestamp.newBuilder().setSeconds(instant.getEpochSecond())
                                    .setNanos(instant.getNano()).build();
                        }
                        record.setField(definition, fieldValue);
                    }
                    value = record.build();
                } else if (type.equals("Timestamp")) value = Artifact.timestamp((String)value);
                variables.put(variable.getKey(), value);
            }
            Object result;
            try { result = expression.program().eval(variables); }
            catch (Exception failure) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause())
                    if (cause instanceof TimestampOverflow) throw new RuntimeFault("BUDGET_EXCEEDED", path);
                throw new RuntimeFault("EXPRESSION_FAILED", path);
            }
            if (expression.resultType().equals("Timestamp")) result = timestampResult(result, path);
            return Artifact.value(dcm, expression.resultType(), result, path, "EXPRESSION_FAILED");
        }

        private static String timestampResult(Object result, String path) {
            Instant instant;
            if (result instanceof Instant nativeInstant) instant = nativeInstant;
            else if (result instanceof Message timestamp && timestamp.getDescriptorForType() == Timestamp.getDescriptor()) {
                var type = timestamp.getDescriptorForType();
                long seconds = (Long)timestamp.getField(type.findFieldByNumber(1));
                int nanos = (Integer)timestamp.getField(type.findFieldByNumber(2));
                if (seconds < MIN_TIMESTAMP.getEpochSecond() || seconds > MAX_TIMESTAMP.getEpochSecond()
                        || nanos < 0 || nanos > 999_999_999) throw new RuntimeFault("EXPRESSION_FAILED", path);
                instant = Instant.ofEpochSecond(seconds, nanos);
            } else throw new RuntimeFault("EXPRESSION_FAILED", path);
            if (!timestampInRange(instant) || instant.getNano() % 1_000_000 != 0)
                throw new RuntimeFault("EXPRESSION_FAILED", path);
            return UTC_MILLIS.format(instant);
        }
    }

    static Bundle prepare(Artifact.Plan plan, Model model) {
        require(model != null && model.dcm.equals(plan.dcm()),"/artifact/modelRef");
        Map<String,Object> dcm = model.dcm;
        try {
            Map<String,Prepared> prepared = new HashMap<>();
            int totalNodes = 0;
            for (var item : plan.contexts().entrySet()) {
                Map<String,Object> context = item.getValue();
                CheckedExpr checked = decode(CheckedExpr.parser(), CheckedExpr.getDescriptor(),
                        base64(context.get("checked"), 16_384, EXPRESSIONS), EXPRESSIONS);
                require(checked.hasExpr() && checked.getExprVersion().isEmpty() && checked.hasSourceInfo(), EXPRESSIONS);
                var source = checked.getSourceInfo();
                require(source.getSyntaxVersion().isEmpty() && source.getExtensionsCount() == 0
                        && source.getMacroCallsCount() == 0
                        && "<input>".equals(source.getLocation()), EXPRESSIONS);
                require(source.getLineOffsetsList().stream().allMatch(n -> n >= 0 && n <= 513)
                        && source.getPositionsMap().values().stream().allMatch(n -> n >= 0 && n <= 512), EXPRESSIONS);
                Map<String,Object> variables = map(context.get("variables"), EXPRESSIONS);
                var gate = new Gate(dcm, checked, variables);
                Value result = gate.visit(checked.getExpr(), 1, Set.of());
                String resultType = (String)context.get("resultType");
                require((Artifact.scalar(dcm, resultType) || Artifact.listType(dcm, resultType))
                        && gate.assignable(result, resultType), EXPRESSIONS);
                require(checked.getTypeMapMap().keySet().equals(gate.ids)
                        && checked.getReferenceMapMap().keySet().equals(gate.references), EXPRESSIONS);
                totalNodes += gate.ids.size();
                budget(totalNodes <= 512);
                var program = model.runtime.createProgram(CelProtoAbstractSyntaxTree.fromCheckedExpr(checked).getAst());
                prepared.put(item.getKey(), new Prepared(program, Map.copyOf(gate.usedVariables), resultType));
            }
            return new Bundle(dcm, model.descriptor, prepared, totalNodes);
        } catch (RuntimeFault failure) { throw failure; }
        catch (Exception failure) { throw new RuntimeFault("ARTIFACT_INVALID", EXPRESSIONS); }
    }

    private static DescriptorProtos.FileDescriptorSet expectedDescriptors(Map<String,Object> dcm) {
        var file = DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_read.proto")
                .setPackage("keir.read").setSyntax("proto3");
        boolean usesTimestamp = false;
        for (String name : new TreeSet<>(Artifact.types(dcm).keySet())) {
            if (!Artifact.record(dcm, name)) continue;
            var message = DescriptorProtos.DescriptorProto.newBuilder().setName(name);
            Map<String,Object> fields = map(Artifact.definition(dcm, name).get("fields"), "/artifact/descriptors");
            var usedNames = new HashSet<>(fields.keySet());
            int number = 1;
            for (String field : new TreeSet<>(fields.keySet())) {
                String type = Artifact.fieldType(fields.get(field));
                boolean repeated = Artifact.listType(dcm, type);
                var descriptor = DescriptorProtos.FieldDescriptorProto.newBuilder().setName(field).setJsonName(field)
                        .setNumber(number++).setLabel(repeated ? DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
                                : DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
                if (type.equals("Timestamp")) {
                    usesTimestamp = true;
                    descriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                            .setTypeName(".google.protobuf.Timestamp");
                } else {
                    String scalar = repeated ? (String)Artifact.definition(dcm, type).get("element") : type;
                    descriptor.setType(scalar.equals("Boolean") ? DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
                            : DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);
                    if (Artifact.optionalField(fields.get(field))) {
                        String oneof = "_" + field;
                        while (!usedNames.add(oneof)) oneof = "_" + oneof;
                        descriptor.setProto3Optional(true).setOneofIndex(message.getOneofDeclCount());
                        message.addOneofDecl(DescriptorProtos.OneofDescriptorProto.newBuilder().setName(oneof));
                    }
                }
                message.addField(descriptor);
            }
            file.addMessageType(message);
        }
        var set = DescriptorProtos.FileDescriptorSet.newBuilder();
        if (usesTimestamp || map(dcm.get("capabilities"), EXPRESSIONS).values().stream()
                .flatMap(cap -> map(map(cap, EXPRESSIONS).get("parameters"), EXPRESSIONS).values().stream())
                .anyMatch(param -> "Timestamp".equals(map(param, EXPRESSIONS).get("type")))) {
            var timestamp = Timestamp.getDescriptor().getFile();
            file.addDependency(timestamp.getName());
            set.addFile(timestamp.toProto());
        }
        return set.addFile(file).build();
    }

    private static final class Gate {
        private record Presence(String variable, String field) {}
        private final Map<String,Object> dcm, variables;
        private final Map<String,Object> usedVariables = new HashMap<>();
        private final CheckedExpr checked;
        private final Set<Long> ids = new HashSet<>(), references = new HashSet<>();

        Gate(Map<String,Object> dcm, CheckedExpr checked, Map<String,Object> variables) {
            this.dcm = dcm; this.checked = checked; this.variables = variables;
        }

        Value visit(Expr expression, int depth, Set<Presence> proofs) {
            budget(depth <= 16 && ids.size() < 64);
            long id = expression.getId();
            require(id > 0 && id <= MAX_ID && ids.add(id), EXPRESSIONS);
            Value value;
            if (expression.hasConstExpr()) {
                var constant = expression.getConstExpr();
                if (constant.hasBoolValue()) value = new Value("Boolean");
                else {
                    require(constant.hasStringValue(), EXPRESSIONS);
                    String literal = (String)Artifact.value(dcm, "String", constant.getStringValue(), EXPRESSIONS, "ARTIFACT_INVALID");
                    value = new Value("String", List.of(literal));
                }
            } else if (expression.hasIdentExpr()) {
                String name = expression.getIdentExpr().getName();
                require(variables.containsKey(name), EXPRESSIONS);
                var reference = checked.getReferenceMapMap().get(id);
                require(reference != null && reference.getName().equals(name) && !reference.hasValue()
                        && reference.getOverloadIdCount() == 0, EXPRESSIONS);
                references.add(id);
                usedVariables.put(name, variables.get(name));
                value = new Value((String)variables.get(name));
            } else if (expression.hasSelectExpr()) {
                var select = expression.getSelectExpr();
                require(select.hasOperand(), EXPRESSIONS);
                Value operand = visit(select.getOperand(), depth + 1, proofs);
                require(Artifact.record(dcm, operand.type()), EXPRESSIONS);
                Map<String,Object> fields = map(Artifact.definition(dcm, operand.type()).get("fields"), EXPRESSIONS);
                require(fields.containsKey(select.getField()), EXPRESSIONS);
                Object field = fields.get(select.getField());
                boolean optional = Artifact.optionalField(field);
                Presence presence = presence(select);
                if (select.getTestOnly()) {
                    require(optional && presence != null, EXPRESSIONS);
                    value = new Value("Boolean");
                } else {
                    require(!optional || presence != null && proofs.contains(presence), EXPRESSIONS);
                    value = new Value(Artifact.fieldType(field));
                }
            } else if (expression.hasCallExpr()) {
                var call = expression.getCallExpr();
                String function = call.getFunction();
                boolean addition = function.equals("_+_");
                boolean constructor = function.equals("timestamp") || function.equals("duration");
                int arity = constructor || function.equals("!_") ? 1 : function.equals("_?_:_") ? 3 : 2;
                require((addition || OVERLOADS.containsKey(function)) && !call.hasTarget()
                        && call.getArgsCount() == arity, EXPRESSIONS);
                List<Value> args = new ArrayList<>();
                for (int i = 0; i < arity; i++) args.add(visit(call.getArgs(i), depth + 1, branchProofs(call, i, proofs)));
                boolean temporalAdd = addition && pair(args, "Timestamp", "Duration");
                String overload = addition ? (temporalAdd ? "add_timestamp_duration" : "add_string") : OVERLOADS.get(function);
                var reference = checked.getReferenceMapMap().get(id);
                require(reference != null && reference.getName().isEmpty() && !reference.hasValue()
                        && reference.getOverloadIdList().equals(List.of(overload)), EXPRESSIONS);
                references.add(id);
                if (constructor) {
                    Expr argument = call.getArgs(0);
                    require(argument.hasConstExpr() && argument.getConstExpr().hasStringValue(), EXPRESSIONS);
                    String literal = argument.getConstExpr().getStringValue();
                    if (function.equals("timestamp")) {
                        Artifact.timestamp(literal);
                        value = new Value("Timestamp");
                    } else {
                        require(literal.matches("(?:0|[1-9][0-9]{0,7})s")
                                && Integer.parseInt(literal.substring(0, literal.length() - 1)) <= 31_536_000, EXPRESSIONS);
                        value = new Value("Duration");
                    }
                } else if (addition) {
                    require(temporalAdd || pair(args, "String", "String"), EXPRESSIONS);
                    value = new Value(temporalAdd ? "Timestamp" : "String");
                } else if (function.equals("@in")) {
                    String collection = args.get(1).type();
                    require(Artifact.listType(dcm, collection), EXPRESSIONS);
                    String element = (String)Artifact.definition(dcm, collection).get("element");
                    require(membershipNeedle(args.get(0), call.getArgs(0), element), EXPRESSIONS);
                    value = new Value("Boolean");
                } else if (function.equals("_<_")) {
                    require(pair(args, "Timestamp", "Timestamp"), EXPRESSIONS);
                    value = new Value("Boolean");
                } else if (function.equals("_==_") || function.equals("_!=_")) {
                    Value left = args.get(0), right = args.get(1);
                    require(Artifact.scalar(dcm, left.type()) && Artifact.scalar(dcm, right.type()), EXPRESSIONS);
                    require(!function.equals("_!=_") || !left.type().equals("Timestamp") && !right.type().equals("Timestamp"), EXPRESSIONS);
                    require(left.type().equals(right.type()) || isEnum(left.type()) && right.literals() != null && assignable(right, left.type())
                            || isEnum(right.type()) && left.literals() != null && assignable(left, right.type()), EXPRESSIONS);
                    value = new Value("Boolean");
                } else if (function.equals("_?_:_")) {
                    require(args.get(0).type().equals("Boolean"), EXPRESSIONS);
                    value = branches(args.get(1), args.get(2));
                } else {
                    require(args.stream().allMatch(arg -> arg.type().equals("Boolean")), EXPRESSIONS);
                    value = new Value("Boolean");
                }
            } else throw new RuntimeFault("ARTIFACT_INVALID", EXPRESSIONS);
            require(wireType(value.type()).equals(checked.getTypeMapMap().get(id)), EXPRESSIONS);
            return value;
        }

        private Type wireType(String name) {
            var type = Type.newBuilder();
            if (name.equals("Timestamp")) return type.setWellKnown(Type.WellKnownType.TIMESTAMP).build();
            if (name.equals("Duration")) return type.setWellKnown(Type.WellKnownType.DURATION).build();
            if (Artifact.record(dcm, name)) return type.setMessageType("keir.read." + name).build();
            if (Artifact.listType(dcm, name)) return type.setListType(Type.ListType.newBuilder()
                    .setElemType(wireType((String)Artifact.definition(dcm, name).get("element")))).build();
            return type.setPrimitive(name.equals("Boolean") ? Type.PrimitiveType.BOOL : Type.PrimitiveType.STRING).build();
        }

        private static boolean pair(List<Value> args, String left, String right) {
            return args.get(0).type().equals(left) && args.get(1).type().equals(right);
        }

        private static Presence presence(Expr.Select select) {
            return select.getOperand().hasIdentExpr()
                    ? new Presence(select.getOperand().getIdentExpr().getName(), select.getField()) : null;
        }

        private static Set<Presence> branchProofs(Expr.Call call, int index, Set<Presence> incoming) {
            if (index == 0 || !call.getFunction().equals("_&&_") && !call.getFunction().equals("_?_:_")) return incoming;
            Expr condition = call.getArgs(0);
            if (index == 2) {
                if (!condition.hasCallExpr() || !condition.getCallExpr().getFunction().equals("!_")
                        || condition.getCallExpr().getArgsCount() != 1) return incoming;
                condition = condition.getCallExpr().getArgs(0);
            }
            if (!condition.hasSelectExpr() || !condition.getSelectExpr().getTestOnly()) return incoming;
            Presence proof = presence(condition.getSelectExpr());
            if (proof == null || incoming.contains(proof)) return incoming;
            var extended = new HashSet<>(incoming);
            extended.add(proof);
            return extended;
        }

        private boolean isEnum(String type) { return "enum".equals(Artifact.definition(dcm, type).get("kind")); }
        private boolean membershipNeedle(Value needle, Expr source, String element) {
            if (needle.type().equals(element)) return true;
            return isEnum(element) && source.hasConstExpr() && source.getConstExpr().hasStringValue()
                    && list(Artifact.definition(dcm, element).get("values"), EXPRESSIONS)
                    .contains(source.getConstExpr().getStringValue());
        }
        private boolean assignable(Value value, String type) {
            return value.type().equals(type) || value.literals() != null && (type.equals("String")
                    || isEnum(type) && list(Artifact.definition(dcm, type).get("values"), EXPRESSIONS).containsAll(value.literals()));
        }
        private Value branches(Value left, Value right) {
            require(Artifact.scalar(dcm, left.type()) && Artifact.scalar(dcm, right.type()), EXPRESSIONS);
            if (left.literals() != null && right.literals() != null) {
                var alternatives = new ArrayList<>(left.literals()); alternatives.addAll(right.literals());
                return new Value("String", alternatives);
            }
            if (left.type().equals(right.type())) return new Value(left.type());
            if (assignable(left, right.type())) return new Value(right.type());
            if (assignable(right, left.type())) return new Value(left.type());
            throw new RuntimeFault("ARTIFACT_INVALID", EXPRESSIONS);
        }
    }

    private static byte[] base64(Object value, int max, String path) {
        require(value instanceof String, path);
        String text = (String)value;
        require(!text.isEmpty() && text.length() % 4 == 0 && text.length() <= ((max + 2) / 3) * 4, path);
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(text); }
        catch (IllegalArgumentException failure) { throw new RuntimeFault("ARTIFACT_INVALID", path); }
        require(bytes.length <= max && Base64.getEncoder().encodeToString(bytes).equals(text), path);
        return bytes;
    }

    private static <T extends Message> T decode(Parser<T> parser, Descriptors.Descriptor schema, byte[] bytes, String path) {
        try {
            inspectWire(CodedInputStream.newInstance(bytes), schema, 1, path);
            var input = CodedInputStream.newInstance(bytes);
            input.setRecursionLimit(36); input.setSizeLimit(bytes.length);
            T message = parser.parseFrom(input);
            require(input.isAtEnd(), path);
            noUnknownFields(message, path);
            require(Arrays.equals(bytes, deterministic(message)), path);
            return message;
        } catch (IOException failure) { throw new RuntimeFault("ARTIFACT_INVALID", path); }
    }

    private static void inspectWire(CodedInputStream input, Descriptors.Descriptor schema, int depth, String path) throws IOException {
        budget(depth <= 36);
        while (!input.isAtEnd()) {
            int tag = input.readTag(), wire = WireFormat.getTagWireType(tag);
            var field = schema.findFieldByNumber(WireFormat.getTagFieldNumber(tag));
            require(field != null && (wire == 0 || wire == 1 || wire == 2 || wire == 5), path);
            if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                require(wire == 2, path);
                int limit = input.pushLimit(input.readRawVarint32());
                inspectWire(input, field.getMessageType(), depth + 1, path);
                require(input.getBytesUntilLimit() == 0, path);
                input.popLimit(limit);
            } else {
                require(wire == field.getLiteType().getWireType() || field.isRepeated() && field.isPackable() && wire == 2, path);
                require(input.skipField(tag), path);
            }
        }
    }

    private static void noUnknownFields(Message message, String path) {
        require(message.getUnknownFields().asMap().isEmpty(), path);
        for (var field : message.getAllFields().entrySet()) {
            if (field.getKey().getJavaType() != Descriptors.FieldDescriptor.JavaType.MESSAGE) continue;
            if (field.getKey().isRepeated()) for (Object child : (List<?>)field.getValue()) noUnknownFields((Message)child, path);
            else noUnknownFields((Message)field.getValue(), path);
        }
    }

    private static byte[] deterministic(Message message) {
        byte[] bytes = new byte[message.getSerializedSize()];
        var output = CodedOutputStream.newInstance(bytes); output.useDeterministicSerialization();
        try { message.writeTo(output); output.checkNoSpaceLeft(); return bytes; }
        catch (IOException failure) { throw new RuntimeFault("ARTIFACT_INVALID", EXPRESSIONS); }
    }
    private static void require(boolean condition, String path) { if (!condition) throw new RuntimeFault("ARTIFACT_INVALID", path); }
    private static void budget(boolean condition) { if (!condition) throw new RuntimeFault("BUDGET_EXCEEDED", EXPRESSIONS); }
}
