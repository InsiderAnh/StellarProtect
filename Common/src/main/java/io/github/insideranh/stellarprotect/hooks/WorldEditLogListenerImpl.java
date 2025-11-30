package io.github.insideranh.stellarprotect.hooks;

import io.github.insideranh.stellarprotect.api.events.WorldEditLogListener;
import io.github.insideranh.stellarprotect.api.events.WorldEditLogManager;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockStateLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

public class WorldEditLogListenerImpl implements WorldEditLogListener {

    public WorldEditLogListenerImpl() {
        WorldEditLogManager.registerListener(this);
    }

    @Override
    public void onBlockPlace(String playerName, Location location, BlockState newBlockState) {
        LoggerCache.addWorldEditLog(new PlayerBlockLogEntry(
            -1000,
            newBlockState,
            ActionType.BLOCK_PLACE
        ));
    }

    @Override
    public void onBlockBreak(String playerName, Location location, BlockState oldBlockState) {
        LoggerCache.addWorldEditLog(new PlayerBlockLogEntry(
            -1000,
            oldBlockState,
            ActionType.BLOCK_BREAK
        ));
    }

    @Override
    public void onBlockReplace(String playerName, Location location, BlockState oldBlockState, BlockState newBlockState) {
        LoggerCache.addWorldEditLog(new PlayerBlockStateLogEntry(
            -1000,
            oldBlockState,
            newBlockState,
            ActionType.BLOCK_PLACE
        ));
    }

    @Override
    public void onContainerBreak(String playerName, Location location, BlockState oldBlockState, ItemStack[] contents) {
        LoggerCache.addWorldEditLog(new PlayerBlockLogEntry(
            -1000,
            oldBlockState,
            ActionType.BLOCK_BREAK
        ));
    }

    public void unregister() {
        WorldEditLogManager.unregisterListener();
    }

}