package org.mikrowellentoast.NMCL.commands.Subcommands;


import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.Suggestion;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.utils.SafeZone;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SafeZoneremove {

    private final NoMoreCombatLog plugin;

    public SafeZoneremove(NoMoreCombatLog plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("remove")
                .withPermission("nomorecombatlog.safezone.remove")
                .withArguments(
                        new StringArgument("name")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    return plugin.getSafeZoneManager().getZones().stream().map(SafeZone::getName).toList().toArray(new String[0]);
                                }))
                )
                .executes((sender, args)-> {
                    String name = (String) args.get("name");

                    SafeZone sz = plugin.getSafeZoneManager().getZones().stream().filter(z -> z.getName().equalsIgnoreCase(name)).findFirst().orElse(null);

                    if (sz == null) {
                        sender.sendMessage("§cCould not find safezone: " + name);
                        return;
                    }

                    plugin.getSafeZoneManager().removeSafeZone(sz);
                    sender.sendMessage("§aSafezone §e" + name + " §awas removed");

                });
    }
}
