package io.github.insideranh.stellarprotect.managers;

import com.google.gson.Gson;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockStateLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerKillLogEntry;
import io.github.insideranh.stellarprotect.entities.DataEntity;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class RestoreManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void preview(Player sender, Map<LocationCache, Set<LogEntry>> groupedLogs, boolean verbose, boolean silent) {
        Gson gson = SerializerUtils.getGson();
        int processedCount = 0;
        final int MAX_PER_TICK = 50;

        for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
            for (LogEntry logEntry : entry.getValue()) {
                if (logEntry instanceof PlayerBlockLogEntry) {
                    PlayerBlockLogEntry blockLogEntry = (PlayerBlockLogEntry) logEntry;
                    Location location = blockLogEntry.asBukkitLocation();

                    if (verbose) {
                        String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "PLACE" : "BREAK";
                        String materialName = blockLogEntry.getDataString().split(":")[0];
                        sender.sendMessage("§7[VERBOSE] §e" + actionName + " §7at §f" +
                            (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                            " §7in §f" + location.getWorld().getName() +
                            " §7material: §f" + materialName +
                            " §7by §f" + PlayerUtils.getNameOfEntity(blockLogEntry.getPlayerId()) +
                            " §7(ID: " + blockLogEntry.getPlayerId() + ") §7at §f" +
                            new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new java.util.Date(blockLogEntry.getCreatedAt())));
                    }

                    try {
                        BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString());
                        if (blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_EMPTY.getId()) {
                            plugin.getStellarTaskHook(() -> blockRestore.previewRemove(sender, location)).runTask(location);
                        } else if (blockLogEntry.getActionType() == ActionType.BLOCK_BREAK.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_FILL.getId()) {
                            plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
                        }
                    } catch (Exception e) {
                        sender.sendMessage("§c[ERROR] Failed to preview block at " + (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() + " " + blockLogEntry.getBlockId());
                    }
                } else if (logEntry instanceof PlayerBlockStateLogEntry) {
                    PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
                    Location location = blockStateLogEntry.asBukkitLocation();
                    try {
                        BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.lastDataString());

                        if (verbose) {
                            String materialName = blockStateLogEntry.lastDataString().split(":")[0];
                            sender.sendMessage("§7[VERBOSE] §eSTATE_CHANGE §7at §f" +
                                (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                                " §7in §f" + location.getWorld().getName() +
                                " §7material: §f" + materialName +
                                " §7by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                                " §7(ID: " + blockStateLogEntry.getPlayerId() + ") §7at §f" +
                                new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date(blockStateLogEntry.getCreatedAt())));
                        }

                        if (!silent) {
                            plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
                        }
                    } catch (Exception e) {
                        sender.sendMessage("§c[ERROR] Failed to preview block state at " + (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ());
                    }
                }

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

    public void rollback(CommandSender sender, Map<LocationCache, Set<LogEntry>> groupedLogs, boolean verbose, boolean silent) {
        int processedCount = 0;
        final int MAX_PER_TICK = 50;

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
                String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "REMOVING" : "RESTORING";
                sender.sendMessage("§7[VERBOSE] §c" + actionName + " §7block at §f" +
                    (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                    " §7in §f" + location.getWorld().getName() +
                    " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                    " §7ID: " + blockLogEntry.getPlayerId() + "§7) §7data: §f" + blockLogEntry.getDataString());
            }

            try {
                BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString(), blockLogEntry.getExtraType(), blockLogEntry.getExtraData());
                if (blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_EMPTY.getId()) {
                    plugin.getStellarTaskHook(() -> blockRestore.remove(location)).runTask(location);
                } else if (blockLogEntry.getActionType() == ActionType.BLOCK_BREAK.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_FILL.getId()) {
                    plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
                }
            } catch (Exception e) {
                sender.sendMessage("§c[ERROR] Failed to restore block at " + (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() + " " + blockLogEntry.getBlockId());
            }
        } else if (logEntry instanceof PlayerBlockStateLogEntry) {
            PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
            Location location = blockStateLogEntry.asBukkitLocation();
            try {
                BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.lastDataString());

                if (verbose) {
                    sender.sendMessage("§7[VERBOSE] §cREVERTING §7state at §f" +
                        (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                        " §7in §f" + location.getWorld().getName() +
                        " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                        " §7ID: " + blockStateLogEntry.getPlayerId() + "§7) §7data: §f" + blockStateLogEntry.lastDataString());
                }

                plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
            } catch (Exception e) {
                sender.sendMessage("§c[ERROR] Failed to restore block state at " + (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ());
            }
        } else if (logEntry instanceof PlayerKillLogEntry) {
            PlayerKillLogEntry playerKillLogEntry = (PlayerKillLogEntry) logEntry;
            if (playerKillLogEntry.getEntityType().equals("PLAYER")) return;

            EntityType entityType = EntityType.valueOf(playerKillLogEntry.getEntityType());
            Location location = playerKillLogEntry.asBukkitLocation();

            Entity entity = location.getWorld().spawnEntity(location, entityType);
            DataEntity dataEntity = plugin.getDataEntity(playerKillLogEntry.getEntityData().getEntityData());

            dataEntity.applyToEntity(entity);
        }
    }

}