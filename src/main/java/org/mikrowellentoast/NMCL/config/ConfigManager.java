package org.mikrowellentoast.NMCL.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.display.DisplayType;
import org.mikrowellentoast.NMCL.punishment.PunishmentDefinition;
import org.mikrowellentoast.NMCL.punishment.PunishmentConfigParser;
import org.mikrowellentoast.NMCL.util.CommandNormalizer;
import org.mikrowellentoast.NMCL.util.DurationParser;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigManager {
    private static ConfigManager legacyInstance;
    private final NoMoreCombatLog plugin;
    private volatile PluginConfig settings;

    public ConfigManager(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        legacyInstance = this;
        reload();
    }

    /** @deprecated Use dependency injection through {@link NoMoreCombatLog}. */
    @Deprecated public static void initialize(NoMoreCombatLog plugin) { legacyInstance = new ConfigManager(plugin); }
    /** @deprecated Use dependency injection through {@link NoMoreCombatLog}. */
    @Deprecated public static ConfigManager getInstance() {
        if (legacyInstance == null) throw new IllegalStateException("ConfigManager not initialized");
        return legacyInstance;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        c.options().copyDefaults(true);
        PluginConfig.Retaliation retaliation = new PluginConfig.Retaliation(
                c.getBoolean("combat.retaliation.enabled", false),
                duration(c, "combat.retaliation.window", Duration.ofSeconds(10), true),
                c.getBoolean("combat.retaliation.tag-attacker-immediately", true));
        PluginConfig.GracePeriod grace = new PluginConfig.GracePeriod(
                duration(c, "combat.grace-period.join", Duration.ofSeconds(5), true),
                duration(c, "combat.grace-period.respawn", Duration.ofSeconds(3), true),
                c.getBoolean("combat.grace-period.mutual", true));
        PluginConfig.DamageSources damage = new PluginConfig.DamageSources(
                c.getBoolean("combat.damage-sources.melee", true),
                c.getBoolean("combat.damage-sources.projectiles", true),
                c.getBoolean("combat.damage-sources.arrows", true),
                c.getBoolean("combat.damage-sources.tridents", true),
                c.getBoolean("combat.damage-sources.other-player-caused-damage", true));
        PluginConfig.Combat combat = new PluginConfig.Combat(
                duration(c, "combat.duration", Duration.ofSeconds(30), false),
                c.getBoolean("combat.creative-mode", false), retaliation, grace, damage);
        DisplayType displayType = enumValue(c, "display.type", DisplayType.class, DisplayType.ACTION_BAR);
        PluginConfig.Display display = new PluginConfig.Display(displayType);
        CommandMode commandMode = enumValue(c, "commands.mode", CommandMode.class, CommandMode.BLACKLIST);
        Set<String> commandList = new HashSet<>();
        c.getStringList("commands.list").stream().map(CommandNormalizer::normalize).filter(s -> !s.isEmpty()).forEach(commandList::add);
        settings = new PluginConfig(c.getBoolean("plugin.enabled", true), combat, display,
                new PluginConfig.Commands(commandMode, Set.copyOf(commandList)),
                new PluginConfig.Teleport(c.getBoolean("teleport.portals", false), c.getBoolean("teleport.ender-pearls", false)),
                lowerSet(c.getStringList("worlds.disabled")),
                new PluginConfig.SafeZones(c.getBoolean("safe-zones.enabled", false), c.getBoolean("safe-zones.remove-combat-on-entry", false)),
                c.getBoolean("punishment.enabled", true), List.copyOf(punishments(c)),
                new PluginConfig.Integrations(c.getBoolean("integrations.placeholder-api", true),
                        new PluginConfig.WorldGuard(c.getBoolean("integrations.worldguard.enabled", false),
                                lowerSet(c.getStringList("integrations.worldguard.safe-regions")))),
                c.getBoolean("debug.enabled", false));
    }

    public PluginConfig settings() { return settings; }
    public boolean isWorldDisabled(String world) {
        return world != null && settings.disabledWorlds().contains(world.toLowerCase(Locale.ROOT));
    }

    private List<PunishmentDefinition> punishments(FileConfiguration c) {
        return PunishmentConfigParser.parse(c.getMapList("punishment.actions"), plugin.getLogger()::warning);
    }

    private Duration duration(ConfigurationSection c, String path, Duration fallback, boolean zeroAllowed) {
        return parsedDuration(c.get(path), path, fallback, zeroAllowed);
    }

    private Duration parsedDuration(Object raw, String path, Duration fallback, boolean zeroAllowed) {
        Duration parsed = raw instanceof Number number ? Duration.ofSeconds(number.longValue())
                : DurationParser.parse(raw == null ? null : raw.toString()).orElse(null);
        if (parsed == null || parsed.isNegative() || (!zeroAllowed && parsed.isZero())) {
            warn(path, raw, DurationParser.format(fallback));
            return fallback;
        }
        return parsed;
    }

    private <T extends Enum<T>> T enumValue(ConfigurationSection c, String path, Class<T> type, T fallback) {
        String raw = c.getString(path, fallback.name());
        try { return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException exception) {
            warn(path, raw, fallback.name());
            return fallback;
        }
    }

    private void warn(String key, Object value, String fallback) {
        plugin.getLogger().warning("Invalid value for '" + key + "' (" + value + "); using " + fallback + ".");
    }

    private Set<String> lowerSet(List<String> values) {
        Set<String> result = new HashSet<>();
        values.stream().filter(v -> v != null && !v.isBlank()).map(v -> v.toLowerCase(Locale.ROOT)).forEach(result::add);
        return Set.copyOf(result);
    }

    // Binary/source compatibility for integrations compiled against NMCL 1.x.
    @Deprecated public long getCombatTagDuration() { return settings.combat().duration().toMillis(); }
    @Deprecated public boolean isEnabledInCreative() { return settings.combat().creativeMode(); }
    @Deprecated public boolean isPluginEnabled() { return settings.enabled(); }
    @Deprecated public boolean isRetaliationOnly() { return settings.combat().retaliation().enabled(); }
    @Deprecated public long getRetaliationWindow() { return settings.combat().retaliation().window().toMillis(); }
    @Deprecated public boolean isSetAttackerOnCombatOnRetaliation() { return settings.combat().retaliation().tagAttackerImmediately(); }
    @Deprecated public String getPunishmentMethod() { return settings.punishments().getFirst().type().name(); }
    @Deprecated public long getBanDuration() { return settings.punishments().getFirst().duration().toMinutes(); }
    @Deprecated public List<String> getDisabledWorlds() { return List.copyOf(settings.disabledWorlds()); }
    @Deprecated public boolean areSafeZonesEnabled() { return settings.safeZones().enabled(); }
    @Deprecated public boolean shouldRemoveTagWhenEnteringSafezone() { return settings.safeZones().removeCombatOnEntry(); }
    @Deprecated public List<String> getBlockedCommands() { return List.copyOf(settings.commands().commands()); }
    @Deprecated public boolean isAllowPortalInCombat() { return settings.teleport().portals(); }
    @Deprecated public boolean isAllowEnderPearlInCombat() { return settings.teleport().enderPearls(); }
}
