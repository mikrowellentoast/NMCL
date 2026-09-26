package org.mikrowellentoast.NMCL.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.config.ConfigManager;
import org.mikrowellentoast.NMCL.display.CombatDisplayManager;
import org.mikrowellentoast.NMCL.events.PlayerCombatEndEvent;
import org.mikrowellentoast.NMCL.events.PlayerCombatRefreshEvent;
import org.mikrowellentoast.NMCL.events.PlayerCombatStartEvent;
import org.mikrowellentoast.NMCL.storage.CombatStorage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CombatManager {
    private final NoMoreCombatLog plugin;
    private final ConfigManager config;
    private final CombatStorage storage;
    private final Map<UUID, CombatTag> tags = new HashMap<>();
    private CombatDisplayManager displays;

    public CombatManager(NoMoreCombatLog plugin, ConfigManager config, CombatStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
    }

    public void setDisplayManager(CombatDisplayManager displays) { this.displays = displays; }
    public void tagPlayer(Player player) { tagPlayer(player, null, config.settings().combat().duration(), CombatTagReason.PLUGIN_API); }
    public void tagPlayer(Player player, Duration duration) { tagPlayer(player, null, duration, CombatTagReason.PLUGIN_API); }
    public void tagPlayer(Player player, Player opponent) { tagPlayer(player, opponent, config.settings().combat().duration(), CombatTagReason.PVP); }

    public void tagPlayer(Player player, Player opponent, Duration duration, CombatTagReason reason) {
        if (!config.settings().enabled()) return;
        if (duration == null || duration.isZero() || duration.isNegative()) throw new IllegalArgumentException("duration must be positive");
        long now = System.currentTimeMillis();
        CombatTag existing = activeTag(player.getUniqueId(), now);
        UUID opponentId = opponent == null ? null : opponent.getUniqueId();
        if (existing == null) {
            CombatTag created = new CombatTag(player.getUniqueId(), opponentId, now, safeExpiry(now, duration), reason);
            PlayerCombatStartEvent event = new PlayerCombatStartEvent(player, opponent, created);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
            tags.put(player.getUniqueId(), created);
            if (displays != null) displays.onStart(player, created);
            debug("player tagged: " + player.getName() + " reason=" + reason);
        } else {
            existing.refresh(opponentId, safeExpiry(now, duration));
            Bukkit.getPluginManager().callEvent(new PlayerCombatRefreshEvent(player, opponent, existing));
            debug("tag refreshed: " + player.getName());
        }
        saveLater();
    }

    public boolean isTagged(UUID uuid) { return getTag(uuid).isPresent(); }

    public Optional<CombatTag> getTag(UUID uuid) {
        CombatTag tag = activeTag(uuid, System.currentTimeMillis());
        return Optional.ofNullable(tag);
    }

    public Duration getRemainingTime(UUID uuid) {
        return getTag(uuid).map(tag -> tag.remaining(System.currentTimeMillis())).orElse(Duration.ZERO);
    }

    public void untagPlayer(UUID uuid) {
        CombatTag removed = tags.remove(uuid);
        if (removed == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) Bukkit.getPluginManager().callEvent(new PlayerCombatEndEvent(player, removed));
        if (displays != null) displays.onEnd(uuid, player);
        debug("tag removed: " + uuid);
        saveLater();
    }

    public boolean extendCombat(UUID uuid, Duration duration) {
        if (!config.settings().enabled()) return false;
        CombatTag tag = activeTag(uuid, System.currentTimeMillis());
        if (tag == null || duration == null || duration.isNegative() || duration.isZero()) return false;
        tag.extend(duration);
        saveLater();
        return true;
    }

    public Collection<CombatTag> getActiveTags() {
        expireTags();
        return List.copyOf(tags.values());
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(tags.keySet())) untagPlayer(uuid);
    }

    public void restore(Collection<CombatTag> restored) {
        long now = System.currentTimeMillis();
        for (CombatTag tag : restored) if (tag.isActive(now)) tags.put(tag.playerId(), tag);
        debug("combat restored: " + tags.size() + " tag(s)");
    }

    public void expireTags() {
        long now = System.currentTimeMillis();
        for (UUID uuid : new ArrayList<>(tags.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            boolean invalidContext = !config.settings().enabled() || (player != null
                    && (config.isWorldDisabled(player.getWorld().getName())
                    || player.hasPermission("nomorecombatlog.bypass")
                    || (!config.settings().combat().creativeMode() && player.getGameMode() == org.bukkit.GameMode.CREATIVE)));
            if (invalidContext || !tags.get(uuid).isActive(now)) untagPlayer(uuid);
        }
    }

    private CombatTag activeTag(UUID uuid, long now) {
        CombatTag tag = tags.get(uuid);
        if (tag != null && !tag.isActive(now)) {
            untagPlayer(uuid);
            return null;
        }
        return tag;
    }

    private void saveLater() {
        if (config.settings().persistenceEnabled()) storage.requestSave(tags.values());
    }

    private void debug(String message) {
        if (config.settings().debug()) plugin.getLogger().info("[Debug] " + message);
    }

    private long safeExpiry(long now, Duration duration) {
        try { return Math.addExact(now, duration.toMillis()); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }
}
