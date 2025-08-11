package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface RestoreRepository {

    CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, @NonNull List<ActionType> actionTypes, int skip, int limit);

    CompletableFuture<Long> countRestoreActions(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, @NonNull List<ActionType> actionTypes);

}