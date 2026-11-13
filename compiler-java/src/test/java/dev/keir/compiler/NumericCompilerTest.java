package dev.keir.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumericCompilerTest {
    // Only the selected new identities change; every other historical authoring byte stays pinned.
    static byte[] retainedAuthoringArtifact(byte[] golden) {
        var expected=parse(golden);
        expected.put("format","keir-artifact/4"); expected.put("compiler",KeirCompiler.IDENTITY);
        return bytes(expected);
    }
    static String retainedAuthoringDigest(byte[] golden) { return StrictData.digest(parse(retainedAuthoringArtifact(golden))); }
    private final KeirCompiler compiler=new KeirCompiler();
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    private static byte[] bytes(Object value) { return StrictData.canonical(value); }
    private static Map<String,Object> parse(byte[] value) { return map(StrictData.parse(value,"/test")); }

    private Map<String,Object> model(String output,boolean optional) {
        var types=new TreeMap<String,Object>();
        types.put("EventId",Map.of("kind","nominal","base","String"));
        types.put("OwnerId",Map.of("kind","nominal","base","String"));
        types.put("Tags",Map.of("kind","list","element","String","maxItems",16L));
        types.put("Input",Map.of("kind","record","fields",optional
                ? Map.of("maybe",Map.of("type","Decimal","optional",true),"zero",Map.of("type","Integer","optional",true))
                : Map.of("flag","Boolean","text","String","tags","Tags")));
        types.put("Output",Map.of("kind","record","fields",Map.of("value",output)));
        return new TreeMap<>(Map.of("format","keir-dcm/4","domain",Map.of("id","numeric.test","version","1"),
                "hostKey",Map.of("source","requestKey","type","numeric.Key"),"types",types,"capabilities",Map.of(),
                "entrypoints",Map.of("Run",Map.of("input","Input","output","Output","trigger","EVENT","placement","AFTER_COMMIT",
                        "consumer",Map.of("id","numeric.report","version","1","kind","EVENT_REPORT"),
                        "event",Map.of("idType","EventId","sourceType","OwnerId")))));
    }
    private Map<String,Object> source(Map<String,Object> model,List<Object> steps,Object returned) {
        return new TreeMap<>(Map.of("language","keir/3","domain",Map.of("id","numeric.test","version","1","fingerprint",StrictData.digest(model)),
                "entrypoint","Run","name","numeric-rule","steps",steps,"return",Map.of("record",Map.of("value",returned))));
    }
    private KeirCompiler.Compilation compile(Object source,Object model) {
        return compiler.compile(bytes(source),bytes(model),KeirCompiler.Environment.supported());
    }

    @Test void emitExactIndependentDescriptorInputs() throws Exception {
        var models=new TreeMap<String,Map<String,Object>>();
        models.put("expression-only",model("Boolean",false));
        models.put("numeric-optionals",model("Decimal",true));
        var timestamp=model("Decimal",true);
        var types=map(timestamp.get("types"));
        var fields=new TreeMap<>(map(map(types.get("Input")).get("fields"))); fields.put("at",Map.of("type","Timestamp","optional",true));
        types.put("Input",Map.of("kind","record","fields",fields));
        models.put("numeric-timestamp",timestamp);
        models.put("retained-names",map(parse(Files.readAllBytes(Path.of("..","conformance","keir-4","retained-names.json"))).get("dcm")));
        models.put("retained-current",parse(Files.readAllBytes(Path.of("..","conformance","keir-3","dcm-guarded-result.json"))));
        for (String name:List.of("stock","quote")) models.put(name,parse(Files.readAllBytes(Path.of("..","examples","numeric",name,"dcm.json"))));
        var rows=new ArrayList<Object>();
        for (var entry:models.entrySet()) {
            var dcm=Dcm.parse(entry.getValue()); byte[] descriptor=dcm.descriptorBytes();
            var names=dcm.descriptors().getFileList().stream().map(com.google.protobuf.DescriptorProtos.FileDescriptorProto::getName).toList();
            assertEquals("keir_read.proto",names.getLast());
            assertEquals("keir-dcm/4".equals(entry.getValue().get("format")),names.contains("keir_numeric.proto"));
            if (names.contains("google/protobuf/timestamp.proto")) assertEquals("google/protobuf/timestamp.proto",names.getFirst());
            if (names.contains("keir_numeric.proto")) assertEquals("keir_numeric.proto",names.get(names.size()-2));
            if (entry.getKey().equals("numeric-optionals")) {
                var record=dcm.fileDescriptor().findMessageTypeByName("Input");
                assertTrue(record.findFieldByName("zero").toProto().getProto3Optional());
                assertEquals(com.google.protobuf.Descriptors.FieldDescriptor.Type.INT64,record.findFieldByName("zero").getType());
                assertFalse(record.findFieldByName("maybe").toProto().getProto3Optional());
                assertEquals("keir.numeric.Decimal",record.findFieldByName("maybe").getMessageType().getFullName());
            }
            assertArrayEquals(descriptor,Dcm.parse(parse(bytes(entry.getValue()))).descriptorBytes());
            rows.add(Map.of("id",entry.getKey(),"dcm",entry.getValue(),"base64",Base64.getEncoder().encodeToString(descriptor),
                    "sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(descriptor))));
        }
        byte[] batch=bytes(Map.of("cases",rows));
        assertArrayEquals(Files.readAllBytes(Path.of("..","conformance","keir-4","descriptors.json")),batch);
        Files.write(Path.of("target","numeric-descriptors.json"),batch);
    }

    @Test void compileRealNonnumericIntegerAndDecimalMeasurementRules() throws Exception {
        var rows=new ArrayList<Object>();
        for (String type:List.of("Boolean","Integer","Decimal")) {
            var model=model(type,false);
            map(model.get("types")).put("Input",Map.of("kind","record","fields",Map.of("owner","OwnerId","value",type,"divisor",type)));
            map(model.get("types")).put("Output",Map.of("kind","record","fields",Map.of("owner","OwnerId","value",type)));
            boolean plain=type.equals("Boolean"), decimal=type.equals("Decimal");
            if (plain) model.put("format","keir-dcm/3");
            var steps=List.<Object>of(Map.of("id","a","let",Map.of("cel","input.value")),
                    Map.of("id","b","let",Map.of("cel",plain ? "a != input.divisor" : decimal ? "a * decimal(\"1.5\")" : "a * 3")),
                    Map.of("id","c","let",Map.of("cel",plain ? "b" : "b / input.divisor")),
                    Map.of("id","d","let",Map.of("cel",plain ? "c" : decimal ? "round(c,2,\"HALF_EVEN\")" : "c + 1")));
            var source=source(model,steps,Map.of("cel","d")); if (plain) source.put("language","keir/2");
            source.put("return",Map.of("record",Map.of("owner",Map.of("cel","input.owner"),"value",Map.of("cel","d"))));
            var compiled=compile(source,model); assertTrue(compiled.succeeded(),type+": "+compiled.diagnostics());
            assertArrayEquals(compiled.artifact(),compile(source,model).artifact());
            rows.add(Map.of("id",type,"dcm",model,"artifact",parse(compiled.artifact()),
                    "values",plain ? List.of(true,false) : decimal ? List.of("1.25","2.555") : List.of("6","8"),
                    "divisor",plain ? false : decimal ? "1.5" : "2",
                    "expected",plain ? List.of(true,false) : decimal ? List.of("1.25","2.56") : List.of("10","13"),
                    "failure",Map.of("value",plain ? "true" : "1","divisor",plain ? false : decimal ? "9" : "2",
                            "code",plain ? "INPUT_INVALID" : "EXPRESSION_FAILED","path",plain ? "/input" : "/steps/2")));
        }
        Files.write(Path.of("target","numeric-runtime-rules.json"),bytes(Map.of("cases",rows)));
    }

    @Test void compileQualifiedExpressionsOnceForBothRuntimeTests() throws Exception {
        var fixtures=map(new org.yaml.snakeyaml.Yaml().load(Files.readString(Path.of("..","conformance","cel-profile","numeric-fixtures.json"))));
        var rows=new ArrayList<Object>();
        for (Object raw:(List<?>)fixtures.get("cases")) {
            var spec=map(raw);
            if (spec.containsKey("omit")) continue; // Missing guarded results are covered by the production guard corpus, not missing scalar BINDs.
            boolean optional="numeric".equals(spec.get("model"));
            var model=model((String)spec.get("type"),optional);
            var values=new TreeMap<String,Object>(Map.of("a","1.25","b","2","i","6","j","2","flag",true,"text","tenant"));
            var supplied=map(spec.getOrDefault("vars",Map.of()));
            for (String name:values.keySet()) if (supplied.containsKey(name)) values.put(name,supplied.get(name));
            var steps=new ArrayList<Object>();
            for (String name:List.of("a","b","i","j","flag","text")) {
                String type=Set.of("a","b").contains(name) ? "Decimal" : Set.of("i","j").contains(name) ? "Integer" : null;
                steps.add(Map.of("id",name,"let",type==null ? Map.of("literal",values.get(name)) : Map.of("literal",values.get(name),"type",type)));
            }
            if (spec.containsKey("bind")) steps.add(Map.of("id","d","let",Map.of("cel",spec.get("bind"))));
            var source=source(model,steps,Map.of("cel",spec.get("source")));
            var result=compile(source,model);
            assertTrue(result.succeeded(),spec.get("id")+": "+result.diagnostics());
            assertArrayEquals(result.artifact(),compile(source,model).artifact(),(String)spec.get("id"));
            Object expected=spec.get("expected");
            if (expected instanceof Map<?,?> candidates) expected=candidates.get("normalized18");
            Object input=supplied.getOrDefault("input",optional ? Map.of() : Map.of("flag",false,"text","","tags",List.of()));
            rows.add(Map.of("id",spec.get("id"),"dcm",model,"artifact",parse(result.artifact()),"input",input,"expected",expected));
        }
        var target=Path.of("target","numeric-integration.json");
        Files.createDirectories(target.getParent());
        byte[] batch=bytes(Map.of("cases",rows));
        assertArrayEquals(Files.readAllBytes(Path.of("..","conformance","keir-4","engine-matrix.json")),batch);
        Files.write(target,batch);
        assertEquals(97,rows.size());
    }

    @Test void numericTokensAndClosedParametersAreCheckedBeforeEmission() {
        var model=model("Boolean",false);
        for (String expression:List.of("0x10 == 16","1u == 1u","1.0 == 1.0","1e2 == 100",
                "decimal(input.text) == decimal(\"1\")","decimal(\"1.0\") == decimal(\"1\")",
                "1 == decimal(\"1\")","input.text.size() == 0","size(input.text) == 0",
                "round(decimal(\"1\"),19,\"DOWN\") == decimal(\"1\")",
                "round(decimal(\"1\"),1,\"UP\") == decimal(\"1\")",
                "round(decimal(\"1\"),size(input.tags),\"DOWN\") == decimal(\"1\")",
                "decimal(\"1\").value == \"1\"", "round(decimal(\"1\"),-1,\"DOWN\") == decimal(\"1\")",
                "round(decimal(\"1\"),2,input.text) == decimal(\"1\")", "size(input) == 0",
                "decimal(\"1\") + 1 == decimal(\"2\")", "decimal(\"1\") == input.text",
                "toDecimal(input.text) == decimal(\"1\")", "OwnerId(1) == OwnerId(1)",
                "keir.numeric.Decimal{value: \"1\"} == decimal(\"1\")")) {
            var source=source(model,List.of(Map.of("id","marker","let",Map.of("literal",true))),Map.of("cel",expression));
            var result=compile(source,model);
            assertFalse(result.succeeded(),expression);
            assertTrue(Set.of("TYPE_MISMATCH","CEL_POLICY_DENIED").contains(result.diagnostics().get(0).code()),result.diagnostics().toString());
        }
        for (String expression:List.of("-9223372036854775808 == -9223372036854775808","16 == 16",
                "\"0x10 1u 1e2\" == \"0x10 1u 1e2\"","\"😀\" == \"😀\" && 16 == 16")) {
            assertTrue(compile(source(model,List.of(Map.of("id","marker","let",Map.of("literal",true))),Map.of("cel",expression)),model).succeeded(),expression);
        }
    }

    private void acceptanceRow(List<Object> rows,String id,String expression,String type,Object wanted,Map<String,Object> spec) {
        var model=model(type,Boolean.TRUE.equals(spec.get("optional"))); var types=map(model.get("types"));
        if (spec.containsKey("fieldType")) types.put("Input",Map.of("kind","record","fields",Map.of("value",spec.get("fieldType"))));
        if (spec.containsKey("maxItems")) types.put("Tags",Map.of("kind","list","element","String","maxItems",spec.get("maxItems")));
        Object input=spec.getOrDefault("input",Map.of("flag",false,"text","","tags",spec.getOrDefault("tags",
                Collections.nCopies(((Number)spec.getOrDefault("tagCount",0)).intValue(),"x"))));
        var source=source(model,List.of(Map.of("id","marker","let",Map.of("literal",true))),Map.of("cel",expression));
        var compiled=compile(source,model); assertTrue(compiled.succeeded(),id+": "+compiled.diagnostics());
        // Numeric source values and literal expression text must survive JSON/YAML identically.
        var yaml=new org.yaml.snakeyaml.Yaml();
        assertArrayEquals(compiled.artifact(),compiler.compile(yaml.dump(source).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                yaml.dump(model).getBytes(java.nio.charset.StandardCharsets.UTF_8),KeirCompiler.Environment.supported()).artifact(),id);
        var row=new TreeMap<String,Object>(Map.of("id",id,"dcm",model,"artifact",parse(compiled.artifact()),"input",input,
                "expected",wanted,"binds",spec.containsKey("error") ? List.of() : List.of("marker")));
        if (spec.containsKey("error")) row.put("error",spec.get("error"));
        if (spec.containsKey("nullField")) row.put("nullField",spec.get("nullField"));
        rows.add(row);
    }

    @Test void compileMissingSectionElevenCasesWithIndependentLiteralOracles() throws Exception {
        var fixture=parse(Files.readAllBytes(Path.of("..","conformance","keir-4","acceptance.json")));
        var rows=new ArrayList<Object>();
        for (Object raw:(List<?>)fixture.get("rounding")) {
            var table=map(raw); String mode=(String)table.get("mode");
            for (String sign:List.of("positive","negative")) for (int i=0;i<3;i++) {
                String value=(sign.equals("negative") ? "-" : "")+List.of("2.344","2.345","2.346").get(i);
                Object wanted=((List<?>)table.get(sign)).get(i);
                for (String operation:List.of("round","multiplyRounded","divideRounded")) {
                    String args="decimal(\""+value+"\")"+(operation.equals("round") ? "" : ",decimal(\"1\")");
                    acceptanceRow(rows,mode+"-"+sign+"-"+i+"-"+operation,operation+"("+args+",2,\""+mode+"\")","Decimal",wanted,Map.of());
                }
                String whole=(sign.equals("negative") ? "-" : "")+List.of("2.4","2.5","2.6").get(i);
                acceptanceRow(rows,mode+"-"+sign+"-"+i+"-whole","round(decimal(\""+whole+"\"),0,\""+mode+"\")","Decimal",
                        ((List<?>)table.get(sign.equals("positive") ? "wholePositive" : "wholeNegative")).get(i),Map.of());
            }
            for (int scale:List.of(0,18)) for (String value:List.of("0","2"))
                acceptanceRow(rows,mode+"-exact-"+scale+"-"+value,"round(decimal(\""+value+"\"),"+scale+",\""+mode+"\")","Decimal",value,Map.of());
            for (String sign:List.of("tinyPositive","tinyNegative")) for (String operation:List.of("multiplyRounded","divideRounded")) {
                String value=sign.equals("tinyNegative") ? "-0.000000000000000001" : "0.000000000000000001";
                acceptanceRow(rows,mode+"-"+sign+"-"+operation,operation+"(decimal(\""+value+"\"),decimal(\""+
                        (operation.equals("multiplyRounded") ? "0.1" : "10")+"\"),18,\""+mode+"\")","Decimal",table.get(sign),Map.of());
            }
        }
        for (Object raw:(List<?>)fixture.get("expressions")) {
            var spec=map(raw); acceptanceRow(rows,(String)spec.get("id"),(String)spec.get("source"),(String)spec.get("type"),spec.get("expected"),spec);
        }
        String time="timestamp(\"9999-12-31T23:59:59.999Z\") + duration(\"1s\") == timestamp(\"9999-12-31T23:59:59.999Z\")";
        String number="decimal(\"1\") / decimal(\"0\") == decimal(\"0\")";
        for (String op:List.of("&&","||")) for (boolean timeFirst:List.of(true,false)) {
            String joined="("+(timeFirst ? time : number)+") "+op+" ("+(timeFirst ? number : time)+")";
            acceptanceRow(rows,"mixed-"+op+"-"+timeFirst,joined,"Boolean",timeFirst ? "TIME_ERROR" : "ERROR",Map.of());
            String mask=op.equals("&&") ? "false" : "true";
            for (boolean maskFirst:List.of(true,false)) acceptanceRow(rows,"masked-"+op+"-"+timeFirst+"-"+maskFirst,
                    maskFirst ? mask+" "+op+" ("+joined+")" : "("+joined+") "+op+" "+mask,"Boolean",op.equals("||"),Map.of());
        }
        var boundaries=parse(Files.readAllBytes(Path.of("..","conformance","keir-4","host-boundaries.json")));
        var handlerSource=map(boundaries.get("source"));
        map(map(map(((List<?>)handlerSource.get("steps")).get(4)).get("onError")).get("DENIED")).put("record",Map.of(
                "integer",Map.of("literal","-7","type","Integer"),"decimal",Map.of("literal","0.5","type","Decimal")));
        var handler=compile(handlerSource,boundaries.get("dcm")); assertTrue(handler.succeeded(),handler.diagnostics().toString());
        byte[] batch=bytes(Map.of("cases",rows,"handlerArtifact",parse(handler.artifact()))); assertTrue(batch.length<1_048_576);
        assertArrayEquals(Files.readAllBytes(Path.of("..","conformance","keir-4","acceptance-artifacts.json")),batch);
        Files.write(Path.of("target","numeric-acceptance.json"),batch);
    }

    @Test void compileCompleteStockAndQuoteProgramsOnceForBothRuntimes() throws Exception {
        var programs=new ArrayList<Object>();
        for (String name:List.of("stock","quote")) {
            var root=Path.of("..","examples","numeric",name);
            byte[] model=Files.readAllBytes(root.resolve("dcm.json"));
            byte[] source=Files.readAllBytes(root.resolve("source.json"));
            var compiled=compiler.compile(source,model,KeirCompiler.Environment.supported());
            assertTrue(compiled.succeeded(),name+": "+compiled.diagnostics());
            assertArrayEquals(compiled.artifact(),compiler.compile(source,model,KeirCompiler.Environment.supported()).artifact());
            assertArrayEquals(Files.readAllBytes(root.resolve("artifact.json")),compiled.artifact());
            Files.write(Path.of("target","numeric-"+name+".json"),compiled.artifact());
            programs.add(Map.of("name",name,"dcm",parse(model),"artifact",parse(compiled.artifact())));
        }
        Files.write(Path.of("target","numeric-programs.json"),bytes(Map.of("programs",programs)));
    }

    @Test void compileGuardedNumericHostBoundariesOnceForBothRuntimes() throws Exception {
        var document=parse(Files.readAllBytes(Path.of("..","conformance","keir-4","host-boundaries.json")));
        var model=map(document.get("dcm")); var source=map(document.get("source"));
        var compiled=compile(source,model);
        assertTrue(compiled.succeeded(),compiled.diagnostics().toString());
        assertArrayEquals(Files.readAllBytes(Path.of("..","conformance","keir-4","host-boundaries-artifact.json")),compiled.artifact());
        Files.write(Path.of("target","numeric-host-boundaries.json"),compiled.artifact());
        // Unguarded access, mixed numeric branches and unguarded presence tests fail closed.
        for (String forbidden:List.of("read.integer","g ? read.integer : decimal(\"0\")",
                "has(read.optionalInteger) ? read.optionalInteger : 0", "!g ? read.integer : 0")) {
            var changed=parse(bytes(source));
            map(((List<?>)changed.get("steps")).get(2)).put("let",Map.of("cel",forbidden));
            assertFalse(compile(changed,model).succeeded(),forbidden);
        }
        var listModel=parse(bytes(model));
        map(listModel.get("types")).put("Tags",Map.of("kind","list","element","String","maxItems",16L));
        map(map(map(listModel.get("types")).get("Data")).get("fields")).put("tags","Tags");
        var changed=parse(bytes(source)); map(changed.get("domain")).put("fingerprint",StrictData.digest(listModel));
        map(((List<?>)changed.get("steps")).get(2)).put("let",Map.of("cel","g ? size(read.tags) : 0"));
        assertFalse(compile(changed,listModel).succeeded());
        for (String element:List.of("Integer","Decimal")) {
            var invalid=parse(bytes(listModel)); map(map(invalid.get("types")).get("Tags")).put("element",element);
            assertThrows(RuntimeException.class,() -> Dcm.parse(invalid));
        }
    }

    @Test void retainedNominalNamesAndDescriptorBytesDoNotAcquireNumericMeaning() throws Exception {
        var retained=model("Boolean",false); retained.put("format","keir-dcm/3");
        var types=map(retained.get("types"));
        types.put("Integer",Map.of("kind","nominal","base","String"));
        types.put("Decimal",Map.of("kind","nominal","base","String"));
        types.put("Input",Map.of("kind","record","fields",Map.of("integer","Integer","decimal","Decimal")));
        var source=source(retained,List.of(Map.of("id","value","let",Map.of("literal","not-a-number","type","Integer"))),
                Map.of("cel","value == input.integer")); source.put("language","keir/2");
        var compiled=compile(source,retained);
        assertTrue(compiled.succeeded(),compiled.diagnostics().toString());
        var dcm=Dcm.parse(retained);
        assertEquals("nominal",dcm.type("Integer").kind());
        assertEquals("keir-record-descriptors/2",map(parse(compiled.artifact()).get("modelRef")).get("descriptorRecipe"));
        assertEquals(1,dcm.descriptors().getFileCount());
        assertEquals(com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING,
                dcm.fileDescriptor().findMessageTypeByName("Input").findFieldByName("integer").getType());
        byte[] batch=bytes(Map.of("dcm",retained,"artifact",parse(compiled.artifact()),
                "input",Map.of("integer","not-a-number","decimal","also-not-a-number")));
        assertArrayEquals(Files.readAllBytes(Path.of("..","conformance","keir-4","retained-names.json")),batch);
        Files.write(Path.of("target","numeric-retained.json"),batch);
        source.put("return",Map.of("record",Map.of("value",Map.of("cel","1 == 1"))));
        assertFalse(compile(source,retained).succeeded());
        source.put("language","keir/3");
        assertFalse(compile(source,retained).succeeded());
        var numeric=model("Boolean",false);
        map(numeric.get("types")).put("Integer",Map.of("kind","nominal","base","String"));
        assertFalse(compile(source(numeric,List.of(Map.of("id","marker","let",Map.of("literal",true))),Map.of("literal",true)),numeric).succeeded());
    }
}
