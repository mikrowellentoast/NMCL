package org.mikrowellentoast.NMCL.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.utils.SafeZone;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BrigadierCommandDispatcher implements CommandExecutor {

    private final ConfigManager config = ConfigManager.getInstance();

    private final NoMoreCombatLog plugin;
    private final CommandDispatcher<CommandSender> dispatcher;

    public BrigadierCommandDispatcher(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        this.dispatcher = new CommandDispatcher<>();
    }

    public void register() {
        buildCommandTree();

        registerCommand("nmcl");
        registerCommand("nomorecombatlog");
    }

    private void registerCommand(String name) {
        org.bukkit.Bukkit.getServer().getCommandMap().register(name, new org.bukkit.command.Command(name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return BrigadierCommandDispatcher.this.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return BrigadierCommandDispatcher.this.getSuggestions(sender, args);
            }
        });
    }

    private List<String> getSuggestions(CommandSender sender, String[] args) {
        StringBuilder input = new StringBuilder("nmcl");
        for (String arg : args) {
            input.append(" ").append(arg);
        }
        try {
            return dispatcher.getCompletionSuggestions(dispatcher.parse(input.toString(), sender))
                    .join()
                    .getList()
                    .stream()
                    .map(com.mojang.brigadier.suggestion.Suggestion::getText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void buildCommandTree() {
        LiteralArgumentBuilder<CommandSender> nmcl = LiteralArgumentBuilder.<CommandSender>literal("nmcl")
                .executes(this::executeInfo);

        nmcl.then(buildReloadCommand());
        nmcl.then(buildSafeZoneCommand());

        dispatcher.register(nmcl);
    }

    private LiteralArgumentBuilder<CommandSender> buildReloadCommand() {
        return LiteralArgumentBuilder.<CommandSender>literal("reload")
                .requires(sender -> sender.hasPermission("nomorecombatlog.reloadCommand"))
                .executes(this::executeReload);
    }

    private LiteralArgumentBuilder<CommandSender> buildSafeZoneCommand() {
        LiteralArgumentBuilder<CommandSender> safeZone = LiteralArgumentBuilder.<CommandSender>literal("safezone")
                .requires(sender -> sender.hasPermission("nomorecombatlog.safezone"));

        safeZone.then(buildSafeZoneAddCommand());
        safeZone.then(buildSafeZoneListCommand());
        safeZone.then(buildSafeZoneRemoveCommand());

        return safeZone;
    }

    private LiteralArgumentBuilder<CommandSender> buildSafeZoneAddCommand() {
        return LiteralArgumentBuilder.<CommandSender>literal("add")
                .requires(sender -> sender.hasPermission("nomorecombatlog.safezone.add"))
                .then(
                        RequiredArgumentBuilder.<CommandSender, String>argument("name", StringArgumentType.word())
                                .then(
                                        RequiredArgumentBuilder.<CommandSender, Double>argument("radius", DoubleArgumentType.doubleArg(0.1))
                                                .executes(this::executeSafeZoneAdd)
                                )
                );
    }

    private LiteralArgumentBuilder<CommandSender> buildSafeZoneListCommand() {
        return LiteralArgumentBuilder.<CommandSender>literal("list")
                .requires(sender -> sender.hasPermission("nomorecombatlog.safezone.list"))
                .executes(this::executeSafeZoneList);
    }

    private LiteralArgumentBuilder<CommandSender> buildSafeZoneRemoveCommand() {
        return LiteralArgumentBuilder.<CommandSender>literal("remove")
                .requires(sender -> sender.hasPermission("nomorecombatlog.safezone.remove"))
                .then(
                        RequiredArgumentBuilder.<CommandSender, String>argument("name", StringArgumentType.word())
                                .executes(this::executeSafeZoneRemove)
                );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        StringBuilder commandString = new StringBuilder("nmcl");
        for (String arg : args) {
            commandString.append(" ").append(arg);
        }

        try {
            dispatcher.execute(commandString.toString(), sender);
            return true;
        } catch (CommandSyntaxException e) {
            sender.sendMessage("§cUnknown command, wrong syntax, or you don't have permission to use it. Try §e/nmcl §cfor help.");
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cCommand error: " + e.getMessage());
            return false;
        }
    }

    private int executeInfo(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSource();

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

    private int executeReload(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSource();
        plugin.reloadPluginConfig();
        sender.sendMessage("§e[NoMoreCombatLog] §aconfiguration reloaded.");
        return 1;
    }

    private int executeSafeZoneAdd(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSource();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return 0;
        }

        if (!(config.areSafeZonesEnabled())) {
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

    private int executeSafeZoneList(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSource();

        if (!(config.areSafeZonesEnabled())) {
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

    private int executeSafeZoneRemove(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSource();

        if (!(config.areSafeZonesEnabled())) {
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