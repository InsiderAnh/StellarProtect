package io.github.insideranh.stellarprotect.blocks;

import lombok.Getter;

@Getter
public class BlockTemplate {

    private final int id;
    private final DataBlock dataBlock;

    public BlockTemplate(int id, DataBlock dataBlock) {
        this.id = id;
        this.dataBlock = dataBlock;
    }

}