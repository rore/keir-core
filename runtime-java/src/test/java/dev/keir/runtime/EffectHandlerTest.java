package dev.keir.runtime;

import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.keir.runtime.BoundEffectRuntimeTest.*;
import static dev.keir.runtime.RuntimeData.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectHandlerTest {
    @Test void handlerOutputFitsExactly8192AndRejects8193WithoutLosingNoneOrCause() throws Exception {
        var model=document(file("dcm.json"));
        var fieldTypes=new LinkedHashMap<String,Object>();
        for(int i=0;i<16;i++) fieldTypes.put("f"+i,"String");
        Artifact.definition(model,"FollowupResult").put("fields",fieldTypes);
        var bound=KeirRuntime.bindModel(canonical(model),implementations("simulation",
                (a,h,ms,c)->done(effectRead("COMPLETED")),
                (a,h,ms,c)->done(Map.of("kind","BUSINESS_REJECTION","code","CREATE_REJECTED","effect","NONE"))));
        assertTrue(bound.succeeded(),()->String.valueOf(bound.diagnostic()));

        for(int padding:List.of(17,18)) {
            var artifact=document(file("artifact-handler.json"));
            artifact.put("modelRef",Artifact.modelRef(model));
            map(artifact.get("expressions"),"").remove("e3");
            var expected=new LinkedHashMap<String,Object>();
            var literals=new LinkedHashMap<String,Object>();
            for(String field:fieldTypes.keySet()) {
                String value=field.equals("f15")?"x".repeat(padding):"\0".repeat(56)+"x".repeat(200);
                expected.put(field,value);
                literals.put(field,Map.of("type","String","literal",value));
            }
            var output=Map.of("type","FollowupResult","fields",literals);
            artifact.put("return",output);
            var write=map(list(artifact.get("steps"),"").getLast(),"");
            write.put("onError",Map.of("CREATE_REJECTED",output));
            assertEquals(padding==17?8192:8193,canonical(expected).length);
            var prepared=KeirRuntime.prepareProgram(canonical(artifact),bound.boundModel());
            assertTrue(prepared.succeeded(),()->String.valueOf(prepared.diagnostic()));
            var outcome=await(KeirRuntime.invokeBoundEffectProgram(prepared.prepared(),Map.of("taskId","task1"),invocation(true)));
            receipt(outcome,"NONE");
            var rejection=Map.of("code","CREATE_REJECTED","path","/steps/3");
            if(padding==17) {
                assertEquals("BUSINESS_REJECTION",outcome.classification());
                assertEquals(expected,outcome.result());
                assertEquals(rejection,outcome.error());
                assertEquals("RETURNED",outcome.trace().get(outcome.trace().size()-2).get("event"));
            } else {
                assertEquals("LIMIT_EXCEEDED",outcome.classification());
                assertNull(outcome.result());
                assertEquals(Map.of("code","BUDGET_EXCEEDED","path","/steps/3/onError/CREATE_REJECTED","cause",rejection),outcome.error());
                assertFalse(outcome.trace().stream().anyMatch(event->"RETURNED".equals(event.get("event"))));
            }
        }
    }
}
