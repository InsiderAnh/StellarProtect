package io.github.insideranh.stellarprotect.hooks.nexo;

import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class NexoHook extends NexoDefaultHook {

    public boolean isNexoBlock(Block block) {
        return NexoBlocks.isCustomBlock(block);
    }

    public boolean isNexoFurniture(ItemStack itemStack) {
        return NexoBlocks.isCustomBlock(itemStack);
    }

}