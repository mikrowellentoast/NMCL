package org.mikrowellentoast.NMCL.config;

import org.mikrowellentoast.NMCL.display.DisplayType;
import org.mikrowellentoast.NMCL.punishment.PunishmentDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public record PluginConfig(boolean enabled, Combat combat, Display display, Commands commands, Teleport teleport,
                           Set<String> disabledWorlds, SafeZones safeZones, boolean punishmentEnabled,
                           List<PunishmentDefinition> punishments,
                           Integrations integrations, boolean debug) {
    public record Combat(Duration duration, boolean creativeMode, Retaliation retaliation,
                         GracePeriod gracePeriod, DamageSources damageSources) {}
    public record Retaliation(boolean enabled, Duration window, boolean tagAttackerImmediately) {}
    public record GracePeriod(Duration join, Duration respawn, boolean mutual) {}
    public record DamageSources(boolean melee, boolean projectiles, boolean arrows, boolean tridents,
                                boolean otherPlayerCausedDamage) {}
    public record Display(DisplayType type, boolean actionBar, boolean bossBar, boolean title) {}
    public record Commands(CommandMode mode, Set<String> commands) {}
    public record Teleport(boolean portals, boolean enderPearls) {}
    public record SafeZones(boolean enabled, boolean removeCombatOnEntry) {}
    public record Integrations(boolean placeholderApi, WorldGuard worldGuard) {}
    public record WorldGuard(boolean enabled, Set<String> safeRegions) {}
}
