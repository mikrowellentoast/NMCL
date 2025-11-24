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
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;
import org.mikrowellentoast.NMCL.utils.SafeZone;

import java.util.*;

public class CombatListener implements Listener {

    private final HashMap<UUID, Long> combatTagged = new HashMap<>();
    private final HashMap<UUID, retaliationdata> retaliationMap = new HashMap<>();
    private final NoMoreCombatLog plugin = NoMoreCombatLog.getInstance();
    private long COMBAT_TAG_DURATION;
    private boolean ENABLED_IN_CREATIVE;
    private boolean PLUGIN_ENABLED;
    private boolean RETALIATION_ONLY;
    private long RETALIATION_WINDOW;
    private boolean SET_ATTACKER_ON_COMBAT_ON_RETALIATION;
    private String punishmentMethod;
    private long banDuration;
    private List<String> disabled_worlds;
    private boolean safe_zones_enabled;
    private boolean remove_tag_when_entering_safezone;


    private int taskId = -1;



    public CombatListener() {
        this.COMBAT_TAG_DURATION = plugin.getConfig().getLong("combat-tag-duration", 15) * 1000;
        this.ENABLED_IN_CREATIVE = plugin.getConfig().getBoolean("enable-in-creative", false);
        this.PLUGIN_ENABLED = plugin.getConfig().getBoolean("enabled", true);
        this.RETALIATION_ONLY = plugin.getConfig().getBoolean("retaliation-attack", false);
        this.RETALIATION_WINDOW = plugin.getConfig().getLong("retaliation-attack-duration", 10) * 1000;
        this.SET_ATTACKER_ON_COMBAT_ON_RETALIATION = plugin.getConfig().getBoolean("set-attacker-on-combat", true);
        this.punishmentMethod = plugin.getConfig().getString("punishment-method", "kill");
        this.banDuration = plugin.getConfig().getLong("ban-duration", 1440);
        this.disabled_worlds = plugin.getConfig().getStringList("disabled-worlds");
        this.safe_zones_enabled = plugin.getConfig().getBoolean("enable-safe-zone", false);
        this.remove_tag_when_entering_safezone = plugin.getConfig().getBoolean("remove-tag-when-entering-safe-zone", false);
        startActionbarTask();
    }

    public boolean isCombatTagged(UUID uuid) {
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
        if (!PLUGIN_ENABLED) {
            return;
        }

        DamageSource source = event.getDamageSource();

        EntityDamageEvent.DamageCause damageCause = event.getCause();



        if (!(event.getEntity() instanceof Player v) || !(source.getCausingEntity() instanceof Player attacker)) {
            return;
        }

        if (disabled_worlds.contains(v.getWorld().getName()) || disabled_worlds.contains(attacker.getWorld().getName())) {
            return;
        }

        if (v.hasPermission("nomorecombatlog.bypass") || attacker.hasPermission("nomorecombatlog.bypass")) {
            return;
        }

        if (!ENABLED_IN_CREATIVE && ((v.getGameMode() == GameMode.CREATIVE || (attacker.getGameMode() == GameMode.CREATIVE)))) {
            return;
        }

        if (safe_zones_enabled && (isInAnySafeZone(v) ||isInAnySafeZone(attacker))) {
            if (!v.hasPermission("nomorecombatlog.safezone.bypass") && !attacker.hasPermission("nomorecombatlog.safezone.bypass")) {
                if (remove_tag_when_entering_safezone) {
                    event.setCancelled(true);
                    return;
                }

                if (isCombatTagged(v.getUniqueId()) && isCombatTagged(attacker.getUniqueId())) {
                    long now = System.currentTimeMillis();
                    if (RETALIATION_ONLY && SET_ATTACKER_ON_COMBAT_ON_RETALIATION) {
                        combatTagged.put(attacker.getUniqueId(), now);
                    } else {
                        combatTagged.put(attacker.getUniqueId(), now);
                        combatTagged.put(v.getUniqueId(), now);
                    }

                    return;
                }

                event.setCancelled(true);
                return;
            }
        }

        long now = System.currentTimeMillis();

        if (SET_ATTACKER_ON_COMBAT_ON_RETALIATION && RETALIATION_ONLY) {
            combatTagged.put(attacker.getUniqueId(), now);
        }

        if (RETALIATION_ONLY) {

            retaliationMap.put(attacker.getUniqueId(), new retaliationdata(v.getUniqueId(), now));
            retaliationdata data = retaliationMap.get(v.getUniqueId());
            if (data != null && data.attacker.equals(attacker.getUniqueId())) {

                if (now - data.timestamp <= RETALIATION_WINDOW) {
                    combatTagged.put(v.getUniqueId(), now);

                    if (!SET_ATTACKER_ON_COMBAT_ON_RETALIATION) {
                        combatTagged.put(attacker.getUniqueId(), now);
                    }

                }

                retaliationMap.remove(v.getUniqueId());
            }

        } else {
            combatTagged.put(v.getUniqueId(), now);
            combatTagged.put(attacker.getUniqueId(), now);
        }



    }



    /*@EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!PLUGIN_ENABLED) {
            return;
        }



        if(!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }


        if (disabled_worlds.contains(victim.getWorld().getName()) || disabled_worlds.contains(attacker.getWorld().getName())) {
            return;
        }

        if (victim.hasPermission("nomorecombatlog.bypass") || attacker.hasPermission("nomorecombatlog.bypass")) {
            return;
        }

        if (!ENABLED_IN_CREATIVE && (victim.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.CREATIVE)) {
            return;
        }

        if (safe_zones_enabled && (isInAnySafeZone(victim) || isInAnySafeZone(attacker))) {
            if (!victim.hasPermission("nomorecombatlog.safezone.bypass") && !attacker.hasPermission("nomorecombatlog.safezone.bypass")) {
               if (remove_tag_when_entering_safezone) {
                   event.setCancelled(true);
                   return;
               }

               if (isCombatTagged(victim.getUniqueId()) && isCombatTagged(attacker.getUniqueId())) {
                   return;
               }

               event.setCancelled(true);
               return;

            }
        }

        long now = System.currentTimeMillis();

        if (SET_ATTACKER_ON_COMBAT_ON_RETALIATION && RETALIATION_ONLY) {
            combatTagged.put(attacker.getUniqueId(), now);
        }

        if (RETALIATION_ONLY) {

            retaliationMap.put(attacker.getUniqueId(), new retaliationdata(victim.getUniqueId(), now));
            retaliationdata data = retaliationMap.get(victim.getUniqueId());
            if (data != null && data.attacker.equals(attacker.getUniqueId())) {

                if (now - data.timestamp <= RETALIATION_WINDOW) {
                    combatTagged.put(victim.getUniqueId(), now);

                    if (!SET_ATTACKER_ON_COMBAT_ON_RETALIATION) {
                        combatTagged.put(attacker.getUniqueId(), now);
                    }

                }

                retaliationMap.remove(victim.getUniqueId());
            }

        } else {
            combatTagged.put(victim.getUniqueId(), now);
            combatTagged.put(attacker.getUniqueId(), now);
        }

    }*/

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

        long duration = COMBAT_TAG_DURATION;
        if (lastTagged != null && now - lastTagged < duration) {
            combatTagged.remove(player.getUniqueId());
            if (punishmentMethod.equalsIgnoreCase("kill")) {
                player.setHealth(0.0);
            } else if (punishmentMethod.equalsIgnoreCase("ban")) {
                player.setHealth(0.0);
                if (banDuration <= 0) {
                    player.ban("You have been banned for combat logging.", (Date) null, null, true);
                } else {
                    long banMillis = System.currentTimeMillis() + (banDuration * 60 * 1000);
                    Date banDate = new Date(banMillis);
                    player.ban("You have been banned for combat logging.",banDate, null, true);
                }

            }
        }
    }

    @EventHandler
    private void onConfigReload(ConfigReloadEvent event) {

        this.COMBAT_TAG_DURATION = plugin.getConfig().getLong("combat-tag-duration", 15) * 1000;
        this.ENABLED_IN_CREATIVE = plugin.getConfig().getBoolean("enable-in-creative", false);
        this.PLUGIN_ENABLED = plugin.getConfig().getBoolean("enabled", true);
        this.RETALIATION_ONLY = plugin.getConfig().getBoolean("retaliationattack", false);
        this.RETALIATION_WINDOW = plugin.getConfig().getLong("retaliation-window", 10) * 1000;
        this.SET_ATTACKER_ON_COMBAT_ON_RETALIATION = plugin.getConfig().getBoolean("set-attacker-on-combat", true);
        this.punishmentMethod = plugin.getConfig().getString("punishment-method", "kill");
        this.banDuration = plugin.getConfig().getLong("ban-duration", 1440);
        this.disabled_worlds = plugin.getConfig().getStringList("disabled-worlds");
        this.safe_zones_enabled = plugin.getConfig().getBoolean("enable-safe-zone", false);
        this.remove_tag_when_entering_safezone = plugin.getConfig().getBoolean("remove-tag-when-entering-safe-zone", false);
        startActionbarTask();
    }

    private void startActionbarTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Long>> it = combatTagged.entrySet().iterator();

            if (safe_zones_enabled) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isInAnySafeZone(p) && (!isCombatTagged(p.getUniqueId()) || remove_tag_when_entering_safezone)) {
                        p.sendActionBar("§aYou're safe");

                        if (remove_tag_when_entering_safezone && isCombatTagged(p.getUniqueId())) {
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
                long remaining = COMBAT_TAG_DURATION - elapsed;
                Player player = Bukkit.getPlayer(uuid);


                if (player == null || !player.isOnline()) {
                    continue;
                }

                if (safe_zones_enabled && isInAnySafeZone(player) && remove_tag_when_entering_safezone) {
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



}


