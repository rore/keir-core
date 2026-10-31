package dev.keir.runtime;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.WireFormat;
import dev.cel.common.CelOptions;
import dev.cel.common.CelProtoAbstractSyntaxTree;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Expr;
import dev.cel.expr.Type;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import java.io.IOException;
import java.util.*;
import static dev.keir.runtime.RuntimeData.*;

/** Admission checks remain independent of the CEL planner's willingness to execute bytes. */
final class CheckedCel {
    private static final String EXPRESSIONS = "/artifact/expressions";
    private static final long MAX_ID = 9_007_199_254_740_991L;
    private static final Map<String,String> OVERLOADS = Map.of("_==_", "equals", "_!=_", "not_equals",
            "!_", "logical_not", "_&&_", "logical_and", "_||_", "logical_or", "_?_:_", "conditional");

    private CheckedCel() {}
    private record Value(String type, List<String> literals) { Value(String type) { this(type, null); } }
    private record Prepared(CelRuntime.Program program, Map<String,Object> variables, String resultType) {}

    static final class Model {
        private final Map<String,Object> dcm;
        private final Descriptors.FileDescriptor descriptor;
        private final CelRuntime runtime;
        private final int descriptorBytes;

        private Model(Map<String,Object> dcm, Descriptors.FileDescriptor descriptor, int descriptorBytes) {
            this.dcm = map(immutable(dcm),"/binding/dcm");
            this.descriptor = descriptor; this.descriptorBytes = descriptorBytes;
            this.runtime = CelRuntimeFactory.plannerRuntimeBuilder()
                    .setOptions(CelOptions.current().enableHeterogeneousNumericComparisons(true)
                            .comprehensionMaxIterations(0).build()).addFileTypes(descriptor).build();
        }

        int descriptorBytes() { return descriptorBytes; }
    }

    static Model bindModel2(Map<String,Object> dcm) {
        var descriptors = expectedDescriptors(dcm);
        int size = descriptors.getSerializedSize();
        if (size > 65_536) throw new RuntimeFault("BUDGET_EXCEEDED","/binding/dcm/types");
        try {
            return new Model(dcm,Descriptors.FileDescriptor.buildFrom(descriptors.getFile(0),
                    new Descriptors.FileDescriptor[0]),size);
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
                    for (var field : map(value, path).entrySet())
                        record.setField(record.getDescriptorForType().findFieldByName(field.getKey()), field.getValue());
                    value = record.build();
                }
                variables.put(variable.getKey(), value);
            }
            Object result;
            try { result = expression.program().eval(variables); }
            catch (Exception failure) { throw new RuntimeFault("EXPRESSION_FAILED", path); }
            return Artifact.value(dcm, expression.resultType(), result, path, "EXPRESSION_FAILED");
        }
    }

    static Bundle prepare(Artifact.Plan plan) {
        if (Set.of("keir-read/2","keir-effect/2").contains(plan.document().get("profile")))
            return prepare2(plan,bindModel2(plan.dcm()));
        Map<String,Object> dcm = plan.dcm();
        byte[] descriptors = base64(plan.document().get("descriptors"), 65_536, "/artifact/descriptors");
        DescriptorProtos.FileDescriptorSet decoded = decode(DescriptorProtos.FileDescriptorSet.parser(),
                DescriptorProtos.FileDescriptorSet.getDescriptor(), descriptors, "/artifact/descriptors");
        require(Arrays.equals(descriptors, deterministic(expectedDescriptors(dcm))), "/artifact/descriptors");
        try {
            var file = Descriptors.FileDescriptor.buildFrom(decoded.getFile(0), new Descriptors.FileDescriptor[0]);
            return prepare2(plan,new Model(dcm,file,descriptors.length));
        } catch (RuntimeFault failure) { throw failure; }
        catch (Exception failure) { throw new RuntimeFault("ARTIFACT_INVALID", EXPRESSIONS); }
    }

    static Bundle prepare2(Artifact.Plan plan, Model model) {
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
                        && Set.of("<input>", "keir-read/1").contains(source.getLocation()), EXPRESSIONS);
                require(source.getLineOffsetsList().stream().allMatch(n -> n >= 0 && n <= 513)
                        && source.getPositionsMap().values().stream().allMatch(n -> n >= 0 && n <= 512), EXPRESSIONS);
                Map<String,Object> variables = map(context.get("variables"), EXPRESSIONS);
                var gate = new Gate(dcm, checked, variables);
                Value result = gate.visit(checked.getExpr(), 1);
                String resultType = (String)context.get("resultType");
                require(Artifact.scalar(dcm, resultType) && gate.assignable(result, resultType), EXPRESSIONS);
                require(checked.getTypeMapMap().keySet().equals(gate.ids)
                        && checked.getReferenceMapMap().keySet().equals(gate.references), EXPRESSIONS);
                totalNodes += gate.ids.size();
                budget(totalNodes <= 512);
                var program = model.runtime.createProgram(CelProtoAbstractSyntaxTree.fromCheckedExpr(checked).getAst());
                prepared.put(item.getKey(), new Prepared(program, variables, resultType));
            }
            return new Bundle(dcm, model.descriptor, prepared, totalNodes);
        } catch (RuntimeFault failure) { throw failure; }
        catch (Exception failure) { throw new RuntimeFault("ARTIFACT_INVALID", EXPRESSIONS); }
    }

    private static DescriptorProtos.FileDescriptorSet expectedDescriptors(Map<String,Object> dcm) {
        var file = DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_read.proto")
                .setPackage("keir.read").setSyntax("proto3");
        for (String name : new TreeSet<>(Artifact.types(dcm).keySet())) {
            if (!Artifact.record(dcm, name)) continue;
            var message = DescriptorProtos.DescriptorProto.newBuilder().setName(name);
            Map<String,Object> fields = map(Artifact.definition(dcm, name).get("fields"), "/artifact/descriptors");
            int number = 1;
            for (String field : new TreeSet<>(fields.keySet())) {
                message.addField(DescriptorProtos.FieldDescriptorProto.newBuilder().setName(field).setJsonName(field)
                        .setNumber(number++).setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                        .setType("Boolean".equals(fields.get(field)) ? DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
                                : DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING));
            }
            file.addMessageType(message);
        }
        return DescriptorProtos.FileDescriptorSet.newBuilder().addFile(file).build();
    }

    private static final class Gate {
        private final Map<String,Object> dcm, variables;
        private final CheckedExpr checked;
        private final Set<Long> ids = new HashSet<>(), references = new HashSet<>();

        Gate(Map<String,Object> dcm, CheckedExpr checked, Map<String,Object> variables) {
            this.dcm = dcm; this.checked = checked; this.variables = variables;
        }

        Value visit(Expr expression, int depth) {
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
                references.add(id); value = new Value((String)variables.get(name));
            } else if (expression.hasSelectExpr()) {
                var select = expression.getSelectExpr();
                require(!select.getTestOnly() && select.hasOperand(), EXPRESSIONS);
                Value operand = visit(select.getOperand(), depth + 1);
                require(Artifact.record(dcm, operand.type()), EXPRESSIONS);
                Map<String,Object> fields = map(Artifact.definition(dcm, operand.type()).get("fields"), EXPRESSIONS);
                require(fields.containsKey(select.getField()), EXPRESSIONS);
                value = new Value((String)fields.get(select.getField()));
            } else if (expression.hasCallExpr()) {
                var call = expression.getCallExpr();
                String function = call.getFunction(), overload = OVERLOADS.get(function);
                int arity = function.equals("!_") ? 1 : function.equals("_?_:_") ? 3 : 2;
                require(overload != null && !call.hasTarget() && call.getArgsCount() == arity, EXPRESSIONS);
                var reference = checked.getReferenceMapMap().get(id);
                require(reference != null && reference.getName().isEmpty() && !reference.hasValue()
                        && reference.getOverloadIdList().equals(List.of(overload)), EXPRESSIONS);
                references.add(id);
                List<Value> args = new ArrayList<>();
                for (Expr argument : call.getArgsList()) args.add(visit(argument, depth + 1));
                if (function.equals("_==_") || function.equals("_!=_")) {
                    Value left = args.get(0), right = args.get(1);
                    require(Artifact.scalar(dcm, left.type()) && Artifact.scalar(dcm, right.type()), EXPRESSIONS);
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
            Type type = checked.getTypeMapMap().get(id);
            require(type != null && (Artifact.record(dcm, value.type())
                    ? type.hasMessageType() && type.getMessageType().equals("keir.read." + value.type())
                    : type.hasPrimitive() && type.getPrimitive() == (value.type().equals("Boolean") ? Type.PrimitiveType.BOOL : Type.PrimitiveType.STRING)), EXPRESSIONS);
            return value;
        }

        private boolean isEnum(String type) { return "enum".equals(Artifact.definition(dcm, type).get("kind")); }
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
