package org.mikrowellentoast.NMCL.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatEligibility;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.combat.CombatTag;
import org.mikrowellentoast.NMCL.combat.CombatTagReason;
import org.mikrowellentoast.NMCL.combat.GracePeriodManager;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.safezone.SafeZone;
import org.mikrowellentoast.NMCL.safezone.SafeZoneManager;
import org.mikrowellentoast.NMCL.util.DurationParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class AdminCommandRegistrar {
    private final NoMoreCombatLog plugin;
    private final ConfigManager config;
    private final CombatManager combat;
    private final CombatEligibility eligibility;
    private final GracePeriodManager grace;
    private final SafeZoneManager zones;
    private final MessageManager messages;

    public AdminCommandRegistrar(NoMoreCombatLog plugin, ConfigManager config, CombatManager combat,
                                 CombatEligibility eligibility, GracePeriodManager grace,
                                 SafeZoneManager zones, MessageManager messages) {
        this.plugin = plugin; this.config = config; this.combat = combat; this.eligibility = eligibility;
        this.grace = grace; this.zones = zones; this.messages = messages;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(build().build(), "NoMoreCombatLog administration", List.of("nomorecombatlog")));
    }

    private LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nmcl").executes(this::info);
        root.then(playerOptionalDuration("tag", "nomorecombatlog.admin.tag", this::tag));
        root.then(playerCommand("untag", "nomorecombatlog.admin.untag", this::untag));
        root.then(playerCommand("status", "nomorecombatlog.admin.status", this::status));
        root.then(Commands.literal("extend").requires(s -> permitted(s, "nomorecombatlog.admin.extend"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("duration", StringArgumentType.word()).executes(this::extend))));
        root.then(Commands.literal("list").requires(s -> permitted(s, "nomorecombatlog.admin.list")).executes(this::list));
        root.then(optionalDuration("tagall", "nomorecombatlog.admin.tagall", this::tagAll));
        root.then(Commands.literal("untagall").requires(s -> permitted(s, "nomorecombatlog.admin.untagall")).executes(this::untagAll));
        root.then(playerCommand("debug", "nomorecombatlog.admin.debug", this::debug));
        root.then(reloadCommand());
        root.then(safeZoneCommand());
        return root;
    }

    private LiteralArgumentBuilder<CommandSourceStack> reloadCommand() {
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal("reload")
                .requires(s -> permitted(s, "nomorecombatlog.admin.reload") || s.getSender().hasPermission("nomorecombatlog.reload")
                        || s.getSender().hasPermission("nomorecombatlog.reloadCommand"))
                .executes(c -> reload(c, "all"));
        for (String target : List.of("config", "messages", "all")) reload.then(Commands.literal(target).executes(c -> reload(c, target)));
        return reload;
    }

    private LiteralArgumentBuilder<CommandSourceStack> safeZoneCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("safezone")
                .requires(s -> s.getSender().hasPermission("nomorecombatlog.safezone"));
        root.then(Commands.literal("add").requires(s -> s.getSender().hasPermission("nomorecombatlog.safezone.add"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.1)).executes(this::zoneAdd))));
        root.then(Commands.literal("cuboid").requires(s -> s.getSender().hasPermission("nomorecombatlog.safezone.add"))
                .then(Commands.argument("name", StringArgumentType.word())
                .then(Commands.argument("x1", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y1", DoubleArgumentType.doubleArg())
                .then(Commands.argument("z1", DoubleArgumentType.doubleArg())
                .then(Commands.argument("x2", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y2", DoubleArgumentType.doubleArg())
                .then(Commands.argument("z2", DoubleArgumentType.doubleArg()).executes(this::zoneCuboid)))))))));
        root.then(Commands.literal("list").requires(s -> s.getSender().hasPermission("nomorecombatlog.safezone.list")).executes(this::zoneList));
        root.then(Commands.literal("info").requires(s -> s.getSender().hasPermission("nomorecombatlog.safezone.list"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(this::zoneInfo)));
        root.then(Commands.literal("remove").requires(s -> s.getSender().hasPermission("nomorecombatlog.safezone.remove"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(this::zoneRemove)));
        return root;
    }

    private LiteralArgumentBuilder<CommandSourceStack> playerCommand(String name, String permission,
            com.mojang.brigadier.Command<CommandSourceStack> command) {
        return Commands.literal(name).requires(s -> permitted(s, permission))
                .then(Commands.argument("player", StringArgumentType.word()).executes(command));
    }

    private LiteralArgumentBuilder<CommandSourceStack> playerOptionalDuration(String name, String permission,
            com.mojang.brigadier.Command<CommandSourceStack> command) {
        return Commands.literal(name).requires(s -> permitted(s, permission))
                .then(Commands.argument("player", StringArgumentType.word()).executes(command)
                        .then(Commands.argument("duration", StringArgumentType.word()).executes(command)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> optionalDuration(String name, String permission,
            com.mojang.brigadier.Command<CommandSourceStack> command) {
        return Commands.literal(name).requires(s -> permitted(s, permission)).executes(command)
                .then(Commands.argument("duration", StringArgumentType.word()).executes(command));
    }

    private int info(CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage(Component.text("NoMoreCombatLog v" + plugin.getPluginMeta().getVersion()
                + " — /nmcl status <player>, /nmcl tag, /nmcl untag, /nmcl list"));
        return 1;
    }

    private int tag(CommandContext<CommandSourceStack> context) {
        if (!config.settings().enabled()) { context.getSource().getSender().sendMessage(Component.text("NoMoreCombatLog is disabled.")); return 0; }
        Player player = player(context); if (player == null) return 0;
        Duration duration = duration(context, config.settings().combat().duration()); if (duration == null) return 0;
        combat.tagPlayer(player, null, duration, CombatTagReason.ADMIN);
        messages.send(context.getSource().getSender(), "admin.tagged", Map.of("player", player.getName(), "duration", DurationParser.format(duration)));
        return 1;
    }

    private int untag(CommandContext<CommandSourceStack> context) {
        Player player = player(context); if (player == null) return 0;
        if (!combat.isTagged(player.getUniqueId())) { messages.send(context.getSource().getSender(), "commands.not-tagged", Map.of("player", player.getName())); return 0; }
        combat.untagPlayer(player.getUniqueId());
        messages.send(context.getSource().getSender(), "admin.untagged", Map.of("player", player.getName()));
        return 1;
    }

    private int extend(CommandContext<CommandSourceStack> context) {
        Player player = player(context); if (player == null) return 0;
        Duration duration = duration(context, null); if (duration == null) return 0;
        if (!combat.extendCombat(player.getUniqueId(), duration)) { messages.send(context.getSource().getSender(), "commands.not-tagged", Map.of("player", player.getName())); return 0; }
        messages.send(context.getSource().getSender(), "admin.extended", Map.of("player", player.getName(), "duration", DurationParser.format(duration)));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> context) { Player p = player(context); if (p == null) return 0; sendStatus(context.getSource().getSender(), p, false); return 1; }
    private int debug(CommandContext<CommandSourceStack> context) { Player p = player(context); if (p == null) return 0; sendStatus(context.getSource().getSender(), p, true); return 1; }

    private void sendStatus(CommandSender sender, Player player, boolean detailed) {
        CombatTag tag = combat.getTag(player.getUniqueId()).orElse(null);
        sender.sendMessage(Component.text("NMCL status for " + player.getName()));
        sender.sendMessage(Component.text("Combat: " + (tag != null) + " | remaining: " + DurationParser.format(combat.getRemainingTime(player.getUniqueId()))));
        sender.sendMessage(Component.text("Opponent: " + (tag == null ? "Unknown" : tag.opponentId().map(this::name).orElse("Unknown"))
                + " | reason: " + (tag == null ? "-" : tag.reason())));
        sender.sendMessage(Component.text("World: " + player.getWorld().getName() + " | safe zone: "
                + zones.at(player.getLocation()).map(SafeZone::name).orElse("none") + " | grace: " + grace.isProtected(player.getUniqueId())));
        if (detailed) sender.sendMessage(Component.text("World disabled: " + config.isWorldDisabled(player.getWorld().getName())
                + " | creative: " + player.getGameMode() + " | bypass: " + player.hasPermission("nomorecombatlog.bypass")
                + " | display: " + config.settings().display().type() + " | command mode: " + config.settings().commands().mode()
                + " | portals allowed: " + config.settings().teleport().portals() + " | pearls allowed: " + config.settings().teleport().enderPearls()));
    }

    private int list(CommandContext<CommandSourceStack> context) {
        List<CombatTag> tags = new ArrayList<>(combat.getActiveTags());
        tags.sort(Comparator.comparing(tag -> name(tag.playerId())));
        context.getSource().getSender().sendMessage(Component.text("Active combat tags: " + tags.size()));
        tags.forEach(tag -> context.getSource().getSender().sendMessage(Component.text("- " + name(tag.playerId()) + ": "
                + DurationParser.format(tag.remaining(System.currentTimeMillis())) + " (" + tag.reason() + ")")));
        return 1;
    }

    private int tagAll(CommandContext<CommandSourceStack> context) {
        Duration duration = duration(context, config.settings().combat().duration()); if (duration == null) return 0;
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) if (eligibility.check(player) == CombatEligibility.Result.ELIGIBLE) {
            combat.tagPlayer(player, null, duration, CombatTagReason.ADMIN); count++;
        }
        messages.send(context.getSource().getSender(), "admin.tagall", Map.of("count", count)); return count;
    }

    private int untagAll(CommandContext<CommandSourceStack> context) {
        int count = combat.getActiveTags().size(); combat.clearAll();
        messages.send(context.getSource().getSender(), "admin.untagall", Map.of("count", count)); return count;
    }

    private int reload(CommandContext<CommandSourceStack> context, String target) {
        try { plugin.reloadServices(target); messages.send(context.getSource().getSender(), "commands.reload-success", Map.of("target", target)); return 1; }
        catch (RuntimeException exception) { context.getSource().getSender().sendMessage(Component.text("Reload failed: " + exception.getMessage())); return 0; }
    }

    private int zoneAdd(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) return 0;
        zones.add(SafeZone.sphere(StringArgumentType.getString(context, "name"), player.getWorld().getName(),
                player.getX(), player.getY(), player.getZ(), DoubleArgumentType.getDouble(context, "radius"), true, false, true));
        player.sendMessage(Component.text("Safe zone added.")); return 1;
    }

    private int zoneCuboid(CommandContext<CommandSourceStack> c) {
        if (!(c.getSource().getSender() instanceof Player player)) return 0;
        zones.add(SafeZone.cuboid(StringArgumentType.getString(c, "name"), player.getWorld().getName(),
                d(c,"x1"), d(c,"y1"), d(c,"z1"), d(c,"x2"), d(c,"y2"), d(c,"z2"), true, false, true));
        player.sendMessage(Component.text("Cuboid safe zone added.")); return 1;
    }

    private int zoneList(CommandContext<CommandSourceStack> c) {
        c.getSource().getSender().sendMessage(Component.text("Safe zones: " + zones.zones().size()));
        zones.zones().forEach(z -> c.getSource().getSender().sendMessage(Component.text("- " + z.name() + " (" + z.type() + ", " + z.world() + ")")));
        return 1;
    }

    private int zoneInfo(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name");
        return zones.find(name).map(zone -> { c.getSource().getSender().sendMessage(Component.text(zone.toString())); return 1; }).orElse(0);
    }

    private int zoneRemove(CommandContext<CommandSourceStack> c) { return zones.remove(StringArgumentType.getString(c, "name")) ? 1 : 0; }
    private double d(CommandContext<CommandSourceStack> c, String key) { return DoubleArgumentType.getDouble(c, key); }

    private Player player(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "player");
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) messages.send(context.getSource().getSender(), "commands.player-not-found", Map.of("player", name));
        return player;
    }

    private Duration duration(CommandContext<CommandSourceStack> context, Duration fallback) {
        String raw;
        try { raw = StringArgumentType.getString(context, "duration"); }
        catch (IllegalArgumentException ignored) { return fallback; }
        Duration duration = DurationParser.parse(raw).orElse(null);
        if (duration == null || duration.isZero() || duration.isNegative()) messages.send(context.getSource().getSender(), "commands.invalid-duration");
        return duration == null || duration.isZero() || duration.isNegative() ? null : duration;
    }

    private boolean permitted(CommandSourceStack source, String permission) {
        return source.getSender().hasPermission(permission) || source.getSender().hasPermission("nomorecombatlog.admin");
    }
    private String name(java.util.UUID uuid) { String name = Bukkit.getOfflinePlayer(uuid).getName(); return name == null ? uuid.toString() : name; }
}
