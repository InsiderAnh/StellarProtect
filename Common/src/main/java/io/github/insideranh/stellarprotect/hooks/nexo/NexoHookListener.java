package io.github.insideranh.stellarprotect.hooks.nexo;

import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentTracker;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerFurnitureLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class NexoHookListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(NexoBlockBreakEvent event) {
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

        String nexoBlockId = event.getMechanic().getItemID();

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_BREAK, nexoBlockId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(NexoBlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        String nexoBlockId = event.getMechanic().getItemID();

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_PLACE, nexoBlockId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(NexoFurnitureBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String nexoBlockId = event.getMechanic().getItemID();
        if (ActionType.FURNITURE_BREAK.shouldSkipLog(player.getWorld().getName(), nexoBlockId))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;
        org.bukkit.Location blockLoc = event.getBaseEntity()
                .getLocation()
                .getBlock()
                .getLocation();
        LoggerCache.addLog(new PlayerFurnitureLogEntry(playerProtect.getPlayerId(), blockLoc, ActionType.FURNITURE_BREAK, "nexo:" + nexoBlockId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(NexoFurniturePlaceEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.FURNITURE_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        String nexoBlockId = event.getMechanic().getItemID();

        LoggerCache.addLog(new PlayerFurnitureLogEntry(playerProtect.getPlayerId(), block.getLocation(), ActionType.FURNITURE_PLACE, "nexo:" + nexoBlockId));
    }

}