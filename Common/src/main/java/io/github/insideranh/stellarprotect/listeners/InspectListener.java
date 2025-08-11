package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.InspectSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class InspectListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInspect(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null || !playerProtect.isInspect())
            return;
        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (playerProtect.getNextInspect() > System.currentTimeMillis())
            return;
        playerProtect.setNextInspect(System.currentTimeMillis() + 500L);

        Location blockLocation = event.getClickedBlock().getLocation();
        playerProtect.setInspectSession(new InspectSession(blockLocation, 0, 10, WorldUtils.isValidChestBlock(event.getClickedBlock().getType())));

        if (WorldUtils.isValidChestBlock(event.getClickedBlock().getType())) {
            plugin.getInspectHandler().handleChestInspection(player, blockLocation, 1, 0, 10);
        } else {
            plugin.getInspectHandler().handleBlockInspection(player, blockLocation, 1, 0, 10);
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

}