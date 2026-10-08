package org.mikrowellentoast.NMCL.util;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern PART = Pattern.compile("(\\d+)(ms|s|m|h|d)", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Optional<Duration> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.matches("\\d+")) {
            try {
                return Optional.of(Duration.ofSeconds(Long.parseLong(value)));
            } catch (ArithmeticException | NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        Matcher matcher = PART.matcher(value);
        long millis = 0;
        int end = 0;
        try {
            while (matcher.find()) {
                if (matcher.start() != end) {
                    return Optional.empty();
                }
                long amount = Long.parseLong(matcher.group(1));
                long multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                    case "ms" -> 1L;
                    case "s" -> 1_000L;
                    case "m" -> 60_000L;
                    case "h" -> 3_600_000L;
                    case "d" -> 86_400_000L;
                    default -> throw new IllegalStateException();
                };
                millis = Math.addExact(millis, Math.multiplyExact(amount, multiplier));
                end = matcher.end();
            }
        } catch (ArithmeticException | NumberFormatException ignored) {
            return Optional.empty();
        }
        return end == value.length() && end > 0 ? Optional.of(Duration.ofMillis(millis)) : Optional.empty();
    }

    public static String format(Duration duration) {
        long seconds = Math.max(0, (duration.toMillis() + 999) / 1_000);
        if (seconds % 86_400 == 0 && seconds >= 86_400) return (seconds / 86_400) + "d";
        if (seconds % 3_600 == 0 && seconds >= 3_600) return (seconds / 3_600) + "h";
        if (seconds % 60 == 0 && seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }
}
