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
        super(player, calculateInventorySize(object), "View Item");
        this.itemLogEntry = (PlayerItemLogEntry) object;
    }

    private static InventorySizes calculateInventorySize(Object object) {
        if (!(object instanceof PlayerItemLogEntry)) return InventorySizes.GENERIC_9X1;
        PlayerItemLogEntry entry = (PlayerItemLogEntry) object;
        int amount = Math.max(1, entry.getAmount());
        ItemTemplate template = StellarProtect.getInstance().getItemsManager().getItemTemplate(entry.getItemReferenceId());
        int maxStack = 64;
        if (template != null && template.getBukkitItem() != null) {
            maxStack = Math.max(1, template.getBukkitItem().getMaxStackSize());
        }
        int stacksNeeded = (int) Math.ceil((double) amount / maxStack);
        if (stacksNeeded <= 9) return InventorySizes.GENERIC_9X1;
        if (stacksNeeded <= 18) return InventorySizes.GENERIC_9X2;
        if (stacksNeeded <= 27) return InventorySizes.GENERIC_9X3;
        if (stacksNeeded <= 36) return InventorySizes.GENERIC_9X4;
        if (stacksNeeded <= 45) return InventorySizes.GENERIC_9X5;
        return InventorySizes.GENERIC_9X6;
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

        ItemStack baseItem = itemTemplate.getBukkitItem().clone();
        int remaining = itemLogEntry.getAmount();
        int maxStackSize = Math.max(1, baseItem.getMaxStackSize());
        int slot = 0;
        int invCapacity = inventory.getSize();

        while (remaining > 0 && slot < invCapacity) {
            ItemStack stack = baseItem.clone();
            int stackAmount = Math.min(remaining, maxStackSize);
            stack.setAmount(stackAmount);
            inventory.setItem(slot, stack);
            remaining -= stackAmount;
            slot++;
        }
    }

}
