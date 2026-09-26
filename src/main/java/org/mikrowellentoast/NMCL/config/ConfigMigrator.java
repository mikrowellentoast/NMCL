package org.mikrowellentoast.NMCL.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigMigrator {
    public static final int CURRENT_VERSION = 2;
    private final NoMoreCombatLog plugin;
    public ConfigMigrator(NoMoreCombatLog plugin) { this.plugin = plugin; }

    public void migrate() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int version = config.getInt("config-version", 0);
        if (version >= CURRENT_VERSION) return;
        backup(file, version);
        migrateLegacySafeZones(config);
        for (var entry : ConfigMigrationRules.legacyMappings().entrySet()) {
            if (config.contains(entry.getKey()) && !config.contains(entry.getValue())) {
                Object value = config.get(entry.getKey());
                if ((entry.getKey().contains("duration") || entry.getKey().equals("retaliation-window")) && value instanceof Number number) {
                    value = number.longValue() + "s";
                }
                config.set(entry.getValue(), value);
                plugin.getLogger().info("Migrated config key '" + entry.getKey() + "' to '" + entry.getValue() + "'.");
            }
        }
        migrateLegacyPunishment(config);
        try (var stream = plugin.getResource("config.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not merge new configuration defaults: " + exception.getMessage());
        }
        config.set("config-version", CURRENT_VERSION);
        try { config.save(file); }
        catch (IOException exception) { plugin.getLogger().severe("Could not save migrated config.yml: " + exception.getMessage()); }
    }

    private void migrateLegacyPunishment(YamlConfiguration config) {
        if (!config.contains("punishment-method") || config.contains("punishment.actions")) return;
        String method = config.getString("punishment-method", "kill").toUpperCase(Locale.ROOT);
        long legacyMinutes = config.getLong("ban-duration", -1);
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", method.equals("BAN") && legacyMinutes > 0 ? "TEMPBAN" : method);
        action.put("reason", "Combat logging");
        if (method.equals("BAN") && legacyMinutes > 0) action.put("duration", legacyMinutes + "m");
        config.set("punishment.enabled", true);
        config.set("punishment.actions", List.of(action));
    }

    private void migrateLegacySafeZones(YamlConfiguration config) {
        List<Map<?, ?>> legacy = config.getMapList("safe-zones");
        if (legacy.isEmpty()) return;
        File zonesFile = new File(plugin.getDataFolder(), "safezones.yml");
        YamlConfiguration zones = YamlConfiguration.loadConfiguration(zonesFile);
        var canonical = zones.getConfigurationSection("safe-zones");
        if (zones.getMapList("safe-zones").isEmpty()
                && (canonical == null || canonical.getKeys(false).isEmpty())) {
            zones.set("safe-zones", legacy);
            try {
                zones.save(zonesFile);
                plugin.getLogger().info("Moved " + legacy.size() + " legacy safe zone(s) to safezones.yml.");
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not move legacy safe zones: " + exception.getMessage());
                return;
            }
        }
        config.set("safe-zones", null);
    }

    private void backup(File file, int version) {
        if (!file.isFile()) return;
        File backup = new File(plugin.getDataFolder(), "config.yml.bak-v" + version + "-" + Instant.now().toEpochMilli());
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            plugin.getLogger().info("Backed up legacy configuration to " + backup.getName());
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not back up config.yml before migration: " + exception.getMessage());
        }
    }
}
