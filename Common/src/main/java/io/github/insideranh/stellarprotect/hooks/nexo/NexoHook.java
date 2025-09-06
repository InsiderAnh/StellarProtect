package io.github.insideranh.stellarprotect.hooks.nexo;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.inventory.ItemStack;

public class NexoHook extends NexoDefaultHook {

    public boolean isNexoBlock(org.bukkit.block.Block block) {
        return NexoBlocks.isCustomBlock(block);
    }

    public boolean isNexoFurniture(ItemStack itemStack) {
        return NexoBlocks.isCustomBlock(itemStack);
    }

    @Override
    public String getNexoItemKey(ItemStack item) {
        if (item == null) return null;
        try {
            String id = NexoItems.idFromItem(item);
            return (id != null && !id.isEmpty()) ? "nexo:" + id : null;
        } catch (Throwable ignored) { return null; }
    }
}
