package io.github.insideranh.stellarprotect.database;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import io.github.insideranh.stellarprotect.database.repositories.DatabaseConnection;
import io.github.insideranh.stellarprotect.database.types.MongoConnection;
import io.github.insideranh.stellarprotect.database.types.MySQLConnection;
import io.github.insideranh.stellarprotect.database.types.SQLConnection;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
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

    public void connect() {
        String databaseType = stellarProtect.getConfig().getString("databases.type", "h2");
        if (databaseType.equalsIgnoreCase("mysql")) {
            this.databaseConnection = new MySQLConnection();
        } else if (databaseType.equalsIgnoreCase("mongodb")) {
            this.databaseConnection = new MongoConnection();
        } else {
            this.databaseConnection = new SQLConnection();
        }
        this.databaseConnection.connect();
        this.stellarProtect.getLookupExecutor().execute(() -> this.databaseConnection.createIndexes());
    }

    public void load() {
        this.databaseConnection.getLoggerRepository().loadTemporaryLogs();

        this.databaseConnection.getIdsRepository().loadWorlds();
        this.databaseConnection.getIdsRepository().loadEntityIds();

        this.databaseConnection.getItemsRepository().loadMostUsedItems();
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

    public CompletableFuture<CallbackLookup<List<ItemLogEntry>, Long>> getChestTransactions(@NonNull Location location, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getChestTransactions(location, skip, limit);
    }

    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getLogs(@NonNull DatabaseFilters databaseFilters, int skip, int limit) {
        return databaseConnection.getLoggerRepository().getLogs(databaseFilters, skip, limit);
    }

    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, @NonNull List<ActionType> actionTypes, int skip, int limit) {
        return databaseConnection.getRestoreRepository().getRestoreActions(timeArg, radiusArg, actionTypes, skip, limit);
    }

    public CompletableFuture<Long> countRestoreActions(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, List<ActionType> actionTypes) {
        return databaseConnection.getRestoreRepository().countRestoreActions(timeArg, radiusArg, actionTypes);
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

}