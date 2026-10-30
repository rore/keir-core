package dev.keir.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.keir.runtime.EffectRuntimeTest.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectHandlerTest {
    private static byte[] fixture(String suffix) throws IOException {
        try (var input=EffectHandlerTest.class.getResourceAsStream("/supplemental/effect-handler."+suffix)) {
            assertNotNull(input); return input.readAllBytes();
        }
    }
    @Test void actualCompiledHandlerOutputFitsExactly8192AndRejects8193WithoutLosingNoneOrCause() throws Exception {
        byte[] bytes=fixture("artifact.json");
        assertEquals(10272,bytes.length);
        String artifactPin="sha256:e9a60681ca2d29b30c651785969d8f42f6441f754aad4b736b3100262f01f05c";
        String modelPin="sha256:47ef3f12f2325e10473525d926171d581cca9499deedcb41ab80517ed164ce2c";
        assertEquals(artifactPin,new String(fixture("artifact.sha256"),StandardCharsets.UTF_8).trim());
        assertEquals(artifactPin,RuntimeData.digest(bytes));
        assertEquals(modelPin,new String(fixture("dcm.sha256"),StandardCharsets.UTF_8).trim());
        assertEquals(modelPin,RuntimeData.digest(RuntimeData.canonical(RuntimeData.parseJson(fixture("dcm.json"),65536))));
        var admission=admission(bytes); admission.put("expectedArtifactDigest",artifactPin); admission.put("expectedDcmDigest",modelPin);
        var prepared=prepared(bytes,admission,adapters((a,h,l,c)->done(read("COMPLETED","project-a")),
                (a,h,l,c)->done(Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"))));
        for(int padding:List.of(17,18)) {
            var input=new LinkedHashMap<String,Object>(INPUT); input.put("title","\0".repeat(56)+"x".repeat(200)); input.put("padding","x".repeat(padding));
            var outcome=await(KeirRuntime.invokeEffect(prepared,input,invocation(UUID.randomUUID()))); receipt(outcome,"NONE");
            var expected=new LinkedHashMap<String,Object>();
            for(int i=0;i<16;i++) expected.put("f"+i,i==15?input.get("padding"):input.get("title"));
            assertEquals(padding==17?8192:8193,RuntimeData.canonical(expected).length);
            if(padding==17) {
                assertEquals("BUSINESS_REJECTION",outcome.classification()); assertEquals(expected,outcome.result());
                assertEquals(Map.of("code","CREATE_REJECTED","path","/steps/2"),outcome.error());
                assertEquals("RETURNED",outcome.trace().get(outcome.trace().size()-2).get("event"));
            } else {
                assertEquals("LIMIT_EXCEEDED",outcome.classification()); assertNull(outcome.result());
                assertEquals(Map.of("code","BUDGET_EXCEEDED","path","/steps/2/onError/CREATE_REJECTED", "cause",Map.of("code","CREATE_REJECTED","path","/steps/2")),outcome.error());
                assertFalse(events(outcome).contains("RETURNED"));
            }
        }
    }
}
