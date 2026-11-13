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
        Files.write(target,bytes(Map.of("cases",rows)));
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
                "decimal(\"1\").value == \"1\"")) {
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

    @Test void compileCompleteStockAndQuoteProgramsOnceForBothRuntimes() throws Exception {
        var programs=new ArrayList<Object>();
        for (String name:List.of("stock","quote")) {
            var root=Path.of("..","examples","numeric",name);
            byte[] model=Files.readAllBytes(root.resolve("dcm.json"));
            byte[] source=Files.readAllBytes(root.resolve("source.json"));
            var compiled=compiler.compile(source,model,KeirCompiler.Environment.supported());
            assertTrue(compiled.succeeded(),name+": "+compiled.diagnostics());
            assertArrayEquals(compiled.artifact(),compiler.compile(source,model,KeirCompiler.Environment.supported()).artifact());
            programs.add(Map.of("name",name,"dcm",parse(model),"artifact",parse(compiled.artifact())));
        }
        Files.write(Path.of("target","numeric-programs.json"),bytes(Map.of("programs",programs)));
    }

    @Test void compileGuardedNumericHostBoundariesOnceForBothRuntimes() throws Exception {
        var document=parse(Files.readAllBytes(Path.of("..","conformance","keir-4","host-boundaries.json")));
        var model=map(document.get("dcm")); var source=map(document.get("source"));
        var compiled=compile(source,model);
        assertTrue(compiled.succeeded(),compiled.diagnostics().toString());
        Files.write(Path.of("target","numeric-host-boundaries.json"),compiled.artifact());
        // Unguarded access, mixed numeric branches and unguarded presence tests fail closed.
        for (String forbidden:List.of("read.integer","g ? read.integer : decimal(\"0\")",
                "has(read.optionalInteger) ? read.optionalInteger : 0")) {
            var changed=parse(bytes(source));
            map(((List<?>)changed.get("steps")).get(2)).put("let",Map.of("cel",forbidden));
            assertFalse(compile(changed,model).succeeded(),forbidden);
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
        Files.write(Path.of("target","numeric-retained.json"),bytes(Map.of("dcm",retained,"artifact",parse(compiled.artifact()),
                "input",Map.of("integer","not-a-number","decimal","also-not-a-number"))));
        source.put("return",Map.of("record",Map.of("value",Map.of("cel","1 == 1"))));
        assertFalse(compile(source,retained).succeeded());
        source.put("language","keir/3");
        assertFalse(compile(source,retained).succeeded());
        var numeric=model("Boolean",false);
        map(numeric.get("types")).put("Integer",Map.of("kind","nominal","base","String"));
        assertFalse(compile(source(numeric,List.of(Map.of("id","marker","let",Map.of("literal",true))),Map.of("literal",true)),numeric).succeeded());
    }
}
