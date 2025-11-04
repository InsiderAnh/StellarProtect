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

        Block block = event.getBlock();
        Location sourceLocation = block.getLocation();
        long playerId;

        switch (event.getCause()) {
            case FLINT_AND_STEEL:
            case FIREBALL:
                Player player = event.getPlayer();
                if (player != null) {
                    PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
                    playerId = playerProtect != null ? playerProtect.getPlayerId() : -2L;
                } else {
                    playerId = PlayerUtils.getPlayerOrEntityId("=fire");
                }
                break;
            case LAVA:
                if (block.getType() == Material.FIRE || block.getType().name().contains("FIRE")) {
                    if (!hasBurnableBlocksNearby(block)) {
                        return;
                    }
                }

                playerId = PlayerUtils.getPlayerOrEntityId("=lava");
                sourceLocation = findNearbyLavaSource(block.getLocation());
                if (sourceLocation != null) {
                    Long sourcePlayerId = BlockSourceCache.getPlayerId(sourceLocation);
                    if (sourcePlayerId != null) {
                        playerId = sourcePlayerId;
                    }
                }
                break;
            case LIGHTNING:
                playerId = PlayerUtils.getPlayerOrEntityId("=lightning");
                break;
            case EXPLOSION:
                playerId = PlayerUtils.getPlayerOrEntityId("=explosion");
                break;
            case SPREAD:
            case ENDER_CRYSTAL:
            default:
                playerId = PlayerUtils.getPlayerOrEntityId("=fire");
                sourceLocation = findNearbyFireSource(block.getLocation());
                if (sourceLocation != null) {
                    Location fireSource = findNearbyFireSource(sourceLocation);
                    if (fireSource != null) {
                        Long sourcePlayerId = BlockSourceCache.getPlayerId(fireSource);
                        if (sourcePlayerId != null) {
                            playerId = sourcePlayerId;
                        }

                        processBlockBreak(sourceLocation.getBlock(), null, playerId);
                    }
                }
                break;
        }
        BlockSourceCache.registerBlockSource(block.getLocation(), playerId, sourceLocation);

        processBlockPlace(block, null, playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.isCancelled()) return;

        processBlockBreak(event.getBlock(), null, PlayerUtils.getPlayerOrEntityId("=fire"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockFade(BlockFadeEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Material material = block.getType();
        long playerId;

        if (material == Material.ICE || material.name().contains("FROSTED_ICE")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=ice_melt");
        } else if (material == Material.SNOW || material == Material.SNOW_BLOCK || material.name().contains("POWDER_SNOW")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=snow_fall");
        } else if (material == Material.FIRE || material.name().contains("FIRE")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=fire");
        } else if (material.name().contains("CORAL")) {
            playerId = PlayerUtils.getPlayerOrEntityId("=natural");
        } else {
            playerId = PlayerUtils.getPlayerOrEntityId("=natural");
        }

        if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), material.name())) return;

        BlockState newState = event.getNewState();
        if (newState.getType() != Material.AIR) {
            processBlockStatePlace(block.getLocation(), block.getState(), newState, playerId);
        } else {
            processBlockBreak(block, null, playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();

        for (BlockState state : event.getReplacedBlockStates()) {
            processBlockPlace(state.getBlock(), player, -2L);
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

        Block block = event.getBlock();
        Player player = event.getPlayer();

        processBlockPlace(block, player, -2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.isCancelled()) return;

        BlockState blockState = event.getBlock().getState();
        BlockState newState = event.getNewState();
        Material newMaterial = newState.getType();
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
        } else if (newMaterial == Material.FIRE || newMaterial.name().contains("FIRE")) {
            playerId = PlayerUtils.getEntityByDirectId("=fire");
        } else if (newMaterial.name().contains("MUSHROOM")) {
            playerId = PlayerUtils.getEntityByDirectId("=natural");
        } else if (newMaterial.name().equals("GRASS_BLOCK") || newMaterial.name().equals("GRASS") || newMaterial.name().equals("MYCELIUM")) {
            playerId = PlayerUtils.getEntityByDirectId("=natural");
        } else if (newMaterial.name().contains("KELP") || newMaterial.name().contains("SEAGRASS")) {
            playerId = PlayerUtils.getEntityByDirectId("=natural");
        } else {
            playerId = PlayerUtils.getEntityByDirectId("=natural");
        }

        processBlockStatePlace(event.getBlock().getLocation(), blockState, newState, playerId);
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


    private Location findNearbyFireSource(Location location) {
        if (location == null || location.getWorld() == null) return null;

        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Location checkLoc = location.clone().add(x, y, z);
                    Block checkBlock = checkLoc.getBlock();

                    if (checkBlock.getType() == Material.FIRE || checkBlock.getType().name().contains("FIRE")) {
                        if (BlockSourceCache.getPlayerId(checkLoc) != null) {
                            return checkLoc;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Location findNearbyLavaSource(Location location) {
        if (location == null || location.getWorld() == null) return null;

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Location checkLoc = location.clone().add(x, y, z);
                    Block checkBlock = checkLoc.getBlock();

                    if (checkBlock.getType() == Material.LAVA || checkBlock.getType().name().contains("LAVA")) {
                        if (BlockSourceCache.getPlayerId(checkLoc) != null) {
                            return checkLoc;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean hasBurnableBlocksNearby(Block block) {
        if (block == null || block.getWorld() == null) return false;

        Location location = block.getLocation();

        int[][] offsets = {
            {0, 0, -1},
            {0, 0, 1},
            {-1, 0, 0},
            {1, 0, 0},
            {0, -1, 0},
            {0, 1, 0}
        };

        for (int[] offset : offsets) {
            Location checkLoc = location.clone().add(offset[0], offset[1], offset[2]);

            if (checkLoc.getY() < 0 || checkLoc.getY() >= block.getWorld().getMaxHeight()) {
                continue;
            }

            Block checkBlock = checkLoc.getBlock();
            if (checkBlock.getType().isBurnable()) {
                return true;
            }
        }

        return false;
    }

}