package io.github.insideranh.stellarprotect.hooks.craftengine;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CraftEngineDefaultHook {

    public boolean isCraftEngineListener(Block block, ItemStack itemStack) {
        return isCraftEngineBlock(block) || isCraftEngineFurniture(itemStack);
    }

    public boolean isCraftEngineBlock(Block block) {
        return false;
    }

    public boolean isCraftEngineFurniture(ItemStack itemStack) {
        return false;
    }

    @Nullable
    public String getCraftEngineItemStackId(ItemStack itemStack) {
        return itemStack.getType().name();
    }

}