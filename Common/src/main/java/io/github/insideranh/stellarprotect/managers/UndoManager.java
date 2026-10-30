package io.github.insideranh.stellarprotect.managers;

import com.google.gson.Gson;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockStateLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerKillLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Set;

public class UndoManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private static final int MAX_PER_TICK = 50;

    public void undoRestoreBlocksPreview(Player sender, Map<LocationCache, Set<LogEntry>> groupedLogs,
            boolean verbose) {
        Gson gson = SerializerUtils.getGson();
        for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
            for (LogEntry logEntry : entry.getValue()) {
                if (logEntry instanceof PlayerBlockLogEntry) {
                    PlayerBlockLogEntry blockLogEntry = (PlayerBlockLogEntry) logEntry;
                    Location location = blockLogEntry.asBukkitLocation();

                    if (verbose) {
                        String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "PLACE"
                                : "BREAK";
                        sender.sendMessage("§7[VERBOSE UNDO] §e" + actionName + " §7at §f" +
                                (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                                " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                                " §7material: §f" + blockLogEntry.getDataString() +
                                " §7by §f" + PlayerUtils.getNameOfEntity(blockLogEntry.getPlayerId()) +
                                " §7(ID: " + blockLogEntry.getPlayerId() + ") §7at §f" +
                                new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
                                        .format(new java.util.Date(blockLogEntry.getCreatedAt())));
                    }

                    boolean isPlace = blockLogEntry.usesOldState();
                    BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString(),
                            blockLogEntry.getExtraType(), blockLogEntry.getExtraData(),
                            isPlace, blockLogEntry.getOldDataString(), blockLogEntry.getBlockEntityNbt(),
                            blockLogEntry.getOldBlockEntityNbt());

                    if (isPlace) {
                        plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
                    } else {
                        plugin.getStellarTaskHook(() -> blockRestore.previewRemove(sender, location)).runTask(location);
                    }
                } else if (logEntry instanceof PlayerBlockStateLogEntry) {
                    PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
                    Location location = blockStateLogEntry.asBukkitLocation();

                    if (verbose) {
                        sender.sendMessage("§7[VERBOSE UNDO] §eSTATE_CHANGE §7at §f" +
                                (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                                " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                                " §7material: §f" + blockStateLogEntry.getDataString() +
                                " §7by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                                " §7(ID: " + blockStateLogEntry.getPlayerId() + ") §7at §f" +
                                new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
                                        .format(new java.util.Date(blockStateLogEntry.getCreatedAt())));
                    }

                    if (!silentCheck(sender)) {
                        BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.getDataString());
                        plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
                    }
                }
            }
        }
    }

    private boolean silentCheck(CommandSender sender) {
        return false;
    }

    public void undoRestoreBlocks(CommandSender sender, Map<LocationCache, Set<LogEntry>> groupedLogs, boolean verbose,
            boolean silent) {
        int processedCount = 0;
        for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
            for (LogEntry logEntry : entry.getValue()) {
                undoRestore(logEntry, sender, verbose);

                processedCount++;

                if (processedCount % MAX_PER_TICK == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    public void undoRestore(LogEntry logEntry, CommandSender sender, boolean verbose) {
        Gson gson = SerializerUtils.getGson();
        if (logEntry instanceof PlayerBlockLogEntry) {
            PlayerBlockLogEntry blockLogEntry = (PlayerBlockLogEntry) logEntry;
            Location location = blockLogEntry.asBukkitLocation();

            if (verbose) {
                String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "RE-PLACING"
                        : "RE-REMOVING";
                sender.sendMessage("§7[VERBOSE UNDO] §a" + actionName + " §7block at §f" +
                        (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                        " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                        " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                        " §7ID: " + blockLogEntry.getPlayerId() + "§7) §7data: §f" + blockLogEntry.getDataString());
            }

            try {
                boolean isPlace = blockLogEntry.usesOldState();
                BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString(),
                        blockLogEntry.getExtraType(), blockLogEntry.getExtraData(),
                        isPlace, blockLogEntry.getOldDataString(), blockLogEntry.getBlockEntityNbt(),
                        blockLogEntry.getOldBlockEntityNbt());

                if (isPlace) {
                    plugin.getStellarTaskHook(() -> blockRestore.undoPlace(gson, location)).runTask(location);
                } else {
                    plugin.getStellarTaskHook(() -> blockRestore.remove(location)).runTask(location);
                }
            } catch (Exception e) {
                sender.sendMessage("§c[ERROR] Failed to undo block at " + (int) location.getX() + ", "
                        + (int) location.getY() + ", " + (int) location.getZ());
            }
        } else if (logEntry instanceof PlayerBlockStateLogEntry) {
            PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
            BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.getDataString());
            Location location = blockStateLogEntry.asBukkitLocation();

            if (verbose) {
                sender.sendMessage("§7[VERBOSE UNDO] §aRESTORING §7state at §f" +
                        (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                        " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                        " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                        " §7ID: " + blockStateLogEntry.getPlayerId() + "§7) §7data: §f"
                        + blockStateLogEntry.getDataString());
            }

            plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
        } else if (logEntry instanceof PlayerKillLogEntry) {
            PlayerKillLogEntry playerKillLogEntry = (PlayerKillLogEntry) logEntry;
            if (playerKillLogEntry.getEntityType().equals("PLAYER"))
                return;

            Location location = playerKillLogEntry.asBukkitLocation();

            if (verbose) {
                sender.sendMessage("§7[VERBOSE UNDO] §aREMOVING §7restored entity at §f" +
                        (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                        " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                        " §7type: §f" + playerKillLogEntry.getEntityType());
            }

            if (location.getWorld() == null)
                return;
            plugin.getStellarTaskHook(() -> {
                NamespacedKey restoreKey = new NamespacedKey(plugin, "restored_entity_log");
                String marker = String.valueOf(playerKillLogEntry.getId());
                location.getWorld().getNearbyEntities(location, 2, 2, 2).stream()
                        .filter(entity -> marker
                                .equals(entity.getPersistentDataContainer().get(restoreKey, PersistentDataType.STRING)))
                        .forEach(Entity::remove);
            }).runTask(location);
        }
    }

}
