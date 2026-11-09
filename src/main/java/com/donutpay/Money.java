package com.donutpay;

import java.util.Locale;

/** Turns numbers like 5000000000 into "5B" and back into pretty text. */
public final class Money {
    private Money() {}

    /** 5000000000 -> "5B", 1500000 -> "1.5M", 750 -> "750" (always positive, ignores sign). */
    public static String format(long value) {
        long a = Math.abs(value);
        if (a >= 1_000_000_000_000L) return trim(a / 1e12) + "T";
        if (a >= 1_000_000_000L) return trim(a / 1e9) + "B";
        if (a >= 1_000_000L) return trim(a / 1e6) + "M";
        if (a >= 1_000L) return trim(a / 1e3) + "K";
        return Long.toString(a);
    }

    /** 10B -> "+$10B", -20B -> "-$20B", 0 -> "$0". */
    public static String signed(long value) {
        String sign = value > 0 ? "+" : value < 0 ? "-" : "";
        return sign + "$" + format(value);
    }

    private static String trim(double x) {
        String s = String.format(Locale.ROOT, "%.2f", x);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
