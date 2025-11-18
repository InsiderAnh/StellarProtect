package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.data.InventoryRollbackSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChestRollbackSessionManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void performInventoryRollback(Player player, Location location) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        InventoryRollbackSession session = playerProtect.getInventoryRollbackSession();
        if (session == null || !session.isActive()) return;

        Block block = location.getBlock();
        if (!(block.getState() instanceof InventoryHolder)) {
            plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.invalidBlock");
            return;
        }

        InventoryHolder holder = (InventoryHolder) block.getState();
        Inventory inventory = holder.getInventory();

        plugin.getProtectDatabase().getRestoreActions(
            session.getDatabaseFilters(),
            0,
            10000
        ).thenAccept(callbackLookup -> {
            Map<LocationCache, Set<LogEntry>> groupedLogs = callbackLookup.getLogs();

            if (groupedLogs.isEmpty()) {
                if (!session.isSilent()) {
                    plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.noTransactions");
                }
                return;
            }

            LocationCache targetLocationCache = LocationCache.of(location);
            Set<LogEntry> locationLogs = groupedLogs.get(targetLocationCache);

            if (locationLogs == null || locationLogs.isEmpty()) {
                if (!session.isSilent()) {
                    plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.noTransactions");
                }
                return;
            }

            List<PlayerTransactionEntry> transactions = new ArrayList<>();
            for (LogEntry log : locationLogs) {
                if (log instanceof PlayerTransactionEntry &&
                    log.getActionType() == ActionType.INVENTORY_TRANSACTION.getId()) {
                    transactions.add((PlayerTransactionEntry) log);
                }
            }

            if (transactions.isEmpty()) {
                if (!session.isSilent()) {
                    plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.noTransactions");
                }
                return;
            }

            transactions.sort(Comparator.comparingLong(LogEntry::getCreatedAt));

            Map<Long, Integer> inventoryState = calculateInventoryState(
                transactions,
                session.getDatabaseFilters().getTimeFilter().getStart()
            );

            applyInventoryRollback(inventory, inventoryState, session.isVerbose(), session.isSilent(), player);

            if (!session.isSilent()) {
                plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.success");
            }

        }).exceptionally(error -> {
            plugin.getLangManager().sendMessage(player, "messages.error");
            plugin.getLogger().severe("Error performing inventory rollback: " + error.getMessage());
            return null;
        });
    }

    private Map<Long, Integer> calculateInventoryState(List<PlayerTransactionEntry> transactions, long targetTimestamp) {
        Map<Long, Integer> deltaToApply = new HashMap<>();

        for (PlayerTransactionEntry transaction : transactions) {
            long transactionTime = transaction.getCreatedAt();

            if (transactionTime >= targetTimestamp) {
                for (Map.Entry<Long, Integer> added : transaction.getAdded().entrySet()) {
                    long itemId = added.getKey();
                    int amount = added.getValue();
                    deltaToApply.put(itemId, deltaToApply.getOrDefault(itemId, 0) + amount);
                }

                for (Map.Entry<Long, Integer> removed : transaction.getRemoved().entrySet()) {
                    long itemId = removed.getKey();
                    int amount = removed.getValue();
                    deltaToApply.put(itemId, deltaToApply.getOrDefault(itemId, 0) - amount);
                }
            }
        }

        return deltaToApply;
    }

    private void applyInventoryRollback(Inventory inventory, Map<Long, Integer> deltaState, boolean verbose, boolean silent, Player player) {
        int itemsChanged = 0;

        for (Map.Entry<Long, Integer> entry : deltaState.entrySet()) {
            long itemId = entry.getKey();
            int delta = entry.getValue();

            if (delta == 0) continue;

            ItemStack itemStack = null;
            ItemTemplate itemTemplate = plugin.getItemsManager().getItemCache().getById(itemId);
            if (itemTemplate != null) {
                itemStack = itemTemplate.getBukkitItem();
            }

            if (itemStack == null) {
                if (verbose && !silent) {
                    player.sendMessage("§7[VERBOSE] §cNo se pudo encontrar el item con ID: §f" + itemId);
                }
                continue;
            }

            if (delta > 0) {
                ItemStack toAdd = itemStack.clone();
                toAdd.setAmount(delta);

                HashMap<Integer, ItemStack> leftover = inventory.addItem(toAdd);

                if (!leftover.isEmpty()) {
                    if (verbose && !silent) {
                        player.sendMessage("§7[VERBOSE] §eInventario lleno, algunos items no pudieron ser agregados");
                    }
                }

                if (verbose && !silent) {
                    player.sendMessage("§7[VERBOSE] §a+ " + delta + "x §f" + itemStack.getType().name());
                }
                itemsChanged += delta;

            } else if (delta < 0) {
                int toRemove = Math.abs(delta);
                ItemStack toRemoveStack = itemStack.clone();
                toRemoveStack.setAmount(toRemove);

                HashMap<Integer, ItemStack> notRemoved = inventory.removeItem(toRemoveStack);

                if (!notRemoved.isEmpty()) {
                    if (verbose && !silent) {
                        player.sendMessage("§7[VERBOSE] §eNo se encontraron suficientes items para remover");
                    }
                }

                if (verbose && !silent) {
                    player.sendMessage("§7[VERBOSE] §c- " + toRemove + "x §f" + itemStack.getType().name());
                }
                itemsChanged += toRemove;
            }
        }

        if (verbose && !silent) {
            player.sendMessage("§7[VERBOSE] §6Total de items modificados: §f" + itemsChanged);
        }
    }

    public boolean toggleInventoryRollbackMode(Player player) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return false;

        InventoryRollbackSession session = playerProtect.getInventoryRollbackSession();
        if (session == null) return false;

        session.toggle();
        return session.isActive();
    }

}