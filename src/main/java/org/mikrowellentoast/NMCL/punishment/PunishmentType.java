package org.mikrowellentoast.NMCL.punishment;

import java.util.Locale;
import java.util.Optional;

public enum PunishmentType {
    KILL, BAN, TEMPBAN, COMMAND, DROP_INVENTORY, DROP_EXPERIENCE;

    public static Optional<PunishmentType> parse(String input) {
        if (input == null) return Optional.empty();
        try { return Optional.of(valueOf(input.trim().toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }
}
