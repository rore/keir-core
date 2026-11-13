package dev.keir.examples;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectConsumerTest {
    @Test void selectedModelResultsDoNotWidenTheDefaultResultOrReceiptContract() {
        var model=Map.<String,Object>of("format","keir-dcm/4","types",Map.of(
                "Status",Map.of("kind","enum","values",List.of("REQUESTED","NO_OP")),
                "Report",Map.of("kind","record","fields",Map.of("status","Status","ordered","Integer"))));
        var value=completed(); value.put("result",Map.of("status","REQUESTED","ordered","3"));
        invalid(value);
        assertEquals("REPORTED",ScenarioHost.consumeEvent(model,"Report",value).get("status"));
        for (Object number:List.of("03",3L,"9223372036854775808")) {
            value.put("result",Map.of("status","REQUESTED","ordered",number));
            assertEquals(INVALID,ScenarioHost.consumeEvent(model,"Report",value));
        }
        value.put("result",Map.of("status","REQUESTED","ordered","3"));
        value.put("effects",List.of(Map.of("step","write","capability","stock.request","state","NONE")));
        assertEquals(INVALID,ScenarioHost.consumeEvent(model,"Report",value));
    }
    private static final Map<String,Object> INVALID=Map.of("status","NOT_REPORTED","code","OUTCOME_INVALID");
    private static final Map<String,Object> ERROR=Map.of("code","BUSINESS_RULE","path","/steps/0");
    private static List<Object> trace(String code) {
        Map<String,Object> end=new LinkedHashMap<>(Map.of("sequence",1,"event","TERMINATED"));
        if(code!=null)end.put("code",code);
        return new ArrayList<>(List.of(Map.of("sequence",0,"event","INVOCATION_START"),end));
    }
    private static Map<String,Object> outcome(String kind,String result,String state,Map<String,Object> error) {
        Map<String,Object> value=new LinkedHashMap<>(); value.put("classification",kind);
        if(result!=null)value.put("result",new LinkedHashMap<>(Map.of("status",result)));
        if(error!=null)value.put("error",error);
        value.put("effects",state==null?List.of():List.of(Map.of("step","write","capability","tasks.create","state",state)));
        value.put("trace",trace(error==null?null:(String)error.get("code"))); return value;
    }
    private static Map<String,Object> completed() { return outcome("COMPLETED","CREATED",null,null); }
    private static void invalid(Object value) { assertEquals(INVALID,EffectConsumer.consume(value)); }
    private static void reported(Map<String,Object> value) { assertEquals("REPORTED",EffectConsumer.consume(value).get("status")); }
    private static List<Object> middle(Map<String,Object> event) {
        return List.of(Map.of("sequence",0,"event","INVOCATION_START"),event,Map.of("sequence",2,"event","TERMINATED"));
    }

    @Test void reportsCreatedAndNoopWithoutInventingEffectEvidenceAndCopiesData() {
        Map<String,Object> value=completed(); Map<String,Object> receipt=EffectConsumer.consume(value);
        assertEquals(Map.of("status","REPORTED","classification","COMPLETED","result",Map.of("status","CREATED"),"effects",List.of()),receipt);
        @SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)value.get("result");
        result.put("status","changed"); assertEquals(Map.of("status","CREATED"),receipt.get("result"));
        assertEquals(List.of(),EffectConsumer.consume(outcome("COMPLETED","NO_OP",null,null)).get("effects"));
        reported(outcome("COMPLETED","CREATED","CONFIRMED",null));
    }

    @Test void handledRejectionRemainsARejectionAndUnhandledRejectionHasNoResult() {
        for(String status:List.of("SOURCE_MISSING","REJECTED","CREATED","NO_OP")) {
            Map<String,Object> receipt=EffectConsumer.consume(outcome("BUSINESS_REJECTION",status,"NONE",ERROR));
            assertEquals("REPORTED",receipt.get("status")); assertEquals("BUSINESS_REJECTION",receipt.get("classification"));
            assertEquals(Map.of("status",status),receipt.get("result")); assertEquals(ERROR,receipt.get("error"));
            assertEquals(List.of(Map.of("step","write","capability","tasks.create","state","NONE")),receipt.get("effects"));
        }
        Map<String,Object> receipt=EffectConsumer.consume(outcome("BUSINESS_REJECTION",null,null,ERROR));
        assertEquals("REPORTED",receipt.get("status")); assertFalse(receipt.containsKey("result"));
    }

    @Test void preservesConfirmedFailureAndAllowsUnknownOnlyWithUncertainty() {
        reported(outcome("FAILED",null,"CONFIRMED",ERROR));
        reported(outcome("LIMIT_EXCEEDED",null,"NONE",ERROR));
        reported(outcome("UNCERTAIN",null,"UNKNOWN",ERROR));
        for(String kind:List.of("FAILED","LIMIT_EXCEEDED","BUSINESS_REJECTION","COMPLETED"))
            invalid(outcome(kind,kind.equals("COMPLETED")?"CREATED":null,"UNKNOWN",kind.equals("COMPLETED")?null:ERROR));
        invalid(outcome("UNCERTAIN",null,null,ERROR)); invalid(outcome("UNCERTAIN",null,"CONFIRMED",ERROR));
        invalid(outcome("UNCERTAIN","CREATED","UNKNOWN",ERROR)); invalid(outcome("COMPLETED","CREATED","NONE",null));
        invalid(outcome("COMPLETED","CREATED",null,ERROR)); invalid(outcome("FAILED","CREATED",null,ERROR));
        invalid(outcome("FAILED",null,null,null));
    }

    @Test void aLaterRejectedReadRetainsAnEarlierConfirmedWrite() {
        List<Object> events=List.of(
            Map.of("sequence",0,"event","INVOCATION_START"),Map.of("sequence",1,"event","STEP_START","step","write"),
            Map.of("sequence",2,"event","EFFECT_INTENT","step","write","capability","tasks.create"),
            Map.of("sequence",3,"event","CALL_DISPATCHED","step","write","capability","tasks.create"),
            Map.of("sequence",4,"event","EFFECT_RECORDED","step","write","capability","tasks.create","state","CONFIRMED"),
            Map.of("sequence",5,"event","CALL_SUCCEEDED","step","write","capability","tasks.create"),
            Map.of("sequence",6,"event","STEP_START","step","source"),
            Map.of("sequence",7,"event","CALL_DISPATCHED","step","source","capability","tasks.get"),
            Map.of("sequence",8,"event","TERMINATED","code","TASK_NOT_FOUND"));
        for(String result:Arrays.asList(null,"SOURCE_MISSING","NO_OP","CREATED")) {
            var value=outcome("BUSINESS_REJECTION",result,"CONFIRMED",Map.of("code","TASK_NOT_FOUND","path","/steps/1"));
            var actualEvents=new ArrayList<>(events);
            if(result!=null) {
                actualEvents.set(8,Map.of("sequence",8,"event","RETURNED"));
                actualEvents.add(Map.of("sequence",9,"event","TERMINATED","code","TASK_NOT_FOUND"));
            }
            value.put("trace",actualEvents);
            var receipt=EffectConsumer.consume(value); assertEquals("REPORTED",receipt.get("status"));
            assertEquals(value.get("effects"),receipt.get("effects")); assertEquals("BUSINESS_REJECTION",receipt.get("classification"));
            value.put("error",Map.of("code","TASK_NOT_FOUND","path","/steps/0")); invalid(value);
            value.put("error",Map.of("code","TASK_NOT_FOUND","path","/steps/1"));
            value.put("effects",List.of(Map.of("step","source","capability","tasks.get","state","CONFIRMED"))); invalid(value);
        }
        invalid(outcome("BUSINESS_REJECTION",null,"CONFIRMED",ERROR));
    }

    @Test void rejectsMalformedAndExtraEnvelopeResultEffectAndErrorFields() {
        for(Object value:Arrays.asList(null,false,List.of(),Map.of()))invalid(value);
        Map<String,Object> value=completed(); value.put("unexpected",true); invalid(value);
        value=completed(); value.put("result",Map.of("status","CREATED","effect","CONFIRMED")); invalid(value);
        value=completed(); value.put("effects",List.of(Map.of("step","write","capability","tasks.create","state","CONFIRMED","extra",true))); invalid(value);
        value=completed(); value.put("effects",List.of(Map.of("step","write","capability","tasks.create","state","CONFIRMED"),Map.of("step","other","capability","tasks.create","state","CONFIRMED"))); invalid(value);
        value=outcome("FAILED",null,null,new LinkedHashMap<>(ERROR));
        @SuppressWarnings("unchecked") Map<String,Object> error=(Map<String,Object>)value.get("error"); error.put("extra",true); invalid(value);
        value=completed(); value.put("result",null); invalid(value);
    }

    @Test void nonrecursiveCauseBelongsOnlyToTheMatchingHandlerSubtree() {
        Map<String,Object> cause=Map.of("code","CREATE_REJECTED","path","/steps/2");
        for(String path:List.of("/input","/steps/2/onError/ACCESS_DENIED","/steps/2/onError/CREATE_REJECTEDextra"))
            invalid(outcome("FAILED",null,"NONE",Map.of("code","EXPRESSION_FAILED","path",path,"cause",cause)));
        for(String path:List.of("/steps/2/onError/CREATE_REJECTED","/steps/2/onError/CREATE_REJECTED/status"))
            reported(outcome("LIMIT_EXCEEDED",null,"NONE",Map.of("code","BUDGET_EXCEEDED","path",path,"cause",cause)));
        Map<String,Object> error=Map.of("code","EXPRESSION_FAILED","path","/steps/2/onError/CREATE_REJECTED/status","cause",cause);
        invalid(outcome("BUSINESS_REJECTION","REJECTED","NONE",error)); invalid(outcome("UNCERTAIN",null,"UNKNOWN",error));
        invalid(outcome("FAILED",null,"NONE",Map.of("code","EXPRESSION_FAILED","path","/steps/2/onError/CREATE_REJECTED","cause",Map.of("code","CREATE_REJECTED","path","/steps/2","cause",cause))));
        reported(outcome("FAILED",null,null,Map.of("code","EXPRESSION_FAILED","path","/steps/2/onError/A~0~1B/status","cause",Map.of("code","A~/B","path","/steps/2"))));
        invalid(outcome("FAILED",null,null,Map.of("code","EXPRESSION_FAILED","path","/steps/8/onError/CREATE_REJECTED","cause",Map.of("code","CREATE_REJECTED","path","/steps/8"))));
    }

    @Test void traceNamesArePrimitiveStringsAndEventsHaveClosedRequiredAttributes() {
        List<Map<String,Object>> malformed=List.of(
            Map.of("sequence",1,"event",List.of("RETURNED")),Map.of("sequence",1,"event",List.of("STEP_START"),"step","source"),
            Map.of("sequence",1,"event","constructor"),Map.of("sequence",1,"event","CALL_DISPATCHED"),
            Map.of("sequence",1,"event","EFFECT_RECORDED","step","write","capability","tasks.create"),
            Map.of("sequence",1,"event","GUARD_EVALUATED","step","write","taken","true"),
            Map.of("sequence",1,"event","RETURNED","extra",true),Map.of("sequence",1.0,"event","RETURNED"),
            Map.of("sequence",1,"event","INVOCATION_START"),Map.of("sequence",1,"event","TERMINATED"));
        for(Map<String,Object> event:malformed) {Map<String,Object> value=completed();value.put("trace",middle(event));invalid(value);}
        Map<String,Object> value=completed();value.put("trace",List.of());invalid(value);
        value=completed();value.put("trace",List.of(Map.of("sequence",0,"event","INVOCATION_START","type","INVOCATION_START"),Map.of("sequence",1,"event","TERMINATED")));invalid(value);
        value=completed();value.put("trace",List.of(Map.of("sequence",1,"event","INVOCATION_START"),Map.of("sequence",2,"event","TERMINATED")));invalid(value);
        value=outcome("FAILED",null,null,ERROR);value.put("trace",trace("DIFFERENT"));invalid(value);
        value=completed();value.put("trace",trace("UNEXPECTED"));invalid(value);
    }

    @Test void traceAndPrimitiveUtf8BoundsAreInclusive() {
        List<Object> events=new ArrayList<>();events.add(Map.of("sequence",0,"event","INVOCATION_START"));
        for(int i=1;i<63;i++)events.add(Map.of("sequence",i,"event","RETURNED"));
        events.add(Map.of("sequence",63,"event","TERMINATED"));
        Map<String,Object> value=completed();value.put("trace",events);reported(value);
        events.set(63,Map.of("sequence",63,"event","RETURNED"));events.add(Map.of("sequence",64,"event","TERMINATED"));invalid(value);
        reported(outcome("FAILED",null,null,Map.of("code","é".repeat(128),"path","é".repeat(512))));
        invalid(outcome("FAILED",null,null,Map.of("code","é".repeat(129),"path","")));
        invalid(outcome("FAILED",null,null,Map.of("code","ERROR","path","é".repeat(513))));
        invalid(outcome("FAILED",null,null,Map.of("code","\ud800","path","")));
        value=completed();value.put("effects",List.of(Map.of("step","","capability","tasks.create","state","CONFIRMED")));invalid(value);
    }

    @Test void jsonCanonicalVectorsPreserveControlsUnicodeAndScalarKeyOrder() {
        assertArrayEquals(bytes("{\"a\":\"x\",\"b\":true}"),EffectJson.encode(Map.of("b",true,"a","x")));
        assertArrayEquals(bytes("\"\\u0000\\u000a\\u0009\\\"\\\\é😀\""),EffectJson.encode("\0\n\t\"\\é😀"));
        assertArrayEquals(bytes("{\"\ue000\":false,\"😀\":true}"),EffectJson.encode(Map.of("😀",true,"\ue000",false)));
        assertEquals(Map.of("empty","","flag",false),EffectJson.parse(bytes("{\"flag\":false,\"empty\":\"\"}")));
        assertNull(EffectJson.parse(bytes("null"))); assertEquals(-1L,EffectJson.parse(bytes("-1")));
        Map<String,Object> original=new LinkedHashMap<>(Map.of("values",new ArrayList<>(List.of(false,""))));
        Object copy=EffectJson.copy(original);original.clear();assertEquals(Map.of("values",List.of(false,"")),copy);
    }

    @Test void jsonRejectsUnsafeEncodingMalformedUtf8DuplicatesTrailingAndExcessBounds() {
        for(String value:List.of("{\"a\":1,\"a\":2}","true false","\ufefftrue","\"\\ud800\"","1.5","-0","9007199254740992","[".repeat(16)+"true"+"]".repeat(16)))
            assertThrows(IllegalArgumentException.class,()->EffectJson.parse(bytes(value)));
        assertThrows(IllegalArgumentException.class,()->EffectJson.parse(new byte[]{(byte)0xc3,0x28}));
        assertThrows(IllegalArgumentException.class,()->EffectJson.parse(new byte[1_048_577]));
        for(Object value:Arrays.asList(null,-1,1.0,Double.NaN,"\ud800",Map.of(1,"x")))
            assertThrows(IllegalArgumentException.class,()->EffectJson.encode(value));
        assertEquals(9_007_199_254_740_991L,EffectJson.parse(EffectJson.encode(9_007_199_254_740_991L)));
        String exact="x".repeat(1_048_574);assertEquals(1_048_576,EffectJson.encode(exact).length);
        assertThrows(IllegalArgumentException.class,()->EffectJson.encode(exact+"x"));
        Object nested=true;for(int i=0;i<15;i++)nested=List.of(nested);EffectJson.encode(nested);
        Object tooDeep=List.of(nested);assertThrows(IllegalArgumentException.class,()->EffectJson.encode(tooDeep));
        AtomicInteger conversions=new AtomicInteger();Object opaque=new Object(){@Override public String toString(){conversions.incrementAndGet();return "private";}};
        assertThrows(IllegalArgumentException.class,()->EffectJson.encode(opaque));assertEquals(0,conversions.get());
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
