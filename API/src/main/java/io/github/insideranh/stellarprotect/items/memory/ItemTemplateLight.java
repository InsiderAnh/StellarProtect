package io.github.insideranh.stellarprotect.items.memory;

import lombok.Getter;

@Getter
public class ItemTemplateLight {

    private final long id;
    private final String base64;
    private final String displayName;
    private final String lore;
    private final String typeName;
    private final String displayNameLower;
    private final String loreLower;
    private final String typeNameLower;

    public ItemTemplateLight(long id, String base64, String displayName, String lore, String typeName, String displayNameLower, String loreLower, String typeNameLower) {
        this.id = id;
        this.base64 = base64;
        this.displayName = displayName;
        this.lore = lore;
        this.typeName = typeName;
        this.displayNameLower = displayNameLower;
        this.loreLower = loreLower;
        this.typeNameLower = typeNameLower;
    }

}