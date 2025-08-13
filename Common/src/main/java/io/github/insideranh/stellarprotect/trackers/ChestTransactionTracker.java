package io.github.insideranh.stellarprotect.trackers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChestTransactionTracker implements Listener {

    private static final ConcurrentHashMap<String, Location> playerChestLocations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ItemStack[]> initialInventoryStates = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> activeEditing = new ConcurrentHashMap<>();
    private static final long CLEANUP_THRESHOLD = 30 * 60 * 1000L;
    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !WorldUtils.isValidChestBlock(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        Location chestLocation = block.getLocation();

        if (activeEditing.getOrDefault(player.getName(), false)) {
            finishChestEditing(player);
        }

        playerChestLocations.put(player.getName(), chestLocation);
        activeEditing.put(player.getName(), true);

        plugin.getStellarTaskHook(() -> {
            if (activeEditing.getOrDefault(player.getName(), false)) {
                captureInitialState(player, chestLocation);
            }
        }).runTask(chestLocation, 1L);

        String inventoryId = getInventoryId(player, chestLocation);
        lastActivity.put(inventoryId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        if (!isValidChestInventory(inventory)) {
            return;
        }

        Location chestLocation = playerChestLocations.get(player.getName());
        if (chestLocation == null || !activeEditing.getOrDefault(player.getName(), false)) {
            return;
        }

        String inventoryId = getInventoryId(player, chestLocation);
        lastActivity.put(inventoryId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        if (!isValidChestInventory(inventory)) {
            return;
        }

        Location chestLocation = playerChestLocations.get(player.getName());
        if (chestLocation == null || !activeEditing.getOrDefault(player.getName(), false)) {
            return;
        }

        String inventoryId = getInventoryId(player, chestLocation);
        lastActivity.put(inventoryId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        if (activeEditing.getOrDefault(player.getName(), false)) {
            finishChestEditing(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (activeEditing.getOrDefault(player.getName(), false)) {
            finishChestEditing(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!WorldUtils.isValidChestBlock(block.getType())) {
            return;
        }

        Location chestLocation = block.getLocation();

        for (Map.Entry<String, Location> entry : playerChestLocations.entrySet()) {
            if (entry.getValue().equals(chestLocation)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && activeEditing.getOrDefault(player.getName(), false)) {
                    finishChestEditing(player);
                }
                break;
            }
        }
    }

    private void captureInitialState(Player player, Location chestLocation) {
        Block block = chestLocation.getBlock();
        if (!WorldUtils.isValidChestBlock(block.getType())) {
            return;
        }

        BlockState state = block.getState();
        if (state instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) state;
            Inventory inventory = holder.getInventory();

            String inventoryId = getInventoryId(player, chestLocation);
            initialInventoryStates.put(inventoryId, cloneContents(inventory.getContents()));
        }
    }

    private void finishChestEditing(Player player) {
        Location chestLocation = playerChestLocations.get(player.getName());
        if (chestLocation == null) {
            return;
        }

        String inventoryId = getInventoryId(player, chestLocation);
        ItemStack[] initialContents = initialInventoryStates.get(inventoryId);

        if (initialContents != null) {
            Block block = chestLocation.getBlock();
            if (WorldUtils.isValidChestBlock(block.getType())) {
                BlockState state = block.getState();
                if (state instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) state;
                    Inventory inventory = holder.getInventory();
                    ItemStack[] currentContents = inventory.getContents();

                    TransactionResult result = compareInventories(player.getName(), chestLocation, initialContents, currentContents);

                    if (!result.itemsAdded.isEmpty() || !result.itemsRemoved.isEmpty()) {
                        handleTransaction(player, result);
                    }
                }
            }
        }

        playerChestLocations.remove(player.getName());
        activeEditing.remove(player.getName());
        initialInventoryStates.remove(inventoryId);
        lastActivity.remove(inventoryId);
    }

    private boolean isValidChestInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BlockState) {
            BlockState state = (BlockState) holder;
            Material type = state.getType();
            return WorldUtils.isValidChestBlock(type);
        }

        return holder instanceof DoubleChest;
    }

    private String getInventoryId(Player player, Location location) {
        return player.getName().toLowerCase() + ":" +
            location.getWorld().getName() + ":" +
            location.getBlockX() + ":" +
            location.getBlockY() + ":" +
            location.getBlockZ();
    }

    private TransactionResult compareInventories(String playerName, Location location, ItemStack[] initial, ItemStack[] current) {
        Map<ItemStack, Integer> added = new HashMap<>();
        Map<ItemStack, Integer> removed = new HashMap<>();

        Map<ItemStack, Integer> initialItems = countItemsAsBase64(initial);
        Map<ItemStack, Integer> currentItems = countItemsAsBase64(current);

        for (Map.Entry<ItemStack, Integer> entry : currentItems.entrySet()) {
            ItemStack base64Key = entry.getKey();
            int currentCount = entry.getValue();
            int initialCount = initialItems.getOrDefault(base64Key, 0);

            if (currentCount > initialCount) {
                added.put(base64Key, currentCount - initialCount);
            }
        }

        for (Map.Entry<ItemStack, Integer> entry : initialItems.entrySet()) {
            ItemStack base64Key = entry.getKey();
            int initialCount = entry.getValue();
            int currentCount = currentItems.getOrDefault(base64Key, 0);

            if (initialCount > currentCount) {
                removed.put(base64Key, initialCount - currentCount);
            }
        }

        return new TransactionResult(playerName, location, added, removed);
    }

    private Map<ItemStack, Integer> countItemsAsBase64(ItemStack[] contents) {
        Map<ItemStack, Integer> itemCounts = new HashMap<>();

        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                ItemStack singleItem = item.clone();
                singleItem.setAmount(1);

                itemCounts.put(singleItem, itemCounts.getOrDefault(singleItem, 0) + item.getAmount());
            }
        }

        return itemCounts;
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                cloned[i] = contents[i].clone();
            }
        }
        return cloned;
    }

    private void handleTransaction(Player player, TransactionResult result) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        plugin.getExecutor().execute(() -> {
            Map<Long, Integer> itemsAdded = new HashMap<>();
            Map<Long, Integer> itemsRemoved = new HashMap<>();

            for (Map.Entry<ItemStack, Integer> entry : result.itemsAdded.entrySet()) {
                ItemReference itemReference = plugin.getItemsManager().getItemReference(entry.getKey(), entry.getValue());
                itemsAdded.put(itemReference.getTemplateId(), entry.getValue());
            }
            for (Map.Entry<ItemStack, Integer> entry : result.itemsRemoved.entrySet()) {
                ItemReference itemReference = plugin.getItemsManager().getItemReference(entry.getKey(), entry.getValue());
                itemsRemoved.put(itemReference.getTemplateId(), entry.getValue());
            }
            LoggerCache.addLog(new PlayerTransactionEntry(playerProtect.getPlayerId(), itemsAdded, itemsRemoved, result.chestLocation, ActionType.INVENTORY_TRANSACTION));
        });
    }

    public void cleanupOldStates() {
        long currentTime = System.currentTimeMillis();
        List<String> keysToRemove = new ArrayList<>();

        for (Map.Entry<String, Long> entry : lastActivity.entrySet()) {
            if (currentTime - entry.getValue() > CLEANUP_THRESHOLD) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (String key : keysToRemove) {
            initialInventoryStates.remove(key);
            lastActivity.remove(key);
        }

        List<String> playersToRemove = new ArrayList<>();
        for (String playerName : playerChestLocations.keySet()) {
            if (Bukkit.getPlayer(playerName) == null) {
                playersToRemove.add(playerName);
            }
        }

        for (String playerName : playersToRemove) {
            playerChestLocations.remove(playerName);
            activeEditing.remove(playerName);
        }

        Debugger.debugExtras("Limpieza completada. Eliminados " + keysToRemove.size() + " estados antiguos y " + playersToRemove.size() + " jugadores desconectados.");
    }

    public static class TransactionResult {

        public final String playerName;
        public final Location chestLocation;
        public final Map<ItemStack, Integer> itemsAdded;
        public final Map<ItemStack, Integer> itemsRemoved;
        public final long timestamp;

        public TransactionResult(String playerName, Location chestLocation, Map<ItemStack, Integer> itemsAdded, Map<ItemStack, Integer> itemsRemoved) {
            this.playerName = playerName;
            this.chestLocation = chestLocation;
            this.itemsAdded = itemsAdded;
            this.itemsRemoved = itemsRemoved;
            this.timestamp = System.currentTimeMillis();
        }
    }

}