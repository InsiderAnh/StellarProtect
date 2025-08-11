package io.github.insideranh.stellarprotect.items;

import lombok.Getter;

@Getter
public class ItemReference {

    public final long templateId;
    public final int amount;

    public ItemReference(long templateId, int amount) {
        this.templateId = templateId;
        this.amount = amount;
    }

}