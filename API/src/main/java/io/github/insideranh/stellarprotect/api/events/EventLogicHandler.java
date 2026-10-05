package io.github.insideranh.stellarprotect.api.events;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public interface EventLogicHandler {

    void onPortalCreate(List<Block> blocks);

    void onSmithEvent(HumanEntity player, ItemStack result);

    void onBrewEvent(@Nullable ItemStack ingredient, @Nullable ItemStack fuel, List<ItemStack> results);

    void onTotemEvent(Entity entity, String hand);

    void onMount(Entity mount, Entity entity);

    void onDismount(Entity dismounted, Entity entity);

    void onLeash(HumanEntity player, Entity entity);

    void onUnleash(HumanEntity player, Entity entity);

    void onRaidTrigger(org.bukkit.entity.Player player, org.bukkit.Raid raid);

    void onRaidSpawn(org.bukkit.Raid raid);

    void onRaidFinish(org.bukkit.Raid raid);

    void onBlockFade(BlockState oldState, BlockState newState);

    void onBlockBurn(Block block);

    void onBlockForm(BlockState oldState, BlockState newState);

    void onDispense(Block block, ItemStack item);

}