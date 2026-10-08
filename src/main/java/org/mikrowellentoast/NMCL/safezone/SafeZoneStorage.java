package org.mikrowellentoast.NMCL.safezone;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SafeZoneStorage {
    private final NoMoreCombatLog plugin;
    private final File file;

    public SafeZoneStorage(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "safezones.yml");
        if (!file.exists()) plugin.saveResource("safezones.yml", false);
    }

    public List<SafeZone> load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<SafeZone> result = new ArrayList<>();
        List<Map<?, ?>> legacy = yaml.getMapList("safe-zones");
        if (!legacy.isEmpty()) {
            for (Map<?, ?> zone : legacy) loadLegacy(zone, result);
            save(result);
            return result;
        }
        ConfigurationSection root = yaml.getConfigurationSection("safe-zones");
        if (root == null) return result;
        for (String name : root.getKeys(false)) {
            try {
                ConfigurationSection section = root.getConfigurationSection(name);
                if (section == null) continue;
                String world = section.getString("world");
                SafeZoneType type = SafeZoneType.valueOf(section.getString("type", "SPHERE").toUpperCase());
                boolean prevent = section.getBoolean("flags.prevent-combat", true);
                boolean clear = section.getBoolean("flags.clear-combat-on-entry", false);
                boolean message = section.getBoolean("flags.show-message", true);
                if (type == SafeZoneType.SPHERE) {
                    double radius = section.getDouble("radius");
                    if (radius <= 0) throw new IllegalArgumentException("radius must be positive");
                    result.add(SafeZone.sphere(name, world, section.getDouble("center.x"), section.getDouble("center.y"),
                            section.getDouble("center.z"), radius, prevent, clear, message));
                } else {
                    result.add(SafeZone.cuboid(name, world, section.getDouble("min.x"), section.getDouble("min.y"),
                            section.getDouble("min.z"), section.getDouble("max.x"), section.getDouble("max.y"),
                            section.getDouble("max.z"), prevent, clear, message));
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Invalid safe zone '" + name + "': " + exception.getMessage());
            }
        }
        return result;
    }

    private void loadLegacy(Map<?, ?> map, List<SafeZone> result) {
        try {
            String name = String.valueOf(map.get("name"));
            String world = String.valueOf(map.get("world"));
            double radius = number(map.get("radius"));
            if (radius <= 0) throw new IllegalArgumentException("radius must be positive");
            result.add(SafeZone.sphere(name, world, number(map.get("x")), number(map.get("y")), number(map.get("z")),
                    radius, true, false, true));
            plugin.getLogger().info("Migrated legacy radius safe zone '" + name + "'.");
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Invalid legacy safe zone: " + exception.getMessage());
        }
    }

    public void save(List<SafeZone> zones) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (SafeZone zone : zones) {
            String path = "safe-zones." + zone.name();
            yaml.set(path + ".type", zone.type().name());
            yaml.set(path + ".world", zone.world());
            if (zone.type() == SafeZoneType.SPHERE) {
                yaml.set(path + ".center.x", zone.minX());
                yaml.set(path + ".center.y", zone.minY());
                yaml.set(path + ".center.z", zone.minZ());
                yaml.set(path + ".radius", zone.radius());
            } else {
                yaml.set(path + ".min.x", zone.minX()); yaml.set(path + ".min.y", zone.minY()); yaml.set(path + ".min.z", zone.minZ());
                yaml.set(path + ".max.x", zone.maxX()); yaml.set(path + ".max.y", zone.maxY()); yaml.set(path + ".max.z", zone.maxZ());
            }
            yaml.set(path + ".flags.prevent-combat", zone.preventCombat());
            yaml.set(path + ".flags.clear-combat-on-entry", zone.clearCombatOnEntry());
            yaml.set(path + ".flags.show-message", zone.showMessage());
        }
        try { yaml.save(file); }
        catch (IOException exception) { plugin.getLogger().severe("Could not save safezones.yml: " + exception.getMessage()); }
    }

    private double number(Object value) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException("missing numeric coordinate");
        return number.doubleValue();
    }
}
