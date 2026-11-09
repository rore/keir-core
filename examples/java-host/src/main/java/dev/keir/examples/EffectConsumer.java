package dev.keir.examples;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** EVENT_REPORT validates the public outcome independently of the runtime. */
final class EffectConsumer {
    private static final Set<String> CLASSIFICATIONS=Set.of("COMPLETED","BUSINESS_REJECTION","FAILED","LIMIT_EXCEEDED","UNCERTAIN");
    private static final Set<String> RESULTS=Set.of("CREATED","NO_OP","SOURCE_MISSING","REJECTED");
    private static final Set<String> STATES=Set.of("CONFIRMED","NONE","UNKNOWN");
    private static final Map<String,Set<String>> TRACE_FIELDS=Map.ofEntries(
        Map.entry("INVOCATION_START",Set.of()),Map.entry("STEP_START",Set.of("step")),
        Map.entry("GUARD_EVALUATED",Set.of("step","taken")),Map.entry("STEP_SKIPPED",Set.of("step")),Map.entry("VALUE_BOUND",Set.of("step")),
        Map.entry("EFFECT_INTENT",Set.of("step","capability")),Map.entry("CALL_DISPATCHED",Set.of("step","capability")),
        Map.entry("EFFECT_RECORDED",Set.of("step","capability","state")),Map.entry("CALL_SUCCEEDED",Set.of("step","capability")),
        Map.entry("RETURNED",Set.of()),Map.entry("TERMINATED",Set.of()));
    private EffectConsumer() {}

    static Map<String,Object> consume(Object value) {
        try {
            if(!shape(value,Set.of("classification","effects","trace"),Set.of("result","error")))return invalid();
            Map<?,?> outcome=(Map<?,?>)value; Object classification=outcome.get("classification");
            if(!(classification instanceof String kind) || !CLASSIFICATIONS.contains(kind) ||
                !(outcome.get("effects") instanceof List<?> effects) || effects.size()>1 || !effects.stream().allMatch(EffectConsumer::effect) || !trace(outcome.get("trace")))return invalid();
            boolean hasResult=outcome.containsKey("result"),hasError=outcome.containsKey("error");
            Object result=outcome.get("result"),error=outcome.get("error");
            boolean failure=kind.equals("FAILED") || kind.equals("LIMIT_EXCEEDED");
            if(hasResult && !result(result) || hasError && !error(error,failure))return invalid();
            if(hasError && ((Map<?,?>)error).containsKey("cause")) {
                Map<?,?> original=(Map<?,?>)((Map<?,?>)error).get("cause");
                String originalPath=(String)original.get("path"),originalCode=(String)original.get("code");
                String handler=originalPath+"/onError/"+originalCode.replace("~","~0").replace("/","~1");
                String path=(String)((Map<?,?>)error).get("path");
                if(!originalPath.matches("/steps/[0-7]") || !(path.equals(handler) || path.startsWith(handler+"/")))return invalid();
            }
            Map<?,?> end=(Map<?,?>)((List<?>)outcome.get("trace")).getLast();
            if(hasError ? !Objects.equals(end.get("code"),((Map<?,?>)error).get("code")) : end.containsKey("code"))return invalid();
            if(kind.equals("COMPLETED") && (!hasResult || hasError || effects.stream().anyMatch(item->!state(item).equals("CONFIRMED"))))return invalid();
            if(kind.equals("BUSINESS_REJECTION") && (!hasError ||
                effects.stream().anyMatch(item->state(item).equals("UNKNOWN") || state(item).equals("CONFIRMED") && !priorConfirmation(item,outcome))))return invalid();
            if(failure && (!hasError || hasResult || effects.stream().anyMatch(item->state(item).equals("UNKNOWN"))))return invalid();
            if(kind.equals("UNCERTAIN") && (!hasError || hasResult || effects.size()!=1 || !state(effects.getFirst()).equals("UNKNOWN")))return invalid();
            Map<String,Object> receipt=new LinkedHashMap<>(); receipt.put("status","REPORTED"); receipt.put("classification",kind);
            if(hasResult)receipt.put("result",result); if(hasError)receipt.put("error",error); receipt.put("effects",effects);
            return copied(receipt);
        } catch(RuntimeException failure) { return invalid(); }
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> copied(Map<String,Object> value) { return (Map<String,Object>)EffectJson.copy(value); }
    private static Map<String,Object> invalid() { return Map.of("status","NOT_REPORTED","code","OUTCOME_INVALID"); }
    private static String state(Object effect) { return (String)((Map<?,?>)effect).get("state"); }
    private static boolean priorConfirmation(Object value,Map<?,?> outcome) {
        Map<?,?> effect=(Map<?,?>)value,lastDispatch=Map.of(),lastStep=Map.of();
        int reached=0,confirmed=-1,dispatched=-1,index=0;
        for(Object item:(List<?>)outcome.get("trace")) {
            Map<?,?> event=(Map<?,?>)item;
            if(event.get("event").equals("STEP_START")) { reached++; lastStep=event; }
            if(event.get("event").equals("EFFECT_RECORDED") && Objects.equals(event.get("step"),effect.get("step")) &&
                Objects.equals(event.get("capability"),effect.get("capability")) && event.get("state").equals("CONFIRMED"))confirmed=index;
            if(event.get("event").equals("CALL_DISPATCHED")) { lastDispatch=event; dispatched=index; }
            index++;
        }
        return confirmed>=0 && confirmed<dispatched && Objects.equals(((Map<?,?>)outcome.get("error")).get("path"),"/steps/"+(reached-1)) &&
            Objects.equals(lastDispatch.get("step"),lastStep.get("step")) && !Objects.equals(lastDispatch.get("step"),effect.get("step")) &&
            !Objects.equals(lastDispatch.get("capability"),effect.get("capability"));
    }
    private static boolean shape(Object value,Set<String> required,Set<String> optional) {
        if(!(value instanceof Map<?,?> map) || !map.keySet().containsAll(required))return false;
        Set<String> allowed=new HashSet<>(required); allowed.addAll(optional);
        return allowed.containsAll(map.keySet());
    }
    private static boolean text(Object value,int max,boolean nonempty) {
        if(!(value instanceof String text) || nonempty && text.isEmpty())return false;
        EffectJson.unicode(text); return text.getBytes(StandardCharsets.UTF_8).length<=max;
    }
    private static boolean member(Set<String> values,Object value) { return value instanceof String text && values.contains(text); }
    private static boolean result(Object value) { return shape(value,Set.of("status"),Set.of()) && member(RESULTS,((Map<?,?>)value).get("status")); }
    private static boolean effect(Object value) {
        return shape(value,Set.of("step","capability","state"),Set.of()) && text(((Map<?,?>)value).get("step"),256,true) &&
            text(((Map<?,?>)value).get("capability"),256,true) && member(STATES,((Map<?,?>)value).get("state"));
    }
    private static boolean error(Object value,boolean causeAllowed) {
        if(!shape(value,Set.of("code","path"),causeAllowed?Set.of("cause"):Set.of()))return false;
        Map<?,?> error=(Map<?,?>)value;
        return text(error.get("code"),256,true) && text(error.get("path"),1024,false) &&
            (!error.containsKey("cause") || error(error.get("cause"),false));
    }
    static boolean trace(Object value) {
        if(!(value instanceof List<?> trace) || trace.size()<2 || trace.size()>64)return false;
        for(int i=0;i<trace.size();i++) {
            if(!(trace.get(i) instanceof Map<?,?> event) || !(event.get("event") instanceof String name) || !TRACE_FIELDS.containsKey(name))return false;
            Set<String> fields=new HashSet<>(TRACE_FIELDS.get(name)); fields.add("sequence"); fields.add("event");
            if(!shape(event,fields,name.equals("TERMINATED")?Set.of("code"):Set.of()) ||
                !(event.get("sequence") instanceof Integer || event.get("sequence") instanceof Long) || ((Number)event.get("sequence")).longValue()!=i)return false;
            if(name.equals("INVOCATION_START") != (i==0) || name.equals("TERMINATED") != (i==trace.size()-1))return false;
            if(event.containsKey("step") && !text(event.get("step"),256,true) || event.containsKey("capability") && !text(event.get("capability"),256,true))return false;
            if(event.containsKey("state") && !member(STATES,event.get("state")) || event.containsKey("taken") && !(event.get("taken") instanceof Boolean))return false;
            if(event.containsKey("code") && !text(event.get("code"),256,true))return false;
        }
        return true;
    }
}
