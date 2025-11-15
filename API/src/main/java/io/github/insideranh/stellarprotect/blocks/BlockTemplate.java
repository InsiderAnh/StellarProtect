package io.github.insideranh.stellarprotect.blocks;

import lombok.Getter;

@Getter
public class BlockTemplate {

    private final int id;
    private final DataBlock dataBlock;
    private final String typeName;
    private final String typeNameLower;

    public BlockTemplate(int id, DataBlock dataBlock) {
        this.id = id;
        this.dataBlock = dataBlock;

        this.typeName = dataBlock.getTypeMaterial();
        this.typeNameLower = typeName.toLowerCase();
    }

}