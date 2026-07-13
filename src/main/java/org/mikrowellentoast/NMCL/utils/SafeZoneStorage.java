package org.mikrowellentoast.NMCL.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SafeZoneStorage {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public SafeZoneStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "safezones.yml");
        loadConfig();
        migrateFromOldConfig();
    }

    private void loadConfig() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                dataConfig = new YamlConfiguration();
                dataConfig.set("safe-zones", new ArrayList<>());
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create safezones.yml: " + e.getMessage());
                dataConfig = new YamlConfiguration();
            }
        } else {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        }
    }

    public List<SafeZone> loadZones() {
        List<SafeZone> zones = new ArrayList<>();
        List<Map<?, ?>> list = dataConfig.getMapList("safe-zones");

        for (Map<?, ?> map : list) {
            try {
                String name = (String) map.get("name");
                String world = (String) map.get("world");
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                double radius = ((Number) map.get("radius")).doubleValue();

                zones.add(new SafeZone(name, world, x, y, z, radius));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load safe zone from data: " + e.getMessage());
            }
        }

        return zones;
    }

    public void saveZones(List<SafeZone> zones) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (SafeZone zone : zones) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", zone.getName());
            map.put("world", zone.getWorld());
            map.put("x", zone.getX());
            map.put("y", zone.getY());
            map.put("z", zone.getZ());
            map.put("radius", zone.getRadius());
            list.add(map);
        }

        dataConfig.set("safe-zones", list);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save safe zones: " + e.getMessage());
        }
    }

    public void reload() {
        loadConfig();
    }

    private void migrateFromOldConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
        List<Map<?, ?>> oldZones = oldConfig.getMapList("safe-zones");

        if (oldZones.isEmpty()) {
            return;
        }

        List<SafeZone> migratedZones = loadZones();

        for (Map<?, ?> map : oldZones) {
            try {
                String name = (String) map.get("name");
                String world = (String) map.get("world");
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                double radius = ((Number) map.get("radius")).doubleValue();

                boolean exists = migratedZones.stream()
                        .anyMatch(zone -> zone.getName().equalsIgnoreCase(name));

                if (!exists) {
                    migratedZones.add(new SafeZone(name, world, x, y, z, radius));
                    plugin.getLogger().info("Migrated safe zone: " + name);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to migrate safe zone: " + e.getMessage());
            }
        }

        if (!oldZones.isEmpty()) {
            saveZones(migratedZones);

            oldConfig.set("safe-zones", null);
            try {
                oldConfig.save(configFile);
                plugin.getLogger().info("Successfully migrated " + oldZones.size() + " safe zone(s) from config.yml to safezones.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save updated config.yml after migration: " + e.getMessage());
            }
        }
    }
}
