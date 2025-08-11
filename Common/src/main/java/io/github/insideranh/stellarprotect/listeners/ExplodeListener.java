package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplodeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

            LoggerCache.addLog(new PlayerBlockLogEntry(-11L, block, ActionType.BLOCK_BREAK));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntity(EntityExplodeEvent event) {
        event.blockList().forEach(block -> {
            if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

            LoggerCache.addLog(new PlayerBlockLogEntry(-11L, block, ActionType.BLOCK_BREAK));
        });
    }

}