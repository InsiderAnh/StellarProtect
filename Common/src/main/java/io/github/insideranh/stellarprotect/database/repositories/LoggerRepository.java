package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LoggerRepository {

    void loadTemporaryLogs();

    void clearOldLogs();

    void save(List<LogEntry> logEntries);

    void purgeLogs(@NonNull DatabaseFilters databaseFilters, Consumer<Long> onFinished);

    CompletableFuture<CallbackLookup<List<ItemLogEntry>, Long>> getChestTransactions(@NonNull Location location, int skip, int limit);

    CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getLogs(@NonNull DatabaseFilters databaseFilters, int skip, int limit);

    CompletableFuture<CallbackLookup<Set<LogEntry>, Long>> getLogs(@NonNull Location location, int skip, int limit);

}