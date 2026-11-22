package io.github.insideranh.stellarprotect.hooks.craftengine;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;

public class CraftEngineHook extends CraftEngineDefaultHook {
    @Override
    public boolean isCraftEngineBlock(Block block) {
        return CraftEngineBlocks.isCustomBlock(block);
    }

    @Override
    public boolean isCraftEngineFurniture(ItemStack itemStack) {
        return CraftEngineItems.isCustomItem(itemStack);
    }

    @Override
    public String getCraftEngineItemStackId(ItemStack itemStack) {
        Key itemKey = CraftEngineItems.getCustomItemId(itemStack);
        if (itemKey != null) {
            return itemKey.asString();
        }
        return itemStack.getType().name();
    }
}
