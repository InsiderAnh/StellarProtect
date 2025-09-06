package io.github.insideranh.stellarprotect.hooks.nexo;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NexoDefaultHook {

    public boolean isNexoListener(Block block, ItemStack itemStack) {
        return isNexoBlock(block) || isNexoFurniture(itemStack);
    }

    public boolean isNexoBlock(Block block) {
        return false;
    }

    public boolean isNexoFurniture(ItemStack itemStack) {
        return false;
    }

    @Nullable
    public String getNexoItemStackId(ItemStack itemStack) {
        return itemStack.getType().name();
    }

}