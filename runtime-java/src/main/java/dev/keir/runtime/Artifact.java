package dev.keir.runtime;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;
import static dev.keir.runtime.RuntimeData.*;

final class Artifact {
    private static final String MODEL = "/artifact/dcm";
    private static final Set<String> SCALARS = Set.of("Boolean", "String", "Timestamp");
    private static final Pattern TIMESTAMP = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z");
    private static final Set<String> RESERVED = Set.of("true", "false", "null", "in", "as", "break", "const", "continue", "else", "for", "function", "if", "import", "let", "loop", "package", "namespace", "return", "var", "void", "while");
    private static final Set<String> BUILTINS = Set.of("Boolean", "String", "Timestamp", "Duration", "bool", "bytes", "double", "dyn", "int", "list", "map", "null_type", "string", "type", "uint", "input");

    private Artifact() {}

    record Plan(Map<String,Object> document, Map<String,Object> dcm, Map<String,Object> entrypoint,
                Map<String,Map<String,Object>> contexts, Map<String,Integer> calls) {
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
        if (type == null) return false;
        Object kind = definition(dcm, type).get("kind");
        return SCALARS.contains(type) || "nominal".equals(kind) || "enum".equals(kind);
    }
    static boolean record(Map<String,Object> dcm, String type) { return "record".equals(definition(dcm, type).get("kind")); }
    static boolean listType(Map<String,Object> dcm, String type) { return "list".equals(definition(dcm, type).get("kind")); }
    static String fieldType(Object declaration) {
        return (String)(declaration instanceof Map<?,?> fields ? fields.get("type") : declaration);
    }
    static boolean optionalField(Object declaration) {
        return declaration instanceof Map<?,?> fields && Boolean.TRUE.equals(fields.get("optional"));
    }
    private static boolean scalarType(Map<String,Object> dcm, Object type) { return type instanceof String text && scalar(dcm, text); }
    private static boolean recordType(Map<String,Object> dcm, Object type) { return type instanceof String text && record(dcm, text); }
    private static boolean valueType(Map<String,Object> dcm, Object type, boolean optional) {
        return type instanceof String text && (scalar(dcm, text) || !optional && listType(dcm, text));
    }

    static Map<String,Object> modelRef(Map<String,Object> dcm) {
        Map<String,Object> domain = map(dcm.get("domain"),MODEL);
        return Map.of("domainId",domain.get("id"),"domainVersion",domain.get("version"),
                "format",dcm.get("format"),"digest",digest(canonical(dcm)),
                "descriptorRecipe","keir-record-descriptors/2");
    }

    static void validateModel(Map<String,Object> dcm) {
        String p = MODEL;
        exact(dcm, p, "format", "domain", "types", "capabilities", "entrypoints", "hostKey");
        require(Set.of("keir-dcm/2","keir-dcm/3").contains(dcm.get("format")), p);
        require(canonical(dcm).length <= Profile.DCM_BYTES, p, "BUDGET_EXCEEDED");
        exact(dcm.get("hostKey"),p+"/hostKey","source","type");
        Map<String,Object> key = map(dcm.get("hostKey"),p+"/hostKey");
        require("requestKey".equals(key.get("source")),p+"/hostKey/source","DCM_INVALID");
        metadataText(key.get("type"),p+"/hostKey/type");
        exact(dcm.get("domain"), p + "/domain", "id", "version");
        Map<String,Object> domain = map(dcm.get("domain"), p);
        text(domain.get("id"), p, true, 256); text(domain.get("version"), p, true, 256);
        Map<String,Object> types = bounded(dcm.get("types"), p + "/types", 16);
        require(!types.isEmpty(), p);
        for (var item : new TreeMap<>(types).entrySet()) {
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
            } else if ("list".equals(kind)) {
                exact(type, p, "kind", "element", "maxItems");
                number(type.get("maxItems"), 0, 16, p);
            } else throw new RuntimeFault("DCM_INVALID", p);
        }
        for (String name : types.keySet()) {
            Map<String,Object> type = definition(dcm, name);
            if (listType(dcm, name)) {
                Object element = type.get("element");
                require(!"Timestamp".equals(element) && scalarType(dcm, element), p);
            } else if (record(dcm, name)) {
                for (Object field : map(type.get("fields"), p).values()) {
                    if (field instanceof Map<?,?> declaration) {
                        exact(declaration, p, "type", "optional");
                        require(optionalField(declaration) && scalarType(dcm, declaration.get("type")), p);
                    } else require(valueType(dcm, field, false), p);
                }
            }
        }
        Map<String,Object> caps = bounded(dcm.get("capabilities"), p + "/capabilities", 8);
        for (var item : new TreeMap<>(caps).entrySet()) {
            named(item.getKey(), p, true); Map<String,Object> cap = map(item.getValue(),p);
            exact(cap,p,"version","parameters","result","effect","errors"); text(cap.get("version"),p,true,256);
            require(("READ".equals(cap.get("effect")) || "WRITE".equals(cap.get("effect"))) && recordType(dcm,cap.get("result")),p);
            for (var parameter : bounded(cap.get("parameters"),p,16).entrySet()) {
                named(parameter.getKey(),p,false);
                Map<String,Object> param = map(parameter.getValue(),p);
                boolean optional = param.containsKey("optional");
                if (optional) exact(param,p,"type","binding","optional");
                else exact(param,p,"type","binding");
                boolean authored = "agent".equals(param.get("binding"));
                require(authored || "host".equals(param.get("binding")),p);
                require(!optional || authored && optionalField(param),p);
                require(valueType(dcm,param.get("type"),optional),p);
            }
            List<Object> errors = list(cap.get("errors"),p); require(new HashSet<>(errors).size() == errors.size(),p);
            require(errors.size()<=16,p+"/capabilities/"+item.getKey()+"/errors","BUDGET_EXCEEDED");
            for (Object error : errors) require(error instanceof String text && text.matches("[A-Z][A-Z0-9_]{0,63}"),p);
        }
        Map<String,Object> entries = bounded(dcm.get("entrypoints"),p + "/entrypoints",4); require(!entries.isEmpty(),p);
        for (var item : new TreeMap<>(entries).entrySet()) {
            named(item.getKey(),p,true); Map<String,Object> entry = map(item.getValue(),p);
            Set<String> fields = new HashSet<>(Set.of("input","output","trigger","placement","consumer","event"));
            if (entry.containsKey("capabilityPolicy")) fields.add("capabilityPolicy");
            if (entry.containsKey("effectGrant")) fields.add("effectGrant");
            shape(entry,fields,p);
            require(recordType(dcm,entry.get("input")) && recordType(dcm,entry.get("output")),p);
            String path = p+"/entrypoints/"+item.getKey();
            exact(entry.get("consumer"),p,"id","version","kind"); Map<String,Object> consumer = map(entry.get("consumer"),p);
            named(consumer.get("id"),p,true); text(consumer.get("version"),p,true,256);
            boolean decision="DECISION".equals(consumer.get("kind"));
            require(decision ? "keir-dcm/3".equals(dcm.get("format")) : "EVENT_REPORT".equals(consumer.get("kind")),path+"/consumer/kind","DCM_INVALID");
            require((decision?"REQUEST":"EVENT").equals(entry.get("trigger")),path+"/trigger","DCM_INVALID");
            require((decision?"BEFORE_COMMIT":"AFTER_COMMIT").equals(entry.get("placement")),path+"/placement","DCM_INVALID");
            try { if (entry.containsKey("effectGrant")) number(entry.get("effectGrant"),0,1,path+"/effectGrant"); }
            catch (RuntimeFault failure) { throw new RuntimeFault("DCM_INVALID",path+"/effectGrant"); }
            if (decision) {
                require(!entry.containsKey("effectGrant") || number(entry.get("effectGrant"),0,1,path+"/effectGrant")==0,path+"/effectGrant","DCM_INVALID");
                Map<String,Object> output=map(definition(dcm,(String)entry.get("output")).get("fields"),path+"/output");
                Object declaration=output.get("decision");
                require(declaration instanceof String && "enum".equals(definition(dcm,(String)declaration).get("kind")),path+"/output","DCM_INVALID");
                List<Object> values=list(definition(dcm,(String)declaration).get("values"),path+"/output");
                require(values.size()==2 && new HashSet<>(values).equals(Set.of("ALLOW","DENY")),path+"/output","DCM_INVALID");
                for(Object field:output.values()) require(scalar(dcm,fieldType(field)),path+"/output","DCM_INVALID");
            }
            exact(entry.get("event"),path+"/event","idType","sourceType");
            Map<String,Object> event = map(entry.get("event"),path+"/event");
            for (String field : List.of("idType","sourceType")) {
                String type = metadataText(event.get(field),path+"/event/"+field);
                require("nominal".equals(definition(dcm,type).get("kind")),path+"/event/"+field,"DCM_INVALID");
            }
            if (entry.containsKey("capabilityPolicy")) CapabilityPolicy.narrow(CapabilityPolicy.catalog(dcm),entry.get("capabilityPolicy"),
                    pointer(p+"/entrypoints",item.getKey())+"/capabilityPolicy","DCM_INVALID");
        }
    }

    private static String metadataText(Object value,String path) {
        try { return text(value,path,true,256); }
        catch (RuntimeFault failure) { throw new RuntimeFault("DCM_INVALID",path); }
    }

    static Instant timestamp(String text) {
        if (text == null || text.length() != 24 || text.startsWith("0000") || !TIMESTAMP.matcher(text).matches())
            throw new IllegalArgumentException("Invalid Timestamp");
        try {
            return LocalDateTime.parse(text.substring(0, 23)).toInstant(ZoneOffset.UTC);
        } catch (DateTimeException invalid) {
            throw new IllegalArgumentException("Invalid Timestamp", invalid);
        }
    }

    private static void validateMembers(Map<String,Object> values, Map<String,Object> declarations, String path) {
        require(declarations.keySet().containsAll(values.keySet()), path);
        for (var field : declarations.entrySet())
            require(optionalField(field.getValue()) || values.containsKey(field.getKey()), path);
    }

    static Object value(Map<String,Object> dcm, String name, Object value, String path, String code) {
        Object checked = typedValue(dcm, name, value, path, code);
        try { canonical(checked, Profile.VALUE_BYTES); }
        catch (RuntimeFault fault) { throw new RuntimeFault(fault.code,path); }
        return checked;
    }

    private static Object typedValue(Map<String,Object> dcm, String name, Object value, String path, String code) {
        try {
            Map<String,Object> type = definition(dcm,name); Object kind = type.get("kind");
            if ("Boolean".equals(name)) require(value instanceof Boolean,path);
            else if ("Timestamp".equals(name)) {
                require(value instanceof String, path);
                timestamp((String)value);
            }
            else if ("String".equals(name) || "nominal".equals(kind) || "enum".equals(kind)) {
                require(value instanceof String,path); String text = (String)value;
                require(text.length() <= Profile.STRING_BYTES,path,"BUDGET_EXCEEDED");
                unicode(text);
                require(text.getBytes(StandardCharsets.UTF_8).length <= Profile.STRING_BYTES,path,"BUDGET_EXCEEDED");
                if ("enum".equals(kind)) require(list(type.get("values"),path).contains(value),path);
            } else if ("list".equals(kind)) {
                List<Object> elements = list(value, path);
                int count = elements.size();
                require(count <= ((Number)type.get("maxItems")).intValue(), path, "BUDGET_EXCEEDED");
                List<Object> copied = new ArrayList<>(count);
                String elementType = (String)type.get("element");
                for (int i = 0; i < count; i++)
                    copied.add(typedValue(dcm,elementType,elements.get(i),pointer(path,Integer.toString(i)),code));
                value = Collections.unmodifiableList(copied);
            } else if ("record".equals(kind)) {
                Map<String,Object> fields = map(type.get("fields"),path);
                require(value instanceof Map<?,?> raw && raw.size() <= fields.size(),path);
                Map<String,Object> supplied = map(value,path);
                validateMembers(supplied,fields,path);
                Map<String,Object> copied = new LinkedHashMap<>();
                for (var field : fields.entrySet()) if (supplied.containsKey(field.getKey()))
                    copied.put(field.getKey(),typedValue(dcm,fieldType(field.getValue()),supplied.get(field.getKey()),pointer(path,field.getKey()),code));
                value = Collections.unmodifiableMap(copied);
            } else throw new RuntimeFault(code,path);
        } catch (RuntimeException error) {
            if (error instanceof RuntimeFault fault && "BUDGET_EXCEEDED".equals(fault.code)) throw fault;
            throw new RuntimeFault(code,path);
        }
        return value;
    }

    static void validateEnvelope(Map<String,Object> artifact, Map<String,Object> dcm) {
        exact(artifact,"/artifact","format","compiler","environment","sourceDigest","modelRef","entrypoint","expressions","steps","return");
        text(artifact.get("compiler"),"/artifact/compiler",true,256);
        require(map(dcm.get("entrypoints"),MODEL).containsKey(artifact.get("entrypoint")),"/artifact/entrypoint");
        require(Profile.ENVIRONMENT.equals(artifact.get("environment")),"/artifact/environment","PIN_MISMATCH");
        require(Set.of("keir-artifact/2","keir-artifact/3").contains(artifact.get("format")),"/artifact/format");
        boolean current="keir-artifact/3".equals(artifact.get("format"));
        require((current ? Set.of("keir-compiler-java/0.3.0", "keir-compiler-java/0.3.1").contains(artifact.get("compiler")) : "keir-compiler-java/0.2.0".equals(artifact.get("compiler"))),"/artifact/compiler","PIN_MISMATCH");
        require((current?"keir-dcm/3":"keir-dcm/2").equals(dcm.get("format")),"/artifact/modelRef","PIN_MISMATCH");
        require(modelRef(dcm).equals(artifact.get("modelRef")),"/artifact/modelRef");
        require(artifact.get("sourceDigest") instanceof String text && text.matches("sha256:[0-9a-f]{64}"),"/artifact/sourceDigest");
    }

    static Plan validateProgram(Map<String,Object> artifact, Map<String,Object> dcm,
                                CapabilityPolicy.State policy, long effectGrant) {
        Map<String,Object> entries = map(dcm.get("entrypoints"),MODEL);
        Map<String,Object> entry = map(entries.get(artifact.get("entrypoint")),MODEL);
        require(artifact.get("steps") instanceof List<?> steps && !steps.isEmpty() && steps.size() <= Profile.STEPS,"/artifact/steps","BUDGET_EXCEEDED");
        List<Object> steps = list(artifact.get("steps"),"/artifact/steps");
        Map<String,Object> expressions = bounded(artifact.get("expressions"),"/artifact/expressions",Profile.TOTAL_AST_NODES);
        Map<String,Object> variables = new LinkedHashMap<>(); variables.put("input",entry.get("input"));
        LinkedHashMap<String,Map<String,Object>> contexts = new LinkedHashMap<>();
        Map<String,Integer> calls = new LinkedHashMap<>(); int callCount = 0, writeCount = 0;
        Set<String> ids = new HashSet<>(), booleanBinds = new HashSet<>();
        for (int i=0; i<steps.size(); i++) {
            String p = "/artifact/steps/"+i; Map<String,Object> step = map(steps.get(i),p);
            named(step.get("id"),p,false); String id = (String)step.get("id");
            require(!"input".equals(id) && !SCALARS.contains(id) && !"Duration".equals(id) && ids.add(id) && !types(dcm).containsKey(id),p);
            if ("CALL".equals(step.get("op"))) {
                Set<String> fields = new HashSet<>(Set.of("op","id","capability","version","args","resultType"));
                for (String optional : List.of("when","onError")) if (step.containsKey(optional)) fields.add(optional);
                shape(step,fields,p);
                if (step.containsKey("when")) require(booleanBinds.contains(step.get("when")),p+"/when");
                Map<String,Object> caps = map(dcm.get("capabilities"),MODEL);
                require(caps.containsKey(step.get("capability")),p,step.get("capability") instanceof String?"CAPABILITY_UNKNOWN":"ARTIFACT_INVALID");
                Map<String,Object> cap = map(caps.get(step.get("capability")),p);
                require(Objects.equals(step.get("version"),cap.get("version")) && Objects.equals(step.get("resultType"),cap.get("result")),p);
                Map<String,Object> params = map(cap.get("parameters"),p); Map<String,Object> agent = new TreeMap<>();
                params.forEach((key,param)-> { if ("agent".equals(map(param,p).get("binding"))) agent.put(key,param); });
                Map<String,Object> args = map(step.get("args"),p+"/args");
                validateMembers(args,agent,p+"/args");
                String capability = (String)step.get("capability"); calls.merge(capability,1,Integer::sum); callCount++;
                require(callCount <= Profile.CALLS,p,"BUDGET_EXCEEDED");
                if ("WRITE".equals(cap.get("effect"))) {
                    require(++writeCount<=1,p,"BUDGET_EXCEEDED");
                    require(writeCount<=effectGrant,p,"ENTRY_GRANT_DENIED");
                }
                require(CapabilityPolicy.permits(policy,capability,calls.get(capability)),p,"ENTRY_GRANT_DENIED");
                for (String arg : agent.keySet()) if (args.containsKey(arg))
                    binding(dcm,expressions,variables,contexts,args.get(arg),fieldType(agent.get(arg)),p+"/args/"+arg);
                if (step.containsKey("onError")) {
                    Map<String,Object> handlers = map(step.get("onError"),p+"/onError");
                    require(!handlers.isEmpty(),p+"/onError");
                    require(handlers.size()<=16,p+"/onError","BUDGET_EXCEEDED");
                    for (String code : new TreeSet<>(handlers.keySet())) {
                        String path = pointer(p+"/onError",code);
                        require(list(cap.get("errors"),path).contains(code),path);
                        output(dcm,entry,expressions,variables,contexts,handlers.get(code),path);
                    }
                }
            } else if ("BIND".equals(step.get("op"))) {
                exact(step,p,"op","id","value","resultType");
                binding(dcm,expressions,variables,contexts,step.get("value"),step.get("resultType"),p+"/value");
                if ("Boolean".equals(step.get("resultType"))) booleanBinds.add(id);
            } else throw new RuntimeFault("ARTIFACT_INVALID",p);
            if (!step.containsKey("when")) variables.put(id,step.get("resultType"));
        }
        output(dcm,entry,expressions,variables,contexts,artifact.get("return"),"/artifact/return");
        require(contexts.size() == expressions.size(),"/artifact/expressions");
        return new Plan(artifact,dcm,entry,contexts,calls);
    }

    private static void output(Map<String,Object> dcm,Map<String,Object> entry,Map<String,Object> expressions,
                               Map<String,Object> variables,LinkedHashMap<String,Map<String,Object>> contexts,Object value,String path) {
        Map<String,Object> returned = map(value,path); exact(returned,path,"type","fields");
        require(Objects.equals(returned.get("type"),entry.get("output")),path);
        Map<String,Object> fields = map(definition(dcm,(String)entry.get("output")).get("fields"),MODEL);
        Map<String,Object> supplied = map(returned.get("fields"),path+"/fields");
        validateMembers(supplied,fields,path+"/fields");
        for (String field : new TreeSet<>(supplied.keySet()))
            binding(dcm,expressions,variables,contexts,supplied.get(field),fieldType(fields.get(field)),pointer(path+"/fields",field));
    }

    private static void binding(Map<String,Object> dcm, Map<String,Object> expressions, Map<String,Object> variables,
                                LinkedHashMap<String,Map<String,Object>> contexts, Object value, Object type, String path) {
        Map<String,Object> binding = map(value,path); boolean literal = binding.containsKey("literal");
        exact(binding,path,literal ? "literal" : "expr","type");
        require(Objects.equals(binding.get("type"),type) && valueType(dcm,type,false),path);
        if (literal) {
            require(!"Timestamp".equals(type) && !listType(dcm,(String)type),path);
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
