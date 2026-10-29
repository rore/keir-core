package dev.keir.compiler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.*;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Bounded data decoding only: semantic shape validation belongs to the compiler. */
final class StrictData {
    static final int MAX_BYTES = 65_536;
    static final int MAX_DEPTH = 16;
    // Every node/key requires at least one source byte; reject before allocating its value.
    private static final int MAX_NODES = MAX_BYTES;
    private static final long MAX_INTEGER = 9_007_199_254_740_991L;
    private static final Pattern NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    private static final JsonFactory JSON = JsonFactory.builder().streamReadConstraints(
            StreamReadConstraints.builder().maxNestingDepth(MAX_DEPTH)
                    .maxStringLength(MAX_BYTES).maxNameLength(MAX_BYTES)
                    .maxNumberLength(MAX_BYTES).maxDocumentLength(MAX_BYTES).build()).build();

    private StrictData() {}

    static Object parse(byte[] bytes, String root) {
        if (bytes.length > MAX_BYTES) throw failure("BUDGET_EXCEEDED", root);
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            validString(text, root);
            if (text.startsWith("\ufeff")) throw failure("SYNTAX_INVALID", root);
            try (JsonParser parser = JSON.createParser(text)) {
                JsonToken first = parser.nextToken();
                if (first == null) throw failure("SYNTAX_INVALID", root);
                Object value = json(parser, first, 0, new Budget(root));
                if (parser.nextToken() != null) throw failure("SYNTAX_INVALID", root);
                return value;
            } catch (JsonParseException invalidJson) {
                return yaml(text, root);
            }
        } catch (StreamConstraintsException bounded) {
            throw failure("BUDGET_EXCEEDED", root);
        } catch (CharacterCodingException malformed) {
            throw failure("SYNTAX_INVALID", root);
        } catch (IOException | IllegalArgumentException malformed) {
            throw failure("SYNTAX_INVALID", root);
        }
    }

    private static Object json(JsonParser p, JsonToken token, int depth, Budget budget) throws IOException {
        budget.node();
        if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
            budget.depth(depth + 1);
            if (token == JsonToken.START_ARRAY) {
                List<Object> values = new ArrayList<>();
                JsonToken next;
                while ((next = p.nextToken()) != JsonToken.END_ARRAY) values.add(json(p, next, depth + 1, budget));
                return values;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                if (p.currentToken() != JsonToken.FIELD_NAME) throw failure("SYNTAX_INVALID", budget.root);
                budget.node();
                String key = p.currentName();
                validString(key, budget.root);
                if (values.containsKey(key)) throw failure("SYNTAX_INVALID", budget.root);
                values.put(key, json(p, p.nextToken(), depth + 1, budget));
            }
            return values;
        }
        if (token == JsonToken.VALUE_STRING) {
            String value = p.getText();
            validString(value, budget.root);
            return value;
        }
        if (token == JsonToken.VALUE_TRUE) return true;
        if (token == JsonToken.VALUE_FALSE) return false;
        if (token == JsonToken.VALUE_NULL) return null;
        if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) return number(p.getText());
        throw failure("SYNTAX_INVALID", budget.root);
    }

    private static Object yaml(String text, String root) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(0);
        options.setNestingDepthLimit(MAX_DEPTH);
        options.setCodePointLimit(MAX_BYTES);
        try {
            Iterator<Event> events = new Yaml(options).parse(new StringReader(text)).iterator();
            if (!(events.next() instanceof StreamStartEvent)) throw failure("SYNTAX_INVALID", root);
            Event start = events.next();
            if (!(start instanceof DocumentStartEvent document)
                    || (document.getTags() != null && !document.getTags().isEmpty())) throw failure("SYNTAX_INVALID", root);
            Object value = yamlValue(events, events.next(), 0, new Budget(root));
            if (!(events.next() instanceof DocumentEndEvent) || !(events.next() instanceof StreamEndEvent)
                    || events.hasNext()) throw failure("SYNTAX_INVALID", root);
            return value;
        } catch (CompileFailure failure) {
            throw failure;
        } catch (RuntimeException malformed) {
            throw failure("SYNTAX_INVALID", root);
        }
    }

    private static Object yamlValue(Iterator<Event> events, Event event, int depth, Budget budget) {
        budget.node();
        if (event instanceof NodeEvent node && node.getAnchor() != null) throw failure("SYNTAX_INVALID", budget.root);
        if (event instanceof ScalarEvent scalar) {
            if (scalar.getTag() != null) throw failure("SYNTAX_INVALID", budget.root);
            String value = scalar.getValue();
            validString(value, budget.root);
            if (!scalar.isPlain()) return value;
            return switch (value) {
                case "true" -> true;
                case "false" -> false;
                case "null" -> null;
                case "" -> throw failure("SYNTAX_INVALID", budget.root);
                default -> NUMBER.matcher(value).matches() ? number(value) : value;
            };
        }
        if (event instanceof CollectionStartEvent collection) {
            if (collection.getTag() != null) throw failure("SYNTAX_INVALID", budget.root);
            budget.depth(depth + 1);
            if (event instanceof SequenceStartEvent) {
                List<Object> values = new ArrayList<>();
                Event next;
                while (!((next = events.next()) instanceof SequenceEndEvent)) values.add(yamlValue(events, next, depth + 1, budget));
                return values;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            Event next;
            while (!((next = events.next()) instanceof MappingEndEvent)) {
                Object keyValue = yamlValue(events, next, depth + 1, budget);
                if (!(keyValue instanceof String key) || key.equals("<<") || values.containsKey(key)) throw failure("SYNTAX_INVALID", budget.root);
                values.put(key, yamlValue(events, events.next(), depth + 1, budget));
            }
            return values;
        }
        throw failure("SYNTAX_INVALID", budget.root);
    }

    private static Number number(String text) {
        if (text.equals("-0") || text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0) return new BigDecimal(text);
        BigInteger integer = new BigInteger(text);
        return integer.bitLength() < 64 ? integer.longValue() : integer;
    }

    static byte[] canonical(Object json) {
        StringBuilder out = new StringBuilder();
        canonical(json, out, 0);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void canonical(Object value, StringBuilder out, int depth) {
        if (value instanceof String string) {
            string(string, out);
        } else if (value instanceof Boolean bool) {
            out.append(bool);
        } else if (value instanceof Long integer && integer >= 0 && integer <= MAX_INTEGER) {
            out.append(integer);
        } else if (value instanceof Map<?, ?> map) {
            if (depth >= MAX_DEPTH) throw failure("BUDGET_EXCEEDED", "/source");
            List<String> keys = new ArrayList<>();
            for (Object key : map.keySet()) {
                if (!(key instanceof String text)) throw failure("SYNTAX_INVALID", "/source");
                validString(text, "/source");
                keys.add(text);
            }
            keys.sort(SCALAR_ORDER);
            out.append('{');
            boolean comma = false;
            for (String key : keys) {
                if (comma) out.append(',');
                comma = true;
                string(key, out);
                out.append(':');
                canonical(map.get(key), out, depth + 1);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            if (depth >= MAX_DEPTH) throw failure("BUDGET_EXCEEDED", "/source");
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                canonical(list.get(i), out, depth + 1);
            }
            out.append(']');
        } else {
            throw failure("SYNTAX_INVALID", "/source");
        }
    }

    private static final Comparator<String> SCALAR_ORDER = (left, right) -> {
        int a = 0, b = 0;
        while (a < left.length() && b < right.length()) {
            int x = left.codePointAt(a), y = right.codePointAt(b);
            if (x != y) return Integer.compare(x, y);
            a += Character.charCount(x);
            b += Character.charCount(y);
        }
        return Integer.compare(left.length() - a, right.length() - b);
    };

    private static void string(String value, StringBuilder out) {
        validString(value, "/source");
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20) out.append("\\u00").append("0123456789abcdef".charAt(c >>> 4)).append("0123456789abcdef".charAt(c & 15));
            else out.append(c);
        }
        out.append('"');
    }

    private static void validString(String value, String root) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) throw failure("SYNTAX_INVALID", root);
            } else if (Character.isLowSurrogate(c)) throw failure("SYNTAX_INVALID", root);
        }
    }

    static String digest(Object json) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(json)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static CompileFailure failure(String code, String root) {
        return new CompileFailure(code, root);
    }

    private static final class Budget {
        final String root;
        int nodes;
        Budget(String root) { this.root = root; }
        void node() { if (++nodes > MAX_NODES) throw failure("BUDGET_EXCEEDED", root); }
        void depth(int depth) { if (depth > MAX_DEPTH) throw failure("BUDGET_EXCEEDED", root); }
    }
}
