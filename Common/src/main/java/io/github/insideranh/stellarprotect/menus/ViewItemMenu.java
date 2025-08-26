package io.github.insideranh.stellarprotect.menus;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.inventories.AInventory;
import io.github.insideranh.stellarprotect.inventories.InventorySizes;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ViewItemMenu extends AInventory {

    private final PlayerItemLogEntry itemLogEntry;

    public ViewItemMenu(Player player, Object object) {
        super(player, InventorySizes.GENERIC_9X1, "View Item");
        this.itemLogEntry = (PlayerItemLogEntry) object;
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
        ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(itemLogEntry.getItemReferenceId());
        if (itemTemplate == null) return;

        ItemStack item = itemTemplate.getBukkitItem().clone();
        item.setAmount(itemLogEntry.getAmount());

        inventory.setItem(0, item);
    }

}