package io.github.insideranh.stellarprotect.api.events;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

public class WorldEditLogManager {

    private static WorldEditLogListener listener = null;

    public static void registerListener(WorldEditLogListener newListener) {
        listener = newListener;
    }

    public static void unregisterListener() {
        listener = null;
    }

    public static void notifyBlockPlace(String playerName, Location location, BlockState newBlockState) {
        if (listener != null) {
            listener.onBlockPlace(playerName, location, newBlockState);
        }
    }

    public static void notifyBlockBreak(String playerName, Location location, BlockState oldBlockState) {
        if (listener != null) {
            listener.onBlockBreak(playerName, location, oldBlockState);
        }
    }

    public static void notifyBlockReplace(String playerName, Location location, BlockState oldBlockState, BlockState newBlockState) {
        if (listener != null) {
            listener.onBlockReplace(playerName, location, oldBlockState, newBlockState);
        }
    }

    public static void notifyContainerBreak(String playerName, Location location, BlockState oldBlockState, ItemStack[] contents) {
        if (listener != null) {
            listener.onContainerBreak(playerName, location, oldBlockState, contents);
        }
    }

}
