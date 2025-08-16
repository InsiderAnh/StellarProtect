package io.github.insideranh.stellarprotect.blocks;

import lombok.Getter;
import org.bukkit.block.data.BlockData;

@Getter
public class BlockTemplate {

    private final int id;
    private final BlockData blockData;
    private final String blockDataString;

    public BlockTemplate(int id, BlockData blockData, String blockDataString) {
        this.id = id;
        this.blockData = blockData;
        this.blockDataString = blockDataString;
    }

}