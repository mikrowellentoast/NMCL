package org.mikrowellentoast.NMCL.commands.Subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

public class reload {

    private final NoMoreCombatLog plugin;

    public reload(NoMoreCombatLog plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("reload")
                .withPermission("nomorecombatlog.reloadCommand")
                .executes((sender, args) -> {
                    plugin.reloadPluginConfig();
                    sender.sendMessage("§aNoMoreCombatLog configuration reloaded.");
                });
    }
}
