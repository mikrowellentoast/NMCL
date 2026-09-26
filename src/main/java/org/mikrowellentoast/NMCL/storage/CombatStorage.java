package org.mikrowellentoast.NMCL.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;
import org.mikrowellentoast.NMCL.combat.CombatTag;
import org.mikrowellentoast.NMCL.combat.CombatTagReason;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class CombatStorage {
    private final NoMoreCombatLog plugin;
    private final File file;
    private volatile List<Snapshot> latest = List.of();
    private volatile BukkitTask pending;
    private volatile long revision;

    public CombatStorage(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "combat-data.yml");
    }

    public List<CombatTag> load() {
        if (!file.isFile()) return List.of();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("tags");
        if (section == null) return List.of();
        long now = System.currentTimeMillis();
        List<CombatTag> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            try {
                UUID player = UUID.fromString(key);
                String opponentValue = section.getString(key + ".opponent");
                UUID opponent = opponentValue == null || opponentValue.isBlank() ? null : UUID.fromString(opponentValue);
                long started = section.getLong(key + ".started-at", now);
                long expires = section.getLong(key + ".expires-at");
                if (expires <= now) continue;
                CombatTagReason reason;
                try { reason = CombatTagReason.valueOf(section.getString(key + ".reason", "RESTORED")); }
                catch (IllegalArgumentException ignored) { reason = CombatTagReason.RESTORED; }
                List<UUID> opponents = new ArrayList<>();
                for (String value : section.getStringList(key + ".opponents")) opponents.add(UUID.fromString(value));
                result.add(new CombatTag(player, opponent, started, expires, reason, opponents));
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Ignoring invalid persisted combat tag '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    public void requestSave(Collection<CombatTag> tags) {
        latest = snapshot(tags);
        revision++;
        if (pending != null) return;
        scheduleSave();
    }

    public void saveNow(Collection<CombatTag> tags) {
        if (pending != null) {
            pending.cancel();
            pending = null;
        }
        saveSnapshot(snapshot(tags));
    }

    private List<Snapshot> snapshot(Collection<CombatTag> tags) {
        return tags.stream().map(tag -> new Snapshot(tag.playerId(), tag.opponentId().orElse(null),
                tag.opponents().stream().toList(), tag.startedAt(), tag.expiresAt(), tag.reason())).toList();
    }

    private void scheduleSave() {
        pending = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            long savedRevision = revision;
            List<Snapshot> snapshot = latest;
            saveSnapshot(snapshot);
            Bukkit.getScheduler().runTask(plugin, () -> {
                pending = null;
                if (revision != savedRevision) scheduleSave();
            });
        }, 20L);
    }

    private synchronized void saveSnapshot(Collection<Snapshot> tags) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Snapshot tag : tags) {
            String path = "tags." + tag.player();
            yaml.set(path + ".opponent", tag.opponent() == null ? null : tag.opponent().toString());
            yaml.set(path + ".opponents", tag.opponents().stream().map(UUID::toString).toList());
            yaml.set(path + ".started-at", tag.startedAt());
            yaml.set(path + ".expires-at", tag.expiresAt());
            yaml.set(path + ".reason", tag.reason().name());
        }
        try { yaml.save(file); }
        catch (IOException exception) { plugin.getLogger().severe("Could not save combat-data.yml: " + exception.getMessage()); }
    }

    private record Snapshot(UUID player, UUID opponent, List<UUID> opponents, long startedAt,
                            long expiresAt, CombatTagReason reason) {}
}
