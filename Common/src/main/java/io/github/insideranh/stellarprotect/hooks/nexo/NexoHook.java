package io.github.insideranh.stellarprotect.hooks.nexo;

import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.block.Block;

public class NexoHook extends NexoDefaultHook {

    public boolean isNexoBlock(Block block) {
        return NexoBlocks.isCustomBlock(block);
    }

}