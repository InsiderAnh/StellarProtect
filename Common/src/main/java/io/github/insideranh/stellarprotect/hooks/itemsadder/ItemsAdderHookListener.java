package io.github.insideranh.stellarprotect.hooks.itemsadder;

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentTracker;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerFurnitureLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class ItemsAdderHookListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        Material material = block.getType();
        if (block.getType().equals(Material.AIR) || ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), material.name()))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        if (AdjacentType.isUp(material)) {
            List<Block> affectedBlocks = AdjacentTracker.getAffectedBlocksAbove(block);
            for (Block affectedBlock : affectedBlocks) {
                LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), affectedBlock, ActionType.BLOCK_BREAK));
            }
        }

        if (AdjacentType.isSide(material)) {
            List<Block> affectedBlocks = AdjacentTracker.getAffectedBlocksSide(block);
            for (Block affectedBlock : affectedBlocks) {
                LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), affectedBlock, ActionType.BLOCK_BREAK));
            }
        }

        String namespacedID = event.getNamespacedID();

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_BREAK, namespacedID));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(CustomBlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        String namespacedID = event.getNamespacedID();

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_PLACE, namespacedID));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(FurnitureBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String namespacedID = event.getNamespacedID();
        if (ActionType.FURNITURE_BREAK.shouldSkipLog(player.getWorld().getName(), namespacedID))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        Location blockLocation = event.getBukkitEntity().getLocation();

        LoggerCache.addLog(new PlayerFurnitureLogEntry(playerProtect.getPlayerId(), blockLocation, ActionType.FURNITURE_BREAK, namespacedID));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(FurniturePlacedEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBukkitEntity().getLocation().getBlock();
        String namespacedID = event.getNamespacedID();
        if (ActionType.FURNITURE_PLACE.shouldSkipLog(block.getWorld().getName(), namespacedID)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerFurnitureLogEntry(playerProtect.getPlayerId(), block.getLocation(), ActionType.FURNITURE_PLACE, namespacedID));
    }

}