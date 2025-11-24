package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

public class PlayerJoinListener implements Listener {

    private final NoMoreCombatLog plugin = NoMoreCombatLog.getInstance();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) return;

        if (!plugin.hasUpdate()) return;

        String current = plugin.getPluginMeta().getVersion();
        String latest = plugin.getUpdate_available();

        player.sendMessage("§e[NoMoreCombatLog] §aA new version is available");
        player.sendMessage("§7Current: §c" + current);
        player.sendMessage("§7New: §a" + latest);
        player.sendMessage("§7Modrinth: §ahttps://modrinth.com/plugin/nomorecombatlog");


    }

}
