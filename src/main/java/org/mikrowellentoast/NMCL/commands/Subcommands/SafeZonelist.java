package org.mikrowellentoast.NMCL.commands.Subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.command.Command;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

public class SafeZonelist {

    private final NoMoreCombatLog plugin;

    public SafeZonelist(NoMoreCombatLog plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("list")
                .executes((sender, args) -> {
                   if (plugin.getSafeZoneManager().getZones().isEmpty()) {
                       sender.sendMessage("§7 No Safezones have been created");
                       return;
                   }

                   sender.sendMessage("§aSafezones:");
                   plugin.getSafeZoneManager().getZones().forEach(z ->
                           sender.sendMessage("§e"+ z.getName() +
                                   " §7(world: " + z.getWorld() + ", radius: " + z.getRadius() + ")"));

                });
    }

}
