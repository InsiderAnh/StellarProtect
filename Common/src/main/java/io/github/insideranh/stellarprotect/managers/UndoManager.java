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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public class UndoManager {

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
                        sender.sendMessage("§7[VERBOSE UNDO] §e" + actionName + " §7at §f" +
                            (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                            " §7in §f" + location.getWorld().getName() +
                            " §7material: §f" + materialName +
                            " §7by §f" + PlayerUtils.getNameOfEntity(blockLogEntry.getPlayerId()) +
                            " §7(ID: " + blockLogEntry.getPlayerId() + ") §7at §f" +
                            new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new java.util.Date(blockLogEntry.getCreatedAt())));
                    }

                    BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString());

                    if (blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_EMPTY.getId()) {
                        plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
                    } else if (blockLogEntry.getActionType() == ActionType.BLOCK_BREAK.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_FILL.getId()) {
                        plugin.getStellarTaskHook(() -> blockRestore.previewRemove(sender, location)).runTask(location);
                    }
                } else if (logEntry instanceof PlayerBlockStateLogEntry) {
                    PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
                    BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.getDataString());
                    Location location = blockStateLogEntry.asBukkitLocation();

                    if (verbose) {
                        String materialName = blockStateLogEntry.getDataString().split(":")[0];
                        sender.sendMessage("§7[VERBOSE UNDO] §eSTATE_CHANGE §7at §f" +
                            (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                            " §7in §f" + location.getWorld().getName() +
                            " §7material: §f" + materialName +
                            " §7by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                            " §7(ID: " + blockStateLogEntry.getPlayerId() + ") §7at §f" +
                            new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new java.util.Date(blockStateLogEntry.getCreatedAt())));
                    }

                    if (!silent) {
                        plugin.getStellarTaskHook(() -> blockRestore.preview(sender, gson, location)).runTask(location);
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

    public void undo(CommandSender sender, Map<LocationCache, Set<LogEntry>> groupedLogs, boolean verbose) {
        int processedCount = 0;
        final int MAX_PER_TICK = 50;

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
                String actionName = blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() ? "RE-PLACING" : "RE-REMOVING";
                sender.sendMessage("§7[VERBOSE UNDO] §a" + actionName + " §7block at §f" +
                    (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                    " §7in §f" + location.getWorld().getName() +
                    " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                    " §7ID: " + blockLogEntry.getPlayerId() + "§7) §7data: §f" + blockLogEntry.getDataString());
            }

            BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getDataString(), blockLogEntry.getExtraType(), blockLogEntry.getExtraData());

            if (blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_EMPTY.getId()) {
                plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
            } else if (blockLogEntry.getActionType() == ActionType.BLOCK_BREAK.getId() || blockLogEntry.getActionType() == ActionType.BUCKET_FILL.getId()) {
                plugin.getStellarTaskHook(() -> blockRestore.remove(location)).runTask(location);
            }
        } else if (logEntry instanceof PlayerBlockStateLogEntry) {
            PlayerBlockStateLogEntry blockStateLogEntry = (PlayerBlockStateLogEntry) logEntry;
            BlockRestore blockRestore = plugin.getBlockRestore(blockStateLogEntry.getDataString());
            Location location = blockStateLogEntry.asBukkitLocation();

            if (verbose) {
                sender.sendMessage("§7[VERBOSE UNDO] §aRESTORING §7state at §f" +
                    (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                    " §7in §f" + location.getWorld().getName() +
                    " §7(originally by §f" + PlayerUtils.getNameOfEntity(logEntry.getPlayerId()) +
                    " §7ID: " + blockStateLogEntry.getPlayerId() + "§7) §7data: §f" + blockStateLogEntry.getDataString());
            }

            plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
        } else if (logEntry instanceof PlayerKillLogEntry) {
            PlayerKillLogEntry playerKillLogEntry = (PlayerKillLogEntry) logEntry;
            if (playerKillLogEntry.getEntityType().equals("PLAYER")) return;

            Location location = playerKillLogEntry.asBukkitLocation();

            if (verbose) {
                sender.sendMessage("§7[VERBOSE UNDO] §aREMOVING §7restored entity at §f" +
                    (int) location.getX() + ", " + (int) location.getY() + ", " + (int) location.getZ() +
                    " §7in §f" + location.getWorld().getName() +
                    " §7type: §f" + playerKillLogEntry.getEntityType());
            }

            location.getWorld().getNearbyEntities(location, 1, 1, 1).stream()
                .filter(entity -> entity.getType().name().equals(playerKillLogEntry.getEntityType()))
                .forEach(Entity::remove);
        }
    }

}