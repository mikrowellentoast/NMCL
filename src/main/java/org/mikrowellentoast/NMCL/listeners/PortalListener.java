package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;


import java.util.UUID;

public class PortalListener implements Listener {

    private final CombatListener combatListener;
    private final NoMoreCombatLog plugin = NoMoreCombatLog.getInstance();
    private boolean allow_portal_in_combat;

    public PortalListener(CombatListener combatListener) {
        this.combatListener = combatListener;
        this.allow_portal_in_combat = plugin.getConfig().getBoolean("allow-portal-teleport", true);
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (combatListener.isCombatTagged(playerUUID) && !allow_portal_in_combat) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use a portal while combat tagged!");
        }

    }

    @EventHandler
    public void onConfigReload(ConfigReloadEvent event) {
        this.allow_portal_in_combat = plugin.getConfig().getBoolean("allow-portal-teleport", true);
    }
}
