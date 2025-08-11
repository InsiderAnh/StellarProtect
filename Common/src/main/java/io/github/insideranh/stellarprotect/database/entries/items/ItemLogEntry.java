package io.github.insideranh.stellarprotect.database.entries.items;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class ItemLogEntry {

    private final ItemStack itemStack;
    private final long playerId;
    private final int amount;
    private final boolean added;
    private final long createdAt;

    public ItemLogEntry(ItemStack itemStack, long playerId, int amount, boolean added, long createdAt) {
        this.itemStack = itemStack;
        this.playerId = playerId;
        this.amount = amount;
        this.added = added;
        this.createdAt = createdAt;
    }

}