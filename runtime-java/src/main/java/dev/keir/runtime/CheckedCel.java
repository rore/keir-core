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
import dev.cel.runtime.standard.DivideOperator;
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
    private static final String DECIMAL = "keir.numeric.Decimal";
    private static final Map<String,Integer> NUMERIC_ARITIES = Map.ofEntries(
            Map.entry("decimal",1), Map.entry("toDecimal",1), Map.entry("toInteger",1), Map.entry("size",1),
            Map.entry("-_",1), Map.entry("_+_",2), Map.entry("_-_",2), Map.entry("_*_",2), Map.entry("_/_",2),
            Map.entry("_<_",2), Map.entry("_<=_",2), Map.entry("_>_",2), Map.entry("_>=_",2),
            Map.entry("round",3), Map.entry("multiplyRounded",4), Map.entry("divideRounded",4));
    private static final Map<String,String> NUMERIC_OPERATORS = Map.of("_+_","add", "_-_","subtract", "_*_","multiply",
            "_/_","divide", "-_","negate", "_<_","less", "_<=_","less_equals", "_>_","greater", "_>=_","greater_equals");
    private static final Set<String> ROUNDING = Set.of("UNNECESSARY","DOWN","FLOOR","CEILING","HALF_UP","HALF_EVEN");
    private static final long MAX_ID = 9_007_199_254_740_991L;
    private static final Map<String,String> OVERLOADS = Map.of("_==_", "equals", "_!=_", "not_equals",
            "!_", "logical_not", "_&&_", "logical_and", "_||_", "logical_or", "_?_:_", "conditional",
            "@in", "in_list", "_<_", "less_timestamp", "timestamp", "string_to_timestamp", "duration", "string_to_duration");
    private static final Instant MIN_TIMESTAMP = Instant.parse("0001-01-01T00:00:00.000Z");
    private static final Instant MAX_TIMESTAMP = Instant.parse("9999-12-31T23:59:59.999Z");
    private static final DateTimeFormatter UTC_MILLIS = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    private CheckedCel() {}
    private record Value(String type, List<String> literals) { Value(String type) { this(type, null); } }
    private record Prepared(CelRuntime.Program program, Map<String,Object> variables, String resultType,
                            Map<String,Object> guardedResults) {}
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
                    overload != AddOperator.AddOverload.ADD_TIMESTAMP_DURATION
                            && (!Artifact.numeric(dcm) || overload != DivideOperator.DivideOverload.DIVIDE_INT64)).build();
            var checkedAdd = CelFunctionBinding.from("add_timestamp_duration", Instant.class, Duration.class,
                    (instant, duration) -> {
                        Instant result = instant.plus(duration);
                        if (!timestampInRange(result)) throw new TimestampOverflow();
                        return result;
                    });
            var builder = CelRuntimeFactory.plannerRuntimeBuilder()
                    .setOptions(CelOptions.current().enableHeterogeneousNumericComparisons(true)
                            .comprehensionMaxIterations(0).build()).addFileTypes(descriptor)
                    .setStandardFunctions(standard).addFunctionBindings(checkedAdd);
            if (Artifact.numeric(dcm)) builder.addFileTypes(descriptor.getDependencies())
                    .addFunctionBindings(numericBindings(decimalDescriptor(descriptor)));
            this.runtime = builder.build();
        }

        int descriptorBytes() { return descriptorBytes; }
    }

    static Model bindModel(Map<String,Object> dcm) {
        var descriptors = expectedDescriptors(dcm);
        int size = descriptors.getSerializedSize();
        if (size > 65_536) throw new RuntimeFault("BUDGET_EXCEEDED","/binding/dcm/types");
        try {
            int recordFile = descriptors.getFileCount() - 1;
            var dependencies = new ArrayList<Descriptors.FileDescriptor>();
            for (int i=0;i<recordFile;i++) {
                var file=descriptors.getFile(i);
                dependencies.add(file.getName().equals("google/protobuf/timestamp.proto") ? Timestamp.getDescriptor().getFile()
                        : Descriptors.FileDescriptor.buildFrom(file,new Descriptors.FileDescriptor[0]));
            }
            return new Model(dcm,Descriptors.FileDescriptor.buildFrom(descriptors.getFile(recordFile),dependencies.toArray(Descriptors.FileDescriptor[]::new)),size);
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
        Activation activation() { return new Activation(dcm,descriptor); }

        Object evaluate(Map<String,Object> binding, Map<String,Object> state, String path) {
            return evaluate(binding,state,activation(),null,path);
        }
        Object evaluate(Map<String,Object> binding,Map<String,Object> state,Activation activation,String retainedId,String path) {
            if (binding.containsKey("literal")) return retainedId == null
                    ? Artifact.value(dcm,(String)binding.get("type"),binding.get("literal"),path,"EXPRESSION_FAILED")
                    : activation.accept(retainedId,(String)binding.get("type"),binding.get("literal"),path,"EXPRESSION_FAILED");
            Prepared expression = expressions.get(binding.get("expr"));
            if (expression == null) throw new RuntimeFault("EXPRESSION_FAILED", path);
            Map<String,Object> variables = new HashMap<>();
            for (var variable : expression.variables().entrySet()) {
                if (expression.guardedResults() != null) {
                    Object guardId = expression.guardedResults().get(variable.getKey());
                    if (guardId != null) {
                        Object guard = state.get(guardId);
                        if (!(guard instanceof Boolean)) throw new RuntimeFault("EXPRESSION_FAILED", path);
                        if (Boolean.FALSE.equals(guard)) continue;
                    }
                    if (!state.containsKey(variable.getKey())) throw new RuntimeFault("EXPRESSION_FAILED", path);
                }
                String type = (String)variable.getValue();
                variables.put(variable.getKey(),activation.get(variable.getKey(),type,state.get(variable.getKey()),path));
            }
            Object result;
            try { result = expression.program().eval(variables); }
            catch (Exception failure) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause())
                    if (cause instanceof TimestampOverflow) throw new RuntimeFault("BUDGET_EXCEEDED", path);
                throw new RuntimeFault("EXPRESSION_FAILED", path);
            }
            Object engine=result;
            String type=expression.resultType();
            if (Artifact.numeric(dcm,type)) {
                try {
                    if (type.equals("Integer")) {
                        if (!(result instanceof Long)) throw new IllegalArgumentException();
                        result=Long.toString((Long)result);
                    } else result=Numeric.text(decimalValue(result));
                } catch (RuntimeException failure) { throw new RuntimeFault("EXPRESSION_FAILED",path); }
                if (retainedId != null) activation.retain(retainedId,type,result,engine);
                return result;
            }
            if (type.equals("Timestamp")) result = timestampResult(result, path);
            return retainedId == null ? Artifact.value(dcm,type,result,path,"EXPRESSION_FAILED")
                    : activation.accept(retainedId,type,result,path,"EXPRESSION_FAILED");
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
                Map<String,Object> guardedResults = context.containsKey("guardedResults")
                        ? map(context.get("guardedResults"), EXPRESSIONS) : null;
                var gate = new Gate(dcm, checked, variables, guardedResults == null ? Map.of() : guardedResults);
                Value result = gate.visit(checked.getExpr(), 1, Set.of(), Set.of(), false);
                String resultType = (String)context.get("resultType");
                require((Artifact.scalar(dcm, resultType) || Artifact.listType(dcm, resultType))
                        && gate.assignable(result, resultType), EXPRESSIONS);
                require(checked.getTypeMapMap().keySet().equals(gate.ids)
                        && checked.getReferenceMapMap().keySet().equals(gate.references), EXPRESSIONS);
                totalNodes += gate.ids.size();
                budget(totalNodes <= 512);
                var program = model.runtime.createProgram(CelProtoAbstractSyntaxTree.fromCheckedExpr(checked).getAst());
                prepared.put(item.getKey(), new Prepared(program,
                        Map.copyOf(guardedResults == null ? gate.usedVariables : variables), resultType,
                        guardedResults == null ? null : Map.copyOf(guardedResults)));
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
                } else if (Artifact.numeric(dcm,type) && type.equals("Decimal")) {
                    descriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".keir.numeric.Decimal");
                } else {
                    String scalar = repeated ? (String)Artifact.definition(dcm, type).get("element") : type;
                    descriptor.setType(Artifact.numeric(dcm,scalar) && scalar.equals("Integer") ? DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64
                            : scalar.equals("Boolean") ? DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
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
        if (Artifact.numeric(dcm)) {
            var numeric=DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_numeric.proto")
                    .setPackage("keir.numeric").setSyntax("proto3")
                    .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Decimal")
                            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder().setName("value").setJsonName("value")
                                    .setNumber(1).setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                                    .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)));
            set.addFile(numeric); file.addDependency("keir_numeric.proto");
        }
        return set.addFile(file).build();
    }

    /** Per-invocation slots only. Guard checks occur before lookup; prepared plans own no values. */
    static final class Activation {
        private final Map<String,Object> dcm;
        private final Descriptors.FileDescriptor descriptor;
        private final Map<String,Slot> slots=new HashMap<>();
        private int projections, boundaryParses;
        private static final class Slot {
            final String type,path;
            final Object canonical;
            final Map<String,Object> numbers;
            Object engine;
            Slot(String type,Object canonical,String path,Map<String,Object> numbers,Object engine) {
                this.type=type; this.canonical=canonical; this.path=path; this.numbers=numbers; this.engine=engine;
            }
        }
        private Activation(Map<String,Object> dcm,Descriptors.FileDescriptor descriptor) { this.dcm=dcm; this.descriptor=descriptor; }
        Object accept(String id,String type,Object raw,String path,String code) {
            var numbers=new HashMap<String,Object>();
            Object canonical=Artifact.value(dcm,type,raw,path,code,numbers);
            boundaryParses+=numbers.size();
            slots.put(id,new Slot(type,canonical,path,Map.copyOf(numbers),null));
            return canonical;
        }
        private void retain(String id,String type,Object canonical,Object engine) {
            slots.put(id,new Slot(type,canonical,"",Map.of(),engine));
        }
        Object get(String id,String type,Object canonical,String path) {
            if (!slots.containsKey(id)) accept(id,type,canonical,path,"EXPRESSION_FAILED");
            Slot slot=slots.get(id);
            if (!slot.type.equals(type)) throw new RuntimeFault("EXPRESSION_FAILED",path);
            if (slot.engine==null) { slot.engine=project(type,slot.canonical,slot.path,slot.numbers); projections++; }
            return slot.engine;
        }
        int projections() { return projections; }
        int boundaryParses() { return boundaryParses; }
        private Object project(String type,Object canonical,String path,Map<String,Object> numbers) {
            if (Artifact.numeric(dcm,type)) return type.equals("Integer") ? numbers.get(path)
                    : decimalMessage((Numeric.Decimal)numbers.get(path),decimalDescriptor(descriptor));
            if (type.equals("Timestamp")) return Artifact.timestamp((String)canonical);
            if (!Artifact.record(dcm,type)) return canonical;
            var schema=descriptor.findMessageTypeByName(type);
            var message=DynamicMessage.newBuilder(schema);
            var fields=map(Artifact.definition(dcm,type).get("fields"),path);
            for (var field:map(canonical,path).entrySet()) {
                String fieldType=Artifact.fieldType(fields.get(field.getKey()));
                Object value=project(fieldType,field.getValue(),pointer(path,field.getKey()),numbers);
                if (fieldType.equals("Timestamp")) {
                    Instant instant=(Instant)value;
                    value=Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
                }
                message.setField(schema.findFieldByName(field.getKey()),value);
            }
            return message.build();
        }
    }

    private static Descriptors.Descriptor decimalDescriptor(Descriptors.FileDescriptor records) {
        return records.getDependencies().stream().filter(file -> file.getName().equals("keir_numeric.proto"))
                .findFirst().orElseThrow().findMessageTypeByName("Decimal");
    }
    private static Message decimalMessage(Numeric.Decimal value,Descriptors.Descriptor descriptor) {
        return DynamicMessage.newBuilder(descriptor).setField(descriptor.findFieldByName("value"),Numeric.text(value)).build();
    }
    private static Numeric.Decimal decimalValue(Object raw) {
        if (!(raw instanceof Message message) || !message.getDescriptorForType().getFullName().equals(DECIMAL)
                || !message.getUnknownFields().asMap().isEmpty()) throw new IllegalArgumentException("Invalid Decimal engine value.");
        return Numeric.decimal(message.getField(message.getDescriptorForType().findFieldByName("value")));
    }
    private static List<CelFunctionBinding> numericBindings(Descriptors.Descriptor descriptor) {
        var bindings=new ArrayList<CelFunctionBinding>();
        bindings.add(CelFunctionBinding.from("divide_int64",Long.class,Long.class,Numeric::divide));
        bindings.add(CelFunctionBinding.from("keir_decimal_from_string",String.class,a -> decimalMessage(Numeric.decimal(a),descriptor)));
        bindings.add(CelFunctionBinding.from("keir_decimal_from_integer",Long.class,a -> decimalMessage(Numeric.fromInteger(a),descriptor)));
        bindings.add(CelFunctionBinding.from("keir_integer_from_decimal",Message.class,a -> Numeric.toInteger(decimalValue(a))));
        for (String operation:List.of("add","subtract","multiply","divide","negate","less","less_equal","greater","greater_equal")) {
            List<Class<?>> args=operation.equals("negate") ? List.of(Message.class) : List.of(Message.class,Message.class);
            bindings.add(CelFunctionBinding.from("keir_decimal_"+operation,args,values -> {
                var a=decimalValue(values[0]);
                if (operation.equals("negate")) return decimalMessage(Numeric.negate(a),descriptor);
                var b=decimalValue(values[1]);
                return switch (operation) {
                    case "less" -> Numeric.compare(a,b)<0;
                    case "less_equal" -> Numeric.compare(a,b)<=0;
                    case "greater" -> Numeric.compare(a,b)>0;
                    case "greater_equal" -> Numeric.compare(a,b)>=0;
                    default -> decimalMessage(switch (operation) {
                        case "add" -> Numeric.add(a,b);
                        case "subtract" -> Numeric.subtract(a,b);
                        case "multiply" -> Numeric.multiply(a,b);
                        default -> Numeric.divide(a,b);
                    },descriptor);
                };
            }));
        }
        bindings.add(CelFunctionBinding.from("keir_decimal_round",List.of(Message.class,Long.class,String.class),args ->
                decimalMessage(Numeric.round(decimalValue(args[0]),Math.toIntExact((Long)args[1]),(String)args[2]),descriptor)));
        for (boolean multiply:List.of(true,false)) bindings.add(CelFunctionBinding.from(
                multiply ? "keir_decimal_multiply_rounded" : "keir_decimal_divide_rounded",
                List.of(Message.class,Message.class,Long.class,String.class),args -> {
                    var a=decimalValue(args[0]); var b=decimalValue(args[1]); int scale=Math.toIntExact((Long)args[2]);
                    return decimalMessage(multiply ? Numeric.multiplyRounded(a,b,scale,(String)args[3])
                            : Numeric.divideRounded(a,b,scale,(String)args[3]),descriptor);
                }));
        return bindings;
    }

    private static final class Gate {
        private record Presence(String variable, String field) {}
        private final Map<String,Object> dcm, variables, guardedResults;
        private final Map<String,Object> usedVariables = new HashMap<>();
        private final CheckedExpr checked;
        private final Set<Long> ids = new HashSet<>(), references = new HashSet<>();

        Gate(Map<String,Object> dcm, CheckedExpr checked, Map<String,Object> variables, Map<String,Object> guardedResults) {
            this.dcm = dcm; this.checked = checked; this.variables = variables; this.guardedResults = guardedResults;
        }

        Value visit(Expr expression, int depth, Set<Presence> proofs, Set<String> guards, boolean selectionOperand) {
            budget(depth <= 16 && ids.size() < 64);
            long id = expression.getId();
            require(id > 0 && id <= MAX_ID && ids.add(id), EXPRESSIONS);
            Value value;
            if (expression.hasConstExpr()) {
                var constant = expression.getConstExpr();
                if (constant.hasBoolValue()) value = new Value("Boolean");
                else if (Artifact.numeric(dcm) && constant.hasInt64Value()) value = new Value("Integer");
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
                if (guardedResults.containsKey(name))
                    require(selectionOperand && guards.contains(guardedResults.get(name)), EXPRESSIONS);
                usedVariables.put(name, variables.get(name));
                value = new Value((String)variables.get(name));
            } else if (expression.hasSelectExpr()) {
                var select = expression.getSelectExpr();
                require(select.hasOperand(), EXPRESSIONS);
                Value operand = visit(select.getOperand(), depth + 1, proofs, guards, true);
                require(Artifact.record(dcm, operand.type()), EXPRESSIONS);
                Map<String,Object> fields = map(Artifact.definition(dcm, operand.type()).get("fields"), EXPRESSIONS);
                require(fields.containsKey(select.getField()), EXPRESSIONS);
                Object field = fields.get(select.getField());
                if (select.getOperand().hasIdentExpr() && guardedResults.containsKey(select.getOperand().getIdentExpr().getName()))
                    require(Artifact.scalar(dcm, Artifact.fieldType(field)), EXPRESSIONS);
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
                int arity = Artifact.numeric(dcm) && NUMERIC_ARITIES.containsKey(function) ? NUMERIC_ARITIES.get(function)
                        : constructor || function.equals("!_") ? 1 : function.equals("_?_:_") ? 3 : 2;
                require((addition || OVERLOADS.containsKey(function) || Artifact.numeric(dcm) && NUMERIC_ARITIES.containsKey(function)) && !call.hasTarget()
                        && call.getArgsCount() == arity, EXPRESSIONS);
                List<Value> args = new ArrayList<>();
                for (int i = 0; i < arity; i++) args.add(visit(call.getArgs(i), depth + 1,
                        branchProofs(call, i, proofs), branchGuards(call, i, guards), false));
                boolean temporalAdd = addition && pair(args, "Timestamp", "Duration");
                Operation numeric=Artifact.numeric(dcm) ? numericCall(call,args) : null;
                String overload = numeric != null ? numeric.overload : addition ? (temporalAdd ? "add_timestamp_duration" : "add_string") : OVERLOADS.get(function);
                var reference = checked.getReferenceMapMap().get(id);
                require(reference != null && reference.getName().isEmpty() && !reference.hasValue()
                        && reference.getOverloadIdList().equals(List.of(overload)), EXPRESSIONS);
                references.add(id);
                if (numeric != null) value=new Value(numeric.type);
                else if (constructor) {
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
            if (Artifact.numeric(dcm,name)) return name.equals("Integer") ? type.setPrimitive(Type.PrimitiveType.INT64).build()
                    : type.setMessageType(DECIMAL).build();
            if (name.equals("Timestamp")) return type.setWellKnown(Type.WellKnownType.TIMESTAMP).build();
            if (name.equals("Duration")) return type.setWellKnown(Type.WellKnownType.DURATION).build();
            if (Artifact.record(dcm, name)) return type.setMessageType("keir.read." + name).build();
            if (Artifact.listType(dcm, name)) return type.setListType(Type.ListType.newBuilder()
                    .setElemType(wireType((String)Artifact.definition(dcm, name).get("element")))).build();
            return type.setPrimitive(name.equals("Boolean") ? Type.PrimitiveType.BOOL : Type.PrimitiveType.STRING).build();
        }

        private record Operation(String type,String overload) {}
        private Operation numericCall(Expr.Call call,List<Value> args) {
            String f=call.getFunction();
            if (f.equals("decimal")) {
                var literal=call.getArgs(0).getConstExpr();
                require(literal.hasStringValue(),EXPRESSIONS); Numeric.decimal(literal.getStringValue());
                return new Operation("Decimal","keir_decimal_from_string");
            }
            if (f.equals("toDecimal") || f.equals("toInteger")) {
                require(args.get(0).type.equals(f.equals("toDecimal") ? "Integer" : "Decimal"),EXPRESSIONS);
                return new Operation(f.equals("toDecimal") ? "Decimal" : "Integer",
                        f.equals("toDecimal") ? "keir_decimal_from_integer" : "keir_integer_from_decimal");
            }
            if (f.equals("size")) {
                require(Artifact.listType(dcm,args.get(0).type),EXPRESSIONS);
                return new Operation("Integer","size_list");
            }
            if (Set.of("round","multiplyRounded","divideRounded").contains(f)) {
                int index=args.size()-2;
                var scale=call.getArgs(index).getConstExpr(); var mode=call.getArgs(index+1).getConstExpr();
                require(args.subList(0,index).stream().allMatch(a -> a.type.equals("Decimal"))
                        && scale.hasInt64Value() && scale.getInt64Value()>=0 && scale.getInt64Value()<=18
                        && mode.hasStringValue() && ROUNDING.contains(mode.getStringValue()),EXPRESSIONS);
                return new Operation("Decimal",Map.of("round","keir_decimal_round","multiplyRounded","keir_decimal_multiply_rounded",
                        "divideRounded","keir_decimal_divide_rounded").get(f));
            }
            if (!NUMERIC_OPERATORS.containsKey(f) || !args.stream().anyMatch(a -> Artifact.numeric(dcm,a.type))) return null;
            String type=args.get(0).type;
            require(Artifact.numeric(dcm,type) && args.stream().allMatch(a -> a.type.equals(type)),EXPRESSIONS);
            String operation=NUMERIC_OPERATORS.get(f);
            return new Operation(Set.of("less","less_equals","greater","greater_equals").contains(operation) ? "Boolean" : type,
                    type.equals("Integer") ? operation+"_int64" : "keir_decimal_"+operation.replace("_equals","_equal"));
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

        private Set<String> branchGuards(Expr.Call call, int index, Set<String> incoming) {
            if (index != 1 || !call.getFunction().equals("_?_:_") || !call.getArgs(0).hasIdentExpr()) return incoming;
            String guard = call.getArgs(0).getIdentExpr().getName();
            if (!guardedResults.containsValue(guard) || incoming.contains(guard)) return incoming;
            var extended = new HashSet<>(incoming);
            extended.add(guard);
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
