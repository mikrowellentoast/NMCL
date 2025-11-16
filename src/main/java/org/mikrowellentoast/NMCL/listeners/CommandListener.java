package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.events.ConfigReloadEvent;

import java.util.List;
import java.util.UUID;

public class CommandListener implements Listener {

    private final CombatListener combatlistener;
    private List<String> blocked_commands;
    private final NoMoreCombatLog plugin = NoMoreCombatLog.getInstance();

    public CommandListener(CombatListener combatlistener) {
        this.combatlistener = combatlistener;
        this.blocked_commands = plugin.getConfig().getStringList("blocked-commands");
    }

    @EventHandler
    public void onConfigReload(ConfigReloadEvent event) {
        this.blocked_commands = plugin.getConfig().getStringList("blocked-commands");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!combatlistener.isCombatTagged(uuid)) { return; }

        String message = event.getMessage().toLowerCase();
        String base = message.split(" ")[0].replace("/", "");

        if (blocked_commands.contains(base)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use that command while in combat!");
        }

    }
}
