package dev.keir.runtime;

import java.util.*;
import static dev.keir.runtime.RuntimeData.*;

/** Each layer can only remove catalog permission or reduce a call ceiling. */
final class CapabilityPolicy {
    private CapabilityPolicy() {}

    record State(Map<String,Long> ceilings) {
        State { ceilings = Collections.unmodifiableMap(new TreeMap<>(ceilings)); }
    }

    static State catalog(Map<String,Object> dcm) {
        Map<String,Long> ceilings = new TreeMap<>();
        map(dcm.get("capabilities"), "/artifact/dcm/capabilities").keySet()
                .forEach(id -> ceilings.put(id, (long)Profile.CALLS));
        return new State(ceilings);
    }

    static State narrow(State inherited, Object policy, String path, String invalidCode) {
        try {
            if (!(policy instanceof Map<?,?> raw) || raw.size()>3) throw new RuntimeFault(invalidCode,path);
            Map<String,Object> controls = map(policy, path);
            if (!Set.of("allow", "deny", "maxCalls").containsAll(controls.keySet())) throw new RuntimeFault(invalidCode,path);
            Set<String> catalog = inherited.ceilings().keySet();
            Set<String> allow = controls.containsKey("allow") ? ids(controls.get("allow"),catalog,path) : null;
            Set<String> deny = controls.containsKey("deny") ? ids(controls.get("deny"),catalog,path) : Set.of();
            Map<String,Long> result = new TreeMap<>(inherited.ceilings());
            if (allow != null) {
                for (String id : allow) if (inherited.ceilings().get(id) == 0) throw new RuntimeFault(invalidCode,path);
                result.replaceAll((id, count) -> allow.contains(id) ? count : 0L);
            }
            deny.forEach(id -> result.put(id,0L));
            if (controls.containsKey("maxCalls")) {
                if (!(controls.get("maxCalls") instanceof Map<?,?> rawQuotas) || rawQuotas.size()>8) throw new RuntimeFault(invalidCode,path);
                Map<String,Object> quotas = map(controls.get("maxCalls"),path);
                if (quotas.size() > 8 || !catalog.containsAll(quotas.keySet())) throw new RuntimeFault(invalidCode,path);
                for (String id : new TreeSet<>(quotas.keySet())) {
                    long count = number(quotas.get(id),0,Profile.CALLS,path);
                    if (count > result.get(id)) throw new RuntimeFault(invalidCode,path);
                    result.put(id,count);
                }
            }
            return new State(result);
        } catch (RuntimeException failure) { throw new RuntimeFault(invalidCode,path); }
    }

    private static Set<String> ids(Object value, Set<String> catalog, String path) {
        List<Object> list = list(value,path);
        if (list.size() > 8) throw new RuntimeFault("ARTIFACT_INVALID",path);
        Set<String> result = new TreeSet<>();
        for (Object id : list)
            if (!(id instanceof String text) || !catalog.contains(text) || !result.add(text))
                throw new RuntimeFault("ARTIFACT_INVALID",path);
        return result;
    }

    static boolean permits(State policy, String capability, int count) {
        return count <= policy.ceilings().getOrDefault(capability,0L);
    }

}
