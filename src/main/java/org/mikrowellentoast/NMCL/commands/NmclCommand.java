package org.mikrowellentoast.NMCL.commands;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.command.Command;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import org.mikrowellentoast.NMCL.commands.Subcommands.SafeZoneadd;
import org.mikrowellentoast.NMCL.commands.Subcommands.SafeZonelist;
import org.mikrowellentoast.NMCL.commands.Subcommands.SafeZoneremove;
import org.mikrowellentoast.NMCL.commands.Subcommands.reload;

public class NmclCommand {

    private final NoMoreCombatLog plugin;

    public NmclCommand(NoMoreCombatLog plugin) {
        this.plugin = plugin;

    }

    public void register() {

        reload reload = new reload(plugin);
        SafeZoneadd safezoneadd = new SafeZoneadd(plugin);
        SafeZoneremove safeZoneremove = new SafeZoneremove(plugin);
        SafeZonelist safeZonelist = new SafeZonelist(plugin);


        new CommandAPICommand("nmcl")
                .withPermission("nomorecombatlog.use")
                .withAliases("nomorecombatlog")


                .withSubcommand(reload.getCommand())
                .withSubcommand(new CommandAPICommand("safezone")
                        .withPermission("nomorecombatlog.safezone")
                        .withSubcommand(safezoneadd.getCommand())
                        .withSubcommand(safeZoneremove.getCommand())
                        .withSubcommand(safeZonelist.getCommand())
                )

                .register();
    }

}
