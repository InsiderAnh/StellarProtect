package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.entity.Entity;

import java.sql.ResultSet;

@Getter
public class PlayerKillLogEntry extends LogEntry {

    private final String entityType;

    public PlayerKillLogEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.entityType = jsonObject.get("et").getAsString();
    }

    public PlayerKillLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.entityType = jsonObject.get("et").getAsString();
    }

    public PlayerKillLogEntry(long playerId, Entity killed, ActionType actionType) {
        super(playerId, actionType.getId(), killed.getLocation(), System.currentTimeMillis());
        this.entityType = killed.getType().name();
    }

    @Override
    public String getDataString() {
        return entityType;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerKillLogEntry entry = this;
        obj.addProperty("et", entry.getEntityType());

        return obj.toString();
    }

}