package io.github.insideranh.stellarprotect.items;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class ItemTemplate {

    private final long id;
    private final ItemStack bukkitItem;
    private final String base64;

    public ItemTemplate(long id, ItemStack bukkitItem, String base64) {
        this.id = id;
        this.bukkitItem = bukkitItem;
        this.base64 = base64;
    }

}