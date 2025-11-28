package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentTracker;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.xseries.XEntityType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplodeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent event) {
        long playerId = PlayerUtils.getPlayerOrEntityId("=explosion");

        processExplosion(event.blockList(), playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntity(EntityExplodeEvent event) {
        long playerId;

        EntityType entityType = event.getEntityType();
        if (entityType == EntityType.CREEPER) {
            playerId = PlayerUtils.getEntityByDirectId("=creeper");
        } else if (entityType == EntityType.WITHER) {
            playerId = PlayerUtils.getEntityByDirectId("=wither");
        } else if (entityType == EntityType.WITHER_SKULL) {
            playerId = PlayerUtils.getEntityByDirectId("=wither");
        } else if (entityType == EntityType.FIREBALL) {
            playerId = PlayerUtils.getEntityByDirectId("=ghast");
        } else if (entityType.name().equals("MINECART_TNT")) {
            playerId = PlayerUtils.getEntityByDirectId("=minecart_tnt");
        } else if (entityType == XEntityType.TNT.get()) {
            playerId = PlayerUtils.getEntityByDirectId("=tnt");
        } else if (entityType == XEntityType.END_CRYSTAL.get()) {
            playerId = PlayerUtils.getEntityByDirectId("=end_crystal");
        } else {
            playerId = PlayerUtils.getPlayerOrEntityId("=explosion");
        }

        processExplosion(event.blockList(), playerId);
    }

    private void processExplosion(List<Block> blockList, long playerId) {
        Map<Location, Block> blockMap = new HashMap<>();

        for (Block block : blockList) {
            blockMap.put(block.getLocation(), block);
        }

        for (Block block : new HashMap<>(blockMap).values()) {
            Material material = block.getType();

            Block above = block.getRelative(0, 1, 0);
            if (AdjacentType.isUp(above.getType()) && !blockMap.containsKey(above.getLocation())) {
                blockMap.put(above.getLocation(), above);

                List<Block> affectedAbove = AdjacentTracker.getAffectedBlocksAbove(block);
                for (Block affectedBlock : affectedAbove) {
                    blockMap.put(affectedBlock.getLocation(), affectedBlock);
                }
            }

            List<Block> affectedSides = AdjacentTracker.getAffectedBlocksSide(block);
            for (Block affectedBlock : affectedSides) {
                if (!blockMap.containsKey(affectedBlock.getLocation())) {
                    blockMap.put(affectedBlock.getLocation(), affectedBlock);
                }
            }

            if (material.hasGravity()) {
                Block gravityBlock = block.getRelative(0, 1, 0);
                while (gravityBlock.getType().hasGravity() && gravityBlock.getY() < gravityBlock.getWorld().getMaxHeight()) {
                    if (!blockMap.containsKey(gravityBlock.getLocation())) {
                        blockMap.put(gravityBlock.getLocation(), gravityBlock);
                    }
                    gravityBlock = gravityBlock.getRelative(0, 1, 0);
                }
            }
        }

        for (Map.Entry<Location, Block> entry : blockMap.entrySet()) {
            Block block = entry.getValue();
            if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) {
                continue;
            }

            LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
        }
    }

}