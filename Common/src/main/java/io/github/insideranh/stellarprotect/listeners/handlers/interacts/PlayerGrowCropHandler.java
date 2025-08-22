package io.github.insideranh.stellarprotect.listeners.handlers.interacts;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.api.ProtectNMS;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.world.CropGrowLogEntry;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerGrowCropHandler extends GenericHandler {

    private final ProtectNMS protectNMS = StellarProtect.getInstance().getProtectNMS();

    @Override
    public GenericHandler canHandle(@NonNull Block block, @NonNull String blockType, @NonNull String itemType) {
        if (itemType.equals("BONE") && protectNMS.canGrow(block)) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(Player player, long playerId, @NonNull Block block, ItemStack itemStack) {
        LoggerCache.addLog(new CropGrowLogEntry(playerId, block));
    }

}