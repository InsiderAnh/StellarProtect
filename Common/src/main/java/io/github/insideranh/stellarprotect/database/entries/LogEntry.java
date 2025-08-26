package io.github.insideranh.stellarprotect.database.entries;

import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.Objects;

@Getter
public class LogEntry {

    // Last ID = 5
    protected final long playerId;
    protected final int worldId;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final int actionType;
    protected final long createdAt;

    public LogEntry(Document document) {
        this.playerId = document.containsKey("player_id") ? document.getLong("player_id") : -2L;
        this.worldId = document.getInteger("world_id");
        this.x = document.getDouble("x");
        this.y = document.getDouble("y");
        this.z = document.getDouble("z");
        this.actionType = document.getInteger("action_type");
        this.createdAt = document.getLong("created_at");
    }

    @SneakyThrows
    public LogEntry(ResultSet resultSet) {
        this.playerId = resultSet.getLong("player_id");
        this.worldId = resultSet.getInt("world_id");
        this.x = resultSet.getDouble("x");
        this.y = resultSet.getDouble("y");
        this.z = resultSet.getDouble("z");
        this.actionType = resultSet.getInt("action_type");
        this.createdAt = resultSet.getLong("created_at");
    }

    public LogEntry(long playerId, int actionType, int worldId, double x, double y, double z, long createdAt) {
        this.playerId = playerId;
        this.worldId = worldId;
        this.x = Math.round(x * 100.0) / 100.0;
        this.y = Math.round(y * 100.0) / 100.0;
        this.z = Math.round(z * 100.0) / 100.0;
        this.actionType = actionType;
        this.createdAt = createdAt;
    }

    public LogEntry(long playerId, int actionType, Location location, long createdAt) {
        this.playerId = playerId;
        this.actionType = actionType;
        this.worldId = WorldUtils.getShortId(location.getWorld().getName());
        this.x = Math.round(location.getX() * 100.0) / 100.0;
        this.y = Math.round(location.getY() * 100.0) / 100.0;
        this.z = Math.round(location.getZ() * 100.0) / 100.0;
        this.createdAt = createdAt;
    }

    public LocationCache asLocation() {
        return LocationCache.of(worldId, (int) x, (int) y, (int) z);
    }

    public Location asBukkitLocation() {
        return new Location(Bukkit.getWorld(WorldUtils.getWorld(worldId)), x, y, z);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
            "worldId=" + worldId +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", actionType=" + actionType +
            ", created_at=" + createdAt +
            '}';
    }

    public String getDataString() {
        return "";
    }

    public String toSaveJson() {
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return playerId == logEntry.playerId && worldId == logEntry.worldId && Double.compare(x, logEntry.x) == 0 && Double.compare(y, logEntry.y) == 0 && Double.compare(z, logEntry.z) == 0 && actionType == logEntry.actionType && createdAt == logEntry.createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, worldId, x, y, z, actionType, createdAt);
    }

}