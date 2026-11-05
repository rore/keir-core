package dev.keir.compiler;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class DcmTest {
    @Test void validatesSampleAndBuildsSortedDescriptors() throws Exception {
        Dcm dcm = Dcm.parse(sample());
        assertEquals(9,dcm.types().size());
        assertEquals("TaskId",dcm.capabilities().get("tasks.get").parameters().get("taskId").type());
        assertEquals("host",dcm.capabilities().get("tasks.get").parameters().get("actorId").binding());
        assertTrue(dcm.type("Boolean").scalar());
        assertTrue(dcm.type("String").scalar());
        assertFalse(dcm.type("Task").scalar());
        assertNull(dcm.record("TaskId"));
        var file = dcm.descriptors().getFile(0);
        assertEquals("keir_read.proto",file.getName());
        assertEquals("keir.read",file.getPackage());
        assertEquals("proto3",file.getSyntax());
        assertEquals(List.of("ClassifyInput","ClassifyResult","Task"),file.getMessageTypeList().stream().map(DescriptorProtos.DescriptorProto::getName).toList());
        var task = dcm.fileDescriptor().findMessageTypeByName("Task");
        assertEquals(List.of("id","status"),task.getFields().stream().map(Descriptors.FieldDescriptor::getName).toList());
        assertEquals(1,task.findFieldByName("id").getNumber());
        assertEquals(2,task.findFieldByName("status").getNumber());
        assertEquals("status",task.findFieldByName("status").getJsonName());
        assertEquals(Descriptors.FieldDescriptor.Type.STRING,task.findFieldByName("status").getType());
        assertEquals(dcm.descriptors(),DescriptorProtos.FileDescriptorSet.parseFrom(dcm.descriptorBytes()));
        assertArrayEquals(dcm.descriptorBytes(),Dcm.parse(reverse(sample())).descriptorBytes());
    }

    @Test void modelAndTypedMapsAreDeepImmutableSnapshots() {
        var model = sample();
        Dcm dcm = Dcm.parse(model);
        map(model,"domain").put("id","changed");
        assertEquals("sample.tasks",map(dcm.root(),"domain").get("id"));
        assertThrows(UnsupportedOperationException.class,() -> map(dcm.root(),"domain").put("id","x"));
        assertThrows(UnsupportedOperationException.class,() -> dcm.types().clear());
        assertThrows(UnsupportedOperationException.class,() -> dcm.type("Task").fields().clear());
        assertThrows(UnsupportedOperationException.class,() -> dcm.type("TaskStatus").values().clear());
    }

    @Test void acceptsNoParametersErrorsOrCapabilitiesAndBooleanFields() {
        var model = sample();
        map(model,"capabilities","tasks.get").put("parameters",new LinkedHashMap<>());
        map(model,"capabilities","tasks.get").put("errors",List.of());
        map(model,"entrypoints","ClassifyTask").put("capabilityPolicy",Map.of());
        map(model,"types","Task","fields").put("input","Boolean");
        map(model,"types","Task","fields").put("plain","String");
        Dcm dcm = Dcm.parse(model);
        assertTrue(dcm.capabilities().get("tasks.get").parameters().isEmpty());
        assertEquals(Descriptors.FieldDescriptor.Type.BOOL,dcm.fileDescriptor().findMessageTypeByName("Task").findFieldByName("input").getType());
        model.put("capabilities",Map.of());
        assertTrue(Dcm.parse(model).capabilities().isEmpty());
    }

    @Test void rejectsMissingAndUnknownFieldsWithEscapedPaths() {
        var model = sample();
        map(model,"types","Task").put("a~/b",true);
        failure(model,"FIELD_UNKNOWN","/dcm/types/Task/a~0~1b");
        model = sample();
        map(model,"entrypoints","ClassifyTask","consumer").remove("version");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/consumer/version");
        model = sample();
        map(model,"capabilities","tasks.get","parameters","actorId").put("extra",true);
        failure(model,"FIELD_UNKNOWN","/dcm/capabilities/tasks.get/parameters/actorId/extra");
    }

    @Test void rejectsRedefinitionReservedNamesAndInvalidIdentifiers() {
        for (String name : List.of("String","Boolean","input","int","if","bad_name","9Type")) {
            var model = sample();
            map(model,"types").put(name,Map.of("kind","nominal","base","String"));
            failure(model,"DCM_INVALID","/dcm/types/"+name);
        }
        var model = sample();
        map(model,"types","Task","fields").put("true","Boolean");
        failure(model,"DCM_INVALID","/dcm/types/Task/fields/true");
    }

    @Test void rejectsUnresolvedTypesNestedRecordsAndScalarResults() {
        var model = sample();
        map(model,"types","Task","fields").put("id","Missing");
        failure(model,"DCM_INVALID","/dcm/types/Task/fields/id");
        model = sample();
        map(model,"types","Task","fields").put("id","ClassifyInput");
        failure(model,"DCM_INVALID","/dcm/types/Task/fields/id");
        model = sample();
        map(model,"capabilities","tasks.get").put("result","TaskId");
        failure(model,"DCM_INVALID","/dcm/capabilities/tasks.get/result");
        model = sample();
        map(model,"capabilities","tasks.get","parameters","actorId").put("type","Task");
        failure(model,"DCM_INVALID","/dcm/capabilities/tasks.get/parameters/actorId/type");
        model = sample();
        map(model,"entrypoints","ClassifyTask").put("output","String");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/output");
    }

    @Test void rejectsUnsupportedKindsEffectsAndTriggersEvenIfUnused() {
        var model = sample();
        map(model,"types").put("Unused",Map.of("kind","list"));
        failure(model,"PROFILE_UNSUPPORTED","/dcm/types/Unused/kind");
        model = sample();
        map(model,"capabilities","tasks.get").put("effect","UNKNOWN");
        failure(model,"PROFILE_UNSUPPORTED","/dcm/capabilities/tasks.get/effect");
        model = sample();
        map(model,"entrypoints","ClassifyTask").put("trigger","REQUEST");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/trigger");
        model = sample();
        map(model,"entrypoints","ClassifyTask","consumer").put("kind","WRITE");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/consumer/kind");
    }

    @Test void rejectsBadEnumsErrorsBindingsAndShapes() {
        var model = sample();
        map(model,"types","TaskStatus").put("values",List.of("OPEN","OPEN"));
        failure(model,"DCM_INVALID","/dcm/types/TaskStatus/values/1");
        model = sample();
        map(model,"types","TaskStatus").put("values",List.of());
        failure(model,"DCM_INVALID","/dcm/types/TaskStatus/values");
        model = sample();
        map(model,"types","Task").put("fields",Map.of());
        failure(model,"DCM_INVALID","/dcm/types/Task/fields");
        model = sample();
        map(model,"capabilities","tasks.get").put("errors",List.of("secret error"));
        failure(model,"DCM_INVALID","/dcm/capabilities/tasks.get/errors/0");
        model = sample();
        map(model,"capabilities","tasks.get","parameters","actorId").put("binding","source");
        failure(model,"DCM_INVALID","/dcm/capabilities/tasks.get/parameters/actorId/binding");
        failure(null,"DCM_INVALID","/dcm");
    }

    @Test void enforcesCollectionBoundsInclusively() {
        var model = sample();
        var types = map(model,"types");
        for (int i=types.size();i<16;i++) types.put("Type"+i,Map.of("kind","nominal","base","String"));
        Dcm.parse(model);
        types.put("TooMany",Map.of("kind","nominal","base","String"));
        failure(model,"BUDGET_EXCEEDED","/dcm/types");
        model = sample();
        var fields = map(model,"types","Task","fields");
        for(int i=fields.size();i<16;i++) fields.put("field"+i,"String");
        Dcm.parse(model);
        fields.put("extra","String");
        failure(model,"BUDGET_EXCEEDED","/dcm/types/Task/fields");
        model = sample();
        var values = new ArrayList<String>();
        for(int i=0;i<16;i++) values.add("VALUE"+i);
        map(model,"types","TaskStatus").put("values",values);
        Dcm.parse(model);
        values.add("EXTRA");
        failure(model,"BUDGET_EXCEEDED","/dcm/types/TaskStatus/values");
        model = sample();
        var parameters = map(model,"capabilities","tasks.get","parameters");
        for(int i=parameters.size();i<16;i++) parameters.put("arg"+i,Map.of("type","String","binding","agent"));
        Dcm.parse(model);
        parameters.put("extra",Map.of("type","String","binding","agent"));
        failure(model,"BUDGET_EXCEEDED","/dcm/capabilities/tasks.get/parameters");
        model = sample();
        var capabilities = map(model,"capabilities");
        for(int i=capabilities.size();i<8;i++) capabilities.put("cap"+i,capabilities.get("tasks.get"));
        Dcm.parse(model);
        capabilities.put("extra",capabilities.get("tasks.get"));
        failure(model,"BUDGET_EXCEEDED","/dcm/capabilities");
        model = sample();
        var entries = map(model,"entrypoints");
        for(int i=entries.size();i<4;i++) entries.put("Entry"+i,entries.get("ClassifyTask"));
        Dcm.parse(model);
        entries.put("Extra",entries.get("ClassifyTask"));
        failure(model,"BUDGET_EXCEEDED","/dcm/entrypoints");
    }

    @Test void stringBoundsCountUtf8AndRejectMalformedUnicode() {
        var model = sample();
        map(model,"domain").put("id","é".repeat(128));
        Dcm.parse(model);
        map(model,"domain").put("id","é".repeat(129));
        failure(model,"BUDGET_EXCEEDED","/dcm/domain/id");
        map(model,"domain").put("id","\ud800");
        failure(model,"DCM_INVALID","/dcm/domain/id");
        map(model,"domain").put("id","");
        failure(model,"DCM_INVALID","/dcm/domain/id");
    }

    @Test void modelRetainsReadAndWriteCapabilitiesAndImmutableMetadata() {
        var model = sample();
        assertEquals("READ",Dcm.parse(model).capabilities().get("tasks.get").effect());
        map(model,"capabilities","tasks.get").put("effect","WRITE");
        Dcm dcm = Dcm.parse(model);
        assertEquals("WRITE",dcm.capabilities().get("tasks.get").effect());
        assertEquals("EVENT",dcm.entrypoints().get("ClassifyTask").trigger());
        assertEquals(1,dcm.entrypoints().get("ClassifyTask").effectGrant());
        map(model,"entrypoints","ClassifyTask").put("effectGrant",0);
        assertEquals(0,Dcm.parse(model).entrypoints().get("ClassifyTask").effectGrant());
        map(model,"hostKey").put("type","changed");
        map(model,"entrypoints","ClassifyTask","event").put("idType","changed");
        assertEquals("sample.RequestUuid/1",map(dcm.root(),"hostKey").get("type"));
        assertEquals("EventId",map(dcm.root(),"entrypoints","ClassifyTask","event").get("idType"));
        assertThrows(UnsupportedOperationException.class,()->map(dcm.root(),"hostKey").clear());
        assertThrows(UnsupportedOperationException.class,()->map(dcm.root(),"entrypoints","ClassifyTask","event").clear());
    }

    @Test void rejectsObsoleteFormatsAndUnknownFields() {
        var model = sample();
        model.put("format","keir-dcm/read-1");
        failure(model,"PROFILE_UNSUPPORTED","/dcm/format");
        model = sample(); model.put("format","keir-dcm/effect-1");
        failure(model,"PROFILE_UNSUPPORTED","/dcm/format");
        model = sample(); model.remove("hostKey");
        failure(model,"DCM_INVALID","/dcm/hostKey");
        model = sample(); model.put("extra",true);
        failure(model,"FIELD_UNKNOWN","/dcm/extra");
        model = sample(); map(model,"hostKey").put("extra",true);
        failure(model,"FIELD_UNKNOWN","/dcm/hostKey/extra");
        model = sample(); map(model,"entrypoints","ClassifyTask").put("extra",true);
        failure(model,"FIELD_UNKNOWN","/dcm/entrypoints/ClassifyTask/extra");
        model = sample(); map(model,"entrypoints","ClassifyTask","event").put("extra",true);
        failure(model,"FIELD_UNKNOWN","/dcm/entrypoints/ClassifyTask/event/extra");
        model = sample(); map(model,"entrypoints","ClassifyTask","event").remove("sourceType");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/event/sourceType");
    }

    @Test void effectLifecycleAndKeyMetadataFailAsInvalidDcm() {
        var model = sample(); model.put("assurance","REAL");
        failure(model,"FIELD_UNKNOWN","/dcm/assurance");
        model = sample(); map(model,"hostKey").put("source","input");
        failure(model,"DCM_INVALID","/dcm/hostKey/source");
        for (Object value : Arrays.asList(null,true,"","\ud800","é".repeat(129))) {
            model = sample(); map(model,"hostKey").put("type",value);
            failure(model,"DCM_INVALID","/dcm/hostKey/type");
        }
        model = sample(); map(model,"hostKey").put("type","é".repeat(128));
        Dcm.parse(model);
        model = sample(); map(model,"entrypoints","ClassifyTask").put("trigger","REQUEST");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/trigger");
        for (Object placement : Arrays.asList(null,1,"BEFORE_COMMIT","UNKNOWN")) {
            model = sample(); map(model,"entrypoints","ClassifyTask").put("placement",placement);
            failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/placement");
        }
        model = sample(); map(model,"entrypoints","ClassifyTask","consumer").put("kind","DISPLAY");
        failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/consumer/kind");
    }

    @Test void effectGrantIsOnlyAnExactZeroOrOneIncludingNativeIntegers() {
        for (Object value : List.of((byte)0,(short)1,0,1L,BigInteger.ZERO,BigInteger.ONE)) {
            var model = sample(); map(model,"entrypoints","ClassifyTask").put("effectGrant",value);
            assertEquals(((Number)value).intValue(),Dcm.parse(model).entrypoints().get("ClassifyTask").effectGrant());
        }
        for (Object value : Arrays.asList(null,-1,2,1.0,"1",true,new BigInteger("9007199254740992"))) {
            var model = sample(); map(model,"entrypoints","ClassifyTask").put("effectGrant",value);
            failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/effectGrant");
        }
    }

    @Test void effectEventsRequireDeclaredNominalStringTypes() {
        for (String field : List.of("idType","sourceType")) for (Object type : Arrays.asList(null,true,"String","Boolean","TaskStatus","Task","Missing")) {
            var model = sample(); map(model,"entrypoints","ClassifyTask","event").put(field,type);
            failure(model,"DCM_INVALID","/dcm/entrypoints/ClassifyTask/event/"+field);
        }
        var model = sample(); map(model,"types","EventId").put("base","Boolean");
        failure(model,"DCM_INVALID","/dcm/types/EventId/base");
    }

    @Test void readAndWriteErrorsAreCapped() {
        var errors = new ArrayList<String>();
        for (int i=0;i<16;i++) errors.add("ERROR"+i);
        var model = sample(); map(model,"capabilities","tasks.get").put("errors",errors);
        assertEquals(16,Dcm.parse(model).capabilities().get("tasks.get").errors().size());
        errors.add("EXTRA");
        failure(model,"BUDGET_EXCEEDED","/dcm/capabilities/tasks.get/errors");
        map(model,"capabilities","tasks.get").put("effect","WRITE");
        failure(model,"BUDGET_EXCEEDED","/dcm/capabilities/tasks.get/errors");
        model = sample(); map(model,"capabilities","tasks.get").put("effect","UNKNOWN");
        failure(model,"PROFILE_UNSUPPORTED","/dcm/capabilities/tasks.get/effect");
    }

    @Test void nativeKeyContractDoesNotBecomeABusinessTypeOrDescriptor() {
        var model = sample();
        assertNull(Dcm.parse(model).fileDescriptor().findMessageTypeByName("sample.RequestUuid/1"));
        map(model,"types","Task","fields").put("id","sample.RequestUuid/1");
        failure(model,"DCM_INVALID","/dcm/types/Task/fields/id");
        model = sample(); map(model,"capabilities","tasks.get","parameters","actorId").put("type","sample.RequestUuid/1");
        failure(model,"DCM_INVALID","/dcm/capabilities/tasks.get/parameters/actorId/type");
    }

    private static void failure(Object model,String code,String path) {
        CompileFailure error = assertThrows(CompileFailure.class,() -> Dcm.parse(model));
        assertEquals(code,error.code);
        assertEquals(path,error.path);
        assertEquals(code,error.getMessage());
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> map(Map<String,Object> root,String... path) {
        var result = root;
        for(String key:path) result = (Map<String,Object>)result.get(key);
        return result;
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> reverse(Map<String,Object> value) {
        var result = new LinkedHashMap<String,Object>();
        var keys = new ArrayList<>(value.keySet());
        Collections.reverse(keys);
        for(String key:keys) {
            Object v=value.get(key);
            result.put(key,v instanceof Map<?,?> m ? reverse((Map<String,Object>)m) : v);
        }
        return result;
    }
    private static Map<String,Object> mutable(Map<String,?> value) {
        var result = new LinkedHashMap<String,Object>();
        value.forEach((k,v) -> result.put(k,v instanceof Map<?,?> m ? mutable(stringMap(m)) : v));
        return result;
    }
    @SuppressWarnings("unchecked") private static Map<String,?> stringMap(Map<?,?> value) { return (Map<String,?>)value; }
    static Map<String,Object> sample() {
        return mutable(Map.of("format","keir-dcm/1","hostKey",Map.of("source","requestKey","type","sample.RequestUuid/1"),
            "domain",Map.of("id","sample.tasks","version","1"),
            "types",Map.of(
                "EventId",Map.of("kind","nominal","base","String"),
                "EventSource",Map.of("kind","nominal","base","String"),
                "TaskId",Map.of("kind","nominal","base","String"),
                "UserId",Map.of("kind","nominal","base","String"),
                "TaskStatus",Map.of("kind","enum","values",List.of("OPEN","COMPLETED")),
                "Classification",Map.of("kind","enum","values",List.of("DONE","PENDING")),
                "ClassifyInput",Map.of("kind","record","fields",Map.of("taskId","TaskId")),
                "Task",Map.of("kind","record","fields",Map.of("id","TaskId","status","TaskStatus")),
                "ClassifyResult",Map.of("kind","record","fields",Map.of("classification","Classification"))),
            "capabilities",Map.of("tasks.get",Map.of("version","1","parameters",Map.of(
                "taskId",Map.of("type","TaskId","binding","agent"),"actorId",Map.of("type","UserId","binding","host")),
                "result","Task","effect","READ","errors",List.of("TASK_NOT_FOUND","ACCESS_DENIED"))),
            "entrypoints",Map.of("ClassifyTask",Map.of("input","ClassifyInput","output","ClassifyResult","trigger","EVENT",
                "placement","AFTER_COMMIT","event",Map.of("idType","EventId","sourceType","EventSource"),
                "consumer",Map.of("id","sample.classification","version","1","kind","EVENT_REPORT")))));
    }
}
