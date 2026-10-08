package org.mikrowellentoast.NMCL.api;

import org.bukkit.entity.Player;
import org.mikrowellentoast.NMCL.combat.CombatTag;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface NMCLApi {
    boolean isInCombat(UUID player);
    Optional<CombatTag> getCombatTag(UUID player);
    Duration getRemainingTime(UUID player);
    Optional<UUID> getOpponent(UUID player);
    void tag(Player player);
    void tag(Player player, Duration duration);
    void tag(Player player, Player opponent, Duration duration);
    void untag(Player player);
}
