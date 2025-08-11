package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PickUpDropListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickUp(PlayerPickupItemEvent event) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack.getType().name().equals("AIR")) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(itemStack);

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, event.getPlayer().getLocation(), ActionType.PICKUP_ITEM));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickUp(PlayerDropItemEvent event) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        ItemStack itemStack = event.getItemDrop().getItemStack();
        if (itemStack.getType().name().equals("AIR")) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(itemStack);

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, event.getPlayer().getLocation(), ActionType.DROP_ITEM));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(event.getInventory().getResult());

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, plugin.getProtectNMS().getBlockLocation(player, event.getInventory()), ActionType.DROP_ITEM));
    }

}