package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.GameMode;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;
import org.mikrowellentoast.NMCL.utils.SafeZone;

import java.util.*;

public class CombatListener implements Listener {

    /** @deprecated Combat state is now owned by CombatManager. */
    @Deprecated
    public boolean isCombatTagged(UUID uuid) {
        NoMoreCombatLog current = NoMoreCombatLog.getInstance();
        return current != null && current.getCombatManager() != null && current.getCombatManager().isTagged(uuid);
    }

/* Legacy implementation disabled; retained in-source for the 1.x migration history.

    private final HashMap<UUID, Long> combatTagged = new HashMap<>();
    private final HashMap<UUID, retaliationdata> retaliationMap = new HashMap<>();
    private final NoMoreCombatLog plugin = NoMoreCombatLog.getInstance();
    private final ConfigManager config = ConfigManager.getInstance();

    private int taskId = -1;

    public CombatListener() {
        startActionbarTask();
    }

    private boolean legacyIsCombatTagged(UUID uuid) {
        return combatTagged.containsKey(uuid);
    }

    private static class retaliationdata {
        UUID attacker;
        long timestamp;

        retaliationdata(UUID attacker, long timestamp) {
            this.attacker = attacker;
            this.timestamp = timestamp;
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!config.isPluginEnabled()) {
            return;
        }

        DamageSource source = event.getDamageSource();
        EntityDamageEvent.DamageCause damageCause = event.getCause();

        if (!(event.getEntity() instanceof Player victim) || !(source.getCausingEntity() instanceof Player attacker)) {
            return;
        }

        if (config.getDisabledWorlds().contains(victim.getWorld().getName()) || 
            config.getDisabledWorlds().contains(attacker.getWorld().getName())) {
            return;
        }

        if (victim.hasPermission("nomorecombatlog.bypass") || attacker.hasPermission("nomorecombatlog.bypass")) {
            return;
        }

        if (!config.isEnabledInCreative() && ((victim.getGameMode() == GameMode.CREATIVE || (attacker.getGameMode() == GameMode.CREATIVE)))) {
            return;
        }

        if (config.areSafeZonesEnabled() && (isInAnySafeZone(victim) || isInAnySafeZone(attacker))) {
            if (!victim.hasPermission("nomorecombatlog.safezone.bypass") && !attacker.hasPermission("nomorecombatlog.safezone.bypass")) {
               if (config.shouldRemoveTagWhenEnteringSafezone()) {
                   event.setCancelled(true);
                   return;
               }

               if (isCombatTagged(victim.getUniqueId()) && isCombatTagged(attacker.getUniqueId())) {
                   long now = System.currentTimeMillis();
                   if (config.isRetaliationOnly() && config.isSetAttackerOnCombatOnRetaliation()) {
                       combatTagged.put(attacker.getUniqueId(), now);
                   } else {
                       combatTagged.put(attacker.getUniqueId(), now);
                       combatTagged.put(victim.getUniqueId(), now);
                   }

                   return;
               }

               event.setCancelled(true);
               return;
            }
        }

        long now = System.currentTimeMillis();

        if (config.isRetaliationOnly()) {
            if (config.isSetAttackerOnCombatOnRetaliation()) {
               combatTagged.put(attacker.getUniqueId(), now);
            }

            retaliationMap.put(attacker.getUniqueId(), new retaliationdata(victim.getUniqueId(), now));
            retaliationdata data = retaliationMap.get(victim.getUniqueId());
            if (data != null && data.attacker.equals(attacker.getUniqueId())) {

               if (now - data.timestamp <= config.getRetaliationWindow()) {
                   combatTagged.put(victim.getUniqueId(), now);

                   if (!config.isSetAttackerOnCombatOnRetaliation()) {
                       combatTagged.put(attacker.getUniqueId(), now);
                   }

               }

               retaliationMap.remove(victim.getUniqueId());
            }

        } else {
            combatTagged.put(victim.getUniqueId(), now);
            combatTagged.put(attacker.getUniqueId(), now);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (combatTagged.containsKey(player.getUniqueId())) {
            combatTagged.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        long now = System.currentTimeMillis();
        Player player = event.getPlayer();
        Long lastTagged = combatTagged.get(player.getUniqueId());

        long duration = config.getCombatTagDuration();
        if (lastTagged != null && now - lastTagged < duration) {
            combatTagged.remove(player.getUniqueId());
            String punishmentMethod = config.getPunishmentMethod();
            if (punishmentMethod.equalsIgnoreCase("kill")) {
               player.setHealth(0.0);
            } else if (punishmentMethod.equalsIgnoreCase("ban")) {
               player.setHealth(0.0);
               long banDuration = config.getBanDuration();
               if (banDuration <= 0) {
                   player.ban("You have been banned for combat logging.", (Date) null, null, true);
               } else {
                   long banMillis = System.currentTimeMillis() + (banDuration * 60 * 1000);
                   Date banDate = new Date(banMillis);
                   player.ban("You have been banned for combat logging.", banDate, null, true);
               }

            }
        }
    }

    @EventHandler
    private void onConfigReload(ConfigReloadEvent event) {
        startActionbarTask();
    }

    private void startActionbarTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Long>> it = combatTagged.entrySet().iterator();

            if (config.areSafeZonesEnabled()) {
               for (Player p : Bukkit.getOnlinePlayers()) {
                   if (isInAnySafeZone(p) && (!isCombatTagged(p.getUniqueId()) || config.shouldRemoveTagWhenEnteringSafezone())) {
                       p.sendActionBar("§aYou're safe");

                       if (config.shouldRemoveTagWhenEnteringSafezone() && isCombatTagged(p.getUniqueId())) {
                           combatTagged.remove(p.getUniqueId());
                       }

                   }
               }
            }

            while (it.hasNext()) {
               Map.Entry<UUID, Long> entry = it.next();
               UUID uuid = entry.getKey();
               long lastCombat = entry.getValue();
               long elapsed = currentTime - lastCombat;
               long remaining = config.getCombatTagDuration() - elapsed;
               Player player = Bukkit.getPlayer(uuid);


               if (player == null || !player.isOnline()) {
                   continue;
               }

               if (config.areSafeZonesEnabled() && isInAnySafeZone(player) && config.shouldRemoveTagWhenEnteringSafezone()) {
                   continue;
               }


               if (remaining <= 0) {
                   it.remove();
                   player.sendActionBar("§cYou're no longer in combat.");
                   continue;
               }

               long seconds = (remaining + 999) / 1000;
               player.sendActionBar("§cCombat: §e" + seconds + "s");
            }
        }, 0L, 20L).getTaskId();
    }

    private boolean isInAnySafeZone(Player player) {
        for (SafeZone sz : plugin.getSafeZoneManager().getZones()) {
            if (sz.isInSafeZone(player.getLocation())) {
               return true;
            }
        }
        return false;
    }
*/
}
