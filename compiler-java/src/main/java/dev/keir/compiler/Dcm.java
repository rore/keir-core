package dev.keir.compiler;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Validates the closed read-1 model and builds its scalar protobuf bridge. */
final class Dcm {
    record Type(String kind, String base, List<String> values, Map<String,String> fields) {
        boolean scalar() { return !kind.equals("record"); }
    }
    record Parameter(String type, String binding) {}
    record Capability(String version, Map<String,Parameter> parameters, String result,
                      String effect, List<String> errors) {}
    record Entrypoint(String input, String output, String trigger, Map<String,Object> consumer,
                      Map<String,Integer> grants) {}
    private static final Set<String> RESERVED = Set.of("as", "break", "const", "continue", "else",
            "false", "for", "function", "if", "import", "in", "let", "loop", "namespace",
            "null", "package", "return", "true", "var", "void", "while");
    private static final Set<String> BUILTIN_NAMES = Set.of("Boolean", "String", "bool", "bytes",
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
        this.types.forEach((name, type) -> {
            if (!type.scalar()) {
                var message = DescriptorProtos.DescriptorProto.newBuilder().setName(name);
                int number = 1;
                for (var field : type.fields.entrySet()) {
                    message.addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                            .setName(field.getKey()).setJsonName(field.getKey()).setNumber(number++)
                            .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
                            .setType(field.getValue().equals("Boolean")
                                    ? DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL
                                    : DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING));
                }
                file.addMessageType(message);
            }
        });
        var proto = file.build();
        descriptors = DescriptorProtos.FileDescriptorSet.newBuilder().addFile(proto).build();
        try {
            fileDescriptor = Descriptors.FileDescriptor.buildFrom(proto, new Descriptors.FileDescriptor[0]);
        } catch (Descriptors.DescriptorValidationException e) {
            throw invalid("/dcm/types");
        }
    }

    static Dcm parse(Object value) {
        var root = shape(value,"/dcm","format","domain","types","capabilities","entrypoints");
        supported(root.get("format"),"keir-dcm/read-1","/dcm/format");
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
                    type = new Type(kind,"String",List.of(),Map.of());
                }
                case "enum" -> {
                    shape(def,path,"kind","values");
                    type = new Type(kind,"String",strings(def.get("values"),child(path,"values"),16,true,false),Map.of());
                }
                case "record" -> {
                    shape(def,path,"kind","fields");
                    var fields = boundedMap(def.get("fields"),child(path,"fields"),16);
                    if (fields.isEmpty()) throw invalid(child(path,"fields"));
                    var result = new TreeMap<String,String>();
                    fields.forEach((name,ref) -> {
                        String fp = child(child(path,"fields"),name);
                        identifier(name,fp,false);
                        result.put(name,reference(ref,fp));
                    });
                    type = new Type(kind,null,List.of(),immutable(result));
                }
                default -> throw unsupported(child(path,"kind"));
            }
            types.put(entry.getKey(),type);
        }
        for (var entry : types.entrySet()) {
            for (var field : entry.getValue().fields.entrySet()) {
                requireType(types,field.getValue(),child(child(child("/dcm/types",entry.getKey()),"fields"),field.getKey()),false);
            }
        }
        var capabilities = new TreeMap<String,Capability>();
        for (var entry : boundedMap(root.get("capabilities"),"/dcm/capabilities",8).entrySet()) {
            String path = child("/dcm/capabilities",entry.getKey());
            identifier(entry.getKey(),path,true);
            var cap = shape(entry.getValue(),path,"version","parameters","result","effect","errors");
            String version = text(cap.get("version"),child(path,"version"));
            supported(cap.get("effect"),"READ",child(path,"effect"));
            String result = reference(cap.get("result"),child(path,"result"));
            requireType(types,result,child(path,"result"),true);
            var parameters = new TreeMap<String,Parameter>();
            for (var param : boundedMap(cap.get("parameters"),child(path,"parameters"),16).entrySet()) {
                String pp = child(child(path,"parameters"),param.getKey());
                identifier(param.getKey(),pp,false);
                var p = shape(param.getValue(),pp,"type","binding");
                String type = reference(p.get("type"),child(pp,"type"));
                requireType(types,type,child(pp,"type"),false);
                String binding = text(p.get("binding"),child(pp,"binding"));
                if (!binding.equals("agent") && !binding.equals("host")) throw invalid(child(pp,"binding"));
                parameters.put(param.getKey(),new Parameter(type,binding));
            }
            var errors = strings(cap.get("errors"),child(path,"errors"),Integer.MAX_VALUE,false,true);
            capabilities.put(entry.getKey(),new Capability(version,immutable(parameters),result,"READ",errors));
        }
        var entries = new TreeMap<String,Entrypoint>();
        for (var entry : boundedMap(root.get("entrypoints"),"/dcm/entrypoints",4).entrySet()) {
            String path = child("/dcm/entrypoints",entry.getKey());
            identifier(entry.getKey(),path,true);
            var ep = shape(entry.getValue(),path,"input","output","trigger","consumer","grants");
            String input = reference(ep.get("input"),child(path,"input"));
            String output = reference(ep.get("output"),child(path,"output"));
            requireType(types,input,child(path,"input"),true);
            requireType(types,output,child(path,"output"),true);
            supported(ep.get("trigger"),"REQUEST",child(path,"trigger"));
            var consumer = shape(ep.get("consumer"),child(path,"consumer"),"id","version","kind");
            String consumerIdPath = child(child(path,"consumer"),"id");
            identifier(text(consumer.get("id"),consumerIdPath),consumerIdPath,true);
            text(consumer.get("version"),child(child(path,"consumer"),"version"));
            supported(consumer.get("kind"),"DISPLAY",child(child(path,"consumer"),"kind"));
            var grants = new TreeMap<String,Integer>();
            for (var grant : boundedMap(ep.get("grants"),child(path,"grants"),8).entrySet()) {
                String gp = child(child(path,"grants"),grant.getKey());
                if (!capabilities.containsKey(grant.getKey())) throw invalid(gp);
                Object n = grant.getValue();
                if (!(n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long || n instanceof BigInteger)) throw invalid(gp);
                var count = new BigInteger(n.toString());
                if (count.signum() <= 0) throw invalid(gp);
                if (count.compareTo(BigInteger.valueOf(4)) > 0) throw budget(gp);
                grants.put(grant.getKey(),count.intValue());
            }
            entries.put(entry.getKey(),new Entrypoint(input,output,"REQUEST",freezeMap(consumer),immutable(grants)));
        }
        if (entries.isEmpty()) throw invalid("/dcm/entrypoints");
        return new Dcm(root,types,capabilities,entries);
    }

    Map<String,Object> root() { return root; }
    Map<String,Type> types() { return types; }
    Map<String,Capability> capabilities() { return capabilities; }
    Map<String,Entrypoint> entrypoints() { return entrypoints; }
    Type type(String name) {
        return name.equals("Boolean") || name.equals("String")
                ? new Type("builtin",name,List.of(),Map.of()) : types.get(name);
    }
    Type record(String name) { Type t = types.get(name); return t != null && !t.scalar() ? t : null; }
    DescriptorProtos.FileDescriptorSet descriptors() { return descriptors; }
    Descriptors.FileDescriptor fileDescriptor() { return fileDescriptor; }
    byte[] descriptorBytes() {
        byte[] bytes = new byte[descriptors.getSerializedSize()];
        var out = CodedOutputStream.newInstance(bytes);
        out.useDeterministicSerialization();
        try { descriptors.writeTo(out); out.checkNoSpaceLeft(); }
        catch (IOException e) { throw invalid("/dcm/types"); }
        if (bytes.length > 65536) throw budget("/dcm/types");
        return bytes;
    }

    private static void requireType(Map<String,Type> types,String name,String path,boolean record) {
        Type t = types.get(name);
        boolean builtin = name.equals("Boolean") || name.equals("String");
        if (record ? t == null || t.scalar() : !builtin && (t == null || !t.scalar())) throw invalid(path);
    }
    private static String reference(Object value,String path) {
        String ref = text(value,path);
        if (!ref.matches("[A-Za-z][A-Za-z0-9]{0,63}")) throw invalid(path);
        return ref;
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
