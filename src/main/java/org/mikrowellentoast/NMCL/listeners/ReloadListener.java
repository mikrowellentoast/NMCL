package org.mikrowellentoast.NMCL.listeners;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;


public class ReloadListener implements Listener {

    @EventHandler
    public void OnServerReload(ServerResourcesReloadedEvent event) {

        NoMoreCombatLog.getInstance().reloadPluginConfig();
        NoMoreCombatLog.getInstance().getLogger().info("NoMoreCombatLog configuration reloaded on server reloadCommand.");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()){
                p.sendMessage("§a[NoMoreCombatLog] Configuration reloaded");
            }
        }
    }
}
