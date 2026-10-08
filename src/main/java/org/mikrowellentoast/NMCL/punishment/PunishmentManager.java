package org.mikrowellentoast.NMCL.punishment;

import org.bukkit.Bukkit;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatTag;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.util.DurationParser;

import java.time.Instant;
import java.util.Date;

public final class PunishmentManager {
    private final NoMoreCombatLog plugin;
    private final ConfigManager config;

    public PunishmentManager(NoMoreCombatLog plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void punish(Player player, CombatTag tag) {
        if (!config.settings().punishmentEnabled()) return;
        for (PunishmentDefinition action : config.settings().punishments().stream()
                .sorted(java.util.Comparator.comparingInt(this::priority)).toList()) {
            try {
                execute(player, tag, action);
                debug("punishment executed: " + action.type() + " for " + player.getName());
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Punishment " + action.type() + " failed for " + player.getName() + ": " + exception.getMessage());
            }
        }
    }

    private int priority(PunishmentDefinition action) {
        return action.type() == PunishmentType.DROP_INVENTORY || action.type() == PunishmentType.DROP_EXPERIENCE ? 0 : 1;
    }

    private void execute(Player player, CombatTag tag, PunishmentDefinition action) {
        switch (action.type()) {
            case KILL -> player.setHealth(0.0);
            case BAN -> player.ban(replace(action.reason(), player, tag), (Date) null, "NoMoreCombatLog", true);
            case TEMPBAN -> player.ban(replace(action.reason(), player, tag),
                    Date.from(Instant.now().plus(action.duration())), "NoMoreCombatLog", true);
            case COMMAND -> action.commands().forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replace(command, player, tag)));
            case DROP_INVENTORY -> {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                }
                player.getInventory().clear();
            }
            case DROP_EXPERIENCE -> {
                int experience = Math.max(0, player.getTotalExperience());
                if (experience > 0) player.getWorld().spawn(player.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(experience));
                player.setExp(0); player.setLevel(0); player.setTotalExperience(0);
            }
        }
    }

    private String replace(String text, Player player, CombatTag tag) {
        String opponent = tag.opponentId().map(id -> {
            String name = Bukkit.getOfflinePlayer(id).getName();
            return name == null ? id.toString() : name;
        }).orElse("Unknown");
        return text.replace("<player>", player.getName()).replace("<opponent>", opponent)
                .replace("<reason>", tag.reason().name()).replace("<world>", player.getWorld().getName())
                .replace("<duration>", DurationParser.format(tag.totalDuration()))
                .replace("<time>", DurationParser.format(tag.remaining(System.currentTimeMillis())));
    }

    private void debug(String message) {
        if (config.settings().debug()) plugin.getLogger().info("[Debug] " + message);
    }
}
