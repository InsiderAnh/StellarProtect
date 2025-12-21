package io.github.insideranh.stellarprotect.database;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.QueuedLog;
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import io.github.insideranh.stellarprotect.database.repositories.DatabaseConnection;
import io.github.insideranh.stellarprotect.database.types.MySQLConnection;
import io.github.insideranh.stellarprotect.database.types.SQLConnection;
import io.github.insideranh.stellarprotect.database.types.SQLQueueConnection;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ProtectDatabase {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private DatabaseConnection databaseConnection;
    private SQLQueueConnection temporalConnection;

    public void connect() {
        String databaseType = stellarProtect.getConfig().getString("databases.type", "h2");
        if (databaseType.equalsIgnoreCase("mysql")) {
            this.databaseConnection = new MySQLConnection();
        } else {
            this.databaseConnection = new SQLConnection();
        }
        this.temporalConnection = new SQLQueueConnection();
        this.databaseConnection.connect();
        this.temporalConnection.connect();
        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.createIndexes());
    }

    public void load() {
        this.databaseConnection.getIdsRepository().loadWorlds();
        this.databaseConnection.getIdsRepository().loadEntityIds();

        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.getItemsRepository().loadMostUsedItems());
        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.getBlocksRepository().loadBlockDatas());
    }

    public void close() {
        this.databaseConnection.close();
    }

    public void clearOldLogs() {
        this.databaseConnection.getLoggerRepository().clearOldLogs();
    }

    public void purgeLogs(@NonNull DatabaseFilters databaseFilters, Consumer<Long> onFinished) {
        this.databaseConnection.getLoggerRepository().purgeLogs(databaseFilters, onFinished);
    }

    public void saveQueue(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) return;

        this.temporalConnection.save(logEntries);
    }


    public CompletableFuture<CallbackLookup<List<ItemLogEntry>, Long>> getChestTransactions(@NonNull Location location, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getChestTransactions(location, skip, limit);
    }

    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getLogs(@NonNull DatabaseFilters databaseFilters, boolean ignoreCache, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getLogs(databaseFilters, ignoreCache, skip, limit);
    }

    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull DatabaseFilters filters, int skip, int limit) {
        return databaseConnection.getRestoreRepository().getRestoreActions(filters, skip, limit);
    }

    public CompletableFuture<Long> countRestoreActions(@NonNull DatabaseFilters filters) {
        return databaseConnection.getRestoreRepository().countRestoreActions(filters);
    }

    public CompletableFuture<CallbackLookup<Set<LogEntry>, Long>> getLogs(@NonNull Location location, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getLogs(location, skip, limit);
    }

    public List<Long> getIdsByNames(List<String> names) {
        return databaseConnection.getPlayerRepository().getIdsByNames(names);
    }

    public PlayerProtect loadOrCreatePlayer(Player player) {
        return databaseConnection.getPlayerRepository().loadOrCreatePlayer(player);
    }

    public void save(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) return;

        this.databaseConnection.getLoggerRepository().save(logEntries);
    }

    public void saveWorld(String world, int id) {
        this.databaseConnection.getIdsRepository().saveWorld(world, id);
    }

    public void saveEntityId(String entityType, long id) {
        this.databaseConnection.getIdsRepository().saveEntityId(entityType, id);
    }

    public void saveItems(List<ItemTemplate> itemTemplates) {
        this.databaseConnection.getItemsRepository().saveItems(itemTemplates);
    }

    public void saveBlocks(List<BlockTemplate> blockTemplates) {
        this.databaseConnection.getBlocksRepository().saveBlocks(blockTemplates);
    }

    public void processQueueLogs() {
        stellarProtect.getLookupExecutor().execute(() -> {
            try {
                int batchSize = stellarProtect.getConfigManager().getBatchQueueSize();
                List<QueuedLog> queuedLogs = temporalConnection.getLogs(batchSize);

                if (queuedLogs.isEmpty()) {
                    return;
                }

                Debugger.debugSave("Processing " + queuedLogs.size() + " logs from queue...");

                List<LogEntry> logEntries = new java.util.ArrayList<>();
                for (QueuedLog queuedLog : queuedLogs) {
                    LogEntry logEntry = new LogEntry(
                        queuedLog.getPlayerId(),
                        queuedLog.getActionType(),
                        queuedLog.getWorldId(),
                        queuedLog.getX(),
                        queuedLog.getY(),
                        queuedLog.getZ(),
                        queuedLog.getCreatedAt()
                    );

                    if (queuedLog.getExtraJson() != null && !queuedLog.getExtraJson().isEmpty()) {
                        logEntry.setForcedJson(queuedLog.getExtraJson());
                    }

                    logEntries.add(logEntry);
                }

                databaseConnection.getLoggerRepository().save(logEntries);

                if (!queuedLogs.isEmpty()) {
                    long maxId = queuedLogs.get(queuedLogs.size() - 1).getId();
                    temporalConnection.deleteProcessedLogs(maxId);
                }

                Debugger.debugSave("Successfully processed " + queuedLogs.size() + " logs from queue");
            } catch (Exception e) {
                stellarProtect.getLogger().warning("Error processing queue logs: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

}