package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentTracker;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTameEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        processBlockBreak(block, player, -2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) return;

        processBlockPlace(event.getBlock(), null, PlayerUtils.getPlayerOrEntityId("=fire"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.isCancelled()) return;

        processBlockBreak(event.getBlock(), null, PlayerUtils.getPlayerOrEntityId("=fire"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        processBlockPlace(block, player, -2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.isCancelled()) return;

        BlockState newState = event.getNewState();
        Material newMaterial = newState.getType();
        if (BlockTracker.isVineState(newMaterial)) {
            processBlockStatePlace(newState, null, PlayerUtils.getPlayerOrEntityId("=vine"));
        } else if (BlockTracker.isSculkState(newMaterial)) {
            processBlockStatePlace(newState, null, PlayerUtils.getPlayerOrEntityId("=sculk"));
        } else if (BlockTracker.isChorusState(newMaterial)) {
            processBlockStatePlace(newState, null, PlayerUtils.getPlayerOrEntityId("=chorus"));
        } else if (BlockTracker.isAmethystState(newMaterial)) {
            processBlockStatePlace(newState, null, PlayerUtils.getPlayerOrEntityId("=amethyst"));
        } else if (BlockTracker.isBambooState(newMaterial)) {
            processBlockStatePlace(newState, null, PlayerUtils.getPlayerOrEntityId("=bamboo"));
        }
        //plugin.getLogger().info("Block spread event " + newMaterial + " data: " + plugin.getDataBlock(event.getNewState()).getBlockDataString());
    }

    void processBlockBreak(Block block, @Nullable Player player, long defaultId) {
        Material material = block.getType();
        if (block.getType().equals(Material.AIR) || ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), material.name()))
            return;

        long playerId = getPlayerId(player, defaultId);

        if (plugin.getNexoHook() != null && plugin.getNexoHook().isNexoBlock(block)) {
            return;
        }
        if (plugin.getItemsAdderHook() != null && plugin.getItemsAdderHook().isItemsAdderBlock(block)) {
            return;
        }

        if (AdjacentType.isUp(material)) {
            List<Block> affectedBlocks = AdjacentTracker.getAffectedBlocksAbove(block);
            for (Block affectedBlock : affectedBlocks) {
                LoggerCache.addLog(new PlayerBlockLogEntry(playerId, affectedBlock, ActionType.BLOCK_BREAK));
            }
        }

        if (AdjacentType.isSide(material)) {
            List<Block> affectedBlocks = AdjacentTracker.getAffectedBlocksSide(block);
            for (Block affectedBlock : affectedBlocks) {
                LoggerCache.addLog(new PlayerBlockLogEntry(playerId, affectedBlock, ActionType.BLOCK_BREAK));
            }
        }

        LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
    }

    void processBlockStatePlace(BlockState blockState, @Nullable Player player, long defaultId) {
        if (blockState.getType().equals(Material.AIR) || ActionType.BLOCK_PLACE.shouldSkipLog(blockState.getWorld().getName(), blockState.getType().name()))
            return;

        long playerId = getPlayerId(player, defaultId);

        LoggerCache.addLog(new PlayerBlockLogEntry(playerId, blockState, ActionType.BLOCK_PLACE));
    }

    void processBlockPlace(Block block, @Nullable Player player, long defaultId) {
        if (block.getType().equals(Material.AIR) || ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name()))
            return;

        long playerId = getPlayerId(player, defaultId);

        if (plugin.getNexoHook() != null && plugin.getNexoHook().isNexoListener(block, plugin.getProtectNMS().getItemInHand(player))) {
            return;
        }
        if (plugin.getItemsAdderHook() != null && plugin.getItemsAdderHook().isItemsAdderListener(block, plugin.getProtectNMS().getItemInHand(player))) {
            return;
        }

        LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_PLACE));
    }

    long getPlayerId(@Nullable Player player, long defaultId) {
        if (player == null) return defaultId;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return defaultId;

        return playerProtect.getPlayerId();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), "=decay")) return;

        LoggerCache.addLog(new PlayerBlockLogEntry(PlayerUtils.getEntityByDirectId("=decay"), block, ActionType.BLOCK_BREAK));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Block block = event.getBlock();
        if (ActionType.FURNACE_EXTRACT.shouldSkipLog(block.getWorld().getName(), event.getItemType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(new ItemStack(event.getItemType(), event.getItemAmount()));

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, block.getLocation(), ActionType.FURNACE_EXTRACT));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;

        Player player = (Player) event.getOwner();

        if (!(event.getEntity() instanceof Animals)) return;
        Animals animal = (Animals) event.getEntity();
        if (ActionType.TAME.shouldSkipLog(animal.getWorld().getName(), animal.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerTameEntry(playerProtect.getPlayerId(), animal));
    }

}