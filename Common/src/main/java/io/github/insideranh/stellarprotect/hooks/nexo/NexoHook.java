package io.github.insideranh.stellarprotect.hooks.nexo;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class NexoHook extends NexoDefaultHook {

    @Override
    public boolean isNexoBlock(Block block) {
        return NexoBlocks.isCustomBlock(block);
    }

    @Override
    public boolean isNexoFurniture(ItemStack itemStack) {
        return NexoBlocks.isCustomBlock(itemStack);
    }

    @Override
    public String getNexoItemStackId(ItemStack itemStack) {
        String id = NexoItems.idFromItem(itemStack);
        if (id != null) {
            return "nexo:" + id;
        }
        return itemStack.getType().name();
    }

}