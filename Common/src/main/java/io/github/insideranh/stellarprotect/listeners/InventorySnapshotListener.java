package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerInventorySnapshotEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InventorySnapshotListener implements Listener {

    private static final long SNAPSHOT_INTERVAL_MS = 5 * 60 * 1000L;
    private final StellarProtect plugin = StellarProtect.getInstance();
    private final ConcurrentMap<UUID, Long> lastSnapshot = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        PlayerProtect pp = PlayerProtect.getPlayer(player);
        if (pp == null) return;
        trySnapshot(player, pp, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerProtect pp = PlayerProtect.getPlayer(player);
        if (pp == null) return;
        trySnapshot(player, pp, true);
    }

    public void trySnapshot(Player player, PlayerProtect pp, boolean force) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Long last = lastSnapshot.get(id);
        if (!force && last != null && (now - last) < SNAPSHOT_INTERVAL_MS) return;

        try {
            LoggerCache.addLog(new PlayerInventorySnapshotEntry(pp.getPlayerId(), player.getInventory().getContents(), player.getLocation(), ActionType.INVENTORY_SNAPSHOT));
            lastSnapshot.put(id, now);
        } catch (Exception ignored) {
        }
    }

    public void forceSnapshot(Player player) {
        PlayerProtect pp = PlayerProtect.getPlayer(player);
        if (pp == null) return;
        trySnapshot(player, pp, true);
    }

}