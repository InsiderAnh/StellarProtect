package io.github.insideranh.stellarprotect.database.types.mongo;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.Nullable;
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
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.database.repositories.LoggerRepository;
import io.github.insideranh.stellarprotect.database.types.factory.LogEntryFactory;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import lombok.NonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LoggerRepositoryMongo implements LoggerRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final MongoDatabase database;
    private final MongoCollection<Document> players;
    private final MongoCollection<Document> logEntries;

    public LoggerRepositoryMongo(MongoDatabase database) {
        this.database = database;
        this.players = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesPlayers());
        this.logEntries = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesLogEntries());
    }

    @Override
    public void clearOldLogs() {
        Debugger.debugLog("Clearing old logs...");
        stellarProtect.getExecutor().execute(() -> {
            int daysToKeep = stellarProtect.getConfigManager().getDaysToKeepLogs();
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);

            Bson filter = Filters.lt("created_at", cutoffTime);
            DeleteResult result = this.logEntries.deleteMany(filter);

            Debugger.debugLog("Cleared " + result.getDeletedCount() + " logs from database.");
        });
    }

    @Override
    public void save(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) return;

        long start = System.currentTimeMillis();
        Debugger.debugSave("Saving log entries...");

        stellarProtect.getExecutor().execute(() -> {
            try {
                List<WriteModel<Document>> playerOps = new ArrayList<>(logEntries.size());

                for (LogEntry logEntry : logEntries) {
                    String extraJson = logEntry.toSaveJson();
                    Document doc = new Document("world_id", logEntry.getWorldId())
                        .append("player_id", logEntry.getPlayerId())
                        .append("x", logEntry.getX())
                        .append("y", logEntry.getY())
                        .append("z", logEntry.getZ())
                        .append("action_type", logEntry.getActionType())
                        .append("extra_json", extraJson)
                        .append("created_at", logEntry.getCreatedAt());

                    playerOps.add(new InsertOneModel<>(doc));
                }

                BulkWriteOptions options = new BulkWriteOptions().ordered(false).bypassDocumentValidation(true);
                BulkWriteResult result = this.logEntries.bulkWrite(playerOps, options);

                Debugger.debugSave("Saved " + result.getInsertedCount() + " log entries in " + (System.currentTimeMillis() - start) + "ms");
            } catch (Exception e) {
                stellarProtect.getLogger().log(Level.SEVERE, "Failed to save log entries to MongoDB", e);
            }
        });
    }

    @Override
    public void purgeLogs(@NonNull DatabaseFilters databaseFilters, Consumer<Long> onFinished) {
        long start = System.currentTimeMillis();
        Debugger.debugSave("Purging logs...");

        ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024))
        );

        executor.execute(() -> {
            TimeArg timeArg = databaseFilters.getTimeFilter();
            RadiusArg radiusArg = databaseFilters.getRadiusFilter();
            UsersArg usersArg = databaseFilters.getUserFilters();
            List<Integer> actionTypes = databaseFilters.getActionTypesFilter();

            List<Bson> filters = new ArrayList<>();

            if (timeArg != null) {
                filters.add(Filters.gte("created_at", timeArg.getStart()));
                filters.add(Filters.lte("created_at", timeArg.getEnd()));
            }

            if (radiusArg != null) {
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

            if (filters.isEmpty()) {
                Debugger.debugSave("No filters provided for purge operation. Skipping for safety.");
                return;
            }

            Bson filter = Filters.and(filters);

            DeleteResult result = this.logEntries.deleteMany(filter);

            long ms = System.currentTimeMillis() - start;
            if (result.getDeletedCount() > 0) {
                Debugger.debugSave("Purged " + result.getDeletedCount() + " logs in " + ms + "ms");
            } else {
                Debugger.debugSave("No logs found to delete.");
            }

            onFinished.accept(ms);
        });
        executor.shutdown();
    }

    @Override
    public CompletableFuture<CallbackLookup<List<ItemLogEntry>, Long>> getChestTransactions(@NonNull Location location, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<ItemLogEntry> cachedLogs = LoggerCache.getChestTransactions(location, 0, Integer.MAX_VALUE)
                .stream()
                .filter(log -> System.currentTimeMillis() - log.getCreatedAt() <= TimeUnit.MINUTES.toMillis(15))
                .sorted(Comparator.comparingLong(ItemLogEntry::getCreatedAt).reversed())
                .collect(Collectors.toList());

            List<ItemLogEntry> paginatedCachedLogs = cachedLogs.stream()
                .skip(skip)
                .limit(limit)
                .collect(Collectors.toList());

            List<ItemLogEntry> result = new LinkedList<>(paginatedCachedLogs);
            int remaining = limit - paginatedCachedLogs.size();

            if (remaining > 0) {
                int effectiveSkipForDB = Math.max(0, skip - cachedLogs.size());
                int processedItemFromDB = 0;

                Set<String> cachedItemKeys = cachedLogs.stream()
                    .map(ItemLogEntry::getItemStack)
                    .filter(Objects::nonNull)
                    .map(this::createItemKey)
                    .collect(Collectors.toSet());

                int addedFromDB = 0;
                int blockStart = 0;
                int blockSize = Math.max(10, remaining);
                long totalDBLogs = 0;
                boolean hasMoreBlocks = true;

                while (addedFromDB < remaining && hasMoreBlocks) {
                    CallbackLookup<Set<LogEntry>, Long> dbLookup = queryLogsFromDB(location, blockStart, blockSize, ActionType.INVENTORY_TRANSACTION);

                    List<LogEntry> dbLogs = dbLookup.getLogs().stream()
                        .filter(PlayerTransactionEntry.class::isInstance)
                        .sorted(Comparator.comparingLong(LogEntry::getCreatedAt).reversed())
                        .collect(Collectors.toList());

                    if (dbLogs.isEmpty()) {
                        break;
                    }

                    int itemsAddedInThisBlock = 0;

                    for (LogEntry log : dbLogs) {
                        PlayerTransactionEntry transaction = (PlayerTransactionEntry) log;
                        if (addedFromDB >= remaining) {
                            totalDBLogs += transaction.getAdded().size() + transaction.getRemoved().size();
                            break;
                        }

                        totalDBLogs += transaction.getAdded().size() + transaction.getRemoved().size();

                        for (Map.Entry<Long, Integer> addedEntry : transaction.getAdded().entrySet()) {
                            processedItemFromDB++;
                            if (processedItemFromDB <= effectiveSkipForDB) {
                                continue;
                            }

                            if (addedFromDB >= remaining) break;

                            ItemTemplate itemTemplate = stellarProtect.getItemsManager().getItemTemplate(addedEntry.getKey());
                            ItemStack item = itemTemplate.getBukkitItem();
                            if (item == null) continue;

                            String itemKey = createItemKey(item);
                            if (cachedItemKeys.contains(itemKey)) continue;

                            result.add(new ItemLogEntry(item, log.getPlayerId(), addedEntry.getValue(), true, log.getCreatedAt()));
                            addedFromDB++;
                            itemsAddedInThisBlock++;
                        }

                        for (Map.Entry<Long, Integer> removedEntry : transaction.getRemoved().entrySet()) {
                            processedItemFromDB++;
                            if (processedItemFromDB <= effectiveSkipForDB) {
                                continue;
                            }

                            if (addedFromDB >= remaining) break;

                            ItemTemplate itemTemplate = stellarProtect.getItemsManager().getItemTemplate(removedEntry.getKey());
                            ItemStack item = itemTemplate.getBukkitItem();
                            if (item == null) continue;

                            String itemKey = createItemKey(item);
                            if (cachedItemKeys.contains(itemKey)) continue;

                            result.add(new ItemLogEntry(item, log.getPlayerId(), removedEntry.getValue(), false, log.getCreatedAt()));
                            addedFromDB++;
                            itemsAddedInThisBlock++;
                        }
                    }

                    if (itemsAddedInThisBlock == 0 && addedFromDB < remaining) {
                        blockStart += blockSize;
                        if (blockStart + blockSize >= totalDBLogs) {
                            hasMoreBlocks = false;
                        }
                    } else if (addedFromDB < remaining && dbLogs.size() == blockSize) {
                        blockStart += blockSize;
                        if (blockStart >= totalDBLogs) {
                            hasMoreBlocks = false;
                        }
                    } else {
                        hasMoreBlocks = false;
                    }
                }

                long estimatedTotalCount = cachedLogs.size() + totalDBLogs;

                return new CallbackLookup<>(result, estimatedTotalCount);
            }

            return new CallbackLookup<>(result, (long) cachedLogs.size());
        }, stellarProtect.getLookupExecutor());
    }

    private String createItemKey(ItemStack item) {
        return item.getType() + ":" + item.getDurability() + ":" + (item.hasItemMeta() ? item.getItemMeta().toString() : "null");
    }

    @Override
    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getLogs(@NonNull DatabaseFilters databaseFilters, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LogEntry> cachedLogs = LoggerCache.getLogs(databaseFilters, skip, limit)
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

                CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> dbLookup = queryLogsFromDB(databaseFilters, dbSkip, remaining);

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

            long total = countLogs(databaseFilters);
            return new CallbackLookup<>(groupedResults, total);
        }, stellarProtect.getLookupExecutor());
    }

    @Override
    public CompletableFuture<CallbackLookup<Set<LogEntry>, Long>> getLogs(@NonNull Location location, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LogEntry> cachedLogs = LoggerCache.getLogs(LocationCache.of(location), skip, limit)
                .stream()
                .filter(log -> System.currentTimeMillis() - log.getCreatedAt() <= TimeUnit.MINUTES.toMillis(15))
                .sorted(Comparator.comparingLong(LogEntry::getCreatedAt).reversed())
                .collect(Collectors.toList());

            Set<LogEntry> result = new LinkedHashSet<>(cachedLogs);

            int remaining = limit - cachedLogs.size();

            if (remaining > 0) {
                int dbSkip = skip + cachedLogs.size();

                CallbackLookup<Set<LogEntry>, Long> dbLookup = queryLogsFromDB(location, dbSkip, remaining, null);

                List<LogEntry> dbLogs = dbLookup.getLogs().stream()
                    .filter(log -> cachedLogs.stream().noneMatch(c -> c.equals(log)))
                    .sorted(Comparator.comparingLong(LogEntry::getCreatedAt).reversed())
                    .limit(remaining)
                    .collect(Collectors.toList());

                result.addAll(dbLogs);

                return new CallbackLookup<>(result, dbLookup.getTotal());
            }

            return new CallbackLookup<>(result, (long) skip + cachedLogs.size());
        }, stellarProtect.getLookupExecutor());
    }

    private CallbackLookup<Set<LogEntry>, Long> queryLogsFromDB(Location location, int skip, int limit, @Nullable ActionType actionType) {
        Set<LogEntry> logs = new LinkedHashSet<>();
        MongoCollection<Document> logCollection = logEntries;

        long now = System.currentTimeMillis();
        long startTime = now - TimeUnit.DAYS.toMillis(30);

        Bson filter = Filters.and(
            Filters.gte("created_at", startTime),
            Filters.lte("created_at", now),
            Filters.eq("x", location.getBlockX()),
            Filters.eq("y", location.getBlockY()),
            Filters.eq("z", location.getBlockZ())
        );
        if (actionType != null) {
            filter = Filters.and(filter, Filters.eq("action_type", actionType.getId()));
        }

        long total = logCollection.countDocuments(filter);

        FindIterable<Document> logDocs = logCollection.find(filter)
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
                Debugger.debugLog("Error al convertir LogEntry: " + doc.toJson());
            }
        }

        return new CallbackLookup<>(logs, total);
    }

    private CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> queryLogsFromDB(DatabaseFilters databaseFilters, int skip, int limit) {
        Bson filter = buildFilters(databaseFilters);
        long totalCount = countLogs(databaseFilters);
        Set<LogEntry> logs = executeLogQuery(filter, skip, limit);

        Debugger.debugLog("Loaded " + logs.size() + " logs from MongoDB. Total count: " + totalCount);

        Map<LocationCache, Set<LogEntry>> groupedLogs = logs.stream().collect(
            Collectors.groupingBy(
                LocationCache::of,
                LinkedHashMap::new,
                Collectors.toCollection(LinkedHashSet::new)
            )
        );

        return new CallbackLookup<>(groupedLogs, totalCount);
    }

    public long countLogs(DatabaseFilters databaseFilters) {
        Bson filter = buildFilters(databaseFilters);

        return countLogsWithPlayerValidation(filter);
    }

    private Bson buildFilters(DatabaseFilters databaseFilters) {
        TimeArg timeArg = databaseFilters.getTimeFilter();
        RadiusArg radiusArg = databaseFilters.getRadiusFilter();
        UsersArg usersArg = databaseFilters.getUserFilters();
        List<Integer> actionTypes = databaseFilters.getActionTypesFilter();

        List<Bson> filters = new ArrayList<>();

        if (timeArg != null) {
            filters.add(Filters.gte("created_at", timeArg.getStart()));
            filters.add(Filters.lte("created_at", timeArg.getEnd()));
        }

        if (radiusArg != null) {
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

        return filters.isEmpty() ? new Document() : Filters.and(filters);
    }

    private Set<LogEntry> executeLogQuery(Bson filter, int skip, int limit) {
        Set<LogEntry> logs = new LinkedHashSet<>();

        FindIterable<Document> logDocs = logEntries.find(filter)
            .sort(Sorts.descending("created_at"))
            .skip(skip)
            .limit(limit);

        for (Document doc : logDocs) {
            try {
                long playerId = doc.getLong("player_id");

                Document playerDoc = players.find(Filters.eq("id", playerId)).first();
                if (playerDoc != null) {
                    String playerName = playerDoc.getString("name");
                    PlayerCache.cacheName(playerId, playerName);
                } else {
                    Debugger.debugLog("Warning: Log entry has player_id " + playerId + " but player not found");
                }

                LogEntry entry = LogEntryFactory.fromDocument(doc);
                logs.add(entry);

            } catch (Exception e) {
                Debugger.debugLog("Error loading log: " + doc.toJson() + " - " + e.getMessage());
            }
        }

        return logs;
    }

    private long countLogsWithPlayerValidation(Bson filter) {
        try {
            List<Bson> pipeline = Arrays.asList(
                Aggregates.match(filter),
                Aggregates.lookup("players", "player_id", "id", "player_info"),
                Aggregates.match(Filters.ne("player_info", Collections.emptyList())), // Solo logs con player
                Aggregates.count("total")
            );

            AggregateIterable<Document> result = logEntries.aggregate(pipeline);
            Document countDoc = result.first();

            return countDoc != null ? countDoc.getInteger("total", 0) : 0;
        } catch (Exception e) {
            Debugger.debugLog("Error in aggregation count, falling back to simple count: " + e.getMessage());
            return logEntries.countDocuments(filter);
        }
    }

}