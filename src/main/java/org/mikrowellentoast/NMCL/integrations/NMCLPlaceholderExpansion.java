package org.mikrowellentoast.NMCL.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.util.DurationParser;

public final class NMCLPlaceholderExpansion extends PlaceholderExpansion {
    private final NoMoreCombatLog plugin;
    private final CombatManager combat;
    public NMCLPlaceholderExpansion(NoMoreCombatLog plugin, CombatManager combat) { this.plugin = plugin; this.combat = combat; }
    @Override public @NotNull String getIdentifier() { return "nmcl"; }
    @Override public @NotNull String getAuthor() { return String.join(", ", plugin.getPluginMeta().getAuthors()); }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        var tag = combat.getTag(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "in_combat" -> Boolean.toString(tag.isPresent());
            case "combat_time" -> DurationParser.format(combat.getRemainingTime(player.getUniqueId()));
            case "combat_time_seconds" -> Long.toString(Math.max(0, (combat.getRemainingTime(player.getUniqueId()).toMillis() + 999) / 1000));
            case "opponent" -> tag.flatMap(value -> value.opponentId().map(id -> {
                String name = Bukkit.getOfflinePlayer(id).getName();
                return name == null ? id.toString() : name;
            })).orElse("");
            case "reason" -> tag.map(value -> value.reason().name()).orElse("");
            default -> null;
        };
    }
}
