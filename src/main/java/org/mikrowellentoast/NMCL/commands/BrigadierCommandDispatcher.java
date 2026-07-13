package org.mikrowellentoast.NMCL.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.utils.SafeZone;

import java.util.List;

public class BrigadierCommandDispatcher {

    private final NoMoreCombatLog plugin;
    private final ConfigManager config = ConfigManager.getInstance();

    public BrigadierCommandDispatcher(NoMoreCombatLog plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            LiteralCommandNode<CommandSourceStack> nmclNode = buildCommandTree();
            registrar.register(nmclNode, "NoMoreCombatLog main command", List.of("nomorecombatlog"));
        });
    }

    private LiteralCommandNode<CommandSourceStack> buildCommandTree() {
        LiteralArgumentBuilder<CommandSourceStack> nmcl = Commands.literal("nmcl")
                .requires(source -> source.getSender().hasPermission("nomorecombatlog.use"))
                .executes(this::executeInfo);

        nmcl.then(buildReloadCommand());
        nmcl.then(buildSafeZoneCommand());

        return nmcl.build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildReloadCommand() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("nomorecombatlog.reloadCommand"))
                .executes(this::executeReload);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildSafeZoneCommand() {
        LiteralArgumentBuilder<CommandSourceStack> safeZone = Commands.literal("safezone")
                .requires(source -> source.getSender().hasPermission("nomorecombatlog.safezone"));

        safeZone.then(buildSafeZoneAddCommand());
        safeZone.then(buildSafeZoneListCommand());
        safeZone.then(buildSafeZoneRemoveCommand());

        return safeZone;
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildSafeZoneAddCommand() {
        return Commands.literal("add")
                .requires(source -> source.getSender().hasPermission("nomorecombatlog.safezone.add"))
                .then(
                        RequiredArgumentBuilder.<CommandSourceStack, String>argument("name", StringArgumentType.word())
                                .then(
                                        RequiredArgumentBuilder.<CommandSourceStack, Double>argument("radius", DoubleArgumentType.doubleArg(0.1))
                                                .executes(this::executeSafeZoneAdd)
                                )
                );
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildSafeZoneListCommand() {
        return Commands.literal("list")
                .requires(source -> source.getSender().hasPermission("nomorecombatlog.safezone.list"))
                .executes(this::executeSafeZoneList);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildSafeZoneRemoveCommand() {
        return Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("nomorecombatlog.safezone.remove"))
                .then(
                        RequiredArgumentBuilder.<CommandSourceStack, String>argument("name", StringArgumentType.word())
                                .executes(this::executeSafeZoneRemove)
                );
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§b§lNoMoreCombatLog §7v" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7Prevents combat logging and manages safezones.");

        if (sender.hasPermission("nomorecombatlog.reloadCommand") ||
                sender.hasPermission("nomorecombatlog.safezone.add") ||
                sender.hasPermission("nomorecombatlog.safezone.list") ||
                sender.hasPermission("nomorecombatlog.safezone.remove")) {
            sender.sendMessage("");
            sender.sendMessage("§eAvailable commands:");
        }

        if (sender.hasPermission("nomorecombatlog.reloadCommand")) {
            sender.sendMessage("§6/nmcl reload §7- Reloads the configuration.");
        }
        if (sender.hasPermission("nomorecombatlog.safezone.add")) {
            sender.sendMessage("§6/nmcl safezone add <name> <radius> §7- Creates a safezone.");
        }
        if (sender.hasPermission("nomorecombatlog.safezone.list")) {
            sender.sendMessage("§6/nmcl safezone list §7- Lists all safezones.");
        }
        if (sender.hasPermission("nomorecombatlog.safezone.remove")) {
            sender.sendMessage("§6/nmcl safezone remove <name> §7- Removes a safezone.");
        }

        sender.sendMessage("§8§m----------------------------------------");
        return 1;
    }

    private int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        plugin.reloadPluginConfig();
        sender.sendMessage("§e[NoMoreCombatLog] §aconfiguration reloaded.");
        return 1;
    }

    private int executeSafeZoneAdd(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return 0;
        }

        if (!config.areSafeZonesEnabled()) {
            sender.sendMessage("§cSafezones are disabled in the configuration.");
            return 0;
        }

        String name = StringArgumentType.getString(context, "name");
        double radius = DoubleArgumentType.getDouble(context, "radius");

        if (name == null || name.trim().isEmpty()) {
            sender.sendMessage("§cPlease provide a valid name for the safe zone.");
            return 0;
        }

        if (radius <= 0) {
            sender.sendMessage("§cPlease provide a radius bigger than 0.");
            return 0;
        }

        SafeZone sz = new SafeZone(name,
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                radius
        );

        plugin.getSafeZoneManager().addSafeZone(sz);
        player.sendMessage("§aSafe zone §e" + name + " §aadded with radius §e" + radius + " §aat your current location.");
        return 1;
    }

    private int executeSafeZoneList(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        if (!config.areSafeZonesEnabled()) {
            sender.sendMessage("§cSafezones are disabled in the configuration.");
            return 0;
        }

        if (plugin.getSafeZoneManager().getZones().isEmpty()) {
            sender.sendMessage("§7No safezones have been created.");
            return 0;
        }

        sender.sendMessage("§aSafezones:");
        plugin.getSafeZoneManager().getZones().forEach(z ->
                sender.sendMessage("§e" + z.getName() +
                        " §7(world: " + z.getWorld() + ", radius: " + z.getRadius() + ")"));
        return 1;
    }

    private int executeSafeZoneRemove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        if (!config.areSafeZonesEnabled()) {
            sender.sendMessage("§cSafezones are disabled in the configuration.");
            return 0;
        }

        String name = StringArgumentType.getString(context, "name");
        SafeZone sz = plugin.getSafeZoneManager().getZones().stream()
                .filter(z -> z.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (sz == null) {
            sender.sendMessage("§cCould not find safezone: " + name);
            return 0;
        }

        plugin.getSafeZoneManager().removeSafeZone(sz);
        sender.sendMessage("§aSafezone §e" + name + " §awas removed.");
        return 1;
    }
}