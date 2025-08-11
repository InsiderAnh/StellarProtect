package io.github.insideranh.stellarprotect.database.types.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.callback.CallbackLookup;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.repositories.RestoreRepository;
import io.github.insideranh.stellarprotect.database.types.factory.LogEntryFactory;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.Debugger;
import lombok.NonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RestoreRepositoryMongo implements RestoreRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final MongoDatabase database;
    private final MongoCollection<Document> players;
    private final MongoCollection<Document> logEntries;

    public RestoreRepositoryMongo(MongoDatabase database) {
        this.database = database;
        this.players = database.getCollection(stellarProtect.getConfigManager().getTablesPlayers());
        this.logEntries = database.getCollection(stellarProtect.getConfigManager().getTablesLogEntries());
    }

    @Override
    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, @NotNull List<ActionType> actionTypes, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LogEntry> cachedLogs = LoggerCache.getLogs(timeArg, radiusArg, actionTypes, skip, limit)
                .stream()
                .sorted(Comparator.comparingLong(LogEntry::getCreatedAt).reversed())
                .collect(Collectors.toList());

            Map<LocationCache, Set<LogEntry>> groupedResults = cachedLogs.stream()
                .collect(Collectors.groupingBy(
                    LocationCache::of,
                    LinkedHashMap::new,
                    Collectors.toCollection(LinkedHashSet::new)
                ));

            int remaining = limit - cachedLogs.size();

            if (remaining > 0) {
                int dbSkip = skip + cachedLogs.size();

                CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> dbLookup = queryLogsFromDB(timeArg, radiusArg, actionTypes, dbSkip, remaining);

                List<LogEntry> dbLogs = dbLookup.getLogs().values().stream()
                    .flatMap(Set::stream)
                    .filter(log -> cachedLogs.stream().noneMatch(c -> c.equals(log)))
                    .sorted(Comparator.comparingLong(LogEntry::getCreatedAt).reversed())
                    .limit(remaining)
                    .collect(Collectors.toList());

                Map<LocationCache, Set<LogEntry>> dbGrouped = dbLogs.stream()
                    .collect(Collectors.groupingBy(
                        LocationCache::of,
                        LinkedHashMap::new,
                        Collectors.toCollection(LinkedHashSet::new)
                    ));

                dbGrouped.forEach((location, logs) ->
                    groupedResults.merge(location, logs, (existing, newLogs) -> {
                        existing.addAll(newLogs);
                        return existing;
                    })
                );

                return new CallbackLookup<>(groupedResults, dbLookup.getTotal());
            }

            return new CallbackLookup<>(groupedResults, (long) skip + cachedLogs.size());
        }, stellarProtect.getLookupExecutor());
    }

    public CompletableFuture<Long> countRestoreActions(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, @NotNull List<ActionType> actionTypes) {
        return CompletableFuture.supplyAsync(() -> {
            long cachedCount = LoggerCache.countLogs(timeArg, radiusArg, actionTypes);
            long dbCount = countLogsFromDB(timeArg, radiusArg, actionTypes);

            return cachedCount + dbCount;
        }, stellarProtect.getLookupExecutor());
    }

    private long countLogsFromDB(TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes) {
        List<Integer> actionTypeNames = actionTypes.stream()
            .map(ActionType::getId)
            .collect(Collectors.toList());

        Bson filter = Filters.and(
            Filters.gte("created_at", timeArg.getStart()),
            Filters.lte("created_at", timeArg.getEnd()),
            Filters.gte("x", radiusArg.getMinX()),
            Filters.lte("x", radiusArg.getMaxX()),
            Filters.gte("y", radiusArg.getMinY()),
            Filters.lte("y", radiusArg.getMaxY()),
            Filters.gte("z", radiusArg.getMinZ()),
            Filters.lte("z", radiusArg.getMaxZ()),
            Filters.in("action_type", actionTypeNames)
        );

        return logEntries.countDocuments(filter);
    }

    private CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> queryLogsFromDB(
        TimeArg timeArg,
        RadiusArg radiusArg,
        List<ActionType> actionTypes,
        int skip,
        int limit) {

        Set<LogEntry> logs = new LinkedHashSet<>();

        List<Integer> actionTypeNames = actionTypes.stream()
            .map(ActionType::getId)
            .collect(Collectors.toList());

        Bson filter = Filters.and(
            Filters.gte("created_at", timeArg.getStart()),
            Filters.lte("created_at", timeArg.getEnd()),
            Filters.gte("x", radiusArg.getMinX()),
            Filters.lte("x", radiusArg.getMaxX()),
            Filters.gte("y", radiusArg.getMinY()),
            Filters.lte("y", radiusArg.getMaxY()),
            Filters.gte("z", radiusArg.getMinZ()),
            Filters.lte("z", radiusArg.getMaxZ()),
            Filters.in("action_type", actionTypeNames)
        );

        long totalCount = logEntries.countDocuments(filter);

        FindIterable<Document> logDocs = logEntries.find(filter)
            .sort(Sorts.descending("created_at"))
            .skip(skip)
            .limit(limit);

        for (Document doc : logDocs) {
            long playerId = doc.getLong("player_id");

            Document playerDoc = players.find(Filters.eq("id", playerId)).first();
            if (playerDoc != null) {
                String playerName = playerDoc.getString("name");
                PlayerCache.cacheName(playerId, playerName);
            }

            try {
                LogEntry entry = LogEntryFactory.fromDocument(doc);
                logs.add(entry);
            } catch (Exception e) {
                Debugger.debugLog("Error al cargar log: " + doc.toJson());
            }
        }

        Map<LocationCache, Set<LogEntry>> groupedLogs = logs.stream().collect(
            Collectors.groupingBy(
                LocationCache::of,
                LinkedHashMap::new,
                Collectors.toCollection(LinkedHashSet::new)
            )
        );

        return new CallbackLookup<>(groupedLogs, totalCount);
    }

}