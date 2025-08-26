package io.github.insideranh.stellarprotect.menus;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.inventories.AInventory;
import io.github.insideranh.stellarprotect.inventories.InventorySizes;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

public class ViewInventoryMenu extends AInventory {

    private final PlayerTransactionEntry playerTransactionEntry;

    public ViewInventoryMenu(Player player, Object object) {
        super(player, InventorySizes.GENERIC_9X6, "View Inventory");
        this.playerTransactionEntry = (PlayerTransactionEntry) object;
    }

    @Override
    protected void onClick(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
        canceled.accept(true);
    }

    @Override
    protected void onAllClick(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
        canceled.accept(true);
    }

    @Override
    protected void onDrag(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
        canceled.accept(true);
    }

    @Override
    protected void onBottom(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
        canceled.accept(true);
    }

    @Override
    protected void onUpdate(Inventory inventory) {
        for (Map.Entry<Long, Integer> addedItem : playerTransactionEntry.getAdded().entrySet()) {
            ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(addedItem.getKey());
            if (itemTemplate == null) continue;

            ItemStack item = itemTemplate.getBukkitItem().clone();
            item.setAmount(addedItem.getValue());

            inventory.addItem(item);
        }
        for (Map.Entry<Long, Integer> removedItem : playerTransactionEntry.getRemoved().entrySet()) {
            ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(removedItem.getKey());
            if (itemTemplate == null) continue;

            ItemStack item = itemTemplate.getBukkitItem().clone();
            item.setAmount(removedItem.getValue());

            inventory.addItem(item);
        }
    }

}