package io.github.insideranh.stellarprotect.nms.v1_21_R1;

import io.github.insideranh.stellarprotect.blocks.DataBlock;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class DataBlock_v1_21_R1 implements DataBlock {

    private final BlockData blockData;
    @Getter
    private final String blockDataString;

    public DataBlock_v1_21_R1(String blockDataString) {
        this.blockData = Bukkit.createBlockData(blockDataString);
        this.blockDataString = blockDataString;
    }

    public DataBlock_v1_21_R1(Block block) {
        this.blockData = block.getBlockData();
        this.blockDataString = blockData.getAsString();
    }

    @Override
    public int hashCode() {
        return blockData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataBlock_v1_21_R1) {
            return blockData.equals(((DataBlock_v1_21_R1) obj).blockData);
        }
        return false;
    }

}