package org.mikrowellentoast.NMCL.combat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class GracePeriodManager {
    private final Map<UUID, Long> protectedUntil = new HashMap<>();

    public void protect(UUID player, Duration duration) {
        if (!duration.isZero() && !duration.isNegative()) protectedUntil.put(player, System.currentTimeMillis() + duration.toMillis());
    }

    public boolean isProtected(UUID player) {
        Long until = protectedUntil.get(player);
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            protectedUntil.remove(player);
            return false;
        }
        return true;
    }

    public Duration remaining(UUID player) {
        Long until = protectedUntil.get(player);
        return Duration.ofMillis(until == null ? 0 : Math.max(0, until - System.currentTimeMillis()));
    }

    public void clear(UUID player) { protectedUntil.remove(player); }
    public void cleanup(long now) {
        Iterator<Long> iterator = protectedUntil.values().iterator();
        while (iterator.hasNext()) if (iterator.next() <= now) iterator.remove();
    }
}
