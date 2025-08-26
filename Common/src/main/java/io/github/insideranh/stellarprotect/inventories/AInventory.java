package io.github.insideranh.stellarprotect.inventories;

import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter(AccessLevel.PROTECTED)
public abstract class AInventory implements Listener {

    private final Player player;
    private final Inventory inventory;
    private final boolean update;

    protected AInventory(Player player, InventorySizes invSizes, String name) {
        this(player, invSizes, name, true);
    }

    protected AInventory(Player player, InventorySizes invSizes, String name, boolean update) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, invSizes.toInv(), name);
        this.update = update;
    }

    @EventHandler
    public void onClickEvent(InventoryClickEvent event) {
        if (!event.getWhoClicked().getName().equalsIgnoreCase(player.getName()) || event.getSlotType().equals(InventoryType.SlotType.OUTSIDE))
            return;

        if (event.getView().getTopInventory().equals(inventory)) {
            if (!event.getCursor().getType().equals(Material.AIR)) {
                event.setCancelled(true);
                return;
            }
            if (event.getView().getBottomInventory().equals(event.getClickedInventory())) {
                this.onBottom(event, event.getCurrentItem(), event.getClick(), event::setCancelled);
                return;
            }
            if (event.getCurrentItem() != null && !event.getCurrentItem().getType().equals(Material.AIR)) {
                this.onClick(event, event.getCurrentItem(), event.getClick(), event::setCancelled);
            } else if (event.getCurrentItem() != null) {
                this.onDrag(event, event.getCurrentItem(), event.getClick(), event::setCancelled);
            } else {
                this.onAllClick(event, event.getCurrentItem(), event.getClick(), event::setCancelled);
            }
        }
    }

    @EventHandler
    public void onCloseEvent(InventoryCloseEvent e) {
        if (!e.getPlayer().equals(player)) return;
        if (e.getInventory().equals(inventory)) {
            this.onClose();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().equals(inventory)) e.setCancelled(true);
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent e) {
        if (e.getDestination().equals(inventory)) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(InventoryPickupItemEvent e) {
        if (e.getInventory().equals(inventory)) e.setCancelled(true);
    }

    protected abstract void onClick(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled);

    protected void onBottom(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
    }

    protected void onAllClick(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
    }

    protected abstract void onUpdate(Inventory inventory);

    protected void onDrag(InventoryClickEvent event, ItemStack currentItem, ClickType click, Consumer<Boolean> canceled) {
    }

    protected void set(int x, int y, ItemStack itemStack) {
        int slot = y * 9 + x;
        this.inventory.setItem(slot, itemStack);
    }

    protected void add(ItemStack... itemStack) {
        this.inventory.addItem(itemStack);
    }


    protected void onClose() {
        HandlerList.unregisterAll(this);
    }

    public void open() {
        if (update)
            this.onUpdate(inventory);

        player.openInventory(inventory);
        Bukkit.getPluginManager().registerEvents(this, StellarProtect.getInstance());
    }

    public void close() {
        player.closeInventory();
        this.onClose();
    }

}
