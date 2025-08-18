package io.github.insideranh.stellarprotect.listeners.handlers.interacts;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerToggleHandler extends GenericHandler {

    @Override
    public GenericHandler canHandle(@NonNull Block block, @NonNull String blockType, @NonNull String itemType) {
        if (BlockTracker.isToggleableState(blockType)) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(Player player, @NonNull Block block, ItemStack itemStack) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        if (playerProtect.getNextUse() > System.currentTimeMillis()) {
            return;
        }
        playerProtect.setNextUse(System.currentTimeMillis() + 300L);

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.INTERACT));
    }

}