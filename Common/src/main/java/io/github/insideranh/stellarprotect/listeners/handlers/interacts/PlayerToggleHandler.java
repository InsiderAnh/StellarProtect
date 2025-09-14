package io.github.insideranh.stellarprotect.listeners.handlers.interacts;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerToggleHandler extends GenericHandler {

    @Override
    public GenericHandler canHandle(@NonNull Block block, @NonNull Material blockType, @NonNull String itemType) {
        if (BlockTracker.isToggleableState(blockType)) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(Player player, long playerId, @NonNull Block block, ItemStack itemStack) {
        LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.INTERACT));
    }

}