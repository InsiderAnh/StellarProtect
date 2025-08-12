package io.github.insideranh.stellarprotect.items.memory;

import lombok.Getter;

@Getter
public class ItemTemplateLight {

    private final long id;
    private final String base64;

    public ItemTemplateLight(long id, String base64) {
        this.id = id;
        this.base64 = base64;
    }

}