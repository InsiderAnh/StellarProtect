package io.github.insideranh.stellarprotect.trackers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.maps.StringBooleanMap;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestTransactionTracker implements Listener {

    private static final ConcurrentHashMap<String, Location> playerChestLocations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ItemCount[]> initialInventoryStates = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();
    private static final StringBooleanMap activeEditing = new StringBooleanMap();
    private static final long CLEANUP_THRESHOLD = 30 * 60 * 1000L;

    private static final ArrayDeque<ItemCount[]> ARRAY_POOL = new ArrayDeque<>();
    private static final ArrayDeque<FastItemCounter> COUNTER_POOL = new ArrayDeque<>();
    private static final int POOL_SIZE = 32;

    static {
        for (int i = 0; i < POOL_SIZE; i++) {
            ARRAY_POOL.offer(new ItemCount[54]);
            COUNTER_POOL.offer(new FastItemCounter());
        }
    }

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !WorldUtils.isValidChestBlock(block.getType())) return;

        Player player = event.getPlayer();
        Location chestLocation = block.getLocation();
        String playerName = player.getName();

        if (activeEditing.get(playerName)) {
            finishChestEditing(player);
        }

        playerChestLocations.put(playerName, chestLocation);
        activeEditing.put(playerName, true);

        plugin.getStellarTaskHook(() -> {
            if (activeEditing.get(playerName)) {
                captureInitialState(player, chestLocation);
            }
        }).runTask(chestLocation, 1L);

        String inventoryId = getInventoryId(playerName, chestLocation);
        lastActivity.put(inventoryId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (!isValidChestInventory(event.getInventory())) return;

        String playerName = player.getName();
        Location chestLocation = playerChestLocations.get(playerName);
        if (chestLocation == null || !activeEditing.get(playerName)) return;

        String inventoryId = getInventoryId(playerName, chestLocation);
        lastActivity.put(inventoryId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (!isValidChestInventory(event.getInventory())) return;

        String playerName = player.getName();
        Location chestLocation = playerChestLocations.get(playerName);
        if (chestLocation == null || !activeEditing.get(playerName)) return;

        String inventoryId = getInventoryId(playerName, chestLocation);
        lastActivity.put(inventoryId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        if (activeEditing.get(player.getName())) {
            finishChestEditing(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeEditing.get(player.getName())) {
            finishChestEditing(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!WorldUtils.isValidChestBlock(block.getType())) return;

        Location chestLocation = block.getLocation();
        for (Map.Entry<String, Location> entry : playerChestLocations.entrySet()) {
            if (entry.getValue().equals(chestLocation)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && activeEditing.get(player.getName())) {
                    finishChestEditing(player);
                }
                break;
            }
        }
    }

    private void captureInitialState(Player player, Location chestLocation) {
        Block block = chestLocation.getBlock();
        if (!WorldUtils.isValidChestBlock(block.getType())) return;

        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder)) return;

        InventoryHolder holder = (InventoryHolder) state;
        Inventory inventory = holder.getInventory();

        String inventoryId = getInventoryId(player.getName(), chestLocation);
        ItemCount[] snapshot = captureInventorySnapshot(inventory.getContents());
        initialInventoryStates.put(inventoryId, snapshot);
    }

    private void finishChestEditing(Player player) {
        String playerName = player.getName();
        Location chestLocation = playerChestLocations.get(playerName);
        if (chestLocation == null) return;

        String inventoryId = getInventoryId(playerName, chestLocation);
        ItemCount[] initialSnapshot = initialInventoryStates.get(inventoryId);

        if (initialSnapshot != null) {
            Block block = chestLocation.getBlock();
            if (WorldUtils.isValidChestBlock(block.getType())) {
                BlockState state = block.getState();
                if (state instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) state;
                    Inventory inventory = holder.getInventory();
                    ItemCount[] currentSnapshot = captureInventorySnapshot(inventory.getContents());

                    TransactionResult result = compareSnapshots(playerName, chestLocation, initialSnapshot, currentSnapshot);

                    if (!result.itemsAdded.isEmpty() || !result.itemsRemoved.isEmpty()) {
                        handleTransaction(player, result);
                    }

                    returnToPool(initialSnapshot);
                    returnToPool(currentSnapshot);
                }
            }
        }

        playerChestLocations.remove(playerName);
        activeEditing.remove(playerName);
        initialInventoryStates.remove(inventoryId);
        lastActivity.remove(inventoryId);
    }

    private boolean isValidChestInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BlockState) {
            BlockState state = (BlockState) holder;
            return WorldUtils.isValidChestBlock(state.getType());
        }
        return holder instanceof DoubleChest;
    }

    private String getInventoryId(String playerName, Location location) {
        return playerName.toLowerCase() + ":" +
            location.getWorld().getName() + ":" +
            location.getBlockX() + ":" +
            location.getBlockY() + ":" +
            location.getBlockZ();
    }

    private ItemCount[] captureInventorySnapshot(ItemStack[] contents) {
        ItemCount[] array = getFromPool();
        if (array == null) array = new ItemCount[54];

        FastItemCounter counter = getCounterFromPool();
        if (counter == null) counter = new FastItemCounter();

        counter.reset();

        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                counter.addItem(item);
            }
        }

        int index = 0;
        for (int i = 0; i < counter.capacity && index < array.length; i++) {
            if (counter.items[i] != null) {
                if (array[index] == null) array[index] = new ItemCount();
                array[index].item = counter.items[i];
                array[index].count = counter.counts[i];
                index++;
            }
        }

        if (index < array.length && array[index] != null) {
            array[index].item = null;
            array[index].count = 0;
        }

        returnCounterToPool(counter);
        return array;
    }

    private TransactionResult compareSnapshots(String playerName, Location location, ItemCount[] initial, ItemCount[] current) {
        Map<ItemStack, Integer> added = new HashMap<>();
        Map<ItemStack, Integer> removed = new HashMap<>();

        Map<Integer, Integer> initialCounts = new HashMap<>();
        Map<Integer, ItemStack> itemMap = new HashMap<>();

        for (ItemCount ic : initial) {
            if (ic == null || ic.item == null) break;
            int hash = generateItemHash(ic.item);
            initialCounts.put(hash, initialCounts.getOrDefault(hash, 0) + ic.count);
            itemMap.put(hash, ic.item);
        }

        Map<Integer, Integer> currentCounts = new HashMap<>();
        for (ItemCount ic : current) {
            if (ic == null || ic.item == null) break;
            int hash = generateItemHash(ic.item);
            currentCounts.put(hash, currentCounts.getOrDefault(hash, 0) + ic.count);
            itemMap.put(hash, ic.item);
        }

        for (Map.Entry<Integer, Integer> entry : currentCounts.entrySet()) {
            int hash = entry.getKey();
            int currentCount = entry.getValue();
            int initialCount = initialCounts.getOrDefault(hash, 0);

            if (currentCount > initialCount) {
                added.put(itemMap.get(hash), currentCount - initialCount);
            }
        }

        for (Map.Entry<Integer, Integer> entry : initialCounts.entrySet()) {
            int hash = entry.getKey();
            int initialCount = entry.getValue();
            int currentCount = currentCounts.getOrDefault(hash, 0);

            if (initialCount > currentCount) {
                removed.put(itemMap.get(hash), initialCount - currentCount);
            }
        }

        return new TransactionResult(playerName, location, added, removed);
    }

    private int generateItemHash(ItemStack item) {
        int hash = item.getType().ordinal();

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                hash = hash * 31 + meta.getDisplayName().hashCode();
            }
            if (meta.hasLore()) {
                hash = hash * 31 + Objects.hashCode(meta.getLore());
            }
            if (meta.hasEnchants()) {
                hash = hash * 31 + meta.getEnchants().hashCode();
            }
        }

        return hash;
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
            ItemCount[] snapshot = initialInventoryStates.remove(key);
            if (snapshot != null) returnToPool(snapshot);
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

    private ItemCount[] getFromPool() {
        return ARRAY_POOL.poll();
    }

    private void returnToPool(ItemCount[] array) {
        if (array != null && ARRAY_POOL.size() < POOL_SIZE) {
            for (ItemCount ic : array) {
                if (ic != null) {
                    ic.item = null;
                    ic.count = 0;
                }
            }
            ARRAY_POOL.offer(array);
        }
    }

    private FastItemCounter getCounterFromPool() {
        return COUNTER_POOL.poll();
    }

    private void returnCounterToPool(FastItemCounter counter) {
        if (counter != null && COUNTER_POOL.size() < POOL_SIZE) {
            COUNTER_POOL.offer(counter);
        }
    }

    private static class ItemCount {
        ItemStack item;
        int count;
    }

    private static class FastItemCounter {
        private static final int DEFAULT_CAPACITY = 64;

        ItemStack[] items;
        int[] counts;
        int[] hashes;
        int capacity;
        int size;

        FastItemCounter() {
            this.capacity = DEFAULT_CAPACITY;
            this.items = new ItemStack[capacity];
            this.counts = new int[capacity];
            this.hashes = new int[capacity];
        }

        void reset() {
            size = 0;
            Arrays.fill(items, 0, size, null);
            Arrays.fill(counts, 0, size, 0);
            Arrays.fill(hashes, 0, size, 0);
        }

        void addItem(ItemStack item) {
            int hash = fastItemHash(item);

            for (int i = 0; i < size; i++) {
                if (hashes[i] == hash && itemsEqual(items[i], item)) {
                    counts[i] += item.getAmount();
                    return;
                }
            }

            if (size < capacity) {
                items[size] = item.clone();
                items[size].setAmount(1);
                counts[size] = item.getAmount();
                hashes[size] = hash;
                size++;
            }
        }

        private int fastItemHash(ItemStack item) {
            int hash = item.getType().ordinal();
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName()) {
                    hash = hash * 31 + meta.getDisplayName().hashCode();
                }
                if (meta.hasLore()) {
                    hash = hash * 31 + Objects.hashCode(meta.getLore());
                }
            }
            return hash;
        }

        private boolean itemsEqual(ItemStack a, ItemStack b) {
            if (a.getType() != b.getType()) return false;

            boolean aMeta = a.hasItemMeta();
            boolean bMeta = b.hasItemMeta();

            if (aMeta != bMeta) return false;
            if (!aMeta) return true;

            ItemMeta metaA = a.getItemMeta();
            ItemMeta metaB = b.getItemMeta();

            return Objects.equals(metaA.getDisplayName(), metaB.getDisplayName()) &&
                Objects.equals(metaA.getLore(), metaB.getLore()) &&
                Objects.equals(metaA.getEnchants(), metaB.getEnchants());
        }
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