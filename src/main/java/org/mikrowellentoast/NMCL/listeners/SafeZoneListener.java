package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.safezone.SafeZone;
import org.mikrowellentoast.NMCL.safezone.SafeZoneManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SafeZoneListener implements Listener {
    private final ConfigManager config;
    private final SafeZoneManager zones;
    private final CombatManager combat;
    private final MessageManager messages;
    private final Map<UUID, String> current = new HashMap<>();
    public SafeZoneListener(ConfigManager config, SafeZoneManager zones, CombatManager combat, MessageManager messages) {
        this.config = config; this.zones = zones; this.combat = combat; this.messages = messages;
    }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        UUID id = event.getPlayer().getUniqueId();
        if (!config.settings().safeZones().enabled()) { current.remove(id); return; }
        SafeZone zone = zones.at(event.getTo()).orElse(null);
        String prior = current.get(id);
        if (zone == null) { current.remove(id); return; }
        current.put(id, zone.name());
        if (zone.name().equalsIgnoreCase(prior == null ? "" : prior)) return;
        if (zone.clearCombatOnEntry() || config.settings().safeZones().removeCombatOnEntry()) combat.untagPlayer(id);
        if (zone.showMessage()) messages.send(event.getPlayer(), "safe-zone.entered");
    }
}
