package io.github.insideranh.stellarprotect.listeners.handlers;

import javax.annotation.Nullable;
import io.github.insideranh.stellarprotect.listeners.handlers.interacts.PlayerGrowCropHandler;
import io.github.insideranh.stellarprotect.listeners.handlers.interacts.PlayerPlaceUseHandler;
import io.github.insideranh.stellarprotect.listeners.handlers.interacts.PlayerToggleHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class Handlers {

    private static final GenericHandler[] GENERIC_HANDLERS = {
        new PlayerToggleHandler(),
        new PlayerPlaceUseHandler(),
        new PlayerGrowCropHandler()
    };

    public static @Nullable GenericHandler canHandle(Block block, Material blockType, ItemStack itemStack) {
        String itemType = itemStack == null ? "" : itemStack.getType().name();

        for (GenericHandler handler : GENERIC_HANDLERS) {
            if (handler.canHandle(block, blockType, itemType) != null) {
                return handler;
            }
        }
        return null;
    }

}