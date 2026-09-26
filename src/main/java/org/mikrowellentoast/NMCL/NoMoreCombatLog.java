package org.mikrowellentoast.NMCL;


import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.scheduler.BukkitTask;
import org.mikrowellentoast.NMCL.api.NMCLApi;
import org.mikrowellentoast.NMCL.api.NMCLApiImpl;
import org.mikrowellentoast.NMCL.combat.CombatEligibility;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.combat.GracePeriodManager;
import org.mikrowellentoast.NMCL.combat.RetaliationTracker;
import org.mikrowellentoast.NMCL.commands.AdminCommandRegistrar;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.config.ConfigMigrator;
import org.mikrowellentoast.NMCL.display.CombatDisplayManager;
import org.mikrowellentoast.NMCL.integrations.IntegrationManager;
import org.mikrowellentoast.NMCL.listeners.CombatEventListener;
import org.mikrowellentoast.NMCL.listeners.PlayerLifecycleListener;
import org.mikrowellentoast.NMCL.listeners.RestrictionListener;
import org.mikrowellentoast.NMCL.listeners.SafeZoneListener;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.punishment.PunishmentManager;
import org.mikrowellentoast.NMCL.safezone.SafeZoneManager;
import org.mikrowellentoast.NMCL.storage.CombatStorage;
import org.mikrowellentoast.NMCL.utils.UpdateChecker;


public final class NoMoreCombatLog extends JavaPlugin {
    private static NoMoreCombatLog instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private SafeZoneManager safeZoneManager;
    private CombatStorage combatStorage;
    private CombatManager combatManager;
    private CombatDisplayManager displayManager;
    private GracePeriodManager gracePeriods;
    private RetaliationTracker retaliation;
    private IntegrationManager integrations;
    private NMCLApi api;
    private BukkitTask serviceTask;
    private volatile boolean shuttingDown;
    private volatile String updateAvailable;

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        new ConfigMigrator(this).migrate();
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        safeZoneManager = new SafeZoneManager(this);
        gracePeriods = new GracePeriodManager();
        retaliation = new RetaliationTracker();
        combatStorage = new CombatStorage(this);
        combatManager = new CombatManager(this, configManager, combatStorage);
        displayManager = new CombatDisplayManager(configManager, messageManager, combatManager);
        combatManager.setDisplayManager(displayManager);
        PunishmentManager punishments = new PunishmentManager(this, configManager);
        CombatEligibility eligibility = new CombatEligibility(configManager, safeZoneManager, gracePeriods);
        api = new NMCLApiImpl(combatManager);
        Bukkit.getServicesManager().register(NMCLApi.class, api, this, ServicePriority.Normal);
        if (configManager.settings().enabled() && configManager.settings().persistenceEnabled()) combatManager.restore(combatStorage.load());

        Bukkit.getPluginManager().registerEvents(new CombatEventListener(this, configManager, combatManager,
                eligibility, safeZoneManager, messageManager, retaliation), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this, configManager, combatManager,
                gracePeriods, punishments, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new RestrictionListener(configManager, combatManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new SafeZoneListener(configManager, safeZoneManager, combatManager, messageManager), this);
        new AdminCommandRegistrar(this, configManager, combatManager, eligibility, gracePeriods, safeZoneManager, messageManager).register();
        integrations = new IntegrationManager(this, configManager, combatManager);
        integrations.enable();
        serviceTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            combatManager.expireTags();
            gracePeriods.cleanup(now);
            retaliation.cleanup(now, configManager.settings().combat().retaliation().window());
            displayManager.tick();
        }, 10L, 10L);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> new UpdateChecker(this).checkForUpdates());
        getLogger().info("NoMoreCombatLog enabled with " + combatManager.getActiveTags().size() + " active tag(s).");
    }

    @Override public void onDisable() {
        shuttingDown = true;
        if (serviceTask != null) serviceTask.cancel();
        if (combatStorage != null && combatManager != null && configManager.settings().enabled()
                && configManager.settings().persistenceEnabled()) {
            combatStorage.saveNow(combatManager.getActiveTags());
        }
        if (displayManager != null) displayManager.hideAll();
        if (integrations != null) integrations.disable();
        Bukkit.getServicesManager().unregisterAll(this);
        instance = null;
    }

    public static NoMoreCombatLog getInstance() {
        return instance;
    }

    public void reloadServices(String target) {
        String normalized = target == null ? "all" : target.toLowerCase();
        if (normalized.equals("config") || normalized.equals("all")) {
            configManager.reload();
            safeZoneManager.reload();
            displayManager.reload();
            integrations.reload();
        }
        if (normalized.equals("messages") || normalized.equals("all")) messageManager.reload();
        if (!normalized.equals("config") && !normalized.equals("messages") && !normalized.equals("all")) {
            throw new IllegalArgumentException("Unknown reload target: " + target);
        }
    }

    /** @deprecated Use {@link #reloadServices(String)}. */
    @Deprecated public void reloadPluginConfig() { reloadServices("all"); }

    public static NMCLApi getAPI() {
        if (instance == null || instance.api == null) throw new IllegalStateException("NoMoreCombatLog is not enabled");
        return instance.api;
    }
    public NMCLApi api() { return api; }
    public CombatManager getCombatManager() { return combatManager; }
    public SafeZoneManager getSafeZoneManagerV2() { return safeZoneManager; }
    public boolean isShuttingDown() { return shuttingDown; }
    public String getUpdateAvailable() { return updateAvailable; }
    /** @deprecated retained for 1.x integrations. */
    @Deprecated public String getUpdate_available() { return updateAvailable; }
    public void setUpdate_available(String value) { updateAvailable = value; }
    public boolean hasUpdate() { return updateAvailable != null; }

}
