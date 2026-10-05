package io.github.insideranh.stellarprotect.database.entries;

import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.Objects;

@Getter
public class LogEntry {

    protected final long id;
    protected final long playerId;
    protected final int worldId;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final int actionType;
    protected final long createdAt;
    protected Integer blockId;
    protected Integer oldBlockId;
    protected Long itemId;
    protected Integer amount;
    protected String entityType;
    protected Long chunkKey;
    private byte restored;
    @Setter
    private String forcedJson;

    @SneakyThrows
    public LogEntry(ResultSet resultSet) {
        this.id = resultSet.getLong("id");
        this.playerId = resultSet.getLong("player_id");
        this.worldId = resultSet.getInt("world_id");
        this.x = resultSet.getDouble("x");
        this.y = resultSet.getDouble("y");
        this.z = resultSet.getDouble("z");
        this.actionType = resultSet.getInt("action_type");
        this.createdAt = resultSet.getLong("created_at");
        this.restored = resultSet.getByte("restored");
        try {
            this.blockId = readNullableInt(resultSet, "block_id");
        } catch (Exception ignored) {
        }
        try {
            this.oldBlockId = readNullableInt(resultSet, "old_block_id");
        } catch (Exception ignored) {
        }
        try {
            long v = resultSet.getLong("item_id");
            this.itemId = resultSet.wasNull() ? null : v;
        } catch (Exception ignored) {
        }
        try {
            this.amount = resultSet.getInt("amount");
        } catch (Exception ignored) {
        }
        try {
            String e = resultSet.getString("entity_type");
            this.entityType = resultSet.wasNull() ? null : e;
        } catch (Exception ignored) {
        }
        try {
            long k = resultSet.getLong("chunk_key");
            this.chunkKey = resultSet.wasNull() ? null : k;
        } catch (Exception ignored) {
        }
    }

    public LogEntry(long playerId, int actionType, int worldId, double x, double y, double z, long createdAt) {
        this.id = PlayerUtils.getNextLogId();
        this.playerId = playerId;
        this.worldId = worldId;
        this.x = Math.round(x * 100.0) / 100.0;
        this.y = Math.round(y * 100.0) / 100.0;
        this.z = Math.round(z * 100.0) / 100.0;
        this.actionType = actionType;
        this.createdAt = createdAt;
        this.chunkKey = chunkKeyOf(worldId, x, z);
    }

    public LogEntry(long playerId, int actionType, Location location, long createdAt) {
        this.id = PlayerUtils.getNextLogId();
        this.playerId = playerId;
        this.actionType = actionType;
        this.worldId = WorldUtils.getShortId(location.getWorld().getName());
        this.x = Math.round(location.getX() * 100.0) / 100.0;
        this.y = Math.round(location.getY() * 100.0) / 100.0;
        this.z = Math.round(location.getZ() * 100.0) / 100.0;
        this.createdAt = createdAt;
        this.chunkKey = chunkKeyOf(this.worldId, this.x, this.z);
    }

    private static Integer readNullableInt(ResultSet rs, String col) throws Exception {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static Long chunkKeyOf(int worldId, double x, double z) {
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        long hi = worldId & 0xFFFFFFFFL;
        long lo = ((long) (cx & 0xFFFF) << 16) | (cz & 0xFFFF);
        return (hi << 32) | (lo & 0xFFFFFFFFL);
    }

    public LocationCache asLocation() {
        return LocationCache.of(worldId, (int) x, (int) y, (int) z);
    }

    public Location asBukkitLocation() {
        String worldName = WorldUtils.getWorld(worldId);
        org.bukkit.World w = worldName == null ? null : Bukkit.getWorld(worldName);
        return new Location(w, x, y, z);
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    public void setOldBlockId(Integer oldBlockId) {
        this.oldBlockId = oldBlockId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public void setAmountInt(int amount) {
        this.amount = amount;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
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
        if (forcedJson != null) {
            return forcedJson;
        }
        return "";
    }

    public boolean isRestored() {
        return restored != 0;
    }

    public void setRestored(boolean restored) {
        this.restored = (byte) (restored ? 1 : 0);
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