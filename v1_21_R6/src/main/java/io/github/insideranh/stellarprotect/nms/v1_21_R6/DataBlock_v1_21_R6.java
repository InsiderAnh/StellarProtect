package io.github.insideranh.stellarprotect.nms.v1_21_R6;

import io.github.insideranh.stellarprotect.blocks.DataBlock;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;

public class DataBlock_v1_21_R6 implements DataBlock {

    @Getter
    private final String blockDataString;
    private BlockData blockData;

    public DataBlock_v1_21_R6(String blockDataString) {
        this.blockData = Bukkit.createBlockData(blockDataString);
        this.blockDataString = blockDataString;
    }

    public DataBlock_v1_21_R6(Block block) {
        this.blockData = block.getBlockData();
        if (blockData instanceof Levelled) {
            Levelled levelled = (Levelled) blockData;
            int currentLevel = levelled.getLevel();
            int maxLevel = levelled.getMaximumLevel();
            int newLevel = (currentLevel + 1) % (maxLevel + 1);
            levelled.setLevel(newLevel);
            this.blockData = levelled;
        }
        this.blockDataString = blockData.getAsString();
    }

    public DataBlock_v1_21_R6(BlockState block) {
        this.blockData = block.getBlockData();
        if (blockData instanceof Levelled) {
            Levelled levelled = (Levelled) blockData;
            int currentLevel = levelled.getLevel();
            int maxLevel = levelled.getMaximumLevel();
            int newLevel = (currentLevel + 1) % (maxLevel + 1);
            levelled.setLevel(newLevel);
            this.blockData = levelled;
        }
        this.blockDataString = blockData.getAsString();
    }

    @Override
    public String getTypeMaterial() {
        return blockData.getMaterial().name();
    }

    @Override
    public int hashCode() {
        return blockData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataBlock_v1_21_R6) {
            return blockData.equals(((DataBlock_v1_21_R6) obj).blockData);
        }
        return false;
    }

}