package org.mikrowellentoast.NMCL.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.mikrowellentoast.NMCL.combat.CombatTag;

public final class PlayerCombatLogEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CombatTag tag;

    public PlayerCombatLogEvent(Player player, CombatTag tag) {
        super(player);
        this.tag = tag;
    }
    public CombatTag getCombatTag() { return tag; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
