package io.github.insideranh.stellarprotect.hooks.craftengine;

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
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.core.block.BlockDefinition;
import org.bukkit.event.Listener;

import java.util.List;

public class CraftEngineHookListener implements Listener {


    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (event.isCancelled()) return;
        // TODO

        BlockDefinition block = event.customBlock();
        Block bukkitBlock = event.bukkitBlock();
        Material material = bukkitBlock.getType();
        if (ActionType.BLOCK_BREAK.shouldSkipLog(event.location().getWorld().getName(), block.id().asString()))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.player().platformPlayer());
        if (playerProtect == null) return;

        if (AdjacentType.isUp(material)) {
            List<Block> affectedBlocks = AdjacentTracker.getAffectedBlocksAbove(bukkitBlock);
            for (Block affectedBlock : affectedBlocks) {
                LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), affectedBlock, ActionType.BLOCK_BREAK));
            }
        }

        if (AdjacentType.isSide(material)) {
            List<Block> affectedBlocks = AdjacentTracker.getAffectedBlocksSide(bukkitBlock);
            for (Block affectedBlock : affectedBlocks) {
                LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), affectedBlock, ActionType.BLOCK_BREAK));
            }
        }

        String namespacedID = block.id().asString();

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), bukkitBlock, ActionType.BLOCK_BREAK, namespacedID));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(CustomBlockPlaceEvent event) {
        if (event.isCancelled()) return;

        BlockDefinition block = event.customBlock();
        Block bukkitBlock = event.bukkitBlock();
        if (ActionType.BLOCK_PLACE.shouldSkipLog(event.location().getWorld().getName(), block.id().asString())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.player());
        if (playerProtect == null) return;

        String namespacedID = event.customBlock().id().asString();

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), bukkitBlock, ActionType.BLOCK_PLACE, namespacedID));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(FurnitureBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.player();
        String namespacedID = event.furniture().id().asString();
        if (ActionType.FURNITURE_BREAK.shouldSkipLog(player.getWorld().getName(), namespacedID))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        Location blockLocation = event.location();

        LoggerCache.addLog(new PlayerFurnitureLogEntry(playerProtect.getPlayerId(), blockLocation, ActionType.FURNITURE_BREAK, namespacedID));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(FurniturePlaceEvent event) {
        if (event.isCancelled()) return;

        Location location = event.location();
        String namespacedID = event.furniture().id().asString();
        if (ActionType.FURNITURE_PLACE.shouldSkipLog(location.getWorld().getName(), namespacedID)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.player());
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerFurnitureLogEntry(playerProtect.getPlayerId(), location, ActionType.FURNITURE_PLACE, namespacedID));
    }
}
