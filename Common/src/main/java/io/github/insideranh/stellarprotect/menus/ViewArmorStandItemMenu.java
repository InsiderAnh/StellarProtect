package io.github.insideranh.stellarprotect.menus;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerArmorStandManipulateEntry;
import io.github.insideranh.stellarprotect.inventories.AInventory;
import io.github.insideranh.stellarprotect.inventories.InventorySizes;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ViewArmorStandItemMenu extends AInventory {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final PlayerArmorStandManipulateEntry manipulateEntry;

    public ViewArmorStandItemMenu(Player player, Object object) {
        super(player, InventorySizes.GENERIC_9X1, "Armor Stand Item");
        this.manipulateEntry = (PlayerArmorStandManipulateEntry) object;
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
        ItemStack newItem = new ItemUtils(Material.ARROW)
            .displayName(plugin.getLangManager().get("menus.view_stand.new_item.nameItem"))
            .lore(plugin.getLangManager().get("menus.view_stand.new_item.loreItem"))
            .build();
        ItemStack oldItem = new ItemUtils(Material.ARROW)
            .displayName(plugin.getLangManager().get("menus.view_stand.old_item.nameItem"))
            .lore(plugin.getLangManager().get("menus.view_stand.old_item.loreItem"))
            .build();
        ItemStack noneItem = new ItemUtils(Material.BARRIER)
            .displayName(plugin.getLangManager().get("menus.view_stand.none.nameItem"))
            .build();

        ItemTemplate oldItemTemplate = plugin.getItemsManager().getItemTemplate(manipulateEntry.getOldItemId());
        ItemTemplate newItemTemplate = plugin.getItemsManager().getItemTemplate(manipulateEntry.getNewItemId());

        inventory.setItem(0, oldItem);
        if (manipulateEntry.getOldItemId() == -1L) {
            inventory.setItem(1, noneItem);
        } else {
            inventory.setItem(1, oldItemTemplate.getBukkitItem());
        }

        inventory.setItem(3, newItem);
        if (manipulateEntry.getNewItemId() == -1L) {
            inventory.setItem(4, noneItem);
        } else {
            inventory.setItem(4, newItemTemplate.getBukkitItem());
        }
    }

}