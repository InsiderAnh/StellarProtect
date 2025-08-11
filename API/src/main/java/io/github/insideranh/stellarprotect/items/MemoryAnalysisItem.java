package io.github.insideranh.stellarprotect.items;

import lombok.Getter;

@Getter
public class MemoryAnalysisItem {

    private final Object object;

    public MemoryAnalysisItem(Object object) {
        this.object = object;
    }

}
