package org.mikrowellentoast.NMCL.punishment;

import org.mikrowellentoast.NMCL.util.DurationParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PunishmentConfigParser {
    private PunishmentConfigParser() {}

    public static List<PunishmentDefinition> parse(List<Map<?, ?>> actions, Consumer<String> warning) {
        if (actions.isEmpty()) return List.of(new PunishmentDefinition(PunishmentType.KILL, Duration.ZERO, "Combat logging", List.of()));
        List<PunishmentDefinition> result = new ArrayList<>();
        for (int index = 0; index < actions.size(); index++) {
            Map<?, ?> section = actions.get(index);
            String path = "punishment.actions." + index;
            String rawType = String.valueOf(section.containsKey("type") ? section.get("type") : "");
            var type = PunishmentType.parse(rawType);
            if (type.isEmpty()) {
                warning.accept("Invalid value for '" + path + ".type' (" + rawType + "); action skipped.");
                continue;
            }
            Duration duration = Duration.ZERO;
            if (type.get() == PunishmentType.TEMPBAN) {
                Object raw = section.get("duration");
                duration = raw instanceof Number number ? Duration.ofSeconds(number.longValue())
                        : DurationParser.parse(raw == null ? null : raw.toString()).orElse(null);
                if (duration == null || duration.isZero() || duration.isNegative()) {
                    warning.accept("Invalid value for '" + path + ".duration' (" + raw + "); using 1d.");
                    duration = Duration.ofDays(1);
                }
            }
            List<String> commands = new ArrayList<>();
            if (section.get("commands") instanceof List<?> values) values.forEach(value -> commands.add(String.valueOf(value)));
            if (type.get() == PunishmentType.COMMAND && commands.isEmpty()) {
                warning.accept("Missing commands for '" + path + "'; action skipped.");
                continue;
            }
            result.add(new PunishmentDefinition(type.get(), duration,
                    String.valueOf(section.containsKey("reason") ? section.get("reason") : "Combat logging"), commands));
        }
        return List.copyOf(result);
    }
}
