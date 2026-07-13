package org.mikrowellentoast.NMCL.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;

import java.util.List;

public class ConfigManager {

    private static ConfigManager instance;
    private final NoMoreCombatLog plugin;

    private long combatTagDuration;
    private boolean enabledInCreative;
    private boolean pluginEnabled;
    private boolean retaliationOnly;
    private long retaliationWindow;
    private boolean setAttackerOnCombatOnRetaliation;
    private String punishmentMethod;
    private long banDuration;
    private List<String> disabledWorlds;
    private boolean safeZonesEnabled;
    private boolean removeTagWhenEnteringSafezone;
    private List<String> blockedCommands;
    private boolean allowPortalInCombat;

    private ConfigManager(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public static void initialize(NoMoreCombatLog plugin) {
        instance = new ConfigManager(plugin);
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("ConfigManager not initialized!");
        }
        return instance;
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        this.combatTagDuration = config.getLong("combat-tag-duration", 15) * 1000;
        this.enabledInCreative = config.getBoolean("enable-in-creative", false);
        this.pluginEnabled = config.getBoolean("enabled", true);
        this.retaliationOnly = config.getBoolean("retaliation-attack", false);
        this.retaliationWindow = config.getLong("retaliation-attack-duration", 10) * 1000;
        this.setAttackerOnCombatOnRetaliation = config.getBoolean("set-attacker-on-combat", true);
        this.punishmentMethod = config.getString("punishment-method", "kill");
        this.banDuration = config.getLong("ban-duration", 1440);
        this.disabledWorlds = config.getStringList("disabled-worlds");
        this.safeZonesEnabled = config.getBoolean("enable-safe-zone", false);
        this.removeTagWhenEnteringSafezone = config.getBoolean("remove-tag-when-entering-safe-zone", false);
        this.blockedCommands = config.getStringList("blocked-commands");
        this.allowPortalInCombat = config.getBoolean("allow-portal-teleport", true);

    }

    public long getCombatTagDuration() {
        return combatTagDuration;
    }

    public boolean isEnabledInCreative() {
        return enabledInCreative;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public boolean isRetaliationOnly() {
        return retaliationOnly;
    }

    public long getRetaliationWindow() {
        return retaliationWindow;
    }

    public boolean isSetAttackerOnCombatOnRetaliation() {
        return setAttackerOnCombatOnRetaliation;
    }

    public String getPunishmentMethod() {
        return punishmentMethod;
    }

    public long getBanDuration() {
        return banDuration;
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public boolean areSafeZonesEnabled() {
        return safeZonesEnabled;
    }

    public boolean shouldRemoveTagWhenEnteringSafezone() {
        return removeTagWhenEnteringSafezone;
    }

    public List<String> getBlockedCommands() {
        return blockedCommands;
    }

    public boolean isAllowPortalInCombat() {
        return allowPortalInCombat;
    }
}
