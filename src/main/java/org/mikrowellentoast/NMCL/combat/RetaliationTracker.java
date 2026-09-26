package org.mikrowellentoast.NMCL.combat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RetaliationTracker {
    private final Map<UUID, Attack> attacks = new HashMap<>();

    public void recordAttack(UUID victim, UUID attacker, long now) {
        attacks.put(victim, new Attack(attacker, now));
    }

    public boolean consumeRetaliation(UUID attacker, UUID victim, long now, Duration window) {
        Attack prior = attacks.get(attacker);
        if (prior == null || !prior.attacker().equals(victim) || now - prior.timestamp() > window.toMillis()) return false;
        attacks.remove(attacker);
        return true;
    }

    public void cleanup(long now, Duration window) {
        Iterator<Attack> iterator = attacks.values().iterator();
        while (iterator.hasNext()) if (now - iterator.next().timestamp() > window.toMillis()) iterator.remove();
    }

    public int size() { return attacks.size(); }
    private record Attack(UUID attacker, long timestamp) {}
}
