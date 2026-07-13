package org.mikrowellentoast.NMCL;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.mikrowellentoast.NMCL.commands.BrigadierCommandDispatcher;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;
import org.mikrowellentoast.NMCL.listeners.*;
import org.mikrowellentoast.NMCL.utils.SafeZoneManager;
import org.mikrowellentoast.NMCL.utils.UpdateChecker;


import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class NoMoreCombatLog extends JavaPlugin {

    private static NoMoreCombatLog instance;
    private SafeZoneManager safeZoneManager;

    private String update_available = null;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        checkAndUpdateConfig();

        ConfigManager.initialize(this);
        BrigadierCommandDispatcher commandDispatcher = new BrigadierCommandDispatcher(this);
        commandDispatcher.register();

        CombatListener combatlistener = new CombatListener();

        PlayerJoinListener playerjoinListener = new PlayerJoinListener();

        safeZoneManager = new SafeZoneManager(this);

        new UpdateChecker().checkForUpdates();


        Bukkit.getPluginManager().registerEvents(playerjoinListener, this);
        Bukkit.getPluginManager().registerEvents(combatlistener, this);
        Bukkit.getPluginManager().registerEvents(new ReloadListener(), this);
        Bukkit.getPluginManager().registerEvents(new PortalListener(combatlistener), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(combatlistener), this);


        getLogger().info("NoMoreCombatLog has been enabled.");

    }

    public static NoMoreCombatLog getInstance() {
        return instance;
    }

    public void reloadPluginConfig() {
        instance.reloadConfig();
        ConfigManager.getInstance().reload();
        Bukkit.getPluginManager().callEvent(new ConfigReloadEvent());
    }

    public SafeZoneManager getSafeZoneManager() {
        return safeZoneManager;
    }

    public void checkAndUpdateConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);

        FileConfiguration newDefaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(getResource("config.yml"), StandardCharsets.UTF_8)
        );

        boolean needsUpdate = false;
        for (String key: newDefaults.getKeys(true)) {
            if (!oldConfig.contains(key)) {
                needsUpdate = true;
                getLogger().info("Updating config file");
            }
        }

        if (!needsUpdate) {
            return;
        }

        saveResource("config.yml", true);

        FileConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);

        for (String key: oldConfig.getKeys(true)) {
            if (newConfig.contains(key)) {
                newConfig.set(key, oldConfig.get(key));
            }
        }

        try {
            newConfig.save(configFile);
        } catch (Exception e) {
            getLogger().warning("Failed to save updated config: " + e.getMessage());
        }

        reloadPluginConfig();
        getLogger().info("Config file has been updated.");


    }

    public String getUpdate_available() {
        return update_available;
    }

    public void setUpdate_available(String update_available) {
        this.update_available = update_available;
    }

    public boolean hasUpdate() {
        return this.update_available != null;
    }

}
