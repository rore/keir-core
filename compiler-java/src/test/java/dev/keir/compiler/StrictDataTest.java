package dev.keir.compiler;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrictDataTest {
    private static Object parse(String text) {
        return StrictData.parse(text.getBytes(StandardCharsets.UTF_8), "/source");
    }

    private static void rejects(String text, String code) {
        CompileFailure failure = assertThrows(CompileFailure.class, () -> parse(text));
        assertEquals(code, failure.code);
        assertEquals("/source", failure.path);
        assertEquals(code, failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test void canonicalVectorsAndEquivalentYaml() {
        Object json = parse("{\"b\":true,\"a\":\"x\"}");
        assertArrayEquals("{\"a\":\"x\",\"b\":true}".getBytes(StandardCharsets.UTF_8), StrictData.canonical(json));
        assertEquals("sha256:92f770af21072f89a908e7f233469b01c9c52af3e3bc8078efa1e41f655369e1", StrictData.digest(json));
        assertArrayEquals(StrictData.canonical(json), StrictData.canonical(parse("b: true\na: x\n")));
        Object unicode = Map.of("text", "é😀", "control", "\0\t\n\u001f");
        assertArrayEquals("{\"control\":\"\\u0000\\u0009\\u000a\\u001f\",\"text\":\"é😀\"}".getBytes(StandardCharsets.UTF_8), StrictData.canonical(unicode));
        assertEquals("sha256:354bcf69477536aa8005e8480714cfe6bd8f277c7d6cb43500461a21491caadf", StrictData.digest(unicode));
        assertEquals("\"\\\"\\\\/\"", new String(StrictData.canonical("\"\\/"), StandardCharsets.UTF_8));
    }

    @Test void scalarOrderingAndNoUnicodeNormalization() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("😀", true);
        values.put("\ue000", false);
        assertEquals("{\"\ue000\":false,\"😀\":true}", new String(StrictData.canonical(values), StandardCharsets.UTF_8));
        assertNotEquals(StrictData.digest("é"), StrictData.digest("e\u0301"));
        assertEquals(List.of("b", "a"), parse("[\"b\",\"a\"]"));
    }

    @Test void retainsNumbersNullAndQuotedScalarsForSchemaValidation() {
        Map<?, ?> values = (Map<?, ?>) parse("a: 1\nb: 1.5\nc: null\nd: 'true'\ne: '1'\nf: false\ng: -2\nh: 9007199254740992\ni: 999999999999999999999999\n");
        assertEquals(1L, values.get("a"));
        assertEquals(new BigDecimal("1.5"), values.get("b"));
        assertTrue(values.containsKey("c"));
        assertNull(values.get("c"));
        assertEquals("true", values.get("d"));
        assertEquals("1", values.get("e"));
        assertEquals(false, values.get("f"));
        assertEquals(-2L, values.get("g"));
        assertEquals(9_007_199_254_740_992L, values.get("h"));
        assertEquals(new BigInteger("999999999999999999999999"), values.get("i"));
        assertEquals(values, parse("{\"a\":1,\"b\":1.5,\"c\":null,\"d\":\"true\",\"e\":\"1\",\"f\":false,\"g\":-2,\"h\":9007199254740992,\"i\":999999999999999999999999}"));
    }

    @Test void rejectsDuplicatesAndYamlGraphFeatures() {
        for (String invalid : List.of("{\"a\":null,\"a\":true}", "a: null\na: true", "a: &secret x", "a: *secret", "a: !!str x", "a: ! x", "!!map {}", "&a {}", "<<: {}", "'<<': {}", "---\na: x\n---\nb: y", "[a, b]: c", "%TAG !e! tag:example.com,2020:\n---\na: x")) {
            rejects(invalid, "SYNTAX_INVALID");
        }
    }

    @Test void rejectsMalformedUnicodeAtEveryBoundary() {
        for (byte[] bytes : List.of(new byte[]{(byte) 0xc0, (byte) 0xaf}, new byte[]{(byte) 0xed, (byte) 0xa0, (byte) 0x80}, new byte[]{(byte) 0xf4, (byte) 0x90, (byte) 0x80, (byte) 0x80}, new byte[]{(byte) 0xe2, (byte) 0x82})) {
            CompileFailure failure = assertThrows(CompileFailure.class, () -> StrictData.parse(bytes, "/dcm"));
            assertEquals("SYNTAX_INVALID", failure.code);
            assertEquals("/dcm", failure.path);
        }
        for (String text : List.of("\"\\ud800\"", "{\"\\udc00\":true}", "a: \"\\uD800\"", "\ufeff{}")) rejects(text, "SYNTAX_INVALID");
        assertEquals("😀", parse("\"\\ud83d\\ude00\""));
        assertThrows(CompileFailure.class, () -> StrictData.canonical("\ud800"));
        assertThrows(CompileFailure.class, () -> StrictData.canonical(Map.of("\udc00", true)));
    }

    @Test void enforcesInclusiveByteAndDepthBounds() {
        assertEquals("x".repeat(65_534), parse("\"" + "x".repeat(65_534) + "\""));
        rejects("\"" + "x".repeat(65_535) + "\"", "BUDGET_EXCEEDED");
        assertDoesNotThrow(() -> parse("[".repeat(16) + "true" + "]".repeat(16)));
        rejects("[".repeat(17) + "true" + "]".repeat(17), "BUDGET_EXCEEDED");
        String yaml16 = "a:\n" + java.util.stream.IntStream.range(1, 16).mapToObj(i -> " ".repeat(i) + "a:\n").collect(java.util.stream.Collectors.joining()) + " ".repeat(16) + "true";
        assertDoesNotThrow(() -> parse(yaml16));
        rejects("a:\n" + yaml16.indent(1), "BUDGET_EXCEEDED");
        assertDoesNotThrow(() -> parse("- x\n".repeat(16_384)));
        rejects("- x\n".repeat(16_384) + " ", "BUDGET_EXCEEDED");
    }

    @Test void canonicalOnlyAdmitsSafeUnsignedLongMetadata() {
        assertEquals("[0,9007199254740991]", new String(StrictData.canonical(List.of(0L, 9_007_199_254_740_991L)), StandardCharsets.UTF_8));
        List<Object> invalid = new ArrayList<>(Arrays.asList(null, -1L, 9_007_199_254_740_992L, 1.0, new BigDecimal("1.0"), new BigInteger("1"), 1));
        for (Object value : invalid) assertThrows(CompileFailure.class, () -> StrictData.canonical(value));
        assertThrows(CompileFailure.class, () -> StrictData.canonical(Map.of(1L, true)));
        for (String number : List.of("-0", "-0.0", "1.0", "1e0", "-1", "9007199254740992")) {
            assertThrows(CompileFailure.class, () -> StrictData.canonical(parse(number)));
            assertThrows(CompileFailure.class, () -> StrictData.canonical(((Map<?, ?>) parse("value: " + number)).get("value")));
        }
    }

    @Test void rejectsEmptyMalformedAndTrailingDocuments() {
        for (String text : List.of("", "   ", "{\"a\":", "{} {}", "\"x\" \"y\"", "a: [x", "a:", "-")) rejects(text, "SYNTAX_INVALID");
    }
}
