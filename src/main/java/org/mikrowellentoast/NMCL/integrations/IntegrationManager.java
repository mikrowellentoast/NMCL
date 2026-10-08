package org.mikrowellentoast.NMCL.integrations;

import org.bukkit.Bukkit;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.config.ConfigManager;

public final class IntegrationManager {
    private final NoMoreCombatLog plugin;
    private final ConfigManager config;
    private final CombatManager combat;
    private NMCLPlaceholderExpansion placeholders;

    public IntegrationManager(NoMoreCombatLog plugin, ConfigManager config, CombatManager combat) {
        this.plugin = plugin; this.config = config; this.combat = combat;
    }

    public void enable() {
        boolean wanted = config.settings().integrations().placeholderApi()
                && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (!wanted && placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        if (wanted
                && placeholders == null) {
            placeholders = new NMCLPlaceholderExpansion(plugin, combat);
            placeholders.register();
        }
        if (config.settings().integrations().worldGuard().enabled()) {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
                plugin.getLogger().warning("WorldGuard integration is enabled, but WorldGuard is not installed; continuing without it.");
            } else {
                plugin.getLogger().info("WorldGuard detected. Region adapter is reserved behind the integration boundary; NMCL safe zones remain active.");
            }
        }
    }

    public void reload() { enable(); }
    public void disable() {
        if (placeholders != null) placeholders.unregister();
        placeholders = null;
    }
}
