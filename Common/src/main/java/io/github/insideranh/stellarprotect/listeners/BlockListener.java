package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentTracker;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockStateLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTameEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

public class BlockListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private static final Method BURN_SOURCE_METHOD = findBurnSourceMethod();

    public static void processBlockBreak(Block block, @Nullable Player player, long defaultId) {
        StellarProtect plugin = StellarProtect.getInstance();
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

        Map<Location, Block> blockMap = new HashMap<>();
        blockMap.put(block.getLocation(), block);

        scanAdjacentBlocks(blockMap);

        for (Map.Entry<Location, Block> entry : blockMap.entrySet()) {
            Block affectedBlock = entry.getValue();
            if (ActionType.BLOCK_BREAK.shouldSkipLog(affectedBlock.getWorld().getName(), affectedBlock.getType().name())) {
                continue;
            }
            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, affectedBlock, ActionType.BLOCK_BREAK));
        }
    }

    private static void scanAdjacentBlocks(Map<Location, Block> blockMap) {
        Queue<Block> blocksToScan = new LinkedList<>(blockMap.values());

        while (!blocksToScan.isEmpty()) {
            Block block = blocksToScan.poll();
            Material material = block.getType();

            Block above = block.getRelative(0, 1, 0);
            if (AdjacentType.isUp(above.getType()) && !blockMap.containsKey(above.getLocation())) {
                blockMap.put(above.getLocation(), above);
                blocksToScan.add(above);

                List<Block> affectedAbove = AdjacentTracker.getAffectedBlocksAbove(block);
                for (Block affectedBlock : affectedAbove) {
                    if (!blockMap.containsKey(affectedBlock.getLocation())) {
                        blockMap.put(affectedBlock.getLocation(), affectedBlock);
                        blocksToScan.add(affectedBlock);
                    }
                }
            }

            List<Block> affectedSides = AdjacentTracker.getAffectedBlocksSide(block);
            for (Block affectedBlock : affectedSides) {
                if (!blockMap.containsKey(affectedBlock.getLocation())) {
                    blockMap.put(affectedBlock.getLocation(), affectedBlock);
                    blocksToScan.add(affectedBlock);
                }
            }

            if (material.hasGravity()) {
                Block gravityBlock = block.getRelative(0, 1, 0);
                while (gravityBlock.getType().hasGravity() && gravityBlock.getY() < gravityBlock.getWorld().getMaxHeight()) {
                    if (!blockMap.containsKey(gravityBlock.getLocation())) {
                        blockMap.put(gravityBlock.getLocation(), gravityBlock);
                        blocksToScan.add(gravityBlock);
                    }
                    gravityBlock = gravityBlock.getRelative(0, 1, 0);
                }
            }
        }
    }

    static long getPlayerId(@Nullable Player player, long defaultId) {
        if (player == null) return defaultId;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return defaultId;

        return playerProtect.getPlayerId();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        processBlockBreak(block, player, -2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockFade(org.bukkit.event.block.BlockFadeEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Material material = block.getType();
        BlockState newState = event.getNewState();
        if (isFire(material) && !isFire(newState.getType())) {
            BlockSourceCache.removeBlockSource(block.getLocation());
        }
        if (ActionType.BLOCK_FADE.shouldSkipLog(block.getWorld().getName(), material.name())) return;

        long playerId;
        if (material == Material.ICE || material.name().contains("FROSTED_ICE")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=ice_melt");
        } else if (material == Material.SNOW || material == Material.SNOW_BLOCK || material.name().contains("POWDER_SNOW")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=snow_fall");
        } else if (material == Material.FIRE || material.name().contains("FIRE")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=fire");
        } else {
            playerId = PlayerUtils.getPlayerOrEntityId("=natural");
        }

        BlockState oldState = block.getState();
        if (newState.getType() != Material.AIR && !oldState.getType().equals(newState.getType())) {
            plugin.getEventLogicHandler().onBlockFade(oldState, newState);
        } else {
            processBlockBreak(block, null, playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        long playerId;

        switch (event.getCause()) {
            case FLINT_AND_STEEL:
            case FIREBALL:
                Player player = event.getPlayer();
                playerId = player != null ? getPlayerId(player, -2L) : PlayerUtils.getPlayerOrEntityId("=fire");
                break;
            case LAVA:
                playerId = getFireSourceId(event.getIgnitingBlock(), "=lava");
                break;
            case LIGHTNING:
                playerId = PlayerUtils.getPlayerOrEntityId("=lightning");
                break;
            case EXPLOSION:
                playerId = PlayerUtils.getPlayerOrEntityId("=explosion");
                break;
            case SPREAD:
                playerId = getFireSourceId(event.getIgnitingBlock(), "=fire");
                if (event.getIgnitingBlock() == null) {
                    Long cachedId = BlockSourceCache.getPlayerId(block.getLocation());
                    if (cachedId != null) playerId = cachedId;
                }
                break;
            case ENDER_CRYSTAL:
            default:
                playerId = getFireSourceId(event.getIgnitingBlock(), "=fire");
                break;
        }

        BlockSourceCache.registerBlockSource(block.getLocation(), playerId);
        processBlockPlace(block, null, playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.isCancelled()) return;

        Block ignitingBlock = getBurnSource(event);
        Long playerId = ignitingBlock != null ? BlockSourceCache.getPlayerId(ignitingBlock.getLocation()) : null;
        if (playerId == null) {
            playerId = BlockSourceCache.getPlayerId(event.getBlock().getLocation());
        }
        if (playerId == null) {
            playerId = PlayerUtils.getPlayerOrEntityId("=fire");
        }

        processBlockBreak(event.getBlock(), null, playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        for (BlockState oldState : event.getReplacedBlockStates()) {
            processBlockPlace(oldState.getBlock(), oldState, player, -2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Entity entity = event.getEntity();
        long playerId;

        if (entity instanceof Player) {
            PlayerProtect playerProtect = PlayerProtect.getPlayer((Player) entity);
            playerId = playerProtect != null ? playerProtect.getPlayerId() : -2L;
        } else if (entity instanceof FallingBlock) {
            playerId = PlayerUtils.getPlayerOrEntityId("=gravity");
        } else if (entity.getType() == EntityType.ENDERMAN) {
            playerId = PlayerUtils.getEntityByDirectId("=enderman");
        } else if (entity.getType() == EntityType.WITHER) {
            playerId = PlayerUtils.getEntityByDirectId("=wither");
        } else if (entity.getType() == EntityType.SILVERFISH) {
            playerId = PlayerUtils.getEntityByDirectId("=silverfish");
        } else {
            playerId = PlayerUtils.getEntityByDirectId("=" + entity.getType().name().toLowerCase());
        }

        Material to = event.getTo();
        if (to == Material.AIR) {
            processBlockBreak(block, null, playerId);
        } else {
            processBlockPlace(block, null, playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;

        Block pistonBlock = event.getBlock();
        if (ActionType.BLOCK_BREAK.shouldSkipLog(pistonBlock.getWorld().getName(), "=piston")) return;

        long playerId = PlayerUtils.getPlayerOrEntityId("=piston");

        for (Block block : event.getBlocks()) {
            Location from = block.getLocation();
            Location to = block.getRelative(event.getDirection()).getLocation();

            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, from, block, ActionType.BLOCK_BREAK));
            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, to, block, ActionType.BLOCK_PLACE));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;

        Block pistonBlock = event.getBlock();
        if (ActionType.BLOCK_BREAK.shouldSkipLog(pistonBlock.getWorld().getName(), "=piston")) return;

        long playerId = PlayerUtils.getPlayerOrEntityId("=piston");

        for (Block block : event.getBlocks()) {
            Location from = block.getLocation();
            Location to = block.getRelative(event.getDirection()).getLocation();

            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, from, block, ActionType.BLOCK_BREAK));
            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, to, block, ActionType.BLOCK_PLACE));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        processBlockPlace(
            event.getBlockPlaced(),
            event.getBlockReplacedState(),
            event.getPlayer(),
            -2L
        );
        if (isFire(event.getBlockPlaced().getType())) {
            BlockSourceCache.registerBlockSource(event.getBlockPlaced().getLocation(), getPlayerId(event.getPlayer(), -2L));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.isCancelled()) return;

        BlockState blockState = event.getBlock().getState();
        BlockState newState = event.getNewState();
        Material newMaterial = newState.getType();
        Long fireSourceId = null;
        if (isFire(newMaterial)) {
            fireSourceId = BlockSourceCache.getPlayerId(event.getSource().getLocation());
            if (fireSourceId == null) fireSourceId = PlayerUtils.getEntityByDirectId("=fire");
            BlockSourceCache.registerBlockSource(event.getBlock().getLocation(), fireSourceId);
        }

        if (ActionType.BLOCK_PLACE.shouldSkipLog(blockState.getWorld().getName(), newMaterial.name())) return;

        long playerId;

        if (BlockTracker.isChorusState(newMaterial)) {
            playerId = PlayerUtils.getEntityByDirectId("=chorus");
        } else if (BlockTracker.isAmethystState(newMaterial)) {
            playerId = PlayerUtils.getEntityByDirectId("=amethyst");
        } else if (BlockTracker.isBambooState(newMaterial)) {
            playerId = PlayerUtils.getEntityByDirectId("=bamboo");
        } else if (BlockTracker.isSculkState(newMaterial)) {
            playerId = PlayerUtils.getEntityByDirectId("=sculk");
        } else if (BlockTracker.isVineState(newMaterial)) {
            playerId = PlayerUtils.getEntityByDirectId("=vine");
        } else if (fireSourceId != null) {
            playerId = fireSourceId;
        } else {
            playerId = PlayerUtils.getEntityByDirectId("=natural");
            return;
        }

        processBlockStatePlace(event.getBlock().getLocation(), blockState, newState, playerId);
    }

    void processBlockStatePlace(Location location, BlockState blockState, BlockState newState, long playerId) {
        if (newState.getType().equals(Material.AIR) || ActionType.BLOCK_SPREAD.shouldSkipLog(newState.getWorld().getName(), newState.getType().name()))
            return;

        LoggerCache.addLog(new PlayerBlockStateLogEntry(playerId, location, blockState, newState, ActionType.BLOCK_SPREAD));
    }

    void processBlockPlace(Block block, @Nullable Player player, long defaultId) {
        if (block.getType().equals(Material.AIR) || ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name()))
            return;

        long playerId = getPlayerId(player, defaultId);

        if (plugin.getNexoHook() != null && player != null && plugin.getNexoHook().isNexoListener(block, plugin.getProtectNMS().getItemInHand(player))) {
            return;
        }
        if (plugin.getItemsAdderHook() != null && player != null && plugin.getItemsAdderHook().isItemsAdderListener(block, plugin.getProtectNMS().getItemInHand(player))) {
            return;
        }

        LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_PLACE));
    }

    private void processBlockPlace(Block block, BlockState oldState, @Nullable Player player, long defaultId) {
        if (block.getType().equals(Material.AIR)
            || ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name())) {
            return;
        }

        long playerId = getPlayerId(player, defaultId);

        if (plugin.getNexoHook() != null
            && player != null
            && plugin.getNexoHook().isNexoListener(block, plugin.getProtectNMS().getItemInHand(player))) {
            return;
        }

        if (plugin.getItemsAdderHook() != null
            && player != null
            && plugin.getItemsAdderHook().isItemsAdderListener(block, plugin.getProtectNMS().getItemInHand(player))) {
            return;
        }

        LoggerCache.addLog(new PlayerBlockLogEntry(
            playerId,
            oldState,
            block.getState(),
            ActionType.BLOCK_PLACE
        ));
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


    private static boolean isFire(Material material) {
        return material == Material.FIRE || "SOUL_FIRE".equals(material.name());
    }

    private static long getFireSourceId(@Nullable Block source, String fallback) {
        if (source != null) {
            Long playerId = BlockSourceCache.getPlayerId(source.getLocation());
            if (playerId != null) return playerId;
        }
        return PlayerUtils.getPlayerOrEntityId(fallback);
    }

    private static Method findBurnSourceMethod() {
        try {
            return BlockBurnEvent.class.getMethod("getIgnitingBlock");
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    @Nullable
    private static Block getBurnSource(BlockBurnEvent event) {
        if (BURN_SOURCE_METHOD == null) return null;
        try {
            return (Block) BURN_SOURCE_METHOD.invoke(event);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return null;
        }
    }

}
