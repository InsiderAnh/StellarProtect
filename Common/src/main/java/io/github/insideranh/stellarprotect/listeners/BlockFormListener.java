package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockStateLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        BlockState newState = event.getNewState();
        Material newType = newState.getType();

        if (!configManager.isLiquidTracking()) {
            return;
        }

        try {
            boolean shouldLog = newType == Material.OBSIDIAN || newType == Material.COBBLESTONE || newType == Material.STONE || newType.name().endsWith("CONCRETE");
            if (shouldLog) {
                Long cachedPlayerId = BlockSourceCache.getPlayerId(block.getLocation());
                long playerId;

                if (cachedPlayerId == null) {
                    playerId = findLiquidSourcePlayer(block);
                } else {
                    playerId = cachedPlayerId;
                }

                if (playerId != -2L) {
                    PlayerBlockStateLogEntry blockStateLogEntry = new PlayerBlockStateLogEntry(playerId, block.getState(), newState, ActionType.BLOCK_PLACE);
                    LoggerCache.addLog(blockStateLogEntry);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private long findLiquidSourcePlayer(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        World world = block.getWorld();

        Location[] adjacentLocations = new Location[4];
        adjacentLocations[0] = new Location(world, x + 1, y, z);
        adjacentLocations[1] = new Location(world, x - 1, y, z);
        adjacentLocations[2] = new Location(world, x, y, z + 1);
        adjacentLocations[3] = new Location(world, x, y, z - 1);

        for (Location location : adjacentLocations) {
            Long cachedPlayerId = BlockSourceCache.getPlayerId(location);
            if (cachedPlayerId != null) {
                Block adjacentBlock = world.getBlockAt(location);
                Material adjacentType = adjacentBlock.getType();

                if (isLiquid(adjacentType)) {
                    return cachedPlayerId;
                }
            }

            LogEntry logEntry = LoggerCache.getPlacedBlockLog(location);
            if (logEntry != null) {
                Block adjacentBlock = world.getBlockAt(location);
                Material adjacentType = adjacentBlock.getType();

                if (isLiquid(adjacentType)) {
                    return logEntry.getPlayerId();
                }
            }
        }

        return -2L;
    }

    private boolean isLiquid(Material material) {
        String name = material.name();
        return material == Material.WATER || material == Material.LAVA || name.equals("STATIONARY_WATER") || name.equals("STATIONARY_LAVA");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();
        Block toBlock = event.getToBlock();
        Material fromType = fromBlock.getType();
        Material toType = toBlock.getType();
        World world = fromBlock.getWorld();

        if (!isLiquid(fromType)) {
            if (fromType == Material.DRAGON_EGG) {
                handleDragonEggTeleport(fromBlock, toBlock);
            }
            return;
        }

        if (!configManager.isLiquidTracking()) {
            return;
        }

        try {
            Long cachedPlayerId = BlockSourceCache.getPlayerId(fromBlock.getLocation());
            long playerId;

            if (cachedPlayerId != null) {
                playerId = cachedPlayerId;
            } else {
                LogEntry logEntry = LoggerCache.getPlacedBlockLog(fromBlock.getLocation());
                if (logEntry != null) {
                    playerId = logEntry.getPlayerId();
                } else {
                    playerId = fromType == Material.WATER ? -4L : -5L;
                }
            }

            if (toType == Material.AIR || toType.name().equals("CAVE_AIR")) {
                if (ActionType.BLOCK_PLACE.shouldSkipLog(world.getName(), fromType.name())) {
                    return;
                }

                BlockSourceCache.registerBlockSource(toBlock.getLocation(), playerId);

                PlayerBlockLogEntry entry = new PlayerBlockLogEntry(playerId, toBlock.getLocation(), fromBlock, ActionType.BLOCK_PLACE);
                LoggerCache.addLog(entry);
            } else {
                if (ActionType.BLOCK_BREAK.shouldSkipLog(world.getName(), toType.name())) {
                    return;
                }

                PlayerBlockLogEntry breakEntry = new PlayerBlockLogEntry(playerId, toBlock, ActionType.BLOCK_BREAK);
                LoggerCache.addLog(breakEntry);

                BlockSourceCache.registerBlockSource(toBlock.getLocation(), playerId);

                PlayerBlockLogEntry placeEntry = new PlayerBlockLogEntry(playerId, toBlock.getLocation(), fromBlock, ActionType.BLOCK_PLACE);
                LoggerCache.addLog(placeEntry);
            }
        } catch (Exception ignored) {
        }
    }

    private void handleDragonEggTeleport(Block fromBlock, Block toBlock) {
        long playerId = -1L;

        try {
            if (!ActionType.BLOCK_BREAK.shouldSkipLog(fromBlock.getWorld().getName(), Material.DRAGON_EGG.name())) {
                PlayerBlockLogEntry breakEntry = new PlayerBlockLogEntry(playerId, fromBlock, ActionType.BLOCK_BREAK);
                LoggerCache.addLog(breakEntry);
            }

            if (!ActionType.BLOCK_PLACE.shouldSkipLog(toBlock.getWorld().getName(), Material.DRAGON_EGG.name())) {
                PlayerBlockLogEntry placeEntry = new PlayerBlockLogEntry(playerId, toBlock.getLocation(), toBlock, ActionType.BLOCK_PLACE);
                LoggerCache.addLog(placeEntry);
            }
        } catch (Exception ignored) {
        }
    }

}