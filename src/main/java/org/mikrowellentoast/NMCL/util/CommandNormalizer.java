package org.mikrowellentoast.NMCL.util;

import java.util.Locale;

public final class CommandNormalizer {
    private CommandNormalizer() {
    }

    public static String normalize(String commandLine) {
        if (commandLine == null) return "";
        String value = commandLine.trim();
        while (value.startsWith("/")) value = value.substring(1);
        int space = value.indexOf(' ');
        if (space >= 0) value = value.substring(0, space);
        int namespace = value.indexOf(':');
        if (namespace >= 0 && namespace + 1 < value.length()) value = value.substring(namespace + 1);
        return value.toLowerCase(Locale.ROOT);
    }
}
