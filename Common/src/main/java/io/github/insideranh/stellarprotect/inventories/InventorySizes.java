package io.github.insideranh.stellarprotect.inventories;

import lombok.Getter;

@Getter
public enum InventorySizes {
    GENERIC_9X1(1),
    GENERIC_9X2(2),
    GENERIC_9X3(3),
    GENERIC_9X4(4),
    GENERIC_9X5(5),
    GENERIC_9X6(6);

    private final int size;

    public int toInv() {
        return this.size * 9;
    }

    InventorySizes(final int size) {
        this.size = size;
    }

}
