package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerFurnitureLogEntry extends LogEntry {

    private final String nexoBlockId;

    public PlayerFurnitureLogEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.nexoBlockId = jsonObject.get("nbId").getAsString();
    }

    @SneakyThrows
    public PlayerFurnitureLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.nexoBlockId = jsonObject.get("nbId").getAsString();
    }

    public PlayerFurnitureLogEntry(long playerId, Location location, ActionType actionType, String nexoBlockId) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.nexoBlockId = nexoBlockId;
    }

    @Override
    public String getDataString() {
        return nexoBlockId;
    }

    @Override
    public String toSaveJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("nbId", nexoBlockId);
        return jsonObject.toString();
    }

}