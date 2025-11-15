package io.github.insideranh.stellarprotect.listeners.handlers;

import javax.annotation.Nullable;
import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class GenericHandler {

    protected final StellarProtect plugin = StellarProtect.getInstance();

    public abstract @Nullable GenericHandler canHandle(@NonNull Block block, @NonNull Material blockType, @NonNull String itemStack);

    public abstract void handle(Player player, long playerId, @NonNull Block block, @Nullable ItemStack itemStack);

}