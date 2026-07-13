package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;

import java.util.UUID;

public class PortalListener implements Listener {

    private final CombatListener combatListener;
    private final ConfigManager config = ConfigManager.getInstance();

    public PortalListener(CombatListener combatListener) {
        this.combatListener = combatListener;
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (combatListener.isCombatTagged(playerUUID) && !config.isAllowPortalInCombat()) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use a portal while combat tagged!");
        }

    }

}
