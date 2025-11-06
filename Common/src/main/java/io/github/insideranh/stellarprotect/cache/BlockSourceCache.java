package io.github.insideranh.stellarprotect.cache;

import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class BlockSourceCache {

    private static final BlockSourceCache INSTANCE = createMedium();
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000L;

    private final Map<LocationCache, CacheEntry> cache;
    private final long expiryMillis;
    private long lastCleanupTime;

    public BlockSourceCache(int initialCapacity, int expiryMinutes) {
        this.cache = new HashMap<>(initialCapacity);
        this.expiryMillis = expiryMinutes * 60_000L;
        this.lastCleanupTime = System.currentTimeMillis();
    }

    public static Long getPlayerId(Location location) {
        CacheEntry entry = INSTANCE.get(LocationCache.of(location));
        if (entry != null) {
            return entry.playerId;
        }
        return null;
    }

    public static void registerBlockSource(@Nullable Location newLocation, long playerId) {
        if (newLocation == null) return;
        INSTANCE.put(LocationCache.of(newLocation), playerId, newLocation);
    }

    public static void registerBlockSource(@Nullable Location sourceKey, long playerId, @Nullable Location newLocation) {
        if (newLocation == null) return;
        if (sourceKey == null) {
            registerBlockSource(newLocation, playerId);
            return;
        }
        INSTANCE.put(LocationCache.of(sourceKey), playerId, newLocation);
    }

    public static void removeBlockSource(Location location) {
        INSTANCE.remove(LocationCache.of(location));
    }

    public static void cleanup() {
        INSTANCE.cleanupExpired();
    }

    // Próximamente configurable tamaños de cache
    public static BlockSourceCache createSmall() {
        return new BlockSourceCache(2048, 10);
    }

    public static BlockSourceCache createMedium() {
        return new BlockSourceCache(4096, 10);
    }

    // Próximamente configurable tamaños de cache
    public static BlockSourceCache createLarge() {
        return new BlockSourceCache(8192, 10);
    }

    // Próximamente configurable tamaños de cache
    public static BlockSourceCache createXLarge() {
        return new BlockSourceCache(16384, 10);
    }

    private CacheEntry put(LocationCache sourceKey, long playerId, Location location) {
        return put(sourceKey, playerId, location.getBlockX(), location.getBlockY(), location.getBlockZ(), sourceKey.getWorldId());
    }

    private CacheEntry put(LocationCache sourceKey, long playerId, int x, int y, int z, int worldId) {
        long now = System.currentTimeMillis();

        if (now - lastCleanupTime > CLEANUP_INTERVAL_MILLIS) {
            cleanupExpired();
            lastCleanupTime = now;
        }

        CacheEntry entry = new CacheEntry(playerId, x, y, z, worldId);
        cache.put(sourceKey, entry);
        return entry;
    }

    private CacheEntry get(LocationCache sourceKey) {
        CacheEntry entry = cache.get(sourceKey);
        if (entry != null) {
            entry.lastAccessTime = System.currentTimeMillis();
        }
        return entry;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        long threshold = now - expiryMillis;

        cache.entrySet().removeIf(entry -> entry.getValue().lastAccessTime < threshold);
    }

    private void remove(LocationCache sourceKey) {
        cache.remove(sourceKey);
    }

    private static class CacheEntry {

        final long playerId;
        final int x;
        final int y;
        final int z;
        final int worldId;
        long lastAccessTime;

        CacheEntry(long playerId, int x, int y, int z, int worldId) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldId = worldId;
            this.lastAccessTime = System.currentTimeMillis();
        }

    }

}
