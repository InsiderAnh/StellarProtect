package io.github.insideranh.stellarprotect.listeners.handlers;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.listeners.handlers.interacts.PlayerGrowCropHandler;
import io.github.insideranh.stellarprotect.listeners.handlers.interacts.PlayerUseHandler;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;

public class Handlers {

    private static final LinkedList<GenericHandler> interactHandlers = new LinkedList<>();

    static {
        interactHandlers.add(new PlayerGrowCropHandler());
        interactHandlers.add(new PlayerUseHandler());
    }

    public static @Nullable GenericHandler canHandle(Block block, ItemStack itemStack) {
        for (GenericHandler handler : interactHandlers) {
            GenericHandler canHandle = handler.canHandle(block, itemStack);
            if (canHandle != null) {
                return canHandle;
            }
        }
        return null;
    }

}