package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

@Getter
public enum ExtraDataType {

    INVENTORY_CONTENT((byte) 1);

    private final byte id;

    ExtraDataType(byte id) {
        this.id = id;
    }

}