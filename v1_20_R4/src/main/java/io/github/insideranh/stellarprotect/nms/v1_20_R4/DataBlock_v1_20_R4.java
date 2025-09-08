package io.github.insideranh.stellarprotect.nms.v1_20_R4;

import io.github.insideranh.stellarprotect.blocks.DataBlock;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public class DataBlock_v1_20_R4 implements DataBlock {

    private final BlockData blockData;
    @Getter
    private final String blockDataString;

    public DataBlock_v1_20_R4(String blockDataString) {
        this.blockData = Bukkit.createBlockData(blockDataString);
        this.blockDataString = blockDataString;
    }

    public DataBlock_v1_20_R4(Block block) {
        this.blockData = block.getBlockData();
        this.blockDataString = blockData.getAsString();
    }

    public DataBlock_v1_20_R4(BlockState block) {
        this.blockData = block.getBlockData();
        this.blockDataString = blockData.getAsString();
    }

    @Override
    public int hashCode() {
        return blockData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataBlock_v1_20_R4) {
            return blockData.equals(((DataBlock_v1_20_R4) obj).blockData);
        }
        return false;
    }

}