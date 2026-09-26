package org.mikrowellentoast.NMCL.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.safezone.SafeZone;
import org.mikrowellentoast.NMCL.safezone.SafeZoneManager;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

final class CommandSuggestions {
    private static final List<String> DURATIONS = List.of("10s", "30s", "1m", "5m", "30m", "1h");

    private CommandSuggestions() {}

    static SuggestionProvider<CommandSourceStack> players() {
        return (context, builder) -> {
            suggest(Bukkit.getOnlinePlayers().stream().map(Player::getName), builder);
            return builder.buildFuture();
        };
    }

    static SuggestionProvider<CommandSourceStack> zones(SafeZoneManager zones) {
        return (context, builder) -> {
            suggest(zones.zones().stream().map(SafeZone::name), builder);
            return builder.buildFuture();
        };
    }

    static SuggestionProvider<CommandSourceStack> durations() {
        return (context, builder) -> {
            suggest(DURATIONS.stream(), builder);
            return builder.buildFuture();
        };
    }

    private static void suggest(Stream<String> values, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        values.filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).forEach(builder::suggest);
    }
}
