package io.github.insideranh.stellarprotect.callback;

import lombok.Getter;

@Getter
public class CallbackBucket<A, B, C> {

    private final A block;
    private final B blockData;
    private final C material;

    public CallbackBucket(A block, B blockData, C material) {
        this.block = block;
        this.blockData = blockData;
        this.material = material;
    }

}