package io.github.insideranh.stellarprotect.listeners.handlers.interacts;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerPlaceRemoveItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerUseEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerUseHandler extends GenericHandler {

    @Override
    public GenericHandler canHandle(@NonNull Block block, @Nullable ItemStack itemStack) {
        if (BlockTracker.isInteractable(block.getType().name())) {
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

        ItemStack item = plugin.getProtectNMS().getItemInHand(player);

        if (BlockTracker.isToggleableState(block.getType().name())) {
            LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.INTERACT));
            return;
        }

        if (BlockTracker.isPlaceableState(block.getType().name()) && item != null && !item.getType().equals(Material.AIR)) {
            ItemReference itemReference = plugin.getItemsManager().getItemReference(item);
            LoggerCache.addLog(new PlayerPlaceRemoveItemLogEntry(playerProtect.getPlayerId(), itemReference, block, true, ActionType.PLACE_ITEM));
            return;
        }

        LoggerCache.addLog(new PlayerUseEntry(playerProtect.getPlayerId(), block, ActionType.USE));
    }

}