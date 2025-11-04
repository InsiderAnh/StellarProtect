package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import lombok.NonNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface RestoreRepository {

    CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull DatabaseFilters filters, int skip, int limit);

    CompletableFuture<Long> countRestoreActions(@NonNull DatabaseFilters filters);

}