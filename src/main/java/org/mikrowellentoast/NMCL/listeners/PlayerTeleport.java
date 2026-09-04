package org.mikrowellentoast.NMCL.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.mikrowellentoast.NMCL.config.ConfigManager;

public class PlayerTeleport implements Listener {

    private final CombatListener combatListener;
    private final ConfigManager config = ConfigManager.getInstance();

    public PlayerTeleport(CombatListener combatListener) {
        this.combatListener = combatListener;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();


        if (combatListener.isCombatTagged(player.getUniqueId())
                && !config.isAllowEnderPearlInCombat()
                && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            event.setCancelled(true);

            player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
            player.updateInventory();

            player.sendMessage("§cYou cannot teleport while combat tagged!");



        }
    }
}