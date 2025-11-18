package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.sql.ResultSet;

@Getter
public class PlayerLeashEntry extends LogEntry {

    // mount = 0 (0 = leash, 1 = unleash)
    private final String entityType;
    private final byte leash;

    public PlayerLeashEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.entityType = jsonObject.get("et").getAsString();
        this.leash = jsonObject.get("mt").getAsByte();
    }

    public PlayerLeashEntry(long playerId, Location location, Entity entity, boolean leash) {
        super(playerId, ActionType.LEASH.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.entityType = entity.getType().name();
        this.leash = (byte) (leash ? 1 : 0);
    }

    @Override
    public String getDataString() {
        return entityType;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerLeashEntry entry = this;
        obj.addProperty("et", entry.getEntityType());
        obj.addProperty("mt", entry.getLeash());

        return obj.toString();
    }

}
