package org.mikrowellentoast.NMCL.display;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.combat.CombatTag;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.messages.MessageManager;
import org.mikrowellentoast.NMCL.util.DurationParser;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatDisplayManager {
    private final ConfigManager config;
    private final MessageManager messages;
    private final CombatManager combat;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public CombatDisplayManager(ConfigManager config, MessageManager messages, CombatManager combat) {
        this.config = config;
        this.messages = messages;
        this.combat = combat;
    }

    public void onStart(Player player, CombatTag tag) {
        Map<String, Object> values = values(player, tag);
        messages.send(player, "combat.started", values);
        if (effectiveType() == DisplayType.TITLE) {
            player.showTitle(Title.title(messages.component("combat.title", values, false),
                    messages.component("combat.subtitle", values, false)));
        }
    }

    public void onEnd(UUID uuid, Player player) {
        BossBar bar = bossBars.remove(uuid);
        if (player != null) {
            if (bar != null) player.hideBossBar(bar);
            messages.send(player, "combat.ended");
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (CombatTag tag : combat.getActiveTags()) {
            Player player = Bukkit.getPlayer(tag.playerId());
            if (player == null || !player.isOnline()) {
                hide(tag.playerId(), player);
                continue;
            }
            Map<String, Object> values = values(player, tag);
            DisplayType type = effectiveType();
            if (type == DisplayType.ACTION_BAR) {
                player.sendActionBar(messages.component("combat.action-bar", values, false));
                hide(tag.playerId(), player);
            } else if (type == DisplayType.BOSS_BAR) {
                long denominator = tag.reason() == org.mikrowellentoast.NMCL.combat.CombatTagReason.PVP
                        ? config.settings().combat().duration().toMillis() : tag.totalDuration().toMillis();
                float progress = (float) Math.max(0.0, Math.min(1.0,
                        tag.remaining(now).toMillis() / (double) Math.max(1, denominator)));
                BossBar bar = bossBars.computeIfAbsent(tag.playerId(), ignored -> {
                    BossBar created = BossBar.bossBar(messages.component("combat.boss-bar", values, false), progress,
                            BossBar.Color.RED, BossBar.Overlay.PROGRESS);
                    player.showBossBar(created);
                    return created;
                });
                bar.name(messages.component("combat.boss-bar", values, false));
                bar.progress(progress);
            } else {
                hide(tag.playerId(), player);
            }
        }
    }

    public void reload() {
        if (effectiveType() != DisplayType.BOSS_BAR) hideAll();
    }

    public void hideAll() {
        for (var entry : new HashMap<>(bossBars).entrySet()) hide(entry.getKey(), Bukkit.getPlayer(entry.getKey()));
    }

    private void hide(UUID uuid, Player player) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null && player != null) player.hideBossBar(bar);
    }

    private DisplayType effectiveType() {
        var display = config.settings().display();
        return switch (display.type()) {
            case ACTION_BAR -> display.actionBar() ? DisplayType.ACTION_BAR : DisplayType.NONE;
            case BOSS_BAR -> display.bossBar() ? DisplayType.BOSS_BAR : DisplayType.NONE;
            case TITLE -> display.title() ? DisplayType.TITLE : DisplayType.NONE;
            case NONE -> DisplayType.NONE;
        };
    }

    private Map<String, Object> values(Player player, CombatTag tag) {
        String opponent = tag.opponentId().map(id -> {
            Player online = Bukkit.getPlayer(id);
            return online == null ? Bukkit.getOfflinePlayer(id).getName() : online.getName();
        }).orElse("Unknown");
        Duration remaining = tag.remaining(System.currentTimeMillis());
        return Map.of("player", player.getName(), "opponent", opponent == null ? "Unknown" : opponent,
                "time", DurationParser.format(remaining), "duration", DurationParser.format(tag.totalDuration()),
                "reason", tag.reason().name(), "world", player.getWorld().getName());
    }
}
