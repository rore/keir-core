package dev.keir.runtime;

import java.nio.charset.StandardCharsets;
import java.util.*;
import static dev.keir.runtime.RuntimeData.*;

final class Artifact {
    private static final String MODEL = "/artifact/dcm";
    private static final Set<String> RESERVED = Set.of("true", "false", "null", "in", "as", "break", "const", "continue", "else", "for", "function", "if", "import", "let", "loop", "package", "namespace", "return", "var", "void", "while");
    private static final Set<String> BUILTINS = Set.of("Boolean", "String", "bool", "bytes", "double", "dyn", "int", "list", "map", "null_type", "string", "type", "uint", "input");

    private Artifact() {}

    record Plan(Map<String,Object> document, Map<String,Object> dcm, Map<String,Object> entrypoint,
                Map<String,Map<String,Object>> contexts, Map<String,Integer> calls, int callCount) {
        Plan {
            document = snapshot(document); dcm = snapshot(dcm); entrypoint = snapshot(entrypoint);
            LinkedHashMap<String,Map<String,Object>> copied = new LinkedHashMap<>();
            contexts.forEach((key,value) -> copied.put(key, snapshot(value)));
            contexts = Collections.unmodifiableMap(copied);
            calls = Collections.unmodifiableMap(new LinkedHashMap<>(calls));
        }

    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> snapshot(Map<String,Object> value) { return (Map<String,Object>)immutable(value); }
    private static void require(boolean condition, String path) { require(condition, path, "ARTIFACT_INVALID"); }
    private static void require(boolean condition, String path, String code) { if (!condition) throw new RuntimeFault(code, path); }
    private static void exact(Object value, String path, String... fields) { shape(value, Set.of(fields), path); }
    private static Map<String,Object> bounded(Object value, String path, int max) {
        Map<String,Object> result = map(value, path); require(result.size() <= max, path); return result;
    }
    private static void named(Object value, String path, boolean dotted) {
        require(value instanceof String name && name.matches(dotted ? "[A-Za-z][A-Za-z0-9.]{0,99}" : "[A-Za-z][A-Za-z0-9]{0,63}") && !RESERVED.contains(name), path);
    }
    static Map<String,Object> types(Map<String,Object> dcm) { return map(dcm.get("types"), MODEL + "/types"); }
    static Map<String,Object> definition(Map<String,Object> dcm, String name) {
        Object value = types(dcm).get(name);
        return value instanceof Map<?,?> ? map(value, MODEL) : Map.of();
    }
    static boolean scalar(Map<String,Object> dcm, String type) {
        return "Boolean".equals(type) || "String".equals(type) || ("nominal".equals(definition(dcm, type).get("kind")) || "enum".equals(definition(dcm, type).get("kind")));
    }
    static boolean record(Map<String,Object> dcm, String type) { return "record".equals(definition(dcm, type).get("kind")); }
    private static boolean scalarType(Map<String,Object> dcm, Object type) { return type instanceof String text && scalar(dcm, text); }
    private static boolean recordType(Map<String,Object> dcm, Object type) { return type instanceof String text && record(dcm, text); }

    private static void model(Map<String,Object> dcm) {
        String p = MODEL;
        exact(dcm, p, "format", "domain", "types", "capabilities", "entrypoints");
        require("keir-dcm/read-1".equals(dcm.get("format")), p);
        require(canonical(dcm).length <= Profile.DCM_BYTES, p, "BUDGET_EXCEEDED");
        exact(dcm.get("domain"), p + "/domain", "id", "version");
        Map<String,Object> domain = map(dcm.get("domain"), p);
        text(domain.get("id"), p, true, 256); text(domain.get("version"), p, true, 256);
        Map<String,Object> types = bounded(dcm.get("types"), p + "/types", 16);
        require(!types.isEmpty(), p);
        for (var item : types.entrySet()) {
            named(item.getKey(), p, false); require(!BUILTINS.contains(item.getKey()), p);
            Map<String,Object> type = map(item.getValue(), p);
            Object kind = type.get("kind");
            if ("nominal".equals(kind)) { exact(type, p, "kind", "base"); require("String".equals(type.get("base")), p); }
            else if ("enum".equals(kind)) {
                exact(type, p, "kind", "values"); List<Object> values = list(type.get("values"), p);
                require(!values.isEmpty() && values.size() <= 16 && new HashSet<>(values).size() == values.size(), p);
                for (Object value : values) text(value, p, true, 256);
            } else if ("record".equals(kind)) {
                exact(type, p, "kind", "fields"); Map<String,Object> fields = bounded(type.get("fields"), p, 16);
                require(!fields.isEmpty(), p); for (String field : fields.keySet()) named(field, p, false);
            } else throw new RuntimeFault("DCM_INVALID", p);
        }
        for (String name : types.keySet()) if (record(dcm, name))
            for (Object type : map(definition(dcm,name).get("fields"), p).values()) require(scalarType(dcm,type),p);
        Map<String,Object> caps = bounded(dcm.get("capabilities"), p + "/capabilities", 8);
        for (var item : caps.entrySet()) {
            named(item.getKey(), p, true); Map<String,Object> cap = map(item.getValue(),p);
            exact(cap,p,"version","parameters","result","effect","errors"); text(cap.get("version"),p,true,256);
            require("READ".equals(cap.get("effect")) && recordType(dcm,cap.get("result")),p);
            for (var parameter : bounded(cap.get("parameters"),p,16).entrySet()) {
                named(parameter.getKey(),p,false); exact(parameter.getValue(),p,"type","binding");
                Map<String,Object> param = map(parameter.getValue(),p);
                require(scalarType(dcm,param.get("type")) && ("agent".equals(param.get("binding")) || "host".equals(param.get("binding"))),p);
            }
            List<Object> errors = list(cap.get("errors"),p); require(new HashSet<>(errors).size() == errors.size(),p);
            for (Object error : errors) require(error instanceof String text && text.matches("[A-Z][A-Z0-9_]{0,63}"),p);
        }
        Map<String,Object> entries = bounded(dcm.get("entrypoints"),p + "/entrypoints",4); require(!entries.isEmpty(),p);
        for (var item : entries.entrySet()) {
            named(item.getKey(),p,true); Map<String,Object> entry = map(item.getValue(),p);
            exact(entry,p,"input","output","trigger","consumer","grants");
            require(recordType(dcm,entry.get("input")) && recordType(dcm,entry.get("output")) && "REQUEST".equals(entry.get("trigger")),p);
            exact(entry.get("consumer"),p,"id","version","kind"); Map<String,Object> consumer = map(entry.get("consumer"),p);
            named(consumer.get("id"),p,true); text(consumer.get("version"),p,true,256); require("DISPLAY".equals(consumer.get("kind")),p);
            for (var grant : bounded(entry.get("grants"),p,8).entrySet()) { require(caps.containsKey(grant.getKey()),p); number(grant.getValue(),1,4,p); }
        }
    }

    static Object value(Map<String,Object> dcm, String name, Object value, String path, String code) {
        try {
            Map<String,Object> type = definition(dcm,name); Object kind = type.get("kind");
            if ("Boolean".equals(name)) require(value instanceof Boolean,path);
            else if ("String".equals(name) || "nominal".equals(kind) || "enum".equals(kind)) {
                require(value instanceof String,path); String text = (String)value; unicode(text);
                require(text.getBytes(StandardCharsets.UTF_8).length <= Profile.STRING_BYTES,path,"BUDGET_EXCEEDED");
                if ("enum".equals(kind)) require(list(type.get("values"),path).contains(value),path);
            } else if ("record".equals(kind)) {
                Map<String,Object> fields = map(type.get("fields"),path); shape(value,fields.keySet(),path);
                for (var field : fields.entrySet()) value(dcm,(String)field.getValue(),map(value,path).get(field.getKey()),pointer(path,field.getKey()),code);
            } else throw new RuntimeFault(code,path);
            require(canonical(value).length <= Profile.VALUE_BYTES,path,"BUDGET_EXCEEDED");
            return immutable(value);
        } catch (RuntimeException error) {
            if (error instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)) throw fault;
            throw new RuntimeFault(code,path);
        }
    }

    static Plan validate(Map<String,Object> artifact) {
        exact(artifact,"/artifact","format","profile","compiler","environment","sourceDigest","dcmDigest","dcm","entrypoint","descriptors","expressions","steps","return");
        require("keir-artifact/read-1".equals(artifact.get("format")) && "keir-read/1".equals(artifact.get("profile")),"/artifact");
        text(artifact.get("compiler"),"/artifact/compiler",true,256);
        shape(artifact.get("environment"),Profile.ENVIRONMENT.keySet(),"/artifact/environment");
        require(Profile.ENVIRONMENT.equals(artifact.get("environment")),"/artifact/environment","PIN_MISMATCH");
        for (String key : List.of("sourceDigest","dcmDigest")) require(artifact.get(key) instanceof String text && text.matches("sha256:[0-9a-f]{64}"),"/artifact/"+key);
        require(digest(canonical(artifact.get("dcm"))).equals(artifact.get("dcmDigest")),"/artifact/dcmDigest");
        Map<String,Object> dcm = map(artifact.get("dcm"),MODEL); model(dcm);
        Map<String,Object> entries = map(dcm.get("entrypoints"),MODEL); require(entries.containsKey(artifact.get("entrypoint")),"/artifact/entrypoint");
        Map<String,Object> entry = map(entries.get(artifact.get("entrypoint")),MODEL);
        require(artifact.get("steps") instanceof List<?> steps && !steps.isEmpty() && steps.size() <= Profile.STEPS,"/artifact/steps","BUDGET_EXCEEDED");
        List<Object> steps = list(artifact.get("steps"),"/artifact/steps");
        Map<String,Object> expressions = bounded(artifact.get("expressions"),"/artifact/expressions",Profile.TOTAL_AST_NODES);
        Map<String,Object> variables = new LinkedHashMap<>(); variables.put("input",entry.get("input"));
        LinkedHashMap<String,Map<String,Object>> contexts = new LinkedHashMap<>();
        Map<String,Integer> calls = new LinkedHashMap<>(); int callCount = 0;
        for (int i=0; i<steps.size(); i++) {
            String p = "/artifact/steps/"+i; Map<String,Object> step = map(steps.get(i),p);
            named(step.get("id"),p,false); String id = (String)step.get("id");
            require(!Set.of("input","Boolean","String").contains(id) && !variables.containsKey(id) && !types(dcm).containsKey(id),p);
            if ("CALL".equals(step.get("op"))) {
                exact(step,p,"op","id","capability","version","args","resultType");
                Map<String,Object> caps = map(dcm.get("capabilities"),MODEL); require(caps.containsKey(step.get("capability")),p);
                Map<String,Object> cap = map(caps.get(step.get("capability")),p);
                require(Objects.equals(step.get("version"),cap.get("version")) && Objects.equals(step.get("resultType"),cap.get("result")),p);
                Map<String,Object> params = map(cap.get("parameters"),p); Set<String> agent = new TreeSet<>();
                params.forEach((key,param)-> { if ("agent".equals(map(param,p).get("binding"))) agent.add(key); });
                shape(step.get("args"),agent,p+"/args");
                for (String arg : agent) binding(dcm,expressions,variables,contexts,map(step.get("args"),p).get(arg),map(params.get(arg),p).get("type"),p+"/args/"+arg);
                String capability = (String)step.get("capability"); calls.merge(capability,1,Integer::sum); callCount++;
                Object grant = map(entry.get("grants"),p).get(capability);
                require(callCount <= Profile.CALLS && grant instanceof Number && calls.get(capability) <= ((Number)grant).longValue(),p,"BUDGET_EXCEEDED");
            } else if ("BIND".equals(step.get("op"))) {
                exact(step,p,"op","id","value","resultType");
                binding(dcm,expressions,variables,contexts,step.get("value"),step.get("resultType"),p+"/value");
            } else throw new RuntimeFault("ARTIFACT_INVALID",p);
            variables.put(id,step.get("resultType"));
        }
        Map<String,Object> returned = map(artifact.get("return"),"/artifact/return"); exact(returned,"/artifact/return","type","fields");
        require(Objects.equals(returned.get("type"),entry.get("output")),"/artifact/return");
        Map<String,Object> fields = map(definition(dcm,(String)entry.get("output")).get("fields"),MODEL);
        shape(returned.get("fields"),fields.keySet(),"/artifact/return/fields");
        for (String field : new TreeSet<>(fields.keySet())) binding(dcm,expressions,variables,contexts,map(returned.get("fields"),"/artifact/return").get(field),fields.get(field),"/artifact/return/fields/"+field);
        require(contexts.size() == expressions.size(),"/artifact/expressions");
        return new Plan(artifact,dcm,entry,contexts,calls,callCount);
    }

    private static void binding(Map<String,Object> dcm, Map<String,Object> expressions, Map<String,Object> variables,
                                LinkedHashMap<String,Map<String,Object>> contexts, Object value, Object type, String path) {
        Map<String,Object> binding = map(value,path); boolean literal = binding.containsKey("literal");
        exact(binding,path,literal ? "literal" : "expr","type");
        require(Objects.equals(binding.get("type"),type) && scalarType(dcm,type),path);
        if (literal) {
            require(!"nominal".equals(definition(dcm,(String)type).get("kind")),path);
            value(dcm,(String)type,binding.get("literal"),path,"ARTIFACT_INVALID");
        } else {
            String id = "e"+contexts.size(); require(id.equals(binding.get("expr")) && expressions.containsKey(id),path);
            Map<String,Object> expression = map(expressions.get(id),path); exact(expression,path,"checked","variables","resultType");
            shape(expression.get("variables"),variables.keySet(),path);
            require(variables.equals(expression.get("variables")) && Objects.equals(type,expression.get("resultType")),path);
            Map<String,Object> context = new LinkedHashMap<>(expression); context.put("variables",new LinkedHashMap<>(variables)); contexts.put(id,context);
        }
    }
}
