package io.github.insideranh.stellarprotect.api.events;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

public interface WorldEditLogListener {

    void onBlockPlace(String playerName, Location location, BlockState newBlockState);

    void onBlockBreak(String playerName, Location location, BlockState oldBlockState);

    void onBlockReplace(String playerName, Location location, BlockState oldBlockState, BlockState newBlockState);

    void onContainerBreak(String playerName, Location location, BlockState oldBlockState, ItemStack[] contents);

}