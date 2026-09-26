package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.config.CommandMode;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.util.CommandNormalizer;

public final class RestrictionListener implements Listener {
    private final ConfigManager config;
    private final CombatManager combat;
    private final MessageManager messages;
    public RestrictionListener(ConfigManager config, CombatManager combat, MessageManager messages) {
        this.config = config; this.combat = combat; this.messages = messages;
    }

    @EventHandler public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!restricted(player) || player.hasPermission("nomorecombatlog.command.bypass")) return;
        String command = CommandNormalizer.normalize(event.getMessage());
        boolean listed = config.settings().commands().commands().contains(command);
        boolean blocked = config.settings().commands().mode() == CommandMode.BLACKLIST ? listed : !listed;
        if (blocked) {
            event.setCancelled(true);
            messages.send(player, "commands.blocked");
        }
    }

    @EventHandler public void onPortal(PlayerPortalEvent event) {
        if (restricted(event.getPlayer()) && !config.settings().teleport().portals()) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "commands.teleport-blocked");
        }
    }

    @EventHandler public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && restricted(event.getPlayer())
                && !config.settings().teleport().enderPearls()) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "commands.teleport-blocked");
        }
    }

    private boolean restricted(Player player) {
        return config.settings().enabled() && !config.isWorldDisabled(player.getWorld().getName())
                && !player.hasPermission("nomorecombatlog.bypass") && combat.isTagged(player.getUniqueId());
    }
}
