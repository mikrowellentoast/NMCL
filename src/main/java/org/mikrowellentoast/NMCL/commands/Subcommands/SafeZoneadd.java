package org.mikrowellentoast.NMCL.commands.Subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.utils.SafeZone;

public class SafeZoneadd {

    private final NoMoreCombatLog plugin;

    public SafeZoneadd(NoMoreCombatLog plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("add")
                .withPermission("nomorecombatlog.safezone.add")
                .withArguments(new StringArgument("name"), new DoubleArgument("radius"))
                .executes((sender, args) -> {
                    if(!(sender instanceof Player player)) return;



                    String name = (String) args.get("name");
                    double radius = (Double) args.get("radius");

                    if (name == null || name.trim().isEmpty()) {
                        sender.sendMessage("§cPlease provide a valid name for the safe zone.");
                        return;
                    }

                    if (radius <= 0) {
                        sender.sendMessage("§cPlease provide a radius bigger than 0.");
                        return;
                    }

                    SafeZone sz = new SafeZone(name,
                            player.getWorld().getName(),
                            player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ(),
                            radius
                    );

                    plugin.getSafeZoneManager().addSafeZone(sz);
                    player.sendMessage("§aSafe zone '" + name + "' added with radius " + radius + " at your current location.");

                });

        }
    }
