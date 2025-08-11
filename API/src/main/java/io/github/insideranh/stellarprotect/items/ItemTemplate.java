package io.github.insideranh.stellarprotect.items;

import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class ItemTemplate {

    private final long id;
    private final ItemStack bukkitItem;
    private final String base64;
    // 0 = false | 1 = true
    private byte shorted = 0;

    public ItemTemplate(long id, ItemStack bukkitItem, String base64) {
        this.id = id;
        this.bukkitItem = bukkitItem;
        if (base64.startsWith(StringCleanerUtils.COMMON_BASE64)) {
            this.base64 = base64.substring(StringCleanerUtils.COMMON_BASE64.length());
            this.shorted = 1;
        } else {
            this.base64 = base64;
        }
    }

}