package dev.keir.compiler;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Validates the selected closed model and builds its protobuf bridge. */
final class Dcm {
    record Type(String kind, String base, List<String> values, Map<String,String> fields,
                Set<String> optionalFields, String element, int maxItems) {
        Type(String kind, String base, List<String> values) {
            this(kind,base,values,Map.of(),Set.of(),null,0);
        }
        boolean record() { return kind.equals("record"); }
        boolean list() { return kind.equals("list"); }
        boolean scalar() { return !record() && !list(); }
    }
    record Parameter(String type, String binding, boolean optional) {}
    record Capability(String version, Map<String,Parameter> parameters, String result,
                      String effect, List<String> errors) {}
    record Entrypoint(String input, String output, String trigger, Map<String,Object> consumer,
                      Map<String,Integer> grants, int effectGrant) {}
    private static final Set<String> RESERVED = Set.of("as", "break", "const", "continue", "else",
            "false", "for", "function", "if", "import", "in", "let", "loop", "namespace",
            "null", "package", "return", "true", "var", "void", "while");
    private static final Set<String> BUILTIN_TYPES = Set.of("Boolean", "String", "Timestamp");
    private static final Set<String> BUILTIN_NAMES = Set.of("Boolean", "String", "Timestamp", "Duration", "bool", "bytes",
            "double", "dyn", "int", "list", "map", "null_type", "string", "type", "uint", "input");
    private final Map<String,Object> root;
    private final Map<String,Type> types;
    private final Map<String,Capability> capabilities;
    private final Map<String,Entrypoint> entrypoints;
    private final DescriptorProtos.FileDescriptorSet descriptors;
    private final Descriptors.FileDescriptor fileDescriptor;

    private Dcm(Map<String,Object> root, Map<String,Type> types,
                Map<String,Capability> capabilities, Map<String,Entrypoint> entrypoints) {
        this.root = freezeMap(root);
        this.types = immutable(types);
        this.capabilities = immutable(capabilities);
        this.entrypoints = immutable(entrypoints);
        var file = DescriptorProtos.FileDescriptorProto.newBuilder().setName("keir_read.proto")
                .setPackage("keir.read").setSyntax("proto3");
        var set = DescriptorProtos.FileDescriptorSet.newBuilder();
        boolean usesTimestamp = types.values().stream().anyMatch(type -> type.fields.containsValue("Timestamp"))
                || capabilities.values().stream().flatMap(cap -> cap.parameters.values().stream())
                        .anyMatch(param -> param.type.equals("Timestamp"));
        var dependencies = new Descriptors.FileDescriptor[usesTimestamp ? 1 : 0];
        if (usesTimestamp) {
            var timestamp = Timestamp.getDescriptor().getFile();
            dependencies[0] = timestamp;
            set.addFile(timestamp.toProto());
            file.addDependency(timestamp.getName());
        }
        this.types.forEach((name, type) -> {
            if (type.record()) {
                var message = DescriptorProtos.DescriptorProto.newBuilder().setName(name);
                var usedNames = new HashSet<>(type.fields.keySet());
                int number = 1;
                for (var field : type.fields.entrySet()) {
                    String fieldName = field.getKey();
                    String fieldType = field.getValue();
                    var definition = type(fieldType);
                    var descriptor = DescriptorProtos.FieldDescriptorProto.newBuilder()
                            .setName(fieldName).setJsonName(fieldName).setNumber(number++)
                            .setLabel(definition.list() ? DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
                                    : DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
                    if (fieldType.equals("Timestamp")) {
                        descriptor.setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName(".google.protobuf.Timestamp");
                    } else {
                        String scalarType = definition.list() ? definition.element : fieldType;
                        descriptor.setType(scalarType.equals("Boolean")
                                    ? DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
                                    : DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);
                        if (type.optionalFields.contains(fieldName)) {
                            String oneof = "_" + fieldName;
                            while (!usedNames.add(oneof)) oneof = "_" + oneof;
                            descriptor.setProto3Optional(true).setOneofIndex(message.getOneofDeclCount());
                            message.addOneofDecl(DescriptorProtos.OneofDescriptorProto.newBuilder().setName(oneof));
                        }
                    }
                    message.addField(descriptor);
                }
                file.addMessageType(message);
            }
        });
        var proto = file.build();
        descriptors = set.addFile(proto).build();
        try {
            fileDescriptor = Descriptors.FileDescriptor.buildFrom(proto, dependencies);
        } catch (Descriptors.DescriptorValidationException e) {
            throw invalid("/dcm/types");
        }
    }

    static Dcm parse(Object value) {
        var root = shape(value,"/dcm","format","domain","types","capabilities","entrypoints","hostKey");
        supported(root.get("format"),"keir-dcm/2","/dcm/format");
        var key = shape(root.get("hostKey"),"/dcm/hostKey","source","type");
        if (!"requestKey".equals(key.get("source"))) throw invalid("/dcm/hostKey/source");
        metadataText(key.get("type"),"/dcm/hostKey/type");
        var domain = shape(root.get("domain"),"/dcm/domain","id","version");
        text(domain.get("id"),"/dcm/domain/id");
        text(domain.get("version"),"/dcm/domain/version");
        var types = new TreeMap<String,Type>();
        for (var entry : boundedMap(root.get("types"),"/dcm/types",16).entrySet()) {
            String path = child("/dcm/types",entry.getKey());
            identifier(entry.getKey(),path,false);
            if (BUILTIN_NAMES.contains(entry.getKey())) throw invalid(path);
            var def = object(entry.getValue(),path);
            String kind = text(def.get("kind"),child(path,"kind"));
            Type type;
            switch (kind) {
                case "nominal" -> {
                    shape(def,path,"kind","base");
                    if (!"String".equals(def.get("base"))) throw invalid(child(path,"base"));
                    type = new Type(kind,"String",List.of());
                }
                case "enum" -> {
                    shape(def,path,"kind","values");
                    type = new Type(kind,"String",strings(def.get("values"),child(path,"values"),16,true,false));
                }
                case "list" -> {
                    shape(def,path,"kind","element","maxItems");
                    String element = reference(def.get("element"),child(path,"element"));
                    String countPath = child(path,"maxItems");
                    Object raw = def.get("maxItems");
                    if (!(raw instanceof Byte || raw instanceof Short || raw instanceof Integer
                            || raw instanceof Long || raw instanceof BigInteger)) throw invalid(countPath);
                    var count = new BigInteger(raw.toString());
                    if (count.signum() < 0 || count.compareTo(BigInteger.valueOf(16)) > 0) throw invalid(countPath);
                    type = new Type(kind,null,List.of(),Map.of(),Set.of(),element,count.intValue());
                }
                case "record" -> {
                    shape(def,path,"kind","fields");
                    var fields = boundedMap(def.get("fields"),child(path,"fields"),16);
                    if (fields.isEmpty()) throw invalid(child(path,"fields"));
                    var result = new TreeMap<String,String>();
                    var optional = new HashSet<String>();
                    fields.forEach((name,ref) -> {
                        String fp = child(child(path,"fields"),name);
                        identifier(name,fp,false);
                        if (ref instanceof Map<?,?>) {
                            var field = shape(ref,fp,"type","optional");
                            if (!Boolean.TRUE.equals(field.get("optional"))) throw invalid(child(fp,"optional"));
                            result.put(name,reference(field.get("type"),child(fp,"type")));
                            optional.add(name);
                        } else result.put(name,reference(ref,fp));
                    });
                    type = new Type(kind,null,List.of(),immutable(result),Set.copyOf(optional),null,0);
                }
                default -> throw unsupported(child(path,"kind"));
            }
            types.put(entry.getKey(),type);
        }
        for (var entry : types.entrySet()) {
            String path = child("/dcm/types",entry.getKey());
            var definition = entry.getValue();
            if (definition.list()) {
                String element = definition.element;
                if (element.equals("Timestamp") || !scalarType(types,element)) throw invalid(child(path,"element"));
            }
            for (var field : definition.fields.entrySet()) {
                String fp = child(child(path,"fields"),field.getKey());
                boolean optional = definition.optionalFields.contains(field.getKey());
                requireValueType(types,field.getValue(),optional ? child(fp,"type") : fp,optional);
            }
        }
        var capabilities = new TreeMap<String,Capability>();
        for (var entry : boundedMap(root.get("capabilities"),"/dcm/capabilities",8).entrySet()) {
            String path = child("/dcm/capabilities",entry.getKey());
            identifier(entry.getKey(),path,true);
            var cap = shape(entry.getValue(),path,"version","parameters","result","effect","errors");
            String version = text(cap.get("version"),child(path,"version"));
            String effect = text(cap.get("effect"),child(path,"effect"));
            if (!effect.equals("READ") && !effect.equals("WRITE")) throw unsupported(child(path,"effect"));
            String result = reference(cap.get("result"),child(path,"result"));
            requireRecord(types,result,child(path,"result"));
            var parameters = new TreeMap<String,Parameter>();
            for (var param : boundedMap(cap.get("parameters"),child(path,"parameters"),16).entrySet()) {
                String pp = child(child(path,"parameters"),param.getKey());
                identifier(param.getKey(),pp,false);
                var p = object(param.getValue(),pp);
                boolean optional = p.containsKey("optional");
                if (optional) shape(p,pp,"type","binding","optional");
                else shape(p,pp,"type","binding");
                String type = reference(p.get("type"),child(pp,"type"));
                requireValueType(types,type,child(pp,"type"),optional);
                String binding = text(p.get("binding"),child(pp,"binding"));
                if (!binding.equals("agent") && !binding.equals("host")) throw invalid(child(pp,"binding"));
                if (optional && (!Boolean.TRUE.equals(p.get("optional")) || binding.equals("host")))
                    throw invalid(child(pp,"optional"));
                parameters.put(param.getKey(),new Parameter(type,binding,optional));
            }
            var errors = strings(cap.get("errors"),child(path,"errors"),16,false,true);
            capabilities.put(entry.getKey(),new Capability(version,immutable(parameters),result,effect,errors));
        }
        var entries = new TreeMap<String,Entrypoint>();
        for (var entry : boundedMap(root.get("entrypoints"),"/dcm/entrypoints",4).entrySet()) {
            String path = child("/dcm/entrypoints",entry.getKey());
            identifier(entry.getKey(),path,true);
            var ep = entryShape(entry.getValue(),path);
            String input = reference(ep.get("input"),child(path,"input"));
            String output = reference(ep.get("output"),child(path,"output"));
            requireRecord(types,input,child(path,"input"));
            requireRecord(types,output,child(path,"output"));
            if (!"EVENT".equals(ep.get("trigger"))) throw invalid(child(path,"trigger"));
            if (!"AFTER_COMMIT".equals(ep.get("placement"))) throw invalid(child(path,"placement"));
            var consumer = shape(ep.get("consumer"),child(path,"consumer"),"id","version","kind");
            String consumerIdPath = child(child(path,"consumer"),"id");
            identifier(text(consumer.get("id"),consumerIdPath),consumerIdPath,true);
            text(consumer.get("version"),child(child(path,"consumer"),"version"));
            if (!"EVENT_REPORT".equals(consumer.get("kind"))) throw invalid(child(child(path,"consumer"),"kind"));
            String gp = child(path,"effectGrant");
            Object n = ep.getOrDefault("effectGrant", 1L);
            if (!(n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long || n instanceof BigInteger)) throw invalid(gp);
            var count = new BigInteger(n.toString());
            if (count.signum() < 0 || count.compareTo(BigInteger.ONE) > 0) throw invalid(gp);
            var event = shape(ep.get("event"),child(path,"event"),"idType","sourceType");
            for (String field : List.of("idType","sourceType")) {
                String type = metadataText(event.get(field),child(child(path,"event"),field));
                if (!types.containsKey(type) || !types.get(type).kind.equals("nominal")) throw invalid(child(child(path,"event"),field));
            }
            var grants = policy(ep, capabilities, path);
            entries.put(entry.getKey(),new Entrypoint(input,output,"EVENT",freezeMap(consumer),immutable(grants),count.intValue()));
        }
        if (entries.isEmpty()) throw invalid("/dcm/entrypoints");
        return new Dcm(root,types,capabilities,entries);
    }

    Map<String,Object> root() { return root; }
    Map<String,Type> types() { return types; }
    Map<String,Capability> capabilities() { return capabilities; }
    Map<String,Entrypoint> entrypoints() { return entrypoints; }
    Type type(String name) {
        return BUILTIN_TYPES.contains(name) ? new Type("builtin",name,List.of()) : types.get(name);
    }
    Type record(String name) { Type t = types.get(name); return t != null && t.record() ? t : null; }
    DescriptorProtos.FileDescriptorSet descriptors() { return descriptors; }
    Descriptors.FileDescriptor fileDescriptor() { return fileDescriptor; }
    byte[] descriptorBytes() {
        int size = descriptors.getSerializedSize();
        if (size > 65536) throw budget("/dcm/types");
        byte[] bytes = new byte[size];
        var out = CodedOutputStream.newInstance(bytes);
        out.useDeterministicSerialization();
        try { descriptors.writeTo(out); out.checkNoSpaceLeft(); }
        catch (IOException e) { throw invalid("/dcm/types"); }
        return bytes;
    }

    private static Map<String,Object> entryShape(Object value,String path) {
        var ep = object(value,path);
        var required = new TreeSet<>(List.of("input","output","trigger","consumer","placement","event"));
        var allowed = new TreeSet<>(required);
        allowed.add("capabilityPolicy");
        allowed.add("effectGrant");
        for (String key : ep.keySet()) if (!allowed.contains(key)) throw new CompileFailure("FIELD_UNKNOWN",child(path,key));
        for (String key : required) if (!ep.containsKey(key)) throw invalid(child(path,key));
        return ep;
    }

    private static Map<String,Integer> policy(Map<String,Object> ep,Map<String,Capability> capabilities,String path) {
        var result = new TreeMap<String,Integer>();
        capabilities.keySet().forEach(id -> result.put(id,4));
        if (!ep.containsKey("capabilityPolicy")) return result;
        String pp = child(path,"capabilityPolicy");
        var policy = object(ep.get("capabilityPolicy"),pp);
        for (String key : policy.keySet()) if (!Set.of("allow","deny","maxCalls").contains(key))
            throw invalid(child(pp,key));
        for (String key : List.of("allow","deny")) if (policy.containsKey(key)) {
            String lp = child(pp,key);
            var ids = strings(policy.get(key),lp,8,false,false);
            for (int i=0;i<ids.size();i++) if (!capabilities.containsKey(ids.get(i))) throw invalid(child(lp,Integer.toString(i)));
            for (String id : result.keySet()) if (key.equals("allow") ? !ids.contains(id) : ids.contains(id)) result.put(id,0);
        }
        if (policy.containsKey("maxCalls")) for (var quota : boundedMap(policy.get("maxCalls"),child(pp,"maxCalls"),8).entrySet()) {
            String qp = child(child(pp,"maxCalls"),quota.getKey());
            if (!capabilities.containsKey(quota.getKey())) throw invalid(qp);
            Object n = quota.getValue();
            if (!(n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long || n instanceof BigInteger)) throw invalid(qp);
            var count = new BigInteger(n.toString());
            if (count.signum()<0 || count.compareTo(BigInteger.valueOf(result.get(quota.getKey())))>0) throw invalid(qp);
            result.put(quota.getKey(),count.intValue());
        }
        return result;
    }

    private static void requireRecord(Map<String,Type> types,String name,String path) {
        Type t = types.get(name);
        if (t == null || !t.record()) throw invalid(path);
    }
    private static boolean scalarType(Map<String,Type> types,String name) {
        Type t = types.get(name);
        return BUILTIN_TYPES.contains(name) || t != null && t.scalar();
    }
    private static void requireValueType(Map<String,Type> types,String name,String path,boolean optional) {
        Type t = types.get(name);
        if (!scalarType(types,name) && (optional || t == null || !t.list())) throw invalid(path);
    }
    private static String reference(Object value,String path) {
        String ref = text(value,path);
        if (!ref.matches("[A-Za-z][A-Za-z0-9]{0,63}")) throw invalid(path);
        return ref;
    }
    private static String metadataText(Object value,String path) {
        try { return text(value,path); }
        catch (CompileFailure failure) { throw invalid(path); }
    }
    private static void identifier(String value,String path,boolean dotted) {
        if (!value.matches(dotted ? "[A-Za-z][A-Za-z0-9.]{0,99}" : "[A-Za-z][A-Za-z0-9]{0,63}") || RESERVED.contains(value)) throw invalid(path);
    }
    private static void supported(Object value,String expected,String path) {
        String actual = text(value,path);
        if (!expected.equals(actual)) throw unsupported(path);
    }
    private static List<String> strings(Object value,String path,int max,boolean nonempty,boolean errors) {
        if (!(value instanceof List<?> list)) throw invalid(path);
        if (list.size() > max) throw budget(path);
        if (nonempty && list.isEmpty()) throw invalid(path);
        var result = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (int i=0;i<list.size();i++) {
            String p = child(path,Integer.toString(i));
            String s = text(list.get(i),p);
            if (!seen.add(s) || errors && !s.matches("[A-Z][A-Z0-9_]{0,63}")) throw invalid(p);
            result.add(s);
        }
        return List.copyOf(result);
    }
    private static String text(Object value,String path) {
        if (!(value instanceof String s) || s.isEmpty()) throw invalid(path);
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i>=s.length() || !Character.isLowSurrogate(s.charAt(i))) throw invalid(path);
            } else if (Character.isLowSurrogate(c)) throw invalid(path);
        }
        if (s.getBytes(StandardCharsets.UTF_8).length>256) throw budget(path);
        return s;
    }
    private static Map<String,Object> boundedMap(Object value,String path,int max) {
        if (!(value instanceof Map<?,?> map)) throw invalid(path);
        if (map.size()>max) throw budget(path);
        return object(value,path);
    }
    private static Map<String,Object> shape(Object value,String path,String... keys) {
        var map = object(value,path);
        var allowed = Set.of(keys);
        for (String key : map.keySet()) if (!allowed.contains(key)) throw new CompileFailure("FIELD_UNKNOWN",child(path,key));
        for (String key : keys) if (!map.containsKey(key)) throw invalid(child(path,key));
        return map;
    }
    private static Map<String,Object> object(Object value,String path) {
        if (!(value instanceof Map<?,?> map)) throw invalid(path);
        var result = new TreeMap<String,Object>();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw invalid(path);
            result.put(key,entry.getValue());
        }
        return result;
    }
    private static String child(String path,String key) { return path+"/"+key.replace("~","~0").replace("/","~1"); }
    private static CompileFailure invalid(String path) { return new CompileFailure("DCM_INVALID",path); }
    private static CompileFailure budget(String path) { return new CompileFailure("BUDGET_EXCEEDED",path); }
    private static CompileFailure unsupported(String path) { return new CompileFailure("PROFILE_UNSUPPORTED",path); }
    private static <T> Map<String,T> immutable(Map<String,T> map) {
        return Collections.unmodifiableMap(new TreeMap<>(map));
    }
    private static Map<String,Object> freezeMap(Map<String,Object> map) {
        var copy = new TreeMap<String,Object>();
        map.forEach((k,v) -> copy.put(k,freeze(v)));
        return immutable(copy);
    }
    private static Object freeze(Object value) {
        if (value instanceof Map<?,?> map) {
            var copy = new TreeMap<String,Object>();
            map.forEach((k,v) -> copy.put((String) k,freeze(v)));
            return immutable(copy);
        }
        if (value instanceof List<?> list) return list.stream().map(Dcm::freeze).toList();
        return value;
    }
}
