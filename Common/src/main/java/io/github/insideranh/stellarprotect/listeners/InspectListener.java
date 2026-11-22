package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.InspectSession;
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
import org.bukkit.inventory.EquipmentSlot;
import io.github.insideranh.stellarprotect.trackers.ChestTransactionTracker;

public class InspectListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInspect(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null)
            return;

        Action action = event.getAction();

        try {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return;
            }
        } catch (Throwable ignored) {
            // 1.8 compatibility
        }

        if (!action.equals(Action.LEFT_CLICK_BLOCK) && !action.equals(Action.RIGHT_CLICK_BLOCK))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (cantInspect(playerProtect, event))
            return;

        Block block = event.getClickedBlock();
        boolean isChest = WorldUtils.isValidChestBlock(block.getType());
        boolean isRightClick = action.equals(Action.RIGHT_CLICK_BLOCK);
        boolean isChestInspection = isChest && isRightClick;

        Location blockLocation = isChest
                ? ChestTransactionTracker.getCanonicalChestLocation(block)
                : block.getLocation();

        playerProtect.setLookupSession(null);
        playerProtect.setInspectSession(
                new InspectSession(blockLocation, 0, 10, isChestInspection));

        if (isChestInspection) {
            plugin.getInspectHandler().handleChestInspection(player, blockLocation, 1, 0, 10);
        } else {
            if (event.hasItem() && event.getItem().getType().isBlock()) {
                Block blockFace = block.getRelative(event.getBlockFace());
                playerProtect.setInspectSession(new InspectSession(blockFace.getLocation(), 0, 10, false));
                plugin.getInspectHandler().handleBlockInspection(player, blockFace.getLocation(), 1, 0, 10);
            } else {
                plugin.getInspectHandler().handleBlockInspection(player, blockLocation, 1, 0, 10);
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null || !playerProtect.isInspect())
            return;

        event.setCancelled(true);
    }

    boolean cantInspect(PlayerProtect playerProtect, Cancellable event) {
        if (playerProtect == null || !playerProtect.isInspect())
            return true;
        event.setCancelled(true);

        if (playerProtect.getNextInspect() > System.currentTimeMillis())
            return true;
        playerProtect.setNextInspect(System.currentTimeMillis() + 500L);

        return false;
    }

}