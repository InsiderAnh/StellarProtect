package io.github.insideranh.stellarprotect.listeners.handlers;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class GenericHandler {

    protected final StellarProtect plugin = StellarProtect.getInstance();

    public abstract @Nullable GenericHandler canHandle(@NonNull Block block, @NonNull String blockName, @NonNull String itemStack);

    public abstract void handle(Player player, @NonNull Block block, @Nullable ItemStack itemStack);

}