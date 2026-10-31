package dev.keir.runtime;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityPolicyTest {
    private static final CapabilityPolicy.State CATALOG = new CapabilityPolicy.State(Map.of("tasks.get",4L,"tasks.create",4L));
    private static CapabilityPolicy.State narrow(CapabilityPolicy.State parent,Object policy) {
        return CapabilityPolicy.narrow(parent,policy,"/policy","ADMISSION_INVALID");
    }
    private static void invalid(CapabilityPolicy.State parent,Object policy) {
        RuntimeFault failure = assertThrows(RuntimeFault.class,()->narrow(parent,policy));
        assertEquals("ADMISSION_INVALID",failure.code); assertEquals("/policy",failure.path);
        assertNull(failure.getCause());
    }

    @Test void omissionEmptyAndExplicitEmptyAllowHaveDifferentMeaning() {
        assertEquals(CATALOG,narrow(CATALOG,Map.of()));
        assertEquals(CATALOG,narrow(CATALOG,Map.of("deny",List.of(),"maxCalls",Map.of())));
        var empty = narrow(CATALOG,Map.of("allow",List.of()));
        assertEquals(Map.of("tasks.get",0L,"tasks.create",0L),empty.ceilings());
        assertFalse(CapabilityPolicy.permits(empty,"tasks.get",1));
        assertEquals(empty,narrow(empty,Map.of()));
        invalid(empty,Map.of("allow",List.of("tasks.get")));
        assertThrows(UnsupportedOperationException.class,()->empty.ceilings().clear());
    }

    @Test void everyLayerRetainsDenialsAndQuotasAndRejectsWidening() {
        var hook = narrow(CATALOG,Map.of("allow",List.of("tasks.get"),"maxCalls",Map.of("tasks.get",2L)));
        var admission = narrow(hook,Map.of("maxCalls",Map.of("tasks.get",1L)));
        assertEquals(admission,narrow(admission,Map.of("deny",List.of("tasks.create"))));
        assertTrue(CapabilityPolicy.permits(admission,"tasks.get",1));
        assertFalse(CapabilityPolicy.permits(admission,"tasks.get",2));
        invalid(admission,Map.of("maxCalls",Map.of("tasks.get",2L)));
        invalid(hook,Map.of("allow",List.of("tasks.get","tasks.create")));
        invalid(CATALOG,Map.of("allow",List.of("tasks.get"),"deny",List.of("tasks.get"),"maxCalls",Map.of("tasks.get",1L)));
        var denied = narrow(CATALOG,Map.of("allow",List.of("tasks.get"),"deny",List.of("tasks.get"),"maxCalls",Map.of("tasks.get",0L)));
        assertFalse(CapabilityPolicy.permits(denied,"tasks.get",1));
    }

    @Test void invalidPolicyValuesNeverBecomeOmissionOrPermission() {
        invalid(CATALOG,null);
        for (Object policy : List.of(Map.of("grants",Map.of()),Map.of("allow",List.of("missing")),
                Map.of("allow",List.of("tasks.get","tasks.get")),Map.of("deny",List.of("tasks.get","tasks.get")),
                Map.of("allow","tasks.get"),Map.of("maxCalls",Map.of("missing",0L)),
                Map.of("maxCalls",Map.of("tasks.get",5L)),Map.of("maxCalls",Map.of("tasks.get",-1L)),
                Map.of("maxCalls",Map.of("tasks.get",1.0)),Map.of("maxCalls",Map.of("tasks.get",true)))) invalid(CATALOG,policy);
        for (String field : List.of("allow","deny","maxCalls")) {
            Map<String,Object> policy = new HashMap<>(); policy.put(field,null); invalid(CATALOG,policy);
        }
    }
}
