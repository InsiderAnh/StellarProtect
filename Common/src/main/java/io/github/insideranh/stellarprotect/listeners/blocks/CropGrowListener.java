package io.github.insideranh.stellarprotect.listeners.blocks;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.world.CropGrowLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public class CropGrowListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGrowCrop(BlockGrowEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.CROP_GROW.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

        CropGrowLogEntry logEntry = new CropGrowLogEntry(block);
        if (logEntry.getAge() == 0) return;

        LoggerCache.addLog(logEntry);
    }

}