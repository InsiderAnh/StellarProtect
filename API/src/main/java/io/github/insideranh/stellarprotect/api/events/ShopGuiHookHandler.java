package io.github.insideranh.stellarprotect.api.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ShopGuiHookHandler {

    void onPostTransaction(Player player, ItemStack itemStack, int amount, double price);

}