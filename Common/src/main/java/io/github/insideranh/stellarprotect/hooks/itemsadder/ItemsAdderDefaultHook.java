package io.github.insideranh.stellarprotect.hooks.itemsadder;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemsAdderDefaultHook {

    public boolean isItemsAdderListener(Block block, ItemStack itemStack) {
        return isItemsAdderBlock(block) || isItemsAdderFurniture(itemStack);
    }

    public boolean isItemsAdderBlock(Block block) {
        return false;
    }

    public boolean isItemsAdderFurniture(ItemStack itemStack) {
        return false;
    }

    @Nullable
    public String getItemsAdderItemStackId(ItemStack itemStack) {
        return itemStack.getType().name();
    }

}