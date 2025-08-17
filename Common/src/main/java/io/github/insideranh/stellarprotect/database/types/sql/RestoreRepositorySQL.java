package io.github.insideranh.stellarprotect.database.types.sql;

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
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RestoreRepositorySQL implements RestoreRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final Connection connection;

    public RestoreRepositorySQL(Connection connection) {
        this.connection = connection;
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

    private long countLogsFromDB(@NonNull TimeArg timeArg, @NonNull RadiusArg radiusArg, List<ActionType> actionTypes) {
        String actionPlaceholders = actionTypes.stream()
            .map(action -> "?")
            .collect(Collectors.joining(","));

        String countQuery =
            "SELECT COUNT(*) FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " " +
                "WHERE created_at BETWEEN ? AND ? " +
                "AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ? " +
                "AND action_type IN (" + actionPlaceholders + ")";

        long totalCount = 0;
        try (PreparedStatement countStmt = connection.prepareStatement(countQuery)) {
            int paramIndex = 1;

            countStmt.setLong(paramIndex++, timeArg.getStart());
            countStmt.setLong(paramIndex++, timeArg.getEnd());
            countStmt.setDouble(paramIndex++, radiusArg.getMinX());
            countStmt.setDouble(paramIndex++, radiusArg.getMaxX());
            countStmt.setDouble(paramIndex++, radiusArg.getMinY());
            countStmt.setDouble(paramIndex++, radiusArg.getMaxY());
            countStmt.setDouble(paramIndex++, radiusArg.getMinZ());
            countStmt.setDouble(paramIndex++, radiusArg.getMaxZ());

            for (ActionType actionType : actionTypes) {
                countStmt.setInt(paramIndex++, actionType.getId());
            }

            try (ResultSet resultSet = countStmt.executeQuery()) {
                if (resultSet.next()) {
                    totalCount = resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in count", e);
        }
        return totalCount;
    }

    private CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> queryLogsFromDB(
        @NonNull TimeArg timeArg,
        @NonNull RadiusArg radiusArg,
        @NonNull List<ActionType> actionTypes,
        int skip,
        int limit) {

        Set<LogEntry> logs = new LinkedHashSet<>();
        long totalCount = 0;

        String actionPlaceholders = actionTypes.stream()
            .map(action -> "?")
            .collect(Collectors.joining(","));

        String dataQuery =
            "SELECT ple.*, p.name, p.uuid " +
                "FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " ple " +
                "JOIN " + stellarProtect.getConfigManager().getTablesPlayers() + " p ON ple.player_id = p.id " +
                "WHERE ple.created_at BETWEEN ? AND ? " +
                "AND ple.x BETWEEN ? AND ? AND ple.y BETWEEN ? AND ? AND ple.z BETWEEN ? AND ? " +
                "AND ple.action_type IN (" + actionPlaceholders + ") " +
                "ORDER BY ple.created_at DESC " +
                "LIMIT ? OFFSET ?";

        String countQuery =
            "SELECT COUNT(*) FROM " + stellarProtect.getConfigManager().getTablesLogEntries() + " " +
                "WHERE created_at BETWEEN ? AND ? " +
                "AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ? " +
                "AND action_type IN (" + actionPlaceholders + ")";

        try (PreparedStatement countStmt = connection.prepareStatement(countQuery)) {
            int paramIndex = 1;

            countStmt.setLong(paramIndex++, timeArg.getStart());
            countStmt.setLong(paramIndex++, timeArg.getEnd());
            countStmt.setDouble(paramIndex++, radiusArg.getMinX());
            countStmt.setDouble(paramIndex++, radiusArg.getMaxX());
            countStmt.setDouble(paramIndex++, radiusArg.getMinY());
            countStmt.setDouble(paramIndex++, radiusArg.getMaxY());
            countStmt.setDouble(paramIndex++, radiusArg.getMinZ());
            countStmt.setDouble(paramIndex++, radiusArg.getMaxZ());

            for (ActionType actionType : actionTypes) {
                countStmt.setInt(paramIndex++, actionType.getId());
            }

            try (ResultSet resultSet = countStmt.executeQuery()) {
                if (resultSet.next()) {
                    totalCount = resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in count", e);
        }

        try (PreparedStatement dataStmt = connection.prepareStatement(dataQuery)) {
            int paramIndex = 1;

            dataStmt.setLong(paramIndex++, timeArg.getStart());
            dataStmt.setLong(paramIndex++, timeArg.getEnd());
            dataStmt.setDouble(paramIndex++, radiusArg.getMinX());
            dataStmt.setDouble(paramIndex++, radiusArg.getMaxX());
            dataStmt.setDouble(paramIndex++, radiusArg.getMinY());
            dataStmt.setDouble(paramIndex++, radiusArg.getMaxY());
            dataStmt.setDouble(paramIndex++, radiusArg.getMinZ());
            dataStmt.setDouble(paramIndex++, radiusArg.getMaxZ());

            for (ActionType actionType : actionTypes) {
                dataStmt.setInt(paramIndex++, actionType.getId());
            }

            dataStmt.setInt(paramIndex++, limit);
            dataStmt.setInt(paramIndex++, skip);

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

        Map<LocationCache, Set<LogEntry>> groupedLogs = logs.stream()
            .collect(Collectors.groupingBy(LocationCache::of, LinkedHashMap::new,
                Collectors.toCollection(LinkedHashSet::new)));

        return new CallbackLookup<>(groupedLogs, totalCount);
    }

}