package io.github.insideranh.stellarprotect.database.types.mysql;

import com.zaxxer.hikari.HikariDataSource;
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
import lombok.NonNull;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RestoreRepositoryMySQL implements RestoreRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final HikariDataSource dataSource;

    public RestoreRepositoryMySQL(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public CompletableFuture<CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long>> getRestoreActions(@NonNull DatabaseFilters filters, int skip, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            TimeArg timeArg = filters.getTimeFilter();
            RadiusArg radiusArg = filters.getRadiusFilter();
            List<Integer> actionTypes = filters.getActionTypesFilter();

            // Convertir IDs de actionTypes a objetos ActionType para cache
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

            // Convertir IDs de actionTypes a objetos ActionType para cache
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

    private long countLogsFromDB(@NonNull DatabaseFilters filters) {
        QueryBuilder queryBuilder = buildBaseQuery(filters);
        String countQuery = "SELECT COUNT(*) " + queryBuilder.getCountQuery();
        List<Object> parameters = queryBuilder.getParameters();

        long totalCount = 0;
        try (Connection connection = getConnection()) {
            totalCount = executeCountQuery(connection, countQuery, parameters);
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in count", e);
        }
        return totalCount;
    }

    private CallbackLookup<Map<LocationCache, Set<LogEntry>>, Long> queryLogsFromDB(
        @NonNull DatabaseFilters filters,
        int skip,
        int limit) {

        Set<LogEntry> logs = new LinkedHashSet<>();
        long totalCount = 0;

        QueryBuilder queryBuilder = buildBaseQuery(filters);
        String countQuery = "SELECT COUNT(*) " + queryBuilder.getCountQuery();
        String dataQuery = "SELECT ple.*, p.name, p.uuid " + queryBuilder.getDataQuery() + " ORDER BY ple.created_at DESC LIMIT ? OFFSET ?";
        List<Object> parameters = queryBuilder.getParameters();

        try (Connection connection = getConnection()) {
            totalCount = executeCountQuery(connection, countQuery, parameters);
            logs = executeDataQuery(connection, dataQuery, parameters, limit, skip);
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in query", e);
        }

        Map<LocationCache, Set<LogEntry>> groupedLogs = logs.stream()
            .collect(Collectors.groupingBy(
                LocationCache::of,
                LinkedHashMap::new,
                Collectors.toCollection(LinkedHashSet::new)
            ));

        return new CallbackLookup<>(groupedLogs, totalCount);
    }

    private QueryBuilder buildBaseQuery(DatabaseFilters databaseFilters) {
        return new QueryBuilder(
            stellarProtect.getConfigManager().getTablesLogEntries(),
            stellarProtect.getConfigManager().getTablesPlayers()
        )
            .addTimeFilter(databaseFilters.getTimeFilter())
            .addRadiusFilter(databaseFilters.getRadiusFilter())
            .addUsersFilter(databaseFilters.getUserFilters())
            .allIncludeFilters(databaseFilters.getAllIncludeFilters())
            .allExcludeFilters(databaseFilters.getAllExcludeFilters())
            .allIncludeFilters(databaseFilters.getIncludeMaterialFilters())
            .allExcludeFilters(databaseFilters.getExcludeMaterialFilters())
            .addActionTypesFilter(databaseFilters.getActionTypesFilter());
    }

    private long executeCountQuery(Connection connection, String countQuery, List<Object> parameters) throws SQLException {
        try (PreparedStatement countStmt = connection.prepareStatement(countQuery)) {
            setParameters(countStmt, parameters);

            try (ResultSet resultSet = countStmt.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return 0;
    }

    private Set<LogEntry> executeDataQuery(Connection connection, String dataQuery, List<Object> parameters, int limit, int skip) throws SQLException {
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

    @SneakyThrows
    private Connection getConnection() {
        return dataSource.getConnection();
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
                }
                whereConditions.add("ple.x BETWEEN ? AND ?");
                whereConditions.add("ple.y BETWEEN ? AND ?");
                whereConditions.add("ple.z BETWEEN ? AND ?");
                if (radiusArg.getWorldId() != -1) {
                    parameters.add(radiusArg.getWorldId());
                }
                parameters.add(radiusArg.getMinX());
                parameters.add(radiusArg.getMaxX());
                parameters.add(radiusArg.getMinY());
                parameters.add(radiusArg.getMaxY());
                parameters.add(radiusArg.getMinZ());
                parameters.add(radiusArg.getMaxZ());
            }
            return this;
        }

        public QueryBuilder allIncludeFilters(List<Long> materialFilters) {
            if (materialFilters != null && !materialFilters.isEmpty()) {
                List<String> jsonConditions = new ArrayList<>();

                for (Long itemId : materialFilters) {
                    List<String> itemConditions = new ArrayList<>();

                    itemConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"id\":" + itemId + ",%");

                    itemConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"ai\":{%\"" + itemId + "\":%");

                    itemConditions.add("ple.extra_json LIKE ?");
                    parameters.add("%\"ri\":{%\"" + itemId + "\":%");

                    jsonConditions.add("(" + String.join(" OR ", itemConditions) + ")");
                }

                whereConditions.add("(" + String.join(" OR ", jsonConditions) + ")");
            }
            return this;
        }

        public QueryBuilder allExcludeFilters(List<Long> materialFilters) {
            if (materialFilters != null && !materialFilters.isEmpty()) {
                List<String> excludeConditions = new ArrayList<>();

                for (Long materialId : materialFilters) {
                    List<String> itemExcludeConditions = new ArrayList<>();

                    itemExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"id\":" + materialId + ",%");

                    itemExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"ai\":{%\"" + materialId + "\":%");

                    itemExcludeConditions.add("ple.extra_json NOT LIKE ?");
                    parameters.add("%\"ri\":{%\"" + materialId + "\":%");

                    excludeConditions.add("(" + String.join(" AND ", itemExcludeConditions) + ")");
                }

                whereConditions.add("(" + String.join(" AND ", excludeConditions) + ")");
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