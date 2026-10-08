package org.mikrowellentoast.NMCL.api;

import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.combat.CombatManager;
import org.mikrowellentoast.NMCL.combat.CombatTag;
import org.mikrowellentoast.NMCL.combat.CombatTagReason;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public final class NMCLApiImpl implements NMCLApi {
    private final CombatManager combat;
    public NMCLApiImpl(CombatManager combat) { this.combat = combat; }
    @Override public boolean isInCombat(UUID player) { return combat.isTagged(player); }
    @Override public Optional<CombatTag> getCombatTag(UUID player) { return combat.getTag(player); }
    @Override public Duration getRemainingTime(UUID player) { return combat.getRemainingTime(player); }
    @Override public Optional<UUID> getOpponent(UUID player) { return combat.getTag(player).flatMap(CombatTag::opponentId); }
    @Override public void tag(Player player) { combat.tagPlayer(player); }
    @Override public void tag(Player player, Duration duration) { combat.tagPlayer(player, duration); }
    @Override public void tag(Player player, Player opponent, Duration duration) {
        combat.tagPlayer(player, opponent, duration, CombatTagReason.PLUGIN_API);
    }
    @Override public void untag(Player player) { combat.untagPlayer(player.getUniqueId()); }
}
