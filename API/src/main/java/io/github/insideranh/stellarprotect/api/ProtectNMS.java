package io.github.insideranh.stellarprotect.api;

import io.github.insideranh.stellarprotect.callback.CallbackBucket;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public abstract class ProtectNMS {

    public abstract Block readMaterial(Location location, String data);

    public abstract String getBlockData(Block block);

    public abstract String[] getSignLines(Block block);

    public abstract Location getBlockLocation(Player player, Inventory inventory);

    public abstract int getAge(Block block);

    public abstract boolean isMaxAge(Block block);

    public abstract boolean canGrow(Block block);

    public abstract void teleport(Player player, Location location);

    public abstract void sendActionTitle(Player player, String message, String tooltipDetails, String command, Function<String, String> replacer);

    public abstract void sendPageButtons(Player player, String pageString, String clickPage, int page, int perPage, int maxPages);

    public abstract CallbackBucket<Block, String, Material> getBucketData(Block block, BlockFace blockFace, Material bucket);

    public abstract ItemStack getItemInHand(Player player);

    public abstract int getHashBlockData(Block block);

}