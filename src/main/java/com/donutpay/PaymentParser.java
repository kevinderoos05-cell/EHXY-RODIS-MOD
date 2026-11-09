package com.donutpay;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the two DonutSMP payment messages:
 *   "NAME paid you $AMOUNT"   (you RECEIVED money)
 *   "You paid NAME $AMOUNT"   (you SENT money)
 * AMOUNT can be 1, 1,500, 500K, 1.5M, 5B, 2T ...
 */
public final class PaymentParser {
    private PaymentParser() {}

    public record Payment(boolean received, String name, String amountText, long amount) {}

    // Group 1 = whole amount text ($5B), 2 = number, 3 = suffix (K/M/B/T)
    private static final String AMOUNT = "(\\$\\s?(\\d[\\d,]*(?:\\.\\d+)?)\\s?([KkMmBbTt]?))";

    // The name may only contain letters, numbers, _ and . (Bedrock players start with a dot).
    // This also stops other players from faking a payment by typing "paid you $5B" in chat,
    // because normal chat looks like "<name> text" or "Name » text".
    private static final Pattern RECEIVED =
            Pattern.compile("^([.\\w]{1,32}) paid you " + AMOUNT + "\\s*$");
    private static final Pattern SENT =
            Pattern.compile("^You paid ([.\\w]{1,32}) " + AMOUNT + "\\s*$");

    public static Payment parse(String rawMessage) {
        String msg = rawMessage.strip();

        Matcher m = RECEIVED.matcher(msg);
        if (m.matches()) {
            return new Payment(true, m.group(1), m.group(2), toNumber(m.group(3), m.group(4)));
        }
        m = SENT.matcher(msg);
        if (m.matches()) {
            return new Payment(false, m.group(1), m.group(2), toNumber(m.group(3), m.group(4)));
        }
        return null;
    }

    private static long toNumber(String number, String suffix) {
        long multiplier = switch (suffix.toUpperCase()) {
            case "K" -> 1_000L;
            case "M" -> 1_000_000L;
            case "B" -> 1_000_000_000L;
            case "T" -> 1_000_000_000_000L;
            default -> 1L;
        };
        return new BigDecimal(number.replace(",", ""))
                .multiply(BigDecimal.valueOf(multiplier))
                .longValue();
    }
}
