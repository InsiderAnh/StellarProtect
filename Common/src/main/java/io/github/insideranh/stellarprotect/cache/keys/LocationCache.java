package io.github.insideranh.stellarprotect.cache.keys;

import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Objects;

@Getter
public class LocationCache {

    private final int worldId;
    private final int x;
    private final int y;
    private final int z;

    public LocationCache(int worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static LocationCache of(LogEntry logEntry) {
        return of(logEntry.getWorldId(), (int) logEntry.getX(), (int) logEntry.getY(), (int) logEntry.getZ());
    }

    public static LocationCache of(Location location) {
        return new LocationCache(WorldUtils.getShortId(location.getWorld().getName()), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static LocationCache of(int worldId, int x, int y, int z) {
        return new LocationCache(worldId, x, y, z);
    }

    public boolean isInside(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        return minX <= this.x && maxX >= this.x && minY <= this.y && maxY >= this.y && minZ <= this.z && maxZ >= this.z;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LocationCache that = (LocationCache) o;
        return worldId == that.worldId && x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        int result = worldId;
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "LocationCache{" +
            "worldId=" + worldId +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }

}