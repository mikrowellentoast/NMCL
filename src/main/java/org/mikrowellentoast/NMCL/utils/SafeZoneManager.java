package org.mikrowellentoast.NMCL.utils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SafeZoneManager implements Listener {

    private final NoMoreCombatLog plugin;
    private final List<SafeZone> zones = new ArrayList<>();

    public SafeZoneManager(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        loadZones();
    }

    public void loadZones() {
        zones.clear();

        List<Map<?, ?>> list = plugin.getConfig().getMapList("safe-zones");
        for (Map<?, ?> map : list) {
            String name = (String) map.get("name");
            String world = (String) map.get("world");

            double x = ((Number) map.get("x")).doubleValue();
            double y = ((Number) map.get("y")).doubleValue();
            double z = ((Number) map.get("z")).doubleValue();
            double radius = ((Number) map.get("radius")).doubleValue();

            zones.add(new SafeZone(name, world, x, y, z, radius));
        }
    }

    @EventHandler
    public void onConfigReload(ConfigReloadEvent event) {
        loadZones();
    }

    public void saveZones() {
        List<Map<String, Object>> list = new ArrayList<>();

        for (SafeZone z : zones) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", z.getName());
            map.put("world", z.getWorld());
            map.put("x", z.getX());
            map.put("y", z.getY());
            map.put("z", z.getZ());
            map.put("radius", z.getRadius());
            list.add(map);
        }

        plugin.getConfig().set("safe-zones", list);
        plugin.saveConfig();
    }

    public void addSafeZone(SafeZone zone) {
        zones.add(zone);
        saveZones();
    }

    public void removeSafeZone(SafeZone zone) {
        zones.remove(zone);
        saveZones();
    }

    public List<SafeZone> getZones() {
        return zones;
    }

}
