package org.mikrowellentoast.NMCL.combat;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CombatTag {
    private final UUID playerId;
    private final long startedAt;
    private final CombatTagReason reason;
    private final LinkedHashSet<UUID> opponents = new LinkedHashSet<>();
    private UUID opponentId;
    private long expiresAt;

    public CombatTag(UUID playerId, UUID opponentId, long startedAt, long expiresAt, CombatTagReason reason) {
        this(playerId, opponentId, startedAt, expiresAt, reason, Set.of());
    }

    public CombatTag(UUID playerId, UUID opponentId, long startedAt, long expiresAt,
                     CombatTagReason reason, Collection<UUID> opponents) {
        if (expiresAt < startedAt) throw new IllegalArgumentException("expiresAt cannot be before startedAt");
        this.playerId = playerId;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.reason = reason == null ? CombatTagReason.OTHER : reason;
        updateOpponent(opponentId);
        if (opponents != null) this.opponents.addAll(opponents);
    }

    public UUID playerId() { return playerId; }
    public Optional<UUID> opponentId() { return Optional.ofNullable(opponentId); }
    public Set<UUID> opponents() { return Set.copyOf(opponents); }
    public long startedAt() { return startedAt; }
    public long expiresAt() { return expiresAt; }
    public CombatTagReason reason() { return reason; }
    public Duration totalDuration() { return Duration.ofMillis(Math.max(0, expiresAt - startedAt)); }
    public Duration remaining(long now) { return Duration.ofMillis(Math.max(0, expiresAt - now)); }
    public boolean isActive(long now) { return expiresAt > now; }

    void refresh(UUID opponent, long newExpiresAt) {
        expiresAt = Math.max(expiresAt, newExpiresAt);
        updateOpponent(opponent);
    }

    void extend(Duration duration) {
        try { expiresAt = Math.addExact(expiresAt, duration.toMillis()); }
        catch (ArithmeticException ignored) { expiresAt = Long.MAX_VALUE; }
    }

    private void updateOpponent(UUID opponent) {
        if (opponent != null && !opponent.equals(playerId)) {
            opponentId = opponent;
            opponents.add(opponent);
        }
    }
}
