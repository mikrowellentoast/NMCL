package org.mikrowellentoast.NMCL.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.mikrowellentoast.NMCL.combat.CombatTag;

public final class PlayerCombatRefreshEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player opponent;
    private final CombatTag tag;

    public PlayerCombatRefreshEvent(Player player, Player opponent, CombatTag tag) {
        super(player);
        this.opponent = opponent;
        this.tag = tag;
    }
    public Player getOpponent() { return opponent; }
    public CombatTag getCombatTag() { return tag; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
