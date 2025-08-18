package io.github.insideranh.stellarprotect.items;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class ItemTemplate {

    public final long id;
    private final long hashCode;
    private final ItemStack bukkitItem;
    private final String base64;

    private final String displayName;
    private final String lore;
    private final String typeName;

    private final String displayNameLower;
    private final String loreLower;
    private final String typeNameLower;

    public ItemTemplate(long id, ItemStack bukkitItem, String base64) {
        this.id = id;
        this.hashCode = -1;
        this.bukkitItem = bukkitItem;
        this.base64 = base64;

        this.displayName = bukkitItem.hasItemMeta() && bukkitItem.getItemMeta().hasDisplayName() ? bukkitItem.getItemMeta().getDisplayName() : null;
        this.lore = bukkitItem.hasItemMeta() && bukkitItem.getItemMeta().hasLore() ? bukkitItem.getItemMeta().getLore().toString() : null;
        this.typeName = bukkitItem.getType().name();

        this.displayNameLower = displayName != null ? displayName.toLowerCase() : null;
        this.loreLower = lore != null ? lore.toLowerCase() : null;
        this.typeNameLower = typeName.toLowerCase();
    }

    public ItemTemplate(long id, long hashCode, ItemStack bukkitItem, String base64) {
        this.id = id;
        this.hashCode = hashCode;
        this.bukkitItem = bukkitItem;
        this.base64 = base64;

        this.displayName = bukkitItem.hasItemMeta() && bukkitItem.getItemMeta().hasDisplayName() ? bukkitItem.getItemMeta().getDisplayName() : null;
        this.lore = bukkitItem.hasItemMeta() && bukkitItem.getItemMeta().hasLore() ? bukkitItem.getItemMeta().getLore().toString() : null;
        this.typeName = bukkitItem.getType().name();

        this.displayNameLower = displayName != null ? displayName.toLowerCase() : null;
        this.loreLower = lore != null ? lore.toLowerCase() : null;
        this.typeNameLower = typeName.toLowerCase();
    }

}