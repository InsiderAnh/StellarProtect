package io.github.insideranh.stellarprotect.hooks.itemsadder;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook extends ItemsAdderDefaultHook {

    @Override
    public boolean isItemsAdderBlock(Block block) {
        return CustomBlock.byAlreadyPlaced(block) != null;
    }

    @Override
    public boolean isItemsAdderFurniture(ItemStack itemStack) {
        return CustomStack.byItemStack(itemStack) != null;
    }

    @Override
    public String getItemsAdderItemStackId(ItemStack itemStack) {
        CustomStack customStack = CustomStack.byItemStack(itemStack);
        if (customStack != null) {
            return customStack.getNamespacedID();
        }
        return itemStack.getType().name();
    }

}