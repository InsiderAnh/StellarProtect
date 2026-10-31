package io.github.insideranh.stellarprotect.trackers;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.api.ProtectNMS;
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
import org.bukkit.block.Chest;
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

    private static final ProtectNMS protectNMS = StellarProtect.getInstance().getProtectNMS();
    private static final ConcurrentHashMap<String, Location> playerChestLocations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<ItemCount>> initialInventoryStates = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();
    private static final StringBooleanMap activeEditing = new StringBooleanMap();
    private static final long CLEANUP_THRESHOLD = 30 * 60 * 1000L;

    private final StellarProtect plugin = StellarProtect.getInstance();

    public static Location getCanonicalChestLocation(Block block) {
        if (block == null) return null;
        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                DoubleChest dc = (DoubleChest) holder;
                InventoryHolder left = dc.getLeftSide();
                if (left instanceof Chest) {
                    return ((Chest) left).getLocation();
                }
            }
        }
        return block.getLocation();
    }

    public static List<Location> getChestLocations(Block block) {
        if (block == null) return Collections.emptyList();
        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest) {
                DoubleChest dc = (DoubleChest) holder;
                List<Location> locs = new ArrayList<>(2);
                if (dc.getLeftSide() instanceof Chest) {
                    locs.add(((Chest) dc.getLeftSide()).getLocation());
                }
                if (dc.getRightSide() instanceof Chest) {
                    locs.add(((Chest) dc.getRightSide()).getLocation());
                }
                if (!locs.isEmpty()) return locs;
            }
        }
        return Collections.singletonList(block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !WorldUtils.isValidChestBlock(block.getType())) return;

        Player player = event.getPlayer();
        Location chestLocation = getCanonicalChestLocation(block);
        String playerName = player.getName();

        if (activeEditing.get(playerName)) {
            finishChestEditing(player);
        }

        playerChestLocations.put(playerName, chestLocation);
        activeEditing.put(playerName, true);

        captureInitialState(player, chestLocation);

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

        Location chestLocation = getCanonicalChestLocation(block);
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
        List<ItemCount> snapshot = captureInventorySnapshot(inventory.getContents());
        initialInventoryStates.put(inventoryId, snapshot);
    }

    private void finishChestEditing(Player player) {
        String playerName = player.getName();
        Location chestLocation = playerChestLocations.get(playerName);
        if (chestLocation == null) return;

        String inventoryId = getInventoryId(playerName, chestLocation);
        List<ItemCount> initialSnapshot = initialInventoryStates.get(inventoryId);

        if (initialSnapshot != null) {
            Block block = chestLocation.getBlock();
            if (WorldUtils.isValidChestBlock(block.getType())) {
                BlockState state = block.getState();
                if (state instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) state;
                    Inventory inventory = holder.getInventory();
                    List<ItemCount> currentSnapshot = captureInventorySnapshot(inventory.getContents());

                    plugin.getExecutor().execute(() -> {
                        TransactionResult result = compareSnapshots(playerName, chestLocation, initialSnapshot, currentSnapshot);

                        if (!result.itemsAdded.isEmpty() || !result.itemsRemoved.isEmpty()) {
                            handleTransaction(player, result);
                        }
                    });
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

    private List<ItemCount> captureInventorySnapshot(ItemStack[] contents) {
        if (contents == null || contents.length == 0) return Collections.emptyList();

        List<ItemCount> snapshot = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;

            boolean merged = false;
            for (ItemCount ic : snapshot) {
                if (itemsEqual(ic.item, item)) {
                    ic.count += item.getAmount();
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                ItemStack single = item.clone();
                single.setAmount(1);
                snapshot.add(new ItemCount(single, item.getAmount()));
            }
        }

        return snapshot;
    }

    private TransactionResult compareSnapshots(String playerName, Location location, List<ItemCount> initial, List<ItemCount> current) {
        Map<ItemStack, Integer> added = new HashMap<>();
        Map<ItemStack, Integer> removed = new HashMap<>();

        if (initial == null) initial = Collections.emptyList();
        if (current == null) current = Collections.emptyList();

        List<ItemCount> remainingInitial = new ArrayList<>(initial.size());
        for (ItemCount ic : initial) {
            remainingInitial.add(new ItemCount(ic.item.clone(), ic.count));
        }

        for (ItemCount cur : current) {
            ItemCount matchedInit = null;
            for (ItemCount init : remainingInitial) {
                if (itemsEqual(cur.item, init.item)) {
                    matchedInit = init;
                    break;
                }
            }

            if (matchedInit != null) {
                if (cur.count > matchedInit.count) {
                    added.put(cur.item.clone(), cur.count - matchedInit.count);
                }
                remainingInitial.remove(matchedInit);
            } else {
                added.put(cur.item.clone(), cur.count);
            }
        }

        for (ItemCount rem : remainingInitial) {
            int currentCount = 0;
            for (ItemCount cur : current) {
                if (itemsEqual(rem.item, cur.item)) {
                    currentCount = cur.count;
                    break;
                }
            }
            if (rem.count > currentCount) {
                removed.put(rem.item.clone(), rem.count - currentCount);
            }
        }

        return new TransactionResult(playerName, location, added, removed);
    }

    private void handleTransaction(Player player, TransactionResult result) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        Map<Long, Integer> itemsAdded = new HashMap<>();
        Map<Long, Integer> itemsRemoved = new HashMap<>();

        for (Map.Entry<ItemStack, Integer> entry : result.itemsAdded.entrySet()) {
            if (entry.getKey() == null || entry.getKey().getType() == Material.AIR) continue;
            ItemReference itemReference = plugin.getItemsManager().getItemReference(entry.getKey(), entry.getValue());
            if (itemReference != null && itemReference.getTemplateId() > 0) {
                itemsAdded.put(itemReference.getTemplateId(), entry.getValue());
            }
        }
        for (Map.Entry<ItemStack, Integer> entry : result.itemsRemoved.entrySet()) {
            if (entry.getKey() == null || entry.getKey().getType() == Material.AIR) continue;
            ItemReference itemReference = plugin.getItemsManager().getItemReference(entry.getKey(), entry.getValue());
            if (itemReference != null && itemReference.getTemplateId() > 0) {
                itemsRemoved.put(itemReference.getTemplateId(), entry.getValue());
            }
        }

        if (!itemsAdded.isEmpty() || !itemsRemoved.isEmpty()) {
            LoggerCache.addLog(new PlayerTransactionEntry(playerProtect.getPlayerId(), itemsAdded, itemsRemoved, result.chestLocation, ActionType.INVENTORY_TRANSACTION));
        }
    }

    public JsonObject getInventoryContent(Inventory inventory) {
        JsonObject jsonObject = new JsonObject();
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && item.getType() != Material.AIR) {
                ItemReference itemReference = plugin.getItemsManager().getItemReference(item, item.getAmount());
                if (itemReference != null && itemReference.getTemplateId() > 0) {
                    JsonObject slotData = new JsonObject();
                    slotData.addProperty("t", itemReference.getTemplateId());
                    slotData.addProperty("a", item.getAmount());
                    jsonObject.add(String.valueOf(slot), slotData);
                }
            }
        }

        return jsonObject;
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

    public static boolean itemsEqual(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;

        boolean aMeta = a.hasItemMeta();
        boolean bMeta = b.hasItemMeta();

        if (aMeta != bMeta) return false;
        if (!aMeta) return true;

        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();

        int modelA = protectNMS.modelDataHashCode(metaA);
        int modelB = protectNMS.modelDataHashCode(metaB);

        return modelA == modelB &&
                Objects.equals(metaA.getDisplayName(), metaB.getDisplayName()) &&
                Objects.equals(metaA.getLore(), metaB.getLore()) &&
                Objects.equals(metaA.getEnchants(), metaB.getEnchants());
    }

    public static class ItemCount {
        public final ItemStack item;
        public int count;

        public ItemCount(ItemStack item, int count) {
            this.item = item;
            this.count = count;
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
