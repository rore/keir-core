package dev.keir.runtime;

import java.util.*;
import dev.cel.expr.CheckedExpr;
import dev.cel.expr.Constant;
import dev.cel.expr.Expr;
import dev.cel.expr.Type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class GenericArtifactTest {
    private static Map<String,Object> fixture(String name) throws Exception {
        try (var input = GenericArtifactTest.class.getResourceAsStream(name)) {
            assertNotNull(input,name);
            return map(parseCanonical(input.readAllBytes()),"/test");
        }
    }
    private static Map<String,Object> model(boolean effect) throws Exception {
        try (var input = GenericArtifactTest.class.getResourceAsStream("/generic-hooks/"+(effect ? "effect" : "read")+"/dcm.json")) {
            assertNotNull(input); return map(parseJson(input.readAllBytes(),Profile.DCM_BYTES),"/test");
        }
    }
    private static Map<String,Object> artifact(boolean effect) throws Exception {
        return fixture("/generic-hooks/"+(effect ? "effect" : "read")+"/artifact.json");
    }
    private static Map<String,Object> entry(Map<String,Object> artifact, Map<String,Object> model) {
        return map(map(model.get("entrypoints"),"/test").get(artifact.get("entrypoint")),"/test");
    }
    private static RuntimeFault rejects(Runnable action,String code,String path) {
        RuntimeFault failure = assertThrows(RuntimeFault.class,action::run);
        assertEquals(code,failure.code); assertEquals(path,failure.path); assertNull(failure.getCause()); return failure;
    }

    @Test void realCompilerProgramsReuseDerivedModelStateWithoutEmbeddedFallback() throws Exception {
        for (boolean effect : List.of(false,true)) {
            var dcm = model(effect); Artifact.validateModel2(dcm,effect);
            var bound = CheckedCel.bindModel2(dcm); assertTrue(bound.descriptorBytes()>0);
            for (String name : effect ? List.of("artifact.json","artifact-project2.json","artifact-title2.json")
                    : List.of("artifact.json","artifact-repeated.json","artifact-alternate.json")) {
                var artifact = fixture("/generic-hooks/"+(effect ? "effect" : "read")+"/"+name);
                assertEquals(Artifact.modelRef(dcm),artifact.get("modelRef"));
                for (String forbidden : List.of("dcm","dcmDigest","descriptors")) assertFalse(artifact.containsKey(forbidden));
                var plan = Artifact.validateProgram2(artifact,dcm,effect);
                assertEquals(effect,plan.effect()); assertTrue(CheckedCel.prepare2(plan,bound).totalNodes()>0);
                assertEquals(effect ? 1 : 0,plan.writeCount());
            }
        }
    }

    @Test void envelopeRejectsEmbeddedOverridesMixedProfilesAndWrongReferences() throws Exception {
        var dcm = model(false);
        for (String field : List.of("dcm","dcmDigest","descriptors")) {
            var artifact = artifact(false); artifact.put(field,Map.of());
            rejects(()->Artifact.validateEnvelope2(artifact,dcm,false),"ARTIFACT_INVALID","/artifact");
        }
        var wrongProfile = artifact(false); wrongProfile.put("profile","keir-read/1");
        rejects(()->Artifact.validateEnvelope2(wrongProfile,dcm,false),"ARTIFACT_INVALID","/artifact/profile");
        var wrongFormat = artifact(false); wrongFormat.put("format","keir-artifact/effect-2");
        rejects(()->Artifact.validateEnvelope2(wrongFormat,dcm,false),"ARTIFACT_INVALID","/artifact/format");
        var wrongReference = artifact(false); map(wrongReference.get("modelRef"),"/test").put("descriptorRecipe","future");
        rejects(()->Artifact.validateEnvelope2(wrongReference,dcm,false),"ARTIFACT_INVALID","/artifact/modelRef");
        var wrongEnvironment = artifact(false); wrongEnvironment.put("environment",Map.of());
        rejects(()->Artifact.validateEnvelope2(wrongEnvironment,dcm,false),"ARTIFACT_INVALID","/artifact/environment");
    }

    @Test void completeCatalogIsValidatedButReadCannotCallItsUnusedWrite() throws Exception {
        var dcm = model(false); var artifact = artifact(false);
        var caps = map(dcm.get("capabilities"),"/test");
        String write = caps.keySet().stream().filter(id->"WRITE".equals(map(caps.get(id),"/test").get("effect"))).findFirst().orElseThrow();
        Artifact.validateModel2(dcm,false);
        var call = map(list(artifact.get("steps"),"/test").stream().filter(s->"CALL".equals(map(s,"/test").get("op"))).findFirst().orElseThrow(),"/test");
        int index = list(artifact.get("steps"),"/test").indexOf(call); call.put("capability",write);
        var capability = map(caps.get(write),"/test"); call.put("resultType",capability.get("result")); call.put("version",capability.get("version"));
        Map<String,Object> args = new TreeMap<>();
        map(capability.get("parameters"),"/test").forEach((name,definition)-> {
            if ("agent".equals(map(definition,"/test").get("binding"))) args.put(name,Map.of());
        });
        call.put("args",args);
        rejects(()->Artifact.validateProgram2(artifact,dcm,false),"ENTRY_GRANT_DENIED","/artifact/steps/"+index);
        map(caps.get(write),"/test").put("result","Unknown");
        assertThrows(RuntimeFault.class,()->Artifact.validateModel2(dcm,false));
    }

    @Test void oldGrantsAndNullOptionalControlsNeverBecomeDefaults() throws Exception {
        for (boolean effect : List.of(false,true)) {
            var grantsModel = model(effect); var artifact = artifact(effect); entry(artifact,grantsModel).put("grants",Map.of());
            assertThrows(RuntimeFault.class,()->Artifact.validateModel2(grantsModel,effect));
            var invalid = model(effect); entry(artifact,invalid).put("capabilityPolicy",null);
            assertThrows(RuntimeFault.class,()->Artifact.validateModel2(invalid,effect));
        }
        var dcm = model(true); entry(artifact(true),dcm).put("effectGrant",null);
        assertThrows(RuntimeFault.class,()->Artifact.validateModel2(dcm,true));
    }

    @Test void literalNominalProvenanceCannotBeForgedIntoAStringEnvironment() throws Exception {
        var dcm = model(true); var artifact = artifact(true);
        var nominal = list(artifact.get("steps"),"/test").stream().map(s->map(s,"/test"))
                .filter(s->"BIND".equals(s.get("op")) && "ProjectId".equals(s.get("resultType"))).findFirst().orElseThrow();
        String id = (String)nominal.get("id"); nominal.put("resultType","String");
        map(nominal.get("value"),"/test").put("type","String");
        for (Object expression : map(artifact.get("expressions"),"/test").values()) {
            var variables = map(map(expression,"/test").get("variables"),"/test");
            if (variables.containsKey(id)) variables.put(id,"String");
        }
        rejects(()->CheckedCel.prepare2(Artifact.validateProgram2(artifact,dcm,true),CheckedCel.bindModel2(dcm)),"ARTIFACT_INVALID","/artifact/expressions");
        var forged = artifact(true); var bind = list(forged.get("steps"),"/test").stream().map(s->map(s,"/test"))
                .filter(s->"BIND".equals(s.get("op")) && "ProjectId".equals(s.get("resultType"))).findFirst().orElseThrow();
        map(bind.get("value"),"/test").put("literal",true);
        assertThrows(RuntimeFault.class,()->Artifact.validateProgram2(forged,dcm,true));
    }

    @Test void nominalLiteralKeepsLegacyRejectionAndExistingUnicodeBounds() throws Exception {
        var legacy = fixture("/single-effect/artifact.json");
        list(legacy.get("steps"),"/test").addFirst(Map.of("op","BIND","id","constant","resultType","ProjectId",
                "value",Map.of("literal","project1","type","ProjectId")));
        rejects(()->Artifact.validate(legacy,true),"ARTIFACT_INVALID","/artifact/steps/0/value");
        var dcm = model(true); var types = CheckedCel.bindModel2(dcm);
        for (String value : List.of("","é".repeat(128),"😀".repeat(64))) {
            var artifact = artifact(true); var literal = map(map(list(artifact.get("steps"),"/test").getFirst(),"/test").get("value"),"/test");
            literal.put("literal",value);
            var bundle = CheckedCel.prepare2(Artifact.validateProgram2(artifact,dcm,true),types);
            assertEquals(value,bundle.evaluate(literal,Map.of(),"/steps/0"));
        }
        var tooLong = artifact(true); map(map(list(tooLong.get("steps"),"/test").getFirst(),"/test").get("value"),"/test").put("literal","é".repeat(129));
        rejects(()->Artifact.validateProgram2(tooLong,dcm,true),"BUDGET_EXCEEDED","/artifact/steps/0/value");
    }

    @Test void checkedStringConstantCannotAcquireNominalProvenanceFromResultMetadata() throws Exception {
        var dcm = model(true); var artifact = artifact(true);
        var expression = map(artifact.get("expressions"),"/test").values().stream().map(value->map(value,"/test"))
                .filter(value->"ProjectId".equals(value.get("resultType"))).findFirst().orElseThrow();
        var checked = CheckedExpr.parseFrom(Base64.getDecoder().decode((String)expression.get("checked"))).toBuilder()
                .setExpr(Expr.newBuilder().setId(1).setConstExpr(Constant.newBuilder().setStringValue("project1")))
                .clearReferenceMap().clearTypeMap().putTypeMap(1,Type.newBuilder().setPrimitive(Type.PrimitiveType.STRING).build()).build();
        expression.put("checked",Base64.getEncoder().encodeToString(checked.toByteArray()));
        rejects(()->CheckedCel.prepare2(Artifact.validateProgram2(artifact,dcm,true),CheckedCel.bindModel2(dcm)),"ARTIFACT_INVALID","/artifact/expressions");
    }

    @Test void staticAdmissionCeilingsPrecedeMalformedBindingMetadataAtTheKnownCall() throws Exception {
        for (boolean effect : List.of(false,true)) {
            var dcm = model(effect); var artifact = artifact(effect);
            var call = list(artifact.get("steps"),"/test").stream().map(value->map(value,"/test"))
                    .filter(value->"CALL".equals(value.get("op")) && (!effect || "tasks.create".equals(value.get("capability"))))
                    .findFirst().orElseThrow();
            var args = map(call.get("args"),"/test"); args.replaceAll((name,value)->Map.of());
            var unrestricted = CapabilityPolicy.catalog(dcm);
            var denied = CapabilityPolicy.narrow(unrestricted,Map.of("maxCalls",Map.of((String)call.get("capability"),0L)),"/policy","ADMISSION_INVALID");
            RuntimeFault malformed = assertThrows(RuntimeFault.class,()->Artifact.validateProgram2(artifact,dcm,effect));
            assertEquals("ARTIFACT_INVALID",malformed.code); assertTrue(malformed.path.contains("/args/"));
            rejects(()->Artifact.validateProgram2(artifact,dcm,effect,Map.of(),denied,1),"AUTHORITY_DENIED","/admission/capabilityPolicy");
            rejects(()->Artifact.validateProgram2(artifact,dcm,effect,Map.of("calls",effect?1L:0L),denied,0),"BUDGET_EXCEEDED","/artifact/steps");
            rejects(()->Artifact.validateProgram2(artifact,dcm,effect,Map.of("steps",0L),unrestricted,1),"BUDGET_EXCEEDED","/artifact/steps");
            if (effect) rejects(()->Artifact.validateProgram2(artifact,dcm,true,Map.of(),denied,0),"AUTHORITY_DENIED","/admission/effectGrant");
        }
    }

    @Test void everyPotentialWriteCountsEvenBehindFalseGuardsAndDefaultIsOne() throws Exception {
        var dcm = model(true); var artifact = artifact(true); entry(artifact,dcm).remove("effectGrant");
        Artifact.validateModel2(dcm,true);
        var caps = map(dcm.get("capabilities"),"/test");
        String id = caps.keySet().stream().filter(key->"WRITE".equals(map(caps.get(key),"/test").get("effect"))).findFirst().orElseThrow();
        var cap = map(caps.get(id),"/test"); Map<String,Object> args = new TreeMap<>();
        map(cap.get("parameters"),"/test").forEach((name,definition)-> {
            var parameter = map(definition,"/test");
            if ("agent".equals(parameter.get("binding"))) args.put(name,Map.of("literal","value","type",parameter.get("type")));
        });
        var first = Map.of("op","CALL","id","first","capability",id,"version",cap.get("version"),"args",args,"resultType",cap.get("result"),"when","never");
        var second = new HashMap<>(first); second.put("id","second");
        artifact.put("steps",List.of(Map.of("op","BIND","id","never","value",Map.of("literal",false,"type","Boolean"),"resultType","Boolean"),first,second));
        artifact.put("expressions",Map.of()); artifact.put("modelRef",Artifact.modelRef(dcm));
        rejects(()->Artifact.validateProgram2(artifact,dcm,true),"BUDGET_EXCEEDED","/artifact/steps/2");
        entry(artifact,dcm).put("effectGrant",0L); artifact.put("modelRef",Artifact.modelRef(dcm));
        rejects(()->Artifact.validateProgram2(artifact,dcm,true),"ENTRY_GRANT_DENIED","/artifact/steps/1");
    }
}
