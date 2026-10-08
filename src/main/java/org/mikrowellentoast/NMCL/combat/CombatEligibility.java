package org.mikrowellentoast.NMCL.combat;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.safezone.SafeZoneManager;

public final class CombatEligibility {
    public enum Result { ELIGIBLE, PLUGIN_DISABLED, DISABLED_WORLD, BYPASS, CREATIVE, SAFE_ZONE, GRACE_PERIOD }
    private final ConfigManager config;
    private final SafeZoneManager safeZones;
    private final GracePeriodManager grace;

    public CombatEligibility(ConfigManager config, SafeZoneManager safeZones, GracePeriodManager grace) {
        this.config = config; this.safeZones = safeZones; this.grace = grace;
    }

    public Result check(Player player) {
        if (!config.settings().enabled()) return Result.PLUGIN_DISABLED;
        if (config.isWorldDisabled(player.getWorld().getName())) return Result.DISABLED_WORLD;
        if (player.hasPermission("nomorecombatlog.bypass")) return Result.BYPASS;
        if (!config.settings().combat().creativeMode() && player.getGameMode() == GameMode.CREATIVE) return Result.CREATIVE;
        if (config.settings().safeZones().enabled() && safeZones.at(player.getLocation()).filter(zone -> zone.preventCombat()).isPresent()
                && !player.hasPermission("nomorecombatlog.safezone.bypass")) return Result.SAFE_ZONE;
        if (grace.isProtected(player.getUniqueId())) return Result.GRACE_PERIOD;
        return Result.ELIGIBLE;
    }
}
