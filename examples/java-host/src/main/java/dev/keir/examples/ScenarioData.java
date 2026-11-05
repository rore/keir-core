package dev.keir.examples;

import java.util.*;

final class ScenarioData {
    private ScenarioData() {}
    static String sha256(byte[] bytes) {
        try {
            return "sha256:" + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
    static void check(boolean valid,String message) { if(!valid) throw new IllegalArgumentException(message); }
    @SuppressWarnings("unchecked") static Map<String,Object> object(Object value) {
        check(value instanceof Map<?,?> map && map.keySet().stream().allMatch(key->key instanceof String),"Expected scenario record");
        return (Map<String,Object>)value;
    }
    @SuppressWarnings("unchecked") static List<Object> array(Object value) {
        check(value instanceof List<?>,"Expected scenario array"); return (List<Object>)value;
    }
    static void same(Object actual,Object expected,String message) { check(Arrays.equals(EffectJson.encode(actual),EffectJson.encode(expected)),message); }

    static List<Map<String,Object>> reconcile(List<Map<String,Object>> captures,List<Map<String,Object>> trace,List<Object> stubs,List<String> violations) {
        var dispatches=trace.stream().filter(event->"CALL_DISPATCHED".equals(event.get("event"))).toList();
        if(captures.size()!=dispatches.size()||captures.size()!=stubs.size()) violations.add("Dispatch/capture/stub count mismatch");
        var calls=new ArrayList<Map<String,Object>>();
        for(int i=0;i<captures.size();i++) {
            var call=captures.get(i); var event=i<dispatches.size()?dispatches.get(i):Map.<String,Object>of();
            if(!Objects.equals(event.get("capability"),call.get("capability"))) violations.add("Dispatch capability mismatch");
            if(i>=stubs.size()||!Objects.equals(event.get("step"),object(stubs.get(i)).get("step"))) violations.add("Dispatch step mismatch");
            var actual=new LinkedHashMap<String,Object>(call); if(event.containsKey("step")) actual.put("step",event.get("step")); calls.add(actual);
        }
        return calls;
    }
}
