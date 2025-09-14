package io.github.insideranh.stellarprotect.listeners.handlers.interacts;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerPlaceRemoveItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerPlaceUseHandler extends GenericHandler {

    @Override
    public GenericHandler canHandle(@NonNull Block block, @NonNull Material blockType, @NonNull String itemType) {
        if (BlockTracker.isPlaceableState(blockType)) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(Player player, long playerId, @NonNull Block block, ItemStack itemStack) {
        ItemStack item = plugin.getProtectNMS().getItemInHand(player);

        ItemReference itemReference = plugin.getItemsManager().getItemReference(item);
        LoggerCache.addLog(new PlayerPlaceRemoveItemLogEntry(playerId, itemReference, block, true, ActionType.PLACE_ITEM));
    }

}