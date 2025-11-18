package io.github.insideranh.stellarprotect.api.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface EventLogicHandler {

    void onPortalCreate(List<Block> blocks);

    void onSmithEvent(HumanEntity player, ItemStack result);

    void onBrewEvent(ItemStack ingredient, ItemStack fuel, List<ItemStack> results);

    void onTotemEvent(Entity entity, String hand);

    void onMount(Entity mount, Entity entity);

    void onDismount(Entity dismounted, Entity entity);

    void onLeash(HumanEntity player, Entity entity);

    void onUnleash(HumanEntity player, Entity entity);

}