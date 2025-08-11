package io.github.insideranh.stellarprotect.listeners.handlers.interacts;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.world.CropGrowLogEntry;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerGrowCropHandler extends GenericHandler {

    @Override
    public GenericHandler canHandle(@NonNull Block block, @Nullable ItemStack itemStack) {
        if (itemStack != null && StellarProtect.getInstance().getProtectNMS().canGrow(block) && itemStack.getType().name().contains("BONE")) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(Player player, @NonNull Block block, ItemStack itemStack) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new CropGrowLogEntry(playerProtect.getPlayerId(), block));
    }

}