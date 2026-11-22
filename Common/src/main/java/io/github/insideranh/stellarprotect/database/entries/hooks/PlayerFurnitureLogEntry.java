package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerFurnitureLogEntry extends LogEntry {

    private final String customBlockId;

    @SneakyThrows
    public PlayerFurnitureLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.customBlockId = jsonObject.get("nbId").getAsString();
    }

    public PlayerFurnitureLogEntry(long playerId, Location location, ActionType actionType, String customBlockId, String hookId) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.customBlockId = hookId + customBlockId;
    }

    @Override
    public String getDataString() {
        return customBlockId;
    }

    @Override
    public String toSaveJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("nbId", customBlockId);
        return jsonObject.toString();
    }

}