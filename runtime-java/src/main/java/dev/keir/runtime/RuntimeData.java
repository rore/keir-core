package dev.keir.runtime;

import com.fasterxml.jackson.core.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

final class RuntimeData {
    private static final int MAX_BYTES = 1_048_576;
    private static final long MAX_INTEGER = 9_007_199_254_740_991L;
    private static final JsonFactory JSON = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(16)
                    .maxStringLength(MAX_BYTES).maxNumberLength(1024).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    private RuntimeData() {}

    static Object parseCanonical(byte[] bytes) {
        Object value = parseJson(bytes, MAX_BYTES);
        if (!Arrays.equals(bytes, canonical(value))) throw invalid();
        return value;
    }

    static Object parseJson(byte[] bytes, int max) {
        return parseJson(bytes, max, false);
    }

    static Object parseScenario(byte[] bytes) {
        return parseJson(bytes, 65_536, true);
    }

    private static Object parseJson(byte[] bytes, int max, boolean scenario) {
        if (bytes == null || max < 0 || bytes.length > max || bytes.length > MAX_BYTES) throw invalid();
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("\ufeff")) throw invalid();
            try (JsonParser parser = JSON.createParser(text)) {
                if (parser.nextToken() == null) throw invalid();
                Object result = read(parser, 1, new int[]{0}, scenario);
                if (parser.nextToken() != null) throw invalid();
                return result;
            }
        } catch (Exception error) { throw invalid(); }
    }

    private static Object read(JsonParser parser, int depth, int[] nodes, boolean scenario) throws java.io.IOException {
        if ((depth > 16 && (!scenario || parser.currentToken().isStructStart())) || ++nodes[0] > MAX_BYTES) throw invalid();
        return switch (parser.currentToken()) {
            case START_OBJECT -> {
                Map<String,Object> result = new LinkedHashMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (parser.currentToken() != JsonToken.FIELD_NAME) throw invalid();
                    String key = parser.currentName(); unicode(key);
                    parser.nextToken(); result.put(key, read(parser, depth + 1, nodes, scenario));
                }
                yield result;
            }
            case START_ARRAY -> {
                List<Object> result = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) result.add(read(parser, depth + 1, nodes, scenario));
                yield result;
            }
            case VALUE_STRING -> { String result = parser.getText(); unicode(result); yield result; }
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case VALUE_NUMBER_INT -> {
                String raw = parser.getText();
                BigInteger integer = new BigInteger(raw);
                if (raw.equals("-0") || integer.signum() < 0 || integer.compareTo(BigInteger.valueOf(MAX_INTEGER)) > 0) {
                    if (scenario) throw invalid();
                    yield new BigDecimal(raw);
                }
                yield integer.longValueExact();
            }
            case VALUE_NUMBER_FLOAT -> {
                if (scenario) throw invalid();
                yield new BigDecimal(parser.getText());
            }
            default -> throw invalid();
        };
    }

    static byte[] canonical(Object value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(value, 1, output, MAX_BYTES, false);
        return output.toByteArray();
    }

    static byte[] canonical(Object value, int maxBytes) {
        if (maxBytes < 0 || maxBytes > MAX_BYTES) throw invalid();
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        write(value, 1, output, maxBytes, true);
        return output.toByteArray();
    }

    private static void append(ByteArrayOutputStream output, String text, int maxBytes, boolean bounded) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ensureCapacity(output, bytes.length, maxBytes, bounded);
        output.writeBytes(bytes);
    }

    private static void ensureCapacity(ByteArrayOutputStream output, int count, int maxBytes, boolean bounded) {
        if (output.size() + count > maxBytes) {
            if (bounded) throw new RuntimeFault("BUDGET_EXCEEDED", "/artifact");
            throw invalid();
        }
    }

    private static void writeByte(ByteArrayOutputStream output, int value, int maxBytes, boolean bounded) {
        ensureCapacity(output, 1, maxBytes, bounded);
        output.write(value);
    }

    private static void writeCodePoint(ByteArrayOutputStream output, int codePoint, int maxBytes, boolean bounded) {
        int count = codePoint < 0x80 ? 1 : codePoint < 0x800 ? 2 : codePoint < 0x10000 ? 3 : 4;
        ensureCapacity(output, count, maxBytes, bounded);
        if (count == 1) output.write(codePoint);
        else if (count == 2) {
            output.write(0xc0 | codePoint >> 6); output.write(0x80 | codePoint & 0x3f);
        } else if (count == 3) {
            output.write(0xe0 | codePoint >> 12); output.write(0x80 | codePoint >> 6 & 0x3f); output.write(0x80 | codePoint & 0x3f);
        } else {
            output.write(0xf0 | codePoint >> 18); output.write(0x80 | codePoint >> 12 & 0x3f);
            output.write(0x80 | codePoint >> 6 & 0x3f); output.write(0x80 | codePoint & 0x3f);
        }
    }

    private static void write(Object value, int depth, ByteArrayOutputStream output, int maxBytes, boolean bounded) {
        if (depth > 16) throw invalid();
        if (value instanceof String text) { quoted(text, output, maxBytes, bounded); return; }
        if (value instanceof Boolean bool) { append(output, bool.toString(), maxBytes, bounded); return; }
        if (value instanceof Integer || value instanceof Long) {
            long number = ((Number)value).longValue();
            if (number < 0 || number > MAX_INTEGER) throw invalid();
            append(output, Long.toString(number), maxBytes, bounded); return;
        }
        if (value instanceof List<?> list) {
            append(output, "[", maxBytes, bounded);
            for (int i = 0; i < list.size(); i++) { if (i > 0) append(output, ",", maxBytes, bounded); write(list.get(i), depth + 1, output, maxBytes, bounded); }
            append(output, "]", maxBytes, bounded); return;
        }
        Map<String,Object> map = map(value, "/artifact");
        List<String> keys = new ArrayList<>(map.keySet());
        keys.sort((left, right) -> Arrays.compare(left.codePoints().toArray(), right.codePoints().toArray()));
        append(output, "{", maxBytes, bounded);
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) append(output, ",", maxBytes, bounded);
            String key = keys.get(i); quoted(key, output, maxBytes, bounded); append(output, ":", maxBytes, bounded); write(map.get(key), depth + 1, output, maxBytes, bounded);
        }
        append(output, "}", maxBytes, bounded);
    }

    private static void quoted(String text, ByteArrayOutputStream output, int maxBytes, boolean bounded) {
        unicode(text);
        writeByte(output, '"', maxBytes, bounded);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                writeByte(output, '\\', maxBytes, bounded); writeByte(output, c, maxBytes, bounded);
            } else if (c < 32) {
                writeByte(output, '\\', maxBytes, bounded); writeByte(output, 'u', maxBytes, bounded);
                for (int shift = 12; shift >= 0; shift -= 4) {
                    int digit = c >> shift & 0xf;
                    writeByte(output, digit < 10 ? '0' + digit : 'a' + digit - 10, maxBytes, bounded);
                }
            } else {
                int codePoint = text.codePointAt(i);
                if (Character.isHighSurrogate(c)) i++;
                writeCodePoint(output, codePoint, maxBytes, bounded);
            }
        }
        writeByte(output, '"', maxBytes, bounded);
    }

    static void unicode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) throw invalid();
            } else if (Character.isLowSurrogate(c)) throw invalid();
        }
    }

    static String digest(byte[] bytes) {
        try { return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    static Object immutable(Object value) {
        if (value instanceof Map<?,?>) {
            Map<String,Object> result = new LinkedHashMap<>();
            map(value, "/artifact").forEach((key,item) -> result.put(key, immutable(item)));
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            list.forEach(item -> result.add(immutable(item)));
            return Collections.unmodifiableList(result);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> map(Object value, String path) {
        if (!(value instanceof Map<?,?> map) || map.keySet().stream().anyMatch(key -> !(key instanceof String)))
            throw new RuntimeFault("ARTIFACT_INVALID", path);
        return (Map<String,Object>)value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object value, String path) {
        if (!(value instanceof List<?>)) throw new RuntimeFault("ARTIFACT_INVALID", path);
        return (List<Object>)value;
    }

    static void shape(Object value, Set<String> expected, String path) {
        if (!map(value, path).keySet().equals(expected)) throw new RuntimeFault("ARTIFACT_INVALID", path);
    }

    static String text(Object value, String path, boolean nonempty, int max) {
        if (!(value instanceof String text)) throw new RuntimeFault("ARTIFACT_INVALID", path);
        try { unicode(text); } catch (RuntimeFault error) { throw new RuntimeFault("ARTIFACT_INVALID", path); }
        if ((nonempty && text.isEmpty()) || text.getBytes(StandardCharsets.UTF_8).length > max)
            throw new RuntimeFault("ARTIFACT_INVALID", path);
        return text;
    }

    static long number(Object value, long min, long max, String path) {
        if (!(value instanceof Integer || value instanceof Long)) throw new RuntimeFault("ARTIFACT_INVALID", path);
        long result = ((Number)value).longValue();
        if (result < min || result > max || result > MAX_INTEGER) throw new RuntimeFault("ARTIFACT_INVALID", path);
        return result;
    }

    static String pointer(String path, String field) { return path + "/" + field.replace("~", "~0").replace("/", "~1"); }
    private static RuntimeFault invalid() { return new RuntimeFault("ARTIFACT_INVALID", "/artifact"); }
}
