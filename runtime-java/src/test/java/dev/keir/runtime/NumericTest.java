package dev.keir.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class NumericTest {
    @Test void independentSharedVectors() throws Exception {
        Map<String,Object> vectors = map(parseJson(Files.readAllBytes(Path.of("..", "conformance", "numeric-values.json")), 65_536), "");
        for (Object raw : list(vectors.get("normalization"), "")) {
            Map<String,Object> row = map(raw, "");
            Object input = row.containsKey("repeat") ? ((String)row.get("repeat")).repeat(((Number)row.get("count")).intValue()) : row.get("input");
            check(row, () -> {
                boolean integer = row.get("kind").equals("integer");
                String result = integer ? Numeric.normalizeInteger(input) : Numeric.normalizeDecimal(input);
                assertEquals(result, integer ? KeirRuntime.normalizeInteger(result) : KeirRuntime.normalizeDecimal(result));
                return result;
            });
        }
        for (Object raw : list(vectors.get("operations"), "")) {
            Map<String,Object> row = map(raw, "");
            check(row, () -> operation(row));
        }
    }

    private static void check(Map<String,Object> row, Supplier<String> action) {
        String id = (String)row.getOrDefault("id", row.get("kind"));
        if (row.get("expected").equals("ERROR")) {
            var error = assertThrows(IllegalArgumentException.class, action::get, id);
            assertEquals("Invalid numeric value or operation.", error.getMessage(), id);
        } else assertEquals(row.get("expected"), action.get(), id);
    }

    private static String operation(Map<String,Object> row) {
        List<Object> args = list(row.get("args"), "");
        String op = (String)row.get("op");
        if (row.get("kind").equals("integer")) {
            long a = Numeric.integer(args.get(0));
            if (op.equals("parse")) return Long.toString(a);
            if (op.equals("negate")) return Long.toString(Numeric.negate(a));
            long b = Numeric.integer(args.get(1));
            return Long.toString(switch (op) {
                case "add" -> Numeric.add(a, b);
                case "subtract" -> Numeric.subtract(a, b);
                case "multiply" -> Numeric.multiply(a, b);
                case "divide" -> Numeric.divide(a, b);
                default -> throw new AssertionError("Unknown vector operation");
            });
        }
        if (op.equals("fromInteger")) return Numeric.text(Numeric.fromInteger(Numeric.integer(args.get(0))));
        Numeric.Decimal a = Numeric.decimal(args.get(0));
        if (op.equals("parse")) return Numeric.text(a);
        if (op.equals("toInteger")) return Long.toString(Numeric.toInteger(a));
        if (op.equals("negate")) return Numeric.text(Numeric.negate(a));
        if (op.equals("round")) return Numeric.text(Numeric.round(a, ((Number)args.get(1)).intValue(), (String)args.get(2)));
        Numeric.Decimal b = Numeric.decimal(args.get(1));
        if (op.equals("compare")) return Integer.toString(Numeric.compare(a, b));
        return Numeric.text(switch (op) {
            case "add" -> Numeric.add(a, b);
            case "subtract" -> Numeric.subtract(a, b);
            case "multiply" -> Numeric.multiply(a, b);
            case "divide" -> Numeric.divide(a, b);
            case "multiplyRounded" -> Numeric.multiplyRounded(a, b, ((Number)args.get(2)).intValue(), (String)args.get(3));
            case "divideRounded" -> Numeric.divideRounded(a, b, ((Number)args.get(2)).intValue(), (String)args.get(3));
            default -> throw new AssertionError("Unknown vector operation");
        });
    }

    @Test void rootHelpersAreOptInAndDoNotChangeCanonicalData() {
        assertEquals("100", KeirRuntime.normalizeInteger("+00100.00"));
        assertEquals("0", KeirRuntime.normalizeDecimal("-000.000"));
        assertThrows(IllegalArgumentException.class, () -> KeirRuntime.normalizeInteger(null));
        assertThrows(IllegalArgumentException.class, () -> KeirRuntime.normalizeDecimal("private-invalid-input"));
        assertThrows(IllegalArgumentException.class, () -> Numeric.integer("100.00"));
        assertThrows(IllegalArgumentException.class, () -> Numeric.decimal("100.00"));
        assertThrows(RuntimeFault.class, () -> RuntimeData.canonical(-1L));
        assertThrows(RuntimeFault.class, () -> RuntimeData.canonical(1.5));
    }

    @Test void immutableIntermediateValuesSurviveFailureAndReuse() {
        Numeric.Decimal a = Numeric.decimal("1.25"), b = Numeric.decimal("2");
        Numeric.Decimal intermediate = Numeric.multiply(a, b);
        assertThrows(IllegalArgumentException.class, () -> Numeric.divide(intermediate, Numeric.decimal("0")));
        assertEquals("3.75", Numeric.text(Numeric.add(intermediate, a)));
        assertEquals("2.5", Numeric.text(intermediate));
        assertEquals("1.25", Numeric.text(a));
        assertEquals("2", Numeric.text(b));
    }
}
