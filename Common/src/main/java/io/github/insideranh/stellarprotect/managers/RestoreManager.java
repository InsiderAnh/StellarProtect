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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class RestoreManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private static final int MAX_PER_TICK = 50;

    public void restoreBlocksPreview(Player sender, Map<LocationCache, Set<LogEntry>> groupedLogs, boolean verbose) {
        Gson gson = SerializerUtils.getGson();
        for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
            for (LogEntry logEntry : entry.getValue()) {
                if (logEntry instanceof PlayerBlockLogEntry) {
                    PlayerBlockLogEntry blockLogEntry = (PlayerBlockLogEntry) logEntry;
                    Location location = blockLogEntry.asBukkitLocation();

                    if (verbose) {
                        String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "PLACE"
                                : "BREAK";
                        sender.sendMessage("§7[VERBOSE] §e" + actionName + " §7at §f" +
                                (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                                " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                                " §7material: §f" + blockLogEntry.getDataString() +
                                " §7by §f" + PlayerUtils.getNameOfEntity(blockLogEntry.getPlayerId()) +
                                " §7(ID: " + blockLogEntry.getPlayerId() + ") §7at §f" +
                                new SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
                                        .format(new Date(blockLogEntry.getCreatedAt())));
                    }

                    try {
                        boolean isPlace = blockLogEntry.usesOldState();
                        BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString(),
                                blockLogEntry.getExtraType(), blockLogEntry.getExtraData(),
                                isPlace, blockLogEntry.getOldDataString(), blockLogEntry.getBlockEntityNbt(),
                                blockLogEntry.getOldBlockEntityNbt());

                        if (isPlace) {
                            plugin.getStellarTaskHook(() -> blockRestore.previewRemove(sender, location))
                                    .runTask(location);
                        } else {
                            plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location))
                                    .runTask(location);
                        }
                    } catch (Exception e) {
                        sender.sendMessage("§c[ERROR] Failed to preview block at " + (int) location.getX() + ", "
                                + (int) location.getY() + ", " + (int) location.getZ() + " "
                                + blockLogEntry.getBlockId());
                    }
                } else if (logEntry instanceof PlayerBlockStateLogEntry) {
                    PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
                    Location location = blockStateLogEntry.asBukkitLocation();
                    try {
                        BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.lastDataString());

                        if (verbose) {
                            sender.sendMessage("§7[VERBOSE] §eSTATE_CHANGE §7at §f" +
                                    (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ()
                                    +
                                    " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                                    " §7material: §f" + blockStateLogEntry.lastDataString() +
                                    " §7by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                                    " §7(ID: " + blockStateLogEntry.getPlayerId() + ") §7at §f" +
                                    new SimpleDateFormat("HH:mm:ss dd/MM/yyyy")
                                            .format(new Date(blockStateLogEntry.getCreatedAt())));
                        }

                        plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
                    } catch (Exception e) {
                        sender.sendMessage("§c[ERROR] Failed to preview block state at " + (int) location.getX() + ", "
                                + (int) location.getY() + ", " + (int) location.getZ());
                    }
                }
            }
        }
    }

    public void restoreBlocks(CommandSender sender, Map<LocationCache, Set<LogEntry>> groupedLogs, boolean verbose,
            boolean silent) {
        int processedCount = 0;
        for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
            for (LogEntry logEntry : entry.getValue()) {
                restore(logEntry, sender, verbose);

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

    public void restore(LogEntry logEntry, CommandSender sender, boolean verbose) {
        Gson gson = SerializerUtils.getGson();
        if (logEntry instanceof PlayerBlockLogEntry) {
            PlayerBlockLogEntry blockLogEntry = (PlayerBlockLogEntry) logEntry;
            Location location = blockLogEntry.asBukkitLocation();

            if (verbose) {
                String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "REMOVING"
                        : "RESTORING";
                sender.sendMessage("§7[VERBOSE] §c" + actionName + " §7block at §f" +
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
                plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
            } catch (Exception e) {
                sender.sendMessage("§c[ERROR] Failed to restore block at " + (int) location.getX() + ", "
                        + (int) location.getY() + ", " + (int) location.getZ() + " " + blockLogEntry.getBlockId() + " "
                        + e.getMessage());
            }
        } else if (logEntry instanceof PlayerBlockStateLogEntry) {
            PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
            Location location = blockStateLogEntry.asBukkitLocation();
            try {
                BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.lastDataString());

                if (verbose) {
                    sender.sendMessage("§7[VERBOSE] §cREVERTING §7state at §f" +
                            (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                            " §7in §f" + (location.getWorld() == null ? "?" : location.getWorld().getName()) +
                            " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                            " §7ID: " + blockStateLogEntry.getPlayerId() + "§7) §7data: §f"
                            + blockStateLogEntry.lastDataString());
                }

                plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
            } catch (Exception e) {
                sender.sendMessage("§c[ERROR] Failed to restore block state at " + (int) location.getX() + ", "
                        + (int) location.getY() + ", " + (int) location.getZ() + " " + e.getMessage());
            }
        } else if (logEntry instanceof PlayerKillLogEntry) {
            PlayerKillLogEntry playerKillLogEntry = (PlayerKillLogEntry) logEntry;
            if (playerKillLogEntry.getEntityType().equals("PLAYER"))
                return;

            EntityType entityType = EntityType.valueOf(playerKillLogEntry.getEntityType());
            Location location = playerKillLogEntry.asBukkitLocation();

            if (location.getWorld() == null)
                return;
            plugin.getStellarTaskHook(() -> {
                NamespacedKey restoreKey = new NamespacedKey(plugin, "restored_entity_log");
                String marker = String.valueOf(playerKillLogEntry.getId());
                for (Entity nearby : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
                    if (marker.equals(nearby.getPersistentDataContainer().get(restoreKey, PersistentDataType.STRING)))
                        return;
                }
                Entity entity = location.getWorld().spawnEntity(location, entityType);
                try {
                    io.github.insideranh.stellarprotect.entities.DataEntity dataEntity = plugin
                            .getDataEntity(playerKillLogEntry.getEntityData().getEntityData());
                    dataEntity.applyToEntity(entity);
                } catch (Exception exception) {
                    plugin.getLogger().warning("Failed to restore entity state: " + exception.getMessage());
                } finally {
                    entity.getPersistentDataContainer().set(restoreKey, PersistentDataType.STRING, marker);
                }
            }).runTask(location);
        }
    }

}
