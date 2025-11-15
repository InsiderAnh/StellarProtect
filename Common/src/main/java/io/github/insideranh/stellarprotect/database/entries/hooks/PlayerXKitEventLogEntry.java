package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerXKitEventLogEntry extends LogEntry {

    private final byte eventType;
    private final String kitId;

    @SneakyThrows
    public PlayerXKitEventLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.eventType = jsonObject.get("e").getAsByte();
        this.kitId = jsonObject.get("k").getAsString();
    }

    // eventType = 0 -> claim
    // eventType = 1 -> give
    public PlayerXKitEventLogEntry(long playerId, Location location, byte eventType, String kitId) {
        super(playerId, ActionType.X_KIT_EVENT.getId(), location, System.currentTimeMillis());

        this.eventType = eventType;
        this.kitId = kitId;
    }

    @Override
    public String getDataString() {
        return kitId;
    }

    @Override
    public String toSaveJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("e", eventType);
        jsonObject.addProperty("k", kitId);
        return jsonObject.toString();
    }

}