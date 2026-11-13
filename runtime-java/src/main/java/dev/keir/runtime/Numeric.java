package dev.keir.runtime;

import java.math.BigInteger;
import java.util.Set;

/** Internal exact numeric values. No model/artifact admission or shared value cache. */
final class Numeric {
    private static final BigInteger TEN = BigInteger.TEN;
    private static final Set<String> MODES = Set.of("UNNECESSARY", "DOWN", "FLOOR", "CEILING", "HALF_UP", "HALF_EVEN");

    private Numeric() {}

    record Decimal(BigInteger coefficient, int scale) {
        Decimal {
            require(coefficient != null && scale >= 0 && scale <= 18);
            require(coefficient.abs().toString().length() <= 38);
            require(scale == 0 || coefficient.remainder(TEN).signum() != 0);
        }
    }

    private static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException("Invalid numeric value or operation.");
    }

    private static BigInteger power(int exponent) {
        require(exponent >= 0 && exponent <= 36);
        return TEN.pow(exponent);
    }

    private static Decimal value(BigInteger coefficient, int scale) {
        if (coefficient.signum() == 0) return new Decimal(BigInteger.ZERO, 0);
        while (scale > 0 && coefficient.remainder(TEN).signum() == 0) {
            coefficient = coefficient.divide(TEN); scale--;
        }
        return new Decimal(coefficient, scale);
    }

    private static Decimal parse(Object raw, boolean normalize) {
        require(raw instanceof String);
        String text = (String)raw;
        require(text.length() <= (normalize ? 256 : 40));
        require(text.matches(normalize ? "[+-]?[0-9]+(?:\\.[0-9]+)?" : "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]*[1-9])?"));
        boolean negative = text.startsWith("-");
        String unsigned = text.substring(negative || text.startsWith("+") ? 1 : 0);
        int point = unsigned.indexOf('.');
        String whole = point < 0 ? unsigned : unsigned.substring(0, point);
        String fraction = point < 0 ? "" : unsigned.substring(point + 1);
        fraction = fraction.replaceFirst("0+$", "");
        String digits = (whole + fraction).replaceFirst("^0+", "");
        if (digits.isEmpty()) digits = "0";
        require(digits.length() <= 38 && fraction.length() <= 18);
        Decimal result = value(new BigInteger((negative ? "-" : "") + digits), fraction.length());
        if (!normalize) require(text(result).equals(text));
        return result;
    }

    static Decimal decimal(Object raw) { return parse(raw, false); }

    static String normalizeDecimal(Object raw) { return text(parse(raw, true)); }

    static String normalizeInteger(Object raw) {
        Decimal value = parse(raw, true);
        return Long.toString(toInteger(value));
    }

    static long integer(Object raw) {
        require(raw instanceof String);
        String text = (String)raw;
        require(text.length() <= 20 && text.matches("0|-?[1-9][0-9]*"));
        return integer(new BigInteger(text));
    }

    private static long integer(BigInteger value) {
        require(value.bitLength() <= 63);
        return value.longValue();
    }

    static String text(Decimal value) {
        String digits = value.coefficient.abs().toString();
        digits = "0".repeat(Math.max(0, value.scale + 1 - digits.length())) + digits;
        return (value.coefficient.signum() < 0 ? "-" : "") + (value.scale == 0 ? digits
                : digits.substring(0, digits.length() - value.scale) + "." + digits.substring(digits.length() - value.scale));
    }

    static Decimal fromInteger(long value) { return value(BigInteger.valueOf(value), 0); }

    static long toInteger(Decimal value) {
        require(value.scale == 0);
        return integer(value.coefficient);
    }

    static long add(long a, long b) { return integer(BigInteger.valueOf(a).add(BigInteger.valueOf(b))); }
    static long subtract(long a, long b) { return integer(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b))); }
    static long multiply(long a, long b) { return integer(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b))); }
    static long negate(long a) { return integer(BigInteger.valueOf(a).negate()); }
    static long divide(long a, long b) {
        return integer(quotient(BigInteger.valueOf(a), BigInteger.valueOf(b), "UNNECESSARY"));
    }

    static Decimal add(Decimal a, Decimal b) {
        int scale = Math.max(a.scale, b.scale);
        return value(a.coefficient.multiply(power(scale - a.scale)).add(b.coefficient.multiply(power(scale - b.scale))), scale);
    }

    static Decimal subtract(Decimal a, Decimal b) { return add(a, negate(b)); }
    static Decimal negate(Decimal a) { return value(a.coefficient.negate(), a.scale); }
    static Decimal multiply(Decimal a, Decimal b) { return value(a.coefficient.multiply(b.coefficient), a.scale + b.scale); }
    static Decimal divide(Decimal a, Decimal b) {
        return value(quotient(a.coefficient.multiply(power(18 + b.scale - a.scale)), b.coefficient, "UNNECESSARY"), 18);
    }

    static int compare(Decimal a, Decimal b) {
        int scale = Math.max(a.scale, b.scale);
        return a.coefficient.multiply(power(scale - a.scale)).compareTo(b.coefficient.multiply(power(scale - b.scale)));
    }

    private static void rounding(int scale, String mode) {
        require(scale >= 0 && scale <= 18 && mode != null && MODES.contains(mode));
    }

    static Decimal round(Decimal a, int scale, String mode) {
        rounding(scale, mode);
        return a.scale <= scale ? a : value(quotient(a.coefficient, power(a.scale - scale), mode), scale);
    }

    static Decimal multiplyRounded(Decimal a, Decimal b, int scale, String mode) {
        rounding(scale, mode);
        BigInteger product = a.coefficient.multiply(b.coefficient);
        int productScale = a.scale + b.scale;
        return productScale <= scale ? value(product, productScale) : value(quotient(product, power(productScale - scale), mode), scale);
    }

    static Decimal divideRounded(Decimal a, Decimal b, int scale, String mode) {
        rounding(scale, mode);
        int exponent = scale + b.scale - a.scale;
        return value(quotient(a.coefficient.multiply(power(Math.max(0, exponent))),
                b.coefficient.multiply(power(Math.max(0, -exponent))), mode), scale);
    }

    private static BigInteger quotient(BigInteger numerator, BigInteger denominator, String mode) {
        require(denominator.signum() != 0);
        if (denominator.signum() < 0) { numerator = numerator.negate(); denominator = denominator.negate(); }
        BigInteger[] parts = numerator.abs().divideAndRemainder(denominator);
        BigInteger quotient = parts[0], remainder = parts[1];
        if (mode.equals("UNNECESSARY")) require(remainder.signum() == 0);
        int half = remainder.shiftLeft(1).compareTo(denominator);
        boolean up = remainder.signum() != 0 && (mode.equals("FLOOR") && numerator.signum() < 0
                || mode.equals("CEILING") && numerator.signum() > 0
                || (mode.equals("HALF_UP") || mode.equals("HALF_EVEN")) && (half > 0
                || half == 0 && (mode.equals("HALF_UP") || quotient.testBit(0))));
        if (up) quotient = quotient.add(BigInteger.ONE);
        return numerator.signum() < 0 ? quotient.negate() : quotient;
    }
}
