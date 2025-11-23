package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.xseries.XEntityType;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplodeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        long playerId = PlayerUtils.getPlayerOrEntityId("=explosion");

        event.blockList().forEach(block -> {
            if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntity(EntityExplodeEvent event) {
        long playerId;

        EntityType entityType = event.getEntityType();
        if (entityType == EntityType.CREEPER) {
            playerId = PlayerUtils.getEntityByDirectId("=creeper");
        } else if (entityType == EntityType.WITHER) {
            playerId = PlayerUtils.getEntityByDirectId("=wither");
        } else if (entityType == EntityType.WITHER_SKULL) {
            playerId = PlayerUtils.getEntityByDirectId("=wither");
        } else if (entityType == EntityType.FIREBALL) {
            playerId = PlayerUtils.getEntityByDirectId("=ghast");
        } else if (entityType.name().equals("MINECART_TNT")) {
            playerId = PlayerUtils.getEntityByDirectId("=tnt");
        } else if (entityType == XEntityType.TNT.get()) {
            playerId = PlayerUtils.getEntityByDirectId("=tnt");
        } else if (entityType == XEntityType.END_CRYSTAL.get()) {
            playerId = PlayerUtils.getEntityByDirectId("=end_crystal");
        } else {
            playerId = PlayerUtils.getPlayerOrEntityId("=explosion");
        }

        event.blockList().forEach(block -> {
            if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
        });
    }

}