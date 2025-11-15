package io.github.insideranh.stellarprotect.database.types.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.arguments.UsersArg;
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
    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull DatabaseFilters filters, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            TimeArg timeArg = filters.getTimeFilter();
            RadiusArg radiusArg = filters.getRadiusFilter();
            List<Integer> actionTypes = filters.getActionTypesFilter();

            List<ActionType> actionTypeObjects = actionTypes != null ?
                actionTypes.stream()
                    .map(ActionType::getById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()) :
                new ArrayList<>();

            List<LogEntry> cachedLogs = new ArrayList<>();
            if (!filters.isIgnoreCache() && !actionTypeObjects.isEmpty()) {
                cachedLogs = LoggerCache.getLogs(timeArg, radiusArg, actionTypeObjects, skip, limit)
                    .stream()
                    .sorted(Comparator.comparingLong(LogEntry::getCreatedAt).reversed())
                    .collect(Collectors.toList());
            }

            Map<LocationCache, Set<LogEntry>> groupedResults = cachedLogs.stream()
                .collect(Collectors.groupingBy(
                    LocationCache::of,
                    LinkedHashMap::new,
                    Collectors.toCollection(LinkedHashSet::new)
                ));

            int remaining = limit - cachedLogs.size();

            if (remaining > 0) {
                int dbSkip = skip + cachedLogs.size();

                CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> dbLookup = queryLogsFromDB(filters, dbSkip, remaining);

                List<LogEntry> finalCachedLogs = cachedLogs;
                List<LogEntry> dbLogs = dbLookup.getLogs().values().stream()
                    .flatMap(Set::stream)
                    .filter(log -> finalCachedLogs.stream().noneMatch(c -> c.equals(log)))
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

    public CompletableFuture<Long> countRestoreActions(@NonNull DatabaseFilters filters) {
        return CompletableFuture.supplyAsync(() -> {
            TimeArg timeArg = filters.getTimeFilter();
            RadiusArg radiusArg = filters.getRadiusFilter();
            List<Integer> actionTypes = filters.getActionTypesFilter();

            List<ActionType> actionTypeObjects = actionTypes != null ?
                actionTypes.stream()
                    .map(ActionType::getById)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()) :
                new ArrayList<>();

            long cachedCount = 0;
            if (!filters.isIgnoreCache() && !actionTypeObjects.isEmpty()) {
                cachedCount = LoggerCache.countLogs(timeArg, radiusArg, actionTypeObjects);
            }
            long dbCount = countLogsFromDB(filters);

            return cachedCount + dbCount;
        }, stellarProtect.getLookupExecutor());
    }

    private long countLogsFromDB(DatabaseFilters filters) {
        Bson filter = buildFilters(filters);
        return logEntries.countDocuments(filter);
    }

    private CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> queryLogsFromDB(
        DatabaseFilters filters,
        int skip,
        int limit) {

        Set<LogEntry> logs = new LinkedHashSet<>();

        Bson filter = buildFilters(filters);
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

    private Bson buildFilters(DatabaseFilters databaseFilters) {
        TimeArg timeArg = databaseFilters.getTimeFilter();
        RadiusArg radiusArg = databaseFilters.getRadiusFilter();
        UsersArg usersArg = databaseFilters.getUserFilters();
        List<Integer> actionTypes = databaseFilters.getActionTypesFilter();
        List<Long> includeFilter = databaseFilters.getAllIncludeFilters();
        List<Long> excludeFilter = databaseFilters.getAllExcludeFilters();
        List<Long> materialIncludeFilters = databaseFilters.getIncludeMaterialFilters();
        List<Long> materialExcludeFilters = databaseFilters.getExcludeMaterialFilters();
        List<Long> blockIncludeFilters = databaseFilters.getIncludeBlockFilters();
        List<Long> blockExcludeFilters = databaseFilters.getExcludeBlockFilters();

        List<Bson> filters = new ArrayList<>();

        if (timeArg != null) {
            filters.add(Filters.gte("created_at", timeArg.getStart()));
            filters.add(Filters.lte("created_at", timeArg.getEnd()));
        }

        if (radiusArg != null) {
            if (radiusArg.getWorldId() != -1) {
                filters.add(Filters.eq("world_id", radiusArg.getWorldId()));
            }
            filters.add(Filters.gte("x", radiusArg.getMinX()));
            filters.add(Filters.lte("x", radiusArg.getMaxX()));
            filters.add(Filters.gte("y", radiusArg.getMinY()));
            filters.add(Filters.lte("y", radiusArg.getMaxY()));
            filters.add(Filters.gte("z", radiusArg.getMinZ()));
            filters.add(Filters.lte("z", radiusArg.getMaxZ()));
        }

        if (usersArg != null && usersArg.getUserIds() != null && !usersArg.getUserIds().isEmpty()) {
            filters.add(Filters.in("player_id", usersArg.getUserIds()));
        }

        if (actionTypes != null && !actionTypes.isEmpty()) {
            filters.add(Filters.in("action_type", actionTypes));
        }

        if (includeFilter != null && !includeFilter.isEmpty()) {
            filters.add(buildIncludeFilter(includeFilter));
        }

        if (excludeFilter != null && !excludeFilter.isEmpty()) {
            filters.add(buildExcludeFilter(excludeFilter));
        }

        if (materialIncludeFilters != null && !materialIncludeFilters.isEmpty()) {
            filters.add(buildMaterialIncludeFilter(materialIncludeFilters));
        }

        if (materialExcludeFilters != null && !materialExcludeFilters.isEmpty()) {
            filters.add(buildMaterialExcludeFilter(materialExcludeFilters));
        }

        if (blockIncludeFilters != null && !blockIncludeFilters.isEmpty()) {
            filters.add(buildBlockIncludeFilter(blockIncludeFilters));
        }

        if (blockExcludeFilters != null && !blockExcludeFilters.isEmpty()) {
            filters.add(buildBlockExcludeFilter(blockExcludeFilters));
        }

        return filters.isEmpty() ? new Document() : Filters.and(filters);
    }

    private Bson buildIncludeFilter(List<Long> includeFilter) {
        if (includeFilter.size() == 1) {
            Long itemId = includeFilter.get(0);
            return Filters.text("\"id\":" + itemId + " OR \"ai\":{\"" + itemId + "\" OR \"ri\":{\"" + itemId + "\"");
        }

        List<Bson> conditions = new ArrayList<>();
        for (Long itemId : includeFilter) {
            conditions.add(Filters.text("\"id\":" + itemId + " OR \"ai\":{\"" + itemId + "\" OR \"ri\":{\"" + itemId + "\""));
        }

        return Filters.or(conditions);
    }

    private Bson buildExcludeFilter(List<Long> excludeFilter) {
        List<Bson> excludeConditions = new ArrayList<>();

        for (Long itemId : excludeFilter) {
            List<Bson> singleExcludeConditions = new ArrayList<>();

            singleExcludeConditions.add(Filters.not(Filters.regex("extra_json",
                ".*\"id\"\\s*:\\s*\"?" + itemId + "\"?\\s*[,}].*")));

            singleExcludeConditions.add(Filters.not(Filters.regex("extra_json",
                ".*\"ai\"\\s*:\\s*\\{[^}]*\"" + itemId + "\"\\s*:\\s*\\d+.*")));

            singleExcludeConditions.add(Filters.not(Filters.regex("extra_json",
                ".*\"ri\"\\s*:\\s*\\{[^}]*\"" + itemId + "\"\\s*:\\s*\\d+.*")));

            excludeConditions.add(Filters.and(singleExcludeConditions));
        }

        return excludeConditions.size() == 1 ? excludeConditions.get(0) : Filters.and(excludeConditions);
    }

    private Bson buildMaterialIncludeFilter(List<Long> materialFilters) {
        if (materialFilters.size() == 1) {
            Long materialId = materialFilters.get(0);
            return Filters.or(
                Filters.regex("extra_json", ".*\"id\"\\s*:\\s*" + materialId + "\\s*[,}].*"),
                Filters.regex("extra_json", ".*\"ai\"\\s*:\\s*\\{[^}]*\"" + materialId + "\"\\s*:.*"),
                Filters.regex("extra_json", ".*\"ri\"\\s*:\\s*\\{[^}]*\"" + materialId + "\"\\s*:.*")
            );
        }

        List<Bson> materialConditions = new ArrayList<>();
        for (Long materialId : materialFilters) {
            materialConditions.add(Filters.or(
                Filters.regex("extra_json", ".*\"id\"\\s*:\\s*" + materialId + "\\s*[,}].*"),
                Filters.regex("extra_json", ".*\"ai\"\\s*:\\s*\\{[^}]*\"" + materialId + "\"\\s*:.*"),
                Filters.regex("extra_json", ".*\"ri\"\\s*:\\s*\\{[^}]*\"" + materialId + "\"\\s*:.*")
            ));
        }

        return Filters.or(materialConditions);
    }

    private Bson buildMaterialExcludeFilter(List<Long> materialFilters) {
        List<Bson> excludeConditions = new ArrayList<>();

        for (Long materialId : materialFilters) {
            excludeConditions.add(Filters.and(
                Filters.not(Filters.regex("extra_json", ".*\"id\"\\s*:\\s*" + materialId + "\\s*[,}].*")),
                Filters.not(Filters.regex("extra_json", ".*\"ai\"\\s*:\\s*\\{[^}]*\"" + materialId + "\"\\s*:.*")),
                Filters.not(Filters.regex("extra_json", ".*\"ri\"\\s*:\\s*\\{[^}]*\"" + materialId + "\"\\s*:.*"))
            ));
        }

        return excludeConditions.size() == 1 ? excludeConditions.get(0) : Filters.and(excludeConditions);
    }

    private Bson buildBlockIncludeFilter(List<Long> blockFilters) {
        if (blockFilters.size() == 1) {
            Long blockId = blockFilters.get(0);
            return Filters.or(
                Filters.regex("extra_json", ".*\"b\"\\s*:\\s*" + blockId + "\\s*[,}].*"),
                Filters.regex("extra_json", ".*\"ob\"\\s*:\\s*" + blockId + "\\s*[,}].*")
            );
        }

        List<Bson> blockConditions = new ArrayList<>();
        for (Long blockId : blockFilters) {
            blockConditions.add(Filters.or(
                Filters.regex("extra_json", ".*\"b\"\\s*:\\s*" + blockId + "\\s*[,}].*"),
                Filters.regex("extra_json", ".*\"ob\"\\s*:\\s*" + blockId + "\\s*[,}].*")
            ));
        }

        return Filters.or(blockConditions);
    }

    private Bson buildBlockExcludeFilter(List<Long> blockFilters) {
        List<Bson> excludeConditions = new ArrayList<>();

        for (Long blockId : blockFilters) {
            excludeConditions.add(Filters.and(
                Filters.not(Filters.regex("extra_json", ".*\"b\"\\s*:\\s*" + blockId + "\\s*[,}].*")),
                Filters.not(Filters.regex("extra_json", ".*\"ob\"\\s*:\\s*" + blockId + "\\s*[,}].*"))
            ));
        }

        return excludeConditions.size() == 1 ? excludeConditions.get(0) : Filters.and(excludeConditions);
    }

}