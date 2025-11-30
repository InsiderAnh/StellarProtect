package io.github.insideranh.stellarprotect.nms.v1_21.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.github.insideranh.stellarprotect.api.events.WorldEditLogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

public class WorldEditLogger {

    protected static BaseBlock getBaseBlock(Extent extent, BlockVector3 position, Location location, Material oldType) {
        if (isSpecialBlock(oldType)) {
            try {
                return extent.getFullBlock(position);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[WorldEdit] Error getting full block: " + e.getMessage());
            }
        }
        return null;
    }

    private static boolean isSpecialBlock(Material type) {
        return type == Material.SPAWNER || isContainer(type);
    }

    private static boolean isContainer(Material type) {
        String typeName = type.name();
        return typeName.contains("CHEST") ||
            typeName.contains("BARREL") ||
            typeName.contains("SHULKER_BOX") ||
            type == Material.HOPPER ||
            type == Material.DROPPER ||
            type == Material.DISPENSER ||
            type == Material.FURNACE ||
            type == Material.BLAST_FURNACE ||
            type == Material.SMOKER;
    }

    protected static void postProcess(Extent extent, Actor actor, BlockVector3 position, Location location, BlockStateHolder<?> blockStateHolder, BaseBlock baseBlock, Material oldType, com.sk89q.worldedit.world.block.BlockState oldBlockState, ItemStack[] containerContents) {
        BlockData oldBlockData = BukkitAdapter.adapt(oldBlockState);
        BlockData newBlockData = BukkitAdapter.adapt(blockStateHolder.toImmutableState());
        Material newType = newBlockData.getMaterial();

        String oldBlockDataString = oldBlockData.getAsString();
        String newBlockDataString = newBlockData.getAsString();

        BlockState oldBlock = new WorldEditBlockState(location, oldType, oldBlockData);
        BlockState newBlock = new WorldEditBlockState(location, newType, newBlockData);

        if (!oldType.equals(newType) || !oldBlockDataString.equals(newBlockDataString)) {
            if (containerContents != null) {
                WorldEditLogManager.notifyContainerBreak(
                    actor.getName(),
                    location,
                    oldBlock,
                    containerContents
                );
            }

            processBlockChange(actor.getName(), location, oldBlock, newBlock);
        }
    }

    private static void processBlockChange(String playerName, Location location, BlockState oldBlock, BlockState newBlock) {
        boolean oldIsAir = isAir(oldBlock.getType());
        boolean newIsAir = isAir(newBlock.getType());

        if (oldIsAir && !newIsAir) {
            WorldEditLogManager.notifyBlockPlace(
                playerName,
                location,
                newBlock
            );
        } else if (!oldIsAir && !newIsAir) {
            WorldEditLogManager.notifyBlockReplace(
                playerName,
                location,
                oldBlock,
                newBlock
            );
        } else if (!oldIsAir) {
            WorldEditLogManager.notifyBlockBreak(
                playerName,
                location,
                oldBlock
            );
        }
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

}