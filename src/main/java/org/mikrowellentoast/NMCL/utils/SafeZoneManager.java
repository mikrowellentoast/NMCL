package org.mikrowellentoast.NMCL.utils;

import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SafeZoneManager {

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
            double x = (double) map.get("x");
            double y = (double) map.get("y");
            double z = (double) map.get("z");
            double radius = (double) map.get("radius");

            zones.add(new SafeZone(name, world, x, y, z, radius));
        }
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

}
