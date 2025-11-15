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
public class PlayerHangingEntry extends LogEntry {

    private final String entityType;

    public PlayerHangingEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.entityType = jsonObject.get("et").getAsString();
    }

    public PlayerHangingEntry(long playerId, Location location, Entity entity) {
        super(playerId, ActionType.HANGING.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.entityType = entity.getType().name();
    }

    @Override
    public String getDataString() {
        return entityType;
    }

    @Override
    public String toSaveJson() {
        return "{\"et\":\"" + entityType + "\"}";
    }

}
