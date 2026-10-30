package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BucketListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_EMPTY.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        captureBucket(player, playerProtect, event.getBlockClicked(), event.getBlockFace(), getEventBlock(event), event.getBucket(), ActionType.BUCKET_EMPTY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_FILL.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        captureBucket(player, playerProtect, event.getBlockClicked(), event.getBlockFace(), getEventBlock(event), event.getBucket(), ActionType.BUCKET_FILL);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useItemInHand() == Event.Result.DENY) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WATER_BUCKET) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || !isWaterloggable(clicked)) return;
        Player player = event.getPlayer();
        if (ActionType.BUCKET_EMPTY.shouldSkipLog(player.getWorld().getName(), item.getType().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        captureBucket(player, playerProtect, clicked, event.getBlockFace(), null, item.getType(), ActionType.BUCKET_EMPTY);
    }

    private Block getEventBlock(PlayerBucketEvent event) {
        try {
            Object affected = event.getClass().getMethod("getBlock").invoke(event);
            return affected instanceof Block ? (Block) affected : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private boolean isWaterloggable(Block block) {
        try {
            Object blockData = block.getClass().getMethod("getBlockData").invoke(block);
            Class<?> waterlogged = Class.forName("org.bukkit.block.data.Waterlogged");
            return waterlogged.isInstance(blockData);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void capture(Map<Location, BlockState> states, Block block) {
        if (block != null) states.computeIfAbsent(block.getLocation(), location -> block.getState());
    }

    private void captureBucket(Player player, PlayerProtect playerProtect, Block clicked, org.bukkit.block.BlockFace face, Block eventBlock, Material bucket, ActionType action) {
        if (clicked == null) return;
        Map<Location, BlockState> states = new LinkedHashMap<>();
        capture(states, eventBlock);
        capture(states, clicked);
        capture(states, clicked.getRelative(face));
        if (action == ActionType.BUCKET_EMPTY) {
            capture(states, plugin.getProtectNMS().getBucketData(clicked, face, bucket).getBlock());
        }
        UUID playerId = player.getUniqueId();
        for (Map.Entry<Location, BlockState> entry : states.entrySet()) {
            BlockState previous = entry.getValue();
            Location location = entry.getKey();
            String key = playerId + ":" + location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ() + ":" + action.getId();
            if (!pending.add(key)) continue;
            plugin.getStellarTaskHook(() -> {
                try {
                    finishBucket(playerProtect, previous, action);
                } finally {
                    pending.remove(key);
                }
            }).runTask(location, 1L);
        }
    }

    private void finishBucket(PlayerProtect playerProtect, BlockState oldState, ActionType action) {
        Block block = oldState.getBlock();
        BlockState newState = block.getState();
        String oldData = plugin.getProtectNMS().getBlockData(oldState);
        String newData = plugin.getProtectNMS().getBlockData(newState);
        if (oldData.equals(newData)) return;
        if (action == ActionType.BUCKET_EMPTY) BlockSourceCache.registerBlockSource(block.getLocation(), playerProtect.getPlayerId());
        else BlockSourceCache.removeBlockSource(block.getLocation());
        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), oldState, newState, action));
    }

}
