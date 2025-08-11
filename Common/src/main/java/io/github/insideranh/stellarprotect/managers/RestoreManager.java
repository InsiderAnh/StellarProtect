package io.github.insideranh.stellarprotect.managers;

import com.google.gson.Gson;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Set;

public class RestoreManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void rollback(CommandSender sender, Map<LocationCache, Set<LogEntry>> groupedLogs) {
        Gson gson = SerializerUtils.getGson();
        int processedCount = 0;
        final int MAX_PER_TICK = 50;

        for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
            for (LogEntry logEntry : entry.getValue()) {
                if (logEntry instanceof PlayerBlockLogEntry) {
                    PlayerBlockLogEntry blockLogEntry = (PlayerBlockLogEntry) logEntry;
                    Location location = blockLogEntry.asBukkitLocation();

                    BlockRestore blockRestore = plugin.getBlockRestore(blockLogEntry.getData());
                    if (blockLogEntry.getActionType() == ActionType.BLOCK_PLACE.getId()) {
                        plugin.getStellarTaskHook(() -> blockRestore.remove(location)).runTask(location);
                    } else if (blockLogEntry.getActionType() == ActionType.BLOCK_BREAK.getId()) {
                        plugin.getStellarTaskHook(() -> blockRestore.reset(gson, location)).runTask(location);
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
    }

}