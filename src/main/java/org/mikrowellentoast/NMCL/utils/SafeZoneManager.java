package org.mikrowellentoast.NMCL.utils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;

import java.util.ArrayList;
import java.util.List;

public class SafeZoneManager implements Listener {

    private final NoMoreCombatLog plugin;
    private final SafeZoneStorage storage;
    private final List<SafeZone> zones = new ArrayList<>();

    public SafeZoneManager(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        this.storage = new SafeZoneStorage(plugin);
        loadZones();
    }

    public void loadZones() {
        zones.clear();
        zones.addAll(storage.loadZones());
    }

    @EventHandler
    public void onConfigReload(ConfigReloadEvent event) {
        loadZones();
    }

    public void addSafeZone(SafeZone zone) {
        zones.add(zone);
        storage.saveZones(zones);
    }

    public void removeSafeZone(SafeZone zone) {
        zones.remove(zone);
        storage.saveZones(zones);
    }

    public List<SafeZone> getZones() {
        return zones;
    }
}
