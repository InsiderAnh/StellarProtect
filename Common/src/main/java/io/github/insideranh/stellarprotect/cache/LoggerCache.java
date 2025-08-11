package io.github.insideranh.stellarprotect.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.arguments.UsersArg;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTransactionEntry;
import io.github.insideranh.stellarprotect.enums.ActionCategory;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LoggerCache {

    private static final StellarProtect plugin = StellarProtect.getInstance();
    @Getter
    private static final Map<ActionCategory, CacheConfig> CATEGORY_CONFIGS = createCategoryConfigs();

    @Getter
    private static final Map<ActionCategory, Queue<LogEntry>> unSavedLogsByCategory = new ConcurrentHashMap<>();
    @Getter
    private static final Map<ActionCategory, ConcurrentHashMap<LocationCache, ConcurrentLinkedQueue<LogEntry>>> cachedLogsByCategory = new ConcurrentHashMap<>();

    @Getter
    private static final ConcurrentHashMap<LocationCache, PlayerBlockLogEntry> placedBlockLogs = new ConcurrentHashMap<>();

    @Getter
    private static final ConcurrentHashMap<String, LRUCache<String, LogEntry>> playerRecentActions = new ConcurrentHashMap<>();

    @Getter
    private static final Cache<String, CachedQuery> queryCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES).build();
    @Getter
    private static final Cache<String, Long> countCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES).build();

    private static final AtomicLong totalLogsProcessed = new AtomicLong(0);
    private static final AtomicInteger cacheHits = new AtomicInteger(0);
    private static final AtomicInteger cacheMisses = new AtomicInteger(0);

    static {
        initializeCaches();
        startCleanupScheduler();
    }

    private static Map<ActionCategory, CacheConfig> createCategoryConfigs() {
        Map<ActionCategory, CacheConfig> configs = new HashMap<>();
        configs.put(ActionCategory.BLOCK_ACTIONS, new CacheConfig(500, 20, 5 * 60 * 1000L));
        configs.put(ActionCategory.ITEM_ACTIONS, new CacheConfig(500, 15, 10 * 60 * 1000L));
        configs.put(ActionCategory.ENTITY_ACTIONS, new CacheConfig(200, 10, 15 * 60 * 1000L));
        configs.put(ActionCategory.PLAYER_ACTIONS, new CacheConfig(200, 2, 30 * 60 * 1000L));
        configs.put(ActionCategory.SYSTEM_ACTIONS, new CacheConfig(100, 5, 30 * 60 * 1000L));
        configs.put(ActionCategory.COMMUNICATION_ACTIONS, new CacheConfig(50, 8, 20 * 60 * 1000L));
        configs.put(ActionCategory.FLUID_ACTIONS, new CacheConfig(500, 5, 12 * 60 * 1000L));
        configs.put(ActionCategory.INVENTORY_ACTIONS, new CacheConfig(1000, 20, 5 * 60 * 1000L));
        configs.put(ActionCategory.SESSION_ACTIONS, new CacheConfig(100, 5, 30 * 60 * 1000L));
        configs.put(ActionCategory.SIGN_ACTIONS, new CacheConfig(100, 5, 30 * 60 * 1000L));
        configs.put(ActionCategory.UNKNOWN_ACTIONS, new CacheConfig(500, 5, 5 * 60 * 1000L));
        return configs;
    }

    private static CacheConfig getCategoryConfig(ActionCategory category) {
        return CATEGORY_CONFIGS.getOrDefault(category, CATEGORY_CONFIGS.get(ActionCategory.UNKNOWN_ACTIONS));
    }

    private static void initializeCaches() {
        for (ActionCategory category : ActionCategory.values()) {
            unSavedLogsByCategory.put(category, new ConcurrentLinkedQueue<>());
            cachedLogsByCategory.put(category, new ConcurrentHashMap<>());
        }
    }

    private static void startCleanupScheduler() {
        plugin.getStellarTaskHook(() -> {
            clearRamCache();
            cleanQueryCache();
        }).runTaskTimerAsynchronously(5 * 60 * 20L, 5 * 60 * 20L);
    }

    public static void addLog(LogEntry logEntry) {
        ActionCategory category = ActionCategory.fromActionTypes(ActionType.getById(logEntry.getActionType()));
        LocationCache location = logEntry.asLocation();

        unSavedLogsByCategory.get(category).add(logEntry);

        cachedLogsByCategory.get(category).computeIfAbsent(location, k -> new ConcurrentLinkedQueue<>()).add(logEntry);

        if (logEntry instanceof PlayerBlockLogEntry && ActionType.getById(logEntry.getActionType()) == ActionType.BLOCK_PLACE) {
            placedBlockLogs.put(location, (PlayerBlockLogEntry) logEntry);
        }

        totalLogsProcessed.incrementAndGet();

        CacheConfig config = CATEGORY_CONFIGS.get(category);
        if (unSavedLogsByCategory.get(category).size() >= config.batchSize) {
            StellarProtect.getInstance().getCacheManager().forceSave(category);
        }
    }

    public static void loadLog(LogEntry logEntry) {
        ActionCategory category = ActionCategory.fromActionTypes(ActionType.getById(logEntry.getActionType()));
        LocationCache location = logEntry.asLocation();

        cachedLogsByCategory.get(category).computeIfAbsent(location, k -> new ConcurrentLinkedQueue<>()).add(logEntry);
    }

    public static List<LogEntry> getFlushLogsToDatabase() {
        List<LogEntry> allBatch = new ArrayList<>();

        for (ActionCategory category : ActionCategory.values()) {
            List<LogEntry> categoryBatch = getFlushLogsToDatabase(category);
            allBatch.addAll(categoryBatch);
        }

        Debugger.debugSave("Flushing " + allBatch.size() + " logs to database.");
        return allBatch;
    }

    public static List<LogEntry> getFlushLogsToDatabase(ActionCategory category) {
        List<LogEntry> batch = new ArrayList<>();
        Queue<LogEntry> categoryQueue = unSavedLogsByCategory.get(category);
        CacheConfig config = getCategoryConfig(category);

        for (int i = 0; i < config.batchSize; i++) {
            LogEntry entry = categoryQueue.poll();
            if (entry == null) break;
            batch.add(entry);
        }

        return batch;
    }

    public static List<ItemLogEntry> getChestTransactions(Location location, int skip, int limit) {
        LocationCache locationCache = LocationCache.of(location);
        ConcurrentHashMap<LocationCache, ConcurrentLinkedQueue<LogEntry>> cached = cachedLogsByCategory.get(ActionCategory.INVENTORY_ACTIONS);

        List<ItemLogEntry> allItems = new ArrayList<>();

        if (cached.containsKey(locationCache)) {
            ConcurrentLinkedQueue<LogEntry> logs = cached.get(locationCache);

            for (LogEntry log : logs) {
                if (!(log instanceof PlayerTransactionEntry)) continue;
                PlayerTransactionEntry transaction = (PlayerTransactionEntry) log;

                for (Map.Entry<Long, Integer> addedEntry : transaction.getAdded().entrySet()) {
                    ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(addedEntry.getKey());
                    ItemStack item = itemTemplate.getBukkitItem();
                    if (item == null) continue;

                    allItems.add(new ItemLogEntry(item, log.getPlayerId(), addedEntry.getValue(), true, log.getCreatedAt()));
                }

                for (Map.Entry<Long, Integer> removedEntry : transaction.getRemoved().entrySet()) {
                    ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(removedEntry.getKey());
                    ItemStack item = itemTemplate.getBukkitItem();
                    if (item == null) continue;

                    allItems.add(new ItemLogEntry(item, log.getPlayerId(), removedEntry.getValue(), false, log.getCreatedAt()));
                }
            }
        }

        return allItems.stream()
            .sorted(Comparator.comparingLong(ItemLogEntry::getCreatedAt).reversed())
            .skip(skip)
            .limit(limit)
            .collect(Collectors.toList());
    }

    public static List<LogEntry> getLogs(DatabaseFilters databaseFilters, int skip, int limit) {
        TimeArg timeArg = databaseFilters.getTimeFilter();
        RadiusArg radiusArg = databaseFilters.getRadiusFilter();
        List<Integer> actionTypeIds = databaseFilters.getActionTypesFilter();
        UsersArg usersArg = databaseFilters.getUserFilters();

        List<ActionType> actionTypes = actionTypeIds.stream()
            .map(ActionType::getById)
            .collect(Collectors.toList());

        String actionKey = actionTypes.stream().map(ActionType::name).collect(Collectors.joining(","));
        String userKey = !usersArg.getUserIds().isEmpty() ? usersArg.getUserIds().stream().map(String::valueOf).collect(Collectors.joining(",")) : "ALL";
        String cacheKey = (timeArg != null ? timeArg.getStart() + "_" + timeArg.getEnd() : "NOTIME") + "_" + (radiusArg != null ? radiusArg.toString() : "NORADIUS") + "_" + (!actionKey.isEmpty() ? actionKey : "ALL") + "_" + userKey + "_" + skip + "_" + limit;

        CachedQuery cached = queryCache.getIfPresent(cacheKey);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.getLogs();
        }

        cacheMisses.incrementAndGet();

        Set<Integer> actionIds = new HashSet<>(actionTypeIds);
        List<LogEntry> logs = new ArrayList<>();

        Set<ActionCategory> categoriesToSearch = new HashSet<>();
        if (actionTypes.isEmpty()) {
            categoriesToSearch.addAll(Arrays.asList(ActionCategory.values()));
        } else {
            categoriesToSearch.addAll(ActionCategory.fromActionTypes(actionTypes));
        }

        for (ActionCategory category : categoriesToSearch) {
            ConcurrentHashMap<LocationCache, ConcurrentLinkedQueue<LogEntry>> categoryCache = cachedLogsByCategory.get(category);

            for (Map.Entry<LocationCache, ConcurrentLinkedQueue<LogEntry>> entry : categoryCache.entrySet()) {
                LocationCache cache = entry.getKey();

                if (radiusArg != null && !cache.isInside(radiusArg.getMinX(), radiusArg.getMaxX(),
                    radiusArg.getMinY(), radiusArg.getMaxY(),
                    radiusArg.getMinZ(), radiusArg.getMaxZ())) {
                    continue;
                }

                for (LogEntry log : entry.getValue()) {
                    if (!actionTypeIds.isEmpty() && !actionIds.contains(log.getActionType())) continue;

                    if (timeArg != null && (log.getCreatedAt() < timeArg.getStart() || log.getCreatedAt() > timeArg.getEnd()))
                        continue;

                    if (!usersArg.getUserIds().isEmpty() && !usersArg.getUserIds().contains(log.getPlayerId()))
                        continue;

                    logs.add(log);
                }
            }
        }

        logs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        List<LogEntry> paginatedLogs = logs.stream()
            .skip(skip)
            .limit(limit)
            .collect(Collectors.toList());

        if (skip == 0 && limit <= 100) {
            queryCache.put(cacheKey, new CachedQuery(paginatedLogs, System.currentTimeMillis() + 5 * 60 * 1000L));
        }

        return paginatedLogs;
    }

    @Deprecated
    public static List<LogEntry> getLogs(TimeArg timeArg, RadiusArg radiusArg, ActionType actionType, int skip, int limit) {
        return getLogs(timeArg, radiusArg, actionType != null ? Collections.singletonList(actionType) : Collections.emptyList(), skip, limit);
    }

    @Deprecated
    public static List<LogEntry> getLogs(TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes, int skip, int limit) {
        String actionKey = actionTypes.stream().map(ActionType::name).collect(Collectors.joining(","));
        String cacheKey = timeArg.getStart() + "_" + timeArg.getEnd() + "_" + radiusArg.toString() + "_" + (!actionKey.isEmpty() ? actionKey : "ALL") + "_" + skip + "_" + limit;

        CachedQuery cached = queryCache.getIfPresent(cacheKey);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.getLogs();
        }

        cacheMisses.incrementAndGet();

        Set<Integer> actionIds = actionTypes.stream().map(ActionType::getId).collect(Collectors.toSet());
        List<LogEntry> logs = new ArrayList<>();

        Set<ActionCategory> categoriesToSearch = new HashSet<>();
        if (actionTypes.isEmpty()) {
            categoriesToSearch.addAll(ActionCategory.fromActionTypes(actionTypes));
        } else {
            categoriesToSearch.addAll(Arrays.asList(ActionCategory.values()));
        }

        for (ActionCategory category : categoriesToSearch) {
            ConcurrentHashMap<LocationCache, ConcurrentLinkedQueue<LogEntry>> categoryCache = cachedLogsByCategory.get(category);

            for (Map.Entry<LocationCache, ConcurrentLinkedQueue<LogEntry>> entry : categoryCache.entrySet()) {
                LocationCache cache = entry.getKey();
                if (!cache.isInside(radiusArg.getMinX(), radiusArg.getMaxX(), radiusArg.getMinY(), radiusArg.getMaxY(), radiusArg.getMinZ(), radiusArg.getMaxZ()))
                    continue;

                for (LogEntry log : entry.getValue()) {
                    if (!actionTypes.isEmpty() && !actionIds.contains(log.getActionType())) continue;
                    if (log.getCreatedAt() < timeArg.getStart() || log.getCreatedAt() > timeArg.getEnd()) continue;
                    logs.add(log);
                }
            }
        }

        logs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        List<LogEntry> paginatedLogs = logs.stream()
            .skip(skip)
            .limit(limit)
            .collect(Collectors.toList());

        if (skip == 0 && limit <= 100) {
            queryCache.put(cacheKey, new CachedQuery(paginatedLogs, System.currentTimeMillis() + 5 * 60 * 1000L));
        }

        return paginatedLogs;
    }

    public static long countLogs(TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes) {
        String actionKey = actionTypes.stream().map(ActionType::name).collect(Collectors.joining(","));
        String countCacheKey = "COUNT_" + timeArg.getStart() + "_" + timeArg.getEnd() + "_" + radiusArg.toString() + "_" + (!actionKey.isEmpty() ? actionKey : "ALL");

        Long cached = countCache.getIfPresent(countCacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        cacheMisses.incrementAndGet();

        Set<Integer> actionIds = actionTypes.stream().map(ActionType::getId).collect(Collectors.toSet());
        long count = 0;

        Set<ActionCategory> categoriesToSearch = new HashSet<>();
        if (actionTypes.isEmpty()) {
            categoriesToSearch.addAll(Arrays.asList(ActionCategory.values()));
        } else {
            categoriesToSearch.addAll(ActionCategory.fromActionTypes(actionTypes));
        }

        for (ActionCategory category : categoriesToSearch) {
            ConcurrentHashMap<LocationCache, ConcurrentLinkedQueue<LogEntry>> categoryCache = cachedLogsByCategory.get(category);

            for (Map.Entry<LocationCache, ConcurrentLinkedQueue<LogEntry>> entry : categoryCache.entrySet()) {
                LocationCache cache = entry.getKey();

                if (!cache.isInside(radiusArg.getMinX(), radiusArg.getMaxX(),
                    radiusArg.getMinY(), radiusArg.getMaxY(),
                    radiusArg.getMinZ(), radiusArg.getMaxZ())) {
                    continue;
                }

                for (LogEntry log : entry.getValue()) {
                    if (!actionTypes.isEmpty() && !actionIds.contains(log.getActionType())) {
                        continue;
                    }

                    if (log.getCreatedAt() < timeArg.getStart() || log.getCreatedAt() > timeArg.getEnd()) {
                        continue;
                    }

                    count++;
                }
            }
        }

        return count;
    }

    public static List<LogEntry> getLogs(Location location, int skip, int limit) {
        LocationCache locationCache = LocationCache.of(location);
        return getLogs(locationCache, skip, limit);
    }

    public static List<LogEntry> getLogs(LocationCache location, int skip, int limit) {
        List<LogEntry> allLogs = new ArrayList<>();

        for (ActionCategory category : ActionCategory.values()) {
            ConcurrentLinkedQueue<LogEntry> categoryLogs = cachedLogsByCategory.get(category).get(location);
            if (categoryLogs != null) {
                allLogs.addAll(categoryLogs);
            }
        }

        allLogs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        return allLogs.stream()
            .skip(skip)
            .limit(limit)
            .collect(Collectors.toList());
    }

    public static boolean hasLogs(LocationCache location) {
        for (ActionCategory category : ActionCategory.values()) {
            if (cachedLogsByCategory.get(category).containsKey(location)) {
                return true;
            }
        }
        return false;
    }

    public static LogEntry getPlacedBlockLog(Location location) {
        return placedBlockLogs.get(LocationCache.of(location));
    }

    public static List<LogEntry> getPlayerRecentActions(String playerName, ActionCategory category, int limit) {
        LRUCache<String, LogEntry> playerCache = playerRecentActions.get(playerName);
        if (playerCache == null) return new ArrayList<>();

        List<LogEntry> result = new ArrayList<>();
        for (Map.Entry<String, LogEntry> entry : playerCache.entrySet()) {
            if (entry.getKey().startsWith(category.name())) {
                result.add(entry.getValue());
                if (result.size() >= limit) break;
            }
        }

        return result;
    }

    public static void clearRamCache() {
        long currentTime = System.currentTimeMillis();

        for (ActionCategory category : ActionCategory.values()) {
            CacheConfig config = CATEGORY_CONFIGS.get(category);
            long limit = currentTime - config.cacheRetentionTime;

            ConcurrentHashMap<LocationCache, ConcurrentLinkedQueue<LogEntry>> categoryCache = cachedLogsByCategory.get(category);

            Iterator<Map.Entry<LocationCache, ConcurrentLinkedQueue<LogEntry>>> mapIterator = categoryCache.entrySet().iterator();

            while (mapIterator.hasNext()) {
                Map.Entry<LocationCache, ConcurrentLinkedQueue<LogEntry>> entry = mapIterator.next();
                ConcurrentLinkedQueue<LogEntry> logs = cleanLogQueue(entry.getValue(), limit, config.maxLogsPerLocation);

                if (logs.isEmpty()) {
                    mapIterator.remove();
                }
            }
        }

        long placedLimit = currentTime - (30 * 1000L);

        placedBlockLogs.entrySet().removeIf(entry -> entry.getValue().getCreatedAt() < placedLimit);

        Iterator<Map.Entry<String, LRUCache<String, LogEntry>>> playerIterator = playerRecentActions.entrySet().iterator();

        while (playerIterator.hasNext()) {
            Map.Entry<String, LRUCache<String, LogEntry>> entry = playerIterator.next();
            entry.getValue().removeOldEntries(currentTime - 10 * 60 * 1000L);
            if (entry.getValue().isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private static ConcurrentLinkedQueue<LogEntry> cleanLogQueue(ConcurrentLinkedQueue<LogEntry> logs, long timeLimit, int maxLogs) {
        logs.removeIf(log -> log.getCreatedAt() < timeLimit);

        while (logs.size() > maxLogs) {
            logs.poll();
        }

        return logs;
    }

    private static void cleanQueryCache() {
        queryCache.cleanUp();
        countCache.cleanUp();
    }

    private static class CacheConfig {

        final int batchSize;
        final int maxLogsPerLocation;
        final long cacheRetentionTime;

        CacheConfig(int batchSize, int maxLogsPerLocation, long cacheRetentionTime) {
            this.batchSize = batchSize;
            this.maxLogsPerLocation = maxLogsPerLocation;
            this.cacheRetentionTime = cacheRetentionTime;
        }

    }

    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {

        private final int maxSize;

        public LRUCache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }

        public void removeOldEntries(long timeLimit) {
            Iterator<Map.Entry<K, V>> iterator = entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, V> entry = iterator.next();
                if (entry.getValue() instanceof LogEntry) {
                    LogEntry logEntry = (LogEntry) entry.getValue();
                    if (logEntry.getCreatedAt() < timeLimit) {
                        iterator.remove();
                    }
                }
            }
        }

    }

    @Getter
    private static class CachedQuery {

        private final List<LogEntry> logs;
        private final long expireTime;

        public CachedQuery(List<LogEntry> logs, long expireTime) {
            this.logs = logs;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentTime) {
            return currentTime > expireTime;
        }

    }

}