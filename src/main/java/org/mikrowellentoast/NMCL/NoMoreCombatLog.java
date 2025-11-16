package org.mikrowellentoast.NMCL;


import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.mikrowellentoast.NMCL.commands.NmclCommand;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;
import org.mikrowellentoast.NMCL.listeners.CombatListener;
import org.mikrowellentoast.NMCL.listeners.CommandListener;
import org.mikrowellentoast.NMCL.listeners.PortalListener;
import org.mikrowellentoast.NMCL.listeners.ReloadListener;
import org.mikrowellentoast.NMCL.utils.SafeZoneManager;


import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class NoMoreCombatLog extends JavaPlugin {

    private static NoMoreCombatLog instance;
    private SafeZoneManager safeZoneManager;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIPaperConfig(this).verboseOutput(true));
    }


    @Override
    public void onEnable() {
        CommandAPI.onEnable();

        NmclCommand nmclCommand = new NmclCommand(this);
        nmclCommand.register();

        instance = this;
        saveDefaultConfig();

        checkAndUpdateConfig();

        CombatListener combatlistener = new CombatListener();

        safeZoneManager = new SafeZoneManager(this);


        Bukkit.getPluginManager().registerEvents(combatlistener, this);
        Bukkit.getPluginManager().registerEvents(new ReloadListener(), this);
        Bukkit.getPluginManager().registerEvents(new PortalListener(combatlistener), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(combatlistener), this);


        getLogger().info("NoMoreCombatLog has been enabled.");

    }

    @Override
    public void onDisable() {

    }

    public static NoMoreCombatLog getInstance() {
        return instance;
    }

    public void reloadPluginConfig() {
        instance.reloadConfig();
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
}
