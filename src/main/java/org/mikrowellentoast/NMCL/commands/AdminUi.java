package org.mikrowellentoast.NMCL.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Locale;

final class AdminUi {
    private static final String RULE = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private AdminUi() {}

    static void header(CommandSender sender, String title) {
        sender.sendMessage(Component.text("━━ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("NMCL  •  " + title, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" ━━", NamedTextColor.DARK_GRAY)));
    }

    static void footer(CommandSender sender) { sender.sendMessage(Component.text(RULE, NamedTextColor.DARK_GRAY)); }

    static void section(CommandSender sender, String title) {
        sender.sendMessage(Component.text("  " + title, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
    }

    static void row(CommandSender sender, String label, String value) { row(sender, label, value, NamedTextColor.WHITE); }

    static void row(CommandSender sender, String label, String value, NamedTextColor color) {
        sender.sendMessage(Component.text("  " + label + "  ", NamedTextColor.GRAY)
                .append(Component.text(value, color)));
    }

    static void entry(CommandSender sender, String name, String detail) {
        sender.sendMessage(Component.text("  • ", NamedTextColor.DARK_GRAY)
                .append(Component.text(name, NamedTextColor.AQUA))
                .append(Component.text("  " + detail, NamedTextColor.GRAY)));
    }

    static String yesNo(boolean value) { return value ? "Yes" : "No"; }
    static String name(Enum<?> value) {
        String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString().replace("Pvp", "PvP").replace("Api", "API");
    }
}
