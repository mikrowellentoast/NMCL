package org.mikrowellentoast.NMCL.safezone;

import org.bukkit.Location;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SafeZoneManager {
    private final SafeZoneStorage storage;
    private final List<SafeZone> zones = new ArrayList<>();
    private final Map<String, List<SafeZone>> byWorld = new HashMap<>();

    public SafeZoneManager(NoMoreCombatLog plugin) {
        storage = new SafeZoneStorage(plugin);
        reload();
    }

    public void reload() {
        zones.clear();
        zones.addAll(storage.load());
        reindex();
    }

    public void add(SafeZone zone) {
        remove(zone.name());
        zones.add(zone);
        reindex();
        storage.save(zones);
    }

    public boolean remove(String name) {
        boolean removed = zones.removeIf(zone -> zone.name().equalsIgnoreCase(name));
        if (removed) {
            reindex();
            storage.save(zones);
        }
        return removed;
    }

    public Optional<SafeZone> find(String name) { return zones.stream().filter(z -> z.name().equalsIgnoreCase(name)).findFirst(); }
    public Collection<SafeZone> zones() { return List.copyOf(zones); }
    public Optional<SafeZone> at(Location location) {
        if (location.getWorld() == null) return Optional.empty();
        return byWorld.getOrDefault(location.getWorld().getName().toLowerCase(Locale.ROOT), List.of()).stream()
                .filter(zone -> zone.contains(location)).findFirst();
    }

    private void reindex() {
        byWorld.clear();
        for (SafeZone zone : zones) byWorld.computeIfAbsent(zone.world().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(zone);
    }
}
