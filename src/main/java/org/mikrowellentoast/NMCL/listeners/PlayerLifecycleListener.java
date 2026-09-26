package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.combat.GracePeriodManager;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.events.PlayerCombatLogEvent;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.punishment.PunishmentManager;

import java.util.Map;

public final class PlayerLifecycleListener implements Listener {
    private final NoMoreCombatLog plugin;
    private final ConfigManager config;
    private final CombatManager combat;
    private final GracePeriodManager grace;
    private final PunishmentManager punishments;
    private final MessageManager messages;

    public PlayerLifecycleListener(NoMoreCombatLog plugin, ConfigManager config, CombatManager combat,
                                   GracePeriodManager grace, PunishmentManager punishments, MessageManager messages) {
        this.plugin = plugin; this.config = config; this.combat = combat; this.grace = grace;
        this.punishments = punishments; this.messages = messages;
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        grace.protect(event.getPlayer().getUniqueId(), config.settings().combat().gracePeriod().join());
        if (event.getPlayer().isOp() && plugin.hasUpdate()) {
            messages.send(event.getPlayer(), "update.available", Map.of("version", plugin.getUpdateAvailable()));
        }
    }

    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        combat.untagPlayer(event.getPlayer().getUniqueId());
        grace.protect(event.getPlayer().getUniqueId(), config.settings().combat().gracePeriod().respawn());
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) { combat.untagPlayer(event.getPlayer().getUniqueId()); }

    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) {
        if (config.isWorldDisabled(event.getPlayer().getWorld().getName())) combat.untagPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        grace.clear(event.getPlayer().getUniqueId());
        if (plugin.isShuttingDown() || !config.settings().enabled()
                || config.isWorldDisabled(event.getPlayer().getWorld().getName())
                || event.getPlayer().hasPermission("nomorecombatlog.bypass")) {
            combat.untagPlayer(event.getPlayer().getUniqueId());
            return;
        }
        combat.getTag(event.getPlayer().getUniqueId()).ifPresent(tag -> {
            Bukkit.getPluginManager().callEvent(new PlayerCombatLogEvent(event.getPlayer(), tag));
            punishments.punish(event.getPlayer(), tag);
            combat.untagPlayer(event.getPlayer().getUniqueId());
        });
    }
}
