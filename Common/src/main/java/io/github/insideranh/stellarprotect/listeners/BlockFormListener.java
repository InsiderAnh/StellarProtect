package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

public class BlockFormListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final ConfigManager configManager = plugin.getConfigManager();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFormTo(BlockFromToEvent event) {
        Block oldBlock = event.getBlock();
        Block newBlock = event.getToBlock();

        String oldBlockName = oldBlock.getType().name();
        long userId = getEntityId(oldBlockName, oldBlock.getLocation());

        boolean isOldBlockAir = this.isAir(oldBlockName);
        if (!isOldBlockAir) {
            PlayerBlockLogEntry blockBreakEntry = new PlayerBlockLogEntry(userId, oldBlock, ActionType.BLOCK_BREAK);
            LoggerCache.addLog(blockBreakEntry);
        }

        String newBlockName = newBlock.getType().name();
        boolean isNewBlockAir = this.isAir(newBlockName);

        if (!isNewBlockAir) {
            PlayerBlockLogEntry blockBreakEntry = new PlayerBlockLogEntry(userId, oldBlock, ActionType.BLOCK_BREAK);
            LoggerCache.addLog(blockBreakEntry);
        }
    }

    private boolean isAir(String blockName) {
        return blockName.startsWith("AIR");
    }

    private long getEntityId(String name, Location location) {
        if (name.equals("LAVA") || name.equals("STATIONARY_LAVA")) {
            return -5L;
        }
        if (name.equals("WATER") || name.equals("STATIONARY_WATER")) {
            return -4L;
        }
        LogEntry logEntry = LoggerCache.getPlacedBlockLog(location);
        // If the block is not placed =natural
        if (logEntry == null || !configManager.isLiquidTracking()) return -2L;
        return logEntry.getPlayerId();
    }

}