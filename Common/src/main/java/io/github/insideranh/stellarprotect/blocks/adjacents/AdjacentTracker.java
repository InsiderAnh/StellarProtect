package io.github.insideranh.stellarprotect.blocks.adjacents;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class AdjacentTracker {

    public static List<Block> getAffectedBlocksAbove(Block brokenBlock) {
        List<Block> affectedBlocks = new ArrayList<>();
        Block currentBlock = brokenBlock.getRelative(BlockFace.UP);

        while (currentBlock.getY() < 380) {
            Material material = currentBlock.getType();

            if (AdjacentType.isUp(material)) {
                affectedBlocks.add(currentBlock);
                currentBlock = currentBlock.getRelative(BlockFace.UP);
            } else {
                break;
            }
        }
        return affectedBlocks;
    }

    public static List<Block> getAffectedBlocksSide(Block brokenBlock) {
        List<Block> affectedBlocks = new ArrayList<>();
        Block block = brokenBlock.getRelative(BlockFace.NORTH);
        if (AdjacentType.isSide(block.getType())) {
            affectedBlocks.add(block);
        }
        block = brokenBlock.getRelative(BlockFace.EAST);
        if (AdjacentType.isSide(block.getType())) {
            affectedBlocks.add(block);
        }
        block = brokenBlock.getRelative(BlockFace.SOUTH);
        if (AdjacentType.isSide(block.getType())) {
            affectedBlocks.add(block);
        }
        block = brokenBlock.getRelative(BlockFace.WEST);
        if (AdjacentType.isSide(block.getType())) {
            affectedBlocks.add(block);
        }
        return affectedBlocks;
    }

}