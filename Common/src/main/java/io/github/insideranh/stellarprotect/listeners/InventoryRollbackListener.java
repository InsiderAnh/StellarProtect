package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.InventoryRollbackSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class InventoryRollbackListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;

        Action action = event.getAction();

        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (!canRollback(playerProtect, event)) return;

        Block block = event.getClickedBlock();
        Location blockLocation = block.getLocation();

        if (!WorldUtils.isValidChestBlock(block.getType())) {
            if (!playerProtect.getInventoryRollbackSession().isSilent()) {
                plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.invalidBlock");
            }
            return;
        }

        plugin.getChestRollbackSessionManager().performInventoryRollback(player, blockLocation);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);

        if (playerProtect != null && isInRollbackMode(playerProtect)) {
            event.setCancelled(true);

            if (!playerProtect.getInventoryRollbackSession().isSilent()) {
                plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.cannotBreak");
            }
        }
    }

    private boolean canRollback(PlayerProtect playerProtect, Cancellable event) {
        if (playerProtect == null || !isInRollbackMode(playerProtect)) {
            return false;
        }

        event.setCancelled(true);
        return true;
    }

    private boolean isInRollbackMode(PlayerProtect playerProtect) {
        InventoryRollbackSession session = playerProtect.getInventoryRollbackSession();
        return session != null && session.isActive();
    }

}