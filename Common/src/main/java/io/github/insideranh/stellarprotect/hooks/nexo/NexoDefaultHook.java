package io.github.insideranh.stellarprotect.hooks.nexo;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

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

    public String getNexoItemKey(ItemStack item) {return null;}

}