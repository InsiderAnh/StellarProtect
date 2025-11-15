package io.github.insideranh.stellarprotect.database.types.sql;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LoggerRepositorySQL implements LoggerRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final Connection connection;

    public LoggerRepositorySQL(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void clearOldLogs() {
        Debugger.debugLog("Clearing old logs...");
        stellarProtect.getExecutor().execute(() -> {
            int daysToKeep = stellarProtect.getConfigManager().getDaysToKeepLogs();
            long expirationTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);

            try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " WHERE created_at <= ?"
            )) {

                stmt.setLong(1, expirationTime);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    Debugger.debugLog("Cleared " + deleted + " old logs from the database.");
                } else {
                    Debugger.debugLog("No old logs found to delete.");
                }
            } catch (SQLException e) {
                stellarProtect.getLogger().log(Level.SEVERE, "Failed to clear old logs.", e);
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
            try {
                TimeArg timeArg = databaseFilters.getTimeFilter();
                RadiusArg radiusArg = databaseFilters.getRadiusFilter();
                UsersArg usersArg = databaseFilters.getUserFilters();
                List<Integer> actionTypes = databaseFilters.getActionTypesFilter();

                List<String> whereConditions = new ArrayList<>();
                List<Object> parameters = new ArrayList<>();

                if (timeArg != null) {
                    whereConditions.add("created_at BETWEEN ? AND ?");
                    parameters.add(timeArg.getStart());
                    parameters.add(timeArg.getEnd());
                }

                if (radiusArg != null) {
                    whereConditions.add("x BETWEEN ? AND ?");
                    whereConditions.add("y BETWEEN ? AND ?");
                    whereConditions.add("z BETWEEN ? AND ?");
                    parameters.add(radiusArg.getMinX());
                    parameters.add(radiusArg.getMaxX());
                    parameters.add(radiusArg.getMinY());
                    parameters.add(radiusArg.getMaxY());
                    parameters.add(radiusArg.getMinZ());
                    parameters.add(radiusArg.getMaxZ());
                }

                if (usersArg != null && usersArg.getUserIds() != null && !usersArg.getUserIds().isEmpty()) {
                    String placeholders = usersArg.getUserIds().stream()
                        .map(id -> "?")
                        .collect(Collectors.joining(","));
                    whereConditions.add("player_id IN (" + placeholders + ")");
                    parameters.addAll(usersArg.getUserIds());
                }

                if (actionTypes != null && !actionTypes.isEmpty()) {
                    String placeholders = actionTypes.stream()
                        .map(type -> "?")
                        .collect(Collectors.joining(","));
                    whereConditions.add("action_type IN (" + placeholders + ")");
                    parameters.addAll(actionTypes);
                }

                String whereClause = whereConditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", whereConditions);

                String sql = "DELETE FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " " + whereClause;

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (int i = 0; i < parameters.size(); i++) {
                        Object param = parameters.get(i);
                        if (param instanceof Long) {
                            stmt.setLong(i + 1, (Long) param);
                        } else if (param instanceof Double) {
                            stmt.setDouble(i + 1, (Double) param);
                        } else if (param instanceof Integer) {
                            stmt.setInt(i + 1, (Integer) param);
                        } else {
                            stmt.setObject(i + 1, param);
                        }
                    }

                    int deleted = stmt.executeUpdate();
                    long ms = System.currentTimeMillis() - start;

                    if (deleted > 0) {
                        Debugger.debugSave("Purged " + deleted + " logs in " + ms + "ms");
                    } else {
                        Debugger.debugSave("No logs found to delete.");
                    }

                    onFinished.accept(ms);
                }
            } catch (SQLException e) {
                stellarProtect.getLogger().log(Level.SEVERE, "Failed to purge logs.", e);
            }
        });
        executor.shutdown();
    }

    @Override
    public void save(List<LogEntry> logEntries) {
        long start = System.currentTimeMillis();
        Debugger.debugSave("Saving log entries...");

        stellarProtect.getExecutor().execute(() -> {
            try {
                connection.setAutoCommit(false);

                try (PreparedStatement playerStmt = connection.prepareStatement(
                    "INSERT INTO " + stellarProtect.getConfigManager().getTablesLogEntries() + " (player_id, world_id, x, y, z, action_type, restored, extra_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )) {
                    for (LogEntry playerLog : logEntries) {
                        String extraJson = playerLog.toSaveJson();

                        playerStmt.setLong(1, playerLog.getPlayerId());
                        playerStmt.setInt(2, playerLog.getWorldId());
                        playerStmt.setDouble(3, playerLog.getX());
                        playerStmt.setDouble(4, playerLog.getY());
                        playerStmt.setDouble(5, playerLog.getZ());
                        playerStmt.setInt(6, playerLog.getActionType());
                        playerStmt.setByte(7, playerLog.getRestored());
                        playerStmt.setString(8, extraJson);
                        playerStmt.setLong(9, playerLog.getCreatedAt());
                        playerStmt.addBatch();
                    }

                    playerStmt.executeBatch();
                    connection.commit();

                } catch (Exception e) {
                    connection.rollback();
                    Debugger.debugSave("Failed to save log entries. Trying again...");
                } finally {
                    connection.setAutoCommit(true);
                }

            } catch (Exception e) {
                Debugger.debugSave("Failed to save log entries. Trying again...");
            }

            Debugger.debugSave("Saved " + logEntries.size() + " log entries in " + (System.currentTimeMillis() - start) + "ms");
        });
    }

    @Override
    public void update(List<LogEntry> logEntries) {
        long start = System.currentTimeMillis();
        Debugger.debugSave("Updating log entries...");

        stellarProtect.getExecutor().execute(() -> {
            try {
                connection.setAutoCommit(false);

                try (PreparedStatement playerStmt = connection.prepareStatement(
                    "UPDATE " + stellarProtect.getConfigManager().getTablesLogEntries() + " SET restored = ? WHERE id = ?"
                )) {
                    for (LogEntry playerLog : logEntries) {
                        playerStmt.setByte(1, playerLog.getRestored());
                        playerStmt.setLong(2, playerLog.getId());
                        playerStmt.addBatch();
                    }

                    playerStmt.executeBatch();
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    Debugger.debugSave("Failed to save log entries. Trying again...");
                } finally {
                    connection.setAutoCommit(true);
                }

            } catch (Exception e) {
                Debugger.debugSave("Failed to update log entries. Trying again...");
            }

            Debugger.debugSave("Updated " + logEntries.size() + " log entries in " + (System.currentTimeMillis() - start) + "ms");
        });
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
    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getLogs(@NonNull DatabaseFilters databaseFilters, boolean ignoreCache, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LogEntry> cachedLogs = ignoreCache ? Collections.emptyList() : LoggerCache.getLogs(databaseFilters, skip, limit)
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

            long totalCount = countLogsFromDB(location, null);

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
            }

            Debugger.debugLog("getLogs: cached=" + cachedLogs.size() + ", total=" + totalCount + ", result=" + result.size());

            return new CallbackLookup<>(result, totalCount);
        }, stellarProtect.getLookupExecutor());
    }

    private long countLogsFromDB(Location location, @Nullable ActionType actionType) {
        String actionTypeFilter = actionType != null ? " AND ple.action_type = ?;" : ";";

        String countQuery = "SELECT COUNT(*) FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " ple " +
            "WHERE ple.created_at BETWEEN ? AND ? " +
            "AND ple.x = ? AND ple.y = ? AND ple.z = ?" + actionTypeFilter;

        long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        long endTime = System.currentTimeMillis();

        try (PreparedStatement countStmt = connection.prepareStatement(countQuery)) {
            countStmt.setLong(1, startTime);
            countStmt.setLong(2, endTime);
            countStmt.setDouble(3, location.getBlockX());
            countStmt.setDouble(4, location.getBlockY());
            countStmt.setDouble(5, location.getBlockZ());

            if (actionType != null) {
                countStmt.setInt(6, actionType.getId());
            }

            try (ResultSet resultSet = countStmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in countLogsFromDB", e);
        }

        return 0;
    }

    private CallbackLookup<Set<LogEntry>, Long> queryLogsFromDB(Location location, int skip, int limit, @Nullable ActionType actionType) {
        Set<LogEntry> logs = new LinkedHashSet<>();

        String actionTypeFilter = actionType != null ? "AND ple.action_type = ? " : "";

        String dataQuery =
            "SELECT ple.*, p.name, p.uuid " +
                "FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " ple " +
                "JOIN " + stellarProtect.getConfigManager().getTablesPlayers() + " p ON ple.player_id = p.id " +
                "WHERE ple.created_at BETWEEN ? AND ? " +
                "AND ple.x = ? AND ple.y = ? AND ple.z = ? " + actionTypeFilter +
                "ORDER BY ple.created_at DESC " +
                "LIMIT ? OFFSET ?";

        long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        long endTime = System.currentTimeMillis();

        try (PreparedStatement dataStmt = connection.prepareStatement(dataQuery)) {
            dataStmt.setLong(1, startTime);
            dataStmt.setLong(2, endTime);
            dataStmt.setDouble(3, location.getBlockX());
            dataStmt.setDouble(4, location.getBlockY());
            dataStmt.setDouble(5, location.getBlockZ());

            if (actionType != null) {
                dataStmt.setInt(6, actionType.getId());
                dataStmt.setInt(7, limit);
                dataStmt.setInt(8, skip);
            } else {
                dataStmt.setInt(6, limit);
                dataStmt.setInt(7, skip);
            }

            try (ResultSet rs = dataStmt.executeQuery()) {
                while (rs.next()) {
                    long playerId = rs.getLong("player_id");
                    String playerName = rs.getString("name");
                    PlayerCache.cacheName(playerId, playerName);

                    logs.add(LogEntryFactory.fromDatabase(rs));
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in queryLogsFromDB", e);
        }

        Debugger.debugLog("Loaded " + logs.size() + " logs from database.");

        return new CallbackLookup<>(logs, 0L);
    }

    private CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> queryLogsFromDB(DatabaseFilters databaseFilters, int skip, int limit) {
        Set<LogEntry> logs;

        QueryBuilder queryBuilder = buildBaseQuery(databaseFilters);

        String dataQuery = "SELECT ple.*, p.name, p.uuid " + queryBuilder.getDataQuery() +
            " ORDER BY ple.created_at DESC LIMIT ? OFFSET ?";

        String countQuery = "SELECT COUNT(*) " + queryBuilder.getCountQuery();

        long totalCount = executeCountQuery(countQuery, queryBuilder.getParameters());

        logs = executeDataQuery(dataQuery, queryBuilder.getParameters(), limit, skip);

        Debugger.debugLog("Loaded " + logs.size() + " logs from database. Total count: " + totalCount);

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
        QueryBuilder queryBuilder = buildBaseQuery(databaseFilters);
        String countQuery = "SELECT COUNT(*) " + queryBuilder.getCountQuery();
        return executeCountQuery(countQuery, queryBuilder.getParameters());
    }

    private QueryBuilder buildBaseQuery(DatabaseFilters databaseFilters) {
        QueryBuilder queryBuilder = new QueryBuilder(
            stellarProtect.getConfigManager().getTablesLogEntries(),
            stellarProtect.getConfigManager().getTablesPlayers()
        )
            .addTimeFilter(databaseFilters.getTimeFilter())
            .addRadiusFilter(databaseFilters.getRadiusFilter())
            .addUsersFilter(databaseFilters.getUserFilters());

        queryBuilder.addCombinedIncludeFilters(
            databaseFilters.getAllIncludeFilters(),
            databaseFilters.getIncludeMaterialFilters(),
            databaseFilters.getIncludeBlockFilters()
        );

        queryBuilder.addCombinedExcludeFilters(
            databaseFilters.getAllExcludeFilters(),
            databaseFilters.getExcludeMaterialFilters(),
            databaseFilters.getExcludeBlockFilters()
        );

        return queryBuilder.addActionTypesFilter(databaseFilters.getActionTypesFilter());
    }

    private long executeCountQuery(String countQuery, List<Object> parameters) {
        try (PreparedStatement countStmt = connection.prepareStatement(countQuery)) {
            setParameters(countStmt, parameters);

            try (ResultSet resultSet = countStmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in count query", e);
        }
        return 0;
    }

    private Set<LogEntry> executeDataQuery(String dataQuery, List<Object> parameters, int limit, int skip) {
        Set<LogEntry> logs = new LinkedHashSet<>();

        try (PreparedStatement dataStmt = connection.prepareStatement(dataQuery)) {
            List<Object> allParams = new ArrayList<>(parameters);
            allParams.add(limit);
            allParams.add(skip);

            setParameters(dataStmt, allParams);

            try (ResultSet rs = dataStmt.executeQuery()) {
                while (rs.next()) {
                    long playerId = rs.getLong("player_id");
                    String playerName = rs.getString("name");

                    PlayerCache.cacheName(playerId, playerName);
                    logs.add(LogEntryFactory.fromDatabase(rs));
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in data query", e);
        }

        return logs;
    }

    private void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof Double) {
                stmt.setDouble(i + 1, (Double) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    private static class QueryBuilder {

        private final List<String> whereConditions = new ArrayList<>();
        private final List<Object> parameters = new ArrayList<>();
        private final String tablesLogEntries;
        private final String tablesPlayers;

        public QueryBuilder(String tablesLogEntries, String tablesPlayers) {
            this.tablesLogEntries = tablesLogEntries;
            this.tablesPlayers = tablesPlayers;
        }

        public QueryBuilder addTimeFilter(TimeArg timeArg) {
            if (timeArg != null) {
                whereConditions.add("ple.created_at BETWEEN ? AND ?");
                parameters.add(timeArg.getStart());
                parameters.add(timeArg.getEnd());
            }
            return this;
        }

        public QueryBuilder addRadiusFilter(RadiusArg radiusArg) {
            if (radiusArg != null) {
                if (radiusArg.getWorldId() != -1) {
                    whereConditions.add("ple.world_id = ?");
                    parameters.add(radiusArg.getWorldId());
                }
                whereConditions.add("ple.x >= ? AND ple.x <= ?");
                whereConditions.add("ple.y >= ? AND ple.y <= ?");
                whereConditions.add("ple.z >= ? AND ple.z <= ?");

                parameters.add(radiusArg.getMinX());
                parameters.add(radiusArg.getMaxX());
                parameters.add(radiusArg.getMinY());
                parameters.add(radiusArg.getMaxY());
                parameters.add(radiusArg.getMinZ());
                parameters.add(radiusArg.getMaxZ());
            }
            return this;
        }

        public QueryBuilder addCombinedIncludeFilters(List<Long> allIncludeFilters, List<Long> materialFilters, List<Long> blockFilters) {
            List<String> allIncludeConditions = new ArrayList<>();

            if (allIncludeFilters != null && !allIncludeFilters.isEmpty()) {
                for (Long wordId : allIncludeFilters) {
                    List<String> worldConditions = new ArrayList<>();

                    worldConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"id\":" + wordId + ",%");

                    worldConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"ai\":{%\"" + wordId + "\":%");

                    worldConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"ri\":{%\"" + wordId + "\":%");

                    allIncludeConditions.add("(" + String.join(" OR ", worldConditions) + ")");
                }
            }

            if (materialFilters != null && !materialFilters.isEmpty()) {
                for (Long wordId : materialFilters) {
                    List<String> worldConditions = new ArrayList<>();

                    worldConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"id\":" + wordId + ",%");

                    worldConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"ai\":{%\"" + wordId + "\":%");

                    worldConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"ri\":{%\"" + wordId + "\":%");

                    allIncludeConditions.add("(" + String.join(" OR ", worldConditions) + ")");
                }
            }

            if (blockFilters != null && !blockFilters.isEmpty()) {
                for (Long blockId : blockFilters) {
                    List<String> blockConditions = new ArrayList<>();

                    blockConditions.add("ple.extra_json = ?");
                    parameters.add("{\"b\":" + blockId + "}");

                    blockConditions.add("ple.extra_json LIKE ?");
                    parameters.add("{\"b\":" + blockId + ",\"ob\":%}");

                    blockConditions.add("ple.extra_json LIKE ?");
                    parameters.add("{\"b\":%,\"ob\":" + blockId + "}");

                    blockConditions.add("ple.extra_json LIKE ?");
                    parameters.add("{\"nb\":\"" + blockId + "\",\"lb\":\"%\"}");

                    blockConditions.add("ple.extra_json LIKE ?");
                    parameters.add("{\"nb\":\"%\",\"lb\":\"" + blockId + "\"}");

                    allIncludeConditions.add("(" + String.join(" OR ", blockConditions) + ")");
                }
            }

            if (!allIncludeConditions.isEmpty()) {
                whereConditions.add("(" + String.join(" OR ", allIncludeConditions) + ")");
            }

            return this;
        }

        public QueryBuilder addCombinedExcludeFilters(List<Long> allExcludeFilters, List<Long> materialFilters, List<Long> blockFilters) {
            List<String> allExcludeConditions = new ArrayList<>();

            if (allExcludeFilters != null && !allExcludeFilters.isEmpty()) {
                for (Long materialId : allExcludeFilters) {
                    List<String> worldExcludeConditions = new ArrayList<>();

                    worldExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"id\":" + materialId + ",%");

                    worldExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"ai\":{%\"" + materialId + "\":%");

                    worldExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"ri\":{%\"" + materialId + "\":%");

                    allExcludeConditions.add("(" + String.join(" AND ", worldExcludeConditions) + ")");
                }
            }

            if (materialFilters != null && !materialFilters.isEmpty()) {
                for (Long materialId : materialFilters) {
                    List<String> worldExcludeConditions = new ArrayList<>();

                    worldExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"id\":" + materialId + ",%");

                    worldExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"ai\":{%\"" + materialId + "\":%");

                    worldExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"ri\":{%\"" + materialId + "\":%");

                    allExcludeConditions.add("(" + String.join(" AND ", worldExcludeConditions) + ")");
                }
            }

            if (blockFilters != null && !blockFilters.isEmpty()) {
                for (Long blockId : blockFilters) {
                    List<String> blockExcludeConditions = new ArrayList<>();

                    blockExcludeConditions.add("ple.extra_json != ?");
                    parameters.add("{\"b\":" + blockId + "}");

                    blockExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("{\"b\":" + blockId + ",\"ob\":%}");

                    blockExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("{\"b\":%,\"ob\":" + blockId + "}");

                    blockExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("{\"nb\":\"" + blockId + "\",\"lb\":\"%\"}");

                    blockExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("{\"nb\":\"%\",\"lb\":\"" + blockId + "\"}");

                    allExcludeConditions.add("(" + String.join(" AND ", blockExcludeConditions) + ")");
                }
            }

            if (!allExcludeConditions.isEmpty()) {
                whereConditions.add("(" + String.join(" AND ", allExcludeConditions) + ")");
            }

            return this;
        }

        public QueryBuilder addUsersFilter(UsersArg usersArg) {
            if (usersArg != null && usersArg.getUserIds() != null && !usersArg.getUserIds().isEmpty()) {
                String placeholders = usersArg.getUserIds().stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));
                whereConditions.add("ple.player_id IN (" + placeholders + ")");
                parameters.addAll(usersArg.getUserIds());
            }
            return this;
        }

        public QueryBuilder addActionTypesFilter(List<Integer> actionTypes) {
            if (actionTypes != null && !actionTypes.isEmpty()) {
                String placeholders = actionTypes.stream()
                    .map(type -> "?")
                    .collect(Collectors.joining(","));
                whereConditions.add("ple.action_type IN (" + placeholders + ")");
                parameters.addAll(actionTypes);
            }
            return this;
        }

        public String getCountQuery() {
            String whereClause = whereConditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", whereConditions);
            return "FROM " + tablesLogEntries + " ple " + whereClause;
        }

        public String getDataQuery() {
            String whereClause = whereConditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", whereConditions);
            return "FROM " + tablesLogEntries + " ple " +
                "JOIN " + tablesPlayers + " p ON ple.player_id = p.id" + whereClause;
        }

        public List<Object> getParameters() {
            return new ArrayList<>(parameters);
        }
    }

}