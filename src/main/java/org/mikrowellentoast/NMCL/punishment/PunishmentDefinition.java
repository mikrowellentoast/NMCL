package org.mikrowellentoast.NMCL.punishment;

import java.time.Duration;
import java.util.List;

public record PunishmentDefinition(PunishmentType type, Duration duration, String reason, List<String> commands) {
    public PunishmentDefinition {
        commands = commands == null ? List.of() : List.copyOf(commands);
        reason = reason == null ? "Combat logging" : reason;
    }
}
