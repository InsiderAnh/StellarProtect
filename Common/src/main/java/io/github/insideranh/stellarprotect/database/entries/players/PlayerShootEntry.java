package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

@Getter
public class PlayerShootEntry extends LogEntry {

    // success = 0 (0 = miss, 1 = hit)
    private final String entityType;
    private final byte success;

    private final String shootEntityType;

    public PlayerShootEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.entityType = jsonObject.get("et").getAsString();
        this.success = jsonObject.has("s") ? jsonObject.get("s").getAsByte() : 0;

        this.shootEntityType = jsonObject.has("st") ? jsonObject.get("st").getAsString() : "";
    }

    public PlayerShootEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.entityType = jsonObject.get("et").getAsString();
        this.success = jsonObject.has("s") ? jsonObject.get("s").getAsByte() : 0;

        this.shootEntityType = jsonObject.has("st") ? jsonObject.get("st").getAsString() : "";
    }

    public PlayerShootEntry(long playerId, Location location, Entity projectile, boolean shoot) {
        super(playerId, ActionType.SHOOT.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.entityType = projectile.getType().name();
        this.success = (byte) (shoot ? 1 : 0);

        this.shootEntityType = "";
    }

    public PlayerShootEntry(long playerId, Location location, Entity projectile, Entity shootEntity, boolean shoot) {
        super(playerId, ActionType.SHOOT.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.entityType = projectile.getType().name();
        this.success = (byte) (shoot ? 1 : 0);

        this.shootEntityType = shootEntity.getType().name();
    }

    public PlayerShootEntry(long playerId, Location location, Entity projectile, Player shootEntity, boolean shoot) {
        super(playerId, ActionType.SHOOT.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.entityType = projectile.getType().name();
        this.success = (byte) (shoot ? 1 : 0);

        this.shootEntityType = shootEntity.getName();
    }

    @Override
    public String getDataString() {
        return entityType;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerShootEntry entry = this;
        obj.addProperty("et", entry.getEntityType());
        if (success != 0) {
            obj.addProperty("s", entry.getSuccess());
        }
        if (!shootEntityType.isEmpty()) {
            obj.addProperty("st", entry.getShootEntityType());
        }
        return obj.toString();
    }

}
