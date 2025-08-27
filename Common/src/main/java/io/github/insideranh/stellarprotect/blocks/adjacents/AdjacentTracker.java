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

        while (currentBlock.getY() < currentBlock.getY() + 5) {
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

}