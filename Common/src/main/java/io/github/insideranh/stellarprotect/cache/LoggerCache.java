package io.github.insideranh.stellarprotect.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.arguments.UsersArg;
import io.github.insideranh.stellarprotect.cache.counters.CategoryCounter;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class LoggerCache {

    private static final StellarProtect plugin = StellarProtect.getInstance();

    @Getter
    private static final CacheConfig[] CATEGORY_CONFIGS_ARRAY = createCategoryConfigsArray();
    @Getter
    private static final FixedSizeCircularBuffer[] unSavedLogsByCategory = new FixedSizeCircularBuffer[ActionCategory.values().length];
    @Getter
    private static final ConcurrentHashMap<LocationCache, LogRingBuffer>[] cachedLogsByCategory = new ConcurrentHashMap[ActionCategory.values().length];
    @Getter
    private static final TimestampedHashMap<LocationCache, PlayerBlockLogEntry> placedBlockLogs = new TimestampedHashMap<>(1024, 30_000L);

    @Getter
    private static final Cache<String, CachedQuery> queryCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();
    private static final Cache<String, Long> countCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(500)
        .build();

    private static final ActionCategory[] ACTION_TO_CATEGORY_CACHE;
    private static final CategoryCounter[] categoryCounters = new CategoryCounter[ActionCategory.values().length];

    private static final AtomicLong totalLogsProcessed = new AtomicLong(0);
    private static final AtomicInteger cacheHits = new AtomicInteger(0);
    private static final AtomicInteger cacheMisses = new AtomicInteger(0);

    private static final ThreadLocal<StringBuilder> STRING_BUILDER_POOL = ThreadLocal.withInitial(() -> new StringBuilder(256));

    private static final ThreadLocal<ArrayList<LogEntry>> LOG_LIST_POOL = ThreadLocal.withInitial(() -> new ArrayList<>(1000));

    private static final ThreadLocal<HashSet<Integer>> ACTION_ID_SET_POOL = ThreadLocal.withInitial(() -> new HashSet<>(64));

    static {
        int maxActionId = Arrays.stream(ActionType.values())
            .mapToInt(ActionType::getId)
            .max()
            .orElse(0);

        ACTION_TO_CATEGORY_CACHE = new ActionCategory[maxActionId + 1];

        initializeCaches();
        startCleanupScheduler();
    }

    private static CacheConfig[] createCategoryConfigsArray() {
        ActionCategory[] categories = ActionCategory.values();
        CacheConfig[] configs = new CacheConfig[categories.length];

        configs[ActionCategory.BLOCK_ACTIONS.ordinal()] = new CacheConfig(1000, 50, 5 * 60 * 1000L);
        configs[ActionCategory.ITEM_ACTIONS.ordinal()] = new CacheConfig(800, 30, 10 * 60 * 1000L);
        configs[ActionCategory.ENTITY_ACTIONS.ordinal()] = new CacheConfig(400, 20, 15 * 60 * 1000L);
        configs[ActionCategory.PLAYER_ACTIONS.ordinal()] = new CacheConfig(200, 5, 30 * 60 * 1000L);
        configs[ActionCategory.SYSTEM_ACTIONS.ordinal()] = new CacheConfig(100, 10, 30 * 60 * 1000L);
        configs[ActionCategory.COMMUNICATION_ACTIONS.ordinal()] = new CacheConfig(100, 15, 20 * 60 * 1000L);
        configs[ActionCategory.FLUID_ACTIONS.ordinal()] = new CacheConfig(600, 10, 12 * 60 * 1000L);
        configs[ActionCategory.INVENTORY_ACTIONS.ordinal()] = new CacheConfig(2000, 40, 5 * 60 * 1000L);
        configs[ActionCategory.SESSION_ACTIONS.ordinal()] = new CacheConfig(100, 10, 30 * 60 * 1000L);
        configs[ActionCategory.SIGN_ACTIONS.ordinal()] = new CacheConfig(100, 10, 30 * 60 * 1000L);
        configs[ActionCategory.HOOK_ACTIONS.ordinal()] = new CacheConfig(300, 10, 5 * 60 * 1000L);
        configs[ActionCategory.WORLD_ACTIONS.ordinal()] = new CacheConfig(100, 2, 30 * 60 * 1000L);
        configs[ActionCategory.UNKNOWN_ACTIONS.ordinal()] = new CacheConfig(500, 10, 5 * 60 * 1000L);

        return configs;
    }

    private static void initializeCaches() {
        ActionCategory[] categories = ActionCategory.values();

        for (ActionCategory category : categories) {
            int categoryOrdinal = category.ordinal();

            for (ActionType actionType : category.getActionSet()) {
                ACTION_TO_CATEGORY_CACHE[actionType.getId()] = category;
            }

            CacheConfig config = CATEGORY_CONFIGS_ARRAY[categoryOrdinal];

            unSavedLogsByCategory[categoryOrdinal] = new FixedSizeCircularBuffer(config.batchSize * 2);
            cachedLogsByCategory[categoryOrdinal] = new ConcurrentHashMap<>(1024, 0.75f, 16);
            categoryCounters[categoryOrdinal] = new CategoryCounter(config.batchSize);
        }
    }

    private static void startCleanupScheduler() {
        plugin.getStellarTaskHook(() -> {
            clearRamCache();
            cleanQueryCache();
        }).runTaskTimerAsynchronously(5 * 60 * 20L, 5 * 60 * 20L);
    }

    public static void addLog(LogEntry logEntry) {
        ActionCategory category = getCategoryByActionId(logEntry.getActionType());
        int categoryOrdinal = category.ordinal();
        LocationCache location = logEntry.asLocation();

        unSavedLogsByCategory[categoryOrdinal].add(logEntry);

        ConcurrentHashMap<LocationCache, LogRingBuffer> categoryCache = cachedLogsByCategory[categoryOrdinal];
        LogRingBuffer buffer = categoryCache.get(location);
        if (buffer == null) {
            CacheConfig config = CATEGORY_CONFIGS_ARRAY[categoryOrdinal];
            buffer = new LogRingBuffer(config.maxLogsPerLocation);
            LogRingBuffer existing = categoryCache.putIfAbsent(location, buffer);
            if (existing != null) {
                buffer = existing;
            }
        }
        buffer.add(logEntry);

        if (logEntry instanceof PlayerBlockLogEntry && logEntry.getActionType() == ActionType.BLOCK_PLACE.getId()) {
            placedBlockLogs.put(location, (PlayerBlockLogEntry) logEntry);
        }

        totalLogsProcessed.incrementAndGet();

        CategoryCounter counter = categoryCounters[categoryOrdinal];
        if (counter.incrementAndCheckThreshold()) {
            StellarProtect.getInstance().getCacheManager().forceSave(category);
            counter.reset();
        }
    }

    private static ActionCategory getCategoryByActionId(int actionTypeId) {
        if ((actionTypeId >>> 31) == 0 && actionTypeId < ACTION_TO_CATEGORY_CACHE.length) {
            ActionCategory category = ACTION_TO_CATEGORY_CACHE[actionTypeId];
            return category != null ? category : ActionCategory.SYSTEM_ACTIONS;
        }
        return ActionCategory.SYSTEM_ACTIONS;
    }

    public static void loadLog(LogEntry logEntry) {
        ActionCategory category = ActionCategory.fromActionTypes(ActionType.getById(logEntry.getActionType()));
        int categoryOrdinal = category.ordinal();
        LocationCache location = logEntry.asLocation();

        ConcurrentHashMap<LocationCache, LogRingBuffer> categoryCache = cachedLogsByCategory[categoryOrdinal];
        LogRingBuffer buffer = categoryCache.get(location);
        if (buffer == null) {
            CacheConfig config = CATEGORY_CONFIGS_ARRAY[categoryOrdinal];
            buffer = new LogRingBuffer(config.maxLogsPerLocation);
            LogRingBuffer existing = categoryCache.putIfAbsent(location, buffer);
            if (existing != null) {
                buffer = existing;
            }
        }
        buffer.add(logEntry);
    }

    public static List<LogEntry> getFlushLogsToDatabase() {
        ArrayList<LogEntry> allBatch = LOG_LIST_POOL.get();
        allBatch.clear();

        ActionCategory[] categories = ActionCategory.values();
        for (ActionCategory category : categories) {
            List<LogEntry> categoryBatch = getFlushLogsToDatabase(category);
            allBatch.addAll(categoryBatch);
        }

        Debugger.debugSave("Flushing " + allBatch.size() + " logs to database.");

        return new ArrayList<>(allBatch);
    }

    public static List<LogEntry> getFlushLogsToDatabase(ActionCategory category) {
        int categoryOrdinal = category.ordinal();
        CacheConfig config = CATEGORY_CONFIGS_ARRAY[categoryOrdinal];

        return unSavedLogsByCategory[categoryOrdinal].drainBatch(config.batchSize);
    }

    public static List<ItemLogEntry> getChestTransactions(Location location, int skip, int limit) {
        LocationCache locationCache = LocationCache.of(location);

        LogRingBuffer logs = cachedLogsByCategory[ActionCategory.INVENTORY_ACTIONS.ordinal()].get(locationCache);
        if (logs == null || logs.isEmpty()) return Collections.emptyList();

        ArrayList<ItemLogEntry> allItems = new ArrayList<>(limit * 2);

        logs.forEachReverse(log -> {
            if (!(log instanceof PlayerTransactionEntry)) return;
            PlayerTransactionEntry transaction = (PlayerTransactionEntry) log;

            transaction.getAdded().forEach((templateId, amount) -> {
                ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(templateId);
                ItemStack item = itemTemplate.getBukkitItem();
                if (item != null) {
                    allItems.add(new ItemLogEntry(item, log.getPlayerId(), amount, true, log.getCreatedAt()));
                }
            });

            transaction.getRemoved().forEach((templateId, amount) -> {
                ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(templateId);
                ItemStack item = itemTemplate.getBukkitItem();
                if (item != null) {
                    allItems.add(new ItemLogEntry(item, log.getPlayerId(), amount, false, log.getCreatedAt()));
                }
            });
        });

        allItems.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        if (skip >= allItems.size()) return Collections.emptyList();
        int toIndex = Math.min(skip + limit, allItems.size());
        return allItems.subList(skip, toIndex);
    }

    public static List<LogEntry> getLogs(LocationCache location, int skip, int limit) {
        ArrayList<LogEntry> allLogs = new ArrayList<>(limit * 2);

        ActionCategory[] categories = ActionCategory.values();
        for (int i = 0; i < categories.length; i++) {
            LogRingBuffer categoryLogs = cachedLogsByCategory[i].get(location);
            if (categoryLogs != null && !categoryLogs.isEmpty()) {
                categoryLogs.addAllTo(allLogs);
            }
        }

        allLogs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        if (skip >= allLogs.size()) return Collections.emptyList();
        int toIndex = Math.min(skip + limit, allLogs.size());
        return allLogs.subList(skip, toIndex);
    }

    public static List<LogEntry> getLogs(DatabaseFilters databaseFilters, int skip, int limit) {
        TimeArg timeArg = databaseFilters.getTimeFilter();
        RadiusArg radiusArg = databaseFilters.getRadiusFilter();
        List<Integer> actionTypeIds = databaseFilters.getActionTypesFilter();
        List<Long> worldsFilter = databaseFilters.getAllIncludeFilters();
        List<Long> worldsExcludeFilter = databaseFilters.getAllExcludeFilters();
        UsersArg usersArg = databaseFilters.getUserFilters();

        StringBuilder keyBuilder = STRING_BUILDER_POOL.get();
        keyBuilder.setLength(0);

        if (timeArg != null) {
            keyBuilder.append(timeArg.getStart()).append('_').append(timeArg.getEnd());
        } else {
            keyBuilder.append("NOTIME");
        }

        keyBuilder.append('_');

        if (radiusArg != null) {
            keyBuilder.append(radiusArg);
        } else {
            keyBuilder.append("NORADIUS");
        }

        keyBuilder.append('_');

        if (!actionTypeIds.isEmpty()) {
            for (int i = 0; i < actionTypeIds.size(); i++) {
                if (i > 0) keyBuilder.append(',');
                keyBuilder.append(actionTypeIds.get(i));
            }
        } else {
            keyBuilder.append("ALL");
        }

        keyBuilder.append('_');

        if (!usersArg.getUserIds().isEmpty()) {
            usersArg.getUserIds().forEach(userId -> keyBuilder.append(userId).append(','));
        } else {
            keyBuilder.append("ALL");
        }

        keyBuilder.append('_');

        if (!worldsFilter.isEmpty()) {
            worldsFilter.forEach(world -> keyBuilder.append(world).append(','));
        } else {
            keyBuilder.append("ALL");
        }

        keyBuilder.append('_');

        if (!worldsExcludeFilter.isEmpty()) {
            worldsExcludeFilter.forEach(world -> keyBuilder.append(world).append(','));
        } else {
            keyBuilder.append("ALL");
        }

        keyBuilder.append('_').append(skip).append('_').append(limit);

        String cacheKey = keyBuilder.toString();

        CachedQuery cached = queryCache.getIfPresent(cacheKey);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.getLogs();
        }

        cacheMisses.incrementAndGet();

        HashSet<Integer> actionIds = ACTION_ID_SET_POOL.get();
        actionIds.clear();
        actionIds.addAll(actionTypeIds);

        ArrayList<LogEntry> logs = LOG_LIST_POOL.get();
        logs.clear();

        Set<ActionCategory> categoriesToSearch = determineCategoriesToSearch(actionTypeIds);

        long timeStart = timeArg != null ? timeArg.getStart() : Long.MIN_VALUE;
        long timeEnd = timeArg != null ? timeArg.getEnd() : Long.MAX_VALUE;
        Set<Long> userIds = usersArg.getUserIds();
        boolean hasUserFilter = !userIds.isEmpty();
        boolean hasActionFilter = !actionTypeIds.isEmpty();

        for (ActionCategory category : categoriesToSearch) {
            int categoryOrdinal = category.ordinal();
            ConcurrentHashMap<LocationCache, LogRingBuffer> categoryCache = cachedLogsByCategory[categoryOrdinal];

            categoryCache.forEach((locationCache, ringBuffer) -> {
                if (radiusArg != null && !locationCache.isInside(
                    radiusArg.getMinX(), radiusArg.getMaxX(),
                    radiusArg.getMinY(), radiusArg.getMaxY(),
                    radiusArg.getMinZ(), radiusArg.getMaxZ())) {
                    return;
                }

                ringBuffer.forEachIf(log -> {
                    long createdAt = log.getCreatedAt();
                    if (createdAt < timeStart || createdAt > timeEnd) return false;
                    if (hasActionFilter && !actionIds.contains(log.getActionType())) return false;
                    return !hasUserFilter || userIds.contains(log.getPlayerId());
                }, logs);
            });
        }

        logs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        List<LogEntry> paginatedLogs;
        if (skip >= logs.size()) {
            paginatedLogs = Collections.emptyList();
        } else {
            int toIndex = Math.min(skip + limit, logs.size());
            paginatedLogs = new ArrayList<>(logs.subList(skip, toIndex));
        }

        if (skip == 0 && limit <= 100) {
            queryCache.put(cacheKey, new CachedQuery(paginatedLogs, System.currentTimeMillis() + 5 * 60 * 1000L));
        }

        return paginatedLogs;
    }

    private static Set<ActionCategory> determineCategoriesToSearch(List<Integer> actionTypeIds) {
        if (actionTypeIds.isEmpty()) {
            return EnumSet.allOf(ActionCategory.class);
        }

        EnumSet<ActionCategory> categories = EnumSet.noneOf(ActionCategory.class);
        for (int actionId : actionTypeIds) {
            ActionCategory category = getCategoryByActionId(actionId);
            categories.add(category);
        }
        return categories;
    }

    public static List<LogEntry> getLogs(Location location, int skip, int limit) {
        return getLogs(LocationCache.of(location), skip, limit);
    }

    public static LogEntry getPlacedBlockLog(Location location) {
        return placedBlockLogs.get(LocationCache.of(location));
    }

    public static void clearRamCache() {
        long currentTime = System.currentTimeMillis();

        ActionCategory[] categories = ActionCategory.values();
        for (int i = 0; i < categories.length; i++) {
            CacheConfig config = CATEGORY_CONFIGS_ARRAY[i];
            long timeLimit = currentTime - config.cacheRetentionTime;

            ConcurrentHashMap<LocationCache, LogRingBuffer> categoryCache = cachedLogsByCategory[i];

            categoryCache.entrySet().removeIf(entry -> {
                LogRingBuffer buffer = entry.getValue();
                buffer.removeOldEntries(timeLimit);
                return buffer.isEmpty();
            });
        }

        placedBlockLogs.cleanup(currentTime);
    }

    private static void cleanQueryCache() {
        queryCache.cleanUp();
        countCache.cleanUp();
    }

    @Deprecated
    public static List<LogEntry> getLogs(TimeArg timeArg, RadiusArg radiusArg, ActionType actionType, int skip, int limit) {
        return getLogs(timeArg, radiusArg, actionType != null ? Collections.singletonList(actionType) : Collections.emptyList(), skip, limit);
    }

    @Deprecated
    public static List<LogEntry> getLogs(TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes, int skip, int limit) {
        StringBuilder keyBuilder = STRING_BUILDER_POOL.get();
        keyBuilder.setLength(0);

        keyBuilder.append(timeArg.getStart()).append('_').append(timeArg.getEnd())
            .append('_').append(radiusArg.toString()).append('_');

        if (!actionTypes.isEmpty()) {
            for (int i = 0; i < actionTypes.size(); i++) {
                if (i > 0) keyBuilder.append(',');
                keyBuilder.append(actionTypes.get(i).name());
            }
        } else {
            keyBuilder.append("ALL");
        }

        keyBuilder.append('_').append(skip).append('_').append(limit);
        String cacheKey = keyBuilder.toString();

        CachedQuery cached = queryCache.getIfPresent(cacheKey);
        if (cached != null && !cached.isExpired()) {
            cacheHits.incrementAndGet();
            return cached.getLogs();
        }

        cacheMisses.incrementAndGet();

        HashSet<Integer> actionIds = ACTION_ID_SET_POOL.get();
        actionIds.clear();

        for (ActionType actionType : actionTypes) {
            actionIds.add(actionType.getId());
        }

        ArrayList<LogEntry> logs = LOG_LIST_POOL.get();
        logs.clear();

        Set<ActionCategory> categoriesToSearch;
        if (actionTypes.isEmpty()) {
            categoriesToSearch = EnumSet.allOf(ActionCategory.class);
        } else {
            categoriesToSearch = EnumSet.noneOf(ActionCategory.class);
            for (ActionType actionType : actionTypes) {
                ActionCategory category = getCategoryByActionId(actionType.getId());
                categoriesToSearch.add(category);
            }
        }

        long timeStart = timeArg.getStart();
        long timeEnd = timeArg.getEnd();
        boolean hasActionFilter = !actionTypes.isEmpty();

        for (ActionCategory category : categoriesToSearch) {
            int categoryOrdinal = category.ordinal();
            ConcurrentHashMap<LocationCache, LogRingBuffer> categoryCache = cachedLogsByCategory[categoryOrdinal];

            categoryCache.forEach((locationCache, ringBuffer) -> {
                if (!locationCache.isInside(radiusArg.getMinX(), radiusArg.getMaxX(),
                    radiusArg.getMinY(), radiusArg.getMaxY(),
                    radiusArg.getMinZ(), radiusArg.getMaxZ())) {
                    return;
                }

                ringBuffer.forEachIf(log -> {
                    long createdAt = log.getCreatedAt();
                    boolean timeValid = (createdAt >= timeStart) & (createdAt <= timeEnd);
                    boolean actionValid = !hasActionFilter || actionIds.contains(log.getActionType());
                    return timeValid & actionValid;
                }, logs);
            });
        }

        logs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        List<LogEntry> paginatedLogs;
        if (skip >= logs.size()) {
            paginatedLogs = Collections.emptyList();
        } else {
            int toIndex = Math.min(skip + limit, logs.size());
            paginatedLogs = new ArrayList<>(logs.subList(skip, toIndex));
        }

        if (skip == 0 && limit <= 100) {
            queryCache.put(cacheKey, new CachedQuery(paginatedLogs, System.currentTimeMillis() + 5 * 60 * 1000L));
        }

        return paginatedLogs;
    }

    public static long countLogs(TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes) {
        StringBuilder keyBuilder = STRING_BUILDER_POOL.get();
        keyBuilder.setLength(0);

        keyBuilder.append("COUNT_").append(timeArg.getStart()).append('_').append(timeArg.getEnd())
            .append('_').append(radiusArg.toString()).append('_');

        if (!actionTypes.isEmpty()) {
            for (int i = 0; i < actionTypes.size(); i++) {
                if (i > 0) keyBuilder.append(',');
                keyBuilder.append(actionTypes.get(i).name());
            }
        } else {
            keyBuilder.append("ALL");
        }

        String countCacheKey = keyBuilder.toString();

        Long cached = countCache.getIfPresent(countCacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        cacheMisses.incrementAndGet();

        HashSet<Integer> actionIds = ACTION_ID_SET_POOL.get();
        actionIds.clear();

        for (ActionType actionType : actionTypes) {
            actionIds.add(actionType.getId());
        }

        Set<ActionCategory> categoriesToSearch;
        if (actionTypes.isEmpty()) {
            categoriesToSearch = EnumSet.allOf(ActionCategory.class);
        } else {
            categoriesToSearch = EnumSet.noneOf(ActionCategory.class);
            for (ActionType actionType : actionTypes) {
                ActionCategory category = getCategoryByActionId(actionType.getId());
                categoriesToSearch.add(category);
            }
        }

        long count = 0;
        long timeStart = timeArg.getStart();
        long timeEnd = timeArg.getEnd();
        boolean hasActionFilter = !actionTypes.isEmpty();

        for (ActionCategory category : categoriesToSearch) {
            int categoryOrdinal = category.ordinal();
            ConcurrentHashMap<LocationCache, LogRingBuffer> categoryCache = cachedLogsByCategory[categoryOrdinal];

            for (Map.Entry<LocationCache, LogRingBuffer> entry : categoryCache.entrySet()) {
                LocationCache cache = entry.getKey();

                if (!cache.isInside(radiusArg.getMinX(), radiusArg.getMaxX(),
                    radiusArg.getMinY(), radiusArg.getMaxY(),
                    radiusArg.getMinZ(), radiusArg.getMaxZ())) {
                    continue;
                }

                LogRingBuffer buffer = entry.getValue();
                count += buffer.countMatching(timeStart, timeEnd, hasActionFilter ? actionIds : null);
            }
        }

        countCache.put(countCacheKey, count);
        return count;
    }

    private static class FixedSizeCircularBuffer {

        private final LogEntry[] buffer;
        private final int capacity;
        private final StampedLock lock = new StampedLock();
        private volatile int writeIndex = 0;
        private volatile int readIndex = 0;
        private volatile int size = 0;

        public FixedSizeCircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new LogEntry[capacity];
        }

        public void add(LogEntry entry) {
            long stamp = lock.writeLock();
            try {
                if (size < capacity) {
                    buffer[writeIndex] = entry;
                    writeIndex = (writeIndex + 1) % capacity;
                    size++;
                } else {
                    buffer[writeIndex] = entry;
                    writeIndex = (writeIndex + 1) % capacity;
                    readIndex = (readIndex + 1) % capacity;
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        public List<LogEntry> drainBatch(int batchSize) {
            long stamp = lock.writeLock();
            try {
                int actualBatch = Math.min(batchSize, size);
                List<LogEntry> batch = new ArrayList<>(actualBatch);

                for (int i = 0; i < actualBatch; i++) {
                    batch.add(buffer[readIndex]);
                    buffer[readIndex] = null;
                    readIndex = (readIndex + 1) % capacity;
                }

                size -= actualBatch;
                return batch;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

    }

    private static class LogRingBuffer {

        private final LogEntry[] buffer;
        private final int capacity;
        private final StampedLock lock = new StampedLock();
        private volatile int head = 0;
        private volatile int size = 0;

        public LogRingBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new LogEntry[capacity];
        }

        public void add(LogEntry entry) {
            long stamp = lock.writeLock();
            try {
                if (size < capacity) {
                    buffer[(head + size) % capacity] = entry;
                    size++;
                } else {
                    buffer[head] = entry;
                    head = (head + 1) % capacity;
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        public boolean isEmpty() {
            long stamp = lock.tryOptimisticRead();
            boolean empty = size == 0;
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    empty = size == 0;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return empty;
        }

        public void forEachReverse(Consumer<LogEntry> consumer) {
            long stamp = lock.readLock();
            try {
                for (int i = size - 1; i >= 0; i--) {
                    LogEntry entry = buffer[(head + i) % capacity];
                    if (entry != null) {
                        consumer.accept(entry);
                    }
                }
            } finally {
                lock.unlockRead(stamp);
            }
        }

        public void addAllTo(List<LogEntry> list) {
            long stamp = lock.readLock();
            try {
                for (int i = 0; i < size; i++) {
                    LogEntry entry = buffer[(head + i) % capacity];
                    if (entry != null) {
                        list.add(entry);
                    }
                }
            } finally {
                lock.unlockRead(stamp);
            }
        }

        public void forEachIf(Predicate<LogEntry> filter, List<LogEntry> collector) {
            long stamp = lock.readLock();
            try {
                for (int i = 0; i < size; i++) {
                    LogEntry entry = buffer[(head + i) % capacity];
                    if (entry != null && filter.test(entry)) {
                        collector.add(entry);
                    }
                }
            } finally {
                lock.unlockRead(stamp);
            }
        }

        public void removeOldEntries(long timeLimit) {
            long stamp = lock.writeLock();
            try {
                int removed = 0;
                for (int i = 0; i < size; i++) {
                    LogEntry entry = buffer[(head + i) % capacity];
                    if (entry != null && entry.getCreatedAt() < timeLimit) {
                        buffer[(head + i) % capacity] = null;
                        removed++;
                    }
                }

                if (removed > 0) {
                    int newIndex = 0;
                    for (int i = 0; i < size; i++) {
                        LogEntry entry = buffer[(head + i) % capacity];
                        if (entry != null) {
                            buffer[newIndex] = entry;
                            newIndex++;
                        }
                    }

                    for (int i = newIndex; i < size; i++) {
                        buffer[i] = null;
                    }

                    size -= removed;
                    head = 0;
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        public long countMatching(long timeStart, long timeEnd, Set<Integer> actionIds) {
            long count = 0;
            long stamp = lock.readLock();
            try {
                for (int i = 0; i < size; i++) {
                    LogEntry entry = buffer[(head + i) % capacity];
                    if (entry != null) {
                        long createdAt = entry.getCreatedAt();
                        if (createdAt >= timeStart && createdAt <= timeEnd) {
                            if (actionIds == null || actionIds.contains(entry.getActionType())) {
                                count++;
                            }
                        }
                    }
                }
            } finally {
                lock.unlockRead(stamp);
            }
            return count;
        }

    }

    private static class TimestampedHashMap<K, V> extends ConcurrentHashMap<K, V> {

        private final long ttl;
        private final ConcurrentHashMap<K, Long> timestamps;

        public TimestampedHashMap(int initialCapacity, long ttlMillis) {
            super(initialCapacity);
            this.ttl = ttlMillis;
            this.timestamps = new ConcurrentHashMap<>(initialCapacity);
        }

        @Override
        public V put(K key, V value) {
            timestamps.put(key, System.currentTimeMillis());
            return super.put(key, value);
        }

        public void cleanup(long currentTime) {
            timestamps.entrySet().removeIf(entry -> {
                boolean expired = (currentTime - entry.getValue()) > ttl;
                if (expired) {
                    super.remove(entry.getKey());
                }
                return expired;
            });
        }

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

    @Getter
    private static class CachedQuery {

        private final List<LogEntry> logs;
        private final long expireTime;

        public CachedQuery(List<LogEntry> logs, long expireTime) {
            this.logs = logs;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

    }

}