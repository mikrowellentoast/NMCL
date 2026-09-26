package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatEligibility;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.combat.CombatTagReason;
import org.mikrowellentoast.NMCL.combat.RetaliationTracker;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.safezone.SafeZoneManager;

public final class CombatEventListener implements Listener {
    private final NoMoreCombatLog plugin;
    private final ConfigManager config;
    private final CombatManager combat;
    private final CombatEligibility eligibility;
    private final SafeZoneManager safeZones;
    private final MessageManager messages;
    private final RetaliationTracker retaliation;

    public CombatEventListener(NoMoreCombatLog plugin, ConfigManager config, CombatManager combat,
                               CombatEligibility eligibility, SafeZoneManager safeZones,
                               MessageManager messages, RetaliationTracker retaliation) {
        this.plugin = plugin; this.config = config; this.combat = combat; this.eligibility = eligibility;
        this.safeZones = safeZones; this.messages = messages; this.retaliation = retaliation;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(event);
        if (attacker == null || attacker.equals(victim) || !sourceEnabled(event)) return;

        if (config.settings().safeZones().enabled() && (prevented(attacker) || prevented(victim))) {
            event.setCancelled(true);
            messages.send(attacker, "safe-zone.blocked");
            debug("combat ignored due to safe zone: " + attacker.getName());
            return;
        }
        CombatEligibility.Result attackerResult = eligibility.check(attacker);
        CombatEligibility.Result victimResult = eligibility.check(victim);
        boolean attackerGrace = attackerResult == CombatEligibility.Result.GRACE_PERIOD;
        boolean victimGrace = victimResult == CombatEligibility.Result.GRACE_PERIOD;
        if ((!attackerGrace && attackerResult != CombatEligibility.Result.ELIGIBLE)
                || (!victimGrace && victimResult != CombatEligibility.Result.ELIGIBLE)) {
            debug("combat ignored: attacker=" + attackerResult + " victim=" + victimResult);
            return;
        }
        if (attackerGrace || victimGrace) {
            if (config.settings().combat().gracePeriod().mutual()) {
                debug("combat ignored due to mutual grace period");
                return;
            }
            if (!attackerGrace) combat.tagPlayer(attacker, victim, config.settings().combat().duration(), CombatTagReason.PVP);
            if (!victimGrace) combat.tagPlayer(victim, attacker, config.settings().combat().duration(), CombatTagReason.PVP);
            return;
        }

        long now = System.currentTimeMillis();
        var settings = config.settings().combat().retaliation();
        if (!settings.enabled()) {
            tagPair(attacker, victim);
            return;
        }
        boolean retaliated = retaliation.consumeRetaliation(attacker.getUniqueId(), victim.getUniqueId(), now, settings.window());
        retaliation.recordAttack(victim.getUniqueId(), attacker.getUniqueId(), now);
        if (settings.tagAttackerImmediately()) combat.tagPlayer(attacker, victim, config.settings().combat().duration(), CombatTagReason.PVP);
        if (retaliated) tagPair(attacker, victim);
    }

    private boolean prevented(Player player) {
        return !player.hasPermission("nomorecombatlog.safezone.bypass")
                && safeZones.at(player.getLocation()).filter(zone -> zone.preventCombat()).isPresent();
    }

    private void tagPair(Player first, Player second) {
        combat.tagPlayer(first, second, config.settings().combat().duration(), CombatTagReason.PVP);
        combat.tagPlayer(second, first, config.settings().combat().duration(), CombatTagReason.PVP);
    }

    private Player attacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player player ? player : null;
        }
        Entity causing = event.getDamageSource().getCausingEntity();
        return causing instanceof Player player && config.settings().combat().damageSources().otherPlayerCausedDamage() ? player : null;
    }

    private boolean sourceEnabled(EntityDamageByEntityEvent event) {
        var sources = config.settings().combat().damageSources();
        Entity damager = event.getDamager();
        if (damager instanceof Player) return sources.melee();
        if (damager instanceof Trident) return sources.tridents();
        if (damager instanceof Arrow || damager instanceof AbstractArrow) return sources.arrows();
        if (damager instanceof Projectile) return sources.projectiles();
        return sources.otherPlayerCausedDamage();
    }

    private void debug(String text) { if (config.settings().debug()) plugin.getLogger().info("[Debug] " + text); }
}
