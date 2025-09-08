package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;

public class BlockFormListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final ConfigManager configManager = plugin.getConfigManager();
    private final int airOrdinal = Material.AIR.ordinal();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        BlockState newState = event.getNewState();
        Material newType = newState.getType();

        try {
            if (newType.equals(Material.OBSIDIAN) || newType.equals(Material.COBBLESTONE) || newType.equals(Material.STONE) || newType.name().endsWith("_CONCRETE_POWDER")) {
                PlayerBlockLogEntry blockBreakEntry = new PlayerBlockLogEntry(-2L, newState, ActionType.BLOCK_PLACE);
                LoggerCache.addLog(blockBreakEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFormTo(BlockFromToEvent event) {
        Block oldBlock = event.getBlock();
        Block newBlock = event.getToBlock();

        Material oldMaterial = oldBlock.getType();
        Material newMaterial = newBlock.getType();
        String oldBlockName = oldMaterial.name();
        long userId = getEntityId(oldBlockName, oldBlock.getLocation());

        boolean isOldBlockAir = oldMaterial.ordinal() == airOrdinal;
        if (!isOldBlockAir) {
            PlayerBlockLogEntry blockBreakEntry = new PlayerBlockLogEntry(userId, newBlock.getLocation(), oldBlock, ActionType.BLOCK_PLACE);
            LoggerCache.addLog(blockBreakEntry);
        }

        boolean isNewBlockAir = newMaterial.ordinal() == airOrdinal;

        if (!isNewBlockAir) {
            PlayerBlockLogEntry blockBreakEntry = new PlayerBlockLogEntry(userId, newBlock, ActionType.BLOCK_BREAK);
            LoggerCache.addLog(blockBreakEntry);
        }
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