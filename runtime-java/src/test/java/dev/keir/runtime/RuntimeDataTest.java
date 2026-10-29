package dev.keir.runtime;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeDataTest {
    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static void rejected(String text) { assertThrows(RuntimeFault.class, () -> RuntimeData.parseCanonical(bytes(text))); }

    @Test void canonicalVectorsAndUnicodeScalarOrdering() {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("b",true); value.put("a","x");
        assertArrayEquals(bytes("{\"a\":\"x\",\"b\":true}"),RuntimeData.canonical(value));
        assertEquals("sha256:92f770af21072f89a908e7f233469b01c9c52af3e3bc8078efa1e41f655369e1",RuntimeData.digest(bytes("{\"a\":\"x\",\"b\":true}")));
        assertArrayEquals(bytes("\"\\u0000\\u000a\\\"\\\\é😀\""),RuntimeData.canonical("\0\n\"\\é😀"));
        assertArrayEquals(bytes("{\"\ue000\":false,\"😀\":true}"),RuntimeData.canonical(Map.of("😀",true,"\ue000",false)));
        assertEquals("é",RuntimeData.parseCanonical(bytes("\"é\"")));
    }

    @Test void rejectsEveryNoncanonicalRepresentation() {
        for (String value : List.of("null","-0","-1","1.0","1e0","9007199254740992","{\"b\":1,\"a\":2}",
                "{\"a\":1,\"a\":1}","true ","\ufefftrue","\"\\u0061\"","\"\\ud800\"","[] []","{\"x\":null}")) rejected(value);
        assertThrows(RuntimeFault.class,()->RuntimeData.parseCanonical(new byte[]{(byte)0xc3,0x28}));
        for (Object value : List.of(-1,1.0,Float.NaN,new java.math.BigInteger("1"),"\udc00"))
            assertThrows(RuntimeFault.class,()->RuntimeData.canonical(value));
        assertThrows(RuntimeFault.class,()->RuntimeData.canonical(null));
        assertEquals(9_007_199_254_740_991L,RuntimeData.parseCanonical(bytes("9007199254740991")));
    }

    @Test void rawTransportPreservesInvalidTypedValues() {
        Map<String,Object> raw = RuntimeData.map(RuntimeData.parseJson(bytes("{\"null\":null,\"float\":1.5,\"negativeZero\":-0}"),100),"/artifact");
        assertNull(raw.get("null")); assertInstanceOf(java.math.BigDecimal.class,raw.get("float"));
        assertInstanceOf(java.math.BigDecimal.class,raw.get("negativeZero"));
        assertThrows(RuntimeFault.class,()->RuntimeData.parseJson(bytes("true"),3));
    }

    @Test void boundsAreInclusiveAndSnapshotsDeep() {
        String atLimit = "x".repeat(1_048_574);
        assertEquals(1_048_576,RuntimeData.canonical(atLimit).length);
        assertThrows(RuntimeFault.class,()->RuntimeData.canonical(atLimit+"x"));
        assertThrows(RuntimeFault.class,()->RuntimeData.parseJson(new byte[1_048_577],Integer.MAX_VALUE));
        Object nested = true;
        for(int i=0;i<15;i++) nested=List.of(nested);
        RuntimeData.canonical(nested);
        Object tooDeep = List.of(nested);
        assertThrows(RuntimeFault.class,()->RuntimeData.canonical(tooDeep));
        rejected("[".repeat(16)+"true"+"]".repeat(16));
        Map<String,Object> source = new LinkedHashMap<>(); List<Object> list = new ArrayList<>(List.of(false,"")); source.put("values",list);
        Map<String,Object> snapshot = RuntimeData.map(RuntimeData.immutable(source),"/input"); list.clear();
        assertEquals(List.of(false,""),snapshot.get("values"));
        assertThrows(UnsupportedOperationException.class,()->snapshot.put("new",true));
        assertThrows(UnsupportedOperationException.class,()->RuntimeData.list(snapshot.get("values"),"/input").clear());
        assertEquals("/input/a~1b~0c",RuntimeData.pointer("/input","a/b~c"));
    }
}
